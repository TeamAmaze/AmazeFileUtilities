/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.net.Uri
import java.io.File
import java.lang.ref.WeakReference

data class AudioProgressHandler(
    var isCancelled: Boolean = false,
    var uriList: List<Uri>?,
    var playingIndex: Int,
    var audioPlaybackInfo: AudioPlaybackInfo
) {
    companion object {
        const val INDEX_UNDEFINED = -3
        const val INDEX_NOT_APPLICABLE = -2
        const val INDEX_NOT_FOUND = -1
    }

    fun getPlayingIndex(recalculate: Boolean): Int {
        if (playingIndex == INDEX_UNDEFINED || recalculate) {
            playingIndex = calculatePlayingIndex()
        }
        return playingIndex
    }

    private fun calculatePlayingIndex(): Int {
        if (uriList != null) {
            var index = 0
            for (uri in uriList!!) {
                if (File(uri.path).name.equals(
                        File(
                                audioPlaybackInfo.audioModel
                                    .getUri().path
                            ).name
                    )
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

interface OnPlaybackInfoUpdate {

    /**
     * Called every x seconds by {@see AudioPlayerRepeatableRunnable}
     */
    fun onPositionUpdate(progressHandler: AudioProgressHandler)

    /**
     * Called as soon as the state changes, either through notification or through button
     */
    fun onPlaybackStateChanged(progressHandler: AudioProgressHandler)

    /**
     * Called once by service connection to initialize button clicks and other views
     */
    fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>)

    fun serviceDisconnected()
}
