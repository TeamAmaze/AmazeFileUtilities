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

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.notification.AudioPlayerNotification
import com.amaze.fileutilities.audio_player.notification.AudioPlayerNotificationImpl
import com.amaze.fileutilities.audio_player.notification.AudioPlayerNotificationImpl24
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioAttributes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

class AudioPlayerService : Service(), ServiceOperationCallback, OnPlayerRepeatingCallback {

    var log: Logger = LoggerFactory.getLogger(AudioPlayerService::class.java)

    companion object {
        const val TAG_BROADCAST_AUDIO_SERVICE_CANCEL = "audio_service_cancel_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_PLAY = "audio_service_play_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_PREVIOUS = "audio_service_previous_broadcast"
        const val TAG_BROADCAST_AUDIO_SERVICE_NEXT = "audio_service_next_broadcast"
        const val ACTION_PLAY_PAUSE = "audio_action_play_pause"
        const val ACTION_CANCEL = "audio_action_cancel"
        const val ACTION_PREVIOUS = "audio_action_previous"
        const val ACTION_NEXT = "audio_action_next"
        const val ACTION_SHUFFLE = "audio_action_shuffle"
        const val ACTION_REPEAT = "audio_action_repeat"
        const val ARG_URI_LIST = "uri_list"
        const val ARG_URI = "uri"
        const val REPEAT_ALL = 100
        const val REPEAT_SINGLE = 101
        const val REPEAT_NONE = 102
        const val BACK_KEEP_PLAYING_THRESHOLD = 50000
        val REPEAT_ARRAY = arrayListOf(REPEAT_NONE, REPEAT_ALL, REPEAT_SINGLE)

        fun runService(uri: Uri, uriList: List<Uri>?, context: Context, action: String? = null) {
            val intent = Intent(context, AudioPlayerService::class.java)
            intent.putExtra(ARG_URI, uri)
            uriList?.let {
                intent.putParcelableArrayListExtra(ARG_URI_LIST, ArrayList(uriList))
            }
            intent.action = action
            context.startService(intent)
        }

        fun sendCancelBroadcast(context: Context) {
            context.sendBroadcast(Intent(TAG_BROADCAST_AUDIO_SERVICE_CANCEL))
        }
    }

    var serviceBinderPlaybackUpdate: OnPlaybackInfoUpdate? = null

