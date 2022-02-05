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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.PathPreferences

class AudioPlayerPrefFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    companion object {
        private const val KEY_EXCLUSIONS = "exclusion_audio_player"
        private val KEYS = listOf(KEY_EXCLUSIONS)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.audio_player_prefs)
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
        when(preference.key) {
            KEY_EXCLUSIONS -> {
                (activity as PreferenceActivity).inflatePreferenceFragment(
                    PathPreferencesFragment.newInstance(PathPreferences.FEATURE_AUDIO_PLAYER),
                    R.string.audio_player
                )
            }
        }
        return true
    }
}
