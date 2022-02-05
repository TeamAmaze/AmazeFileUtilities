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

/**
 * While fetching and processing, be sure to validate that file exists
 */
@Entity(indices = [Index(value = ["path", "feature"], unique = true)])
data class PathPreferences(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "feature") val feature: Int,
    @ColumnInfo(name = "excludes") val excludes: Boolean,
) {
    constructor(path: String, feature: Int, excludes: Boolean = false) :
        this(0, path, feature, excludes)

    companion object {
        const val FEATURE_AUDIO_PLAYER = 0
        const val FEATURE_ANALYSIS_MEME = 1
        const val FEATURE_ANALYSIS_BLUR = 2
        const val FEATURE_ANALYSIS_IMAGE_FEATURES = 3
        const val FEATURE_ANALYSIS_DOWNLOADS = 8
        const val FEATURE_ANALYSIS_RECORDING = 9
        const val FEATURE_ANALYSIS_SCREENSHOTS = 10
        const val FEATURE_ANALYSIS_TELEGRAM = 10

        fun getSharedPreferenceKey(feature: Int): String {
            return "${feature}_enabled"
        }
    }
}
