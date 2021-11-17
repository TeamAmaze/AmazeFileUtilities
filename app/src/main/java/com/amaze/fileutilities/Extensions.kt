package com.amaze.fileutilities

import android.net.Uri
import android.util.Log
import java.io.File
import java.lang.Exception

fun Uri.getSiblingUriFiles() : ArrayList<Uri>? {
    try {
        val currentPath = File(this.path?.substring(this.path?.indexOf("/", 1)!!+1))
        if (currentPath.exists()) {
            val parent = currentPath.parentFile
            var siblings: ArrayList<Uri>? = null
            parent.listFiles()?.run {
                if (this.isNotEmpty()) {
                    siblings = ArrayList()
                    for (currentSibling in this) {
                        siblings!!.add(Uri.parse(currentSibling.path))
                    }
                }
            }
            return siblings
        }
    } catch (exception: Exception) {
        Log.w(javaClass.simpleName, "Failed to get siblings", exception)
        return null
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