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
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IntDef
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.audio_player.AudioUtils
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.home_page.ui.files.MediaInfoRecyclerViewHolder
import com.amaze.fileutilities.image_viewer.ImageViewerDialogActivity
import com.amaze.fileutilities.video_player.VideoPlayerDialogActivity
import com.bumptech.glide.Glide
import kotlin.math.roundToInt

abstract class AbstractMediaFilesAdapter(
    private val superContext: Context,
    private val superPreloader: MediaAdapterPreloader
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                        superContext.resources.getDimension(R.dimen.fifty_six_dp) +
                            superContext.resources.getDimension(R.dimen.material_generic)
                        )
                        .roundToInt()
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
                                holder.root.setOnClickListener {
                                    startExternalViewAction(mediaFileInfo)
                                }
                            }
                            MediaFileInfo.MEDIA_TYPE_UNKNOWN -> {
                                superPreloader.loadImage(mediaFileInfo.path, holder.iconView)
                                holder.infoSummary.text = "$formattedDate | $formattedSize"
                                holder.extraInfo.text = ""
                                holder.root.setOnClickListener {
                                    startExternalViewAction(mediaFileInfo)
                                }
                                holder.root.background = ResourcesCompat
                                    .getDrawable(
                                        superContext.resources,
                                        R.drawable.background_curved_recents, superContext.theme
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    private val mInflater: LayoutInflater
        get() = superContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun startExternalViewAction(mediaFileInfo: MediaFileInfo) {
        val intent = Intent()
        intent.data = mediaFileInfo.getContentUri(superContext)
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        superContext.startActivity(intent)
    }

    private fun processImageMediaInfo(
        holder: MediaInfoRecyclerViewHolder,
        mediaFileInfo: MediaFileInfo
    ) {
        holder.infoSummary.text =
            "${mediaFileInfo.extraInfo!!.imageMetaData?.width}" +
            "x${mediaFileInfo.extraInfo.imageMetaData?.height}"
        superPreloader.loadImage(mediaFileInfo.path, holder.iconView)
        holder.extraInfo.text = ""
        holder.root.setOnClickListener {
            val intent = Intent(superContext, ImageViewerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(superContext)
            superContext.startActivity(intent)
        }
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
        holder.root.setOnClickListener {
            val intent = Intent(superContext, AudioPlayerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(superContext)
            superContext.startActivity(intent)
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
        superPreloader.loadImage(mediaFileInfo.path, holder.iconView)
        holder.root.setOnClickListener {
            val intent = Intent(superContext, VideoPlayerDialogActivity::class.java)
            intent.data = mediaFileInfo.getContentUri(superContext)
            superContext.startActivity(intent)
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
        var header: String? = null
    ) {
        constructor(listItemType: @ListItemType Int) : this(null, listItemType)
        constructor(listItemType: @ListItemType Int, header: String) : this(
            null,
            listItemType, header
        )
    }
}
