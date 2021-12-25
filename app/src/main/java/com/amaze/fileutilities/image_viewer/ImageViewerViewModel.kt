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

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.isImageMimeType

class ImageViewerViewModel : ViewModel() {
    private var imageModelList: ArrayList<LocalImageModel>? = null

    fun getSiblingImageModels(imageModel: LocalImageModel, uriList: ArrayList<Uri>?):
        ArrayList<LocalImageModel>? {
            if (imageModelList == null) {
                uriList.run {
                    imageModelList = ArrayList()
                    if (this != null) {
                        imageModelList?.addAll(
                            this.filter { it.isImageMimeType() }
                                .map { LocalImageModel(it, "") }.asReversed()
                        )
                    } else {
                        imageModelList?.add(imageModel)
                    }
                }
            }
            return imageModelList
        }
}
