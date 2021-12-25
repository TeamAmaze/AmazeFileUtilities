/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.content.Context
import com.amaze.fileutilities.utilis.safeLet
import java.util.*

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
                GROUP_PARENT -> {
                    compareGroupBy = groupByAsc * f1.listHeader[0].compareTo(f2.listHeader[0])
                }
                GROUP_DATE -> {
                    compareGroupBy = f1.listHeader.compareTo(f2.listHeader)
                    if (compareGroupBy != 0) {
                        compareGroupBy = groupByAsc * f1.date.compareTo(f2.date)
                    }
                }
                GROUP_NAME -> {
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
        const val GROUP_PARENT = 3
        const val GROUP_DATE = 4
        const val GROUP_NAME = 5

        fun generateMediaFileListHeadersAndSort(
            context: Context,
            mediaFileList: MutableList<MediaFileInfo>,
            sortingPreference: SortingPreference
        ) {
            mediaFileList.forEach {
                when (sortingPreference.groupBy) {
                    GROUP_PARENT -> {
                        it.listHeader = it.getParentName()
                    }
                    GROUP_DATE -> {
                        it.listHeader = it.getModificationDate(context)
                    }
                    GROUP_NAME -> {
                        it.listHeader = it.title[0].toString()
                    }
                }
            }
            Collections.sort(mediaFileList, MediaFileListSorter(sortingPreference))
        }
    }

    data class SortingPreference(
        val groupBy: Int,
        val sortBy: Int,
        val isGroupByAsc: Boolean,
        val isSortByAsc: Boolean
    )
}
