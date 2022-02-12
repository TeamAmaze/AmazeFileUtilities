/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.isAudioMimeType

class AudioPlayerInterfaceHandlerViewModel : ViewModel() {
    private var localAudioModelList: ArrayList<LocalAudioModel>? = null
    var uriList: ArrayList<Uri>? = null
    // approx value if player is playing
    var isPlaying: Boolean = true
    var forceShowSeekbar = false

    fun getSiblingAudioModels(
        videoModel: LocalAudioModel,
        uriList: ArrayList<Uri>?
    ): ArrayList<LocalAudioModel>? {
        if (localAudioModelList == null) {
            uriList.run {
                localAudioModelList = ArrayList()
                if (this != null) {
                    localAudioModelList?.addAll(
                        this.filter { it.isAudioMimeType() }
                            .map { LocalAudioModel(-1, it, "") }.asReversed()
                    )
                } else {
                    localAudioModelList?.add(videoModel)
                }
            }
        }
        return localAudioModelList!!
    }
}
