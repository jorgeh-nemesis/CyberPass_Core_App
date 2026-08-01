package com.cybercastle.cyberpass

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * App-wide vault lock trigger. Locks the vault after [IDLE_TIMEOUT_MILLIS] of
 * user inactivity, or immediately whenever the whole app (not just one
 * activity) is sent to the background. MainActivity forwards touch/key
 * events via [notifyUserInteraction]; MainViewModel observes [lockRequested]
 * and clears the in-memory encryption key when it fires.
 */
object AppLockManager : DefaultLifecycleObserver {
    const val IDLE_TIMEOUT_MILLIS = 15 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob())
    private var idleJob: Job? = null

    private val _lockRequested = MutableStateFlow(false)
    val lockRequested: StateFlow<Boolean> = _lockRequested

    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        // Fires once the last visible activity of the whole process has
        // stopped, i.e. the app truly left the foreground (not a rotation).
        idleJob?.cancel()
        _lockRequested.value = true
    }

    override fun onStart(owner: LifecycleOwner) {
        resetIdleTimer()
    }

    fun notifyUserInteraction() {
        resetIdleTimer()
    }

    /** Called by the ViewModel once it has acted on a lock request. */
    fun acknowledgeLock() {
        _lockRequested.value = false
    }

    /** Called once the vault is unlocked again, to restart the idle countdown. */
    fun acknowledgeUnlock() {
        _lockRequested.value = false
        resetIdleTimer()
    }

    private fun resetIdleTimer() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MILLIS)
            _lockRequested.value = true
        }
    }
}
