package com.rideguard.ai.data;

import android.content.Context;

import com.rideguard.ai.BuildConfig;

public final class RepositoryProvider {
    private RepositoryProvider() {
    }

    /** DEMO_MODE is true automatically when app/google-services.json is missing. */
    public static DataRepository get(Context context) {
        if (BuildConfig.DEMO_MODE) return DemoRepository.get(context);
        return FirebaseRepository.get();
    }
}
