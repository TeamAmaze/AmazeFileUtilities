/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px

class CustomBottomBarView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.custom_bottom_bar_translucent, this, true)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
//        setBackgroundColor(resources.getColor(R.color.translucent_toolbar))
    }

    fun addButton(drawable: Drawable, onPress: () -> Unit) {
        val imageView = getImageView(drawable)
        imageView.setOnClickListener {
            onPress.invoke()
        }
        addView(imageView)
        invalidate()
    }

    private fun getImageView(drawable: Drawable): ImageView {
        val imageView = ImageView(context)
        setParams(imageView)
        imageView.setImageDrawable(drawable)
        imageView.setBackgroundColor(resources.getColor(android.R.color.transparent))
        return imageView
    }

    private fun setParams(button: ImageView) {
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        params.rightMargin = 16.px.toInt()
        params.leftMargin = 16.px.toInt()
        params.topMargin = 16.px.toInt()
        params.bottomMargin = 16.px.toInt()
        params.weight = 1f
        button.layoutParams = params
    }
}
