/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.options

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class DonationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    val ROOT_VIEW: LinearLayout = itemView.findViewById(R.id.adapter_donation_root)

    @JvmField
    val TITLE: TextView = itemView.findViewById(R.id.adapter_donation_title)

    @JvmField
    val SUMMARY: TextView = itemView.findViewById(R.id.adapter_donation_summary)

    @JvmField
    val PRICE: TextView = itemView.findViewById(R.id.adapter_donation_price)
}
