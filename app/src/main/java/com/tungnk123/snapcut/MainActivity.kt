package com.tungnk123.snapcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tungnk123.snapcut.navigation.SnapCutNavGraph
import com.tungnk123.snapcut.ui.theme.SnapCutTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapCutTheme {
                SnapCutNavGraph()
            }
        }
    }
}
