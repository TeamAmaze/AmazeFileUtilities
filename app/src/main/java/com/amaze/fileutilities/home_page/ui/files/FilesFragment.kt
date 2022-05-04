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
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeView
import com.amaze.fileutilities.utilis.*
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.ramijemli.percentagechartview.callback.AdaptiveColorProvider

class FilesFragment : ItemsActionBarFragment() {

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
            internalStorageStats().observe(
                viewLifecycleOwner
            ) {
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
                            resources.getString(
                                R.string.num_of_media_files,
                                String.format("%,d", it.items)
                            )
                    }
                }
            }
            usedImagesSummaryTransformations.observe(
                viewLifecycleOwner
            ) { metaInfoAndSummaryPair ->
                binding.imagesTab.setOnClickListener {
                    if (metaInfoAndSummaryPair != null) {
                        if (metaInfoAndSummaryPair.second.isNotEmpty()) {
                            startListFragment(ImagesListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.no_files)
                            )
                        }
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
                    val usedTotalSpace =
                        FileUtils.formatStorageLength(
                            requireContext(),
                            storageSummary.totalUsedSpace!!
                        )
                    binding.imagesTab.setProgress(
                        MediaTypeView.MediaTypeContent(
                            storageSummary.items, usedSpace,
                            storageSummary.progress, usedTotalSpace, storageSummary.totalItems
                        )
                    )
                }
            }
            usedAudiosSummaryTransformations.observe(
                viewLifecycleOwner
            ) { metaInfoAndSummaryPair ->
                binding.audiosTab.setOnClickListener {
                    if (metaInfoAndSummaryPair != null) {
                        if (metaInfoAndSummaryPair.second.isNotEmpty()) {
                            startListFragment(AudiosListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.no_files)
                            )
                        }
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
                    val usedTotalSpace = FileUtils
                        .formatStorageLength(
                            requireContext(),
                            storageSummary.totalUsedSpace!!
                        )
                    binding.audiosTab.setProgress(
                        MediaTypeView.MediaTypeContent(
                            storageSummary.items, usedSpace,
                            storageSummary.progress, usedTotalSpace, storageSummary.totalItems
                        )
                    )
                }
            }
            usedVideosSummaryTransformations.observe(
                viewLifecycleOwner
            ) { metaInfoAndSummaryPair ->
                binding.videosTab.setOnClickListener {
                    if (metaInfoAndSummaryPair != null) {
                        if (metaInfoAndSummaryPair.second.isNotEmpty()) {
                            startListFragment(VideosListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.no_files)
                            )
                        }
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
                    val usedTotalSpace = FileUtils
                        .formatStorageLength(
                            requireContext(),
                            storageSummary.totalUsedSpace!!
                        )
                    binding.videosTab.setProgress(
                        MediaTypeView
                            .MediaTypeContent(
                                storageSummary.items,
                                usedSpace, storageSummary.progress, usedTotalSpace,
                                storageSummary.totalItems
                            )
                    )
                }
            }
            usedDocsSummaryTransformations.observe(
                viewLifecycleOwner
            ) { metaInfoAndSummaryPair ->
                binding.documentsTab.setOnClickListener {
                    if (metaInfoAndSummaryPair != null) {
                        if (metaInfoAndSummaryPair.second.isNotEmpty()) {
                            startListFragment(DocumentsListFragment())
                        } else {
                            requireContext().showToastInCenter(
                                resources
                                    .getString(R.string.no_files)
                            )
                        }
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
                    val usedTotalSpace = FileUtils
                        .formatStorageLength(
                            requireContext(),
                            storageSummary.totalUsedSpace!!
                        )
                    binding.documentsTab.setProgress(
                        MediaTypeView
                            .MediaTypeContent(
                                storageSummary.items,
                                usedSpace, storageSummary.progress, usedTotalSpace,
                                storageSummary.totalItems
                            )
                    )
                }
            }
            recentFilesLiveData.observe(
                viewLifecycleOwner
            ) { mediaFileInfoList ->
                binding.recentFilesInfoText.text = resources.getString(R.string.loading)
                mediaFileInfoList?.run {
                    if (this.isEmpty()) {
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
                        requireActivity(),
                        preloader!!,
                        ArrayList(this)
                    ) {
                        checkedSize, itemsCount, bytesFormatted ->
                        val title = "$checkedSize / $itemsCount" +
                            " ($bytesFormatted)"
                        if (checkedSize > 0) {
                            setupShowActionBar()
                            setupCommonButtons()
                            getLocateFileButton()?.visibility = if (checkedSize == 1)
                                View.VISIBLE else View.GONE
                        } else {
                            hideActionBar()
                        }

                        val countView = getCountView()
                        countView?.text = title
                    }
                    binding.recentFilesList
                        .addOnScrollListener(recyclerViewPreloader!!)
                    binding.recentFilesList.layoutManager = linearLayoutManager
                    binding.recentFilesList.adapter = mediaFileAdapter
                }
            }
        }

        binding.storagePercent.setAdaptiveColorProvider(colorProvider)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun hideActionBarOnClick(): Boolean {
        return true
    }

    override fun getMediaFileAdapter(): AbstractMediaFilesAdapter? {
        return mediaFileAdapter
    }

    override fun getMediaListType(): Int {
        return MediaFileAdapter.MEDIA_TYPE_UNKNOWN
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
