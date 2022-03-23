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
import com.amaze.fileutilities.home_page.ui.files.FilesViewModel
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.share.showShareDialog
import com.amaze.fileutilities.utilis.showToastInCenter
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder

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
        binding.privacyPolicy.setOnClickListener {
            Utils.openURL(Utils.URL_PRIVACY_POLICY, this)
        }
        binding.openSource.setOnClickListener {
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
            libsBuilder.start(this)
        }
        binding.shareLogs.setOnClickListener {
            var processed = false
            viewModel.getShareLogsAdapter().observe(this) {
                shareAdapter ->
                if (shareAdapter == null) {
                    if (processed) {
                        showToastInCenter(
                            applicationContext.resources
                                .getString(R.string.failed_to_extract_logs)
                        )
                    } else {
                        this.showToastInCenter(resources.getString(R.string.please_wait))
                        processed = true
                    }
                } else {
                    showShareDialog(this, this.layoutInflater, shareAdapter)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
