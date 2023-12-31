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

import android.net.Uri
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import java.lang.ref.WeakReference
import java.util.Stack

data class AudioProgressHandler(
    var isCancelled: Boolean = false,
    var uriList: List<Uri>?,
    var playingIndex: Int,
    var audioPlaybackInfo: AudioPlaybackInfo,
    var doShuffle: Boolean = PreferencesConstants.DEFAULT_AUDIO_PLAYER_SHUFFLE,
    var repeatMode: Int = PreferencesConstants.DEFAULT_AUDIO_PLAYER_REPEAT_MODE
) {
    companion object {
        const val INDEX_UNDEFINED = -3
        const val INDEX_NOT_APPLICABLE = -2
        const val INDEX_NOT_FOUND = -1
    }

    val playingIndexStack: Stack<Int> = Stack()

    fun getPlayingIndex(recalculate: Boolean): Int {
        if (playingIndex < 0 || recalculate) {
            playingIndex = calculatePlayingIndex()
        }
        playingIndexStack.push(playingIndex)
        return playingIndex
    }

    fun getNextPlayingIndex(): Int {
        if (playingIndex < 0) {
            playingIndex = calculatePlayingIndex()
        }
        if (playingIndex < 0) {
            // recalculating didn't help, return
            return playingIndex
        }
        if (doShuffle) {
            playingIndex = Utils.generateRandom(0, uriList!!.size - 1)
        } else {
            // force repeat next on custom next press
            if (!uriList.isNullOrEmpty()) {
                if (playingIndex >= uriList!!.size - 1) {
                    playingIndex = 0
                } else {
                    playingIndex++
                }
            }
        }
        return playingIndex
    }

    fun getPreviousPlayingIndex(): Int {
        if (playingIndexStack.size > 1) {
            // pop current extra
            playingIndexStack.pop()
            playingIndex = playingIndexStack.pop()
        } else if (playingIndex > 0) {
            if (playingIndexStack.size == 1) {
                playingIndexStack.pop()
            }
            playingIndex--
        }
        return playingIndex
    }

    fun calculatePlayingIndex(): Int {
        if (uriList != null) {
            var index = 0
            for (uri in uriList!!) {
                if (audioPlaybackInfo.audioModel
                    .getUri() == uri
                ) {
                    return index
                } else {
                    index++
                }
            }
            return INDEX_NOT_FOUND
        } else {
            return INDEX_NOT_APPLICABLE
        }
    }
}

/**
 * Callback interface shared between service and fragment (different objects for each)
 */
interface OnPlaybackInfoUpdate {

    /**
     * Called every x seconds by {@see AudioPlayerRepeatableRunnable}
     */
    fun onPositionUpdate(progressHandler: AudioProgressHandler)

    /**
     * Called as soon as the state changes, either through notification or through button
     * @param renderWaveform should render waveform, in case we press next / prev buttons
     * as waveform seekbar requires loading the waves from sample file manually
     */
    fun onPlaybackStateChanged(progressHandler: AudioProgressHandler, renderWaveform: Boolean)

    /**
     * Called once by service connection to initialize button clicks and other views
     */
    fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>)

    fun serviceDisconnected()

    /**
     * Is the UI fragment visible and should we listen to updates
     */
    fun shouldListenToUpdates(): Boolean
}
