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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import androidx.lifecycle.*
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysis
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysisDao
import com.amaze.fileutilities.home_page.database.MediaFileAnalysis
import com.amaze.fileutilities.utilis.CursorUtils
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.ImgUtils
import com.amaze.fileutilities.utilis.StorageDirectoryParcelable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class FilesViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var isMediaFilesAnalysing = true
    var isInternalStorageAnalysing = true

    val internalStorageStats: LiveData<StorageSummary?> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            val storageData: StorageDirectoryParcelable? = if (SDK_INT >= N) {
                FileUtils.getStorageDirectoriesNew(applicationContext.applicationContext)
            } else {
                FileUtils.getStorageDirectoriesLegacy(applicationContext.applicationContext)
            }
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

    val recentFilesLiveData: LiveData<ArrayList<MediaFileInfo>?> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            val mediaFileInfoList = CursorUtils
                .listRecentFiles(applicationContext.applicationContext)
            emit(mediaFileInfoList)
        }

    val usedImagesSummaryTransformations:
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getImagesSummaryLiveData(input)
            }

    fun analyseImagesTransformation(mediaFileInfoList: ArrayList<MediaFileInfo>) {
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).analysisDao()
            isMediaFilesAnalysing = true
            mediaFileInfoList.forEach {
                if (dao.findByPath(it.path) == null) {
                    val isBlur = ImgUtils.isImageBlur(it.path)
                    val isMeme = ImgUtils.isImageMeme(
                        it.path,
                        applicationContext.externalCacheDir!!.path
                    )
                    if (isBlur || isMeme) {
                        dao.insert(MediaFileAnalysis(it.path, isBlur, isMeme))
                    }
                }
            }
            isMediaFilesAnalysing = false
        }
    }

    fun analyseInternalStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(applicationContext).internalStorageAnalysisDao()
            isInternalStorageAnalysing = true
            val storageData: StorageDirectoryParcelable? = if (SDK_INT >= N) {
                FileUtils.getStorageDirectoriesNew(applicationContext.applicationContext)
            } else {
                FileUtils.getStorageDirectoriesLegacy(applicationContext.applicationContext)
            }
            storageData?.run {
                val file = File(this.path)
                processInternalStorageAnalysis(dao, file)
            }
            isInternalStorageAnalysing = false
        }
    }

    val usedAudiosSummaryTransformations:
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getAudiosSummaryLiveData(input)
            }

    val usedVideosSummaryTransformations:
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getVideosSummaryLiveData(input)
            }

    val usedDocsSummaryTransformations:
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> =
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
                searchFilter.imagesMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterVideos) {
                searchFilter.videosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterAudios) {
                searchFilter.audiosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterDocuments) {
                searchFilter.docsMediaFilesList?.let { mediaInfoList ->
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
                searchFilter.imagesMediaFilesList?.let { mediaInfoList ->
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
                searchFilter.videosMediaFilesList?.let { mediaInfoList ->
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
                searchFilter.audiosMediaFilesList?.let { mediaInfoList ->
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
                searchFilter.docsMediaFilesList?.let { mediaInfoList ->
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

    fun copyTrainedData() {
        viewModelScope.launch(Dispatchers.IO) {
            val externalFilesDir: File = applicationContext.applicationContext
                .externalCacheDir ?: return@launch
            val trainedFilesBase = File(externalFilesDir.path, "tessdata")
            val trainedFilesList = arrayListOf("eng.traineddata")
            trainedFilesList.forEach {
                writeTrainedFile(trainedFilesBase, it)
            }
        }
    }

    private fun processInternalStorageAnalysis(dao: InternalStorageAnalysisDao, file: File) {
        if (file.isDirectory) {
            val filesInDir = file.listFiles()
            if (filesInDir == null) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, true
                    )
                )
            } else {
                if (filesInDir.isNotEmpty()) {
                    for (currFile in filesInDir) {
                        processInternalStorageAnalysis(dao, currFile)
                    }
                }
            }
        } else {
            if (file.length() == 0L) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, false
                    )
                )
            } else {
                val checksum = getSHA256Checksum(file)
                val existingChecksum = dao.findBySha256Checksum(checksum)
                if (existingChecksum != null && !existingChecksum.files.contains(file.path)) {
                    dao.insert(
                        InternalStorageAnalysis(
                            existingChecksum.checksum,
                            existingChecksum.files + file.path,
                            false, false, false
                        )
                    )
                } else {
                    dao.insert(
                        InternalStorageAnalysis(
                            checksum,
                            listOf(file.path), false, false, false
                        )
                    )
                }
            }
        }
    }

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    private fun getSHA256Checksum(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val input = ByteArray(FileUtils.DEFAULT_BUFFER_SIZE)
        var length: Int
        val inputStream: InputStream = FileInputStream(file)
        while (inputStream.read(input).also { length = it } != -1) {
            if (length > 0) messageDigest.update(input, 0, length)
        }
        val hash = messageDigest.digest()
        val hexString = StringBuilder()
        for (aHash in hash) {
            // convert hash to base 16
            val hex = Integer.toHexString(0xff and aHash.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        inputStream.close()
        return hexString.toString()
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
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
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
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
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
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
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
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
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
