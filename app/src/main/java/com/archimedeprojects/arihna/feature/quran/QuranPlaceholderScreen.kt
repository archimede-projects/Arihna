package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop

@Composable
fun QuranPlaceholderScreen(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Corano — placeholder", style = MaterialTheme.typography.titleLarge)
    }
}
