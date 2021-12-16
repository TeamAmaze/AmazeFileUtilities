package com.amaze.fileutilities.pdf_viewer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalPdfModel(
    var uri: Uri,
    val mimeType: String
) : Parcelable