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
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.MediaTypeHeaderView
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.HeaderViewHolder
import com.amaze.fileutilities.utilis.ListBannerViewHolder

class MediaFileAdapter(
    val context: Context,
    val preloader: MediaAdapterPreloader,
    private var sortingPreference: MediaFileListSorter.SortingPreference,
    private val mediaFileInfoList: MutableList<MediaFileInfo>,
    private val mediaType: Int,
    private val drawBannerCallback: (mediaTypeHeader: MediaTypeHeaderView) -> Unit
) :
    AbstractMediaFilesAdapter(context, preloader) {

    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            MediaFileListSorter.generateMediaFileListHeadersAndSort(
                context,
                mediaFileInfoList, sortingPreference
            )
            var lastHeader: String? = null
            value.add(ListItem(TYPE_BANNER))
            preloader.addItem("")
            mediaFileInfoList.forEach {
                if (lastHeader == null || it.listHeader != lastHeader) {
                    value.add(ListItem(TYPE_HEADER, it.listHeader))
                    preloader.addItem("")
                    lastHeader = it.listHeader
                }
                value.add(ListItem(it))
                preloader.addItem(it.path)
            }
            preloader.addItem("")
            value.add(ListItem(EMPTY_LAST_ITEM))
            field = value
        }

    init {
        mediaFileListItems = mutableListOf()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is HeaderViewHolder -> {
                holder.setText(
                    mediaFileListItems[position].mediaFileInfo?.listHeader
                        ?: context.resources.getString(R.string.undetermined)
                )
            }
            is ListBannerViewHolder -> {
                setBannerResources(holder)
            }
        }
    }

    override fun getItemCount(): Int {
        return mediaFileListItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFileListItems[position].listItemType
    }

    /**
     * Set list elements
     */
    fun setData(data: List<MediaFileInfo>, sortPref: MediaFileListSorter.SortingPreference) {
        mediaFileInfoList.run {
            clear()
            preloader.clear()
            sortingPreference = sortPref
            addAll(data)
            // triggers set call
            mediaFileListItems = mutableListOf()
            notifyDataSetChanged()
        }
    }

    private fun setBannerResources(holder: ListBannerViewHolder) {
        when (mediaType) {
            MediaFileInfo.MEDIA_TYPE_AUDIO -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.peach, context.theme
                        )
                )
                holder.mediaTypeHeaderView.setTypeImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_outline_audio_file_32, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.orange, context.theme
                        )
                )
                holder.mediaTypeHeaderView.setTypeImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_outline_video_library_32, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.pink, context.theme
                        )
                )
                holder.mediaTypeHeaderView.setTypeImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_outline_image_32, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_DOCUMENT -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.green, context.theme
                        )
                )
                holder.mediaTypeHeaderView.setTypeImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_outline_insert_drive_file_32, context.theme
                    )!!
                )
            }
        }
        drawBannerCallback.invoke(holder.mediaTypeHeaderView)
    }

    override fun getMediaFilesListItems(): MutableList<ListItem> {
        return mediaFileListItems
    }
}
