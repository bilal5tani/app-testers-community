package com.apptesters.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val languageKey = stringPreferencesKey("language")

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeKey] ?: "system"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[languageKey] ?: "en"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = theme
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[languageKey] = language
        }
    }
}
