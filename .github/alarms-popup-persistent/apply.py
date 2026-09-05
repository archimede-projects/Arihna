from pathlib import Path

SPEC_SHA = "4e6c1b7c10c6b18b536790fe23b4b9d404b596b6"


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:80]!r}")
    file.write_text(text.replace(old, new, 1))


service = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingService.kt"
replace_once(service, "import android.os.Looper\n", "import android.os.Looper\nimport android.widget.RemoteViews\n")
replace_once(
    service,
    '''        val builder = NotificationCompat.Builder(context, channel)\n            .setSmallIcon(R.drawable.ic_notification_arihna)\n            .setContentTitle(payload.title)\n            .setContentText(subtitle)\n            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))\n            .setTicker("${payload.title} • Sveglia")\n            .setCategory(NotificationCompat.CATEGORY_ALARM)\n            .setPriority(NotificationCompat.PRIORITY_MAX)\n            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)\n            .setOngoing(true)\n            .setAutoCancel(false)\n            .setOnlyAlertOnce(false)\n            .setShowWhen(true)\n            .setWhen(System.currentTimeMillis())\n            .setContentIntent(activityPendingIntent)\n            .addAction(0, "Interrompi", stopPendingIntent)\n            .addAction(0, "Rinvia", snoozePendingIntent)\n        if (fullScreenAccess.isGranted()) {\n            builder.setFullScreenIntent(activityPendingIntent, true)\n        }\n        return builder.build()\n''',
    '''        fun alarmRemoteViews(): RemoteViews = RemoteViews(\n            context.packageName,\n            R.layout.notification_alarm_heads_up,\n        ).apply {\n            setTextViewText(R.id.notification_alarm_title, payload.title)\n            setTextViewText(R.id.notification_alarm_subtitle, subtitle)\n            setOnClickPendingIntent(R.id.notification_alarm_stop, stopPendingIntent)\n            setOnClickPendingIntent(R.id.notification_alarm_snooze, snoozePendingIntent)\n        }\n        val builder = NotificationCompat.Builder(context, channel)\n            .setSmallIcon(R.drawable.ic_notification_arihna)\n            .setContentTitle(payload.title)\n            .setContentText(subtitle)\n            .setTicker("${payload.title} • Sveglia")\n            .setCategory(NotificationCompat.CATEGORY_ALARM)\n            .setPriority(NotificationCompat.PRIORITY_MAX)\n            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)\n            .setOngoing(true)\n            .setAutoCancel(false)\n            .setOnlyAlertOnce(false)\n            .setShowWhen(true)\n            .setWhen(System.currentTimeMillis())\n            .setContentIntent(activityPendingIntent)\n            .setCustomHeadsUpContentView(alarmRemoteViews())\n            .setCustomBigContentView(alarmRemoteViews())\n            .setStyle(NotificationCompat.DecoratedCustomViewStyle())\n            .setFullScreenIntent(activityPendingIntent, true)\n            .addAction(0, "Interrompi", stopPendingIntent)\n            .addAction(0, "Rinvia", snoozePendingIntent)\n        return builder.build()\n''',
)

activity = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingActivity.kt"
replace_once(activity, "import androidx.compose.material3.MaterialTheme\n", "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.OutlinedButton\n")
replace_once(
    activity,
    "AlarmRingingScreen(current, onStop = ::stopAndFinish)",
    "AlarmRingingScreen(current, onStop = ::stopAndFinish, onSnooze = ::snoozeAndFinish)",
)
replace_once(
    activity,
    "AlarmRingingScreen(current, onStop = ::stopAndFinish)",
    "AlarmRingingScreen(current, onStop = ::stopAndFinish, onSnooze = ::snoozeAndFinish)",
)
replace_once(
    activity,
    '''    private fun stopAndFinish() {\n        payload?.alarmId?.let { alarmId ->\n            startService(AlarmRingingService.stopIntent(this, alarmId))\n        } ?: stopService(android.content.Intent(this, AlarmRingingService::class.java))\n        finishAndRemoveTask()\n    }\n\n    @Composable\n    private fun AlarmRingingScreen(payload: AlarmRingingPayload, onStop: () -> Unit) {\n''',
    '''    private fun stopAndFinish() {\n        payload?.alarmId?.let { alarmId ->\n            startService(AlarmRingingService.stopIntent(this, alarmId))\n        } ?: stopService(android.content.Intent(this, AlarmRingingService::class.java))\n        finishAndRemoveTask()\n    }\n\n    private fun snoozeAndFinish() {\n        payload?.let { current ->\n            startService(AlarmRingingService.snoozeIntent(this, current))\n        }\n        finishAndRemoveTask()\n    }\n\n    @Composable\n    private fun AlarmRingingScreen(\n        payload: AlarmRingingPayload,\n        onStop: () -> Unit,\n        onSnooze: () -> Unit,\n    ) {\n''',
)
replace_once(
    activity,
    '''                    Button(\n                        onClick = onStop,\n                        modifier = Modifier.fillMaxWidth().height(64.dp),\n                        shape = RoundedCornerShape(22.dp),\n                        colors = ButtonDefaults.buttonColors(\n                            containerColor = ArihnaGold,\n                            contentColor = ArihnaGreen,\n                        ),\n                    ) {\n                        Text("Stop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)\n                    }\n''',
    '''                    Button(\n                        onClick = onStop,\n                        modifier = Modifier.fillMaxWidth().height(64.dp),\n                        shape = RoundedCornerShape(22.dp),\n                        colors = ButtonDefaults.buttonColors(\n                            containerColor = ArihnaGold,\n                            contentColor = ArihnaGreen,\n                        ),\n                    ) {\n                        Text("Interrompi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)\n                    }\n                    Spacer(Modifier.height(12.dp))\n                    OutlinedButton(\n                        onClick = onSnooze,\n                        modifier = Modifier.fillMaxWidth().height(56.dp),\n                        shape = RoundedCornerShape(20.dp),\n                    ) {\n                        Text("Rinvia 5 min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)\n                    }\n''',
)

