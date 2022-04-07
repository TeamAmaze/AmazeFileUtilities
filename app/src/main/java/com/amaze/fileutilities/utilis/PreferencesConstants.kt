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

import com.amaze.fileutilities.audio_player.AudioPlayerService
import java.util.*

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

        const val VAL_SEARCH_DUPLICATES_MEDIA_STORE = 0
        const val VAL_SEARCH_DUPLICATES_INTERNAL_SHALLOW = 1
        const val VAL_SEARCH_DUPLICATES_INTERNAL_DEEP = 2
        // increment this if add new default paths for analysis
        const val VAL_PATH_PREFS_MIGRATION = 1

        const val VAL_MIGRATION_FEATURE_ANALYSIS_MEME = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_BLUR = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_IMAGE_FEATURES = 1
        const val VAL_MIGRATION_FEATURE_ANALYSIS_LOW_LIGHT = 1

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
        const val DEFAULT_SUBTITLE_LANGUAGE_CODE = "eng"
        val DEFAULT_LICENSE_LAST_DAY_DONT_SHOW = Date()
    }
}
