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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

class MediaAdapterPreloader(private val context: Context, private val loadingDrawable: Int) :
    PreloadModelProvider<String> {
    private var request: RequestBuilder<Drawable> = Glide.with(context).asDrawable().fitCenter()
    private var items: MutableList<String>? = null

    fun addItem(item: String) {
        if (items == null) {
            items = arrayListOf()
        }
        items!!.add(item)
    }

    fun clear() {
        items?.clear()
    }

    override fun getPreloadItems(position: Int): List<String> {
        if (items == null) return emptyList()
        return listOf(items!![position])
    }

    override fun getPreloadRequestBuilder(item: String): RequestBuilder<*> {
        return request.clone().fallback(R.drawable.ic_outline_broken_image_24)
            .placeholder(loadingDrawable).load(item)
    }

    fun loadImage(item: MediaFileInfo, view: ImageView, isGrid: Boolean) {
        val toLoadPath: String = item.path
        val toLoadBitmap: Bitmap? = item.extraInfo?.audioMetaData?.albumArt
        val toLoadDrawable = item.extraInfo?.apkMetaData?.drawable
        var transformedRequest = request.fallback(R.drawable.ic_outline_broken_image_24)
            .placeholder(loadingDrawable).load(toLoadDrawable ?: toLoadBitmap ?: toLoadPath)
        if (toLoadBitmap == null) {
            // apply size constraint when we don't have bitmap, as bitmap already is resized see CursorUtils
            transformedRequest = transformedRequest
                .apply(
                    RequestOptions().override(
                        if (isGrid) 500 else 100,
                        if (isGrid) 500 else 100
                    )
                )
        }
        transformedRequest = if (isGrid) {
            transformedRequest.centerCrop()
                .transform(CenterCrop(), GranularRoundedCorners(24.px, 24.px, 0f, 0f))
        } else {
            transformedRequest.centerCrop()
                .transform(CenterCrop(), GranularRoundedCorners(40.px, 40.px, 40.px, 40.px))
        }

        transformedRequest.addListener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                if (isGrid) {
                    view.setPadding(16.px.toInt(), 16.px.toInt(), 16.px.toInt(), 16.px.toInt())
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                // do nothing
                return false
            }
        }).into(view)
    }
}
