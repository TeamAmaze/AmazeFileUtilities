package com.amaze.fileutilities.video_player

import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.MediaItem

class VideoPlayerFragmentViewModel: ViewModel() {

    var playWhenReady = false
    var currentWindow = 0
    var playbackPosition = 0L
    var videoModel: LocalVideoModel? = null
    var fullscreen = false
    var fitToScreen = false
}