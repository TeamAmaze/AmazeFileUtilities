/*
 * Copyright (C) 2021-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.stephentuso.welcome.BasicPage
import com.stephentuso.welcome.FragmentWelcomePage
import com.stephentuso.welcome.WelcomeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WelcomeScreen : WelcomePermissionScreen() {
    private var log: Logger = LoggerFactory.getLogger(WelcomeScreen::class.java)

    companion object {
        fun welcomeKey(): String {
            return "WelcomeScreen"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*WelcomeSharedPreferencesHelper.storeWelcomeCompleted(this,
            WelcomeScreen.welcomeKey())*/
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    override fun configuration(): WelcomeConfiguration? {
        return WelcomeConfiguration.Builder(this)
            .defaultBackgroundColor(R.color.navy_blue)
            .page(
                BasicPage(
                    R.drawable.banner_app,
                    getString(R.string.welcome_media_title),
                    getString(R.string.welcome_media_summary)
                )
                    .background(R.color.purple)
            )
            .page(
                BasicPage(
                    R.drawable.ic_outline_analytics_32,
                    getString(R.string.title_analyse),
                    getString(R.string.welcome_analyse_summary)
                )
                    .background(R.color.orange_70)
            )
            .page(
                BasicPage(
                    R.drawable.ic_outline_connect_without_contact_32,
                    getString(R.string.title_transfer),
                    getString(R.string.welcome_transfer_summary)
                )
                    .background(R.color.peach_70)
            )
            .page(
                object : FragmentWelcomePage() {
                    override fun fragment(): Fragment {
                        return PermissionFragmentWelcome()
                    }
                }.background(R.color.navy_blue)
            )
            .canSkip(false)
            .swipeToDismiss(false)
            .useCustomDoneButton(true)
            .build()
    }
}
