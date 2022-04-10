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
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.*
import com.amaze.fileutilities.home_page.database.*
import com.amaze.fileutilities.home_page.ui.AggregatedMediaFileInfoObserver
import com.amaze.fileutilities.home_page.ui.options.Billing
import com.amaze.fileutilities.home_page.ui.transfer.TransferFragment
import com.amaze.fileutilities.utilis.*
import com.amaze.fileutilities.utilis.share.ShareAdapter
import com.amaze.fileutilities.utilis.share.getShareIntents
import com.google.common.io.ByteStreams
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

class FilesViewModel(val applicationContext: Application) :
    AndroidViewModel(applicationContext) {

    var isImageFeaturesAnalysing = true
    var isImageBlurAnalysing = true
    var isImageLowLightAnalysing = true
    var isImageMemesAnalysing = true
    var isInternalStorageAnalysing = true
    var isMediaStoreAnalysing = true
    var isCasting = false
    var wifiIpAddress: String? = null
    val uniqueIdSalt = "#%36zkpCE2"

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
            storageData?.let {
                data ->
                val file = File(data.path)
                val items = CursorUtils.getMediaFilesCount(applicationContext)
                FileUtils.scanFile(Uri.fromFile(file), applicationContext)
                val usedSpace = file.totalSpace - file.usableSpace
                val progress = (usedSpace * 100) / file.totalSpace
                emit(
                    StorageSummary(
                        items, progress.toInt(), usedSpace, usedSpace,
                        file.usableSpace,
                        file.totalSpace
                    )
                )
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

    var usedImagesSummaryTransformations:
        LiveData<Pair<StorageSummary, ArrayList<MediaFileInfo>>?> =
            Transformations.switchMap(internalStorageStats) {
                input ->
                getImagesSummaryLiveData(input)
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

    var usedDocsSummaryTransformations:
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

    fun analyseImageFeatures(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).analysisDao()
            isImageFeaturesAnalysing = true
            var featuresProcessed = 0
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_IMAGE_FEATURES
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true) &&
                    PathPreferences.isEnabled(
                        applicationContext.getAppCommonSharedPreferences(),
                        PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                    )
            }.forEach {
                if (dao.findByPath(it.path) == null) {
                    if (featuresProcessed++ > 10000) {
                        // hard limit in a single run
                        return@forEach
                    }
                    var features = ImgUtils.ImageFeatures()
                    it.getContentUri(applicationContext)?.let {
                        uri ->
                        ImgUtils.getImageFeatures(
                            applicationContext,
                            faceDetector,
                            uri
                        ) { isSuccess, imageFeatures ->
                            if (isSuccess) {
                                imageFeatures?.run {
                                    features = this
                                }
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
                }
            }
            isImageFeaturesAnalysing = false
        }
    }

    fun analyseBlurImages(
        mediaFileInfoList: List<MediaFileInfo>,
        pathPreferencesList: List<PathPreferences>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).blurAnalysisDao()
            isImageBlurAnalysing = true
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_BLUR
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true) &&
                    PathPreferences.isEnabled(
                        applicationContext.getAppCommonSharedPreferences(),
                        PathPreferences.FEATURE_ANALYSIS_BLUR
                    )
            }.forEach {
                if (dao.findByPath(it.path) == null) {
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
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).memesAnalysisDao()
            isImageMemesAnalysing = true
            var memesProcessed = 0
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences.FEATURE_ANALYSIS_MEME
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true) &&
                    PathPreferences.isEnabled(
                        applicationContext.getAppCommonSharedPreferences(),
                        PathPreferences.FEATURE_ANALYSIS_MEME
                    )
            }.forEach {
                if (dao.findByPath(it.path) == null) {
                    if (memesProcessed++ > 10000) {
                        // hard limit in a single run
                        return@forEach
                    }
                    it.getContentUri(applicationContext)?.let {
                        uri ->
                        ImgUtils.isImageMeme(
                            applicationContext,
                            textRecognizer,
                            uri,
                        ) { isMeme ->
                            dao.insert(
                                MemeAnalysis(
                                    it.path, isMeme
                                )
                            )
                        }
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
        viewModelScope.launch(Dispatchers.Default) {
            val dao = AppDatabase.getInstance(applicationContext).lowLightAnalysisDao()
            isImageLowLightAnalysing = true
            mediaFileInfoList.filter {
                val pathPrefsList = pathPreferencesList.filter { pref ->
                    pref.feature == PathPreferences
                        .FEATURE_ANALYSIS_LOW_LIGHT
                }
                Utils.containsInPreferences(it.path, pathPrefsList, true) &&
                    PathPreferences.isEnabled(
                        applicationContext.getAppCommonSharedPreferences(),
                        PathPreferences.FEATURE_ANALYSIS_LOW_LIGHT
                    )
            }.forEach {
                if (dao.findByPath(it.path) == null) {
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
                    getMediaFileChecksumAndWriteToDatabase(dao, File(image.path + ""))
                }
            }
            aggregatedMediaFiles.audiosMediaFilesList?.let {
                val audiosList = ArrayList(it)
                audiosList.forEach {
                    audio ->
                    getMediaFileChecksumAndWriteToDatabase(dao, File(audio.path + ""))
                }
            }
            aggregatedMediaFiles.videosMediaFilesList?.let {
                val videosList = ArrayList(it)
                videosList.forEach {
                    video ->
                    getMediaFileChecksumAndWriteToDatabase(dao, File(video.path + ""))
                }
            }
            aggregatedMediaFiles.docsMediaFilesList?.let {
                val docsList = ArrayList(it)
                docsList.forEach {
                    docs ->
                    getMediaFileChecksumAndWriteToDatabase(dao, File(docs.path + ""))
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
            val outputPath = copyLogsFileToInternalStorage()
            if (outputPath != null) {
                log.info("Sharing logs file at path $outputPath")
                val logsFile = File(outputPath)
                emit(
                    getShareIntents(
                        Collections.singletonList(Uri.fromFile(logsFile)),
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
            log.info("Sharing media files $mediaFileInfoList")
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
                if (index % 5 == 0) {
                    emit(successProcessedPair)
                }
            }
            emit(successProcessedPair)
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
                        trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_INACTIVE
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
                    if (cal.time.before(Date())) {
                        // we haven't fetched today, check if active from remote
                        fetchBillingStatusAndInitTrial(
                            deviceId, dao, isNetworkAvailable,
                            trialResponse
                        )
                    } else {
                        // we've fetcehd today, consider it active
                        trialResponse.invoke(
                            TrialValidationApi.TrialResponse(
                                false, false,
                                TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
                                trial.trialDaysLeft,
                                trial.subscriptionStatus,
                                null
                            )
                        )
                    }
                }
            }
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
                        null
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
                return nullTrialResponse
            } else {
                log.info("no network available, return database saved trial state")
                return TrialValidationApi.TrialResponse(
                    false, false,
                    TrialValidationApi.TrialResponse.CODE_TRIAL_ACTIVE,
                    trial.trialDaysLeft,
                    trial.subscriptionStatus,
                    null
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
            .build()
        val service = retrofit.create(TrialValidationApi::class.java)
        try {
            val subscriptionStatus = trial?.subscriptionStatus ?: Trial.SUBSCRIPTION_STATUS_DEFAULT
            val purchaseToken = trial?.purchaseToken
            service.postValidation(
                TrialValidationApi.TrialRequest(
                    TrialValidationApi.AUTH_TOKEN, deviceId,
                    subscriptionStatus, purchaseToken
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

    /**
     * Copies logs file to internal storage and returns the written file path
     */
    private fun copyLogsFileToInternalStorage(): String? {
        applicationContext.getExternalStorageDirectory()?.let {
            internalStoragePath ->
            FileInputStream(File("${applicationContext.filesDir}/logs.txt")).use {
                inputStream ->
                val file = File(
                    internalStoragePath.path +
                        "/${TransferFragment.RECEIVER_BASE_PATH}/files"
                )
                file.mkdirs()
                val logFile = File(file, "logs.txt")
                FileOutputStream(logFile).use {
                    outputStream ->
                    ByteStreams.copy(inputStream, outputStream)
                }
                return logFile.path
            }
        }
        return null
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
            return
        }
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
            val metaInfoAndSummaryPair = CursorUtils
                .listDocs(applicationContext.applicationContext)
            setMediaInfoSummary(metaInfoAndSummaryPair.first, storageSummary)
            emit(metaInfoAndSummaryPair)
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
