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
import com.amaze.fileutilities.home_page.database.*
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.invalidate
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class AnalyseViewModel : ViewModel() {

    var analysisType: Int? = null

    fun getBlurImages(dao: BlurAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllBlur(), dao))
        }
    }

    fun getLowLightImages(dao: LowLightAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllLowLight(), dao))
        }
    }

    fun getMemeImages(dao: MemeAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllMeme(), dao))
        }
    }

    fun getSleepingImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllSleeping(), dao))
        }
    }

    fun getSadImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllSad(), dao))
        }
    }

    fun getDistractedImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllDistracted(), dao))
        }
    }

    fun getSelfieImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllSelfie(), dao))
        }
    }

    fun getGroupPicImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformAnalysisToMediaFileInfo(dao.getAllGroupPic(), dao))
        }
    }

    fun getClutteredVideos(videosList: List<MediaFileInfo>): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val countIdx = IntArray(101) { 0 }
            videosList.forEach {
                it.extraInfo?.videoMetaData?.duration?.let {
                    duration ->
                    val idx = duration / 60
                    if (idx < 101) {
                        countIdx[idx.toInt()]++
                    }
                }
            }
            var maxIdxValue = 0
            var maxIdx = 0
            countIdx.forEachIndexed { index, i ->
                if (i > maxIdxValue) {
                    maxIdxValue = i
                    maxIdx = index
                }
            }
            val result = videosList.filter {
                it.extraInfo?.videoMetaData?.duration?.let {
                    duration ->
                    if ((duration / 60).toInt() == maxIdx) {
                        return@filter true
                    }
                }
                return@filter false
            }
            emit(result)
        }
    }

    fun getLargeVideos(videosList: List<MediaFileInfo>): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }
            videosList.forEachIndexed { index, mediaFileInfo ->
                if (index > 99) {
                    priorityQueue.remove()
                }
                priorityQueue.add(mediaFileInfo)
            }
            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            emit(result.reversed())
        }
    }

    fun getLargeDownloads(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }

            prefPaths.forEach {
                processFileRecursive(File(it.path), priorityQueue, MediaFileInfo.MEDIA_TYPE_UNKNOWN)
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            emit(result.reversed())
        }
    }

    fun getOldDownloads(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> -1 * o1.date.compareTo(o2.date) }

            prefPaths.forEach {
                processFileRecursive(File(it.path), priorityQueue, MediaFileInfo.MEDIA_TYPE_UNKNOWN)
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            emit(result.reversed())
        }
    }

    fun getOldScreenshots(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> -1 * o1.date.compareTo(o2.date) }

            prefPaths.forEach {
                processFileRecursive(File(it.path), priorityQueue, MediaFileInfo.MEDIA_TYPE_IMAGE)
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            emit(result.reversed())
        }
    }

    fun getOldRecordings(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_RECORDING)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> -1 * o1.date.compareTo(o2.date) }

            prefPaths.forEach {
                processFileRecursive(File(it.path), priorityQueue, MediaFileInfo.MEDIA_TYPE_AUDIO)
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            emit(result.reversed())
        }
    }

    fun getDuplicateDirectories(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ):
        LiveData<List<List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformInternalStorageAnalysisToMediaFileList(dao, searchMediaFiles, deepSearch))
        }
    }

    fun getEmptyFiles(dao: InternalStorageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            emit(transformInternalStorageAnalysisToMediaFile(dao))
        }
    }

    fun cleanImageAnalysis(
        dao: ImageAnalysisDao,
        analysisType: Int,
        checkItemsList: List<AbstractMediaFilesAdapter.ListItem>
    ):
        LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(false)
            checkItemsList.filter {
                listItem ->
                listItem.mediaFileInfo != null
            }.map {
                listItem ->
                listItem.mediaFileInfo!!.path
            }.let {
                list ->
                when (analysisType) {
                    ReviewImagesFragment.TYPE_SELFIE ->
                        dao.cleanIsSelfie(list)
                    ReviewImagesFragment.TYPE_SAD ->
                        dao.cleanIsSad(list)
                    ReviewImagesFragment.TYPE_SLEEPING ->
                        dao.cleanIsSleeping(list)
                    ReviewImagesFragment.TYPE_DISTRACTED ->
                        dao.cleanIsDistracted(list)
                    ReviewImagesFragment.TYPE_GROUP_PIC ->
                        dao.cleanIsGroupPic(list)
                }
            }
            emit(true)
        }
    }

    fun cleanMemeAnalysis(
        dao: MemeAnalysisDao,
        checkItemsList: List<AbstractMediaFilesAdapter.ListItem>
    ):
        LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(false)
            checkItemsList.filter { listItem ->
                listItem.mediaFileInfo != null
            }.map { listItem ->
                listItem.mediaFileInfo!!.path
            }.let { list ->
                dao.cleanIsMeme(list)
            }
            emit(true)
        }
    }

    fun cleanBlurAnalysis(
        dao: BlurAnalysisDao,
        checkItemsList: List<AbstractMediaFilesAdapter.ListItem>
    ):
        LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(false)
            checkItemsList.filter { listItem ->
                listItem.mediaFileInfo != null
            }.map { listItem ->
                listItem.mediaFileInfo!!.path
            }.let { list ->
                dao.cleanIsBlur(list)
            }
            emit(true)
        }
    }

    fun cleanLowLightAnalysis(
        dao: LowLightAnalysisDao,
        checkItemsList: List<AbstractMediaFilesAdapter.ListItem>
    ):
        LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(false)
            checkItemsList.filter { listItem ->
                listItem.mediaFileInfo != null
            }.map { listItem ->
                listItem.mediaFileInfo!!.path
            }.let { list ->
                dao.cleanIsLowLight(list)
            }
            emit(true)
        }
    }

    private fun processFileRecursive(
        file: File,
        priorityQueue: PriorityQueue<MediaFileInfo>,
        mediaType: Int
    ) {
        if (file.exists()) {
            if (file.isDirectory) {
                val filesInDir = file.listFiles()
                filesInDir?.forEach {
                    processFileRecursive(it, priorityQueue, mediaType)
                }
            } else {
                val mediaFile = MediaFileInfo.fromFile(
                    file,
                    MediaFileInfo.ExtraInfo(mediaType, null, null, null)
                )
                if (priorityQueue.size > 99) {
                    priorityQueue.remove()
                }
                priorityQueue.add(mediaFile)
            }
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
                    MediaFileInfo.MEDIA_TYPE_UNKNOWN,
                    null, null, null
                )
            )
        }
        return response
    }

    private fun transformInternalStorageAnalysisToMediaFileList(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ):
        List<List<MediaFileInfo>> {
        val analysis: List<InternalStorageAnalysis> = when {
            searchMediaFiles -> {
                dao.getAllMediaFiles()
            }
            deepSearch -> {
                dao.getAll()
            }
            else -> {
                dao.getAllShallow(PreferencesConstants.DEFAULT_DUPLICATE_SEARCH_DEPTH_INCL)
            }
        }
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
                        MediaFileInfo.MEDIA_TYPE_UNKNOWN,
                        null, null, null
                    )
                )
            }
        }
        return response
    }

    private fun transformAnalysisToMediaFileInfo(
        imageAnalysis: List<ImageAnalysis>,
        dao: ImageAnalysisDao
    ):
        List<MediaFileInfo> {
        val response = imageAnalysis.filter {
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

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<BlurAnalysis>,
        dao: BlurAnalysisDao
    ):
        List<MediaFileInfo> {
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

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<LowLightAnalysis>,
        dao: LowLightAnalysisDao
    ):
        List<MediaFileInfo> {
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

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<MemeAnalysis>,
        dao: MemeAnalysisDao
    ):
        List<MediaFileInfo> {
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
