package com.archimedeprojects.arihna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.debug.DeviceLocationDebugScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataSource = (application as ArihnaApplication).appContainer.deviceLocationDataSource
        setContent {
            ArihnaTheme {
                DeviceLocationDebugScreen(dataSource = dataSource)
            }
        }
    }
}
