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

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding
import com.amaze.fileutilities.utilis.getFileFromUri
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isAudioMimeType
import com.amaze.fileutilities.utilis.showToastInCenter
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import linc.com.amplituda.exceptions.io.FileNotFoundException
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudioPlayerDialogActivity : PermissionActivity(), OnPlaybackInfoUpdate {

    private val _binding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }

    private lateinit var viewModel: AudioPlayerDialogActivityViewModel
    private lateinit var audioPlaybackServiceConnection: ServiceConnection
    private var forceShowSeekbar = false

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
        if (!viewModel.isPlaying) {
            AudioPlayerService.sendCancelBroadcast(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        viewModel = ViewModelProvider(this)
            .get(AudioPlayerDialogActivityViewModel::class.java)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val audioUri = intent.data
            if (audioUri == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
            }
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
        title = getString(R.string.audio_player)
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        _binding.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                waveformSeekbar.maxProgress = progressHandler.audioPlaybackInfo.duration.toFloat()
                waveformSeekbar.progress = progressHandler
                    .audioPlaybackInfo.currentPosition.toFloat()
            } else {
                seekBar.max = progressHandler.audioPlaybackInfo.duration.toInt()
                seekBar.progress = progressHandler.audioPlaybackInfo.currentPosition.toInt()
            }
            timeElapsed.text = AudioUtils.getReadableDurationString(
                progressHandler
                    .audioPlaybackInfo.currentPosition
            ) ?: ""
            trackLength.text = AudioUtils.getReadableDurationString(
                progressHandler
                    .audioPlaybackInfo.duration
            ) ?: ""
            invalidateActionButtons(progressHandler)
        }
    }

    override fun onPlaybackStateChanged(
        progressHandler: AudioProgressHandler,
        renderWaveform: Boolean
    ) {
        invalidateActionButtons(progressHandler)
        if (renderWaveform) {
            loadWaveFormSeekbar(progressHandler)
        }
    }

    override fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        _binding.run {
            val audioService = audioServiceRef.get()
            title.text = audioService?.getAudioPlaybackInfo()?.title
            album.text = audioService?.getAudioPlaybackInfo()?.albumName
            artist.text = audioService?.getAudioPlaybackInfo()?.artistName

            audioService?.let {
                playButton.setOnClickListener {
                    audioService.invokePlayPausePlayer()
                    invalidateActionButtons(audioService.getAudioProgressHandlerCallback())
                }
                prevButton.setOnClickListener {
                    startService(retrievePlaybackAction(AudioPlayerService.ACTION_PREVIOUS))
                }
                nextButton.setOnClickListener {
                    startService(retrievePlaybackAction(AudioPlayerService.ACTION_NEXT))
                }
            }
            setupSeekBars(audioService)
        }
    }

    override fun serviceDisconnected() {
        // do nothing
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler) {
        viewModel.isPlaying = progressHandler.audioPlaybackInfo.isPlaying
        progressHandler.audioPlaybackInfo.let {
            info ->
            if (progressHandler.isCancelled || !info.isPlaying) {
                _binding.playButton
                    .setImageResource(R.drawable.ic_round_play_circle_32)
            } else {
                _binding.playButton
                    .setImageResource(R.drawable.ic_round_pause_circle_32)
            }
            if (progressHandler.isCancelled) {
                setSeekbarProgress(0)
            }
            _binding.title.text = info.title
            _binding.album.text = info.albumName
            _binding.artist.text = info.artistName
        }
    }

    private fun setSeekbarProgress(progress: Int) {
        _binding.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                waveformSeekbar.progress = progress.toFloat()
            } else {
                seekBar.visibility = View.VISIBLE
                waveformSeekbar.visibility = View.GONE
                seekBar.progress = progress
            }
        }
    }

    private fun retrievePlaybackAction(action: String): Intent {
        val serviceName = ComponentName(this, AudioPlayerService::class.java)
        val intent = Intent(action)
        intent.component = serviceName
        return intent
    }

    private fun loadWaveFormSeekbar(progressHandler: AudioProgressHandler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceShowSeekbar) {
            _binding.run {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                val file = progressHandler.audioPlaybackInfo
                    .audioModel.getUri().getFileFromUri(this@AudioPlayerDialogActivity)
                if (file != null) {
                    try {
                        // TODO: hack to get valid wavebar path
                        /*if (!file.path.startsWith("storage")) {
                            file = File("storage/" + file.path)
                        }*/
                        waveformSeekbar.setSampleFrom(file)
                    } catch (fe: FileNotFoundException) {
                        fe.printStackTrace()
                        forceShowSeekbar = true
                    }
                } else {
                    forceShowSeekbar = true
                }
            }
        }
    }

    private fun setupSeekBars(audioService: ServiceOperationCallback?) {
        _binding.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceShowSeekbar) {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                val file = audioService?.getAudioProgressHandlerCallback()?.audioPlaybackInfo
                    ?.audioModel?.getUri()?.getFileFromUri(this@AudioPlayerDialogActivity)
                if (file != null) {
                    try {
                        waveformSeekbar.setSampleFrom(file)
                    } catch (fe: FileNotFoundException) {
                        fe.printStackTrace()
                        forceShowSeekbar = true
                        setupSeekBars(audioService)
                    }
                } else {
                    forceShowSeekbar = true
                    setupSeekBars(audioService)
                }
                waveformSeekbar.maxProgress = audioService
                    ?.getAudioPlaybackInfo()?.duration?.toFloat() ?: 0f
                waveformSeekbar.onProgressChanged = object : SeekBarOnProgressChanged {
                    override fun onProgressChanged(
                        waveformSeekBar: WaveformSeekBar,
                        progress: Float,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
//                            mediaController.transportControls.seekTo(progress.toLong())
//                            audioService.seekPlayer(progress.toLong())
                            audioService?.invokeSeekPlayer(progress.toLong())
                            val x: Int = ceil(progress / 1000f).toInt()

                            if (x == 0 && audioService?.getAudioProgressHandlerCallback()
                                ?.isCancelled == true
                            ) {
                                waveformSeekBar.progress = 0f
                            }
                        }
                    }
                }
            } else {
                seekBar.visibility = View.VISIBLE
                waveformSeekbar.visibility = View.GONE
                seekBar.max = audioService?.getAudioPlaybackInfo()?.duration?.toInt() ?: 0
                seekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            if (fromUser) {
//                            mediaController.transportControls.seekTo(progress.toLong())
//                            audioService.seekPlayer(progress.toLong())
                                audioService?.invokeSeekPlayer(progress.toLong())
                                val x: Int = ceil(progress / 1000f).toInt()

                                if (x == 0 && audioService?.getAudioProgressHandlerCallback()
                                    ?.isCancelled == true
                                ) {
                                    seekBar?.progress = 0
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
}
