/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.image_viewer.editor

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface StickersApi {

    companion object {
        const val API_STICKERS_BASE =
            "https://us-central1-useful-cathode-91310.cloudfunctions.net/amaze-utils-sticker-pack/"
        const val API_QUERY_PARAM = "token"
        const val API_QUERY_PARAM_VALUE = "c2PxRdya"
    }

    @Headers(value = ["Accept: application/json"])
    @GET(API_STICKERS_BASE)
    fun getStickerList(
        @Query(value = API_QUERY_PARAM) query: String = API_QUERY_PARAM_VALUE,
    ): Call<ArrayList<String>>?
}
