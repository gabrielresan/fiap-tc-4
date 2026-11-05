package com.example.weeklyreport.model;

import java.util.Objects;

public record ReportEmail(
        String subject,
        String htmlBody
) {

    public ReportEmail {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(htmlBody, "htmlBody must not be null");
    }
}
