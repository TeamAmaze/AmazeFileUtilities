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
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.files.FileFilter
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.databinding.VideoPlayerActivityBinding
import com.amaze.fileutilities.home_page.CustomToolbar
import com.amaze.fileutilities.utilis.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.MimeTypes
import com.google.common.collect.ImmutableList
import java.io.File
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

        if (intent != null) {
            val pos = intent.getLongExtra(
                VideoPlayerActivity.VIDEO_PLAYBACK_POSITION,
                0L
            )
            videoPlayerViewModel?.playbackPosition = pos
        }
        player = ExoPlayer.Builder(this).build()
        viewBinding.videoView.player = player
        initMediaItem()
        initializePlayer()
        refactorPlayerController(
            !(
                videoPlayerViewModel?.isInPictureInPicture ?: false ||
                    videoPlayerViewModel?.isUiLocked ?: false
                )
        )
        refactorSystemUi(
            resources.configuration.orientation
                == Configuration.ORIENTATION_LANDSCAPE
        )
        mAudioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        getScreenSize()
        if (!isDialogActivity()) {
            viewBinding.videoView.setOnTouchListener(this)
            viewBinding.volumeHintParent.bringToFront()
            viewBinding.brightnessHintParent.bringToFront()
            viewBinding.lockUi.bringToFront()
        }
    }

    override fun onPause() {
        super.onPause()
        savePlayerState()
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
                when {
                    viewBinding.videoView.isControllerVisible -> {
                        viewBinding.videoView.hideController()
                    }
                    videoPlayerViewModel?.isUiLocked == true -> {
                        viewBinding.lockUi.showFade(300)
                        object : CountDownTimer(3000, 3000) {
                            override fun onTick(millisUntilFinished: Long) {
                                // do nothing
                            }

                            override fun onFinish() {
                                viewBinding.lockUi.hideFade(300)
                            }
                        }.start()
                        return true
                    }
                    else -> {
                        viewBinding.videoView.showController()
                    }
                }

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
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
                if (videoPlayerViewModel?.isUiLocked == true) {
                    return true
                }
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
                        if (!viewBinding.videoView.isControllerVisible &&
                            videoPlayerViewModel?.isUiLocked == false
                        ) {
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
        refactorPlayerController(
            !(
                videoPlayerViewModel?.isInPictureInPicture ?: false ||
                    videoPlayerViewModel?.isUiLocked ?: false
                )
        )
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
                    intent.putExtra(
                        VideoPlayerActivity.VIDEO_PLAYBACK_POSITION,
                        player?.currentPosition
                    )
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
            customToolbar.addActionButton(resources.getDrawable(R.drawable.ic_outline_lock_24)) {
                videoPlayerViewModel?.isUiLocked = true
                refactorPlayerController(
                    !(
                        videoPlayerViewModel?.isInPictureInPicture ?: false ||
                            videoPlayerViewModel?.isUiLocked ?: false
                        )
                )
            }
            videoView.findViewById<FrameLayout>(R.id.exo_fullscreen_button).visibility = View.GONE
            videoView.findViewById<ImageView>(R.id.fit_to_screen).visibility = View.VISIBLE
            videoView.findViewById<ImageView>(R.id.pip_video_player).visibility = View.VISIBLE
            viewBinding.lockUi.setOnClickListener {
                videoPlayerViewModel?.let {
                    viewModel ->
                    viewModel.isUiLocked = false
                    refactorPlayerController(
                        !(
                            videoPlayerViewModel?.isInPictureInPicture ?: false ||
                                videoPlayerViewModel?.isUiLocked ?: false
                            )
                    )
                    viewBinding.lockUi.hideFade(300)
                }
            }
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
                    R.id.playback_speed -> {
                        showPlaybackSpeedDialog()
                    }
                    R.id.subtitles -> {
                        showSubtitlePopup()
                    }
                }
                true
            }
        }
    }

    private fun showSubtitlePopup() {
        val customToolbar = viewBinding.videoView
            .findViewById<ConstraintLayout>(R.id.top_bar_video_player) as CustomToolbar
        setupSubtitlePopup {
            item ->
            when (item!!.itemId) {
                R.id.sync_subtitles -> {
                    val popupWindow = setupSyncSubtitlesPopupWindow()
                    popupWindow.showAsDropDown(customToolbar.getOverflowButton())
                }
                R.id.open_subtitles -> {
                    val filter: FileFilter = {
                        it.isDirectory ||
                            it.name.endsWith(".srt", true)
                    }
                    this.showFileChooserDialog(filter) {
                        saveStateAndSetSubtitleFile(it)
                    }
                }
                R.id.enable_subtitles -> {
                    item.setChecked(!item.isChecked)
                    videoPlayerViewModel?.isSubtitleEnabled = item.isChecked
                    if (item.isChecked) {
                        videoPlayerViewModel?.subtitleFilePath?.let {
                            filePath ->
                            saveStateAndSetSubtitleFile(File(filePath))
                        }
                    } else {
                        saveStateAndSetSubtitleFile(null)
                    }
                }
            }
            true
        }
    }

    private fun saveStateAndSetSubtitleFile(file: File?) {
        savePlayerState()
        setMediaItemWithSubtitle(file)
        initializePlayer()
    }

    private fun setupSyncSubtitlesPopupWindow(): PopupWindow {
        val inflater =
            applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.subtitle_sync_menu_item, null)

        val popup = PopupWindow(
            view, ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popup.setBackgroundDrawable(BitmapDrawable())
        popup.isOutsideTouchable = true
        return popup
    }

    private fun setupSubtitlePopup(onMenuItemClickListener: PopupMenu.OnMenuItemClickListener) {
        val customToolbar = viewBinding.videoView
            .findViewById<ConstraintLayout>(R.id.top_bar_video_player) as CustomToolbar
        val overflowContext = ContextThemeWrapper(this, R.style.custom_action_mode_dark)
        val popupMenu = PopupMenu(
            overflowContext, customToolbar.getOverflowButton()
        )
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            onMenuItemClickListener.onMenuItemClick(item)
        }
        popupMenu.inflate(R.menu.video_subtitle_popup)
        popupMenu.menu.findItem(R.id.enable_subtitles).let {
            item ->
            item.isVisible =
                videoPlayerViewModel?.isSubtitleAvailable == true
            item.setChecked(videoPlayerViewModel?.isSubtitleEnabled == true)
        }
        popupMenu.menu.findItem(R.id.sync_subtitles).let {
            item ->
            item.isVisible =
                videoPlayerViewModel?.isSubtitleAvailable == true
            item.setChecked(videoPlayerViewModel?.isSubtitleEnabled == true)
        }
        popupMenu.show()
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
                    videoPlayerViewModel?.playbackSpeed = i.key
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
        player?.videoFormat?.let {
            dialogMessage += "\n${resources.getString(R.string.video)}\n---\n"
            dialogMessage += "${resources.getString(R.string.width)}: " +
                "${it.width}" + "\n"
            dialogMessage += "${resources.getString(R.string.height)}: " +
                "${it.height}" + "\n"
            dialogMessage += "${resources.getString(R.string.codecs)}: " +
                "${it.codecs}" + "\n"
            dialogMessage += "${resources.getString(R.string.frame_rate)}: " +
                "${it.frameRate}" + "\n"
            dialogMessage += "${resources.getString(R.string.bitrate)}: " +
                "${it.averageBitrate}" + "\n"
            dialogMessage += "${resources.getString(R.string.channel)}: " +
                "${it.channelCount}" + "\n"
            dialogMessage += "${resources.getString(R.string.mime_type)}: " +
                "${it.containerMimeType}" + "\n"
            dialogMessage += "${resources.getString(R.string.sample_rate)}: " +
                "${it.sampleRate}" + "\n"
        }
        player?.audioFormat?.let {
            dialogMessage += "\n${resources.getString(R.string.audio)}\n---\n"
            "${it.containerMimeType}" + "\n"
            dialogMessage += "${resources.getString(R.string.codecs)}: " +
                "${it.codecs}" + "\n"
            dialogMessage += "${resources.getString(R.string.sample_rate)}: " +
                "${it.sampleRate}" + "\n"
            dialogMessage += "${resources.getString(R.string.bitrate)}: " +
                "${it.averageBitrate}" + "\n"
            dialogMessage += "${resources.getString(R.string.channel)}: " +
                "${it.channelCount}" + "\n"
            dialogMessage += "${resources.getString(R.string.mime_type)}: " +
                "${it.containerMimeType}" + "\n"
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
        setMediaItemWithSubtitle(videoPlayerViewModel?.videoModel?.uri?.getFileFromUri(this))
    }

    private fun setMediaItemWithSubtitle(subtitleFile: File?) {
        val mediaItemBuilder = MediaItem.Builder().setUri(videoPlayerViewModel?.videoModel!!.uri)
        if (subtitleFile == null) {
            mediaItemBuilder.setSubtitleConfigurations(Collections.emptyList())
        } else {
            val filePath = subtitleFile.path
            filePath.let {
                path ->
                val srt = path.substring(0, path.lastIndexOf(".")) + ".srt"
                val srtFile = File(srt)
                if (srtFile.exists()) {
                    Log.i(javaClass.simpleName, "Found srt file with name $srt")
                    val subtitleConfig = SubtitleConfiguration.Builder(Uri.fromFile(srtFile))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP) // The correct MIME type (required).
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT) // Selection flags for the track (optional).
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(ImmutableList.of(subtitleConfig))
                    videoPlayerViewModel?.isSubtitleAvailable = true
                    videoPlayerViewModel?.isSubtitleEnabled = true
                    videoPlayerViewModel?.subtitleFilePath = srtFile.path
                }
            }
        }

        player?.setMediaItem(mediaItemBuilder.build())
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
                it.videoView.setControllerVisibilityListener {
                    view ->
                    if (view == View.VISIBLE) {
                        it.videoView.showController()
                    } else {
                        it.videoView.hideController()
                    }
                }
            }
        }
    }

    private fun savePlayerState() {
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
                val param = PlaybackParameters(videoPlayerViewModel?.playbackSpeed ?: 1f)
                exoPlayer.playbackParameters = param
                exoPlayer.play()
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
