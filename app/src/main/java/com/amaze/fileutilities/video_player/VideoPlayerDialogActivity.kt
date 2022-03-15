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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.showToastInCenter

class VideoPlayerDialogActivity : BaseVideoPlayerActivity() {

    private var localVideoModel: LocalVideoModel? = null

    override fun getVideoModel(): LocalVideoModel? {
        return localVideoModel
    }

    override fun isDialogActivity(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initLocalVideoModel(intent)
        super.onCreate(savedInstanceState)
        handleViewPlayerDialogActivityResources()
    }

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            initLocalVideoModel(it)
        }
    }*/

    private fun initLocalVideoModel(intent: Intent) {
        val mimeType = intent.type
        val videoUri = intent.data
        if (videoUri == null) {
            showToastInCenter(resources.getString(R.string.unsupported_content))
        }
        Log.i(
            javaClass.simpleName,
            "Loading video from path ${videoUri?.path} " +
                "and mimetype $mimeType"
        )
        localVideoModel = LocalVideoModel(uri = videoUri!!, mimeType = mimeType)
    }
}
