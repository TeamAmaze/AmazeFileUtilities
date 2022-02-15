/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.hideFade
import com.amaze.fileutilities.utilis.px
import com.amaze.fileutilities.utilis.showToastInCenter
import com.bumptech.glide.Glide
import java.lang.ref.WeakReference

class AnalysisTypeView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val imagesListScroll: HorizontalScrollView
    private val titleParent: LinearLayout
    private val titleTextView: TextView
    private val titleHint: ImageView
    private val cleanButtonParent: RelativeLayout
    private val imagesListParent: LinearLayout
    private val cleanButton: Button
    private val loadingProgress: ProgressBar
    private val loadingHorizontalScroll: ProgressBar

    companion object {
        private const val PREVIEW_COUNT = 5
    }

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.analysis_type_view, this, true)
        imagesListScroll = getChildAt(0) as HorizontalScrollView
        titleParent = getChildAt(1) as LinearLayout
        titleTextView = titleParent.findViewById(R.id.title)
        titleHint = titleParent.findViewById(R.id.title_hint)
        cleanButtonParent = getChildAt(2) as RelativeLayout
        imagesListParent = imagesListScroll.findViewById(R.id.images_list_parent)
        cleanButton = cleanButtonParent.findViewById(R.id.clean_button)
        loadingProgress = cleanButtonParent.findViewById(R.id.loading_progress)
        loadingHorizontalScroll = imagesListParent.findViewById(R.id.scroll_progress)

        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.AnalysisTypeView, 0, 0
        )
        val titleText = a.getString(R.styleable.AnalysisTypeView_analysisTitle)
        val showPreview = a.getBoolean(R.styleable.AnalysisTypeView_showPreview, false)
        val hintText = a.getString(R.styleable.AnalysisTypeView_hint)
        if (showPreview) {
            imagesListScroll.visibility = View.VISIBLE
        }
        titleTextView.text = titleText
        if (hintText != null) {
            titleHint.visibility = View.VISIBLE
            titleHint.setOnClickListener {
                context.showToastInCenter(hintText)
            }
        }

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(8.px.toInt(), 8.px.toInt(), 8.px.toInt(), 8.px.toInt())
        background = resources.getDrawable(R.drawable.background_curved)
    }

    fun invalidateProgress(doShow: Boolean) {
        loadingProgress.visibility = if (doShow) View.VISIBLE else View.GONE
    }

    fun loadPreviews(mediaFileInfoList: List<MediaFileInfo>) {
        loadingHorizontalScroll.visibility = View.GONE
        if (mediaFileInfoList.isEmpty()) {
            hideFade(300)
        }
        var count =
            if (mediaFileInfoList.size > PREVIEW_COUNT) PREVIEW_COUNT else mediaFileInfoList.size
        while (count -- > 1) {
            val idx = Utils.generateRandom(0, mediaFileInfoList.size - 1)
            val imageView = getImageView(mediaFileInfoList[idx])
            imagesListParent.addView(imageView)
        }
        imagesListParent.addView(getSummaryView(mediaFileInfoList.size))
    }

    private fun getImageView(mediaFileInfo: MediaFileInfo): ImageView {
        val imageView = ImageView(context)
        imageView.setOnClickListener {
            mediaFileInfo.triggerMediaFileInfoAction(WeakReference(context))
        }
        imageView.layoutParams = getParams()
        imageView.scaleType = ImageView.ScaleType.CENTER
        Glide.with(context).load(mediaFileInfo.path).fallback(R.drawable.ic_outline_broken_image_24)
            .placeholder(R.drawable.ic_outline_insert_drive_file_32).into(imageView)
        return imageView
    }

    private fun getSummaryView(count: Int): LinearLayout {
        val textView = TextView(context)
        textView.setTextColor(resources.getColor(R.color.white))
        textView.text = "${resources.getText(R.string.view_all)} $count"
        val view = LinearLayout(context)
        view.layoutParams = getParams()
        view.addView(textView)
        view.setHorizontalGravity(Gravity.CENTER_HORIZONTAL)
        view.setVerticalGravity(Gravity.CENTER_VERTICAL)
        view.background = resources.getDrawable(R.drawable.button_curved_unselected)
        view.setOnClickListener {
            rootView.performClick()
        }
        return view
    }

    private fun getParams(): LayoutParams {
        val params = LayoutParams(
            106.px.toInt(),
            106.px.toInt()
        )
        params.leftMargin = 4.px.toInt()
        return params
    }
}
