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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnMiddle
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSageStrong
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
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

private val HomeBackgroundTop = ArihnaDawnTop
private val HomeBackgroundMiddle = ArihnaDawnMiddle
private val HomeBackgroundBottom = ArihnaDawnBottom
private val HomeSurface = ArihnaCream
private val HomeSurfaceRaised = ArihnaCream
private val HomeHero = ArihnaGreen
private val HomeHeroDeep = ArihnaForest
private val HomeText = ArihnaForest
private val HomeMuted = ArihnaMutedText
private val HomeHeroText = ArihnaCream
private val HomeAccent = ArihnaDawnGold
private val HomeAccentSoft = ArihnaSageStrong
private val HomeOutline = ArihnaWarmOutline

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    HomePrayerScheduleScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenLocationSettings = onOpenLocationSettings,
        onOpenQibla = onOpenQibla,
        onOpenAlarms = onOpenAlarms,
        onRefreshLocation = onRefreshLocation,
    )
}

@Composable
fun HomePrayerScheduleScreen(
    contentPadding: PaddingValues,
    uiState: PrayerScheduleUiState,
    onOpenLocationSettings: () -> Unit,
    onOpenQibla: () -> Unit = {},
    onOpenAlarms: () -> Unit = {},
    onRefreshLocation: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HomeBackgroundTop, HomeBackgroundMiddle, HomeBackgroundBottom)))
            .islamicBackdrop()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("home-prayer-root"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        when (uiState) {
            PrayerScheduleUiState.Loading -> LoadingContent()
            is PrayerScheduleUiState.NoLocation -> NoLocationContent(uiState, onOpenLocationSettings)
            is PrayerScheduleUiState.CalculationUnavailable -> CalculationUnavailableContent(uiState)
            is PrayerScheduleUiState.Ready -> ReadyContent(
                state = uiState,
                onOpenLocationSettings = onOpenLocationSettings,
                onOpenQibla = onOpenQibla,
                onOpenAlarms = onOpenAlarms,
                onRefreshLocation = onRefreshLocation,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    BrandHeader()
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
    BrandHeader()
    StateCard {
        Text(state.message, style = MaterialTheme.typography.bodyLarge, color = HomeText)
        Button(
            onClick = onOpenLocationSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeAccent,
                contentColor = HomeHeroDeep,
            ),
        ) {
            Text("Configura posizione", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
    BrandHeader()
    StateCard {
        Text("Orari non disponibili", fontWeight = FontWeight.Bold, color = HomeText)
        Text(state.message, color = HomeMuted)
        Text(
            "Nessun orario viene mostrato finché il calcolo non torna disponibile.",
            style = MaterialTheme.typography.bodySmall,
            color = HomeMuted,
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun BrandHeader() {
    Text(
        text = "ARIHNA",
        color = HomeAccent,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        letterSpacing = 3.sp,
    )
}

@Composable
private fun ReadyContent(
    state: PrayerScheduleUiState.Ready,
    onOpenLocationSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val zoneId = state.today.zoneId

    HomeHeader(state, onRefreshLocation)
    NextPrayerHero(state, zoneId)
    TodayPrayerStrip(state, zoneId)
    WeekStrip(state.localDate)
    DailyInspirationCard(state.localDate)
    QuickActions(
        onOpenQibla = onOpenQibla,
        onOpenAlarms = onOpenAlarms,
        onOpenLocationSettings = onOpenLocationSettings,
    )
}

@Composable
private fun HomeHeader(state: PrayerScheduleUiState.Ready, onRefreshLocation: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ARIHNA",
                color = HomeAccent,
                fontWeight = FontWeight.Black,
                fontSize = 21.sp,
                letterSpacing = 3.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = HomeMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = state.location.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeText,
                    modifier = Modifier.testTag("home-location"),
                )
            }
            Text(
                text = formatDate(state.localDate),
                style = MaterialTheme.typography.bodySmall,
                color = HomeMuted,
                modifier = Modifier.testTag("home-current-date"),
            )
        }
        IconButton(
            onClick = onRefreshLocation,
            modifier = Modifier.testTag("home-refresh-location"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Aggiorna posizione",
                tint = HomeAccent,
            )
        }
    }
}

@Composable
private fun NextPrayerHero(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-next-prayer-hero"),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = HomeHero),
        border = BorderStroke(1.dp, HomeAccent.copy(alpha = 0.62f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(HomeHeroDeep, HomeHero)))
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "PROSSIMA PREGHIERA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    color = HomeAccent,
                )
                val nextPrayer = state.nextPrayer
                if (nextPrayer == null) {
                    Text("Nessuna prossima preghiera disponibile.", color = HomeHeroText)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = prayerLabel(nextPrayer.prayer),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = HomeHeroText,
                            modifier = Modifier.testTag("home-next-prayer-name"),
                        )
                        Text(
                            text = formatTime(nextPrayer.time, zoneId),
                            fontSize = 46.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = HomeHeroText,
                            modifier = Modifier.testTag("home-next-prayer-time"),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(HomeAccent.copy(alpha = 0.13f), RoundedCornerShape(50))
                            .padding(horizontal = 13.dp, vertical = 7.dp)
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
}

@Composable
private fun TodayPrayerStrip(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    val next = state.nextPrayer?.prayer
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "OGGI",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = HomeMuted,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home-today-schedule"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PrayerStripTile("Fajr", state.today.times.fajr, zoneId, next == PrayerName.FAJR, Modifier.weight(1f))
            PrayerStripTile("Alba", state.today.times.sunrise, zoneId, false, Modifier.weight(1f))
            PrayerStripTile("Dhuhr", state.today.times.dhuhr, zoneId, next == PrayerName.DHUHR, Modifier.weight(1f))
            PrayerStripTile("Asr", state.today.times.asr, zoneId, next == PrayerName.ASR, Modifier.weight(1f))
            PrayerStripTile("Maghrib", state.today.times.maghrib, zoneId, next == PrayerName.MAGHRIB, Modifier.weight(1f))
            PrayerStripTile("Isha", state.today.times.isha, zoneId, next == PrayerName.ISHA, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrayerStripTile(
    label: String,
    time: Instant,
    zoneId: ZoneId,
    highlighted: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .background(
                if (highlighted) HomeAccentSoft else HomeSurface,
                RoundedCornerShape(13.dp),
            )
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = if (highlighted) HomeHeroDeep else HomeMuted,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = formatTime(time, zoneId),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = HomeText,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeekStrip(localDate: LocalDate) {
    val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..6L).map(monday::plusDays)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "SETTIMANA",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
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
                            if (isToday) HomeAccentSoft else HomeSurface,
                            RoundedCornerShape(15.dp),
                        )
                        .padding(vertical = 7.dp)
                        .then(if (isToday) Modifier.testTag("home-week-today") else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = dayInitial(day.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) HomeHeroDeep else HomeMuted,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) HomeHeroDeep else HomeText,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-quick-actions"),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        QuickActionButton("Qibla", Icons.Rounded.Explore, onOpenQibla, Modifier.weight(1f))
        QuickActionButton("Sveglie", Icons.Rounded.Alarm, onOpenAlarms, Modifier.weight(1f))
        QuickActionButton("Posizione", Icons.Rounded.LocationOn, onOpenLocationSettings, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HomeOutline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = HomeSurfaceRaised, contentColor = HomeText),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = HomeAccent, modifier = Modifier.size(19.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
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
