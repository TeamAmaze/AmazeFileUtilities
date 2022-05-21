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

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import com.amaze.fileutilities.utilis.log
import java.io.FileDescriptor
import java.util.*

class AudioUtils {

    companion object {
        val BLACKLIST_PATHS = arrayListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS)
                .canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
                .canonicalPath,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                .canonicalPath
        )

        fun getMediaStoreAlbumCoverUri(albumId: Long): Uri {
            val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
            return ContentUris.withAppendedId(sArtworkUri, albumId)
        }

        /**
         * Returns bitmap for content uri compatible album art
         * eg: content://media/external/audio/albumart/1234
         */
        fun getAlbumBitmap(context: Context, uri: Uri): Bitmap? {
            var bitmap: Bitmap? = null
            try {
                val parcelFileDescriptor: ParcelFileDescriptor? =
                    context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                }
            } catch (e: Exception) {
                log.warn("failed to extract album art", e)
            }
            return bitmap
        }

        fun makeSongCursor(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionValues: Array<String?>?
        ): Cursor? {
            return makeSongCursor(
                context,
                uri,
                selection,
                selectionValues,
                null
            )
        }

        fun getReadableDurationString(songDurationMillis: Long): String? {
            var minutes = songDurationMillis / 1000 / 60
            val seconds = songDurationMillis / 1000 % 60
            return if (minutes < 60) {
                String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
            } else {
                val hours = minutes / 60
                minutes %= 60
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            }
        }

        fun generatePalette(bitmap: Bitmap?): Palette? {
            return if (bitmap == null) null else Palette.from(bitmap).generate()
        }

        @ColorInt
        fun getColor(palette: Palette?, fallback: Int): Int {
            if (palette != null) {
                if (palette.vibrantSwatch != null) {
                    return palette.vibrantSwatch!!.rgb
                } else if (palette.mutedSwatch != null) {
                    return palette.mutedSwatch!!.rgb
                } else if (palette.darkVibrantSwatch != null) {
                    return palette.darkVibrantSwatch!!.rgb
                } else if (palette.darkMutedSwatch != null) {
                    return palette.darkMutedSwatch!!.rgb
                } else if (palette.lightVibrantSwatch != null) {
                    return palette.lightVibrantSwatch!!.rgb
                } else if (palette.lightMutedSwatch != null) {
                    return palette.lightMutedSwatch!!.rgb
                } else if (palette.swatches.isNotEmpty()) {
                    return Collections.max(palette.swatches, SwatchComparator.instance).rgb
                }
            }
            return fallback
        }

        private class SwatchComparator : Comparator<Swatch?> {

            companion object {
                private var sInstance: SwatchComparator? = null
                val instance: SwatchComparator?
                    get() {
                        if (sInstance == null) {
                            sInstance = SwatchComparator()
                        }
                        return sInstance
                    }
            }

            override fun compare(o1: Swatch?, o2: Swatch?): Int {
                if (o1 != null && o2 != null) {
                    return o1.population - o2.population
                }
                return 0
            }
        }

        private fun makeSongCursor(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionValues: Array<String?>?,
            sortOrder: String?
        ): Cursor? {
            var selection = selection
            var selectionValues = selectionValues
            selection = if (selection != null && selection.trim { it <= ' ' } != "") {
                "${AudioPlaybackInfo.BASE_SELECTION} AND $selection"
            } else {
                AudioPlaybackInfo.BASE_SELECTION
            }

            // Blacklist
            selection = generateBlacklistSelection(
                selection,
                BLACKLIST_PATHS.size
            )
            selectionValues = addBlacklistSelectionValues(
                selectionValues,
                BLACKLIST_PATHS
            )

            return try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    AudioPlaybackInfo.BASE_PROJECTION,
                    selection,
                    selectionValues,
                    sortOrder
                )
            } catch (e: SecurityException) {
                log.warn("cannot query external content uri, not allowed", e)
                null
            }
        }

        private fun generateBlacklistSelection(selection: String?, pathCount: Int): String {
            var newSelection =
                if (selection != null && selection.trim { it <= ' ' } != "") {
                    "$selection AND "
                } else {
                    ""
                }
            newSelection += AudioColumns.DATA + " NOT LIKE ?"
            for (i in 0 until pathCount - 1) {
                newSelection += " AND " + AudioColumns.DATA + " NOT LIKE ?"
            }
            return newSelection
        }

        private fun addBlacklistSelectionValues(
            selectionValues: Array<String?>?,
            paths: ArrayList<String>
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
