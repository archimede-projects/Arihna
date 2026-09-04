package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.archimedeprojects.arihna.ArihnaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmOccurrenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val envelope = AlarmIntentContract.decode(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as? ArihnaApplication ?: return@launch
                val rule = application.appContainer.alarmRuleRepository.get(envelope.alarmId)
                AlarmOccurrenceEnvelopeValidator.isCurrent(envelope, rule)
                // STEP 3 deliberately has no user-visible delivery. STEP 5 owns notification/sound.
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class AlarmSystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reason = reasonFor(intent.action) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as? ArihnaApplication ?: return@launch
                application.appContainer.alarmReconciliationTrigger.request(reason)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        internal fun reasonFor(action: String?): AlarmReconciliationReason? = when (action) {
            Intent.ACTION_BOOT_COMPLETED -> AlarmReconciliationReason.BOOT_COMPLETED
            Intent.ACTION_TIME_CHANGED -> AlarmReconciliationReason.WALL_CLOCK_CHANGED
            Intent.ACTION_TIMEZONE_CHANGED -> AlarmReconciliationReason.TIMEZONE_CHANGED
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                AlarmReconciliationReason.EXACT_ALARM_ACCESS_CHANGED
            Intent.ACTION_MY_PACKAGE_REPLACED -> AlarmReconciliationReason.APP_REPLACED
            else -> null
        }
    }
}
