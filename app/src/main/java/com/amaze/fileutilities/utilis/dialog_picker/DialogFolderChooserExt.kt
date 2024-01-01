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

@file:Suppress("unused")

package com.amaze.fileutilities.utilis.dialog_picker

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton.POSITIVE
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.files.R
import com.afollestad.materialdialogs.internal.list.DialogRecyclerView
import com.afollestad.materialdialogs.utils.MDUtil.maybeSetTextColor
import java.io.File

/** Gets the selected folder for the current folder chooser dialog. */
@CheckResult
fun MaterialDialog.selectedFolder(): File? {
    val list: DialogRecyclerView = getCustomView().findViewById(R.id.list)
    return (list.adapter as? FileChooserAdapter)?.selectedFile
}

/**
 * Shows a dialog that lets the user select a local folder.
 *
 * @param initialDirectory The directory that is listed initially, defaults to external storage.
 * @param filter A filter to apply when listing folders, defaults to only show non-hidden folders.
 * @param waitForPositiveButton When true, the callback isn't invoked until the user selects a
 *    folder and taps on the positive action button. Defaults to true if the dialog has buttons.
 * @param emptyTextRes A string resource displayed on the empty view shown when a directory is
 *    empty. Defaults to "This folder's empty!".
 * @param selection A callback invoked when a folder is selected.
 */
@SuppressLint("CheckResult")
fun MaterialDialog.folderChooser(
    context: Context,
    initialDirectory: File? = context.getExternalFilesDir(),
    filter: FileFilter = null,
    waitForPositiveButton: Boolean = true,
    emptyTextRes: Int = R.string.files_default_empty_text,
    allowFolderCreation: Boolean = false,
    @StringRes folderCreationLabel: Int? = null,
    selection: FileCallback = null
): MaterialDialog {
    var actualFilter: FileFilter = filter

    if (allowFolderCreation) {
        // we already have permissions at app startup
//        check(hasWriteStoragePermission()) {
//            "You must have the WRITE_EXTERNAL_STORAGE permission first."
//        }
        if (filter == null) {
            actualFilter = { !it.isHidden && it.canWrite() }
        }
    } else {
        // we already have permissions at app startup
//        check(hasWriteStoragePermission()) {
//            "You must have the READ_EXTERNAL_STORAGE permission first."
//        }
        if (filter == null) {
            actualFilter = { !it.isHidden && it.canRead() }
        }
    }

    check(initialDirectory != null) {
        "The initial directory is null."
    }

    customView(R.layout.md_file_chooser_base, noVerticalPadding = true)
    setActionButtonEnabled(POSITIVE, false)

    val customView = getCustomView()
    val list: DialogRecyclerView = customView.findViewById(R.id.list)
    val emptyText: TextView = customView.findViewById(R.id.empty_text)
    emptyText.setText(emptyTextRes)
    emptyText.maybeSetTextColor(windowContext, R.attr.md_color_content)

    list.attach(this)
    list.layoutManager = LinearLayoutManager(windowContext)

    val adapter = FileChooserAdapter(
        dialog = this,
        initialFolder = initialDirectory,
        waitForPositiveButton = waitForPositiveButton,
        emptyView = emptyText,
        onlyFolders = true,
        filter = actualFilter,
        allowFolderCreation = allowFolderCreation,
        folderCreationLabel = folderCreationLabel,
        callback = selection
    )
    list.adapter = adapter

    if (waitForPositiveButton && selection != null) {
        positiveButton {
            val selectedFile = adapter.selectedFile
            if (selectedFile != null) {
                selection.invoke(this, selectedFile)
            }
        }
    }

    return this
}
