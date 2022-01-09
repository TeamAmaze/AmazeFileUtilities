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
import com.amaze.fileutilities.utilis.*
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import linc.com.amplituda.exceptions.io.FileNotFoundException
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.lang.ref.WeakReference
import kotlin.math.ceil

class AudiosListFragment : Fragment(), OnPlaybackInfoUpdate, MediaFileAdapter.OptionsMenuSelected {
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

        activity?.volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(requireContext(), AudioPlayerService::class.java)
        requireContext().bindService(intent, audioPlaybackServiceConnection, 0)
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
                            this@AudiosListFragment,
                            MediaFileListSorter.SortingPreference.newInstance(
                                requireContext()
                                    .getAppCommonSharedPreferences()
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            binding.fastscroll.visibility = View.GONE
                            FastScrollerBuilder(binding.audiosListView).useMd2Style().build()
                        } else {
                            binding.fastscroll.visibility = View.VISIBLE
                            binding.fastscroll.setRecyclerView(binding.audiosListView, 1)
                        }
                    }
                }
            }
        )
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_files))
        (activity as MainActivity).invalidateBottomBar(true)
        requireContext().unbindService(audioPlaybackServiceConnection)
        if (!isPlaying) {
            AudioPlayerService.sendCancelBroadcast(requireContext())
        }
        _binding = null
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        _binding?.run {
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
            timeSummarySmall.text = "${timeElapsed.text} / ${trackLength.text}"
            invalidateActionButtons(progressHandler)
        }
    }

    override fun onPlaybackStateChanged(
        progressHandler: AudioProgressHandler,
        renderWaveform: Boolean
    ) {
        invalidateActionButtons(progressHandler)
        // invalidate wavebar
        if (renderWaveform) {
            loadWaveFormSeekbar(progressHandler)
        }
    }

    override fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        if (!isBottomFragmentVisible) {
            binding.layoutBottomSheet.visibility = View.VISIBLE
            val params: CoordinatorLayout.LayoutParams = binding.layoutBottomSheet
                .layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior as BottomSheetBehavior
            behavior.addBottomSheetCallback(bottomSheetCallback)
            binding.layoutBottomSheet.setOnClickListener {
                if (behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            isBottomFragmentVisible = true
        }

        _binding?.run {
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

    override fun serviceDisconnected() {
        binding.layoutBottomSheet.hideTranslateY(500)
        isBottomFragmentVisible = false
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler) {
        isPlaying = progressHandler.audioPlaybackInfo.isPlaying
        progressHandler.audioPlaybackInfo.let {
            info ->
            _binding?.let {
                if (progressHandler.isCancelled || !info.isPlaying) {
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
                it.title.text = info.title
                it.titleSmall.text = info.title
                it.album.text = info.albumName
                it.artist.text = info.artistName
                it.summarySmall.text = "${it.album.text} | ${it.artist.text}"
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
        _binding?.run {
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

    private fun loadWaveFormSeekbar(progressHandler: AudioProgressHandler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceShowSeekbar) {
            _binding?.run {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                val file = progressHandler.audioPlaybackInfo
                    .audioModel.getUri().getFileFromUri(requireContext())
                if (file != null) {
                    try {
                        // TODO: hack to get valid wavebar path
                        /*if (!file.path.startsWith("/storage")) {
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
        _binding?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceShowSeekbar) {
                waveformSeekbar.visibility = View.VISIBLE
                seekBar.visibility = View.GONE
                val file = audioService?.getAudioProgressHandlerCallback()?.audioPlaybackInfo
                    ?.audioModel?.getUri()?.getFileFromUri(requireContext())
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

    override fun sortBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun groupBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun switchView(isList: Boolean) {
        // do nothing
    }

    override fun select(headerPosition: Int) {
        binding.audiosListView.smoothScrollToPosition(headerPosition + 5)
    }
}
