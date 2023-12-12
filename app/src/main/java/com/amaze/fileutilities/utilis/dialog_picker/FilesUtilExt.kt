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

@file:Suppress("SpellCheckingInspection")

package com.amaze.fileutilities.utilis.dialog_picker

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import java.io.File

internal fun File.hasParent(
    context: Context,
    writeable: Boolean,
    filter: FileFilter
) = betterParent(context, writeable, filter) != null

internal fun File.isExternalStorage(context: Context) =
    absolutePath == context.getExternalFilesDir()?.absolutePath

internal fun File.isRoot() = absolutePath == "/"

internal fun File.betterParent(
    context: Context,
    writeable: Boolean,
    filter: FileFilter
): File? {
    val parentToUse = (
        if (isExternalStorage(context)) {
            // Emulated external storage's parent is empty so jump over it
            context.getExternalFilesDir()?.parentFile?.parentFile
        } else {
            parentFile
        }
        ) ?: return null

    if ((writeable && !parentToUse.canWrite()) || !parentToUse.canRead()) {
        // We can't access this folder
        return null
    }

    val folderContent =
        parentToUse.listFiles()?.filter { filter?.invoke(it) ?: true } ?: emptyList()
    if (folderContent.isEmpty()) {
        // There is nothing in this folder most likely because we can't access files inside of it.
        // We don't want to get stuck here.
        return null
    }

    return parentToUse
}

internal fun File.jumpOverEmulated(context: Context): File {
    val externalFileDir = context.getExternalFilesDir()
    externalFileDir?.parentFile?.let { externalParentFile ->
        if (absolutePath == externalParentFile.absolutePath) {
            return externalFileDir
        }
    }
    return this
}

internal fun File.friendlyName(context: Context) = when {
    isExternalStorage(context) -> "External Storage"
    isRoot() -> "Root"
    else -> name
}

internal fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
        PackageManager.PERMISSION_GRANTED
}

internal fun MaterialDialog.hasReadStoragePermission(): Boolean {
    return windowContext.hasPermission(permission.READ_EXTERNAL_STORAGE)
}

internal fun MaterialDialog.hasWriteStoragePermission(): Boolean {
    return windowContext.hasPermission(permission.WRITE_EXTERNAL_STORAGE)
}
