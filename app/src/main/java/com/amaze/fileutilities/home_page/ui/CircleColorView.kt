package com.amaze.fileutilities.home_page.ui

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.amaze.fileutilities.R

class CircleColorView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val labeltext: TextView
    private val labelColor: View

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.CircleColorView, 0, 0
        )
        val titleText = a.getString(R.styleable.CircleColorView_titleText)
        val titleTextSize = a.getDimensionPixelSize(
            R.styleable.CircleColorView_titleTextSize,
            resources.getDimension(R.dimen.twelve_sp).toInt()
        )
        val valueColor = a.getColor(
            R.styleable.CircleColorView_labelColor,
            resources.getColor(R.color.blue)
        )
        val titleTextColor = a.getColor(
            R.styleable.CircleColorView_titleTextColor,
            resources.getColor(R.color.white_translucent_2)
        )
        a.recycle()
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.legend_layout, this, true)
        labelColor = getChildAt(0)
        labeltext = getChildAt(1) as TextView
        labeltext.setTextColor(titleTextColor)
        labeltext.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize.toFloat())
        setColorAndLabel(valueColor, titleText ?: "")
        orientation = HORIZONTAL
    }

    fun setColorAndLabel(@ColorInt color: Int, text: String) {
        labelColor.background.setColorFilter(color, PorterDuff.Mode.ADD);
        labeltext.text = text
        invalidate()
    }
}