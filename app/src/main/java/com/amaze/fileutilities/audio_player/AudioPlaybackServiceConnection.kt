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

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import java.lang.ref.WeakReference

class AudioPlaybackServiceConnection(private val activityRef: WeakReference<OnPlaybackInfoUpdate>) :
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
    fun getAudioPlaybackInfo(): AudioPlaybackInfo
    fun invokePlayPausePlayer()
    fun invokeSeekPlayer(position: Long)
}
