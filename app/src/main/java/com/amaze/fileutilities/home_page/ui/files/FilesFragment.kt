/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import com.amaze.fileutilities.home_page.ui.analyse.ReviewImagesFragment
import com.amaze.fileutilities.home_page.ui.media_tile.MediaTypeView
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.FileUtils
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.showToastInCenter
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
            usedImagesSummaryTransformations().observe(
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
            usedAudiosSummaryTransformations().observe(
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
            usedVideosSummaryTransformations().observe(
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
            usedDocsSummaryTransformations().observe(
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
                            if (checkedSize == 1) enableLocateFileFab() else disableLocateFileFab()
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
            progressTrashBinFilesLiveData().observe(
                viewLifecycleOwner
            ) { trashBinFiles ->
                if (trashBinFiles != null) {
                    binding.trashBinTab.setOnClickListener {
                        ReviewImagesFragment.newInstance(
                            ReviewImagesFragment.TYPE_TRASH_BIN,
                            this@FilesFragment
                        )
                    }
                    val capacity = getTrashBinInstance().getTrashBinMetadata().getCapacity()
                    if (capacity == -1) {
                        binding.trashBinTab.setItemsAndHideProgress(
                            MediaTypeView.MediaTypeContent(
                                trashBinFiles.size, trashBinFiles.size.toString(), capacity
                            )
                        )
                    } else {
                        binding.trashBinTab.setProgress(
                            MediaTypeView.MediaTypeContent(
                                trashBinFiles.size, trashBinFiles.size.toString(), capacity
                            )
                        )
                    }
                } else {
                    binding.trashBinTab.setOnClickListener {
                        requireContext().showToastInCenter(
                            resources
                                .getString(R.string.please_wait)
                        )
                    }
                }
            }
        }

        binding.storagePercent.setAdaptiveColorProvider(colorProvider)
        return root
    }

    override fun onDestroyView() {
        preloader?.clear()
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

    override fun getAllOptionsFAB(): List<FloatingActionButton> {
        return arrayListOf(
            binding.optionsButtonFab, binding.deleteButtonFab,
            binding.shareButtonFab, binding.locateFileButtonFab
        )
    }

    override fun showOptionsCallback() {
        getPlayNextButton()?.visibility = View.GONE
    }

    override fun hideOptionsCallback() {
        // do nothing
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
