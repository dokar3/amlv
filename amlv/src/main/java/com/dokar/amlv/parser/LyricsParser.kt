package com.dokar.amlv.parser

import com.dokar.amlv.Lyrics

interface LyricsParser {
    fun parse(input: String): Lyrics?
}
