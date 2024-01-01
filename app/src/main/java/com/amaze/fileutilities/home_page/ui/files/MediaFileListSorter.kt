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

package com.amaze.fileutilities.home_page.ui.files

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.safeLet
import java.util.Collections

class MediaFileListSorter(private val sortingPreference: SortingPreference) :
    Comparator<MediaFileInfo?> {
    private val groupByAsc: Int = if (sortingPreference.isGroupByAsc) 1 else -1
    private val sortByAsc: Int = if (sortingPreference.isSortByAsc) 1 else -1

    /**
     * Compares two elements and return negative, zero and positive integer if first argument is
     * less than, equal to or greater than second
     */
    override fun compare(file1: MediaFileInfo?, file2: MediaFileInfo?): Int {
        safeLet(file1, file2) {
            f1, f2 ->
            var compareGroupBy = 0
            when (sortingPreference.groupBy) {
                GROUP_NAME -> {
                    compareGroupBy = groupByAsc * f1.listHeader[0].compareTo(f2.listHeader[0])
                }
                GROUP_DATE -> {
                    compareGroupBy = f1.listHeader.compareTo(f2.listHeader)
                    if (compareGroupBy != 0) {
                        compareGroupBy = groupByAsc * f1.date.compareTo(f2.date)
                    }
                }
                GROUP_PARENT, GROUP_ALBUM, GROUP_ARTIST, GROUP_PLAYLISTS -> {
                    compareGroupBy = groupByAsc * f1.listHeader.compareTo(f2.listHeader)
                }
            }
            if (compareGroupBy != 0) {
                return compareGroupBy
            } else {
                return when (sortingPreference.sortBy) {
                    SORT_NAME -> {
                        sortByAsc * f1.title.compareTo(f2.title)
                    }
                    SORT_MODIF -> {
                        sortByAsc * f1.date.compareTo(f2.date)
                    }
                    SORT_SIZE -> {
                        sortByAsc * f1.longSize.compareTo(f2.longSize)
                    }
                    SORT_LENGTH -> {
                        if (f1.extraInfo != null && f2.extraInfo != null) {
                            if (f1.extraInfo?.videoMetaData != null &&
                                f2.extraInfo?.videoMetaData != null &&
                                f1.extraInfo?.videoMetaData?.duration != null &&
                                f2.extraInfo?.videoMetaData?.duration != null
                            ) {
                                sortByAsc * f1.extraInfo?.videoMetaData?.duration!!.compareTo(
                                    f2.extraInfo?.videoMetaData?.duration ?: 0
                                )
                            } else if (f1.extraInfo?.audioMetaData != null &&
                                f2.extraInfo?.audioMetaData != null &&
                                f1.extraInfo?.audioMetaData?.duration != null &&
                                f2.extraInfo?.audioMetaData?.duration != null
                            ) {
                                sortByAsc * f1.extraInfo?.audioMetaData?.duration!!.compareTo(
                                    f2.extraInfo?.audioMetaData?.duration ?: 0
                                )
                            } else {
                                0
                            }
                        } else {
                            0
                        }
                    }
                    else -> {
                        0
                    }
                }
            }
        }
        return 0
    }

    companion object {
        const val SORT_NAME = 0
        const val SORT_MODIF = 1
        const val SORT_SIZE = 2
        const val SORT_LENGTH = 3
        const val GROUP_PARENT = 3
        const val GROUP_DATE = 4
        const val GROUP_NAME = 5
        const val GROUP_ALBUM = 6
        const val GROUP_ARTIST = 7
        const val GROUP_PLAYLISTS = 8

        val GROUP_BY_MEDIA_TYPE_MAP = mapOf(
            Pair(
                MediaFileAdapter.MEDIA_TYPE_AUDIO,
                listOf(
                    GROUP_NAME, GROUP_DATE, GROUP_PARENT,
                    GROUP_ALBUM, GROUP_ARTIST, GROUP_PLAYLISTS
                )
            ),
            Pair(MediaFileAdapter.MEDIA_TYPE_VIDEO, listOf(GROUP_NAME, GROUP_DATE, GROUP_PARENT)),
            Pair(MediaFileAdapter.MEDIA_TYPE_IMAGES, listOf(GROUP_NAME, GROUP_DATE, GROUP_PARENT)),
            Pair(MediaFileAdapter.MEDIA_TYPE_DOCS, listOf(GROUP_NAME, GROUP_DATE, GROUP_PARENT))
        )

        val SORT_BY_MEDIA_TYPE_MAP = mapOf(
            Pair(
                MediaFileAdapter.MEDIA_TYPE_AUDIO,
                listOf(
                    SORT_NAME, SORT_SIZE, SORT_MODIF,
                    SORT_LENGTH
                )
            ),
            Pair(
                MediaFileAdapter.MEDIA_TYPE_VIDEO,
                listOf(
                    SORT_NAME, SORT_SIZE, SORT_MODIF,
                    SORT_LENGTH
                )
            ),
            Pair(MediaFileAdapter.MEDIA_TYPE_IMAGES, listOf(SORT_NAME, SORT_SIZE, SORT_MODIF)),
            Pair(MediaFileAdapter.MEDIA_TYPE_DOCS, listOf(SORT_NAME, SORT_SIZE, SORT_MODIF))
        )

        fun generateMediaFileListHeadersAndSort(
            context: Context,
            mediaFileList: MutableList<MediaFileInfo>,
            sortingPreference: SortingPreference
        ) {
            mediaFileList.forEach {
                when (sortingPreference.groupBy) {
                    GROUP_PARENT -> {
                        it.listHeader = it.getParentName().uppercase()
                    }
                    GROUP_DATE -> {
                        it.listHeader = it.getModificationDate(context)
                    }
                    GROUP_NAME -> {
                        it.listHeader = if (it.title.isNotBlank())
                            it.title[0].toString().uppercase() else "-"
                    }
                    GROUP_ALBUM -> {
                        it.listHeader = it.extraInfo?.audioMetaData?.albumName?.uppercase()
                            ?: context
                                .getString(R.string.unknown_artist)
                    }
                    GROUP_ARTIST -> {
                        it.listHeader = it.extraInfo?.audioMetaData?.artistName?.uppercase()
                            ?: context.getString(R.string.unknown_artist)
                    }
                    GROUP_PLAYLISTS -> {
                        it.listHeader = it.extraInfo?.audioMetaData?.playlist?.name?.uppercase()
                            ?: context.getString(R.string.unknown_artist)
                    }
                }
            }
            Collections.sort(mediaFileList, MediaFileListSorter(sortingPreference))
        }

        fun getGroupNameByType(groupType: Int, isAsc: Boolean, resources: Resources): String {
            val text = when (groupType) {
                GROUP_PARENT -> resources.getString(R.string.parent)
                GROUP_NAME -> resources.getString(R.string.name)
                GROUP_DATE -> resources.getString(R.string.date)
                GROUP_ALBUM -> resources.getString(R.string.album)
                GROUP_ARTIST -> resources.getString(R.string.artist)
                GROUP_PLAYLISTS -> resources.getString(R.string.playlists)
                else -> return ""
            }
            return text.plus(if (!isAsc) " ↑" else " ↓")
        }

        fun getSortNameByType(sortBy: Int, isAsc: Boolean, resources: Resources): String {
            val text = when (sortBy) {
                SORT_NAME -> resources.getString(R.string.name)
                SORT_SIZE -> resources.getString(R.string.size)
                SORT_MODIF -> resources.getString(R.string.date)
                SORT_LENGTH -> resources.getString(R.string.duration)
                else -> return ""
            }
            return text.plus(if (!isAsc) " ↑" else " ↓")
        }
    }

    data class SortingPreference(
        var groupBy: Int,
        var sortBy: Int,
        var isGroupByAsc: Boolean,
        var isSortByAsc: Boolean
    ) {
        companion object {
            fun newInstance(
                sharedPreferences: SharedPreferences,
                mediaListType: Int
            ): SortingPreference {
                val groupBy = sharedPreferences.getInt(
                    getGroupByKey(mediaListType),
                    PreferencesConstants.DEFAULT_MEDIA_LIST_GROUP_BY
                )
                val sortBy = sharedPreferences.getInt(
                    getSortByKey(mediaListType),
                    PreferencesConstants.DEFAULT_MEDIA_LIST_SORT_BY
                )
                val isGroupByAsc = sharedPreferences
                    .getBoolean(
                        getIsGroupByAscKey(mediaListType),
                        PreferencesConstants.DEFAULT_MEDIA_LIST_GROUP_BY_ASC
                    )
                val isSortByAsc = sharedPreferences
                    .getBoolean(
                        getIsSortByAscKey(mediaListType),
                        PreferencesConstants.DEFAULT_MEDIA_LIST_SORT_BY_ASC
                    )
                return SortingPreference(groupBy, sortBy, isGroupByAsc, isSortByAsc)
            }

            /**
             * Preference key for media list type from MediaFileAdapter
             */
            fun getGroupByKey(mediaListType: Int): String {
                return "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_GROUP_BY}"
            }

            fun getSortByKey(mediaListType: Int): String {
                return "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_SORT_BY}"
            }
            fun getIsGroupByAscKey(mediaListType: Int): String {
                return "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_GROUP_BY_IS_ASC}"
            }
            fun getIsSortByAscKey(mediaListType: Int): String {
                return "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_SORT_BY_IS_ASC}"
            }
        }
    }
}
