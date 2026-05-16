# SavingsGoal — Deployment Guide (no XAMPP)

End-to-end, free, no credit card. Real PostgreSQL + REST API hosted online so
the Android app can reach it from anywhere — meets the IPT 102 rubric:
hosted backend, hosted relational DB, JSON REST, CRUD.

| Layer        | Service                       | Free plan limit                |
|--------------|-------------------------------|--------------------------------|
| Database     | **Aiven for PostgreSQL**      | 1 service, 5 GB, real PG 15 + TLS |
| REST API     | **Render** web service        | 750 hrs / month                |
| Source repo  | **GitHub** (free)             | unlimited                      |

Total time: ~20 minutes.

---

## 1 · Grab your Aiven connection info

You've already created the service. From the service page, the **Connection
information** panel shows:

- **Host** — e.g. `pg-3fcf7164-dhruvnamikaze10-c376.c.aivencloud.com`
- **Port** — e.g. `22390`
- **User** — `avnadmin`
- **Password** — click the eye icon to reveal
- **Database name** — `defaultdb`
- **SSL mode** — `require`

---

## 2 · Configure local env

```bash
cd "C:\Users\Dhruv Jae\Desktop\SavingsGoal\server"
copy .env.example .env
```

Open `server\.env` and fill in:

```
DB_HOST=pg-3fcf7164-dhruvnamikaze10-c376.c.aivencloud.com
DB_PORT=22390
DB_USER=avnadmin
DB_PASSWORD=<your aiven password>
DB_NAME=defaultdb
DB_SSL=true
JWT_SECRET=<paste output of:  node -e "console.log(require('crypto').randomBytes(48).toString('hex'))">
```

---

## 3 · Apply the schema and run locally

```bash
npm install
npm run migrate              # creates users / goals / contributions / view
npm start                    # http://localhost:8080
```

Smoke-test in another terminal:

```bash
curl http://localhost:8080/
curl -X POST http://localhost:8080/auth/register ^
     -H "Content-Type: application/json" ^
     -d "{\"name\":\"Test\",\"email\":\"t@example.com\",\"password\":\"secret1\"}"
```

You should get `{"ok":true,"data":{"token":"...","user":{...}}}`.

From the Android emulator, the API base URL is `http://10.0.2.2:8080`.

---

## 4 · Push the code to GitHub

```bash
cd "C:\Users\Dhruv Jae\Desktop\SavingsGoal"
git init
git add server DEPLOY.md
git commit -m "Add Node.js REST API"
# Create an empty repo on github.com, then:
git branch -M main
git remote add origin https://github.com/<you>/SavingsGoal.git
git push -u origin main
```

`server/.gitignore` already excludes `node_modules/` and `.env`.

---

## 5 · Deploy the API to Render

1. Sign up at https://render.com/ (GitHub login).
2. **New +** → **Blueprint** → pick the SavingsGoal repo → Render reads
   `server/render.yaml` and proposes the `savingsgoal-api` web service.
3. Click **Apply**. The service builds (`npm install`) and starts (`npm start`).
4. Open the service → **Environment** → set the same Aiven values you used
   locally (`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`,
   `DB_SSL=true`). `JWT_SECRET` is auto-generated. **Save Changes** redeploys.
5. After deploy, your URL looks like
   `https://savingsgoal-api.onrender.com`. Hit `/` in a browser — you should
   see the endpoint catalog JSON.

Render's free plan sleeps after 15 min idle. First request after sleep takes
~30 s to wake. Mention this in your demo.

---

## 6 · Point the Android app at the hosted API

```java
public static final String BASE_URL = "https://savingsgoal-api.onrender.com";
```

Send `Authorization: Bearer <token>` on every authenticated request. Store the
token in `SessionManager` (SharedPreferences) after `/auth/login` returns it.

---

## 7 · Quick API reference

| Method | Path                              | Body (JSON)                                                              |
|--------|-----------------------------------|--------------------------------------------------------------------------|
| POST   | `/auth/register`                  | `{name, email, password}`                                                |
| POST   | `/auth/login`                     | `{email, password}`                                                      |
| GET    | `/goals?search=trip`              | –                                                                        |
| POST   | `/goals`                          | `{title, targetAmount, deadline}`                                        |
| PUT    | `/goals/:id`                      | `{title, targetAmount, deadline}` *or* `{status}`                        |
| DELETE | `/goals/:id`                      | –                                                                        |
| POST   | `/goals/:id/contributions`        | `{amount, note?}`                                                        |
| GET    | `/dashboard`                      | –                                                                        |
| GET    | `/report`                         | –                                                                        |
| GET/PUT| `/profile`                        | `{name?, email?, bio?, avatarEmoji?, accentColor?, currentPassword?, newPassword?}` |

Every response: `{ ok: true, data }` or `{ ok: false, error }`.

---

## 8 · Troubleshooting

- **`password authentication failed`** — wrong DB_PASSWORD. Reveal it again in Aiven.
- **`ETIMEDOUT` / `ECONNREFUSED`** — Aiven service still provisioning, wait 1 min.
- **`no pg_hba.conf entry … SSL off`** — set `DB_SSL=true`.
- **`self signed certificate`** — already handled (`rejectUnauthorized: false`).
- **Render deploy failed** — open the deploy logs; line above the red error
  is the cause (usually a missing env var).
- **Android `CLEARTEXT_NOT_PERMITTED`** — Render gives HTTPS, so this shouldn't
  fire. If it does, you typed `http://` somewhere.
