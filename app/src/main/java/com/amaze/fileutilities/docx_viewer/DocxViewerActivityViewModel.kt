/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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

import android.content.Intent
import androidx.lifecycle.ViewModel

class DocxViewerActivityViewModel : ViewModel() {
    var nightMode = false
    private var docxModel: LocalDocxModel? = null

    fun getDocxModel(intent: Intent?): LocalDocxModel? {
        if (docxModel == null) {
            intent?.let {
                val mimeType = intent.type
                val docxUri = intent.data ?: return null
                docxModel = LocalDocxModel(docxUri, mimeType)
            }
        }
        return docxModel
    }
}
