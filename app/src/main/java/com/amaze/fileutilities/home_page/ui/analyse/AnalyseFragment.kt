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

package com.amaze.fileutilities.home_page.ui.analyse

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.amaze.fileutilities.BuildConfig
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
import kotlin.concurrent.thread

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
            val similarImagesAnalysisDao = appDatabase.similarImagesAnalysisDao()
            val installedAppsDao = AppDatabase.getInstance(requireContext()).installedAppsDao()

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

            /*analyseViewModel.getSadImages(dao).observe(viewLifecycleOwner) {
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
            }*/

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

            analyseViewModel.getSimilarImages(similarImagesAnalysisDao)
                .observe(viewLifecycleOwner) {
                    if (it != null) {
                        similarImagesPreview.loadPreviews(it) {
                            val checksumHash: HashMap<String, Boolean> = HashMap()
                            cleanButtonClick(
                                it.filter {
                                    mediaFile ->
                                    val fileChecksum = mediaFile.extraInfo?.extraMetaData?.checksum
                                    if (fileChecksum == null ||
                                        checksumHash[fileChecksum] == true
                                    ) {
                                        true
                                    } else {
                                        checksumHash[fileChecksum] = true
                                        false
                                    }
                                }
                            ) {
                                analyseViewModel.similarImagesLiveData = null
                            }
                        }
                    }
                }
            similarImagesPreview.invalidateProgress(filesViewModel.isSimilarImagesAnalysing) {
                filesViewModel.isSimilarImagesAnalysing = false
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

            filesViewModel.getLargeFilesLiveData().observe(viewLifecycleOwner) {
                largeFiles ->
                largeFilesPreview.invalidateProgress(true, null)
                largeFiles?.let {
                    largeFilesPreview.invalidateProgress(false, null)
                    largeFilesPreview.loadPreviews(largeFiles) {
                        cleanButtonClick(it) {
                            filesViewModel.largeFilesMutableLiveData = null
                        }
                    }
                }
            }

            filesViewModel.getJunkFilesLiveData().observe(viewLifecycleOwner) {
                junkFiles ->
                junkFilesPreview.invalidateProgress(true, null)
                junkFiles?.let {
                    junkFilesPreview.invalidateProgress(false, null)
                    junkFilesPreview.loadSummaryTextPreview(
                        if (it.first.isEmpty())
                            null else it.second,
                        null
                    ) {
                        cleanButtonClick(it.first) {
                            filesViewModel.junkFilesLiveData = null
                            thread {
                                installedAppsDao.deleteAll()
                            }
                        }
                    }
                }
            }

            analyseViewModel.getDuplicateDirectories(
                internalStorageDao, searchMediaFiles, deepSearch
            ).observe(viewLifecycleOwner) {
                if (it != null) {
                    duplicateFilesPreview.loadPreviews(it) {
                        val checksumHash: HashMap<String, Boolean> = HashMap()
                        cleanButtonClick(
                            it.filter {
                                mediaFile ->
                                val fileChecksum = mediaFile.extraInfo?.extraMetaData?.checksum
                                if (fileChecksum == null || checksumHash[fileChecksum] == true) {
                                    true
                                } else {
                                    checksumHash[fileChecksum] = true
                                    false
                                }
                            }
                        ) {
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
                                    clutteredVideoPreview.invalidateProgress(
                                        false,
                                        null
                                    )
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
                                    largeVideoPreview.invalidateProgress(
                                        false,
                                        null
                                    )
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
                filesViewModel.getLargeDownloads(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        largeDownloads ->
                        largeDownloadPreview.invalidateProgress(true, null)
                        largeDownloads?.let {
                            largeDownloadPreview.invalidateProgress(
                                false,
                                null
                            )
                            largeDownloadPreview.loadPreviews(largeDownloads) {
                                cleanButtonClick(it) {
                                    filesViewModel.largeDownloadsLiveData = null
                                }
                            }
                        }
                    }
                filesViewModel.getOldDownloads(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldDownloads ->
                    oldDownloadPreview.invalidateProgress(true, null)
                    oldDownloads?.let {
                        oldDownloadPreview.invalidateProgress(false, null)
                        oldDownloadPreview.loadPreviews(oldDownloads) {
                            cleanButtonClick(it) {
                                filesViewModel.oldDownloadsLiveData = null
                            }
                        }
                    }
                }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS)) {
                filesViewModel.getOldScreenshots(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        oldScreenshots ->
                        oldScreenshotsPreview.invalidateProgress(
                            true,
                            null
                        )
                        oldScreenshots?.let {
                            oldScreenshotsPreview.invalidateProgress(
                                false,
                                null
                            )
                            oldScreenshotsPreview.loadPreviews(oldScreenshots) {
                                cleanButtonClick(it) {
                                    filesViewModel.oldScreenshotsLiveData = null
                                }
                            }
                        }
                    }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_RECORDING)) {
                filesViewModel.getOldRecordings(pathPreferencesDao).observe(viewLifecycleOwner) {
                    oldRecordings ->
                    oldRecordingsPreview.invalidateProgress(true, null)
                    oldRecordings?.let {
                        oldRecordingsPreview.invalidateProgress(false, null)
                        oldRecordingsPreview.loadPreviews(oldRecordings) {
                            cleanButtonClick(it) {
                                filesViewModel.oldRecordingsLiveData = null
                            }
                        }
                    }
                }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_WHATSAPP)) {
                filesViewModel.getWhatsappMediaLiveData(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        whatsappMedia ->
                        whatsappPreview.invalidateProgress(true, null)
                        whatsappMedia?.let {
                            whatsappPreview.invalidateProgress(false, null)
                            whatsappPreview.loadPreviews(whatsappMedia) {
                                cleanButtonClick(it) {
                                    filesViewModel.whatsappMediaMutableLiveData = null
                                }
                            }
                        }
                    }
            }
            if (PathPreferences.isEnabled(prefs, PathPreferences.FEATURE_ANALYSIS_TELEGRAM)) {
                filesViewModel.getTelegramMediaFiles(pathPreferencesDao)
                    .observe(viewLifecycleOwner) {
                        telegramMedia ->
                        telegramPreview.invalidateProgress(true, null)
                        telegramMedia?.let {
                            telegramPreview.invalidateProgress(false, null)
                            telegramPreview.loadPreviews(telegramMedia) {
                                cleanButtonClick(it) {
                                    filesViewModel.telegramMediaMutableLiveData = null
                                }
                            }
                        }
                    }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                memoryUsagePreview.visibility = View.VISIBLE
                filesViewModel.getMemoryInfo().observe(viewLifecycleOwner) {
                    memoryUsage ->
                    memoryUsagePreview.invalidateProgress(true, null)
                    memoryUsage?.let {
                        memoryUsagePreview.invalidateProgress(false, null)
                        memoryUsagePreview.loadSummaryTextPreview(it, {
                            filesViewModel.memoryInfoLiveData = null
                            reloadFragment()
                        }, {
                            filesViewModel.killBackgroundProcesses(
                                requireContext()
                                    .packageManager
                            ) {
                                requireActivity().runOnUiThread {
                                    requireContext()
                                        .showToastOnBottom(getString(R.string.ram_usage_clear))
                                    filesViewModel.memoryInfoLiveData = null
                                    reloadFragment()
                                }
                            }
                        })
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
                                cleanButtonClick(it, true) {
                                    filesViewModel.unusedAppsLiveData = null
                                }
                            }
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                mostUsedAppsPreview.visibility = View.VISIBLE
                leastUsedAppsPreview.visibility = View.VISIBLE
                if (!isUsageStatsPermissionGranted()) {
                    mostUsedAppsPreview.loadRequireElevatedPermission({
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                    }, {
                        reloadFragment()
                    })
                    leastUsedAppsPreview.loadRequireElevatedPermission({
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivity(intent)
                    }, {
                        reloadFragment()
                    })
                } else {
                    filesViewModel.getMostUsedApps().observe(viewLifecycleOwner) {
                        mediaFileInfoList ->
                        mostUsedAppsPreview.invalidateProgress(true, null)
                        mediaFileInfoList?.let {
                            mostUsedAppsPreview.invalidateProgress(
                                false,
                                null
                            )
                            mostUsedAppsPreview.loadPreviews(mediaFileInfoList) {
                                cleanButtonClick(it, true) {
                                    filesViewModel.mostUsedAppsLiveData = null
                                }
                            }
                        }
                    }
                    filesViewModel.getLeastUsedApps().observe(viewLifecycleOwner) {
                        mediaFileInfoList ->
                        leastUsedAppsPreview.invalidateProgress(true, null)
                        mediaFileInfoList?.let {
                            leastUsedAppsPreview.invalidateProgress(
                                false,
                                null
                            )
                            leastUsedAppsPreview.loadPreviews(mediaFileInfoList) {
                                cleanButtonClick(it, true) {
                                    filesViewModel.leastUsedAppsLiveData = null
                                }
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                !isUsageStatsPermissionGranted()
            ) {
                networkIntensiveAppsPreview.loadRequireElevatedPermission({
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivity(intent)
                }, {
                    reloadFragment()
                })
            } else {
                filesViewModel.getNetworkIntensiveApps().observe(viewLifecycleOwner) {
                    mediaFileInfoList ->
                    networkIntensiveAppsPreview.invalidateProgress(true, null)
                    mediaFileInfoList?.let {
                        networkIntensiveAppsPreview.invalidateProgress(
                            false,
                            null
                        )
                        networkIntensiveAppsPreview.loadPreviews(mediaFileInfoList) {
                            cleanButtonClick(it, true) {
                                filesViewModel.networkIntensiveAppsLiveData = null
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
                        cleanButtonClick(it, true) {
                            filesViewModel.largeAppsLiveData = null
                        }
                    }
                }
            }
            filesViewModel.getNewlyInstalledApps().observe(viewLifecycleOwner) {
                mediaFileInfoList ->
                newlyInstalledAppsPreview.invalidateProgress(true, null)
                mediaFileInfoList?.let {
                    newlyInstalledAppsPreview.invalidateProgress(false, null)
                    newlyInstalledAppsPreview.loadPreviews(mediaFileInfoList) {
                        cleanButtonClick(it, true) {
                            filesViewModel.newlyInstalledAppsLiveData = null
                        }
                    }
                }
            }
            filesViewModel.getRecentlyUpdatedApps().observe(viewLifecycleOwner) {
                mediaFileInfoList ->
                recentlyUpdatedAppsPreview.invalidateProgress(true, null)
                mediaFileInfoList?.let {
                    recentlyUpdatedAppsPreview.invalidateProgress(false, null)
                    recentlyUpdatedAppsPreview.loadPreviews(mediaFileInfoList) {
                        cleanButtonClick(it, true) {
                            filesViewModel.recentlyUpdatedAppsLiveData = null
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
                            cleanButtonClick(it, true) {
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

            filesViewModel.getHiddenFilesLiveData().observe(viewLifecycleOwner) {
                mediaFileInfoList ->
                hiddenFilesPreview.invalidateProgress(true, null)
                mediaFileInfoList?.let {
                    hiddenFilesPreview.invalidateProgress(false, null)
                    hiddenFilesPreview.loadPreviews(mediaFileInfoList) {
                        cleanButtonClick(it) {
                            filesViewModel.hiddenFilesLiveData = null
                        }
                    }
                }
            }
            if (analyseViewModel.fragmentScrollPosition != null) {
                Handler().postDelayed({
                    analyseScrollView.scrollY = analyseViewModel.fragmentScrollPosition!!
                    analyseViewModel.fragmentScrollPosition = null
                }, 1000)
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

    private fun cleanButtonClick(
        toDelete: List<MediaFileInfo>,
        shouldDeletePermanently: Boolean = false,
        deletedCallback: () -> Unit
    ) {
        if (shouldDeletePermanently) {
            setupDeletePermanentlyButton(toDelete) {
                // reset interal storage stats so that we recalculate storage remaining
                filesViewModel.internalStorageStatsLiveData = null
                filesViewModel.resetTrashBinConfig()
                deletedCallback.invoke()

                // deletion complete, no need to check analysis data to remove
                // as it will get deleted lazily while loading analysis lists
                requireContext().showToastOnBottom(
                    resources
                        .getString(R.string.successfully_deleted)
                )
                reloadFragment()
            }
        } else {
            setupDeleteButton(toDelete) {
                // reset interal storage stats so that we recalculate storage remaining
                filesViewModel.internalStorageStatsLiveData = null
                filesViewModel.resetTrashBinConfig()
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
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun isUsageStatsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Utils.checkUsageStatsPermission(requireContext())
        } else {
            Utils.getAppsUsageStats(requireContext(), 30).isNotEmpty()
        }
    }

    private fun reloadFragment() {
        analyseViewModel.fragmentScrollPosition = binding.analyseScrollView.scrollY
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
                ) && !BuildConfig.IS_VERSION_FDROID
            ) View.VISIBLE else View.GONE

            /*sadPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE

            distractedPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                )
            ) View.VISIBLE else View.GONE*/

            sleepingPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                ) && !BuildConfig.IS_VERSION_FDROID
            ) View.VISIBLE else View.GONE
            selfiePreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                ) && !BuildConfig.IS_VERSION_FDROID
            ) View.VISIBLE else View.GONE
            groupPicPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES
                ) && !BuildConfig.IS_VERSION_FDROID
            ) View.VISIBLE else View.GONE
            similarImagesPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_SIMILAR_IMAGES
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
            whatsappPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_WHATSAPP
                )
            ) View.VISIBLE else View.GONE
            telegramPreview.visibility = if (PathPreferences.isEnabled(
                    sharedPrefs,
                    PathPreferences.FEATURE_ANALYSIS_TELEGRAM
                )
            ) View.VISIBLE else View.GONE
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
            /*sadPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SAD,
                    this@AnalyseFragment
                )
            }
            distractedPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_DISTRACTED,
                    this@AnalyseFragment
                )
            }*/
            sleepingPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SLEEPING,
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
            similarImagesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_SIMILAR_IMAGES,
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
            largeFilesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_FILES,
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
            largeFilesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_LARGE_FILES,
                    this@AnalyseFragment
                )
            }
            whatsappPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_WHATSAPP,
                    this@AnalyseFragment
                )
            }
            telegramPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_TELEGRAM,
                    this@AnalyseFragment
                )
            }
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
                mostUsedAppsPreview.setOnClickListener {
                    shouldCallbackAppUninstall = false
                    ReviewImagesFragment.newInstance(
                        ReviewImagesFragment.TYPE_MOST_USED_APPS,
                        this@AnalyseFragment
                    )
                }
                leastUsedAppsPreview.setOnClickListener {
                    shouldCallbackAppUninstall = false
                    ReviewImagesFragment.newInstance(
                        ReviewImagesFragment.TYPE_LEAST_USED_APPS,
                        this@AnalyseFragment
                    )
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 ||
                isUsageStatsPermissionGranted()
            ) {
                shouldCallbackAppUninstall = false
                networkIntensiveAppsPreview.setOnClickListener {
                    ReviewImagesFragment.newInstance(
                        ReviewImagesFragment.TYPE_NETWORK_INTENSIVE_APPS,
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
            newlyInstalledAppsPreview.setOnClickListener {
                shouldCallbackAppUninstall = false
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_NEWLY_INSTALLED_APPS,
                    this@AnalyseFragment
                )
            }
            recentlyUpdatedAppsPreview.setOnClickListener {
                shouldCallbackAppUninstall = false
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_RECENTLY_UPDATED_APPS,
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
            hiddenFilesPreview.setOnClickListener {
                ReviewImagesFragment.newInstance(
                    ReviewImagesFragment.TYPE_HIDDEN_FILES,
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
