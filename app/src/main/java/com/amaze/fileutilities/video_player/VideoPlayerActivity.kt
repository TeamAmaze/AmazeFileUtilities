/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.video_player

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.amaze.fileutilities.R

class VideoPlayerActivity : BaseVideoPlayerActivity() {

    companion object {
        const val VIDEO_PLAYBACK_POSITION = "playback_position"
    }

    override fun isDialogActivity(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                setTheme(R.style.Theme_AmazeFileUtilities_FullScreen_Dark)
            } catch (e: Exception) {
                log.warn("failed to set theme Theme_AmazeFileUtilities_FullScreen_Dark", e)
                setTheme(R.style.Theme_AmazeFileUtilities_FullScreen_Dark_Fallback)
            }
        }
        initLocalVideoModel(intent)
        super.onCreate(savedInstanceState)
        handleVideoPlayerActivityResources()
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // allow to go in notch area in landscape mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Set the system UI visibility flags
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                // Get the WindowInsetsController
                val controller = window.insetsController

                // Hide the system bars (navigation bar, status bar)
                controller?.hide(WindowInsets.Type.systemBars())

                // Enable the extended layout to be displayed in the notch area
                controller?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }
    }
}
