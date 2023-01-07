/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

package com.amaze.fileutilities.home_page

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.WelcomePermissionPrivacyLayoutBinding
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.showToastInCenter
import com.stephentuso.welcome.WelcomeFinisher

class PermissionFragmentWelcome : Fragment() {
    private var _binding: WelcomePermissionPrivacyLayoutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = WelcomePermissionPrivacyLayoutBinding.inflate(
            inflater, container,
            false
        )
        val root: View = binding.root
        binding.privacyPolicyButton.setOnClickListener {
            Utils.openURL(Utils.URL_PRIVACY_POLICY, requireContext())
        }
        binding.grantStorageButton.setOnClickListener {
            val activity = requireActivity() as WelcomeScreen
            activity.run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!checkStoragePermission()) {
                        requestStoragePermission(onPermissionGranted, true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestAllFilesAccess(onPermissionGranted)
                    }
                }
            }
        }
        binding.doneButton.setOnClickListener {
            val activity = requireActivity() as WelcomeScreen
            activity.run {
                if (!activity.haveStoragePermissions()) {
                    showToastInCenter(getString(R.string.grantfailed))
                } else if (!binding.licenseAgreementCheckbox.isChecked) {
                    showToastInCenter(getString(R.string.license_agreement_not_accepted))
                } else {
                    val component = ComponentName(this, MainActivity::class.java)
                    val action = Intent.makeRestartActivityTask(component)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    action.addCategory(Intent.CATEGORY_LAUNCHER)
                    startActivity(action)
                    WelcomeFinisher(this@PermissionFragmentWelcome).finish()
                }
            }
        }
        binding.licenseAgreementText.setOnClickListener {
            binding.licenseAgreementCheckbox.isChecked = !binding.licenseAgreementCheckbox.isChecked
        }
        binding.licenseAgreementText.text = Html.fromHtml(getString(R.string.license_agreement))
        Linkify.addLinks(binding.licenseAgreementText, Linkify.WEB_URLS)
        binding.licenseAgreementText.movementMethod = LinkMovementMethod.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Utils.isIgnoringBatteryOptimizations(requireContext())
        ) {
            binding.removeOptimizationsParent.visibility = View.VISIBLE
            binding.removeOptimizationsButton.setOnClickListener {
                Utils.invokeNotOptimizeBatteryScreen(requireContext())
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
