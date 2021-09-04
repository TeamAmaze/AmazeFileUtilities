package com.amaze.fileutilities.image_viewer

import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.getSiblingUriFiles
import com.amaze.fileutilities.isImageMimeType

class ImageViewerViewModel : ViewModel() {
    private var imageModelList: ArrayList<LocalImageModel>? = null

    fun getSiblingImageModels(imageModel: LocalImageModel): ArrayList<LocalImageModel>? {
        if (imageModelList == null) {
            imageModel.uri.getSiblingUriFiles()?.run {
                imageModelList = ArrayList()
                imageModelList?.addAll(this.filter { it.isImageMimeType() }
                    .map { LocalImageModel(it, "") }.asReversed())
            }
        }
        return imageModelList
    }
}