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
import com.archimedeprojects.arihna.core.location.diagnostics.FusedLocationUpdatesProbe
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.location.diagnostics.NetworkLocationUpdatesProbe
import com.archimedeprojects.arihna.core.location.diagnostics.ProviderCurrentLocationProbe
import com.archimedeprojects.arihna.core.location.diagnostics.render
import kotlinx.coroutines.launch

@Composable
fun LocationDiagnosticOverlay(
    providerProbe: ProviderCurrentLocationProbe,
    requestUpdatesProbe: NetworkLocationUpdatesProbe,
    fusedRequestUpdatesProbe: FusedLocationUpdatesProbe,
    modifier: Modifier = Modifier,
) {
    val events by LocationDiagnosticTrace.events.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }
    var activeProbe by remember { mutableStateOf<String?>(null) }

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
                    .fillMaxHeight(0.92f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Diagnostica S25 — current vs updates", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Solo osservabilità: nessun cambio production. I probe possono modificare le cache di sistema: eseguili separatamente, Pulisci prima di ciascuno e copia la traccia solo dopo il terminale.",
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
                                clipboard.setText(AnnotatedString(events.joinToString("\n") { it.render() }))
                                LocationDiagnosticTrace.record("TRACE_COPIED", "events=${events.size}")
                            },
                        ) {
                            Text("Copia")
                        }
                    }

                    Button(
                        enabled = activeProbe == null,
                        onClick = {
                            scope.launch {
                                activeProbe = "AB"
                                try {
                                    providerProbe.runParallel()
                                } catch (error: Throwable) {
                                    LocationDiagnosticTrace.record(
                                        "AB_PROBE_THROW",
                                        "${error.javaClass.simpleName}: ${error.message}",
                                    )
                                } finally {
                                    activeProbe = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (activeProbe == "AB") "A/B in corso…" else "A/B getCurrent network vs fused (35s)")
                    }

                    Button(
                        enabled = activeProbe == null,
                        onClick = {
                            scope.launch {
                                activeProbe = "RU"
                                try {
                                    requestUpdatesProbe.run()
                                } catch (error: Throwable) {
                                    LocationDiagnosticTrace.record(
                                        "RU_PROBE_THROW",
                                        "${error.javaClass.simpleName}: ${error.message}",
                                    )
                                } finally {
                                    activeProbe = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (activeProbe == "RU") {
                                "Network updates in corso…"
                            } else {
                                "Piano B network updates bounded (35s)"
                            },
                        )
                    }

                    Button(
                        enabled = activeProbe == null,
                        onClick = {
                            scope.launch {
                                activeProbe = "FRU"
                                try {
                                    fusedRequestUpdatesProbe.run()
                                } catch (error: Throwable) {
                                    LocationDiagnosticTrace.record(
                                        "FRU_PROBE_THROW",
                                        "${error.javaClass.simpleName}: ${error.message}",
                                    )
                                } finally {
                                    activeProbe = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (activeProbe == "FRU") {
                                "Fused updates in corso…"
                            } else {
                                "Piano B fused updates bounded (35s)"
                            },
                        )
                    }

                    Text(
                        "Probe bounded network/fused: interval=10s, minInterval=0, distance=0, no batching, balanced, duration=35s. ACCEPTED significa solo age≤10s nel probe; NON definisce FRESH production.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Cerca: PRODUCTION_ONE_SHOT_*, AB_*, RU_*, FRU_*, CITY_NEAREST_*, UPDATES_FIX.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Text("Eventi: ${events.size} (più recenti in fondo)", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(events, key = { it.sequence }) { event ->
                            Text(event.render(), style = MaterialTheme.typography.bodySmall)
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
