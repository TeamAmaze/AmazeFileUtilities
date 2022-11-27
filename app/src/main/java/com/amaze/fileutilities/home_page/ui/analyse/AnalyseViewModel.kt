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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.home_page.database.BlurAnalysis
import com.amaze.fileutilities.home_page.database.BlurAnalysisDao
import com.amaze.fileutilities.home_page.database.ImageAnalysis
import com.amaze.fileutilities.home_page.database.ImageAnalysisDao
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysis
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysisDao
import com.amaze.fileutilities.home_page.database.LowLightAnalysis
import com.amaze.fileutilities.home_page.database.LowLightAnalysisDao
import com.amaze.fileutilities.home_page.database.MemeAnalysis
import com.amaze.fileutilities.home_page.database.MemeAnalysisDao
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.invalidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.PriorityQueue

class AnalyseViewModel : ViewModel() {

    var analysisType: Int? = null

    var duplicateFilesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var emptyFilesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeVideosLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var clutteredVideosLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var blurImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var lowLightImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var memeImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var sleepingImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var sadImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var distractedImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var selfieImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var groupPicImagesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null

    fun getBlurImages(dao: BlurAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (blurImagesLiveData == null) {
            blurImagesLiveData = MutableLiveData()
            blurImagesLiveData?.value = null
            processBlurImages(dao)
        }
        return blurImagesLiveData!!
    }

