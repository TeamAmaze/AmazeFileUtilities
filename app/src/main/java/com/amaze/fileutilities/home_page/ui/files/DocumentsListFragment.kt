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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentDocumentsListBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeView
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class DocumentsListFragment : Fragment(), MediaFileAdapter.OptionsMenuSelected {
    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentDocumentsListBinding? = null
    private var mediaFileAdapter: MediaFileAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager = LinearLayoutManager(context)
    private var gridLayoutManager: GridLayoutManager? = GridLayoutManager(context, 3)
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
            viewLifecycleOwner
        ) { metaInfoAndSummaryPair ->
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
                    val isList = requireContext()
                        .getAppCommonSharedPreferences().getBoolean(
                            PreferencesConstants.KEY_MEDIA_LIST_TYPE,
                            PreferencesConstants.DEFAULT_MEDIA_LIST_TYPE
                        )
                    mediaFileAdapter = MediaFileAdapter(
                        requireContext(),
                        preloader!!,
                        this@DocumentsListFragment, !isList,
                        MediaFileListSorter.SortingPreference.newInstance(
                            requireContext()
                                .getAppCommonSharedPreferences()
                        ),
                        ArrayList(this), MediaFileInfo.MEDIA_TYPE_DOCUMENT
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
                    Utils.setGridLayoutManagerSpan(gridLayoutManager!!, mediaFileAdapter!!)
                    binding.documentsListView.layoutManager =
                        if (isList) linearLayoutManager else gridLayoutManager
                    binding.documentsListView.adapter = mediaFileAdapter
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        binding.fastscroll.visibility = View.GONE
                        FastScrollerBuilder(binding.documentsListView).useMd2Style().build()
                    } else {
                        binding.fastscroll.visibility = View.VISIBLE
                        binding.fastscroll.setRecyclerView(binding.documentsListView, 1)
                    }
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity)
            .setCustomTitle(resources.getString(R.string.title_files))
        (activity as MainActivity).invalidateBottomBar(true)
        _binding = null
    }

    override fun sortBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun groupBy(sortingPreference: MediaFileListSorter.SortingPreference) {
        mediaFileAdapter?.invalidateData(sortingPreference)
    }

    override fun switchView(isList: Boolean) {
        binding.documentsListView.layoutManager = if (isList)
            linearLayoutManager else gridLayoutManager
        binding.documentsListView.adapter = mediaFileAdapter
    }

    override fun select(headerPosition: Int) {
        binding.documentsListView.scrollToPosition(headerPosition + 5)
    }
}
