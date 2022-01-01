/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.utilis

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.amaze.fileutilities.R

class Utils {

    companion object {

        const val URL_PRIVACY_POLICY = "https://www.teamamaze.xyz/privacy-policy"
        const val AMAZE_FILE_MANAGER_MAIN = "com.amaze.filemanager.ui.activities.MainActivity"
        const val AMAZE_PACKAGE = "com.amaze.filemanager"

        /**
         * Open url in browser
         *
         * @param url given url
         */
        fun openURL(url: String?, context: Context) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }

        fun openActivity(context: Context, packageName: String, className: String) {
            try {
                val intent = Intent()
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.setClassName(packageName, className)
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                context.showToastOnBottom(context.resources.getString(R.string.install_amaze))
                popupPlay(packageName, context)
            }
        }

        fun popupPlay(packageName: String, context: Context) {
            val intent1 =
                Intent(Intent.ACTION_VIEW)
            try {
                intent1.data = Uri.parse(
                    String.format(
                        "market://details?id=%s",
                        packageName
                    )
                )
                context.startActivity(intent1)
            } catch (ifPlayStoreNotInstalled: ActivityNotFoundException) {
                intent1.data = Uri.parse(
                    String.format(
                        "https://play.google.com/store/apps/details?id=%s",
                        packageName
                    )
                )
                context.startActivity(intent1)
            }
        }
    }
}
