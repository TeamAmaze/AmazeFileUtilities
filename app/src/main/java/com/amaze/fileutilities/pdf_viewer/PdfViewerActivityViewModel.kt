/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.pdf_viewer

import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.amaze.fileutilities.utilis.ImgUtils
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    fun getCurrentPageText(bitmap: Bitmap, externalDirPath: String): LiveData<String?> {
        return liveData(context = viewModelScope.coroutineContext + Dispatchers.Default) {
            emit(null)
            val tessBaseAPI = ImgUtils.getTessInstance(
                ImgUtils.convertMatToBitmap(
                    ImgUtils.processPdfImg(ImgUtils.convertBitmapToMat(bitmap)))!!,
                externalDirPath)
            val extractedText: String? = tessBaseAPI?.getUTF8Text()
            tessBaseAPI?.end()
            emit(extractedText)
        }
    }
}
