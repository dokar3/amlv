package com.dokar.amlv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.dokar.amlv.parser.LrcLyricsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

@Composable
fun rememberLyricsViewState(lrcContent: String): LyricsViewState {
    val scope = rememberCoroutineScope()
    val parser = remember { LrcLyricsParser() }
    return remember(scope, parser, lrcContent) {
        val lyrics = parser.parse(lrcContent)
        LyricsViewState(lyrics, scope)
    }
}

@Composable
fun rememberLyricsViewState(lyrics: Lyrics?): LyricsViewState {
    val scope = rememberCoroutineScope()
    return remember(scope, lyrics) { LyricsViewState(lyrics, scope) }
}

@Stable
class LyricsViewState(
    lyrics: Lyrics?,
    private val scope: CoroutineScope,
    private val tickMillis: Long = 50L,
) {
    val lyrics: Lyrics?

    private val lineCount = lyrics?.lines?.size ?: 0

    var position by mutableStateOf(0L)
        private set

    internal var currentLineIndex by mutableStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    private var playbackJob: Job? = null

    init {
        require(tickMillis > 0) { "tickMillis must > 0" }
        this.lyrics = lyrics?.run {
            // Make sure all lines are sorted by the start time
            copy(lines = lines.sortedBy { it.startAt })
        }
    }

    fun play() {
        if (lyrics == null) return

        val lines = lyrics.lines
        if (lines.isEmpty()) return

        if (position !in 0..lyrics.optimalDurationMillis) {
            return
        }

        playbackJob?.cancel()
        playbackJob = scope.launch {
            var currLineIdx = findLineIndexAt(position)
            currentLineIndex = currLineIdx

            isPlaying = true

            fun checkFinished(): Boolean {
                return !isActive || !isPlaying || position >= lyrics.optimalDurationMillis
            }

            /**
             * @return deviation millis
             */
            suspend fun startTicking(duration: Long): Long {
                if (duration <= 0) return 0L

                val loops = duration / tickMillis
                val extraDelay = duration % tickMillis
                var i = 0L
                var millis = measureTimeMillis {
                    while (i < loops && !checkFinished()) {
                        delay(tickMillis)
                        position += tickMillis
                        i++
                    }
                }
                val deviation = i * tickMillis - millis

                if (checkFinished()) {
                    return deviation
                }

                millis = measureTimeMillis {
                    delay(extraDelay)
                    position += extraDelay
                }
                val extraDeviation = extraDelay - millis

                return extraDeviation + deviation
            }

            var deviationMillis = 0L

            while (currLineIdx < lineCount && !checkFinished()) {
                currentLineIndex = currLineIdx

                val duration = if (currLineIdx < 0) {
                    lines.first().startAt - position
                } else {
                    lines[currLineIdx].durationMillis
                }
                deviationMillis = startTicking(duration + deviationMillis)

                if (checkFinished()) {
                    return@launch
                }

                if (currLineIdx in 0 until lineCount - 1) {
                    val lineStopAt = lines[currLineIdx].let { it.startAt + it.durationMillis }
                    val nextLineStartAt = lines[currLineIdx + 1].startAt
                    val inactiveDuration = nextLineStartAt - lineStopAt
                    if (inactiveDuration > 0) {
                        currentLineIndex = -1
                        val actualDuration = inactiveDuration + deviationMillis
                        if (actualDuration > 0L) {
                            deviationMillis = startTicking(actualDuration)
                        } else {
                            deviationMillis -= inactiveDuration
                        }
                    }
                }

                if (checkFinished()) {
                    return@launch
                }

                currLineIdx++
            }
        }
        playbackJob!!.invokeOnCompletion { cause ->
            if (cause == null) {
                isPlaying = false
            }
        }
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
    }

    fun seekToLine(index: Int) {
        val lines = lyrics?.lines ?: return
        val idx = index.coerceIn(-1, lineCount - 1)
        val position = if (idx >= 0) lines[idx].startAt else 0L
        seekTo(position)
    }

    fun seekTo(position: Long) {
        val playAfterSeeking = isPlaying
        if (isPlaying) {
            playbackJob?.cancel()
        }
        this.position = position
        if (playAfterSeeking) {
            play()
        } else {
            currentLineIndex = findLineIndexAt(position)
        }
    }

    private fun findLineIndexAt(position: Long): Int {
        if (position < 0 || lyrics == null) return -1
        val lines = lyrics.lines
        for (i in lines.lastIndex downTo 0) {
            if (position >= lines[i].startAt) {
                return i
            }
        }
        return -1
    }
}