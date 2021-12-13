package com.amaze.fileutilities.audio_player

import android.net.Uri
import java.lang.ref.WeakReference

data class AudioProgressHandler(var isCancelled: Boolean = false, var uriList: List<Uri>?,
                                private var playingIndex: Int,
                                var audioPlaybackInfo: AudioPlaybackInfo) {
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
                if (uri == audioPlaybackInfo.audioModel.getUri()) {
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
    fun onPositionUpdate(progressHandler: AudioProgressHandler)
    fun onPlaybackStateChanged(progressHandler: AudioProgressHandler)
    fun setupActionButtons(audioService: WeakReference<AudioPlayerService>)
}