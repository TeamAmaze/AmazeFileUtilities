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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import com.amaze.fileutilities.CastActivity
import com.amaze.fileutilities.R
import com.amaze.fileutilities.audio_player.AudioPlayerDialogActivity
import com.amaze.fileutilities.image_viewer.ImageViewerDialogActivity
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.showToastOnBottom
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
        return if (exists()) {
            File(path).parentFile?.name ?: UNKNOWN
        } else {
            UNKNOWN
        }
    }

    fun getParentFile(): File? {
        return if (exists()) {
            File(path).parentFile
        } else {
            null
        }
    }

    fun getFormattedSize(context: Context): String {
        return FileUtils.formatStorageLength(context, longSize)
    }

    fun delete(): Boolean {
        if (!exists()) {
            return true
        }
        return File(path).delete()
    }

    fun getContentUri(context: Context): Uri? {
        return if (exists()) {
            FileProvider.getUriForFile(context, context.packageName, File(path))
        } else {
            null
        }
    }

    fun triggerMediaFileInfoAction(contextRef: WeakReference<Context>) {
        if (!exists()) {
            contextRef.get()?.let {
                context ->
                context.showToastOnBottom(context.resources.getString(R.string.file_not_found))
                return
            }
        }
        contextRef.get()?.let {
            context ->
            val castActivity = (context as CastActivity)
            when (this.extraInfo?.mediaType) {
                MEDIA_TYPE_IMAGE -> {
                    castActivity.showCastFileDialog(
                        this,
                        MediaFileAdapter.MEDIA_TYPE_IMAGES
                    ) {
                        startImageViewer(this, context)
                    }
                }
                MEDIA_TYPE_VIDEO -> {
                    castActivity.showCastFileDialog(
                        this,
                        MediaFileAdapter.MEDIA_TYPE_VIDEO
                    ) {
                        startVideoViewer(this, context)
                    }
                }
                MEDIA_TYPE_AUDIO -> {
                    castActivity.showCastFileDialog(
                        this,
                        MediaFileAdapter.MEDIA_TYPE_AUDIO
                    ) {
                        startAudioViewer(this, context)
                    }
                }
                MEDIA_TYPE_DOCUMENT, MEDIA_TYPE_UNKNOWN -> {
                    castActivity.showCastFileDialog(
                        this,
                        MediaFileAdapter.MEDIA_TYPE_DOCS
                    ) {
                        startExternalViewAction(this, context)
                    }
                }
                else -> {
                    startExternalViewAction(this, context)
                }
            }
        }
    }

    fun exists(): Boolean {
        val file = File(this.path)
        return file.exists()
    }

    fun startLocateFileAction(context: Context) {
        File(this.path).let {
            file ->
            if (file.parentFile != null) {
                val intent = Intent()
                intent.setDataAndType(
                    FileProvider.getUriForFile(context, context.packageName, file.parentFile),
                    "resource/folder"
                )
                intent.putExtra("com.amaze.fileutilities.AFM_LOCATE_FILE_NAME", file.name)
                intent.action = Intent.ACTION_VIEW
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    context.showToastOnBottom(context.resources.getString(R.string.no_app_found))
                }
            } else {
                context.showToastOnBottom(context.resources.getString(R.string.operation_failed))
            }
        }
    }

    private fun startExternalViewAction(mediaFileInfo: MediaFileInfo, context: Context) {
        val intent = Intent()
        intent.data = mediaFileInfo.getContentUri(context)
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.showToastOnBottom(context.resources.getString(R.string.no_app_found))
        }
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
        val duration: Long?,
        val albumId: Long?,
        val albumArt: Bitmap?
    )
    data class VideoMetaData(val duration: Long?, val width: Int?, val height: Int?)
    data class ImageMetaData(val width: Int?, val height: Int?)
}
