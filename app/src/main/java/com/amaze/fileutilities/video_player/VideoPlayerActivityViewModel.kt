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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import retrofit2.Retrofit
import java.util.*
import kotlin.collections.ArrayList

class VideoPlayerActivityViewModel : ViewModel() {

    var playWhenReady = true
    var currentWindow = 0
    var playbackPosition = 0L
    var videoModel: LocalVideoModel? = null
    var fullscreen = false
    var fitToScreen = false
    var isInPictureInPicture = false
    var playbackSpeed = 1f
    var isUiLocked = false
    var isSubtitleAvailable = false
    var isSubtitleEnabled = false
    var subtitleFilePath: String? = null

    fun getSubtitlesAvailableLanguages(): LiveData<List<LanguageSelectionAdapter
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
                    val optionTags = document.select("select[name=SubLanguageID]")
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
                        for (i in languageCodes.indices) {
                            languageAndCodeList
                                .add(
                                    LanguageSelectionAdapter
                                        .SubtitleLanguageAndCode(
                                            languageValues[i],
                                            languageCodes[i]
                                        )
                                )
                        }
                        emit(languageAndCodeList)
                    } catch (e: Exception) {
                        e.printStackTrace()
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
    ): LiveData<List<LanguageSelectionAdapter
            .SubtitleLanguageAndCode>?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val retrofit = Retrofit.Builder()
                .baseUrl(SubtitlesApi.OPEN_SUBTITLES_BASE)
                .build()
            val service = retrofit.create(SubtitlesApi::class.java)
            service.postSearchQuery(
                languageList.map { it.code }.filter { it.isNotEmpty() }
                    .joinToString(","),
                movieName
            )?.execute()?.let {
                response ->
                Log.i(
                    javaClass.simpleName,
                    "Subtitles list doc " +
                        response.body()!!.string()
                )
                val document = Jsoup.parse(response.body().toString())
                emit(null)
            }
        }
    }
}
