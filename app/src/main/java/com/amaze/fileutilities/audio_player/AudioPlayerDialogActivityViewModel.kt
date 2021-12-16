package com.amaze.fileutilities.audio_player

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isAudioMimeType

class AudioPlayerDialogActivityViewModel : ViewModel() {
    private var localAudioModelList: ArrayList<LocalAudioModel>? = null
    var uriList: ArrayList<Uri>? = null

    fun getSiblingAudioModels(videoModel: LocalAudioModel, uriList: ArrayList<Uri>?): ArrayList<LocalAudioModel>? {
        if (localAudioModelList == null) {
            uriList.run {
                localAudioModelList = ArrayList()
                if (this != null) {
                    localAudioModelList?.addAll(this.filter { it.isAudioMimeType() }
                        .map { LocalAudioModel(-1, it, "") }.asReversed())
                } else {
                    localAudioModelList?.add(videoModel)
                }
            }
        }
        return localAudioModelList!!
    }
}