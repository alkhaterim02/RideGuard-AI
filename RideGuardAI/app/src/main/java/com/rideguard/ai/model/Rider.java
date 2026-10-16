package com.rideguard.ai.model;

/** Rider profile, including the minimum health data needed in an emergency. */
public class Rider {
    public String id;
    public String fullName;
    public String phone;
    public String email;
    public String companyId;
    public String companyName;
    public String riderCode;

    public String age;
    public String bloodType;
    public String allergies;
    public String conditions;
    public String medication;
    public String medicalNotes;

    public String contactName;
    public String contactRelationship;
    public String contactPhone;

    public boolean consent;
    public String deviceId;
    /** active | inactive | invited | removed */
    public String status = "active";

    public Rider() {
    }

    public String healthSnapshot() {
        StringBuilder sb = new StringBuilder();
        append(sb, "Blood type", bloodType);
        append(sb, "Allergies", allergies);
        append(sb, "Conditions", conditions);
        append(sb, "Medication", medication);
        append(sb, "Notes", medicalNotes);
        append(sb, "Age", age);
        return sb.length() == 0 ? "No health information provided" : sb.toString();
    }

    private static void append(StringBuilder sb, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(label).append(": ").append(value.trim());
    }

    public Rider copy() {
        Rider r = new Rider();
        r.id = id;
        r.fullName = fullName;
        r.phone = phone;
        r.email = email;
        r.companyId = companyId;
        r.companyName = companyName;
        r.riderCode = riderCode;
        r.age = age;
        r.bloodType = bloodType;
        r.allergies = allergies;
        r.conditions = conditions;
        r.medication = medication;
        r.medicalNotes = medicalNotes;
        r.contactName = contactName;
        r.contactRelationship = contactRelationship;
        r.contactPhone = contactPhone;
        r.consent = consent;
        r.deviceId = deviceId;
        r.status = status;
        return r;
    }
}
