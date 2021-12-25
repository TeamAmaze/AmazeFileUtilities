package com.amaze.fileutilities.home_page.ui.files

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder

class MediaAdapterPreloader(context: Context) :
    PreloadModelProvider<String> {
    private var request: RequestBuilder<Drawable> = Glide.with(context).asDrawable().fitCenter()
    private var items: MutableList<String>? = null

    fun addItem(item: String) {
        if (items == null) {
            items = arrayListOf()
        }
        items!!.add(item)
    }

    override fun getPreloadItems(position: Int): List<String> {
        if (items == null) return emptyList()
        return listOf(items!![position])
    }

    override fun getPreloadRequestBuilder(item: String): RequestBuilder<*> {
        return request.clone().load(item)
    }

    fun loadImage(item: String, v: ImageView) {
        request.load(item).into(v)
    }
}