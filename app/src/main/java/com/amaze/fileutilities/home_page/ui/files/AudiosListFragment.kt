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

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.*
import com.amaze.fileutilities.databinding.FragmentAudiosListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.slider.Slider
import com.masoudss.lib.WaveformSeekBar
import me.tankery.lib.circularseekbar.CircularSeekBar
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

class AudiosListFragment : AbstractMediaInfoListFragment(), IAudioPlayerInterfaceHandler {

    private var log: Logger = LoggerFactory.getLogger(AudiosListFragment::class.java)

    private val filesViewModel: FilesViewModel by activityViewModels()
    private lateinit var viewModel: AudioPlayerInterfaceHandlerViewModel
    private var _binding: FragmentAudiosListBinding? = null
    private var isBottomFragmentVisible = false
    private lateinit var fileStorageSummaryAndMediaFileInfo:
        Pair<FilesViewModel.StorageSummary, List<MediaFileInfo>?>

    private lateinit var audioPlaybackServiceConnection: ServiceConnection
    private var preloader: MediaAdapterPreloader? = null
    private var isWaveformProcessing = false

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
        viewModel =
            ViewModelProvider(this).get(AudioPlayerInterfaceHandlerViewModel::class.java)
        _binding = FragmentAudiosListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        (requireActivity() as MainActivity).setCustomTitle(resources.getString(R.string.audios))
        (activity as MainActivity).invalidateBottomBar(false)
        val sharedPrefs = requireContext().getAppCommonSharedPreferences()
        viewModel.forceShowSeekbar = !sharedPrefs.getBoolean(
            PreferencesConstants.KEY_ENABLE_WAVEFORM,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_WAVEFORM
        )
        filesViewModel.usedAudiosSummaryTransformations().observe(
            viewLifecycleOwner
        ) { metaInfoAndSummaryPair ->
            binding.audiosListInfoText.text = resources.getString(R.string.loading)
            metaInfoAndSummaryPair?.let {
                val metaInfoList = metaInfoAndSummaryPair.second
                metaInfoList.run {
                    if (this.isEmpty()) {
                        binding.audiosListInfoText.text =
                            resources.getString(R.string.no_files)
                        binding.loadingProgress.visibility = View.GONE
                    } else {
                        binding.audiosListInfoText.visibility = View.GONE
                        binding.loadingProgress.visibility = View.GONE
                    }
                    fileStorageSummaryAndMediaFileInfo = it
                    resetAdapter()

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
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_utilities))
        (activity as MainActivity).invalidateBottomBar(true)
        requireContext().unbindService(audioPlaybackServiceConnection)
        if (!viewModel.isPlaying) {
            AudioPlayerService.sendCancelBroadcast(requireContext())
        }
        _binding = null
    }

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        super.onPositionUpdate(progressHandler)
        _binding?.run {
            timeSummarySmall.text = "${timeElapsed.text} / ${trackLength.text}"
        }
        invalidateActionButtons(progressHandler)
    }

    override fun onPlaybackStateChanged(
        progressHandler: AudioProgressHandler,
        renderWaveform: Boolean
    ) {
        super.onPlaybackStateChanged(progressHandler, renderWaveform)
        invalidateActionButtons(progressHandler)
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
        super.setupActionButtons(audioServiceRef)
        _binding?.run {
            val audioService = audioServiceRef.get()
            titleSmall.text = audioService?.getAudioPlaybackInfo()?.title
            summarySmall.text = "${album.text} | ${artist.text}"
            Utils.marqueeAfterDelay(2000, titleSmall)
            Utils.marqueeAfterDelay(2000, summarySmall)

            audioService?.let {
                playButtonSmallParent.setOnClickListener {
                    audioService.invokePlayPausePlayer()
                    invalidateActionButtons(audioService.getAudioProgressHandlerCallback())
                }
            }
        }
    }

    override fun serviceDisconnected() {
        binding.layoutBottomSheet.hideTranslateY(500)
        isBottomFragmentVisible = false
    }

    override fun getFileStorageSummaryAndMediaFileInfoPair(): Pair<FilesViewModel.StorageSummary,
        List<MediaFileInfo>?>? {
        return if (::fileStorageSummaryAndMediaFileInfo.isInitialized)
            fileStorageSummaryAndMediaFileInfo else null
    }

    override fun getMediaAdapterPreloader(): MediaAdapterPreloader {
        if (preloader == null) {
            preloader = MediaAdapterPreloader(
                requireContext(),
                R.drawable.ic_outline_audio_file_32
            )
        }
        return preloader!!
    }

    override fun getRecyclerView(): RecyclerView {
        return binding.audiosListView
    }

    override fun getMediaListType(): Int {
        return MediaFileAdapter.MEDIA_TYPE_AUDIO
    }

    override fun getItemPressedCallback(mediaFileInfo: MediaFileInfo) {
        if (mediaFileInfo.longSize >
            AudioPlayerInterfaceHandlerViewModel.WAVEFORM_THRESHOLD_BYTES
        ) {
            viewModel.forceShowSeekbar = true
        }
    }

    override fun getParentView(): View? {
        return _binding?.layoutBottomSheet
    }

    override fun getSeekbar(): Slider? {
        return _binding?.seekBar
    }

    override fun getSeekbarSmall(): CircularSeekBar? {
        return _binding?.miniPlayerSeekBar
    }

    override fun getWaveformSeekbar(): WaveformSeekBar? {
        return _binding?.waveformSeekbar
    }

    override fun getTimeElapsedTextView(): TextView? {
        return _binding?.timeElapsed
    }

    override fun getTrackLengthTextView(): TextView? {
        return _binding?.trackLength
    }

    override fun getTitleTextView(): TextView? {
        return _binding?.title
    }

    override fun getAlbumTextView(): TextView? {
        return _binding?.album
    }

    override fun getArtistTextView(): TextView? {
        return _binding?.artist
    }

    override fun getPlayButton(): ImageView? {
        return _binding?.playButton
    }

    override fun getPrevButton(): ImageView? {
        return _binding?.prevButton
    }

    override fun getNextButton(): ImageView? {
        return _binding?.nextButton
    }

    override fun getShuffleButton(): ImageView? {
        return _binding?.shuffleButton
    }

    override fun getRepeatButton(): ImageView? {
        return _binding?.repeatButton
    }

    override fun getAlbumImage(): ImageView? {
        return _binding?.albumImage
    }

    override fun getAlbumSmallImage(): ImageView? {
        return _binding?.albumImageSmall
    }

    override fun getPlaybackPropertiesButton(): ImageView? {
        return _binding?.playbackProperties
    }

    override fun getContextWeakRef(): WeakReference<Context> {
        return try {
            WeakReference(requireContext())
        } catch (e: IllegalStateException) {
            log.warn("failed to get context", e)
            WeakReference(null)
        }
    }

    override fun getAudioPlayerHandlerViewModel(): AudioPlayerInterfaceHandlerViewModel {
        return viewModel
    }

    override fun layoutInflater(): LayoutInflater {
        return layoutInflater
    }

    override fun getLogger(): Logger {
        return log
    }

    override fun getIsWaveformProcessing(): Boolean {
        return isWaveformProcessing
    }

    override fun setIsWaveformProcessing(bool: Boolean) {
        this.isWaveformProcessing = bool
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler?) {
        progressHandler?.audioPlaybackInfo?.let {
            info ->
            _binding?.let {
                if (progressHandler.isCancelled || !info.isPlaying) {
                    binding.playButtonSmall.setImageResource(R.drawable.ic_round_play_arrow_32)
                } else {
                    binding.playButtonSmall.setImageResource(R.drawable.ic_round_pause_32)
                }
                if (info.title != it.titleSmall.text) {
                    // needed to not invalidate view for marquee effect
                    it.titleSmall.text = info.title
                }
                val newAlbumAndArtist = "${it.album.text} | ${it.artist.text}"
                if (newAlbumAndArtist != it.summarySmall.text) {
                    it.summarySmall.text = "${it.album.text} | ${it.artist.text}"
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
}
