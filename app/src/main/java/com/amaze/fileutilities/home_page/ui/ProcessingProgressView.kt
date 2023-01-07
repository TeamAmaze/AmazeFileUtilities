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

package com.amaze.fileutilities.home_page.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.amaze.fileutilities.R

class ProcessingProgressView(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    private val progressBar: ProgressBar
    private val imageView: ImageView
    private val textView: TextView

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.processing_progress_layout, this, true)
        progressBar = getChildAt(0) as ProgressBar
        imageView = getChildAt(1) as ImageView
        textView = getChildAt(2) as TextView
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun invalidateProcessing(isProcessing: Boolean, showEmpty: Boolean, infoText: String?) {
        if (isProcessing) {
            visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        } else {
            visibility = View.GONE
            progressBar.visibility = View.GONE
            textView.visibility = View.GONE
            imageView.visibility = View.GONE
        }
        if (showEmpty) {
            visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            textView.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_forest_32))
        }
        textView.text = infoText
    }
}
