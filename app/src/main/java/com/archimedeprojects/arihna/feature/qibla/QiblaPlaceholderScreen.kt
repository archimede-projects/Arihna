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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.Flow

@Composable
fun QiblaRoute(contentPadding: PaddingValues, states: Flow<QiblaState>, onOpenLocationSettings: () -> Unit) {
    val state by states.collectAsState(initial = QiblaState.NoLocation(LocationResolutionState.Unconfigured))
    QiblaScreen(contentPadding, state, onOpenLocationSettings)
}

@Composable
fun QiblaScreen(contentPadding: PaddingValues, state: QiblaState, onOpenLocationSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Qibla", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Direzione verso la Kaaba · riferimento nord vero", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
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
    MessageCard("Posizione necessaria", "Imposta una posizione per calcolare la direzione della Qibla. Non viene usata alcuna posizione predefinita.")
    Spacer(Modifier.height(16.dp))
    Button(onClick = onOpenLocationSettings) { Text("Configura posizione") }
}

@Composable
private fun BearingUnavailableContent(state: QiblaState.BearingUnavailable) {
    LocationSummary(state.location)
    Spacer(Modifier.height(16.dp))
    val body = when (state.reason) {
        QiblaBearingUnavailableReason.INVALID_COORDINATES -> "Direzione Qibla non disponibile per la posizione selezionata."
        QiblaBearingUnavailableReason.AT_KAABA_OR_COINCIDENT -> "La posizione selezionata coincide con la Kaaba: il bearing è matematicamente indefinito."
    }
    MessageCard("Direzione non disponibile", body)
}

@Composable
private fun StaticBearingContent(state: QiblaState.StaticBearing) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(state.bearingTrueDegrees, live = false, animationKey = state.location)
    Spacer(Modifier.height(14.dp))
    MessageCard("Bussola statica", "La direzione è calcolata per la città selezionata. Per usare la bussola live e allineare il telefono, scegli la posizione del dispositivo.")
}

@Composable
private fun LiveStartingContent(state: QiblaState.LiveCompassStarting) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(state.bearingTrueDegrees, live = false, animationKey = state.location)
    Spacer(Modifier.height(14.dp))
    MessageCard("Avvio bussola…", "Il bearing è già disponibile. Attendo una lettura valida dei sensori per l’allineamento live.")
}

@Composable
private fun LiveCompassContent(state: QiblaState.LiveCompass) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${displayDegrees(state.deviceHeadingTrueDegrees)}° ${headingCardinalLabel(state.deviceHeadingTrueDegrees)}",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag("qibla-live-heading"),
    )
    Spacer(Modifier.height(8.dp))
    CompassDial(
        directionDegrees = state.relativeQiblaDirectionDegrees,
        deviceHeadingTrueDegrees = state.deviceHeadingTrueDegrees,
        live = true,
        animationKey = state.location,
    )
    Spacer(Modifier.height(14.dp))
    Text("Bussola live", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    InstrumentationGrid(state)
    Spacer(Modifier.height(10.dp))
    val fieldStatus = classifyMagneticField(state.magneticFieldMicroTesla)
    if (fieldStatus == MagneticFieldStatus.INTERFERENCE) {
        MessageCard("Possibile interferenza magnetica", "Il campo magnetico rilevato è fuori dall’intervallo ambientale atteso. Allontana caricatore, metallo, magneti e accessori magnetici, poi ricontrolla la direzione.")
        Spacer(Modifier.height(10.dp))
    }
    MessageCard("Per una maggiore precisione", "Tieni il telefono circa orizzontale e lontano da caricabatterie, metallo, magneti e custodie/accessori magnetici. L’intensità del campo è solo diagnostica e non garantisce da sola la precisione della direzione.")
}

@Composable
private fun InstrumentationGrid(state: QiblaState.LiveCompass) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InstrumentCard(
            title = "Intensità campo magnetico",
            value = state.magneticFieldMicroTesla?.takeIf { it.isFinite() }?.let { "${it.roundToInt()} µT" } ?: "Non disponibile",
            detail = magneticFieldStatusLabel(classifyMagneticField(state.magneticFieldMicroTesla)),
            modifier = Modifier.weight(1f).testTag("qibla-magnetic-field"),
        )
        InstrumentCard(
            title = "Accuratezza bussola",
            value = qualityShortLabel(state.quality),
            detail = state.estimatedAccuracyDegrees?.takeIf { it.isFinite() && it >= 0.0 }?.let { "±${it.roundToInt()}° stimati" } ?: "Stima non disponibile",
            modifier = Modifier.weight(1f).testTag("qibla-heading-quality"),
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InstrumentCard("Sensore utilizzato", headingSourceLabel(state.headingSource), "Nord vero", Modifier.weight(1f).testTag("qibla-heading-source"))
        InstrumentCard(
            "Declinazione magnetica",
            state.declinationDegrees?.takeIf { it.isFinite() }?.let(::formatDeclination) ?: "Non applicabile",
            if (state.declinationDegrees == null) "Sensore nord vero diretto" else "Correzione geomagnetica",
            Modifier.weight(1f).testTag("qibla-declination"),
        )
    }
}

