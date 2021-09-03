package com.amaze.fileutilities

import android.net.Uri
import java.io.File
import java.lang.Exception

fun Uri.getSiblingUriFiles() : ArrayList<Uri>? {
    try {
        val currentPath = File(this.path?.substring(this.path?.indexOf("/", 1)!!+1))
        if (currentPath.exists()) {
            val parent = currentPath.parentFile
            val siblings: ArrayList<Uri> = ArrayList()
            for (currentSibling in parent.listFiles()) {
                siblings.add(Uri.parse(currentSibling.path))
            }
            return siblings
        }
    } catch (exception: Exception) {
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