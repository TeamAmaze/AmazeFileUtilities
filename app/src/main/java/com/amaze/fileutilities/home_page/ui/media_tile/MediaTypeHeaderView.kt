/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.media_tile

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import android.widget.LinearLayout
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.mediarouter.app.MediaRouteButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileListSorter
import com.amaze.fileutilities.utilis.*
import com.google.android.material.progressindicator.LinearProgressIndicator

class MediaTypeHeaderView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val typeHeaderParent: RelativeLayout
    private val typeImageView: ImageView
    private val mediaRouteButton: MediaRouteButton
    private val infoLayoutParent: LinearLayout
    private val usedSpaceTextView: TextView
    private val usedTotalSpaceTextView: TextView
    private val progressIndicatorsParent: LinearLayout
    private val mediaProgressIndicator: LinearProgressIndicator
    private val progressPercentTextView: TextView
    private val storageCountsParent: RelativeLayout
    private val itemsCountTextView: TextView
    private val totalMediaFiles: TextView
    private val optionsParentLayout: LinearLayout
    private val optionsItemsScroll: HorizontalScrollView
    private val optionsIndexImage: ImageView
    private val optionsSwitchView: ImageView
    private val optionsGroupView: ImageView
    private val optionsSortView: ImageView
    private val optionsListParent: LinearLayout
    private val optionsRecyclerViewParent: FrameLayout
    private val optionsRecyclerView: RecyclerView
    private var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(
        context,
        LinearLayoutManager.HORIZONTAL, false
    )

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.media_type_header_layout, this, true)
        typeHeaderParent = getChildAt(0) as RelativeLayout
        typeImageView = typeHeaderParent.findViewById(R.id.type_image)
        mediaRouteButton = typeHeaderParent.findViewById(R.id.media_route_button)
        infoLayoutParent = getChildAt(1) as LinearLayout
        optionsParentLayout = getChildAt(2) as LinearLayout
        optionsItemsScroll = getChildAt(4) as HorizontalScrollView
        optionsRecyclerViewParent = getChildAt(3) as FrameLayout
        optionsRecyclerView = optionsRecyclerViewParent.findViewById(R.id.options_recycler_view)
        optionsItemsScroll.isHorizontalScrollBarEnabled = false
        usedSpaceTextView = infoLayoutParent.findViewById(R.id.usedSpaceTextView)
        usedTotalSpaceTextView = infoLayoutParent.findViewById(R.id.usedTotalSpaceTextView)
        progressIndicatorsParent = infoLayoutParent.findViewById(R.id.progressIndicatorsParent)
        mediaProgressIndicator = progressIndicatorsParent.findViewById(R.id.mediaProgress)
        progressPercentTextView = progressIndicatorsParent
            .findViewById(R.id.progressPercentTextView)
        storageCountsParent = infoLayoutParent.findViewById(R.id.storageCountsParent)
        itemsCountTextView = storageCountsParent.findViewById(R.id.itemsCountTextView)
        totalMediaFiles = storageCountsParent.findViewById(R.id.internalStorageTextView)
        optionsIndexImage = optionsParentLayout.findViewById(R.id.index_image)
        optionsSwitchView = optionsParentLayout.findViewById(R.id.switch_view)
        optionsGroupView = optionsParentLayout.findViewById(R.id.group_view)
        optionsSortView = optionsParentLayout.findViewById(R.id.sort_view)
        optionsListParent = optionsItemsScroll.findViewById(R.id.options_list_parent)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        // init values
        usedSpaceTextView.text = resources.getString(R.string.used_space)
        usedTotalSpaceTextView.text = resources.getString(R.string.used_total_space)
