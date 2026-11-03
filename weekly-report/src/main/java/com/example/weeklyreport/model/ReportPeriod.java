package com.example.weeklyreport.model;

import java.time.*;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

public record ReportPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public ReportPeriod {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }
    }

    public static ReportPeriod previousWeek(Clock clock) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new ReportPeriod(currentMonday.minusWeeks(1), currentMonday);
    }

    public Instant startInstant() {
        return startDate.atStartOfDay(ZONE).toInstant();
    }

    public Instant endInstant() {
        return endDate.atStartOfDay(ZONE).toInstant();
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(startInstant()) && instant.isBefore(endInstant());
    }

    public List<LocalDate> days() {
        return startDate.datesUntil(endDate).toList();
    }
}
