package com.dokar.amlv.parser

import com.dokar.amlv.Lyrics

class LrcLyricsParser : LyricsParser {
    override fun parse(input: String): Lyrics? {
        if (input.isEmpty()) return null

        val title = LRC_TITLE_REGEX.find(input)?.groupValues?.get(1)?.trim() ?: ""
        val artist = LRC_ARTIST_REGEX.find(input)?.groupValues?.get(1)?.trim()
        val album = LRC_ALBUM_REGEX.find(input)?.groupValues?.get(1)?.trim()

        var length: Long? = null
        val lengthMatchResult = LRC_LENGTH_REGEX.find(input)
        if (lengthMatchResult != null) {
            length = parseTimeTag(lengthMatchResult.groupValues[1]).first()
        }

        val lines = mutableListOf<Lyrics.Line>()

        for (result in LRC_LINE_REGEX.findAll(input)) {
            val values = result.groupValues
            val content = values.last().trim()
            val times = parseTimeTag(values[1])
            for (time in times) {
                val line = Lyrics.Line(
                    content = content,
                    startAt = time,
                    durationMillis = 0,
                )
                lines.add(line)
            }
        }

        lines.sortBy { it.startAt }

        // Update durations
        for (i in 0 until lines.lastIndex) {
            lines[i] = lines[i].copy(
                durationMillis = lines[i + 1].startAt - lines[i].startAt,
            )
        }
        if (length != null) {
            val last = lines.last()
            lines[lines.lastIndex] = last.copy(
                durationMillis = (length - last.startAt).takeIf { it > 0L } ?: Long.MAX_VALUE,
            )
        } else {
            lines[lines.lastIndex] = lines.last().copy(
                durationMillis = Long.MAX_VALUE,
            )
        }

        return Lyrics(
            title = title,
            artist = artist,
            album = album,
            durationMillis = length,
            lines = lines.toList(),
        )
    }

    private fun parseTimeTag(timeTags: String): LongArray {
        val results = TIME_TAG_REGEX.findAll(timeTags).toList()
        val millisArray = LongArray(size = results.size)
        for ((index, result) in results.withIndex()) {
            val values = result.groupValues
            val minutes = values[1].toInt()
            val seconds = values[2].toInt()
            val millis = (values[4].toIntOrNull() ?: 0) * 10
            millisArray[index] = minutes * 60 * 1000L + seconds * 1000 + millis
        }
        return millisArray
    }

    companion object {
        private val LRC_TITLE_REGEX by lazy { Regex("\\[ti:(.*)]") }

        private val LRC_ARTIST_REGEX by lazy { Regex("\\[ar:(.*)]") }

        private val LRC_ALBUM_REGEX by lazy { Regex("\\[al:(.*)]") }

        private val TIME_TAG_REGEX by lazy { Regex("(\\d+):(\\d+)(\\.(\\d+))?") }

        private val LRC_LENGTH_REGEX by lazy { Regex("\\[length: ?(${TIME_TAG_REGEX.pattern})]") }

        private val LRC_LINE_REGEX by lazy { Regex("((\\[${TIME_TAG_REGEX.pattern}])+)(.*)") }
    }
}