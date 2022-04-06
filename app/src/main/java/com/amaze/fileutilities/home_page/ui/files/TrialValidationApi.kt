/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.home_page.ui.files

import com.amaze.fileutilities.home_page.database.Trial
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TrialValidationApi {
    companion object {
        const val CLOUD_FUNCTION_BASE =
            "https://us-central1-useful-cathode-91310.cloudfunctions.net"
        const val AUTH_TOKEN = "anG*XojCjZQ44x"
    }

    @POST("/amaze-utils-trial-validator-1")
    fun postValidation(
        @Body trialRequest: TrialRequest
    ): Call<TrialResponse>?

    data class TrialRequest(
        val token: String,
        val deviceId: String,
        var subscriptionStatus: Int = 1001
    )

    data class TrialResponse(
        val isLastDay: Boolean = false,
        val isNewSignup: Boolean = false,
        val trialStatus: Int,
        val trialDaysLeft: Int = 0,
        val subscriptionStatus: Int = Trial.SUBSCRIPTION_STATUS_DEFAULT
    ) {

        companion object {
            const val TRIAL_ACTIVE = "trial_active"
            const val TRIAL_EXPIRED = "trial_expired"
            const val TRIAL_INACTIVE = "trial_inactive"
            const val TRIAL_EXCLUSIVE = "trial_exclusive"
            const val SUBSCRIPTION = "subscribed"
            const val CODE_TRIAL_ACTIVE = 12341343
            const val CODE_TRIAL_EXPIRED = 24523424
            const val CODE_TRIAL_INACTIVE = 33452345
            const val CODE_TRIAL_EXCLUSIVE = 45345234

            val trialStatusMap = mapOf(
                Pair(TRIAL_ACTIVE, "trial"),
                Pair(TRIAL_EXPIRED, "trial-expired"),
                Pair(TRIAL_INACTIVE, "inactive"),
                Pair(TRIAL_EXCLUSIVE, "exclusive")
            )

            private val trialStatusCodeMap = mapOf(
                Pair(CODE_TRIAL_ACTIVE, TRIAL_ACTIVE),
                Pair(CODE_TRIAL_EXPIRED, TRIAL_EXPIRED),
                Pair(CODE_TRIAL_INACTIVE, TRIAL_INACTIVE),
                Pair(CODE_TRIAL_EXCLUSIVE, TRIAL_EXCLUSIVE)
            )
        }

        fun getTrialStatus(): String {
            val statusCode = getTrialStatusCode()
            return trialStatusMap[statusCode] ?: SUBSCRIPTION
        }

        fun getTrialStatusCode(): String {
            return trialStatusCodeMap[trialStatus] ?: TRIAL_ACTIVE
        }
    }
}
