package com.example.weeklyreport.repository;

import com.example.weeklyreport.exception.FeedbackRepositoryException;
import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportPeriod;
import com.example.weeklyreport.model.Urgency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcFeedbackRepository implements FeedbackRepository {

    private static final String FIND_SUBMITTED_WITHIN = """
            SELECT description, rating, urgency, submitted_at
            FROM feedbacks
            WHERE submitted_at >= ? AND submitted_at < ?
            ORDER BY submitted_at
            """;

    private final ConnectionProvider connectionProvider;

    public JdbcFeedbackRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    @Override
    public List<Feedback> findSubmittedWithin(ReportPeriod period) {
        Objects.requireNonNull(period, "period must not be null");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_SUBMITTED_WITHIN)) {
            statement.setObject(1, period.startInstant().atOffset(ZoneOffset.UTC));
            statement.setObject(2, period.endInstant().atOffset(ZoneOffset.UTC));

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Feedback> feedbacks = new ArrayList<>();
                while (resultSet.next()) {
                    feedbacks.add(toFeedback(resultSet));
                }
                return feedbacks;
            }
        } catch (SQLException e) {
            throw new FeedbackRepositoryException(
                    "Failed to fetch feedbacks submitted between "
                            + period.startInstant() + " and " + period.endInstant(), e);
        }
    }

    private static Feedback toFeedback(ResultSet resultSet) throws SQLException {
        return new Feedback(
                resultSet.getString("description"),
                resultSet.getInt("rating"),
                Urgency.valueOf(resultSet.getString("urgency")),
                resultSet.getObject("submitted_at", OffsetDateTime.class).toInstant());
    }
}