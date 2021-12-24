package com.amaze.fileutilities.home_page.ui.files

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import androidx.lifecycle.*
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.CircleColorView
import com.amaze.fileutilities.utilis.CursorUtils
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.StorageDirectoryParcelable
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilesViewModel(val applicationContext: Application) : AndroidViewModel(applicationContext) {

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
            emit(StorageSummary(items, progress.toInt(), usedSpace, file.usableSpace, file.totalSpace))
        }
    }

    val usedImagesSummaryTransformations: LiveData<StorageSummary?> =
        Transformations.switchMap(internalStorageStats) { input -> getImagesSummaryLiveData(input) }

    val usedAudiosSummaryTransformations: LiveData<StorageSummary?> =
        Transformations.switchMap(internalStorageStats) { input -> getAudiosSummaryLiveData(input) }

    val usedVideosSummaryTransformations: LiveData<StorageSummary?> =
        Transformations.switchMap(internalStorageStats) { input -> getVideosSummaryLiveData(input) }

    val usedDocsSummaryTransformations: LiveData<StorageSummary?> =
        Transformations.switchMap(internalStorageStats) { input -> getDocumentsSummaryLiveData(input) }

    private fun getImagesSummaryLiveData(storageSummary: StorageSummary?): LiveData<StorageSummary?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val imagesStorageSummary = StorageSummary(0, 0, null, null, null)
            CursorUtils.listImages(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    imagesStorageSummary.items = items
                    imagesStorageSummary.progress = ((size * 100) / storageSummary.totalSpace!!).toInt()
                    imagesStorageSummary.usedSpace = size
                    emit(imagesStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    private fun getAudiosSummaryLiveData(storageSummary: StorageSummary?): LiveData<StorageSummary?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val audiosStorageSummary = StorageSummary(0, 0, null, null, null)
            CursorUtils.listAudio(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    audiosStorageSummary.items = items
                    audiosStorageSummary.progress = ((size * 100) / storageSummary.totalSpace!!).toInt()
                    audiosStorageSummary.usedSpace = size
                    emit(audiosStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    private fun getVideosSummaryLiveData(storageSummary: StorageSummary?): LiveData<StorageSummary?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val videosStorageSummary = StorageSummary(0,0, null, null, null)
            CursorUtils.listVideos(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    videosStorageSummary.items = items
                    videosStorageSummary.progress = ((size * 100) / storageSummary.totalSpace!!).toInt()
                    videosStorageSummary.usedSpace = size
                    emit(videosStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    private fun getDocumentsSummaryLiveData(storageSummary: StorageSummary?): LiveData<StorageSummary?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val documentsStorageSummary = StorageSummary(0,0, null, null, null)
            CursorUtils.listDocs(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    documentsStorageSummary.items = items
                    documentsStorageSummary.progress = ((size * 100) / storageSummary.totalSpace!!).toInt()
                    documentsStorageSummary.usedSpace = size
                    emit(documentsStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    data class StorageSummary(
        var items: Int, var progress: Int,
        var usedSpace: Long?, val freeSpace: Long?, val totalSpace: Long?)

    interface StorageSummaryCallback {
        suspend fun getStorageSummary(items: Int, size: Long)
        suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo)
    }
}