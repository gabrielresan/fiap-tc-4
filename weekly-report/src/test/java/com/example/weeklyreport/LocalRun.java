package com.example.weeklyreport;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class LocalRun {

    private static final Map<String, String> LOCAL_DEFAULTS = Map.of(
            "DB_URL", "jdbc:postgresql://localhost:55432/feedbacks",
            "DB_USER", "weekly_report",
            "DB_PASSWORD", "weekly_report",
            "NOTIFICATION_FROM", "reports@example.com",
            "NOTIFICATION_RECIPIENTS", "admin@example.com");

    public static void main(String[] args) {
        Map<String, String> env = new HashMap<>(LOCAL_DEFAULTS);
        env.putAll(System.getenv());

        new WeeklyReportHandler(env, Clock.systemUTC()).run();
    }
}