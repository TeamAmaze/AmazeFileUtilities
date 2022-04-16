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

import androidx.lifecycle.*
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isImageMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageViewerViewModel : ViewModel() {

    var siblingImagesLiveData = MutableLiveData<ArrayList<LocalImageModel>?>()

    fun processSiblingImageModels(imageModel: LocalImageModel) {
        viewModelScope.launch(Dispatchers.Default) {
            /*withContext(Dispatchers.Main) {
                siblingImagesLiveData.value = null
            }*/
            if (siblingImagesLiveData.value.isNullOrEmpty()) {
                imageModel.uri.getSiblingUriFiles().run {
                    val imageModelList = ArrayList<LocalImageModel>()
                    if (this != null) {
                        imageModelList.addAll(
                            this.filter { it.isImageMimeType() }
                                .map { LocalImageModel(it, "") }.asReversed()
                        )
                    } else {
                        imageModelList.add(imageModel)
                    }
                    siblingImagesLiveData.postValue(imageModelList)
                }
            }
        }
    }
}
