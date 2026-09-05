package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

data class AlarmVolumeState(
    val current: Int,
    val min: Int,
    val max: Int,
) {
    val percent: Int
        get() = if (max <= min) {
            100
        } else {
            (((current - min).toFloat() / (max - min).toFloat()) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
        }
}

sealed interface AlarmVolumeChangeResult {
    data class Success(val state: AlarmVolumeState) : AlarmVolumeChangeResult
    data class Failure(val state: AlarmVolumeState, val message: String) : AlarmVolumeChangeResult
}

class AlarmVolumeController(context: Context) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun read(): AlarmVolumeState {
        val min = audioManager.getStreamMinVolume(AudioManager.STREAM_ALARM)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM).coerceIn(min, max)
        return AlarmVolumeState(current = current, min = min, max = max)
    }

    fun setVolume(requested: Int): AlarmVolumeChangeResult {
        val before = read()
        if (audioManager.isVolumeFixed) {
            return AlarmVolumeChangeResult.Failure(
                state = before,
                message = "Il dispositivo usa un volume fisso e non consente questa modifica.",
            )
        }
        val target = requested.coerceIn(before.min, before.max)
        return try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
            val after = read()
            if (after.current == target) {
                AlarmVolumeChangeResult.Success(after)
            } else {
                AlarmVolumeChangeResult.Failure(
                    state = after,
                    message = "Android non ha applicato il volume richiesto.",
                )
            }
        } catch (_: SecurityException) {
            AlarmVolumeChangeResult.Failure(
                state = read(),
                message = "Android ha bloccato la modifica del volume sveglia.",
            )
        } catch (_: RuntimeException) {
            AlarmVolumeChangeResult.Failure(
                state = read(),
                message = "Impossibile modificare il volume sveglia su questo dispositivo.",
            )
        }
    }
}
