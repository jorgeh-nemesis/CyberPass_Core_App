package com.cybercastle.cyberpass

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class LoadResult {
    data class Success(val entries: List<PasswordEntry>) : LoadResult()
    data class Failure(val error: Throwable) : LoadResult()
}

sealed class SaveResult {
    object Success : SaveResult()
    data class Failure(val error: Throwable) : SaveResult()
}

class PasswordRepository(private val context: Context) {
    private val fileName = "passwords.enc"
    private val file: File
        get() = File(context.filesDir, fileName)

    private val gson = Gson()
    private var encryptionKey: javax.crypto.SecretKey? = null

    fun setEncryptionKey(key: javax.crypto.SecretKey) {
        encryptionKey = key
    }

    fun getEncryptionKey(): javax.crypto.SecretKey? = encryptionKey

    fun clearEncryptionKey() {
        encryptionKey = null
    }

    suspend fun loadEntries(): LoadResult = withContext(Dispatchers.IO) {
        val key = encryptionKey ?: return@withContext LoadResult.Success(emptyList())
        if (!file.exists()) return@withContext LoadResult.Success(emptyList())
        try {
            val encryptedData = file.readBytes()
            val json = CryptoManager.decrypt(encryptedData, key).toString(Charsets.UTF_8)
            val type = object : TypeToken<List<PasswordEntry>>() {}.type
            LoadResult.Success(gson.fromJson(json, type) ?: emptyList())
        } catch (e: Exception) {
            // Only log the exception type - never e.message/toString(), and
            // never the entries/decrypted payload. A garbled decrypt (wrong
            // key, corruption) could echo plaintext fragments into either.
            Log.e(TAG, "Failed to load vault (${e.javaClass.simpleName})")
            LoadResult.Failure(e)
        }
    }

    suspend fun saveEntries(entries: List<PasswordEntry>): SaveResult = withContext(Dispatchers.IO) {
        val key = encryptionKey
            ?: return@withContext SaveResult.Failure(IllegalStateException("No encryption key set"))
        try {
            val json = gson.toJson(entries)
            val encrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
            file.writeBytes(encrypted)
            SaveResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save vault (${e.javaClass.simpleName})")
            SaveResult.Failure(e)
        }
    }

    companion object {
        private const val TAG = "PasswordRepository"
    }
}
