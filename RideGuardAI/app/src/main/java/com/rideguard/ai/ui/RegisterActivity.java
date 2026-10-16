package com.rideguard.ai.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.rideguard.ai.R;
import com.rideguard.ai.data.SessionManager;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.FormUtils;
import com.rideguard.ai.util.UiUtils;

/** 13.3 Rider registration. */
public class RegisterActivity extends BaseActivity {
    private EditText etName, etPhone, etEmail, etCompany, etRiderCode, etPassword;
    private EditText etContactName, etContactRel, etContactPhone;
    private EditText etBlood, etAllergies, etConditions, etMedication;
    private CheckBox cbConsent;
    private TextView tvError;
    private View progress;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        setTitle("Create rider account");
        showBack();

        ((TextView) findViewById(R.id.tvIntro)).setText("Fields marked * are required.");
        LinearLayout form = findViewById(R.id.formFields);

        FormUtils.addSection(form, "Your details");
        etName = FormUtils.addField(form, "Full name *", FormUtils.NAME);
        etPhone = FormUtils.addField(form, "Phone number *", FormUtils.PHONE);
        etEmail = FormUtils.addField(form, "Email *", FormUtils.EMAIL);
        etCompany = FormUtils.addField(form, "Company code (from your employer) *", FormUtils.TEXT);
        etRiderCode = FormUtils.addField(form, "Rider ID number", FormUtils.TEXT);
        etPassword = FormUtils.addField(form, "Password (at least 6 characters) *", FormUtils.PASSWORD);

        FormUtils.addSection(form, "Emergency contact");
        etContactName = FormUtils.addField(form, "Contact name *", FormUtils.NAME);
        etContactRel = FormUtils.addField(form, "Relationship (e.g. brother)", FormUtils.TEXT);
        etContactPhone = FormUtils.addField(form, "Contact phone number *", FormUtils.PHONE);

        FormUtils.addSection(form, "Health details (optional, helps responders)");
        etBlood = FormUtils.addField(form, "Blood type (e.g. O+)", FormUtils.TEXT);
        etAllergies = FormUtils.addField(form, "Allergies", FormUtils.TEXT);
        etConditions = FormUtils.addField(form, "Chronic medical conditions", FormUtils.TEXT);
        etMedication = FormUtils.addField(form, "Important medication", FormUtils.TEXT);

        if (repo.isDemo()) FormUtils.set(etCompany, "DEMO");

        cbConsent = findViewById(R.id.cbConsent);
        cbConsent.setVisibility(View.VISIBLE);
        cbConsent.setText("I agree that my location, health details and accident videos may be shared "
                + "with my emergency contact and my company only when an emergency happens.");
        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setText("Create account");
        btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String name = FormUtils.text(etName);
        String phone = FormUtils.text(etPhone);
        String email = FormUtils.text(etEmail);
        String company = FormUtils.text(etCompany);
        String pw = FormUtils.raw(etPassword);
        String contactName = FormUtils.text(etContactName);
        String contactPhone = FormUtils.text(etContactPhone);

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || company.isEmpty()
                || contactName.isEmpty() || contactPhone.isEmpty()) {
            showError("Please fill in all fields marked *.");
            return;
        }
        if (!email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }
        if (pw.length() < 6) {
            showError("The password must have at least 6 characters.");
            return;
        }
        if (!cbConsent.isChecked()) {
            showError("Please agree to emergency data sharing. RideGuard cannot alert anyone without it.");
            return;
        }

        Rider r = new Rider();
        r.fullName = name;
        r.phone = phone;
        r.email = email;
        r.companyId = company;
        r.companyName = company;
        r.riderCode = FormUtils.orNull(FormUtils.text(etRiderCode));
        r.contactName = contactName;
        r.contactRelationship = FormUtils.orNull(FormUtils.text(etContactRel));
        r.contactPhone = contactPhone;
        r.bloodType = FormUtils.orNull(FormUtils.text(etBlood));
        r.allergies = FormUtils.orNull(FormUtils.text(etAllergies));
        r.conditions = FormUtils.orNull(FormUtils.text(etConditions));
        r.medication = FormUtils.orNull(FormUtils.text(etMedication));
        r.consent = true;

        tvError.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        repo.register(r, pw, (s, err) -> {
            if (isFinishing()) return;
            progress.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            if (err != null || s == null) {
                showError(err != null ? err : "Registration failed.");
                return;
            }
            SessionManager.save(this, s);
            toast("Account created. Welcome, " + name + "!");
            UiUtils.goHome(this, s);
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
