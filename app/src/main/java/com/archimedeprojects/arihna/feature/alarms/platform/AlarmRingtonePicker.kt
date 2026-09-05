package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

object AlarmRingtonePicker {
    fun createIntent(existingUri: String? = null): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            existingUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }

    @Suppress("DEPRECATION")
    fun pickedUri(data: Intent?): Uri? =
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

    fun title(context: Context, uri: Uri): String? = runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
