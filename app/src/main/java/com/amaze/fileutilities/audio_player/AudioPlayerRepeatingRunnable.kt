package com.amaze.fileutilities.audio_player

import com.amaze.fileutilities.utilis.AbstractRepeatingRunnable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class AudioPlayerRepeatingRunnable(startImmediately: Boolean, private val serviceRef: WeakReference<OnPlayerRepeatingCallback>):
    AbstractRepeatingRunnable(1, 1, TimeUnit.SECONDS,
        startImmediately) {

    override fun run() {
        if (serviceRef.get() == null) {
            cancel()
            return
        }
        val callback = serviceRef.get()
        callback?.let {
            if (it.getAudioProgressHandlerCallback().isCancelled) {
                it.onProgressUpdate(it.getAudioProgressHandlerCallback())
                cancel()
                return
            }
            val audioPlaybackInfo = it.getAudioProgressHandlerCallback().audioPlaybackInfo
            audioPlaybackInfo.currentPosition = it.getPlayerPosition()
            audioPlaybackInfo.duration = it.getPlayerDuration().toLong()
            audioPlaybackInfo.isPlaying = it.isPlaying()
            it.onProgressUpdate(it.getAudioProgressHandlerCallback())
        }
    }
}

interface OnPlayerRepeatingCallback {
    fun getAudioProgressHandlerCallback(): AudioProgressHandler
    fun onProgressUpdate(audioProgressHandler: AudioProgressHandler)
    fun getPlayerPosition(): Int
    fun getPlayerDuration(): Int
    fun isPlaying(): Boolean
}