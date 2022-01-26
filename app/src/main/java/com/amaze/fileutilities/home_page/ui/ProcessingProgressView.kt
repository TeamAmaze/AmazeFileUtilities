/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
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
