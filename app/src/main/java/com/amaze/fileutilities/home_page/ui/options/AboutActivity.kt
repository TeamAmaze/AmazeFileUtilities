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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityAboutBinding
import com.amaze.fileutilities.home_page.database.AppDatabase
import com.amaze.fileutilities.home_page.database.Trial
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi

class AboutActivity : AppCompatActivity() {

    private var _binding: ActivityAboutBinding? = null
    private lateinit var viewModel: FilesViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FilesViewModel::class.java)
        _binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.getUniqueId().observe(this) {
            deviceId ->
            val dao = AppDatabase.getInstance(applicationContext).trialValidatorDao()
            val trial = dao.findByDeviceId(deviceId)
            if (trial != null) {
                binding.subscriptionStatus.text = getString(R.string.subscription_status)
                    .format(trial.deviceId)
            }
            trial?.let {
                if (it.trialStatus == TrialValidationApi.TrialResponse.TRIAL_ACTIVE &&
                    it.subscriptionStatus == Trial.SUBSCRIPTION_STATUS_DEFAULT
                ) {
                    binding.subscriptionStatus.text = getString(R.string.subscription_status)
                        .format(trial.getTrialStatusName()) + " (${it.trialDaysLeft} days left)"
                } else {
                    binding.subscriptionStatus.text = getString(R.string.subscription_status)
                        .format(trial.getTrialStatusName())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
