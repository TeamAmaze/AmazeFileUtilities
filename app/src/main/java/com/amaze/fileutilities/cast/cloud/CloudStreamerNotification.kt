/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
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

package com.amaze.fileutilities.cast.cloud

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.NotificationConstants
import com.amaze.fileutilities.utilis.Utils.Companion.getPendingIntentFlag

class CloudStreamerNotification {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "httpServerChannel"
        const val NOTIFICATION_ID = 1006

        fun startNotification(context: Context): Notification {
            val builder = buildNotification(
                context,
                R.string.cast_notification_title,
                context.getString(R.string.cast_notification_summary)
            )
            return builder.build()
        }

        private fun buildNotification(
            context: Context,
            @StringRes contentTitleRes: Int,
            contentText: String
        ): NotificationCompat.Builder {
            val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentIntent = PendingIntent.getActivity(
                context, 0,
                notificationIntent, getPendingIntentFlag(FLAG_IMMUTABLE)
            )
            val `when` = System.currentTimeMillis()
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(context.getString(contentTitleRes))
                    .setContentText(contentText)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_baseline_cast_32)
                    .setTicker(context.getString(R.string.cast_notification_title))
                    .setWhen(`when`)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
            val stopIcon = android.R.drawable.ic_menu_close_clear_cancel
            val stopText: CharSequence = context.getString(R.string.stop)
            val stopIntent: Intent =
                Intent(CloudStreamerService.TAG_BROADCAST_STREAMER_STOP)
                    .setPackage(context.packageName)
            val stopPendingIntent =
                PendingIntent.getBroadcast(
                    context, 0, stopIntent,
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S)
                        PendingIntent.FLAG_ONE_SHOT and PendingIntent.FLAG_IMMUTABLE
                    else PendingIntent.FLAG_ONE_SHOT
                )

            builder.addAction(stopIcon, stopText, stopPendingIntent)
            NotificationConstants.setMetadata(context, builder, NotificationConstants.TYPE_NORMAL)
            return builder
        }
    }
}
