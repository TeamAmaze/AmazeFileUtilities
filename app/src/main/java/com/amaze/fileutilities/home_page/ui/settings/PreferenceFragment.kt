/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.home_page.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R

class PreferenceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_ANALYSIS = "analysis"
        private const val KEY_AUDIO_PLAYER = "audio_player"
        private const val KEY_IMAGE_VIEWER = "image_viewer"
        private val KEYS = listOf(KEY_APPEARANCE, KEY_ANALYSIS, KEY_AUDIO_PLAYER, KEY_IMAGE_VIEWER)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)
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
        when (preference.key) {
            KEY_APPEARANCE -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    AppearancePrefFragment(),
                    R.string.appearance
                )
            }
            KEY_ANALYSIS -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    AnalysisPrefFragment(),
                    R.string.analysis
                )
            }
            KEY_AUDIO_PLAYER -> {
                (activity as PreferenceActivity)
                    .inflatePreferenceFragment(AudioPlayerPrefFragment(), R.string.audio_player)
            }
            KEY_IMAGE_VIEWER -> {
                (activity as PreferenceActivity)
                    .inflatePreferenceFragment(
                        ImageViewerPrefFragment(),
                        R.string.image_viewer_normal
                    )
            }
        }
        return true
    }
}
