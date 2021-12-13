package com.amaze.fileutilities.audio_player

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.isAudioMimeType

class AudioPlayerDialogActivityViewModel : ViewModel() {
    private var localAudioModelList: ArrayList<LocalAudioModel>? = null
    private var audioProgressHandler: AudioProgressHandler? = null

    fun getSiblingAudioModels(videoModel: LocalAudioModel, uriList: ArrayList<Uri>): ArrayList<LocalAudioModel>? {
        if (localAudioModelList == null) {
            uriList.run {
                localAudioModelList = ArrayList()
                localAudioModelList?.addAll(this.filter { it.isAudioMimeType() }
                    .map { LocalAudioModel(it, "") }.asReversed())
            }
        }
        return localAudioModelList
    }
}