/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.audio_player

import android.os.Parcelable
import com.amaze.fileutilities.home_page.database.Lyrics
import com.amaze.fileutilities.home_page.database.LyricsDao
import com.amaze.fileutilities.home_page.ui.files.AudiosListFragment
import kotlinx.parcelize.Parcelize
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Matcher
import java.util.regex.Pattern

class LyricsParser {

    var lyricsMap: LinkedHashMap<Long, String>? = null
    var lyricsRaw: Lyrics? = null
    var lastLyrics: String? = null
    var lyricsStrings: LyricsStrings? = null

    @Parcelize
    data class LyricsStrings(
        var lastLyrics: String?,
        var currentLyrics: String,
        var nextLyrics: String?,
        var isSynced: Boolean
    ) : Parcelable

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

    fun getLyricsNew(timestamp: Long): LyricsStrings? {
        var lyricsObj: LyricsStrings?
        if (lyricsMap != null) {
            var found = false
            var lastFound: String? = "♪"
            var currFound = "♪"
            var nextFound: String? = "♪"
            for ((timeStampMapped, lyricsMapped) in lyricsMap!!) {
                if (timestamp == timeStampMapped && !found) {
                    found = true
                    currFound = lyricsMapped
                } else if (!found) {
                    lastFound = lyricsMapped
                } else if (found) {
                    nextFound = lyricsMapped
                    break
                }
            }
            if (!found) {
                // if not found, check previous values
                if (lyricsStrings != null) {
                    lastFound = lyricsStrings!!.lastLyrics
                    nextFound = lyricsStrings!!.nextLyrics
                    currFound = lyricsStrings!!.currentLyrics
                } else {
                    // no previous values so set default values
                    lastFound = null
                    nextFound = null
                }
            }
            lyricsObj = LyricsStrings(
                lastFound, currFound, nextFound,
                lyricsRaw?.isSynced == true
            )
        } else if (lyricsRaw != null) {
            lyricsObj = if (lyricsStrings == null) {
                LyricsStrings(
                    null, lyricsRaw?.lyricsText ?: "♪", null,
                    lyricsRaw?.isSynced == true
                )
            } else {
                lyricsStrings
            }
        } else {
            lyricsObj = null
        }
        lyricsStrings = lyricsObj
        return lyricsStrings
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
            lyricsMap = LinkedHashMap()
            val lyricsLines = lyricsRaw!!.lyricsText.split("\n")
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
                            if (!lyricsLine.isNullOrBlank()) {
                                lyricsMap?.put(timeStamp, lyricsLine)
                            } else {
                                lyricsMap?.put(timeStamp, "♪")
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.warn("failed to extract lyrics from line {}", lyricsLineRaw)
                }
            }
        }
    }
}
