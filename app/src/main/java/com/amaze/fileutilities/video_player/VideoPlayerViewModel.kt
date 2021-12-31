/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.video_player

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amaze.fileutilities.utilis.isVideoMimeType
import java.lang.ref.WeakReference

class VideoPlayerViewModel : ViewModel() {
    private var localVideoModelList: ArrayList<LocalVideoModel>? = null
    val playerFragmentMap = mutableMapOf<Int, WeakReference<VideoPlayerFragment>>()

    fun getSiblingVideoModels(videoModel: LocalVideoModel, uriList: ArrayList<Uri>?):
        ArrayList<LocalVideoModel>? {
        if (localVideoModelList == null) {
            uriList.run {
                localVideoModelList = ArrayList()
                if (this != null) {
                    localVideoModelList?.addAll(
                        this.filter { it.isVideoMimeType() }
                            .map { LocalVideoModel(it, "") }.asReversed()
                    )
                } else {
                    localVideoModelList?.add(videoModel)
                }
            }
        }
        return localVideoModelList
    }
}
