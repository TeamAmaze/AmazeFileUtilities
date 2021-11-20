package com.amaze.fileutilities.video_player

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.GenericPagerViewerActivityBinding
import com.amaze.fileutilities.databinding.VideoPlayerDialogActivityBinding
import java.io.File
import java.util.*

class VideoPlayerActivity: PermissionActivity() {

    private lateinit var viewModel: VideoPlayerViewModel

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        GenericPagerViewerActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel = ViewModelProvider(this).get(VideoPlayerViewModel::class.java)

        val videoModel = intent.extras?.getParcelable<LocalVideoModel>(VideoPlayerFragment.VIEW_TYPE_ARGUMENT)
        viewModel.getSiblingVideoModels(videoModel!!).let {
            val pagerAdapter = VideoPlayerAdapter(supportFragmentManager,
                lifecycle, it ?: Collections.singletonList(videoModel)
            )
            viewBinding.pager.adapter = pagerAdapter
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
}