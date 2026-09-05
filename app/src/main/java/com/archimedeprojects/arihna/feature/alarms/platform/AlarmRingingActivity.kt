package com.archimedeprojects.arihna.feature.alarms.platform

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class AlarmRingingActivity : ComponentActivity() {
    private var payload: AlarmRingingPayload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAlarmWindow()
        payload = AlarmRingingIntentContract.decode(intent)
        val current = payload ?: run {
            finish()
            return
        }
        setContent {
            ArihnaTheme(darkTheme = true) {
                AlarmRingingScreen(current, onStop = ::stopAndFinish, onSnooze = ::snoozeAndFinish)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        payload = AlarmRingingIntentContract.decode(intent)
        payload?.let { current ->
            setContent {
                ArihnaTheme(darkTheme = true) {
                    AlarmRingingScreen(current, onStop = ::stopAndFinish, onSnooze = ::snoozeAndFinish)
                }
            }
        }
    }

    private fun configureAlarmWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun stopAndFinish() {
        payload?.alarmId?.let { alarmId ->
            startService(AlarmRingingService.stopIntent(this, alarmId))
        } ?: stopService(android.content.Intent(this, AlarmRingingService::class.java))
        finishAndRemoveTask()
    }

    private fun snoozeAndFinish() {
        payload?.let { current ->
            startService(AlarmRingingService.snoozeIntent(this, current))
        }
        finishAndRemoveTask()
    }

    @Composable
    private fun AlarmRingingScreen(
        payload: AlarmRingingPayload,
        onStop: () -> Unit,
        onSnooze: () -> Unit,
    ) {
        BackHandler(onBack = onStop)
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = LocalTime.now()
                delay(1_000L)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF062E22), Color(0xFF0A4734), Color(0xFF041F18)),
                    ),
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = ArihnaGold.copy(alpha = 0.10f)
                drawCircle(glow, size.minDimension * 0.42f, Offset(size.width * 0.82f, size.height * 0.15f))
                drawCircle(glow, size.minDimension * 0.25f, Offset(size.width * 0.15f, size.height * 0.82f))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = ArihnaGold.copy(alpha = 0.16f),
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        Text(
                            text = if (payload.soundProfile == AlarmSoundProfile.ADHAN) "☾" else "♪",
                            color = ArihnaGold,
                            fontSize = 54.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                    Spacer(Modifier.height(34.dp))
                    Text(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        payload.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ArihnaGold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (payload.soundProfile) {
                            AlarmSoundProfile.ADHAN -> "È il momento della preghiera"
                            AlarmSoundProfile.SYSTEM_DEFAULT -> "Sveglia Arihna"
                            AlarmSoundProfile.SILENT -> "Sveglia silenziosa"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Scorri dai bordi per mostrare temporaneamente i controlli di sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArihnaGold,
                            contentColor = ArihnaGreen,
                        ),
                    ) {
                        Text("Interrompi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Rinvia 5 min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
