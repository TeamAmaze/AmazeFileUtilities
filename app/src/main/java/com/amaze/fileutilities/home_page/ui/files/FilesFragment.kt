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

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentFilesBinding
import com.amaze.fileutilities.home_page.ui.MediaTypeView
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.showToastInCenter
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider

class FilesFragment : Fragment() {

    private val filesViewModel: FilesViewModel by activityViewModels()
    private var _binding: FragmentFilesBinding? = null
    private var mediaFileAdapter: RecentMediaFilesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private val MAX_PRELOAD = 100

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFilesBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        // needed to avoid NPE in progress library when closing activity
        binding.storagePercent.isSaveEnabled = false
        filesViewModel.run {
            internalStorageStats.observe(
                viewLifecycleOwner,
                {
                    it?.run {
                        binding.internalStorageTab.setOnClickListener {
                            Utils.openActivity(
                                requireContext(), Utils.AMAZE_PACKAGE,
                                Utils.AMAZE_FILE_MANAGER_MAIN
                            )
                        }
                        val usedSpace = FileUtils.formatStorageLength(
                            requireContext(),
                            it.usedSpace!!
                        )
                        val freeSpace = FileUtils.formatStorageLength(
                            requireContext(),
                            it.freeSpace!!
                        )
                        binding.usedSpace.setColorAndLabel(
                            colorProvider
                                .provideProgressColor(it.progress.toFloat()),
                            usedSpace
                        )
                        binding.freeSpace.setColorAndLabel(
                            colorProvider
                                .provideBackgroundBarColor(it.progress.toFloat()),
                            freeSpace
                        )
                        binding.storagePercent.setProgress(
                            it.progress.toFloat(),
                            true
                        )
                        if (it.items == 0) {
                            binding.filesAmount.text = resources.getString(
                                R.string.num_of_files,
                                resources.getString(R.string.undetermined)
                            )
                        } else {
                            binding.filesAmount.text =
                                resources.getString(R.string.num_of_files, it.items.toString())
                        }
                    }
                }
            )
            usedImagesSummaryTransformations.observe(
                viewLifecycleOwner,
                {
                    metaInfoAndSummaryPair ->
                    binding.imagesTab.setOnClickListener {
                        if (metaInfoAndSummaryPair != null) {
                            startListFragment(ImagesListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.please_wait)
                            )
                        }
                    }
                    metaInfoAndSummaryPair?.let {
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace =
                            FileUtils.formatStorageLength(
                                requireContext(),
                                storageSummary.usedSpace!!
                            )
                        binding.imagesTab.setProgress(
                            MediaTypeView.MediaTypeContent(
                                storageSummary.items, usedSpace,
                                storageSummary.progress
                            )
                        )
                    }
                }
            )
            usedAudiosSummaryTransformations.observe(
                viewLifecycleOwner,
                {
                    metaInfoAndSummaryPair ->
                    binding.audiosTab.setOnClickListener {
                        if (metaInfoAndSummaryPair != null) {
                            startListFragment(AudiosListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.please_wait)
                            )
                        }
                    }
                    metaInfoAndSummaryPair?.let {
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace = FileUtils
                            .formatStorageLength(
                                requireContext(),
                                storageSummary.usedSpace!!
                            )
                        binding.audiosTab.setProgress(
                            MediaTypeView.MediaTypeContent(
                                storageSummary.items, usedSpace,
                                storageSummary.progress
                            )
                        )
                    }
                }
            )
            usedVideosSummaryTransformations.observe(
                viewLifecycleOwner,
                {
                    metaInfoAndSummaryPair ->
                    binding.videosTab.setOnClickListener {
                        if (metaInfoAndSummaryPair != null) {
                            startListFragment(VideosListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.please_wait)
                            )
                        }
                    }
                    metaInfoAndSummaryPair?.let {
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace = FileUtils
                            .formatStorageLength(
                                requireContext(),
                                storageSummary.usedSpace!!
                            )
                        binding.videosTab.setProgress(
                            MediaTypeView
                                .MediaTypeContent(
                                    storageSummary.items,
                                    usedSpace, storageSummary.progress
                                )
                        )
                    }
                }
            )
            usedDocsSummaryTransformations.observe(
                viewLifecycleOwner,
                {
                    metaInfoAndSummaryPair ->
                    binding.documentsTab.setOnClickListener {
                        if (metaInfoAndSummaryPair != null) {
                            startListFragment(DocumentsListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.please_wait)
                            )
                        }
                    }
                    metaInfoAndSummaryPair?.let {
                        val storageSummary = metaInfoAndSummaryPair.first
                        val usedSpace = FileUtils.formatStorageLength(
                            requireContext(),
                            storageSummary.usedSpace!!
                        )
                        binding.documentsTab.setProgress(
                            MediaTypeView
                                .MediaTypeContent(
                                    storageSummary.items,
                                    usedSpace, storageSummary.progress
                                )
                        )
                    }
                }
            )
            recentFilesLiveData.observe(
                viewLifecycleOwner,
                {
                    mediaFileInfoList ->
                    binding.recentFilesInfoText.text = resources.getString(R.string.loading)
                    mediaFileInfoList?.run {
                        if (this.size == 0) {
                            binding.recentFilesInfoText.text =
                                resources.getString(R.string.no_files)
                        } else {
                            binding.recentFilesInfoText.visibility = View.GONE
                        }
                        preloader = MediaAdapterPreloader(
                            applicationContext,
                            R.drawable.ic_outline_insert_drive_file_32
                        )
                        val sizeProvider = ViewPreloadSizeProvider<String>()
                        recyclerViewPreloader = RecyclerViewPreloader(
                            Glide.with(applicationContext),
                            preloader!!,
                            sizeProvider,
                            MAX_PRELOAD
                        )
                        linearLayoutManager = LinearLayoutManager(context)
                        mediaFileAdapter = RecentMediaFilesAdapter(
                            applicationContext,
                            preloader!!,
                            this
                        )
                        binding.recentFilesList
                            .addOnScrollListener(recyclerViewPreloader!!)
                        binding.recentFilesList.layoutManager = linearLayoutManager
                        binding.recentFilesList.adapter = mediaFileAdapter
                    }
                }
            )
        }

        binding.storagePercent.setAdaptiveColorProvider(colorProvider)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startListFragment(listFragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.add(R.id.nav_host_fragment_activity_main, listFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private var colorProvider: AdaptiveColorProvider = object : AdaptiveColorProvider {
        override fun provideProgressColor(progress: Float): Int {
            return when {
                progress <= 25 -> {
                    resources.getColor(R.color.green)
                }
                progress <= 50 -> {
                    resources.getColor(R.color.yellow)
                }
                progress <= 75 -> {
                    resources.getColor(R.color.orange)
                }
                else -> {
                    resources.getColor(R.color.red)
                }
            }
        }

        override fun provideBackgroundColor(progress: Float): Int {
            // This will provide a bg color that is
            // 80% darker than progress color.
            // return ColorUtils.blendARGB(provideProgressColor(progress),
            // Color.BLACK, .8f)
            return resources.getColor(R.color.white_grey_1)
        }

        override fun provideTextColor(progress: Float): Int {
            return resources.getColor(R.color.white)
        }

        override fun provideBackgroundBarColor(progress: Float): Int {
            return ColorUtils.blendARGB(
                provideProgressColor(progress),
                Color.BLACK, .5f
            )
        }
    }
}
