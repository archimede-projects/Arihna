package com.archimedeprojects.arihna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.archimedeprojects.arihna.app.ArihnaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as ArihnaApplication).appContainer
        setContent {
            ArihnaApp(
                appContainer = appContainer,
                activity = this,
            )
        }
    }
}
