const router = require('express').Router();
const { q }   = require('../db');
const { requireAuth } = require('../auth');
const { ok, fail, goalRow, isStatus } = require('../util');

router.use(requireAuth);

// GET /goals  (?search=foo to filter by title)
router.get('/', async (req, res) => {
  const search = String(req.query.search || '').trim();
  if (search) {
    const r = await q(
      `SELECT * FROM v_goals_with_progress
        WHERE user_id = $1 AND title ILIKE $2
        ORDER BY created_at DESC`,
      [req.userId, `%${search}%`]
    );
    return ok(res, r.rows.map(goalRow));
  }
  const r = await q(
    'SELECT * FROM v_goals_with_progress WHERE user_id = $1 ORDER BY created_at DESC',
    [req.userId]
  );
  ok(res, r.rows.map(goalRow));
});

// GET /goals/:id
router.get('/:id', async (req, res) => {
  const id = Number(req.params.id);
  const r = await q(
    'SELECT * FROM v_goals_with_progress WHERE id = $1 AND user_id = $2 LIMIT 1',
    [id, req.userId]
  );
  if (!r.rows.length) return fail(res, 404, 'Goal not found.');
  ok(res, goalRow(r.rows[0]));
});

// POST /goals
router.post('/', async (req, res) => {
  const title    = String(req.body?.title    || '').trim();
  const target   = Number(req.body?.targetAmount || 0);
  const deadline = String(req.body?.deadline || '').trim();
  if (!title || target <= 0 || !deadline) {
    return fail(res, 400, 'Title, positive target amount and deadline are required.');
  }
  const r = await q(
    `INSERT INTO goals (user_id, title, target_amount, deadline, status, created_at)
     VALUES ($1, $2, $3, $4, 'Not Started', $5) RETURNING id`,
    [req.userId, title, target, deadline, Date.now()]
  );
  ok(res, { id: Number(r.rows[0].id) });
});

// PUT /goals/:id  — full edit OR status-only when body has only `status`
router.put('/:id', async (req, res) => {
  const id = Number(req.params.id);

  const own = await q('SELECT id FROM goals WHERE id = $1 AND user_id = $2', [id, req.userId]);
  if (!own.rows.length) return fail(res, 404, 'Goal not found.');

  const keys = Object.keys(req.body || {});
  if (keys.length === 1 && keys[0] === 'status') {
    if (!isStatus(req.body.status)) return fail(res, 400, 'Invalid status.');
    await q('UPDATE goals SET status = $1 WHERE id = $2', [req.body.status, id]);
    return ok(res);
  }

  const title    = String(req.body?.title    || '').trim();
  const target   = Number(req.body?.targetAmount || 0);
  const deadline = String(req.body?.deadline || '').trim();
  if (!title || target <= 0 || !deadline) {
    return fail(res, 400, 'Title, positive target amount and deadline are required.');
  }
  await q(
    'UPDATE goals SET title = $1, target_amount = $2, deadline = $3 WHERE id = $4',
    [title, target, deadline, id]
  );
  ok(res);
});

// DELETE /goals/:id
router.delete('/:id', async (req, res) => {
  const id = Number(req.params.id);
  const r = await q('DELETE FROM goals WHERE id = $1 AND user_id = $2', [id, req.userId]);
  if (r.rowCount === 0) return fail(res, 404, 'Goal not found.');
  ok(res);
});

// POST /goals/:id/contributions  — log a contribution
router.post('/:id/contributions', async (req, res) => {
  const goalId = Number(req.params.id);
  const amount = Number(req.body?.amount || 0);
  const note   = String(req.body?.note   || '').trim();
  if (amount <= 0) return fail(res, 400, 'Amount must be greater than zero.');

  const own = await q('SELECT status FROM goals WHERE id = $1 AND user_id = $2', [goalId, req.userId]);
  if (!own.rows.length) return fail(res, 404, 'Goal not found.');

  await q(
    'INSERT INTO contributions (goal_id, amount, note, date_added) VALUES ($1, $2, $3, $4)',
    [goalId, amount, note || null, Date.now()]
  );
  if (own.rows[0].status === 'Not Started') {
    await q("UPDATE goals SET status = 'In Progress' WHERE id = $1", [goalId]);
  }
  ok(res);
});

module.exports = router;
