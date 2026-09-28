# ITServe — Local Setup Guide (Windows, step by step)

Follow these steps in order. Everything runs in **PowerShell** (press `Win + R`, type `powershell`, press Enter).

---

## Step 1 — Install the required tools (skip whatever is already installed)

You need 3 things: **JDK 17+**, **Maven**, **Node.js 18+**.

Check what you already have:

```powershell
java -version    # should print 17 or higher
mvn -version     # should print Apache Maven 3.x
node -v          # should print v18 or higher
npm -v           # should print 9 or higher
```

If a command is "not recognized", install it:

```powershell
# JDK 21 (LTS)
winget install Oracle.JDK.21

# Maven
winget install Apache.Maven

# Node.js (LTS)
winget install OpenJS.NodeJS.LTS
```

> After installing, **close and reopen PowerShell** so the new tools are on the PATH.
> No winget? Download manually: [JDK](https://www.oracle.com/java/technologies/downloads/), [Maven](https://maven.apache.org/download.cgi) (unzip and add its `bin` folder to PATH), [Node.js](https://nodejs.org/).

MySQL is **not required** — the app runs on an in-memory H2 database by default and seeds its own demo data.

---

## Step 2 — Start the backend

```powershell
cd C:\Users\bompr\Documents\Qoder\2026-09-27\022491c8\itserve\backend
mvn spring-boot:run
```

The first run downloads dependencies (a few minutes). **Keep this window open.**

You know it worked when you see a line like:

```
Started ItServeApplication in X.X seconds
```

Then it's live at:
- API: `http://localhost:8081/api`
- Swagger UI (API docs): `http://localhost:8081/swagger-ui.html`

---

## Step 3 — Start the frontend

Open a **second PowerShell window** (leave the backend running):

```powershell
cd C:\Users\bompr\Documents\Qoder\2026-09-27\022491c8\itserve\frontend
npm install     # first time only
npm run dev
```

You'll see something like:

```
VITE ready in XXX ms
➜  Local:   http://localhost:5173/
```

Open that URL (if it says **5174** instead, use 5174 — that just means 5173 was busy).

---

## Step 4 — Log in

The login page has one-click demo buttons, or type:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@itserve.com` | `Password@123` |
| Support Agent | `agent1@itserve.com` | `Password@123` |
| Employee | `employee1@itserve.com` | `Password@123` |

**Try the full workflow:** log in as employee1 → *Raise Ticket* → log out → admin → assign it to an agent → log out → agent1 → move it to In Progress, comment, Resolve → log out → employee1 → rate it 5★ and Close.

---

## Step 5 — Stopping the app

- Stop backend or frontend: click its PowerShell window and press `Ctrl + C`.
- To start again later, just repeat Steps 2 and 3 (`npm install` not needed again).

> Note: the H2 database is in-memory, so all data resets to the seeded demo set every time the backend restarts. That's intended for the dev profile.

---

## Troubleshooting

**`Port 8081 was already in use`** — a previous backend is still running:
```powershell
netstat -ano | findstr :8081     # note the PID (last column)
taskkill /PID <pid> /F
```

**`mvn` / `java` / `node` is not recognized** — the tool isn't on PATH. Reinstall it (Step 1), then open a *new* PowerShell window. For Maven installed manually, add `C:\<maven-folder>\bin` to your PATH environment variable.

**`Unable to find a suitable main class`** — another Maven process was touching the `target` folder. Close other terminals running the backend, then run:
```powershell
mvn clean spring-boot:run
```

**Backend starts but the frontend shows login errors** — the backend isn't up yet or is on a different port. Check `http://localhost:8081/swagger-ui.html` loads in your browser.

**Port 5173 shows a different project** — something else is using 5173; Vite automatically falls back to 5174. Use whichever URL your `npm run dev` printed.

**Reset everything** — stop both, then in the backend folder run `mvn clean`, and in the frontend folder delete `node_modules` and run `npm install` again.

---

## Optional: run the automated checks

With the backend running:

```powershell
# 69 API checks (auth, tickets, SLA, roles, dashboards, KB...)
cd C:\Users\bompr\Documents\Qoder\2026-09-27\022491c8\itserve\scripts\api-verification
node run.mjs

# 18 backend unit/integration tests
cd C:\Users\bompr\Documents\Qoder\2026-09-27\022491c8\itserve\backend
mvn test
```
