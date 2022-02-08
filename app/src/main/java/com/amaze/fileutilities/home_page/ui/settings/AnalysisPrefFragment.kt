/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

class AnalysisPrefFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        private const val KEY_DUPLICATES = "search_duplicates"
        private const val KEY_MEMES = "meme_paths"
        private const val KEY_BLUR = "blur_paths"
        private const val KEY_LOW_LIGHT = "low_light_paths"
        private const val KEY_FEATURES = "features_paths"
        private const val KEY_DOWNLOAD = "download_paths"
        private const val KEY_RECORDING = "recording_paths"
        private const val KEY_SCREENSHOT = "screenshot_paths"
        private const val KEY_TELEGRAM = "telegram_paths"
        private val KEYS = listOf(
            KEY_DUPLICATES, KEY_MEMES, KEY_BLUR, KEY_LOW_LIGHT, KEY_FEATURES, KEY_DOWNLOAD,
            KEY_RECORDING, KEY_SCREENSHOT, KEY_TELEGRAM
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.analysis_prefs)
        KEYS.forEach {
            findPreference<Preference>(it)?.onPreferenceClickListener = this
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.background = null
        return view
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val prefs = requireContext().getAppCommonSharedPreferences()
        when (preference.key) {
            KEY_DUPLICATES -> {
                val searchIdx = prefs.getInt(
                    PreferencesConstants.KEY_SEARCH_DUPLICATES_IN,
                    PreferencesConstants.DEFAULT_SEARCH_DUPLICATES_IN
                )
                val dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.duplicates)
                    .setSingleChoiceItems(
                        arrayOf(
                            getString(R.string.media_store),
                            getString(R.string.internal_storage_shallow),
                            getString(R.string.internal_storage_deep)
                        ),
                        searchIdx
                    ) { dialog, p1 ->
                        prefs.edit()
                            .putInt(PreferencesConstants.KEY_SEARCH_DUPLICATES_IN, p1).apply()
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
            }
            KEY_BLUR -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment.newInstance(PathPreferences.FEATURE_ANALYSIS_BLUR),
                    R.string.blurred_pics
                )
            }
            KEY_LOW_LIGHT -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment.newInstance(PathPreferences.FEATURE_ANALYSIS_LOW_LIGHT),
                    R.string.low_light
                )
            }
            KEY_MEMES -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment.newInstance(PathPreferences.FEATURE_ANALYSIS_MEME),
                    R.string.memes
                )
            }
            KEY_FEATURES -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_IMAGE_FEATURES),
                    R.string.image_features
                )
            }
            KEY_DOWNLOAD -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_DOWNLOADS),
                    R.string.download_paths
                )
            }
            KEY_RECORDING -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_RECORDING),
                    R.string.old_recordings
                )
            }
            KEY_SCREENSHOT -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_SCREENSHOTS),
                    R.string.old_screenshots
                )
            }
            KEY_TELEGRAM -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_TELEGRAM),
                    R.string.telegram_files
                )
            }
        }
        return true
    }
}
