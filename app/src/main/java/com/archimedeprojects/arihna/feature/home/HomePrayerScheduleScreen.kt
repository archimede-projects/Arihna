package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationSourceUi
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

private val HomeBackground = Color(0xFF06100D)
private val HomeSurface = Color(0xFF0D1A16)
private val HomeSurfaceRaised = Color(0xFF13241D)
private val HomeHero = Color(0xFF193229)
private val HomeText = Color(0xFFF8F5EC)
private val HomeMuted = Color(0xFF9DAEA5)
private val HomeAccent = Color(0xFFD6B957)
private val HomeOutline = Color(0xFF294038)

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    HomePrayerScheduleScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenLocationSettings = onOpenLocationSettings,
        onRefreshLocation = onRefreshLocation,
    )
}

@Composable
fun HomePrayerScheduleScreen(
    contentPadding: PaddingValues,
    uiState: PrayerScheduleUiState,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("home-prayer-root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState) {
            PrayerScheduleUiState.Loading -> LoadingContent()
            is PrayerScheduleUiState.NoLocation -> NoLocationContent(
                state = uiState,
                onOpenLocationSettings = onOpenLocationSettings,
            )
            is PrayerScheduleUiState.CalculationUnavailable -> CalculationUnavailableContent(uiState)
            is PrayerScheduleUiState.Ready -> ReadyContent(uiState, onRefreshLocation)
        }
    }
}

@Composable
private fun LoadingContent() {
    StateCard {
        CircularProgressIndicator(color = HomeAccent)
        Text(
            text = "Calcolo degli orari in corso…",
            style = MaterialTheme.typography.bodyLarge,
            color = HomeText,
        )
    }
}

@Composable
private fun NoLocationContent(
    state: PrayerScheduleUiState.NoLocation,
    onOpenLocationSettings: () -> Unit,
) {
    Text(
        text = "Home",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = HomeText,
    )
    StateCard {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            color = HomeText,
        )
        Button(
            onClick = onOpenLocationSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeAccent,
                contentColor = HomeBackground,
            ),
        ) {
            Text("Configura posizione", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
    Text(
        text = "Home",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = HomeText,
    )
    StateCard {
        Text(
            text = "Orari non disponibili",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HomeText,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            color = HomeMuted,
        )
        Text(
            text = "Nessun orario viene mostrato finché il calcolo non torna disponibile.",
            style = MaterialTheme.typography.bodyMedium,
            color = HomeMuted,
        )
    }
}

@Composable
private fun StateCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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

@Composable
private fun ReadyContent(
    state: PrayerScheduleUiState.Ready,
    onRefreshLocation: () -> Unit,
) {
    val zoneId = state.today.zoneId

    HomeHeader(state)
    NextPrayerHero(state, zoneId)
    WeekStrip(state.localDate)
    TodaySchedule(state, zoneId)

    Text(
        text = "Metodo: ${methodLabel(state.settings.method)}",
        style = MaterialTheme.typography.bodySmall,
        color = HomeMuted,
        modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
    )

    if (
        state.location.source == PrayerScheduleLocationSourceUi.DEVICE &&
        state.locationFreshness == LocationFreshness.CACHED
    ) {
        val age = state.locationAge ?: Duration.ZERO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home-cached-location"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = HomeSurface),
            border = BorderStroke(1.dp, HomeOutline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Basato su posizione di ${formatLocationAge(age)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMuted,
                )
                TextButton(
                    onClick = onRefreshLocation,
                    colors = ButtonDefaults.textButtonColors(contentColor = HomeAccent),
                ) {
                    Text("Aggiorna posizione", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: PrayerScheduleUiState.Ready) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = formatDate(state.localDate),
                style = MaterialTheme.typography.labelLarge,
                color = HomeAccent,
                modifier = Modifier.testTag("home-current-date"),
            )
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HomeText,
                modifier = Modifier.testTag("home-location"),
            )
        }
        Text(
            text = if (state.location.source == PrayerScheduleLocationSourceUi.DEVICE) {
                "Posizione dispositivo"
            } else {
                "Posizione manuale"
            },
            style = MaterialTheme.typography.bodySmall,
            color = HomeMuted,
        )
    }
}

@Composable
private fun NextPrayerHero(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-next-prayer-hero"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HomeHero),
        border = BorderStroke(1.dp, HomeAccent.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "Prossima preghiera",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HomeAccent,
            )
            val nextPrayer = state.nextPrayer
            if (nextPrayer == null) {
                Text(
                    text = "Nessuna prossima preghiera disponibile.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = HomeText,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = prayerLabel(nextPrayer.prayer),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = HomeText,
                        modifier = Modifier.testTag("home-next-prayer-name"),
                    )
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
                        .background(HomeAccent.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("home-next-prayer-countdown"),
                ) {
                    Text(
                        text = "Tra ${formatCountdown(nextPrayer.remaining)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeAccent,
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

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Questa settimana",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = HomeMuted,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home-week-strip"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            days.forEach { day ->
                val isToday = day == localDate
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isToday) HomeAccent else HomeSurface,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 7.dp)
                        .then(if (isToday) Modifier.testTag("home-week-today") else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = dayInitial(day.dayOfWeek),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) HomeBackground else HomeMuted,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) HomeBackground else HomeText,
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySchedule(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-today-schedule"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurfaceRaised),
        border = BorderStroke(1.dp, HomeOutline),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Oggi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HomeAccent,
            )
            PrayerPair(
                firstLabel = "Fajr",
                firstTime = state.today.times.fajr,
                secondLabel = "Alba",
                secondTime = state.today.times.sunrise,
                zoneId = zoneId,
            )
            PrayerPair(
                firstLabel = "Dhuhr",
                firstTime = state.today.times.dhuhr,
                secondLabel = "Asr",
                secondTime = state.today.times.asr,
                zoneId = zoneId,
            )
            PrayerPair(
                firstLabel = "Maghrib",
                firstTime = state.today.times.maghrib,
                secondLabel = "Isha",
                secondTime = state.today.times.isha,
                zoneId = zoneId,
            )
        }
    }
}

@Composable
private fun PrayerPair(
    firstLabel: String,
    firstTime: Instant,
    secondLabel: String,
    secondTime: Instant,
    zoneId: ZoneId,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrayerTile(firstLabel, firstTime, zoneId, Modifier.weight(1f))
        PrayerTile(secondLabel, secondTime, zoneId, Modifier.weight(1f))
    }
}

@Composable
private fun PrayerTile(
    label: String,
    time: Instant,
    zoneId: ZoneId,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(HomeSurface, RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = HomeMuted)
        Text(
            formatTime(time, zoneId),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = HomeText,
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
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League (MWL)"
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

private fun formatLocationAge(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    return when {
        totalSeconds < 60L -> if (totalSeconds == 1L) "1 secondo fa" else "$totalSeconds secondi fa"
        totalSeconds < 3_600L -> {
            val minutes = totalSeconds / 60L
            if (minutes == 1L) "1 minuto fa" else "$minutes minuti fa"
        }
        totalSeconds < 86_400L -> {
            val hours = totalSeconds / 3_600L
            if (hours == 1L) "1 ora fa" else "$hours ore fa"
        }
        else -> {
            val days = totalSeconds / 86_400L
            if (days == 1L) "1 giorno fa" else "$days giorni fa"
        }
    }
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