//        typeImageView.setColorFilter(context.resources.getColor(R.color.white))
        progressPercentTextView.text = "--"
    }

    fun setProgress(mediaTypeContent: MediaTypeView.MediaTypeContent) {
        mediaTypeContent.run {
            usedSpaceTextView.text = resources.getString(
                R.string.used_space, size
            )
            usedTotalSpaceTextView.text = resources.getString(
                R.string.used_total_space, totalUsedSpace
            )
            progressPercentTextView.text = "$progress %"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaProgressIndicator.setProgress(progress, true)
            } else {
                mediaProgressIndicator.progress = progress
            }
            itemsCountTextView.text = resources.getString(
                R.string.num_of_files, String.format("%,d", itemsCount)
            )
            totalMediaFiles.text = resources.getString(
                R.string.media_files_subs, String.format("%,d", totalItemsCount)
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

    fun getMediaRouteButton(): MediaRouteButton {
        return mediaRouteButton
    }

    fun setHeaderColor(headerColor: Int, headerRes: Int) {
//        setBackgroundResource(headerRes)
        setBackgroundResource(R.drawable.background_curved)
        mediaProgressIndicator.trackColor = ColorUtils.blendARGB(
            headerColor,
            Color.BLACK, .2f
        )
    }

    fun initOptionsItems(
        optionsMenuSelected: MediaFileAdapter.OptionsMenuSelected,
        headerListItems: MutableList<AbstractMediaFilesAdapter.ListItem>,
        sortingPreference: MediaFileListSorter.SortingPreference,
        mediaListType: Int
    ) {
        val adapter = MediaTypeViewOptionsListAdapter(
            context, headerListItems,
            optionsMenuSelected
        )
        optionsRecyclerView.layoutManager = linearLayoutManager
        optionsRecyclerView.adapter = adapter
        clickOptionsIndex()
        val sharedPreferences = context.getAppCommonSharedPreferences()
        optionsIndexImage.setOnClickListener {
            clickOptionsIndex()
        }
        optionsSwitchView.setOnClickListener {
            clickOptionsSwitchView(optionsMenuSelected, sharedPreferences, mediaListType)
        }
        optionsGroupView.setOnClickListener {
            clickOptionsGroupView(
                optionsMenuSelected, sharedPreferences, sortingPreference,
                mediaListType
            )
        }
        optionsSortView.setOnClickListener {
            clickOptionsSortView(
                optionsMenuSelected, sharedPreferences, sortingPreference,
                mediaListType
            )
        }
    }

    private fun clickOptionsIndex() {
        clearOptionItemsBackgrounds()
        optionsItemsScroll.hideFade(200)
        optionsRecyclerViewParent.showFade(300)
        optionsIndexImage.background = resources.getDrawable(R.drawable.button_selected_dark)
    }

    private fun clickOptionsSwitchView(
        optionsMenuSelected: MediaFileAdapter.OptionsMenuSelected,
        sharedPreferences: SharedPreferences,
        mediaListType: Int
    ) {
        clearOptionItemsBackgrounds()
        optionsRecyclerViewParent.hideFade(300)
        optionsItemsScroll.showFade(200)
        optionsSwitchView.background = resources.getDrawable(R.drawable.button_selected_dark)
        val listViewButton: Button
        val gridViewButton: Button
        if (sharedPreferences.getBoolean(
                "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_TYPE}",
                PreferencesConstants.DEFAULT_MEDIA_LIST_TYPE
            )
        ) {
            optionsSwitchView.setImageDrawable(
                resources
                    .getDrawable(R.drawable.ic_round_view_list_32)
            )
            listViewButton = getSelectedTextButton(resources.getString(R.string.list_view))
            gridViewButton = getUnSelectedTextButton(resources.getString(R.string.grid_view))
        } else {
            optionsSwitchView.setImageDrawable(
                resources
                    .getDrawable(R.drawable.ic_round_grid_on_32)
            )
            listViewButton = getUnSelectedTextButton(resources.getString(R.string.list_view))
            gridViewButton = getSelectedTextButton(resources.getString(R.string.grid_view))
        }

        listViewButton.setOnClickListener {
            setSelectButton(listViewButton)
            setUnSelectButton(gridViewButton)
            sharedPreferences.edit {
                this.putBoolean(
                    "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_TYPE}",
                    true
                ).apply()
            }
            optionsMenuSelected.switchView(true)
        }
        gridViewButton.setOnClickListener {
            setSelectButton(gridViewButton)
            setUnSelectButton(listViewButton)
            sharedPreferences.edit {
                this.putBoolean(
                    "${mediaListType}_${PreferencesConstants.KEY_MEDIA_LIST_TYPE}",
                    false
                ).apply()
            }
            optionsMenuSelected.switchView(false)
        }

        optionsListParent.addView(listViewButton)
        optionsListParent.addView(gridViewButton)
    }

    private fun clickOptionsGroupView(
        optionsMenuSelected: MediaFileAdapter.OptionsMenuSelected,
        sharedPreferences: SharedPreferences,
        sortingPreference: MediaFileListSorter.SortingPreference,
        mediaListType: Int
    ) {
        clearOptionItemsBackgrounds()
        optionsRecyclerViewParent.hideFade(300)
        optionsItemsScroll.showFade(200)
        optionsGroupView.background = resources.getDrawable(R.drawable.button_selected_dark)

        val buttonsList = ArrayList<Button>()
        var isAsc = sharedPreferences
            .getBoolean(
                MediaFileListSorter.SortingPreference.getIsGroupByAscKey(mediaListType),
                PreferencesConstants.DEFAULT_MEDIA_LIST_GROUP_BY_ASC
            )
        MediaFileListSorter.GROUP_BY_MEDIA_TYPE_MAP[mediaListType]?.forEach {
            groupByType ->
            val button = if (groupByType == sortingPreference.groupBy) {
                getSelectedTextButton(
                    MediaFileListSorter.getGroupNameByType(
                        groupByType,
                        resources
                    )
                )
            } else {
                getUnSelectedTextButton(
                    MediaFileListSorter.getGroupNameByType(
                        groupByType,
                        resources
                    )
                )
            }
            button.setOnClickListener {
                buttonsList.forEach {
                    allButtons ->
                    setUnSelectButton(allButtons)
                }
                setSelectButton(button)

                sharedPreferences.edit {
                    this.putInt(
                        MediaFileListSorter.SortingPreference.getGroupByKey(mediaListType),
                        groupByType
                    ).apply()
                }
                sortingPreference.groupBy = groupByType
                isAsc = !isAsc
                sharedPreferences.edit().putBoolean(
                    MediaFileListSorter.SortingPreference.getIsGroupByAscKey(mediaListType),
                    isAsc
                ).apply()
                sortingPreference.isGroupByAsc = isAsc
                optionsMenuSelected.groupBy(sortingPreference)
            }
            buttonsList.add(button)
        }
        buttonsList.forEach {
            optionsListParent.addView(it)
        }
    }

    private fun clickOptionsSortView(
        optionsMenuSelected: MediaFileAdapter.OptionsMenuSelected,
        sharedPreferences: SharedPreferences,
        sortingPreference: MediaFileListSorter.SortingPreference,
        mediaListType: Int
    ) {
        clearOptionItemsBackgrounds()
        optionsRecyclerViewParent.hideFade(300)
        optionsItemsScroll.showFade(200)
        optionsSortView.background = resources.getDrawable(R.drawable.button_selected_dark)

        val buttonsList = ArrayList<Button>()
        var isAsc = sharedPreferences
            .getBoolean(
                MediaFileListSorter.SortingPreference.getIsSortByAscKey(mediaListType),
                PreferencesConstants.DEFAULT_MEDIA_LIST_SORT_BY_ASC
            )
        MediaFileListSorter.SORT_BY_MEDIA_TYPE_MAP[mediaListType]?.forEach {
            sortByType ->
            val button = if (sortByType == sortingPreference.sortBy) {
                getSelectedTextButton(
                    MediaFileListSorter.getSortNameByType(
                        sortByType,
                        resources
                    )
                )
            } else {
                getUnSelectedTextButton(
                    MediaFileListSorter.getSortNameByType(
                        sortByType,
                        resources
                    )
                )
            }
            button.setOnClickListener {
                buttonsList.forEach {
                    allButtons ->
                    setUnSelectButton(allButtons)
                }
                setSelectButton(button)

                sharedPreferences.edit {
                    this.putInt(
                        MediaFileListSorter.SortingPreference.getSortByKey(mediaListType),
                        sortByType
                    ).apply()
                }
                sortingPreference.sortBy = sortByType
                isAsc = !isAsc
                sharedPreferences.edit().putBoolean(
                    MediaFileListSorter.SortingPreference.getIsSortByAscKey(mediaListType),
                    isAsc
                ).apply()
                sortingPreference.isSortByAsc = isAsc
                optionsMenuSelected.sortBy(sortingPreference)
            }
            buttonsList.add(button)
        }
        buttonsList.forEach {
            optionsListParent.addView(it)
        }
    }

    private fun getSelectedTextButton(text: String): Button {
        val button = Button(context)
        setSelectButton(button)
        setParams(button)
        button.text = text
        return button
    }

    private fun getUnSelectedTextButton(text: String): Button {
        val button = Button(context)
        setUnSelectButton(button)
        setParams(button)
        button.text = text
        return button
    }

    private fun setParams(button: Button) {
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 16.px.toInt()
        button.layoutParams = params
    }

    private fun setSelectButton(button: Button) {
        button.background = resources.getDrawable(R.drawable.button_curved_selected)
        button.setTextColor(resources.getColor(R.color.navy_blue))
    }

    private fun setUnSelectButton(button: Button) {
        button.background = resources.getDrawable(R.drawable.button_curved_unselected)
        button.setTextColor(resources.getColor(R.color.white))
    }

    private fun clearOptionItemsBackgrounds() {
        optionsIndexImage.background = null
        optionsListParent.removeAllViews()
        optionsSwitchView.background = null
        optionsGroupView.background = null
        optionsSortView.background = null
    }
}
