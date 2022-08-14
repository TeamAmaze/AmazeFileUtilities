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

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.FragmentAnalyseBinding
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.AbstractMediaFileInfoOperationsFragment
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.showToastOnBottom

class AnalyseFragment : AbstractMediaFileInfoOperationsFragment() {

    private lateinit var analyseViewModel: AnalyseViewModel
    private val filesViewModel: FilesViewModel by activityViewModels()

    private var _binding: FragmentAnalyseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var shouldCallbackAppUninstall = true

    override fun getFilesViewModelObj(): FilesViewModel {
        return filesViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        analyseViewModel =
            ViewModelProvider(this.requireActivity()).get(AnalyseViewModel::class.java)
        _binding = FragmentAnalyseBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val prefs = requireContext().getAppCommonSharedPreferences()
        binding.run {
            blurredPicsPreview.invalidateProgress(filesViewModel.isImageBlurAnalysing) {
                filesViewModel.isImageBlurAnalysing = false
            }
            lowLightPreview.invalidateProgress(filesViewModel.isImageLowLightAnalysing) {
                filesViewModel.isImageLowLightAnalysing = false
            }
            memesPreview.invalidateProgress(filesViewModel.isImageMemesAnalysing) {
                filesViewModel.isImageMemesAnalysing = false
            }
            setVisibility(prefs)
            setClickListeners()

            val appDatabase = AppDatabase.getInstance(requireContext())
            val dao = appDatabase.analysisDao()
            val blurAnalysisDao = appDatabase.blurAnalysisDao()
            val lowLightAnalysisDao = appDatabase.lowLightAnalysisDao()
            val memeAnalysisDao = appDatabase.memesAnalysisDao()
            val pathPreferencesDao = appDatabase.pathPreferencesDao()
            val internalStorageDao = appDatabase.internalStorageAnalysisDao()

            analyseViewModel.getBlurImages(blurAnalysisDao).observe(viewLifecycleOwner) {
                if (it != null) {
                    blurredPicsPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.blurImagesLiveData = null
                        }
                    }
                }
            }

