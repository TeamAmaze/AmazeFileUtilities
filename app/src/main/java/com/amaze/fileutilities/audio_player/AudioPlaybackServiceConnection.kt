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

import android.content.ComponentName
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import com.google.android.exoplayer2.PlaybackParameters
import java.lang.ref.WeakReference

class AudioPlaybackServiceConnection(private val activityRef: WeakReference<OnPlaybackInfoUpdate>) :
    ServiceConnection {

    private var specificService: ServiceOperationCallback? = null
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder: ObtainableServiceBinder<out AudioPlayerService?> =
            service as ObtainableServiceBinder<out AudioPlayerService?>
        specificService = binder.service
        specificService?.let {
            audioPlayerService ->
            activityRef.get()?.apply {
                audioPlayerService.getPlaybackInfoUpdateCallback(activityRef.get()!!)
                activityRef.get()?.setupActionButtons(WeakReference(audioPlayerService))
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        activityRef.get()?.serviceDisconnected()
        specificService?.getPlaybackInfoUpdateCallback(null)
    }

    fun getAudioServiceInstance(): ServiceOperationCallback? {
        return specificService
    }
}

interface ServiceOperationCallback {
    fun getPlaybackInfoUpdateCallback(onPlaybackInfoUpdate: OnPlaybackInfoUpdate?)
    fun getAudioProgressHandlerCallback(): AudioProgressHandler?
    fun getAudioPlaybackInfo(): AudioPlaybackInfo?
    fun invokePlayPausePlayer()
    fun initLyrics(lyricsText: String, isSynced: Boolean, filePath: String)
    fun clearLyrics()
    fun invokeSeekPlayer(position: Long)
    fun cycleShuffle(): Boolean
    fun cycleRepeat(): Int
    fun getShuffle(): Boolean
    fun getRepeat(): Int
    fun invokePlaybackProperties(playbackSpeed: Float, pitch: Float): Unit
    fun getPlaybackParameters(): PlaybackParameters?
    fun insertPlayNextSong(uri: Uri)
}
