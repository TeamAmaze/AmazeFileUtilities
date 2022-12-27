/*
 * Copyright (C) 2021-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioUtils
import com.amaze.fileutilities.audio_player.playlist.PlaylistLoader
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.BlurAnalysis
import com.amaze.fileutilities.home_page.database.ImageAnalysis
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysis
import com.amaze.fileutilities.home_page.database.InternalStorageAnalysisDao
import com.amaze.fileutilities.home_page.database.LowLightAnalysis
import com.amaze.fileutilities.home_page.database.MemeAnalysis
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.database.PathPreferencesDao
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.streams.toList

class FilesViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var isImageFeaturesAnalysing = true
    var isImageBlurAnalysing = true
    var isImageLowLightAnalysing = true
    var isImageMemesAnalysing = true
    var isInternalStorageAnalysing = true
    var isMediaStoreAnalysing = true
    var isCasting = false
    var castSetupSuccess = true
    var wifiIpAddress: String? = null
    val uniqueIdSalt = "#%36zkpCE2"

    var unusedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var mostUsedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var leastUsedAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeAppsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var apksLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var gamesInstalledLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeFilesMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var whatsappMediaMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var telegramMediaMutableLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var largeDownloadsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldDownloadsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldRecordingsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var oldScreenshotsLiveData: MutableLiveData<ArrayList<MediaFileInfo>?>? = null
    var allMediaFilesPair: ArrayList<MediaFileInfo>? = null

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

    private var allApps: AtomicReference<List<ApplicationInfo>?> = AtomicReference()

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
                    FileUtils.scanFile(uri, applicationContext)
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
                if (migrationPref < PathPreferences.MIGRATION_PREF_MAP[it] ?: 1) {
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
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            log.info("Deleting media files $mediaFileInfoList")
            var successProcessedPair = Pair(0, 0)
            mediaFileInfoList.forEachIndexed { index, mediaFileInfo ->
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
                    Utils.deleteFromMediaDatabase(applicationContext, mediaFileInfo.path)
                } catch (e: Exception) {
                    log.warn("failed to delete media from system database", e)
                    mediaFileInfo.getContentUri(applicationContext)?.let {
                        uri ->
                        FileUtils.scanFile(
                            uri,
                            applicationContext
                        )
                    }
                }
                emit(successProcessedPair)
            }
        }
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
            try {
                val socket = Socket()
                val socketAddress = InetSocketAddress("8.8.8.8", 53)

                socket.connect(socketAddress, timeoutMs)
                socket.close()
                emit(true)
            } catch (ex: IOException) {
                log.warn("failed to ping for connection", ex)
                emit(applicationContext.isNetworkAvailable())
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
                    fetchBillingStatusAndInitTrial(deviceId, dao, isNetworkAvailable, trialResponse)
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
                it.lastTimeUsed != 0L
            }.map {
                it.packageName
            }.toSet()
            val unusedAppsList =
                allApps.get()?.filter { !usageStatsPackages.contains(it.packageName) }?.mapNotNull {
                    MediaFileInfo.fromApplicationInfo(applicationContext, it)
                }
            unusedAppsLiveData?.postValue(unusedAppsList?.let { ArrayList(it) })
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
                    it.packageName.equals(appName, true)
                }?.let {
                    MediaFileInfo.fromApplicationInfo(applicationContext, it)?.let {
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
                    it.packageName.equals(appName, true)
                }?.let {
                    MediaFileInfo.fromApplicationInfo(applicationContext, it)?.let {
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
                if (index > 49) {
                    priorityQueue.remove()
                }
                MediaFileInfo.fromApplicationInfo(applicationContext, applicationInfo)?.let {
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
                    it.extraInfo?.mediaType = MediaFileInfo.MEDIA_TYPE_APK
                    it
                }?.let {
                    apksLiveData?.postValue(ArrayList(it))
                }
            }
        }
        return apksLiveData!!
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
            if (priorityQueue.size > limit - 1) {
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun processGamesInstalled(packageManager: PackageManager) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAllInstalledApps(packageManager)

            val games = allApps.get()?.filter {
                Utils.applicationIsGame(it)
            }?.mapNotNull {
                MediaFileInfo.fromApplicationInfo(applicationContext, it)
            }
            gamesInstalledLiveData?.postValue(ArrayList(games))
        }
    }

    private fun loadAllInstalledApps(packageManager: PackageManager) {
        if (allApps.get() == null) {
            allApps.set(
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter {
                    var info: PackageInfo?
                    var androidInfo: PackageInfo? = null
                    try {
                        info = packageManager.getPackageInfo(
                            it.packageName,
                            PackageManager.GET_SIGNATURES
                        )
                        androidInfo =
                            packageManager.getPackageInfo(
                                "android",
                                PackageManager.GET_SIGNATURES
                            )
                    } catch (e: PackageManager.NameNotFoundException) {
                        log.warn(
                            "failed to find package name {} while loading apps list",
                            it.packageName,
                            e
                        )
                        info = null
                    }
                    !Utils.isAppInSystemPartition(it) && (
                        info == null ||
                            (
                                !Utils.isSignedBySystem(info, androidInfo) && !info.packageName
                                    .equals(applicationContext.packageName)
                                )
                        )
                }
            )
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
                    arrayListOf(".pdf", ".epub", ".docx").stream()
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
