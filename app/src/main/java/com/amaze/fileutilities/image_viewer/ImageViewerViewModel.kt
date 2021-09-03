package com.amaze.fileutilities.image_viewer

import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.getSiblingUriFiles
import com.amaze.fileutilities.isImageMimeType

class ImageViewerViewModel : ViewModel() {
    private lateinit var imageModelList: ArrayList<LocalImageModel>

    fun getSiblingImageModels(imageModel: LocalImageModel): ArrayList<LocalImageModel> {
        if (!this::imageModelList.isInitialized) {
            imageModelList = ArrayList()
            imageModelList.addAll(imageModel.uri.getSiblingUriFiles()
                ?.filter { it -> it.isImageMimeType() }
                ?.map { it -> LocalImageModel(it, "") }?.asReversed()!!)
        }
        return imageModelList
    }
}