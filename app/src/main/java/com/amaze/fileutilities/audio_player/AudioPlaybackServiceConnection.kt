package com.amaze.fileutilities.audio_player

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import java.lang.ref.WeakReference

class AudioPlaybackServiceConnection(private val activityRef: WeakReference<OnPlaybackInfoUpdate>):
    ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder: ObtainableServiceBinder<out AudioPlayerService?> =
            service as ObtainableServiceBinder<out AudioPlayerService?>
        val specificService: ServiceOperationCallback? = binder.service
        specificService?.let {
                audioPlayerService ->
            activityRef.get()?.apply {
                audioPlayerService.getPlaybackInfoUpdateCallback(activityRef.get()!!)
                activityRef.get()?.setupActionButtons(WeakReference(audioPlayerService))
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }
}

interface ServiceOperationCallback {
    fun getPlaybackInfoUpdateCallback(onPlaybackInfoUpdate: OnPlaybackInfoUpdate)
    fun getAudioProgressHandlerCallback(): AudioProgressHandler
    fun invokePlayPausePlayer()
    fun invokeSeekPlayer(position: Long)
}