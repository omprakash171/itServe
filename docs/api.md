# ITServe — REST API Reference

Base URL (local): `http://localhost:8081/api`
Interactive docs: `http://localhost:8081/swagger-ui.html` (OpenAPI JSON at `/v3/api-docs`)

All endpoints except `/api/auth/login` and `/api/auth/register` require:

```http
Authorization: Bearer <JWT>
```

Roles: `ADMIN`, `SUPPORT_AGENT`, `EMPLOYEE`. "any" below means any authenticated user.
Errors always use the standard envelope `{timestamp, status, error, message, path, fieldErrors}`.

## Authentication — `/api/auth`

| Method | Path        | Role | Description                                            |
|--------|-------------|------|--------------------------------------------------------|
| POST   | `/login`    | public | Body `{email, password}` → `{token, user}`           |
| POST   | `/register` | public | Body `{fullName, email, password, department}` → `{token, user}` (always EMPLOYEE role) |
| GET    | `/me`       | any  | Current user profile                                   |
| PATCH  | `/password` | any  | Body `{currentPassword, newPassword}`                  |

## Users — `/api/users` (ADMIN only)

| Method | Path     | Description                                        |
|--------|----------|----------------------------------------------------|
| GET    | ``       | List users, optional `?role=` filter               |
| GET    | `/{id}`  | Get one user                                       |
| POST   | ``       | Create user `{fullName, email, password, role, department, skills[]}` |
| PUT    | `/{id}`  | Update `{fullName, email, department, skills[], active}` (role immutable) |
| DELETE | `/{id}`  | Deactivate (soft delete; cannot deactivate self)   |

## Agents — `/api/agents` (ADMIN, SUPPORT_AGENT)

| Method | Path        | Description                                                      |
|--------|-------------|------------------------------------------------------------------|
| GET    | ``          | Active support agents `{id, fullName, skills[]}` (for filter dropdowns) |
| GET    | `/workload` | Per-agent counts: assigned / open / inProgress / resolved / breached |

## Tickets — `/api/tickets`

| Method | Path                     | Role   | Description |
|--------|--------------------------|--------|-------------|
| GET    | ``                       | any    | Paged list. Params: `search`, `status`, `priority`, `categoryId`, `assignedTo` (`me` \| `unassigned` \| agent id), `createdBy` (`me` \| id), `sla` (`breached` \| `at_risk`), `createdFrom`, `createdTo`, `page`, `size`, `sort` (e.g. `createdAt,desc`). EMPLOYEE results are always scoped to their own tickets. |
| GET    | `/{id}`                  | any*   | `TicketDetailResponse {ticket, comments[], history[]}`. *Owner, assignee or ADMIN. |
| POST   | ``                       | any    | Create `{subject, description, categoryId, priority, location?, preferredContact?}` → ticket with generated `ticketNumber` (`INC-YYYY-NNNNNN`), status OPEN and SLA deadline |
| PUT    | `/{id}`                  | any*   | Update subject/description/category/priority/location/contact. *Creator while OPEN, or ADMIN. |
| PATCH  | `/{id}/status`           | any*   | `{status, note?}` — validated state machine (see architecture.md). RESOLVED requires `note` (422 otherwise). |
| PATCH  | `/{id}/assign`           | ADMIN  | `{agentId}` → status becomes ASSIGNED; notifies agent + creator |
| PATCH  | `/{id}/priority`         | ADMIN  | `{priority}` → recalculates SLA deadline |
| PATCH  | `/{id}/escalate`         | ADMIN, SUPPORT_AGENT | Sets ESCALATED (from OPEN/ASSIGNED/IN_PROGRESS) |
| GET    | `/{id}/suggested-agents` | ADMIN, SUPPORT_AGENT | Ranked suggestions `{agentId, fullName, skills, skillMatch, activeTickets, breachedTickets}` |
| DELETE | `/{id}`                  | ADMIN  | Delete ticket and its children |
| POST   | `/{id}/attachments`      | any*   | `multipart/form-data` field `file` (max 5 MB). *Participants only, not CLOSED. |
| GET    | `/attachments/{attachmentId}` | any* | File download. *Participants or ADMIN. |

## Comments — `/api/tickets/{ticketId}/comments`

| Method | Path   | Role | Description |
|--------|--------|------|-------------|
| GET    | ``     | any* | List comments. *Ticket participants or ADMIN. |
| POST   | ``     | any* | `{message}`; agents/admins set first-response time; notifies the other side. Not allowed on CLOSED tickets. |

## Ratings

| Method | Path                        | Role | Description |
|--------|-----------------------------|------|-------------|
| POST   | `/api/tickets/{id}/rating`  | EMPLOYEE (owner) | `{stars 1-5, feedback?}`; only RESOLVED/CLOSED, once per ticket |
| GET    | `/api/tickets/{id}/rating`  | any  | Rating for a ticket (or 404) |
| GET    | `/api/ratings`              | ADMIN, SUPPORT_AGENT | All ratings with ticket/agent context |

## Notifications — `/api/notifications`

| Method | Path             | Role | Description |
|--------|------------------|------|-------------|
| GET    | ``               | any  | Paged list for the current user (`page`, `size`) |
| GET    | `/unread-count`  | any  | `{count: n}` |
| PATCH  | `/{id}/read`     | any  | Mark one as read |
| PATCH  | `/read-all`      | any  | Mark all as read |

## Dashboard — `/api/dashboard`

| Method | Path        | Role          | Description |
|--------|-------------|---------------|-------------|
| GET    | `/admin`    | ADMIN         | KPIs (totals, SLA compliance %, avg resolution/first response, avg rating, resolution rate), by-status/priority/category series, 14-day created-vs-resolved series, agent workload, incident trends |
| GET    | `/agent`    | SUPPORT_AGENT | Personal KPIs + today's tasks ordered by SLA deadline |
| GET    | `/employee` | EMPLOYEE      | Personal KPIs + recent tickets |
| GET    | `/me`       | any           | Role-aware alias that dispatches to the right dashboard |

## Knowledge Base — `/api/articles`

| Method | Path          | Role | Description |
|--------|---------------|------|-------------|
| GET    | ``            | any  | List; optional `?search=` (title/content) |
| GET    | `/{id}`       | any  | Article detail |
| POST   | ``            | ADMIN, SUPPORT_AGENT | `{title, category, content}` |
| PUT    | `/{id}`       | ADMIN, SUPPORT_AGENT | Update article |
| POST   | `/{id}/vote`  | any  | `{helpful: true|false}` → `{helpfulYes, helpfulNo}` |

## SLA Rules — `/api/sla-rules`

| Method | Path     | Role  | Description |
|--------|----------|-------|-------------|
| GET    | ``       | any   | All priority → hours rules |
| PUT    | `/{id}`  | ADMIN | `{resolutionHours 1-168}` |

## Categories — `/api/categories`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET    | ``   | any  | All ticket categories |
