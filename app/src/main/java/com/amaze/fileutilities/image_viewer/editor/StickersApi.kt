/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.image_viewer.editor

import com.amaze.fileutilities.BuildConfig
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface StickersApi {

    companion object {
        const val API_STICKERS_BASE = BuildConfig.BASE_API_STICKER_PACK
        const val API_QUERY_PARAM = "token"
        const val API_QUERY_PARAM_VALUE = "c2PxRdya"
    }

    @Headers(value = ["Accept: application/json"])
    @GET(API_STICKERS_BASE)
    fun getStickerList(
        @Query(value = API_QUERY_PARAM) query: String = API_QUERY_PARAM_VALUE,
    ): Call<ArrayList<String>>?
}
