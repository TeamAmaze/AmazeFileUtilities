/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.audio_player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class AudioPlaybackInfo(
    var audioModel: LocalAudioModel = LocalAudioModel(-1, Uri.EMPTY, ""),
    var title: String = "",
    var trackNumber: Int = -1,
    var year: Int = -1,
    var duration: Long = -1,
    var data: String = "",
    var dateModified: Long = -1,
    var albumId: Long = -1,
    var albumName: String = "",
    var artistId: Long = -1,
    var artistName: String = "",
    var albumArt: Bitmap?,
    var currentPosition: Long = -1,
    var isPlaying: Boolean = false,
    var currentLyrics: String? = null,
    var isLyricsSynced: Boolean = false,
    var lyricsStrings: LyricsParser.LyricsStrings? = null
) : Parcelable {
    companion object {
        const val BASE_SELECTION =
            AudioColumns.IS_MUSIC + "=1" + " AND " + AudioColumns.TITLE + " != ''"
        val BASE_PROJECTION = arrayOf(
            BaseColumns._ID, // 0
            AudioColumns.TITLE, // 1
            AudioColumns.TRACK, // 2
            AudioColumns.YEAR, // 3
            AudioColumns.DURATION, // 4
            AudioColumns.DATA, // 5
            AudioColumns.DATE_MODIFIED, // 6
            AudioColumns.ALBUM_ID, // 7
            AudioColumns.ALBUM, // 8
            AudioColumns.ARTIST_ID, // 9
            AudioColumns.ARTIST
        )

        val EMPTY_PLAYBACK = AudioPlaybackInfo(
            LocalAudioModel(-1, Uri.EMPTY, ""), "", -1, -1, -1, "", -1, -1,
            "", -1, "", null,
            -1, false
        )

        fun init(context: Context, uri: Uri): AudioPlaybackInfo {
            val cursor = AudioUtils.makeSongCursor(
                context,
                uri,
                MediaStore.Audio.Media.DATA + "=?",
                arrayOf(uri.path?.replace("/storage_root", ""))
            )
            val audioModel = LocalAudioModel(-1, uri, "")
            cursor?.moveToFirst()
            if (cursor != null && cursor.count > 0) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1)
                val trackNumber = cursor.getInt(2)
                val year = cursor.getInt(3)
                val duration = cursor.getLong(4)
                val data = cursor.getString(5)
                val dateModified = cursor.getLong(6)
                val albumId = cursor.getLong(7)
                val albumName = cursor.getString(8)
                val artistId = cursor.getLong(9)
                val artistName = cursor.getString(10)
                audioModel.id = id
                val albumUri = AudioUtils.getMediaStoreAlbumCoverUri(albumId)
                val albumBitmap = AudioUtils.getAlbumBitmap(context, albumUri)
                return AudioPlaybackInfo(
                    audioModel,
                    title, trackNumber, year, duration, data, dateModified, albumId, albumName,
                    artistId, artistName, albumBitmap, -1, false
                )
            } else {
                val playbackInfo = EMPTY_PLAYBACK
                playbackInfo.audioModel = audioModel
                if (uri.path != null) {
                    val file = File(uri.path!!)
                    val title = if (file.exists()) file.name else uri.path!!
                    playbackInfo.title = title
                } else {
                    playbackInfo.title = ""
                }
                return playbackInfo
            }
        }
    }
}
