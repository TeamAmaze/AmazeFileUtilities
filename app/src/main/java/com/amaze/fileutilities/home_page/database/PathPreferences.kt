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

package com.amaze.fileutilities.home_page.database

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.amaze.fileutilities.utilis.PreferencesConstants
import java.lang.ref.WeakReference

/**
 * While fetching and processing, be sure to validate that file exists
 */
@Keep
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
        const val FEATURE_ANALYSIS_DOWNLOADS = 4
        const val FEATURE_ANALYSIS_RECORDING = 5
        const val FEATURE_ANALYSIS_SCREENSHOTS = 6
        const val FEATURE_ANALYSIS_TELEGRAM = 7
        const val FEATURE_ANALYSIS_LOW_LIGHT = 8
        const val FEATURE_ANALYSIS_WHATSAPP = 9
        const val FEATURE_ANALYSIS_LARGE_FILES = 10

        val ANALYSE_FEATURES_LIST = arrayListOf(
            FEATURE_ANALYSIS_MEME, FEATURE_ANALYSIS_BLUR,
            FEATURE_ANALYSIS_IMAGE_FEATURES, FEATURE_ANALYSIS_LOW_LIGHT
        )

        val MIGRATION_PREF_MAP = mapOf(
            Pair(FEATURE_ANALYSIS_MEME, PreferencesConstants.VAL_MIGRATION_FEATURE_ANALYSIS_MEME),
            Pair(FEATURE_ANALYSIS_BLUR, PreferencesConstants.VAL_MIGRATION_FEATURE_ANALYSIS_BLUR),
            Pair(
                FEATURE_ANALYSIS_IMAGE_FEATURES,
                PreferencesConstants.VAL_MIGRATION_FEATURE_ANALYSIS_IMAGE_FEATURES
            ),
            Pair(
                FEATURE_ANALYSIS_LOW_LIGHT,
                PreferencesConstants.VAL_MIGRATION_FEATURE_ANALYSIS_LOW_LIGHT
            )
        )

        fun getEnablePreferenceKey(feature: Int): String {
            return "${feature}_enabled"
        }

        fun getAnalysisMigrationPreferenceKey(feature: Int): String {
            return "${feature}_migration_version"
        }

        fun isEnabled(sharedPreferences: SharedPreferences, feature: Int): Boolean {
            return sharedPreferences.getBoolean(
                getEnablePreferenceKey(feature),
                PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
            )
        }

        fun deleteAnalysisData(
            pathPreferences: List<PathPreferences>,
            contextRef: WeakReference<Context>
        ) {
            contextRef.get()?.let {
                context ->
                val db = AppDatabase.getInstance(context)
                val analysisDao = db.analysisDao()
                val memesDao = db.memesAnalysisDao()
                val blurDao = db.blurAnalysisDao()
                val lowLightDao = db.lowLightAnalysisDao()
                pathPreferences.forEach {
                    when (it.feature) {
                        FEATURE_ANALYSIS_IMAGE_FEATURES -> {
                            analysisDao.deleteByPathContains(it.path)
                        }
                        FEATURE_ANALYSIS_BLUR -> {
                            blurDao.deleteByPathContains(it.path)
                        }
                        FEATURE_ANALYSIS_MEME -> {
                            memesDao.deleteByPathContains(it.path)
                        }
                        FEATURE_ANALYSIS_LOW_LIGHT -> {
                            lowLightDao.deleteByPathContains(it.path)
                        }
                    }
                }
            }
        }
    }
}
