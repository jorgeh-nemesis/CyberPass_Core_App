package com.cybercastle.cyberpass

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Packs the vault's encrypted database together with the KDF salt,
 * iteration count, and verifier into a single self-contained archive, so a
 * backup file alone (without any prior on-device state) is enough to
 * restore and unlock the vault on a fresh install.
 *
 * Layout: MAGIC(4) | VERSION(1) | saltLen(1)+salt | iterations(4) |
 * verifierLen(1)+verifier | payloadLen(4)+payload
 */
object BackupManager {
    private val MAGIC = "CPBK".toByteArray(Charsets.US_ASCII)
    private const val VERSION = 1
    private const val VAULT_FILE_NAME = "passwords.enc"

    class InvalidBackupException(message: String) : Exception(message)

    fun createBackup(context: Context): ByteArray? {
        val vaultFile = File(context.filesDir, VAULT_FILE_NAME)
        if (!vaultFile.exists()) return null
        val salt = SecurePrefs.getSalt(context) ?: return null
        val verifier = SecurePrefs.getVerifier(context) ?: return null
        val iterations = SecurePrefs.getIterations(context)
        val payload = vaultFile.readBytes()

        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { dos ->
            dos.write(MAGIC)
            dos.writeByte(VERSION)
            dos.writeByte(salt.size)
            dos.write(salt)
            dos.writeInt(iterations)
            dos.writeByte(verifier.size)
            dos.write(verifier)
            dos.writeInt(payload.size)
            dos.write(payload)
        }
        return out.toByteArray()
    }

    /**
     * Restores salt/iterations/verifier and the encrypted payload from a
     * backup archive. Returns true on success. The caller is responsible for
     * re-locking the vault afterward since the currently held in-memory key
     * (if any) no longer matches the restored vault.
     */
    fun restoreBackup(context: Context, data: ByteArray): Boolean {
        return try {
            DataInputStream(ByteArrayInputStream(data)).use { input ->
                val magic = ByteArray(MAGIC.size)
                input.readFully(magic)
                if (!magic.contentEquals(MAGIC)) {
                    throw InvalidBackupException("Not a CyberPass backup file")
                }
                val version = input.readByte().toInt()
                if (version != VERSION) {
                    throw InvalidBackupException("Unsupported backup version: $version")
                }
                val saltLen = input.readUnsignedByte()
                val salt = ByteArray(saltLen).also { input.readFully(it) }
                val iterations = input.readInt()
                val verifierLen = input.readUnsignedByte()
                val verifier = ByteArray(verifierLen).also { input.readFully(it) }
                val payloadLen = input.readInt()
                val payload = ByteArray(payloadLen).also { input.readFully(it) }

                SecurePrefs.saveSalt(context, salt)
                SecurePrefs.saveIterations(context, iterations)
                SecurePrefs.saveVerifier(context, verifier)

                // Any biometric-wrapped key was encrypted under the previous
                // vault's key; it can't unwrap the restored vault's key.
                SecurePrefs.setBiometricEnabled(context, false)
                SecurityManager.deleteBiometricKey()

                File(context.filesDir, VAULT_FILE_NAME).writeBytes(payload)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
