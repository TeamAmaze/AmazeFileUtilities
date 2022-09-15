/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.CastActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeView
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

abstract class AbstractMediaInfoListFragment :
    ItemsActionBarFragment(),
    MediaFileAdapter.OptionsMenuSelected {

    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var mediaFileAdapter: MediaFileAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: GridLayoutManager? = null
    private val MAX_PRELOAD = 100

    override fun onDestroyView() {
        getMediaAdapterPreloader().clear()
        super.onDestroyView()
    }

    override fun sortBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun groupBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun switchView(isList: Boolean) {
        resetAdapter()
    }

    override fun select(headerItem: AbstractMediaFilesAdapter.ListItem) {
        getMediaFileAdapter()?.getMediaFilesListItems()?.forEachIndexed { index, listItem ->
            if (listItem == headerItem) {
                getRecyclerView().scrollToPosition(index + 5)
                return@forEachIndexed
            }
        }
    }

    abstract fun getFileStorageSummaryAndMediaFileInfoPair():
        Pair<FilesViewModel.StorageSummary, List<MediaFileInfo>?>?

    abstract fun getMediaAdapterPreloader(): MediaAdapterPreloader

    abstract fun getRecyclerView(): RecyclerView

    abstract fun getItemPressedCallback(mediaFileInfo: MediaFileInfo)

    override fun hideActionBarOnClick(): Boolean {
        return true
    }

    override fun getMediaFileAdapter(): AbstractMediaFilesAdapter? {
        return mediaFileAdapter
    }

    // make sure to set getFileStorageSummaryAndMediaFileInfoPair before calling
    fun resetAdapter() {
        getFileStorageSummaryAndMediaFileInfoPair()?.let {
            val storageSummary = it.first
            val mediaFileInfoList = it.second
            mediaFileInfoList?.let {
                val usedSpace =
                    FileUtils.formatStorageLength(
                        requireContext(), storageSummary.usedSpace!!
                    )
                val usedTotalSpace = FileUtils.formatStorageLength(
                    requireContext(),
                    storageSummary.totalUsedSpace!!
                )
                // set list adapter
                val sizeProvider = ViewPreloadSizeProvider<String>()
                recyclerViewPreloader = RecyclerViewPreloader(
                    Glide.with(requireActivity()),
                    getMediaAdapterPreloader(),
                    sizeProvider,
                    MAX_PRELOAD
                )
                val isList = requireContext()
                    .getAppCommonSharedPreferences().getBoolean(
                        "${getMediaListType()}_${PreferencesConstants.KEY_MEDIA_LIST_TYPE}",
                        PreferencesConstants.DEFAULT_MEDIA_LIST_TYPE
                    )
                mediaFileAdapter = MediaFileAdapter(
                    requireActivity(),
                    getMediaAdapterPreloader(),
                    this@AbstractMediaInfoListFragment, !isList,
                    MediaFileListSorter.SortingPreference.newInstance(
                        requireContext()
                            .getAppCommonSharedPreferences(),
                        getMediaListType()
                    ),
                    ArrayList(mediaFileInfoList),
                    getMediaListType(),
                    {
                        mediaTypeHeader ->
                        (requireContext() as CastActivity)
                            .refactorCastButton(mediaTypeHeader.getMediaRouteButton())
                        mediaTypeHeader.setProgress(
                            MediaTypeView.MediaTypeContent(
                                storageSummary.items, usedSpace,
                                storageSummary.progress, usedTotalSpace,
                                storageSummary.totalItems
                            )
                        )
                    }, {
                    mediaFileInfo ->
                    getItemPressedCallback(mediaFileInfo)
                }, {
                    checkedSize, itemsCount, bytesFormatted ->
                    val title = "$checkedSize / $itemsCount" +
                        " ($bytesFormatted)"
                    if (checkedSize > 0) {
                        setupShowActionBar()
                        setupCommonButtons()
                        getLocateFileButton()?.visibility = if (checkedSize == 1)
                            View.VISIBLE else View.GONE
                    } else {
                        hideActionBar()
                    }

                    val countView = getCountView()
                    countView?.text = title
                }, {
                    item, actionItems ->
                    when (item.itemId) {
                        R.id.share -> {
                            performShareAction(actionItems)
                        }
                        R.id.delete -> {
                            performDeleteAction(actionItems)
                        }
                        R.id.shuffle -> {
                            context?.let {
                                context ->
                                performShuffleAction(context, actionItems)
                            }
                        }
                    }
                }
                )
                getRecyclerView().addOnScrollListener(recyclerViewPreloader!!)
                linearLayoutManager = LinearLayoutManager(context)
                gridLayoutManager = GridLayoutManager(
                    context,
                    requireContext()
                        .getAppCommonSharedPreferences()
                        .getInt(
                            PreferencesConstants.KEY_GRID_VIEW_COLUMN_COUNT,
                            PreferencesConstants.DEFAULT_GRID_VIEW_COLUMN_COUNT
                        )
                )
                Utils.setGridLayoutManagerSpan(gridLayoutManager!!, mediaFileAdapter!!)
                getRecyclerView().layoutManager =
                    if (isList) linearLayoutManager else gridLayoutManager
                val animator = DefaultItemAnimator()
                getRecyclerView().itemAnimator = animator
                getRecyclerView().adapter = mediaFileAdapter
            }
        }
    }
}
