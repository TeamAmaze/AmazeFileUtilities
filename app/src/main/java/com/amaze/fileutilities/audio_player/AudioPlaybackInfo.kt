package com.amaze.fileutilities.audio_player

data class AudioPlaybackInfo(var audioModel: AudioModel, var duration: Int,
                             var currentPosition: Int, var playbackState: Int)