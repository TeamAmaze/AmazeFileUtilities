/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.FragmentManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.*
import com.amaze.fileutilities.databinding.BottomSheetAudioPlayerBinding
import com.amaze.fileutilities.utilis.getFileFromUri
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudioPlayerBottomSheet : BottomSheetDialogFragment(), OnPlaybackInfoUpdate {

    private var _binding: BottomSheetAudioPlayerBinding? = null
    private lateinit var audioPlaybackServiceConnection: ServiceConnection

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var isPlaying = true
    private var forceShowSeekbar = false

    companion object {
        fun showDialog(fragmentManager: FragmentManager) {
            val fragment = AudioPlayerBottomSheet()
            fragment.show(
                fragmentManager, javaClass.simpleName
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState) as BottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.appBottomSheetDialogTheme)
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAudioPlayerBinding.inflate(
            inflater, container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onResume() {
        super.onResume()
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(requireContext(), AudioPlayerService::class.java)
        requireContext().bindService(intent, audioPlaybackServiceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unbindService(audioPlaybackServiceConnection)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        _binding?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
            onPlaybackStateChanged(progressHandler)
        }
    }

    override fun onPlaybackStateChanged(progressHandler: AudioProgressHandler) {
        invalidateActionButtons(progressHandler)
    }

    override fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        binding.run {
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
                    requireContext()
                        .startService(retrievePlaybackAction(AudioPlayerService.ACTION_PREVIOUS))
                }
                nextButton.setOnClickListener {
                    requireContext()
                        .startService(retrievePlaybackAction(AudioPlayerService.ACTION_NEXT))
                }
            }
            setupSeekBars(audioService)
        }
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler) {
        isPlaying = progressHandler.audioPlaybackInfo.isPlaying
        progressHandler.audioPlaybackInfo.isPlaying.let {
            isPlaying ->
            if (progressHandler.isCancelled || !isPlaying) {
                binding.playButton
                    .setImageResource(R.drawable.ic_round_play_circle_32)
            } else {
                binding.playButton
                    .setImageResource(R.drawable.ic_round_pause_circle_32)
            }

            if (progressHandler.isCancelled) {
                setSeekbarProgress(0)
            }
        }
    }

    private fun setSeekbarProgress(progress: Int) {
        binding.run {
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
        val serviceName = ComponentName(requireContext(), AudioPlayerService::class.java)
        val intent = Intent(action)
        intent.component = serviceName
        return intent
    }

    private fun setupSeekBars(audioService: ServiceOperationCallback?) {
        binding.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceShowSeekbar) {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                val file = audioService?.getAudioProgressHandlerCallback()?.audioPlaybackInfo
                    ?.audioModel?.getUri()?.getFileFromUri(requireContext())
                if (file != null) {
                    waveformSeekbar.setSampleFrom(file)
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
