package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val HomeBackgroundTop = Color(0xFF06110D)
private val HomeBackgroundBottom = Color(0xFF020805)
private val HomeSurface = Color(0xFF0D1B16)
private val HomeSurfaceRaised = Color(0xFF12251D)
private val HomeHeroTop = Color(0xFF173D2E)
private val HomeHeroBottom = Color(0xFF092018)
private val HomeText = Color(0xFFF8F4E9)
private val HomeMuted = Color(0xFFA8B7AF)
private val HomeAccent = Color(0xFFD7B95A)
private val HomeAccentSoft = Color(0xFF7E6C35)
private val HomeOutline = Color(0xFF274338)

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
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
            .background(Brush.verticalGradient(listOf(HomeBackgroundTop, HomeBackgroundBottom)))
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("home-prayer-root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState) {
            PrayerScheduleUiState.Loading -> {
                BrandHeader(onOpenLocationSettings)
                LoadingContent()
            }
            is PrayerScheduleUiState.NoLocation -> {
                BrandHeader(onOpenLocationSettings)
                NoLocationContent(uiState, onOpenLocationSettings)
            }
            is PrayerScheduleUiState.CalculationUnavailable -> {
                BrandHeader(onOpenLocationSettings)
                CalculationUnavailableContent(uiState)
            }
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
private fun BrandHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "ARIHNA",
                color = HomeText,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp,
            )
            Text(
                text = "الصلاة راحة",
                color = HomeAccent,
                fontSize = 12.sp,
            )
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("home-open-settings"),
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Impostazioni", tint = HomeAccent)
        }
    }
}

