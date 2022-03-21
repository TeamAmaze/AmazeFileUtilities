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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["file_path"], unique = true)])
data class VideoPlayerState(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "playback_position") val playbackPosition: Long,
) {
    constructor(
        filePath: String,
        playbackPosition: Long
    ) : this(0, filePath, playbackPosition)
}
