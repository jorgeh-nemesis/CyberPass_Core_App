package com.cybercastle.cyberpass

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId
import androidx.annotation.RequiresApi

/** The fields CyberPass could fill on the current screen, and how to match a saved entry to it. */
@RequiresApi(Build.VERSION_CODES.O)
data class AutofillTarget(
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val webDomain: String?,
    val appPackage: String?
)

@RequiresApi(Build.VERSION_CODES.O)
object AutofillFieldParser {

    fun parse(structure: AssistStructure): AutofillTarget {
        var usernameId: AutofillId? = null
        var passwordId: AutofillId? = null
        var webDomain: String? = null
        val appPackage = structure.activityComponent?.packageName

        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode
            walk(root) { node ->
                if (webDomain == null) {
                    node.webDomain?.let { webDomain = it }
                }
                val hints = node.autofillHints
                when {
                    hints != null && hints.any { it == View.AUTOFILL_HINT_PASSWORD } -> {
                        if (passwordId == null) passwordId = node.autofillId
                    }
                    hints != null && hints.any {
                        it == View.AUTOFILL_HINT_USERNAME || it == View.AUTOFILL_HINT_EMAIL_ADDRESS
                    } -> {
                        if (usernameId == null) usernameId = node.autofillId
                    }
                    passwordId == null && isPasswordInput(node) -> {
                        passwordId = node.autofillId
                    }
                    usernameId == null && isLikelyUsernameField(node) -> {
                        usernameId = node.autofillId
                    }
                }
            }
        }
        return AutofillTarget(usernameId, passwordId, webDomain, appPackage)
    }

    private fun walk(node: AssistStructure.ViewNode, action: (AssistStructure.ViewNode) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            walk(node.getChildAt(i), action)
        }
    }

    private fun isEditText(node: AssistStructure.ViewNode): Boolean =
        node.className?.contains("EditText") == true

    private fun isPasswordInput(node: AssistStructure.ViewNode): Boolean {
        if (!isEditText(node)) return false
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun isLikelyUsernameField(node: AssistStructure.ViewNode): Boolean {
        if (!isEditText(node)) return false
        val cls = node.inputType and InputType.TYPE_MASK_CLASS
        if (cls != InputType.TYPE_CLASS_TEXT) return false
        val idEntry = node.idEntry?.lowercase().orEmpty()
        val hint = node.hint?.lowercase().orEmpty()
        return idEntry.contains("user") || idEntry.contains("email") || idEntry.contains("login") ||
            hint.contains("user") || hint.contains("email")
    }
}
