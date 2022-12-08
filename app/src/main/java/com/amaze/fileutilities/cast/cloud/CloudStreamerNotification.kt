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
