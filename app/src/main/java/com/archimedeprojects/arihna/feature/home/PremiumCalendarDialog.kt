package com.archimedeprojects.arihna.feature.home

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.calendar.HijriDateFormatter
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaInk
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CalendarSystem { HIJRI, GREGORIAN }

private val hijriArabicMonths = listOf(
    "مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الآخِر",
    "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان",
    "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة",
)

private val hijriItalianMonths = listOf(
    "Muharram", "Safar", "Rabiʿ I", "Rabiʿ II", "Jumada I", "Jumada II",
    "Rajab", "Shaʿban", "Ramadan", "Shawwal", "Dhu al-Qiʿda", "Dhu al-Hijja",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PremiumCalendarDialog(
    initialDate: LocalDate,
    arabic: Boolean,
    onDismiss: () -> Unit,
) {
    var system by remember { mutableStateOf(CalendarSystem.HIJRI) }
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val gregorianState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    var hijriSelectedMillis by remember(initialDate) { mutableStateOf(initialMillis) }
    var hijriMonthMillis by remember(initialDate) { mutableStateOf(hijriMonthStart(initialMillis)) }

    val activeMillis = if (system == CalendarSystem.HIJRI) {
        hijriSelectedMillis
    } else {
        gregorianState.selectedDateMillis ?: initialMillis
    }
    val activeDate = remember(activeMillis) { millisToDate(activeMillis) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(appText("Chiudi", "إغلاق")) }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArihnaCream)
                .padding(top = 10.dp)
                .testTag("home-calendar-dialog"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = system == CalendarSystem.HIJRI,
                        onClick = { system = CalendarSystem.HIJRI },
                        label = { Text(appText("Hijri", "الهجري")) },
                        modifier = Modifier.weight(1f).testTag("home-calendar-hijri"),
                    )
                    FilterChip(
                        selected = system == CalendarSystem.GREGORIAN,
                        onClick = { system = CalendarSystem.GREGORIAN },
                        label = { Text(appText("Gregoriano", "الميلادي")) },
                        modifier = Modifier.weight(1f).testTag("home-calendar-gregorian"),
                    )
                }
                Text(
                    text = HijriDateFormatter.format(activeDate, arabic),
                    color = ArihnaGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
                Text(
                    text = activeDate.format(
                        DateTimeFormatter.ofPattern(if (arabic) "d MMMM yyyy" else "d MMMM yyyy", if (arabic) Locale("ar") else Locale.ITALIAN),
                    ),
                    color = ArihnaMutedText,
                    fontSize = 11.sp,
                )
            }

            if (system == CalendarSystem.HIJRI) {
                HijriMonthGrid(
                    monthMillis = hijriMonthMillis,
                    selectedMillis = hijriSelectedMillis,
                    arabic = arabic,
                    onPreviousMonth = { hijriMonthMillis = addHijriMonths(hijriMonthMillis, -1) },
                    onNextMonth = { hijriMonthMillis = addHijriMonths(hijriMonthMillis, 1) },
                    onSelect = { hijriSelectedMillis = it },
                )
            } else {
                DatePicker(
                    state = gregorianState,
                    showModeToggle = false,
                    modifier = Modifier.testTag("home-gregorian-date-picker"),
                )
            }
        }
    }
}

@Composable
private fun HijriMonthGrid(
    monthMillis: Long,
    selectedMillis: Long,
    arabic: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val month = remember(monthMillis) { monthSnapshot(monthMillis) }
    val selected = remember(selectedMillis) { islamicCalendar(selectedMillis) }
    val selectedYear = selected.get(Calendar.YEAR)
    val selectedMonth = selected.get(Calendar.MONTH)
    val selectedDay = selected.get(Calendar.DAY_OF_MONTH)
    val weekdayLabels = if (arabic) {
        listOf("ن", "ث", "ر", "خ", "ج", "س", "ح")
    } else {
        listOf("L", "M", "M", "G", "V", "S", "D")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = appText("Mese precedente", "الشهر السابق"), tint = ArihnaForest)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (arabic) hijriArabicMonths[month.month] else hijriItalianMonths[month.month],
                    color = ArihnaForest,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Text(
                    text = if (arabic) toArabicDigits(month.year) + " هـ" else "${month.year} AH",
                    color = ArihnaDawnGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = appText("Mese successivo", "الشهر التالي"), tint = ArihnaForest)
            }
        }

        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    color = ArihnaMutedText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }
        }

        val cells = remember(month) {
            List(month.leadingBlankDays) { null } + (1..month.daysInMonth).map { it }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(270.dp).testTag("home-hijri-month-grid"),
            userScrollEnabled = false,
        ) {
            items(cells) { day ->
                if (day == null) {
                    Box(Modifier.size(38.dp))
                } else {
                    val isSelected = month.year == selectedYear && month.month == selectedMonth && day == selectedDay
                    val dayMillis = remember(monthMillis, day) { hijriDayMillis(monthMillis, day) }
                    Box(
                        modifier = Modifier.padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onSelect(dayMillis) }
                                .testTag("home-hijri-day-$day"),
                            shape = CircleShape,
                            color = if (isSelected) ArihnaGreen else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.34f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (arabic) toArabicDigits(day) else day.toString(),
                                    color = if (isSelected) Color.White else ArihnaInk,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            color = ArihnaSage.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.6f)),
        ) {
            Text(
                text = appText(
                    "Calendario Hijri selezionabile: tocca un giorno o cambia mese.",
                    "التقويم الهجري قابل للاختيار: اختر يوماً أو انتقل إلى شهر آخر.",
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                color = ArihnaForest,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class HijriMonthSnapshot(
    val year: Int,
    val month: Int,
    val daysInMonth: Int,
    val leadingBlankDays: Int,
)

private fun islamicCalendar(millis: Long): IslamicCalendar =
    IslamicCalendar(TimeZone.getTimeZone("UTC"), Locale("ar")).apply {
        timeInMillis = millis
    }

private fun hijriMonthStart(millis: Long): Long = islamicCalendar(millis).apply {
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

private fun addHijriMonths(millis: Long, months: Int): Long = islamicCalendar(millis).apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, months)
}.timeInMillis

private fun hijriDayMillis(monthMillis: Long, day: Int): Long = islamicCalendar(monthMillis).apply {
    set(Calendar.DAY_OF_MONTH, day)
}.timeInMillis

private fun monthSnapshot(monthMillis: Long): HijriMonthSnapshot {
    val calendar = islamicCalendar(monthMillis).apply { set(Calendar.DAY_OF_MONTH, 1) }
    // Calendar.SUNDAY=1. Convert to Monday-first grid: Monday=>0 ... Sunday=>6.
    val leading = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return HijriMonthSnapshot(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH).coerceIn(0, 11),
        daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH),
        leadingBlankDays = leading,
    )
}

private fun millisToDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun toArabicDigits(value: Int): String = value.toString().map { char ->
    if (char in '0'..'9') ('٠'.code + (char - '0')).toChar() else char
}.joinToString("")
