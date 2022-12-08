/*
 * Copyright (C) 2014-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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

import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

/**
 * General purpose fragment that supports show / hide system bars on top
 */
abstract class AbstractMediaFragment : Fragment() {

    companion object {
        const val ANIM_FADE = 500L
    }

    abstract fun getRootLayout(): View

    abstract fun getToolbarLayout(): View?

    abstract fun getBottomBarLayout(): View?

    /**
     * Do hide or show layouts
     * @param performActionOnSystemBars whether to perform action on status bar,
     * because that may cause screen ui to refresh
     */
    fun refactorSystemUi(hide: Boolean, performActionOnSystemBars: Boolean) {
        if (hide) {
            if (performActionOnSystemBars) {
                WindowInsetsControllerCompat(
                    requireActivity().window,
                    getRootLayout()
                ).let {
                    controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            getToolbarLayout()?.hideFade(ANIM_FADE)
            getBottomBarLayout()?.hideFade(ANIM_FADE)
        } else {
            if (performActionOnSystemBars) {
                WindowInsetsControllerCompat(
                    requireActivity().window,
                    getRootLayout()
                ).let {
                    controller ->
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_BARS_BY_TOUCH
                }
            }
            getToolbarLayout()?.showFade(ANIM_FADE)
            getBottomBarLayout()?.showFade(ANIM_FADE)
        }
    }
}
