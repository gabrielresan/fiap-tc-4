-- Sample feedbacks spread over the previous Monday-to-Sunday week (Sao Paulo time),
-- so a local run always has data no matter when the stack is started.
WITH last_monday AS (
    SELECT date_trunc('week', now() AT TIME ZONE 'America/Sao_Paulo') - INTERVAL '7 days' AS day
    )
INSERT INTO feedbacks (description, rating, urgency, submitted_at)
SELECT description, rating, urgency, (day + offset_from_monday) AT TIME ZONE 'America/Sao_Paulo'
FROM last_monday,
     (VALUES
          ('Aula muito bem explicada', 9, 'LOW', INTERVAL '0 days 10 hours'),
          ('Exemplos práticos ajudaram bastante', 8, 'LOW', INTERVAL '0 days 19 hours'),
          ('O áudio cortou algumas vezes', 5, 'MEDIUM', INTERVAL '1 days 21 hours'),
          ('Vídeo da aula não carrega', 1, 'CRITICAL', INTERVAL '3 days 9 hours 30 minutes'),
          ('Material complementar <desatualizado> & incompleto', 4, 'MEDIUM', INTERVAL '3 days 22 hours'),
          ('Excelente didática do professor', 10, 'LOW', INTERVAL '5 days 15 hours'),
          ('Link do exercício quebrado', 2, 'CRITICAL', INTERVAL '6 days 23 hours 45 minutes')
     ) AS sample(description, rating, urgency, offset_from_monday);