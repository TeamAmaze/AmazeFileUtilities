package com.amaze.fileutilities.home_page.ui.files

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.core.content.FileProvider
import com.amaze.fileutilities.utilis.FileUtils
import java.io.File

data class MediaFileInfo(
//    val iconData: IconDataParcelable? = null
    val title: String,
    val path: String,
    val date: Long = 0,
    val longSize: Long = 0,
    val isDirectory: Boolean = false,
    var listHeader: String = "",
    val extraInfo: ExtraInfo? = null
) {

    companion object {
        private const val DATE_TIME_FORMAT = "%s %s, %s"
        private const val UNKNOWN = "UNKNOWN"
        const val MEDIA_TYPE_UNKNOWN = 1000
        const val MEDIA_TYPE_IMAGE = 1001
        const val MEDIA_TYPE_AUDIO = 1002
        const val MEDIA_TYPE_VIDEO = 1003
        const val MEDIA_TYPE_DOCUMENT = 1004

        fun fromFile(file: File, extraInfo: ExtraInfo): MediaFileInfo {
            return MediaFileInfo(file.name, file.path, file.lastModified(), file.length(), extraInfo = extraInfo)
        }
    }

    fun getModificationDate(context: Context): String {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_ALL)
    }

    fun getParentName(): String {
        return File(path).parentFile?.name?: UNKNOWN
    }

    fun getFormattedSize(context: Context): String {
        return FileUtils.formatStorageLength(context, longSize)
    }

    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName, file)
    }

    @JvmInline
    value class MediaType(val mediaType: Int)

    data class ExtraInfo(val mediaType: MediaType, val audioMetaData: AudioMetaData?,
                         val videoMetaData: VideoMetaData?, val imageMetaData: ImageMetaData?)

    data class AudioMetaData(val albumName: String?, val artistName: String?, val duration: Long?)
    data class VideoMetaData(val duration: Long?, val width: Int?, val height: Int?)
    data class ImageMetaData(val width: Int?, val height: Int?)
}
