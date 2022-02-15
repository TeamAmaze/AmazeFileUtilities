/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlaybackInfo
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

class AudioPlayerNotificationImpl24 : AudioPlayerNotification() {
    @Synchronized
    override fun update() {
        stopped = false
        val preferences = service.getAppCommonSharedPreferences()
        val doShuffle = preferences.getBoolean(
            PreferencesConstants.KEY_AUDIO_PLAYER_SHUFFLE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_SHUFFLE
        )
        val repeatMode = preferences.getInt(
            PreferencesConstants.KEY_AUDIO_PLAYER_REPEAT_MODE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_REPEAT_MODE
        )
        val playbackInfo = if (service.audioProgressHandler != null)
            service.audioProgressHandler!!.audioPlaybackInfo else AudioPlaybackInfo.EMPTY_PLAYBACK
        val isPlaying: Boolean = service.isPlaying()
        val playButtonResId: Int =
            if (isPlaying) {
                R.drawable.ic_baseline_pause_circle_outline_32
            } else {
                R.drawable.ic_baseline_play_circle_outline_32
            }
        val shuffleButtonResId: Int =
            if (doShuffle) {
                R.drawable.ic_round_shuffle_32
            } else {
                R.drawable.ic_round_shuffle_gray_32
            }
        val repeatButtonResId: Int =
            when (repeatMode) {
                AudioPlayerService.REPEAT_NONE -> R.drawable.ic_round_repeat_gray_32
                AudioPlayerService.REPEAT_ALL -> R.drawable.ic_round_repeat_32
                AudioPlayerService.REPEAT_SINGLE -> R.drawable.ic_round_repeat_one_32
                else -> R.drawable.ic_round_repeat_32
            }
        val action = Intent(service, MainActivity::class.java)
        action.putExtra(MainActivity.KEY_INTENT_AUDIO_PLAYER, true)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val clickIntent = PendingIntent.getActivity(service, 0, action, 0)

        val bitmap = BitmapFactory.decodeResource(
            service.resources,
            R.drawable.ic_baseline_fullscreen_32
        )
        val playPauseAction =
            NotificationCompat.Action(
                playButtonResId,
                service.getString(R.string.play),
                retrievePlaybackAction(AudioPlayerService.ACTION_PLAY_PAUSE)
            )
        val previousAction: NotificationCompat.Action =
            NotificationCompat.Action(
                R.drawable.ic_round_skip_previous_32,
                service.getString(R.string.previous),
                retrievePlaybackAction(AudioPlayerService.ACTION_PREVIOUS)
            )
        val nextAction: NotificationCompat.Action =
            NotificationCompat.Action(
                R.drawable.ic_round_skip_next_32,
                service.getString(R.string.next),
                retrievePlaybackAction(AudioPlayerService.ACTION_NEXT)
            )
        val shuffleAction =
            NotificationCompat.Action(
                shuffleButtonResId,
                service.getString(R.string.shuffle),
                retrievePlaybackAction(AudioPlayerService.ACTION_SHUFFLE)
            )
        val repeatAction =
            NotificationCompat.Action(
                repeatButtonResId,
                service.getString(R.string.repeat),
                retrievePlaybackAction(AudioPlayerService.ACTION_REPEAT)
            )
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(
                service,
                NOTIFICATION_CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_baseline_play_circle_outline_32)
                .setSubText(playbackInfo.albumName)
                .setLargeIcon(bitmap)
                .setContentIntent(clickIntent)
                .setDeleteIntent(retrievePlaybackAction(AudioPlayerService.ACTION_CANCEL))
                .setContentTitle(playbackInfo.title)
                .setContentText(playbackInfo.artistName)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .addAction(repeatAction)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .addAction(shuffleAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(service.mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.color = service.resources.getColor(R.color.blue)
        }
        if (stopped) return // notification has been stopped before loading was finished
        updateNotifyModeAndPostNotification(builder.build())
    }

    private fun retrievePlaybackAction(action: String): PendingIntent {
        val serviceName = ComponentName(service, AudioPlayerService::class.java)
        val intent = Intent(action)
        intent.component = serviceName
        return PendingIntent.getService(service, 0, intent, 0)
    }
}
