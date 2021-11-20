package com.amaze.fileutilities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

open class PermissionActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private val permissionCode = 0
    var onPermissionGrantedCallback: OnPermissionGrantedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        triggerPermissionCheck()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            if (isGranted(grantResults)) {
                onPermissionGrantedCallback?.onPermissionGranted()
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                onPermissionGrantedCallback?.let {
                    requestStoragePermission(
                        it, false
                    )
                }
            }
        }
    }

    private fun triggerPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkStoragePermission()) {
            buildExplicitPermissionAlertDialog ({
                startExplicitPermissionActivity()
            }, {
                // do nothing
            }).show()
        }
    }

    fun checkStoragePermission(): Boolean {
        // Verify that all required contact permissions have been granted.
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    fun requestStoragePermission(
        onPermissionGrantedCallback: OnPermissionGrantedCallback,
        isInitialStart: Boolean
    ) {
        requestPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            permissionCode,
            onPermissionGrantedCallback,
            isInitialStart
        )
    }

    fun startExplicitPermissionActivity() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(java.lang.String.format("package:%s", packageName))
            )
        )
    }

    fun buildExplicitPermissionAlertDialog(grantCallback: () -> Unit, cancelCallback: () -> Unit): AlertDialog.Builder {
        val builder: AlertDialog.Builder = this.let {
            AlertDialog.Builder(it)
        }
        builder.setMessage(R.string.grant_storage_read_permission)
            .setTitle(R.string.grant_permission)
            .setNegativeButton(R.string.cancel) { dialog, _ -> run {
                cancelCallback.invoke()
                dialog.cancel()
            } }
            .setPositiveButton(R.string.grant) { dialog, _ ->
                run {
                    grantCallback.invoke()
                    dialog.cancel()
                }
            }.setCancelable(false)
        return builder
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
        onPermissionGrantedCallback: OnPermissionGrantedCallback,
        isInitialStart: Boolean
    ) {
        this.onPermissionGrantedCallback = onPermissionGrantedCallback
        when {
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                buildExplicitPermissionAlertDialog ({
                    ActivityCompat.requestPermissions(
                        this@PermissionActivity, arrayOf(permission), code
                    )
                }, {
                    onPermissionGrantedCallback.onPermissionNotGranted()
                }).show()
            }
            isInitialStart -> {
                ActivityCompat.requestPermissions(this, arrayOf(permission), code)
            }
            else -> {
                onPermissionGrantedCallback.onPermissionNotGranted()
            }
        }
    }

    private fun isGranted(grantResults: IntArray): Boolean {
        return grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    interface OnPermissionGrantedCallback {
        fun onPermissionGranted()
        fun onPermissionNotGranted()
    }
}