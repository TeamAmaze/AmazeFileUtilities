package com.amaze.fileutilities.pdf_viewer

import androidx.lifecycle.ViewModel

class PdfViewerActivityViewModel: ViewModel() {

    var pageNumber = 0
    var pdfFileName: String? = null
    var nightMode = false
}