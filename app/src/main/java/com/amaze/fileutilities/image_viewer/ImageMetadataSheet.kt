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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.amaze.fileutilities.databinding.ImageMetadataSheetBinding
import com.drew.imaging.ImageMetadataReader
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class ImageMetadataSheet() : BottomSheetDialogFragment() {

    private var localImageModel: LocalImageModel? = null
    private var imageMetadataSheetBinding: ImageMetadataSheetBinding? = null
    private val viewBinding get() = imageMetadataSheetBinding!!

    companion object {
        const val KEY_LOCAL_IMAGE = "image"

        fun showMetadata(localImageModel: LocalImageModel, fragmentManager: FragmentManager) {
            val metadataSheet = newInstance(localImageModel)
            metadataSheet.show(fragmentManager, javaClass.simpleName)
        }

        private fun newInstance(localImageModel: LocalImageModel): ImageMetadataSheet {
            val args = Bundle()
            val fragment = ImageMetadataSheet()
            args.putParcelable(KEY_LOCAL_IMAGE, localImageModel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localImageModel = arguments?.getParcelable(KEY_LOCAL_IMAGE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageMetadataSheetBinding = ImageMetadataSheetBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageMetadataSheetBinding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localImageModel?.let {
            var metadata = ImageMetadataReader.readMetadata(File(it.uri.path))
            var result = ""
            metadata.directories.forEach { directory ->
                directory.tags.forEach {
                    tag ->
                    result += tag.description
                    result += "\n"
                }
                result += "\n\n"
            }
            viewBinding.metadata.text = result
        }
    }
}
