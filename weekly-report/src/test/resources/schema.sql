CREATE TABLE feedbacks (
                           id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           description  TEXT        NOT NULL,
                           rating       SMALLINT    NOT NULL CHECK (rating BETWEEN 0 AND 10),
                           urgency      VARCHAR(10) NOT NULL CHECK (urgency IN ('LOW', 'MEDIUM', 'CRITICAL')),
                           submitted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedbacks_submitted_at ON feedbacks (submitted_at);