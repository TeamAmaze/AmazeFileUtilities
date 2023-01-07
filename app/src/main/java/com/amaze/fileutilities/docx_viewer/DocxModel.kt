/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    val mimeType: String?
) : Parcelable, DocxModel {
    override fun getInputStream(context: Context): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    override fun getUri(): Uri {
        return uri
    }

    override fun getName(context: Context): String {
        uri.getFileFromUri()?.run {
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
