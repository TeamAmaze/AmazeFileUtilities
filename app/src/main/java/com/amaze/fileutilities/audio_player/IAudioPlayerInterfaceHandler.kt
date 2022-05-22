/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.slider.Slider
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import linc.com.amplituda.exceptions.io.FileNotFoundException
import me.tankery.lib.circularseekbar.CircularSeekBar
import org.slf4j.Logger
import java.lang.ref.WeakReference
import kotlin.math.ceil

interface IAudioPlayerInterfaceHandler : OnPlaybackInfoUpdate, LifecycleOwner {
    fun getParentView(): View?
    fun getSeekbar(): Slider?
    fun getSeekbarSmall(): CircularSeekBar?
    fun getWaveformSeekbar(): WaveformSeekBar?
    fun getTimeElapsedTextView(): TextView?
    fun getTrackLengthTextView(): TextView?
    fun getTitleTextView(): TextView?
    fun getAlbumTextView(): TextView?
    fun getArtistTextView(): TextView?
    fun getPlayButton(): ImageView?
    fun getPrevButton(): ImageView?
    fun getNextButton(): ImageView?
    fun getShuffleButton(): ImageView?
    fun getRepeatButton(): ImageView?
    fun getAlbumImage(): ImageView?
    fun getAlbumSmallImage(): ImageView?
    fun getContextWeakRef(): WeakReference<Context>
    fun getAudioPlayerHandlerViewModel(): AudioPlayerInterfaceHandlerViewModel
    fun getLogger(): Logger

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
        progressHandler.audioPlaybackInfo.duration.toFloat()
        getSeekbar()?.let {
            seekbar ->
            seekbar.valueTo = progressHandler.audioPlaybackInfo.duration.toFloat()
                .coerceAtLeast(0.1f)
            seekbar.value = progressHandler.audioPlaybackInfo.currentPosition.toFloat()
                .coerceAtMost(seekbar.valueTo)
        }
        getSeekbarSmall()?.let {
            seekbar ->
            seekbar.max = progressHandler.audioPlaybackInfo.duration.toFloat()
                .coerceAtLeast(0.1f)
            seekbar.progress = progressHandler.audioPlaybackInfo.currentPosition.toFloat()
                .coerceAtMost(seekbar.max)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWaveformSeekbar()?.let {
                waveformSeekBar ->
                waveformSeekBar.maxProgress = progressHandler.audioPlaybackInfo.duration
                    .toFloat()
                waveformSeekBar.progress = progressHandler
                    .audioPlaybackInfo.currentPosition.toFloat()
                    .coerceAtMost(waveformSeekBar.maxProgress)
            }
        }
        getTimeElapsedTextView()?.text = AudioUtils.getReadableDurationString(
            progressHandler
                .audioPlaybackInfo.currentPosition
        ) ?: ""
        getTrackLengthTextView()?.text = AudioUtils.getReadableDurationString(
            progressHandler
                .audioPlaybackInfo.duration
        ) ?: ""
        invalidateActionButtons(progressHandler)
    }

    override fun onPlaybackStateChanged(
        progressHandler: AudioProgressHandler,
        renderWaveform: Boolean
    ) {
        invalidateActionButtons(progressHandler)
        // invalidate wavebar
        if (renderWaveform) {
            loadWaveFormSeekbar(progressHandler)
            getContextWeakRef().get()?.let {
                getAlbumImage()?.let {
                    imageView ->
                    Glide.with(it).load(progressHandler.audioPlaybackInfo.albumArt)
                        .centerCrop()
                        .transform(CenterCrop(), RoundedCorners(80.px.toInt()))
                        .fallback(R.drawable.ic_outline_audio_file_32)
                        .placeholder(R.drawable.ic_outline_audio_file_32)
                        .addListener(paletteListener)
                        .into(imageView)
                }
                getAlbumSmallImage()?.let {
                    imageView ->
                    Glide.with(it).load(progressHandler.audioPlaybackInfo.albumArt)
                        .transform(RoundedCorners(12.px.toInt()))
                        .fallback(R.drawable.ic_outline_audio_file_32)
                        .placeholder(R.drawable.ic_outline_audio_file_32).into(imageView)
                }
            }
        }
    }

    override fun setupActionButtons(audioServiceRef: WeakReference<ServiceOperationCallback>) {
        val audioService = audioServiceRef.get()
        getTitleTextView()?.text = audioService?.getAudioPlaybackInfo()?.title
        getAlbumTextView()?.text = audioService?.getAudioPlaybackInfo()?.albumName
        getArtistTextView()?.text = audioService?.getAudioPlaybackInfo()?.artistName
        getContextWeakRef().get()?.let {
            getAlbumImage()?.let {
                imageView ->
                Glide.with(it).load(audioService?.getAudioPlaybackInfo()?.albumArt)
                    .centerCrop()
                    .transform(CenterCrop(), RoundedCorners(80.px.toInt()))
                    .fallback(R.drawable.ic_outline_audio_file_32)
                    .placeholder(R.drawable.ic_outline_audio_file_32)
                    .addListener(paletteListener)
                    .into(imageView)
            }
            getAlbumSmallImage()?.let {
                imageView ->
                Glide.with(it).load(audioService?.getAudioPlaybackInfo()?.albumArt)
                    .transform(RoundedCorners(12.px.toInt()))
                    .fallback(R.drawable.ic_outline_audio_file_32)
                    .placeholder(R.drawable.ic_outline_audio_file_32).into(imageView)
            }
        }

        audioService?.let {
            setShuffleButton(it.getShuffle())
            setRepeatButton(it.getRepeat())
        }

        audioService?.let {
            getPlayButton()?.setOnClickListener {
                audioService.invokePlayPausePlayer()
                invalidateActionButtons(audioService.getAudioProgressHandlerCallback())
            }
            getPrevButton()?.setOnClickListener {
                getContextWeakRef()
                    .get()?.startService(retrievePlaybackAction(AudioPlayerService.ACTION_PREVIOUS))
            }
            getNextButton()?.setOnClickListener {
                getContextWeakRef()
                    .get()?.startService(retrievePlaybackAction(AudioPlayerService.ACTION_NEXT))
            }
            getShuffleButton()?.setOnClickListener {
                setShuffleButton(audioService.cycleShuffle())
            }
            getRepeatButton()?.setOnClickListener {
                setRepeatButton(audioService.cycleRepeat())
            }
        }
        setupSeekBars(audioService)
    }

    private val paletteListener: RequestListener<Drawable>
        get() = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                // do nothing
                log.warn("failed to load album", e)
                getContextWeakRef().get()?.resources?.getColor(R.color.navy_blue_alt_3)?.let {
                    color ->
                    getParentView()?.background?.setColorFilter(
                        color,
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                resource?.let {
                    getAudioPlayerHandlerViewModel().getPaletteColor(it)
                        .observe(this@IAudioPlayerInterfaceHandler) {
                            color ->
                            if (color != null) {
                                getParentView()?.background?.setColorFilter(
                                    color,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                            }
                        }
                }
                return false
            }
        }

    private fun setShuffleButton(doShuffle: Boolean) {
        getContextWeakRef().get()?.let {
            val gray = it.resources.getColor(R.color.grey_color)
            getShuffleButton()?.setImageDrawable(
                it.resources
                    .getDrawable(R.drawable.ic_round_shuffle_24)
            )
            if (!doShuffle) {
                getShuffleButton()?.setColorFilter(gray)
            } else {
                getShuffleButton()?.setColorFilter(null)
            }
        }
    }

    private fun setRepeatButton(repeatMode: Int) {
        getContextWeakRef().get()?.let {
            when (repeatMode) {
                AudioPlayerService.REPEAT_NONE -> {
                    val gray = it.resources.getColor(R.color.grey_color)
                    getRepeatButton()?.setImageDrawable(
                        it.resources
                            .getDrawable(R.drawable.ic_round_repeat_24)
                    )
                    getRepeatButton()?.setColorFilter(gray)
                }
                AudioPlayerService.REPEAT_ALL -> {
                    getRepeatButton()?.setImageDrawable(
                        it.resources
                            .getDrawable(R.drawable.ic_round_repeat_24)
                    )
                    getRepeatButton()?.setColorFilter(null)
                }
                AudioPlayerService.REPEAT_SINGLE -> {
                    getRepeatButton()?.setImageDrawable(
                        it.resources
                            .getDrawable(R.drawable.ic_round_repeat_one_24)
                    )
                    getRepeatButton()?.setColorFilter(null)
                }
                else -> {
                    getRepeatButton()?.setImageDrawable(
                        it.resources
                            .getDrawable(R.drawable.ic_round_repeat_24)
                    )
                    getRepeatButton()?.setColorFilter(null)
                }
            }
        }
    }

    private fun invalidateActionButtons(progressHandler: AudioProgressHandler?) {
        getAudioPlayerHandlerViewModel().isPlaying =
            progressHandler?.audioPlaybackInfo?.isPlaying ?: false
        progressHandler?.audioPlaybackInfo?.let {
            info ->
            if (progressHandler.isCancelled || !info.isPlaying) {
                getPlayButton()?.setImageResource(R.drawable.ic_round_play_circle_32)
            } else {
                getPlayButton()?.setImageResource(R.drawable.ic_round_pause_circle_32)
            }
            setShuffleButton(progressHandler.doShuffle)
            setRepeatButton(progressHandler.repeatMode)
            if (progressHandler.isCancelled) {
                setSeekbarProgress(0)
            }
            getTitleTextView()?.text = info.title
            getAlbumTextView()?.text = info.albumName
            getArtistTextView()?.text = info.artistName
        }
    }

    private fun setSeekbarProgress(progress: Int) {
        getSeekbar()?.value = progress.toFloat().coerceAtMost(
            getSeekbar()?.valueTo ?: 0f
        )
        getSeekbarSmall()?.progress = progress.toFloat().coerceAtMost(
            getSeekbarSmall()?.max ?: 0f
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWaveformSeekbar()?.visibility = View.VISIBLE
            getSeekbar()?.visibility = View.GONE
            getWaveformSeekbar()?.progress = progress.toFloat()
        } else {
            getSeekbar()?.visibility = View.VISIBLE
            getSeekbarSmall()?.visibility = View.VISIBLE
            getWaveformSeekbar()?.visibility = View.GONE
        }
    }

    private fun retrievePlaybackAction(action: String): Intent? {
        getContextWeakRef().get()?.let {
            val serviceName = ComponentName(it, AudioPlayerService::class.java)
            val intent = Intent(action)
            intent.component = serviceName
            return intent
        }
        return null
    }

    private fun loadWaveFormSeekbar(progressHandler: AudioProgressHandler) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar
        ) {
            scheduleWaveformSeekbarVisibility()
            getContextWeakRef().get()?.let {
                context ->
                val file = progressHandler.audioPlaybackInfo
                    .audioModel.getUri().getFileFromUri()
                if (file != null) {
                    try {
                        // TODO: hack to get valid wavebar path
                        /*if (!file.path.startsWith("/storage")) {
                            file = File("storage/" + file.path)
                        }*/
                        getWaveformSeekbar()?.setSampleFrom(file)
                    } catch (fe: FileNotFoundException) {
                        getLogger().warn("file not found for waveform, force seekbar", fe)
                        getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                    } catch (e: Exception) {
                        getLogger().warn("waveform seekbar exception, force seekbar", e)
                        getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                    }
                } else {
                    getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                }
            }
        }
    }

    private fun setupSeekBars(audioService: ServiceOperationCallback?) {
        val valueTo = audioService?.getAudioPlaybackInfo()?.duration?.toFloat()
        getSeekbar()?.valueTo = valueTo?.coerceAtLeast(0.1f) ?: 0f
        getSeekbarSmall()?.max = valueTo?.coerceAtLeast(0.1f) ?: 0f
        getWaveformSeekbar()?.maxProgress = audioService
            ?.getAudioPlaybackInfo()?.duration?.toFloat() ?: 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar
        ) {
            scheduleWaveformSeekbarVisibility()
            getContextWeakRef().get().let {
                context ->
                if (context != null) {
                    val file = audioService?.getAudioProgressHandlerCallback()?.audioPlaybackInfo
                        ?.audioModel?.getUri()?.getFileFromUri()
                    if (file != null) {
                        try {
                            getWaveformSeekbar()?.setSampleFrom(file)
                        } catch (fe: FileNotFoundException) {
                            getLogger().warn("file not found for waveform, setup seekbar", fe)
                            getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                            setupSeekBars(audioService)
                            return
                        } catch (e: Exception) {
                            getLogger().warn("waveform seekbar exception, force seekbar", e)
                            getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                            setupSeekBars(audioService)
                            return
                        }
                    } else {
                        getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                        setupSeekBars(audioService)
                        return
                    }
                } else {
                    getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                    setupSeekBars(audioService)
                    return
                }
            }

            getWaveformSeekbar()?.onProgressChanged = object : SeekBarOnProgressChanged {
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
            getSeekbar()?.visibility = View.VISIBLE
            getSeekbarSmall()?.visibility = View.VISIBLE
            getWaveformSeekbar()?.visibility = View.GONE
        }
        getSeekbar()?.addOnChangeListener(
            Slider.OnChangeListener { slider, value, fromUser ->
                if (fromUser) {
//                    mediaController.transportControls.seekTo(progress.toLong())
//                    audioService.seekPlayer(progress.toLong())
                    audioService?.invokeSeekPlayer(value.toLong())
                    val x: Int = ceil(value / 1000f).toInt()

                    if (x == 0 && audioService?.getAudioProgressHandlerCallback()
                        ?.isCancelled == true
                    ) {
                        slider.value = 0f
                    }
                }
            }
        )
    }

    private fun scheduleWaveformSeekbarVisibility() {
        getWaveformSeekbar()?.hideFade(300)
        getSeekbar()?.showFade(200)
        object : CountDownTimer(5000, 5000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                if (!getAudioPlayerHandlerViewModel().forceShowSeekbar) {
                    getSeekbar()?.cancelPendingInputEvents()
                    getWaveformSeekbar()?.showFade(300)
                    getSeekbar()?.hideFade(200)
                }
            }
        }.start()
    }
}
