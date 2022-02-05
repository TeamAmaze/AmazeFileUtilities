package com.amaze.fileutilities.home_page.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.PathPreferences
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences

class PathPreferencesFragment: PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private var preferencesList: PreferenceCategory? = null
    private var featureName: Int? = null
    private val dao = AppDatabase.getInstance(requireContext()).pathPreferencesDao()
    private val sharedPrefs = requireContext().getAppCommonSharedPreferences()

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

        findPreference<Preference>("add_pref_path")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
//                showCreateDialog()
                true
            }
        preferencesList = findPreference("prefs_list")
        if (featureName!! != PathPreferences.FEATURE_AUDIO_PLAYER) {
            val preference = CheckBoxPreference(preferenceScreen.context)
            preference.title = getString(R.string.show_analysis)
            preference.key = "show_analysis"
            preference.setDefaultValue(sharedPrefs
                .getBoolean(PathPreferences.getSharedPreferenceKey(featureName!!),
                    PreferencesConstants.DEFAULT_ENABLED_ANALYSIS))
            preference.onPreferenceChangeListener = this
            preferenceScreen.addPreference(preference)
            preferencesList?.dependency = "show_analysis"
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
            val prefSwitch = PathSwitchPreference(requireContext(), null,
                itemOnDeleteListener)
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

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when(preference.key) {
            "show_analysis" -> {
                sharedPrefs.edit().putBoolean(PathPreferences.getSharedPreferenceKey(featureName!!),
                    newValue as Boolean
                ).apply()
            }
        }
        return true
    }
}