@Composable
private fun InstrumentCard(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SensorUnavailableContent(state: QiblaState.SensorUnavailable) {
    BearingHeader(state.location, state.bearingTrueDegrees)
    Spacer(Modifier.height(18.dp))
    CompassDial(state.bearingTrueDegrees, live = false, animationKey = state.location)
    Spacer(Modifier.height(14.dp))
    MessageCard("Bussola live non disponibile", headingUnavailableText(state.reason))
    Text("Il bearing numerico resta valido rispetto al nord vero.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun BearingHeader(location: SelectedLocation, bearingTrueDegrees: Double) {
    LocationSummary(location)
    Spacer(Modifier.height(8.dp))
    Text("Qibla ${displayDegrees(bearingTrueDegrees)}°", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = ArihnaGreen)
}

@Composable
private fun LocationSummary(location: SelectedLocation) {
    Text(location.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Text(if (location.source is LocationSource.Device) "Posizione dispositivo" else "Posizione manuale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (location.freshness == LocationFreshness.CACHED) Text("Posizione memorizzata (CACHED)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CompassDial(directionDegrees: Double, deviceHeadingTrueDegrees: Double = 0.0, live: Boolean, animationKey: Any) {
    val wrappedDirection = directionDegrees.toFloat()
    var directionTarget by remember(animationKey) { mutableFloatStateOf(wrappedDirection) }
    LaunchedEffect(animationKey, wrappedDirection, live) {
        directionTarget = if (live) shortestUnwrappedCompassTarget(directionTarget, wrappedDirection) else wrappedDirection
    }
    val animatedDirection by animateFloatAsState(directionTarget, label = "qiblaDirection")
    val displayedDirection = if (live) animatedDirection else wrappedDirection

    val wrappedRose = if (live) cardinalRoseWrappedTarget(deviceHeadingTrueDegrees.toFloat()) else 0f
    var roseTarget by remember(animationKey) { mutableFloatStateOf(wrappedRose) }
    LaunchedEffect(animationKey, wrappedRose, live) {
        roseTarget = if (live) shortestUnwrappedCompassTarget(roseTarget, wrappedRose) else 0f
    }
    val animatedRose by animateFloatAsState(roseTarget, label = "cardinalRoseDirection")
    val displayedRose = if (live) animatedRose else 0f
    val radiusPx = with(LocalDensity.current) { 123.dp.toPx() }
    val dialTickColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(Modifier.size(310.dp).testTag(if (live) "qibla-full-compass" else "qibla-static-compass"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(ArihnaGreen.copy(alpha = 0.08f))
            drawCircle(ArihnaGreen, style = Stroke(width = 3.dp.toPx()))
        }
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = displayedRose }.testTag(if (live) "qibla-live-cardinal-rose" else "qibla-static-cardinal-rose")) {
            Canvas(Modifier.fillMaxSize()) {
                val center = this.center
                val outer = size.minDimension * 0.46f
                for (degree in 0 until 360 step 5) {
                    val major = degree % 30 == 0
                    val medium = degree % 10 == 0
                    val length = when { major -> 16.dp.toPx(); medium -> 11.dp.toPx(); else -> 7.dp.toPx() }
                    val rad = degree * PI / 180.0
                    val ux = sin(rad).toFloat()
                    val uy = -cos(rad).toFloat()
                    drawLine(
                        color = dialTickColor,
                        start = Offset(center.x + ux * (outer - length), center.y + uy * (outer - length)),
                        end = Offset(center.x + ux * outer, center.y + uy * outer),
                        strokeWidth = if (major) 2.5.dp.toPx() else 1.4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            for (degree in 0 until 360 step 30) {
                if (degree == 0 || degree == 90 || degree == 180 || degree == 270) continue
                PolarLabel(degree.toString(), degree.toDouble(), radiusPx, displayedRose, fontSize = 11f)
            }
            PolarLabel("N", 0.0, radiusPx * 0.73f, displayedRose, true)
            PolarLabel("E", 90.0, radiusPx * 0.73f, displayedRose, true)
            PolarLabel("S", 180.0, radiusPx * 0.73f, displayedRose, true)
            PolarLabel("O", 270.0, radiusPx * 0.73f, displayedRose, true)
        }
        Text("▼", modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.size(214.dp).graphicsLayer { rotationZ = displayedDirection }.testTag(if (live) "qibla-live-direction" else "qibla-static-direction")) {
            Text("▲", modifier = Modifier.align(Alignment.TopCenter), color = ArihnaGold, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.size(14.dp).background(ArihnaGreen, CircleShape))
        Text("Q", color = MaterialTheme.colorScheme.surface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScopePolarText() = Unit

@Composable
private fun androidx.compose.foundation.layout.BoxScope.PolarLabel(
    text: String,
    degree: Double,
    radiusPx: Float,
    roseRotation: Float,
    bold: Boolean = false,
    fontSize: Float = 15f,
) {
    val rad = degree * PI / 180.0
    Text(
        text = text,
        modifier = Modifier.align(Alignment.Center).graphicsLayer {
            translationX = (sin(rad) * radiusPx).toFloat()
            translationY = (-cos(rad) * radiusPx).toFloat()
            rotationZ = -roseRotation
        },
        fontSize = fontSize.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        color = if (text == "N") ArihnaGreen else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MessageCard(title: String, body: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal enum class MagneticFieldStatus { NORMAL, INTERFERENCE, UNAVAILABLE }

internal fun classifyMagneticField(value: Double?): MagneticFieldStatus = when {
    value == null || !value.isFinite() || value < 0.0 -> MagneticFieldStatus.UNAVAILABLE
    value in 20.0..70.0 -> MagneticFieldStatus.NORMAL
    else -> MagneticFieldStatus.INTERFERENCE
}

internal fun headingCardinalLabel(value: Double): String {
    val d = ((value % 360.0) + 360.0) % 360.0
    return when (((d + 22.5) / 45.0).toInt() % 8) {
        0 -> "N"; 1 -> "NE"; 2 -> "E"; 3 -> "SE"; 4 -> "S"; 5 -> "SO"; 6 -> "O"; else -> "NO"
    }
}

private fun magneticFieldStatusLabel(status: MagneticFieldStatus): String = when (status) {
    MagneticFieldStatus.NORMAL -> "Normale"
    MagneticFieldStatus.INTERFERENCE -> "Possibile interferenza"
    MagneticFieldStatus.UNAVAILABLE -> "Sensore non disponibile"
}

private fun qualityShortLabel(quality: HeadingQuality): String = when (quality) {
    HeadingQuality.HIGH -> "Alta"
    HeadingQuality.MEDIUM -> "Media"
    HeadingQuality.LOW -> "Bassa"
    HeadingQuality.UNRELIABLE -> "Non affidabile"
    HeadingQuality.UNKNOWN -> "Non disponibile"
}

private fun formatDeclination(value: Double): String = "${kotlin.math.abs(value * 10.0).roundToInt() / 10.0}° ${if (value >= 0) "E" else "O"}"

private fun displayDegrees(value: Double): Int {
    val rounded = value.roundToInt() % 360
    return if (rounded < 0) rounded + 360 else rounded
}

private fun headingSourceLabel(source: HeadingSource): String = when (source) {
    HeadingSource.TRUE_HEADING_SENSOR -> "True Heading"
    HeadingSource.ROTATION_VECTOR -> "Rotation Vector"
    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD -> "Accelerometro + magnetometro"
}

private fun headingUnavailableText(reason: HeadingUnavailableReason): String = when (reason) {
    HeadingUnavailableReason.NO_SUPPORTED_SENSOR -> "Nessun sensore di orientamento supportato è disponibile su questo dispositivo."
    HeadingUnavailableReason.REGISTRATION_FAILED -> "I sensori di orientamento non hanno potuto avviarsi."
    HeadingUnavailableReason.INVALID_COORDINATES -> "Le coordinate disponibili non consentono la correzione della bussola."
    HeadingUnavailableReason.INVALID_SENSOR_READING -> "Il sensore ha prodotto una lettura non valida."
    HeadingUnavailableReason.INVALID_DECLINATION -> "La correzione tra nord magnetico e nord vero non è disponibile."
}
