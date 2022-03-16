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

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.databinding.VideoPlayerActivityBinding
import com.amaze.fileutilities.home_page.CustomToolbar
import com.amaze.fileutilities.utilis.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

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
    private val verticalSwipeThreshold = 75
    private val horizontalSwipeThreshold = 150
    private var onStopCalled = false

    private var mAudioManager: AudioManager? = null
    private var mAttrs: AudioAttributes? = null

    private var localVideoModel: LocalVideoModel? = null

    abstract fun isDialogActivity(): Boolean

    private var player: ExoPlayer? = null
    private var videoPlayerViewModel: VideoPlayerActivityViewModel? = null
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlayerActivityBinding.inflate(layoutInflater)
    }

    private val pipIntentFilter = IntentFilter().apply {
        addAction(ACTION_PLAY)
        addAction(ACTION_FORWARD)
        addAction(ACTION_BACKGROUND)
    }

    companion object {
        const val ACTION_FORWARD = "action_forward"
        const val ACTION_PLAY = "action_play"
        const val ACTION_BACKGROUND = "action_background"
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

    override fun onStop() {
        super.onStop()
        onStopCalled = true
    }

    override fun onResume() {
        super.onResume()
        onStopCalled = false
        pipActionsReceiver.also { receiver ->
            registerReceiver(receiver, pipIntentFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        pipActionsReceiver.also { receiver ->
            unregisterReceiver(receiver)
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
                    // calculate horizontal swipe millis seek forward
                    gestureSkipStepMs = ((player?.duration!! * 5.0) / sWidth).toInt()
                }
                if (viewBinding.videoView.isControllerVisible) {
                    viewBinding.videoView.hideController()
                } else {
                    viewBinding.videoView.showController()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
                val x2 = event.x
                val y2 = event.y
                diffX = ceil((event.x - downX).toDouble()).toLong()
                diffY = ceil((event.y - downY).toDouble()).toLong()
                if (abs(diffY) > abs(diffX) && abs(diffY) > verticalSwipeThreshold) {
                    if (intLeft) {
                        // if left its for brightness
                        invalidateBrightness(downY > y2)
                    } else if (intRight) {
                        // if right its for audio
                        invalidateVolume(downY > y2)
                    }
                } else if (abs(diffY) < abs(diffX) && abs(diffX) > horizontalSwipeThreshold) {
                    player?.currentPosition?.let {
                        if (downX < x2) {
                            player?.seekTo(it + gestureSkipStepMs)
                        } else {
                            player?.seekTo(it - gestureSkipStepMs)
                        }
                        if (!viewBinding.videoView.isControllerVisible) {
                            viewBinding.videoView.showController()
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
        refactorPlayerController(!(videoPlayerViewModel?.isInPictureInPicture ?: false))
        if (!isInPictureInPictureMode && onStopCalled) {
            finish()
        }
    }

    fun initLocalVideoModel(intent: Intent) {
        val mimeType = intent.type
        val videoUri = intent.data
        if (videoUri == null) {
            showToastInCenter(resources.getString(R.string.unsupported_content))
        }
        Log.i(
            javaClass.simpleName,
            "Loading video from path ${videoUri?.path} " +
                "and mimetype $mimeType"
        )
        localVideoModel = LocalVideoModel(uri = videoUri!!, mimeType = mimeType)
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
                    )
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(
                        videoPlayerViewModel?.videoModel?.uri,
                        videoPlayerViewModel?.videoModel?.mimeType
                    )
                    if (!videoPlayerViewModel?.videoModel?.uri?.authority
                        .equals(packageName, true)
                    ) {
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            val customToolbar = videoView
                .findViewById<ConstraintLayout>(R.id.top_bar_video_player) as CustomToolbar
            customToolbar.visibility = View.VISIBLE
            videoView.findViewById<FrameLayout>(R.id.exo_fullscreen_button).visibility = View.GONE
            videoView.findViewById<ImageView>(R.id.fit_to_screen).visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.pip_video_player).visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.lock_ui)
                .visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.fit_to_screen)
                .setOnClickListener {
                    videoPlayerViewModel?.fitToScreen?.also {
                        if (!it) {
                            viewBinding.videoView.resizeMode =
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            videoPlayerViewModel?.fitToScreen = true
                        } else {
                            viewBinding.videoView.resizeMode =
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            videoPlayerViewModel?.fitToScreen = false
                        }
                    }
                }
            videoView.findViewById<ImageView>(R.id.pip_video_player).setOnClickListener {
                enterPIPMode()
            }
            customToolbar.setBackButtonClickListener {
                onBackPressed()
            }
            val fileName = videoPlayerViewModel?.videoModel?.uri?.getFileFromUri(
                this@BaseVideoPlayerActivity
            )?.name
            customToolbar.setTitle(fileName ?: "")
            customToolbar.setOverflowPopup(R.menu.video_activity) { item ->
                when (item!!.itemId) {
                    R.id.info -> {
                        showInfoDialog()
                    }
                    R.id.aspect_ratio -> {
                    }
                    R.id.playback_speed -> {
                        showPlaybackSpeedDialog()
                    }
                    R.id.subtitles -> {
                    }
                }
                true
            }
            videoView.setControllerVisibilityListener {
                refactorSystemUi(it == View.GONE)
            }
        }
    }

    private fun showPlaybackSpeedDialog() {
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it)
        }
        val items = arrayOf(
            "0.25x", "0.50x", "0.75x",
            "1.0x " +
                "(${resources.getString(R.string.default_name)})",
            "1.25x", "1.50x", "1.75x", "2.0x"
        )
        val itemsMap = mapOf(
            Pair(0.25f, 0),
            Pair(0.50f, 1),
            Pair(0.75f, 2),
            Pair(1.0f, 3),
            Pair(1.25f, 4),
            Pair(1.50f, 5),
            Pair(1.75f, 6),
            Pair(2.0f, 7),
        )
        val checkedItem = itemsMap[player?.playbackParameters?.speed] ?: 3
        builder.setSingleChoiceItems(
            items, checkedItem
        ) { dialog, which ->
            for (i in itemsMap.entries) {
                if (i.value == which) {
                    val param = PlaybackParameters(i.key)
                    player?.playbackParameters = param
                    break
                }
            }
            player?.play()
            dialog.dismiss()
        }
            .setTitle(R.string.playback_speed)
            .setNegativeButton(R.string.close) { dialog, _ ->
                player?.play()
                dialog.dismiss()
            }.show()
        player?.pause()
    }

    private fun showInfoDialog() {
        var dialogMessage = ""
        val uri = videoPlayerViewModel?.videoModel?.uri
        uri?.let {
            val file = uri.getFileFromUri(this)
            file?.let {
                dialogMessage += "${resources.getString(R.string.file)}\n---\n"
                dialogMessage += "${resources.getString(R.string.name)}: ${file.name}" + "\n"
                dialogMessage += "${resources.getString(R.string.file_path)}: ${file.path}" + "\n"
                dialogMessage += "${resources.getString(R.string.last_modified)}: " +
                    Date(file.lastModified()) + "\n"
            }
        }
        player?.mediaMetadata?.let {
            dialogMessage += "\n${resources.getString(R.string.media)}\n---\n"
            dialogMessage += "${resources.getString(R.string.description)}: " +
                "${it.description}" + "\n"
            dialogMessage += "${resources.getString(R.string.recording_date)}: ${it.artist}" + "\n"
            dialogMessage += "${resources.getString(R.string.release_date)}: " +
                "${it.releaseMonth}/${it.releaseYear}" + "\n"
            dialogMessage += "${resources.getString(R.string.recording_date)}: " +
                "${it.recordingMonth}/${it.recordingYear}" + "\n"
        }
        player?.videoFormat?.let {
            dialogMessage += "\n${resources.getString(R.string.video)}\n---\n"
            dialogMessage += "${resources.getString(R.string.width)}: " +
                "${it.width}" + "\n"
            dialogMessage += "${resources.getString(R.string.height)}: " +
                "${it.height}" + "\n"
            dialogMessage += "${resources.getString(R.string.bitrate)}: " +
                "${it.averageBitrate}" + "\n"
            dialogMessage += "${resources.getString(R.string.channel)}: " +
                "${it.channelCount}" + "\n"
            dialogMessage += "${resources.getString(R.string.frame_rate)}: " +
                "${it.frameRate}" + "\n"
            dialogMessage += "${resources.getString(R.string.mime_type)}: " +
                "${it.containerMimeType}" + "\n"
            dialogMessage += "${resources.getString(R.string.codecs)}: " +
                "${it.codecs}" + "\n"
            dialogMessage += "${resources.getString(R.string.sample_rate)}: " +
                "${it.sampleRate}" + "\n"
        }
        player?.audioFormat?.let {
            dialogMessage += "\n${resources.getString(R.string.audio)}\n---\n"
            dialogMessage += "${resources.getString(R.string.bitrate)}: " +
                "${it.averageBitrate}" + "\n"
            dialogMessage += "${resources.getString(R.string.channel)}: " +
                "${it.channelCount}" + "\n"
            dialogMessage += "${resources.getString(R.string.mime_type)}: " +
                "${it.containerMimeType}" + "\n"
            dialogMessage += "${resources.getString(R.string.codecs)}: " +
                "${it.codecs}" + "\n"
            dialogMessage += "${resources.getString(R.string.sample_rate)}: " +
                "${it.sampleRate}" + "\n"
        }
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it)
        }
        builder.setMessage(dialogMessage)
            .setTitle(R.string.information)
            .setNegativeButton(R.string.close) { dialog, _ ->
                player?.play()
                dialog.dismiss()
            }.show()
        player?.pause()
    }

    private val pipActionsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action

            action.also {
                when (action) {
                    ACTION_PLAY ->
                        player?.apply {
                            if (isPlaying) {
                                pause()
                            } else {
                                play()
                            }
                        }
                    ACTION_FORWARD ->
                        player?.seekForward()
                    ACTION_BACKGROUND -> {
                        videoPlayerViewModel?.videoModel?.uri?.let {
                            uri ->
                            AudioPlayerService.runService(
                                uri,
                                null, this@BaseVideoPlayerActivity
                            )
                            finish()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun releasePlayer() {
        player?.run {
            videoPlayerViewModel?.also {
                it.playbackPosition = this.currentPosition
                it.currentWindow = this.currentWindowIndex
                it.playWhenReady = this.playWhenReady
            }
            release()
        }
    }

    // For N devices that support it, not "officially"
    @Suppress("DEPRECATION")
    private fun enterPIPMode() {
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
                params.setActions(
                    listOf(
                        getBackgroundPlayAction(), getPlayAction(),
                        getForwardPlayAction()
                    )
                )
                val width = player?.videoFormat?.width ?: 16
                val height = player?.videoFormat?.height ?: 9
                params.setAspectRatio(Rational.parseRational("${abs(width)}:${abs(height)}"))
                params.setSourceRectHint(viewBinding.videoView.clipBounds)

                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
        }
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
        viewBinding.brightnessProgress.max = 100
        viewBinding.brightnessProgress.progress = (currentBrightness * 100).toInt()
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
        viewBinding.volumeProgress.max = maxVolume
        viewBinding.volumeProgress.progress = currentVolume
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
            videoPlayerViewModel?.videoModel = localVideoModel
        }
        if (videoPlayerViewModel?.videoModel == null) {
            this.showToastInCenter(resources.getString(R.string.unsupported_operation))
            return
        }
        val mediaItem = MediaItem.fromUri(videoPlayerViewModel?.videoModel!!.uri)
        player?.setMediaItem(mediaItem)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getPlayAction(): RemoteAction {
        val intent = Intent(ACTION_PLAY)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(
            Icon.createWithResource(
                this,
                if (player?.isPlaying != false) R.drawable.ic_round_pause_circle_32
                else R.drawable.ic_round_play_circle_32
            ),
            "Play", "", pendingIntent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getForwardPlayAction(): RemoteAction {
        val intent = Intent(ACTION_FORWARD)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(
            Icon.createWithResource(
                this,
                R.drawable.ic_outline_fast_forward_32
            ),
            "Forward", "", pendingIntent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getBackgroundPlayAction(): RemoteAction {
        val intent = Intent(ACTION_BACKGROUND)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteAction(
            Icon.createWithResource(
                this,
                R.drawable.ic_round_headphones_24
            ),
            "Background", "", pendingIntent
        )
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
                    } else {
                        it.videoView.hideController()
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
                initializeAttributes()
                exoPlayer.setAudioAttributes(mAttrs!!, true)
                exoPlayer.playWhenReady = it.playWhenReady
                exoPlayer.seekTo(it.currentWindow, it.playbackPosition)
                exoPlayer.prepare()
            }

            /*val mediaSession = MediaSessionCompat(this, this.packageName)
            val mediaSessionConnector = MediaSessionConnector(mediaSession)
            mediaSessionConnector.setPlayer(player)
            mediaSession.isActive = true*/
        }
    }

    private fun initializeAttributes() {
        mAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()
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
}
