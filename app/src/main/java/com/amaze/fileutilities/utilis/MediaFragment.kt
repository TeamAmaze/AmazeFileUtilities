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

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

abstract class MediaFragment : Fragment() {

    abstract fun getRootLayout(): View

    fun refactorSystemUi(hide: Boolean) {
        if (hide) {
            WindowInsetsControllerCompat(
                requireActivity().window,
                getRootLayout()
            ).let {
                controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // TODO: Bug, use custom action bar so that hiding doesn't refresh the content
            (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        } else {
            WindowInsetsControllerCompat(
                requireActivity().window,
                getRootLayout()
            ).let {
                controller ->
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_BARS_BY_TOUCH
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.show()
        }
    }
}
