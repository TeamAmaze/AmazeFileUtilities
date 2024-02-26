package com.amaze.fileutilities.utilis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.amaze.fileutilities.R;

import java.lang.IllegalArgumentException;

public class NotificationConstants {

    public static final String CHANNEL_NORMAL_ID = "normalChannel";
    public static final int TYPE_NORMAL = 0;
    public static final int AUDIO_PLAYER_ID = 1001;

    /**
     * This creates a channel (API >= 26) or applies the correct metadata to a notification (API < 26)
     */
    public static void setMetadata(Context context, NotificationCompat.Builder notification, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switch (type) {
                case TYPE_NORMAL:
                    createNormalChannel(context);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized type:" + type);
            }
        } else {
            switch (type) {
                case TYPE_NORMAL:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        notification.setCategory(Notification.CATEGORY_SERVICE);
                        notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        notification.setPriority(Notification.PRIORITY_LOW);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized type:" + type);
            }
        }
    }

    /**
     * You CANNOT call this from android < O. THis channel is set so it doesn't bother the user, with
     * the lowest importance.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createNormalChannel(Context context) {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        if (mNotificationManager.getNotificationChannel(CHANNEL_NORMAL_ID) == null) {
            NotificationChannel mChannel = new NotificationChannel(
                    CHANNEL_NORMAL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            // Configure the notification channel.
            mChannel.setDescription(context.getString(R.string.app_name));
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }
}

