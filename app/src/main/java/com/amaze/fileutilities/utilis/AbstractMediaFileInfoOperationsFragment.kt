/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.text.format.Formatter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.Utils.Companion.showProcessingDialog

abstract class AbstractMediaFileInfoOperationsFragment : Fragment() {

    abstract fun getFilesViewModelObj(): FilesViewModel
    abstract fun uninstallAppCallback(mediaFileInfo: MediaFileInfo)

    private val uninstallAppFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_REMOVED)
//        addAction(Intent.ACTION_PACKAGE_REPLACED)
//        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addDataScheme("package")
    }

    private val uninstallAppReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val actionStr = intent.action
            if (Intent.ACTION_PACKAGE_REMOVED == actionStr) {
                val data: Uri? = intent.data
                val pkgName = data?.encodedSchemeSpecificPart ?: ""
                // dummy object as we're just refreshing the whole list instead of removing an element from apps list
                uninstallAppCallback(
                    MediaFileInfo(
                        pkgName, pkgName, 0, 0,
                        false, "",
                        MediaFileInfo.ExtraInfo(
                            MediaFileInfo.MEDIA_TYPE_APK, null,
                            null, null, null
                        )
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(uninstallAppReceiver, uninstallAppFilter)
    }

    override fun onPause() {
        requireActivity().unregisterReceiver(uninstallAppReceiver)
        super.onPause()
    }

    /**
     * setup delete click
     * @param toDelete to delete media files
     * @param deletedCallback callback once deletion finishes
     */
    fun setupDeleteButton(
        toDelete: List<MediaFileInfo>,
        deletedCallback: () -> Unit
    ) {
        if (toDelete.isEmpty()) {
            requireContext().showToastOnBottom(getString(R.string.no_item_selected))
            return
        }
        val progressDialogBuilder = requireContext()
            .showProcessingDialog(layoutInflater, "")
        val summaryDialogBuilder = Utils.buildDeleteSummaryDialog(requireContext()) {
            if (toDelete[0].extraInfo?.mediaType == MediaFileInfo.MEDIA_TYPE_APK) {
                toDelete.forEachIndexed { index, mediaFileInfo ->
                    Utils.uninstallPackage(
                        mediaFileInfo.extraInfo!!.apkMetaData!!.packageName,
                        requireActivity()
                    )
                }
            } else {
                val progressDialog = progressDialogBuilder.create()
                progressDialog.show()
                getFilesViewModelObj().deleteMediaFiles(toDelete).observe(viewLifecycleOwner) {
                    progressDialog.findViewById<TextView>(R.id.please_wait_text)?.text =
                        resources.getString(R.string.deleted_progress)
                            .format(it.first, toDelete.size)
                    if (it.second == toDelete.size) {
                        deletedCallback.invoke()
                        progressDialog.dismiss()
                    }
                }
            }
        }
        val summaryDialog = summaryDialogBuilder.create()
        summaryDialog.show()
        getFilesViewModelObj().getMediaFileListSize(toDelete).observe(viewLifecycleOwner) {
            sizeRaw ->
            val size = Formatter.formatFileSize(requireContext(), sizeRaw)
            summaryDialog.setMessage(
                resources.getString(R.string.delete_files_message)
                    .format(toDelete.size, size)
            )
        }
    }
}
