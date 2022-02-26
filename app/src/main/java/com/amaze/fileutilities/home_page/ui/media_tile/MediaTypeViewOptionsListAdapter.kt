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

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter

class MediaTypeViewOptionsListAdapter(
    val context: Context,
    private val headerListItems: List<AbstractMediaFilesAdapter.ListItem>,
    private val optionsMenuSelected:
        MediaFileAdapter.OptionsMenuSelected
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mInflater: LayoutInflater
        get() = context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(
            R.layout.button_view_holder, parent,
            false
        )
        return ButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ButtonViewHolder) {
            holder.headerListButton.text = headerListItems[position].header
            holder.headerListButton.setOnClickListener {
                optionsMenuSelected.select(headerListItems[position].position)
            }
        }
    }

    override fun getItemCount(): Int {
        return headerListItems.size
    }
}
