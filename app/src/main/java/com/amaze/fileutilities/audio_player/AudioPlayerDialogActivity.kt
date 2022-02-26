/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding
import com.amaze.fileutilities.utilis.*
import com.google.android.material.slider.Slider
import com.masoudss.lib.WaveformSeekBar
import java.lang.ref.WeakReference

class AudioPlayerDialogActivity : PermissionsActivity(), IAudioPlayerInterfaceHandler {

    private val _binding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: AudioPlayerInterfaceHandlerViewModel
    private lateinit var audioPlaybackServiceConnection: ServiceConnection

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC

        val intent = Intent(this, AudioPlayerService::class.java)
        this.bindService(intent, audioPlaybackServiceConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        this.unbindService(audioPlaybackServiceConnection)
    }

    override fun onStop() {
        super.onStop()
        if (!viewModel.isPlaying) {
            AudioPlayerService.sendCancelBroadcast(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)
        viewModel = ViewModelProvider(this)
            .get(AudioPlayerInterfaceHandlerViewModel::class.java)
        val sharedPrefs = getAppCommonSharedPreferences()
        viewModel.forceShowSeekbar = !sharedPrefs.getBoolean(
            PreferencesConstants.KEY_ENABLE_WAVEFORM,
            PreferencesConstants.DEFAULT_AUDIO_PLAYER_WAVEFORM
        )
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val audioUri = intent.data
            if (audioUri == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
            }
            Log.i(
                javaClass.simpleName,
                "Loading audio from path ${audioUri?.path} " +
                    "and mimetype $mimeType"
            )
            viewModel.uriList = ArrayList(
                audioUri!!.getSiblingUriFiles(this)!!.filter {
                    it.isAudioMimeType()
                }.asReversed()
            )
            AudioPlayerService.runService(audioUri, viewModel.uriList, this)
        }
        title = getString(R.string.amaze_audio_player)
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
    }

    override fun getSeekbar(): Slider {
        return _binding.seekBar
    }

    override fun getWaveformSeekbar(): WaveformSeekBar {
        return _binding.waveformSeekbar
    }

    override fun getTimeElapsedTextView(): TextView {
        return _binding.timeElapsed
    }

    override fun getTrackLengthTextView(): TextView {
        return _binding.trackLength
    }

    override fun getContextWeakRef(): WeakReference<Context> {
        return WeakReference(this)
    }

    override fun getAudioPlayerHandlerViewModel(): AudioPlayerInterfaceHandlerViewModel {
        return viewModel
    }

    override fun getTitleTextView(): TextView {
        return _binding.title
    }

    override fun getAlbumTextView(): TextView {
        return _binding.album
    }

    override fun getArtistTextView(): TextView {
        return _binding.artist
    }

    override fun getPlayButton(): ImageView {
        return _binding.playButton
    }

    override fun getPrevButton(): ImageView {
        return _binding.prevButton
    }

    override fun getNextButton(): ImageView {
        return _binding.nextButton
    }

    override fun getShuffleButton(): ImageView {
        return _binding.shuffleButton
    }

    override fun getRepeatButton(): ImageView {
        return _binding.repeatButton
    }

    override fun serviceDisconnected() {
        // do nothing
    }
}
