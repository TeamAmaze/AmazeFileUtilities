/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
            siblingImagesLiveData.postValue(null)
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
