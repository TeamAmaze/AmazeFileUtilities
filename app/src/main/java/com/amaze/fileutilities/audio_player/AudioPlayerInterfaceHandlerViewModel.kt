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

package com.amaze.fileutilities.audio_player

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getSiblingUriFiles
import com.amaze.fileutilities.utilis.isAudioMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioPlayerInterfaceHandlerViewModel : ViewModel() {
    private var localAudioModelList: ArrayList<LocalAudioModel>? = null
//    var uriList: ArrayList<Uri>? = null
    // approx value if player is playing
    var isPlaying: Boolean = false
    var forceShowSeekbar = false

    companion object {
        const val WAVEFORM_THRESHOLD_BYTES = 50000000L
    }

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

    var siblingsLiveData = MutableLiveData<ArrayList<Uri>?>()

    fun processSiblings(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            /*withContext(Dispatchers.Main) {
                siblingImagesLiveData.value = null
            }*/
            if (siblingsLiveData.value.isNullOrEmpty()) {
                val uriList = ArrayList<Uri>()
                uri.getSiblingUriFiles { it.isAudioMimeType() }.run {
                    if (this != null) {
                        uriList.addAll(
                            this.asReversed()
                        )
                    } else {
                        uriList.add(uri)
                    }
                }
                siblingsLiveData.postValue(uriList)
            }
        }
    }

    fun getPaletteColor(drawable: Drawable, fallbackColor: Int): LiveData<Int?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val bitmap = drawable.toBitmap()
            val color = Utils.getColorDark(
                Utils.generatePalette(bitmap),
                fallbackColor
            )
            emit(color)
        }
    }
}
