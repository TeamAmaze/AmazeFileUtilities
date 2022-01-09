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

import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R

class BaseVideoPlayerActivity : PermissionsActivity() {

    private fun releasePlayer() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
        if (fragment is VideoPlayerFragment) {

            /*}
        exoPlayer?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            playWhenReady = this.playWhenReady
            release()
        }
        exoPlayer = null*/
        }
    }

    private fun initializePlayer() {
        /*exoPlayer?.let {
                exoPlayer ->
//            exoPlayer.playWhenReady = playWhenReady
//            exoPlayer.seekTo(currentWindow, playbackPosition)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }*/
    }

    override fun onStart() {
        super.onStart()
//        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
//            hideSystemUi()
//        initializePlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}
