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

import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isAudioMimeType
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudioPlayerDialogActivity : PermissionActivity(), OnPlaybackInfoUpdate {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }

    private lateinit var viewModel: AudioPlayerDialogActivityViewModel
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
//        AudioPlayerService.sendCancelBroadcast(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel = ViewModelProvider(this)
            .get(AudioPlayerDialogActivityViewModel::class.java)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val audioUri = intent.data
            Log.i(
                javaClass.simpleName,
                "Loading audio from path ${audioUri?.path} " +
                    "and mimetype $mimeType"
            )
            viewModel.uriList = ArrayList(
                audioUri!!.getSiblingUriFiles(this)!!.filter {
                    it.isAudioMimeType()
                }.asReversed()
            )
            AudioPlayerService.runService(audioUri, viewModel.uriList, this)
        }
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
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
                binding.seekbar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
//                            mediaController.transportControls.seekTo(progress.toLong())
//                            audioService.seekPlayer(progress.toLong())
                                audioService.invokeSeekPlayer(progress.toLong())
                                val x: Int = ceil(progress / 1000f).toInt()

                                if (x == 0 && audioService.getAudioProgressHandlerCallback()
                                    .isCancelled
                                ) {
                                    viewBinding.seekbar.progress = 0
                                }
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        }
                    })
                binding.title.text = audioService.getAudioPlaybackInfo().title
                binding.album.text = audioService.getAudioPlaybackInfo().albumName
                binding.artist.text = audioService.getAudioPlaybackInfo().artistName
                binding.seekbar.max = audioService.getAudioPlaybackInfo().duration.toInt()
            }
        }
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler) {
        progressHandler.audioPlaybackInfo.isPlaying.let {
            isPlaying ->
            if (progressHandler.isCancelled || !isPlaying) {
                viewBinding.playButton
                    .setImageResource(R.drawable.ic_baseline_play_circle_outline_32)
            } else {
                viewBinding.playButton
                    .setImageResource(R.drawable.ic_baseline_pause_circle_outline_32)
            }

            if (progressHandler.isCancelled) {
                viewBinding.seekbar.progress = 0
            }
        }
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        viewBinding.seekbar.progress = progressHandler.audioPlaybackInfo.currentPosition
        viewBinding.title.text = progressHandler.audioPlaybackInfo.title
        viewBinding.album.text = progressHandler.audioPlaybackInfo.albumName
        viewBinding.artist.text = progressHandler.audioPlaybackInfo.artistName
        viewBinding.seekbar.max = progressHandler.audioPlaybackInfo.duration.toInt()
        onPlaybackStateChanged(progressHandler)
    }

    override fun onPlaybackStateChanged(progressHandler: AudioProgressHandler) {
        invalidateActionButtons(progressHandler)
    }

    override fun setupActionButtons(audioService: WeakReference<ServiceOperationCallback>) {
        initActionButtons(audioService)
    }
}
