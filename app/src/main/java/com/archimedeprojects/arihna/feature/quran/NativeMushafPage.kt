package com.archimedeprojects.arihna.feature.quran

import android.content.Context
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.util.LruCache
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

private object MushafPictureCache {
    private val pictures = LruCache<String, Picture>(12)

    fun load(context: Context, riwaya: QuranRiwaya, pageName: String): Picture? {
        val cacheKey = "${riwaya.name}:$pageName"
        pictures.get(cacheKey)?.let { return it }
        val folder = if (riwaya == QuranRiwaya.WARSH) "mushaf-warsh" else "mushaf"
        val picture = runCatching {
            context.assets.open("$folder/$pageName.svg").use { input ->
                SVG.getFromInputStream(input).renderToPicture()
            }
        }.getOrNull() ?: return null
        pictures.put(cacheKey, picture)
        return picture
    }
}

/** Renders one pinned Muṣḥaf page for the explicitly selected riwāya. */
@Composable
internal fun NativeMushafPage(
    page: Int,
    riwaya: QuranRiwaya,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageName = String.format(Locale.US, "%03d", page.coerceIn(1, 604))
    val picture by produceState<Picture?>(initialValue = null, pageName, riwaya) {
        value = withContext(Dispatchers.IO) {
            MushafPictureCache.load(context.applicationContext, riwaya, pageName)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val ready = picture
        if (ready == null) {
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
                update = { image -> image.setImageDrawable(PictureDrawable(ready)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun MushafPageLoadError(page: Int) {
    Text(text = "Pagina Muṣḥaf $page non disponibile", color = Color.Gray, fontSize = 12.sp)
}
