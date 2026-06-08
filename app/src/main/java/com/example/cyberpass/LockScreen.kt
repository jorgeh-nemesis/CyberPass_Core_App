package com.example.cyberpass

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import javax.crypto.spec.SecretKeySpec

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    val isFirstTime = remember { SecurePrefs.getSalt(context) == null }
    val isBiometricEnabled = remember { SecurePrefs.isBiometricEnabled(context) }
    var error by remember { mutableStateOf<String?>(null) }

    val incorrectPasswordMsg = stringResource(R.string.incorrect_password)
    val bioDecryptionFailedMsg = stringResource(R.string.biometric_decryption_failed)
    val bioInitFailedMsg = stringResource(R.string.biometric_init_failed)

    val bioPromptTitle = stringResource(R.string.biometric_prompt_title)
    val bioPromptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val bioNegativeButton = stringResource(R.string.biometric_negative_button)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isFirstTime) stringResource(R.string.create_master_password) else stringResource(R.string.enter_master_password),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.master_password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isFirstTime) {
                    val salt = CryptoManager.generateSalt()
                    val key = CryptoManager.deriveKey(password.toCharArray(), salt)
                    SecurePrefs.saveSalt(context, salt)
                    val verifierHash = CryptoManager.deriveKey(password.toCharArray(), salt).encoded
                    SecurePrefs.saveVerifier(context, verifierHash)
                    viewModel.setEncryptionKey(key)
                    onUnlocked()
                } else {
                    val salt = SecurePrefs.getSalt(context) ?: return@Button
                    val storedVerifier = SecurePrefs.getVerifier(context) ?: return@Button
                    val key = CryptoManager.deriveKey(password.toCharArray(), salt)
                    if (key.encoded.contentEquals(storedVerifier)) {
                        viewModel.setEncryptionKey(key)
                        onUnlocked()
                    } else {
                        error = incorrectPasswordMsg
                    }
                }
            },
            enabled = password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isFirstTime) stringResource(R.string.create) else stringResource(R.string.unlock))
        }

        if (!isFirstTime && isBiometricEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    val encryptedKey = SecurePrefs.getEncryptedKey(context)
                    if (activity != null && encryptedKey != null) {
                        try {
                            val iv = encryptedKey.sliceArray(0 until 12)
                            val cipher = SecurityManager.getDecryptCipher(iv)
                            BiometricAuthManager.showBiometricPrompt(
                                activity = activity,
                                title = bioPromptTitle,
                                subtitle = bioPromptSubtitle,
                                negativeButtonText = bioNegativeButton,
                                cryptoObject = androidx.biometric.BiometricPrompt.CryptoObject(cipher),
                                onSuccess = { result ->
                                    try {
                                        val authenticatedCipher = result.cryptoObject?.cipher
                                        if (authenticatedCipher != null) {
                                            val decryptedRawKey = authenticatedCipher.doFinal(
                                                encryptedKey, 12, encryptedKey.size - 12
                                            )
                                            val key = SecretKeySpec(decryptedRawKey, "AES")
                                            viewModel.setEncryptionKey(key)
                                            onUnlocked()
                                        }
                                    } catch (_: Exception) {
                                        error = bioDecryptionFailedMsg
                                    }
                                },
                                onError = { err -> error = err }
                            )
                        } catch (_: Exception) {
                            error = bioInitFailedMsg
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.unlock_with_biometrics))
            }
        }
    }
}
