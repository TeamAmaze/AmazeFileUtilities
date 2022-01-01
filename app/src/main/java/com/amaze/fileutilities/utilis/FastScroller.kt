/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R

class FastScroller : FrameLayout {
    private var bar: View? = null
    private var handle: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private val scrollListener: ScrollListener
    var manuallyChangingPosition = false
    var columns = 1

    private inner class ScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, i: Int, i2: Int) {
            if (handle != null && !manuallyChangingPosition) {
                updateHandlePosition()
            }
        }
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        scrollListener = ScrollListener()
        initialise(context)
    }

    constructor(context: Context, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        scrollListener = ScrollListener()
        initialise(context)
    }

    private fun computeHandlePosition(): Float {
        val firstVisibleView = recyclerView!!.getChildAt(0)
        handle!!.visibility = VISIBLE
        val recyclerViewOversize: Float // how much is recyclerView bigger than fastScroller
        val recyclerViewAbsoluteScroll: Int
        if (firstVisibleView == null || recyclerView == null) return (-1).toFloat()
        recyclerViewOversize = (
            firstVisibleView.height / columns * recyclerView!!.adapter!!
                .itemCount -
                heightMinusPadding
            ).toFloat()
        recyclerViewAbsoluteScroll = (
            recyclerView!!.getChildLayoutPosition(firstVisibleView) /
                columns
                * firstVisibleView.height -
                firstVisibleView.top
            )
        return recyclerViewAbsoluteScroll / recyclerViewOversize
    }

    private val heightMinusPadding: Int
        private get() = height - paddingBottom - paddingTop

    private fun initialise(context: Context) {
        clipChildren = false
        inflate(context, R.layout.fastscroller, this)
        handle = findViewById(R.id.scroll_handle)
        bar = findViewById(R.id.scroll_bar)
        handle?.isEnabled = true
        setPressedHandleColor(context.resources.getColor(R.color.highlight_yellow))
        setUpBarBackground()
        visibility = VISIBLE
    }

    private fun setHandlePosition1(relativePos: Float) {
        handle!!.y = clamp(
            0f,
            (heightMinusPadding - handle!!.height).toFloat(),
            relativePos * (heightMinusPadding - handle!!.height)
        )
    }

    private fun setUpBarBackground() {
        val insetDrawable: InsetDrawable
        val resolveColor = resolveColor(context, R.attr.colorControlNormal)
        insetDrawable = InsetDrawable(
            ColorDrawable(resolveColor),
            resources.getDimensionPixelSize(R.dimen.fastscroller_track_padding),
            0,
            0,
            0
        )
        bar!!.setBackgroundDrawable(insetDrawable)
    }

    fun resolveColor(context: Context, @AttrRes i: Int): Int {
        val obtainStyledAttributes = context.obtainStyledAttributes(intArrayOf(i))
        val color = obtainStyledAttributes.getColor(0, 0)
        obtainStyledAttributes.recycle()
        return color
    }

    var a: onTouchListener? = null
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (motionEvent.action == 0 || motionEvent.action == 2) {
            handle!!.isPressed = true
            bar!!.visibility = VISIBLE
            val relativePos = getRelativeTouchPosition(motionEvent)
            setHandlePosition1(relativePos)
            manuallyChangingPosition = true
            setRecyclerViewPosition(relativePos)
            // showIfHidden();
            if (a != null) a!!.onTouch()
            true
        } else if (motionEvent.action != 1) {
            super.onTouchEvent(motionEvent)
        } else {
            bar!!.visibility = INVISIBLE
            manuallyChangingPosition = false
            handle!!.isPressed = false
            // scheduleHide();
            true
        }
    }

    private fun invalidateVisibility() {
        visibility =
            if (recyclerView!!.adapter == null || recyclerView!!.adapter!!.itemCount == 0 ||
                recyclerView!!.getChildAt(
                        0
                    ) == null || isRecyclerViewScrollable
            ) {
                INVISIBLE
            } else {
                VISIBLE
            }
    }

    private val isRecyclerViewScrollable: Boolean
        private get() = (
            recyclerView!!.getChildAt(0).height
                * recyclerView!!.adapter!!.itemCount /
                columns
                <= heightMinusPadding ||
                recyclerView!!.adapter!!.itemCount / columns < 25
            )

    private fun setRecyclerViewPosition(relativePos: Float) {
        if (recyclerView != null) {
            val itemCount = recyclerView!!.adapter!!.itemCount
            val targetPos = clamp(
                0f, (itemCount - 1).toFloat(),
                relativePos * itemCount.toFloat()
            ).toInt()
            recyclerView!!.scrollToPosition(targetPos)
        }
    }

    private fun getRelativeTouchPosition(event: MotionEvent): Float {
        val yInParent: Float = event.rawY - getViewRawY(handle!!)
        return yInParent / (heightMinusPadding - handle!!.height)
    }

    interface onTouchListener {
        fun onTouch()
    }

    fun registerOnTouchListener(onTouchListener: onTouchListener?) {
        a = onTouchListener
    }

    fun setPressedHandleColor(i: Int) {
        handle!!.setColorFilter(i)
        val stateListDrawable = StateListDrawable()
        val drawable = ContextCompat.getDrawable(
            context,
            R.drawable.fastscroller_handle_normal
        )
        val drawable1 = ContextCompat.getDrawable(
            context,

            R.drawable.fastscroller_handle_pressed
        )
        stateListDrawable.addState(
            PRESSED_ENABLED_STATE_SET,
            InsetDrawable(
                drawable1,
                resources.getDimensionPixelSize(R.dimen.fastscroller_track_padding),
                0,
                0,
                0
            )
        )
        stateListDrawable.addState(
            EMPTY_STATE_SET,
            InsetDrawable(
                drawable,
                resources.getDimensionPixelSize(R.dimen.fastscroller_track_padding),
                0,
                0,
                0
            )
        )
        handle!!.setImageDrawable(stateListDrawable)
    }

    fun setRecyclerView(recyclerView: RecyclerView, columns: Int) {
        this.recyclerView = recyclerView
        this.columns = columns
        bar!!.visibility = INVISIBLE
        recyclerView.addOnScrollListener(scrollListener)
        invalidateVisibility()
        recyclerView.setOnHierarchyChangeListener(
            object : OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {
                    invalidateVisibility()
                }

                override fun onChildViewRemoved(parent: View, child: View) {
                    invalidateVisibility()
                }
            })
    }

    fun updateHandlePosition() {
        setHandlePosition1(computeHandlePosition())
    }

    var vx1 = -1
    fun updateHandlePosition(vx: Int, l: Int) {
        if (vx != vx1) {
            setPadding(paddingLeft, paddingTop, paddingRight, l + vx)
            setHandlePosition1(computeHandlePosition())
            vx1 = vx
        }
    }

    private fun clamp(min: Float, max: Float, value: Float): Float {
        val minimum = Math.max(min, value)
        return Math.min(minimum, max)
    }

    private fun getViewRawY(view: View): Float {
        val location = IntArray(2)
        location[0] = 0
        location[1] = view.y.toInt()
        (view.parent as View).getLocationInWindow(location)
        return location[1].toFloat()
    }
}
