package com.jaehoon.minigcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jaehoon.minigcs.ui.theme.MiniGCSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniGCSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaceholderScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Day 3 이후 DroneState를 그리는 화면으로 교체된다.
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "MiniGCS — 기체 미연결")
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    MiniGCSTheme {
        PlaceholderScreen()
    }
}
