package com.dokar.amlv

import androidx.compose.runtime.Immutable

@Immutable
data class Lyrics(
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMillis: Long?,
    val lines: List<Line>,
) {
    val optimalDurationMillis = optimalDurationMillis()

    init {
        for (line in lines) {
            require(line.startAt >= 0) { "startAt in the LyricsLine must >= 0" }
            require(line.durationMillis >= 0) { "durationMillis in the LyricsLine >= 0" }
        }
    }

    private fun optimalDurationMillis(): Long {
        if (durationMillis != null) {
            return durationMillis
        }
        return lines.maxOfOrNull { it.startAt + it.durationMillis } ?: 0L
    }

    @Immutable
    data class Line(
        val content: String,
        val startAt: Long,
        val durationMillis: Long,
    )
}