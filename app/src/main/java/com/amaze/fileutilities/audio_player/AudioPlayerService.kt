package com.amaze.fileutilities.audio_player

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.media.session.MediaButtonReceiver
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.NotificationConstants
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes
import java.lang.ref.WeakReference

class AudioPlayerService: Service(), OnPlaybackInfoUpdate {

    companion object {
        const val TAG_BROADCAST_AUDIO_SERVICE_CANCEL = "audio_service_cancel_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_PLAY = "audio_service_play_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_PREVIOUS = "audio_service_previous_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_NEXT = "audio_service_next_broadcast"
        const val ARG_URI_LIST = "uri_list"
        const val ARG_URI = "uri"

        fun runService(uri: Uri, uriList: ArrayList<Uri>?, context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java)
            intent.putExtra(ARG_URI, uri)
            intent.putParcelableArrayListExtra(ARG_URI_LIST, uriList)
            context.startService(intent)
        }
    }

    var serviceBinderPlaybackUpdate: OnPlaybackInfoUpdate? = null

    var exoPlayer: ExoPlayer? = null
    private var mAttrs: AudioAttributes? = null
    private val mBinder: IBinder = ObtainableServiceBinder(this)
    private lateinit var notificationManager: NotificationManager
    var audioProgressHandler: AudioProgressHandler? = null
    private var uriList: List<Uri>? = null
    private var currentUri: Uri? = null
    private var customSmallContentViews: RemoteViews? = null
    private var customBigContentViews: RemoteViews? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var audioPlayerRepeatingRunnable: AudioPlayerRepeatingRunnable? = null

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeAttributes()
        registerReceiver(cancelReceiver, IntentFilter(TAG_BROADCAST_AUDIO_SERVICE_CANCEL)
        )
        registerReceiver(pauseReceiver, IntentFilter(TAG_BROADCAST_AUDIO_SERVICE_CANCEL)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            currentUri = it.getParcelableExtra(ARG_URI)
            uriList = it.getParcelableArrayListExtra(ARG_URI_LIST)

            val mediaItem = extractMediaSourceFromUri(currentUri!!)
            if (audioProgressHandler != null) {
                if (audioProgressHandler!!.audioPlaybackInfo.audioModel.getUri() == currentUri) {
                    playMediaItem()
                } else {
                    playMediaItem(mediaItem)
                }
            } else {
                playMediaItem(mediaItem)
            }
            val audioPlaybackInfo = AudioPlaybackInfo(LocalAudioModel(currentUri!!, ""),
                exoPlayer!!.duration.toInt(), exoPlayer!!.currentPosition.toInt(), exoPlayer!!.playbackState)
            audioProgressHandler = AudioProgressHandler(false, uriList,
                AudioProgressHandler.INDEX_UNDEFINED, audioPlaybackInfo)
        }
        initiateNotification()

        // set default notification views text
        NotificationConstants.setMetadata(this, mBuilder!!, NotificationConstants.TYPE_NORMAL)

        startForeground(NotificationConstants.AUDIO_PLAYER_ID, mBuilder!!.build())

        audioPlayerRepeatingRunnable = AudioPlayerRepeatingRunnable(true, WeakReference(this))
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopExoPlayer()
        notificationManager.cancelAll()
        unregisterReceiver(cancelReceiver)
        unregisterReceiver(pauseReceiver)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        serviceBinderPlaybackUpdate?.onPositionUpdate(progressHandler)
        customBigContentViews?.setProgressBar(R.id.audio_progress,
            progressHandler.audioPlaybackInfo.duration,
            progressHandler.audioPlaybackInfo.currentPosition, false)
        notificationManager.let {
            notificationManager.notify(
                NotificationConstants.AUDIO_PLAYER_ID,
                mBuilder?.build()
            )
        }
    }

    override fun onPlaybackStateChanged(progressHandler: AudioProgressHandler) {
        serviceBinderPlaybackUpdate?.onPlaybackStateChanged(progressHandler)
        if (progressHandler.isCancelled) {
            stopSelf()
        }
    }

    override fun setupActionButtons(audioService: WeakReference<AudioPlayerService>) {
        // do nothing
    }

    private fun initiateNotification() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, AudioPlayerDialogActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        notificationIntent.data = currentUri
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        customBigContentViews = RemoteViews(packageName, R.layout.audio_player_notification_big)

        customBigContentViews?.setTextViewText(R.id.audio_name, currentUri?.path)
        customBigContentViews?.setProgressBar(R.id.audio_progress, 0, 0, true)

        mBuilder = NotificationCompat.Builder(this, NotificationConstants.CHANNEL_NORMAL_ID).apply {
            setContentIntent(pendingIntent)
            setCustomBigContentView(customBigContentViews)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            addAction(getStopAction())
            addAction(getPlayAction())
            addAction(getPreviousAction())
            addAction(getNextAction())
            addAction(NotificationCompat.Action(
                R.drawable.ic_baseline_play_circle_outline_32, getString(R.string.cancel),
                MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_PAUSE)
            ))
            setOngoing(true)
            setVisibility(VISIBILITY_PUBLIC)
            setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_STOP))
            setColor(resources.getColor(R.color.blue))
            /*setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession!!.sessionToken)
                .setShowActionsInCompactView(0)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        baseContext,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
            )*/
        }
    }

    private fun getStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(TAG_BROADCAST_AUDIO_SERVICE_CANCEL)
        val stopPendingIntent =
            PendingIntent.getBroadcast(this, 1234, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(
            R.drawable.ic_baseline_arrow_back_32, getString(R.string.cancel),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_STOP)
            stopPendingIntent
        )
    }

    private fun getPlayAction(): NotificationCompat.Action {
        val intent = Intent(TAG_BROADCAST_AUDIO_SERVICE_PLAY)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 1235, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(
            R.drawable.ic_baseline_play_circle_outline_32, getString(R.string.play),
//            MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_PLAY)
            pendingIntent
        )
    }

    private fun getPreviousAction(): NotificationCompat.Action {
        val intent = Intent(TAG_BROADCAST_AUDIO_SERVICE_PREVIOUS)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 1236, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(
            R.drawable.ic_outline_fast_rewind_32, getString(R.string.previous),
            MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )
    }

    private fun getNextAction(): NotificationCompat.Action {
        val intent = Intent(TAG_BROADCAST_AUDIO_SERVICE_NEXT)
        val pendingIntent =
            PendingIntent.getBroadcast(this, 1237, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(
            R.drawable.ic_outline_fast_forward_32, getString(R.string.next),
            MediaButtonReceiver.buildMediaButtonPendingIntent(baseContext, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
    }

    private fun playMediaItem(mediaItem: MediaItem) {
        if (exoPlayer == null) initializePlayer()
        exoPlayer?.apply {
            // AudioAttributes here from exoplayer package !!!
            mAttrs?.let { initializeAttributes() }
//            setAudioAttributes(mAttrs!!, true)
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun playMediaItem() {
        exoPlayer?.apply {
            if (isPlaying) {
                pausePlayer()
            } else {
                exoPlayer?.playWhenReady = true
                exoPlayer?.play()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    fun seekPlayer(position: Long) {
        exoPlayer?.apply {
            seekTo(position)
        }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    private fun pausePlayer() {
        exoPlayer?.apply {
            playWhenReady = false
            if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    fun stopExoPlayer() {
        // release the resources when the service is destroyed
        exoPlayer?.playWhenReady = false
        exoPlayer?.release()
        exoPlayer = null
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
        audioProgressHandler?.isCancelled = true
    }

    private fun updatePlaybackState(state: Int) {
        audioProgressHandler?.let {
            it.audioPlaybackInfo.playbackState = state
        }
    }

    private fun initializeAttributes() {
        mAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
    }

    private fun extractMediaSourceFromUri(uri: Uri): MediaItem {
        return MediaItem.fromUri(uri)
    }


    private val cancelReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // cancel operation
            audioProgressHandler?.isCancelled = true
        }
    }

    private val pauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            exoPlayer?.let {
                playMediaItem()
                invalidateNotificationPlayButton()
                audioProgressHandler!!.audioPlaybackInfo.playbackState = if (exoPlayer!!.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                serviceBinderPlaybackUpdate?.onPlaybackStateChanged(audioProgressHandler!!)
            }
        }
    }

    private fun invalidateNotificationPlayButton() {
        exoPlayer?.let {
            getPlayAction().icon = if (it.isPlaying)
                R.drawable.ic_baseline_pause_circle_outline_32
            else R.drawable.ic_baseline_play_circle_outline_32
            notificationManager.let {
                notificationManager.notify(
                    NotificationConstants.AUDIO_PLAYER_ID,
                    mBuilder?.build()
                )
            }
        }
    }
}