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
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.home_page.database.PathPreferencesDao
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.showFolderChooserDialog

class PathPreferencesFragment : PreferenceFragmentCompat() {

    private var preferencesList: PreferenceCategory? = null
    private var featureName: Int? = null
    private lateinit var dao: PathPreferencesDao
    private lateinit var sharedPrefs: SharedPreferences

    private val preferenceDbMap: MutableMap<Preference, PathPreferences> = HashMap()

    private val itemOnDeleteListener = { it: PathSwitchPreference ->
        showDeleteDialog(it)
    }

    companion object {

        const val KEY_FEATURE_NAME = "feature_name"

        fun newInstance(featureName: Int): PathPreferencesFragment {
            val pathPrefsFragment = PathPreferencesFragment()
            pathPrefsFragment.apply {
                val bundle = Bundle()
                bundle.putInt(KEY_FEATURE_NAME, featureName)
                arguments = bundle
            }
            return pathPrefsFragment
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.path_prefs, rootKey)
        featureName = arguments?.getInt(KEY_FEATURE_NAME)
        dao = AppDatabase.getInstance(requireContext()).pathPreferencesDao()
        sharedPrefs = requireContext().getAppCommonSharedPreferences()

        findPreference<Preference>("add_pref_path")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                showCreateDialog()
                true
            }
        preferencesList = findPreference("prefs_list")
        if (featureName!! != PathPreferences.FEATURE_AUDIO_PLAYER) {
            val preference = CheckBoxPreference(preferenceScreen.context)
            preference.title = getString(R.string.show_analysis)
            preference.key = PathPreferences.getSharedPreferenceKey(featureName!!)
            preference.setDefaultValue(
                sharedPrefs.getBoolean(
                    PathPreferences
                        .getSharedPreferenceKey(featureName!!),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS
                )
            )
            preference.order = 0
            preferencesList?.order = 1
            preferenceScreen.addPreference(preference)
            preferencesList?.dependency = PathPreferences.getSharedPreferenceKey(featureName!!)
        }
        reload()
    }

    private fun reload() {
        for (p in preferenceDbMap) {
            preferencesList?.removePreference(p.key)
        }

        preferenceDbMap.clear()
        val preferences = dao.findByFeature(featureName!!)
        preferences.forEach {
            val prefSwitch = PathSwitchPreference(
                requireContext(), null,
                itemOnDeleteListener
            )
            preferenceDbMap[prefSwitch] = it
            prefSwitch.summary = it.path
            preferencesList?.addPreference(prefSwitch)
        }
    }

    private fun showDeleteDialog(prefSwitch: PathSwitchPreference) {
        val dialog = AlertDialog.Builder(requireContext()).setTitle(R.string.delete_preference)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val prefDb = preferenceDbMap[prefSwitch]
                prefDb?.let { dao.delete(it) }
                preferencesList?.removePreference(prefSwitch)
                preferenceDbMap.remove(prefSwitch)
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.show()
    }

    private fun showCreateDialog() {
        requireContext().showFolderChooserDialog {
            val pathPreferences = PathPreferences(
                it.path, featureName!!,
                featureName == PathPreferences.FEATURE_AUDIO_PLAYER
            )
            dao.insert(pathPreferences)
            val prefSwitch = PathSwitchPreference(
                requireContext(), null,
                itemOnDeleteListener
            )
            preferenceDbMap[prefSwitch] = pathPreferences
            prefSwitch.summary = it.path
            preferencesList?.addPreference(prefSwitch)
        }
    }
}
