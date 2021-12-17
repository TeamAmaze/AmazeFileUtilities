package com.amaze.fileutilities.epub_viewer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocalEpubModel(
    var uri: Uri,
    val mimeType: String
) : Parcelable