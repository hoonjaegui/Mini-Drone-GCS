package com.jaehoon.minigcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.jaehoon.minigcs.ui.telemetry.TelemetryRoute
import com.jaehoon.minigcs.ui.theme.MiniGCSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniGCSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TelemetryRoute(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
