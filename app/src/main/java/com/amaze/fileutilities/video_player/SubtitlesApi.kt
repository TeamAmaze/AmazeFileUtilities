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

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface SubtitlesApi {

    companion object {
        const val OPEN_SUBTITLES_BASE = "https://www.opensubtitles.org/"
    }

    @GET("en/search/subs")
    fun searchSubsConfigs(): Call<ResponseBody>?

    @GET("en/search2/sublanguageid-{languages}/moviename-{name}")
    fun postSearchQuery(
        @Path("languages") languages: String,
        @Path("name") name: String
    ): Call<ResponseBody>?
}
