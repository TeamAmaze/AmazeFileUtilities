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

package com.amaze.fileutilities.home_page.ui

import androidx.lifecycle.LifecycleOwner
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo

interface AggregatedMediaFileInfoObserver {

    fun getFilesModel(): FilesViewModel

    fun lifeCycleOwner(): LifecycleOwner

    fun observeMediaInfoLists(
        callback: (
            isLoading: Boolean,
            aggregatedFiles: AggregatedMediaFiles?
        ) -> Unit
    ) {
        getFilesModel().usedImagesSummaryTransformations()
            .observe(
                lifeCycleOwner()
            ) { imagesPair ->
                imagesPairObserver(callback, imagesPair)
            }
    }

    private fun imagesPairObserver(
        callback: (
            isLoading: Boolean,
            aggregatedFiles: AggregatedMediaFiles?
        ) -> Unit,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>?
    ) {
        if (imagesPair?.second != null) {
//            showLoadingViews(false)
            getFilesModel().usedVideosSummaryTransformations()
                .observe(
                    lifeCycleOwner()
                ) { videosPair ->
                    videosPairObserver(callback, videosPair, imagesPair)
                }
        } else {
            callback.invoke(true, null)
        }
    }

    private fun videosPairObserver(
        callback: (
            isLoading: Boolean,
            aggregatedFiles: AggregatedMediaFiles?
        ) -> Unit,
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>?,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>
    ) {
        if (videosPair?.second != null) {
//            showLoadingViews(false)
            getFilesModel().usedAudiosSummaryTransformations()
                .observe(
                    lifeCycleOwner()
                ) { audiosPair ->
                    audiosPairObserver(
                        callback,
                        audiosPair, videosPair, imagesPair
                    )
                }
        } else {
            callback.invoke(true, null)
        }
    }

    private fun audiosPairObserver(
        callback: (
            isLoading: Boolean,
            aggregatedFiles: AggregatedMediaFiles?
        ) -> Unit,
        audiosPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>?,
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>
    ) {
        if (audiosPair?.second != null) {
//            showLoadingViews(false)
            getFilesModel().usedDocsSummaryTransformations()
                .observe(
                    lifeCycleOwner()
                ) { docsPair ->
                    docsPairObserver(
                        callback,
                        docsPair, audiosPair, videosPair, imagesPair
                    )
                }
        } else {
            callback.invoke(true, null)
        }
    }

    private fun docsPairObserver(
        callback: (
            isLoading: Boolean,
            aggregatedFiles: AggregatedMediaFiles?
        ) -> Unit,
        docsPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>?,
        audiosPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>,
        videosPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>,
        imagesPair:
            Pair<FilesViewModel.StorageSummary,
                List<MediaFileInfo>>
    ) {
        if (docsPair?.second != null) {
            /*showLoadingViews(false)
            showEmptyViews()
            searchQueryInput.run {
                imagesMediaFilesList = imagesPair.second
                videosMediaFilesList = videosPair.second
                audiosMediaFilesList = audiosPair.second
                docsMediaFilesList = docsPair.second
            }*/
            callback.invoke(
                false,
                AggregatedMediaFiles(
                    imagesPair.second,
                    videosPair.second, audiosPair.second, docsPair.second
                )
            )
        } else {
            callback.invoke(true, null)
//            showLoadingViews(true)
        }
    }

    data class AggregatedMediaFiles(
        var imagesMediaFilesList: List<MediaFileInfo>? = null,
        var videosMediaFilesList: List<MediaFileInfo>? = null,
        var audiosMediaFilesList: List<MediaFileInfo>? = null,
        var docsMediaFilesList: List<MediaFileInfo>? = null
    ) {
        fun mediaListsLoaded(): Boolean {
            return imagesMediaFilesList != null && videosMediaFilesList != null &&
                audiosMediaFilesList != null && docsMediaFilesList != null
        }
    }
}
