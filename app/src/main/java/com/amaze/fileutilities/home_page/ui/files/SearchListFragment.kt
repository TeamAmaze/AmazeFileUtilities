/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amaze.fileutilities.databinding.FragmentImagesListBinding
import com.amaze.fileutilities.home_page.MainActivity

class SearchListFragment : Fragment() {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentImagesListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).invalidateSearchBar(true)
    }

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).invalidateSearchBar(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentImagesListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root

        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
