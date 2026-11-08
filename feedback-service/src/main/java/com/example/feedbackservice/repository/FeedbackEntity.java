package com.example.feedbackservice.repository;

import com.example.feedbackservice.model.Feedback;
import com.example.feedbackservice.model.Urgency;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA mapping of the {@code feedbacks} table. Hibernate generates the table from this class, so the
 * constraints, index and defaults below define the schema that the weekly-report reads.
 * Rows are written once and never updated.
 */
@Entity
@Table(name = "feedbacks",
        indexes = @Index(name = "idx_feedbacks_submitted_at", columnList = "submitted_at"))
public class FeedbackEntity implements Persistable<UUID> {

    @Id
    @ColumnDefault("gen_random_uuid()")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false, check = @CheckConstraint(
            name = "feedbacks_rating_check", constraint = "rating BETWEEN 0 AND 10"))
    private int rating;

    // Hibernate adds a CHECK restricting the column to the enum names (LOW, MEDIUM, CRITICAL).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @ColumnDefault("now()")
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    // The id is assigned by the application, so Spring Data cannot infer newness from a null id.
    @Transient
    private boolean isNew = true;

    protected FeedbackEntity() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    private FeedbackEntity(UUID id, String description, int rating, Urgency urgency, Instant submittedAt) {
        this.id = id;
        this.description = description;
        this.rating = rating;
        this.urgency = urgency;
        this.submittedAt = submittedAt;
    }

    public static FeedbackEntity from(Feedback feedback) {
        return new FeedbackEntity(
                feedback.id(),
                feedback.description(),
                feedback.rating(),
                feedback.urgency(),
                feedback.submittedAt());
    }

    public Feedback toFeedback() {
        return new Feedback(id, description, rating, urgency, submittedAt);
    }
}
