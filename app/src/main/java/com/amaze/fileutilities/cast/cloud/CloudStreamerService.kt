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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.amaze.fileutilities.R
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo
import com.amaze.fileutilities.utilis.ObtainableServiceBinder
import java.io.File
import java.io.FileInputStream

class CloudStreamerService : Service() {

    private var cloudStreamer: CloudStreamer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var notificationManager: NotificationManagerCompat? = null
    private val mBinder: IBinder = ObtainableServiceBinder(this)

    companion object {
        const val TAG_BROADCAST_STREAMER_STOP = "streamer_stop_broadcast"

        fun runService(context: Context) {
            val intent = Intent(context, CloudStreamerService::class.java)
//            intent.putExtra(ARG_MEDIA_PATH, mediaInfo.path)
//            intent.putExtra(ARG_MEDIA_TITLE, mediaInfo.title)
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (cloudStreamer == null) {
            cloudStreamer = CloudStreamer.getInstance()
        }
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(stopReceiver, IntentFilter(TAG_BROADCAST_STREAMER_STOP))
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        wakeLock?.setReferenceCounted(false)
        initNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        cloudStreamer?.stop()
        wakeLock!!.release()
        cloudStreamer = null
        notificationManager!!.cancel(CloudStreamerNotification.NOTIFICATION_ID)
        unregisterReceiver(stopReceiver)
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    fun setStreamSrc(mediaFileInfo: MediaFileInfo) {
        cloudStreamer?.setStreamSrc(
            FileInputStream(mediaFileInfo.path),
            mediaFileInfo.title,
            File(mediaFileInfo.path).length()
        )
    }

    private val stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // cancel operation
            stopSelf()
            notificationManager!!.cancel(CloudStreamerNotification.NOTIFICATION_ID)
            stopForeground(true)
        }
    }

    private fun initNotification() {
        notificationManager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val notification = CloudStreamerNotification.startNotification(this)
        startForeground(CloudStreamerNotification.NOTIFICATION_ID, notification)
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        var notificationChannel = notificationManager!!.getNotificationChannel(
            CloudStreamerNotification.NOTIFICATION_CHANNEL_ID
        )
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                CloudStreamerNotification.NOTIFICATION_CHANNEL_ID,
                getString(R.string.chromecast),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description =
                getString(R.string.cast_notification_summary)
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager!!.createNotificationChannel(notificationChannel)
        }
    }
}
