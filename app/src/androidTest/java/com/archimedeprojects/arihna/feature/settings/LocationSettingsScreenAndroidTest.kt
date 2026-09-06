package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun premiumNonScrollableSettingsKeepsRequiredControlsAndHidesTechnicalProvenance() {
        setScreen {
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.Ready(
                    location = selectedDevice(),
                    freshness = LocationFreshness.CACHED,
                ),
                activeMode = LocationModeUi.Device,
            )
        }

        composeRule.onNodeWithText("Impostazioni").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-location-search").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-use-current-location").assertIsDisplayed()
        composeRule.onNodeWithText("Sveglia").assertIsDisplayed()
        composeRule.onNodeWithText("Volume sveglia").assertIsDisplayed()
        composeRule.onNodeWithText("Test rapidi").assertIsDisplayed()
        composeRule.onNodeWithText("Sveglia · 10 s").assertIsDisplayed()
        composeRule.onNodeWithText("Adhan · 10 s").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isEmpty())
        assertTextAbsent("Device")
        assertTextAbsent("CACHED")
        assertTextAbsent("FRESH")
    }

    @Test
    fun mainResolutionStatesRemainUnderstandable() {
        var state by mutableStateOf(LocationSettingsUiState())
        setScreen { state }

        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.Unconfigured,
                activeMode = LocationModeUi.Unconfigured,
            ),
            { state = it },
            "Posizione non configurata",
        )
        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.Resolving,
                activeMode = LocationModeUi.Device,
            ),
            { state = it },
            "Aggiornamento posizione",
        )
        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.Ready(
                    location = selectedDevice(),
                    freshness = LocationFreshness.FRESH,
                ),
                activeMode = LocationModeUi.Device,
            ),
            { state = it },
            "Ferrara, Italia",
            "Europe/Rome",
        )
    }

    @Test
    fun failureActionsRemainAvailableWithoutTechnicalCacheLabels() {
        var state by mutableStateOf(LocationSettingsUiState())
        setScreen { state }

        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.PermissionDenied(
                    canRequestAgain = false,
                    cachedLocation = null,
                ),
                activeMode = LocationModeUi.Device,
            ),
            { state = it },
            "Permesso posizione non concesso",
            "Apri impostazioni app",
        )
        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.LocationServicesDisabled(null),
                activeMode = LocationModeUi.Device,
            ),
            { state = it },
            "Servizi di localizzazione disattivati",
            "Apri Posizione Android",
        )
        assertState(
            LocationSettingsUiState(
                resolutionState = LocationResolutionState.Unavailable(
                    reason = LocationFailure.TIMEOUT,
                    cachedLocation = null,
                ),
                activeMode = LocationModeUi.Device,
            ),
            { state = it },
            "Posizione non ricevuta",
        )
        assertTextAbsent("CACHED")
    }

    @Test
    fun currentLocationCallbackIsGatedBehindRationaleConfirmation() {
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

        composeRule.onNodeWithTag("settings-use-current-location").performClick()
        composeRule.onNodeWithText("Posizione per Arihna").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, permissionLaunchRequests) }
        composeRule.onNodeWithText("Continua").performClick()
        composeRule.runOnIdle { assertEquals(1, permissionLaunchRequests) }
    }

    @Test
    fun manualSuggestionsStayBoundedSurfaceAndSelectionWorks() {
        val results = (1L..8L).map { id ->
            CitySearchResult(
                id = id,
                name = "Mirandola $id",
                regionName = "Emilia-Romagna",
                countryName = "Italy",
                countryCode = "IT",
                coordinates = Coordinates(44.88 + id / 1000.0, 11.06),
                timeZoneId = "Europe/Rome",
                timeZoneSupported = true,
            )
        }
        val state = LocationSettingsUiState(
            searchQuery = "Mira",
            searchResults = results,
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

        composeRule.onNodeWithTag("settings-location-suggestions").assertIsDisplayed()
        composeRule.onNodeWithText("Mirandola 1, Emilia-Romagna, Italy").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1L, selectedCityId) }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
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
        expectedTexts.forEach { text -> composeRule.onNodeWithText(text).assertIsDisplayed() }
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
}
