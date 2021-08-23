package com.amaze.fileutilities.image_viewer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class represents any filetype that is openable in a [ImageViewerActivity]
 * and contains all information to show it in one
 *
 */
@Parcelize
data class LocalImageModel(
    var uri: Uri,
    val mimeType: String
) : Parcelable