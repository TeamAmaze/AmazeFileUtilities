package com.amaze.fileutilities.image_viewer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.drew.imaging.ImageMetadataReader
import java.io.File


class ImageViewerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val imageModel: List<LocalImageModel>)
    : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return imageModel.size
    }

    override fun createFragment(position: Int): Fragment {

        /*val imageMetadata = Imaging.getMetadata(File(imageModel[position].uri.path!!))
        if (imageMetadata is JpegImageMetadata) {

        }*/
        return ImageViewerFragment.newInstance(imageModel[position])
    }
}