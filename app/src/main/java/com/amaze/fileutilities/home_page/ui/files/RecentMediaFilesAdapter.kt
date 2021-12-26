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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter.*
import com.amaze.fileutilities.utilis.EmptyViewHolder
import com.bumptech.glide.Glide
import kotlin.math.roundToInt

class RecentMediaFilesAdapter(
    val context: Context,
    val preloader: MediaAdapterPreloader,
    private val mediaFileInfoList: MutableList<MediaFileInfo>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            if (mediaFileInfoList.size == 0) {
                return
            }
            mediaFileInfoList.forEach {
                value.add(ListItem(it))
                preloader.addItem(it.path)
            }
            preloader.addItem("")
            value.add(ListItem(EMPTY_LAST_ITEM))
            field = value
        }

    private val mInflater: LayoutInflater
        get() = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        mediaFileListItems = mutableListOf()
    }

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_HEADER = 1
        const val EMPTY_LAST_ITEM = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
        RecyclerView.ViewHolder {
            var view = View(context)
            when (viewType) {
                TYPE_ITEM -> {
                    view = mInflater.inflate(
                        R.layout.media_info_row_layout, parent,
                        false
                    )
                    return MediaInfoRecyclerViewHolder(view)
                }
                EMPTY_LAST_ITEM -> {
                    view.minimumHeight =
                        (
                            context.resources.getDimension(R.dimen.fifty_six_dp) +
                                context.resources.getDimension(R.dimen.material_generic)
                            )
                            .roundToInt()
                    return EmptyViewHolder(view)
                }
                else -> {
                    throw IllegalStateException("Illegal $viewType in apps adapter")
                }
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MediaInfoRecyclerViewHolder) {
            mediaFileListItems[position].run {
                mediaFileInfo?.let {
                    mediaFileInfo ->
                    holder.infoTitle.text = mediaFileInfo.title
                    Glide.with(context).clear(holder.iconView)
                    val formattedDate = mediaFileInfo.getModificationDate(context)
                    val formattedSize = mediaFileInfo.getFormattedSize(context)
                    mediaFileInfo.extraInfo?.let { extraInfo ->
                        if (extraInfo.mediaType == MediaFileInfo.MEDIA_TYPE_UNKNOWN) {
                            preloader.loadImage(mediaFileInfo.path, holder.iconView)
                            holder.infoSummary.text = "$formattedDate | $formattedSize"
                            holder.root.setOnClickListener {
                                startExternalViewAction(mediaFileInfo)
                            }
                            holder.root.background = ResourcesCompat
                                .getDrawable(
                                    context.resources,
                                    R.drawable.background_curved_recents, context.theme
                                )
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mediaFileListItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFileListItems[position].listItemType
    }

    private fun startExternalViewAction(mediaFileInfo: MediaFileInfo) {
        val intent = Intent()
        intent.data = mediaFileInfo.getContentUri(context)
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
