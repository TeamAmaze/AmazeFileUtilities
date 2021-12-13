package com.amaze.fileutilities.audio_player

import com.amaze.fileutilities.utilis.AbstractRepeatingRunnable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class AudioPlayerRepeatingRunnable(startImmediately: Boolean, val serviceRef: WeakReference<AudioPlayerService>):
    AbstractRepeatingRunnable(1, 1, java.util.concurrent.TimeUnit.SECONDS,
        startImmediately) {

    override fun run() {
        if (serviceRef.get() == null) {
            cancel(false)
            return
        }
        val service = serviceRef.get()
        service?.audioProgressHandler?.let {
            audioPlaybackHandler ->
            if (audioPlaybackHandler.isCancelled) {
                service.onPlaybackStateChanged(audioPlaybackHandler)
                cancel(false)
                return
            }
            val audioPlaybackInfo = audioPlaybackHandler.audioPlaybackInfo
            service.exoPlayer?.let {
                audioPlaybackInfo.currentPosition = it.currentPosition.toInt()
                audioPlaybackInfo.duration = it.duration.toInt()
            }
            service.onPositionUpdate(audioPlaybackHandler)
        }
    }
}