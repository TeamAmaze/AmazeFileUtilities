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
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import java.util.Date

@Keep
@Entity(indices = [Index(value = ["device_id"], unique = true)])
data class Trial(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "trial_status") var trialStatus: String,
    @ColumnInfo(name = "trial_days_left") val trialDaysLeft: Int,
    @ColumnInfo(name = "fetch_time") val fetchTime: Date,
    @ColumnInfo(name = "subscription_status") var subscriptionStatus: Int,
    @ColumnInfo(name = "purchase_token") var purchaseToken: String? = null
) {
    @Ignore
    constructor(
        deviceId: String,
        trialStatus: String,
        trialDaysLeft: Int,
        fetchTime: Date,
        subscriptionStatus: Int
    ) :
        this(0, deviceId, trialStatus, trialDaysLeft, fetchTime, subscriptionStatus)

    companion object {
        const val SUBSCRIPTION_STATUS_DEFAULT = 1001
        const val TRIAL_DEFAULT_DAYS = 7
    }

    fun getTrialStatusName(): String {
        return TrialValidationApi.TrialResponse.trialStatusMap[trialStatus]
            ?: TrialValidationApi.TrialResponse.SUBSCRIPTION
    }
}
