package br.com.fiap.tc4.reporting.integration.formatting;

import br.com.fiap.tc4.reporting.model.ReportEmail;
import br.com.fiap.tc4.reporting.model.ReportPeriod;
import br.com.fiap.tc4.reporting.model.WeeklyReport;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class HtmlReportFormatter {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ReportPeriod.ZONE);
    private static final String TABLE = "<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">";

    public ReportEmail format(WeeklyReport report) {
        Objects.requireNonNull(report, "report must not be null");
        String period = periodLabel(report.period());
        return new ReportEmail("Relatório semanal de feedbacks - " + period, body(report, period));
    }

    private static String periodLabel(ReportPeriod period) {
        return DATE.format(period.startDate()) + " a " + DATE.format(period.endDate().minusDays(1));
    }

    private static String body(WeeklyReport report, String period) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"></head><body>");
        html.append("<h1>Relatório semanal de feedbacks</h1>");
        html.append("<p>Período: ").append(period).append("</p>");

        html.append("<h2>Resumo</h2>");
        html.append("<p>Total de avaliações: ").append(report.totalFeedbacks()).append("</p>");
        html.append("<p>Média das avaliações: ").append(averageLabel(report)).append("</p>");

        html.append("<h2>Avaliações por dia</h2>");
        html.append(TABLE).append("<tr><th>Dia</th><th>Quantidade</th></tr>");
        report.countByDay().forEach((day, count) -> row(html, DATE.format(day), String.valueOf(count)));
        html.append("</table>");

        html.append("<h2>Avaliações por urgência</h2>");
        html.append(TABLE).append("<tr><th>Urgência</th><th>Quantidade</th></tr>");
        report.countByUrgency().forEach((urgency, count) -> row(html, urgency.name(), String.valueOf(count)));
        html.append("</table>");

        html.append("<h2>Avaliações</h2>");
        if (report.feedbacks().isEmpty()) {
            html.append("<p>Nenhuma avaliação recebida no período.</p>");
        } else {
            html.append(TABLE)
                    .append("<tr><th>Data de envio</th><th>Urgência</th><th>Nota</th><th>Descrição</th></tr>");
            report.feedbacks().forEach(feedback -> row(html,
                    DATE_TIME.format(feedback.submittedAt()),
                    feedback.urgency().name(),
                    String.valueOf(feedback.rating()),
                    escape(feedback.description())));
            html.append("</table>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String averageLabel(WeeklyReport report) {
        if (report.totalFeedbacks() == 0) {
            return "sem avaliações no período";
        }
        return String.format(PT_BR, "%.2f", report.averageRating());
    }

    private static void row(StringBuilder html, String... cells) {
        html.append("<tr>");
        for (String cell : cells) {
            html.append("<td>").append(cell).append("</td>");
        }
        html.append("</tr>");
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}