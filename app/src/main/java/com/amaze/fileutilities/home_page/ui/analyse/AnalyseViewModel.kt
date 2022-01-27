/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import androidx.lifecycle.*
import com.amaze.fileutilities.home_page.database.MediaFilesAnalysisDao
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import kotlinx.coroutines.Dispatchers
import java.io.File

class AnalyseViewModel : ViewModel() {

    fun getBlurImages(dao: MediaFilesAnalysisDao): LiveData<MutableList<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao, true))
        }
    }

    fun getMemeImages(dao: MediaFilesAnalysisDao): LiveData<MutableList<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao, false))
        }
    }

    private fun transformAnalysisToMediaFileInfo(
        dao: MediaFilesAnalysisDao,
        requiredBlur: Boolean
    ):
        MutableList<MediaFileInfo> {
        val analysis = dao.getAll()
        val response = analysis.filter {
            val file = File(it.filePath)
            if (!file.exists()) {
                dao.delete(it)
                false
            } else true
        }.filter { if (requiredBlur) it.isBlur else it.isMeme }.map {
            MediaFileInfo.fromFile(
                File(it.filePath),
                MediaFileInfo.ExtraInfo(
                    MediaFileInfo.MEDIA_TYPE_IMAGE,
                    null, null, null
                )
            )
        }
        return ArrayList(response)
    }
}
