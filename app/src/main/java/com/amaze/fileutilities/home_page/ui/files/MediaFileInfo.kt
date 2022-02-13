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
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.image_viewer.ImageViewerDialogActivity
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.video_player.VideoPlayerDialogActivity
import java.io.File
import java.lang.ref.WeakReference

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

    fun triggerMediaFileInfoAction(contextRef: WeakReference<Context>) {
        contextRef.get()?.let {
            context ->
            when (this.extraInfo?.mediaType) {
                MEDIA_TYPE_IMAGE -> {
                    startImageViewer(this, context)
                }
                MEDIA_TYPE_VIDEO -> {
                    startVideoViewer(this, context)
                }
                MEDIA_TYPE_AUDIO -> {
                    startAudioViewer(this, context)
                }
                MEDIA_TYPE_DOCUMENT, MEDIA_TYPE_UNKNOWN -> {
                    startExternalViewAction(this, context)
                }
                else -> {
                    startExternalViewAction(this, context)
                }
            }
        }
    }

    private fun startExternalViewAction(mediaFileInfo: MediaFileInfo, context: Context) {
        val intent = Intent()
        intent.data = mediaFileInfo.getContentUri(context)
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun startImageViewer(mediaFileInfo: MediaFileInfo, context: Context) {
        val intent = Intent(context, ImageViewerDialogActivity::class.java)
        intent.data = mediaFileInfo.getContentUri(context)
        context.startActivity(intent)
    }

    private fun startAudioViewer(mediaFileInfo: MediaFileInfo, context: Context) {
        val intent = Intent(context, AudioPlayerDialogActivity::class.java)
        intent.data = mediaFileInfo.getContentUri(context)
        context.startActivity(intent)
    }

    private fun startVideoViewer(mediaFileInfo: MediaFileInfo, context: Context) {
        val intent = Intent(context, VideoPlayerDialogActivity::class.java)
        intent.data = mediaFileInfo.getContentUri(context)
        context.startActivity(intent)
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
