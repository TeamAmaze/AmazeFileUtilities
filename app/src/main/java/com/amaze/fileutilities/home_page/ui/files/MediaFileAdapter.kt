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
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.image_viewer.ImageViewerDialogActivity
import com.amaze.fileutilities.utilis.EmptyViewHolder
import com.amaze.fileutilities.utilis.HeaderViewHolder
import com.amaze.fileutilities.video_player.VideoPlayerDialogActivity
import com.bumptech.glide.Glide
import kotlin.math.roundToInt

class MediaFileAdapter(
    val context: Context,
    val preloader: MediaAdapterPreloader,
    private var sortingPreference: MediaFileListSorter.SortingPreference,
    private val mediaFileInfoList: MutableList<MediaFileInfo>,
    private val isRecentFilesList: Boolean
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            if (mediaFileInfoList.size == 0) {
                return
            }
            if (!isRecentFilesList) {
                MediaFileListSorter.generateMediaFileListHeadersAndSort(
                    context,
                    mediaFileInfoList, sortingPreference
                )
            }
            var lastHeader: String? = null
            mediaFileInfoList.forEach {
                if ((lastHeader == null || it.listHeader != lastHeader) &&
                    !isRecentFilesList
                ) {
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
                TYPE_HEADER -> {
                    view = mInflater.inflate(
                        R.layout.list_header, parent,
                        false
                    )
                    return HeaderViewHolder(view)
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
        when (holder) {
            is HeaderViewHolder -> {
                holder.setText(
                    mediaFileListItems[position].header
                        ?: context.resources.getString(R.string.undetermined)
                )
            }
            is MediaInfoRecyclerViewHolder -> {
                mediaFileListItems[position].run {
                    mediaFileInfo?.let {
                        mediaFileInfo ->
                        holder.infoTitle.text = mediaFileInfo.title
                        Glide.with(context).clear(holder.iconView)
                        val formattedDate = mediaFileInfo.getModificationDate(context)
                        val formattedSize = mediaFileInfo.getFormattedSize(context)
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
                                MediaFileInfo.MEDIA_TYPE_UNKNOWN -> {
                                    preloader.loadImage(mediaFileInfo.path, holder.iconView)
                                    holder.infoSummary.text = "$formattedDate | $formattedSize"
                                    holder.root.setOnClickListener {
                                        startExternalViewAction(mediaFileInfo)
                                    }
                                }
                                MediaFileInfo.MEDIA_TYPE_DOCUMENT -> {
                                    holder.infoSummary.text = "$formattedDate | $formattedSize"
                                    holder.root.setOnClickListener {
                                        startExternalViewAction(mediaFileInfo)
                                    }
                                }
                            }
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

    /**
     * Set list elements
     */
    fun setData(data: List<MediaFileInfo>, sortPref: MediaFileListSorter.SortingPreference) {
        mediaFileInfoList.run {
            clear()
            sortingPreference = sortPref
            addAll(data)
            mediaFileListItems = mutableListOf()
            notifyDataSetChanged()
        }
    }

    private fun startExternalViewAction(mediaFileInfo: MediaFileInfo) {
        val intent = Intent()
        intent.data = mediaFileInfo.getContentUri(context)
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun processImageMediaInfo(holder: MediaInfoRecyclerViewHolder, mediaFileInfo: MediaFileInfo) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.imageMetaData?.width}" +
                    "x${mediaFileInfo.extraInfo.imageMetaData?.height}"
        preloader.loadImage(mediaFileInfo.path, holder.iconView)
        holder.root.setOnClickListener {
            val intent = Intent(context, ImageViewerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(context)
            context.startActivity(intent)
        }
    }

    private fun processAudioMediaInfo(holder: MediaInfoRecyclerViewHolder, mediaFileInfo: MediaFileInfo) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.audioMetaData?.albumName} " +
                    "| ${mediaFileInfo.extraInfo.audioMetaData?.artistName}"
        holder.extraInfo.text =
            mediaFileInfo.extraInfo.videoMetaData?.duration?.toString() ?: ""
        holder.root.setOnClickListener {
            val intent = Intent(context, AudioPlayerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(context)
            context.startActivity(intent)
        }
    }

    private fun processVideoMediaInfo(holder: MediaInfoRecyclerViewHolder, mediaFileInfo: MediaFileInfo) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.videoMetaData?.width}" +
                    "x${mediaFileInfo.extraInfo.videoMetaData?.height}"
        holder.extraInfo.text =
            mediaFileInfo.extraInfo.videoMetaData?.duration?.toString() ?: ""
        preloader.loadImage(mediaFileInfo.path, holder.iconView)
        holder.root.setOnClickListener {
            val intent = Intent(context, VideoPlayerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(context)
            context.startActivity(intent)
        }
    }

    @Target(AnnotationTarget.TYPE)
    @IntDef(
        TYPE_ITEM,
        TYPE_HEADER,
        EMPTY_LAST_ITEM,
    )
    annotation class ListItemType

    data class ListItem(
        var mediaFileInfo: MediaFileInfo?,
        var listItemType: @ListItemType Int = TYPE_ITEM,
        var header: String? = null
    ) {
        constructor(listItemType: @ListItemType Int) : this(null, listItemType)
        constructor(listItemType: @ListItemType Int, header: String) : this(
            null,
            listItemType, header
        )
    }
}
