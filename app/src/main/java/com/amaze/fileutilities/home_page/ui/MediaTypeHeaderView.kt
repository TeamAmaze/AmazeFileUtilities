/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.amaze.fileutilities.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class MediaTypeHeaderView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val typeImageView: ImageView
    private val infoLayoutParent: LinearLayout
    private val usedSpaceTextView: TextView
    private val progressIndicatorsParent: LinearLayout
    private val mediaProgressIndicator: LinearProgressIndicator
    private val progressPercentTextView: TextView
    private val storageCountsParent: RelativeLayout
    private val itemsCountTextView: TextView
    private val internalStorageTextView: TextView

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.media_type_header_layout, this, true)
        typeImageView = getChildAt(0) as ImageView
        infoLayoutParent = getChildAt(1) as LinearLayout
        usedSpaceTextView = infoLayoutParent.findViewById(R.id.usedSpaceTextView)
        progressIndicatorsParent = infoLayoutParent.findViewById(R.id.progressIndicatorsParent)
        mediaProgressIndicator = progressIndicatorsParent.findViewById(R.id.mediaProgress)
        progressPercentTextView = progressIndicatorsParent
            .findViewById(R.id.progressPercentTextView)
        storageCountsParent = infoLayoutParent.findViewById(R.id.storageCountsParent)
        itemsCountTextView = storageCountsParent.findViewById(R.id.itemsCountTextView)
        internalStorageTextView = storageCountsParent.findViewById(R.id.internalStorageTextView)

        orientation = HORIZONTAL
//        gravity = Gravity.CENTER_VERTICAL

        // init values
        usedSpaceTextView.text = resources.getString(R.string.used_space)
        typeImageView.setColorFilter(context.resources.getColor(R.color.white))
        progressPercentTextView.text = "--"
    }

    fun setProgress(mediaTypeContent: MediaTypeView.MediaTypeContent) {
        mediaTypeContent.run {
            usedSpaceTextView.text = resources.getString(
                R.string.used_space, size
            )
            progressPercentTextView.text = "$progress %"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaProgressIndicator.setProgress(progress, true)
            } else {
                mediaProgressIndicator.progress = progress
            }
            itemsCountTextView.text = resources.getString(
                R.string.num_of_files, itemsCount.toString()
            )
            internalStorageTextView.text = resources.getString(
                R.string.internal_storage_subs, totalSpace
            )
        }
        invalidate()
    }

    fun setTypeImageSrc(imageRes: Drawable) {
        typeImageView.setImageDrawable(imageRes)
    }

    fun setMediaImageSrc(mediaRes: Drawable) {
        background = mediaRes
    }

    fun setHeaderColor(headerColor: Int, headerRes: Int) {
        setBackgroundResource(headerRes)
        mediaProgressIndicator.trackColor = ColorUtils.blendARGB(
            headerColor,
            Color.BLACK, .5f
        )
    }
}
