package br.com.fiap.tc4.reporting.repository;

import br.com.fiap.tc4.reporting.exception.FeedbackRepositoryException;
import br.com.fiap.tc4.reporting.model.Feedback;
import br.com.fiap.tc4.reporting.model.ReportPeriod;
import br.com.fiap.tc4.reporting.model.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class JdbcFeedbackRepositoryTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine").withInitScript("schema.sql");

    private static final ReportPeriod PERIOD =
            new ReportPeriod(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2));

    private ConnectionProvider connectionProvider;
    private JdbcFeedbackRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connectionProvider = new PasswordConnectionProvider(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        repository = new JdbcFeedbackRepository(connectionProvider);

        try (Connection connection = connectionProvider.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE feedbacks");
        }
    }

    private void insert(String description, int rating, Urgency urgency, String submittedAt) throws SQLException {
        String sql = "INSERT INTO feedbacks (description, rating, urgency, submitted_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, description);
            statement.setInt(2, rating);
            statement.setString(3, urgency.name());
            statement.setObject(4, OffsetDateTime.parse(submittedAt));
            statement.executeUpdate();
        }
    }

    @Test
    void returnsOnlyFeedbacksWithinPeriodOrderedBySubmissionDate() throws SQLException {
        insert("before start", 5, Urgency.MEDIUM, "2026-10-26T02:59:59Z");
        insert("last second", 9, Urgency.LOW, "2026-11-02T02:59:59Z");
        insert("exactly at start", 2, Urgency.CRITICAL, "2026-10-26T03:00:00Z");
        insert("exactly at end", 7, Urgency.LOW, "2026-11-02T03:00:00Z");

        List<Feedback> feedbacks = repository.findSubmittedWithin(PERIOD);

        assertEquals(List.of("exactly at start", "last second"),
                feedbacks.stream().map(Feedback::description).toList());
    }

    @Test
    void mapsAllColumns() throws SQLException {
        insert("Great class", 10, Urgency.LOW, "2026-10-28T15:30:00Z");

        List<Feedback> feedbacks = repository.findSubmittedWithin(PERIOD);

        assertEquals(
                List.of(new Feedback("Great class", 10, Urgency.LOW, Instant.parse("2026-10-28T15:30:00Z"))),
                feedbacks);
    }

    @Test
    void returnsEmptyListWhenThereAreNoFeedbacks() {
        assertTrue(repository.findSubmittedWithin(PERIOD).isEmpty());
    }

    @Test
    void wrapsSqlExceptions() {
        JdbcFeedbackRepository failingRepository = new JdbcFeedbackRepository(() -> {
            throw new SQLException("connection refused");
        });

        FeedbackRepositoryException exception = assertThrows(FeedbackRepositoryException.class,
                () -> failingRepository.findSubmittedWithin(PERIOD));
        assertInstanceOf(SQLException.class, exception.getCause());
    }
}