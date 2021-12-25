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
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.amaze.fileutilities.PermissionActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ImageViewerDialogActivityBinding

class ImageViewerDialogActivity : PermissionActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ImageViewerDialogActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        if (savedInstanceState == null) {
            val mimeType = intent.type
            val imageUri = intent.data
            Log.i(
                javaClass.simpleName,
                "Loading image from path ${imageUri?.path} " +
                    "and mimetype $mimeType"
            )
            val bundle = bundleOf(
                ImageViewerFragment.VIEW_TYPE_ARGUMENT
                    to LocalImageModel(uri = imageUri!!, mimeType = mimeType!!)
            )
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<ImageViewerFragment>(R.id.fragment_container_view, args = bundle)
            }
        }
    }
}
