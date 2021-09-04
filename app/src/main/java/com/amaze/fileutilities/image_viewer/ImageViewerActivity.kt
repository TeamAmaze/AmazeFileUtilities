package com.amaze.fileutilities.image_viewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.amaze.fileutilities.R
import java.util.*
import kotlin.collections.ArrayList

class ImageViewerActivity : AppCompatActivity(R.layout.image_viewer_activity) {

    private lateinit var mPager: ViewPager2
    private lateinit var viewModel: ImageViewerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ImageViewerViewModel::class.java)

        val imageModel = intent.extras?.getParcelable<LocalImageModel>(ImageViewerFragment.VIEW_TYPE_ARGUMENT)
        mPager = findViewById(R.id.pager)
        viewModel.getSiblingImageModels(imageModel!!).let {
            val pagerAdapter = ImageViewerAdapter(supportFragmentManager,
                lifecycle, it ?: Collections.singletonList(imageModel)
            )
            mPager.adapter = pagerAdapter
        }
    }
}