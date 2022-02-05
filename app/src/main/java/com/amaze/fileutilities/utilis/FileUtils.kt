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

import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.DrawableRes
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

class FileUtils {

    companion object {
        private const val INTERNAL_SHARED_STORAGE = "Internal shared storage"
        const val DEFAULT_BUFFER_SIZE = 8192

        private const val DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0"
        private const val WHATSAPP_BASE_ANDROID = "Android/media/com.whatsapp"
        private const val WHATSAPP_BASE = "WhatsApp/Media"
        private const val WHATSAPP_IMAGES_BASE = "WhatsApp Images"
        private const val WHATSAPP_AUDIO_BASE = "WhatsApp Audio"
        private const val WHATSAPP_IMAGES = "$WHATSAPP_BASE_ANDROID/$WHATSAPP_BASE/$WHATSAPP_IMAGES_BASE"
        private const val WHATSAPP_IMAGES_2 = "$WHATSAPP_BASE/$WHATSAPP_IMAGES_BASE"
        private const val WHATSAPP_AUDIO = "$WHATSAPP_BASE_ANDROID/$WHATSAPP_BASE/$WHATSAPP_AUDIO_BASE"
        private const val WHATSAPP_AUDIO_2 = "$WHATSAPP_BASE/$WHATSAPP_AUDIO_BASE"
        private const val RECORDINGS = "Recordings"
        private const val SCREENSHOTS = "Screenshots"
        private const val INSTAGRAM = "Instagram"
        private const val CAMERA_BASE = "Camera"
        private const val ADM = "ADM"
        private const val TELEGRAM = "Telegram"
        private const val TELEGRAM_IMAGES = "Telegram Images"
        private const val TELEGRAM_VIDEO = "Telegram Video"
        private val CAMERA = "${Environment.DIRECTORY_DCIM}/$CAMERA_BASE"

        private val DEFAULT_MEMES = listOf(Environment.DIRECTORY_DOWNLOADS, WHATSAPP_IMAGES,
            WHATSAPP_IMAGES_2)

        private val DEFAULT_BLUR = listOf(CAMERA,
            "${Environment.DIRECTORY_PICTURES}/$INSTAGRAM")

        private val DEFAULT_IMAGE_FEATURES = listOf(CAMERA,
            "${Environment.DIRECTORY_PICTURES}/$INSTAGRAM")

        private val DEFAULT_DOWNLOADS = listOf(Environment.DIRECTORY_DOWNLOADS,
            "${Environment.DIRECTORY_DOWNLOADS}/$ADM", ADM)

        private val DEFAULT_RECORDINGS = if (VERSION.SDK_INT >= VERSION_CODES.S) {
            listOf(Environment.DIRECTORY_RECORDINGS)
        } else {
            listOf(RECORDINGS)
        }

        private val DEFAULT_AUDIO_PLAYER = listOf(if (VERSION.SDK_INT >= VERSION_CODES.S)
            Environment.DIRECTORY_RECORDINGS else RECORDINGS, WHATSAPP_AUDIO, WHATSAPP_AUDIO_2)

        private val DEFAULT_SCREENSHOTS = listOf(if (VERSION.SDK_INT >= VERSION_CODES.Q)
            Environment.DIRECTORY_SCREENSHOTS else SCREENSHOTS, "${Environment.DIRECTORY_PICTURES}/$SCREENSHOTS")

        private val DEFAULT_TELEGRAM = listOf("${Environment.DIRECTORY_PICTURES}/$TELEGRAM/$TELEGRAM_IMAGES",
            "${Environment.DIRECTORY_PICTURES}/$TELEGRAM/$TELEGRAM_VIDEO",
            "$TELEGRAM/$TELEGRAM_IMAGES",
            "$TELEGRAM/$TELEGRAM_VIDEO"
        )

        /*val DEFAULT_PATH_PREFS_INCLUSIVE = listOf(DEFAULT_MEMES, DEFAULT_BLUR, DEFAULT_GROUP_PIC,
            DEFAULT_SELFIE, DEFAULT_SLEEP, DEFAULT_DISTRACTED, DEFAULT_DOWNLOADS,
            DEFAULT_RECORDINGS,
            DEFAULT_SCREENSHOTS, DEFAULT_TELEGRAM)*/
        val DEFAULT_PATH_PREFS_INCLUSIVE = mapOf<Int, List<String>>(
            Pair(PathPreferences.FEATURE_ANALYSIS_MEME, DEFAULT_MEMES),
            Pair(PathPreferences.FEATURE_ANALYSIS_BLUR, DEFAULT_BLUR),
            Pair(PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES, DEFAULT_IMAGE_FEATURES),
            Pair(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS, DEFAULT_DOWNLOADS),
            Pair(PathPreferences.FEATURE_ANALYSIS_RECORDING, DEFAULT_RECORDINGS),
            Pair(PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS, DEFAULT_SCREENSHOTS),
            Pair(PathPreferences.FEATURE_ANALYSIS_TELEGRAM, DEFAULT_TELEGRAM)
        )

        val DEFAULT_PATH_PREFS_EXCLUSIVE = mapOf(
            Pair(PathPreferences.FEATURE_AUDIO_PLAYER, DEFAULT_AUDIO_PLAYER)
        )

        private val DIR_SEPARATOR = Pattern.compile("/")

        @TargetApi(VERSION_CODES.N)
        fun getStorageDirectoriesNew(context: Context): StorageDirectoryParcelable? {
            val volumes: ArrayList<StorageDirectoryParcelable> =
                ArrayList<StorageDirectoryParcelable>()
            val sm: StorageManager = context.getSystemService(StorageManager::class.java)
            for (volume in sm.storageVolumes) {
                if (!volume.state.equals(Environment.MEDIA_MOUNTED, ignoreCase = true) &&
                    !volume.state.equals(
                            Environment.MEDIA_MOUNTED_READ_ONLY,
                            ignoreCase = true
                        )
                ) {
                    continue
                }
                val file: File = getVolumeDirectory(volume)
                var name = volume.getDescription(context)
                if (INTERNAL_SHARED_STORAGE.equals(name, ignoreCase = true)) {
                    name = context.resources.getString(R.string.internal_storage)
                    return StorageDirectoryParcelable(file.path, name)
                }
            }
            return null
        }

        /**
         * Returns all available SD-Cards in the system (include emulated)
         *
         *
         * Warning: Hack! Based on Android source code of version 4.3 (API 18)
         * Because there was no
         * standard way to get it before android N
         *
         * @return All available SD-Cards in the system (include emulated)
         */
        @Synchronized
        fun getStorageDirectoriesLegacy(context: Context):
            StorageDirectoryParcelable? {
            val rv: MutableList<String> = ArrayList()

            // Primary physical SD-CARD (not emulated)
            val rawExternalStorage = System
                .getenv("EXTERNAL_STORAGE")
            // All Secondary SD-CARDs (all exclude primary) separated by ":"
            val rawSecondaryStoragesStr = System
                .getenv("SECONDARY_STORAGE")
            // Primary emulated SD-CARD
            val rawEmulatedStorageTarget = System
                .getenv("EMULATED_STORAGE_TARGET")
            if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
                // Device has physical external storage; use plain paths.
                if (TextUtils.isEmpty(rawExternalStorage)) {
                    // EXTERNAL_STORAGE undefined; falling back to default.
                    // Check for actual existence of
                    // the directory before adding to list
                    if (File(DEFAULT_FALLBACK_STORAGE_PATH).exists()) {
                        rv.add(DEFAULT_FALLBACK_STORAGE_PATH)
                    } else {
                        // We know nothing else, use Environment's fallback
                        rv.add(
                            Environment
                                .getExternalStorageDirectory().absolutePath
                        )
                    }
                } else {
                    rv.add(rawExternalStorage)
                }
            } else {
                // Device has emulated storage; external storage paths should have
                // userId burned into them.
                val rawUserId: String
                if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR1) {
                    rawUserId = ""
                } else {
                    val path = Environment
                        .getExternalStorageDirectory().absolutePath
                    val folders: Array<String> = DIR_SEPARATOR.split(path)
                    val lastFolder = folders[folders.size - 1]
                    var isDigit = false
                    try {
                        Integer.valueOf(lastFolder)
                        isDigit = true
                    } catch (ignored: NumberFormatException) {
                    }
                    rawUserId = if (isDigit) lastFolder else ""
                }
                // /storage/emulated/0[1,2,...]
                if (TextUtils.isEmpty(rawUserId)) {
                    rv.add(rawEmulatedStorageTarget)
                } else {
                    rv.add(
                        rawEmulatedStorageTarget +
                            File.separator + rawUserId
                    )
                }
            }
            // Add all secondary storages
            if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                // All Secondary SD-CARDs splited into array
                val rawSecondaryStorages =
                    rawSecondaryStoragesStr.split(File.pathSeparator).toTypedArray()
                Collections.addAll(rv, *rawSecondaryStorages)
            }
