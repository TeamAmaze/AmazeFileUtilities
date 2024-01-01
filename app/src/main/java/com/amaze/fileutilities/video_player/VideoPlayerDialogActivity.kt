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

import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.px
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerDialogActivity : BaseVideoPlayerActivity() {

    override fun isDialogActivity(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleViewPlayerDialogActivityResources()
        findViewById<RelativeLayout>(R.id.video_parent).setPadding(
            16.px.toInt(), 16.px.toInt(),
            16.px.toInt(), 16.px.toInt()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val playerView = findViewById<PlayerView>(R.id.video_view)
            playerView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    view?.let {
                        view ->
                        outline?.setRoundRect(0, 0, view.width, view.height, 24.px)
                    }
                }
            }
            playerView.clipToOutline = true
        }
    }

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            initLocalVideoModel(it)
        }
    }*/
}
