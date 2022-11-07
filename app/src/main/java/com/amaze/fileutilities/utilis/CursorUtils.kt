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
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE,
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
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE,
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
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE,
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
                var dataColumnIdx = -1
                val mediaColumnIdxValues = MediaColumnIdxValues()
                do {
                    if (dataColumnIdx == -1) {
                        dataColumnIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    }
                    if (dataColumnIdx >= 0) {
                        val path = cursor.getString(dataColumnIdx)
                        val f = File(path)
                        if (d.compareTo(Date(f.lastModified())) != 1 && !f.isDirectory) {
                            val mediaFileInfo = MediaFileInfo.fromFile(
                                f,
                                queryMetaInfo(
                                    cursor,
                                    MediaFileInfo.MEDIA_TYPE_UNKNOWN,
                                    mediaColumnIdxValues
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
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.Files.FileColumns.DATA
            )
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
                var dataColumnIdx = -1
                val mediaColumnIdxValues = MediaColumnIdxValues()
                do {
                    if (dataColumnIdx == -1) {
                        dataColumnIdx = cursor
                            .getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    }
                    if (dataColumnIdx >= 0) {
                        val path = cursor.getString(dataColumnIdx)
                        if (path != null && endsWith.stream().anyMatch { path.endsWith(it) }) {
                            val mediaFileInfo = buildMediaFileInfoFromCursor(
                                context, dataColumnIdx,
                                cursor, mediaColumnIdxValues, MediaFileInfo.MEDIA_TYPE_DOCUMENT
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
                var dataColumnIdx = -1
                val mediaColumnIdxValues = MediaColumnIdxValues()
                do {
                    if (dataColumnIdx == -1) {
                        dataColumnIdx =
                            cursor
                                .getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    }
                    if (dataColumnIdx >= 0) {
                        val mediaFileInfo = buildMediaFileInfoFromCursor(
                            context, dataColumnIdx,
                            cursor, mediaColumnIdxValues, mediaType
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

        private fun loadMediaColumnIdx(
            cursor: Cursor,
            mediaType: Int,
            mediaColumnIdxValues: MediaColumnIdxValues
        ) {
            when (mediaType) {
                MediaFileInfo.MEDIA_TYPE_AUDIO -> {
                    if (mediaColumnIdxValues.audioDurationIdx == -1) {
                        mediaColumnIdxValues.audioDurationIdx = cursor
                            .getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)
                    }
                    if (mediaColumnIdxValues.audioAlbumIdx == -1) {
                        mediaColumnIdxValues.audioAlbumIdx = cursor
                            .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
                    }
                    if (mediaColumnIdxValues.audioArtistIdx == -1) {
                        mediaColumnIdxValues.audioArtistIdx = cursor
                            .getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)
                    }
                    if (mediaColumnIdxValues.audioAlbumIdIdx == -1) {
                        mediaColumnIdxValues.audioAlbumIdIdx = cursor
                            .getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID)
                    }
                }
                MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                    if (mediaColumnIdxValues.videoWidthIdx == -1) {
                        mediaColumnIdxValues.videoWidthIdx = cursor
                            .getColumnIndex(MediaStore.Video.VideoColumns.WIDTH)
                    }
                    if (mediaColumnIdxValues.videoHeightIdx == -1) {
                        mediaColumnIdxValues.videoHeightIdx = cursor
                            .getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT)
                    }
                    if (mediaColumnIdxValues.videoDurationIdx == -1) {
                        mediaColumnIdxValues.videoDurationIdx = cursor
                            .getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                    }
                }
                MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                    if (mediaColumnIdxValues.imgWidthIdx == -1) {
                        mediaColumnIdxValues.imgWidthIdx = cursor
                            .getColumnIndex(MediaStore.Images.ImageColumns.WIDTH)
                    }
                    if (mediaColumnIdxValues.imgHeightIdx == -1) {
                        mediaColumnIdxValues.imgHeightIdx = cursor
                            .getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT)
                    }
                }
            }
            if (mediaColumnIdxValues.commonIdIdx == -1) {
                mediaColumnIdxValues.commonIdIdx =
                    cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            }
            if (mediaColumnIdxValues.commonNameIdx == -1) {
                mediaColumnIdxValues.commonNameIdx =
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            }
            if (mediaColumnIdxValues.commonTitleIdx == -1) {
                mediaColumnIdxValues.commonTitleIdx =
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)
            }
            if (mediaColumnIdxValues.commonLastModifiedIdx == -1) {
                mediaColumnIdxValues.commonLastModifiedIdx =
                    cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            }
            if (mediaColumnIdxValues.commonSizeIdx == -1) {
                mediaColumnIdxValues.commonSizeIdx =
                    cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            }
        }

        private fun buildMediaFileInfoFromCursor(
            context: Context,
            dataColumnIdx: Int,
            cursor: Cursor,
            mediaColumnIdxValues: MediaColumnIdxValues,
            mediaType: Int
        ): MediaFileInfo {
            val path = cursor.getString(dataColumnIdx)
            loadMediaColumnIdx(cursor, mediaType, mediaColumnIdxValues)
            var id: Long? = -1L
            var title: String?
            var lastModified: Long?
            var size: Long?
            var file: File? = null
            if (mediaColumnIdxValues.commonIdIdx >= 0) {
                id = cursor.getLong(mediaColumnIdxValues.commonIdIdx)
            }
            if (mediaColumnIdxValues.commonNameIdx >= 0) {
                title = cursor.getString(mediaColumnIdxValues.commonNameIdx)
                if (title == null && mediaColumnIdxValues.commonTitleIdx >= 0) {
                    title = cursor.getString(mediaColumnIdxValues.commonTitleIdx)
                }
                if (title == null) {
                    if (file == null) {
                        file = File(path)
                    }
                    title = file.name
                }
            } else {
                if (file == null) {
                    file = File(path)
                }
                title = file.name
            }
            if (mediaColumnIdxValues.commonLastModifiedIdx >= 0) {
                lastModified = cursor.getLong(mediaColumnIdxValues.commonLastModifiedIdx) * 1000
            } else {
                if (file == null) {
                    file = File(path)
                }
                lastModified = file.lastModified()
            }
            if (mediaColumnIdxValues.commonSizeIdx >= 0) {
                size = cursor.getLong(mediaColumnIdxValues.commonSizeIdx)
            } else {
                if (file == null) {
                    file = File(path)
                }
                size = file.length()
            }
            return MediaFileInfo.fromFile(
                mediaType,
                id ?: -1L,
                title, path, lastModified, size,
                context,
                queryMetaInfo(cursor, mediaType, mediaColumnIdxValues)
            )
        }

        private fun queryMetaInfo(
            cursor: Cursor,
            mediaType: Int,
            mediaColumnIdxValues: MediaColumnIdxValues
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
                        if (mediaColumnIdxValues.audioDurationIdx >= 0) {
                            audioDuration = cursor.getLong(mediaColumnIdxValues.audioDurationIdx)
                        }
                        if (mediaColumnIdxValues.audioAlbumIdx >= 0) {
                            audioAlbum = cursor.getString(mediaColumnIdxValues.audioAlbumIdx)
                        }
                        if (mediaColumnIdxValues.audioArtistIdx >= 0) {
                            audioArtist = cursor.getString(mediaColumnIdxValues.audioArtistIdx)
                        }
                        if (mediaColumnIdxValues.audioAlbumIdIdx >= 0) {
                            albumId = cursor.getLong(mediaColumnIdxValues.audioAlbumIdIdx)
                        }
                        audioMetaData = MediaFileInfo
                            .AudioMetaData(
                                audioAlbum, audioArtist, audioDuration, albumId,
                                null
                            )
                    }
                    MediaFileInfo.MEDIA_TYPE_VIDEO -> {
                        // video
                        var videoWidth: Int? = null
                        var videoHeight: Int? = null
                        var videoDuration: Long? = null
                        if (mediaColumnIdxValues.videoWidthIdx >= 0) {
                            videoWidth = cursor.getInt(mediaColumnIdxValues.videoWidthIdx)
                        }
                        if (mediaColumnIdxValues.videoHeightIdx >= 0) {
                            videoHeight = cursor.getInt(mediaColumnIdxValues.videoHeightIdx)
                        }
                        if (mediaColumnIdxValues.videoDurationIdx >= 0) {
                            videoDuration = cursor.getLong(mediaColumnIdxValues.videoDurationIdx)
                        }
                        videoMetaData = MediaFileInfo.VideoMetaData(
                            videoDuration,
                            videoWidth, videoHeight
                        )
                    }
                    MediaFileInfo.MEDIA_TYPE_IMAGE -> {
                        var imageWidth: Int? = null
                        var imageHeight: Int? = null
                        if (mediaColumnIdxValues.imgWidthIdx >= 0) {
                            imageWidth = cursor.getInt(mediaColumnIdxValues.imgWidthIdx)
                        }
                        if (mediaColumnIdxValues.imgHeightIdx >= 0) {
                            imageHeight = cursor.getInt(mediaColumnIdxValues.imgHeightIdx)
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

        data class MediaColumnIdxValues(
            var audioDurationIdx: Int = -1,
            var audioAlbumIdx: Int = -1,
            var audioArtistIdx: Int = -1,
            var audioAlbumIdIdx: Int = -1,
            var videoWidthIdx: Int = -1,
            var videoHeightIdx: Int = -1,
            var videoDurationIdx: Int = -1,
            var imgWidthIdx: Int = -1,
            var imgHeightIdx: Int = -1,
            var commonIdIdx: Int = -1,
            var commonNameIdx: Int = -1,
            var commonTitleIdx: Int = -1,
            var commonLastModifiedIdx: Int = -1,
            var commonSizeIdx: Int = -1
        )
    }
}
