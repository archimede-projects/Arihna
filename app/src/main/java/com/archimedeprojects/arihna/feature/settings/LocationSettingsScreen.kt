package com.archimedeprojects.arihna.feature.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver

@Composable
fun LocationSettingsRoute(
    contentPadding: PaddingValues,
    activity: Activity,
    viewModel: LocationSettingsViewModel,
    environment: AndroidLocationEnvironment,
    permissionResolver: AndroidLocationPermissionStateResolver,
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        val permissionState = permissionResolver.resolve(
            activity = activity,
            hasRequestedBefore = viewModel.hasRequestedPermissionBefore(),
        )
        viewModel.selectDevice(
            permissionState = permissionState,
            locationServicesEnabled = environment.isLocationServicesEnabled(),
        )
    }

    LocationSettingsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onUseDevice = viewModel::onUseDeviceClick,
        onDismissRationale = viewModel::dismissRationale,
        onConfirmRationale = {
            if (environment.isCoarsePermissionGranted()) {
                viewModel.dismissRationale()
                viewModel.selectDevice(
                    permissionState = LocationPermissionState.Granted,
                    locationServicesEnabled = environment.isLocationServicesEnabled(),
                )
            } else {
                viewModel.markPermissionRequestStarted()
                permissionLauncher.launch(permissionResolver.permission)
            }
        },
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSelectCity = viewModel::selectManual,
        onOpenAppSettings = {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
        },
        onOpenLocationSettings = {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
    )
}

@Composable
fun LocationSettingsScreen(
    contentPadding: PaddingValues,
    uiState: LocationSettingsUiState,
    onUseDevice: () -> Unit,
    onDismissRationale: () -> Unit,
    onConfirmRationale: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCity: (Long) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val presentation = uiState.resolutionState.toPresentation()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Posizione",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Pannello funzionale STEP 6 — la Home definitiva verrà costruita più avanti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Stato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SourceBadge(uiState.activeMode.label())
                    }

                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = presentation.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    presentation.locationName?.let {
                        HorizontalDivider()
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    presentation.zoneId?.let {
                        Text(
                            text = "Fuso orario: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    presentation.freshness?.let {
                        Text(
                            text = "Dati: $it",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (it == "FRESH") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        )
                    }

                    if (uiState.resolutionState is LocationResolutionState.Resolving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    if (presentation.showAppSettingsAction) {
                        OutlinedButton(onClick = onOpenAppSettings) {
                            Text("Apri impostazioni app")
                        }
                    }
                    if (presentation.showLocationSettingsAction) {
                        OutlinedButton(onClick = onOpenLocationSettings) {
                            Text("Apri impostazioni Posizione")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Posizione del dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Arihna richiede solo la posizione approssimativa e la usa localmente per calcolare gli orari di preghiera.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onUseDevice,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Usa posizione attuale")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Città manuale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "La ricerca usa l’archivio GeoNames offline incluso in Arihna. Non serve concedere la posizione.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        label = { Text("Cerca città") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.searchInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Ricerca nell’archivio locale…")
                        }
                    }
                    uiState.searchMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(
            items = uiState.searchResults,
            key = { city -> city.id },
        ) { city ->
            CityResultCard(city = city, onSelectCity = onSelectCity)
        }
    }

    if (uiState.rationaleVisible) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = { Text("Perché Arihna chiede la posizione") },
            text = {
                Text(
                    "La posizione approssimativa è sufficiente per calcolare gli orari di preghiera. " +
                        "Resta sul dispositivo e non viene inviata a servizi esterni. " +
                        "Se preferisci non concederla, puoi sempre scegliere una città manualmente.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRationale) {
                    Text("Continua")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRationale) {
                    Text("Annulla")
                }
            },
        )
    }
}

@Composable
private fun SourceBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = "Origine: $label",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CityResultCard(
    city: CitySearchResult,
    onSelectCity: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCity(city.id) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = city.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Fuso: ${city.timeZoneId}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!city.timeZoneSupported) {
                Text(
                    text = "Fuso non supportato su questa versione Android: selezionando la città Arihna mostrerà un errore controllato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
