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

import android.os.Bundle

class VideoPlayerActivity : BaseVideoPlayerActivity() {

    companion object {
        const val VIEW_TYPE_ARGUMENT = "videoPlayerUri"
    }

    private var localVideoModel: LocalVideoModel? = null

    override fun getVideoModel(): LocalVideoModel? {
        return localVideoModel
    }

    override fun isDialogActivity(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        localVideoModel = intent.extras?.getParcelable(
            VIEW_TYPE_ARGUMENT
        )
        super.onCreate(savedInstanceState)
        handleVideoPlayerActivityResources()
    }
}
