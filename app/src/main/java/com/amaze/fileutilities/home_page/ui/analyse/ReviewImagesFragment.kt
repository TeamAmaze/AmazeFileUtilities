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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentReviewImagesBinding
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaAdapterPreloader
import com.amaze.fileutilities.home_page.ui.files.MediaFileAdapter
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter
import com.amaze.fileutilities.utilis.AbstractMediaFilesAdapter.ListItem
import com.amaze.fileutilities.utilis.ItemsActionBarFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReviewImagesFragment : ItemsActionBarFragment() {

    private var log: Logger = LoggerFactory.getLogger(ReviewImagesFragment::class.java)

    private var _binding: FragmentReviewImagesBinding? = null

    private val filesViewModel: FilesViewModel by activityViewModels()
    private lateinit var viewModel: AnalyseViewModel
    private var gridLayoutManager: GridLayoutManager? = GridLayoutManager(context, 3)
    private var mediaFileAdapter: ReviewAnalysisAdapter? = null
    private var preloader: MediaAdapterPreloader? = null
    private var recyclerViewPreloader: RecyclerViewPreloader<String>? = null
    private val MAX_PRELOAD = 50
    private var analysisType: Int? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    companion object {

        private const val ANALYSIS_TYPE = "analysis_type"
        const val FRAGMENT_TAG = "review_fragment"
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
        const val TYPE_UNUSED_APPS = 19
        const val TYPE_LARGE_APPS = 20
        const val TYPE_GAMES_INSTALLED = 21
        const val TYPE_APK_FILES = 22

        fun newInstance(type: Int, fragment: Fragment) {
            val analyseFragment = ReviewImagesFragment()
            analyseFragment.apply {
                val bundle = Bundle()
                bundle.putInt(ANALYSIS_TYPE, type)
                arguments = bundle
            }

            val transaction = fragment.parentFragmentManager.beginTransaction()
            transaction.add(R.id.nav_host_fragment_activity_main, analyseFragment, FRAGMENT_TAG)
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
            ViewModelProvider(this.requireActivity()).get(AnalyseViewModel::class.java)

        _binding = FragmentReviewImagesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        analysisType = arguments?.getInt(ANALYSIS_TYPE)!!
        viewModel.analysisType = analysisType
        setupShowActionBar()
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
        initAnalysisView(analysisType!!)
        return root
    }

    override fun onDestroyView() {
        preloader?.clear()
        super.onDestroyView()
        _binding = null
    }

    override fun hideActionBarOnClick(): Boolean {
        return false
    }

    override fun getMediaFileAdapter(): AbstractMediaFilesAdapter? {
        return mediaFileAdapter
    }

    /**
     * Required because we need to know while deleting what type of media file info we want to delete
     */
    override fun getMediaListType(): Int {
        // this will never be called, as it's only used by abstract media list fragment
        return when (analysisType) {
            TYPE_BLUR, TYPE_LOW_LIGHT, TYPE_MEME, TYPE_SAD, TYPE_DISTRACTED, TYPE_SLEEPING,
            TYPE_SELFIE, TYPE_GROUP_PIC -> {
                MediaFileAdapter.MEDIA_TYPE_IMAGES
            }
            TYPE_LARGE_VIDEOS, TYPE_CLUTTERED_VIDEOS -> {
                MediaFileAdapter.MEDIA_TYPE_VIDEO
            }
            TYPE_OLD_RECORDINGS -> {
                MediaFileAdapter.MEDIA_TYPE_AUDIO
            }
            TYPE_LARGE_APPS, TYPE_UNUSED_APPS, TYPE_GAMES_INSTALLED, TYPE_APK_FILES -> {
                MediaFileAdapter.MEDIA_TYPE_APKS
            }
            else -> {
                MediaFileAdapter.MEDIA_TYPE_UNKNOWN
            }
        }
    }

    private fun initAnalysisView(analysisType: Int) {
        val appDatabase = AppDatabase.getInstance(requireContext())
        val dao = appDatabase.analysisDao()
        val blurAnalysisDao = appDatabase.blurAnalysisDao()
        val lowLightAnalysisDao = appDatabase.lowLightAnalysisDao()
        val memeAnalysisDao = appDatabase.memesAnalysisDao()
        val pathPreferencesDao = appDatabase.pathPreferencesDao()
        val internalStorageDao = appDatabase.internalStorageAnalysisDao()

        val prefs = requireContext().getAppCommonSharedPreferences()
        val duplicatePref = prefs.getInt(
            PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
            PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
        )

        when (analysisType) {
            TYPE_BLUR ->
                {
                    viewModel.getBlurImages(blurAnalysisDao).observe(viewLifecycleOwner) {
                        if (it == null) {
                            invalidateProcessing(
                                true,
                                filesViewModel.isImageBlurAnalysing
                            )
                        } else {
                            setMediaInfoList(it, true) {
                                checkedItems ->
                                viewModel.cleanBlurAnalysis(blurAnalysisDao, checkedItems)
                            }
                            invalidateProcessing(
                                false,
                                filesViewModel.isImageBlurAnalysing
                            )
                        }
                    }
                }
            TYPE_LOW_LIGHT ->
                {
                    viewModel.getLowLightImages(lowLightAnalysisDao).observe(viewLifecycleOwner) {
                        if (it == null) {
                            invalidateProcessing(
                                true,
                                filesViewModel.isImageLowLightAnalysing
                            )
                        } else {
                            setMediaInfoList(it, true) {
                                checkedItems ->
                                viewModel.cleanLowLightAnalysis(
                                    lowLightAnalysisDao,
                                    checkedItems
                                )
                            }
                            invalidateProcessing(
                                false,
                                filesViewModel.isImageLowLightAnalysing
                            )
                        }
                    }
                }
            TYPE_MEME -> {
                viewModel.getMemeImages(memeAnalysisDao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isImageMemesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanMemeAnalysis(memeAnalysisDao, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageMemesAnalysing
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
                            setMediaInfoList(it, false)
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
                        setMediaInfoList(it, false)
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
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanImageAnalysis(dao, TYPE_SAD, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    }
                }
            }
            TYPE_DISTRACTED -> {
                viewModel.getDistractedImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanImageAnalysis(dao, TYPE_DISTRACTED, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    }
                }
            }
            TYPE_SLEEPING -> {
                viewModel.getSleepingImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanImageAnalysis(dao, TYPE_SLEEPING, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    }
                }
            }
            TYPE_SELFIE -> {
                viewModel.getSelfieImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanImageAnalysis(dao, TYPE_SELFIE, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    }
                }
            }
            TYPE_GROUP_PIC -> {
                viewModel.getGroupPicImages(dao).observe(viewLifecycleOwner) {
                    if (it == null) {
                        invalidateProcessing(
                            true,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    } else {
                        setMediaInfoList(it, true) {
                            checkedItems ->
                            viewModel.cleanImageAnalysis(dao, TYPE_GROUP_PIC, checkedItems)
                        }
                        invalidateProcessing(
                            false,
                            filesViewModel.isImageFeaturesAnalysing
                        )
                    }
                }
            }
            TYPE_CLUTTERED_VIDEOS -> {
                filesViewModel.usedVideosSummaryTransformations()
                    .observe(viewLifecycleOwner) { mediaFilePair ->
                        invalidateProcessing(true, false)
                        mediaFilePair?.let {
                            viewModel.getClutteredVideos(mediaFilePair.second)
                                .observe(viewLifecycleOwner) { clutteredVideosInfo ->
                                    clutteredVideosInfo?.let {
                                        setMediaInfoList(it, false)
                                        invalidateProcessing(false, false)
                                    }
                                }
                        }
                    }
            }
            TYPE_LARGE_VIDEOS -> {
                filesViewModel.usedVideosSummaryTransformations()
                    .observe(viewLifecycleOwner) { mediaFilePair ->
                        invalidateProcessing(true, false)
                        mediaFilePair?.let {
                            viewModel.getLargeVideos(mediaFilePair.second)
                                .observe(viewLifecycleOwner) { largeVideosList ->
                                    largeVideosList?.let {
                                        setMediaInfoList(it, false)
                                        invalidateProcessing(false, false)
                                    }
                                }
                        }
                    }
            }
            TYPE_LARGE_DOWNLOADS -> {
                viewModel.getLargeDownloads(pathPreferencesDao).observe(viewLifecycleOwner) {
                    largeDownloads ->
                    invalidateProcessing(true, false)
                    largeDownloads?.let {
                        setMediaInfoList(it, false)
                        invalidateProcessing(false, false)
                    }
                }
            }
            TYPE_OLD_DOWNLOADS -> {
                viewModel.getOldDownloads(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldDownloads ->
                    invalidateProcessing(true, false)
                    oldDownloads?.let {
                        setMediaInfoList(it, false)
                        invalidateProcessing(false, false)
                    }
                }
            }
            TYPE_OLD_SCREENSHOTS -> {
                viewModel.getOldScreenshots(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldScreenshots ->
                    invalidateProcessing(true, false)
                    oldScreenshots?.let {
                        setMediaInfoList(it, false)
                        invalidateProcessing(false, false)
                    }
                }
            }
            TYPE_OLD_RECORDINGS -> {
                viewModel.getOldRecordings(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldRecordings ->
                    invalidateProcessing(true, false)
                    oldRecordings?.let {
                        setMediaInfoList(it, false)
                        invalidateProcessing(false, false)
                    }
                }
            }
            TYPE_UNUSED_APPS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    filesViewModel.getUnusedApps()
                        .observe(viewLifecycleOwner) { unusedApps ->
                            invalidateProcessing(true, false)
                            unusedApps?.let {
                                setMediaInfoList(it, false)
                                invalidateProcessing(false, false)
                            }
                        }
                }
            }
            TYPE_LARGE_APPS -> {
                filesViewModel.getLargeApps()
                    .observe(viewLifecycleOwner) { largeApps ->
                        invalidateProcessing(true, false)
                        largeApps?.let {
                            setMediaInfoList(it, false)
                            invalidateProcessing(false, false)
                        }
                    }
            }
            TYPE_GAMES_INSTALLED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    filesViewModel.getGamesInstalled()
                        .observe(viewLifecycleOwner) { games ->
                            invalidateProcessing(true, false)
                            games?.let {
                                setMediaInfoList(it, false)
                                invalidateProcessing(false, false)
                            }
                        }
                }
            }
            TYPE_APK_FILES -> {
                filesViewModel.getApksLiveData()
                    .observe(viewLifecycleOwner) { apkFiles ->
                        invalidateProcessing(true, false)
                        apkFiles?.let {
                            setMediaInfoList(it, false)
                            invalidateProcessing(false, false)
                        }
                    }
            }
        }
    }

    /**
     * @param doShowDown whether to show thumbs down button
     * @param cleanData if doShowDown is true, set a way to get cleaned data result
     */
    private fun setMediaInfoList(
        mediaInfoList: MutableList<MediaFileInfo>,
        doShowDown: Boolean,
        cleanData: ((List<ListItem>) -> LiveData<Boolean>)? = null
    ) {
        val thumbsDownButton = getThumbsDown()
        mediaFileAdapter = ReviewAnalysisAdapter(
            requireActivity(),
            preloader!!, mediaInfoList
        ) { checkedSize, itemsCount, bytesFormatted ->
            val title = "$checkedSize / $itemsCount" +
                " ($bytesFormatted)"
            val countView = getCountView()
            countView?.text = title
            if (checkedSize > 0) {
                if (doShowDown) {
                    thumbsDownButton?.visibility = View.VISIBLE
                }
                getLocateFileButton()?.visibility = if (checkedSize == 1)
                    View.VISIBLE else View.GONE
            } else {
                thumbsDownButton?.visibility = View.GONE
                getLocateFileButton()?.visibility = View.GONE
            }
        }
        setupActionBarButtons(cleanData)
        binding.listView
            .addOnScrollListener(recyclerViewPreloader!!)
        gridLayoutManager = GridLayoutManager(
            context,
            requireContext()
                .getAppCommonSharedPreferences()
                .getInt(
                    PreferencesConstants.KEY_GRID_VIEW_COLUMN_COUNT,
                    PreferencesConstants.DEFAULT_GRID_VIEW_COLUMN_COUNT
                )
        )
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

    private fun setupActionBarButtons(cleanData: ((List<ListItem>) -> LiveData<Boolean>)?) {
        val thumbsDownButton = getThumbsDown()
        setupCommonButtons()
        thumbsDownButton?.setOnClickListener {
            mediaFileAdapter?.let {
                if (!it.checkItemsList.isNullOrEmpty()) {
                    if (cleanData != null) {
                        cleanData(it.checkItemsList).observe(viewLifecycleOwner) { success ->
                            if (success) {
                                if (mediaFileAdapter?.removeChecked() != true) {
                                    log.warn("Failed to update analysis adapter to remove items")
                                    viewModel.analysisType?.let { type ->
                                        initAnalysisView(type)
                                    }
                                }
                                requireActivity().showToastOnBottom(
                                    resources
                                        .getString(R.string.analysis_updated)
                                )
                                setupShowActionBar()
                            }
                        }
                    }
                } else {
                    requireActivity().showToastOnBottom(
                        resources
                            .getString(R.string.no_item_selected)
                    )
                }
            }
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
}
