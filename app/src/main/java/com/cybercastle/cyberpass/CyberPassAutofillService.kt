package com.cybercastle.cyberpass

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fully offline autofill service: it never makes a network call, matching
 * saved entries purely against the requesting app's package name or the
 * browser's web domain. If the vault is already unlocked in this process
 * (see VaultSession), it fills directly; otherwise it offers a single
 * auth-gated dataset that routes through AutofillUnlockActivity.
 */
@RequiresApi(Build.VERSION_CODES.O)
class CyberPassAutofillService : AutofillService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val target = AutofillFieldParser.parse(structure)
        if (target.usernameId == null && target.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            callback.onSuccess(buildFillResponse(target))
        }
    }

    private suspend fun buildFillResponse(target: AutofillTarget): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        val key = VaultSession.currentKey()

        if (key != null) {
            val repo = PasswordRepository(applicationContext)
            repo.setEncryptionKey(key)
            val matches = matchEntries(repo.loadEntries(), target)
            if (matches.isEmpty()) return null
            matches.take(MAX_DATASETS).forEach { entry ->
                responseBuilder.addDataset(buildDataset(entry, target))
            }
        } else {
            responseBuilder.addDataset(buildAuthDataset(target))
        }

        if (target.passwordId != null) {
            val requiredIds = listOfNotNull(target.usernameId, target.passwordId).toTypedArray()
            responseBuilder.setSaveInfo(SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, requiredIds).build())
        }
        return responseBuilder.build()
    }

    private fun matchEntries(entries: List<PasswordEntry>, target: AutofillTarget): List<PasswordEntry> {
        val needle = (target.webDomain ?: appLabel(target.appPackage))?.lowercase()
        if (needle.isNullOrBlank()) return entries
        val filtered = entries.filter { entry ->
            val title = entry.title.lowercase()
            title.contains(needle) || needle.contains(title)
        }
        return filtered.ifEmpty { entries }
    }

    private fun appLabel(packageName: String?): String? {
        if (packageName == null) return null
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun buildDataset(entry: PasswordEntry, target: AutofillTarget): Dataset {
        val builder = Dataset.Builder()
        target.usernameId?.let { id ->
            builder.setValue(id, AutofillValue.forText(entry.username), presentation(entry.title, entry.username))
        }
        target.passwordId?.let { id ->
            builder.setValue(id, AutofillValue.forText(entry.password), presentation(entry.title, "••••••••"))
        }
        return builder.build()
    }

    private fun buildAuthDataset(target: AutofillTarget): Dataset {
        val intent = AutofillUnlockActivity.createIntent(applicationContext, target)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            System.identityHashCode(target),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val unlockPresentation = presentation(getString(R.string.autofill_unlock_title), getString(R.string.app_name))
        val builder = Dataset.Builder()
        target.usernameId?.let { builder.setValue(it, null, unlockPresentation) }
        target.passwordId?.let { builder.setValue(it, null, unlockPresentation) }
        builder.setAuthentication(pendingIntent.intentSender)
        return builder.build()
    }

    private fun presentation(title: String, subtitle: String): RemoteViews {
        val views = RemoteViews("android", android.R.layout.simple_list_item_2)
        views.setTextViewText(android.R.id.text1, title)
        views.setTextViewText(android.R.id.text2, subtitle)
        return views
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        val key = VaultSession.currentKey()
        if (structure == null || key == null) {
            callback.onFailure(getString(R.string.autofill_save_failed))
            return
        }
        val target = AutofillFieldParser.parse(structure)
        val username = target.usernameId?.let { findValue(structure, it) }
        val password = target.passwordId?.let { findValue(structure, it) }
        if (password.isNullOrBlank()) {
            callback.onFailure(getString(R.string.autofill_save_failed))
            return
        }

        serviceScope.launch {
            val repo = PasswordRepository(applicationContext)
            repo.setEncryptionKey(key)
            val entries = repo.loadEntries().toMutableList()
            entries.add(
                PasswordEntry(
                    title = target.webDomain ?: appLabel(target.appPackage) ?: getString(R.string.app_name),
                    username = username.orEmpty(),
                    password = password,
                    category = VaultCategories.LOGINS
                )
            )
            repo.saveEntries(entries)
            callback.onSuccess()
        }
    }

    private fun findValue(structure: AssistStructure, id: AutofillId): String? {
        for (i in 0 until structure.windowNodeCount) {
            findNode(structure.getWindowNodeAt(i).rootViewNode, id)?.let { node ->
                val value = node.autofillValue
                if (value != null && value.isText) return value.textValue.toString()
            }
        }
        return null
    }

    private fun findNode(node: AssistStructure.ViewNode, id: AutofillId): AssistStructure.ViewNode? {
        if (node.autofillId == id) return node
        for (i in 0 until node.childCount) {
            findNode(node.getChildAt(i), id)?.let { return it }
        }
        return null
    }

    companion object {
        private const val MAX_DATASETS = 5
    }
}
