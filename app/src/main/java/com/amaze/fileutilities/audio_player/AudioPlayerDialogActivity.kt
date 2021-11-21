package com.amaze.fileutilities.audio_player

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding

class AudioPlayerDialogActivity: PermissionActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }

    private lateinit var audioModel: LocalAudioModel
    private lateinit var mMediaBrowserCompat: MediaBrowserCompat
    private val connectionCallback: MediaBrowserCompat.ConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            mMediaBrowserCompat.sessionToken.also { token ->
                val mediaController = MediaControllerCompat(this@AudioPlayerDialogActivity, token)
                MediaControllerCompat.setMediaController(this@AudioPlayerDialogActivity, mediaController)
            }
            playPauseBuild()
            Log.d("onConnected", "Controller Connected")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d("onConnectionFailed", "Connection Failed")

        }

    }
    private val mControllerCallback = object : MediaControllerCompat.Callback() {
    }

    fun playPauseBuild() {
        val mediaController = MediaControllerCompat.getMediaController(this@AudioPlayerDialogActivity)
        viewBinding.playButton.setOnClickListener {
            val state = mediaController.playbackState.state
            if (state == PlaybackStateCompat.STATE_PAUSED ||
                state == PlaybackStateCompat.STATE_STOPPED ||
                state == PlaybackStateCompat.STATE_NONE
            ) {

                mediaController.transportControls.playFromUri(audioModel.uri, null)
                viewBinding.playButton.setImageResource(R.drawable.ic_baseline_pause_32)
            }
            else if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_BUFFERING ||
                state == PlaybackStateCompat.STATE_CONNECTING
            ) {
                mediaController.transportControls.pause()
                viewBinding.playButton.setImageResource(R.drawable.ic_baseline_play_arrow_32)
            }
        }
        mediaController.registerCallback(mControllerCallback)
    }

    override fun onStart() {
        super.onStart()
        // connect the controllers again to the session
        // without this connect() you won't be able to start the service neither control it with the controller
        mMediaBrowserCompat.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        // Release the resources
        val controllerCompat = MediaControllerCompat.getMediaController(this)
        controllerCompat?.unregisterCallback(mControllerCallback)
        mMediaBrowserCompat.disconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val audioUri = intent.data
            Log.i(javaClass.simpleName, "Loading audio from path ${audioUri?.path} " +
                    "and mimetype $mimeType")
            audioModel = LocalAudioModel(uri = audioUri!!, mimeType = mimeType!!)

            val componentName = ComponentName(this, AudioPlayerService::class.java)
            // initialize the browser
            mMediaBrowserCompat = MediaBrowserCompat(
                this, componentName,
                connectionCallback,
                null
            )
            viewBinding.fileName.text = audioUri.path
        }
    }
}