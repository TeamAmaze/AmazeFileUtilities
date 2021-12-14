package com.amaze.fileutilities.audio_player

import com.amaze.fileutilities.utilis.AbstractRepeatingRunnable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class AudioPlayerRepeatingRunnable(startImmediately: Boolean, val serviceRef: WeakReference<OnPlayerRepeatingCallback>):
    AbstractRepeatingRunnable(1, 1, TimeUnit.SECONDS,
        startImmediately) {

    override fun run() {
        if (serviceRef.get() == null) {
            cancel(false)
            return
        }
        val callback = serviceRef.get()
        callback?.let {
            if (it.getAudioProgressHandlerCallback().isCancelled) {
                it.onProgressUpdate(it.getAudioProgressHandlerCallback())
                cancel(false)
                return
            }
            val audioPlaybackInfo = it.getAudioProgressHandlerCallback().audioPlaybackInfo
            audioPlaybackInfo.currentPosition = it.getPlayerPosition()
            audioPlaybackInfo.duration = it.getPlayerDuration()
            audioPlaybackInfo.playbackState = it.getPlaybackState()
            it.onProgressUpdate(it.getAudioProgressHandlerCallback())
        }
    }
}

interface OnPlayerRepeatingCallback {
    fun getAudioProgressHandlerCallback(): AudioProgressHandler
    fun onProgressUpdate(audioProgressHandler: AudioProgressHandler)
    fun getPlayerPosition(): Int
    fun getPlayerDuration(): Int
    fun getPlaybackState(): Int
}