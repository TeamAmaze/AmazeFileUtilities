/*
 * Copyright (C) 2021-2022 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.cast.cloud

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.MainActivity
import com.amaze.fileutilities.utilis.NotificationConstants

class CloudStreamerNotification {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "httpServerChannel"
        const val NOTIFICATION_ID = 1005

        fun startNotification(context: Context, noStopButton: Boolean): Notification {
            val builder = buildNotification(
                context,
                R.string.cast_notification_title,
                context.getString(R.string.cast_notification_summary)
            )
//            context.startForeground(NotificationConstants.FTP_ID, notification)
            return builder.build()
        }

        fun removeNotification(context: Context) {
            val ns = Context.NOTIFICATION_SERVICE
            val nm = context.getSystemService(ns) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
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
                notificationIntent, 0
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
            NotificationConstants.setMetadata(context, builder, 1)
            return builder
        }
    }
}
