/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.amaze.fileutilities.home_page.ui.files.TrialValidationApi
import java.util.*

@Entity(indices = [Index(value = ["device_id"], unique = true)])
data class Trial(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val uid: Int,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "trial_status") val trialStatus: String,
    @ColumnInfo(name = "trial_days_left") val trialDaysLeft: Int,
    @ColumnInfo(name = "fetch_time") val fetchTime: Date,
    @ColumnInfo(name = "subscription_status") var subscriptionStatus: Int,
    @ColumnInfo(name = "purchase_token") var purchaseToken: String? = null
) {
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
