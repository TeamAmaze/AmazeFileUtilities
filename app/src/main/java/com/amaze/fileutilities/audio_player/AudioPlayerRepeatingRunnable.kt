/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import com.amaze.fileutilities.utilis.AbstractRepeatingRunnable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class AudioPlayerRepeatingRunnable(
    startImmediately: Boolean,
    private val serviceRef: WeakReference<OnPlayerRepeatingCallback>
) :
    AbstractRepeatingRunnable(
        1, 1, TimeUnit.SECONDS,
        startImmediately
    ) {

    private var lastPosition = 0L

    override fun run() {
        if (serviceRef.get() == null) {
            cancel()
            return
        }
        val callback = serviceRef.get()
        callback?.let {
            it.getAudioProgressHandlerCallback()?.let {
                audioProgressHandler ->
                if (audioProgressHandler.isCancelled) {
                    it.onProgressUpdate(audioProgressHandler)
                    cancel()
                    return
                }
                val audioPlaybackInfo = audioProgressHandler.audioPlaybackInfo
                audioPlaybackInfo.currentPosition = it.getPlayerPosition()
                audioPlaybackInfo.duration = it.getPlayerDuration()
                audioPlaybackInfo.isPlaying = it.isPlaying()
                if (lastPosition != audioPlaybackInfo.currentPosition) {
                    it.onProgressUpdate(audioProgressHandler)
                }
                lastPosition = it.getPlayerPosition()
            }
        }
    }
}

interface OnPlayerRepeatingCallback {
    fun getAudioProgressHandlerCallback(): AudioProgressHandler?
    fun onProgressUpdate(audioProgressHandler: AudioProgressHandler)
    fun getPlayerPosition(): Long
    fun getPlayerDuration(): Long
    fun isPlaying(): Boolean
}
