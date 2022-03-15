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

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Rational
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.VideoPlayerActivityBinding
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.showFade
import com.amaze.fileutilities.utilis.showToastInCenter
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

abstract class BaseVideoPlayerActivity : PermissionsActivity(), View.OnTouchListener {

    private var intLeft = false
    private var intRight = false
    private var sWidth = 0
    private var sHeight = 0
    private var diffX = 0L
    private var diffY = 0L
    private var downX = 0f
    private var downY = 0f
    private var currentBrightness = 0.5f
    private var size: Point? = null
    private var deviceDisplay: Display? = null
    private var gestureSkipStepMs: Int = 200
    private val gestureThreshold = 75

    private var mAudioManager: AudioManager? = null

    abstract fun getVideoModel(): LocalVideoModel?

    abstract fun isDialogActivity(): Boolean

    private var player: ExoPlayer? = null
    private var videoPlayerViewModel: VideoPlayerActivityViewModel? = null
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlayerActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        videoPlayerViewModel = ViewModelProvider(this).get(VideoPlayerActivityViewModel::class.java)

        player = ExoPlayer.Builder(this).build()
        viewBinding.videoView.player = player
        initMediaItem()
        initializePlayer()
        refactorPlayerController(!(videoPlayerViewModel?.isInPictureInPicture ?: false))
        mAudioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        getScreenSize()
        if (!isDialogActivity()) {
            viewBinding.videoView.setOnTouchListener(this)
        }
    }

    override fun onPause() {
        super.onPause()
        pausePlayer()
    }

    /*override fun onResume() {
        super.onResume()
        initializePlayer()
    }*/

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun getScreenSize() {
        deviceDisplay = windowManager.defaultDisplay
        size = Point()
        deviceDisplay?.getSize(size)
        sWidth = size!!.x
        sHeight = size!!.y
    }

    private fun initMediaItem() {
        if (videoPlayerViewModel?.videoModel == null) {
            videoPlayerViewModel?.videoModel = getVideoModel()
        }
        if (videoPlayerViewModel?.videoModel == null) {
            this.showToastInCenter(resources.getString(R.string.unsupported_operation))
            return
        }
        val mediaItem = MediaItem.fromUri(videoPlayerViewModel?.videoModel!!.uri)
        player?.setMediaItem(mediaItem)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isDialogActivity()) {
            enterPIPMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        videoPlayerViewModel?.isInPictureInPicture = isInPictureInPictureMode
    }

    fun handleViewPlayerDialogActivityResources() {
        viewBinding.run {
            videoView.findViewById<ConstraintLayout>(R.id.top_bar_video_player)
                .visibility = View.GONE
            videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
                .visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
                .setOnClickListener {
                    val intent = Intent(
                        this@BaseVideoPlayerActivity,
                        VideoPlayerActivity::class.java
                    ).apply {
                        putExtra(
                            VideoPlayerActivity.VIEW_TYPE_ARGUMENT,
                            videoPlayerViewModel?.videoModel
                        )
                    }
                    startActivity(intent)
                    finish()
                }
            videoView.setShowNextButton(false)
            videoView.setShowPreviousButton(false)
        }
    }

    fun handleVideoPlayerActivityResources() {
        viewBinding.run {
            videoView.updateLayoutParams {
                width = FrameLayout.LayoutParams.MATCH_PARENT
                height = FrameLayout.LayoutParams.MATCH_PARENT
            }
            videoView.findViewById<ConstraintLayout>(R.id.top_bar_video_player)
                .visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
                .visibility = View.GONE
            videoView.findViewById<ImageView>(R.id.fit_to_screen_video_player)
                .setOnClickListener {
                    /*viewModel?.fitToScreen?.also {
                        if (!it) {
                            viewBinding.videoView.resizeMode =
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            viewModel?.fitToScreen = true
                        } else {
                            viewBinding.videoView.resizeMode =
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            viewModel?.fitToScreen = false
                        }
                    }*/
                    enterPIPMode()
                }
            videoView.findViewById<ImageView>(R.id.orientation_video_player)
                .setOnClickListener {
                    videoPlayerViewModel?.fullscreen?.also {
                        if (!it) {
                            this@BaseVideoPlayerActivity.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            videoPlayerViewModel?.fullscreen = true
                        } else {
                            this@BaseVideoPlayerActivity.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            videoPlayerViewModel?.fullscreen = false
                        }
                    }
                }
            videoView.findViewById<ImageView>(R.id.back_video_player)
                .setOnClickListener {
                    onBackPressed()
                }
            videoView.setControllerVisibilityListener {
                refactorSystemUi(it == View.GONE)
            }
        }
    }

    fun releasePlayer() {
        player?.run {
            videoPlayerViewModel?.also {
                it.playbackPosition = this.currentPosition
                it.currentWindow = this.currentWindowIndex
                it.playWhenReady = this.playWhenReady
            }
            release()
        }
    }

    private fun refactorPlayerController(doShow: Boolean) {
        viewBinding.let {
            if (!doShow) {
                it.videoView.hideController()
                it.videoView.useController = false
                it.videoView.setControllerVisibilityListener { visibility ->
                    if (visibility == View.VISIBLE) {
                        it.videoView.hideController()
                    }
                }
            } else {
                it.videoView.showController()
                it.videoView.useController = true
                it.videoView.setControllerVisibilityListener { visibility ->
                    if (visibility == View.VISIBLE) {
                        it.videoView.showController()
                    }
                }
            }
        }
    }

    private fun pausePlayer() {
        player?.run {
            videoPlayerViewModel?.also {
                it.playbackPosition = this.currentPosition
                it.currentWindow = this.currentWindowIndex
                it.playWhenReady = this.playWhenReady
//                this.playWhenReady = false
//                this.pause()
            }
        }
    }

    private fun initializePlayer() {
        player?.let {
            exoPlayer ->
            videoPlayerViewModel?.also {
                exoPlayer.playWhenReady = it.playWhenReady
                exoPlayer.seekTo(it.currentWindow, it.playbackPosition)
                exoPlayer.prepare()
            }

            val mediaSession = MediaSessionCompat(this, this.packageName)
            val mediaSessionConnector = MediaSessionConnector(mediaSession)
            mediaSessionConnector.setPlayer(player)
            mediaSession.isActive = true
        }
    }

    private fun refactorSystemUi(hide: Boolean) {
        if (hide) {
            WindowInsetsControllerCompat(
                this.window,
                viewBinding.root
            ).let {
                controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowInsetsControllerCompat(
                this.window,
                viewBinding.root
            ).let {
                controller ->
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_BARS_BY_TOUCH
            }
        }
    }

    // For N devices that support it, not "officially"
    @Suppress("DEPRECATION")
    fun enterPIPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            this.packageManager
                .hasSystemFeature(
                        PackageManager.FEATURE_PICTURE_IN_PICTURE
                    )
        ) {
            videoPlayerViewModel?.playbackPosition = player?.currentPosition ?: 0
            viewBinding.videoView.useController = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                /*requireActivity().setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational.parseRational("1:2.39"))
                        .setSourceRectHint(sourceRectHint)
                        .setAutoEnterEnabled(true)
                        .build()
                )*/
                val params = PictureInPictureParams.Builder()
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    params.setAspectRatio(Rational.parseRational("3:+6"))
                }
                params.setSourceRectHint(viewBinding.videoView.clipBounds)

                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                // touch is start
                downX = event.x
                downY = event.y
                if (event.x < sWidth / 2) {

                    // here check touch is screen left side
                    intLeft = true
                    intRight = false
                    viewBinding.brightnessHintParent.showFade(300)
                } else if (event.x > sWidth / 2) {

                    // here check touch is screen right side
                    intLeft = false
                    intRight = true
                    viewBinding.volumeHintParent.showFade(300)
                }

                if (gestureSkipStepMs == 200) {
                    gestureSkipStepMs = ((player?.duration!! * 5.0) / sWidth).toInt()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {

                // finger move to screen
                val x2 = event.x
                val y2 = event.y
                diffX = Math.ceil((event.x - downX).toDouble()).toLong()
                diffY = Math.ceil((event.y - downY).toDouble()).toLong()
                if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > gestureThreshold) {
                    if (intLeft) {
                        // if left its for brightness
                        invalidateBrightness(downY > y2)
                    } else if (intRight) {
                        // if right its for audio
                        invalidateVolume(downY > y2)
                    }
                } else if (Math.abs(diffY) < Math.abs(diffX)) {
                    player?.currentPosition?.let {
                        if (downX < x2) {
                            player?.seekTo(it + gestureSkipStepMs)
                        } else {
                            player?.seekTo(it - gestureSkipStepMs)
                        }
                    }
                }

                if (event.action == MotionEvent.ACTION_UP) {
                    viewBinding.volumeHintParent.hideFade(300)
                    viewBinding.brightnessHintParent.hideFade(300)
                }
            }
        }
        return true
    }

    private fun invalidateBrightness(doIncrease: Boolean) {
        if (doIncrease) {
            if (currentBrightness <= 0.993f) {
                currentBrightness += 0.007f
            }
        } else {
            if (currentBrightness >= 0.007f) {
                currentBrightness -= 0.007f
            }
        }
        val layout = window.attributes
        layout.screenBrightness = currentBrightness
        window.attributes = layout
        viewBinding.brightnessProgress.setMax(100)
        viewBinding.brightnessProgress.setProgress((currentBrightness * 100).toInt())
    }

    private fun invalidateVolume(doIncrease: Boolean) {
        if (doIncrease) {
            mAudioManager!!.adjustVolume(
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND
            )
        } else {
            mAudioManager!!.adjustVolume(
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_PLAY_SOUND
            )
        }
        val currentVolume: Int = mAudioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume: Int = mAudioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        viewBinding.volumeProgress.setMax(maxVolume)
        viewBinding.volumeProgress.setProgress(currentVolume)
    }
}
