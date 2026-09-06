package com.archimedeprojects.arihna.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class TopLevelNavigationAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeRemainsReachableAfterQiblaAlarmsAndSettingsQuickRoutes() {
        composeRule.setContent { NavigationHarness() }
        assertRoundTrip("quick-qibla", "screen-qibla")
        assertRoundTrip("quick-alarms", "screen-alarms")
        assertRoundTrip("quick-settings", "screen-settings")
    }

    private fun assertRoundTrip(quickTag: String, destinationTag: String) {
        composeRule.onNodeWithTag(quickTag).performClick()
        composeRule.onNodeWithTag(destinationTag).assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-home").performClick()
        composeRule.onNodeWithTag("screen-home").assertIsDisplayed()
    }
}

@Composable
private fun NavigationHarness() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            Button(
                onClick = { navController.navigateTopLevel("home", "home") },
                modifier = Modifier.testTag("bottom-home"),
            ) { Text("Home") }
        },
    ) { padding ->
        NavHost(navController, "home", Modifier.padding(padding)) {
            composable("home") {
                Column(modifier = Modifier.testTag("screen-home")) {
                    Button(
                        onClick = { navController.navigateTopLevel("qibla", "home") },
                        modifier = Modifier.testTag("quick-qibla"),
                    ) { Text("Qibla") }
                    Button(
                        onClick = { navController.navigateTopLevel("alarms", "home") },
                        modifier = Modifier.testTag("quick-alarms"),
                    ) { Text("Sveglie") }
                    Button(
                        onClick = { navController.navigateTopLevel("settings", "home") },
                        modifier = Modifier.testTag("quick-settings"),
                    ) { Text("Posizione") }
                }
            }
            composable("qibla") { Text("Qibla", modifier = Modifier.testTag("screen-qibla")) }
            composable("alarms") { Text("Sveglie", modifier = Modifier.testTag("screen-alarms")) }
            composable("settings") { Text("Impostazioni", modifier = Modifier.testTag("screen-settings")) }
        }
    }
}
