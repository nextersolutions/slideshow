package com.nextersolutions.slideshow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextersolutions.slideshow.R
import com.nextersolutions.slideshow.core.common.EMPTY
import com.nextersolutions.slideshow.ui.player.SlideshowPlayer
import com.nextersolutions.slideshow.ui.widgets.PlaybackControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    var keyText by remember { mutableStateOf(EMPTY) }
    LaunchedEffect(uiState.screenKey) {
        if (keyText.isEmpty()) keyText = uiState.screenKey
    }

    if (uiState.isPlaying) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            SlideshowPlayer(
                playlist = playlist,
                skipEvents = viewModel.skipEvents,
                modifier = Modifier.fillMaxSize(),
            )

            PlaybackControls(
                onStop = viewModel::onStop,
                onSkip = viewModel::onSkip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
            )

            if (playlist.isEmpty()) {
                Text(
                    text = stringResource(R.string.waiting_for_downloads),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.title_slideshow))
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = stringResource(R.string.label_screen_key),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error != null,
                supportingText = uiState.error?.let { err ->
                    @Composable { Text(err) }
                },
            )

            Spacer(Modifier.height(12.dp))
            Row {
                Button(
                    onClick = { viewModel.onScreenKeyEntered(keyText) },
                    enabled = keyText.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.action_save_key))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(
                    id = R.string.items_downloaded,
                    playlist.size.toString()
                ),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::onStart,
                enabled = playlist.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(text = stringResource(R.string.action_start_playback))
            }

            if (playlist.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.no_items),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
