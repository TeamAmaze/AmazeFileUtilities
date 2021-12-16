package com.amaze.fileutilities.utilis

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.lang.Exception

fun Uri.getSiblingUriFiles(context: Context) : ArrayList<Uri>? {
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
                            siblings!!.add(Uri.parse(if (!currentSibling.path.startsWith("/"))
                                "/${currentSibling.path}"
                            else currentSibling.path))
                        }
                    }
                }
                return siblings
            }
        }
    } catch (exception: Exception) {
        Log.w(javaClass.simpleName, "Failed to get siblings", exception)
        return null
    }
    return null
}

fun Uri.getFileFromUri(context: Context): File? {
    var songFile: File? = null
    if (this.authority != null && this.authority == "com.android.externalstorage.documents") {
        songFile = File(
            Environment.getExternalStorageDirectory(),
            this.path!!.split(":".toRegex(), 2).toTypedArray()[1]
        )
    }
    if (songFile == null) {
        val path: String? = getContentResolverFilePathFromUri(context, this)
        if (path != null) songFile = File(path)
    }
    if (songFile == null && this.path != null) {
        songFile = File(this.path?.substring(this.path?.indexOf("/", 1)!!+1))
//        songFile = File(this.path)
    }
    return songFile
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
        e.printStackTrace()
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