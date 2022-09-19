/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class MediaInfoRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    @JvmField
    val iconView: ImageView = view.findViewById(R.id.icon_view)

    @JvmField
    val infoTitle: TextView = view.findViewById(R.id.info_title)

    @JvmField
    val infoSummary: TextView = view.findViewById(R.id.info_summary)

    @JvmField
    val extraInfo: TextView = view.findViewById(R.id.extra_info)

    @JvmField
    val checkIconGrid: ImageView = view.findViewById(R.id.check_icon_grid)

    @JvmField
    val expand: ImageView = view.findViewById(R.id.expand)

    @JvmField
    val root: RelativeLayout = view.findViewById(R.id.row_layout_parent)

    @JvmField
    val currentPlayingImageView: ImageView = view.findViewById(R.id.currentPlayingAnimation)
}
