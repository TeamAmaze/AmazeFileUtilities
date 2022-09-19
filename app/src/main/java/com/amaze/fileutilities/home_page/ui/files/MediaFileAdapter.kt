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
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Build
import android.view.MenuItem
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.CastActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeHeaderView
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.HeaderViewHolder
import com.amaze.fileutilities.utilis.ListBannerViewHolder
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.executeAsyncTask

class MediaFileAdapter(
    val context: Context,
    private val preloader: MediaAdapterPreloader,
    private val optionsMenuSelected: OptionsMenuSelected,
    isGrid: Boolean,
    private var sortingPreference: MediaFileListSorter.SortingPreference,
    private val mediaFileInfoList: MutableList<MediaFileInfo>,
    private val mediaListType: Int,
    private val drawBannerCallback: (mediaTypeHeader: MediaTypeHeaderView) -> Unit,
    listItemPressedCallback: (mediaFileInfo: MediaFileInfo) -> Unit,
    toggleCheckCallback: (checkedSize: Int, itemsCount: Int, bytesFormatted: String) -> Unit,
    private val titleOverflowPopupClick:
        ((item: MenuItem, actionItems: List<MediaFileInfo>) -> Unit)?
) : AbstractMediaFilesAdapter(
    context,
    preloader, isGrid, listItemPressedCallback, toggleCheckCallback
) {

    companion object {
        const val MEDIA_TYPE_AUDIO = 0
        const val MEDIA_TYPE_VIDEO = 1
        const val MEDIA_TYPE_DOCS = 2
        const val MEDIA_TYPE_IMAGES = 3
        const val MEDIA_TYPE_UNKNOWN = 4
        const val MEDIA_TYPE_APKS = 5
    }

    private var onlyItemsCounts: Int = 0
    private var lastCurrentPlayingPositionAnimation: Int = -1
    private var currentPlayingPositionAnimation: Int = -1
    private var headerListItems: MutableList<ListItem> = mutableListOf()
    private var mediaFileListItems: MutableList<ListItem> = mutableListOf()
        set(value) {
            value.clear()
            preloader.clear()
            onlyItemsCounts = 0
            headerListItems.clear()
            MediaFileListSorter.generateMediaFileListHeadersAndSort(
                context,
                mediaFileInfoList, sortingPreference
            )
            var lastHeader: String? = null
            var position = 0
            value.add(ListItem(TYPE_BANNER))
            preloader.addItem("")
            for (i in 0 until mediaFileInfoList.size) {
                if (lastHeader == null || mediaFileInfoList[i].listHeader != lastHeader) {
                    value.add(ListItem(TYPE_HEADER, mediaFileInfoList[i].listHeader))
                    preloader.addItem("")
                    headerListItems.add(
                        ListItem(
                            TYPE_HEADER, mediaFileInfoList[i].listHeader
                        )
                    )
                    lastHeader = mediaFileInfoList[i].listHeader
                }
                // position here is irrelevant
                value.add(
                    ListItem(
                        mediaFileInfo = mediaFileInfoList[i],
                        header = mediaFileInfoList[i].listHeader
                    )
                )
                preloader.addItem(mediaFileInfoList[i].path)
                onlyItemsCounts++
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
                    mediaFileListItems[position].header
                        ?: context.resources.getString(R.string.undetermined)
                )

                holder.setOverflowButtons(
                    context,
                    if (mediaListType == MEDIA_TYPE_AUDIO)
                        R.menu.audio_list_overflow else R.menu.generic_list_overflow
                ) {
                    item ->
                    titleOverflowPopupClick?.invoke(
                        item,
                        mediaFileInfoList
                            .filter { it.listHeader == mediaFileListItems[position].header }
                    )
                    true
                }
            }
            is ListBannerViewHolder -> {
                setBannerResources(holder)
            }
            is MediaInfoRecyclerViewHolder -> {
                if (mediaListType == MEDIA_TYPE_AUDIO) {
                    if (lastCurrentPlayingPositionAnimation != -1 &&
                        position == lastCurrentPlayingPositionAnimation
                    ) {
                        holder.currentPlayingImageView.visibility = View.GONE
                    }
                    if (currentPlayingPositionAnimation != -1 &&
                        position == currentPlayingPositionAnimation
                    ) {
                        val drawable = holder.currentPlayingImageView.drawable
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                            drawable is AnimatedVectorDrawable
                        ) {
                            holder.currentPlayingImageView.visibility = View.VISIBLE
                            drawable.start()
                        }
                    } else {
                        holder.currentPlayingImageView.visibility = View.GONE
                    }
                    Utils.marqueeAfterDelay(2000, holder.infoTitle)
                    Utils.marqueeAfterDelay(2000, holder.infoSummary)
                    holder.root.setOnClickListener {
                        // for audio list fragment we want to just show bottom sheet
                        val listItem = mediaFileListItems[position]
                        if (checkItemsList.size > 0) {
                            toggleChecked(listItem, position)
                            invalidateCheckedTitle(getOnlyItemsCount())
                        } else {
                            listItem.mediaFileInfo?.getContentUri(context)?.let {
                                uri ->
                                (context as CastActivity)
                                    .showCastFileDialog(
                                        mediaFileListItems[position].mediaFileInfo!!,
                                        MEDIA_TYPE_AUDIO
                                    ) {
                                        context.lifecycleScope
                                            .executeAsyncTask<Void, List<Uri>>({}, {
                                                mediaFileInfoList.mapNotNull {
                                                    it.getContentUri(context)
                                                }
                                            }, {
                                                AudioPlayerService.runService(
                                                    uri,
                                                    it,
                                                    context
                                                )
                                            }, {})
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

    override fun getOnlyItemsCount(): Int {
        return onlyItemsCounts
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

    fun invalidateData(sortPref: MediaFileListSorter.SortingPreference) {
        mediaFileInfoList.run {
            sortingPreference = sortPref
            // triggers set call
            mediaFileListItems = mutableListOf()
            notifyDataSetChanged()
        }
    }

    fun invalidateCurrentPlayingAnimation(uri: Uri) {
        (context as CastActivity).lifecycleScope
            .executeAsyncTask<Void, Int?>({}, {
                getMediaFilesListItems().forEachIndexed { index, listItem ->
                    if (listItem.mediaFileInfo != null &&
                        listItem.mediaFileInfo!!.getContentUri(context) == uri
                    ) {
                        return@executeAsyncTask index
                    }
                }
                null
            }, {
                if (it != null) {
                    if (currentPlayingPositionAnimation != -1) {
                        lastCurrentPlayingPositionAnimation = currentPlayingPositionAnimation
                        notifyItemChanged(lastCurrentPlayingPositionAnimation)
                    }
                    currentPlayingPositionAnimation = it
                    notifyItemChanged(currentPlayingPositionAnimation)
                }
            }, {})
    }

    private fun setBannerResources(holder: ListBannerViewHolder) {
        when (mediaFileInfoList[0].extraInfo?.mediaType) {
            MediaFileInfo.MEDIA_TYPE_AUDIO -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.peach_70, context.theme
                        ),
                    R.drawable.background_curved_bottom_peach
                )
                holder.mediaTypeHeaderView.setAccentImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.banner_audio_list, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.orange_70, context.theme
                        ),
                    R.drawable.background_curved_bottom_orange
                )
                holder.mediaTypeHeaderView.setAccentImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.banner_video_list, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.purple_70, context.theme
                        ),
                    R.drawable.background_curved_bottom_pink
                )
                holder.mediaTypeHeaderView.setAccentImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.banner_image_list, context.theme
                    )!!
                )
            }
            MediaFileInfo.MEDIA_TYPE_DOCUMENT -> {
                holder.mediaTypeHeaderView.setHeaderColor(
                    ResourcesCompat
                        .getColor(
                            context.resources,
                            R.color.green_banner_70, context.theme
                        ),
                    R.drawable.background_curved_bottom_green
                )
                holder.mediaTypeHeaderView.setAccentImageSrc(
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.banner_document_list, context.theme
                    )!!
                )
            }
        }
        holder.mediaTypeHeaderView.initOptionsItems(
            optionsMenuSelected, headerListItems,
            sortingPreference, mediaListType
        )
        drawBannerCallback.invoke(holder.mediaTypeHeaderView)
    }

    override fun getMediaFilesListItems(): MutableList<ListItem> {
        return mediaFileListItems
    }

    interface OptionsMenuSelected {
        fun sortBy(sortingPreference: MediaFileListSorter.SortingPreference)
        fun groupBy(sortingPreference: MediaFileListSorter.SortingPreference)
        fun switchView(isList: Boolean)
        fun select(headerItem: ListItem)
    }
}
