package com.amaze.fileutilities.image_viewer

import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import java.io.File
import java.util.*

class ImageViewerActivity : PermissionActivity(R.layout.image_viewer_activity) {

    private lateinit var mPager: ViewPager2
    private lateinit var viewModel: ImageViewerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ImageViewerViewModel::class.java)

        val imageModel = intent.extras?.getParcelable<LocalImageModel>(ImageViewerFragment.VIEW_TYPE_ARGUMENT)
        mPager = findViewById(R.id.pager)
        triggerPermissionCheck()
        viewModel.getSiblingImageModels(imageModel!!).let {
            val pagerAdapter = ImageViewerAdapter(supportFragmentManager,
                lifecycle, it ?: Collections.singletonList(imageModel)
            )
            mPager.adapter = pagerAdapter
            if (it != null) {
                var position = 0
                for (i in it.indices) {
                    if (File(it[i].uri.path).name.equals(File(imageModel.uri.path).name)) {
                        position = i
                        break
                    }
                }
                mPager.currentItem = position
            }
        }
    }

    private fun triggerPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkStoragePermission()) {
            buildExplicitPermissionAlertDialog ({
                startExplicitPermissionActivity()
            }, {
                // do nothing
            }).show()
        }
    }
}