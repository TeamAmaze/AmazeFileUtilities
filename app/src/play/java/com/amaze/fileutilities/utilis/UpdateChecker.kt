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

package com.amaze.fileutilities.utilis

import com.amaze.fileutilities.home_page.MainActivity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Random

object UpdateChecker {

    private var appUpdateManager: AppUpdateManager? = null

    fun checkForAppUpdates(context: MainActivity) {

        log.info("Checking for app update")
        appUpdateManager = AppUpdateManagerFactory.create(context)
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager?.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // This example applies an immediate update. To apply a flexible update
                // instead, pass in AppUpdateType.FLEXIBLE
            ) {
                /**
                 * check for app updates - flexible update dialog is showing till 7 days for any
                 * app update which has priority less than 4 after which he is shown
                 * immediate update dialog, for priority 4 and 5 user is asked to update
                 * immediately
                 */
                log.info("App update available")
                /*val immediateUpdate = (
                    appUpdateInfo.clientVersionStalenessDays()
                        ?: -1
                    ) >= DAYS_FOR_IMMEDIATE_UPDATE || appUpdateInfo.updatePriority() >= 4
                log.info("Immediate criteria $immediateUpdate")*/
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                    appUpdateInfo.updatePriority() >= 4
                ) {
                    log.info("Immediate update conditions met, triggering immediate update")
                    appUpdateManager?.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // The current activity making the update request.
                        context,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                            .setAllowAssetPackDeletion(true)
                            .build(),
                        // Include a request code to later monitor this update request.
                        MainActivity.UPDATE_REQUEST_CODE
                    )
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) &&
                    appUpdateInfo.updatePriority() >= 2
                ) {
                    log.info("flexible update conditions met, triggering flexible update")
                    appUpdateManager?.startUpdateFlowForResult(
                        // Pass the intent that is returned by 'getAppUpdateInfo()'.
                        appUpdateInfo,
                        // The current activity making the update request.
                        context,
                        // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                        // flexible updates.
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE)
                            .setAllowAssetPackDeletion(true)
                            .build(),
                        // Include a request code to later monitor this update request.
                        MainActivity.UPDATE_REQUEST_CODE
                    )
                }
            } else if (appUpdateInfo.updateAvailability()
                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                // Checks that the update is not stalled during 'onResume()'.
                // However, you should execute this check at all entry points into the app.
                log.info("resuming update that was already in progress")
                appUpdateManager?.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // The current activity making the update request.
                    context,
                    // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                    // flexible updates.
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                        .setAllowAssetPackDeletion(true)
                        .build(),
                    // Include a request code to later monitor this update request.
                    MainActivity.UPDATE_REQUEST_CODE
                )
            }

            appUpdateManager?.registerListener(updateListener)

            /*val cal1 = GregorianCalendar.getInstance()
            cal1.time = Date()
            cal1.add(Calendar.DAY_OF_YEAR, -2)
            val fetchTime = applicationContext.getAppCommonSharedPreferences()
                .getLong(PreferencesConstants.KEY_UPDATE_APP_LAST_SHOWN_DATE, cal1.timeInMillis)

            // check for update only once a day
            val cal = GregorianCalendar.getInstance()
            cal.time = Date(fetchTime)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            if (cal.time.before(Date()) || true) {


                    /*if (appUpdateInfo.updateAvailability()
                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                .setAllowAssetPackDeletion(true)
                                .build(),
                            UPDATE_REQUEST_CODE)
                    }*/

                applicationContext.getAppCommonSharedPreferences()
                    .edit().putLong(
                        PreferencesConstants.KEY_UPDATE_APP_LAST_SHOWN_DATE,
                        Date().time
                    ).apply()
            }*/
        }
    }

    fun unregisterListener() {
        appUpdateManager?.unregisterListener(updateListener)
    }

    fun shouldRateApp(context: MainActivity) {
        val alreadyRated = context.getAppCommonSharedPreferences()
            .getBoolean(PreferencesConstants.KEY_RATE_APP_AUTOMATED, false)
        if (!alreadyRated) {
            val random = Random()
            val chance = random.nextInt(10) + 1
            if (chance == 5) {
                // check if user using app for over 7 days
                val calWeek = GregorianCalendar.getInstance()
                val calWeekDefault = GregorianCalendar.getInstance()
                calWeekDefault.time = Date()
                calWeekDefault.add(Calendar.DAY_OF_YEAR, 8)
                val installDate = context.getAppCommonSharedPreferences()
                    .getLong(PreferencesConstants.KEY_INSTALL_DATE, calWeekDefault.time.time)
                calWeek.time = Date(installDate)
                calWeek.add(Calendar.DAY_OF_YEAR, 7)
                if (calWeek.time.before(Date())) {
                    val manager = ReviewManagerFactory.create(context)
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // We got the ReviewInfo object
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(context, reviewInfo)
                            flow.addOnCompleteListener { _ ->
                                // The flow has finished. The API does not indicate whether the user
                                // reviewed or not, or even whether the review dialog was shown. Thus, no
                                // matter the result, we continue our app flow.
                                // add install time in preferences
                                context.getAppCommonSharedPreferences()
                                    .edit()
                                    .putBoolean(
                                        PreferencesConstants.KEY_RATE_APP_AUTOMATED,
                                        true
                                    ).apply()
                            }
                        } else {
                            // There was some problem, log or handle the error code.
                            log.warn("failed to request review", task.exception)
                        }
                    }
                }
            }
        }
    }

    private val updateListener: InstallStateUpdatedListener =
        InstallStateUpdatedListener { installState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                log.info("update has been downloaded")
                appUpdateManager!!.completeUpdate()
            }
        }
}
