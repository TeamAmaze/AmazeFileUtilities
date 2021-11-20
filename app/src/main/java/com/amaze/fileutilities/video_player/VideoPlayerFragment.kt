package com.amaze.fileutilities.video_player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.VideoPlayerFragmentBinding
import com.amaze.fileutilities.image_viewer.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.util.Util

class VideoPlayerFragment : Fragment() {

    private var player: ExoPlayer? = null
    private var viewModel: VideoPlayerFragmentViewModel? = null

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
        val videoModel = requireArguments().getParcelable<LocalVideoModel>(VIEW_TYPE_ARGUMENT)
        viewModel = ViewModelProvider(this).get(VideoPlayerFragmentViewModel::class.java)
        player = ExoPlayer.Builder(requireContext()).build().also {
            exoPlayer ->
            viewBinding.videoView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(videoModel!!.uri)
            exoPlayer.setMediaItem(mediaItem)
            initializePlayer()
        }
        if (activity is VideoPlayerDialogActivity) {
            viewBinding.root.setOnClickListener {
                activity?.finish()
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                    putExtra(VIEW_TYPE_ARGUMENT, videoModel)
                }
                startActivity(intent)
            }
            viewBinding.videoView.setShowNextButton(false)
            viewBinding.videoView.setShowPreviousButton(false)
        } else if (activity is VideoPlayerActivity) {
            hideSystemUi()
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
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(requireActivity().window, viewBinding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
}