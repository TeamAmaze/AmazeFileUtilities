/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

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
