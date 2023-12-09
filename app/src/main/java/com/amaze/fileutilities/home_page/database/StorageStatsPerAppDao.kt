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

package com.amaze.fileutilities.home_page.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import java.util.Date

@Dao
interface StorageStatsPerAppDao {
    @Transaction
    @Query("SELECT * FROM InstalledApps")
    fun findAll(): List<StorageStatsPerApp>

    @Transaction
    @Query("SELECT * FROM InstalledApps WHERE package_name=:packageName")
    fun findByPackageName(packageName: String): StorageStatsPerApp?

    @Transaction
    fun findByDay(dayStart: Date, dayEnd: Date): List<StorageStatsPerApp> {
        val all = findAll()
        return all.map { storageStatsPerApp ->
            storageStatsPerApp.copy(
                appStorageStats = storageStatsPerApp.appStorageStats.filter {
                    it.timestamp.after(dayStart) && it.timestamp.before(dayEnd)
                }
            )
        }
    }
}
