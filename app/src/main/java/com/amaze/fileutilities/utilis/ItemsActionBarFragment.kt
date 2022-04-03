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

import android.text.format.Formatter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog
import com.amaze.fileutilities.utilis.share.showShareDialog

abstract class ItemsActionBarFragment : Fragment() {

    abstract fun hideActionBarOnClick(): Boolean
    abstract fun getMediaFileAdapter(): AbstractMediaFilesAdapter?

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
                // do nothing when we want to go back on back pressed
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

    fun setupCommonButtons() {
        getShareButton()?.setOnClickListener {
            var processed = false
            getMediaFileAdapter()?.checkItemsList?.let {
                checkedItems ->
                val checkedMediaFiles = checkedItems.filter { it.mediaFileInfo != null }
                    .map { it.mediaFileInfo!! }
                filesViewModel.getShareMediaFilesAdapter(checkedMediaFiles)
                    .observe(viewLifecycleOwner) {
                        shareAdapter ->
                        if (shareAdapter == null) {
                            if (processed) {
                                requireActivity().showToastInCenter(
                                    this.resources
                                        .getString(R.string.failed_to_share)
                                )
                            } else {
                                requireActivity()
                                    .showToastInCenter(resources.getString(R.string.please_wait))
                                processed = true
                            }
                        } else {
                            showShareDialog(requireActivity(), this.layoutInflater, shareAdapter)
                        }
                    }
            }
        }
        getTrashButton()?.setOnClickListener {
            getMediaFileAdapter()?.checkItemsList?.filter {
                it.mediaFileInfo != null
            }?.map { it.mediaFileInfo!! }?.let {
                toDelete ->
                val progressDialogBuilder = requireContext()
                    .showProcessingDialog(layoutInflater, "")
                val progressDialog = progressDialogBuilder.create()
                val summaryDialogBuilder = Utils.buildDeleteSummaryDialog(requireContext()) {
                    progressDialog.show()
                    filesViewModel.deleteMediaFiles(toDelete).observe(viewLifecycleOwner) {
                        progressDialog.findViewById<TextView>(R.id.please_wait_text)?.text =
                            resources.getString(R.string.deleted_progress)
                                .format(it.first, toDelete.size)
                        if (it.second == toDelete.size) {
                            if (getMediaFileAdapter()?.removeChecked() != true) {
                                log.warn("Failed to update list after deletion")
                                requireContext().showToastOnBottom(
                                    getString(
                                        R.string
                                            .failed_to_update_list_reopen
                                    )
                                )
                            }
                            // deletion complete, no need to check analysis data to remove
                            // as it will get deleted lazily while loading analysis lists
                            requireContext().showToastOnBottom(
                                resources
                                    .getString(R.string.successfully_deleted)
                            )
                            if (hideActionBarOnClick()) {
                                hideActionBar()
                            }
                            progressDialog.dismiss()
                        }
                    }
                }
                val summaryDialog = summaryDialogBuilder.create()
                summaryDialog.show()
                filesViewModel.getMediaFileListSize(toDelete).observe(viewLifecycleOwner) {
                    sizeRaw ->
                    val size = Formatter.formatFileSize(requireContext(), sizeRaw)
                    summaryDialog.setMessage(
                        resources
                            .getString(R.string.delete_files_message).format(toDelete.size, size)
                    )
                }
            }
        }
    }
}
