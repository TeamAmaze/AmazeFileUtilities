/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
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
