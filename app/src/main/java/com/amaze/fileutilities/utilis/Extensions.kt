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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.core.content.FileProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.files.folderChooser
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

var log: Logger = LoggerFactory.getLogger(Utils::class.java)

fun Uri.getSiblingUriFiles(context: Context): ArrayList<Uri>? {
    try {
        val currentPath = getFileFromUri(context)
        currentPath?.let {
            if (currentPath.exists()) {
                val parent = currentPath.parentFile
                var siblings: ArrayList<Uri>? = null
                parent.listFiles()?.run {
                    if (this.isNotEmpty()) {
                        siblings = ArrayList()
                        for (currentSibling in this) {
                            siblings!!.add(
                                Uri.parse(
                                    if (!currentSibling.path
                                        .startsWith("/")
                                    )
                                        "/${currentSibling.path}"
                                    else currentSibling.path
                                )
                            )
                        }
                    }
                }
                return siblings
            }
        }
    } catch (exception: Exception) {
        log.warn("Failed to get siblings", exception)
        return null
    }
    return null
}

fun Uri.getFileFromUri(context: Context): File? {
    if (this == Uri.EMPTY) {
        return null
    }
    var songFile: File? = null
    if (this.authority != null && this.authority == "com.android.externalstorage.documents") {
        songFile = File(
            Environment.getExternalStorageDirectory(),
            this.path!!.split(":".toRegex(), 2).toTypedArray()[1]
        )
    }
    /*if (songFile == null) {
        val path: String? = getContentResolverFilePathFromUri(context, this)
        if (path != null) songFile = File(path)
    }*/
    if ((songFile == null || !songFile.exists()) && this.path != null) {
        songFile = File(
            this.path?.substring(
                this.path?.indexOf("/", 1)!! + 1
            )
        )
        if (songFile == null || !songFile.exists()) {
            songFile = File(this.path)
        }
    }
    if (songFile == null || !songFile.exists()) {
        return null
    }
    return songFile
}

fun File.getUriFromFile(context: Context): Uri {
    return FileProvider.getUriForFile(context, context.packageName, this)
}

private fun getContentResolverFilePathFromUri(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column
    )
    try {
        cursor = context.contentResolver.query(
            uri, projection, null, null,
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(columnIndex)
        }
    } catch (e: Exception) {
        log.warn("failed to get cursor resolver from task", e)
    } finally {
        cursor?.close()
    }
    return null
}

fun Uri.isImageMimeType(): Boolean {
    return this.path?.endsWith("jpg")!! ||
        this.path?.endsWith("jpe")!! ||
        this.path?.endsWith("jpeg")!! ||
        this.path?.endsWith("jfif")!! ||
        this.path?.endsWith("pjpeg")!! ||
        this.path?.endsWith("pjp")!! ||
        this.path?.endsWith("gif")!! ||
        this.path?.endsWith("png")!! ||
        this.path?.endsWith("svg")!! ||
        this.path?.endsWith("webp")!!
}

fun Uri.isVideoMimeType(): Boolean {
    return this.path?.endsWith("mp4")!! ||
        this.path?.endsWith("mkv")!! ||
        this.path?.endsWith("webm")!! ||
        this.path?.endsWith("mpa")!! ||
        this.path?.endsWith("flv")!! ||
        this.path?.endsWith("mts")!! ||
        this.path?.endsWith("jpgv")!!
}

fun Uri.isAudioMimeType(): Boolean {
    return this.path?.endsWith("mp3")!! ||
        this.path?.endsWith("wav")!! ||
        this.path?.endsWith("ogg")!! ||
        this.path?.endsWith("mp4")!! ||
        this.path?.endsWith("m4a")!! ||
        this.path?.endsWith("fmp4")!! ||
        this.path?.endsWith("flv")!! ||
        this.path?.endsWith("flac")!! ||
        this.path?.endsWith("amr")!! ||
        this.path?.endsWith("aac")!! ||
        this.path?.endsWith("ac3")!! ||
        this.path?.endsWith("eac3")!! ||
        this.path?.endsWith("dca")!! ||
        this.path?.endsWith("opus")!!
}

val Int.dp get() = this / (
    Resources.getSystem().displayMetrics.densityDpi.toFloat() /
        DisplayMetrics.DENSITY_DEFAULT
    )
val Float.dp get() = this / (
    Resources.getSystem().displayMetrics.densityDpi.toFloat() /
        DisplayMetrics.DENSITY_DEFAULT
    )

val Int.px get() = this * Resources.getSystem().displayMetrics.density
val Float.px get() = this * Resources.getSystem().displayMetrics.density

/**
 * Allow null checks on more than one parameters at the same time.
 * Alternative of doing nested p1?.let p2?.let
 */
inline fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    p3: T3?,
    p4: T4?,
    p5: T5?,
    block: (T1, T2, T3, T4, T5) -> R?
): R? {
    return if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null) block(
        p1,
        p2, p3, p4, p5
    ) else null
}

