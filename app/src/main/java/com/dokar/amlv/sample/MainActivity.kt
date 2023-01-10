package com.dokar.amlv.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dokar.amlv.FadingEdges
import com.dokar.amlv.Lyrics
import com.dokar.amlv.LyricsView
import com.dokar.amlv.LyricsViewState
import com.dokar.amlv.rememberLyricsViewState
import com.dokar.amlv.sample.theme.AMLVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = 0x00000000
        window.navigationBarColor = 0x00000000
        setContent {
            AMLVTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LyricsViewSample()
                }
            }
        }
    }
}

@Composable
fun LyricsViewSample(modifier: Modifier = Modifier) {
    val state = rememberLyricsViewState(lrcContent = LRC_HELP)

    Column(
        modifier = modifier
            .fillMaxSize()
            .animatedGradient(animating = state.isPlaying)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        TitleBar(
            lyrics = state.lyrics,
            contentColor = Color.White,
        )

        LyricsView(
            state = state,
            modifier = Modifier.weight(weight = 1f, fill = false),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 150.dp,
            ),
            darkTheme = true,
            fadingEdges = FadingEdges(top = 16.dp, bottom = 150.dp),
        )

        PlaybackControls(
            state = state,
            modifier = Modifier,
            contentColor = Color.White,
        )
    }
}

@Composable
fun TitleBar(
    lyrics: Lyrics?,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier.padding(
            horizontal = 32.dp,
            vertical = 8.dp,
        ),
    ) {
        Text(
            text = lyrics?.title ?: "",
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        val artist = lyrics?.artist
        if (!artist.isNullOrEmpty()) {
            Text(
                text = artist,
                modifier = Modifier.alpha(0.7f),
                color = contentColor,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
fun PlaybackControls(
    state: LyricsViewState,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        val duration = state.lyrics?.optimalDurationMillis
        if (duration != null && duration > 0L) {
            var progress by remember { mutableStateOf(0f) }

            var positionText by remember { mutableStateOf("0:00") }

            fun millisToText(millis: Long): String {
                val minutes = millis / 1000 / 60
                val seconds = (millis - minutes * 1000 * 60) / 1000
                return "$minutes:${String.format("%02d", seconds)}"
            }

            LaunchedEffect(state, duration) {
                launch(Dispatchers.Default) {
                    snapshotFlow { state.position }
                        .distinctUntilChanged()
                        .collect {
                            progress = ((it.toFloat() / duration) * 100).toInt() / 100f
                            positionText = millisToText(it)
                        }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Text(
                    text = positionText,
                    color = contentColor,
                    fontSize = 14.sp,
                )

                val durationText = remember(duration) { millisToText(duration) }
                Text(
                    text = durationText,
                    color = contentColor,
                    fontSize = 14.sp,
                )
            }

            Slider(
                value = progress,
                onValueChange = { state.seekTo((duration * it).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.8f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.5f),
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            val playIcon = if (state.isPlaying) {
                R.drawable.outline_pause_24
            } else {
                R.drawable.outline_play_arrow_24
            }
            Icon(
                painter = painterResource(playIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = {
                            if (state.isPlaying) {
                                state.pause()
                            } else {
                                state.play()
                            }
                        },
                    ),
                tint = contentColor,
            )
        }
    }
}

// Based on: https://gist.github.com/KlassenKonstantin/d5f6ed1d74b3ddbdca699d66c6b9a3b2
fun Modifier.animatedGradient(animating: Boolean): Modifier = composed {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(rotation, animating) {
        if (!animating) return@LaunchedEffect
        val target = rotation.value + 360f
        rotation.animateTo(
            targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 30_000,
                    easing = LinearEasing,
                ),
            ),
        )
    }

    drawWithCache {
        val rectSize = sqrt(size.width * size.width + size.height * size.height)
        val topLeft = Offset(
            x = -(rectSize - size.width) / 2,
            y = -(rectSize - size.height) / 2,
        )

        val brush1 = Brush.linearGradient(
            0f to Color.Magenta,
            1f to Color.Cyan,
            start = topLeft,
            end = Offset(rectSize * 0.7f, rectSize * 0.7f),
        )

        val brush2 = Brush.linearGradient(
            0f to Color(0xFF9E03FF),
            1f to Color(0xFF11D36E),
            start = Offset(rectSize, 0f),
            end = Offset(0f, rectSize),
        )

        val maskBrush = Brush.linearGradient(
            0f to Color.White,
            1f to Color.Transparent,
            start = Offset(rectSize / 2f, 0f),
            end = Offset(rectSize / 2f, rectSize),
        )

        onDrawBehind {
            val value = rotation.value

            withTransform(transformBlock = { rotate(value) }) {
                drawRect(
                    brush = brush1,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                )
            }

            withTransform(transformBlock = { rotate(-value) }) {
                drawRect(
                    brush = maskBrush,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                    blendMode = BlendMode.DstOut,
                )
            }

            withTransform(transformBlock = { rotate(value) }) {
                drawRect(
                    brush = brush2,
                    topLeft = topLeft,
                    size = Size(rectSize, rectSize),
                    blendMode = BlendMode.DstAtop,
                )
            }
        }
    }
}

private const val LRC_HELP = """
[ar:The Beatles]
[ti:Help!]
[al:Help!]
[length:02:19.23]

[00:01.00]Help! I need somebody
[00:02.91]Help! Not just anybody
[00:05.16]Help! You know I need someone
[00:07.91]Help!
[00:10.66](When) When I was younger (When I was young) so much younger than today
[00:15.66](I never need) I never needed anybody's help in any way
[00:20.76](Now) But now these days are gone (These days are gone) and I'm not so self assured
[00:25.43](And now I find) Now I find I've changed my mind, I've opened up the doors
[00:30.42]Help me if you can, I'm feeling down
[00:34.94]And I do appreciate you being 'round
[00:40.74]Help me get my feet back on the ground
[00:44.94]Won't you please, please help me?
[00:50.94](Now) And now my life has changed (My life has changed) in oh so many ways
[00:56.20](My independence) My independence seems to vanish in the haze
[01:01.21](But) But ev'ry now (Every now and then) and then I feel so insecure
[01:05.92](I know that I) I know that I just need you like I've never done before
[01:11.00]Help me if you can, I'm feeling down
[01:15.43]And I do appreciate you being 'round
[01:20.47]Help me get my feet back on the ground
[01:25.64]Won't you please, please help me?
[01:31.43]When I was younger, so much younger than today
[01:36.65]I never needed anybody's help in any way
[01:41.39](Now) But now these days are gone (These days are gone) and I'm not so self assured
[01:46.17](And now I find) Now I find I've changed my mind, I've opened up the doors
[01:51.38]Help me if you can, I'm feeling down
[01:56.17]And I do appreciate you being 'round
[02:01.36]Help me get my feet back on the ground
[02:06.08]Won't you please, please help me?
[02:10.12]Help me, help me
[02:12.85]Ooh
"""