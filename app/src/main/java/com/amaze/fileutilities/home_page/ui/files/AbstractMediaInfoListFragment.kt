/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.home_page.ui.files

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.playlist.PlaylistsUtil
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeView
import com.amaze.fileutilities.home_page.ui.options.CastActivity
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractMediaInfoListFragment :
    ItemsActionBarFragment(),
    MediaFileAdapter.OptionsMenuSelected {

    private var mediaFileAdapter: MediaFileAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: GridLayoutManager? = null
    private val MAX_PRELOAD = 100
    private var log: Logger = LoggerFactory.getLogger(AbstractMediaInfoListFragment::class.java)
    private var mediaPreloader: MediaAdapterPreloader<MediaFileInfo>? = null

    override fun onDestroyView() {
        mediaPreloader?.clear()
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

    abstract fun getMediaAdapterPreloader(isGrid: Boolean): MediaAdapterPreloader<MediaFileInfo>

    abstract fun getRecyclerView(): RecyclerView

    abstract fun getItemPressedCallback(mediaFileInfo: MediaFileInfo)

    abstract fun setupAdapter()

    abstract fun adapterItemSelected(checkedCount: Int)

    override fun hideActionBarOnClick(): Boolean {
        return true
    }

    override fun getMediaFileAdapter(): MediaFileAdapter? {
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
                val sizeProvider = ViewPreloadSizeProvider<MediaFileInfo>()
                val isList = requireContext()
                    .getAppCommonSharedPreferences().getBoolean(
                        "${getMediaListType()}_${PreferencesConstants.KEY_MEDIA_LIST_TYPE}",
                        PreferencesConstants.DEFAULT_MEDIA_LIST_TYPE
                    )
                mediaPreloader = getMediaAdapterPreloader(!isList)
                val recyclerViewPreloader = RecyclerViewPreloader(
                    Glide.with(requireActivity()),
                    mediaPreloader!!,
                    sizeProvider,
                    MAX_PRELOAD
                )
                mediaFileAdapter = MediaFileAdapter(
                    requireActivity(),
                    mediaPreloader!!,
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
                        if (checkedSize == 1) enableLocateFileFab() else disableLocateFileFab()
                    } else {
                        hideActionBar()
                    }
                    adapterItemSelected(checkedSize)
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
                        R.id.delete_playlist -> {
                            if (actionItems.isNotEmpty()) {
                                val playlist = actionItems[0]
                                    .extraInfo?.audioMetaData?.playlist?.name
                                playlist?.let {
                                    Utils.buildDeletePlaylistDialog(requireContext(), playlist) {
                                        PlaylistsUtil.deletePlaylists(
                                            requireContext(),
                                            arrayListOf(
                                                actionItems[0]
                                                    .extraInfo?.audioMetaData?.playlist
                                            )
                                        )
                                        // reset the dataset
                                        getFilesViewModelObj()
                                            .usedPlaylistsSummaryTransformations = null
                                        setupAdapter()
                                    }.show()
                                }
                            }
                        }
                        R.id.rename_playlist -> {
                            if (actionItems.isNotEmpty()) {
                                val playlist = actionItems[0]
                                    .extraInfo?.audioMetaData?.playlist?.name
                                playlist?.let {
                                    val playlistId = actionItems[0]
                                        .extraInfo?.audioMetaData?.playlist?.id
                                    if (playlistId != -1L) {
                                        Utils.buildRenamePlaylistDialog(
                                            requireContext(),
                                            playlist
                                        ) {
                                            newName ->
                                            PlaylistsUtil.renamePlaylist(
                                                requireContext(),
                                                playlistId!!,
                                                newName
                                            )
                                            // reset the dataset
                                            getFilesViewModelObj()
                                                .usedPlaylistsSummaryTransformations = null
                                            setupAdapter()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, {
                    getRecyclerView().clearOnScrollListeners()
                    setupAdapter()
                }, {
                    when (getMediaListType()) {
                        MediaFileAdapter.MEDIA_TYPE_IMAGES -> {
                            getFilesViewModelObj().usedImagesSummaryTransformations = null
                        }
                        MediaFileAdapter.MEDIA_TYPE_DOCS -> {
                            getFilesViewModelObj().usedDocsSummaryTransformations = null
                        }
                        MediaFileAdapter.MEDIA_TYPE_AUDIO -> {
                            getFilesViewModelObj().usedAudiosSummaryTransformations = null
                        }
                        MediaFileAdapter.MEDIA_TYPE_VIDEO -> {
                            getFilesViewModelObj().usedVideosSummaryTransformations = null
                        }
                        else -> {
                            log.warn("unsupported operation to reset data on banner action")
                        }
                    }
                    reloadFragment()
                }
                )
                getRecyclerView().addOnScrollListener(recyclerViewPreloader)
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

                val slideUpAnimation: Animation =
                    AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in)
                getRecyclerView().startAnimation(slideUpAnimation)
                getRecyclerView().visibility = View.VISIBLE
            }
        }
    }
    private fun reloadFragment() {
        val navController = NavHostFragment.findNavController(this)
        navController.popBackStack()
        navController.navigate(R.id.navigation_files)
    }
}
