const jwt = require('jsonwebtoken');

function sign(userId) {
  return jwt.sign({ uid: userId }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || '30d',
  });
}

// Express middleware that requires a valid Bearer token.
function requireAuth(req, res, next) {
  const h = req.headers.authorization || '';
  if (!h.toLowerCase().startsWith('bearer ')) {
    return res.status(401).json({ ok: false, error: 'Missing auth token.' });
  }
  try {
    const payload = jwt.verify(h.slice(7).trim(), process.env.JWT_SECRET);
    req.userId = Number(payload.uid);
    next();
  } catch (_e) {
    res.status(401).json({ ok: false, error: 'Invalid or expired token.' });
  }
}

module.exports = { sign, requireAuth };
