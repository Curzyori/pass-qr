package com.passqr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.passqr.ui.screen.DashboardScreen
import com.passqr.ui.theme.PassQRTheme
import com.passqr.util.LocaleManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PassQRTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen()
                }
            }
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }
}
