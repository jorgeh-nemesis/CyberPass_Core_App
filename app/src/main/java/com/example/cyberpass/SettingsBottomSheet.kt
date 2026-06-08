package com.example.cyberpass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SectionHeader(label: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                color = Color(0xFF555555),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(bottom = 4.dp),
        )
        HorizontalDivider(color = Color(0xFF222222))
    }
}

@Composable
fun SettingsItem(
    label: String,
    sublabel: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF252525))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2EFC54),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (!sublabel.isNullOrBlank()) {
                    Text(
                        text = sublabel,
                        style = TextStyle(
                            color = Color(0xFF555555),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
fun <T> ChipSelector(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selectedOption
            Surface(
                onClick = { onOptionSelected(value) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0x152EFC54) else Color(0xFF1A1A1A),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0x502EFC54) else Color(0xFF282828)
                )
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = TextStyle(
                            color = if (isSelected) Color(0xFF2EFC54) else Color(0xFF666666),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: MainViewModel,
    onBackupComplete: (Boolean) -> Unit,
    onRestoreComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val showChangePasswordDialog = remember { mutableStateOf(value = false) }
    
    val biometricEnabled = remember { mutableStateOf(SecurePrefs.isBiometricEnabled(context)) }

    val bioPromptTitle = stringResource(R.string.biometric_prompt_title)
    val bioPromptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val bioNegativeButton = stringResource(R.string.biometric_negative_button)

    fun toggleBiometric(enabled: Boolean) {
        if (enabled) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                try {
                    SecurityManager.createBiometricKey()
                    val cipher = SecurityManager.getEncryptCipher()
                    BiometricAuthManager.showBiometricPrompt(
                        activity = activity,
                        title = bioPromptTitle,
                        subtitle = bioPromptSubtitle,
                        negativeButtonText = bioNegativeButton,
                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                        onSuccess = { result ->
                            val authenticatedCipher = result.cryptoObject?.cipher
                            val key = viewModel.getEncryptionKey()
                            if ((key != null) && (authenticatedCipher != null)) {
                                try {
                                    val iv = authenticatedCipher.iv
                                    val encrypted = authenticatedCipher.doFinal(key.encoded)
                                    SecurePrefs.saveEncryptedKey(context, iv + encrypted)
                                    SecurePrefs.setBiometricEnabled(context, true)
                                    biometricEnabled.value = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onError = { err ->
                            coroutineScope.launch {
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            SecurePrefs.setBiometricEnabled(context, false)
            biometricEnabled.value = false
        }
    }

    // Observe current language preference
    val languageFlow = LanguageManager.getLanguageFlow(context)
    val currentLanguage by languageFlow.collectAsState(initial = LanguageManager.LANGUAGE_DEFAULT)

    if (showChangePasswordDialog.value) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog.value = false },
            onSuccess = {
                // Success is handled inside the dialog or by re-loading entries
            },
            viewModel = viewModel
        )
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val success = try {
                val file = File(context.filesDir, "passwords.enc")
                if (file.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { `in` ->
                            `in`.copyTo(out)
                        }
                    }
                    true
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            onBackupComplete(success)
        } else {
            onBackupComplete(false)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = try {
                context.contentResolver.openInputStream(uri)?.use { `in` ->
                    val destFile = File(context.filesDir, "passwords.enc")
                    FileOutputStream(destFile).use { out ->
                        `in`.copyTo(out)
                    }
                }
                viewModel.loadEntries()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            onRestoreComplete(success)
        } else {
            onRestoreComplete(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF2EFC54),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dados Section
        SectionHeader(stringResource(R.string.section_data))
        SettingsItem(
            label = stringResource(R.string.backup_database),
            sublabel = stringResource(R.string.export_data_sublabel),
            icon = Icons.Default.Save,
            onClick = { backupLauncher.launch("cyberpass_backup.enc") }
        )
        SettingsItem(
            label = stringResource(R.string.restore_database),
            sublabel = stringResource(R.string.import_data_sublabel),
            icon = Icons.Default.Restore,
            onClick = { restoreLauncher.launch("application/octet-stream") }
        )
        SettingsItem(
            label = stringResource(R.string.change_master_password),
            icon = Icons.Default.Lock,
            onClick = { showChangePasswordDialog.value = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Segurança Section
        SectionHeader(stringResource(R.string.section_security))
        SettingsItem(
            label = stringResource(R.string.biometric_unlock),
            sublabel = stringResource(R.string.biometric_sublabel),
            icon = Icons.Default.Fingerprint,
            onClick = { toggleBiometric(!biometricEnabled.value) },
            trailingContent = {
                Switch(
                    checked = biometricEnabled.value,
                    onCheckedChange = { toggleBiometric(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF2EFC54),
                        checkedTrackColor = Color(0x302EFC54),
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Idioma Section
        SectionHeader(stringResource(R.string.section_language))
        ChipSelector(
            options = listOf(
                LanguageManager.LANGUAGE_DEFAULT to "🇺🇸 English",
                LanguageManager.LANGUAGE_PORTUGUESE to "🇧🇷 Português"
            ),
            selectedOption = currentLanguage,
            onOptionSelected = { lang ->
                coroutineScope.launch { 
                    LanguageManager.setLanguage(context, lang)
                    LanguageManager.applyLanguage(lang)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
