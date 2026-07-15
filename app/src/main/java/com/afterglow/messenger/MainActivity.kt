package com.afterglow.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.afterglow.messenger.navigation.AfterglowNavGraph
import com.afterglow.messenger.ui.theme.AfterglowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AfterglowTheme {
                AfterglowNavGraph()
            }
        }
    }
}
