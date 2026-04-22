package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.data.storage.ScreenKeyPreferences
import javax.inject.Inject

class SetScreenKeyUseCase @Inject constructor(
    private val prefs: ScreenKeyPreferences,
    private val syncPlaylist: SyncPlaylistUseCase,
) {
    suspend operator fun invoke(screenKey: String) {
        val trimmed = screenKey.trim()
        require(trimmed.isNotEmpty()) { "Screen key must not be empty" }
        prefs.setScreenKey(trimmed)
        // Changing the screen means the current cached playlist is stale —
        // fetch again right now.
        syncPlaylist(immediate = true)
    }
}
