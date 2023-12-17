/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amaze.fileutilities.home_page.database.AppDatabase
import java.time.ZonedDateTime
import java.util.Date

/**
 * A [CoroutineWorker] to insert the size of each app into the database and
 * delete entries that are older than [PreferencesConstants.MAX_LARGE_SIZE_DIFF_APPS_DAYS] days.
 */
class QueryAppSizeWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val appStorageStatsDao = AppDatabase.getInstance(applicationContext).appStorageStatsDao()
        val packageManager = applicationContext.packageManager
        // Get all currently installed apps
        val allApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }
        // Find the current size of each app and store them in the database
        for (appInfo in allApps) {
            val currentSize = Utils.findApplicationInfoSize(applicationContext, appInfo)
            val timestamp = Date.from(ZonedDateTime.now().toInstant())
            appStorageStatsDao.insert(appInfo.packageName, timestamp, currentSize)
        }

        // Delete all AppStorageStats entries that are older than MAX_LARGE_SIZE_DIFF_APPS_DAYS
        val minDate = Date.from(
            ZonedDateTime
                .now()
                .minusDays(PreferencesConstants.MAX_LARGE_SIZE_DIFF_APPS_DAYS.toLong())
                .toInstant()
        )
        appStorageStatsDao.deleteOlderThan(minDate)

        return Result.success()
    }

    companion object {
        const val NAME: String = "query_app_size_worker"
    }
}
