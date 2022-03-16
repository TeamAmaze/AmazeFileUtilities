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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px

class CustomToolbar(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val backButton: ImageView
    private val titleTextView: TextView
    private val overflowButton: ImageView
    private val actionButtonsParent: LinearLayout

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomToolbar, 0, 0
        )
        val titleText = a.getString(R.styleable.CustomToolbar_toolbarTitle)
        val bgColor = a.getColor(
            R.styleable.CustomToolbar_bgColor,
            resources.getColor(R.color.translucent_toolbar)
        )
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.custom_toolbar_view, this, true)
        backButton = getChildAt(0) as ImageView
        titleTextView = getChildAt(1) as TextView
        actionButtonsParent = getChildAt(2) as LinearLayout
        overflowButton = getChildAt(3) as ImageView
        titleTextView.text = titleText
        setBackgroundColor(bgColor)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setBackButtonClickListener(callback: () -> Unit) {
        backButton.setOnClickListener { callback.invoke() }
    }

    fun addActionButton(drawable: Drawable, onPress: () -> Unit) {
        val imageView = getImageView(drawable)
        imageView.setOnClickListener {
            onPress.invoke()
        }
        actionButtonsParent.addView(imageView)
        actionButtonsParent.invalidate()
    }

    fun setOverflowPopup(
        menuRes: Int,
        onMenuItemClickListener: PopupMenu.OnMenuItemClickListener
    ) {
        val overflowContext = ContextThemeWrapper(context, R.style.custom_action_mode_dark)
        val popupMenu = PopupMenu(
            overflowContext, overflowButton
        )
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            onMenuItemClickListener.onMenuItemClick(item)
        }
        popupMenu.inflate(menuRes)
        overflowButton.visibility = View.VISIBLE
        overflowButton.setOnClickListener {
            popupMenu.show()
        }
    }

    fun getOverflowButton(): ImageView {
        return overflowButton
    }

    private fun getImageView(drawable: Drawable): ImageView {
        val imageView = ImageView(context)
        setParams(imageView)
        imageView.setImageDrawable(drawable)
        imageView.setBackgroundColor(resources.getColor(android.R.color.transparent))
        return imageView
    }

    private fun setParams(button: ImageView) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        params.rightMargin = 16.px.toInt()
        params.leftMargin = 16.px.toInt()
        params.topMargin = 16.px.toInt()
        params.bottomMargin = 16.px.toInt()
        params.weight = 1f
        button.layoutParams = params
    }
}
