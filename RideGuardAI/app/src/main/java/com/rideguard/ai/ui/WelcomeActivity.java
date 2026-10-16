package com.rideguard.ai.ui;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rideguard.ai.R;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.util.NotificationHelper;
import com.rideguard.ai.util.UiUtils;

/** 13.1 Welcome and role selection. */
public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (session != null) {
            UiUtils.goHome(this, session);
            return;
        }
        setContentView(R.layout.activity_welcome);
        findViewById(R.id.demoBanner).setVisibility(repo.isDemo() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnRider).setOnClickListener(v ->
                startActivity(LoginActivity.intent(this, UserSession.ROLE_RIDER)));
        findViewById(R.id.btnCompany).setOnClickListener(v ->
                startActivity(LoginActivity.intent(this, UserSession.ROLE_COMPANY)));
        findViewById(R.id.btnHelp).setOnClickListener(v -> showHelp());
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    private void showHelp() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("How RideGuard AI works")
                .setMessage("• Riders: wear the vest and log in to see its status.\n\n"
                        + "• If the vest detects a possible accident, you get a countdown. "
                        + "Press CANCEL on the vest or in the app if you are OK.\n\n"
                        + "• If you do not cancel, your emergency contact receives an SMS with your "
                        + "location and your company is notified.\n\n"
                        + "• Hold the SOS button for 2 seconds to ask for help at any time.\n\n"
                        + "• Companies: log in to see emergencies, locations and accident videos.\n\n"
                        + "RideGuard helps report accidents faster. It does not replace calling "
                        + "official emergency services.")
                .setPositiveButton("OK", null)
                .show();
    }
}
