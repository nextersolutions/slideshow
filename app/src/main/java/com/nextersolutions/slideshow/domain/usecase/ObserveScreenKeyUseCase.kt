package com.nextersolutions.slideshow.domain.usecase

import com.nextersolutions.slideshow.data.storage.ScreenKeyPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveScreenKeyUseCase @Inject constructor(
    private val prefs: ScreenKeyPreferences,
) {
    operator fun invoke(): Flow<String> = prefs.observeScreenKey()
}
