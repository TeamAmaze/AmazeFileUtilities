/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.image_viewer

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.GenericPagerViewerActivityBinding
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog
import com.amaze.fileutilities.utilis.showToastInCenter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ImageViewerActivity : PermissionsActivity() {

    var log: Logger = LoggerFactory.getLogger(ImageViewerActivity::class.java)

    private lateinit var viewModel: ImageViewerViewModel
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        GenericPagerViewerActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel = ViewModelProvider(this).get(ImageViewerViewModel::class.java)

        val mimeType = intent.type
        val imageUri = intent.data
        if (imageUri == null) {
            showToastInCenter(resources.getString(R.string.unsupported_content))
            return
        }
        log.info(
            "Loading image from path ${imageUri.path} " +
                "and mimetype $mimeType"
        )

        val imageModel = LocalImageModel(uri = imageUri, mimeType = mimeType)
        viewModel.processSiblingImageModels(imageModel)
        viewModel.siblingImagesLiveData.observe(this) {
            val dialog = showProcessingDialog(
                layoutInflater,
                getString(R.string.please_wait)
            ).create()
            if (it == null) {
                dialog.show()
            } else {
                dialog.dismiss()
                val pagerAdapter = ImageViewerAdapter(
                    supportFragmentManager,
                    lifecycle, it
                )
                viewBinding.pager.adapter = pagerAdapter
                var position = 0
                if (it.size > 1) {
                    for (i in it.indices) {
                        // TODO: avoid using file
                        if (File(it[i].uri.path!!).name.equals(File(imageModel.uri.path!!).name)) {
                            position = i
                            break
                        }
                    }
                }
                viewBinding.pager.currentItem = position
            }
        }
    }

    fun getViewpager(): ViewPager2 {
        return viewBinding.pager
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }*/
}
