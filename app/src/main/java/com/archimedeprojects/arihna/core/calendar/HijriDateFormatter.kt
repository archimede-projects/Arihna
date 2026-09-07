package com.archimedeprojects.arihna.core.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

object HijriDateFormatter {
    private val italianMonths = listOf(
        "Muharram", "Safar", "Rabiʿ al-awwal", "Rabiʿ al-thani",
        "Jumada al-awwal", "Jumada al-thani", "Rajab", "Shaʿban",
        "Ramadan", "Shawwal", "Dhu al-Qiʿdah", "Dhu al-Hijjah",
    )
    private val arabicMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    fun format(date: LocalDate, arabic: Boolean): String {
        val hijri = HijrahDate.from(date)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(ChronoField.YEAR_OF_ERA)
        val monthName = (if (arabic) arabicMonths else italianMonths)[month - 1]
        return if (arabic) "$day $monthName $year هـ" else "$day $monthName $year AH"
    }
}
