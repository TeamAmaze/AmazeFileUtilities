package com.amaze.fileutilities.audio_player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes

class AudioPlayerService: MediaBrowserServiceCompat() {
    var mediaSession: MediaSessionCompat? = null
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private var exoPlayer: ExoPlayer? = null
    private var oldUri: Uri? = null
    private var mAttrs: AudioAttributes? = null
    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            uri?.let {
                val mediaItem = extractMediaSourceFromUri(uri)
                if (uri != oldUri)
                    play(mediaItem)
                else play() // this song was paused so we don't need to reload it
                oldUri = uri
            }
        }

        override fun onPause() {
            super.onPause()
            pause()
        }

        override fun onStop() {
            super.onStop()
            stop()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        initializeAttributes()
        mediaSession = MediaSessionCompat(baseContext, javaClass.simpleName).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            // Set initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            mStateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            setPlaybackState(mStateBuilder.build())

            // methods that handle callbacks from a media controller
            setCallback(mMediaSessionCallback)

            // Set the session's token so that client activities can communicate with it
            setSessionToken(sessionToken)
            isActive = true
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
    }

    private fun play(mediaItem: MediaItem) {
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

    private fun play() {
        exoPlayer?.apply {
            exoPlayer?.playWhenReady = true
            exoPlayer?.play()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    private fun pause() {
        exoPlayer?.apply {
            playWhenReady = false
            if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stop() {
        // release the resources when the service is destroyed
        exoPlayer?.playWhenReady = false
        exoPlayer?.release()
        exoPlayer = null
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
        mediaSession?.isActive = false
        mediaSession?.release()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    private fun updatePlaybackState(state: Int) {
        // You need to change the state because the action taken in the controller depends on the state !!!
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder().setState(
                state // this state is handled in the media controller
                , 0L
                , 1.0f // Speed playing
            ).build()
        )
    }

    private fun initializeAttributes() {
        mAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
    }

    private fun extractMediaSourceFromUri(uri: Uri): MediaItem {
        return MediaItem.fromUri(uri)
    }
}