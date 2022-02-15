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

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlaybackInfo
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.utilis.BitmapPaletteWrapper
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.bumptech.glide.request.target.Target

class AudioPlayerNotificationImpl : AudioPlayerNotification() {
    private var target: Target<BitmapPaletteWrapper>? = null
    @Synchronized
    override fun update() {
        stopped = false
        val playbackInfo = if (service.audioProgressHandler != null)
            service.audioProgressHandler!!.audioPlaybackInfo else AudioPlaybackInfo.EMPTY_PLAYBACK
        val isPlaying: Boolean = service.isPlaying()
        val notificationLayout = RemoteViews(
            service.packageName,
            R.layout.audio_player_notification
        )
        val notificationLayoutBig = RemoteViews(
            service.packageName,
            R.layout.audio_player_notification_big
        )
        if (TextUtils.isEmpty(playbackInfo.title) && TextUtils.isEmpty(playbackInfo.artistName)) {
            notificationLayout.setViewVisibility(R.id.titles, View.INVISIBLE)
        } else {
            notificationLayout.setViewVisibility(R.id.titles, View.VISIBLE)
            notificationLayout.setTextViewText(R.id.audio_name, playbackInfo.title)
            notificationLayout.setTextViewText(R.id.audio_artist, playbackInfo.artistName)
        }
        if (TextUtils.isEmpty(playbackInfo.title) && TextUtils.isEmpty(playbackInfo.artistName) &&
            TextUtils.isEmpty(
                    playbackInfo.albumName
                )
        ) {
            notificationLayoutBig.setViewVisibility(R.id.titles, View.INVISIBLE)
        } else {
            notificationLayoutBig.setViewVisibility(R.id.titles, View.VISIBLE)
            notificationLayoutBig.setTextViewText(R.id.audio_name, playbackInfo.title)
            notificationLayoutBig.setTextViewText(R.id.audio_artist, playbackInfo.artistName)
//            notificationLayoutBig.setTextViewText(R.id.text2, song.albumName)
        }
        linkButtons(notificationLayout, notificationLayoutBig)
        val action = Intent(service, AudioPlayerDialogActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val clickIntent = PendingIntent.getActivity(service, 0, action, 0)
        val deleteIntent = buildPendingIntent(service, AudioPlayerService.ACTION_CANCEL, null)
        val notification: Notification =
            NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_play_circle_outline_32)
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(isPlaying)
                .build()
        updateNotifyModeAndPostNotification(notification)
    }

    private fun linkButtons(notificationLayout: RemoteViews, notificationLayoutBig: RemoteViews) {
        var pendingIntent: PendingIntent
        val serviceName = ComponentName(service, AudioPlayerService::class.java)
        val preferences = service.getAppCommonSharedPreferences()
        val doShuffle = preferences.getBoolean(
            PreferencesConstants.KEY_AUDIO_PLAYER_SHUFFLE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_SHUFFLE
        )
        val repeatMode = preferences.getInt(
            PreferencesConstants.KEY_AUDIO_PLAYER_REPEAT_MODE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_REPEAT_MODE
        )

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
        notificationLayout.setImageViewResource(R.id.action_repeat, repeatButtonResId)
        notificationLayout.setImageViewResource(R.id.action_shuffle, shuffleButtonResId)
        notificationLayoutBig.setImageViewResource(R.id.action_repeat, repeatButtonResId)
        notificationLayoutBig.setImageViewResource(R.id.action_shuffle, shuffleButtonResId)

        // repeat
        pendingIntent = buildPendingIntent(
            service, AudioPlayerService.ACTION_REPEAT,
            serviceName
        )
        notificationLayout.setOnClickPendingIntent(R.id.action_repeat, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_repeat, pendingIntent)

        // Previous track
        pendingIntent = buildPendingIntent(service, AudioPlayerService.ACTION_PREVIOUS, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_previous, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_previous, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(
            service, AudioPlayerService.ACTION_PLAY_PAUSE,
            serviceName
        )
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

        // shuffle
        pendingIntent = buildPendingIntent(
            service, AudioPlayerService.ACTION_SHUFFLE,
            serviceName
        )
        notificationLayout.setOnClickPendingIntent(R.id.action_shuffle, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_shuffle, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(service, AudioPlayerService.ACTION_NEXT, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent)
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_next, pendingIntent)
    }

    private fun buildPendingIntent(
        context: Context,
        action: String,
        serviceName: ComponentName?
    ): PendingIntent {
        val intent = Intent(action)
        intent.component = serviceName
        return PendingIntent.getService(context, 0, intent, 0)
    }
}
