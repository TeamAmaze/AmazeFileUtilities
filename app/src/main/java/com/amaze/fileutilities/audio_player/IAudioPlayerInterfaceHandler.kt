/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.audio_player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.executeAsyncTask
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.getFileFromUri
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.log
import com.amaze.fileutilities.utilis.px
import com.amaze.fileutilities.utilis.showFade
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.slider.Slider
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.launch
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
    fun getPlaybackPropertiesButton(): ImageView?
    fun getContextWeakRef(): WeakReference<Context>
    fun getAudioPlayerHandlerViewModel(): AudioPlayerInterfaceHandlerViewModel
    fun layoutInflater(): LayoutInflater
    fun getLogger(): Logger
    fun getIsWaveformProcessing(): Boolean
    fun setIsWaveformProcessing(bool: Boolean): Unit
    // used to transition between 2 colors
    fun getLastColor(): Int
    // used to transition between 2 colors
    fun setLastColor(lastColor: Int)

    fun animateCurrentPlayingItem(playingUri: Uri)

    override fun onPositionUpdate(progressHandler: AudioProgressHandler) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar &&
            getWaveformSeekbar()?.isVisible == true
        ) {
            getWaveformSeekbar()?.let {
                waveformSeekBar ->
                waveformSeekBar.maxProgress = progressHandler.audioPlaybackInfo.duration
                    .toFloat()
                waveformSeekBar.progress = progressHandler
                    .audioPlaybackInfo.currentPosition.toFloat()
                    .coerceAtMost(waveformSeekBar.maxProgress)
            }
        }
        invalidateActionButtons(progressHandler)
    }

    override fun onPlaybackStateChanged(
        progressHandler: AudioProgressHandler,
        renderWaveform: Boolean
    ) {
        invalidateActionButtons(progressHandler)
        // invalidate wavebar
        if (renderWaveform) {
            loadWaveFormSeekbar(progressHandler, false)
            progressHandler.audioPlaybackInfo.audioModel.getUri().let {
                animateCurrentPlayingItem(it)
            }
            getContextWeakRef().get()?.let {
                getAlbumImage()?.let {
                    imageView ->
                    var glide = Glide.with(it).load(progressHandler.audioPlaybackInfo.albumArt)
                        .centerCrop()
                        .transform(CenterCrop(), RoundedCorners(80.px.toInt()))
                        .fallback(R.drawable.ic_outline_audio_file_32)
                        .placeholder(R.drawable.ic_outline_audio_file_32)
                    val paletteEnabled = it.getAppCommonSharedPreferences()
                        .getBoolean(
                            PreferencesConstants.KEY_ENABLE_AUDIO_PALETTE,
                            PreferencesConstants.DEFAULT_PALETTE_EXTRACT
                        )
                    if (paletteEnabled) {
                        glide = glide.addListener(paletteListener)
                    }
                    glide.into(imageView)
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
                var glide = Glide.with(it).load(audioService?.getAudioPlaybackInfo()?.albumArt)
                    .centerCrop()
                    .transform(CenterCrop(), RoundedCorners(80.px.toInt()))
                    .fallback(R.drawable.ic_outline_audio_file_32)
                    .placeholder(R.drawable.ic_outline_audio_file_32)
                val paletteEnabled = it.getAppCommonSharedPreferences()
                    .getBoolean(
                        PreferencesConstants.KEY_ENABLE_AUDIO_PALETTE,
                        PreferencesConstants.DEFAULT_PALETTE_EXTRACT
                    )
                if (paletteEnabled) {
                    glide = glide.addListener(paletteListener)
                }
                glide.into(imageView)
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
            getPlaybackPropertiesButton()?.setOnClickListener {
                getContextWeakRef().get()?.let {
                    context ->
                    val playbackParameters = audioService.getPlaybackParameters()
                    // required because conversion from pitch to semitones doesn't match slider steps
                    val defaultSemitones =
                        context.getAppCommonSharedPreferences()
                            .getFloat(
                                PreferencesConstants.KEY_PLAYBACK_SEMITONES,
                                PreferencesConstants.DEFAULT_PLAYBACK_SEMITONES
                            )
                    Utils.showPlaybackPropertiesDialog(
                        context,
                        layoutInflater(),
                        playbackParameters?.speed ?: 1f,
                        defaultSemitones,
                        {
                            playbackSpeed, pitch ->
                            audioService.invokePlaybackProperties(playbackSpeed, pitch)
                            audioService.invokePlayPausePlayer()
                        }, {
                        audioService.invokePlayPausePlayer()
                    }
                    ).show()
                    audioService.invokePlayPausePlayer()
                }
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
            audioService.getAudioProgressHandlerCallback()
                ?.audioPlaybackInfo?.audioModel?.getUri()?.let {
                    uri ->
                    animateCurrentPlayingItem(uri)
                }
        }
        setupSeekBars(audioService, false)
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
                    setParentBackground(color)
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
                getContextWeakRef().get()?.let {
                    context ->
                    resource?.let {
                        getAudioPlayerHandlerViewModel().getPaletteColor(
                            it,
                            context.getColor(R.color.navy_blue_alt_3)
                        )
                            .observe(this@IAudioPlayerInterfaceHandler) {
                                color ->
                                if (color != null) {
                                    setParentBackground(color)
                                }
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

    private fun setParentBackground(color: Int) {
        val evaluator = ArgbEvaluatorCompat.getInstance()
        if (getLastColor() != 0 && getLastColor() != color) {
            var i = 0f
            while (i <= 1.0f) {
                val finalColor = evaluator.evaluate(i, getLastColor(), color)
                getParentView()?.background?.setColorFilter(
                    finalColor,
                    PorterDuff.Mode.SRC_ATOP
                )
                i += 0.0005f
            }
        } else {
            getParentView()?.background?.setColorFilter(
                color,
                PorterDuff.Mode.SRC_ATOP
            )
        }
        setLastColor(color)
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

            getTimeElapsedTextView()?.text = AudioUtils.getReadableDurationString(
                progressHandler
                    .audioPlaybackInfo.currentPosition
            ) ?: ""
            getTrackLengthTextView()?.text = AudioUtils.getReadableDurationString(
                progressHandler
                    .audioPlaybackInfo.duration
            ) ?: ""
        }
    }

    private fun setSeekbarProgress(progress: Int) {
        getSeekbar()?.value = progress.toFloat().coerceAtMost(
            getSeekbar()?.valueTo ?: 0f
        )
        getSeekbarSmall()?.progress = progress.toFloat().coerceAtMost(
            getSeekbarSmall()?.max ?: 0f
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar &&
            getWaveformSeekbar()?.isVisible == true
        ) {
            getWaveformSeekbar()?.visibility = View.VISIBLE
            getSeekbar()?.visibility = View.GONE
            getWaveformSeekbar()?.progress = progress.toFloat()
        } else {
            getSeekbar()?.visibility = View.VISIBLE
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

    private fun loadWaveFormSeekbar(
        progressHandler: AudioProgressHandler,
        forceShowSeekbar: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar && !forceShowSeekbar
        ) {
            getContextWeakRef().get()?.let {
                context ->
                val file = progressHandler.audioPlaybackInfo
                    .audioModel.getUri().getFileFromUri()
                if (file != null) {
                    lifecycleScope.launch {
                        try {
                            // TODO: hack to get valid wavebar path
                            /*if (!file.path.startsWith("/storage")) {
                                file = File("storage/" + file.path)
                            }*/
                            this.executeAsyncTask<Void, IntArray?>({}, {
                                if (!getIsWaveformProcessing()) {
                                    setIsWaveformProcessing(true)
                                    CustomWaveformOptions.getSampleFrom(context, file.path)
                                } else {
                                    null
                                }
                            }, {
                                sample ->
                                setIsWaveformProcessing(false)
                                if (sample != null) {
                                    setSampleAndShowWaveformSeekbar(sample)
                                } else {
                                    getLogger().warn("failed to fetch sample waves for audio")
//                                    getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                                    loadWaveFormSeekbar(progressHandler, true)
                                }
                            }, {})
                        } catch (fe: FileNotFoundException) {
                            getLogger().warn("file not found for waveform, force seekbar", fe)
//                            getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                            loadWaveFormSeekbar(progressHandler, true)
                        } catch (e: Exception) {
                            getLogger().warn("waveform seekbar exception, force seekbar", e)
                            getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                            loadWaveFormSeekbar(progressHandler, true)
                        }
                    }
                } else {
//                    getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                    loadWaveFormSeekbar(progressHandler, true)
                }
            }
        } else {
            getSeekbar()?.showFade(300)
            getWaveformSeekbar()?.hideFade(300)
        }
    }

    private fun setupSeekBars(audioService: ServiceOperationCallback?, forceShowSeekbar: Boolean) {
        val valueTo = audioService?.getAudioPlaybackInfo()?.duration?.toFloat()
        getSeekbar()?.valueTo = valueTo?.coerceAtLeast(0.1f) ?: 0f
        getSeekbarSmall()?.max = valueTo?.coerceAtLeast(0.1f) ?: 0f
        getWaveformSeekbar()?.maxProgress = audioService
            ?.getAudioPlaybackInfo()?.duration?.toFloat() ?: 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            !getAudioPlayerHandlerViewModel().forceShowSeekbar && !forceShowSeekbar
        ) {
            getContextWeakRef().get().let {
                context ->
                if (context != null) {
                    val file = audioService?.getAudioProgressHandlerCallback()?.audioPlaybackInfo
                        ?.audioModel?.getUri()?.getFileFromUri()
                    if (file != null) {
                        lifecycleScope.launch {
                            try {
                                // TODO: hack to get valid wavebar path
                                /*if (!file.path.startsWith("/storage")) {
                                    file = File("storage/" + file.path)
                                }*/
                                this.executeAsyncTask<Void, IntArray?>({}, {
                                    if (!getIsWaveformProcessing()) {
                                        setIsWaveformProcessing(true)
                                        CustomWaveformOptions.getSampleFrom(context, file.path)
                                    } else {
                                        null
                                    }
                                }, { sample ->
                                    setIsWaveformProcessing(false)
                                    if (sample != null) {
                                        setSampleAndShowWaveformSeekbar(sample)
                                    } else {
                                        getLogger().warn("failed to fetch sample waves for audio")
//                                        getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                                        setupSeekBars(audioService, true)
                                    }
                                }, {})
                            } catch (fe: FileNotFoundException) {
                                getLogger().warn("file not found for waveform, setup seekbar", fe)
//                                getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                                setupSeekBars(audioService, true)
                            } catch (e: Exception) {
                                getLogger().warn("waveform seekbar exception, force seekbar", e)
                                getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                                setupSeekBars(audioService, true)
                            }
                        }
                    } else {
//                        getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                        setupSeekBars(audioService, true)
                        return
                    }
                } else {
//                    getAudioPlayerHandlerViewModel().forceShowSeekbar = true
                    setupSeekBars(audioService, true)
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
            getSeekbar()?.showFade(300)
            getWaveformSeekbar()?.hideFade(300)
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

    private fun setSampleAndShowWaveformSeekbar(sample: IntArray) {
        getWaveformSeekbar()?.sample = sample
        if (!getAudioPlayerHandlerViewModel().forceShowSeekbar) {
            getSeekbar()?.cancelPendingInputEvents()
            getWaveformSeekbar()?.showFade(300)
            getSeekbar()?.hideFade(200)
        }
    }
}
