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
import com.amaze.fileutilities.R
import com.amaze.fileutilities.databinding.ActivityAboutBinding
import com.amaze.fileutilities.utilis.Utils
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder

class AboutActivity : AppCompatActivity() {

    private var _binding: ActivityAboutBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
