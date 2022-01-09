/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.video_player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.VideoPlayerFragmentBinding
import com.amaze.fileutilities.image_viewer.*
import com.amaze.fileutilities.utilis.AbstractMediaFragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerFragment : AbstractMediaFragment() {

    private var player: ExoPlayer? = null
    private var viewModel: VideoPlayerFragmentViewModel? = null
    var scaleGestureDetector: ScaleGestureDetector? = null

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlayerFragmentBinding.inflate(layoutInflater)
    }

    companion object {
        const val VIEW_TYPE_ARGUMENT = "VideoPlayerFragment.viewTypeArgument"

        /**
         * Creates a new instance of [VideoPlayerFragment]
         *
         * [viewType] is the [LocalVideoModel] that will be shown
         */
        @JvmStatic
        fun newInstance(videoModel: LocalVideoModel): VideoPlayerFragment {
            val arguments = Bundle().also {
                it.putParcelable(VIEW_TYPE_ARGUMENT, videoModel)
            }

            return VideoPlayerFragment().also {
                it.arguments = arguments
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)
            .get(VideoPlayerFragmentViewModel::class.java)
        viewModel?.videoModel = requireArguments().getParcelable(VIEW_TYPE_ARGUMENT)
        player = ExoPlayer.Builder(requireContext()).build().also {
            exoPlayer ->
            viewBinding.videoView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(viewModel?.videoModel!!.uri)
            exoPlayer.setMediaItem(mediaItem)
            initializePlayer()
            scaleGestureDetector = ScaleGestureDetector(
                requireContext(),
                CustomOnScaleGestureListener(viewBinding.videoView)
            )
        }
        if (activity is VideoPlayerDialogActivity) {
            handleViewPlayerDialogActivityResources()
        } else if (activity is VideoPlayerActivity) {
            handleVideoPlayerActivityResources()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        pausePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun handleViewPlayerDialogActivityResources() {
        viewBinding.videoView.findViewById<ConstraintLayout>(R.id.top_bar_video_player)
            .visibility = View.GONE
        viewBinding.videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
            .visibility = View.VISIBLE
        viewBinding.videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
            .setOnClickListener {
                val intent = Intent(
                    requireContext(),
                    VideoPlayerActivity::class.java
                ).apply {
                    putExtra(VIEW_TYPE_ARGUMENT, viewModel?.videoModel)
                }
                startActivity(intent)
                activity?.finish()
            }
        viewBinding.videoView.setShowNextButton(false)
        viewBinding.videoView.setShowPreviousButton(false)
    }

    private fun handleVideoPlayerActivityResources() {
        viewBinding.videoView.updateLayoutParams {
            width = FrameLayout.LayoutParams.MATCH_PARENT
            height = FrameLayout.LayoutParams.MATCH_PARENT
        }
        viewBinding.videoView.findViewById<ConstraintLayout>(R.id.top_bar_video_player)
            .visibility = View.VISIBLE
        viewBinding.videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
            .visibility = View.GONE
        viewBinding.videoView.findViewById<ImageView>(R.id.fit_to_screen_video_player)
            .setOnClickListener {
                viewModel?.fitToScreen?.also {
                    if (!it) {
                        viewBinding.videoView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        viewModel?.fitToScreen = true
                    } else {
                        viewBinding.videoView.resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        viewModel?.fitToScreen = false
                    }
                }
            }
        viewBinding.videoView.findViewById<ImageView>(R.id.orientation_video_player)
            .setOnClickListener {
                viewModel?.fullscreen?.also {
                    if (!it) {
                        requireActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        viewModel?.fullscreen = true
                    } else {
                        requireActivity().requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        viewModel?.fullscreen = false
                    }
                }
            }
        viewBinding.videoView.findViewById<ImageView>(R.id.back_video_player)
            .setOnClickListener {
                requireActivity().onBackPressed()
            }
        viewBinding.videoView.setControllerVisibilityListener {
            refactorSystemUi(it == View.GONE)
        }
    }

    override fun getRootLayout(): View {
        return viewBinding.root
    }

    override fun getToolbarLayout(): View? {
        return null
    }

    override fun getBottomBarLayout(): View? {
        return null
    }

    private fun releasePlayer() {
        player?.run {
            viewModel?.also {
                it.playbackPosition = this.currentPosition
                it.currentWindow = this.currentWindowIndex
                it.playWhenReady = this.playWhenReady
            }
            release()
        }
        player = null
    }

    private fun pausePlayer() {
        player?.run {
            viewModel?.also {
                it.playbackPosition = this.currentPosition
                it.currentWindow = this.currentWindowIndex
                it.playWhenReady = this.playWhenReady
                this.playWhenReady = false
                it.playWhenReady = false
            }
        }
    }

    private fun initializePlayer() {
        player?.let {
            exoPlayer ->
            viewModel?.also {
                exoPlayer.playWhenReady = it.playWhenReady
                exoPlayer.seekTo(it.currentWindow, it.playbackPosition)
                exoPlayer.prepare()
            }
        }
    }

    private class CustomOnScaleGestureListener(
        private val player: PlayerView
    ) : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var scaleFactor = 0f

        override fun onScale(
            detector: ScaleGestureDetector
        ): Boolean {
            scaleFactor = detector.scaleFactor
            return true
        }

        override fun onScaleBegin(
            detector: ScaleGestureDetector
        ): Boolean {
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (scaleFactor > 1) {
                player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                player.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    }
}
