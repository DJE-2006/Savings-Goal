const router  = require('express').Router();
const bcrypt  = require('bcryptjs');
const { q }   = require('../db');
const { sign }    = require('../auth');
const { ok, fail, publicUser, isEmail } = require('../util');

// POST /auth/register
router.post('/register', async (req, res) => {
  const name     = String(req.body?.name     || '').trim();
  const email    = String(req.body?.email    || '').trim().toLowerCase();
  const password = String(req.body?.password || '');

  if (!name || !email || !password) return fail(res, 400, 'Name, email and password are required.');
  if (!isEmail(email))               return fail(res, 400, 'Please enter a valid email address.');
  if (password.length < 6)           return fail(res, 400, 'Password must be at least 6 characters.');

  const dup = await q('SELECT id FROM users WHERE email = $1 LIMIT 1', [email]);
  if (dup.rows.length) return fail(res, 409, 'An account with that email already exists.');

  const hash = await bcrypt.hash(password, 10);
  const now  = Date.now();
  const ins  = await q(
    `INSERT INTO users (name, email, password_hash, accent_color, created_at)
     VALUES ($1, $2, $3, '#0F766E', $4) RETURNING *`,
    [name, email, hash, now]
  );
  const u = ins.rows[0];
  ok(res, { token: sign(u.id), user: publicUser(u) });
});

// POST /auth/login
router.post('/login', async (req, res) => {
  const email    = String(req.body?.email    || '').trim().toLowerCase();
  const password = String(req.body?.password || '');
  if (!email || !password) return fail(res, 400, 'Email and password are required.');

  const r = await q('SELECT * FROM users WHERE email = $1 LIMIT 1', [email]);
  const u = r.rows[0];
  if (!u || !(await bcrypt.compare(password, u.password_hash))) {
    return fail(res, 401, 'Incorrect email or password.');
  }
  ok(res, { token: sign(u.id), user: publicUser(u) });
});

module.exports = router;
