package com.archimedeprojects.arihna.core.i18n

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLanguageAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arabicSelectionPersistsAndMapsToRtl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("arihna_ui_preferences", 0)
        prefs.edit().clear().commit()
        try {
            val controller = AppLanguageController(context)
            assertEquals(AppLanguage.ITALIAN, controller.language)
            controller.updateLanguage(AppLanguage.ARABIC)
            assertEquals(AppLanguage.ARABIC, controller.language)
            val reloaded = AppLanguageController(context)
            assertEquals(AppLanguage.ARABIC, reloaded.language)
            composeRule.setContent {
                CompositionLocalProvider(
                    LocalAppLanguageController provides reloaded,
                    LocalLayoutDirection provides if (reloaded.language == AppLanguage.ARABIC) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    },
                ) {
                    Text(if (LocalLayoutDirection.current == LayoutDirection.Rtl) "RTL_OK" else "RTL_BAD")
                }
            }
            composeRule.onNodeWithText("RTL_OK").assertIsDisplayed()
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
