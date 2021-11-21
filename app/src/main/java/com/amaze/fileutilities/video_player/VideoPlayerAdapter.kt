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
    private val playerFragmentMap: MutableMap<Int, WeakReference<VideoPlayerFragment>>)
    : FragmentStateAdapter(fragmentManager, lifecycle) {


    override fun getItemCount(): Int {
        return videoModel.size
    }

    override fun createFragment(position: Int): Fragment {
        val videoPlayerFragment = VideoPlayerFragment.newInstance(videoModel[position])
        playerFragmentMap[position] = WeakReference(videoPlayerFragment)
        return videoPlayerFragment
    }
}