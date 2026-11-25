# FlowSuite

A production-style vertical slice of **FlowSuite** — a multi-tenant SaaS workspace platform where independent customer organizations ("tenants") manage teams, customers, projects, tasks, subscriptions, and billing with **strict row-level data isolation**.

> Built iteratively across 4 milestones — **all four are now implemented**: structure + Docker + schema/seed + Auth/multi-tenancy (P1); RBAC + CRUD APIs and the React UI (P2); entitlements, Redis usage/rate limits, Stripe checkout/webhooks, usage dashboard, audit logs (P3); unit + cross-tenant integration tests, Swagger, CI, and the Architecture Note (P4).

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React (Vite) + TypeScript, Tailwind CSS, TanStack Query, React Hook Form, Recharts _(Phase 2)_ |
| Backend API | **Java 17 + Spring Boot 3 (Spring Web, Spring Security, Spring Data JPA)** |
| Database | PostgreSQL + **Hibernate (JPA)**, schema managed by **Flyway** (versioned SQL) |
| Cache / Rate limiting / Jobs | Redis (Spring Data Redis) + scheduled usage-reset job |
| Auth | JWT (short-lived access + refresh tokens), BCrypt password hashing |
| Payments | Stripe (Test Mode checkout + webhooks) |
| Infra / CI | Docker + Docker Compose, GitHub Actions, OpenAPI/Swagger (springdoc) |
| Build | **Maven** |

---

## Monorepo layout

```
flowsuite/
├── apps/
│   ├── api/                              # Spring Boot + Java backend
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/flowsuite/api/
│   │       │   ├── FlowSuiteApplication.java
│   │       │   ├── config/               # Jpa auditing, Redis, OpenAPI, CORS
│   │       │   ├── common/               # Error model, ApiException, handlers
│   │       │   ├── domain/
│   │       │   │   ├── entity/           # JPA entities (one file per table)
│   │       │   │   └── enums/            # Role, SubscriptionStatus, TaskStatus...
│   │       │   ├── repository/           # Spring Data JPA repositories   [Phase 2]
│   │       │   ├── security/             # JWT filter, tenant resolution   [next]
│   │       │   └── module/               # Feature modules (auth, org, ...) [Phase 2]
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/
│   │               ├── V1__init_schema.sql   # All tables, enums, indexes, FKs
│   │               └── R__seed_plans.sql     # Subscription plans (config data)
│   └── web/                              # React (Vite) + TS SPA
│       ├── Dockerfile  nginx.conf
│       └── src/
│           ├── lib/                      # api client, auth context, token store
│           ├── components/               # Layout, nav
│           └── pages/                    # auth, dashboard, projects, tasks, ...
├── packages/shared/                      # Shared TS types / enums (placeholder)
├── docs/ARCHITECTURE.md                  # 1–2 page Architecture Note
├── .github/workflows/ci.yml              # lint / test / build
├── docker-compose.yml                    # Postgres + Redis + API + Web, one command
├── .env.example
└── README.md
```

---

## Running the stack

```bash
cp .env.example .env
docker compose up --build
```

This starts PostgreSQL, Redis, the API, and the web app. On startup **Flyway** applies the
schema migration and the repeatable plan-seed migration automatically — no manual seed step.

- Web UI: http://localhost:5173
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Local dev without a full rebuild:

```bash
docker compose up -d postgres redis
cd apps/api && mvn spring-boot:run          # backend on :8080
cd apps/web && npm install && npm run dev    # frontend on :5173 (proxies /api)
```

## Tests

```bash
cd apps/api && mvn verify   # unit tests + cross-tenant integration test (needs Docker)
```

The mandatory cross-tenant isolation test (`CrossTenantIsolationIT`) uses Testcontainers to
run the full stack against real Postgres + Redis.

---

## Why Flyway instead of `schema.prisma`

Prisma is Node-only. In a Spring Boot stack the schema is owned by **Flyway SQL migrations**
(`src/main/resources/db/migration/`), and the application maps to those tables with **JPA
entities**. Hibernate `ddl-auto` is set to `validate` so the entities and the Flyway-managed
schema are checked for drift on boot, but Hibernate never mutates the schema itself. Enum
columns are stored as `VARCHAR` + `CHECK` constraints (mapped with `@Enumerated(STRING)`),
which is the JPA-friendly equivalent of native DB enums.

---

## Multi-tenancy model (critical)

Every user belongs to exactly **one** organization. Every tenant-owned table carries an
`organization_id` FK, and **every** query is filtered by the authenticated user's resolved
`organizationId`. Authorization runs the 4-step check on every request:

1. Who is the authenticated user?
2. Which organization does the requested resource belong to?
3. Is the user a member of that organization?
4. Does the user's role permit this specific action?

Roles: `OWNER` > `ADMIN` > `MANAGER` > `MEMBER`.

---

## Roadmap

- [x] **Phase 1** — Structure, Docker, DB schema (JPA + Flyway) + plan seed, Auth + tenant foundation
- [x] **Phase 2** — RBAC + CRUD APIs & React UI (orgs, members, projects, tasks, customers)
- [x] **Phase 3** — Entitlements, Redis usage/rate limits, Stripe checkout/webhooks, dashboard, audit logs
- [x] **Phase 4** — Unit + cross-tenant integration tests, Swagger, CI, Architecture Note
