package com.archimedeprojects.arihna.core.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val storageValue: String) {
    ITALIAN("it"),
    ARABIC("ar");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: ITALIAN
    }
}

@Stable
class AppLanguageController(context: Context? = null) {
    private val preferences = context?.getSharedPreferences("arihna_ui_preferences", Context.MODE_PRIVATE)

    var language by mutableStateOf(AppLanguage.fromStorage(preferences?.getString(KEY_LANGUAGE, null)))
        private set

    fun updateLanguage(value: AppLanguage) {
        if (value == language) return
        preferences?.edit()?.putString(KEY_LANGUAGE, value.storageValue)?.apply()
        language = value
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
    }
}

val LocalAppLanguageController = staticCompositionLocalOf { AppLanguageController() }

@Composable
fun appText(italian: String, arabic: String): String =
    if (LocalAppLanguageController.current.language == AppLanguage.ARABIC) arabic else italian

@Composable
fun isArabicLanguage(): Boolean =
    LocalAppLanguageController.current.language == AppLanguage.ARABIC
