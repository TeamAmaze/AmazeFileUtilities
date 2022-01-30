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
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysisDao
import com.amaze.fileutilities.home_page.database.MediaFilesAnalysisDao
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.invalidate
import kotlinx.coroutines.Dispatchers
import java.io.File

class AnalyseViewModel : ViewModel() {

    fun getBlurImages(dao: MediaFilesAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao, true))
        }
    }

    fun getMemeImages(dao: MediaFilesAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao, false))
        }
    }

    fun getDuplicateDirectories(dao: InternalStorageAnalysisDao):
        LiveData<List<List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformInternalStorageAnalysisToMediaFileList(dao))
        }
    }

    fun getEmptyFiles(dao: InternalStorageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformInternalStorageAnalysisToMediaFile(dao))
        }
    }

    private fun transformInternalStorageAnalysisToMediaFile(dao: InternalStorageAnalysisDao):
        List<MediaFileInfo> {
        val analysis = dao.getAllEmptyFiles()
        val response = analysis.filter {
            it.invalidate(dao)
        }.map {
            MediaFileInfo.fromFile(
                File(it.files[0]),
                MediaFileInfo.ExtraInfo(
                    MediaFileInfo.MEDIA_TYPE_IMAGE,
                    null, null, null
                )
            )
        }
        return response
    }

    private fun transformInternalStorageAnalysisToMediaFileList(dao: InternalStorageAnalysisDao):
        List<List<MediaFileInfo>> {
        val analysis = dao.getAll()
        val response = analysis.filter {
            it.invalidate(dao)
        }.filter {
            it.files.size > 1
        }.map {
            it.files.map {
                filePath ->
                MediaFileInfo.fromFile(
                    File(filePath),
                    MediaFileInfo.ExtraInfo(
                        MediaFileInfo.MEDIA_TYPE_IMAGE,
                        null, null, null
                    )
                )
            }
        }
        return response
    }

    private fun transformAnalysisToMediaFileInfo(
        dao: MediaFilesAnalysisDao,
        requiredBlur: Boolean
    ):
        List<MediaFileInfo> {
        val analysis = if (requiredBlur) dao.getAllBlur() else dao.getAllMeme()
        val response = analysis.filter {
            it.invalidate(dao)
        }.map {
            MediaFileInfo.fromFile(
                File(it.filePath),
                MediaFileInfo.ExtraInfo(
                    MediaFileInfo.MEDIA_TYPE_IMAGE,
                    null, null, null
                )
            )
        }
        return response
    }
}