@Composable
private fun LoadingContent() {
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
    StateCard {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = HomeAccent)
        Text(state.message, style = MaterialTheme.typography.bodyLarge, color = HomeText)
        Button(
            onClick = onOpenLocationSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeAccent,
                contentColor = HomeBackgroundBottom,
            ),
        ) {
            Text("Configura posizione", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
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
private fun ReadyContent(
    state: PrayerScheduleUiState.Ready,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
) {
    val zoneId = state.today.zoneId

    HomeHeader(
        state = state,
        onOpenSettings = onOpenLocationSettings,
        onRefreshLocation = onRefreshLocation,
    )
    NextPrayerHero(state, zoneId)
    PrayerTimesStrip(state, zoneId)
    WeekStrip(state.localDate)
    InspirationCard()
    QuickActions(
        onOpenQibla = onOpenQibla,
        onOpenAlarms = onOpenAlarms,
        onOpenLocationSettings = onOpenLocationSettings,
    )
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun HomeHeader(
    state: PrayerScheduleUiState.Ready,
    onOpenSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    BrandHeader(onOpenSettings)
    Surface(
        color = HomeSurface.copy(alpha = 0.84f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HomeOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = HomeAccent.copy(alpha = 0.13f)) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = HomeAccent,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = state.location.displayName,
                    color = HomeText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("home-location"),
                )
                Text(
                    text = formatDate(state.localDate),
                    color = HomeMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("home-current-date"),
                )
            }
            if (state.location.source == PrayerScheduleLocationSourceUi.DEVICE) {
                IconButton(
                    onClick = onRefreshLocation,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("home-refresh-location"),
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Aggiorna posizione",
                        tint = HomeAccent,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerHero(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(HomeHeroTop, HomeHeroBottom)))
            .border(1.dp, HomeAccent.copy(alpha = 0.42f), shape)
            .testTag("home-next-prayer-hero"),
    ) {
        MosqueSilhouette(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .align(Alignment.BottomCenter),
        )

        val nextPrayer = state.nextPrayer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "PROSSIMA PREGHIERA",
                    color = HomeAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                if (nextPrayer == null) {
                    Text("Nessuna preghiera disponibile", color = HomeText)
                } else {
                    Text(
                        text = prayerLabel(nextPrayer.prayer),
                        color = HomeText,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("home-next-prayer-name"),
                    )
                    Surface(
                        color = HomeAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("home-next-prayer-countdown"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = HomeAccent,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "manca ${formatCountdownCompact(nextPrayer.remaining)}",
                                color = HomeAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            if (nextPrayer != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        prayerHeroIcon(nextPrayer.prayer),
                        contentDescription = null,
                        tint = HomeAccent,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = formatTime(nextPrayer.time, zoneId),
                        color = HomeText,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("home-next-prayer-time"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MosqueSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = HomeAccent.copy(alpha = 0.065f)
        val baseY = size.height * 0.78f
        drawRect(c, topLeft = Offset(0f, baseY), size = Size(size.width, size.height - baseY))

        val center = size.width * 0.55f
        drawRect(c, topLeft = Offset(center - 34f, baseY - 28f), size = Size(68f, 30f))
        drawCircle(c, radius = 31f, center = Offset(center, baseY - 28f))
        drawRect(c, topLeft = Offset(center - 4f, baseY - 70f), size = Size(8f, 20f))

        val left = size.width * 0.23f
        drawRect(c, topLeft = Offset(left - 8f, baseY - 58f), size = Size(16f, 60f))
        drawCircle(c, radius = 10f, center = Offset(left, baseY - 60f))

        val right = size.width * 0.83f
        drawRect(c, topLeft = Offset(right - 7f, baseY - 50f), size = Size(14f, 52f))
        drawCircle(c, radius = 9f, center = Offset(right, baseY - 52f))
    }
}

@Composable
private fun PrayerTimesStrip(state: PrayerScheduleUiState.Ready, zoneId: ZoneId) {
    val nextPrayer = state.nextPrayer?.prayer
    val items = listOf(
        PrayerStripItem("Fajr", state.today.times.fajr, Icons.Filled.Brightness2, nextPrayer == PrayerName.FAJR),
        PrayerStripItem("Alba", state.today.times.sunrise, Icons.Filled.WbSunny, false),
        PrayerStripItem("Dhuhr", state.today.times.dhuhr, Icons.Filled.WbSunny, nextPrayer == PrayerName.DHUHR),
        PrayerStripItem("Asr", state.today.times.asr, Icons.Filled.WbSunny, nextPrayer == PrayerName.ASR),
        PrayerStripItem("Maghrib", state.today.times.maghrib, Icons.Filled.Brightness2, nextPrayer == PrayerName.MAGHRIB),
        PrayerStripItem("Isha", state.today.times.isha, Icons.Filled.Brightness2, nextPrayer == PrayerName.ISHA),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-today-schedule"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurface),
        border = BorderStroke(1.dp, HomeOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (item.isNext) HomeAccent.copy(alpha = 0.17f) else Color.Transparent)
                        .border(
                            width = if (item.isNext) 1.dp else 0.dp,
                            color = if (item.isNext) HomeAccentSoft else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (item.isNext) HomeAccent else HomeMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        item.label,
                        color = if (item.isNext) HomeAccent else HomeMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        formatTime(item.time, zoneId),
                        color = HomeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private data class PrayerStripItem(
    val label: String,
    val time: Instant,
    val icon: ImageVector,
    val isNext: Boolean,
)

@Composable
private fun WeekStrip(localDate: LocalDate) {
    val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0L..6L).map(monday::plusDays)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "Questa settimana",
            color = HomeMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isToday) HomeAccent else HomeSurface)
                        .border(1.dp, if (isToday) HomeAccent else HomeOutline, RoundedCornerShape(16.dp))
                        .padding(vertical = 7.dp)
                        .then(if (isToday) Modifier.testTag("home-week-today") else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        dayInitial(day.dayOfWeek),
                        color = if (isToday) HomeBackgroundBottom else HomeMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        color = if (isToday) HomeBackgroundBottom else HomeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = HomeSurfaceRaised),
        border = BorderStroke(1.dp, HomeAccent.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = HomeAccent.copy(alpha = 0.13f)) {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = HomeAccent,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "ISPIRAZIONE DEL GIORNO",
                    color = HomeAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    "Con la difficoltà viene il sollievo.",
                    color = HomeText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Corano 94:5–6", color = HomeMuted, fontSize = 11.sp)
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
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "Accessi rapidi",
            color = HomeMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Explore,
                label = "Qibla",
                tag = "home-quick-qibla",
                onClick = onOpenQibla,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Alarm,
                label = "Sveglie",
                tag = "home-quick-alarms",
                onClick = onOpenAlarms,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.MyLocation,
                label = "Posizione",
                tag = "home-quick-location",
                onClick = onOpenLocationSettings,
            )
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = HomeSurface,
        border = BorderStroke(1.dp, HomeOutline),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = HomeAccent, modifier = Modifier.size(21.dp))
            Text(label, color = HomeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun prayerHeroIcon(prayer: PrayerName): ImageVector = when (prayer) {
    PrayerName.DHUHR, PrayerName.ASR -> Icons.Filled.WbSunny
    PrayerName.FAJR, PrayerName.MAGHRIB, PrayerName.ISHA -> Icons.Filled.Brightness2
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

private fun formatCountdownCompact(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%dh %02dm", hours, minutes)
    } else {
        String.format(Locale.ROOT, "%02dm %02ds", minutes, seconds)
    }
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
