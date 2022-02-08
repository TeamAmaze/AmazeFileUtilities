/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.Utils
import com.amaze.fileutilities.utilis.showToastInCenter
import java.lang.Exception

open class PermissionsActivity :
    AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkStoragePermission()) {
                requestStoragePermission(onPermissionGranted, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestAllFilesAccess(onPermissionGranted)
            }
        }
    }

    private val TAG = PermissionsActivity::class.java.simpleName

    companion object {
        private const val PERMISSION_LENGTH = 3
        private const val STORAGE_PERMISSION = 0
        private const val ALL_FILES_PERMISSION = 2
    }

    private val permissionCallbacks = arrayOfNulls<OnPermissionGranted>(PERMISSION_LENGTH)

    private val onPermissionGranted = object : OnPermissionGranted {
        override fun onPermissionGranted() {
            val action = Intent(this@PermissionsActivity, MainActivity::class.java)
            action.addCategory(Intent.CATEGORY_LAUNCHER)
            startActivity(action)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION) {
            if (isGranted(grantResults)) {
                Utils.enableScreenRotation(this)
                permissionCallbacks[STORAGE_PERMISSION]!!.onPermissionGranted()
                permissionCallbacks[STORAGE_PERMISSION] = null
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                requestStoragePermission(
                    permissionCallbacks[STORAGE_PERMISSION]!!,
                    false
                )
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        // Verify that all required contact permissions have been granted.
        return (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
                == PackageManager.PERMISSION_GRANTED
            )
    }

    private fun requestStoragePermission(
        onPermissionGranted: OnPermissionGranted,
        isInitialStart: Boolean
    ) {
        Utils.disableScreenRotation(this)
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it)
        }
        builder.setMessage(R.string.grant_storage_read_permission)
            .setTitle(R.string.grant_permission)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                finish()
            }.setCancelable(false)
        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            STORAGE_PERMISSION,
            builder,
            onPermissionGranted,
            isInitialStart
        )
    }

    /**
     * Requests permission, overrides {@param rationale}'s POSITIVE button dialog action.
     *
     * @param permission The permission to ask for
     * @param code [.STORAGE_PERMISSION] or [.INSTALL_APK_PERMISSION]
     * @param rationale MaterialLayout to provide an additional rationale to the user if the
     * permission was not granted and the user would benefit from additional context for the use
     * of the permission. For example, if the request has been denied previously.
     * @param isInitialStart is the permission being requested for the first time in the application
     * lifecycle
     */
    private fun requestPermission(
        permission: String,
        code: Int,
        rationale: AlertDialog.Builder,
        onPermissionGranted: OnPermissionGranted,
        isInitialStart: Boolean
    ) {
        permissionCallbacks[code] = onPermissionGranted
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            rationale
                .setPositiveButton(R.string.grant) { dialog, _ ->
                    run {
                        ActivityCompat.requestPermissions(
                            this@PermissionsActivity, arrayOf(permission), code
                        )
                        dialog.cancel()
                    }
                }
            rationale.show()
        } else if (isInitialStart) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), code)
        } else {
            /*Snackbar.make(
                findViewById(R.id.frameLayout),
                R.string.grantfailed,
                BaseTransientBottomBar.LENGTH_INDEFINITE
            )
                .setAction(
                    R.string.grant
                ) { v ->
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse(java.lang.String.format("package:%s", packageName))
                        )
                    )
                }
                .show()*/
            applicationContext.showToastInCenter(getString(R.string.grantfailed))
            finish()
        }
    }

    /**
     * Request all files access on android 11+
     *
     * @param onPermissionGranted permission granted callback
     */
    private fun requestAllFilesAccess(onPermissionGranted: OnPermissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            val builder: AlertDialog.Builder = this.let {
                AlertDialog.Builder(it)
            }
            builder.setMessage(R.string.grant_all_files_permission)
                .setTitle(R.string.grant_permission)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    run {
                        finish()
                    }
                }
                .setPositiveButton(R.string.grant) { dialog, _ ->
                    run {
                        Utils.disableScreenRotation(this)
                        permissionCallbacks[ALL_FILES_PERMISSION] = onPermissionGranted
                        try {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    .setData(Uri.parse("package:$packageName"))
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(
                                TAG, "Failed to initial activity to grant all files access",
                                e
                            )
                            applicationContext.showToastInCenter(getString(R.string.grantfailed))
                        }
                        dialog.cancel()
                    }
                }.setCancelable(false).show()
        }
    }

    private fun isGranted(grantResults: IntArray): Boolean {
        return grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    interface OnPermissionGranted {
        fun onPermissionGranted()
    }
}
