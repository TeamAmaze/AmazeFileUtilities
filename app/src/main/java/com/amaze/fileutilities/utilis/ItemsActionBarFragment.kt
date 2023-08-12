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

package com.amaze.fileutilities.utilis

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.audio_player.playlist.PlaylistsUtil
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.analyse.ReviewImagesFragment
import com.amaze.fileutilities.home_page.ui.files.AbstractMediaInfoListFragment
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ItemsActionBarFragment : AbstractMediaFileInfoOperationsFragment() {

    private var log: Logger = LoggerFactory.getLogger(ItemsActionBarFragment::class.java)

    abstract fun hideActionBarOnClick(): Boolean
    abstract fun getMediaFileAdapter(): AbstractMediaFilesAdapter?
    abstract fun getMediaListType(): Int
    abstract fun getAllOptionsFAB(): List<FloatingActionButton>
    abstract fun showOptionsCallback()
    abstract fun hideOptionsCallback()

    override fun getFilesViewModelObj(): FilesViewModel {
        return filesViewModel
    }

    override fun uninstallAppCallback(mediaFileInfo: MediaFileInfo) {
        refreshListAfterTrashCallback(listOf(mediaFileInfo), emptyList())
        requireContext().showToastOnBottom(
            resources
                .getString(R.string.successfully_deleted)
        )
    }

    private val filesViewModel: FilesViewModel by activityViewModels()

    private var optionsActionBar: View? = null

    private var areNonOptionsFabShown = false
    private var isLocateFabEnabled = true

    override fun onDestroyView() {
        (activity as MainActivity).invalidateSelectedActionBar(
            false,
            hideActionBarOnClick(), handleBackPressed()
        )
        (activity as MainActivity).invalidateBottomBar(true)
        super.onDestroyView()
    }

    fun setupShowActionBar() {
        optionsActionBar = (activity as MainActivity).invalidateSelectedActionBar(
            true,
            hideActionBarOnClick(), handleBackPressed()
        )
        getOptionsFab().show()
        areNonOptionsFabShown = false
        isLocateFabEnabled = true
        getOptionsFab().setOnClickListener {
            areNonOptionsFabShown = !areNonOptionsFabShown
            if (areNonOptionsFabShown) {
                showNonOptionsFab()
            } else {
                hideNonOptionsFab()
            }
        }
        hideNonOptionsFab()
        showOptionsCallback()
    }

    fun hideActionBar() {
        optionsActionBar = (activity as MainActivity)
            .invalidateSelectedActionBar(
                false, hideActionBarOnClick(),
                handleBackPressed()
            )
        getAllOptionsFAB().forEach {
            it.hide()
        }
        getPlayNextButton()?.visibility = View.GONE
        hideOptionsCallback()
    }

    fun getLocateFileFab(): FloatingActionButton {
        return getNonOptionsFab().filter {
            R.id.locateFileButtonFab == it.id
        }[0]
    }

    fun disableLocateFileFab() {
        isLocateFabEnabled = false
        showNonOptionsFab()
    }

    fun enableLocateFileFab() {
        isLocateFabEnabled = true
        showNonOptionsFab()
    }

    fun handleBackPressed(): (() -> Unit) {
        return {
            if (hideActionBarOnClick()) {
                getMediaFileAdapter()?.uncheckChecked()
            } else {
                val reviewFragment = parentFragmentManager
                    .findFragmentByTag(ReviewImagesFragment.FRAGMENT_TAG)
                val transaction = parentFragmentManager.beginTransaction()
                reviewFragment?.let {
                    transaction.remove(reviewFragment)
                    transaction.commit()
                }
            }
        }
    }

    fun getCountView(): AppCompatTextView? {
        return optionsActionBar?.findViewById(R.id.title)
    }

    fun getThumbsDown(): ImageView? {
        return optionsActionBar?.findViewById(R.id.thumbsDown)
    }

    fun getPlayNextButton(): TextView? {
        return optionsActionBar?.findViewById(R.id.playNextButton)
    }

    private fun getOptionsFab(): FloatingActionButton {
        return getAllOptionsFAB().filter {
            it.id == R.id.optionsButtonFab
        }[0]
    }

    private fun getNonOptionsFab(): List<FloatingActionButton> {
        return getAllOptionsFAB().filter {
            it.id != R.id.optionsButtonFab
        }
    }

    private fun showNonOptionsFab() {
        if (areNonOptionsFabShown) {
            getNonOptionsFab().forEach {
                if (it.id == R.id.locateFileButtonFab && !isLocateFabEnabled) {
                    it.hide()
                    it.visibility = View.GONE
                } else {
                    it.show()
                }
            }
        }
    }

    private fun hideNonOptionsFab() {
        if (!areNonOptionsFabShown) {
            getNonOptionsFab().forEach {
                if (it.id == R.id.locateFileButtonFab && !isLocateFabEnabled) {
                    it.hide()
                    it.visibility = View.GONE
                } else {
                    it.hide()
                }
            }
        }
    }

    private fun deleteFromFileViewmodelLists(toDelete: List<MediaFileInfo>) {
        if (toDelete.isEmpty()) {
            return
        }
        val imagesToDelete = toDelete.filter {
            it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_IMAGE
        }
        val videosToDelete = toDelete.filter {
            it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_VIDEO
        }
        val audioToDelete = toDelete.filter {
            it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_AUDIO
        }
        val docsToDelete = toDelete.filter {
            it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_DOCUMENT
        }
        val apksToDelete = toDelete.filter {
            it.extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_APK
        }
        if (imagesToDelete.isNotEmpty()) {
            filesViewModel.usedImagesSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, imagesToDelete)
                }
            }
        }
        if (videosToDelete.isNotEmpty()) {
            filesViewModel.usedVideosSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, videosToDelete)
                }
            }
        }
        if (audioToDelete.isNotEmpty()) {
            filesViewModel.usedAudiosSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, audioToDelete)
                }
            }
        }
        if (docsToDelete.isNotEmpty()) {
            filesViewModel.usedDocsSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, docsToDelete)
                }
            }
        }
        if (apksToDelete.isNotEmpty()) {
            // currently no way to distinguish whether user is deleting large apps or unused apps
            // so we clear both
            filesViewModel.unusedAppsLiveData = null
            filesViewModel.largeAppsLiveData = null
        }
    }
    private fun addToFileViewmodelLists(toAdd: List<MediaFileInfo>) {
        if (toAdd.isEmpty()) {
            return
        }
        when (toAdd[0].extraInfo?.mediaType) {
            MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                filesViewModel.usedImagesSummaryTransformations().observe(viewLifecycleOwner) {
                    if (it != null) {
                        filesViewModel.addMediaFilesToList(it.second, toAdd)
                    }
                }
            }
            MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                filesViewModel.usedVideosSummaryTransformations().observe(viewLifecycleOwner) {
                    if (it != null) {
                        filesViewModel.addMediaFilesToList(it.second, toAdd)
                    }
                }
            }
        }
    }

    fun setupCommonButtons() {
        getNonOptionsFab().forEach {
            fab ->
            fab.setOnClickListener {
                when (fab.id) {
                    R.id.selectAllButtonFab -> {
                        getMediaFileAdapter()?.checkAll()
                    }
                    R.id.deleteButtonFab -> {
                        getMediaFileAdapter()
                            ?.checkItemsList?.filter { it.mediaFileInfo != null }
                            ?.map { it.mediaFileInfo!! }
                            ?.let { toDelete ->
                                if (getMediaListType() == MediaFileAdapter.MEDIA_TYPE_APKS ||
                                    getMediaListType() == MediaFileAdapter.MEDIA_TYPE_TRASH_BIN
                                ) {
                                    performDeletePermanentlyAction(toDelete)
                                } else {
                                    performDeleteAction(toDelete)
                                }
                            }
                    }
                    R.id.restoreTrashButtonFab -> {
                        if (getMediaListType() == MediaFileAdapter.MEDIA_TYPE_TRASH_BIN) {
                            getMediaFileAdapter()?.checkItemsList?.filter {
                                it.mediaFileInfo != null
                            }?.map { it.mediaFileInfo!! }
                                ?.let { toRestore ->
                                    performRestoreAction(toRestore)
                                }
                        }
                    }
                    R.id.compressButtonFab -> {
                        getMediaFileAdapter()?.checkItemsList?.filter {
                            it.mediaFileInfo != null
                        }?.map { it.mediaFileInfo!! }
                            ?.let { toCompress ->
                                if (getMediaListType() == MediaFileAdapter.MEDIA_TYPE_IMAGES) {
                                    performCompressImagesAction(toCompress)
                                } else if (getMediaListType() == MediaFileAdapter
                                    .MEDIA_TYPE_VIDEO
                                ) {
                                    performCompressVideosAction(toCompress)
                                }
                            }
                    }
                    R.id.shareButtonFab -> {
                        getMediaFileAdapter()?.checkItemsList?.let {
                            checkedItems ->
                            val checkedMediaFiles = checkedItems.map { it.mediaFileInfo!! }
                            if (!checkedMediaFiles.isNullOrEmpty()) {
                                performShareAction(checkedMediaFiles)
                                getMediaFileAdapter()?.uncheckChecked()
                            } else {
                                requireContext()
                                    .showToastOnBottom(getString(R.string.no_item_selected))
                            }
                        }
                    }
                    R.id.locateFileButtonFab -> {
                        getMediaFileAdapter()?.checkItemsList?.map { it.mediaFileInfo!! }?.let {
                            openFile ->
                            if (openFile.isNotEmpty()) {
                                openFile[0].startLocateFileAction(requireContext())
                                getMediaFileAdapter()?.uncheckChecked()
                            }
                        }
                    }
                    R.id.addToPlaylistButtonFab -> {
                        getMediaFileAdapter()?.checkItemsList?.map { it.mediaFileInfo!! }?.let {
                            checkedItems ->
                            if (checkedItems.isNotEmpty()) {
                                Utils.buildAddToPlaylistDialog(requireContext(), {
                                    playlist ->
                                    PlaylistsUtil.addToPlaylist(
                                        requireContext(), checkedItems,
                                        playlist.id, true
                                    )
                                    // reset the dataset
                                    getFilesViewModelObj()
                                        .usedPlaylistsSummaryTransformations = null
                                    (this as AbstractMediaInfoListFragment).setupAdapter()
                                }, {
                                    Utils.buildCreateNewPlaylistDialog(requireContext()) {
                                        val playlistId = PlaylistsUtil
                                            .createPlaylist(requireContext(), it)
                                        if (playlistId != -1L) {
                                            PlaylistsUtil.addToPlaylist(
                                                requireContext(),
                                                checkedItems,
                                                playlistId, true
                                            )

                                            // reset the dataset
                                            getFilesViewModelObj()
                                                .usedPlaylistsSummaryTransformations = null
                                            (this as AbstractMediaInfoListFragment).setupAdapter()
                                        }
                                    }
                                }, {
                                    Utils.buildRemoveFromPlaylistDialog(requireContext()) {
                                        PlaylistsUtil.removeFromPlaylist(
                                            requireContext(),
                                            checkedItems
                                        )
                                        // reset the dataset
                                        getFilesViewModelObj()
                                            .usedPlaylistsSummaryTransformations = null
                                        (this as AbstractMediaInfoListFragment).setupAdapter()
                                    }.show()
                                }).show()
                            }
                        }
                    }
                    else -> {
                        // do nothing
                    }
                }
                hideNonOptionsFab()
            }
        }
    }

    fun performDeletePermanentlyAction(toDelete: List<MediaFileInfo>) {
        setupDeletePermanentlyButton(toDelete) {
            refreshListAfterTrashCallback(toDelete, emptyList())
            getMediaFileAdapter()?.invalidateList(toDelete)
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.successfully_deleted)
            )
        }
    }

    fun performDeleteAction(toDelete: List<MediaFileInfo>) {
        setupDeleteButton(toDelete) {
            refreshListAfterTrashCallback(toDelete, emptyList())
            getMediaFileAdapter()?.invalidateList(toDelete)
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.successfully_deleted)
            )
        }
    }

    fun performRestoreAction(toRestore: List<MediaFileInfo>) {
        setupRestoreButton(toRestore) {
            refreshListAfterTrashCallback(toRestore, emptyList())
            getMediaFileAdapter()?.invalidateList(toRestore)
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.successfully_restored)
            )
        }
    }

    fun performCompressImagesAction(toCompress: List<MediaFileInfo>) {
        setupCompressImagesButton(toCompress, layoutInflater) {
            toDelete, toAdd ->
            refreshListAfterTrashCallback(toDelete, toAdd)
            getMediaFileAdapter()?.notifyDataSetChanged()
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.compression_successful_images)
            )
        }
    }

    fun performCompressVideosAction(toCompress: List<MediaFileInfo>) {
        setupCompressVideosButton(toCompress, layoutInflater) {
            toDelete, toAdd ->
            refreshListAfterTrashCallback(toDelete, toAdd)
            getMediaFileAdapter()?.notifyDataSetChanged()
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.compression_successful_videos)
            )
        }
    }

    fun performShareAction(toShare: List<MediaFileInfo>) {
        var processed = false
        filesViewModel.getShareMediaFilesAdapter(toShare)
            .observe(viewLifecycleOwner) {
                shareAdapter ->
                if (shareAdapter == null) {
                    if (processed) {
                        requireActivity().showToastInCenter(
                            this.resources.getString(R.string.failed_to_share)
                        )
                    } else {
                        requireActivity()
                            .showToastInCenter(
                                resources
                                    .getString(R.string.please_wait)
                            )
                        processed = true
                    }
                } else {
                    showShareDialog(
                        requireActivity(), this.layoutInflater,
                        shareAdapter
                    )
                }
            }
    }

    fun performShuffleAction(context: Context, toShuffle: List<MediaFileInfo>) {
        if (toShuffle.isNotEmpty()) {
            val randomId = (toShuffle.indices).random()
            toShuffle[randomId].getContentUri(context)?.let {
                uri ->
                lifecycleScope.executeAsyncTask<Unit, List<Uri>>({}, {
                    toShuffle.mapNotNull {
                        it.getContentUri(context)
                    }
                }, {
                    AudioPlayerService.runService(
                        uri,
                        it,
                        context,
                        AudioPlayerService.ACTION_SHUFFLE
                    )
                }, {})
            }
        }
    }

    private fun refreshListAfterTrashCallback(
        toDelete: List<MediaFileInfo>,
        toAdd: List<MediaFileInfo>
    ) {
        if (getMediaFileAdapter()?.removeChecked() != true) {
            log.warn("Failed to update list after deletion")
            requireContext().showToastOnBottom(
                getString(
                    R.string
                        .failed_to_update_list_reopen
                )
            )
        }
        // delete deleted data from observables in fileviewmodel
        // for fileviewmodels, underlying list isn't passed in adapters beacause of size
        // and because that list is continuously iterated by dupe analysis
        deleteFromFileViewmodelLists(toDelete)
        addToFileViewmodelLists(toAdd)

        // reset interal storage stats so that we recalculate storage remaining
        filesViewModel.internalStorageStatsLiveData = null
        filesViewModel.trashBinFilesLiveData = null

        // deletion complete, no need to check analysis data to remove
        // as it will get deleted lazily while loading analysis lists
        if (hideActionBarOnClick()) {
            hideActionBar()
        }
    }
}
