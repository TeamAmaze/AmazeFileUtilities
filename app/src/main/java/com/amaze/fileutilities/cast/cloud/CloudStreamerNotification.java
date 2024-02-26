package com.amaze.fileutilities.cast.cloud;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.amaze.fileutilities.R;
import com.amaze.fileutilities.home_page.MainActivity;
import com.amaze.fileutilities.utilis.NotificationConstants;
import com.amaze.fileutilities.utilis.Utils;

public class CloudStreamerNotification {

    public static final String NOTIFICATION_CHANNEL_ID = "httpServerChannel";
    public static final int NOTIFICATION_ID = 1006;

    public static Notification startNotification(Context context) {
        NotificationCompat.Builder builder = buildNotification(
                context,
                R.string.cast_notification_title,
                context.getString(R.string.cast_notification_summary)
        );
        return builder.build();
    }

    private static NotificationCompat.Builder buildNotification(
            Context context,
            @StringRes int contentTitleRes,
            String contentText) {

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, getPendingIntentFlag(0));

        long when = System.currentTimeMillis();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(contentTitleRes))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_baseline_cast_32)
                .setTicker(context.getString(R.string.cast_notification_title))
                .setWhen(when)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        int stopIcon = android.R.drawable.ic_menu_close_clear_cancel;
        CharSequence stopText = context.getString(R.string.stop);
        Intent stopIntent = new Intent(CloudStreamerService.TAG_BROADCAST_STREAMER_STOP);
        stopIntent.setPackage(context.getPackageName());
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context, 0, stopIntent, getPendingIntentFlag(PendingIntent.FLAG_ONE_SHOT));

        builder.addAction(stopIcon, stopText, stopPendingIntent);
        NotificationConstants.setMetadata(context, builder, NotificationConstants.TYPE_NORMAL);
        return builder;
    }

    /**
     * For compatibility purposes. Wraps the pending intent flag, return with FLAG_IMMUTABLE if device
     * SDK >= 32.
     *
     * @param pendingIntentFlag proposed PendingIntent flag
     * @return original PendingIntent flag if SDK < 32, otherwise adding FLAG_IMMUTABLE flag.
     */
    public static int getPendingIntentFlag(int pendingIntentFlag) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return pendingIntentFlag;
        } else {
            return pendingIntentFlag | PendingIntent.FLAG_IMMUTABLE;
        }
    }
}

