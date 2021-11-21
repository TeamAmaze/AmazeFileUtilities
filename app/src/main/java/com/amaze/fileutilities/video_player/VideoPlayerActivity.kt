package com.amaze.fileutilities.video_player

import android.os.Bundle
import android.view.MotionEvent
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.databinding.GenericPagerViewerActivityBinding
import java.io.File
import java.util.*

class VideoPlayerActivity: PermissionActivity() {

    private lateinit var viewModel: VideoPlayerViewModel
    private var videoPlayerAdapter: VideoPlayerAdapter? = null

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        GenericPagerViewerActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel = ViewModelProvider(this).get(VideoPlayerViewModel::class.java)

        val videoModel = intent.extras?.getParcelable<LocalVideoModel>(VideoPlayerFragment.VIEW_TYPE_ARGUMENT)
        viewModel.getSiblingVideoModels(videoModel!!).let {
            videoPlayerAdapter = VideoPlayerAdapter(supportFragmentManager,
                lifecycle, it ?: Collections.singletonList(videoModel), viewModel.playerFragmentMap)
            viewBinding.pager.adapter = videoPlayerAdapter
            if (it != null) {
                var position = 0
                for (i in it.indices) {
                    if (File(it[i].uri.path).name.equals(File(videoModel.uri.path).name)) {
                        position = i
                        break
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
            viewModel.playerFragmentMap[viewBinding
                .pager.currentItem]?.get()?.scaleGestureDetector?.onTouchEvent(event)!!
            this.viewBinding.pager.isUserInputEnabled = false
            false
        } else {
            this.viewBinding.pager.isUserInputEnabled = true
            super.onTouchEvent(event)
        }
    }

}