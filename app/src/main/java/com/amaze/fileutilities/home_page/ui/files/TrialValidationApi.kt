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

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TrialValidationApi {
    companion object {
        const val CLOUD_FUNCTION_BASE =
            "https://us-central1-useful-cathode-91310.cloudfunctions.net"
        const val AUTH_TOKEN = "anG*XojCjZQ44x"
    }

    @POST("/amaze-utils-trial-validator")
    fun postValidation(
        @Body trialRequest: TrialRequest
    ): Call<TrialResponse>?

    data class TrialRequest(
        val token: String,
        val deviceId: String,
        var subscriptionStatus: Int = 1001
    )

    data class TrialResponse(
        val isLastDay: Boolean,
        val isNewSignup: Boolean,
        val trialStatus: Int
    ) {

        companion object {
            const val TRIAL_ACTIVE = "trial_active"
            const val TRIAL_EXPIRED = "trial_expired"
            const val TRIAL_INACTIVE = "trial_inactive"
            const val TRIAL_EXCLUSIVE = "trial_exclusive"
        }

        private val trialStatusMap = mapOf(
            Pair(12341343, TRIAL_ACTIVE),
            Pair(24523424, TRIAL_EXPIRED),
            Pair(33452345, TRIAL_INACTIVE),
            Pair(45345234, TRIAL_EXCLUSIVE)
        )

        fun getTrialStatus(): String {
            return trialStatusMap[trialStatus] ?: TRIAL_ACTIVE
        }
    }
}
