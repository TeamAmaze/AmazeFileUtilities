/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.media_tile

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class ButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    @JvmField
    val headerListButton: Button = view.findViewById(R.id.header_list_button)
}
