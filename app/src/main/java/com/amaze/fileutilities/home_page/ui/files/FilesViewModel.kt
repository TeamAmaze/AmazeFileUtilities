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
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilesViewModel(val applicationContext: Application) : AndroidViewModel(applicationContext) {

    val internalStorageStats: LiveData<StorageSummary?> = liveData {
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

    val transformations: LiveData<AggregatedStorageSummary?> = Transformations.switchMap(internalStorageStats,
        object : androidx.arch.core.util.Function<StorageSummary?, LiveData<AggregatedStorageSummary?>> {
        override fun apply(input: StorageSummary?): LiveData<AggregatedStorageSummary?> {
            return getUsedSpaceLiveData(input)
        }
    })

    fun getUsedSpaceLiveData(storageSummary: StorageSummary?): LiveData<AggregatedStorageSummary?> {
        return liveData {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            var imagesStorageSummary = StorageSummary(0, 0, null, null, null)
            var audiosStorageSummary = StorageSummary(0, 0, null, null, null)
            var videosStorageSummary = StorageSummary(0,0, null, null, null)
            var documentsStorageSummary = StorageSummary(0,0, null, null, null)
            val aggregatedStorageSummary = AggregatedStorageSummary(imagesStorageSummary, audiosStorageSummary,
                videosStorageSummary, documentsStorageSummary)
            CursorUtils.listImages(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    imagesStorageSummary.items = items
                    imagesStorageSummary.progress = (size / storageSummary.totalSpace!! * 100).toInt()
                    emit(aggregatedStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
            CursorUtils.listAudio(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    audiosStorageSummary.items = items
                    audiosStorageSummary.progress = (size / storageSummary.totalSpace!! * 100).toInt()
                    emit(aggregatedStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
            CursorUtils.listVideos(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    videosStorageSummary.items = items
                    videosStorageSummary.progress = (size / storageSummary.totalSpace!! * 100).toInt()
                    emit(aggregatedStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
            CursorUtils.listDocs(applicationContext.applicationContext, object : StorageSummaryCallback {
                override suspend fun getStorageSummary(items: Int, size: Long) {
                    documentsStorageSummary.items = items
                    documentsStorageSummary.progress = (size / storageSummary.totalSpace!! * 100).toInt()
                    emit(aggregatedStorageSummary)
                }

                override suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    data class StorageSummary(
        var items: Int, var progress: Int,
        val usedSpace: Long?, val freeSpace: Long?, val totalSpace: Long?)

    data class AggregatedStorageSummary(val imagesInfo: StorageSummary,
                               val audiosInfo: StorageSummary,
                               val videosInfo: StorageSummary,
                               val documentsInfo: StorageSummary)

    interface StorageSummaryCallback {
        suspend fun getStorageSummary(items: Int, size: Long)
        suspend fun getMediaFileInfo(mediaFileInfo: MediaFileInfo)
    }
}