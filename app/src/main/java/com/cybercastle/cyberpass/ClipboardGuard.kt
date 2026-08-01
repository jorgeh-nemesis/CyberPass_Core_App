package com.cybercastle.cyberpass

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copies sensitive text (passwords, etc.) to the clipboard, marking it as
 * sensitive on API 33+ so the OS hides it in previews/suggestions, and
 * automatically wipes the clipboard after [clearAfterMillis] so a copied
 * credential doesn't linger indefinitely for other apps to read.
 */
object ClipboardGuard {
    private var pendingClearJob: Job? = null

    fun copySensitive(
        scope: CoroutineScope,
        clipboard: Clipboard,
        label: String,
        text: String,
        clearAfterMillis: Long = 45_000L
    ) {
        val clipData = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clipData.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        pendingClearJob?.cancel()
        pendingClearJob = scope.launch {
            clipboard.setClipEntry(ClipEntry(clipData))
            delay(clearAfterMillis)
            // Only wipe it if the clipboard still holds what we put there -
            // avoid clobbering something the user copied from elsewhere since.
            val current = clipboard.getClipEntry()?.clipData
            val currentText = if (current != null && current.itemCount > 0) {
                current.getItemAt(0).text?.toString()
            } else {
                null
            }
            if (currentText == text) {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", "")))
            }
        }
    }
}
