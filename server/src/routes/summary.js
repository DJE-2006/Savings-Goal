// Dashboard + Report aggregate endpoints.
const router = require('express').Router();
const { q }  = require('../db');
const { requireAuth } = require('../auth');
const { ok, goalRow } = require('../util');

router.use(requireAuth);

async function counts(userId) {
  const r = await q(
    `WITH goal_stats AS (
       SELECT
         COUNT(*)                                               AS total_goals,
         COUNT(*) FILTER (WHERE status = 'Completed')          AS completed,
         COUNT(*) FILTER (WHERE status = 'In Progress')        AS in_progress,
         COUNT(*) FILTER (WHERE status = 'Cancelled')          AS cancelled,
         COALESCE(SUM(target_amount), 0)                       AS total_target
       FROM goals WHERE user_id = $1
     ),
     saved_stats AS (
       SELECT COALESCE(SUM(c.amount), 0) AS total_saved
       FROM contributions c
       JOIN goals g ON g.id = c.goal_id
       WHERE g.user_id = $1
     )
     SELECT * FROM goal_stats, saved_stats`, [userId]);
  const row = r.rows[0];
  return {
    totalGoals:  Number(row.total_goals  || 0),
    completed:   Number(row.completed    || 0),
    inProgress:  Number(row.in_progress  || 0),
    cancelled:   Number(row.cancelled    || 0),
    totalTarget: Number(row.total_target || 0),
    totalSaved:  Number(row.total_saved  || 0),
  };
}

router.get('/dashboard', async (req, res) => {
  const c = await counts(req.userId);
  ok(res, {
    totalGoals: c.totalGoals, completed: c.completed,
    inProgress: c.inProgress, totalTarget: c.totalTarget, totalSaved: c.totalSaved,
  });
});

router.get('/report', async (req, res) => {
  const c = await counts(req.userId);
  const r = await q(
    `SELECT * FROM v_goals_with_progress
      WHERE user_id = $1 AND status = 'Completed'
      ORDER BY created_at DESC`, [req.userId]);
  ok(res, { ...c, completedGoals: r.rows.map(goalRow) });
});

module.exports = router;
