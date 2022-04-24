/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page

import androidx.fragment.app.Fragment
import com.amaze.fileutilities.R
import com.stephentuso.welcome.*

class WelcomeScreen : WelcomePermissionScreen() {

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
