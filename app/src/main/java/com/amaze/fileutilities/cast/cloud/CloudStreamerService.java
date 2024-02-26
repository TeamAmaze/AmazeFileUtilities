package com.amaze.fileutilities.cast.cloud;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

import com.amaze.fileutilities.R;
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo;
import com.amaze.fileutilities.utilis.ObtainableServiceBinder;

import java.io.File;
import java.io.FileInputStream;

public class CloudStreamerService extends Service {

    private CloudStreamer cloudStreamer;
    private PowerManager.WakeLock wakeLock;
    private NotificationManagerCompat notificationManager;
    private final IBinder mBinder = new ObtainableServiceBinder(this);
    public static final String TAG_BROADCAST_STREAMER_STOP = "streamer_stop_broadcast";

    public static void runService(Context context) {
        Intent intent = new Intent(context, CloudStreamerService.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (cloudStreamer == null) {
            cloudStreamer = CloudStreamer.getInstance();
        }
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(stopReceiver, new IntentFilter(TAG_BROADCAST_STREAMER_STOP));
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);
        initNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cloudStreamer != null) {
            cloudStreamer.stop();
            cloudStreamer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (notificationManager != null) {
            notificationManager.cancel(CloudStreamerNotification.NOTIFICATION_ID);
        }
        unregisterReceiver(stopReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setStreamSrc(MediaFileInfo mediaFileInfo) {
        if (cloudStreamer != null) {
            try {
                cloudStreamer.setStreamSrc(
                        new FileInputStream(mediaFileInfo.getPath()),
                        mediaFileInfo.getTitle(),
                        new File(mediaFileInfo.getPath()).length()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
            if (notificationManager != null) {
                notificationManager.cancel(CloudStreamerNotification.NOTIFICATION_ID);
            }
            stopForeground(true);
        }
    };

    private void initNotification() {
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        android.app.Notification notification = CloudStreamerNotification.startNotification(this);
        startForeground(CloudStreamerNotification.NOTIFICATION_ID, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(
                CloudStreamerNotification.NOTIFICATION_CHANNEL_ID
        );
        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(
                    CloudStreamerNotification.NOTIFICATION_CHANNEL_ID,
                    getString(R.string.chromecast),
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationChannel.setDescription(getString(R.string.cast_notification_summary));
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
