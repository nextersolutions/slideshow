package com.nextersolutions.slideshow.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextersolutions.slideshow.R

@Composable
internal fun PlaybackControls(
    onStop: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onStop) {
            Text(text = stringResource(R.string.action_stop))
        }
        Button(onClick = onSkip) {
            Text(text = stringResource(R.string.action_skip))
        }
    }
}