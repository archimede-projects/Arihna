package com.archimedeprojects.arihna.feature.qibla

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.HeadingUnavailableReason
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.feature.qibla.domain.QiblaBearingUnavailableReason
import com.archimedeprojects.arihna.feature.qibla.domain.QiblaState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow

@Composable
fun QiblaRoute(
    contentPadding: PaddingValues,
    states: Flow<QiblaState>,
    onOpenLocationSettings: () -> Unit,
) {
    val state by states.collectAsState(
        initial = QiblaState.NoLocation(LocationResolutionState.Unconfigured),
    )
    QiblaScreen(
        contentPadding = contentPadding,
        state = state,
        onOpenLocationSettings = onOpenLocationSettings,
    )
}

@Composable
fun QiblaScreen(
    contentPadding: PaddingValues,
    state: QiblaState,
    onOpenLocationSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Qibla",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Direzione verso la Kaaba · riferimento nord vero",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        when (state) {
            is QiblaState.NoLocation -> NoLocationContent(onOpenLocationSettings)
            is QiblaState.BearingUnavailable -> BearingUnavailableContent(state)
            is QiblaState.StaticBearing -> StaticBearingContent(state)
            is QiblaState.LiveCompassStarting -> LiveStartingContent(state)
            is QiblaState.LiveCompass -> LiveCompassContent(state)
            is QiblaState.SensorUnavailable -> SensorUnavailableContent(state)
        }
    }
}

@Composable
private fun NoLocationContent(onOpenLocationSettings: () -> Unit) {
    MessageCard(
        title = "Posizione necessaria",
        body = "Imposta una posizione per calcolare la direzione della Qibla. Non viene usata alcuna posizione predefinita.",
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onOpenLocationSettings) {
        Text("Configura posizione")
    }
}

@Composable
private fun BearingUnavailableContent(state: QiblaState.BearingUnavailable) {
    LocationSummary(state.location)
    Spacer(Modifier.height(16.dp))
    val body = when (state.reason) {
        QiblaBearingUnavailableReason.INVALID_COORDINATES ->
            "Direzione Qibla non disponibile per la posizione selezionata."

        QiblaBearingUnavailableReason.AT_KAABA_OR_COINCIDENT ->
            "La posizione selezionata coincide con la Kaaba: il bearing è matematicamente indefinito."
    }
    MessageCard(title = "Direzione non disponibile", body = body)
}

@Composable
private fun StaticBearingContent(state: QiblaState.StaticBearing) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(
        directionDegrees = state.bearingTrueDegrees,
        live = false,
        animationKey = state.location,
    )
    Spacer(Modifier.height(14.dp))
    MessageCard(
        title = "Bussola statica",
        body = "La direzione è calcolata per la città selezionata. Per usare la bussola live e allineare il telefono, scegli la posizione del dispositivo.",
    )
}

@Composable
private fun LiveStartingContent(state: QiblaState.LiveCompassStarting) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(
        directionDegrees = state.bearingTrueDegrees,
        live = false,
        animationKey = state.location,
    )
    Spacer(Modifier.height(14.dp))
    MessageCard(
        title = "Avvio bussola…",
        body = "Il bearing è già disponibile. Attendo una lettura valida dei sensori per l’allineamento live.",
    )
}

@Composable
private fun LiveCompassContent(state: QiblaState.LiveCompass) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(
        directionDegrees = state.relativeQiblaDirectionDegrees,
        deviceHeadingTrueDegrees = state.deviceHeadingTrueDegrees,
        live = true,
        animationKey = state.location,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = "Bussola live",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = headingQualityText(state.quality),
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.quality == HeadingQuality.LOW || state.quality == HeadingQuality.UNRELIABLE) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    state.estimatedAccuracyDegrees
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.let {
            Text(
                text = "Accuratezza stimata: ±${it.roundToInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    Text(
        text = "Sensore: ${headingSourceLabel(state.headingSource)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SensorUnavailableContent(state: QiblaState.SensorUnavailable) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(
        directionDegrees = state.bearingTrueDegrees,
        live = false,
        animationKey = state.location,
    )
    Spacer(Modifier.height(14.dp))
    MessageCard(
        title = "Bussola live non disponibile",
        body = headingUnavailableText(state.reason),
    )
    Text(
        text = "Il bearing numerico resta valido rispetto al nord vero.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun BearingHeader(location: SelectedLocation, bearingTrueDegrees: Double) {
    LocationSummary(location)
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Qibla ${displayDegrees(bearingTrueDegrees)}°",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = ArihnaGreen,
    )
}

