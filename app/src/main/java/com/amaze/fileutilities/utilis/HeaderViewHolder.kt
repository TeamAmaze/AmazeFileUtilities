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

package com.amaze.fileutilities.utilis

import android.content.Context
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // each data item is just a string in this case
    private val txtTitle: TextView = view.findViewById(R.id.header_title)
    private val summaryTextView: TextView = view.findViewById(R.id.header_summary)
    private val overflowButton: ImageView = view.findViewById(R.id.overflow_button)

    fun setText(headerText: String) {
        txtTitle.text = headerText
        Utils.marqueeAfterDelay(3000, txtTitle)
    }

    fun setSummaryText(summaryText: String) {
        summaryTextView.visibility = View.VISIBLE
        summaryTextView.text = summaryText
    }

    fun setOverflowButtons(
        context: Context,
        menuRes: Int,
        onMenuItemClickListener: PopupMenu.OnMenuItemClickListener
    ) {
        val overflowContext = ContextThemeWrapper(context, R.style.custom_action_mode_dark)
        val popupMenu = androidx.appcompat.widget.PopupMenu(
            overflowContext, overflowButton, Gravity.END
        )
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            onMenuItemClickListener.onMenuItemClick(item)
        }
        popupMenu.menuInflater.inflate(menuRes, popupMenu.menu)
        overflowButton.setOnClickListener {
            popupMenu.show()
        }
    }
}
