/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.home_page.ui.files.MediaInfoRecyclerViewHolder
import com.amaze.fileutilities.image_viewer.ImageViewerDialogActivity
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.EmptyViewHolder

class ReviewImagesAdapter(
    val context: Context,
    val preloader: MediaAdapterPreloader,
    private val mediaFileInfoList: MutableList<MediaFileInfo>,
    private val toggleCheckCallback: (
        checkedSize: Int,
        itemsCount: Int,
        bytesFormatted: String
    ) -> Unit,
) :
    AbstractMediaFilesAdapter(context, preloader, true) {

    var isProcessing = true

    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            mediaFileInfoList.forEach {
                value.add(ListItem(mediaFileInfo = it, position = 0))
                preloader.addItem(it.path)
            }
            if (mediaFileInfoList.size != 0) {
                preloader.addItem("")
                value.add(ListItem(EMPTY_LAST_ITEM, -1))
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
                    mediaFileInfo.extraInfo?.let { extraInfo ->
                        when (extraInfo.mediaType) {
                            MediaFileInfo.MEDIA_TYPE_UNKNOWN -> {
                                holder.infoSummary.text =
                                    this.mediaFileInfo?.getFormattedSize(context)
                                holder.expand.visibility = View.VISIBLE
                                holder.expand.setOnClickListener {
                                    val intent = Intent(
                                        context,
                                        ImageViewerDialogActivity::class.java
                                    )
                                    intent.data = mediaFileInfo.getContentUri(context)
                                    context.startActivity(intent)
                                }
                                invalidateCheckedTitle()
                                holder.root.setOnClickListener {
                                    toggleChecked(this)
                                    holder.checkIconGrid.visibility =
                                        if (isChecked) View.VISIBLE else View.INVISIBLE
                                    invalidateCheckedTitle()
                                }
                            }
                        }
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
//            notifyDataSetChanged()
        }
    }

    private fun invalidateCheckedTitle() {
        toggleCheckCallback.invoke(checkItemsList.size, itemCount, checkedItemBytes())
    }
}
