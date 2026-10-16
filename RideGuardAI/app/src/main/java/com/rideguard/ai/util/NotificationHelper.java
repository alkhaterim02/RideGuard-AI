package com.rideguard.ai.util;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.rideguard.ai.R;
import com.rideguard.ai.ui.IncidentDetailActivity;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "rideguard_emergency";

    private NotificationHelper() {
    }

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Emergency alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Accident and SOS alerts from RideGuard vests");
            ch.enableVibration(true);
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static boolean canNotify(Context c) {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissionIfNeeded(Activity a) {
        if (!canNotify(a)) {
            ActivityCompat.requestPermissions(a, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    public static void show(Context c, String title, String body, String incidentId) {
        if (!canNotify(c)) return;
        Intent i = new Intent(c, IncidentDetailActivity.class);
        i.putExtra(Extras.INCIDENT_ID, incidentId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int requestCode = incidentId == null ? 0 : incidentId.hashCode();
        PendingIntent pi = PendingIntent.getActivity(c, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify(requestCode, b.build());
        } catch (SecurityException ignored) {
            // Permission was revoked between the check and the call.
        }
    }
}
