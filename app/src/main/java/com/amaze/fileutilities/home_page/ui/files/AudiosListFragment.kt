/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.*
import com.amaze.fileutilities.databinding.FragmentAudiosListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.MediaTypeView
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.getFileFromUri
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudiosListFragment : Fragment(), OnPlaybackInfoUpdate {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentAudiosListBinding? = null
    private var mediaFileAdapter: MediaFileAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val MAX_PRELOAD = 100
    private var isBottomFragmentVisible = false
    private var isPlaying = true
    private var forceShowSeekbar = false

    private lateinit var audioPlaybackServiceConnection: ServiceConnection

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudiosListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        (requireActivity() as MainActivity).setCustomTitle(resources.getString(R.string.audios))
        (activity as MainActivity).invalidateBottomBar(false)
        filesViewModel.usedAudiosSummaryTransformations.observe(
            viewLifecycleOwner,
            {
                metaInfoAndSummaryPair ->
                binding.audiosListInfoText.text = resources.getString(R.string.loading)
                metaInfoAndSummaryPair?.let {
                    val metaInfoList = metaInfoAndSummaryPair.second
                    metaInfoList.run {
                        if (this.size == 0) {
                            binding.audiosListInfoText.text =
                                resources.getString(R.string.no_files)
                            binding.loadingProgress.visibility = View.GONE
                        } else {
                            binding.audiosListInfoText.visibility = View.GONE
                            binding.loadingProgress.visibility = View.GONE
                        }
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace =
                            FileUtils.formatStorageLength(
                                requireContext(), storageSummary.usedSpace!!
                            )
                        val totalSpace = FileUtils.formatStorageLength(
                            requireContext(), storageSummary.totalSpace!!
                        )
                        // set list adapter
                        preloader = MediaAdapterPreloader(
                            requireContext(),
                            R.drawable.ic_outline_audio_file_32
                        )
                        val sizeProvider = ViewPreloadSizeProvider<String>()
                        recyclerViewPreloader = RecyclerViewPreloader(
                            Glide.with(requireActivity()),
                            preloader!!,
                            sizeProvider,
                            MAX_PRELOAD
                        )
                        linearLayoutManager = LinearLayoutManager(context)
                        mediaFileAdapter = MediaFileAdapter(
                            requireContext(),
                            preloader!!,
                            MediaFileListSorter.SortingPreference(
                                MediaFileListSorter.GROUP_NAME,
                                MediaFileListSorter.SORT_SIZE,
                                true,
                                true
                            ),
                            this, MediaFileInfo.MEDIA_TYPE_AUDIO
                        ) {
                            it.setProgress(
                                MediaTypeView.MediaTypeContent(
                                    storageSummary.items, usedSpace,
                                    storageSummary.progress, totalSpace
                                )
                            )
                        }
                        binding.audiosListView
                            .addOnScrollListener(recyclerViewPreloader!!)
                        binding.audiosListView.layoutManager = linearLayoutManager
                        binding.audiosListView.adapter = mediaFileAdapter
                        binding.fastscroll.setRecyclerView(binding.audiosListView, 1)
                    }
                }
            }
        )
        return root
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
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_files))
        (activity as MainActivity).invalidateBottomBar(true)
        if (!isPlaying) {
            AudioPlayerService.sendCancelBroadcast(requireContext())
        }
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
            timeSummarySmall.text = "${timeElapsed.text} / ${trackLength.text}"
            onPlaybackStateChanged(progressHandler)
        }
    }

    override fun onPlaybackStateChanged(progressHandler: AudioProgressHandler) {
        invalidateActionButtons(progressHandler)
    }

    override fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        if (!isBottomFragmentVisible) {
            binding.layoutBottomSheet.visibility = View.VISIBLE
            val params: CoordinatorLayout.LayoutParams = binding.layoutBottomSheet
                .layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior as BottomSheetBehavior
            behavior.addBottomSheetCallback(bottomSheetCallback)
            isBottomFragmentVisible = true
        }

        binding.run {
            val audioService = audioServiceRef.get()
            title.text = audioService?.getAudioPlaybackInfo()?.title
            titleSmall.text = audioService?.getAudioPlaybackInfo()?.title
            album.text = audioService?.getAudioPlaybackInfo()?.albumName
            artist.text = audioService?.getAudioPlaybackInfo()?.artistName
            summarySmall.text = "${album.text} | ${artist.text}"

            audioService?.let {
                playButton.setOnClickListener {
                    audioService.invokePlayPausePlayer()
                    invalidateActionButtons(audioService.getAudioProgressHandlerCallback())
                }
                playButtonSmall.setOnClickListener {
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
            _binding?.let {
                if (progressHandler.isCancelled || !isPlaying) {
                    binding.playButton
                        .setImageResource(R.drawable.ic_round_play_circle_32)
                    binding.playButtonSmall.setImageResource(R.drawable.ic_round_play_circle_32)
                } else {
                    binding.playButton
                        .setImageResource(R.drawable.ic_round_pause_circle_32)
                    binding.playButtonSmall.setImageResource(R.drawable.ic_round_pause_circle_32)
                }

                if (progressHandler.isCancelled) {
                    setSeekbarProgress(0)
                }
            }
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            /*if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                binding.bottomSheetSmall.visibility = View.GONE
            }*/
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            binding.bottomSheetSmall.alpha = 1 - slideOffset
            binding.bottomSheetBig.alpha = slideOffset
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
