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
import java.util.Date

@Dao
interface StorageStatToAppNameDao {
    @Query(
        "SELECT I.package_name AS package_name, " +
            "A.timestamp AS timestamp, " +
            "A.package_size AS package_size " +
            "FROM AppStorageStats AS A " +
            "INNER JOIN InstalledApps AS I " +
            "ON I._id = A.package_id"
    )
    fun findAll(): List<StorageStatToAppName>

    @Query(
        "SELECT I.package_name AS package_name, " +
            "A.timestamp AS timestamp, " +
            "A.package_size AS package_size " +
            "FROM AppStorageStats AS A " +
            "INNER JOIN InstalledApps AS I " +
            "ON I._id = A.package_id " +
            "WHERE I.package_name=:packageName"
    )
    fun findByPackageName(packageName: String): List<StorageStatToAppName>

    @Query(
        "SELECT I.package_name AS package_name, " +
            "A.timestamp AS timestamp, " +
            "A.package_size AS package_size " +
            "FROM AppStorageStats AS A " +
            "INNER JOIN InstalledApps AS I " +
            "ON I._id = A.package_id " +
            "WHERE I.package_name=:packageName " +
            "AND A.timestamp >= :periodStart " + // Ensure that timestamp is after `periodStart`
            "AND A.timestamp < :periodEnd " + // Ensure that timestamp is before `periodEnd`
            "ORDER BY A.timestamp ASC LIMIT 1" // Get the oldest entry based on timestamp
    )
    fun findOldestWithinPeriod(
        packageName: String,
        periodStart: Date,
        periodEnd: Date
    ): StorageStatToAppName?
}
