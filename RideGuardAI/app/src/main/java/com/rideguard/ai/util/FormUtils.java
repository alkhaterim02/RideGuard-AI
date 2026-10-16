package com.rideguard.ai.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.rideguard.ai.R;

/** Builds simple form fields so long forms stay readable in code. */
public final class FormUtils {
    public static final int TEXT = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
    public static final int NAME = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    public static final int EMAIL = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    public static final int PHONE = InputType.TYPE_CLASS_PHONE;
    public static final int NUMBER = InputType.TYPE_CLASS_NUMBER;
    public static final int PASSWORD = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

    private FormUtils() {
    }

    public static EditText addField(LinearLayout parent, String hint, int inputType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_field, parent, false);
        TextInputLayout til = (TextInputLayout) v;
        til.setHint(hint);
        EditText et = v.findViewById(R.id.fieldEdit);
        et.setInputType(inputType);
        if (inputType == PASSWORD) til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        parent.addView(v);
        return et;
    }

    public static void addSection(LinearLayout parent, String title) {
        Context c = parent.getContext();
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(19);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ContextCompat.getColor(c, R.color.rg_text));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = UiUtils.dp(c, 22);
        parent.addView(t, lp);
    }

    /** Trimmed text, never null. */
    public static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    /** Untrimmed text (for passwords). */
    public static String raw(EditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    public static void set(EditText e, String value) {
        e.setText(value == null ? "" : value);
    }

    public static String orNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
