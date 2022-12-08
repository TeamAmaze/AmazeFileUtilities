/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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
import com.amaze.fileutilities.utilis.Utils
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
        private const val KEY_UNUSED_APPS = "unused_apps"
        private const val KEY_MOST_USED_APPS = "most_used_apps"
        private const val KEY_LEAST_USED_APPS = "least_used_apps"
        private const val KEY_WHATSAPP_MEDIA = "whatsapp_media"
        private val KEYS = listOf(
            KEY_DUPLICATES, KEY_MEMES, KEY_BLUR, KEY_LOW_LIGHT, KEY_FEATURES, KEY_DOWNLOAD,
            KEY_RECORDING, KEY_SCREENSHOT, KEY_UNUSED_APPS, KEY_MOST_USED_APPS, KEY_LEAST_USED_APPS,
            KEY_WHATSAPP_MEDIA, KEY_TELEGRAM
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
                val dialog = AlertDialog.Builder(requireContext(), R.style.Custom_Dialog_Dark)
                    .setTitle(R.string.duplicates)
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
            KEY_WHATSAPP_MEDIA -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment
                        .newInstance(PathPreferences.FEATURE_ANALYSIS_WHATSAPP),
                    R.string.whatsapp_media
                )
            }
            KEY_UNUSED_APPS -> {
                val days = prefs.getInt(
                    PreferencesConstants.KEY_UNUSED_APPS_DAYS,
                    PreferencesConstants.DEFAULT_UNUSED_APPS_DAYS
                )
                Utils.buildDigitInputDialog(requireContext(), days) {
                    prefs.edit().putInt(PreferencesConstants.KEY_UNUSED_APPS_DAYS, it).apply()
                }
            }
            KEY_MOST_USED_APPS -> {
                val days = prefs.getInt(
                    PreferencesConstants.KEY_MOST_USED_APPS_DAYS,
                    PreferencesConstants.DEFAULT_MOST_USED_APPS_DAYS
                )
                Utils.buildDigitInputDialog(requireContext(), days) {
                    prefs.edit().putInt(PreferencesConstants.KEY_MOST_USED_APPS_DAYS, it).apply()
                }
            }
            KEY_LEAST_USED_APPS -> {
                val days = prefs.getInt(
                    PreferencesConstants.KEY_LEAST_USED_APPS_DAYS,
                    PreferencesConstants.DEFAULT_LEAST_USED_APPS_DAYS
                )
                Utils.buildDigitInputDialog(requireContext(), days) {
                    prefs.edit().putInt(PreferencesConstants.KEY_LEAST_USED_APPS_DAYS, it).apply()
                }
            }
        }
        return true
    }
}
