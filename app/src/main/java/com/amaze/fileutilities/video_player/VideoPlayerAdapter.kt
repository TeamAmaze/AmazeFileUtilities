/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.video_player

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.ref.WeakReference

class VideoPlayerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val videoModel: List<LocalVideoModel>,
    private val playerFragmentMap: MutableMap<Int, WeakReference<VideoPlayerFragment>>
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return videoModel.size
    }

    override fun createFragment(position: Int): Fragment {
        val videoPlayerFragment = VideoPlayerFragment.newInstance(videoModel[position])
        playerFragmentMap[position] = WeakReference(videoPlayerFragment)
        return videoPlayerFragment
    }
}
