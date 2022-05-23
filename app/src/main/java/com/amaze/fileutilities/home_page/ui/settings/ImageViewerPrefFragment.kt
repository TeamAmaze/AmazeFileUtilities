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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

class ImageViewerPrefFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private lateinit var sharedPrefs: SharedPreferences

    companion object {
        private const val KEY_ENABLE_PALETTE = "pref_image_enable_palette"
        private val KEYS = emptyList<String>()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.image_viewer_prefs)
        sharedPrefs = requireContext().getAppCommonSharedPreferences()
        KEYS.forEach {
            findPreference<Preference>(it)?.onPreferenceClickListener = this
        }
        val enablePaletteChange = Preference.OnPreferenceChangeListener { pref, newValue ->
            sharedPrefs.edit().putBoolean(
                PreferencesConstants.KEY_ENABLE_IMAGE_PALETTE, newValue as Boolean
            ).apply()
            PreferencesConstants.DEFAULT_PALETTE_EXTRACT
        }
        val paletteCheckbox = findPreference<CheckBoxPreference>(KEY_ENABLE_PALETTE)
        paletteCheckbox?.setDefaultValue(
            sharedPrefs.getBoolean(
                PreferencesConstants.KEY_ENABLE_IMAGE_PALETTE,
                PreferencesConstants.DEFAULT_PALETTE_EXTRACT
            )
        )
        paletteCheckbox?.onPreferenceChangeListener = enablePaletteChange
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
        }
        return true
    }
}
