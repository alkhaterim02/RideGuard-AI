package com.rideguard.ai.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rideguard.ai.R;
import com.rideguard.ai.data.DataRepository;
import com.rideguard.ai.data.ListenerHandle;
import com.rideguard.ai.data.RepositoryProvider;
import com.rideguard.ai.data.SessionManager;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Common code: data access, the logged-in session, live listeners and logout. */
public abstract class BaseActivity extends AppCompatActivity {
    protected DataRepository repo;
    protected UserSession session;
    private final List<ListenerHandle> handles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = RepositoryProvider.get(this);
        session = SessionManager.load(this);
    }

    /** Listeners registered here are removed automatically when the screen closes. */
    protected void track(ListenerHandle h) {
        if (h != null) handles.add(h);
    }

    protected void showBack() {
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    protected boolean requireSession() {
        if (session != null) return true;
        Intent i = new Intent(this, WelcomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
        return false;
    }

    protected boolean showLogoutMenu() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (showLogoutMenu()) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            UiUtils.confirm(this, "Log out?", "You will need your password to log in again.",
                    "Log out", this::logout);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void logout() {
        repo.logout();
        SessionManager.clear(this);
        Intent i = new Intent(this, WelcomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    protected void toast(String message) {
        if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        for (ListenerHandle h : handles) h.remove();
        handles.clear();
        super.onDestroy();
    }
}
