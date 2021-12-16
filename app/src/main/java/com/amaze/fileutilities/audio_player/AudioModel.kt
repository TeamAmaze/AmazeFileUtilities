package com.amaze.fileutilities.audio_player

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class represents any filetype that is openable in a [AudioPlayerDialogActivity]
 * and contains all information to show it in one
 *
 */
@Parcelize
data class LocalAudioModel(
    var id: Long,
    private var uri: Uri,
    private val mimeType: String
) : Parcelable, AudioModel {
    override fun getUri(): Uri {
        return uri
    }

    override fun getName(): String {
        return uri.path!!
    }

    override fun getMimeType(): String {
        return mimeType
    }

}

interface AudioModel {
    fun getUri(): Uri
    fun getName(): String
    fun getMimeType(): String
}