/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.video_player

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.home_page.database.VideoPlayerState
import com.amaze.fileutilities.home_page.database.VideoPlayerStateDao
import com.amaze.fileutilities.utilis.PreferencesConstants
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

class VideoPlayerActivityViewModel : ViewModel() {

    var log: Logger = LoggerFactory.getLogger(VideoPlayerActivityViewModel::class.java)

    var playWhenReady = true
    var currentWindow = 0
    var playbackPosition = 0L
    var videoModel: LocalVideoModel? = null
    var currentlyPlaying = true
    var fullscreen = false
    var fitToScreen = 0
    var isInPictureInPicture = false
    var playbackSpeed = 1f
    var isUiLocked = false
    var isSubtitleAvailable = false
    var isSubtitleEnabled = false
    var subtitleFilePath: String? = null
    var isContinuePlayingDisplayed = false
    var isRotationLocked = false
    var brightnessLevel = 0.3f
    var volumeLevel = 0.3f

    fun getPlaybackSavedState(videoPlayerStateDao: VideoPlayerStateDao):
        LiveData<VideoPlayerState?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            videoModel?.uri?.path?.let {
                emit(videoPlayerStateDao.getStateByUriPath(it))
            }
        }
    }

    fun savePlaybackState(videoPlayerStateDao: VideoPlayerStateDao, playbackPosition: Long) {
        videoModel?.uri?.path?.let {
            videoPlayerStateDao.insert(VideoPlayerState(it, playbackPosition))
        }
    }

    fun getSubtitlesAvailableLanguages(sharedPreferences: SharedPreferences):
        LiveData<List<LanguageSelectionAdapter
                .SubtitleLanguageAndCode>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val retrofit = Retrofit.Builder()
                .baseUrl(SubtitlesApi.OPEN_SUBTITLES_BASE)
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            service.searchSubsConfigs()?.execute()?.let {
                response ->
                if (response.body() != null) {
                    val document = Jsoup.parse(response.body()!!.string())
                    val optionTags = document.select("select[id=SubLanguageID]")
                        .select("option")
                    var languageCodes = optionTags.eachAttr("value")
                    var languageValues = optionTags.eachText()
                    if (languageCodes.isEmpty() || languageCodes.isEmpty()) {
                        languageCodes = Collections.singletonList("all")
                        languageValues = Collections.singletonList("ALL")
                    }
                    try {
                        val languageAndCodeList = ArrayList<LanguageSelectionAdapter
                            .SubtitleLanguageAndCode>()
                        languageAndCodeList.add(
                            LanguageSelectionAdapter
                                .SubtitleLanguageAndCode("Languages", "all")
                        )
                        val prefLanguageCode = sharedPreferences
                            .getString(
                                PreferencesConstants.KEY_SUBTITLE_LANGUAGE_CODE,
                                PreferencesConstants.DEFAULT_SUBTITLE_LANGUAGE_CODE
                            )
                        for (i in languageCodes.indices) {
                            // first add english on top
                            if (languageCodes[i].equals(prefLanguageCode, true)) {
                                languageAndCodeList
                                    .add(
                                        LanguageSelectionAdapter
                                            .SubtitleLanguageAndCode(
                                                languageValues[i],
                                                languageCodes[i]
                                            )
                                    )
                                break
                            }
                        }
                        for (i in languageCodes.indices) {
                            if (!languageCodes[i].equals(prefLanguageCode, true)) {
                                languageAndCodeList
                                    .add(
                                        LanguageSelectionAdapter
                                            .SubtitleLanguageAndCode(
                                                languageValues[i],
                                                languageCodes[i]
                                            )
                                    )
                            }
                        }
                        emit(languageAndCodeList)
                    } catch (e: Exception) {
                        log.warn("failed to fetch languges from remote for subtitles", e)
                        emit(
                            Collections.singletonList(
                                LanguageSelectionAdapter
                                    .SubtitleLanguageAndCode("ALl", "all")
                            )
                        )
                    }
                } else {
                    emit(null)
                }
            }
        }
    }

    fun getSubtitlesList(
        languageList: List<LanguageSelectionAdapter.SubtitleLanguageAndCode>,
        movieName: String
    ): LiveData<List<SubtitlesSearchResultsAdapter.SubtitleResult>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val retrofit = Retrofit.Builder()
                .baseUrl(SubtitlesApi.OPEN_SUBTITLES_BASE)
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            val languageListRequestString = languageList.map { it.code }.filter { it.isNotEmpty() }
                .joinToString(",")
            service.postSearchQuery(
                languageListRequestString,
                movieName
            )?.execute()?.let {
                response ->
                val document = Jsoup.parse(response.body()!!.string())
                val table = document.select("table[id=search_results]")
                if (table.isNullOrEmpty()) {
                    // no search results
                    emit(Collections.emptyList())
                } else {
                    val tableBody = table.select("tbody")
                    val tableRows = tableBody.select("tr")
                    val movieIds = ArrayList<String>()
                    for (i in tableRows.indices) {
                        if (i == 0) {
                            // skip table headers
                            continue
                        }
                        val tableData = tableRows[i].select("td")
                        if (tableData.size> 1) {
                            // first table data is empty, we expect atleast 2
                            val secondTableData = tableData[1]
                            // only first href tag has movie id link
                            val ahrefTag = secondTableData.select("a")
                            if (ahrefTag.size > 0) {
                                // /en/search/sublanguageid-abk,afr,alb/idmovie-1872
                                val attr = ahrefTag[0].attr("href")
                                val movieId = attr.substring(
                                    attr.lastIndexOf("-") + 1
                                )
                                movieIds.add(movieId)
                            }
                        }
                    }
                    val subtitleResult = ArrayList<SubtitlesSearchResultsAdapter.SubtitleResult>()
                    for (movieId in movieIds) {
                        if (subtitleResult.size <50) {
                            getSubtitlesResults(languageListRequestString, movieId)?.let {
                                subtitleResult.addAll(it)
                            }
                        }
                    }
                    emit(subtitleResult)
                }
            }
        }
    }

    fun downloadSubtitle(
        downloadId: String,
        targetFile: File,
        fallbackSubtitleDownloadPath: String
    ): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val retrofit = Retrofit.Builder()
                .baseUrl(SubtitlesApi.DOWNLOAD_SUBTITLES_BASE)
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            log.debug("Download request for id $downloadId")
            service.downloadSubtitle(downloadId)?.execute()?.body()?.byteStream()?.use {
                if (targetFile.parent != null) {
                    log.debug("get subtitle download response from opensubtitles")
                    emit(extractSubtitles(targetFile.parent!!, it, fallbackSubtitleDownloadPath))
                } else {
                    emit("")
                }
            }
        }
    }

    private fun extractSubtitles(
        parentPath: String,
        inputStream: InputStream,
        fallbackSubtitleDownloadPath: String
    ): String {
        var extractPath = ""
        ZipInputStream(BufferedInputStream(inputStream)).use {
            zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            // iterates over entries in the zip file
            while (entry != null) {
                val filePath: String = parentPath
                if (!entry.isDirectory && !entry.name.endsWith(".nfo")) {
                    // if the entry is a file, extracts it
                    log.debug("Found zip entry for subtitles ${entry.name}")
                    extractPath = try {
                        extractFile(
                            zipIn, filePath, entry.name, false,
                            fallbackSubtitleDownloadPath
                        )
                    } catch (e: IOException) {
                        log.warn("failed to extract subtitles from downloaded file", e)
                        ""
                    }
                    break
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return extractPath
    }

    @Throws(IOException::class)
    private fun extractFile(
        zipIn: ZipInputStream,
        folderPath: String,
        fileName: String,
        isRetry: Boolean,
        fallbackSubtitleDownloadPath: String
    ): String {
        return try {
            val filePath = folderPath + File.separator + fileName
            BufferedOutputStream(FileOutputStream(filePath)).use {
                bos ->
                val bytesIn = ByteArray(1024)
                log.debug("Extract subtitle zip entry at $filePath")
                var read = 0
                while (zipIn.read(bytesIn).also { read = it } != -1) {
                    bos.write(bytesIn, 0, read)
                }
            }
            return filePath
        } catch (e: FileNotFoundException) {
            if (isRetry) {
                log.warn("Exhausted retries, failing to get subtitles")
            }
            log.warn(
                "Failed to write subtitle file, " +
                    "probably due to video file being in sd card",
                e
            )
            if (!isRetry) {
                log.warn(
                    "Retrying to extract subtitle at fallback path $fallbackSubtitleDownloadPath"
                )
                File(fallbackSubtitleDownloadPath).mkdirs()
                return extractFile(
                    zipIn, fallbackSubtitleDownloadPath, fileName,
                    true, fallbackSubtitleDownloadPath
                )
            } else {
                log.warn("Exhausted retries, failing to get subtitles")
                ""
            }
        }
    }

    private fun getSubtitlesResults(
        languageList: String,
        movieId: String
    ): List<SubtitlesSearchResultsAdapter.SubtitleResult>? {
        val retrofit = Retrofit.Builder()
            .baseUrl(SubtitlesApi.OPEN_SUBTITLES_BASE)
            .build()
        val service = retrofit.create(SubtitlesApi::class.java)
        service.getSearchResultsInfo(
            languageList, movieId
        )?.execute()?.let { response ->
            val document = Jsoup.parse(response.body()!!.string())
            val table = document.select("table[id=search_results]")
            if (table.isNullOrEmpty()) {
                // no search results
                return Collections.emptyList()
            } else {
                val tableBody = table.select("tbody")
                val tableRows = tableBody.select("tr")
                val subtitleResultsList = ArrayList<SubtitlesSearchResultsAdapter.SubtitleResult>()
                for (i in tableRows.indices) {
                    if (i == 0) {
                        // skip table headers
                        continue
                    }
                    val subtitleResult = SubtitlesSearchResultsAdapter.SubtitleResult()
                    val tableDataList = tableRows[i].select("td")
                    if (tableDataList.size < 9) {
                        continue
                    }
                    for (j in tableDataList.indices) {
                        when (j) {
                            0 -> {
                                // first td is title
                                val hrefs = tableDataList[0].select("a")
                                if (hrefs.size > 0) {
                                    // we need first href for title
                                    val title = hrefs[0].text()
                                    subtitleResult.title = title
                                }
                            }
                            1 -> {
                                // language
                                val language = tableDataList[1].select("a")[0]
                                    .attr("title")
                                subtitleResult.language = language
                            }
                            2 -> {
                                val cd = tableDataList[2].text()
                                subtitleResult.cdNumber = cd
                            }
                            3 -> {
                                val uploadDate = tableDataList[3].select("time")[0]
                                    .attr("title")
                                subtitleResult.uploadDate = uploadDate
                            }
                            4 -> {
                                // /en/subtitleserve/sub/5136695
                                val href = tableDataList[4].select("a")[0]
                                    .attr("href")
                                val downloadId = href.substring(href.lastIndexOf("/") + 1)
                                subtitleResult.downloadId = downloadId
                            }
                            5 -> {
                                val rating = tableDataList[5].select("span").text()
                                subtitleResult.subtitleRating = rating
                            }
                            7 -> {
                                val imdb = tableDataList[7].select("a")[0].text()
                                subtitleResult.imdb = imdb
                            }
                            8 -> {
                                val a_uploader = tableDataList[8].select("a")
                                var uploader = a_uploader[0].text()
                                if (a_uploader.size > 1) {
                                    val badge = a_uploader[1].attr("title")
                                    uploader += " ($badge)"
                                    subtitleResult.uploader = uploader
                                }
                            }
                        }
                    }
                    subtitleResultsList.add(subtitleResult)
                }
                return subtitleResultsList
            }
        }
        return null
    }
}
