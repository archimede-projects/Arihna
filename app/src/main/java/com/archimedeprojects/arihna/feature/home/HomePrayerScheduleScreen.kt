package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val HomeBackground = Color(0xFF050B09)
private val HomeBackgroundGlow = Color(0xFF0A1A14)
private val HomeSurface = Color(0xFF0A1511)
private val HomeSurfaceRaised = Color(0xFF10231B)
private val HomeHeroTop = Color(0xFF102D21)
private val HomeHeroBottom = Color(0xFF091812)
private val HomeText = Color(0xFFFFFBF1)
private val HomeMuted = Color(0xFF9FAEA5)
private val HomeAccent = Color(0xFFD9B95B)
private val HomeOutline = Color(0xFF294138)

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenQibla: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    HomePrayerScheduleScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenLocationSettings = onOpenLocationSettings,
        onRefreshLocation = onRefreshLocation,
        onOpenQibla = onOpenQibla,
        onOpenAlarms = onOpenAlarms,
    )
}

@Composable
fun HomePrayerScheduleScreen(
    contentPadding: PaddingValues,
    uiState: PrayerScheduleUiState,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit = {},
    onOpenQibla: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(HomeBackgroundGlow, HomeBackground, HomeBackground),
                ),
            )
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("home-prayer-root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState) {
            PrayerScheduleUiState.Loading -> LoadingContent()
            is PrayerScheduleUiState.NoLocation -> NoLocationContent(uiState, onOpenLocationSettings)
            is PrayerScheduleUiState.CalculationUnavailable -> CalculationUnavailableContent(uiState)
            is PrayerScheduleUiState.Ready -> ReadyContent(
                state = uiState,
                onOpenLocationSettings = onOpenLocationSettings,
                onRefreshLocation = onRefreshLocation,
                onOpenQibla = onOpenQibla,
                onOpenAlarms = onOpenAlarms,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    BrandHeader(onOpenSettings = {})
    StateCard {
        CircularProgressIndicator(color = HomeAccent)
        Text("Calcolo degli orari in corso…", color = HomeText)
    }
}

@Composable
private fun NoLocationContent(
    state: PrayerScheduleUiState.NoLocation,
    onOpenLocationSettings: () -> Unit,
) {
    BrandHeader(onOpenSettings = onOpenLocationSettings)
    StateCard {
        Text(
            text = "La tua giornata, al ritmo della preghiera",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HomeText,
        )
        Text(state.message, color = HomeMuted)
        Button(
            onClick = onOpenLocationSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeAccent,
                contentColor = HomeBackground,
            ),
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Configura posizione", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
    BrandHeader(onOpenSettings = {})
    StateCard {
        Text(
            "Orari non disponibili",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = HomeText,
        )
        Text(state.message, color = HomeMuted)
        Text(
            "Nessun orario viene mostrato finché il calcolo non torna disponibile.",
            style = MaterialTheme.typography.bodySmall,
            color = HomeMuted,
        )
    }
}

@Composable
private fun ReadyContent(
    state: PrayerScheduleUiState.Ready,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    val zoneId = state.today.zoneId
    BrandHeader(onOpenSettings = onOpenLocationSettings)
    LocationContext(state, onRefreshLocation)
    NextPrayerHero(state, zoneId)
    TodayPrayerStrip(state, zoneId)
    WeekStrip(state.localDate)
    InspirationCard()
    QuickActions(
        onOpenQibla = onOpenQibla,
        onOpenAlarms = onOpenAlarms,
        onOpenLocation = onOpenLocationSettings,
    )
    Text(
        text = methodLabel(state.settings.method),
        style = MaterialTheme.typography.labelSmall,
        color = HomeMuted.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BrandHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Arihna",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = HomeText,
            )
            Text(
                text = "La tua guida quotidiana",
                style = MaterialTheme.typography.labelMedium,
                color = HomeAccent,
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("home-open-settings"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Impostazioni",
                tint = HomeAccent,
            )
        }
    }
}

@Composable
private fun LocationContext(
    state: PrayerScheduleUiState.Ready,
    onRefreshLocation: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeSurface, RoundedCornerShape(18.dp))
            .border(1.dp, HomeOutline, RoundedCornerShape(18.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = HomeAccent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = HomeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("home-location"),
            )
            Text(
                text = formatDate(state.localDate),
                style = MaterialTheme.typography.bodySmall,
                color = HomeMuted,
                modifier = Modifier.testTag("home-current-date"),
            )
        }
        IconButton(
            onClick = onRefreshLocation,
            modifier = Modifier
                .size(40.dp)
                .testTag("home-refresh-location"),
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Aggiorna posizione",
                tint = HomeAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NextPrayerHero(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(HomeHeroTop, HomeHeroBottom)),
                RoundedCornerShape(28.dp),
            )
            .border(1.dp, HomeAccent.copy(alpha = 0.60f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("home-next-prayer-hero"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Prossima preghiera",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HomeAccent,
            )
            val nextPrayer = state.nextPrayer
            if (nextPrayer == null) {
                Text("Nessuna prossima preghiera disponibile.", color = HomeText)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = prayerLabel(nextPrayer.prayer),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = HomeText,
                            modifier = Modifier.testTag("home-next-prayer-name"),
                        )
                        Text(
                            text = "Tra ${formatCountdown(nextPrayer.remaining)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeAccent,
                            modifier = Modifier.testTag("home-next-prayer-countdown"),
                        )
                    }
                    Text(
                        text = formatTime(nextPrayer.time, zoneId),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = HomeText,
                        modifier = Modifier.testTag("home-next-prayer-time"),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(HomeOutline, RoundedCornerShape(50)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(4.dp)
                            .background(HomeAccent, RoundedCornerShape(50)),
                    )
                }
            }
        }
    }
}