    private fun processBlurImages(dao: BlurAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            blurImagesLiveData?.postValue(transformAnalysisToMediaFileInfo(dao.getAllBlur(), dao))
        }
    }

    fun getLowLightImages(dao: LowLightAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (lowLightImagesLiveData == null) {
            lowLightImagesLiveData = MutableLiveData()
            lowLightImagesLiveData?.value = null
            processLowLightImages(dao)
        }
        return lowLightImagesLiveData!!
    }

    private fun processLowLightImages(dao: LowLightAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            lowLightImagesLiveData?.postValue(
                transformAnalysisToMediaFileInfo(
                    dao
                        .getAllLowLight(),
                    dao
                )
            )
        }
    }

    fun getMemeImages(dao: MemeAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (memeImagesLiveData == null) {
            memeImagesLiveData = MutableLiveData()
            memeImagesLiveData?.value = null
            processMemeImages(dao)
        }
        return memeImagesLiveData!!
    }

    private fun processMemeImages(dao: MemeAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            memeImagesLiveData?.postValue(transformAnalysisToMediaFileInfo(dao.getAllMeme(), dao))
        }
    }

    fun getSleepingImages(dao: ImageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (sleepingImagesLiveData == null) {
            sleepingImagesLiveData = MutableLiveData()
            sleepingImagesLiveData?.value = null
            processSleepingImages(dao)
        }
        return sleepingImagesLiveData!!
    }

    private fun processSleepingImages(dao: ImageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            sleepingImagesLiveData?.postValue(
                transformAnalysisToMediaFileInfo(
                    dao
                        .getAllSleeping(),
                    dao
                )
            )
        }
    }

    fun getSadImages(dao: ImageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (sadImagesLiveData == null) {
            sadImagesLiveData = MutableLiveData()
            sadImagesLiveData?.value = null
            processSadImages(dao)
        }
        return sadImagesLiveData!!
    }

    private fun processSadImages(dao: ImageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            sadImagesLiveData?.postValue(transformAnalysisToMediaFileInfo(dao.getAllSad(), dao))
        }
    }

    fun getDistractedImages(dao: ImageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (distractedImagesLiveData == null) {
            distractedImagesLiveData = MutableLiveData()
            distractedImagesLiveData?.value = null
            processDistractedImages(dao)
        }
        return distractedImagesLiveData!!
    }

    private fun processDistractedImages(dao: ImageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            distractedImagesLiveData?.postValue(
                transformAnalysisToMediaFileInfo(
                    dao
                        .getAllDistracted(),
                    dao
                )
            )
        }
    }

    fun getSelfieImages(dao: ImageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (selfieImagesLiveData == null) {
            selfieImagesLiveData = MutableLiveData()
            selfieImagesLiveData?.value = null
            processSelfieImages(dao)
        }
        return selfieImagesLiveData!!
    }

    private fun processSelfieImages(dao: ImageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            selfieImagesLiveData?.postValue(
                transformAnalysisToMediaFileInfo(
                    dao
                        .getAllSelfie(),
                    dao
                )
            )
        }
    }

    fun getGroupPicImages(dao: ImageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (groupPicImagesLiveData == null) {
            groupPicImagesLiveData = MutableLiveData()
            groupPicImagesLiveData?.value = null
            processGroupPicImages(dao)
        }
        return groupPicImagesLiveData!!
    }

    private fun processGroupPicImages(dao: ImageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            groupPicImagesLiveData?.postValue(
                transformAnalysisToMediaFileInfo(
                    dao
                        .getAllGroupPic(),
                    dao
                )
            )
        }
    }

    fun getClutteredVideos(videosList: List<MediaFileInfo>): LiveData<ArrayList<MediaFileInfo>?> {
        if (clutteredVideosLiveData == null) {
            clutteredVideosLiveData = MutableLiveData()
            clutteredVideosLiveData?.value = null
            processClutteredVideos(videosList)
        }
        return clutteredVideosLiveData!!
    }

    private fun processClutteredVideos(videosList: List<MediaFileInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val countIdx = IntArray(101) { 0 }
            videosList.forEach {
                it.extraInfo?.videoMetaData?.duration?.let {
                    duration ->
                    val idx = duration / 1000 / 60
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
                    if ((duration / 1000 / 60).toInt() == maxIdx) {
                        return@filter true
                    }
                }
                return@filter false
            }
            clutteredVideosLiveData?.postValue(ArrayList(result))
        }
    }

    fun getLargeVideos(videosList: List<MediaFileInfo>): LiveData<ArrayList<MediaFileInfo>?> {
        if (largeVideosLiveData == null) {
            largeVideosLiveData = MutableLiveData()
            largeVideosLiveData?.value = null
            processLargeVideos(videosList)
        }
        return largeVideosLiveData!!
    }

    private fun processLargeVideos(videosList: List<MediaFileInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
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
            largeVideosLiveData?.postValue(ArrayList(result.reversed()))
        }
    }

    fun getDuplicateDirectories(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ): LiveData<ArrayList<MediaFileInfo>?> {
        if (duplicateFilesLiveData == null) {
            duplicateFilesLiveData = MutableLiveData()
            duplicateFilesLiveData?.value = null
            processDuplicateDirectories(dao, searchMediaFiles, deepSearch)
        }
        return duplicateFilesLiveData!!
    }

    private fun processDuplicateDirectories(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            duplicateFilesLiveData?.postValue(
                transformInternalStorageAnalysisToMediaFileList(
                    dao,
                    searchMediaFiles, deepSearch
                )
            )
        }
    }

    fun getEmptyFiles(dao: InternalStorageAnalysisDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (emptyFilesLiveData == null) {
            emptyFilesLiveData = MutableLiveData()
            emptyFilesLiveData?.value = null
            processEmptyFiles(dao)
        }
        return emptyFilesLiveData!!
    }

    private fun processEmptyFiles(dao: InternalStorageAnalysisDao) {
        viewModelScope.launch(Dispatchers.IO) {
            emptyFilesLiveData?.postValue(transformInternalStorageAnalysisToMediaFile(dao))
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
        mediaType: Int,
        limit: Int = 99,
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
                if (priorityQueue.size > limit) {
                    priorityQueue.remove()
                }
                priorityQueue.add(mediaFile)
            }
        }
    }

    private fun transformInternalStorageAnalysisToMediaFile(dao: InternalStorageAnalysisDao):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response)
    }

    private fun transformInternalStorageAnalysisToMediaFileList(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response.flatten())
    }

    private fun transformAnalysisToMediaFileInfo(
        imageAnalysis: List<ImageAnalysis>,
        dao: ImageAnalysisDao
    ):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response)
    }

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<BlurAnalysis>,
        dao: BlurAnalysisDao
    ):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response)
    }

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<LowLightAnalysis>,
        dao: LowLightAnalysisDao
    ):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response)
    }

    private fun transformAnalysisToMediaFileInfo(
        analysis: List<MemeAnalysis>,
        dao: MemeAnalysisDao
    ):
        ArrayList<MediaFileInfo> {
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
        return ArrayList(response)
    }
}
