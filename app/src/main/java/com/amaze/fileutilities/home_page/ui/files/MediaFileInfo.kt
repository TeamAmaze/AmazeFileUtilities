package com.amaze.fileutilities.home_page.ui.files

import android.content.Context
import android.net.Uri
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
) {

    companion object {
        private const val DATE_TIME_FORMAT = "%s | %s"

        fun fromFile(file: File): MediaFileInfo {
            return MediaFileInfo(file.name, file.path, file.lastModified(), file.length())
        }
    }

    fun getModificationDate(context: Context): String {
        return String.format(
            DATE_TIME_FORMAT,
            DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_MONTH),
            DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_TIME)
        )
    }

    fun getFormattedSize(context: Context): String {
        return FileUtils.formatStorageLength(context, longSize)
    }

    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName, file)
    }
}
