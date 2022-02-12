/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.analyse

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentReviewImagesBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class ReviewImagesFragment : Fragment() {

    private var _binding: FragmentReviewImagesBinding? = null
    private val filesViewModel: FilesViewModel by activityViewModels()
    private lateinit var viewModel: AnalyseViewModel
    private var gridLayoutManager: GridLayoutManager? = GridLayoutManager(context, 3)
    private var mediaFileAdapter: ReviewImagesAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private val MAX_PRELOAD = 50
    private var optionsActionBar: View? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {

        private const val ANALYSIS_TYPE = "analysis_type"
        const val TYPE_BLUR = 1
        const val TYPE_MEME = 2
        const val TYPE_DUPLICATES = 3
        const val TYPE_LARGE_VIDEOS = 4
        const val TYPE_OLD_DOWNLOADS = 5
        const val TYPE_OLD_SCREENSHOTS = 6
        const val TYPE_OLD_RECORDINGS = 7
        const val TYPE_EMPTY_FILES = 8
        const val TYPE_JUNK_FILES = 9
        const val TYPE_SLEEPING = 10
        const val TYPE_DISTRACTED = 11
        const val TYPE_SAD = 12
        const val TYPE_SELFIE = 13
        const val TYPE_GROUP_PIC = 14
        const val TYPE_TELEGRAM = 15
        const val TYPE_LARGE_DOWNLOADS = 16
        const val TYPE_LOW_LIGHT = 17
        const val TYPE_CLUTTERED_VIDEOS = 18

        fun newInstance(type: Int, fragment: Fragment) {
            val analyseFragment = ReviewImagesFragment()
            analyseFragment.apply {
                val bundle = Bundle()
                bundle.putInt(ANALYSIS_TYPE, type)
                arguments = bundle
            }

            val transaction = fragment.parentFragmentManager.beginTransaction()
            transaction.add(R.id.nav_host_fragment_activity_main, analyseFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(AnalyseViewModel::class.java)

        _binding = FragmentReviewImagesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val analysisType: Int = arguments?.getInt(ANALYSIS_TYPE)!!
        val prefs = requireContext().getAppCommonSharedPreferences()
        val duplicatePref = prefs.getInt(
            PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
            PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
        )
        optionsActionBar = (activity as MainActivity).invalidateSelectedActionBar(true)!!
        (activity as MainActivity).invalidateBottomBar(false)

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
        val appDatabase = AppDatabase.getInstance(requireContext())
        val dao = appDatabase.analysisDao()
        val internalStorageDao = appDatabase.internalStorageAnalysisDao()
        when (analysisType) {
            TYPE_BLUR ->
                {
                    viewModel.getBlurImages(dao).observe(viewLifecycleOwner) {
                        if (it == null) {
                            invalidateProcessing(
                                true,
                                filesViewModel.isMediaFilesAnalysing
                            )
                        } else {
                            setMediaInfoList(ArrayList(it), true)
                            invalidateProcessing(
                                false,
                                filesViewModel.isMediaFilesAnalysing
                            )
                        }
                    }
                }
            TYPE_LOW_LIGHT ->
                {
                    viewModel.getLowLightImages(dao).observe(viewLifecycleOwner) {
                        if (it == null) {
                            invalidateProcessing(
                                true,
                                filesViewModel.isMediaFilesAnalysing
                            )
                        } else {
                            setMediaInfoList(ArrayList(it), true)
                            invalidateProcessing(
                                false,
                                filesViewModel.isMediaFilesAnalysing
                            )
                        }
                    }
                }
            TYPE_MEME -> {
                viewModel.getMemeImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_DUPLICATES -> {
                viewModel.getDuplicateDirectories(
                    internalStorageDao,
                    duplicatePref
                        == PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE,
                    duplicatePref
                        == PreferencesConstants.VAL_SEARCH_DUPLICATES_INTERNAL_DEEP
                )
                    .observe(viewLifecycleOwner) {
                        val isMediaFilePref = duplicatePref ==
                            PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE
                        val invalidateProgress = if (isMediaFilePref)
                            filesViewModel.isMediaStoreAnalysing
                        else filesViewModel.isInternalStorageAnalysing
                        if (it == null) {
                            invalidateProcessing(true, invalidateProgress)
                        } else {
                            setMediaInfoList(ArrayList(it.flatten()), false)
                            invalidateProcessing(false, invalidateProgress)
                        }
                    }
            }
            TYPE_EMPTY_FILES -> {
                viewModel.getEmptyFiles(internalStorageDao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isInternalStorageAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), false)
                        invalidateProcessing(
                            false,
                            filesViewModel.isInternalStorageAnalysing
                        )
                    }
                }
            }
            TYPE_SAD -> {
                viewModel.getSadImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_DISTRACTED -> {
                viewModel.getDistractedImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_SLEEPING -> {
                viewModel.getSleepingImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_SELFIE -> {
                viewModel.getSelfieImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_GROUP_PIC -> {
                viewModel.getGroupPicImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    } else {
                        setMediaInfoList(ArrayList(it), true)
                        invalidateProcessing(
                            false,
                            filesViewModel.isMediaFilesAnalysing
                        )
                    }
                }
            }
            TYPE_CLUTTERED_VIDEOS -> {
                filesViewModel.usedVideosSummaryTransformations
                    .observe(viewLifecycleOwner) { mediaFilePair ->
                        invalidateProcessing(true, false)
                        mediaFilePair?.let {
                            viewModel.getClutteredVideos(mediaFilePair.second)
                                .observe(viewLifecycleOwner) { clutteredVideosInfo ->
                                    clutteredVideosInfo?.let {
                                        setMediaInfoList(ArrayList(it), false)
                                        invalidateProcessing(false, false)
                                    }
                                }
                        }
                    }
            }
        }
        return root
    }

    /**
     * @param doShowDown whether to show thumbs down button
     */
    private fun setMediaInfoList(mediaInfoList: MutableList<MediaFileInfo>, doShowDown: Boolean) {
        mediaFileAdapter = ReviewImagesAdapter(
            requireContext(),
            preloader!!, mediaInfoList
        ) { checkedSize, itemsCount, bytesFormatted ->
            val title = "$checkedSize / $itemsCount" +
                " ($bytesFormatted)"
            val countView = optionsActionBar?.findViewById<AppCompatTextView>(R.id.title)
            val thumbsDownButton =
                optionsActionBar?.findViewById<ImageView>(R.id.thumbsDown)
            countView?.text = title
            if (checkedSize > 0 && doShowDown) {
                thumbsDownButton?.visibility = View.VISIBLE
            } else {
                thumbsDownButton?.visibility = View.GONE
            }
        }
        binding.listView
            .addOnScrollListener(recyclerViewPreloader!!)
        Utils.setGridLayoutManagerSpan(gridLayoutManager!!, mediaFileAdapter!!)
        binding.listView.layoutManager = gridLayoutManager
        binding.listView.adapter = mediaFileAdapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.fastscroll.visibility = View.GONE
            FastScrollerBuilder(binding.listView).useMd2Style().build()
        } else {
            binding.fastscroll.visibility = View.VISIBLE
            binding.fastscroll.setRecyclerView(binding.listView, 1)
        }
    }

    private fun invalidateProcessing(isProcessing: Boolean, isAnalysing: Boolean) {
        when {
            isProcessing -> {
                binding.processingProgressView.invalidateProcessing(
                    true, false,
                    resources.getString(R.string.please_wait)
                )
            }
            isAnalysing -> {
                binding.processingProgressView.invalidateProcessing(
                    false, false,
                    null
                )
            }
            mediaFileAdapter?.itemCount == 0 -> {
                binding.processingProgressView.invalidateProcessing(
                    false, true,
                    resources.getString(R.string.its_quiet_here)
                )
            }
            else -> {
                binding.processingProgressView.invalidateProcessing(
                    false, false,
                    null
                )
            }
        }
        mediaFileAdapter?.isProcessing = isAnalysing
    }

    override fun onDestroyView() {
        (activity as MainActivity).invalidateSelectedActionBar(false)
        (activity as MainActivity).invalidateBottomBar(true)
        super.onDestroyView()
    }
}
