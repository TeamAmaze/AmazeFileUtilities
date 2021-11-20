package com.amaze.fileutilities.video_player

import androidx.fragment.app.FragmentContainerView
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R

class BaseVideoPlayerActivity: PermissionActivity() {

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