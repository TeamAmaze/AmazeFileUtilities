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

package com.amaze.fileutilities.utilis

import com.amaze.fileutilities.audio_player.AudioPlayerService

class PreferencesConstants {

    companion object {
        const val PREFERENCE_FILE = "pref_file"
        const val KEY_MEDIA_LIST_TYPE = "media_list_is_list"
        const val KEY_MEDIA_LIST_GROUP_BY = "media_list_group_by"
        const val KEY_MEDIA_LIST_SORT_BY = "media_list_sort_by"
        const val KEY_MEDIA_LIST_GROUP_BY_IS_ASC = "media_list_group_by_is_asc"
        const val KEY_MEDIA_LIST_SORT_BY_IS_ASC = "media_list_sort_by_is_asc"
        const val KEY_SEARCH_DUPLICATES_IN = "search_duplicates_in"
        const val KEY_PATH_PREFS_MIGRATION = "path_prefs_migration"
        const val KEY_AUDIO_PLAYER_SHUFFLE = "audio_player_shuffle"
        const val KEY_AUDIO_PLAYER_REPEAT_MODE = "audio_player_repeat_mode"
        const val KEY_ENABLE_WAVEFORM = "pref_enable_waveform"
        const val KEY_SUBTITLE_LANGUAGE_CODE = "subtitle_language_code"
        const val KEY_UPDATE_APP_LAST_SHOWN_DATE = "update_app_last_show_date"
        const val KEY_DEVICE_UNIQUE_ID = "device_unique_id"
        const val KEY_NOT_CONNECTED_TRIAL_COUNT = "not_connected_trial"
        const val KEY_NOT_CONNECTED_SUBSCRIBED_COUNT = "not_connected_subscribed"
        const val KEY_RATE_APP_AUTOMATED = "rate_app_automated"
        const val KEY_INSTALL_DATE = "install_date"
        const val KEY_GRID_VIEW_COLUMN_COUNT = "grid_view_column_count"
        const val KEY_PLAYBACK_SEMITONES = "playback_semitones"
        const val KEY_ENABLE_AUDIO_PALETTE = "pref_audio_enable_palette"
        const val KEY_ENABLE_IMAGE_PALETTE = "pref_image_enable_palette"
        const val KEY_UNUSED_APPS_DAYS = "unused_apps_days"
        const val KEY_MOST_USED_APPS_DAYS = "most_used_apps_days"
        const val KEY_LEAST_USED_APPS_DAYS = "most_used_apps_days"

        const val VAL_SEARCH_DUPLICATES_MEDIA_STORE = 0
        const val VAL_SEARCH_DUPLICATES_INTERNAL_SHALLOW = 1
        const val VAL_SEARCH_DUPLICATES_INTERNAL_DEEP = 2
        // increment this if add new default paths for analysis
        const val VAL_PATH_PREFS_MIGRATION = 2

        const val VAL_MIGRATION_FEATURE_ANALYSIS_MEME = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_BLUR = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_IMAGE_FEATURES = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_LOW_LIGHT = 1
        const val VAL_THRES_NOT_CONNECTED_TRIAL = 10
        const val VAL_THRES_NOT_CONNECTED_SUBSCRIBED = 100

        const val DEFAULT_MEDIA_LIST_TYPE = true
        const val DEFAULT_MEDIA_LIST_GROUP_BY = 5
        const val DEFAULT_MEDIA_LIST_SORT_BY = 0
        const val DEFAULT_MEDIA_LIST_GROUP_BY_ASC = true
        const val DEFAULT_MEDIA_LIST_SORT_BY_ASC = true
        const val DEFAULT_SEARCH_DUPLICATES_IN = VAL_SEARCH_DUPLICATES_MEDIA_STORE
        const val DEFAULT_DUPLICATE_SEARCH_DEPTH_INCL = 2
        const val DEFAULT_PATH_PREFS_INITIALIZED = 0
        const val DEFAULT_ANALYSIS_MIGRATION_INITIALIZED = 1
        const val DEFAULT_ENABLED_ANALYSIS = true
        const val DEFAULT_AUDIO_PLAYER_SHUFFLE = false
        const val DEFAULT_AUDIO_PLAYER_REPEAT_MODE = AudioPlayerService.REPEAT_NONE
        const val DEFAULT_AUDIO_PLAYER_WAVEFORM = true
        const val DEFAULT_SUBTITLE_LANGUAGE_CODE = "en"
        const val DEFAULT_GRID_VIEW_COLUMN_COUNT = 3
        const val DEFAULT_PLAYBACK_SEMITONES = 0f
        const val DEFAULT_PALETTE_EXTRACT = true
        const val DEFAULT_UNUSED_APPS_DAYS = 30
        const val DEFAULT_MOST_USED_APPS_DAYS = 7
        const val DEFAULT_LEAST_USED_APPS_DAYS = 7
    }
}
