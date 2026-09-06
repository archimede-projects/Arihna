from pathlib import Path
import re
import textwrap

SPEC_SHA = "fd8134a6e7539d3e69248b6470014d1ba6df6d8c"


def write(path: str, content: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(textwrap.dedent(content).lstrip())


# Navigation: one top-level policy used by both the bottom bar and Home quick actions.
write(
    "app/src/main/java/com/archimedeprojects/arihna/app/TopLevelNavigation.kt",
    r'''
    package com.archimedeprojects.arihna.app

    import androidx.navigation.NavGraph.Companion.findStartDestination
    import androidx.navigation.NavHostController

    internal fun NavHostController.navigateTopLevel(
        route: String,
        homeRoute: String,
    ) {
        if (route == homeRoute) {
            val poppedToHome = popBackStack(homeRoute, inclusive = false)
            if (!poppedToHome && currentDestination?.route != homeRoute) {
                navigate(homeRoute) {
                    popUpTo(graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            return
        }

        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    ''',
)

nav_path = Path("app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt")
nav = nav_path.read_text()
old_bottom = '''                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(Destination.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },'''
new_bottom = '''                        onClick = {
                            navController.navigateTopLevel(
                                route = destination.route,
                                homeRoute = Destination.Home.route,
                            )
                        },'''
if old_bottom not in nav:
    raise SystemExit("Bottom navigation block not found")
nav = nav.replace(old_bottom, new_bottom, 1)
for old_value, new_value in {
    'navController.navigate(Destination.Settings.route) { launchSingleTop = true }':
        'navController.navigateTopLevel(Destination.Settings.route, Destination.Home.route)',
    'navController.navigate(Destination.Qibla.route) { launchSingleTop = true }':
        'navController.navigateTopLevel(Destination.Qibla.route, Destination.Home.route)',
    'navController.navigate(Destination.Alarms.route) { launchSingleTop = true }':
        'navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)',
}.items():
    if old_value not in nav:
        raise SystemExit(f"Navigation callback not found: {old_value}")
    nav = nav.replace(old_value, new_value)
nav_path.write_text(nav)

# Curated, deterministic daily inspiration with a native Android share sheet.
write(
    "app/src/main/java/com/archimedeprojects/arihna/feature/home/DailyInspiration.kt",
    r'''
    package com.archimedeprojects.arihna.feature.home

    import android.content.Context
    import android.content.Intent
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.rounded.Share
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.Icon
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.platform.testTag
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import java.time.LocalDate

    internal data class DailyInspiration(
        val kind: String,
        val text: String,
        val reference: String,
    )

    internal val curatedDailyInspirations = listOf(
        DailyInspiration("CORANO", "Con la difficoltà viene il sollievo.", "Corano 94:5–6"),
        DailyInspiration("CORANO", "Nel ricordo di Allah i cuori trovano quiete.", "Corano 13:28"),
        DailyInspiration("CORANO", "Non disperate della misericordia di Allah.", "Corano 39:53"),
        DailyInspiration(
            "CORANO",
            "Allah non impone a nessuna anima un peso superiore alle sue capacità.",
            "Corano 2:286",
        ),
        DailyInspiration(
            "HADITH",
            "Le opere più amate da Allah sono quelle compiute con costanza, anche se piccole.",
            "Sahih al-Bukhari 6464",
        ),
        DailyInspiration("HADITH", "Una buona parola è carità.", "Sahih al-Bukhari 2989"),
    )

    internal fun dailyInspirationFor(date: LocalDate): DailyInspiration {
        val index = Math.floorMod(date.toEpochDay(), curatedDailyInspirations.size.toLong()).toInt()
        return curatedDailyInspirations[index]
    }

    internal fun DailyInspiration.shareText(): String =
        "“$text”\n$reference\n\nCondiviso da Arihna"

    @Composable
    internal fun DailyInspirationCard(localDate: LocalDate) {
        val inspiration = remember(localDate) { dailyInspirationFor(localDate) }
        var showDetail by remember(localDate) { mutableStateOf(false) }
        val context = LocalContext.current

        Card(
            onClick = { showDetail = true },
            modifier = Modifier.fillMaxWidth().testTag("home-inspiration"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11251D)),
            border = BorderStroke(1.dp, Color(0xFFD8B95A).copy(alpha = 0.38f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ISPIRAZIONE DEL GIORNO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD8B95A),
                    )
                    Text(
                        inspiration.kind,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA8B4AC),
                    )
                }
                Text(
                    "“${inspiration.text}”",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF7F2E7),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        inspiration.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA8B4AC),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = null,
                            tint = Color(0xFFD8B95A),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            "Tocca per leggere e condividere",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD8B95A),
                        )
                    }
                }
            }
        }

        if (showDetail) {
            AlertDialog(
                onDismissRequest = { showDetail = false },
                modifier = Modifier.testTag("home-inspiration-detail"),
                containerColor = Color(0xFF10241C),
                titleContentColor = Color(0xFFD8B95A),
                textContentColor = Color(0xFFF7F2E7),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Ispirazione del giorno", fontWeight = FontWeight.Bold)
                        Text(
                            inspiration.kind,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA8B4AC),
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "“${inspiration.text}”",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            inspiration.reference,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD8B95A),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { shareInspiration(context, inspiration) }) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Condividi", modifier = Modifier.padding(start = 6.dp))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDetail = false }) { Text("Chiudi") }
                },
            )
        }
    }

    private fun shareInspiration(context: Context, inspiration: DailyInspiration) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inspiration.shareText())
        }
        context.startActivity(Intent.createChooser(sendIntent, "Condividi ispirazione"))
    }
    ''',
)

# Low-contrast eight-point rosettes plus a pointed arch: decorative only, no semantics or sacred text.
write(
    "app/src/main/java/com/archimedeprojects/arihna/feature/home/IslamicBackdrop.kt",
    r'''
    package com.archimedeprojects.arihna.feature.home

    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.drawBehind
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.Path
    import androidx.compose.ui.graphics.drawscope.Stroke
    import androidx.compose.ui.unit.dp
    import kotlin.math.PI
    import kotlin.math.cos
    import kotlin.math.min
    import kotlin.math.sin

    private val BackdropGold = Color(0xFFD8B95A)
    private val BackdropGreen = Color(0xFF1D5A43)

    internal fun Modifier.islamicBackdrop(): Modifier = drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    BackdropGold.copy(alpha = 0.08f),
                    BackdropGreen.copy(alpha = 0.025f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.78f, size.height * 0.08f),
                radius = size.width * 0.9f,
            ),
        )

        val cell = 74.dp.toPx()
        val outerRadius = 19.dp.toPx()
        val innerRadius = outerRadius * 0.47f
        val lineWidth = 0.7.dp.toPx()
        var row = 0
        var centerY = cell * 0.45f
        while (centerY < size.height + cell) {
            var centerX = if (row % 2 == 0) cell * 0.35f else cell * 0.85f
            while (centerX < size.width + cell) {
                val star = Path()
                repeat(16) { index ->
                    val angle = -PI / 2.0 + index * PI / 8.0
                    val radius = if (index % 2 == 0) outerRadius else innerRadius
                    val x = centerX + cos(angle).toFloat() * radius
                    val y = centerY + sin(angle).toFloat() * radius
                    if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(
                    star,
                    BackdropGold.copy(alpha = 0.055f),
                    style = Stroke(width = lineWidth),
                )
                drawCircle(
                    BackdropGreen.copy(alpha = 0.06f),
                    radius = innerRadius * 0.42f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = lineWidth),
                )
                centerX += cell
            }
            centerY += cell * 0.82f
            row += 1
        }

        val archBottom = min(size.height * 0.43f, 320.dp.toPx())
        val archTop = 34.dp.toPx()
        val middle = size.width / 2f
        val left = size.width * 0.10f
        val right = size.width * 0.90f
        val shoulder = 76.dp.toPx()
        val arch = Path().apply {
            moveTo(left, archBottom)
            cubicTo(left, archBottom * 0.58f, middle - shoulder, archTop + shoulder, middle, archTop)
            cubicTo(middle + shoulder, archTop + shoulder, right, archBottom * 0.58f, right, archBottom)
        }
        drawPath(
            arch,
            BackdropGold.copy(alpha = 0.085f),
            style = Stroke(width = 1.05.dp.toPx()),
        )
    }
    ''',
)

home_path = Path("app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt")
home = home_path.read_text()
old_bg = '''            .background(Brush.verticalGradient(listOf(HomeBackgroundTop, HomeBackgroundBottom)))
            .padding(contentPadding)'''
new_bg = '''            .background(Brush.verticalGradient(listOf(HomeBackgroundTop, HomeBackgroundBottom)))
            .islamicBackdrop()
            .padding(contentPadding)'''
if old_bg not in home:
    raise SystemExit("Home background chain not found")
home = home.replace(old_bg, new_bg, 1)
if "    InspirationCard()" not in home:
    raise SystemExit("Legacy inspiration call not found")
home = home.replace("    InspirationCard()", "    DailyInspirationCard(state.localDate)", 1)
home, count = re.subn(
    r'\n@Composable\nprivate fun InspirationCard\(\) \{.*?\n\}\n\n@Composable\nprivate fun QuickActions',
    '\n@Composable\nprivate fun QuickActions',
    home,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit(f"Legacy inspiration removal count={count}")
home_path.write_text(home)

# Existing Home instrumentation now verifies the interactive detail affordance.
home_test_path = Path(
    "app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt"
)
home_test = home_test_path.read_text()
old_assertions = '''        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Corano 94:5–6"))
        composeRule.onNodeWithText("Con la difficoltà viene il sollievo.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Corano 94:5–6").assertIsDisplayed()
        composeRule.onNodeWithTag("home-quick-actions").assertIsDisplayed()'''
new_assertions = '''        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("ISPIRAZIONE DEL GIORNO"))
        composeRule.onNodeWithTag("home-inspiration").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("home-inspiration-detail").assertIsDisplayed()
        composeRule.onNodeWithText("Ispirazione del giorno").assertIsDisplayed()
        composeRule.onNodeWithText("Condividi").assertIsDisplayed()
        composeRule.onNodeWithText("Chiudi").performClick()
        composeRule.onNodeWithText("Tocca per leggere e condividere").assertIsDisplayed()
        composeRule.onNodeWithTag("home-quick-actions").assertIsDisplayed()'''
if old_assertions not in home_test:
    raise SystemExit("Existing inspiration assertions not found")
home_test_path.write_text(home_test.replace(old_assertions, new_assertions, 1))

# Focused navigation regression uses the same production helper without constructing app dependencies.
write(
    "app/src/androidTest/java/com/archimedeprojects/arihna/app/TopLevelNavigationAndroidTest.kt",
    r'''
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
    ''',
)

write(
    "app/src/test/java/com/archimedeprojects/arihna/feature/home/DailyInspirationTest.kt",
    r'''
    package com.archimedeprojects.arihna.feature.home

    import java.time.LocalDate
    import org.junit.Assert.assertEquals
    import org.junit.Assert.assertTrue
    import org.junit.Test

    class DailyInspirationTest {
        @Test
        fun selectionIsStableForSameCivilDateAndReferencesAreExplicit() {
            val date = LocalDate.of(2026, 9, 6)
            assertEquals(dailyInspirationFor(date), dailyInspirationFor(date))
            assertTrue(curatedDailyInspirations.all { it.reference.isNotBlank() })
            assertTrue(
                curatedDailyInspirations.all {
                    it.reference.startsWith("Corano ") || it.reference.startsWith("Sahih al-Bukhari ")
                },
            )
        }

        @Test
        fun shareTextContainsDisplayedTextReferenceAndArihnaAttribution() {
            val inspiration = dailyInspirationFor(LocalDate.of(2026, 9, 6))
            val shared = inspiration.shareText()
            assertTrue(shared.contains(inspiration.text))
            assertTrue(shared.contains(inspiration.reference))
            assertTrue(shared.contains("Arihna"))
        }
    }
    ''',
)
