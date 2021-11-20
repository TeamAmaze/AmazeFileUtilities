package com.amaze.fileutilities.video_player

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class represents any filetype that is openable in a [VideoPlayerDialogActivity]
 * and contains all information to show it in one
 *
 */
@Parcelize
data class LocalVideoModel(
    var uri: Uri,
    val mimeType: String
) : Parcelable