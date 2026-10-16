package com.rideguard.ai.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.rideguard.ai.model.UserSession;

/** Remembers who is logged in, so the app opens straight to the right home page. */
public final class SessionManager {
    private static final String PREFS = "rideguard_session";

    private SessionManager() {
    }

    public static void save(Context c, UserSession s) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("uid", s.uid)
                .putString("role", s.role)
                .putString("riderId", s.riderId)
                .putString("companyId", s.companyId)
                .putString("displayName", s.displayName)
                .apply();
    }

    public static UserSession load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uid = p.getString("uid", null);
        if (uid == null) return null;
        return new UserSession(uid, p.getString("role", null), p.getString("riderId", null),
                p.getString("companyId", null), p.getString("displayName", null));
    }

    public static void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
