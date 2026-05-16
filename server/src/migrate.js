// Apply db/schema.sql to whichever Postgres database .env points at.
require('dotenv').config();
const fs    = require('fs');
const path  = require('path');
const { Client } = require('pg');

(async () => {
  const useSsl = String(process.env.DB_SSL || '').toLowerCase() === 'true';
  const client = new Client({
    host:     process.env.DB_HOST,
    port:     Number(process.env.DB_PORT || 5432),
    user:     process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    ssl:      useSsl ? { rejectUnauthorized: false } : false,
  });
  await client.connect();
  const sql = fs.readFileSync(path.join(__dirname, '..', 'db', 'schema.sql'), 'utf8');
  console.log(`Applying schema to ${process.env.DB_HOST}/${process.env.DB_NAME} ...`);
  await client.query(sql);
  await client.end();
  console.log('Schema applied OK.');
})().catch(err => {
  console.error('Migration failed:', err.message);
  process.exit(1);
});
