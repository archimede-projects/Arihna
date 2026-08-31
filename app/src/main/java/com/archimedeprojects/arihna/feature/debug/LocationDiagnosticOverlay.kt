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
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.location.diagnostics.ProviderCurrentLocationProbe
import com.archimedeprojects.arihna.core.location.diagnostics.render
import kotlinx.coroutines.launch

@Composable
fun LocationDiagnosticOverlay(
    providerProbe: ProviderCurrentLocationProbe,
    modifier: Modifier = Modifier,
) {
    val events by LocationDiagnosticTrace.events.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var open by remember { mutableStateOf(false) }
    var probeRunning by remember { mutableStateOf(false) }

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
                    .fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Diagnostica A/B Location + Home", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Solo osservabilità: nessuna correzione production. A/B esegue getCurrentLocation esplicito su network e fused in parallelo e registra anche l'età reale dei fix.",
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
                        enabled = !probeRunning,
                        onClick = {
                            scope.launch {
                                probeRunning = true
                                try {
                                    providerProbe.runParallel()
                                } catch (error: Throwable) {
                                    LocationDiagnosticTrace.record(
                                        "AB_PROBE_THROW",
                                        "${error.javaClass.simpleName}: ${error.message}",
                                    )
                                } finally {
                                    probeRunning = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (probeRunning) "A/B in corso…" else "A/B network vs fused")
                    }

                    Text(
                        "Per la Home: pulisci la traccia mentre Location è già Ready, chiudi DIAG e apri Home. Poi attendi gli orari, riapri DIAG e copia tutto.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Text("Eventi: ${events.size} (più recenti in fondo)", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp),
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
