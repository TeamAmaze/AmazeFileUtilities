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

import androidx.lifecycle.ViewModel

class VideoPlayerActivityViewModel : ViewModel() {

    var playWhenReady = false
    var currentWindow = 0
    var playbackPosition = 0L
    var videoModel: LocalVideoModel? = null
    var fullscreen = false
    var fitToScreen = false
    var isInPictureInPicture = false
    var playbackSpeed = 1f
}
