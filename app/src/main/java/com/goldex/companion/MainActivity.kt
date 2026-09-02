package com.goldex.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.goldex.companion.ui.calculator.GoldCalculatorScreen
import com.goldex.companion.ui.theme.DarkBg
import com.goldex.companion.ui.theme.GoldExCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoldExCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    GoldCalculatorScreen()
                }
            }
        }
    }
}
