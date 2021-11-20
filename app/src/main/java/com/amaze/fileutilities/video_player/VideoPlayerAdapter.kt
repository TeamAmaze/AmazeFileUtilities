package com.amaze.fileutilities.video_player

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.amaze.fileutilities.image_viewer.ImageViewerFragment
import com.amaze.fileutilities.image_viewer.LocalImageModel

class VideoPlayerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val videoModel: List<LocalVideoModel>)
    : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return videoModel.size
    }

    override fun createFragment(position: Int): Fragment {

        /*val imageMetadata = Imaging.getMetadata(File(imageModel[position].uri.path!!))
        if (imageMetadata is JpegImageMetadata) {

        }*/
        return VideoPlayerFragment.newInstance(videoModel[position])
    }
}