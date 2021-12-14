package com.amaze.fileutilities.audio_player

import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.SeekBar
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudioPlayerDialogActivity: PermissionActivity(), OnPlaybackInfoUpdate {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }

    private lateinit var audioModel: LocalAudioModel
    private lateinit var audioPlaybackServiceConnection: ServiceConnection

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, AudioPlayerService::class.java)
        this.bindService(intent, audioPlaybackServiceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        this.unbindService(audioPlaybackServiceConnection)
    }

    override fun onStop() {
        super.onStop()
        AudioPlayerService.sendCancelBroadcast(this)
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

            viewBinding.fileName.text = audioUri.path
            AudioPlayerService.runService(audioUri, null, this)
        }
        audioPlaybackServiceConnection = AudioPlaybackServiceConnection(WeakReference(this))
    }

    private fun isMediaStopped(progressHandler: AudioProgressHandler): Boolean {
        progressHandler.audioPlaybackInfo.playbackState.let {
            return it == PlaybackStateCompat.STATE_STOPPED ||
                    it == PlaybackStateCompat.STATE_NONE || progressHandler.isCancelled
        }
    }

    private fun isMediaPaused(state: Int): Boolean {
        state.let {
            return it == PlaybackStateCompat.STATE_PAUSED
        }
    }

    private fun initActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        viewBinding.let {
            binding ->
            audioServiceRef.get()?.also {
                audioService ->
                binding.playButton.setOnClickListener {
                    audioService.invokePlayPausePlayer()
                    invalidateActionButtons(audioService.getAudioProgressHandlerCallback())
                }
                binding.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
//                            mediaController.transportControls.seekTo(progress.toLong())
//                            audioService.seekPlayer(progress.toLong())
                            audioService.invokeSeekPlayer(progress.toLong())
                            val x: Int = ceil(progress / 1000f).toInt()

                            if (x == 0 && !isMediaStopped(audioService.getAudioProgressHandlerCallback())) {
                                viewBinding.seekbar.progress = 0
                            }
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
            }
        }
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler) {
        progressHandler.audioPlaybackInfo.playbackState.let {
            playbackState ->
            if (isMediaStopped(progressHandler) || isMediaPaused(playbackState)) {
                    viewBinding.playButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_32)
                }
            else if (playbackState == PlaybackStateCompat.STATE_PLAYING ||
                playbackState == PlaybackStateCompat.STATE_BUFFERING ||
                playbackState == PlaybackStateCompat.STATE_CONNECTING
            ) {
                viewBinding.playButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_32)
            }

            if (isMediaStopped(progressHandler)) {
                viewBinding.seekbar.progress = 0
            }
        }
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        viewBinding.seekbar.progress = progressHandler.audioPlaybackInfo.currentPosition
        viewBinding.seekbar.max = progressHandler.audioPlaybackInfo.duration
        onPlaybackStateChanged(progressHandler)
    }

    override fun onPlaybackStateChanged(progressHandler: AudioProgressHandler) {
        invalidateActionButtons(progressHandler)
    }

    override fun setupActionButtons(audioService: WeakReference<ServiceOperationCallback>) {
        initActionButtons(audioService)
    }
}