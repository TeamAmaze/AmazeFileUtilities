/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
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
        } else if (listState.isNotEmpty() && position == 1) {
            holder.mCheckBox?.isChecked = true
            listState[position].isSelected = true
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
