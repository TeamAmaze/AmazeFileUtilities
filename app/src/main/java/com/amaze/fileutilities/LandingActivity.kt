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
            requestStoragePermission(object: OnPermissionGrantedCallback{
                override fun onPermissionGranted() {
                    landingActivityBinding.notGrantedTextView.visibility = View.GONE
                    landingActivityBinding.grantButton.visibility = View.GONE
                }

                override fun onPermissionNotGranted() {
                    landingActivityBinding.notGrantedTextView.visibility = View.VISIBLE
                    landingActivityBinding.grantButton.visibility = View.VISIBLE
                }

            }, true)
        }
    }
}