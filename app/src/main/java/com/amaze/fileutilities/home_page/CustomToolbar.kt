package com.amaze.fileutilities.home_page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import com.amaze.fileutilities.R

class CustomToolbar(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs)  {

    private val backButton: ImageView
    private val titleTextView: TextView
    private val overflowButton: ImageView

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.custom_toolbar_view, this, true)
        backButton = getChildAt(0) as ImageView
        titleTextView = getChildAt(1) as TextView
        overflowButton = getChildAt(2) as ImageView
        setBackgroundColor(resources.getColor(R.color.translucent_toolbar))
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setBackButtonClickListener(callback: () -> Unit) {
        backButton.setOnClickListener { callback.invoke() }
    }

    fun setOverflowPopup(menuRes: Int,
                         onMenuItemClickListener: PopupMenu.OnMenuItemClickListener) {
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
            popupMenu.dismiss()
            popupMenu.show()
        }
    }
}