@Composable
private fun LocationSummary(location: SelectedLocation) {
    Text(
        text = location.displayName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when (location.source) {
                is LocationSource.Device -> "Posizione dispositivo"
                is LocationSource.Manual -> "Posizione manuale"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (location.freshness == LocationFreshness.CACHED) {
        Text(
            text = "Posizione memorizzata (CACHED)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompassDial(
    directionDegrees: Double,
    deviceHeadingTrueDegrees: Double = 0.0,
    live: Boolean,
    animationKey: Any,
) {
    val wrappedDirection = directionDegrees.toFloat()
    var unwrappedDirectionTarget by remember(animationKey) { mutableFloatStateOf(wrappedDirection) }
    LaunchedEffect(animationKey, wrappedDirection, live) {
        unwrappedDirectionTarget = if (live) {
            shortestUnwrappedCompassTarget(unwrappedDirectionTarget, wrappedDirection)
        } else {
            wrappedDirection
        }
    }
    val animatedDirection by animateFloatAsState(
        targetValue = unwrappedDirectionTarget,
        label = "qiblaDirection",
    )
    val displayedDirection = if (live) animatedDirection else wrappedDirection

    val wrappedCardinalRose = if (live) {
        cardinalRoseWrappedTarget(deviceHeadingTrueDegrees.toFloat())
    } else {
        0f
    }
    var unwrappedCardinalRoseTarget by remember(animationKey) {
        mutableFloatStateOf(wrappedCardinalRose)
    }
    LaunchedEffect(animationKey, wrappedCardinalRose, live) {
        unwrappedCardinalRoseTarget = if (live) {
            shortestUnwrappedCompassTarget(unwrappedCardinalRoseTarget, wrappedCardinalRose)
        } else {
            0f
        }
    }
    val animatedCardinalRose by animateFloatAsState(
        targetValue = unwrappedCardinalRoseTarget,
        label = "cardinalRoseDirection",
    )
    val displayedCardinalRose = if (live) animatedCardinalRose else 0f

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = ArihnaGreen.copy(alpha = 0.07f))
            drawCircle(
                color = ArihnaGreen,
                style = Stroke(width = 3.dp.toPx()),
            )
            drawCircle(
                color = ArihnaGold.copy(alpha = 0.55f),
                radius = size.minDimension * 0.38f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = displayedCardinalRose }
                .testTag(if (live) "qibla-live-cardinal-rose" else "qibla-static-cardinal-rose"),
        ) {
            Text(
                text = "N",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .graphicsLayer { rotationZ = -displayedCardinalRose },
                color = ArihnaGreen,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "E",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .graphicsLayer { rotationZ = -displayedCardinalRose },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "S",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .graphicsLayer { rotationZ = -displayedCardinalRose },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "O",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
                    .graphicsLayer { rotationZ = -displayedCardinalRose },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .size(214.dp)
                .graphicsLayer { rotationZ = displayedDirection }
                .testTag(if (live) "qibla-live-direction" else "qibla-static-direction"),
        ) {
            Text(
                text = "▲",
                modifier = Modifier.align(Alignment.TopCenter),
                color = ArihnaGold,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(ArihnaGreen, CircleShape),
        )
        Text(
            text = "Q",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.surface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MessageCard(title: String, body: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun displayDegrees(value: Double): Int {
    val rounded = value.roundToInt() % 360
    return if (rounded < 0) rounded + 360 else rounded
}

private fun headingQualityText(quality: HeadingQuality): String = when (quality) {
    HeadingQuality.HIGH -> "Accuratezza bussola: alta"
    HeadingQuality.MEDIUM -> "Accuratezza bussola: media"
    HeadingQuality.LOW ->
        "Accuratezza bassa: allontanati da metallo o interferenze e muovi il telefono per calibrare."

    HeadingQuality.UNRELIABLE ->
        "Orientamento non affidabile: allontanati da interferenze e ricalibra il telefono."

    HeadingQuality.UNKNOWN -> "Accuratezza bussola: non disponibile"
}

private fun headingSourceLabel(source: HeadingSource): String = when (source) {
    HeadingSource.TRUE_HEADING_SENSOR -> "true heading"
    HeadingSource.ROTATION_VECTOR -> "rotation vector"
    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> "geomagnetic rotation vector"
    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD -> "accelerometro + magnetometro"
}

private fun headingUnavailableText(reason: HeadingUnavailableReason): String = when (reason) {
    HeadingUnavailableReason.NO_SUPPORTED_SENSOR ->
        "Nessun sensore di orientamento supportato è disponibile su questo dispositivo."

    HeadingUnavailableReason.REGISTRATION_FAILED ->
        "I sensori di orientamento non hanno potuto avviarsi."

    HeadingUnavailableReason.INVALID_COORDINATES ->
        "Le coordinate disponibili non consentono la correzione della bussola."

    HeadingUnavailableReason.INVALID_SENSOR_READING ->
        "Il sensore ha prodotto una lettura non valida."

    HeadingUnavailableReason.INVALID_DECLINATION ->
        "La correzione tra nord magnetico e nord vero non è disponibile."
}
