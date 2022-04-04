/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
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
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.amaze.fileutilities.utilis.showToastInCenter
import com.amaze.fileutilities.utilis.showToastOnBottom
import com.mikepenz.aboutlibraries.Libs
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
        private const val KEY_OPEN_SOURCE = "open_source"
        private val KEYS = listOf(
            KEY_VERSION,
            KEY_ABOUT, KEY_LICENSE, KEY_PRIVACY_POLICY, KEY_SUBMIT_ISSUE, KEY_LOGS, KEY_DEVICE_ID,
            KEY_CONTACT, KEY_OPEN_SOURCE
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
        filesViewModel.getUniqueId().observe(viewLifecycleOwner) {
            deviceId ->
            deviceIdPref?.summary = deviceId
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
            KEY_VERSION -> {
                // do nothing
            }
            KEY_ABOUT -> {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
            }
            KEY_LICENSE -> {
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
                filesViewModel.getUniqueId().observe(viewLifecycleOwner) {
                    deviceId ->
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
                libsBuilder.withActivityStyle(Libs.ActivityStyle.DARK)
                libsBuilder.start(requireContext())
            }
        }
        return true
    }
}
