// Lazy PostgreSQL connection pool (works with Aiven free tier).
const { Pool } = require('pg');

let pool;

function getPool() {
  if (pool) return pool;
  const useSsl = String(process.env.DB_SSL || '').toLowerCase() === 'true';
  pool = new Pool({
    host:     process.env.DB_HOST,
    port:     Number(process.env.DB_PORT || 5432),
    user:     process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    max: 8,
    // Aiven requires TLS. The free-tier CA is self-signed — accept it.
    ssl: useSsl ? { rejectUnauthorized: false } : false,
  });
  return pool;
}

// Convenience wrapper so routes can do `await q('SELECT ... $1', [x])`.
async function q(text, params = []) {
  const res = await getPool().query(text, params);
  return res;
}

module.exports = { getPool, q };
