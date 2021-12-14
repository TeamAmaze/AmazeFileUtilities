package com.amaze.fileutilities.audio_player.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlaybackInfo
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.audio_player.AudioPlayerService

class AudioPlayerNotificationImpl24 : AudioPlayerNotification() {
    @Synchronized
    override fun update() {
        stopped = false
        val playbackInfo: AudioPlaybackInfo = service.audioProgressHandler!!.audioPlaybackInfo
        val isPlaying: Boolean = service.isPlaying()
        val playButtonResId: Int =
            if (isPlaying) R.drawable.ic_baseline_pause_circle_outline_32 else R.drawable.ic_baseline_play_circle_outline_32
        val action = Intent(service, AudioPlayerDialogActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val clickIntent = PendingIntent.getActivity(service, 0, action, 0)
        val serviceName = ComponentName(service, AudioPlayerDialogActivity::class.java)
        val intent = Intent(AudioPlayerService.ACTION_CANCEL)
        intent.component = serviceName
        val deleteIntent = PendingIntent.getService(service, 0, intent, 0)

        var bitmap = BitmapFactory.decodeResource(
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
                R.drawable.ic_outline_fast_rewind_32,
                service.getString(R.string.previous),
                retrievePlaybackAction(AudioPlayerService.ACTION_PREVIOUS)
            )
        val nextAction: NotificationCompat.Action =
            NotificationCompat.Action(
                R.drawable.ic_outline_fast_rewind_32,
                service.getString(R.string.next),
                retrievePlaybackAction(AudioPlayerService.ACTION_NEXT)
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
                .setDeleteIntent(deleteIntent)
                .setContentTitle(playbackInfo.title)
                .setContentText(playbackInfo.artistName)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(service.mediaSession!!.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder.color = service.resources.getColor(R.color.blue)
        }
        if (stopped) return   // notification has been stopped before loading was finished
        updateNotifyModeAndPostNotification(builder.build())
    }

    private fun retrievePlaybackAction(action: String): PendingIntent {
        val serviceName = ComponentName(service, AudioPlayerService::class.java)
        val intent = Intent(action)
        intent.component = serviceName
        return PendingIntent.getService(service, 0, intent, 0)
    }
}
