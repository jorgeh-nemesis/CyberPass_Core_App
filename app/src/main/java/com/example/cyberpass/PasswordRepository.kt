package com.example.cyberpass

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    suspend fun loadEntries(): List<PasswordEntry> = withContext(Dispatchers.IO) {
        val key = encryptionKey ?: return@withContext emptyList()
        if (!file.exists()) return@withContext emptyList()
        try {
            val encryptedData = file.readBytes()
            val json = CryptoManager.decrypt(encryptedData, key).toString(Charsets.UTF_8)
            val type = object : TypeToken<List<PasswordEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveEntries(entries: List<PasswordEntry>) = withContext(Dispatchers.IO) {
        val key = encryptionKey ?: return@withContext
        try {
            val json = gson.toJson(entries)
            val encrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), key)
            file.writeBytes(encrypted)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}