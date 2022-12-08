/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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

package com.amaze.fileutilities.home_page.database

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * While fetching and processing, be sure to validate that file exists
 */
@Keep
@Entity(indices = [Index(value = ["sha256_checksum"], unique = true)])
data class InternalStorageAnalysis(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "sha256_checksum") val checksum: String,
    @ColumnInfo(name = "files_path") val files: List<String>,
    @ColumnInfo(name = "is_empty") val isEmpty: Boolean,
    @ColumnInfo(name = "is_junk") val isJunk: Boolean,
    @ColumnInfo(name = "is_directory") val isDirectory: Boolean,
    @ColumnInfo(name = "is_mediastore") val isMediaStore: Boolean,
    @ColumnInfo(name = "depth") val depth: Int
) {
    constructor(
        checksum: String,
        filesPath: List<String>,
        isEmpty: Boolean,
        isJunk: Boolean,
        isDirectory: Boolean,
        isMediaStore: Boolean,
        depth: Int
    ) :
        this(0, checksum, filesPath, isEmpty, isJunk, isDirectory, isMediaStore, depth)
}
