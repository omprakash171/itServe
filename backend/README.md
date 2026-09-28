# ITServe Backend

Spring Boot 3.5 REST API for the ITServe Enterprise IT Service Desk — JWT authentication, role-based authorization, ticket lifecycle workflow, SLA engine, dashboards, knowledge base, notifications, and reporting.

## Tech Stack

| Concern | Choice |
| --- | --- |
| Language / Runtime | Java 17+ (built and verified on JDK 26) |
| Framework | Spring Boot 3.5.x (Web, Data JPA, Security, Validation, Scheduling) |
| Auth | JWT (jjwt 0.12.x), BCrypt password hashing |
| Database | H2 in-memory (default, dev/demo) · MySQL 8 (`mysql` profile) |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven, Lombok |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc |

## Run

```bash
cd backend
mvn spring-boot:run
```

- API base URL: `http://localhost:8081/api`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- H2 console: `http://localhost:8081/h2-console` — JDBC URL `jdbc:h2:mem:itserve`, user `sa`, empty password

The default profile uses an in-memory H2 database with `ddl-auto: create-drop` and seeds full demo data on startup (`app.seed-demo-data=true`): 7 users, 9 categories, 4 SLA rules, 20+ tickets with history, comments, ratings, notifications, and knowledge-base articles.

### MySQL profile

```bash
export DB_URL="jdbc:mysql://localhost:3306/itserve?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME=root
export DB_PASSWORD=secret
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Reference DDL for MySQL is in [`../database/schema.sql`](../database/schema.sql) (Hibernate can also generate the schema from entities).

## Configuration (environment variables)

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP port |
| `JWT_SECRET` | dev-only placeholder | HMAC signing key — **must** be overridden in production |
| `JWT_EXPIRATION_MINUTES` | `480` | Token lifetime |
| `CORS_ORIGINS` | `http://localhost:[*]` | Allowed origin patterns (comma-separated) |
| `SEED_DEMO_DATA` | `true` | Seed demo data on startup |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | MySQL profile only |

See [`../.env.example`](../.env.example) for a template.

## Test

```bash
mvn test
```

18 tests covering the ticket workflow state machine (valid/invalid transitions and role rules), SLA deadline/breach/at-risk computation, and MockMvc API integration tests (auth, ticket lifecycle, authorization boundaries).

## Package Structure

```
com.itserve
├── ItServeApplication.java     # entry point (@EnableScheduling)
├── config/
│   ├── SecurityConfig.java     # filter chain, JWT filter, CORS, stateless session
│   ├── OpenApiConfig.java      # Swagger bearer-auth scheme
│   └── DataSeeder.java         # demo data (active when app.seed-demo-data=true)
├── controller/                 # REST controllers (DTOs only, no entities exposed)
├── dto/                        # request/response records with Bean Validation
├── entity/                     # JPA entities (User, Ticket, Category, SlaRule,
│                               #  Comment, Attachment, Notification,
│                               #  KnowledgeArticle, Rating, TicketHistory)
├── exception/                  # @RestControllerAdvice → {timestamp,status,error,message,path}
├── repository/                 # Spring Data JPA repositories + JpaSpecificationExecutor
├── security/                   # JwtService, JwtAuthenticationFilter, UserPrincipal
└── service/
    ├── TicketService.java      # CRUD, search/filter/pagination (Specifications)
    ├── TicketWorkflow.java     # static state machine + role transition rules
    ├── TicketSpecifications.java
    ├── SlaService.java         # deadline/breach/at-risk computation per priority
    ├── SlaMonitor.java         # @Scheduled breach & at-risk notifications
    ├── DashboardService.java   # role KPIs + rule-based incident trend detection
    ├── AuthService.java        # login/register/me/password change
    ├── UserService.java        # admin user management + agent workload
    ├── CommentService.java / RatingService.java / NotificationService.java
    └── KnowledgeBaseService.java
```

## Key Design Points

- **Ticket numbers** are human-readable: `INC-<year>-<6-digit sequence>` (e.g. `INC-2026-000027`).
- **Workflow** is enforced server-side (`TicketWorkflow`): `OPEN → ASSIGNED → IN_PROGRESS → WAITING_FOR_USER → RESOLVED → CLOSED`, with `REOPENED` and `ESCALATED` branches. Employees can only close/reopen their own tickets; agents cannot resolve-and-close in one step; role claims from the frontend are never trusted.
- **SLA**: CRITICAL 2h / HIGH 4h / MEDIUM 8h / LOW 24h response-resolution targets (admin-editable, 1–168h). Remaining time, `AT_RISK` (≤60 min), and `BREACHED` are computed per request; a scheduled monitor emits notifications.
- **Agent suggestions** are deterministic: skill match desc → active workload asc → name asc.
- **Errors** always return the consistent JSON envelope above; stack traces are never exposed.
- **Security**: BCrypt hashing, no passwords in responses, authorization checks on every protected operation, stateless JWT sessions.
