package com.example.smartcoaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smartcoaster.ui.SmartCoasterApp
import com.example.smartcoaster.ui.theme.SmartCoasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartCoasterTheme { SmartCoasterApp() }
        }
    }
}
