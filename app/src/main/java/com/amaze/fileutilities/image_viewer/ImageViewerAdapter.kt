package com.amaze.fileutilities.image_viewer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class ImageViewerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val imageModel: ArrayList<LocalImageModel>)
    : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return imageModel.size
    }

    override fun createFragment(position: Int): Fragment {
        return ImageViewerFragment.newInstance(imageModel[position])
    }
}