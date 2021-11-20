package com.amaze.fileutilities.video_player

import androidx.lifecycle.ViewModel

class VideoPlayerFragmentViewModel: ViewModel() {

    var playWhenReady = false
    var currentWindow = 0
    var playbackPosition = 0L
}