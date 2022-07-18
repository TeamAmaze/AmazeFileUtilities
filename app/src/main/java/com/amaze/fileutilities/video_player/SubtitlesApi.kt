/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.video_player

import androidx.annotation.Keep
import com.amaze.fileutilities.BuildConfig
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface SubtitlesApi {

    companion object {
        const val API_OPEN_SUBTITLES_BASE = "https://api.opensubtitles.com/api/v1/"
        const val API_SEARCH_SUBTITLES = "subtitles"
        const val API_DOWNLOAD_SUBTITLES = "download"
        const val API_LANGUAGE = "infos/languages"
        private const val API_KEY = BuildConfig.OPENSUBTITLES_API_KEY
        private const val USER_AGENT = "AmazeFileUtils"
    }

    @Headers(value = ["Accept: application/json", "Api-Key: $API_KEY", "User-Agent: $USER_AGENT"])
    @GET(API_LANGUAGE)
    fun getLanguageList(): Call<LanguageResult>?

    @Headers(
        value = [
            "Accept: application/json",
            "Api-Key: $API_KEY", "User-Agent: $USER_AGENT"
        ]
    )
    @GET(API_SEARCH_SUBTITLES)
    fun getSearchResults(
        @Query(value = "query") query: String,
        @Query(value = "languages") languages: String
    ): Call<SearchResultsResponse>?

    @Headers(
        value = [
            "Accept: application/json",
            "Content-type:application/json", "Api-Key: $API_KEY", "User-Agent: $USER_AGENT"
        ]
    )
    @POST(API_DOWNLOAD_SUBTITLES)
    fun getDownloadLink(
        @Body downloadLinkRequest: GetDownloadLinkRequest
    ): Call<GetDownloadLinkResponse>?

    @GET
    @Streaming
    fun downloadSubtitleFile(@Url downloadUrl: String): Call<ResponseBody>?

    @Keep
    data class SearchResultsResponse(val data: List<Data>) {
        @Keep
        data class Attributes(
            val language: String?,
            val ratings: String?,
            val download_count: String?,
            val upload_date: String?,
            val uploader: Uploader?,
            val files: List<Files>?
        )
        @Keep
        data class Uploader(val name: String?, val rank: String?)
        @Keep
        data class Files(val file_id: String?, val file_name: String?, val cd_number: String?)
        @Keep
        data class Data(val attributes: Attributes?)
    }

    @Keep
    data class GetDownloadLinkRequest(val file_id: String?)

    @Keep
    data class GetDownloadLinkResponse(val link: String?, val file_name: String?)

    @Keep
    data class LanguageResult(val data: List<Data>) {
        @Keep
        data class Data(val language_code: String?, val language_name: String?)
    }
}
