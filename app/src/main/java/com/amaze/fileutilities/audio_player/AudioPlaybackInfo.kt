package com.amaze.fileutilities.audio_player

data class AudioPlaybackInfo(var audioModel: AudioModel, var duration: Int,
                             var currentPosition: Int, var playbackState: Int,
                             var title: String = "test", var artistName:String = "",
                             var albumName: String = "", var year: Long = 1970L)