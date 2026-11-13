package br.com.fiap.tc4.reporting.integration.formatting;

import br.com.fiap.tc4.reporting.model.Feedback;
import br.com.fiap.tc4.reporting.model.ReportEmail;
import br.com.fiap.tc4.reporting.model.ReportPeriod;
import br.com.fiap.tc4.reporting.model.Urgency;
import br.com.fiap.tc4.reporting.service.WeeklyReportGenerator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportFormatterTest {

    private static final ReportPeriod PERIOD =
            new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

    private final HtmlReportFormatter formatter = new HtmlReportFormatter();
    private final WeeklyReportGenerator generator = new WeeklyReportGenerator();

    private ReportEmail format(Feedback... feedbacks) {
        return formatter.format(generator.generate(List.of(feedbacks), PERIOD));
    }

    private static Feedback feedback(String description, int rating, Urgency urgency, String submittedAt) {
        return new Feedback(description, rating, urgency, Instant.parse(submittedAt));
    }

    @Test
    void subjectShowsPeriodWithLastReportedDay() {
        assertEquals("Relatório semanal de feedbacks - 26/10/2026 a 01/11/2026", format().subject());
    }

    @Test
    void showsTotalAndAverageWithTwoDecimals() {
        String html = format(
                feedback("a", 2, Urgency.CRITICAL, "2026-10-26T15:00:00Z"),
                feedback("b", 7, Urgency.LOW, "2026-10-27T15:00:00Z"),
                feedback("c", 8, Urgency.LOW, "2026-10-28T15:00:00Z")).htmlBody();

        assertTrue(html.contains("Total de avaliações: 3"));
        assertTrue(html.contains("Média das avaliações: 5,67"));
    }

    @Test
    void emptyWeekShowsNoRatingsMessages() {
        String html = format().htmlBody();

        assertTrue(html.contains("Total de avaliações: 0"));
        assertTrue(html.contains("Média das avaliações: sem avaliações no período"));
        assertTrue(html.contains("Nenhuma avaliação recebida no período."));
    }

    @Test
    void listsEveryDayOfThePeriodWithItsCount() {
        String html = format(feedback("a", 5, Urgency.MEDIUM, "2026-10-26T15:00:00Z")).htmlBody();

        assertTrue(html.contains("<tr><td>26/10/2026</td><td>1</td></tr>"));
        assertTrue(html.contains("<tr><td>27/10/2026</td><td>0</td></tr>"));
        assertTrue(html.contains("<tr><td>01/11/2026</td><td>0</td></tr>"));
    }

    @Test
    void listsCountPerUrgency() {
        String html = format(
                feedback("a", 1, Urgency.CRITICAL, "2026-10-26T15:00:00Z"),
                feedback("b", 0, Urgency.CRITICAL, "2026-10-27T15:00:00Z")).htmlBody();

        assertTrue(html.contains("<tr><td>LOW</td><td>0</td></tr>"));
        assertTrue(html.contains("<tr><td>MEDIUM</td><td>0</td></tr>"));
        assertTrue(html.contains("<tr><td>CRITICAL</td><td>2</td></tr>"));
    }

    @Test
    void listsEachFeedbackWithSaoPauloSubmissionTime() {
        // 2026-10-28 01:30 UTC is 2026-10-27 22:30 in Sao Paulo
        String html = format(feedback("Aula travou", 3, Urgency.CRITICAL, "2026-10-28T01:30:00Z")).htmlBody();

        assertTrue(html.contains(
                "<tr><td>27/10/2026 22:30</td><td>CRITICAL</td><td>3</td><td>Aula travou</td></tr>"));
    }

    @Test
    void escapesHtmlInDescriptions() {
        String html = format(feedback("<script>alert('x')</script> & \"mais\"", 4, Urgency.MEDIUM,
                "2026-10-26T15:00:00Z")).htmlBody();

        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;mais&quot;"));
        assertFalse(html.contains("<script>"));
    }
}