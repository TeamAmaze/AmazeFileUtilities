package com.amaze.fileutilities.image_viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isImageMimeType

class ImageViewerViewModel : ViewModel() {
    private var imageModelList: ArrayList<LocalImageModel>? = null

    fun getSiblingImageModels(imageModel: LocalImageModel, uriList: ArrayList<Uri>?): ArrayList<LocalImageModel>? {
        if (imageModelList == null) {
            uriList.run {
                imageModelList = ArrayList()
                if (this != null) {
                    imageModelList?.addAll(this.filter { it.isImageMimeType() }
                        .map { LocalImageModel(it, "") }.asReversed())
                } else {
                    imageModelList?.add(imageModel)
                }
            }
        }
        return imageModelList
    }
}