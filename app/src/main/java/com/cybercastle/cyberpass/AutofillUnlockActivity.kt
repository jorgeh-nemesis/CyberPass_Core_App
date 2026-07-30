package com.cybercastle.cyberpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import kotlinx.coroutines.launch

/**
 * Launched via the autofill authentication PendingIntent when the vault is
 * locked. Prompts for the master password, decrypts the vault in-process,
 * lets the user pick which saved entry to use, then returns it as the
 * dataset for the field(s) the requesting app/browser asked to fill.
 */
@RequiresApi(Build.VERSION_CODES.O)
class AutofillUnlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usernameId = IntentCompat.getParcelableExtra(intent, EXTRA_USERNAME_ID, AutofillId::class.java)
        val passwordId = IntentCompat.getParcelableExtra(intent, EXTRA_PASSWORD_ID, AutofillId::class.java)
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val appPackage = intent.getStringExtra(EXTRA_APP_PACKAGE)

        setContent {
            CyberPassTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AutofillUnlockScreen(
                        webDomain = webDomain,
                        appPackage = appPackage,
                        onEntrySelected = { entry -> finishWithDataset(entry, usernameId, passwordId) },
                        onCancelled = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun finishWithDataset(entry: PasswordEntry, usernameId: AutofillId?, passwordId: AutofillId?) {
        val builder = Dataset.Builder()
        usernameId?.let { builder.setValue(it, AutofillValue.forText(entry.username)) }
        passwordId?.let { builder.setValue(it, AutofillValue.forText(entry.password)) }
        val result = Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, builder.build())
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        private const val EXTRA_USERNAME_ID = "extra_username_id"
        private const val EXTRA_PASSWORD_ID = "extra_password_id"
        private const val EXTRA_WEB_DOMAIN = "extra_web_domain"
        private const val EXTRA_APP_PACKAGE = "extra_app_package"

        fun createIntent(context: Context, target: AutofillTarget): Intent {
            return Intent(context, AutofillUnlockActivity::class.java).apply {
                putExtra(EXTRA_USERNAME_ID, target.usernameId)
                putExtra(EXTRA_PASSWORD_ID, target.passwordId)
                putExtra(EXTRA_WEB_DOMAIN, target.webDomain)
                putExtra(EXTRA_APP_PACKAGE, target.appPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

@Composable
private fun AutofillUnlockScreen(
    webDomain: String?,
    appPackage: String?,
    onEntrySelected: (PasswordEntry) -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var matches by remember { mutableStateOf<List<PasswordEntry>?>(null) }
    var isUnlocking by remember { mutableStateOf(false) }

    val incorrectPasswordMsg = stringResource(R.string.incorrect_password)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = if (matches == null) Arrangement.Center else Arrangement.Top
    ) {
        val entryMatches = matches
        if (entryMatches == null) {
            Text(
                text = stringResource(R.string.autofill_unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.master_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = LocalTextStyle.current.merge(MonoCredentialStyle),
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancelled, enabled = !isUnlocking) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = password.isNotBlank() && !isUnlocking,
                    onClick = {
                        isUnlocking = true
                        scope.launch {
                            val salt = SecurePrefs.getSalt(context)
                            val verifier = SecurePrefs.getVerifier(context)
                            if (salt == null || verifier == null) {
                                error = incorrectPasswordMsg
                                isUnlocking = false
                                return@launch
                            }
                            val iterations = SecurePrefs.getIterations(context)
                            val key = CryptoManager.deriveKey(password.toCharArray(), salt, iterations)
                            if (!key.encoded.contentEquals(verifier)) {
                                error = incorrectPasswordMsg
                                isUnlocking = false
                                return@launch
                            }
                            VaultSession.setKey(key)
                            val repo = PasswordRepository(context)
                            repo.setEncryptionKey(key)
                            val entries = repo.loadEntries()
                            val needle = (webDomain ?: appLabelFor(context, appPackage))?.lowercase()
                            matches = if (needle.isNullOrBlank()) {
                                entries
                            } else {
                                entries.filter { it.title.lowercase().contains(needle) || needle.contains(it.title.lowercase()) }
                                    .ifEmpty { entries }
                            }
                            isUnlocking = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.unlock))
                }
            }
        } else {
            Text(
                text = stringResource(R.string.autofill_pick_entry),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(entryMatches, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title) },
                        supportingContent = { Text(entry.username) },
                        modifier = Modifier.clickable { onEntrySelected(entry) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun appLabelFor(context: Context, packageName: String?): String? {
    if (packageName == null) return null
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (_: Exception) {
        packageName
    }
}
