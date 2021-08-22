package com.amaze.fileutilities.image_viewer

import android.os.Parcelable
import com.amaze.filemanager.adapters.data.IconDataParcelable
import com.amaze.filemanager.ui.fragments.quickview.HasQuickView
import com.amaze.filemanager.ui.fragments.quickview.QuickViewFragment
import kotlinx.parcelize.Parcelize

/**
 * This class represents any filetype that is openable in a [QuickViewFragment]
 * and contains all information to show it in one
 *
 * Check [HasQuickView] to see if the filetype is openable in a [QuickViewFragment]
 */
@Parcelize
open class QuickViewType(open val name: String) : Parcelable

data class QuickViewImage(
    val reference: IconDataParcelable,
    override val name: String
) : QuickViewType(name)