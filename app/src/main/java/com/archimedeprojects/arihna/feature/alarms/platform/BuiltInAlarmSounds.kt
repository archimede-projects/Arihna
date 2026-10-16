package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.R

internal const val ARIHNA_CLASSIC_ALARM_URI = "arihna://alarm/classic-vintage"
internal const val ARIHNA_CLASSIC_ALARM_TITLE = "Sveglia classica Arihna"

internal fun isArihnaClassicAlarm(uri: String?): Boolean = uri == ARIHNA_CLASSIC_ALARM_URI

internal fun arihnaClassicAlarmRawResource(): Int = R.raw.arihna_classic_alarm
