package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationSettingsScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainResolutionStatesRenderExplicitly() {
        var state by mutableStateOf(LocationSettingsUiState())
        setScreen { state }

        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Unconfigured,
                activeMode = LocationModeUi.Unconfigured,
            ),
            updateState = { state = it },
            "Posizione non configurata",
            "Non configurata",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Resolving,
                activeMode = LocationModeUi.Device,
            ),
            updateState = { state = it },
            "Risoluzione in corso",
            "Device",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Ready(
                    location = selectedDevice(),
                    freshness = LocationFreshness.FRESH,
                ),
                activeMode = LocationModeUi.Device,
            ),
            updateState = { state = it },
            "Ferrara, Italia",
            "FRESH",
            "Europe/Rome",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Ready(
                    location = selectedManual(),
                    freshness = null,
                ),
                activeMode = LocationModeUi.Manual,
            ),
            updateState = { state = it },
            "Makkah, Saudi Arabia",
            "Manuale",
        )
    }

    @Test
    fun premiumSectionsReplaceVerboseTemporaryCopy() {
        setScreen { LocationSettingsUiState() }

        composeRule.onNodeWithText("Impostazioni").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Pannello funzionale STEP 6 — la Home definitiva verrà costruita più avanti.")
                .fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            composeRule.onAllNodesWithText("Controlli di sistema e test rapidi per sveglie e Adhan.")
                .fetchSemanticsNodes().isEmpty(),
        )

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Sveglie e notifiche"))
        composeRule.onNodeWithText("Sveglie e notifiche").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Test rapidi"))
        composeRule.onNodeWithText("Test rapidi").assertIsDisplayed()
    }

    @Test
    fun permissionServicesTimeoutAndUnsupportedErrorsAreUnderstandable() {
        var state by mutableStateOf(LocationSettingsUiState())
        setScreen { state }

        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.PermissionDenied(
                    canRequestAgain = false,
                    cachedLocation = null,
                ),
                activeMode = LocationModeUi.Device,
            ),
            updateState = { state = it },
            "Permesso posizione non concesso",
            "Apri impostazioni app",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.LocationServicesDisabled(null),
                activeMode = LocationModeUi.Device,
            ),
            updateState = { state = it },
            "Servizi di localizzazione disattivati",
            "Apri impostazioni Posizione",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Unavailable(
                    reason = LocationFailure.TIMEOUT,
                    cachedLocation = null,
                ),
                activeMode = LocationModeUi.Device,
            ),
            updateState = { state = it },
            "Posizione non ricevuta",
            "Nessuna posizione corrente è arrivata entro 30 secondi e non è disponibile alcuna posizione reale salvata. Puoi riprovare o scegliere una città manuale.",
        )
        assertState(
            newState = LocationSettingsUiState(
                resolutionState = LocationResolutionState.Unavailable(
                    reason = LocationFailure.UNSUPPORTED_TIME_ZONE,
                    cachedLocation = null,
                ),
                activeMode = LocationModeUi.Manual,
            ),
            updateState = { state = it },
            "Fuso orario non supportato",
            "Questa città usa un fuso orario che questa versione di Android non può risolvere in modo affidabile. Scegli un’altra città.",
        )
    }

    @Test
    fun permissionRequestCallbackIsGatedBehindExplicitRationaleConfirmation() {
        var state by mutableStateOf(LocationSettingsUiState())
        var permissionLaunchRequests = 0

        composeRule.setContent {
            ArihnaTheme {
                LocationSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = state,
                    onUseDevice = { state = state.copy(rationaleVisible = true) },
                    onDismissRationale = { state = state.copy(rationaleVisible = false) },
                    onConfirmRationale = {
                        permissionLaunchRequests += 1
                        state = state.copy(rationaleVisible = false)
                    },
                    onSearchQueryChanged = {},
                    onSelectCity = {},
                    onOpenAppSettings = {},
                    onOpenLocationSettings = {},
                )
            }
        }

        composeRule.runOnIdle { assertEquals(0, permissionLaunchRequests) }
        composeRule.onNodeWithText("Usa posizione attuale").performClick()
        composeRule.onNodeWithText("Perché Arihna chiede la posizione").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, permissionLaunchRequests) }
        composeRule.onNodeWithText("Continua").performClick()
        composeRule.runOnIdle { assertEquals(1, permissionLaunchRequests) }
    }

    @Test
    fun manualSearchResultShowsUnsupportedWarningAndSelectionCallback() {
        val unsupportedCity = CitySearchResult(
            id = 3412093L,
            name = "Nuuk",
            regionName = "Sermersooq",
            countryName = "Greenland",
            countryCode = "GL",
            coordinates = Coordinates(64.18347, -51.72157),
            timeZoneId = "America/Nuuk",
            timeZoneSupported = false,
        )
        val state = LocationSettingsUiState(
            searchQuery = "Nuuk",
            searchResults = listOf(unsupportedCity),
        )
        var selectedCityId: Long? = null

        composeRule.setContent {
            ArihnaTheme {
                LocationSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = state,
                    onUseDevice = {},
                    onDismissRationale = {},
                    onConfirmRationale = {},
                    onSearchQueryChanged = {},
                    onSelectCity = { selectedCityId = it },
                    onOpenAppSettings = {},
                    onOpenLocationSettings = {},
                )
            }
        }

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Nuuk, Sermersooq, Greenland"))
        composeRule.onNodeWithText("Nuuk, Sermersooq, Greenland")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Fuso non supportato su questa versione Android: selezionando la città Arihna mostrerà un errore controllato.",
        )
            .assertIsDisplayed()
        composeRule.onNodeWithText("Nuuk, Sermersooq, Greenland")
            .performClick()
        composeRule.runOnIdle { assertEquals(3412093L, selectedCityId) }
    }

    private fun setScreen(state: () -> LocationSettingsUiState) {
        composeRule.setContent {
            ArihnaTheme {
                LocationSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = state(),
                    onUseDevice = {},
                    onDismissRationale = {},
                    onConfirmRationale = {},
                    onSearchQueryChanged = {},
                    onSelectCity = {},
                    onOpenAppSettings = {},
                    onOpenLocationSettings = {},
                )
            }
        }
    }

    private fun assertState(
        newState: LocationSettingsUiState,
        updateState: (LocationSettingsUiState) -> Unit,
        vararg expectedTexts: String,
    ) {
        composeRule.runOnIdle { updateState(newState) }
        expectedTexts.forEach { text ->
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    private fun selectedDevice() = SelectedLocation(
        source = LocationSource.Device(
            capturedAt = Instant.parse("2026-08-30T10:44:05.566Z"),
            accuracyMeters = 2_000f,
        ),
        coordinates = Coordinates(44.8, 11.0),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Ferrara, Italia",
    )

    private fun selectedManual() = SelectedLocation(
        source = LocationSource.Manual(cityId = 104515L),
        coordinates = Coordinates(21.4225, 39.8262),
        zoneId = ZoneId.of("Asia/Riyadh"),
        displayName = "Makkah, Saudi Arabia",
    )
}
