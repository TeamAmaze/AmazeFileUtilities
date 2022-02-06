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

        const val VAL_SEARCH_DUPLICATES_MEDIA_STORE = 0
        const val VAL_SEARCH_DUPLICATES_INTERNAL_SHALLOW = 1
        const val VAL_SEARCH_DUPLICATES_INTERNAL_DEEP = 2
        const val VAL_PATH_PREFS_MIGRATION = 1

        const val DEFAULT_MEDIA_LIST_TYPE = true
        const val DEFAULT_MEDIA_LIST_GROUP_BY = 5
        const val DEFAULT_MEDIA_LIST_SORT_BY = 0
        const val DEFAULT_MEDIA_LIST_GROUP_BY_ASC = true
        const val DEFAULT_MEDIA_LIST_SORT_BY_ASC = true
        const val DEFAULT_SEARCH_DUPLICATES_IN = VAL_SEARCH_DUPLICATES_MEDIA_STORE
        const val DEFAULT_DUPLICATE_SEARCH_DEPTH_INCL = 2
        const val DEFAULT_PATH_PREFS_INITIALIZED = 0
        const val DEFAULT_ENABLED_ANALYSIS = true
    }
}
