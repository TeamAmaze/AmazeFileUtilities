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
import android.view.View
import androidx.annotation.Nullable
import androidx.coordinatorlayout.widget.CoordinatorLayout

class CustomCoordinatorLayout : CoordinatorLayout {
    private var proxyView: View? = null

    constructor(context: Context) : super(context) {}
    constructor(
        context: Context,
        @Nullable attrs: AttributeSet?
    ) : super(context, attrs)

    constructor(
        context: Context,
        @Nullable attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    override fun isPointInChildBounds(
        child: View,
        x: Int,
        y: Int
    ): Boolean {
        if (super.isPointInChildBounds(child, x, y)) {
            return true
        }

        // we want to intercept touch events if they are
        // within the proxy view bounds, for this reason
        // we instruct the coordinator layout to check
        // if this is true and let the touch delegation
        // respond to that result
        return if (proxyView != null) {
            super.isPointInChildBounds(proxyView!!, x, y)
        } else false
    }

    // for this example we are only interested in intercepting
    // touch events for a single view, if more are needed use
    // a List<View> viewList instead and iterate in
    // isPointInChildBounds
    fun setProxyView(proxyView: View?) {
        this.proxyView = proxyView
    }
}
