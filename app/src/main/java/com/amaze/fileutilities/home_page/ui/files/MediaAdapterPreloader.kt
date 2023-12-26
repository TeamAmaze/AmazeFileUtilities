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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

class MediaAdapterPreloader<T : MediaFileInfo>(
    private val context: Context,
    private val loadingDrawable: Int,
    private val isGrid: Boolean
) :
    PreloadModelProvider<T> {
    private var request: RequestBuilder<Drawable> = initRequestBuilder()
    private var items: MutableList<T?>? = null

    fun addItem(item: T?) {
        if (items == null) {
            items = arrayListOf()
        }
        items!!.add(item)
    }

    // be sure to call clear once you're done with this, to avoid memory leaks
    fun clear() {
        items?.clear()
    }

    override fun getPreloadItems(position: Int): List<T?> {
        if (items == null || items!![position] == null) return emptyList()
        return listOf(items!![position])
    }

    override fun getPreloadRequestBuilder(item: T): RequestBuilder<*> {
//        return request.clone().fallback(R.drawable.ic_outline_broken_image_24)
//            .placeholder(loadingDrawable).load(item)
//        return request.load(item)
        return getReadyRequestBuilder(item)
    }

    private fun initRequestBuilder(): RequestBuilder<Drawable> {
        var transformedRequest = Glide.with(context).asDrawable()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .fallback(R.drawable.ic_outline_broken_image_24)
            .placeholder(loadingDrawable).fitCenter()
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
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                if (isGrid) {
                    // fallback drawable in case image fails to load.
                    // we add padding because svg placeholder is too big and looks ugly.
//                    view.setPadding(16.px.toInt(), 16.px.toInt(), 16.px.toInt(), 16.px.toInt())
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                // do nothing
                return false
            }
        })
        return transformedRequest
    }

    private fun getReadyRequestBuilder(item: MediaFileInfo): RequestBuilder<Drawable> {
        var transformedRequest = request.load(getLoadingModel(item))
        if (item.extraInfo?.audioMetaData?.albumArt == null) {
            // apply size constraint when we don't have bitmap, as bitmap already is resized see CursorUtils
            transformedRequest = transformedRequest
                .apply(
                    RequestOptions().override(
                        if (isGrid) 250 else 100,
                        if (isGrid) 250 else 100
                    ).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                )
        }
        return transformedRequest
    }

    private fun getLoadingModel(mediaFileInfo: MediaFileInfo): Any {
        val toLoadPath: String = mediaFileInfo.path
        val toLoadBitmap: Bitmap? = mediaFileInfo.extraInfo?.audioMetaData?.albumArt
        val toLoadDrawable = mediaFileInfo.extraInfo?.apkMetaData?.drawable
        return toLoadDrawable ?: toLoadBitmap ?: toLoadPath
    }

    fun loadImage(item: MediaFileInfo, view: ImageView) {
        getReadyRequestBuilder(item).into(view)
    }
}
