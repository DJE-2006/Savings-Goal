# SavingsGoal REST API

Node.js + Express + **PostgreSQL** backend for the SavingsGoal Android app.
No XAMPP / no local DB — uses managed Postgres on Aiven (free tier).

PostgreSQL is a relational database, which is what the IPT 102 rubric asks for
("a relational database such as MySQL"). The Android-facing JSON contract is
identical regardless of which DB is underneath.

## Stack
- **Runtime**: Node.js 18+ (LTS), Express 4
- **DB driver**: `pg` (node-postgres)
- **Auth**: bcrypt password hashing + JWT bearer tokens
- **Host (API)**: Render free web service
- **Host (DB)**: Aiven for PostgreSQL free plan (real Postgres 15, TLS)

## Endpoints (all JSON)

| Method | Path                          | Auth | Purpose                          |
|--------|-------------------------------|:----:|----------------------------------|
| GET    | `/`                           |  –   | Health probe + endpoint catalog  |
| POST   | `/auth/register`              |  –   | Create account, returns JWT      |
| POST   | `/auth/login`                 |  –   | Issue JWT                        |
| GET    | `/dashboard`                  |  ✓   | Summary counters                 |
| GET    | `/report`                     |  ✓   | Full report incl. completed list |
| GET    | `/goals?search=...`           |  ✓   | List + search                    |
| GET    | `/goals/:id`                  |  ✓   | Single goal                      |
| POST   | `/goals`                      |  ✓   | Create                           |
| PUT    | `/goals/:id`                  |  ✓   | Edit (full) or status-only       |
| DELETE | `/goals/:id`                  |  ✓   | Delete                           |
| POST   | `/goals/:id/contributions`    |  ✓   | Log a contribution               |
| GET    | `/profile`                    |  ✓   | Current user                     |
| PUT    | `/profile`                    |  ✓   | Update profile / password        |

Auth: send `Authorization: Bearer <token>` on every `✓` row.

Response shape: `{ "ok": true, "data": ... }` or `{ "ok": false, "error": "..." }`.

## Local dev (against hosted DB)

```bash
cd server
cp .env.example .env        # fill DB_* with your Aiven creds
npm install
npm run migrate             # applies db/schema.sql once
npm run dev                 # http://localhost:8080
```

See **DEPLOY.md** for the full walkthrough.
