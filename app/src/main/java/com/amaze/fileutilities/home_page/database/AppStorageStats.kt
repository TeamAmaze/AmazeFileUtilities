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

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    indices = [
        Index(value = ["timestamp", "package_id"], unique = false),
        Index(value = ["package_id"], unique = false) // separate index because it is foreign key
    ],
    foreignKeys = [
        ForeignKey(
            entity = InstalledApps::class,
            parentColumns = ["_id"],
            childColumns = ["package_id"],
            onDelete = CASCADE,
            onUpdate = CASCADE
        )
    ]
)
@Keep
data class AppStorageStats(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "package_id") val packageId: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Date,
    @ColumnInfo(name = "package_size") val packageSize: Long
) {
    @Ignore
    constructor(
        packageId: Int,
        timestamp: Date,
        packageSize: Long
    ) : this(0, packageId, timestamp, packageSize)
}
