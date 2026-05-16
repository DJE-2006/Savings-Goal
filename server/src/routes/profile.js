const router = require('express').Router();
const bcrypt = require('bcryptjs');
const { q }  = require('../db');
const { requireAuth } = require('../auth');
const { ok, fail, publicUser, isEmail } = require('../util');

router.use(requireAuth);

router.get('/', async (req, res) => {
  const r = await q('SELECT * FROM users WHERE id = $1', [req.userId]);
  if (!r.rows.length) return fail(res, 404, 'Profile not found.');
  ok(res, publicUser(r.rows[0]));
});

router.put('/', async (req, res) => {
  const r = await q('SELECT * FROM users WHERE id = $1', [req.userId]);
  if (!r.rows.length) return fail(res, 404, 'Profile not found.');
  const u = r.rows[0];

  const name        = String(req.body?.name        ?? u.name).trim();
  const email       = String(req.body?.email       ?? u.email).trim().toLowerCase();
  const bio         = req.body?.bio         !== undefined ? String(req.body.bio).trim()         : (u.bio         || '');
  const avatarEmoji = req.body?.avatarEmoji !== undefined ? String(req.body.avatarEmoji).trim() : (u.avatar_emoji || '');
  const accentColor = req.body?.accentColor !== undefined ? String(req.body.accentColor).trim() : (u.accent_color || '');
  const current     = String(req.body?.currentPassword || '');
  const newPassword = String(req.body?.newPassword     || '');

  if (!name)            return fail(res, 400, 'Name is required.');
  if (!isEmail(email))  return fail(res, 400, 'Please enter a valid email address.');

  if (email !== String(u.email).toLowerCase()) {
    const dup = await q('SELECT id FROM users WHERE email = $1 AND id <> $2 LIMIT 1', [email, req.userId]);
    if (dup.rows.length) return fail(res, 409, 'That email is already in use.');
  }

  let newHash = null;
  if (newPassword) {
    if (!current || !(await bcrypt.compare(current, u.password_hash))) {
      return fail(res, 401, 'Current password is incorrect.');
    }
    if (newPassword.length < 6) return fail(res, 400, 'New password must be at least 6 characters.');
    newHash = await bcrypt.hash(newPassword, 10);
  }

  const sets = ['name = $1', 'email = $2', 'bio = $3', 'avatar_emoji = $4', 'accent_color = $5'];
  const args = [name, email, bio || null, avatarEmoji || null, accentColor || null];
  if (newHash) { sets.push(`password_hash = $${sets.length + 1}`); args.push(newHash); }
  args.push(req.userId);
  const where = `WHERE id = $${args.length}`;

  await q(`UPDATE users SET ${sets.join(', ')} ${where}`, args);
  const fresh = await q('SELECT * FROM users WHERE id = $1', [req.userId]);
  ok(res, publicUser(fresh.rows[0]));
});

module.exports = router;
