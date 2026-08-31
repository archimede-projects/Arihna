package com.archimedeprojects.arihna.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.location.diagnostics.render
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun LocationDiagnosticOverlay(
    dataSource: DeviceLocationDataSource,
    modifier: Modifier = Modifier,
) {
    val events by LocationDiagnosticTrace.events.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }
    var directProbeRunning by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        ) {
            Text("DIAG")
        }
    }

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Diagnostica Location",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Solo tracciamento: nessuna correzione della logica Location è applicata.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { LocationDiagnosticTrace.clear() }) {
                            Text("Pulisci")
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(events.joinToString("\n") { it.render() }),
                                )
                                LocationDiagnosticTrace.record("TRACE_COPIED", "events=${events.size}")
                            },
                        ) {
                            Text("Copia")
                        }
                    }

                    Button(
                        enabled = !directProbeRunning,
                        onClick = {
                            scope.launch {
                                directProbeRunning = true
                                LocationDiagnosticTrace.record("DIRECT_PROBE_START")
                                val result = withTimeoutOrNull(DIRECT_PROBE_TIMEOUT_MILLIS) {
                                    dataSource.getCurrentLocation()
                                }
                                when (result) {
                                    is DeviceLocationResult.Success -> LocationDiagnosticTrace.record(
                                        "DIRECT_PROBE_SUCCESS",
                                        "accuracy=${result.fix.accuracyMeters} zone=${result.fix.zoneId.id}",
                                    )

                                    is DeviceLocationResult.Unavailable -> LocationDiagnosticTrace.record(
                                        "DIRECT_PROBE_UNAVAILABLE",
                                        "reason=${result.reason}",
                                    )

                                    null -> LocationDiagnosticTrace.record(
                                        "DIRECT_PROBE_TIMEOUT",
                                        "after=${DIRECT_PROBE_TIMEOUT_MILLIS}ms",
                                    )
                                }
                                directProbeRunning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (directProbeRunning) "Probe diretto in corso…" else "Test diretto bridge")
                    }

                    Text(
                        text = "Eventi: ${events.size} (più recenti in fondo)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(events, key = { it.sequence }) { event ->
                            Text(
                                text = event.render(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { open = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Chiudi")
                    }
                }
            }
        }
    }
}

private const val DIRECT_PROBE_TIMEOUT_MILLIS = 20_000L
