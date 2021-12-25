/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.image_viewer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ImageViewerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val imageModel: List<LocalImageModel>
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

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
