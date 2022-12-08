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
