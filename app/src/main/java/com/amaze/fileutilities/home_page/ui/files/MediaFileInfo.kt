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
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import com.amaze.fileutilities.utilis.FileUtils
import java.io.File

data class MediaFileInfo(
    val title: String,
    val path: String,
    val date: Long = 0,
    val longSize: Long = 0,
    var isDirectory: Boolean = false,
    var listHeader: String = "",
    val extraInfo: ExtraInfo? = null
) {

    companion object {
//        private const val DATE_TIME_FORMAT = "%s %s, %s"
        private const val UNKNOWN = "UNKNOWN"
        const val MEDIA_TYPE_UNKNOWN = 1000
        const val MEDIA_TYPE_IMAGE = 1001
        const val MEDIA_TYPE_AUDIO = 1002
        const val MEDIA_TYPE_VIDEO = 1003
        const val MEDIA_TYPE_DOCUMENT = 1004

        fun fromFile(file: File, extraInfo: ExtraInfo): MediaFileInfo {
            return MediaFileInfo(
                file.name, file.path, file.lastModified(), file.length(),
                extraInfo = extraInfo
            )
        }
    }

    fun getModificationDate(context: Context): String {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_ALL)
    }

    fun getParentName(): String {
        return File(path).parentFile?.name ?: UNKNOWN
    }

    fun getFormattedSize(context: Context): String {
        return FileUtils.formatStorageLength(context, longSize)
    }

    fun getContentUri(context: Context): Uri {
        return FileProvider.getUriForFile(context, context.packageName, File(path))
    }

    data class ExtraInfo(
        val mediaType: Int,
        val audioMetaData: AudioMetaData?,
        val videoMetaData: VideoMetaData?,
        val imageMetaData: ImageMetaData?
    )

    data class AudioMetaData(
        val albumName: String?,
        val artistName: String?,
        val duration: Long?
    )
    data class VideoMetaData(val duration: Long?, val width: Int?, val height: Int?)
    data class ImageMetaData(val width: Int?, val height: Int?)
}
