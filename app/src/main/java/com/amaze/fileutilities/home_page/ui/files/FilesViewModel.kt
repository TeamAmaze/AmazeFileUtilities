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

package com.amaze.fileutilities.home_page.ui.files

import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioUtils
import com.amaze.fileutilities.audio_player.playlist.PlaylistLoader
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.BlurAnalysis
import com.amaze.fileutilities.home_page.database.ImageAnalysis
import com.amaze.fileutilities.home_page.database.InstalledApps
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysis
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysisDao
import com.amaze.fileutilities.home_page.database.LowLightAnalysis
import com.amaze.fileutilities.home_page.database.MemeAnalysis
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.database.PathPreferencesDao
import com.amaze.fileutilities.home_page.database.SimilarImagesAnalysis
import com.amaze.fileutilities.home_page.database.SimilarImagesAnalysisDao
import com.amaze.fileutilities.home_page.database.SimilarImagesAnalysisMetadata
import com.amaze.fileutilities.home_page.database.SimilarImagesAnalysisMetadataDao
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.database.TrialValidatorDao
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.home_page.ui.options.Billing
import com.amaze.fileutilities.utilis.CursorUtils
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.ImgUtils
import com.amaze.fileutilities.utilis.MLUtils
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.getExternalStorageDirectory
import com.amaze.fileutilities.utilis.isNetworkAvailable
import com.amaze.fileutilities.utilis.log
import com.amaze.fileutilities.utilis.share.ShareAdapter
import com.amaze.fileutilities.utilis.share.getShareIntents
import com.amaze.trashbin.DeletePermanentlyCallback
import com.amaze.trashbin.MoveFilesCallback
import com.amaze.trashbin.TrashBin
import com.amaze.trashbin.TrashBinConfig
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.streams.toList

class FilesViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var isImageFeaturesAnalysing = true
    var isSimilarImagesAnalysing = true
    var isImageBlurAnalysing = true
    var isImageLowLightAnalysing = true
    var isImageMemesAnalysing = true
    var isInternalStorageAnalysing = true
    var isMediaStoreAnalysing = true
    var isCasting = false
    var castSetupSuccess = true
    var wifiIpAddress: String? = null
    var backPressedToExitOnce = false
    private val uniqueIdSalt = BuildConfig.SALT_DEVICE_ID

    var unusedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var mostUsedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var leastUsedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var networkIntensiveAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var newlyInstalledAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var recentlyUpdatedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var junkFilesLiveData: MutableLiveData<Pair<ArrayList<MediaFileInfo>, String>?>? = null
    var apksLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var hiddenFilesLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var gamesInstalledLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeFilesMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var whatsappMediaMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var telegramMediaMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeDownloadsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldDownloadsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldRecordingsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldScreenshotsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var trashBinFilesLiveData: MutableLiveData<MutableList<MediaFileInfo>?>? = null
    var memoryInfoLiveData: MutableLiveData<String?>? = null
    var allMediaFilesPair: ArrayList<MediaFileInfo>? = null

    private var trashBinConfig: TrashBinConfig? = null
    private val TRASH_BIN_BASE_PATH = Environment.getExternalStorageDirectory()
        .path + File.separator + ".AmazeData"
    private var usedVideosSummaryTransformations: LiveData<Pair<StorageSummary,
            ArrayList<MediaFileInfo>>?>? = null
    private var usedAudiosSummaryTransformations: LiveData<Pair<StorageSummary,
            ArrayList<MediaFileInfo>>?>? = null
    var usedPlaylistsSummaryTransformations: LiveData<Pair<StorageSummary,
            ArrayList<MediaFileInfo>>?>? = null
    private var usedImagesSummaryTransformations: LiveData<Pair<StorageSummary,
            ArrayList<MediaFileInfo>>?>? = null
    private var usedDocsSummaryTransformations: LiveData<Pair<StorageSummary,
            ArrayList<MediaFileInfo>>?>? = null

    private var allApps: AtomicReference<List<Pair<ApplicationInfo,
                PackageInfo?>>?> = AtomicReference()

    var internalStorageStatsLiveData: MutableLiveData<StorageSummary?>? = null


    fun internalStorageStats(): LiveData<StorageSummary?> {
        if (internalStorageStatsLiveData == null) {
            internalStorageStatsLiveData = MutableLiveData()
            internalStorageStatsLiveData?.value = null
            processInternalStorageStats()
        }
        return internalStorageStatsLiveData!!
    }

    private fun processInternalStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val storageData = applicationContext.applicationContext.getExternalStorageDirectory()
            storageData?.let {
                data ->
                val file = File(data.path)
                try {
                    val items = CursorUtils.getMediaFilesCount(applicationContext)
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName, file
                    )
                    FileUtils.scanFile(uri, data.path, applicationContext)
                    val usedSpace = file.totalSpace - file.usableSpace

                    val progress = if (file.totalSpace != 0L) {
                        (usedSpace * 100) / file.totalSpace
                    } else 0
                    val result = StorageSummary(
                        items, progress.toInt(), usedSpace, usedSpace,
                        file.usableSpace,
                        file.totalSpace
                    )
                    internalStorageStatsLiveData?.postValue(result)
                } catch (se: SecurityException) {
                    log.warn("failed to list recent files due to no permission", se)
                    internalStorageStatsLiveData?.postValue(null)
                }
            }
        }
    }

    private var trashBin: TrashBin? = null
    val initAnalysisMigrations: LiveData<Boolean> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(false)
            val prefs = applicationContext.getAppCommonSharedPreferences()
            PathPreferences.ANALYSE_FEATURES_LIST.forEach {
                val migrationPref = prefs.getInt(
                    PathPreferences.getAnalysisMigrationPreferenceKey(it),
                    PreferencesConstants.DEFAULT_ANALYSIS_MIGRATION_INITIALIZED
                )
                val db = AppDatabase.getInstance(applicationContext)
                val dao = db.pathPreferencesDao()
                if (migrationPref < (PathPreferences.MIGRATION_PREF_MAP[it] ?: 1)) {
                    PathPreferences.deleteAnalysisData(
                        dao.findByFeature(it),
                        WeakReference(applicationContext)
                    )
                    prefs.edit().putInt(
                        PathPreferences.getAnalysisMigrationPreferenceKey(it),
                        PathPreferences.MIGRATION_PREF_MAP[it] ?: 1
                    ).apply()
                }
            }
            emit(true)
        }

    val recentFilesLiveData: LiveData<List<MediaFileInfo>?> =
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            try {
                val mediaFileInfoList = CursorUtils
                    .listRecentFiles(applicationContext.applicationContext)
                emit(mediaFileInfoList)
            } catch (se: SecurityException) {
                log.warn("failed to list recent files due to no permission", se)
                emit(null)
            }
        }

    fun deleteMediaFilesFromList(
        mediaFileInfoList: ArrayList<MediaFileInfo>,
        toDelete: List<MediaFileInfo>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val syncList = Collections.synchronizedList(mediaFileInfoList)
//            val toDeletePaths = toDelete.map { it.path }
            synchronized(syncList) {
                /*syncList.forEachIndexed { index, mediaFileInfo ->
                    if (toDeletePaths.contains(mediaFileInfo.path)) {
                        syncList.removeAt(index)
                    }
                }*/
                /*while (syncList.iterator().hasNext()) {
                    val next = syncList.iterator().next()
                    if (toDeletePaths.contains(next.path)) {
                        syncList.iterator().remove()
                    }
                }*/
                syncList.removeAll(toDelete)
            }
        }
    }

    fun addMediaFilesToList(
        mediaFileInfoList: ArrayList<MediaFileInfo>,
        toAdd: List<MediaFileInfo>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val syncList = Collections.synchronizedList(mediaFileInfoList)
//            val toDeletePaths = toDelete.map { it.path }
            synchronized(syncList) {
                /*syncList.forEachIndexed { index, mediaFileInfo ->
                    if (toDeletePaths.contains(mediaFileInfo.path)) {
                        syncList.removeAt(index)
                    }
                }*/
                /*while (syncList.iterator().hasNext()) {
                    val next = syncList.iterator().next()
                    if (toDeletePaths.contains(next.path)) {
                        syncList.iterator().remove()
                    }
                }*/
                syncList.addAll(toAdd)
            }
        }
    }

    fun usedImagesSummaryTransformations():
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        if (usedImagesSummaryTransformations == null) {
            usedImagesSummaryTransformations = Transformations.switchMap(internalStorageStats()) {
                input ->
                getImagesSummaryLiveData(input)
            }
        }
        return usedImagesSummaryTransformations!!
    }

    fun usedAudiosSummaryTransformations():
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        if (usedAudiosSummaryTransformations == null) {
            usedAudiosSummaryTransformations = Transformations.switchMap(internalStorageStats()) {
                input ->
                getAudiosSummaryLiveData(input)
            }
        }
        return usedAudiosSummaryTransformations!!
    }

    fun usedPlaylistsSummaryTransformations():
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        if (usedPlaylistsSummaryTransformations == null) {
            usedPlaylistsSummaryTransformations =
                Transformations.switchMap(internalStorageStats()) {
                    input ->
                    getPlaylistsSummaryLiveData(input)
                }
        }
        return usedPlaylistsSummaryTransformations!!
    }

    fun usedVideosSummaryTransformations():
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        if (usedVideosSummaryTransformations == null) {
            usedVideosSummaryTransformations = Transformations.switchMap(internalStorageStats()) {
                input ->
                getVideosSummaryLiveData(input)
            }
        }
        return usedVideosSummaryTransformations!!
    }

    fun usedDocsSummaryTransformations():
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        if (usedDocsSummaryTransformations == null) {
            usedDocsSummaryTransformations = Transformations.switchMap(internalStorageStats()) {
                input ->
                getDocumentsSummaryLiveData(input)
            }
        }
        return usedDocsSummaryTransformations!!
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
                        if (it.title.contains(query, true)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterVideos) {
                searchFilter.aggregatedMediaFiles.videosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query, true)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterAudios) {
                searchFilter.aggregatedMediaFiles.audiosMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query, true)) {
                            mediaFileResults.add(it)
                            textResults.add(it.title)
                        }
                    }
                }
            }
            if (searchFilter.searchFilter.searchFilterDocuments) {
                searchFilter.aggregatedMediaFiles.docsMediaFilesList?.let { mediaInfoList ->
                    mediaInfoList.forEach {
                        if (it.title.contains(query, true)) {
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
                        if (it.title.contains(query, true)) {
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
                        if (it.title.contains(query, true)) {
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
                        if (it.title.contains(query, true)) {
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
                        if (it.title.contains(query, true)) {
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

    fun analyseImageFeatures(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        if (!PathPreferences.isEnabled(
                applicationContext.getAppCommonSharedPreferences(),
                PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
            )
        ) {
            log.info("analyse facial features for images not enabled")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).analysisDao()
            isImageFeaturesAnalysing = true
            var featuresProcessed = 0
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_IMAGE_FEATURES
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true)
            }.forEach {
                if (isImageFeaturesAnalysing && dao.findByPath(it.path) == null) {
                    if (featuresProcessed++ > 10000) {
                        // hard limit in a single run
                        return@forEach
                    }
                    MLUtils.processImageFeatures(it.path) {
                        features ->
                        dao.insert(
                            ImageAnalysis(
                                it.path,
                                features.isSad,
                                features.isDistracted,
                                features.isSleeping,
                                features.facesCount
                            )
                        )
                    }
                }
            }
            isImageFeaturesAnalysing = false
        }
    }

    fun analyseBlurImages(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        if (!PathPreferences.isEnabled(
                applicationContext.getAppCommonSharedPreferences(),
                PathPreferences.FEATURE_ANALYSIS_BLUR
            )
        ) {
            log.info("analyse blur not enabled")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).blurAnalysisDao()
            isImageBlurAnalysing = true
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_BLUR
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true)
            }.forEach {
                if (isImageBlurAnalysing && dao.findByPath(it.path) == null) {
                    val isBlur = ImgUtils.isImageBlur(it.path)
                    isBlur?.let {
                        isBlur ->
                        dao.insert(
                            BlurAnalysis(
                                it.path, isBlur
                            )
                        )
                    }
                }
            }
            isImageBlurAnalysing = false
        }
    }

    fun analyseMemeImages(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        if (!PathPreferences.isEnabled(
                applicationContext.getAppCommonSharedPreferences(),
                PathPreferences.FEATURE_ANALYSIS_MEME
            )
        ) {
            log.info("analyse memes not enabled")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).memesAnalysisDao()
            isImageMemesAnalysing = true
            var memesProcessed = 0
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences.FEATURE_ANALYSIS_MEME
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true)
            }.forEach {
                if (isImageMemesAnalysing && dao.findByPath(it.path) == null) {
                    if (memesProcessed++ > 10000) {
                        // hard limit in a single run
                        return@forEach
                    }
                    MLUtils.isImageMeme(
                        it.path,
                    ) { isMeme ->
                        dao.insert(
                            MemeAnalysis(
                                it.path, isMeme
                            )
                        )
                    }
                }
            }
            isImageMemesAnalysing = false
        }
    }

    fun analyseLowLightImages(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        if (!PathPreferences.isEnabled(
                applicationContext.getAppCommonSharedPreferences(),
                PathPreferences.FEATURE_ANALYSIS_LOW_LIGHT
            )
        ) {
            log.info("analyse low light not enabled")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).lowLightAnalysisDao()
            isImageLowLightAnalysing = true
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_LOW_LIGHT
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true)
            }.forEach {
                if (isImageLowLightAnalysing && dao.findByPath(it.path) == null) {
                    val isLowLight = ImgUtils.isImageLowLight(
                        it.path
                    )
                    isLowLight?.let {
                        isLowLight ->
                        dao.insert(
                            LowLightAnalysis(
                                it.path, isLowLight
                            )
                        )
                    }
                }
            }
            isImageLowLightAnalysing = false
        }
    }

    fun analyseSimilarImages(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        if (!PathPreferences.isEnabled(
                applicationContext.getAppCommonSharedPreferences(),
                PathPreferences.FEATURE_ANALYSIS_SIMILAR_IMAGES
            )
        ) {
            log.info("analyse similar images not enabled")
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext)
                .similarImagesAnalysisMetadataDao()
            val similarImagesAnalysisDao =
                AppDatabase.getInstance(applicationContext).similarImagesAnalysisDao()
            isSimilarImagesAnalysing = true
            val filterList = mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_SIMILAR_IMAGES
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true)
            }
            if (isSimilarImagesAnalysing) {
                val metadataCount = dao.getAllCount()
                if (filterList.size != metadataCount) {
                    var imagesProcessed = 0
                    filterList.forEach {
                        mediaFileInfo ->
                        if (isSimilarImagesAnalysing &&
                            dao.findByPath(mediaFileInfo.path) == null
                        ) {
                            if (imagesProcessed++ > 5000) {
                                // hard limit in a single run
                                return@forEach
                            }
                            val histogramPeaks = ImgUtils
                                .getHistogramChannelsWithPeaks(mediaFileInfo.path)
                            histogramPeaks?.let {
                                dao.insert(
                                    SimilarImagesAnalysisMetadata(
                                        mediaFileInfo.getParentPath(),
                                        mediaFileInfo.path,
                                        it[0], it[1], it[2], ImgUtils.DATAPOINTS,
                                        ImgUtils.THRESHOLD
                                    )
                                )
                            }
                        }
                    }
                } else {
                    filterList.forEach {
                        if (isSimilarImagesAnalysing) {
                            val savedInfo = dao.findByPath(it.path)
                            if (savedInfo != null && !savedInfo.isAnalysed) {
                                analyseHistogramForMatch(similarImagesAnalysisDao, dao, savedInfo)
                            }
                        }
                    }
                }
            }
            isSimilarImagesAnalysing = false
        }
    }

    private fun analyseHistogramForMatch(
        similarImagesAnalysisDao: SimilarImagesAnalysisDao,
        similarAnalysisMetadataDao:
            SimilarImagesAnalysisMetadataDao,
        analysisMetadata: SimilarImagesAnalysisMetadata
    ) {
        val parentFiles = similarAnalysisMetadataDao
            .findAllByParentPath(analysisMetadata.parentPath)
        if (parentFiles.size > 1) {
            log.info(
                "analysing image with parent files {} path at {}", parentFiles.size,
                analysisMetadata.filePath
            )
            // normalize channels
            val normalizedBlueMap = mutableMapOf<Int, Int>()
            val normalizedGreenMap = mutableMapOf<Int, Int>()
            val normalizedRedMap = mutableMapOf<Int, Int>()
            analysisMetadata.blueChannel.forEach {
                normalizedBlueMap[
                    it.first /
                        ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                ] =
                    it.second / ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
            }
            analysisMetadata.greenChannel.forEach {
                normalizedGreenMap[
                    it.first /
                        ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                ] =
                    it.second / ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
            }
            analysisMetadata.redChannel.forEach {
                normalizedRedMap[
                    it.first /
                        ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                ] =
                    it.second / ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
            }
            val histChecksum = ImgUtils.getHistogramChecksum(
                normalizedBlueMap, normalizedGreenMap,
                normalizedRedMap, analysisMetadata.parentPath
            )
            var matchedDatapointsBlue = 0
            var matchedDatapointsGreen = 0
            var matchedDatapointsRed = 0

            val similarFilePaths = mutableSetOf<String>()
            val similarFilesMetadata = mutableSetOf<SimilarImagesAnalysisMetadata>()
            val existingAnalysedData =
                similarImagesAnalysisDao.findByHistogramChecksum(histChecksum)
            if (existingAnalysedData != null) {
                similarFilePaths.addAll(existingAnalysedData.files)
            }

            for (currentFile in parentFiles) {
                if (currentFile.filePath != analysisMetadata.filePath && !currentFile.isAnalysed) {
                    currentFile.blueChannel.forEach {
                        if (normalizedBlueMap[
                            it.first /
                                ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                        ]
                            == it.second /
                            ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
                        ) {
                            matchedDatapointsBlue++
                        }
                        if (matchedDatapointsBlue
                            >= ImgUtils.ASSERT_DATAPOINTS
                        ) {
                            log.info(
                                "matched datapoints for blue channel for path {} and {}",
                                analysisMetadata.filePath, currentFile.filePath
                            )
                            return@forEach
                        }
                    }
                    currentFile.greenChannel.forEach {
                        if (normalizedGreenMap[
                            it.first /
                                ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                        ]
                            == it.second /
                            ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
                        ) {
                            matchedDatapointsGreen++
                        }
                        if (matchedDatapointsGreen
                            >= ImgUtils.ASSERT_DATAPOINTS
                        ) {
                            log.info(
                                "matched datapoints for green channel for path {} and {}",
                                analysisMetadata.filePath, currentFile.filePath
                            )
                            return@forEach
                        }
                    }
                    currentFile.redChannel.forEach {
                        if (normalizedRedMap[
                            it.first /
                                ImgUtils.PIXEL_POSITION_NORMALIZE_FACTOR
                        ]
                            == it.second /
                            ImgUtils.PIXEL_INTENSITY_NORMALIZE_FACTOR
                        ) {
                            matchedDatapointsRed++
                        }
                        if (matchedDatapointsRed
                            >= ImgUtils.ASSERT_DATAPOINTS
                        ) {
                            log.info(
                                "matched datapoints for red channel for path {} and {}",
                                analysisMetadata.filePath, currentFile.filePath
                            )
                            return@forEach
                        }
                    }
                    if (matchedDatapointsBlue >= ImgUtils.ASSERT_DATAPOINTS &&
                        matchedDatapointsGreen
                        >= ImgUtils.ASSERT_DATAPOINTS &&
                        matchedDatapointsRed
                        >= ImgUtils.ASSERT_DATAPOINTS
                    ) {
                        log.info(
                            "matched datapoints for all channels for path {} and {}",
                            analysisMetadata.filePath, currentFile.filePath
                        )
                        similarFilePaths.add(currentFile.filePath)
                        similarFilesMetadata.add(currentFile)
                    }
                    matchedDatapointsBlue = 0
                    matchedDatapointsGreen = 0
                    matchedDatapointsRed = 0
                }
            }
            if (similarFilePaths.size >= 1) {
                similarFilesMetadata.add(analysisMetadata)
                similarFilePaths.add(analysisMetadata.filePath)
                similarImagesAnalysisDao.insert(
                    SimilarImagesAnalysis(
                        histChecksum,
                        similarFilePaths
                    )
                )
            }
            similarFilesMetadata.forEach {
                it.isAnalysed = true
                similarAnalysisMetadataDao.insert(it)
            }
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
            aggregatedMediaFiles.imagesMediaFilesList?.let {
                val imagesList = ArrayList(it)
                imagesList.forEach {
                    image ->
                    if (isMediaStoreAnalysing) {
                        getMediaFileChecksumAndWriteToDatabase(dao, File(image.path + ""))
                    }
                }
            }
            aggregatedMediaFiles.audiosMediaFilesList?.let {
                val audiosList = ArrayList(it)
                audiosList.forEach {
                    audio ->
                    if (isMediaStoreAnalysing) {
                        getMediaFileChecksumAndWriteToDatabase(dao, File(audio.path + ""))
                    }
                }
            }
            aggregatedMediaFiles.videosMediaFilesList?.let {
                val videosList = ArrayList(it)
                videosList.forEach {
                    video ->
                    if (isMediaStoreAnalysing) {
                        getMediaFileChecksumAndWriteToDatabase(dao, File(video.path + ""))
                    }
                }
            }
            aggregatedMediaFiles.docsMediaFilesList?.let {
                val docsList = ArrayList(it)
                docsList.forEach {
                    docs ->
                    if (isMediaStoreAnalysing) {
                        getMediaFileChecksumAndWriteToDatabase(dao, File(docs.path + ""))
                    }
                }
            }
            isMediaStoreAnalysing = false
        }
    }

    /**
     * Fetch path preferences set by user about what paths should we search in
     */
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

    fun getShareLogsAdapter(): LiveData<ShareAdapter?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val outputPath = Utils.copyLogsFileToInternalStorage(applicationContext)
            if (outputPath != null) {
                log.info("Sharing logs file at path $outputPath")
                val logsFile = File(outputPath)
                val uri = FileProvider.getUriForFile(
                    applicationContext,
                    applicationContext.packageName, logsFile
                )
                emit(
                    getShareIntents(
                        Collections.singletonList(uri),
                        applicationContext
                    )
                )
            } else {
                log.warn("Failed to share logs file")
                emit(null)
            }
        }
    }

    fun getShareMediaFilesAdapter(mediaFileInfoList: List<MediaFileInfo>):
        LiveData<ShareAdapter?> {
        return getShareMediaFilesAdapterFromUriList(
            mediaFileInfoList.filter { it.getContentUri(applicationContext) != null }
                .map { it.getContentUri(applicationContext)!! }
        )
    }

    fun getShareMediaFilesAdapterFromUriList(mediaFileInfoList: List<Uri>):
        LiveData<ShareAdapter?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            log.info("Sharing media files $mediaFileInfoList")
            emit(
                getShareIntents(
                    mediaFileInfoList,
                    applicationContext
                )
            )
        }
    }
    fun deleteMediaFiles(mediaFileInfoList: List<MediaFileInfo>): LiveData<Pair<Int, Int>> {
        val deleteMediaFilesLiveData: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
        var successProcessedPair = Pair(0, 0)
        deleteMediaFilesLiveData.value = successProcessedPair
        viewModelScope.launch(Dispatchers.IO) {
            log.info("Deleting media files $mediaFileInfoList")
            val trashBinFilesList = mediaFileInfoList.map { it.toTrashBinFile() }

            getTrashBinInstance().deletePermanently(
                trashBinFilesList,
                object : DeletePermanentlyCallback {
                    override fun invoke(deletePath: String): Boolean {
                        val mediaFileInfo = mediaFileInfoList.find {
                            it.path == deletePath
                        }
                        if (mediaFileInfo != null) {
                            successProcessedPair = if (mediaFileInfo.delete()) {
                                successProcessedPair.copy(
                                    successProcessedPair.first + 1,
                                    successProcessedPair.second + 1
                                )
                            } else {
                                successProcessedPair.copy(
                                    successProcessedPair.first,
                                    successProcessedPair.second + 1
                                )
                            }

                            try {
                                Utils.deleteFromMediaDatabase(
                                    applicationContext,
                                    mediaFileInfo.path
                                )
                            } catch (e: Exception) {
                                log.warn("failed to delete media from system database", e)
                            } finally {
                                mediaFileInfo.getContentUri(applicationContext)?.let {
                                    uri ->
                                    FileUtils.scanFile(
                                        uri,
                                        mediaFileInfo.path,
                                        applicationContext
                                    )
                                }
                            }
                        } else {
                            successProcessedPair.copy(
                                successProcessedPair.first,
                                mediaFileInfoList.size
                            )
                        }
                        deleteMediaFilesLiveData.postValue(successProcessedPair)
                        return true
                    }
                },
                true
            )
        }
        return deleteMediaFilesLiveData
    }

    /**
     * Process compressed files, returned pair - first refers to compressed files,
     * second refers to compressed data
     */
    fun compressMediaFiles(
        mediaFileInfoList: List<MediaFileInfo>,
        compressQuality: Int,
        qualityFormat: Bitmap.CompressFormat,
        deleteOriginal: Boolean
    ): LiveData<Triple<Int, Long, MediaFileInfo?>> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            var successProcessedPair = Triple<Int, Long, MediaFileInfo?>(
                0, 0L,
                null
            )
            mediaFileInfoList.forEach {
                if (it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_IMAGE) {
                    log.info("Compressing image files $mediaFileInfoList")
                    try {
                        val file = File(it.path)
                        val newFile = File(
                            Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            "(COPY) ${file.name}"
                        )
                        if (file.exists()) {
                            val compressedImageFile = Compressor.compress(
                                applicationContext,
                                file
                            ) {
                                quality(compressQuality)
                                format(qualityFormat)
                                destination(newFile)
                            }
                            if (deleteOriginal) {
                                runBlocking {
                                    moveToBinLightWeight(arrayListOf(it))
                                }
                                it.getContentUri(applicationContext)?.let {
                                    originalUri ->
                                    FileUtils.scanFile(
                                        originalUri,
                                        it.path,
                                        applicationContext
                                    )
                                }
                                log.info("deleted original image file {}", it.path)
                            }
                            val uri = FileProvider.getUriForFile(
                                applicationContext,
                                applicationContext.packageName, compressedImageFile
                            )
                            FileUtils.scanFile(
                                uri,
                                it.path,
                                applicationContext
                            )
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first + 1,
                                successProcessedPair.second + it.longSize -
                                    newFile.length(),
                                MediaFileInfo.fromFile(
                                    newFile,
                                    MediaFileInfo.ExtraInfo(
                                        MediaFileInfo.MEDIA_TYPE_IMAGE,
                                        null, null, null
                                    )
                                )
                            )
                            emit(successProcessedPair)
                        } else {
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first + 1,
                                successProcessedPair.second, null
                            )
                            emit(successProcessedPair)
                        }
                    } catch (e: Exception) {
                        log.warn("failed to compress image file at {}", it.path)
                        successProcessedPair = successProcessedPair.copy(
                            successProcessedPair.first + 1,
                            successProcessedPair.second, null
                        )
                        emit(successProcessedPair)
                    }
                }
            }
        }
    }

    fun compressMediaFiles(
        mediaFileInfoList: List<MediaFileInfo>,
        quality: VideoQuality,
        disableAudio: Boolean,
        deleteOriginal: Boolean,
        onProgressCallback: (ConcurrentHashMap<String, Int>) -> (Unit),
        onCompleteCallback: (Triple<Int, Long, MediaFileInfo?>) -> (Unit)
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            log.warn("compressing video failed not supported in version lower than N")
            return
        }
        var successProcessedPair = Triple<Int, Long, MediaFileInfo?>(0, 0L, null)
        val progressMap = ConcurrentHashMap<String, Int>()
        mediaFileInfoList.forEach {
            if (it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_VIDEO) {
                log.info("Compressing image files $mediaFileInfoList")
                try {
                    it.getContentUri(applicationContext)?.let { contentUri ->
                        VideoCompressor.start(
                            applicationContext,
                            arrayListOf(contentUri),
                            true,
                            SharedStorageConfiguration(
                                saveAt = SaveLocation.movies,
                            ),
                            configureWith = Configuration(
                                videoNames = arrayListOf(it.title),
                                quality = quality,
                                isMinBitrateCheckEnabled = false,
                                disableAudio = disableAudio,
                                keepOriginalResolution = true
                            ),
                            listener = object : CompressionListener {
                                override fun onProgress(index: Int, percent: Float) {
                                    runBlocking {
                                        val currTime = System.currentTimeMillis() / 1000
                                        progressMap[it.title] = percent.toInt()
                                        if (currTime % 5 == 0L) {
                                            // publish update every 5 sec
                                            onProgressCallback.invoke(progressMap)
                                        }
                                    }
                                }

                                override fun onStart(index: Int) {
                                    // do nothing
                                }

                                override fun onSuccess(index: Int, size: Long, path: String?) {
                                    if (path != null) {
                                        if (deleteOriginal) {
                                            runBlocking {
                                                moveToBinLightWeight(arrayListOf(it))
                                            }
                                            FileUtils.scanFile(
                                                contentUri,
                                                path,
                                                applicationContext
                                            )
                                        }
                                        val file = File(path)
                                        val uri = FileProvider.getUriForFile(
                                            applicationContext,
                                            applicationContext.packageName, file
                                        )
                                        FileUtils.scanFile(
                                            uri,
                                            path,
                                            applicationContext
                                        )
                                        successProcessedPair = successProcessedPair.copy(
                                            successProcessedPair.first + 1,
                                            successProcessedPair.second + it.longSize -
                                                size,
                                            MediaFileInfo.fromFile(
                                                file,
                                                MediaFileInfo.ExtraInfo(
                                                    MediaFileInfo.MEDIA_TYPE_VIDEO,
                                                    null, null,
                                                    null
                                                )
                                            )
                                        )
                                    } else {
                                        successProcessedPair = successProcessedPair.copy(
                                            successProcessedPair.first + 1,
                                            successProcessedPair.second + it.longSize -
                                                size,
                                            null
                                        )
                                    }
                                    onCompleteCallback.invoke(successProcessedPair)
                                }

                                override fun onFailure(index: Int, failureMessage: String) {
                                    successProcessedPair = successProcessedPair.copy(
                                        successProcessedPair.first + 1,
                                        successProcessedPair.second, null
                                    )
                                    onCompleteCallback.invoke(successProcessedPair)
                                }

                                override fun onCancelled(index: Int) {
                                    // do nothing
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    log.warn("failed to compress video file at {}", it.path)
                    successProcessedPair = successProcessedPair.copy(
                        successProcessedPair.first + 1,
                        successProcessedPair.second
                    )
                    onCompleteCallback.invoke(successProcessedPair)
                }
            }
        }
    }

    fun moveToTrashBin(mediaFileInfoList: List<MediaFileInfo>): LiveData<Pair<Int, Int>> {
        val moveToTrashLiveData: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
        var successProcessedPair = Pair(0, 0)
        moveToTrashLiveData.value = successProcessedPair
        viewModelScope.launch(Dispatchers.IO) {
            log.info("Moving media files to bin $mediaFileInfoList")
            val trashBinFilesList = mediaFileInfoList.map { it.toTrashBinFile() }
            getTrashBinInstance().moveToBin(
                trashBinFilesList, true,
                object : MoveFilesCallback {
                    override fun invoke(
                        originalFilePath: String,
                        trashBinDestination: String
                    ): Boolean {
                        val source = File(originalFilePath)
                        val dest = File(trashBinDestination)
                        if (!source.renameTo(dest)) {
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first,
                                successProcessedPair.second + 1
                            )
                            moveToTrashLiveData.postValue(successProcessedPair)
                            return false
                        } else {
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first + 1,
                                successProcessedPair.second + 1
                            )
                            val uri = FileProvider.getUriForFile(
                                applicationContext,
                                applicationContext.packageName, File(originalFilePath)
                            )
                            FileUtils.scanFile(
                                uri,
                                originalFilePath,
                                applicationContext
                            )
                            moveToTrashLiveData.postValue(successProcessedPair)
//                        return@moveToBin true
                            return true
                        }
                    }
                }
            )
        }
        return moveToTrashLiveData
    }

    private fun moveToBinLightWeight(mediaFileInfoList: List<MediaFileInfo>) {
        val trashBinFilesList = mediaFileInfoList.map { it.toTrashBinFile() }
        getTrashBinInstance().moveToBin(
            trashBinFilesList, true,
            object : MoveFilesCallback {
                override fun invoke(
                    originalFilePath: String,
                    trashBinDestination: String
                ): Boolean {
                    val source = File(originalFilePath)
                    val dest = File(trashBinDestination)
                    if (!source.renameTo(dest)) {
                        return false
                    }
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName, File(originalFilePath)
                    )
                    FileUtils.scanFile(
                        uri,
                        originalFilePath,
                        applicationContext
                    )
                    return true
                }
            }
        )
    }

    fun restoreFromBin(mediaFileInfoList: List<MediaFileInfo>): LiveData<Pair<Int, Int>> {
        val restoreFromTrashLiveData: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
        var successProcessedPair = Pair(0, 0)
        restoreFromTrashLiveData.value = successProcessedPair
        viewModelScope.launch(Dispatchers.IO) {
            log.info("Moving media files to bin $mediaFileInfoList")
            val trashBinFilesList = mediaFileInfoList.map { it.toTrashBinFile() }
            getTrashBinInstance().restore(
                trashBinFilesList, true,
                object : MoveFilesCallback {
                    override fun invoke(source: String, dest: String): Boolean {
                        val sourceFile = File(source)
                        val destFile = File(dest)
                        if (!sourceFile.renameTo(destFile)) {
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first,
                                successProcessedPair.second + 1
                            )
                            restoreFromTrashLiveData.postValue(successProcessedPair)
                            return false
                        } else {
                            successProcessedPair = successProcessedPair.copy(
                                successProcessedPair.first + 1,
                                successProcessedPair.second + 1
                            )
                            val uri = FileProvider.getUriForFile(
                                applicationContext,
                                applicationContext.packageName, File(dest)
                            )
                            FileUtils.scanFile(
                                uri,
                                dest,
                                applicationContext
                            )
                            restoreFromTrashLiveData.postValue(successProcessedPair)
                            return true
                        }
                    }
                }
            )
        }
        return restoreFromTrashLiveData
    }

    fun getMediaFileListSize(mediaFileInfoList: List<MediaFileInfo>): LiveData<Long> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            var size = 0L
            mediaFileInfoList.forEachIndexed { index, mediaFileInfo ->
                size += mediaFileInfo.longSize
                if (index % 5 == 0) {
                    emit(size)
                }
            }
            emit(size)
        }
    }

    /*var uniqueDeviceId = ""
    fun getAndSaveUniqueDeviceId() {
        viewModelScope.launch(Dispatchers.Default) {
            applicationContext.getExternalStorageDirectory()?.let { internalStoragePath ->
                val basePath = "${internalStoragePath.path}/" +
                        "${TransferFragment.RECEIVER_BASE_PATH}/" +
                        TransferFragment.NO_MEDIA
                val baseFile = File(basePath)
                val uniqueFile = File(baseFile, TransferFragment.ID_LOG)
                if (!uniqueFile.exists()) {
                    baseFile.mkdirs()
                    var `in`: InputStream? = null
                    var out: OutputStream? = null
                    try {
                        val id = UUID.randomUUID().toString()
                        `in` = id.byteInputStream(Charset.defaultCharset())
                        out = FileOutputStream(uniqueFile)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (`in`.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                        uniqueDeviceId = id
                    } finally {
                        out!!.close()
                        `in`!!.close()
                    }
                } else {
                    val fileReader = FileReader(uniqueFile)
                    BufferedReader(fileReader).use { bufferedReader ->
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            uniqueDeviceId = line ?: ""
                            break
                        }
                    }
                }
            }
        }
    }*/

    /*fun getUniqueId2(): String? {
        applicationContext.getExternalStorageDirectory()?.let { internalStoragePath ->
            val basePath = "${internalStoragePath.path}/" +
                    "${TransferFragment.RECEIVER_BASE_PATH}/" +
                    TransferFragment.NO_MEDIA
            val baseFile = File(basePath)
            val uniqueFile = File(baseFile, TransferFragment.ID_LOG)
            if (uniqueFile.exists()) {
                val fileReader = FileReader(uniqueFile)
                BufferedReader(fileReader).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        return line
                    }
                }
            } else {
                return null
            }
        }
        return null
    }*/

    fun getUniqueId(): LiveData<String> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            var secureId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            for (i in 1..7) {
                // hash secret 7 times
                if (i % 2 == 0) {
                    // add salt alternatively
                    secureId + uniqueIdSalt
                }
                secureId = FileUtils
                    .getSHA256Checksum(secureId.byteInputStream(Charset.defaultCharset()))
            }
            applicationContext.getAppCommonSharedPreferences().edit()
                .putString(PreferencesConstants.KEY_DEVICE_UNIQUE_ID, secureId).apply()
            emit(secureId)
        }
    }

    fun checkInternetConnection(timeoutMs: Int): LiveData<Boolean> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            if (BuildConfig.IS_VERSION_FDROID) {
                emit(applicationContext.isNetworkAvailable())
            } else {
                val socket = Socket()
                try {
                    val socketAddress = InetSocketAddress("208.67.222.222", 53)
                    socket.connect(socketAddress, timeoutMs)
                    socket.close()
                    emit(true)
                } catch (ex: IOException) {
                    log.info("failed to ping for connection", ex)
                    emit(applicationContext.isNetworkAvailable())
                }
            }
        }
    }

    fun validateTrial(
        deviceId: String,
        isNetworkAvailable: Boolean,
        trialResponse: (TrialValidationApi.TrialResponse) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).trialValidatorDao()
            val trial = dao.findByDeviceId(deviceId)
            // in this method decide when do we refresh trial / subscription, immediately or after some time
            if (trial == null) {
                fetchBillingStatusAndInitTrial(deviceId, dao, isNetworkAvailable, trialResponse)
            } else {
                if ((
                    trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_EXPIRED ||
                        trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_INACTIVE ||
                        trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_UNOFFICIAL
                    //     || trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
                    // maybe done to force validate for exclusive refunded people, want to convert to custom exclusive
                    // but downside is this'll always check for valid license for customer exclusive people
                    ) &&
                    trial.subscriptionStatus == Trial.SUBSCRIPTION_STATUS_DEFAULT
                ) {
                    // check immediately if there's an update in trial status
                    fetchBillingStatusAndInitTrial(
                        deviceId, dao, isNetworkAvailable,
                        trialResponse
                    )
                } else {
                    // trial is active, if we've already processed today, don't fetch
                    val cal = GregorianCalendar.getInstance()
                    cal.time = trial.fetchTime
                    cal.add(Calendar.DAY_OF_YEAR, 1)

                    val calWeek = GregorianCalendar.getInstance()
                    calWeek.time = trial.fetchTime
                    calWeek.add(Calendar.DAY_OF_YEAR, 7)
                    if ((
                        cal.time.before(Date()) && trial.trialStatus
                            != TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
                        ) ||
                        calWeek.time.before(Date()) &&
                        trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE
                    ) {
                        // fetch if membership liftime once every week. else fetch everyday.
                        fetchBillingStatusAndInitTrial(
                            deviceId, dao, isNetworkAvailable,
                            trialResponse
                        )
                    } else {
                        // we've fetcehd today, consider it active
                        trialResponse.invoke(
                            TrialValidationApi.TrialResponse(
                                false, false,
                                TrialValidationApi.TrialResponse
                                    .trialCodeStatusMap[trial.trialStatus]
                                    ?: TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
                                trial.trialDaysLeft,
                                trial.subscriptionStatus,
                                trial.purchaseToken
                            )
                        )
                    }
                }
            }
        }
    }

    fun getPaletteColors(drawable: Drawable): LiveData<Pair<Int, Int>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val bitmap = drawable.toBitmap()
            val color = Utils.getColor(
                Utils.generatePalette(bitmap),
                applicationContext.resources.getColor(R.color.navy_blue_alt_3),
                applicationContext.resources.getColor(R.color.navy_blue_alt)
            )
            emit(color)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getUnusedApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (unusedAppsLiveData == null) {
            unusedAppsLiveData = MutableLiveData()
            unusedAppsLiveData?.value = null
            processUnusedApps(applicationContext.packageManager)
        }
        return unusedAppsLiveData!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun processUnusedApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_UNUSED_APPS_DAYS,
                PreferencesConstants.DEFAULT_UNUSED_APPS_DAYS
            )
            val usageStats = Utils.getAppsUsageStats(applicationContext, days)
            val usageStatsPackages = usageStats.filter {
                it.lastTimeUsed != 0L || it.packageName == applicationContext.packageName
            }.map {
                it.packageName
            }.toSet()
            val unusedAppsList =
                allApps.get()?.filter {
                    !usageStatsPackages.contains(it.first.packageName)
                }?.mapNotNull {
                    MediaFileInfo.fromApplicationInfo(applicationContext, it.first, it.second)
                }
            unusedAppsLiveData?.postValue(unusedAppsList?.let { ArrayList(it) })
        }
    }

    fun getNetworkIntensiveApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (networkIntensiveAppsLiveData == null) {
            networkIntensiveAppsLiveData = MutableLiveData()
            networkIntensiveAppsLiveData?.value = null
            processNetworkIntensiveApps(applicationContext.packageManager)
        }
        return networkIntensiveAppsLiveData!!
    }

    fun resetTrashBinConfig() {
        trashBinConfig = null
        trashBin = null
        trashBinFilesLiveData = null
    }

    private fun processNetworkIntensiveApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)

            val priorityQueue = PriorityQueue<MediaFileInfo>(
                50
            ) { o1, o2 ->
                o2.extraInfo?.apkMetaData?.networkBytes?.let {
                    o1.extraInfo?.apkMetaData?.networkBytes?.compareTo(
                        it
                    )
                } ?: 0
            }

            allApps.get()?.forEachIndexed { index, applicationInfo ->
                if (index > 49 && priorityQueue.isNotEmpty()) {
                    priorityQueue.remove()
                }
                MediaFileInfo.fromApplicationInfo(
                    applicationContext, applicationInfo.first,
                    applicationInfo.second
                )?.let {
                    priorityQueue.add(it)
                }
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            networkIntensiveAppsLiveData?.postValue(ArrayList(result.reversed()))
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getMostUsedApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (mostUsedAppsLiveData == null) {
            mostUsedAppsLiveData = MutableLiveData()
            mostUsedAppsLiveData?.value = null
            processMostUsedApps(applicationContext.packageManager)
        }
        return mostUsedAppsLiveData!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun processMostUsedApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_MOST_USED_APPS_DAYS,
                PreferencesConstants.DEFAULT_MOST_USED_APPS_DAYS
            )
            val usageStats = Utils.getAppsUsageStats(applicationContext, days)
            val freqMap = linkedMapOf<String, Long>()
            usageStats.filter {
                it.lastTimeUsed != 0L
            }.forEach {
                if (!freqMap.contains(it.packageName)) {
                    freqMap[it.packageName] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        it.totalTimeVisible else it.totalTimeInForeground
                } else {
                    freqMap[it.packageName] = freqMap[it.packageName]!! +
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            it.totalTimeVisible else it.totalTimeInForeground
                }
            }
            val mostUsedAppsListRaw = arrayListOf<String>()
            freqMap.entries.stream()
                .sorted { o1, o2 -> -1 * o1.value.compareTo(o2.value) }
                .forEach {
                    mostUsedAppsListRaw.add(it.key)
                }
            val mostUsedApps = arrayListOf<MediaFileInfo>()
            mostUsedAppsListRaw.forEach {
                appName ->
                allApps.get()?.find {
                    it.first.packageName.equals(appName, true)
                }?.let {
                    MediaFileInfo.fromApplicationInfo(
                        applicationContext,
                        it.first, it.second
                    )?.let {
                        mediaFileInfo ->
                        mostUsedApps.add(mediaFileInfo)
                    }
                }
            }
            mostUsedAppsLiveData?.postValue(mostUsedApps)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getLeastUsedApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (leastUsedAppsLiveData == null) {
            leastUsedAppsLiveData = MutableLiveData()
            leastUsedAppsLiveData?.value = null
            processLeastUsedApps(applicationContext.packageManager)
        }
        return leastUsedAppsLiveData!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun processLeastUsedApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_LEAST_USED_APPS_DAYS,
                PreferencesConstants.DEFAULT_LEAST_USED_APPS_DAYS
            )
            val usageStats = Utils.getAppsUsageStats(applicationContext, days)
            val freqMap = linkedMapOf<String, Long>()
            usageStats.filter {
                it.lastTimeUsed != 0L && it.packageName != applicationContext.packageName
            }.forEach {
                if (!freqMap.contains(it.packageName)) {
                    freqMap[it.packageName] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        it.totalTimeVisible else it.totalTimeInForeground
                } else {
                    freqMap[it.packageName] = freqMap[it.packageName]!! +
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            it.totalTimeVisible else it.totalTimeInForeground
                }
            }
            val leastUsedAppsListRaw = arrayListOf<String>()
            freqMap.entries.stream()
                .sorted { o1, o2 -> o1.value.compareTo(o2.value) }
                .forEach {
                    leastUsedAppsListRaw.add(it.key)
                }
            val leastUsedApps = arrayListOf<MediaFileInfo>()
            leastUsedAppsListRaw.forEach {
                appName ->
                allApps.get()?.find {
                    it.first.packageName.equals(appName, true)
                }?.let {
                    MediaFileInfo.fromApplicationInfo(
                        applicationContext, it.first,
                        it.second
                    )?.let {
                        mediaFileInfo ->
                        leastUsedApps.add(mediaFileInfo)
                    }
                }
            }
            leastUsedAppsLiveData?.postValue(leastUsedApps)
        }
    }

    fun getLargeApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (largeAppsLiveData == null) {
            largeAppsLiveData = MutableLiveData()
            largeAppsLiveData?.value = null
            processLargeApps(applicationContext.packageManager)
        }
        return largeAppsLiveData!!
    }

    private fun processLargeApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)

            val priorityQueue = PriorityQueue<MediaFileInfo>(
                50
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }

            allApps.get()?.forEachIndexed { index, applicationInfo ->
                if (index > 49 && priorityQueue.isNotEmpty()) {
                    priorityQueue.remove()
                }
                MediaFileInfo.fromApplicationInfo(
                    applicationContext, applicationInfo.first,
                    applicationInfo.second
                )?.let {
                    priorityQueue.add(it)
                }
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            largeAppsLiveData?.postValue(ArrayList(result.reversed()))
        }
    }

    fun getNewlyInstalledApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (newlyInstalledAppsLiveData == null) {
            newlyInstalledAppsLiveData = MutableLiveData()
            newlyInstalledAppsLiveData?.value = null
            processNewlyInstalledApps(applicationContext.packageManager)
        }
        return newlyInstalledAppsLiveData!!
    }

    private fun processNewlyInstalledApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_NEWLY_INSTALLED_APPS_DAYS,
                PreferencesConstants.DEFAULT_NEWLY_INSTALLED_APPS_DAYS
            )
            val pastDate = LocalDateTime.now().minusDays(days.toLong())
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                50
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }

            allApps.get()?.filter {
                if (it.second == null) return@filter false
                val installDateTime = Instant.ofEpochMilli(it.second?.firstInstallTime!!)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                installDateTime.isAfter(pastDate.toLocalDate())
            }?.forEachIndexed { index, applicationInfo ->
                if (index > 49 && priorityQueue.isNotEmpty()) {
                    priorityQueue.remove()
                }
                MediaFileInfo.fromApplicationInfo(
                    applicationContext, applicationInfo.first,
                    applicationInfo.second
                )?.let {
                    priorityQueue.add(it)
                }
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            newlyInstalledAppsLiveData?.postValue(ArrayList(result.reversed()))
        }
    }

    fun getRecentlyUpdatedApps(): LiveData<ArrayList<MediaFileInfo>?> {
        if (recentlyUpdatedAppsLiveData == null) {
            recentlyUpdatedAppsLiveData = MutableLiveData()
            recentlyUpdatedAppsLiveData?.value = null
            processRecentlyUpdatedApps(applicationContext.packageManager)
        }
        return recentlyUpdatedAppsLiveData!!
    }

    private fun processRecentlyUpdatedApps(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()
            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_RECENTLY_UPDATED_APPS_DAYS,
                PreferencesConstants.DEFAULT_RECENTLY_UPDATED_APPS_DAYS
            )
            val pastDate = LocalDateTime.now().minusDays(days.toLong())
            val priorityQueue = PriorityQueue<MediaFileInfo>(
                50
            ) { o1, o2 -> o1.longSize.compareTo(o2.longSize) }

            allApps.get()?.filter {
                if (it.second == null) return@filter false
                val updateDateTime = Instant.ofEpochMilli(it.second?.lastUpdateTime!!)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                updateDateTime.isAfter(pastDate.toLocalDate())
            }?.forEachIndexed { index, applicationInfo ->
                if (index > 49 && priorityQueue.isNotEmpty()) {
                    priorityQueue.remove()
                }
                MediaFileInfo.fromApplicationInfo(
                    applicationContext, applicationInfo.first,
                    applicationInfo.second
                )?.let {
                    priorityQueue.add(it)
                }
            }

            val result = ArrayList<MediaFileInfo>()
            while (!priorityQueue.isEmpty()) {
                priorityQueue.remove()?.let {
                    result.add(it)
                }
            }
            recentlyUpdatedAppsLiveData?.postValue(ArrayList(result.reversed()))
        }
    }

    fun getJunkFilesLiveData(): LiveData<Pair<ArrayList<MediaFileInfo>, String>?> {
        if (junkFilesLiveData == null) {
            junkFilesLiveData = MutableLiveData()
            junkFilesLiveData?.value = null
            processJunkFiles(applicationContext.packageManager)
        }
        return junkFilesLiveData!!
    }

    private fun processJunkFiles(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            val dao = AppDatabase.getInstance(applicationContext).installedAppsDao()
            val savedInstalledApps = dao.findAll().filter {
                savedAppData ->
                allApps.get()?.any {
                    !it.first.packageName.equals(savedAppData.packageName)
                } == false
            }
            log.info("found following apps not installed {}", savedInstalledApps)
            val result = ArrayList<MediaFileInfo>()
            savedInstalledApps.forEach {
                savedApp ->
                savedApp.dataDirs.forEach {
                    result.add(
                        MediaFileInfo.fromFile(
                            File(it),
                            MediaFileInfo.ExtraInfo(
                                MediaFileInfo.MEDIA_TYPE_UNKNOWN,
                                null, null, null
                            )
                        )
                    )
                }
            }
            var size = 0L
            result.forEach {
                size += it.longSize
            }
            junkFilesLiveData?.postValue(
                Pair(
                    result,
                    Formatter.formatFileSize(applicationContext, size)
                )
            )
        }
    }

    fun getApksLiveData(): LiveData<ArrayList<MediaFileInfo>?> {
        if (apksLiveData == null) {
            apksLiveData = MutableLiveData()
            apksLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                if (allMediaFilesPair == null) {
                    allMediaFilesPair = CursorUtils.listAll(applicationContext)
                }
                allMediaFilesPair?.filter {
                    it.path.endsWith(".apk")
                }?.map {
                    it.extraInfo?.mediaType = MediaFileInfo.MEDIA_TYPE_UNKNOWN
                    it
                }?.let {
                    apksLiveData?.postValue(ArrayList(it))
                }
            }
        }
        return apksLiveData!!
    }

    fun getHiddenFilesLiveData(): LiveData<ArrayList<MediaFileInfo>?> {
        if (hiddenFilesLiveData == null) {
            hiddenFilesLiveData = MutableLiveData()
            hiddenFilesLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                if (allMediaFilesPair == null) {
                    allMediaFilesPair = CursorUtils.listAll(applicationContext)
                }
                allMediaFilesPair?.filter {
                    it.title.startsWith(".")
                }?.sortedByDescending { it.longSize }?.map {
                    it.extraInfo?.mediaType = MediaFileInfo.MEDIA_TYPE_UNKNOWN
                    it
                }?.let {
                    hiddenFilesLiveData?.postValue(ArrayList(it))
                }
            }
        }
        return hiddenFilesLiveData!!
    }

    fun getLargeFilesLiveData(): LiveData<ArrayList<MediaFileInfo>?> {
        if (largeFilesMutableLiveData == null) {
            largeFilesMutableLiveData = MutableLiveData()
            largeFilesMutableLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                largeFilesMutableLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> o1.longSize.compareTo(o2.longSize) },
                        emptyList(), 1000
                    )
                )
            }
        }
        return largeFilesMutableLiveData!!
    }

    fun getWhatsappMediaLiveData(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (whatsappMediaMutableLiveData == null) {
            whatsappMediaMutableLiveData = MutableLiveData()
            whatsappMediaMutableLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_WHATSAPP)
                whatsappMediaMutableLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> o1.date.compareTo(o2.date) },
                        prefPaths.stream().map { it.path }.toList(), 1000
                    )
                )
            }
        }
        return whatsappMediaMutableLiveData!!
    }

    fun getTelegramMediaFiles(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (telegramMediaMutableLiveData == null) {
            telegramMediaMutableLiveData = MutableLiveData()
            telegramMediaMutableLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_TELEGRAM)
                telegramMediaMutableLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> o1.date.compareTo(o2.date) },
                        prefPaths.stream().map { it.path }.toList(), 1000
                    )
                )
            }
        }
        return telegramMediaMutableLiveData!!
    }

    fun getLargeDownloads(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (largeDownloadsLiveData == null) {
            largeDownloadsLiveData = MutableLiveData()
            largeDownloadsLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
                largeDownloadsLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> o1.longSize.compareTo(o2.longSize) },
                        prefPaths.stream().map { it.path }.toList(), 100
                    )
                )
            }
        }
        return largeDownloadsLiveData!!
    }

    fun getOldDownloads(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (oldDownloadsLiveData == null) {
            oldDownloadsLiveData = MutableLiveData()
            oldDownloadsLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)
                oldDownloadsLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> -1 * o1.date.compareTo(o2.date) },
                        prefPaths.stream().map { it.path }.toList(), 100
                    )
                )
            }
        }
        return oldDownloadsLiveData!!
    }

    fun getOldScreenshots(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (oldScreenshotsLiveData == null) {
            oldScreenshotsLiveData = MutableLiveData()
            oldScreenshotsLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS)
                oldScreenshotsLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> -1 * o1.date.compareTo(o2.date) },
                        prefPaths.stream().map { it.path }.toList(), 100
                    )
                )
            }
        }
        return oldScreenshotsLiveData!!
    }

    fun getOldRecordings(dao: PathPreferencesDao): LiveData<ArrayList<MediaFileInfo>?> {
        if (oldRecordingsLiveData == null) {
            oldRecordingsLiveData = MutableLiveData()
            oldRecordingsLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                val prefPaths = dao.findByFeature(PathPreferences.FEATURE_ANALYSIS_RECORDING)
                oldRecordingsLiveData?.postValue(
                    getMediaFilesWithFilter(
                        { o1, o2 -> -1 * o1.date.compareTo(o2.date) },
                        prefPaths.stream().map { it.path }.toList(), 100
                    )
                )
            }
        }
        return oldRecordingsLiveData!!
    }

    private fun getMediaFilesWithFilter(
        sortBy: Comparator<MediaFileInfo>,
        paths: List<String>,
        limit: Int
    ): ArrayList<MediaFileInfo> {
        val priorityQueue = PriorityQueue(limit, sortBy)
        if (allMediaFilesPair == null) {
            allMediaFilesPair = CursorUtils.listAll(applicationContext)
        }
        allMediaFilesPair?.filter {
            paths.isEmpty() || paths.stream().anyMatch {
                pathPref ->
                it.path.contains(pathPref, true)
            }
        }?.forEach {
            if (priorityQueue.isNotEmpty() && priorityQueue.size > limit - 1) {
                priorityQueue.remove()
            }
            priorityQueue.add(it)
        }

        val result = ArrayList<MediaFileInfo>()
        while (!priorityQueue.isEmpty()) {
            priorityQueue.remove()?.let {
                result.add(it)
            }
        }
        return ArrayList(result.reversed())
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getGamesInstalled(): LiveData<ArrayList<MediaFileInfo>?> {
        if (gamesInstalledLiveData == null) {
            gamesInstalledLiveData = MutableLiveData()
            gamesInstalledLiveData?.value = null
            processGamesInstalled(applicationContext.packageManager)
        }
        return gamesInstalledLiveData!!
    }

    fun getTrashBinInstance(): TrashBin {
        if (trashBin == null) {
            trashBin = TrashBin(
                context = applicationContext,
                true,
                getTrashbinConfig(),
                object : DeletePermanentlyCallback {
                    override fun invoke(deletePath: String): Boolean {
                        viewModelScope.launch(Dispatchers.IO) {
                            FileUtils.deleteFileByPath(applicationContext, deletePath)
                        }
                        return true
                    }
                },
                null
            )
        }
        return trashBin!!
    }

    fun progressTrashBinFilesLiveData(): LiveData<MutableList<MediaFileInfo>?> {
        if (trashBinFilesLiveData == null) {
            trashBinFilesLiveData = MutableLiveData()
            trashBinFilesLiveData?.value = null
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    trashBinFilesLiveData?.postValue(
                        ArrayList(
                            getTrashBinInstance().listFilesInBin()
                                .map {
                                    MediaFileInfo.fromTrashBinFile(it, getTrashbinConfig())
                                }
                        )
                    )
                } catch (e: NullPointerException) {
                    log.warn("failed to init trashbin livedata", e)
                    val metadataPath = File(TRASH_BIN_BASE_PATH, "metadata.json")
                    metadataPath.delete()
                }
            }
        }
        return trashBinFilesLiveData!!
    }

    fun getTrashbinConfig(): TrashBinConfig {
        if (trashBinConfig == null) {
            val sharedPrefs = applicationContext.getAppCommonSharedPreferences()

            val days = sharedPrefs.getInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_DAYS,
                TrashBinConfig.RETENTION_DAYS_INFINITE
            )
            val bytes = sharedPrefs.getLong(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_BYTES,
                TrashBinConfig.RETENTION_BYTES_INFINITE
            )
            val numOfFiles = sharedPrefs.getInt(
                PreferencesConstants.KEY_TRASH_BIN_RETENTION_NUM_OF_FILES,
                TrashBinConfig.RETENTION_NUM_OF_FILES
            )
            val interval = sharedPrefs.getInt(
                PreferencesConstants.KEY_TRASH_BIN_CLEANUP_INTERVAL_HOURS,
                TrashBinConfig.INTERVAL_CLEANUP_HOURS
            )
            trashBinConfig = TrashBinConfig(
                TRASH_BIN_BASE_PATH, days, bytes,
                numOfFiles,
                interval,
                false, true
            )
        }
        return trashBinConfig!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun processGamesInstalled(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)

            allApps.get()?.filter {
                Utils.applicationIsGame(it.first)
            }?.mapNotNull {
                MediaFileInfo.fromApplicationInfo(applicationContext, it.first, it.second)
            }?.let {
                gamesInstalledLiveData?.postValue(ArrayList(it))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getMemoryInfo(): LiveData<String?> {
        if (memoryInfoLiveData == null) {
            memoryInfoLiveData = MutableLiveData()
            memoryInfoLiveData?.value = null
            processMemoryUsage()
        }
        return memoryInfoLiveData!!
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun processMemoryUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val activityManager = applicationContext
                .getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalMemory = memInfo.totalMem
            val availMemory = memInfo.availMem
            val usageSummary = applicationContext.resources.getString(
                R.string.ram_usage_title,
                String.format("%s", Formatter.formatFileSize(applicationContext, availMemory)),
                String.format("%s", Formatter.formatFileSize(applicationContext, totalMemory))
            )
            memoryInfoLiveData?.postValue(usageSummary)
        }
    }

    fun killBackgroundProcesses(packageManager: PackageManager, callback: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)
            allApps.get()?.filter {
                it.first.packageName != applicationContext.packageName
            }?.forEach {
                val activityManager = applicationContext
                    .getSystemService(ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(it.first.packageName)
            }
            callback.invoke()
        }
    }

    private fun loadAllInstalledApps(packageManager: PackageManager) {
        if (allApps.get() == null) {
            try {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                log.warn("failed to load all installed applications", e)
                null
            }?.let {
                apps ->
                allApps.set(
                    apps.map {
                        val info: PackageInfo? = try {
                            packageManager.getPackageInfo(
                                it.packageName,
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                                    PackageManager.GET_SIGNATURES
                                else PackageManager.GET_SIGNING_CERTIFICATES
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            log.warn(
                                "failed to find package name {} while loading apps list",
                                it.packageName,
                                e
                            )
                            null
                        }
                        Pair(it, info)
                    }.filter {
                        val androidInfo: PackageInfo?
                        try {
                            androidInfo =
                                packageManager.getPackageInfo(
                                    "android",
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                                        PackageManager.GET_SIGNATURES
                                    else PackageManager.GET_SIGNING_CERTIFICATES
                                )
                            !Utils.isAppInSystemPartition(it.first) && (
                                it.second == null ||
                                    (
                                        !Utils.isSignedBySystem(it.second, androidInfo) &&
                                            !it.second!!.packageName
                                                .equals(applicationContext.packageName)
                                        )
                                )
                        } catch (e: PackageManager.NameNotFoundException) {
                            log.warn(
                                "failed to find package name {} while loading apps list",
                                it.first.packageName,
                                e
                            )
                            true
                        }
                    }
                )
                insertInstalledApps()
            }
        }
    }

    private fun insertInstalledApps() {
        allApps.get()?.let {
            infoListPair ->
            val installedApps = infoListPair.map {
                InstalledApps(it.first.packageName, listOf(it.first.sourceDir, it.first.dataDir))
            }
            val dao = AppDatabase.getInstance(applicationContext).installedAppsDao()
            dao.insert(installedApps)
        }
    }

    /**
     * If trial not null then save it's billing state in remote
     * Remote is never the source of truth for billing state, sending just for audit
     */
    private fun fetchAndSaveTrail(
        deviceId: String,
        dao: TrialValidatorDao,
        trial: Trial?
    ): TrialValidationApi.TrialResponse? {
        val trialResponse = getTrialResponse(deviceId, trial)
        return if (trialResponse != null) {
            /*val cal = GregorianCalendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -1)*/
            val saveTrial = Trial(
                deviceId, trialResponse.getTrialStatusCode(), trialResponse.trialDaysLeft,
                Date(), trialResponse.subscriptionStatus
            )
            saveTrial.purchaseToken = trialResponse.purchaseToken
            dao.insert(saveTrial)
            trialResponse
        } else null
    }

    private fun submitTrialToRemote(
        deviceId: String,
        dao: TrialValidatorDao,
        isNetworkAvailable: Boolean,
    ): TrialValidationApi.TrialResponse {
        val trial = dao.findByDeviceId(deviceId)
        val nullTrialResponse = TrialValidationApi.TrialResponse(
            false, false,
            TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
            Trial.TRIAL_DEFAULT_DAYS,
            Trial.SUBSCRIPTION_STATUS_DEFAULT,
            null
        )
        // for now always call remote to update subscription state
        if (isNetworkAvailable) {
            log.info("updated remote trial state")
            val fetchedTrialResponse = fetchAndSaveTrail(deviceId, dao, trial)
            if (fetchedTrialResponse == null) {
                if (trial == null) {
                    return nullTrialResponse
                } else {
                    return TrialValidationApi.TrialResponse(
                        false, false,
                        TrialValidationApi.TrialResponse
                            .trialCodeStatusMap[trial.trialStatus]
                            ?: TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
                        trial.trialDaysLeft,
                        trial.subscriptionStatus,
                        trial.purchaseToken
                    )
                }
            } else {
                return fetchedTrialResponse
            }
        } else {
            // no network available, no local info, return active temporarily
            if (trial == null) {
                log.warn(
                    "no network available to check trial, " +
                        "no local subscription status for {}",
                    deviceId
                )
                nullTrialResponse.isNotConnected = true
                return nullTrialResponse
            } else {
                log.info("no network available, return database saved trial state")
                return TrialValidationApi.TrialResponse(
                    false, false,
                    TrialValidationApi.TrialResponse
                        .trialCodeStatusMap[trial.trialStatus]
                        ?: TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
                    trial.trialDaysLeft,
                    trial.subscriptionStatus,
                    trial.purchaseToken,
                    true
                )
            }
        }
    }

    private fun fetchBillingStatusAndInitTrial(
        deviceId: String,
        dao: TrialValidatorDao,
        isNetworkAvailable: Boolean,
        trialResponse: (TrialValidationApi.TrialResponse) -> Unit
    ) {
        val billing = Billing.getInstance(applicationContext)
        if (billing != null) {
            billing.getSubscriptions {
                trialResponse.invoke(submitTrialToRemote(deviceId, dao, isNetworkAvailable))
            }
        } else {
            // unable to get billing, temporarily respond with success
            log.warn("unable to get billing info, try to fetch from remote regardless")
            trialResponse.invoke(submitTrialToRemote(deviceId, dao, isNetworkAvailable))
        }
    }

    private fun getTrialResponse(deviceId: String, trial: Trial?):
        TrialValidationApi.TrialResponse? {
        val retrofit = Retrofit.Builder()
            .baseUrl(TrialValidationApi.CLOUD_FUNCTION_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(Utils.getOkHttpClient())
            .build()
        val service = retrofit.create(TrialValidationApi::class.java)
        try {
            val subscriptionStatus = trial?.subscriptionStatus ?: Trial.SUBSCRIPTION_STATUS_DEFAULT
            val purchaseToken = trial?.purchaseToken
            service.postValidation(
                TrialValidationApi.TrialRequest(
                    TrialValidationApi.AUTH_TOKEN, deviceId,
                    applicationContext.packageName +
                        "_" + BuildConfig.API_REQ_TRIAL_APP_HASH,
                    subscriptionStatus,
                    purchaseToken
                )
            )?.execute()?.let { response ->
                return if (response.isSuccessful && response.body() != null) {
                    log.info("get trial response ${response.body()}")
                    response.body()
                } else {
                    log.warn(
                        "failed to get trial response code: ${response.code()} " +
                            "error: ${response.message()}"
                    )
                    null
                }
            }
        } catch (e: Exception) {
            log.warn("failed to contact function for trial validation", e)
            return null
        }
        log.warn("failed to call trial validation api")
        return null
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
        if (!isInternalStorageAnalysing || (
            !deepSearch && currentDepth
            > PreferencesConstants.DEFAULT_DUPLICATE_SEARCH_DEPTH_INCL
            )
        ) {
            return
        }
        if (file.isDirectory) {
            val filesInDir = file.listFiles()
            if (filesInDir == null) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, true, false,
                        currentDepth
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
            if (!file.exists()) {
                return
            }
            if (file.length() == 0L) {
                dao.insert(
                    InternalStorageAnalysis(
                        file.path, listOf(file.path),
                        true, false, false, false,
                        currentDepth
                    )
                )
            } else {
                val checksum = FileUtils.getSHA256Checksum(file.inputStream())
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
                            listOf(file.path), false, false, false,
                            false,
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
        if (!file.exists()) {
            log.info("file not found while calculating checksum at path {}", file.path)
            return
        }
        try {
            val checksum = FileUtils.getSHA256Checksum(file.inputStream())
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
                        listOf(file.path), false, false, false,
                        true,
                        0
                    )
                )
            }
        } catch (e: Exception) {
            log.warn("failed to get checksum and write to database", e)
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
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val metaInfoAndSummaryPair = CursorUtils
                .listImages(applicationContext.applicationContext)
            setMediaInfoSummary(metaInfoAndSummaryPair.first, storageSummary)
            emit(metaInfoAndSummaryPair)
        }
    }

    /**
     * Sets storage summary for individual images/videos/audio/docs from aggregated storagesummary
     * from internalStorageStats
     */
    private fun setMediaInfoSummary(
        summary: StorageSummary,
        storageSummary: StorageSummary
    ) {
        /*summary.progress = ((summary.usedSpace!! * 100) / storageSummary.totalSpace!!)
            .toInt()*/
        if (storageSummary.items != 0) {
            summary.progress = ((summary.items * 100) / storageSummary.items)
        }
        summary.totalSpace = storageSummary.totalSpace
        summary.totalUsedSpace = storageSummary.usedSpace
        summary.totalItems = storageSummary.items
    }

    private fun getAudiosSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val dao = AppDatabase.getInstance(applicationContext).pathPreferencesDao()
            val pathPreferences = dao.findByFeature(PathPreferences.FEATURE_AUDIO_PLAYER)
            val metaInfoAndSummaryPair = CursorUtils
                .listAudio(
                    applicationContext.applicationContext,
                    pathPreferences.map {
                        it.path
                    }
                )
            setMediaInfoSummary(metaInfoAndSummaryPair.first, storageSummary)
            emit(metaInfoAndSummaryPair)
            metaInfoAndSummaryPair.second.forEach {
                mediaFileInfo ->
                // load album arts lazily
                mediaFileInfo.extraInfo?.audioMetaData?.albumId?.let {
                    albumId ->
                    val albumUri = AudioUtils.getMediaStoreAlbumCoverUri(albumId)
                    val albumBitmap = AudioUtils
                        .getAlbumBitmap(applicationContext.applicationContext, albumUri)
                    mediaFileInfo.extraInfo?.audioMetaData?.albumArt = albumBitmap
                }
            }
        }
    }

    private fun getPlaylistsSummaryLiveData(storageSummary: StorageSummary?):
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(null)
            if (storageSummary == null) {
                return@liveData
            }
            val dao = AppDatabase.getInstance(applicationContext).pathPreferencesDao()
            val pathPreferences = dao.findByFeature(PathPreferences.FEATURE_AUDIO_PLAYER)
            val playlists = PlaylistLoader.getAllPlaylists(applicationContext)
            val playlistFiles = arrayListOf<MediaFileInfo>()
            var mediaStorageSummary: StorageSummary? = null
            playlists.forEach {
                if (it.id != -1L) {
                    val metaInfoAndSummaryPair = CursorUtils
                        .listPlaylists(
                            applicationContext.applicationContext,
                            it.id,
                            pathPreferences.map {
                                pathPrefs ->
                                pathPrefs.path
                            }
                        )
                    metaInfoAndSummaryPair.second.forEach {
                        mediaFileInfo ->
                        mediaFileInfo.extraInfo?.audioMetaData?.playlist = it
                    }
                    playlistFiles.addAll(metaInfoAndSummaryPair.second)
                    mediaStorageSummary = metaInfoAndSummaryPair.first
                } else {
                    log.warn("invalid playlist {}", it)
                }
            }
            mediaStorageSummary?.let {
                setMediaInfoSummary(it, storageSummary)
                emit(Pair(it, playlistFiles))
                playlistFiles.forEach {
                    mediaFileInfo ->
                    // load album arts lazily
                    mediaFileInfo.extraInfo?.audioMetaData?.albumId?.let {
                        albumId ->
                        val albumUri = AudioUtils.getMediaStoreAlbumCoverUri(albumId)
                        val albumBitmap = AudioUtils
                            .getAlbumBitmap(applicationContext.applicationContext, albumUri)
                        mediaFileInfo.extraInfo?.audioMetaData?.albumArt = albumBitmap
                    }
                }
            }
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
            setMediaInfoSummary(metaInfoAndSummaryPair.first, storageSummary)
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
            /*val metaInfoAndSummaryPair = CursorUtils
                .listDocs(applicationContext.applicationContext)*/
            if (allMediaFilesPair == null) {
                allMediaFilesPair = CursorUtils.listAll(applicationContext)
            }
            allMediaFilesPair?.let {
                pair ->
                var longSize = 0L
                var size = 0
                val mediaFiles = ArrayList<MediaFileInfo>()
                pair.filter {
                    mediaFileInfo ->
                    arrayListOf(
                        ".pdf", ".epub", ".docx", ".xps", ".oxps",
                        ".cbz", ".fb2", ".mobi"
                    ).stream()
                        .anyMatch { mediaFileInfo.path.endsWith(it) }
                }.forEach {
                    mediaFileInfo ->
                    longSize += mediaFileInfo.longSize
                    size++
                    mediaFiles.add(mediaFileInfo)
                }
                val docsSummary = StorageSummary(size, 0, longSize)
                emit(Pair(docsSummary, mediaFiles))
                setMediaInfoSummary(docsSummary, storageSummary)
            }
        }
    }

    data class StorageSummary(
        var items: Int,
        var progress: Int,
        var usedSpace: Long? = 0L,
        var totalUsedSpace: Long? = 0L,
        val freeSpace: Long? = 0L,
        var totalSpace: Long? = 0L,
        var totalItems: Int? = 0
    )
}
