/*
 * Copyright (C) 2021-2021 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities

import android.os.Build
import android.os.Bundle
import android.view.View
import com.amaze.fileutilities.databinding.LandingActivityBinding

class LandingActivity : PermissionActivity() {

    private lateinit var landingActivityBinding: LandingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        landingActivityBinding = LandingActivityBinding.inflate(layoutInflater)
        setContentView(landingActivityBinding.root)
        requestStorageTrigger()
        landingActivityBinding.grantButton.setOnClickListener {
            startExplicitPermissionActivity()
        }
    }

    private fun requestStorageTrigger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkStoragePermission()) {
            requestStoragePermission(
                object : OnPermissionGrantedCallback {
                    override fun onPermissionGranted() {
                        landingActivityBinding.notGrantedTextView.visibility = View.GONE
                        landingActivityBinding.grantButton.visibility = View.GONE
                    }

                    override fun onPermissionNotGranted() {
                        landingActivityBinding.notGrantedTextView.visibility = View.VISIBLE
                        landingActivityBinding.grantButton.visibility = View.VISIBLE
                    }
                },
                true
            )
        }
    }
}
