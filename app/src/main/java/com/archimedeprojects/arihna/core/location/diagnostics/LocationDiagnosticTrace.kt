package com.archimedeprojects.arihna.core.location.diagnostics

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationDiagnosticEvent(
    val sequence: Long,
    val elapsedMillis: Long,
    val thread: String,
    val stage: String,
    val detail: String?,
)

object LocationDiagnosticTrace {
    private const val TAG = "ArihnaDiagnostic"
    private const val MAX_EVENTS = 600
    private val originElapsedMillis = SystemClock.elapsedRealtime()
    private val sequence = AtomicLong(0L)
    private val _events = MutableStateFlow<List<LocationDiagnosticEvent>>(emptyList())

    val events: StateFlow<List<LocationDiagnosticEvent>> = _events.asStateFlow()

    fun record(stage: String, detail: String? = null) {
        val event = LocationDiagnosticEvent(
            sequence = sequence.incrementAndGet(),
            elapsedMillis = SystemClock.elapsedRealtime() - originElapsedMillis,
            thread = Thread.currentThread().name,
            stage = stage,
            detail = detail,
        )
        Log.i(TAG, event.render())
        _events.update { current -> (current + event).takeLast(MAX_EVENTS) }
    }

    fun clear() {
        _events.value = emptyList()
        record("TRACE_CLEARED")
    }
}

fun LocationDiagnosticEvent.render(): String = buildString {
    append('#')
    append(sequence)
    append(" +")
    append(elapsedMillis)
    append("ms [")
    append(thread)
    append("] ")
    append(stage)
    detail?.takeIf { it.isNotBlank() }?.let {
        append(" | ")
        append(it)
    }
}
