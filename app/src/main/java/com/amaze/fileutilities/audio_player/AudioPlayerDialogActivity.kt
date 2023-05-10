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

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.AudioPlayerDialogActivityBinding
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.getDocumentFileFromUri
import com.amaze.fileutilities.utilis.showToastInCenter
import com.google.android.material.slider.Slider
import com.masoudss.lib.WaveformSeekBar
import me.tankery.lib.circularseekbar.CircularSeekBar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference

class AudioPlayerDialogActivity : PermissionsActivity(), IAudioPlayerInterfaceHandler {

    var log: Logger = LoggerFactory.getLogger(AudioPlayerDialogActivity::class.java)

    private val _binding by lazy(LazyThreadSafetyMode.NONE) {
        AudioPlayerDialogActivityBinding.inflate(layoutInflater)
    }
    private lateinit var viewModel: AudioPlayerInterfaceHandlerViewModel
    private lateinit var audioPlaybackServiceConnection: ServiceConnection
    private var isWaveformProcessing = false

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
            if (intent == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
                return
            }
            val mimeType = intent.type
            val audioUri = intent.data
            if (audioUri == null) {
                showToastInCenter(resources.getString(R.string.unsupported_content))
                return
            }
            log.info(
                "Loading audio from path ${audioUri?.path} " +
                    "and mimetype $mimeType"
            )
            viewModel.processSiblings(audioUri!!)
            viewModel.siblingsLiveData.observe(this) {
                uriList ->
                val dialog = showProcessingDialog(
                    layoutInflater,
                    getString(R.string.please_wait)
                ).create()
                if (uriList == null) {
                    dialog.show()
                } else {
                    dialog.dismiss()
                    audioUri.getDocumentFileFromUri(this)?.length()?.also {
                        if (it > AudioPlayerInterfaceHandlerViewModel.WAVEFORM_THRESHOLD_BYTES) {
                            viewModel.forceShowSeekbar = true
                        }
                    }
                    AudioPlayerService.runService(audioUri, uriList, this)
                }
            }
        }
        title = getString(R.string.amaze_audio_player)
        audioPlaybackServiceConnection =
            AudioPlaybackServiceConnection(WeakReference(this))
    }

    override fun getParentView(): View? {
        return null
    }

    override fun getSeekbar(): Slider {
        return _binding.seekBar
    }

    override fun getSeekbarSmall(): CircularSeekBar? {
        return null
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

    override fun layoutInflater(): LayoutInflater {
        return layoutInflater
    }

    override fun getLogger(): Logger {
        return log
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

    override fun getAlbumImage(): ImageView? {
        return _binding.albumImage
    }

    override fun getAlbumSmallImage(): ImageView? {
        return null
    }

    override fun getPlaybackPropertiesButton(): ImageView? {
        return null
    }

    override fun serviceDisconnected() {
        // do nothing
    }

    override fun shouldListenToUpdates(): Boolean {
        return lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    override fun getIsWaveformProcessing(): Boolean {
        return isWaveformProcessing
    }

    override fun setIsWaveformProcessing(bool: Boolean) {
        isWaveformProcessing = bool
    }

    override fun getLastColor(): Int {
        return 0
    }

    override fun setLastColor(lastColor: Int) {
        // do nothing as there is no color set to transition
    }

    override fun animateCurrentPlayingItem(playingUri: Uri) {
        // do nothing as there's nothing to animate in dialog
    }
}
