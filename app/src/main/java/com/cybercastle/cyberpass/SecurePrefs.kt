@file:Suppress("DEPRECATION")

package com.cybercastle.cyberpass

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_SALT = "salt"
    private const val KEY_VERIFIER = "verifier"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_ENCRYPTED_KEY = "encrypted_key"
    private const val KEY_ITERATIONS = "kdf_iterations"

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
        getPrefs(context).edit { putString(KEY_SALT, encodeBytes(salt)) }
    }

    fun getSalt(context: Context): ByteArray? {
        val saltStr = getPrefs(context).getString(KEY_SALT, null) ?: return null
        return decodeBytes(saltStr)
    }

    fun saveVerifier(context: Context, verifier: ByteArray) {
        getPrefs(context).edit { putString(KEY_VERIFIER, encodeBytes(verifier)) }
    }

    fun getVerifier(context: Context): ByteArray? {
        val verifierStr = getPrefs(context).getString(KEY_VERIFIER, null) ?: return null
        return decodeBytes(verifierStr)
    }

    fun saveIterations(context: Context, iterations: Int) {
        getPrefs(context).edit { putInt(KEY_ITERATIONS, iterations) }
    }

    // Vaults created before this field existed derived their key with
    // CryptoManager.LEGACY_ITERATIONS, so that's the safe fallback.
    fun getIterations(context: Context): Int {
        return getPrefs(context).getInt(KEY_ITERATIONS, CryptoManager.LEGACY_ITERATIONS)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun saveEncryptedKey(context: Context, encryptedKey: ByteArray) {
        getPrefs(context).edit { putString(KEY_ENCRYPTED_KEY, encodeBytes(encryptedKey)) }
    }

    fun getEncryptedKey(context: Context): ByteArray? {
        val keyStr = getPrefs(context).getString(KEY_ENCRYPTED_KEY, null) ?: return null
        return decodeBytes(keyStr)
    }

    private fun encodeBytes(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    // TODO(remove after v1.1): installs that saved these values before this
    // change wrote them as comma-joined decimal strings (e.g. "18,-3,127"),
    // which aren't valid Base64. Fall back to that legacy parse once so
    // upgrading users aren't locked out; every save* call above rewrites the
    // value as Base64, so the fallback naturally stops being hit over time.
    private fun decodeBytes(value: String): ByteArray {
        return try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            value.split(",").map { it.toByte() }.toByteArray()
        }
    }
}