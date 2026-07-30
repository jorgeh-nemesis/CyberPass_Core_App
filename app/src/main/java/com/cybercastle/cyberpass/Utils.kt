package com.cybercastle.cyberpass

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

fun findActivity(context: Context): FragmentActivity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}