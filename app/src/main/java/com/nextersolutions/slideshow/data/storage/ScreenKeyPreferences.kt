package com.nextersolutions.slideshow.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "slideshow_prefs")

/**
 * Small wrapper around DataStore for persisting the screen key the user enters.
 * When nothing has been stored yet, [DEFAULT_SCREEN_KEY] is returned which
 * satisfies "set it as the default on first launch".
 */
@Singleton
class ScreenKeyPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun observeScreenKey(): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_SCREEN] ?: DEFAULT_SCREEN_KEY
        }

    suspend fun getScreenKey(): String = observeScreenKey().first()

    suspend fun setScreenKey(screenKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREEN] = screenKey
        }
    }

    companion object {
        private val KEY_SCREEN = stringPreferencesKey("screen_key")
        const val DEFAULT_SCREEN_KEY = "7d47b6d7-8294-4b33-8887-066961d79993"
    }
}
