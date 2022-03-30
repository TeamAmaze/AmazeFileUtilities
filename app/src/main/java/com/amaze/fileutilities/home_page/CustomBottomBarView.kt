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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px

class CustomBottomBarView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomBottomBarView, 0, 0
        )
        val valueColor = a.getColor(
            R.styleable.CustomBottomBarView_bottomBarBgColor,
            resources.getColor(R.color.translucent_toolbar)
        )
        a.recycle()
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.custom_bottom_bar_translucent, this, true)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(valueColor)
    }

    fun addButton(drawable: Drawable, hint: String, onPress: () -> Unit) {
        val itemView = getItem(drawable, hint)
        itemView.setOnClickListener {
            onPress.invoke()
        }
        addView(itemView)
        invalidate()
    }

    private fun getItem(drawable: Drawable, hint: String): View {
        val itemView = inflate(context, R.layout.bottom_bar_item, null)
        setParams(itemView)
        itemView.findViewById<ImageView>(R.id.item_icon).setImageDrawable(drawable)
        itemView.findViewById<TextView>(R.id.item_hint).text = hint
        return itemView
    }

    private fun setParams(item: View) {
        val params = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.rightMargin = 16.px.toInt()
        params.leftMargin = 16.px.toInt()
        params.topMargin = 8.px.toInt()
        params.bottomMargin = 16.px.toInt()
        params.weight = 1f
        item.layoutParams = params
    }
}
