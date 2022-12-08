/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.video_player

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import com.amaze.fileutilities.R

class LanguageSelectionAdapter(
    val appContext: Context,
    val listState: List<SubtitleLanguageAndCode>
) :
    ArrayAdapter<LanguageSelectionAdapter.SubtitleLanguageAndCode?>(appContext, 0, listState) {
    private var isFromView = false
    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    fun getCheckedList(): List<SubtitleLanguageAndCode> {
        return listState.filter { it.isSelected }
    }

    private fun getCustomView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        var convertView: View? = convertView
        val holder: ViewHolder
        if (convertView == null) {
            val layoutInflator = LayoutInflater.from(context)
            convertView = layoutInflator.inflate(R.layout.language_selection_row_item, null)
            holder = ViewHolder()
            holder.mTextView = convertView
                .findViewById(R.id.text)
            holder.mCheckBox = convertView
                .findViewById(R.id.checkbox)
            holder.languageSelectionParent = convertView
                .findViewById(R.id.language_selection_parent)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.mTextView?.text = listState[position].title

        // To check weather checked event fire from getview() or user input
        isFromView = true
        holder.mCheckBox!!.isChecked = listState[position].isSelected
        isFromView = false
        if (listState.isNotEmpty() && position == 0) {
            holder.mCheckBox?.visibility = View.INVISIBLE
            listState[position].isSelected = false
        } else {
            holder.mCheckBox?.visibility = View.VISIBLE
        }
        holder.mCheckBox?.tag = position
        holder.mCheckBox?.setOnClickListener { buttonView ->
            if (!isFromView) {
                listState[position].isSelected = !listState[position].isSelected
                holder.mCheckBox?.isChecked = listState[position].isSelected
            }
        }
        holder.languageSelectionParent?.setOnClickListener {
            if (!isFromView) {
                listState[position].isSelected = !listState[position].isSelected
                holder.mCheckBox?.isChecked = listState[position].isSelected
            }
        }
        return convertView!!
    }

    private inner class ViewHolder {
        var mTextView: TextView? = null
        var mCheckBox: CheckBox? = null
        var languageSelectionParent: RelativeLayout? = null
    }

    data class SubtitleLanguageAndCode(
        var title: String,
        val code: String,
        var isSelected: Boolean = false
    )
}