            analyseViewModel.getLowLightImages(lowLightAnalysisDao).observe(viewLifecycleOwner) {
                if (it != null) {
                    lowLightPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.lowLightImagesLiveData = null
                        }
                    }
                }
            }

            analyseViewModel.getMemeImages(memeAnalysisDao).observe(viewLifecycleOwner) {
                if (it != null) {
                    memesPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.memeImagesLiveData = null
                        }
                    }
                }
            }

            analyseViewModel.getSadImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    sadPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.sadImagesLiveData = null
                        }
                    }
                }
            }
            sadPreview.invalidateProgress(filesViewModel.isImageFeaturesAnalysing) {
                filesViewModel.isImageFeaturesAnalysing = false
            }

            analyseViewModel.getDistractedImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    distractedPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.distractedImagesLiveData = null
                        }
                    }
                }
            }
            distractedPreview.invalidateProgress(filesViewModel.isImageFeaturesAnalysing) {
                filesViewModel.isImageFeaturesAnalysing = false
            }

            analyseViewModel.getSleepingImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    sleepingPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.sleepingImagesLiveData = null
                        }
                    }
                }
            }
            sleepingPreview.invalidateProgress(filesViewModel.isImageFeaturesAnalysing) {
                filesViewModel.isImageFeaturesAnalysing = false
            }

            analyseViewModel.getSelfieImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    selfiePreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.selfieImagesLiveData = null
                        }
                    }
                }
            }
            selfiePreview.invalidateProgress(filesViewModel.isImageFeaturesAnalysing) {
                filesViewModel.isImageFeaturesAnalysing = false
            }

            analyseViewModel.getGroupPicImages(dao).observe(viewLifecycleOwner) {
                if (it != null) {
                    groupPicPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.groupPicImagesLiveData = null
                        }
                    }
                }
            }
            groupPicPreview.invalidateProgress(filesViewModel.isImageFeaturesAnalysing) {
                filesViewModel.isImageFeaturesAnalysing = false
            }

            val duplicatePref = prefs.getInt(
                PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
                PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
            )
            val doProgressDuplicateFiles = if (duplicatePref ==
                PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE
            ) {
                filesViewModel.isMediaStoreAnalysing
            } else {
                filesViewModel.isInternalStorageAnalysing
            }
            if (duplicatePref != PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE) {
                emptyFilesPreview.visibility = View.VISIBLE
                emptyFilesPreview.invalidateProgress(filesViewModel.isInternalStorageAnalysing) {
                    filesViewModel.isInternalStorageAnalysing = false
                }
                analyseViewModel.getEmptyFiles(internalStorageDao)
                    .observe(viewLifecycleOwner) {
                        if (it != null) {
                            emptyFilesPreview.loadPreviews(it) {
                                cleanButtonClick(it) {
                                    analyseViewModel.emptyFilesLiveData = null
                                }
                            }
                        }
                    }
            }
            val deepSearch = duplicatePref == PreferencesConstants
                .VAL_SEARCH_DUPLICATES_INTERNAL_DEEP
            val searchMediaFiles = duplicatePref == PreferencesConstants
                .VAL_SEARCH_DUPLICATES_MEDIA_STORE
            duplicateFilesPreview.invalidateProgress(doProgressDuplicateFiles) {
                if (duplicatePref ==
                    PreferencesConstants.VAL_SEARCH_DUPLICATES_MEDIA_STORE
                )
                    filesViewModel.isMediaStoreAnalysing = false
                else {
                    filesViewModel.isInternalStorageAnalysing = false
                }
            }
            analyseViewModel.getDuplicateDirectories(
                internalStorageDao, searchMediaFiles, deepSearch
            ).observe(viewLifecycleOwner) {
                if (it != null) {
                    duplicateFilesPreview.loadPreviews(it) {
                        cleanButtonClick(it) {
                            analyseViewModel.duplicateFilesLiveData = null
                        }
                    }
                }
            }

            filesViewModel.usedVideosSummaryTransformations()
                .observe(viewLifecycleOwner) { mediaFilePair ->
                    clutteredVideoPreview.invalidateProgress(true, null)
                    largeVideoPreview.invalidateProgress(true, null)
                    mediaFilePair?.let {
                        analyseViewModel.getClutteredVideos(mediaFilePair.second)
                            .observe(viewLifecycleOwner) { clutteredVideosInfo ->
                                clutteredVideosInfo?.let {
                                    clutteredVideoPreview.invalidateProgress(false, null)
                                    clutteredVideoPreview.loadPreviews(clutteredVideosInfo) {
                                        cleanButtonClick(it) {
                                            analyseViewModel.clutteredVideosLiveData = null
                                        }
                                    }
                                }
                            }
                        analyseViewModel.getLargeVideos(mediaFilePair.second)
                            .observe(viewLifecycleOwner) {
                                largeVideosList ->
                                largeVideosList?.let {
                                    largeVideoPreview.invalidateProgress(false, null)
                                    largeVideoPreview.loadPreviews(largeVideosList) {
                                        cleanButtonClick(it) {
                                            analyseViewModel.largeVideosLiveData = null
                                        }
                                    }
                                }
                            }
                    }
                }

            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_DOWNLOADS)) {
                analyseViewModel.getLargeDownloads(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        largeDownloads ->
                        largeDownloadPreview.invalidateProgress(true, null)
                        largeDownloads?.let {
                            largeDownloadPreview.invalidateProgress(false, null)
                            largeDownloadPreview.loadPreviews(largeDownloads) {
                                cleanButtonClick(it) {
                                    analyseViewModel.largeDownloadsLiveData = null
                                }
                            }
                        }
                    }
                analyseViewModel.getOldDownloads(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldDownloads ->
                    oldDownloadPreview.invalidateProgress(true, null)
                    oldDownloads?.let {
                        oldDownloadPreview.invalidateProgress(false, null)
                        oldDownloadPreview.loadPreviews(oldDownloads) {
                            cleanButtonClick(it) {
                                analyseViewModel.oldDownloadsLiveData = null
                            }
                        }
                    }
                }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS)) {
                analyseViewModel.getOldScreenshots(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        oldScreenshots ->
                        oldScreenshotsPreview.invalidateProgress(true, null)
                        oldScreenshots?.let {
                            oldScreenshotsPreview.invalidateProgress(false, null)
                            oldScreenshotsPreview.loadPreviews(oldScreenshots) {
                                cleanButtonClick(it) {
                                    analyseViewModel.oldScreenshotsLiveData = null
                                }
                            }
                        }
                    }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_RECORDING)) {
                analyseViewModel.getOldRecordings(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldRecordings ->
                    oldRecordingsPreview.invalidateProgress(true, null)
                    oldRecordings?.let {
                        oldRecordingsPreview.invalidateProgress(false, null)
                        oldRecordingsPreview.loadPreviews(oldRecordings) {
                            cleanButtonClick(it) {
                                analyseViewModel.oldRecordingsLiveData = null
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                unusedAppsPreview.visibility = View.VISIBLE
                if (!isUsageStatsPermissionGranted()) {
                    unusedAppsPreview.loadRequireElevatedPermission({
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                    }, {
                        reloadFragment()
                    })
                } else {
                    filesViewModel.getUnusedApps().observe(viewLifecycleOwner) {
                        mediaFileInfoList ->
                        unusedAppsPreview.invalidateProgress(true, null)
                        mediaFileInfoList?.let {
                            unusedAppsPreview.invalidateProgress(false, null)
                            unusedAppsPreview.loadPreviews(mediaFileInfoList) {
                                cleanButtonClick(it) {
                                    filesViewModel.unusedAppsLiveData = null
                                }
                            }
                        }
                    }
                }
            }
            filesViewModel.getLargeApps().observe(viewLifecycleOwner) {
                mediaFileInfoList ->
                largeAppsPreview.invalidateProgress(true, null)
                mediaFileInfoList?.let {
                    largeAppsPreview.invalidateProgress(false, null)
                    largeAppsPreview.loadPreviews(mediaFileInfoList) {
                        cleanButtonClick(it) {
                            filesViewModel.largeAppsLiveData = null
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gamesPreview.visibility = View.VISIBLE
                filesViewModel.getGamesInstalled().observe(viewLifecycleOwner) {
                    mediaFileInfoList ->
                    gamesPreview.invalidateProgress(true, null)
                    mediaFileInfoList?.let {
                        gamesPreview.invalidateProgress(false, null)
                        gamesPreview.loadPreviews(mediaFileInfoList) {
                            cleanButtonClick(it) {
                                filesViewModel.gamesInstalledLiveData = null
                            }
                        }
                    }
                }
            }

            filesViewModel.getApksLiveData().observe(viewLifecycleOwner) {
                mediaFileInfoList ->
                allApksPreview.invalidateProgress(true, null)
                mediaFileInfoList?.let {
                    allApksPreview.invalidateProgress(false, null)
                    allApksPreview.loadPreviews(mediaFileInfoList) {
                        cleanButtonClick(it) {
                            filesViewModel.apksLiveData = null
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

    override fun uninstallAppCallback(mediaFileInfo: MediaFileInfo) {
        if (shouldCallbackAppUninstall) {
            // callback only if this fragment is visible

            // reset interal storage stats so that we recalculate storage remaining
            filesViewModel.internalStorageStatsLiveData = null

            // below code is same in ItemsActionBarFragment, make sure to change there as well if any
            // currently no way to distinguish whether user is deleting large apps or unused apps
            // so we clear both
            filesViewModel.unusedAppsLiveData = null
            filesViewModel.largeAppsLiveData = null

            // deletion complete, no need to check analysis data to remove
            // as it will get deleted lazily while loading analysis lists
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.successfully_deleted)
            )
            reloadFragment()
        }
    }

    private fun cleanButtonClick(toDelete: List<MediaFileInfo>, deletedCallback: () -> Unit) {
        setupDeleteButton(toDelete) {
            // reset interal storage stats so that we recalculate storage remaining
            filesViewModel.internalStorageStatsLiveData = null
            deletedCallback.invoke()

            // deletion complete, no need to check analysis data to remove
            // as it will get deleted lazily while loading analysis lists
            requireContext().showToastOnBottom(
                resources
                    .getString(R.string.successfully_deleted)
            )
            reloadFragment()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun isUsageStatsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Utils.checkUsageStatsPermission(requireContext())
        } else {
            Utils.getAppsUsageStats(requireContext()).isNotEmpty()
        }
    }

    private fun reloadFragment() {
        val navController = NavHostFragment.findNavController(this)
        navController.popBackStack()
        navController.navigate(R.id.navigation_analyse)
    }

    private fun setVisibility(sharedPrefs: SharedPreferences) {
        binding.run {
            blurredPicsPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_BLUR
                )
            ) View.VISIBLE else View.GONE
            lowLightPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_LOW_LIGHT
                )
            ) View.VISIBLE else View.GONE

            memesPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_MEME
                )
            ) View.VISIBLE else View.GONE

            sadPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE

            sleepingPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE

            distractedPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE
            selfiePreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE
            groupPicPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE

            largeDownloadPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_DOWNLOADS
                )
            ) View.VISIBLE else View.GONE
            oldDownloadPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_DOWNLOADS
                )
            ) View.VISIBLE else View.GONE
            oldRecordingsPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_RECORDING
                )
            ) View.VISIBLE else View.GONE
            oldScreenshotsPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS
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
            clutteredVideoPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_CLUTTERED_VIDEOS,
                    this@AnalyseFragment
                )
            }
            largeVideoPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_VIDEOS,
                    this@AnalyseFragment
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                isUsageStatsPermissionGranted()
            ) {
                unusedAppsPreview.setOnClickListener {
                    shouldCallbackAppUninstall = false
                    ReviewImagesFragment.newInstance(
                        ReviewImagesFragment.TYPE_UNUSED_APPS,
                        this@AnalyseFragment
                    )
                }
            }
            largeAppsPreview.setOnClickListener {
                shouldCallbackAppUninstall = false
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_APPS,
                    this@AnalyseFragment
                )
            }
            allApksPreview.setOnClickListener {
                shouldCallbackAppUninstall = false
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_APK_FILES,
                    this@AnalyseFragment
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gamesPreview.setOnClickListener {
                    shouldCallbackAppUninstall = false
                    ReviewImagesFragment.newInstance(
                        ReviewImagesFragment.TYPE_GAMES_INSTALLED,
                        this@AnalyseFragment
                    )
                }
            }
        }
    }
}
