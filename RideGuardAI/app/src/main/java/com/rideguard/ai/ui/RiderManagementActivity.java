package com.rideguard.ai.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rideguard.ai.R;
import com.rideguard.ai.model.DeviceStatus;
import com.rideguard.ai.model.Rider;
import com.rideguard.ai.util.FormUtils;
import com.rideguard.ai.util.UiUtils;

import java.util.HashMap;
import java.util.Map;

/** 14.5 Rider management: add, assign vest, activate/deactivate, reset password, remove. */
public class RiderManagementActivity extends BaseActivity {
    private RiderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;
        setContentView(R.layout.activity_list);
        setTitle("Rider management");
        showBack();

        MaterialButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setVisibility(View.VISIBLE);
        btnAdd.setText("+ Add rider");
        btnAdd.setOnClickListener(v -> showAddDialog());

        TextView empty = findViewById(R.id.tvEmpty);
        empty.setText("No riders yet. Press \"Add rider\".");
        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RiderAdapter(this::showActions);
        rv.setAdapter(adapter);

        track(repo.observeRiders(session.companyId, (list, err) -> {
            adapter.setRiders(list);
            empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            if (err != null) toast(err);
        }));
        track(repo.observeDevices(session.companyId, (list, err) -> {
            Map<String, DeviceStatus> map = new HashMap<>();
            if (list != null) {
                for (DeviceStatus d : list) map.put(d.deviceId, d);
            }
            adapter.setDevices(map);
        }));
    }

    private void showActions(Rider r) {
        boolean active = !"inactive".equals(r.status);
        String[] items = {
                "Assign vest",
                active ? "Mark as inactive" : "Mark as active",
                "Reset password",
                "Call rider",
                "Remove rider"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(r.fullName)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            assignVest(r);
                            break;
                        case 1:
                            r.status = active ? "inactive" : "active";
                            repo.saveRider(r, (ok, err) -> toast(err == null ? "Rider status updated." : err));
                            break;
                        case 2:
                            if (r.email == null) {
                                toast("This rider has no email address.");
                            } else {
                                repo.sendPasswordReset(r.email, (ok, err) ->
                                        toast(err == null ? "Password reset email sent to " + r.email : err));
                            }
                            break;
                        case 3:
                            UiUtils.dial(this, r.phone);
                            break;
                        default:
                            UiUtils.confirm(this, "Remove " + r.fullName + "?",
                                    "The rider will no longer appear in your list. Past incidents are kept.",
                                    "Remove", () -> repo.removeRider(r.id, (ok, err) ->
                                            toast(err == null ? "Rider removed." : err)));
                            break;
                    }
                })
                .show();
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = UiUtils.dp(this, 20);
        form.setPadding(p, p / 2, p, 0);
        return form;
    }

    private void assignVest(Rider r) {
        LinearLayout form = dialogForm();
        EditText et = FormUtils.addField(form, "Vest ID (e.g. VEST-003)", FormUtils.TEXT);
        FormUtils.set(et, r.deviceId);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Assign vest to " + r.fullName)
                .setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    r.deviceId = FormUtils.orNull(FormUtils.text(et).toUpperCase());
                    repo.saveRider(r, (ok, err) -> toast(err == null ? "Vest assignment saved." : err));
                })
                .setNegativeButton("Go back", null)
                .show();
    }

    private void showAddDialog() {
        LinearLayout form = dialogForm();
        EditText etName = FormUtils.addField(form, "Full name *", FormUtils.NAME);
        EditText etPhone = FormUtils.addField(form, "Phone number", FormUtils.PHONE);
        EditText etEmail = FormUtils.addField(form, "Email", FormUtils.EMAIL);
        EditText etCode = FormUtils.addField(form, "Rider ID number", FormUtils.TEXT);
        EditText etVest = FormUtils.addField(form, "Vest ID (optional)", FormUtils.TEXT);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add rider")
                .setView(form)
                .setPositiveButton("Add", (d, w) -> {
                    String name = FormUtils.text(etName);
                    if (name.isEmpty()) {
                        toast("The rider's name is required.");
                        return;
                    }
                    Rider r = new Rider();
                    r.fullName = name;
                    r.phone = FormUtils.orNull(FormUtils.text(etPhone));
                    r.email = FormUtils.orNull(FormUtils.text(etEmail));
                    r.riderCode = FormUtils.orNull(FormUtils.text(etCode));
                    r.deviceId = FormUtils.orNull(FormUtils.text(etVest).toUpperCase());
                    r.companyId = session.companyId;
                    repo.addRider(r, (ok, err) -> {
                        if (err != null) toast(err);
                        else if (repo.isDemo()) toast("Rider added. Demo password: demo123");
                        else toast("Rider invited. Ask them to register in the app with company code "
                                    + session.companyId);
                    });
                })
                .setNegativeButton("Go back", null)
                .show();
    }
}
