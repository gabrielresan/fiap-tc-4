package br.com.fiap.tc4.reporting.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPeriodTest {

    private static Clock clockAt(String utcInstant) {
        return Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC);
    }

    @Test
    void previousWeekFromMidweekReturnsLastMondayToMonday() {
        // Wednesday 2026-11-04 10:00 in Sao Paulo
        ReportPeriod period = ReportPeriod.previousWeek(clockAt("2026-11-04T13:00:00Z"));

        assertEquals(LocalDate.of(2026, 10, 26), period.startDate());
        assertEquals(LocalDate.of(2026, 11, 2), period.endDate());
    }

    @Test
    void previousWeekWhenRunningOnMondayMorning() {
        // Monday 2026-11-02 08:00 in Sao Paulo (scheduled run time)
        ReportPeriod period = ReportPeriod.previousWeek(clockAt("2026-11-02T11:00:00Z"));

        assertEquals(LocalDate.of(2026, 10, 26), period.startDate());
        assertEquals(LocalDate.of(2026, 11, 2), period.endDate());
    }

    @Test
    void usesSaoPauloTimeZoneNotUtc() {
        // Already Monday 02:30 in UTC, but still Sunday 2026-11-01 23:30 in Sao Paulo
        ReportPeriod period = ReportPeriod.previousWeek(clockAt("2026-11-02T02:30:00Z"));

        assertEquals(LocalDate.of(2026, 10, 19), period.startDate());
        assertEquals(LocalDate.of(2026, 10, 26), period.endDate());
    }

    @Test
    void instantsAreMidnightInSaoPaulo() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

        assertEquals(Instant.parse("2026-10-26T03:00:00Z"), period.startInstant());
        assertEquals(Instant.parse("2026-11-02T03:00:00Z"), period.endInstant());
    }

    @Test
    void daysReturnsSevenConsecutiveDates() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

        assertEquals(7, period.days().size());
        assertEquals(LocalDate.of(2026, 10, 26), period.days().getFirst());
        assertEquals(LocalDate.of(2026, 11, 1), period.days().getLast());
    }

    @Test
    void rejectsStartNotBeforeEnd() {
        LocalDate day = LocalDate.of(2026, 10, 26);
        assertThrows(IllegalArgumentException.class, () -> new ReportPeriod(day, day));
    }

    @Test
    void containsIsInclusiveAtStartAndExclusiveAtEnd() {
        ReportPeriod period = new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

        assertTrue(period.contains(Instant.parse("2026-10-26T03:00:00Z")));
        assertTrue(period.contains(Instant.parse("2026-11-02T02:59:59Z")));
        assertFalse(period.contains(Instant.parse("2026-11-02T03:00:00Z")));
        assertFalse(period.contains(Instant.parse("2026-10-26T02:59:59Z")));
    }
}