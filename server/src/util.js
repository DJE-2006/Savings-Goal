// Shared response helpers + row -> JSON shapers.

const ok   = (res, data = null)            => res.json({ ok: true,  data });
const fail = (res, status, error)          => res.status(status).json({ ok: false, error });

function publicUser(u) {
  return {
    id:          Number(u.id),
    name:        u.name,
    email:       u.email,
    bio:         u.bio,
    avatarEmoji: u.avatar_emoji,
    accentColor: u.accent_color,
    createdAt:   Number(u.created_at),
  };
}

function goalRow(r) {
  return {
    id:              Number(r.id),
    userId:          Number(r.user_id),
    title:           r.title,
    targetAmount:    Number(r.target_amount),
    savedAmount:     Number(r.saved_amount ?? 0),
    percentComplete: Number(r.percent_complete ?? 0),
    deadline:        r.deadline,
    status:          r.status,
    createdAt:       Number(r.created_at),
  };
}

const isEmail = v => typeof v === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
const isStatus = v => ['Not Started','In Progress','Completed','Cancelled'].includes(v);

module.exports = { ok, fail, publicUser, goalRow, isEmail, isStatus };
