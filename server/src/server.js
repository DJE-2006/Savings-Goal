require('dotenv').config();
const express = require('express');
const cors    = require('cors');

const app = express();
app.disable('x-powered-by');
app.use(cors());
app.use(express.json({ limit: '256kb' }));

// Health probe — handy for Render's auto health check.
app.get('/', (_req, res) => {
  res.json({
    ok: true,
    service: 'SavingsGoal API',
    version: '1.0.0',
    endpoints: [
      'POST   /auth/register',
      'POST   /auth/login',
      'GET    /dashboard',
      'GET    /report',
      'GET    /goals[?search=...]',
      'GET    /goals/:id',
      'POST   /goals',
      'PUT    /goals/:id',
      'DELETE /goals/:id',
      'POST   /goals/:id/contributions',
      'GET    /profile',
      'PUT    /profile',
    ],
  });
});

app.use('/auth',    require('./routes/auth'));
app.use('/goals',   require('./routes/goals'));
app.use('/profile', require('./routes/profile'));
app.use('/',        require('./routes/summary'));      // /dashboard, /report

// 404
app.use((req, res) => res.status(404).json({ ok: false, error: `Not found: ${req.method} ${req.path}` }));

// Uncaught errors become JSON, not stack traces.
app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ ok: false, error: 'Server error.' });
});

const port = Number(process.env.PORT || 8080);
app.listen(port, '0.0.0.0', () => {
  console.log(`SavingsGoal API listening on http://0.0.0.0:${port}`);
});