    var exoPlayer: ExoPlayer? = null
    private var mAttrs: AudioAttributes? = null
    private val mBinder: IBinder = ObtainableServiceBinder(this)
    var audioProgressHandler: AudioProgressHandler? = null
    private var uriList: List<Uri>? = null
    private var currentUri: Uri? = null
    private var playingNotification: AudioPlayerNotification? = null
    private var audioPlayerRepeatingRunnable: AudioPlayerRepeatingRunnable? = null
    private var wakeLock: WakeLock? = null
    private var audioManager: AudioManager? = null
    private var pausedByTransientLossOfFocus = false
    var mediaSession: MediaSessionCompat? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext.getAppCommonSharedPreferences()
        initializePlayer()
        initializeAttributes()
        registerReceiver(
            cancelReceiver, IntentFilter(TAG_BROADCAST_AUDIO_SERVICE_CANCEL)
        )
        registerReceiver(
            pauseReceiver, IntentFilter(TAG_BROADCAST_AUDIO_SERVICE_PLAY)
        )

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock?.setReferenceCounted(false)
        setupMediaSession()
        initNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val intentUri: Uri? = it.getParcelableExtra(ARG_URI)
            if (currentUri == null && intentUri != null) {
                currentUri = intentUri
            }
            val intentUriList: List<Uri>? = it.getParcelableArrayListExtra(ARG_URI_LIST)
            if (uriList == null && intentUriList != null) {
                uriList = intentUriList
            }
            if (currentUri == null) {
                log.warn("No intent uri to start audio service")
                return super.onStartCommand(intent, flags, startId)
            }
            when {
                it.action != null -> {
                    when (it.action) {
                        ACTION_CANCEL -> {
                            triggerStopEverything()
                        }
                        ACTION_PLAY_PAUSE -> {
                            playMediaItem()
                        }
                        ACTION_NEXT -> {
                            uriList?.let {
                                audioProgressHandler?.let { handler ->
                                    val nextIndex = handler.getNextPlayingIndex()
                                    if (nextIndex < 0) {
                                        // do nothing
                                        Toast.makeText(
                                            baseContext,
                                            resources.getString(R.string.not_allowed),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return super.onStartCommand(intent, flags, startId)
                                    } else {
                                        initCurrentUriAndPlayer(
                                            it[nextIndex],
                                            true
                                        )
                                    }
                                }
                            }
                        }
                        ACTION_PREVIOUS -> {
                            uriList?.let {
                                audioProgressHandler?.let { handler ->
                                    if (handler.audioPlaybackInfo.currentPosition
                                        >= BACK_KEEP_PLAYING_THRESHOLD
                                    ) {
                                        exoPlayer?.seekTo(0)
                                    } else {
                                        val previousIndex = handler.getPreviousPlayingIndex()
                                        if (previousIndex < 0) {
                                            // do nothing
                                            Toast.makeText(
                                                baseContext,
                                                resources.getString(R.string.not_allowed),
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return super.onStartCommand(intent, flags, startId)
                                        } else {
                                            initCurrentUriAndPlayer(
                                                it[previousIndex],
                                                true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        ACTION_SHUFFLE -> {
                            log.info("cycling shuffle")
                            cycleShuffle()
                        }
                        ACTION_REPEAT -> {
                            log.info("cycling repeat")
                            cycleRepeat()
                        }
                        else -> {
                            initCurrentUriAndPlayer(intentUri!!, true)
                        }
                    }
                }
                else -> {
                    initCurrentUriAndPlayer(intentUri!!, true)
                }
            }
        }

        if (audioPlayerRepeatingRunnable == null) {
            audioPlayerRepeatingRunnable = AudioPlayerRepeatingRunnable(
                true,
                WeakReference(this)
            )
        }
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log.info("destroying audio player service")
        closeAudioEffectSession()
        stopExoPlayer()
        wakeLock!!.release()
        mediaSession?.release()
        playingNotification?.stop()
        getAudioManager().abandonAudioFocus(audioFocusListener)
        audioPlayerRepeatingRunnable?.cancel()
        unregisterReceiver(cancelReceiver)
        unregisterReceiver(pauseReceiver)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    private fun initCurrentUriAndPlayer(uri: Uri, renderWaveform: Boolean) {
        if (audioProgressHandler != null) {
            // TODO validate following condition
            if (audioProgressHandler!!.audioPlaybackInfo.audioModel.getUri()
                .path.equals(uri.path)
            ) {
                playMediaItem()
            } else {
                val mediaItem = extractMediaSourceFromUri(uri)
                initAudioPlaybackInfoAndHandler(uri)
                playMediaItem(mediaItem, renderWaveform)
            }
        } else {
            val mediaItem = extractMediaSourceFromUri(uri)
            initAudioPlaybackInfoAndHandler(uri)
            playMediaItem(mediaItem, renderWaveform)
        }
    }

    private fun initAudioPlaybackInfoAndHandler(uri: Uri) {
        val doShuffle = sharedPreferences.getBoolean(
            PreferencesConstants.KEY_AUDIO_PLAYER_SHUFFLE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_SHUFFLE
        )
        val repeatMode = sharedPreferences.getInt(
            PreferencesConstants.KEY_AUDIO_PLAYER_REPEAT_MODE,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_REPEAT_MODE
        )
        val audioPlaybackInfo = AudioPlaybackInfo.init(baseContext, uri)
        if (audioProgressHandler == null) {
            audioProgressHandler = AudioProgressHandler(
                false, uriList,
                AudioProgressHandler.INDEX_UNDEFINED, audioPlaybackInfo, doShuffle, repeatMode
            )
        } else {
            audioProgressHandler?.isCancelled = false
            audioProgressHandler?.uriList = uriList
            audioProgressHandler?.audioPlaybackInfo = audioPlaybackInfo
            audioProgressHandler?.doShuffle = doShuffle
            audioProgressHandler?.repeatMode = repeatMode
        }
        audioProgressHandler!!.getPlayingIndex(true)
    }

    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (!exoPlayer!!.isPlaying &&
                        pausedByTransientLossOfFocus
                    ) {
                        playMediaItem()
                        pausedByTransientLossOfFocus = false
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS ->
                    // Lost focus for an unbounded amount of time:
                    // stop playback and release media playback
                    pausePlayer()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Lost focus for a short time, but we have to stop
                    // playback. We don't release the media playback because playback
                    // is likely to resume
                    val wasPlaying: Boolean = isPlaying()
                    pausePlayer()
                    pausedByTransientLossOfFocus = wasPlaying
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Lost focus for a short time, but it's ok to keep playing
                    // at an attenuated level
                    pausePlayer()
                }
            }
        }

    private fun initNotification() {
        playingNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AudioPlayerNotificationImpl24()
        } else {
            AudioPlayerNotificationImpl()
        }
        playingNotification?.init(this)
    }

    private fun updateNotification() {
        if (playingNotification != null) {
            playingNotification?.update()
        }
    }

    private fun getCancelPendingIntent(): PendingIntent {
        val stopIntent = Intent(TAG_BROADCAST_AUDIO_SERVICE_CANCEL)
        return PendingIntent.getBroadcast(
            this, 1234, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPlayPendingIntent(): PendingIntent {
        val intent = Intent(TAG_BROADCAST_AUDIO_SERVICE_PLAY)
        return PendingIntent.getBroadcast(
            this, 1235, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val MEDIA_SESSION_ACTIONS = (
        PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_SEEK_TO
        )

    private fun updateMediaSessionPlaybackState() {
        mediaSession!!.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MEDIA_SESSION_ACTIONS)
                .setState(
                    if (isPlaying()) {
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        PlaybackStateCompat.STATE_PAUSED
                    },
                    exoPlayer!!.currentPosition, 1f
                )
                .build()
        )
    }

    private fun updateMediaSessionMetaData() {
        audioProgressHandler?.let {
            audioProgressHandler ->
            val song: AudioPlaybackInfo = audioProgressHandler.audioPlaybackInfo
            /*if (song.id === -1) {
                mediaSession!!.setMetadata(null)
                return
            }*/
            val metaData = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                    (audioProgressHandler.playingIndex).toLong()
                )
                .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            val albumUri = AudioUtils.getMediaStoreAlbumCoverUri(song.albumId)
            AudioUtils.getAlbumBitmap(applicationContext, albumUri)?.let {
                bitmap ->
                metaData.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    bitmap
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioProgressHandler.uriList?.let {
                    metaData.putLong(
                        MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                        audioProgressHandler.uriList!!.size.toLong()
                    )
                }
            }
            mediaSession!!.setMetadata(metaData.build())
        }
    }

    private fun getAudioManager(): AudioManager {
        if (audioManager == null) {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        }
        return audioManager!!
    }

    private fun setupMediaSession() {
        val mediaButtonReceiverComponentName = ComponentName(
            applicationContext,
            MediaButtonIntentReceiver::class.java
        )
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.component = mediaButtonReceiverComponentName
        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, mediaButtonIntent, 0
        )
        mediaSession = MediaSessionCompat(
            this,
            javaClass.name,
            mediaButtonReceiverComponentName,
            mediaButtonReceiverPendingIntent
        )
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playMediaItem()
            }

            override fun onPause() {
                pausePlayer()
            }

            override fun onSkipToNext() {
                // do nothing yet
            }

            override fun onSkipToPrevious() {
                // do nothing yet
            }

            override fun onStop() {
                triggerStopEverything()
            }

            override fun onSeekTo(pos: Long) {
                seekPlayer(pos)
            }

            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonIntentReceiver.handleIntent(
                    this@AudioPlayerService,
                    mediaButtonEvent
                )
            }
        })
        mediaSession?.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
        mediaSession?.isActive = true
    }

    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, exoPlayer!!.audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
    }

    private fun requestFocus(): Boolean {
        return getAudioManager().requestAudioFocus(
            audioFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun playMediaItem(mediaItem: MediaItem, renderWaveform: Boolean) {
        if (exoPlayer == null) initializePlayer()
        exoPlayer?.apply {
            // AudioAttributes here from exoplayer package !!!
            initializeAttributes()
            setAudioAttributes(mAttrs!!, true)
            if (isPlaying) {
                stop()
                clearMediaItems()
            }
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            play()
        }
        updatePlaybackState(true, renderWaveform)
        invalidateNotificationPlayButton()
    }

    private fun playMediaItem() {
        exoPlayer?.apply {
            if (isPlaying) {
                pausePlayer()
            } else {
                if (audioProgressHandler!!.audioPlaybackInfo.duration.toInt() <=
                    audioProgressHandler!!.audioPlaybackInfo.currentPosition ||
                    audioProgressHandler!!.audioPlaybackInfo.duration <= exoPlayer!!
                        .currentPosition
                ) {
                    seekTo(0)
                }
                exoPlayer?.playWhenReady = true
                exoPlayer?.play()
                updatePlaybackState(true, false)
                invalidateNotificationPlayButton()
            }
        }
    }

    private fun seekPlayer(position: Long) {
        exoPlayer?.apply {
            seekTo(position)
            invalidateNotificationPlayButton()
        }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        // reset playback parameter
        getAppCommonSharedPreferences().edit()
            .remove(PreferencesConstants.KEY_PLAYBACK_SEMITONES).apply()
    }

    private fun pausePlayer() {
        exoPlayer?.apply {
            playWhenReady = false
            pause()
            updatePlaybackState(false, false)
            invalidateNotificationPlayButton()
        }
    }

    private fun stopExoPlayer() {
        // release the resources when the service is destroyed
        exoPlayer?.playWhenReady = false
        updatePlaybackState(false, false)
        updateMediaSessionPlaybackState()
        exoPlayer?.release()
        exoPlayer = null
        audioProgressHandler?.isCancelled = true
    }

    // don't forget to update notification after this function
    private fun updatePlaybackState(isPlaying: Boolean, renderWaveform: Boolean) {
        audioProgressHandler?.let {
            it.audioPlaybackInfo.isPlaying = isPlaying
//            onProgressUpdate(it)
            serviceBinderPlaybackUpdate?.onPlaybackStateChanged(
                audioProgressHandler!!,
                renderWaveform
            )
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
            triggerStopEverything()
        }
    }

    private fun triggerStopEverything() {
        audioProgressHandler?.isCancelled = true
        serviceBinderPlaybackUpdate?.onPlaybackStateChanged(audioProgressHandler!!, false)
    }

    private val pauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            exoPlayer?.let {
                playMediaItem()
                serviceBinderPlaybackUpdate?.onPlaybackStateChanged(audioProgressHandler!!, false)
            }
        }
    }

    private fun invalidateNotificationPlayButton() {
        updateMediaSessionPlaybackState()
        updateMediaSessionMetaData()
        playingNotification?.update()
    }

    override fun getPlaybackInfoUpdateCallback(onPlaybackInfoUpdate: OnPlaybackInfoUpdate?) {
        serviceBinderPlaybackUpdate = onPlaybackInfoUpdate
    }

    override fun invokePlayPausePlayer() {
        playMediaItem()
    }

    override fun invokePlaybackProperties(playbackSpeed: Float, pitch: Float) {
        val param = PlaybackParameters(playbackSpeed, pitch)
        exoPlayer?.playbackParameters = param
    }

    override fun getPlaybackParameters(): PlaybackParameters? {
        return exoPlayer?.playbackParameters
    }

    override fun invokeSeekPlayer(position: Long) {
        seekPlayer(position)
    }

    override fun cycleShuffle(): Boolean {
        audioProgressHandler!!.doShuffle = !audioProgressHandler!!.doShuffle
        sharedPreferences.edit().putBoolean(
            PreferencesConstants.KEY_AUDIO_PLAYER_SHUFFLE,
            audioProgressHandler!!.doShuffle
        ).apply()
        updateNotification()
        return audioProgressHandler!!.doShuffle
    }

    override fun cycleRepeat(): Int {
        var repeatModeIdx = 0
        REPEAT_ARRAY.forEachIndexed { index, i ->
            if (i == audioProgressHandler!!.repeatMode) {
                repeatModeIdx = index
            }
        }
        audioProgressHandler!!.repeatMode = if (repeatModeIdx == REPEAT_ARRAY.size - 1) {
            REPEAT_ARRAY[0]
        } else {
            REPEAT_ARRAY[repeatModeIdx + 1]
        }
        sharedPreferences.edit().putInt(
            PreferencesConstants.KEY_AUDIO_PLAYER_REPEAT_MODE,
            audioProgressHandler!!.repeatMode
        ).apply()
        updateNotification()
        return audioProgressHandler!!.repeatMode
    }

    override fun getShuffle(): Boolean {
        audioProgressHandler?.let {
            return it.doShuffle
        }
        return PreferencesConstants.DEFAULT_AUDIO_PLAYER_SHUFFLE
    }

    override fun getRepeat(): Int {
        audioProgressHandler?.let {
            return it.repeatMode
        }
        return PreferencesConstants.DEFAULT_AUDIO_PLAYER_REPEAT_MODE
    }

    override fun getAudioProgressHandlerCallback(): AudioProgressHandler? {
        return audioProgressHandler
    }

    override fun getAudioPlaybackInfo(): AudioPlaybackInfo? {
        return audioProgressHandler?.audioPlaybackInfo
    }

    override fun onProgressUpdate(audioProgressHandler: AudioProgressHandler) {
        if (audioProgressHandler.audioPlaybackInfo.duration.toInt() <=
            audioProgressHandler.audioPlaybackInfo.currentPosition ||
            audioProgressHandler.audioPlaybackInfo.duration <= exoPlayer!!
                .currentPosition
        ) {
            if (getShuffle()) {
                if (!uriList.isNullOrEmpty()) {
                    val randomIdx = Utils.generateRandom(0, uriList!!.size - 1)
                    audioProgressHandler.playingIndexStack.push(randomIdx)
                    runService(
                        uriList!![randomIdx],
                        uriList!!, applicationContext
                    )
                }
            } else {
                when (getRepeat()) {
                    REPEAT_SINGLE -> {
                        playMediaItem()
                    }
                    REPEAT_ALL -> {
                        if (!uriList.isNullOrEmpty()) {
                            runService(currentUri!!, uriList, applicationContext, ACTION_NEXT)
                        }
                    }
                    REPEAT_NONE -> {
                        // do nothing
                    }
                }
            }
        }
        serviceBinderPlaybackUpdate?.onPositionUpdate(audioProgressHandler)
        if (audioProgressHandler.isCancelled) {
            stopSelf()
        }
        /*customBigContentViews?.setProgressBar(R.id.audio_progress,
            audioProgressHandler.audioPlaybackInfo.duration,
            audioProgressHandler.audioPlaybackInfo.currentPosition, false)
        notificationManager.let {
            notificationManager.notify(
                NotificationConstants.AUDIO_PLAYER_ID,
                mBuilder?.build()
            )
        }*/
        updateMediaSessionPlaybackState()
        playingNotification?.update()
    }

    override fun getPlayerPosition(): Long {
        return exoPlayer!!.currentPosition
    }

    override fun getPlayerDuration(): Long {
        return exoPlayer!!.contentDuration
    }

    override fun isPlaying(): Boolean {
        exoPlayer?.let {
            return it.isPlaying
        }
        return false
    }
}
