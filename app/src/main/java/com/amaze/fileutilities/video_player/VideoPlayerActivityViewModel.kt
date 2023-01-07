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

package com.amaze.fileutilities.video_player

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.home_page.database.VideoPlayerState
import com.amaze.fileutilities.home_page.database.VideoPlayerStateDao
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import kotlinx.coroutines.Dispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collections

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
    var pitchSpeed = 1f
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
                .baseUrl(SubtitlesApi.API_OPEN_SUBTITLES_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .client(Utils.getOkHttpClient())
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            service.getLanguageList()?.execute()?.let {
                response ->
                if (response.isSuccessful && response.body() != null) {
                    val languageResponse = response.body()!!
                    var languageCodes = languageResponse.data.map { it.language_code }
                    var languageValues = languageResponse.data.map { it.language_name }
                    if (languageCodes.isEmpty() || languageCodes.isEmpty()) {
                        languageCodes = Collections.singletonList("all")
                        languageValues = Collections.singletonList("ALL")
                    }
                    try {
                        val languageAndCodeList = ArrayList<LanguageSelectionAdapter
                            .SubtitleLanguageAndCode>()
                        languageAndCodeList.add(
                            LanguageSelectionAdapter
                                .SubtitleLanguageAndCode("Languages", "")
                        )
                        languageAndCodeList.add(
                            LanguageSelectionAdapter
                                .SubtitleLanguageAndCode("All", "all")
                        )
                        val prefLanguageCode = sharedPreferences
                            .getString(
                                PreferencesConstants.KEY_SUBTITLE_LANGUAGE_CODE,
                                PreferencesConstants.DEFAULT_SUBTITLE_LANGUAGE_CODE
                            )
                        for (i in languageCodes.indices) {
                            // first add last selected on top
                            if (languageCodes[i].equals(prefLanguageCode, true)) {
                                languageAndCodeList
                                    .add(
                                        LanguageSelectionAdapter
                                            .SubtitleLanguageAndCode(
                                                languageValues[i] ?: "",
                                                languageCodes[i] ?: ""
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
                                                languageValues[i] ?: "",
                                                languageCodes[i] ?: ""
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
                .baseUrl(SubtitlesApi.API_OPEN_SUBTITLES_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .client(Utils.getOkHttpClient())
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            val languageListRequestString = languageList.map { it.code }.filter { it.isNotEmpty() }
                .joinToString(",")

            service.getSearchResults(movieName, languageListRequestString)?.execute()?.let {
                response ->
                if (response.isSuccessful && response.body() != null) {
                    val searchResultsResponse = response.body()!!
                    val subtitleResultList =
                        ArrayList<SubtitlesSearchResultsAdapter.SubtitleResult>()
                    for (data in searchResultsResponse.data) {
                        data.attributes?.let {
                            attributes ->
                            val subtitleResult = SubtitlesSearchResultsAdapter.SubtitleResult()
                            subtitleResult.downloads = attributes.download_count
                            subtitleResult.language = attributes.language
                            subtitleResult.subtitleRating = attributes.ratings
                            subtitleResult.uploadDate = attributes.upload_date
                            if (!attributes.files.isNullOrEmpty()) {
                                subtitleResult.title = attributes.files[0].file_name
                                subtitleResult.cdNumber = attributes.files[0].cd_number
                                attributes.files[0].file_id?.let {
                                    fileId ->
                                    log.info("found subtitle download id {}", fileId)
                                    getSubtitleDownloadLink(fileId)?.let {
                                        linkResponse ->
                                        log.info("found subtitle download link {}", linkResponse)
                                        subtitleResult.downloadId = linkResponse.link
                                        subtitleResult.downloadFileName = linkResponse.file_name
                                    }
                                }
                            }
                            attributes.uploader?.let {
                                uploader ->
                                subtitleResult.uploader = "${uploader.name} (${uploader.rank})"
                            }
                            subtitleResultList.add(subtitleResult)
                        }
                    }
                    emit(subtitleResultList)
                } else {
                    // no search results
                    log.info("no subtitle search results")
                    emit(Collections.emptyList())
                }
            }
        }
    }

    fun downloadSubtitle(
        downloadLink: String,
        downloadFileName: String?,
        targetFile: File,
        fallbackSubtitleDownloadPath: String
    ): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val retrofit = Retrofit.Builder()
                .baseUrl(SubtitlesApi.API_OPEN_SUBTITLES_BASE)
                .client(Utils.getOkHttpClient())
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            log.debug("Download request for link $downloadLink and $downloadFileName")
            service.downloadSubtitleFile(downloadLink)?.execute()?.body()?.byteStream()?.use {
                if (targetFile.parent != null) {
                    log.debug("get subtitle download response from opensubtitles")
                    val fileName = downloadFileName ?: downloadLink
                        .substring(downloadLink.lastIndexOf("/") + 1)
                    try {
                        emit(
                            extractFile(
                                it, targetFile.parent!!, fileName, false,
                                fallbackSubtitleDownloadPath
                            )
                        )
                    } catch (e: IOException) {
                        log.warn("failed to extract downloaded subs at $fileName", e)
                        emit("")
                    }
                } else {
                    emit("")
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun extractFile(
        inputStream: InputStream,
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
                while (inputStream.read(bytesIn).also { read = it } != -1) {
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
                    inputStream, fallbackSubtitleDownloadPath, fileName,
                    true, fallbackSubtitleDownloadPath
                )
            } else {
                log.warn("Exhausted retries, failing to get subtitles")
                ""
            }
        }
    }

    private fun getSubtitleDownloadLink(
        fileId: String
    ): SubtitlesApi.GetDownloadLinkResponse? {
        val retrofit = Retrofit.Builder()
            .baseUrl(SubtitlesApi.API_OPEN_SUBTITLES_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(Utils.getOkHttpClient())
            .build()
        val service = retrofit.create(SubtitlesApi::class.java)
        service.getDownloadLink(
            SubtitlesApi.GetDownloadLinkRequest(
                fileId
            )
        )?.execute()?.let { response ->
            return if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        }
        return null
    }
}
