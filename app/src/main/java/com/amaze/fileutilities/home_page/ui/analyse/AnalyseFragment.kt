/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.databinding.FragmentAnalyseBinding
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel

class AnalyseFragment : Fragment() {

    private lateinit var analyseViewModel: AnalyseViewModel
    private val filesViewModel: FilesViewModel by activityViewModels()

    private var _binding: FragmentAnalyseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        analyseViewModel =
            ViewModelProvider(this).get(AnalyseViewModel::class.java)
        _binding = FragmentAnalyseBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.run {
            blurredPicsPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)
            memesPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)
            blurredPicsPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_BLUR,
                    this@AnalyseFragment
                )
            }
            memesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_MEME,
                    this@AnalyseFragment
                )
            }

            val appDatabase = AppDatabase.getInstance(requireContext())
            val dao = appDatabase.analysisDao()

            analyseViewModel.getBlurImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    blurredPicsPreview.loadPreviews(it)
                }
            }
            analyseViewModel.getMemeImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    memesPreview.loadPreviews(it)
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
