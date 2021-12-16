package com.amaze.fileutilities.audio_player

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioPlaybackInfo(var audioModel: LocalAudioModel = LocalAudioModel(-1, Uri.EMPTY, ""),
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
                             var currentPosition: Int = -1,
                             var isPlaying: Boolean = false): Parcelable {
    companion object {
        const val BASE_SELECTION =
            AudioColumns.IS_MUSIC + "=1" + " AND " + AudioColumns.TITLE + " != ''"
        val BASE_PROJECTION = arrayOf(
            BaseColumns._ID,  // 0
            AudioColumns.TITLE,  // 1
            AudioColumns.TRACK,  // 2
            AudioColumns.YEAR,  // 3
            AudioColumns.DURATION,  // 4
            AudioColumns.DATA,  // 5
            AudioColumns.DATE_MODIFIED,  // 6
            AudioColumns.ALBUM_ID,  // 7
            AudioColumns.ALBUM,  // 8
            AudioColumns.ARTIST_ID,  // 9
            AudioColumns.ARTIST
        )

        val EMPTY_PLAYBACK = AudioPlaybackInfo(LocalAudioModel(-1, Uri.EMPTY, "")
            , "", -1, -1, -1, "", -1, -1,
            "", -1, "",
            -1, false)


        fun init(context: Context, uri: Uri): AudioPlaybackInfo {
            val cursor = AudioUtils.makeSongCursor(
                context,
                uri,
                MediaStore.Audio.Media.DATA + "=?",
                arrayOf(uri.path?.replace("/storage_root", ""))
            )
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
                return AudioPlaybackInfo(
                    LocalAudioModel(id, uri, ""),
                    title, trackNumber, year, duration, data, dateModified, albumId, albumName,
                    artistId, artistName, -1, false
                )
            } else {
                var playbackInfo = EMPTY_PLAYBACK
                playbackInfo.title = uri.path!!
                return playbackInfo
            }
        }
    }


}