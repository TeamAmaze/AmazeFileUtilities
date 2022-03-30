/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amaze.fileutilities.R
import com.amaze.fileutilities.utilis.MimeTypes

fun getShareIntents(sharingUris: List<Uri>, context: Context): ShareAdapter? {

    val targetShareIntents = ArrayList<Intent>()
    val labels = ArrayList<String>()
    val drawables = ArrayList<Drawable>()

    var b = true
    var mime: String? = null
    if (sharingUris.size > 1) {
        mime = MimeTypes.getMimeType(sharingUris[0].path, false)
        for (f in sharingUris) {
            if (mime != MimeTypes.getMimeType(f.path, false)) {
                b = false
            }
        }
    }

    if (!b || mime == null) {
        mime = MimeTypes.ALL_MIME_TYPES
    }
    if (sharingUris.isNotEmpty()) {
        var bluetooth_present = false
        val shareIntent = Intent().setAction(getShareIntentAction(sharingUris)).setType(mime)
        val packageManager: PackageManager = context.packageManager
        val resInfos = packageManager.queryIntentActivities(shareIntent, 0)
        if (!resInfos.isEmpty()) {
            for (resInfo in resInfos) {
                val packageName = resInfo.activityInfo.packageName
                drawables.add(resInfo.loadIcon(packageManager))
                labels.add(resInfo.loadLabel(packageManager).toString())
                if (packageName.contains("android.bluetooth")) {
                    bluetooth_present = true
                }
                val intent = Intent()
                intent.component = ComponentName(packageName, resInfo.activityInfo.name)
                intent.action = getShareIntentAction(sharingUris)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.type = mime
                if (sharingUris.size == 1) {
                    intent.putExtra(Intent.EXTRA_STREAM, sharingUris[0])
                } else {
                    intent.putParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList(sharingUris)
                    )
                }
                intent.setPackage(packageName)
                targetShareIntents.add(intent)
            }
        }
        if (!bluetooth_present && appInstalledOrNot(
                "com.android.bluetooth",
                packageManager
            )
        ) {
            val intent = Intent()
            intent.component = ComponentName(
                "com.android.bluetooth",
                "com.android.bluetooth.opp.BluetoothOppLauncherActivity"
            )
            intent.action = getShareIntentAction(sharingUris)
            intent.type = mime
            if (sharingUris.size == 1) {
                intent.putExtra(Intent.EXTRA_STREAM, sharingUris.get(0))
            } else {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(sharingUris))
            }
            intent.setPackage("com.android.bluetooth")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            targetShareIntents.add(intent)
            labels.add(context.getString(R.string.bluetooth))
            drawables.add(
                context.getResources().getDrawable(R.drawable.ic_round_bluetooth_32)
            )
        }

        return ShareAdapter(context, targetShareIntents, labels, drawables)
    }
    return null
}

fun showShareDialog(context: Context, inflater: LayoutInflater, adapter: ShareAdapter) {
    val dialogBuilder = AlertDialog.Builder(context, R.style.Custom_Dialog_Dark)
        .setTitle(R.string.share)
        .setNegativeButton(R.string.close) { dialog, _ ->
            dialog.dismiss()
        }
    val dialogView: View = inflater.inflate(R.layout.subtitles_search_results_view, null)
    dialogBuilder.setView(dialogView)
    val recyclerView = dialogView.findViewById<RecyclerView>(R.id.search_results_list)
    val emptyTextView = dialogView.findViewById<TextView>(R.id.empty_text_view)
    if (adapter.itemCount == 0) {
        emptyTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyTextView.text = context.resources.getString(R.string.no_apps_found_to_share)
    } else {
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    val alertDialog = dialogBuilder.create()
    alertDialog.show()
}

fun showSetAsDialog(uri: Uri, context: Context) {
    val intent = Intent(Intent.ACTION_ATTACH_DATA)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.setDataAndType(uri, "image/*")
    intent.putExtra("mimeType", "image/*")
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.startActivity(
        Intent.createChooser(
            intent,
            context.resources.getString(R.string.set_as)
        )
    )
}

fun showEditImageDialog(uri: Uri, context: Context) {
    val editIntent = Intent(Intent.ACTION_EDIT)
    editIntent.setDataAndType(uri, "image/*")
    editIntent.putExtra(Intent.EXTRA_STREAM, uri)
    editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.startActivity(
        Intent.createChooser(
            editIntent,
            context.resources.getString(R.string.edit)
        )
    )
}

private fun getShareIntentAction(sharingUris: List<Uri>): String {
    return if (sharingUris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
}

private fun appInstalledOrNot(uri: String, pm: PackageManager): Boolean {
    val app_installed: Boolean
    app_installed = try {
        pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
    return app_installed
}
