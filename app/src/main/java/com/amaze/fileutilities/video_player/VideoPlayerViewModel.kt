package com.amaze.fileutilities.video_player

import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.getSiblingUriFiles
import com.amaze.fileutilities.isVideoMimeType

class VideoPlayerViewModel : ViewModel() {
    private var localVideoModelList: ArrayList<LocalVideoModel>? = null

    fun getSiblingVideoModels(videoModel: LocalVideoModel): ArrayList<LocalVideoModel>? {
        if (localVideoModelList == null) {
            videoModel.uri.getSiblingUriFiles()?.run {
                localVideoModelList = ArrayList()
                localVideoModelList?.addAll(this.filter { it.isVideoMimeType() }
                    .map { LocalVideoModel(it, "") }.asReversed())
            }
        }
        return localVideoModelList
    }
}