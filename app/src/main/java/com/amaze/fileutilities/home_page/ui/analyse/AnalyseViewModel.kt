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
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class AnalyseViewModel : ViewModel() {

    var analysisType: Int? = null

    private var duplicateFilesLiveData: MutableLiveData<List<List<MediaFileInfo>>?>? = null
    private var emptyFilesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var oldRecordingsLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var oldScreenshotsLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var oldDownloadsLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var largeDownloadsLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var largeVideosLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var clutteredVideosLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var blurImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var lowLightImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var memeImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var sleepingImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var sadImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var distractedImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var selfieImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null
    private var groupPicImagesLiveData: MutableLiveData<List<MediaFileInfo>?>? = null

    fun getBlurImages(dao: BlurAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (blurImagesLiveData == null) {
            blurImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getLowLightImages(dao: LowLightAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (lowLightImagesLiveData == null) {
            lowLightImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getMemeImages(dao: MemeAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (memeImagesLiveData == null) {
            memeImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getSleepingImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (sleepingImagesLiveData == null) {
            sleepingImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getSadImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (sadImagesLiveData == null) {
            sadImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getDistractedImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (distractedImagesLiveData == null) {
            distractedImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getSelfieImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (selfieImagesLiveData == null) {
            selfieImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getGroupPicImages(dao: ImageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (groupPicImagesLiveData == null) {
            groupPicImagesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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

    fun getClutteredVideos(videosList: List<MediaFileInfo>): LiveData<List<MediaFileInfo>?> {
        if (clutteredVideosLiveData == null) {
            clutteredVideosLiveData = MutableLiveData<List<MediaFileInfo>?>()
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
            clutteredVideosLiveData?.postValue(result)
        }
    }

    fun getLargeVideos(videosList: List<MediaFileInfo>): LiveData<List<MediaFileInfo>?> {
        if (largeVideosLiveData == null) {
            largeVideosLiveData = MutableLiveData<List<MediaFileInfo>?>()
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
            largeVideosLiveData?.postValue(result.reversed())
        }
    }

    fun getLargeDownloads(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        if (largeDownloadsLiveData == null) {
            largeDownloadsLiveData = MutableLiveData<List<MediaFileInfo>?>()
            largeDownloadsLiveData?.value = null
            processLargeDownloads(dao)
        }
        return largeDownloadsLiveData!!
    }

    private fun processLargeDownloads(dao: PathPreferencesDao) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }

            prefPaths.forEach {
                processFileRecursive(
                    File(it.path), priorityQueue,
                    MediaFileInfo.MEDIA_TYPE_UNKNOWN
                )
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            largeDownloadsLiveData?.postValue(result.reversed())
        }
    }

    fun getOldDownloads(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        if (oldDownloadsLiveData == null) {
            oldDownloadsLiveData = MutableLiveData<List<MediaFileInfo>?>()
            oldDownloadsLiveData?.value = null
            processOldDownloads(dao)
        }
        return oldDownloadsLiveData!!
    }

    private fun processOldDownloads(dao: PathPreferencesDao) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                100
            ) { o1, o2 -> -1 * o1.date.compareTo(o2.date) }

            prefPaths.forEach {
                processFileRecursive(
                    File(it.path), priorityQueue,
                    MediaFileInfo.MEDIA_TYPE_UNKNOWN
                )
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            oldDownloadsLiveData?.postValue(result.reversed())
        }
    }

    fun getOldScreenshots(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        if (oldScreenshotsLiveData == null) {
            oldScreenshotsLiveData = MutableLiveData<List<MediaFileInfo>?>()
            oldScreenshotsLiveData?.value = null
            processOldScreenshots(dao)
        }
        return oldScreenshotsLiveData!!
    }

    private fun processOldScreenshots(dao: PathPreferencesDao) {
        viewModelScope.launch(Dispatchers.IO) {
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
            oldScreenshotsLiveData?.postValue(result.reversed())
        }
    }

    fun getOldRecordings(dao: PathPreferencesDao): LiveData<List<MediaFileInfo>?> {
        if (oldRecordingsLiveData == null) {
            oldRecordingsLiveData = MutableLiveData<List<MediaFileInfo>?>()
            oldRecordingsLiveData?.value = null
            processOldRecordings(dao)
        }
        return oldRecordingsLiveData!!
    }

    private fun processOldRecordings(dao: PathPreferencesDao) {
        viewModelScope.launch(Dispatchers.IO) {
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
            oldRecordingsLiveData?.postValue(result.reversed())
        }
    }

    fun getDuplicateDirectories(
        dao: InternalStorageAnalysisDao,
        searchMediaFiles: Boolean,
        deepSearch: Boolean
    ): LiveData<List<List<MediaFileInfo>>?> {
        if (duplicateFilesLiveData == null) {
            duplicateFilesLiveData = MutableLiveData<List<List<MediaFileInfo>>?>()
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

    fun getEmptyFiles(dao: InternalStorageAnalysisDao): LiveData<List<MediaFileInfo>?> {
        if (emptyFilesLiveData == null) {
            emptyFilesLiveData = MutableLiveData<List<MediaFileInfo>?>()
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
