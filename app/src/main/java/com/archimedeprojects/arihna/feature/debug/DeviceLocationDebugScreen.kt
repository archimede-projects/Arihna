package com.archimedeprojects.arihna.feature.debug

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Temporary manual-test surface for STEP 5 real-device verification.
 *
 * This is intentionally not the STEP 6 product UI and must be removed before promoting
 * the bridge out of the diagnostic branch.
 */
@Composable
fun DeviceLocationDebugScreen(
    dataSource: DeviceLocationDataSource,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var coarseGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var currentResult by remember { mutableStateOf("Non richiesto") }
    var observing by remember { mutableStateOf(false) }
    var observationState by remember { mutableStateOf("Non attivo") }
    var updateCount by remember { mutableIntStateOf(0) }
    var lastUpdate by remember { mutableStateOf("Nessun aggiornamento ricevuto") }

    suspend fun fetchCurrentFix() {
        currentResult = "Richiesta in corso…"
        val result = withTimeoutOrNull(CURRENT_FIX_UI_TIMEOUT_MILLIS) {
            dataSource.getCurrentLocation()
        }
        currentResult = when (result) {
            is DeviceLocationResult.Success -> formatFix("SUCCESS", result.fix)
            is DeviceLocationResult.Unavailable -> "UNAVAILABLE: ${result.reason}"
            null -> "TIMEOUT diagnostico dopo 20 s"
        }
    }

    val coarsePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        coarseGranted = granted
        if (granted) {
            scope.launch { fetchCurrentFix() }
        } else {
            currentResult = "ACCESS_COARSE_LOCATION negato"
            observing = false
        }
    }

    LaunchedEffect(observing, coarseGranted) {
        if (!observing || !coarseGranted) return@LaunchedEffect
        observationState = "In ascolto tramite observeSignificantUpdates()"
        dataSource.observeSignificantUpdates().collect { fix ->
            updateCount += 1
            lastUpdate = formatFix("UPDATE #$updateCount", fix)
        }
        if (observing) {
            observationState = "Stream chiuso dal bridge/provider"
            observing = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Arihna — STEP 5 Device Test",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text("Hook temporaneo: LocationManager, ACCESS_COARSE_LOCATION soltanto.")
        Text("Permesso COARSE: ${if (coarseGranted) "GRANTED" else "NOT GRANTED"}")

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (coarseGranted) {
                    scope.launch { fetchCurrentFix() }
                } else {
                    coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
        ) {
            Text(if (coarseGranted) "Ottieni fix corrente" else "Concedi posizione + ottieni fix")
        }

        Text("getCurrentLocation():")
        Text(currentResult, style = MaterialTheme.typography.bodyMedium)

        HorizontalDivider()

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = coarseGranted,
            onClick = { observing = !observing },
        ) {
            Text(if (observing) "Ferma aggiornamenti" else "Avvia aggiornamenti")
        }
        Text("observeSignificantUpdates(): $observationState")
        Text("Aggiornamenti ricevuti: $updateCount")
        Text(lastUpdate, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Nota: lo stream usa l'intervallo foreground STEP 5 (15 min). Il fix corrente è il controllo manuale principale; un update dello stream può non arrivare immediatamente.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatFix(label: String, fix: DeviceLocationFix): String = buildString {
    append(label)
    append("\nlat=")
    append(fix.coordinates.latitude)
    append("\nlon=")
    append(fix.coordinates.longitude)
    append("\naccuracyMeters=")
    append(fix.accuracyMeters ?: "<none>")
    append("\nzoneId=")
    append(fix.zoneId.id)
    append("\ncapturedAt=")
    append(fix.capturedAt)
}

private const val CURRENT_FIX_UI_TIMEOUT_MILLIS = 20_000L
