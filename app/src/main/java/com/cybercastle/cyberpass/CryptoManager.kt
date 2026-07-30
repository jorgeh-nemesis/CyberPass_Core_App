package com.cybercastle.cyberpass

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    // Work factor for newly created or rotated vaults. Existing vaults keep the
    // iteration count they were created with (see SecurePrefs.getIterations) so
    // raising this constant never locks out installs created under an older value.
    const val ITERATIONS = 600_000

    // Legacy default for vaults created before iteration count was persisted.
    const val LEGACY_ITERATIONS = 100_000

    private const val KEY_LENGTH = 256 // bits
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12 // GCM recommended IV length

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, ALGORITHM)
    }

    // At 600,000 iterations PBKDF2 takes anywhere from ~0.5s to a few seconds
    // on mobile CPUs - calling deriveKey() straight from a UI callback blocks
    // the main thread long enough to trigger an ANR. Always derive through
    // this suspend wrapper from a coroutine instead.
    suspend fun deriveKeySuspending(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKey =
        withContext(Dispatchers.Default) {
            deriveKey(password, salt, iterations)
        }

    fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        // Prepend IV to encrypted data
        return iv + encrypted
    }

    fun decrypt(encryptedData: ByteArray, key: SecretKey): ByteArray {
        val iv = encryptedData.sliceArray(0 until IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(encryptedData, IV_LENGTH, encryptedData.size - IV_LENGTH)
    }
}