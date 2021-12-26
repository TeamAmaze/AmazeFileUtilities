package com.amaze.fileutilities.home_page.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.utilis.px
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
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.MediaTypeHeaderView, 0, 0
        )
        val typeImageSrc = a.getDrawable(R.styleable.MediaTypeHeaderView_typeImageSrc)
        val mediaSrc = a.getDrawable(R.styleable.MediaTypeHeaderView_backgroundImageSrc)
        val themeColor = a.getColor(
            R.styleable.MediaTypeHeaderView_headerThemeColor,
            resources.getColor(R.color.blue)
        )
        a.recycle()
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.media_type_header_layout, this, true)
        typeImageView = getChildAt(0) as ImageView
        infoLayoutParent = getChildAt(1) as LinearLayout
        usedSpaceTextView = infoLayoutParent.findViewById(R.id.usedSpaceTextView)
        progressIndicatorsParent = infoLayoutParent.findViewById(R.id.progressIndicatorsParent)
        mediaProgressIndicator = progressIndicatorsParent.findViewById(R.id.mediaProgress)
        progressPercentTextView = progressIndicatorsParent.findViewById(R.id.progressPercentTextView)
        storageCountsParent = infoLayoutParent.findViewById(R.id.storageCountsParent)
        itemsCountTextView = storageCountsParent.findViewById(R.id.itemsCountTextView)
        internalStorageTextView = storageCountsParent.findViewById(R.id.internalStorageTextView)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = context.resources.getDrawable(R.drawable.background_curved)
//        setPadding(16.px.toInt(), 16.px.toInt(), 16.px.toInt(), 16.px.toInt())

        typeImageView.setImageDrawable(typeImageSrc)
        typeImageView.setColorFilter(themeColor)
//        mediaTitleTextView.setTextColor(themeColor)
//        mediaProgressIndicator.setIndicatorColor(themeColor)

        // init values
        usedSpaceTextView.text = resources.getString(R.string.used_space)

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
}