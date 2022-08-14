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
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.PermissionsActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityImageEditorBinding
import com.amaze.fileutilities.utilis.getFileFromUri
import com.amaze.fileutilities.utilis.showToastInCenter
import com.bumptech.glide.Glide
import ja.burhanrashid52.photoeditor.PhotoEditor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ImageEditorActivity : PermissionsActivity() {

    var log: Logger = LoggerFactory.getLogger(ImageEditorActivity::class.java)

    private lateinit var viewModel: ImageViewerViewModel
    private var photoEditor: PhotoEditor? = null
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityImageEditorBinding.inflate(layoutInflater)
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

        viewBinding.run {
            customToolbar.setBackButtonClickListener {
                onBackPressed()
            }
            customToolbar.setTitle(
                DocumentFile.fromSingleUri(
                    this@ImageEditorActivity,
                    imageModel.uri
                )?.name ?: imageModel.uri.getFileFromUri()?.name ?: getString(R.string.edit)
            )
            customToolbar.setBackButtonClickListener {
                this@ImageEditorActivity.onBackPressed()
            }
            customToolbar.addActionButton(resources.getDrawable(R.drawable.ic_round_undo_32)) {
            }
            customToolbar.addActionButton(resources.getDrawable(R.drawable.ic_round_redo_32)) {
            }
            customBottomBar.addButton(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_round_edit_32, theme
                )!!,
                resources.getString(R.string.edit)
            ) {
            }
        }

        photoEditor = PhotoEditor.Builder(this, viewBinding.photoEditorView)
            .setPinchTextScalable(true)
            .setClipSourceImage(true)
            .build()
        photoEditor?.run {
            setBrushDrawingMode(true)
        }
        showImage(imageModel)
    }

    private fun showImage(localTypeModel: LocalImageModel) {
        log.info(
            "Edit image in fragment ${localTypeModel.uri.path} " +
                "and mimetype ${localTypeModel.mimeType}"
        )

        Glide.with(this).load(localTypeModel.uri.toString())
            .thumbnail(
                Glide.with(this)
                    .load(
                        resources.getDrawable(R.drawable.ic_outline_image_32)
                    )
            ).into(viewBinding.photoEditorView.source)
    }

    /*override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }*/
}
