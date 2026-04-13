package com.vorsaciew.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vorsaciew.app.core.navigation.VorsaCiewNavHost
import com.vorsaciew.app.core.theme.VorsaCiewTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VorsaCiewTheme {
                VorsaCiewNavHost()
            }
        }
    }
}
