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

package com.amaze.fileutilities.pdf_viewer

import android.content.Intent
import androidx.lifecycle.ViewModel

class PdfViewerActivityViewModel : ViewModel() {

    var pageNumber = 0
    var pdfFileName: String? = null
    var nightMode = false
    private var pdfModel: LocalPdfModel? = null

    fun getPdfModel(intent: Intent?): LocalPdfModel? {
        if (pdfModel == null) {
            intent?.let {
                val mimeType = intent.type
                val pdfUri = intent.data ?: return null
                pdfModel = LocalPdfModel(uri = pdfUri, mimeType = mimeType)
            }
        }
        return pdfModel
    }

    /*fun getCurrentPageText(bitmap: Bitmap, externalDirPath: String): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val tessBaseAPI = ImgUtils.getTessInstance(
                ImgUtils.convertMatToBitmap(
                    ImgUtils.processPdfImg(ImgUtils.convertBitmapToMat(bitmap))
                )!!,
                externalDirPath
            )
            val extractedText: String? = tessBaseAPI?.getUTF8Text()
            emit(extractedText)
        }
    }*/
}
