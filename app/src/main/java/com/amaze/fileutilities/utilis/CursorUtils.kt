package com.amaze.fileutilities.utilis

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class CursorUtils {

    companion object {
        const val BASE_SELECTION_AUDIO =
            MediaStore.Audio.AudioColumns.IS_MUSIC + "=1" + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''"

        const val BASE_SELECTION_IMAGES = MediaStore.Images.ImageColumns.TITLE + " != ''"

        const val BASE_SELECTION_VIDEOS = MediaStore.Video.VideoColumns.TITLE + " != ''"

        val BLACKLIST_PATHS_AUDIO = arrayListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).canonicalPath)

        suspend fun listImages(context: Context, callback: SummaryCallbackAlias): ArrayList<MediaFileInfo> {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            return listMediaCommon(context, callback,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, BASE_SELECTION_IMAGES, null)
        }

        suspend fun listVideos(context: Context, callback: SummaryCallbackAlias): ArrayList<MediaFileInfo> {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            return listMediaCommon(context, callback,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, BASE_SELECTION_VIDEOS, null)
        }

        suspend fun listAudio(context: Context, callback: SummaryCallbackAlias): ArrayList<MediaFileInfo> {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            return listMediaCommon(context, callback,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null, BASE_SELECTION_AUDIO, BLACKLIST_PATHS_AUDIO
            )
        }

        suspend fun listDocs(context: Context, callback: SummaryCallbackAlias): ArrayList<MediaFileInfo> {
            val docs: ArrayList<MediaFileInfo> = ArrayList()
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val cursor = context
                .contentResolver
                .query(MediaStore.Files.getContentUri("external"),
                    projection, null, null, null)
            if (cursor == null) return docs else if (cursor.count > 0 && cursor.moveToFirst()) {
                callback.getStorageSummary(cursor.count, 0)
                var longSize = 0L
                do {
                    val path =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                    if (path != null
                        && (path.endsWith(".pdf")
                                || path.endsWith(".epub")
                                || path.endsWith(".docx"))
                    ) {
                        val mediaFileInfo = MediaFileInfo.fromFile(File(path))
                        docs.add(mediaFileInfo)
                        longSize += mediaFileInfo.longSize
                        callback.getStorageSummary(cursor.count, longSize)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            docs.sortWith { lhs: MediaFileInfo, rhs: MediaFileInfo ->
                -1 * java.lang.Long.valueOf(lhs.date).compareTo(rhs.date)
            }
            return docs
        }

        suspend fun listRecentFiles(context: Context, callback: SummaryCallbackAlias): ArrayList<MediaFileInfo> {
            val recentFiles: ArrayList<MediaFileInfo> = ArrayList(20)
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val c = Calendar.getInstance()
            c[Calendar.DAY_OF_YEAR] = c[Calendar.DAY_OF_YEAR] - 2
            val d = c.time
            val cursor: Cursor?
            cursor = if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                val queryArgs = Bundle()
                queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, 20)
                queryArgs.putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED)
                )
                queryArgs.putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                context
                    .contentResolver
                    .query(MediaStore.Files.getContentUri("external"), projection, queryArgs, null)
            } else {
                context
                    .contentResolver
                    .query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        null,
                        null,
                        MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT 20"
                    )
            }
            if (cursor == null) return recentFiles
            if (cursor.count > 0 && cursor.moveToFirst()) {
                callback.getStorageSummary(cursor.count, 0)
                var longSize = 0L
                do {
                    val path =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                    val f = File(path)
                    if (d.compareTo(Date(f.lastModified())) != 1 && !f.isDirectory) {
                        val mediaFileInfo = MediaFileInfo.fromFile(f)
                        recentFiles.add(mediaFileInfo)
                        longSize += mediaFileInfo.longSize
                        callback.getStorageSummary(cursor.count, longSize)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return recentFiles
        }

        private suspend fun listMediaCommon(context: Context, storageSummaryCallback: FilesViewModel.StorageSummaryCallback,
            contentUri: Uri, projection: Array<String>,
                                    selection: String?,
                                    selectionValues: Array<String?>?,
                                    baseSelection: String?,
                                    blacklistPaths: ArrayList<String>?
        ): ArrayList<MediaFileInfo> {
            var selection = selection
            var selectionValues = selectionValues
            selection = if (selection != null && selection.trim { it <= ' ' } != "") {
                "$baseSelection AND $selection"
            } else {
                baseSelection
            }

            // Blacklist
            blacklistPaths?.run {
                selection = generateBlacklistSelection(
                    selection,
                    blacklistPaths.size,
                    projection[0]
                )
                selectionValues = addBlacklistSelectionValues(
                    selectionValues,
                    blacklistPaths
                )
            }
            val cursor =
                context.contentResolver.query(contentUri, projection, selection, selectionValues, null)
            val mediaFileInfoFile: ArrayList<MediaFileInfo> = ArrayList()
            if (cursor == null) return mediaFileInfoFile else if (cursor.count > 0 && cursor.moveToFirst()) {
                storageSummaryCallback.getStorageSummary(cursor.count, 0)
                var longSize = 0L
                do {
                    val path =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                    val mediaFileInfo = MediaFileInfo.fromFile(File(path))
                    mediaFileInfoFile.add(mediaFileInfo)
                    longSize += mediaFileInfo.longSize
                    storageSummaryCallback.getStorageSummary(cursor.count, longSize)
                } while (cursor.moveToNext())
            }
            cursor.close()
            return mediaFileInfoFile
        }

        private fun generateBlacklistSelection(selection: String?, pathCount: Int, dataColumn: String): String {
            var newSelection =
                if (selection != null && selection.trim { it <= ' ' } != "") "$selection AND " else ""
            newSelection += "$dataColumn NOT LIKE ?"
            for (i in 0 until pathCount - 1) {
                newSelection += " AND " + MediaStore.Audio.AudioColumns.DATA + " NOT LIKE ?"
            }
            return newSelection
        }

        private fun addBlacklistSelectionValues(
            selectionValues: Array<String?>?,
            paths: ArrayList<String>
        ): Array<String?>? {
            var selectionValues: Array<String?>? = selectionValues
            if (selectionValues == null) selectionValues = arrayOfNulls(0)
            val newSelectionValues = arrayOfNulls<String>(selectionValues.size + paths.size)
            System.arraycopy(selectionValues, 0, newSelectionValues, 0, selectionValues.size)
            for (i in selectionValues.size until newSelectionValues.size) {
                newSelectionValues[i] = paths[i - selectionValues.size] + "%"
            }
            return newSelectionValues
        }
    }
}

typealias SummaryCallbackAlias = FilesViewModel.StorageSummaryCallback