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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.databinding.FragmentAnalyseBinding
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

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
        val prefs = requireContext().getAppCommonSharedPreferences()
        binding.run {
            blurredPicsPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)
            lowLightPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)
            memesPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)
            setVisibility(prefs)
            setClickListeners()

            val appDatabase = AppDatabase.getInstance(requireContext())
            val dao = appDatabase.analysisDao()
            val internalStorageDao = appDatabase.internalStorageAnalysisDao()

            analyseViewModel.getBlurImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    blurredPicsPreview.loadPreviews(it)
                }
            }

            analyseViewModel.getLowLightImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    lowLightPreview.loadPreviews(it)
                }
            }

            analyseViewModel.getMemeImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    memesPreview.loadPreviews(it)
                }
            }

            analyseViewModel.getSadImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    sadPreview.loadPreviews(it)
                }
            }
            sadPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)

            analyseViewModel.getDistractedImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    distractedPreview.loadPreviews(it)
                }
            }
            distractedPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)

            analyseViewModel.getSleepingImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    sleepingPreview.loadPreviews(it)
                }
            }
            sleepingPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)

            analyseViewModel.getSelfieImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    selfiePreview.loadPreviews(it)
                }
            }
            selfiePreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)

            analyseViewModel.getGroupPicImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    groupPicPreview.loadPreviews(it)
                }
            }
            groupPicPreview.invalidateProgress(filesViewModel.isMediaFilesAnalysing)

            val duplicatePref = prefs.getInt(
                PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
                PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
            )
            val doProgressDuplicateFiles = if (duplicatePref ==
                PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE
            )
                filesViewModel.isMediaStoreAnalysing
            else filesViewModel.isInternalStorageAnalysing
            if (duplicatePref != PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE) {
                emptyFilesPreview.visibility = View.VISIBLE
                emptyFilesPreview.invalidateProgress(filesViewModel.isInternalStorageAnalysing)
                analyseViewModel.getEmptyFiles(internalStorageDao)
                    .observe(viewLifecycleOwner) {
                        if (it != null) {
                            emptyFilesPreview.loadPreviews(it)
                        }
                    }
            }
            val deepSearch = duplicatePref == PreferencesConstants
                .VAL_SEARCH_DUPLICATES_INTERNAL_DEEP
            val searchMediaFiles = duplicatePref == PreferencesConstants
                .VAL_SEARCH_DUPLICATES_MEDIA_STORE
            duplicateFilesPreview.invalidateProgress(doProgressDuplicateFiles)
            analyseViewModel.getDuplicateDirectories(
                internalStorageDao, searchMediaFiles, deepSearch
            ).observe(viewLifecycleOwner) {
                if (it != null) {
                    duplicateFilesPreview.loadPreviews(it.flatten())
                }
            }

            filesViewModel.usedVideosSummaryTransformations
                .observe(viewLifecycleOwner) { mediaFilePair ->
                    clutteredVideoPreview.invalidateProgress(true)
                    mediaFilePair?.let {
                        analyseViewModel.getClutteredVideos(mediaFilePair.second)
                            .observe(viewLifecycleOwner) { clutteredVideosInfo ->
                                clutteredVideosInfo?.let {
                                    clutteredVideoPreview.invalidateProgress(false)
                                    clutteredVideoPreview.loadPreviews(clutteredVideosInfo)
                                }
                            }
                    }
                }
        }
        return root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setVisibility(sharedPrefs: SharedPreferences) {
        binding.run {
            blurredPicsPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(PathPreferences.FEATURE_ANALYSIS_BLUR),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            lowLightPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(PathPreferences.FEATURE_ANALYSIS_LOW_LIGHT),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE

            memesPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(PathPreferences.FEATURE_ANALYSIS_MEME),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE

            sadPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_IMAGE_FEATURES
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE

            sleepingPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_IMAGE_FEATURES
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE

            distractedPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_IMAGE_FEATURES
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            selfiePreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_IMAGE_FEATURES
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            groupPicPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_IMAGE_FEATURES
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE

            largeDownloadPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_DOWNLOADS
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            oldDownloadPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_DOWNLOADS
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            oldRecordingsPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_RECORDING
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            oldScreenshotsPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(
                                PathPreferences
                                    .FEATURE_ANALYSIS_SCREENSHOTS
                            ),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE
            /*telegramPreview.visibility = if (sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(PathPreferences.FEATURE_ANALYSIS_TELEGRAM),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            ) View.VISIBLE else View.GONE*/
        }
    }

    private fun setClickListeners() {
        binding.run {
            blurredPicsPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_BLUR,
                    this@AnalyseFragment
                )
            }
            lowLightPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LOW_LIGHT,
                    this@AnalyseFragment
                )
            }
            memesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_MEME,
                    this@AnalyseFragment
                )
            }
            sadPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SAD,
                    this@AnalyseFragment
                )
            }
            sleepingPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SLEEPING,
                    this@AnalyseFragment
                )
            }
            distractedPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_DISTRACTED,
                    this@AnalyseFragment
                )
            }
            selfiePreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SELFIE,
                    this@AnalyseFragment
                )
            }
            groupPicPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_GROUP_PIC,
                    this@AnalyseFragment
                )
            }
            emptyFilesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_EMPTY_FILES,
                    this@AnalyseFragment
                )
            }
            duplicateFilesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_DUPLICATES,
                    this@AnalyseFragment
                )
            }

            largeDownloadPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_DOWNLOADS,
                    this@AnalyseFragment
                )
            }
            oldDownloadPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_OLD_DOWNLOADS,
                    this@AnalyseFragment
                )
            }
            oldScreenshotsPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_OLD_SCREENSHOTS,
                    this@AnalyseFragment
                )
            }
            oldRecordingsPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_OLD_RECORDINGS,
                    this@AnalyseFragment
                )
            }
            /*telegramPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_TELEGRAM,
                    this@AnalyseFragment
                )
            }*/
            largeVideoPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_VIDEOS,
                    this@AnalyseFragment
                )
            }
            clutteredVideoPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_CLUTTERED_VIDEOS,
                    this@AnalyseFragment
                )
            }
        }
    }
}
