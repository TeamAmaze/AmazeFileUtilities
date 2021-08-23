package com.amaze.fileutilities.image_viewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.amaze.fileutilities.R

class ImageViewerActivity : AppCompatActivity(R.layout.image_viewer_activity) {

    private lateinit var mPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val imageModel = intent.extras?.getParcelable<LocalImageModel>(ImageViewerFragment.VIEW_TYPE_ARGUMENT)
            mPager = findViewById(R.id.pager)
            val pagerAdapter = ImageViewerAdapter(supportFragmentManager, lifecycle, imageModel!!)
            mPager.adapter = pagerAdapter
        }
    }
}