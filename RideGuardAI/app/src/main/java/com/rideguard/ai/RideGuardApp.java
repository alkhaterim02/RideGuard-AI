package com.rideguard.ai;

import android.app.Application;

import com.rideguard.ai.util.NotificationHelper;

public class RideGuardApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }
}
