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

package com.amaze.fileutilities.home_page.ui.media_tile

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px
import com.google.android.material.progressindicator.LinearProgressIndicator

class MediaTypeView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val typeImageView: ImageView
    private val mediaTitleTextView: TextView
    private val mediaSummaryTextView: TextView
    private val mediaProgressParent: LinearLayout
    private val mediaProgressIndicator: LinearProgressIndicator
    private val progressPercentTextView: TextView

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.MediaTypeView, 0, 0
        )
        val titleText = a.getString(R.styleable.MediaTypeView_title)
        val mediaSrc = a.getDrawable(R.styleable.MediaTypeView_mediaImageSrc)
        val themeColor = a.getColor(
            R.styleable.MediaTypeView_themeColor,
            resources.getColor(R.color.blue)
        )
        a.recycle()
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.media_type_layout, this, true)
        typeImageView = getChildAt(0) as ImageView
        mediaTitleTextView = getChildAt(1) as TextView
        mediaSummaryTextView = getChildAt(2) as TextView
        mediaProgressParent = getChildAt(3) as LinearLayout
        mediaProgressIndicator = mediaProgressParent.findViewById(R.id.media_progress)
        progressPercentTextView = mediaProgressParent.findViewById(R.id.progress_percent)

        typeImageView.setImageDrawable(mediaSrc)
        typeImageView.setColorFilter(themeColor)
        mediaTitleTextView.text = titleText
//        mediaTitleTextView.setTextColor(themeColor)
        mediaProgressIndicator.setIndicatorColor(themeColor)

        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        background = context.resources.getDrawable(R.drawable.background_curved)
        setPadding(16.px.toInt(), 16.px.toInt(), 16.px.toInt(), 16.px.toInt())

        // init values
        mediaSummaryTextView.text = resources.getString(R.string.calculating)
        progressPercentTextView.text = "--"
    }

    fun setProgress(mediaTypeContent: MediaTypeContent) {
        mediaTypeContent.run {
            mediaSummaryTextView.text = resources.getString(
                R.string.num_of_files,
                String.format("%,d", itemsCount)
            )
            progressPercentTextView.text = "$progress %"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaProgressIndicator.setProgress(progress, true)
            } else {
                mediaProgressIndicator.progress = progress
            }
        }
        invalidate()
    }

    fun setItemsAndHideProgress(mediaTypeContent: MediaTypeContent) {
        mediaTypeContent.run {
            mediaSummaryTextView.text = resources.getString(
                R.string.num_of_files,
                String.format("%,d", itemsCount)
            )
            progressPercentTextView.visibility = View.GONE
            mediaProgressIndicator.visibility = View.GONE
        }
        invalidate()
    }

    data class MediaTypeContent(
        val itemsCount: Int,
        val size: String,
        val progress: Int,
        var totalUsedSpace: String? = "",
        var totalItemsCount: Int? = 0
    )
}
