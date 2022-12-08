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
//        params.topMargin = 8.px.toInt()
//        params.bottomMargin = 16.px.toInt()
        params.weight = 1f
        item.layoutParams = params
    }
}
