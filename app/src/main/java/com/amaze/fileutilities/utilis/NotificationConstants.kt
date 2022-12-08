/*
 * Copyright (C) 2021-2021 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amaze.fileutilities.R
import java.lang.IllegalArgumentException

class NotificationConstants {
    companion object {

        const val CHANNEL_NORMAL_ID = "normalChannel"
        const val TYPE_NORMAL = 0
        const val AUDIO_PLAYER_ID = 1001

        /**
         * This creates a channel (API >= 26) or applies the correct metadata to a notification (API < 26)
         */
        fun setMetadata(
            context: Context?,
            notification: NotificationCompat.Builder,
            type: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    TYPE_NORMAL -> createNormalChannel(
                        context!!
                    )
                    else -> throw IllegalArgumentException("Unrecognized type:$type")
                }
            } else {
                when (type) {
                    TYPE_NORMAL -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            notification.setCategory(Notification.CATEGORY_SERVICE)
                            notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            notification.priority = Notification.PRIORITY_LOW
                        }
                    }
                    else -> throw IllegalArgumentException("Unrecognized type:$type")
                }
            }
        }

        /**
         * You CANNOT call this from android < O. THis channel is set so it doesn't bother the user, with
         * the lowest importance.
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createNormalChannel(context: Context) {
            val mNotificationManager = NotificationManagerCompat.from(context)
            if (mNotificationManager.getNotificationChannel(CHANNEL_NORMAL_ID) == null) {
                val mChannel = NotificationChannel(
                    CHANNEL_NORMAL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                // Configure the notification channel.
                mChannel.description = context.getString(R.string.app_name)
                mNotificationManager.createNotificationChannel(mChannel)
            }
        }
    }
}
