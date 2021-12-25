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
import android.view.MotionEvent
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.databinding.GenericPagerViewerActivityBinding
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import java.io.File
import java.util.*

class VideoPlayerActivity : PermissionActivity() {

    private lateinit var viewModel: VideoPlayerViewModel
    private var videoPlayerAdapter: VideoPlayerAdapter? = null

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        GenericPagerViewerActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel = ViewModelProvider(this).get(VideoPlayerViewModel::class.java)

        val videoModel = intent.extras?.getParcelable<LocalVideoModel>(
            VideoPlayerFragment.VIEW_TYPE_ARGUMENT
        )
        viewModel.getSiblingVideoModels(
            videoModel!!,
            videoModel.uri.getSiblingUriFiles(this)
        ).let {
            videoPlayerAdapter = VideoPlayerAdapter(
                supportFragmentManager,
                lifecycle, it ?: Collections.singletonList(videoModel),
                viewModel.playerFragmentMap
            )
            viewBinding.pager.adapter = videoPlayerAdapter
            if (it != null) {
                var position = 0
                if (it.size > 1) {
                    for (i in it.indices) {
                        if (File(it[i].uri.path).name.equals(File(videoModel.uri.path).name)) {
                            position = i
                            break
                        }
                    }
                }
                viewBinding.pager.currentItem = position
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        /*event?.pointerCount?.also {
            if (it > 1) {
                videoPlayerAdapter?.playerFragmentMap?.get(viewBinding
                    .pager.currentItem)?.get()?.scaleGestureDetector?.onTouchEvent(event)
                return false
            }
        }*/
        return if (event?.pointerCount!! > 1) {
            viewModel.playerFragmentMap[
                viewBinding
                    .pager.currentItem
            ]?.get()?.scaleGestureDetector?.onTouchEvent(event)!!
            this.viewBinding.pager.isUserInputEnabled = false
            false
        } else {
            this.viewBinding.pager.isUserInputEnabled = true
            super.onTouchEvent(event)
        }
    }
}
