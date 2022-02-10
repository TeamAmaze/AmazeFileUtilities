/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.app.Application
import androidx.lifecycle.*
import com.amaze.fileutilities.home_page.database.*
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.utilis.*
import com.amaze.fileutilities.utilis.FileUtils.Companion.getExternalStorageDirectory
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class FilesViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var isMediaFilesAnalysing = true
    var isInternalStorageAnalysing = true
    var isMediaStoreAnalysing = true

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetector = FaceDetection.getClient(highAccuracyOpts)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val internalStorageStats: LiveData<StorageSummary?> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            val storageData = applicationContext.applicationContext.getExternalStorageDirectory()
            storageData?.run {
                val file = File(this.path)
                var items = 0
                file.list()?.let {
                    items = it.size
                }
                val usedSpace = file.totalSpace - file.usableSpace
                val progress = (usedSpace * 100) / file.totalSpace
                emit(
                    StorageSummary(
                        items, progress.toInt(), usedSpace, file.usableSpace,
                        file.totalSpace
                    )
                )
            }
        }

    val recentFilesLiveData: LiveData<List<MediaFileInfo>?> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            val mediaFileInfoList = CursorUtils
                .listRecentFiles(applicationContext.applicationContext)
            emit(mediaFileInfoList)
        }

    val usedImagesSummaryTransformations:
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getImagesSummaryLiveData(input)
            }

    val usedAudiosSummaryTransformations:
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getAudiosSummaryLiveData(input)
            }

    val usedVideosSummaryTransformations:
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getVideosSummaryLiveData(input)
            }

    val usedDocsSummaryTransformations:
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getDocumentsSummaryLiveData(input)
            }

    fun queryOnAggregatedMediaFiles(
        query: String,
        searchFilter: SearchListFragment.SearchQueryInput
    ):
        LiveData<MutableList<MediaFileInfo>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val mediaFileResults = mutableListOf<MediaFileInfo>()
            val textResults = mutableListOf<String>()
            if (searchFilter.searchFilter.searchFilterImages) {
                searchFilter.aggregatedMediaFiles.imagesMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterVideos) {
                searchFilter.aggregatedMediaFiles.videosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterAudios) {
                searchFilter.aggregatedMediaFiles.audiosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterDocuments) {
                searchFilter.aggregatedMediaFiles.docsMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            emit(mediaFileResults)
        }
    }

    fun queryHintOnAggregatedMediaFiles(
        query: String,
        resultsThreshold: Int,
        searchFilter: SearchListFragment.SearchQueryInput
    ):
        LiveData<MutableList<String>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val textResults = mutableListOf<String>()
            var currentResultsCount = 0

            if (searchFilter.searchFilter.searchFilterImages) {
                searchFilter.aggregatedMediaFiles.imagesMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach il@{
                        if (currentResultsCount> resultsThreshold) {
                            return@il
                        }
                        if (it.title.contains(query)) {
                            textResults.add(it.title)
                            currentResultsCount++
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterVideos) {
                searchFilter.aggregatedMediaFiles.videosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach vl@{
                        if (currentResultsCount> resultsThreshold) {
                            return@vl
                        }
                        if (it.title.contains(query)) {
                            textResults.add(it.title)
                            currentResultsCount++
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterAudios) {
                searchFilter.aggregatedMediaFiles.audiosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach al@{
                        if (currentResultsCount> resultsThreshold) {
                            return@al
                        }
                        if (it.title.contains(query)) {
                            textResults.add(it.title)
                            currentResultsCount++
                        }
                    }
                }
            }

            if (searchFilter.searchFilter.searchFilterDocuments) {
                searchFilter.aggregatedMediaFiles.docsMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach dl@{
                        if (currentResultsCount> resultsThreshold) {
                            return@dl
                        }
                        if (it.title.contains(query)) {
                            textResults.add(it.title)
                            currentResultsCount++
                        }
                    }
                }
            }
            emit(textResults)
        }
    }

    /*fun copyTrainedData() {
        viewModelScope.launch(Dispatchers.IO) {
            val externalFilesDir: File = applicationContext.applicationContext
                .externalCacheDir ?: return@launch
            val trainedFilesBase = File(externalFilesDir.path, "tessdata")
            val trainedFilesList = arrayListOf("eng.traineddata")
            trainedFilesList.forEach {
                writeTrainedFile(trainedFilesBase, it)
            }
        }
    }*/

    fun analyseImagesTransformation(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).analysisDao()
            isMediaFilesAnalysing = true
            var memesProcessed = 0
            var featuresProcessed = 0
            mediaFileInfoList.forEach {
                if (dao.findByPath(it.path) == null) {
                    ImgUtils.isImageMeme(
                        applicationContext,
                        textRecognizer,
                        it.getContentUri(applicationContext),
                        pathPreferencesList.filter { pref ->
                            pref.feature == PathPreferences.FEATURE_ANALYSIS_MEME
                        },
                        memesProcessed++
                    ) { isMeme ->
                        viewModelScope.launch(Dispatchers.Default) {
                            var features = ImgUtils.ImageFeatures()
                            ImgUtils.getImageFeatures(
                                applicationContext,
                                faceDetector,
                                it.getContentUri(applicationContext),
                                featuresProcessed++,
                                pathPreferencesList.filter { pref ->
                                    pref.feature == PathPreferences
                                        .FEATURE_ANALYSIS_IMAGE_FEATURES
                                }
                            ) { isSuccess, imageFeatures ->
                                viewModelScope.launch(Dispatchers.Default) {
                                    if (isSuccess) {
                                        imageFeatures?.run {
                                            features = this
                                        }
                                    }

                                    val isBlur = ImgUtils.isImageBlur(
                                        it.path,
                                        pathPreferencesList.filter { pref ->
                                            pref.feature == PathPreferences
                                                .FEATURE_ANALYSIS_BLUR
                                        }
                                    )

                                    val isLowLight = ImgUtils.isImageLowLight(
                                        it.path,
                                        pathPreferencesList.filter { pref ->
                                            pref.feature == PathPreferences
                                                .FEATURE_ANALYSIS_LOW_LIGHT
                                        }
                                    )

                                    if (isBlur || isLowLight || isMeme ||
                                        features.featureDetected()
                                    ) {
                                        dao.insert(
                                            ImageAnalysis(
                                                it.path, isBlur, isMeme,
                                                features.isSad,
                                                features.isDistracted,
                                                features.isSleeping,
                                                isLowLight,
                                                features.facesCount
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            isMediaFilesAnalysing = false
        }
    }

    fun analyseInternalStorage(deepSearch: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(applicationContext).internalStorageAnalysisDao()
            isInternalStorageAnalysing = true
            val storageData = applicationContext.applicationContext.getExternalStorageDirectory()
            storageData?.run {
                val file = File(this.path)
                processInternalStorageAnalysis(dao, file, deepSearch, 0)
            }
            isInternalStorageAnalysing = false
        }
    }

    fun analyseMediaStoreFiles(
        aggregatedMediaFiles:
            AggregatedMediaFileInfoObserver.AggregatedMediaFiles
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(applicationContext).internalStorageAnalysisDao()
            isMediaStoreAnalysing = true
            aggregatedMediaFiles.imagesMediaFilesList?.forEach {
                getMediaFileChecksumAndWriteToDatabase(dao, File(it.path + ""))
            }
            aggregatedMediaFiles.audiosMediaFilesList?.forEach {
                getMediaFileChecksumAndWriteToDatabase(dao, File(it.path + ""))
            }
            aggregatedMediaFiles.videosMediaFilesList?.forEach {
                getMediaFileChecksumAndWriteToDatabase(dao, File(it.path + ""))
            }
            aggregatedMediaFiles.docsMediaFilesList?.forEach {
                getMediaFileChecksumAndWriteToDatabase(dao, File(it.path + ""))
            }
            isMediaStoreAnalysing = false
        }
    }

    fun initAndFetchPathPreferences(): LiveData<List<PathPreferences>> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            val prefs = applicationContext.getAppCommonSharedPreferences()
            val pathPrefs = prefs.getInt(
                PreferencesConstants.KEY_PATH_PREFS_MIGRATION,
                PreferencesConstants.DEFAULT_PATH_PREFS_INITIALIZED
            )
            val dao = AppDatabase.getInstance(applicationContext).pathPreferencesDao()
            if (pathPrefs < PreferencesConstants.VAL_PATH_PREFS_MIGRATION) {
                val storageData = applicationContext.applicationContext
                    .getExternalStorageDirectory()
                val prefsList = ArrayList<PathPreferences>()
                FileUtils.DEFAULT_PATH_PREFS_INCLUSIVE.forEach {
                    keyValue ->
                    keyValue.value.forEach {
                        value ->
                        prefsList.add(
                            PathPreferences(
                                "${storageData?.path}/$value",
                                keyValue.key
                            )
                        )
                    }
                }
                FileUtils.DEFAULT_PATH_PREFS_EXCLUSIVE.forEach {
                    keyValue ->
                    keyValue.value.forEach {
                        value ->
                        prefsList.add(
                            PathPreferences(
                                "${storageData?.path}/$value",
                                keyValue.key, true
                            )
                        )
                    }
                }
                dao.insertAll(prefsList)
                prefs.edit().putInt(
                    PreferencesConstants.KEY_PATH_PREFS_MIGRATION,
                    PreferencesConstants.VAL_PATH_PREFS_MIGRATION
                ).apply()
            }
            emit(dao.getAll())
        }

    override fun onCleared() {
//        faceDetector.close()
//        textRecognizer.close()
        super.onCleared()
    }

    private fun processInternalStorageAnalysis(
        dao: InternalStorageAnalysisDao,
        file: File,
        deepSearch: Boolean,
        currentDepth: Int
    ) {
        if (!deepSearch && currentDepth
        > PreferencesConstants.DEFAULT_DUPLICATE_SEARCH_DEPTH_INCL
        ) {
            return
        }
        if (file.isDirectory) {
            val filesInDir = file.listFiles()
            if (filesInDir == null) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, true, false, currentDepth
                    )
                )
            } else {
                if (filesInDir.isNotEmpty()) {
                    for (currFile in filesInDir) {
                        processInternalStorageAnalysis(
                            dao, currFile, deepSearch,
                            currentDepth + 1
                        )
                    }
                }
            }
        } else {
            if (file.length() == 0L) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, false, false,
                        currentDepth
                    )
                )
            } else {
                val checksum = FileUtils.getSHA256Checksum(file)
                val existingChecksum = dao.findBySha256Checksum(checksum)
                if (existingChecksum != null && !existingChecksum.files.contains(file.path)) {
                    dao.insert(
                        InternalStorageAnalysis(
                            existingChecksum.checksum,
                            existingChecksum.files + file.path,
                            false, false, false, false,
                            currentDepth
                        )
                    )
                } else {
                    dao.insert(
                        InternalStorageAnalysis(
                            checksum,
                            listOf(file.path), false, false, false, false,
                            currentDepth
                        )
                    )
                }
            }
        }
    }

    private fun getMediaFileChecksumAndWriteToDatabase(
        dao: InternalStorageAnalysisDao,
        file: File
    ) {
        val checksum = FileUtils.getSHA256Checksum(file)
        val existingChecksum = dao.findMediaFileBySha256Checksum(checksum)
        if (existingChecksum != null && !existingChecksum.files.contains(file.path)) {
            dao.insert(
                InternalStorageAnalysis(
                    existingChecksum.checksum,
                    existingChecksum.files + file.path,
                    false, false, false, true, 0
                )
            )
        } else {
            dao.insert(
                InternalStorageAnalysis(
                    checksum,
                    listOf(file.path), false, false, false, true,
                    0
                )
            )
        }
    }

    private fun writeTrainedFile(basePath: File, fileName: String) {
        val trained = File(basePath, fileName)
        if (!trained.exists()) {
            basePath.mkdirs()
            var `in`: InputStream? = null
            var out: OutputStream? = null
            try {
                `in` = applicationContext.assets.open("training/$fileName")
                out = FileOutputStream(trained)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (`in`.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
            } finally {
                out!!.close()
                `in`!!.close()
            }
        }
    }

    private fun getImagesSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val metaInfoAndSummaryPair = CursorUtils
                .listImages(applicationContext.applicationContext)
            val summary = metaInfoAndSummaryPair.first
            summary.progress = ((summary.usedSpace!! * 100) / storageSummary.totalSpace!!)
                .toInt()
            summary.totalSpace = storageSummary.totalSpace
            emit(metaInfoAndSummaryPair)
        }
    }

    private fun getAudiosSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val metaInfoAndSummaryPair = CursorUtils
                .listAudio(applicationContext.applicationContext)
            val summary = metaInfoAndSummaryPair.first
            summary.progress = ((summary.usedSpace!! * 100) / storageSummary.totalSpace!!)
                .toInt()
            summary.totalSpace = storageSummary.totalSpace
            emit(metaInfoAndSummaryPair)
        }
    }

    private fun getVideosSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val metaInfoAndSummaryPair = CursorUtils
                .listVideos(applicationContext.applicationContext)
            val summary = metaInfoAndSummaryPair.first
            summary.progress = ((summary.usedSpace!! * 100) / storageSummary.totalSpace!!)
                .toInt()
            summary.totalSpace = storageSummary.totalSpace
            emit(metaInfoAndSummaryPair)
        }
    }

    private fun getDocumentsSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, List<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val metaInfoAndSummaryPair = CursorUtils
                .listDocs(applicationContext.applicationContext)
            val summary = metaInfoAndSummaryPair.first
            summary.progress = ((summary.usedSpace!! * 100) / storageSummary.totalSpace!!)
                .toInt()
            summary.totalSpace = storageSummary.totalSpace
            emit(metaInfoAndSummaryPair)
        }
    }

    data class StorageSummary(
        var items: Int,
        var progress: Int,
        var usedSpace: Long? = null,
        val freeSpace: Long? = null,
        var totalSpace: Long? = null
    )
}
