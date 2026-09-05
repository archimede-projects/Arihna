package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class AlarmOverlayAccess(context: Context) {
    private val appContext = context.applicationContext

    fun isGranted(): Boolean = Settings.canDrawOverlays(appContext)

    fun createSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
        data = Uri.parse("package:${appContext.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
