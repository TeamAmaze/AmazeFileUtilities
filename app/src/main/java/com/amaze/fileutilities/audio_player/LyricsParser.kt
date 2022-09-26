/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import com.amaze.fileutilities.home_page.database.Lyrics
import com.amaze.fileutilities.home_page.database.LyricsDao
import com.amaze.fileutilities.home_page.ui.files.AudiosListFragment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Matcher
import java.util.regex.Pattern

class LyricsParser {

    var lyricsMap: HashMap<Long, String>? = null
    var lyricsRaw: Lyrics? = null
    var lastLyrics: String? = null
    var nextLyrics: String? = null

    data class LyricsStrings(
        var lastLyrics: String,
        var currentLyrics: String,
        var nextLyrics: String
    )

    constructor(filePath: String, lyricsDao: LyricsDao) {
        this.lyricsRaw = lyricsDao.findByPath(filePath)
        parseAndStore()
    }

    constructor(
        filePath: String,
        lyricsRawText: String,
        isSynced: Boolean,
        lyricsDao: LyricsDao
    ) {
        lyricsDao.insert(Lyrics(filePath, lyricsRawText, isSynced))
        this.lyricsRaw = lyricsDao.findByPath(filePath)
        parseAndStore()
    }

    companion object {
        private var log: Logger = LoggerFactory.getLogger(AudiosListFragment::class.java)
        var timerPattern: Pattern = Pattern.compile("(\\[)([\\.:\\w]*)?(\\])")
    }

    fun getLyrics(timestamp: Long): String? {
        val currentLyric = if (lyricsMap != null) {
            val mappedLyrics = lyricsMap!![timestamp]
            if (mappedLyrics == null && lastLyrics == null) {
                "♪"
            } else {
                mappedLyrics
            }
        } else if (lyricsRaw != null) {
            lyricsRaw!!.lyricsText
        } else {
            null
        }
        return if (currentLyric == null && lastLyrics != null) {
            lastLyrics
        } else {
            lastLyrics = currentLyric
            currentLyric
        }
    }

    fun clearLyrics(lyricsDao: LyricsDao) {
        lyricsRaw?.let {
            lyricsDao.delete(it)
        }
        if (lyricsMap != null) {
            lyricsMap = null
        }
        lyricsRaw = null
        lastLyrics = null
    }

    private fun parseAndStore() {
        if (lyricsRaw != null && lyricsRaw!!.isSynced) {
            lyricsMap = HashMap()
            val lyricsLines = lyricsRaw!!.lyricsText.split("\n")
            var lastTime = 0
            lyricsLines.forEach {
                lyricsLineRaw ->
                var matcher: Matcher = timerPattern.matcher(lyricsLineRaw)
                val output: MutableList<Long> = ArrayList()
                var lyricsLine: String? = null
                try {
                    while (matcher.find()) {
                        val time: String? = matcher.group(2)
                        time?.let {
                            val min = time.substring(0, 2).toInt().toLong()
                            val sec = time.substring(3, 5).toInt().toLong()
                            val t = min * 60L + sec
                            output.add(t)
                        }
                    }
                    val lastIdxOf = lyricsLineRaw.lastIndexOf("]") + 1
                    if (lyricsLineRaw.length > lastIdxOf) {
                        lyricsLine = lyricsLineRaw.substring(lastIdxOf)
                    }
                    if (!lyricsLine.isNullOrBlank()) {
                        output.forEach {
                            timeStamp ->
                            lyricsMap?.put(timeStamp, lyricsLine)
                        }
                    }
                } catch (e: Exception) {
                    log.warn("failed to extract lyrics from line {}", lyricsLineRaw)
                }
            }
        }
    }
}
