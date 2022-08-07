/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import com.amaze.fileutilities.audio_player.AudioUtils
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Calendar
import java.util.Date

class CursorUtils {

    companion object {
        var log: Logger = LoggerFactory.getLogger(CursorUtils::class.java)

        private const val BASE_SELECTION_AUDIO =
            MediaStore.Audio.AudioColumns.IS_MUSIC + "=1" + " AND " +
                MediaStore.Audio.AudioColumns.TITLE + " != ''"

        private const val BASE_SELECTION_IMAGES = MediaStore.Images.ImageColumns.TITLE + " != ''"

        private const val BASE_SELECTION_VIDEOS = MediaStore.Video.VideoColumns.TITLE + " != ''"

        private val BLACKLIST_PATHS_AUDIO = arrayListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS)
                .canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
                .canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                .canonicalPath
        )

        fun listImages(context: Context): Pair<FilesViewModel.StorageSummary,
            ArrayList<MediaFileInfo>> {
            val projection = arrayOf(
                MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.WIDTH,
                MediaStore.Images.ImageColumns.HEIGHT
            )
            return listMediaCommon(
                context,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, BASE_SELECTION_IMAGES, null,
                MediaFileInfo.MEDIA_TYPE_IMAGE
            )
        }

        fun listVideos(context: Context): Pair<FilesViewModel.StorageSummary,
            ArrayList<MediaFileInfo>> {
            val projection = arrayOf(
                MediaStore.Video.Media.DATA,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.WIDTH,
                MediaStore.Video.VideoColumns.HEIGHT
            )
            return listMediaCommon(
                context,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, BASE_SELECTION_VIDEOS,

                null,
                MediaFileInfo.MEDIA_TYPE_VIDEO
            )
        }

        fun listAudio(context: Context, blacklistPaths: List<String>):
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>> {
            val projection = arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ALBUM_ID
            )
            return listMediaCommon(
                context,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null, BASE_SELECTION_AUDIO, blacklistPaths,
                MediaFileInfo.MEDIA_TYPE_AUDIO
            )
        }

        fun getMediaFilesCount(context: Context): Int {
            val cursor = context
                .contentResolver
                .query(
                    MediaStore.Files.getContentUri("external"),
                    null, null, null, null
                )
            cursor.use { cur ->
                return cur?.count ?: 0
            }
        }

        fun listDocs(context: Context): Pair<FilesViewModel.StorageSummary,
            ArrayList<MediaFileInfo>> {
            return listFilesWithExtension(context, arrayListOf(".pdf", ".epub", ".docx"))
        }

        fun listApks(context: Context): Pair<FilesViewModel.StorageSummary,
            ArrayList<MediaFileInfo>> {
            return listFilesWithExtension(context, arrayListOf(".apk"))
        }

        fun listRecentFiles(context: Context): ArrayList<MediaFileInfo> {
            val recentFiles: ArrayList<MediaFileInfo> = ArrayList()
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )
            val c = Calendar.getInstance()
            c[Calendar.DAY_OF_YEAR] = c[Calendar.DAY_OF_YEAR] - 2
            val d = c.time

            /*val mimeTypePdf = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpg")
            val mimeTypeEpub = MimeTypeMap.getSingleton().getMimeTypeFromExtension("epub")
            val mimeTypeDocx = MimeTypeMap.getSingleton().getMimeTypeFromExtension("docx")
            val selectionArgsPdf = arrayOf(mimeTypePdf, mimeTypeEpub, mimeTypeDocx)
            val selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE +
                    "=(${mimeTypePdf.toString()})"*/

            val selectionArgs: Array<String>? = null // there is no ? in selection so null here

            val selection = MediaStore.Files.FileColumns.MEDIA_TYPE +
                " IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}, " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"

            val queryCursor: Cursor?
            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
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
                queryCursor = context
                    .contentResolver
                    .query(
                        MediaStore.Files.getContentUri("external"), projection,
                        queryArgs, null
                    )
            } else {
                queryCursor = context
                    .contentResolver
                    .query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        selection,
                        selectionArgs,
                        MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC LIMIT 20"
                    )
            }

            val cursor = queryCursor ?: return recentFiles
            if (cursor.count > 0 && cursor.moveToFirst()) {
                do {
                    if (cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA) >= 0) {
                        val path =
                            cursor.getString(
                                cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                            )
                        val f = File(path)
                        if (d.compareTo(Date(f.lastModified())) != 1 && !f.isDirectory) {
                            val mediaFileInfo = MediaFileInfo.fromFile(
                                f,
                                queryMetaInfo(
                                    context,
                                    cursor,
                                    MediaFileInfo.MEDIA_TYPE_UNKNOWN
                                )
                            )
                            recentFiles.add(mediaFileInfo)
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return recentFiles
        }

        private fun listFilesWithExtension(context: Context, endsWith: List<String>):
            Pair<FilesViewModel.StorageSummary,
                ArrayList<MediaFileInfo>> {
            val docs: ArrayList<MediaFileInfo> = ArrayList()
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val cursor = context
                .contentResolver
                .query(
                    MediaStore.Files.getContentUri("external"),
                    projection, null, null, null
                )
            var longSize = 0L
            if (cursor == null) {
                return Pair(FilesViewModel.StorageSummary(0, 0), docs)
            } else if (cursor.count > 0 && cursor.moveToFirst()) {
                do {
                    if (cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA) >= 0) {
                        val path =
                            cursor.getString(
                                cursor
                                    .getColumnIndex(MediaStore.Files.FileColumns.DATA)
                            )
                        if (path != null && endsWith.stream().anyMatch { path.endsWith(it) }) {
                            val mediaFileInfo = MediaFileInfo.fromFile(
                                File(path),
                                MediaFileInfo.ExtraInfo(
                                    MediaFileInfo.MEDIA_TYPE_DOCUMENT,
                                    null, null, null
                                )
                            )
                            docs.add(mediaFileInfo)
                            longSize += mediaFileInfo.longSize
                        }
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            /*docs.sortWith { lhs: MediaFileInfo, rhs: MediaFileInfo ->
                -1 * java.lang.Long.valueOf(lhs.date).compareTo(rhs.date)
            }*/
            return Pair(FilesViewModel.StorageSummary(docs.size, 0, longSize), docs)
        }

        private fun listMediaCommon(
            context: Context,
            contentUri: Uri,
            projection: Array<String>,
            selection: String?,
            selectionValues: Array<String?>?,
            baseSelection: String?,
            blacklistPaths: List<String>?,
            mediaType: Int
        ): Pair<FilesViewModel.StorageSummary, ArrayList<MediaFileInfo>> {
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
                context.contentResolver.query(
                    contentUri, projection, selection,
                    selectionValues, null
                )
            val mediaFileInfoFile: ArrayList<MediaFileInfo> = ArrayList()
//            var cursorCount = 0
            var longSize = 0L
            if (cursor == null) return Pair(
                FilesViewModel.StorageSummary(0, 0),
                mediaFileInfoFile
            ) else if (cursor.count > 0 && cursor.moveToFirst()) {
//                storageSummaryCallback.getStorageSummary(cursor.count, 0)
//                cursorCount = cursor.count
                do {
                    if (cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA) >= 0) {
                        val path =
                            cursor.getString(
                                cursor
                                    .getColumnIndex(MediaStore.Files.FileColumns.DATA)
                            )

                        val mediaFileInfo = MediaFileInfo.fromFile(
                            File(path),
                            queryMetaInfo(context, cursor, mediaType)
                        )
                        mediaFileInfoFile.add(mediaFileInfo)
                        longSize += mediaFileInfo.longSize
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            return Pair(
                FilesViewModel.StorageSummary(mediaFileInfoFile.size, 0, longSize),
                mediaFileInfoFile
            )
        }

        private fun queryMetaInfo(
            context: Context,
            cursor: Cursor,
            mediaType: Int
        ): MediaFileInfo.ExtraInfo {
            var audioMetaData: MediaFileInfo.AudioMetaData? = null
            var imageMetaData: MediaFileInfo.ImageMetaData? = null
            var videoMetaData: MediaFileInfo.VideoMetaData? = null
            try {
                when (mediaType) {
                    MediaFileInfo.MEDIA_TYPE_AUDIO -> {
                        // audio
                        var audioDuration: Long? = null
                        var audioAlbum: String? = null
                        var audioArtist: String? = null
                        var albumId: Long? = null
                        var albumBitmap: Bitmap? = null
                        if (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION) >= 0) {
                            audioDuration = cursor.getLong(
                                cursor
                                    .getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM) >= 0) {
                            audioAlbum = cursor.getString(
                                cursor
                                    .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST) >= 0) {
                            audioArtist = cursor.getString(
                                cursor
                                    .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID) >= 0) {
                            albumId = cursor.getLong(
                                cursor
                                    .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)
                            )
                            val albumUri = AudioUtils.getMediaStoreAlbumCoverUri(albumId)
                            albumBitmap = AudioUtils.getAlbumBitmap(context, albumUri)
                        }
                        audioMetaData = MediaFileInfo
                            .AudioMetaData(
                                audioAlbum, audioArtist, audioDuration, albumId,
                                albumBitmap
                            )
                    }
                    MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                        // video
                        var videoWidth: Int? = null
                        var videoHeight: Int? = null
                        var videoDuration: Long? = null
                        if (cursor.getColumnIndex(MediaStore.Video.VideoColumns.WIDTH) >= 0) {
                            videoWidth = cursor.getInt(
                                cursor
                                    .getColumnIndex(MediaStore.Video.VideoColumns.WIDTH)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT) >= 0) {
                            videoHeight = cursor.getInt(
                                cursor
                                    .getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) >= 0) {
                            videoDuration = cursor.getLong(
                                cursor
                                    .getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                            )
                        }
                        videoMetaData = MediaFileInfo.VideoMetaData(
                            videoDuration,
                            videoWidth, videoHeight
                        )
                    }
                    MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                        var imageWidth: Int? = null
                        var imageHeight: Int? = null
                        if (cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH) >= 0) {
                            imageWidth = cursor.getInt(
                                cursor
                                    .getColumnIndex(MediaStore.Images.ImageColumns.WIDTH)
                            )
                        }
                        if (cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT) >= 0) {
                            imageHeight = cursor.getInt(
                                cursor
                                    .getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT)
                            )
                        }
                        imageMetaData = MediaFileInfo.ImageMetaData(imageWidth, imageHeight)
                    }
                }
            } catch (e: Exception) {
                log.warn(
                    "Error while fetching metadata for " +
                        "$mediaType",
                    e
                )
            }
            return MediaFileInfo.ExtraInfo(
                mediaType,
                audioMetaData, videoMetaData, imageMetaData
            )
        }

        private fun generateBlacklistSelection(
            selection: String?,
            pathCount: Int,
            dataColumn: String
        ): String {
            var newSelection =
                if (selection != null && selection.trim { it <= ' ' } != "") {
                    "$selection AND "
                } else {
                    ""
                }
            newSelection += "$dataColumn NOT LIKE ?"
            for (i in 0 until pathCount - 1) {
                newSelection += " AND $dataColumn NOT LIKE ?"
            }
            return newSelection
        }

        private fun addBlacklistSelectionValues(
            selectionValues: Array<String?>?,
            paths: List<String>
        ): Array<String?> {
            var selectionValues: Array<String?>? = selectionValues
            if (selectionValues == null) selectionValues = arrayOfNulls(0)
            val newSelectionValues = arrayOfNulls<String>(selectionValues.size + paths.size)
            System.arraycopy(
                selectionValues, 0, newSelectionValues, 0,
                selectionValues.size
            )
            for (i in selectionValues.size until newSelectionValues.size) {
                newSelectionValues[i] = paths[i - selectionValues.size] + "%"
            }
            return newSelectionValues
        }
    }
}
