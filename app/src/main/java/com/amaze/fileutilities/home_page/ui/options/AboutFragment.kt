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

package com.amaze.fileutilities.home_page.ui.options

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.amaze.fileutilities.BuildConfig
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import com.amaze.fileutilities.home_page.ui.settings.PreferenceActivity
import com.amaze.fileutilities.utilis.PreferencesConstants
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.getAppCommonSharedPreferences
import com.amaze.fileutilities.utilis.log
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.amaze.fileutilities.utilis.showToastInCenter
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.mikepenz.aboutlibraries.LibsBuilder

class AboutFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private val filesViewModel: FilesViewModel by activityViewModels()

    companion object {
        private const val KEY_VERSION = "version"
        private const val KEY_ABOUT = "about"
        private const val KEY_LICENSE = "license"
        private const val KEY_PRIVACY_POLICY = "privacy_policy"
        private const val KEY_SUBMIT_ISSUE = "submit_issue"
        private const val KEY_LOGS = "share_logs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CONTACT = "contact"
        private const val KEY_TRANSLATE = "translate"
        private const val KEY_OPEN_SOURCE = "open_source"
        private const val KEY_SUBSCRIPTION_STATUS = "subscription_status"
        private val KEYS = listOf(
            KEY_VERSION,
            KEY_ABOUT, KEY_LICENSE, KEY_PRIVACY_POLICY, KEY_SUBMIT_ISSUE, KEY_LOGS, KEY_DEVICE_ID,
            KEY_CONTACT, KEY_TRANSLATE, KEY_OPEN_SOURCE, KEY_SUBSCRIPTION_STATUS
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_about)
        KEYS.forEach {
            findPreference<Preference>(it)?.onPreferenceClickListener = this
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val deviceIdPref = findPreference<Preference>(KEY_DEVICE_ID)
        val subscriptionStatus = findPreference<Preference>(KEY_SUBSCRIPTION_STATUS)
        val deviceId = requireActivity().getAppCommonSharedPreferences()
            .getString(PreferencesConstants.KEY_DEVICE_UNIQUE_ID, null)
        if (deviceId != null) {
            deviceIdPref?.summary = deviceId
            val dao = AppDatabase.getInstance(requireContext()).trialValidatorDao()
            val trial = dao.findByDeviceId(deviceId)
            if (trial != null) {
                subscriptionStatus?.summary =
                    if (trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_EXCLUSIVE ||
                        trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_UNOFFICIAL
                    ) {
                        trial.getTrialStatusName()
                    } else if (trial.trialStatus == TrialValidationApi.TrialResponse.TRIAL_ACTIVE &&
                        trial.subscriptionStatus == Trial.SUBSCRIPTION_STATUS_DEFAULT
                    ) {
                        trial.getTrialStatusName() +
                            " (${trial.trialDaysLeft} days left)"
                    } else if (trial.subscriptionStatus != Trial.SUBSCRIPTION_STATUS_DEFAULT) {
                        TrialValidationApi.TrialResponse.SUBSCRIPTION
                    } else {
                        trial.getTrialStatusName()
                    }
            }
        }

        val versionName = findPreference<Preference>(KEY_VERSION)
        versionName?.summary = BuildConfig.SUDO_VERSION_NAME
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
            KEY_VERSION -> {
                // do nothing
            }
            KEY_ABOUT -> {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
            }
            KEY_LICENSE -> {
                Utils.openURL(Utils.URL_LICENSE_AGREEMENT, requireContext())
            }
            KEY_PRIVACY_POLICY -> {
                Utils.openURL(Utils.URL_PRIVACY_POLICY, requireContext())
            }
            KEY_SUBMIT_ISSUE -> {
                Utils.openURL(Utils.URL_GITHUB_ISSUES, requireContext())
            }
            KEY_LOGS -> {
                var processed = false
                filesViewModel.getShareLogsAdapter().observe(this) {
                    shareAdapter ->
                    if (shareAdapter == null) {
                        if (processed) {
                            requireContext().showToastInCenter(
                                requireContext().resources
                                    .getString(R.string.failed_to_extract_logs)
                            )
                        } else {
                            requireContext()
                                .showToastInCenter(resources.getString(R.string.please_wait))
                            processed = true
                        }
                    } else {
                        showShareDialog(requireContext(), this.layoutInflater, shareAdapter)
                    }
                }
            }
            KEY_DEVICE_ID -> {
                val deviceId = requireActivity().getAppCommonSharedPreferences()
                    .getString(PreferencesConstants.KEY_DEVICE_UNIQUE_ID, null)
                if (deviceId != null) {
                    Utils.copyToClipboard(
                        requireContext(), deviceId,
                        getString(R.string.device_id_copied)
                    )
                    requireContext().showToastOnBottom(getString(R.string.device_id_copied))
                }
            }
            KEY_CONTACT -> {
                Utils.openTelegramURL(requireContext())
            }
            KEY_TRANSLATE -> {
                Utils.openTranslateURL(this.requireContext())
            }
            KEY_OPEN_SOURCE -> {
                val libsBuilder = LibsBuilder()
                    .withActivityTitle(getString(R.string.app_name))
                    .withAboutIconShown(true)
                    .withAboutVersionShownName(true)
                    .withAboutVersionShownCode(false)
                    .withAboutDescription(getString(R.string.about_app))
                    .withAboutSpecial1(getString(R.string.licenses))
                    .withAboutSpecial1Description(getString(R.string.amaze_license))
                    .withLicenseShown(true)
                libsBuilder.start(requireContext())
            }
            KEY_SUBSCRIPTION_STATUS -> {
                log.info("purchase subscription for device")
                Billing.getInstance(
                    requireActivity()
                        as PreferenceActivity
                )?.initiatePurchaseFlow()
            }
        }
        return true
    }
}
