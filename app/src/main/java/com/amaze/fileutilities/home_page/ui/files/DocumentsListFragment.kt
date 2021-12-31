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
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentDocumentsListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.MediaTypeView
import com.amaze.fileutilities.utilis.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

class DocumentsListFragment : Fragment() {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentDocumentsListBinding? = null
    private var mediaFileAdapter: MediaFileAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val MAX_PRELOAD = 50

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentsListBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        (requireActivity() as MainActivity).setCustomTitle(resources.getString(R.string.documents))
        (activity as MainActivity).invalidateBottomBar(false)
        filesViewModel.usedDocsSummaryTransformations.observe(
            viewLifecycleOwner,
            {
                metaInfoAndSummaryPair ->
                binding.documentsListInfoText.text = resources.getString(R.string.loading)
                metaInfoAndSummaryPair?.let {
                    val metaInfoList = metaInfoAndSummaryPair.second
                    metaInfoList.run {
                        if (this.size == 0) {
                            binding.documentsListInfoText.text =
                                resources.getString(R.string.no_files)
                            binding.loadingProgress.visibility = View.GONE
                        } else {
                            binding.documentsListInfoText.visibility = View.GONE
                            binding.loadingProgress.visibility = View.GONE
                        }
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace =
                            FileUtils.formatStorageLength(
                                requireContext(), storageSummary.usedSpace!!
                            )
                        val totalSpace = FileUtils.formatStorageLength(
                            requireContext(), storageSummary.totalSpace!!
                        )
                        // set list adapter
                        preloader = MediaAdapterPreloader(
                            requireContext(),
                            R.drawable.ic_outline_insert_drive_file_32
                        )
                        val sizeProvider = ViewPreloadSizeProvider<String>()
                        recyclerViewPreloader = RecyclerViewPreloader(
                            Glide.with(requireActivity()),
                            preloader!!,
                            sizeProvider,
                            MAX_PRELOAD
                        )
                        linearLayoutManager = LinearLayoutManager(context)
                        mediaFileAdapter = MediaFileAdapter(
                            requireContext(),
                            preloader!!,
                            MediaFileListSorter.SortingPreference(
                                MediaFileListSorter.GROUP_NAME,
                                MediaFileListSorter.SORT_SIZE,
                                true,
                                true
                            ),
                            this, MediaFileInfo.MEDIA_TYPE_DOCUMENT
                        ) {
                            it.setProgress(
                                MediaTypeView.MediaTypeContent(
                                    storageSummary.items, usedSpace,
                                    storageSummary.progress, totalSpace
                                )
                            )
                        }
                        binding.documentsListView
                            .addOnScrollListener(recyclerViewPreloader!!)
                        binding.documentsListView.layoutManager = linearLayoutManager
                        binding.documentsListView.adapter = mediaFileAdapter
                    }
                }
            }
        )
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_files))
        (activity as MainActivity).invalidateBottomBar(true)
        _binding = null
    }
}
