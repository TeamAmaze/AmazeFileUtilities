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
import android.widget.RelativeLayout
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px

class VideoPlayerDialogActivity : BaseVideoPlayerActivity() {

    override fun isDialogActivity(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initLocalVideoModel(intent)
        super.onCreate(savedInstanceState)
        handleViewPlayerDialogActivityResources()
        findViewById<RelativeLayout>(R.id.video_parent).setPadding(
            16.px.toInt(), 16.px.toInt(),
            16.px.toInt(), 16.px.toInt()
        )
    }

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            initLocalVideoModel(it)
        }
    }*/
}
