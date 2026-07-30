package com.cybercastle.cyberpass

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

class MainViewModel : ViewModel() {
    private val repository = PasswordRepository(MyApp.appContext)
    private val gson = Gson()

    private val _entries = MutableStateFlow<List<PasswordEntry>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _categoryFilter = MutableStateFlow(CategoryFilter.ALL)
    val categoryFilter: StateFlow<CategoryFilter> = _categoryFilter

    val entries: StateFlow<List<PasswordEntry>> = combine(
        _entries, _searchQuery, _categoryFilter
    ) { entries, query, categoryFilter ->
        entries.filter { entry ->
            val matchesQuery = entry.title.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true)
            matchesQuery && categoryFilter.matches(entry)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(filter: CategoryFilter) {
        _categoryFilter.value = filter
    }

    fun loadEntries() {
        viewModelScope.launch {
            _entries.value = repository.loadEntries()
        }
    }

    fun addEntry(entry: PasswordEntry) {
        viewModelScope.launch {
            val updated = _entries.value.toMutableList().apply { add(entry) }
            repository.saveEntries(updated)
            _entries.value = updated
        }
    }

    fun updateEntry(updatedEntry: PasswordEntry) {
        viewModelScope.launch {
            val updated = _entries.value.map { if (it.id == updatedEntry.id) updatedEntry else it }
            repository.saveEntries(updated)
            _entries.value = updated
        }
    }

    fun deleteEntry(entry: PasswordEntry) {
        viewModelScope.launch {
            val updated = _entries.value.filter { it.id != entry.id }
            repository.saveEntries(updated)
            _entries.value = updated
        }
    }

    fun moveEntry(fromIndex: Int, toIndex: Int) {
        val currentList = _entries.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            Collections.swap(currentList, fromIndex, toIndex)
            _entries.value = currentList
            viewModelScope.launch {
                repository.saveEntries(currentList)
            }
        }
    }

    private var _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    init {
        viewModelScope.launch {
            AppLockManager.lockRequested.collect { requested ->
                if (requested) lock()
            }
        }
    }

    /** Wipes the in-memory key and entries and returns to the lock screen. */
    fun lock() {
        repository.clearEncryptionKey()
        VaultSession.clear()
        _entries.value = emptyList()
        _isLocked.value = true
        AppLockManager.acknowledgeLock()
    }

    fun setEncryptionKey(key: javax.crypto.SecretKey) {
        repository.setEncryptionKey(key)
        VaultSession.setKey(key)
        _isLocked.value = false
        AppLockManager.acknowledgeUnlock()
        loadEntries()
    }

    fun getEncryptionKey(): javax.crypto.SecretKey? {
        // This is a bit insecure to expose but needed for biometric setup
        return repository.getEncryptionKey()
    }

    fun changeMasterPassword(
        context: Context,
        oldPassword: String,
        newPassword: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            // 1. Verify old password and get current encryption key
            val salt = SecurePrefs.getSalt(context)
            val storedVerifier = SecurePrefs.getVerifier(context)
            if (salt == null || storedVerifier == null) {
                onResult(false)
                return@launch
            }

            val oldIterations = SecurePrefs.getIterations(context)
            val oldKey = CryptoManager.deriveKey(oldPassword.toCharArray(), salt, oldIterations)
            if (!oldKey.encoded.contentEquals(storedVerifier)) {
                onResult(false)
                return@launch
            }

            // 2. Decrypt current database using old key
            val encryptedFile = File(context.filesDir, "passwords.enc")
            val entries = if (encryptedFile.exists()) {
                val encryptedData = encryptedFile.readBytes()
                val decryptedJson = CryptoManager.decrypt(encryptedData, oldKey).toString(Charsets.UTF_8)
                val type = object : TypeToken<List<PasswordEntry>>() {}.type
                gson.fromJson<List<PasswordEntry>>(decryptedJson, type) ?: emptyList()
            } else {
                emptyList()
            }

            // 3. Generate new salt and derive new key, upgrading to the current work factor
            val newSalt = CryptoManager.generateSalt()
            val newKey = CryptoManager.deriveKey(newPassword.toCharArray(), newSalt, CryptoManager.ITERATIONS)

            // 4. Re-encrypt database with new key
            val json = gson.toJson(entries)
            val newEncrypted = CryptoManager.encrypt(json.toByteArray(Charsets.UTF_8), newKey)
            encryptedFile.writeBytes(newEncrypted)

            // 5. Save new salt, iteration count, and verifier (using the new key's encoded bytes as verifier)
            SecurePrefs.saveSalt(context, newSalt)
            SecurePrefs.saveIterations(context, CryptoManager.ITERATIONS)
            SecurePrefs.saveVerifier(context, newKey.encoded)

            // 6. Update repository's current key
            repository.setEncryptionKey(newKey)
            loadEntries()

            onResult(true)
        }
    }
}
