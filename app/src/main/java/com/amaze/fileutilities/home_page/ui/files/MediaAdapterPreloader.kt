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

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.amaze.fileutilities.R
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder

class MediaAdapterPreloader(context: Context, val loadingDrawable: Int) :
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

    fun loadImage(item: String, v: ImageView) {
        request.fallback(R.drawable.ic_outline_broken_image_24)
            .placeholder(loadingDrawable).load(item).into(v)
    }
}
