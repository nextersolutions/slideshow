package com.nextersolutions.slideshow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextersolutions.slideshow.core.common.EMPTY
import com.nextersolutions.slideshow.core.common.TIMEOUT
import com.nextersolutions.slideshow.core.common.VALUE_1
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import com.nextersolutions.slideshow.domain.usecase.ObservePlaylistUseCase
import com.nextersolutions.slideshow.domain.usecase.ObserveScreenKeyUseCase
import com.nextersolutions.slideshow.domain.usecase.SetScreenKeyUseCase
import com.nextersolutions.slideshow.domain.usecase.SyncPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SlideshowUiState(
    val screenKey: String = EMPTY,
    val isPlaying: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
internal class SlideshowViewModel @Inject constructor(
    observePlaylist: ObservePlaylistUseCase,
    observeScreenKey: ObserveScreenKeyUseCase,
    private val setScreenKey: SetScreenKeyUseCase,
    private val syncPlaylist: SyncPlaylistUseCase,
) : ViewModel() {

    val playlist: StateFlow<List<PlaylistItemViewData>> = observePlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT),
            initialValue = emptyList(),
        )

    private val _uiState = MutableStateFlow(SlideshowUiState())
    val uiState: StateFlow<SlideshowUiState> = _uiState.asStateFlow()

    private val _skipEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = VALUE_1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val skipEvents = _skipEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            observeScreenKey().collect { key ->
                _uiState.value = _uiState.value.copy(screenKey = key)
            }
        }
        syncPlaylist(immediate = true)
    }

    fun onScreenKeyEntered(screenKey: String) {
        viewModelScope.launch {
            runCatching { setScreenKey(screenKey) }
                .onFailure { t ->
                    _uiState.value = _uiState.value.copy(error = t.message ?: "Invalid key")
                }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(error = null)
                }
        }
    }

    fun onStart() {
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun onStop() {
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun onSkip() {
        if (_uiState.value.isPlaying) {
            _skipEvents.tryEmit(Unit)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
