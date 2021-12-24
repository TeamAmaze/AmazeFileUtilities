package com.amaze.fileutilities.utilis

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StorageDirectoryParcelable(val path: String, val name: String): Parcelable