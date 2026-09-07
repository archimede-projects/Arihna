package com.archimedeprojects.arihna.feature.quran

import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders one pinned Muṣḥaf page from assets/mushaf/NNN.svg.
 * The SVG is visual presentation only; textual Quran acceptance still comes from QuranCorpus.
 */
@Composable
internal fun NativeMushafPage(
    page: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageName = String.format(Locale.US, "%03d", page.coerceIn(1, 604))
    val drawable by produceState<PictureDrawable?>(initialValue = null, pageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("mushaf/$pageName.svg").use { input ->
                    PictureDrawable(SVG.getFromInputStream(input).renderToPicture())
                }
            }.getOrNull()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (drawable == null) {
            CircularProgressIndicator()
        } else {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { image -> image.setImageDrawable(drawable) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun MushafPageLoadError(page: Int) {
    Text(
        text = "Pagina Muṣḥaf $page non disponibile",
        color = Color.Gray,
        fontSize = 12.sp,
    )
}
