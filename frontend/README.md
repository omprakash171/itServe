# ITServe Frontend

React 18 + Vite single-page app for the ITServe Enterprise IT Service Desk. Bootstrap 5 UI with role-aware navigation, live dashboards, ticket lifecycle actions, SLA badges, notifications, knowledge base, and user management.

## Tech Stack

| Concern | Choice |
| --- | --- |
| UI | React 18, React Router 6, Bootstrap 5 (custom modals/badges — no react-bootstrap) |
| Data | Axios (Bearer-token interceptor, 401 → auto logout/redirect) |
| Charts | Recharts |
| Build | Vite |

## Run

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173` (Vite automatically falls back to `5174`+ if 5173 is busy — the app works on any port). The dev server proxies `/api` → `http://localhost:8081`, so start the backend first (see [`../backend/README.md`](../backend/README.md)).

Production build:

```bash
npm run build   # outputs to dist/
npm run preview
```

## Demo Accounts

All seeded accounts use password `Password@123`:

| Role | Emails |
| --- | --- |
| Admin | `admin@itserve.com` |
| Support Agent | `agent1@itserve.com`, `agent2@itserve.com`, `agent3@itserve.com` |
| Employee | `employee1@itserve.com`, `employee2@itserve.com`, `employee3@itserve.com` |

## Project Structure

```
src/
├── main.jsx / App.jsx        # entry + routes (ProtectedRoute with role guards)
├── index.css                 # Bootstrap imports + app theme
├── context/
│   └── AuthContext.jsx       # token/user state, localStorage persistence
├── services/
│   ├── api.js                # axios instance, interceptors, error envelope helper
│   ├── authService.js        # login / register / me / change password
│   ├── ticketService.js      # tickets, comments, ratings, attachments, categories
│   └── dashboardService.js   # dashboards, users, knowledge base, notifications
├── components/
│   ├── Layout.jsx            # navbar (role-filtered), sidebar, notification bell
│   ├── ProtectedRoute.jsx    # auth + role route guard
│   ├── NotificationBell.jsx  # unread badge, dropdown, mark-read
│   ├── StatusBadge / PriorityBadge / SlaBadge / KpiCard / TicketTable / ToastProvider
│   └── dashboard/            # Admin / Agent / Employee dashboards + Charts
├── pages/
│   ├── Login.jsx             # login + registration tabs
│   ├── Dashboard.jsx         # role switch → Admin/Agent/Employee dashboard
│   ├── Tickets.jsx           # search, filters, pagination (role-scoped)
│   ├── TicketDetails.jsx     # timeline, comments, status actions, assign,
│   │                         # escalate, attachments, rating, history
│   ├── CreateTicket.jsx
│   ├── Users.jsx             # admin user management
│   ├── KnowledgeBase.jsx     # search, helpful votes, agent/admin authoring
│   └── Profile.jsx           # details + change password
└── utils/format.js           # dates, labels, SLA text formatting
```

## Notes

- **Roles are a UI concern only** — the backend re-checks authorization on every request; frontend role state is never trusted for security.
- SLA badges render `XXh XXm remaining`, `AT RISK`, or `SLA BREACHED` from server-computed values.
- Employee views are automatically scoped to their own tickets (also enforced server-side).
- The `/api` proxy means no CORS issues in dev; `CORS_ORIGINS` on the backend covers direct cross-origin access.
