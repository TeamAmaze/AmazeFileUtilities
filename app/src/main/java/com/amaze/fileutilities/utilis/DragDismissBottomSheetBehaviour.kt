/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.abs

class DragDismissBottomSheetBehaviour<V : View?>(
    val context: Context,
    @Nullable attrs: AttributeSet?
) : BottomSheetBehavior<V>(context, attrs) {
    // we'll use the device's touch slop value to find out when a tap
    // becomes a scroll by checking how far the finger moved to be
    // considered a scroll. if the finger moves more than the touch
    // slop then it's a scroll, otherwise it is just a tap and we
    // ignore the touch events
    private var proxyView: View? = null
    private var dragToDismissCallback: (() -> Unit)? = null
    private var dragToDismissRestored: (() -> Unit)? = null
    private var dragToDismissInvoked = false
    private val touchSlop: Int
    private var initialY = 0f

    private var downX = 0f
    private var downY = 0f
    private var originalImageX = 0f
    private var originalImageY = 0f
    private val verticalSwipeThreshold = 50
    private val verticalFinishActivityThreshold = 500

    private var ignoreUntilClose = false
    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {

        // touch events are ignored if the bottom sheet is already
        // open and we save that state for further processing
        if (state == STATE_EXPANDED) {
            ignoreUntilClose = true
            return super.onInterceptTouchEvent(parent, child, event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialY = Math.abs(event.rawY)
                downX = event.x
                downY = event.y
                return super.onInterceptTouchEvent(parent, child, event)
            }
            MotionEvent.ACTION_MOVE -> {
                val x2 = event.x
                val y2 = event.y
                val diffX = (x2 - downX).toLong()
                val diffY = (y2 - downY).toLong()

                if (diffY > 0) {
                    // swipe going down, allow for some time till threshold is reached
                    if (abs(diffY) > verticalFinishActivityThreshold) {
                        // finish activity
                        proxyView?.alpha = 0f
                        proxyView?.visibility = View.INVISIBLE
                        (context as AppCompatActivity).finish()
                    } else {
                        proxyView?.let {
                            proxyView ->
                            proxyView.x = originalImageX + diffX
                            proxyView.y = originalImageY + diffY
                            proxyView.alpha = (
                                1f - (
                                    diffY.toFloat() /
                                        verticalFinishActivityThreshold.toFloat()
                                    )
                                )
                            if (!dragToDismissInvoked) {
                                dragToDismissCallback?.invoke()
                                dragToDismissInvoked = true
                            }
                        }
                    }
                    return super.onInterceptTouchEvent(parent, child, event)
                } else {
                    val result = (
                        !ignoreUntilClose &&
                            Math.abs(initialY - Math.abs(event.rawY)) > touchSlop ||
                            super.onInterceptTouchEvent(parent, child, event)
                        )
                    if (result) {
                        dragToDismissRestored?.invoke()
                    }
                    return result
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                initialY = 0f
                ignoreUntilClose = false
                proxyView?.let {
                    proxyView ->
                    proxyView.x = originalImageX
                    proxyView.y = originalImageY
                    proxyView.alpha = 1f
                    if (dragToDismissInvoked) {
                        dragToDismissRestored?.invoke()
                        dragToDismissInvoked = false
                    }
                }
                return super.onInterceptTouchEvent(parent, child, event)
            }
        }
        return super.onInterceptTouchEvent(parent, child, event)
    }

    fun setProxyView(
        proxyView: View?,
        dragToDismissInvoked: () -> Unit,
        dragToDismissRestored: () -> Unit
    ) {
        this.proxyView = proxyView
        this.dragToDismissCallback = dragToDismissInvoked
        this.dragToDismissRestored = dragToDismissRestored
        originalImageX = proxyView?.x ?: 0f
        originalImageY = proxyView?.y ?: 0f
    }

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }
}