/**
 * Allow null checks on more than one parameters at the same time.
 * Alternative of doing nested p1?.let p2?.let
 */
inline fun <T1 : Any, T2 : Any, T3 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    p3: T3?,
    block: (T1, T2, T3) -> R?
): R? {
    return if (p1 != null && p2 != null && p3 != null) block(
        p1,
        p2, p3
    ) else null
}

/**
 * Allow null checks on more than one parameters at the same time.
 * Alternative of doing nested p1?.let p2?.let
 */
inline fun <T1 : Any, T2 : Any, R : Any> safeLet(
    p1: T1?,
    p2: T2?,
    block: (T1, T2) -> R?
): R? {
    return if (p1 != null && p2 != null) block(
        p1,
        p2
    ) else null
}

fun Context.showToastOnTop(message: String) = Toast.makeText(
    this,
    message, Toast.LENGTH_SHORT
)
    .apply { setGravity(Gravity.TOP, 16.px.toInt(), 0); show() }

fun Context.showToastInCenter(message: String) = Toast.makeText(
    this,
    message, Toast.LENGTH_SHORT
)
    .apply { setGravity(Gravity.CENTER, 0, 0); show() }

fun Context.showToastOnBottom(message: String) = Toast.makeText(
    this,
    message, Toast.LENGTH_SHORT
)
    .apply { setGravity(Gravity.BOTTOM, 0, 0); show() }

fun View.hideFade(duration: Long) {
    this.animate().alpha(0f).duration = duration
    this.visibility = View.GONE
}

fun View.showFade(duration: Long) {
    this.animate().alpha(1f).duration = duration
    this.visibility = View.VISIBLE
}

fun View.hideTranslateY(duration: Long) {
    val animation: Animation = TranslateAnimation(0f, 0f, 0f, this.y)
    animation.duration = duration
//    animation.fillAfter = true
    this.startAnimation(animation)
    this.visibility = View.GONE
}

fun View.showTranslateY(duration: Long) {
    val animation: Animation = TranslateAnimation(0f, 0f, this.y, 0f)
    animation.duration = duration
//    animation.fillAfter = true
    this.startAnimation(animation)
    this.visibility = View.VISIBLE
}

fun Context.getAppCommonSharedPreferences(): SharedPreferences {
    return this.getSharedPreferences(
        PreferencesConstants.PREFERENCE_FILE,
        Context.MODE_PRIVATE
    )
}

fun ImageAnalysis.invalidate(dao: ImageAnalysisDao): Boolean {
    val file = File(filePath)
    return if (!file.exists()) {
        dao.delete(this)
        false
    } else {
        true
    }
}

fun BlurAnalysis.invalidate(dao: BlurAnalysisDao): Boolean {
    val file = File(filePath)
    return if (!file.exists()) {
        dao.delete(this)
        false
    } else {
        true
    }
}

fun MemeAnalysis.invalidate(dao: MemeAnalysisDao): Boolean {
    val file = File(filePath)
    return if (!file.exists()) {
        dao.delete(this)
        false
    } else {
        true
    }
}

fun LowLightAnalysis.invalidate(dao: LowLightAnalysisDao): Boolean {
    val file = File(filePath)
    return if (!file.exists()) {
        dao.delete(this)
        false
    } else {
        true
    }
}

fun InternalStorageAnalysis.invalidate(dao: InternalStorageAnalysisDao): Boolean {
    this.files.forEach {
        val file = File(it)
        if (!file.exists()) {
            dao.delete(this)
            return false
        }
    }
    return true
}

fun Context.getExternalStorageDirectory(): StorageDirectoryParcelable? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileUtils.getStorageDirectoriesNew(applicationContext.applicationContext)
    } else {
        FileUtils.getStorageDirectoriesLegacy(applicationContext.applicationContext)
    }
}

fun Context.showFolderChooserDialog(chooserPath: (file: File) -> Unit) {
    val initialFolder = getExternalStorageDirectory()
    initialFolder?.let {
        val baseFile = File(it.path)
        MaterialDialog(this).show {
            folderChooser(
                this@showFolderChooserDialog,
                baseFile,
            ) { dialog, folder ->
                chooserPath.invoke(folder)
                dialog.dismiss()
            }
        }
    }
}

fun Context.showFileChooserDialog(filter: FileFilter = null, chooserPath: (file: File) -> Unit) {
    val initialFolder = getExternalStorageDirectory()
    initialFolder?.let {
        val baseFile = File(it.path)
        MaterialDialog(this).show {
            fileChooser(
                this@showFileChooserDialog,
                baseFile,
                filter
            ) { dialog, folder ->
                chooserPath.invoke(folder)
                dialog.dismiss()
            }
        }
    }
}

fun String.removeExtension(): String {
    return this.substring(0, this.lastIndexOf("."))
}

fun Context.isPlayServicesAvailable(): Boolean {
    return GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
}
