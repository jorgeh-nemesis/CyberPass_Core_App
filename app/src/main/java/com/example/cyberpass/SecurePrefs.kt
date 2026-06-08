@file:Suppress("DEPRECATION")

package com.example.cyberpass

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_SALT = "salt"
    private const val KEY_VERIFIER = "verifier"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_ENCRYPTED_KEY = "encrypted_key"

    @Suppress("DEPRECATION")
    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSalt(context: Context, salt: ByteArray) {
        getPrefs(context).edit { putString(KEY_SALT, salt.joinToString(",")) }
    }

    fun getSalt(context: Context): ByteArray? {
        val saltStr = getPrefs(context).getString(KEY_SALT, null) ?: return null
        return saltStr.split(",").map { it.toByte() }.toByteArray()
    }

    fun saveVerifier(context: Context, verifier: ByteArray) {
        getPrefs(context).edit { putString(KEY_VERIFIER, verifier.joinToString(",")) }
    }

    fun getVerifier(context: Context): ByteArray? {
        val verifierStr = getPrefs(context).getString(KEY_VERIFIER, null) ?: return null
        return verifierStr.split(",").map { it.toByte() }.toByteArray()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun saveEncryptedKey(context: Context, encryptedKey: ByteArray) {
        getPrefs(context).edit { putString(KEY_ENCRYPTED_KEY, encryptedKey.joinToString(",")) }
    }

    fun getEncryptedKey(context: Context): ByteArray? {
        val keyStr = getPrefs(context).getString(KEY_ENCRYPTED_KEY, null) ?: return null
        return keyStr.split(",").map { it.toByte() }.toByteArray()
    }
}