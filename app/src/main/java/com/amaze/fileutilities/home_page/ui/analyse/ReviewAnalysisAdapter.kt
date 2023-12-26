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

package com.amaze.fileutilities.home_page.ui.analyse

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.home_page.ui.files.MediaInfoRecyclerViewHolder
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.EmptyViewHolder
import com.amaze.fileutilities.utilis.FileUtils
import java.lang.ref.WeakReference

class ReviewAnalysisAdapter(
    val context: Context,
    val analysisType: Int?,
    private val preloader: MediaAdapterPreloader<MediaFileInfo>,
    private val mediaFileInfoList: MutableList<MediaFileInfo>,
    toggleCheckCallback: (
        checkedSize: Int,
        itemsCount: Int,
        bytesFormatted: String
    ) -> Unit,
) :
    AbstractMediaFilesAdapter(
        context, preloader, true, null,
        toggleCheckCallback
    ) {

    var isProcessing = true

    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            for (i in mediaFileInfoList.indices) {
                value.add(ListItem(mediaFileInfo = mediaFileInfoList[i]))
                preloader.addItem(mediaFileInfoList[i])
            }
            if (mediaFileInfoList.size != 0) {
                preloader.addItem(null)
                value.add(ListItem(EMPTY_LAST_ITEM))
            }
            field = value
        }

    init {
        mediaFileListItems = mutableListOf()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (holder is MediaInfoRecyclerViewHolder) {
            getMediaFilesListItems()[position].run {
                mediaFileInfo?.let { mediaFileInfo ->
                    if (analysisType == ReviewImagesFragment.TYPE_NETWORK_INTENSIVE_APPS) {
                        val networkBytes = this.mediaFileInfo?.extraInfo?.apkMetaData?.networkBytes
                        if (networkBytes != null) {
                            holder.infoSummary.text = FileUtils.formatStorageLength(
                                context,
                                networkBytes
                            )
                        } else {
                            holder.infoSummary.text = this.mediaFileInfo?.getFormattedSize(context)
                        }
                    } else {
                        holder.infoSummary.text = this.mediaFileInfo?.getFormattedSize(context)
                    }
                    holder.expand.visibility = View.VISIBLE
                    invalidateCheckedTitle(getOnlyItemsCount())
                    holder.root.setOnClickListener {
                        toggleChecked(this)
                        holder.checkIconGrid.visibility =
                            if (isChecked) View.VISIBLE else View.INVISIBLE
                        invalidateCheckedTitle(getOnlyItemsCount())
                    }
                    holder.expand.setOnClickListener {
                        mediaFileInfo.triggerMediaFileInfoAction(WeakReference(context))
                    }
                }
            }
        } else if (holder is EmptyViewHolder) {
            holder.processingProgressView.invalidateProcessing(
                isProcessing, itemCount == 0,
                context.resources.getString(R.string.analysing)
            )
        }
    }

    override fun getItemCount(): Int {
        return mediaFileListItems.size
    }

    override fun getOnlyItemsCount(): Int {
        return mediaFileListItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return mediaFileListItems[position].listItemType
    }

    override fun getMediaFilesListItems(): MutableList<ListItem> {
        return mediaFileListItems
    }

    /**
     * for review fragments, adapter underlying list is the main list fetched from analysis, so we
     * can directly delete from that list
     */
    override fun removeChecked(): Boolean {
        val toRemove = getMediaFilesListItems().filter { it.isChecked && it.mediaFileInfo != null }
            .map { it.mediaFileInfo }
        val didRemove = mediaFileInfoList.removeAll(toRemove)
        return super.removeChecked() && didRemove
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
//            notifyDataSetChanged()
        }
    }
}
