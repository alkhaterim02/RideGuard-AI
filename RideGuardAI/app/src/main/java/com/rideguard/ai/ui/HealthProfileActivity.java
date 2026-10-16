package com.rideguard.ai.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.rideguard.ai.R;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.FormUtils;

/** 13.5 Rider health profile and emergency contact. */
public class HealthProfileActivity extends BaseActivity {
    private Rider rider;
    private EditText etName, etAge, etBlood, etAllergies, etConditions, etMedication, etNotes;
    private EditText etContactName, etContactRel, etContactPhone;
    private TextView tvError;
    private View progress;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_form);
        setTitle("Health profile");
        showBack();

        ((TextView) findViewById(R.id.tvIntro)).setText("Only add information that would help emergency "
                + "responders. It is shared only when an emergency happens.");
        LinearLayout form = findViewById(R.id.formFields);

        FormUtils.addSection(form, "About you");
        etName = FormUtils.addField(form, "Full name", FormUtils.NAME);
        etAge = FormUtils.addField(form, "Age", FormUtils.NUMBER);

        FormUtils.addSection(form, "Medical information");
        etBlood = FormUtils.addField(form, "Blood type (e.g. O+)", FormUtils.TEXT);
        etAllergies = FormUtils.addField(form, "Allergies", FormUtils.TEXT);
        etConditions = FormUtils.addField(form, "Chronic medical conditions", FormUtils.TEXT);
        etMedication = FormUtils.addField(form, "Important medication", FormUtils.TEXT);
        etNotes = FormUtils.addField(form, "Other information for emergency treatment", FormUtils.TEXT);

        FormUtils.addSection(form, "Emergency contact");
        etContactName = FormUtils.addField(form, "Contact name", FormUtils.NAME);
        etContactRel = FormUtils.addField(form, "Relationship", FormUtils.TEXT);
        etContactPhone = FormUtils.addField(form, "Contact phone number", FormUtils.PHONE);

        tvError = findViewById(R.id.tvError);
        progress = findViewById(R.id.progress);
        btnSave = findViewById(R.id.btnSubmit);
        btnSave.setText("Save health profile");
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> save());

        progress.setVisibility(View.VISIBLE);
        repo.getRider(session.riderId, (r, err) -> {
            if (isFinishing()) return;
            progress.setVisibility(View.GONE);
            if (r == null) {
                showError(err != null ? err : "Could not load your profile.");
                return;
            }
            rider = r;
            FormUtils.set(etName, r.fullName);
            FormUtils.set(etAge, r.age);
            FormUtils.set(etBlood, r.bloodType);
            FormUtils.set(etAllergies, r.allergies);
            FormUtils.set(etConditions, r.conditions);
            FormUtils.set(etMedication, r.medication);
            FormUtils.set(etNotes, r.medicalNotes);
            FormUtils.set(etContactName, r.contactName);
            FormUtils.set(etContactRel, r.contactRelationship);
            FormUtils.set(etContactPhone, r.contactPhone);
            btnSave.setEnabled(true);
        });
    }

    private void save() {
        if (rider == null) return;
        if (FormUtils.text(etName).isEmpty() || FormUtils.text(etContactName).isEmpty()
                || FormUtils.text(etContactPhone).isEmpty()) {
            showError("Your name and an emergency contact with a phone number are required.");
            return;
        }
        rider.fullName = FormUtils.text(etName);
        rider.age = FormUtils.orNull(FormUtils.text(etAge));
        rider.bloodType = FormUtils.orNull(FormUtils.text(etBlood));
        rider.allergies = FormUtils.orNull(FormUtils.text(etAllergies));
        rider.conditions = FormUtils.orNull(FormUtils.text(etConditions));
        rider.medication = FormUtils.orNull(FormUtils.text(etMedication));
        rider.medicalNotes = FormUtils.orNull(FormUtils.text(etNotes));
        rider.contactName = FormUtils.text(etContactName);
        rider.contactRelationship = FormUtils.orNull(FormUtils.text(etContactRel));
        rider.contactPhone = FormUtils.text(etContactPhone);

        tvError.setVisibility(View.GONE);
        btnSave.setEnabled(false);
        repo.saveRider(rider, (ok, err) -> {
            if (isFinishing()) return;
            btnSave.setEnabled(true);
            if (err != null) {
                showError("Could not save: " + err);
                return;
            }
            toast("Health profile saved.");
            finish();
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
