package com.cybercastle.cyberpass

import javax.crypto.SecretKey

/**
 * Cross-component handle to the vault's in-memory encryption key while the
 * app process is alive and unlocked. CyberPassAutofillService runs in this
 * same process (no android:process override), so when the vault is already
 * unlocked it can fill/save credentials directly through this key instead of
 * prompting again. MainViewModel remains the sole owner of lock/unlock
 * transitions and keeps this in sync.
 */
object VaultSession {
    @Volatile
    private var key: SecretKey? = null

    fun setKey(newKey: SecretKey) {
        key = newKey
    }

    fun clear() {
        key = null
    }

    fun currentKey(): SecretKey? = key
}