diagnostic = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmDiagnosticTestScheduler.kt"
replace_once(diagnostic, "const val TEST_DELAY_MILLIS = 60_000L", "const val TEST_DELAY_MILLIS = 20_000L")

settings = "app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt"
for old, new in [
    ("Test Adhan programmato tra 1 minuto", "Test Adhan programmato tra 20 secondi"),
    ("Test sveglia programmato tra 1 minuto", "Test sveglia programmato tra 20 secondi"),
    ("Esegue un test reale tra 1 minuto usando lo stesso percorso di sveglie, notifica e schermo intero.", "Esegue un test reale tra 20 secondi usando lo stesso percorso di sveglie, notifica e schermo intero."),
    ("Test sveglia (1 minuto)", "Test sveglia (20 secondi)"),
    ("Test Adhan (1 minuto)", "Test Adhan (20 secondi)"),
]:
    replace_once(settings, old, new)

android_test = "app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmNotificationAndroidTest.kt"
replace_once(
    android_test,
    '''        assertNotNull(notification.contentIntent)\n        assertEquals(Notification.CATEGORY_ALARM, notification.category)\n''',
    '''        assertNotNull(notification.contentIntent)\n        assertNotNull(notification.headsUpContentView)\n        assertNotNull(notification.bigContentView)\n        assertEquals(Notification.CATEGORY_ALARM, notification.category)\n''',
)

layout = Path("app/src/main/res/layout/notification_alarm_heads_up.xml")
layout.parent.mkdir(parents=True, exist_ok=True)
if layout.exists():
    raise SystemExit(f"unexpected existing layout: {layout}")
layout.write_text('''<?xml version="1.0" encoding="utf-8"?>\n<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n    android:layout_width="match_parent"\n    android:layout_height="wrap_content"\n    android:orientation="vertical"\n    android:paddingStart="12dp"\n    android:paddingTop="8dp"\n    android:paddingEnd="12dp"\n    android:paddingBottom="8dp">\n\n    <TextView\n        android:id="@+id/notification_alarm_title"\n        android:layout_width="match_parent"\n        android:layout_height="wrap_content"\n        android:ellipsize="end"\n        android:maxLines="1"\n        android:textColor="#FFFFFFFF"\n        android:textSize="17sp"\n        android:textStyle="bold" />\n\n    <TextView\n        android:id="@+id/notification_alarm_subtitle"\n        android:layout_width="match_parent"\n        android:layout_height="wrap_content"\n        android:layout_marginTop="2dp"\n        android:ellipsize="end"\n        android:maxLines="1"\n        android:textColor="#CCFFFFFF"\n        android:textSize="14sp" />\n\n    <LinearLayout\n        android:layout_width="match_parent"\n        android:layout_height="wrap_content"\n        android:layout_marginTop="6dp"\n        android:orientation="horizontal">\n\n        <Button\n            android:id="@+id/notification_alarm_stop"\n            android:layout_width="0dp"\n            android:layout_height="42dp"\n            android:layout_weight="1"\n            android:text="Interrompi"\n            android:textAllCaps="false"\n            android:textSize="14sp" />\n\n        <Space\n            android:layout_width="8dp"\n            android:layout_height="1dp" />\n\n        <Button\n            android:id="@+id/notification_alarm_snooze"\n            android:layout_width="0dp"\n            android:layout_height="42dp"\n            android:layout_weight="1"\n            android:text="Rinvia"\n            android:textAllCaps="false"\n            android:textSize="14sp" />\n    </LinearLayout>\n</LinearLayout>\n''')
