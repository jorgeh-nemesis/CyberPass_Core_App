package com.example.cyberpass

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Apply saved language before setContent
        val savedLanguage = runBlocking {
            LanguageManager.getLanguageFlow(applicationContext).first()
        }
        LanguageManager.applyLanguage(savedLanguage)

        setContent {
            CyberPassTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: MainViewModel = viewModel()
                    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()

                    if (isLocked) {
                        LockScreen(
                            onUnlocked = { /* already handled in ViewModel */ },
                            viewModel = viewModel
                        )
                    } else {
                        PasswordApp(viewModel)
                    }
                }
            }
        }
    }
}
