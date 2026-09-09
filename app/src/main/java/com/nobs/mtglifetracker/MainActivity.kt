package com.nobs.mtglifetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nobs.mtglifetracker.ui.GameScreen
import com.nobs.mtglifetracker.ui.GameViewModel
import com.nobs.mtglifetracker.ui.theme.MtgTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GameViewModel = viewModel()
            val ui by viewModel.state.collectAsState()

            MtgTrackerTheme(mode = ui.settings.theme) {
                val view = LocalView.current
                // A tracker that dims out halfway through a turn is useless on a table.
                DisposableEffect(ui.settings.keepScreenOn) {
                    view.keepScreenOn = ui.settings.keepScreenOn
                    onDispose { view.keepScreenOn = false }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GameScreen(viewModel)
                }
            }
        }
    }
}
