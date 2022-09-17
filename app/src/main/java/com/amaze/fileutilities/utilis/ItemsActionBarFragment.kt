/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerService
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.analyse.ReviewImagesFragment
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.share.showShareDialog
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ItemsActionBarFragment : AbstractMediaFileInfoOperationsFragment() {

    private var log: Logger = LoggerFactory.getLogger(ItemsActionBarFragment::class.java)

    abstract fun hideActionBarOnClick(): Boolean
    abstract fun getMediaFileAdapter(): AbstractMediaFilesAdapter?
    abstract fun getMediaListType(): Int

    override fun getFilesViewModelObj(): FilesViewModel {
        return filesViewModel
    }

    override fun uninstallAppCallback(mediaFileInfo: MediaFileInfo) {
        refreshListAfterTrashCallback(listOf(mediaFileInfo))
    }

    private val filesViewModel: FilesViewModel by activityViewModels()

    private var optionsActionBar: View? = null

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
    }

    fun hideActionBar() {
        optionsActionBar = (activity as MainActivity)
            .invalidateSelectedActionBar(false, hideActionBarOnClick(), handleBackPressed())
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

    private fun getShareButton(): ImageView? {
        return optionsActionBar?.findViewById(R.id.shareFiles)
    }

    private fun getTrashButton(): ImageView? {
        return optionsActionBar?.findViewById(R.id.trashButton)
    }

    fun getLocateFileButton(): ImageView? {
        return optionsActionBar?.findViewById(R.id.locateFile)
    }

    private fun deleteFromFileViewmodelLists(toDelete: List<MediaFileInfo>) {
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
        if (!imagesToDelete.isNullOrEmpty()) {
            filesViewModel.usedImagesSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, imagesToDelete)
                }
            }
        }
        if (!videosToDelete.isNullOrEmpty()) {
            filesViewModel.usedVideosSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, videosToDelete)
                }
            }
        }
        if (!audioToDelete.isNullOrEmpty()) {
            filesViewModel.usedAudiosSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, audioToDelete)
                }
            }
        }
        if (!docsToDelete.isNullOrEmpty()) {
            filesViewModel.usedDocsSummaryTransformations().observe(viewLifecycleOwner) {
                if (it != null) {
                    filesViewModel.deleteMediaFilesFromList(it.second, docsToDelete)
                }
            }
        }
        if (!apksToDelete.isNullOrEmpty()) {
            // currently no way to distinguish whether user is deleting large apps or unused apps
            // so we clear both
            filesViewModel.unusedAppsLiveData = null
            filesViewModel.largeAppsLiveData = null
        }
    }

    fun setupCommonButtons() {
        getShareButton()?.setOnClickListener {
            getMediaFileAdapter()?.checkItemsList?.let {
                checkedItems ->
                val checkedMediaFiles = checkedItems.map { it.mediaFileInfo!! }
                if (!checkedMediaFiles.isNullOrEmpty()) {
                    performShareAction(checkedMediaFiles)
                    getMediaFileAdapter()?.uncheckChecked()
                } else {
                    requireContext().showToastOnBottom(getString(R.string.no_item_selected))
                }
            }
        }
        getTrashButton()?.setOnClickListener {
            getMediaFileAdapter()?.checkItemsList?.map { it.mediaFileInfo!! }?.let { toDelete ->
                performDeleteAction(toDelete)
            }
        }
        getLocateFileButton()?.setOnClickListener {
            getMediaFileAdapter()?.checkItemsList?.map { it.mediaFileInfo!! }?.let {
                openFile ->
                if (openFile.isNotEmpty()) {
                    openFile[0].startLocateFileAction(requireContext())
                    getMediaFileAdapter()?.uncheckChecked()
                }
            }
        }
    }

    fun performDeleteAction(toDelete: List<MediaFileInfo>) {
        setupDeleteButton(toDelete) {
            refreshListAfterTrashCallback(toDelete)
            getMediaFileAdapter()?.invalidateList(toDelete)
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
            toShuffle[0].getContentUri(context)?.let {
                uri ->
                lifecycleScope.executeAsyncTask<Unit, List<Uri>>({}, {
                    toShuffle.mapNotNull {
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

    private fun refreshListAfterTrashCallback(toDelete: List<MediaFileInfo>) {
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

        // reset interal storage stats so that we recalculate storage remaining
        filesViewModel.internalStorageStatsLiveData = null

        // deletion complete, no need to check analysis data to remove
        // as it will get deleted lazily while loading analysis lists
        requireContext().showToastOnBottom(
            resources
                .getString(R.string.successfully_deleted)
        )
        if (hideActionBarOnClick()) {
            hideActionBar()
        }
    }
}
