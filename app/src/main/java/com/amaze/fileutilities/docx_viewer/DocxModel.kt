package com.amaze.fileutilities.docx_viewer

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.amaze.fileutilities.utilis.BaseIntentModel
import com.amaze.fileutilities.utilis.getFileFromUri
import kotlinx.parcelize.Parcelize
import java.io.InputStream

@Parcelize
data class LocalDocxModel(
    private var uri: Uri,
    val mimeType: String
) : Parcelable, DocxModel {
    override fun getInputStream(context: Context): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    override fun getUri(): Uri {
        return uri
    }

    override fun getName(context: Context): String {
        uri.getFileFromUri(context)?.run {
            return this.name
        }
        uri.path?.run {
            return this
        }
        return uri.toString()
    }
}

interface DocxModel: BaseIntentModel {
    fun getInputStream(context: Context): InputStream?
}