package com.amaze.fileutilities.video_player

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isVideoMimeType
import java.lang.ref.WeakReference

class VideoPlayerViewModel : ViewModel() {
    private var localVideoModelList: ArrayList<LocalVideoModel>? = null
    val playerFragmentMap = mutableMapOf<Int, WeakReference<VideoPlayerFragment>>()

    fun getSiblingVideoModels(videoModel: LocalVideoModel, uriList: ArrayList<Uri>?): ArrayList<LocalVideoModel>? {
        if (localVideoModelList == null) {
            uriList.run {
                localVideoModelList = ArrayList()
                if (this != null) {
                    localVideoModelList?.addAll(this.filter { it.isVideoMimeType() }
                        .map { LocalVideoModel(it, "") }.asReversed())
                } else {
                    localVideoModelList?.add(videoModel)
                }
            }
        }
        return localVideoModelList
    }
}