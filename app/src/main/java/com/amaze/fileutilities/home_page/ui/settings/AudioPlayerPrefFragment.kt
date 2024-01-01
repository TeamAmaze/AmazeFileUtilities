/*
 * Copyright (C) 2021-2024 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

class AudioPlayerPrefFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        private const val KEY_EXCLUSIONS = "exclusion_audio_player"
        private const val KEY_ENABLE_WAVEFORM = "pref_enable_waveform"
        private const val KEY_ENABLE_PALETTE = "pref_audio_enable_palette"
        private const val KEY_REMOVE_BATTERY_OPTIMIZATIONS = "remove_battery_optimizations"
        private val KEYS = listOf(KEY_EXCLUSIONS, KEY_REMOVE_BATTERY_OPTIMIZATIONS)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.audio_player_prefs)
        sharedPrefs = requireContext().getAppCommonSharedPreferences()
        KEYS.forEach {
            findPreference<Preference>(it)?.onPreferenceClickListener = this
        }
        val enableWaveformChange = Preference.OnPreferenceChangeListener { pref, newValue ->
            sharedPrefs.edit().putBoolean(
                PreferencesConstants.KEY_ENABLE_WAVEFORM, newValue as Boolean
            ).apply()
            true
        }
        val enablePaletteChange = Preference.OnPreferenceChangeListener { pref, newValue ->
            sharedPrefs.edit().putBoolean(
                PreferencesConstants.KEY_ENABLE_AUDIO_PALETTE, newValue as Boolean
            ).apply()
            PreferencesConstants.DEFAULT_PALETTE_EXTRACT
        }
        val waveformCheckbox = findPreference<CheckBoxPreference>(KEY_ENABLE_WAVEFORM)
        waveformCheckbox?.setDefaultValue(
            sharedPrefs.getBoolean(
                PreferencesConstants.KEY_ENABLE_WAVEFORM,
                PreferencesConstants.DEFAULT_AUDIO_PLAYER_WAVEFORM
            )
        )
        val paletteCheckbox = findPreference<CheckBoxPreference>(KEY_ENABLE_PALETTE)
        paletteCheckbox?.setDefaultValue(
            sharedPrefs.getBoolean(
                PreferencesConstants.KEY_ENABLE_AUDIO_PALETTE,
                PreferencesConstants.DEFAULT_PALETTE_EXTRACT
            )
        )
        waveformCheckbox?.onPreferenceChangeListener = enableWaveformChange
        paletteCheckbox?.onPreferenceChangeListener = enablePaletteChange
        val removeOptimizationsPref = findPreference<Preference>(
            KEY_REMOVE_BATTERY_OPTIMIZATIONS
        )
        removeOptimizationsPref?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Utils.isIgnoringBatteryOptimizations(requireContext())
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
            KEY_EXCLUSIONS -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment.newInstance(PathPreferences.FEATURE_AUDIO_PLAYER),
                    R.string.audio_player
                )
            }
            KEY_REMOVE_BATTERY_OPTIMIZATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Utils.invokeNotOptimizeBatteryScreen(requireContext())
                }
            }
        }
        return true
    }
}
