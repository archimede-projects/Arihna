package com.archimedeprojects.arihna.app

import androidx.compose.runtime.Composable
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme

@Composable
fun ArihnaApp() {
    ArihnaTheme {
        ArihnaNavHost()
    }
}