private data class PrayerSlot(
    val label: String,
    val time: Instant,
    val icon: ImageVector,
    val prayer: PrayerName? = null,
)

@Composable
private fun TodayPrayerStrip(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    val slots = listOf(
        PrayerSlot("Fajr", state.today.times.fajr, Icons.Rounded.Brightness4, PrayerName.FAJR),
        PrayerSlot("Alba", state.today.times.sunrise, Icons.Rounded.WbSunny),
        PrayerSlot("Dhuhr", state.today.times.dhuhr, Icons.Rounded.WbSunny, PrayerName.DHUHR),
        PrayerSlot("Asr", state.today.times.asr, Icons.Rounded.Schedule, PrayerName.ASR),
        PrayerSlot("Maghrib", state.today.times.maghrib, Icons.Rounded.Brightness4, PrayerName.MAGHRIB),
        PrayerSlot("Isha", state.today.times.isha, Icons.Rounded.Brightness4, PrayerName.ISHA),
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "Orari di oggi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HomeText,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home-today-schedule"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            slots.forEach { slot ->
                val active = slot.prayer != null && slot.prayer == state.nextPrayer?.prayer
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) HomeAccent.copy(alpha = 0.17f) else HomeSurface,
                            RoundedCornerShape(16.dp),
                        )
                        .border(
                            1.dp,
                            if (active) HomeAccent.copy(alpha = 0.75f) else HomeOutline,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        slot.icon,
                        contentDescription = null,
                        tint = if (active) HomeAccent else HomeMuted,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        slot.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) HomeAccent else HomeMuted,
                        maxLines = 1,
                    )
                    Text(
                        formatTime(slot.time, zoneId),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HomeText,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(localDate: LocalDate) {
    val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..6L).map(monday::plusDays)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "Questa settimana",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HomeText,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home-week-strip"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            days.forEach { day ->
                val today = day == localDate
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (today) HomeAccent else HomeSurface,
                            RoundedCornerShape(14.dp),
                        )
                        .border(
                            1.dp,
                            if (today) HomeAccent else HomeOutline,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 7.dp)
                        .then(if (today) Modifier.testTag("home-week-today") else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        dayInitial(day.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (today) HomeBackground else HomeMuted,
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (today) HomeBackground else HomeText,
                    )
                }
            }
        }
    }
}

@Composable
private fun InspirationCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-inspiration"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurfaceRaised),
        border = BorderStroke(1.dp, HomeAccent.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .background(HomeAccent.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(9.dp),
            ) {
                Icon(
                    Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = HomeAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Ispirazione del giorno",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HomeAccent,
                )
                Text(
                    "“Con la difficoltà viene il sollievo.”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeText,
                )
                Text(
                    "Corano 94:5–6",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMuted,
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenLocation: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-quick-actions"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAction("Qibla", Icons.Rounded.Explore, onOpenQibla, Modifier.weight(1f))
        QuickAction("Sveglie", Icons.Rounded.Alarm, onOpenAlarms, Modifier.weight(1f))
        QuickAction("Posizione", Icons.Rounded.MyLocation, onOpenLocation, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(HomeSurface, RoundedCornerShape(18.dp))
            .border(1.dp, HomeOutline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = HomeAccent, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = HomeText,
        )
    }
}

@Composable
private fun StateCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurfaceRaised),
        border = BorderStroke(1.dp, HomeOutline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

private fun prayerLabel(prayer: PrayerName): String = when (prayer) {
    PrayerName.FAJR -> "Fajr"
    PrayerName.DHUHR -> "Dhuhr"
    PrayerName.ASR -> "Asr"
    PrayerName.MAGHRIB -> "Maghrib"
    PrayerName.ISHA -> "Isha"
}

private fun dayInitial(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "M"
    DayOfWeek.THURSDAY -> "G"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

private fun methodLabel(method: PrayerCalculationMethod): String = when (method) {
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League · MWL"
    PrayerCalculationMethod.UMM_AL_QURA -> "Umm al-Qura"
    PrayerCalculationMethod.ISNA -> "ISNA"
    PrayerCalculationMethod.EGYPTIAN -> "Egyptian General Authority"
    PrayerCalculationMethod.KARACHI -> "University of Islamic Sciences, Karachi"
    PrayerCalculationMethod.DUBAI -> "Dubai"
    PrayerCalculationMethod.KUWAIT -> "Kuwait"
    PrayerCalculationMethod.QATAR -> "Qatar"
    PrayerCalculationMethod.MOONSIGHTING_COMMITTEE -> "Moonsighting Committee"
    PrayerCalculationMethod.SINGAPORE -> "Singapore"
    PrayerCalculationMethod.TURKEY -> "Turkey"
}

private fun formatDate(date: LocalDate): String = DATE_FORMATTER.format(date)

private fun formatTime(time: Instant, zoneId: ZoneId): String =
    TIME_FORMATTER.withZone(zoneId).format(time)

private fun formatCountdown(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
