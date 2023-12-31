/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    val infoSubSummary: TextView = view.findViewById(R.id.info_sub_summary)

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
