-- SavingsGoal PostgreSQL schema (Aiven for PostgreSQL, free tier).
-- Apply via:  npm run migrate
-- Or paste into Aiven's web SQL console.

CREATE TABLE IF NOT EXISTS users (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  bio           VARCHAR(500),
  avatar_emoji  VARCHAR(16),
  accent_color  VARCHAR(16) DEFAULT '#0F766E',
  created_at    BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS goals (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title         VARCHAR(200) NOT NULL,
  target_amount NUMERIC(14,2) NOT NULL,
  deadline      VARCHAR(20) NOT NULL,
  status        VARCHAR(20) NOT NULL DEFAULT 'Not Started'
                CHECK (status IN ('Not Started','In Progress','Completed','Cancelled')),
  created_at    BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_goals_user ON goals(user_id);

CREATE TABLE IF NOT EXISTS contributions (
  id         BIGSERIAL PRIMARY KEY,
  goal_id    BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
  amount     NUMERIC(14,2) NOT NULL,
  note       VARCHAR(500),
  date_added BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_contrib_goal ON contributions(goal_id);

CREATE OR REPLACE VIEW v_goals_with_progress AS
SELECT
  g.id, g.user_id, g.title, g.target_amount, g.deadline,
  g.status, g.created_at,
  COALESCE(SUM(c.amount), 0)::NUMERIC(14,2) AS saved_amount,
  CASE
    WHEN g.target_amount > 0
      THEN LEAST(100, ROUND(COALESCE(SUM(c.amount),0) / g.target_amount * 100, 2))
    ELSE 0
  END AS percent_complete
FROM goals g
LEFT JOIN contributions c ON c.goal_id = g.id
GROUP BY g.id;
