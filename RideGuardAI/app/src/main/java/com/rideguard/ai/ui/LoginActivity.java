package com.rideguard.ai.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.rideguard.ai.R;
import com.rideguard.ai.data.SessionManager;
import com.rideguard.ai.model.UserSession;
import com.rideguard.ai.util.Extras;
import com.rideguard.ai.util.FormUtils;
import com.rideguard.ai.util.UiUtils;

/** 13.2 Rider login and 14.1 Company login (same screen, different role). */
public class LoginActivity extends BaseActivity {
    private String role;
    private EditText etId;
    private EditText etPassword;
    private TextView tvError;
    private View progress;
    private MaterialButton btnLogin;

    public static Intent intent(Context c, String role) {
        return new Intent(c, LoginActivity.class).putExtra(Extras.ROLE, role);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        showBack();

        role = getIntent().getStringExtra(Extras.ROLE);
        if (role == null) role = UserSession.ROLE_RIDER;
        boolean rider = UserSession.ROLE_RIDER.equals(role);

        String title = rider ? "Rider Login" : "Company Login";
        setTitle(title);
        ((TextView) findViewById(R.id.tvTitle)).setText(title);
        TextInputLayout tilId = findViewById(R.id.tilId);
        tilId.setHint(rider ? "Email, phone number or rider ID" : "Company email");

        etId = findViewById(R.id.etId);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        btnLogin = findViewById(R.id.btnLogin);

        TextView hint = findViewById(R.id.tvDemoHint);
        if (repo.isDemo()) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(rider ? "Demo account: rider@demo.com   Password: demo123"
                    : "Demo account: company@demo.com   Password: demo123");
        }

        View btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setVisibility(rider ? View.VISIBLE : View.GONE);
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnLogin.setOnClickListener(v -> doLogin());
        findViewById(R.id.btnForgot).setOnClickListener(v -> forgotPassword());
    }

    private void doLogin() {
        String id = FormUtils.text(etId);
        String pw = FormUtils.raw(etPassword);
        if (id.isEmpty() || pw.isEmpty()) {
            showError("Please enter your login details and password.");
            return;
        }
        tvError.setVisibility(View.GONE);
        setLoading(true);
        repo.login(id, pw, role, (s, err) -> {
            if (isFinishing()) return;
            setLoading(false);
            if (err != null || s == null) {
                showError(err != null ? err : "Login failed. Please try again.");
                return;
            }
            SessionManager.save(this, s);
            UiUtils.goHome(this, s);
        });
    }

    private void forgotPassword() {
        String id = FormUtils.text(etId);
        if (id.isEmpty()) {
            showError("Type your email above first, then press \"Forgot password?\" again.");
            return;
        }
        repo.sendPasswordReset(id, (ok, err) -> {
            if (err != null) showError(err);
            else toast("Password reset instructions were sent to " + id);
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
