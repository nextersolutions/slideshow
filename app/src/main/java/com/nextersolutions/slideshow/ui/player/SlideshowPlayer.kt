package com.nextersolutions.slideshow.ui.player

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nextersolutions.slideshow.core.common.DELAY_50
import com.nextersolutions.slideshow.core.common.SECOND
import com.nextersolutions.slideshow.core.common.VALUE_1
import com.nextersolutions.slideshow.core.common.ZERO
import com.nextersolutions.slideshow.domain.model.MediaType
import com.nextersolutions.slideshow.domain.model.PlaylistItemViewData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val CROSSFADE_MS = 500L

@OptIn(UnstableApi::class)
@Composable
fun SlideshowPlayer(
    playlist: List<PlaylistItemViewData>,
    skipEvents: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    if (playlist.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    var frontSlotIsA by remember { mutableStateOf(true) }
    var slotAItem by remember { mutableStateOf(playlist.getOrNull(ZERO)) }
    var slotBItem by remember { mutableStateOf<PlaylistItemViewData?>(null) }
    val alphaA = remember { Animatable(VALUE_1.toFloat()) }
    val alphaB = remember { Animatable(ZERO.toFloat()) }

    var slotAReady by remember { mutableStateOf(true) }
    var slotBReady by remember { mutableStateOf(false) }

    var currentIndex by remember { mutableIntStateOf(ZERO) }
    LaunchedEffect(playlist) {
        if (currentIndex >= playlist.size) currentIndex = ZERO
        if (slotAItem == null && slotBItem == null) {
            slotAItem = playlist.getOrNull(currentIndex)
            alphaA.snapTo(VALUE_1.toFloat())
            frontSlotIsA = true
        }
    }

    var advanceToken by remember { mutableIntStateOf(ZERO) }
    LaunchedEffect(skipEvents) {
        skipEvents.collect { advanceToken++ }
    }

    LaunchedEffect(advanceToken) {
        if (advanceToken == ZERO) return@LaunchedEffect
        if (playlist.isEmpty()) return@LaunchedEffect

        val nextIndex = (currentIndex + VALUE_1) % playlist.size
        val nextItem = playlist.getOrNull(nextIndex) ?: return@LaunchedEffect

        val f0 = ZERO.toFloat()
        val f1 = VALUE_1.toFloat()

        if (frontSlotIsA) {
            slotBReady = nextItem.mediaType == MediaType.IMAGE
            slotBItem = nextItem
            alphaB.snapTo(f0)

            while (!slotBReady) { delay(DELAY_50) }
            val out = launch {
                alphaA.animateTo(f0, tween(CROSSFADE_MS.toInt()))
            }
            val inn = launch {
                alphaB.animateTo(f1, tween(CROSSFADE_MS.toInt()))
            }

            out.join(); inn.join()
            slotAItem = null
            frontSlotIsA = false
        } else {
            slotAReady = nextItem.mediaType == MediaType.IMAGE
            slotAItem = nextItem
            alphaA.snapTo(f0)

            while (!slotAReady) { delay(DELAY_50) }

            val out = launch {
                alphaB.animateTo(f0, tween(CROSSFADE_MS.toInt()))
            }
            val inn = launch {
                alphaA.animateTo(f1, tween(CROSSFADE_MS.toInt()))
            }
            out.join(); inn.join()
            slotBItem = null
            frontSlotIsA = true
        }
        currentIndex = nextIndex
    }

    val frontItem = if (frontSlotIsA) slotAItem else slotBItem
    LaunchedEffect(frontItem?.id) {
        val item = frontItem ?: return@LaunchedEffect
        if (item.mediaType == MediaType.IMAGE) {
            val totalMs = item.durationSec.coerceAtLeast(VALUE_1) * SECOND
            val holdMs = (totalMs - CROSSFADE_MS).coerceAtLeast(CROSSFADE_MS)
            delay(holdMs)
            advanceToken++
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        slotAItem?.let { item ->
            key(item.id) {
                MediaLayer(
                    item = item,
                    modifier = Modifier.fillMaxSize().alpha(alphaA.value),
                    isDriving = frontSlotIsA,
                    onVideoNearEnd = { advanceToken++ },
                    onFirstFrameReady = { slotAReady = true },
                )
            }
        }
        slotBItem?.let { item ->
            key(item.id) {
                MediaLayer(
                    item = item,
                    modifier = Modifier.fillMaxSize().alpha(alphaB.value),
                    isDriving = !frontSlotIsA,
                    onVideoNearEnd = { advanceToken++ },
                    onFirstFrameReady = { slotBReady = true },
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MediaLayer(
    item: PlaylistItemViewData,
    modifier: Modifier,
    isDriving: Boolean,
    onVideoNearEnd: () -> Unit,
    onFirstFrameReady: () -> Unit,
) {
    when (item.mediaType) {
        MediaType.IMAGE -> {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(item.localPath))
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier,
            )
            LaunchedEffect(item.id) { onFirstFrameReady() }
        }
        MediaType.VIDEO -> VideoLayer(
            item = item,
            modifier = modifier,
            isDriving = isDriving,
            onNearEnd = onVideoNearEnd,
            onFirstFrameReady = onFirstFrameReady,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoLayer(
    item: PlaylistItemViewData,
    modifier: Modifier,
    isDriving: Boolean,
    onNearEnd: () -> Unit,
    onFirstFrameReady: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val thumbnail = remember(item.localPath) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(item.localPath) {
        thumbnail.value = withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.media.MediaMetadataRetriever().use { mmr ->
                    mmr.setDataSource(item.localPath)
                    mmr.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
            } catch (_: Exception) { null }
        }
    }

    // Whether ExoPlayer has rendered its first real frame — once true we hide the thumbnail.
    var playerReady by remember { mutableStateOf(false) }
    val playerAlpha = remember { Animatable(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("file://${item.localPath}"))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (!playerReady) {
                    playerReady = true
                    coroutineScope.launch {
                        playerAlpha.animateTo(1f, tween(150))
                        onFirstFrameReady()
                    }
                }
            }
        }
        player.addListener(listener)
    }

    LaunchedEffect(player, isDriving) {
        if (!isDriving) return@LaunchedEffect
        var fired = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (!fired && state == Player.STATE_ENDED) {
                    fired = true
                    onNearEnd()
                }
            }
        }
        player.addListener(listener)
        try {
            while (!fired) {
                val duration = player.duration
                val position = player.currentPosition
                if (duration > ZERO && duration - position <= CROSSFADE_MS) {
                    fired = true
                    onNearEnd()
                    break
                }
                delay(DELAY_50)
            }
        } finally {
            player.removeListener(listener)
        }
    }

    Box(modifier = modifier) {
        thumbnail.value?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f - playerAlpha.value),
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize().alpha(playerAlpha.value),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { view -> view.player = player },
        )
    }
}