//            if (VERSION.SDK_INT >= VERSION_CODES.M
//            && checkStoragePermission()) rv.clear()
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                val strings: Array<String> =
                    getExtSdCardPathsForActivity(context)
                for (s in strings) {
                    val f = File(s)
                    if (!rv.contains(s) && f.canRead() && f.isDirectory) rv.add(s)
                }
            }
            // Assign a label and icon to each directory
            val volumes = ArrayList<StorageDirectoryParcelable>()
            for (file in rv) {
                val f = File(file)
                @DrawableRes var icon: Int
                if ("/storage/emulated/legacy" == file ||
                    "/storage/emulated/0" == file ||
                    "/mnt/sdcard" == file
                ) {
                    return StorageDirectoryParcelable(
                        file,
                        context.resources.getString(R.string.internal_storage)
                    )
                } else {
                    // ignore sd cards for now
                }
            }
            return null
        }

        fun formatStorageLength(context: Context, longSize: Long): String {
            return Formatter.formatFileSize(context, longSize)
        }

        @Throws(NoSuchAlgorithmException::class, IOException::class)
        fun getSHA256Checksum(file: File): String {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val input = ByteArray(DEFAULT_BUFFER_SIZE)
            var length: Int
            val inputStream: InputStream = FileInputStream(file)
            while (inputStream.read(input).also { length = it } != -1) {
                if (length > 0) messageDigest.update(input, 0, length)
            }
            val hash = messageDigest.digest()
            val hexString = StringBuilder()
            for (aHash in hash) {
                // convert hash to base 16
                val hex = Integer.toHexString(0xff and aHash.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            inputStream.close()
            return hexString.toString()
        }

        @TargetApi(VERSION_CODES.N)
        private fun getVolumeDirectory(volume: StorageVolume): File {
            return try {
                val f = StorageVolume::class.java.getDeclaredField("mPath")
                f.isAccessible = true
                f[volume] as File
            } catch (e: Exception) {
                // This shouldn't fail, as mPath has been there in every version
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        @TargetApi(VERSION_CODES.KITKAT)
        private fun getExtSdCardPathsForActivity(context: Context):
            Array<String> {
            val paths: MutableList<String> = ArrayList()
            for (file in context.getExternalFilesDirs("external")) {
                if (file != null) {
                    val index = file.absolutePath
                        .lastIndexOf("/Android/data")
                    if (index < 0) {
                        Log.w(
                            javaClass.simpleName,
                            "Unexpected external file dir: " +
                                file.absolutePath
                        )
                    } else {
                        var path = file.absolutePath.substring(0, index)
                        try {
                            path = File(path).canonicalPath
                        } catch (e: IOException) {
                            // Keep non-canonical path.
                        }
                        paths.add(path)
                    }
                }
            }
            if (paths.isEmpty()) paths.add("/storage/sdcard1")
            return paths.toTypedArray()
        }
    }
}
