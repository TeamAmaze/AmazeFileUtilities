/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioUtils
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.home_page.ui.files.MediaInfoRecyclerViewHolder
import com.bumptech.glide.Glide
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.lang.ref.WeakReference
import java.util.*

abstract class AbstractMediaFilesAdapter(
    private val superContext: Context,
    private val superPreloader: MediaAdapterPreloader,
    private val isGrid: Boolean,
    private val listItemPressedCallback: ((mediaFileInfo: MediaFileInfo) -> Unit)?,
    private val toggleCheckCallback: (
        (checkedSize: Int, itemsCount: Int, bytesFormatted: String)
        -> Unit
    )?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), PopupTextProvider {

    val checkItemsList: MutableList<ListItem> = mutableListOf()

    fun toggleChecked(listItem: ListItem) {
        listItem.toggleChecked()
        if (!listItem.isChecked) {
            checkItemsList.remove(listItem)
        } else {
            checkItemsList.add(listItem)
        }
    }

    fun toggleChecked(listItem: ListItem, holder: MediaInfoRecyclerViewHolder) {
        toggleChecked(listItem)
        if (listItem.isChecked) {
            if (isGrid) {
                holder.checkIconGrid.visibility = View.VISIBLE
            } else {
                holder.root.setBackgroundColor(
                    superContext.resources
                        .getColor(R.color.highlight_yellow_50)
                )
            }
        } else {
            if (isGrid) {
                holder.checkIconGrid.visibility = View.INVISIBLE
            } else {
                holder.root.background = superContext.resources.getDrawable(R.drawable.ripple)
            }
        }
    }

    fun uncheckChecked() {
//        val removeItemsIdx = checkItemsList.map { it.position }
        val removeItemsIdx = arrayListOf<Int>()
        /*for (listItem in checkItemsList) {
            listItem.toggleChecked()
        }*/
        getMediaFilesListItems().forEachIndexed { index, listItem ->
            if (listItem.isChecked) {
                removeItemsIdx.add(index)
                listItem.toggleChecked()
            }
        }
        checkItemsList.clear()
        removeItemsIdx.forEach {
            notifyItemChanged(it)
        }
    }

    fun removeChecked(): Boolean {
        val syncList = Collections.synchronizedList(getMediaFilesListItems())
//        val removeItemsIdx = arrayListOf<Int>()
        val toRemove = arrayListOf<ListItem>()
//        val removeItemsIdx = checkItemsList.map { it.position }
        synchronized(syncList) {
            syncList.forEachIndexed { index, listItem ->
                if (listItem.isChecked) {
//                    removeItemsIdx.add(index)
                    toRemove.add(listItem)
                    listItem.toggleChecked()
                }
            }

            syncList.removeAll(toRemove)
            notifyDataSetChanged()
            /*removeItemsIdx.forEach {
                notifyItemRemoved(it)
            }*/
        }
        checkItemsList.clear()
        return true
    }

    fun checkedItemBytes(): String {
        var size = 0L
        checkItemsList.forEach {
            size += it.mediaFileInfo?.longSize ?: 0L
        }
        return FileUtils.formatStorageLength(superContext, size)
    }

    abstract fun getMediaFilesListItems(): MutableList<ListItem>

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_HEADER = 1
        const val EMPTY_LAST_ITEM = 2
        const val TYPE_BANNER = 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
        RecyclerView.ViewHolder {
        var view = View(superContext)
        when (viewType) {
            TYPE_BANNER -> {
                view = mInflater.inflate(
                    R.layout.list_banner_layout, parent,
                    false
                )
                return ListBannerViewHolder(view)
            }
            TYPE_ITEM -> {
                view = mInflater.inflate(
                    if (isGrid) R.layout.media_info_grid_layout
                    else R.layout.media_info_row_layout,
                    parent,
                    false
                )
                return MediaInfoRecyclerViewHolder(view)
            }
            TYPE_HEADER -> {
                view = mInflater.inflate(
                    R.layout.list_header, parent,
                    false
                )
                return HeaderViewHolder(view)
            }
            EMPTY_LAST_ITEM -> {
                view = mInflater.inflate(
                    R.layout.empty_viewholder_layout, parent,
                    false
                )
                return EmptyViewHolder(view)
            }
            else -> {
                throw IllegalStateException("Illegal $viewType in apps adapter")
            }
        }
    }

    @CallSuper
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MediaInfoRecyclerViewHolder) {
            getMediaFilesListItems()[position].run {
                mediaFileInfo?.let {
                    mediaFileInfo ->
                    holder.infoTitle.text = mediaFileInfo.title
                    Glide.with(superContext).clear(holder.iconView)
                    superPreloader.loadImage(mediaFileInfo.path, holder.iconView, isGrid)
                    if (isChecked) {
                        if (isGrid) {
                            holder.checkIconGrid.visibility = View.VISIBLE
                        } else {
                            holder.root.setBackgroundColor(
                                superContext.resources
                                    .getColor(R.color.highlight_yellow_50)
                            )
                        }
                    } else {
                        if (isGrid) {
                            holder.checkIconGrid.visibility = View.INVISIBLE
                        } else {
                            holder.root.background =
                                superContext.resources.getDrawable(R.drawable.ripple)
                        }
                    }
                    holder.root.setOnLongClickListener {
                        toggleChecked(this, holder)
                        invalidateCheckedTitle()
                        true
                    }
                    val formattedDate = mediaFileInfo.getModificationDate(superContext)
                    val formattedSize = mediaFileInfo.getFormattedSize(superContext)
                    mediaFileInfo.extraInfo?.let { extraInfo ->
                        when (extraInfo.mediaType) {
                            MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                                processImageMediaInfo(holder, mediaFileInfo)
                            }
                            MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                                processVideoMediaInfo(holder, mediaFileInfo)
                            }
                            MediaFileInfo.MEDIA_TYPE_AUDIO -> {
                                processAudioMediaInfo(holder, mediaFileInfo)
                            }
                            MediaFileInfo.MEDIA_TYPE_DOCUMENT -> {
                                holder.infoSummary.text = "$formattedDate | $formattedSize"
                                holder.extraInfo.text = ""
                            }
                            MediaFileInfo.MEDIA_TYPE_UNKNOWN -> {
                                holder.infoSummary.text = "$formattedDate | $formattedSize"
                                holder.extraInfo.text = ""
                                holder.root.background = ResourcesCompat
                                    .getDrawable(
                                        superContext.resources,
                                        R.drawable.background_curved_recents, superContext.theme
                                    )
                            }
                        }
                    }

                    // override click listener in case we have any single item checked

                    holder.root.setOnClickListener {
                        if (checkItemsList.size > 0) {
                            toggleChecked(this, holder)
                            invalidateCheckedTitle()
                        } else {
                            listItemPressedCallback?.invoke(mediaFileInfo)
                            mediaFileInfo
                                .triggerMediaFileInfoAction(WeakReference(superContext))
                        }
                    }
                }
            }
        }
    }

    override fun getPopupText(position: Int): String {
        return getMediaFilesListItems()[position].header ?: ""
    }

    fun invalidateCheckedTitle() {
        toggleCheckCallback?.invoke(checkItemsList.size, itemCount, checkedItemBytes())
    }

    private val mInflater: LayoutInflater
        get() = superContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun processImageMediaInfo(
        holder: MediaInfoRecyclerViewHolder,
        mediaFileInfo: MediaFileInfo
    ) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.imageMetaData?.width}" +
            "x${mediaFileInfo.extraInfo.imageMetaData?.height}"
        holder.extraInfo.text = ""
    }

    private fun processAudioMediaInfo(
        holder: MediaInfoRecyclerViewHolder,
        mediaFileInfo: MediaFileInfo
    ) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.audioMetaData?.albumName} " +
            "| ${mediaFileInfo.extraInfo.audioMetaData?.artistName}"
        mediaFileInfo.extraInfo.audioMetaData?.duration?.let {
            holder.extraInfo.text = AudioUtils.getReadableDurationString(it) ?: ""
        }
    }

    private fun processVideoMediaInfo(
        holder: MediaInfoRecyclerViewHolder,
        mediaFileInfo: MediaFileInfo
    ) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.videoMetaData?.width}" +
            "x${mediaFileInfo.extraInfo.videoMetaData?.height}"
        mediaFileInfo.extraInfo.videoMetaData?.duration?.let {
            holder.extraInfo.text = AudioUtils.getReadableDurationString(it) ?: ""
        }
    }

    @Target(AnnotationTarget.TYPE)
    @IntDef(
        TYPE_ITEM,
        TYPE_HEADER,
        EMPTY_LAST_ITEM,
        TYPE_BANNER
    )
    annotation class ListItemType

    data class ListItem(
        var mediaFileInfo: MediaFileInfo?,
        var listItemType: @ListItemType Int = TYPE_ITEM,
        var header: String? = null,
//        val position: Int,
        var isChecked: Boolean = false
    ) {
        constructor(listItemType: @ListItemType Int) : this(
            null,
            listItemType
        )
        constructor(listItemType: @ListItemType Int, header: String) : this(
            null,
            listItemType, header
        )

        fun toggleChecked() {
            isChecked = !isChecked
        }
    }
}
