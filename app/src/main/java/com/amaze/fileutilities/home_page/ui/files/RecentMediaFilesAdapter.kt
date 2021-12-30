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
import android.view.LayoutInflater
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter.*
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter

class RecentMediaFilesAdapter(
    val context: Context,
    val preloader: MediaAdapterPreloader,
    private val mediaFileInfoList: MutableList<MediaFileInfo>
) :
    AbstractMediaFilesAdapter(context, preloader) {

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

    override fun getItemCount(): Int {
        return mediaFileListItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFileListItems[position].listItemType
    }

    override fun getMediaFilesListItems(): MutableList<ListItem> {
        return mediaFileListItems
    }

    /**
     * Set list elements
     */
    fun setData(data: List<MediaFileInfo>) {
        mediaFileInfoList.run {
            clear()
            preloader.clear()
            addAll(data)
            // triggers set call
            mediaFileListItems = mutableListOf()
            notifyDataSetChanged()
        }
    }
}
