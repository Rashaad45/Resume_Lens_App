package com.resumelens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.resumelens.ui.navigation.AppNavHost
import com.resumelens.ui.theme.ResumeLensTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity: every screen is a Compose destination inside AppNavHost. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ResumeLensTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
}
