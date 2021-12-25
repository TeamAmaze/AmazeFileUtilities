/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

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

interface DocxModel : BaseIntentModel {
    fun getInputStream(context: Context): InputStream?
}
