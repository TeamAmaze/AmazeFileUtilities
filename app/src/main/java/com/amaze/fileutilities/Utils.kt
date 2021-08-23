package com.amaze.fileutilities

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorRes
import android.provider.MediaStore




class Utils {

    companion object {

        fun getPathFromUri(context: Context, uri: Uri) : String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.apply {
                val columnIndex: Int = this.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                this.moveToFirst()
                val s: String = this.getString(columnIndex)
                this.close()
                return s
            }
            return null
        }
    }

}