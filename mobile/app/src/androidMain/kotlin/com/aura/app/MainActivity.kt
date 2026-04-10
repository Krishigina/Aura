package com.aura.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aura.core.data.api.AuraApiClient
import com.aura.core.navigation.AuraNavigation
import com.aura.core.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    
    private val apiClient by lazy {
        AuraApiClient("http://10.0.2.2:3002")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraNavigation(apiClient = apiClient)
                }
            }
        }
    }
}
