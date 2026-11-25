# FlowSuite — Architecture Note

_A production-style vertical slice of a multi-tenant SaaS workspace platform._

## 1. System overview

FlowSuite is a multi-tenant SaaS backend with a React SPA client. Independent customer
organizations ("tenants") manage teams, customers, projects, tasks, subscriptions, and
billing with strict row-level data isolation.

```
┌────────────┐     HTTPS/JSON      ┌──────────────────────────┐
│ React SPA  │ ──────────────────▶ │  Spring Boot API          │
│ (Vite/TS)  │  Bearer access JWT  │  (stateless, JWT-secured) │
└────────────┘                     └───────────┬──────────────┘
                                   JPA/Hibernate │  Spring Data Redis
                                   ┌─────────────▼──────────┐  ┌─────────────┐
                                   │ PostgreSQL (Flyway DDL)│  │   Redis     │
                                   │  tenant data + audit   │  │ usage/limits│
                                   └────────────────────────┘  └─────────────┘
                                                 ▲ webhooks
                                          ┌──────┴───────┐
                                          │ Stripe (test)│
                                          └──────────────┘
```

**Stack:** Java 17 · Spring Boot 3 (Web, Security, Data JPA) · PostgreSQL + Hibernate with
Flyway-managed schema · Redis · JWT (jjwt) + BCrypt · Stripe (Test Mode) · springdoc OpenAPI
· Docker Compose · GitHub Actions. Frontend: React (Vite) + TypeScript, Tailwind, TanStack
Query, React Hook Form, Recharts.

The backend is organized by **feature module** (`module/auth`, `module/member`,
`module/project`, `module/task`, `module/customer`, `module/billing`, `module/usage`,
`module/audit`) over shared infrastructure (`config`, `common`, `security`, `domain`,
`repository`).

## 2. Multi-tenancy & data isolation (the core invariant)

Every user belongs to **exactly one** organization (enforced by a unique constraint on
`memberships.user_id`). Every tenant-owned table carries an `organization_id` FK and is
indexed on it.

Isolation is enforced in the application, consistently, at two layers:

1. **Tenant resolution** — `JwtAuthenticationFilter` validates the access token, loads the
   user's single **active** membership, and builds an `AuthPrincipal { userId,
   organizationId, role }` placed in the `SecurityContext`. A removed/suspended user is
   rejected immediately even with a valid token.
2. **Query scoping** — service code reads `SecurityUtils.currentOrganizationId()` and every
   query is filtered by it. Single-entity reads use `findByIdAndOrganization_Id(...)`, so a
   request for another tenant's id returns **404** (existence is never leaked).

This gives the 4-step authorization check on every request: _who is the user_ (token) →
_which org owns the resource_ (query filter) → _is the user a member_ (active membership) →
_does the role permit it_ (method security). The mandatory `CrossTenantIsolationIT` proves a
user in Org A cannot read, edit, or delete Org B's projects, tasks, or customers.

## 3. AuthN & AuthZ

- **Authentication:** short-lived access **JWT** (HS256, subject = user id) + opaque
  **refresh token** persisted as a SHA-256 hash and rotated on every refresh. BCrypt for
  passwords. Time-limited, single-use password-reset tokens; failed-login **rate limiting**
  in Redis.
- **Authorization:** roles `OWNER > ADMIN > MANAGER > MEMBER` map to Spring `ROLE_*`
  authorities. Coarse gating uses `@PreAuthorize` on controllers (e.g. project mutations are
  `OWNER/ADMIN/MANAGER`; role changes and org deletion are `OWNER`). Finer, data-dependent
  rules live in services (e.g. a **Member** may change the status only of a task assigned to
  them, and sees only projects/tasks they are assigned).

## 4. Data model

Core entities: `Organization`, `User`, `Membership` (role carrier), `Plan`, `Subscription`,
`Project`, `Task`, `Customer`, `Invitation`, `AuditLog`, plus auth tables (`RefreshToken`,
`PasswordResetToken`) and `ApiUsage`. The schema is authored as **Flyway SQL**
(`V1__init_schema.sql`); Hibernate runs `ddl-auto: validate` so entities and DDL cannot
drift. Enums are stored as `VARCHAR + CHECK` (mapped `@Enumerated(STRING)`). FK delete
behavior is deliberate: `CASCADE` for org-scoped children, `SET NULL` for `created_by` /
`assignee`, `RESTRICT` for `subscriptions.plan_id`.

## 5. Plans, entitlements & billing

Plans are **configuration data** seeded via a repeatable Flyway migration
(`R__seed_plans.sql`), never hardcoded. `EntitlementService` resolves an org's **effective
plan** (its subscribed plan while ACTIVE/TRIALING, or PAST_DUE within grace; otherwise a
Free fallback) and answers by **named entitlement** (`hasEntitlement("ADVANCED_ANALYTICS")`)
or asserts numeric limits (`assertCanAddMember`, `assertCanAddProject`) with actionable,
upgrade-oriented error messages. Plan changes flow through **Stripe Test Mode** checkout and
a signature-verified **webhook** that updates limits immediately; a clearly-labelled
test-mode `simulate-upgrade` path makes limit changes demonstrable without live price ids.

## 6. Usage metering & rate limiting

An MVC interceptor increments a **Redis** per-organization, per-month counter on each
authenticated request and rejects requests over the plan's API quota (HTTP 429). Counters
are month-keyed, so a new month starts at zero; a **scheduled monthly job** persists the
closing count to `api_usage` for the dashboard. The usage dashboard reports seats, projects,
and API requests used vs. limits.

## 7. Cross-cutting concerns

- **Errors:** every failure is rendered as `{ code, message, status }` by a single
  `@RestControllerAdvice`, including bean-validation (with field errors), security
  denials, and unexpected errors (logged server-side, never leaked).
- **Audit:** sensitive actions (membership/role changes, org/subscription changes) are
  written to a tenant-scoped, append-only `AuditLog`, exposed via a paginated, filterable
  endpoint.
- **Pagination:** all list endpoints return a consistent `PageResponse` envelope.
- **API docs:** springdoc serves OpenAPI at `/v3/api-docs` and Swagger UI at
  `/swagger-ui.html`, with a global bearer-JWT scheme.

## 8. Testing & CI

- **Unit tests** (Mockito) cover auth, RBAC/self-scope, plan-limit enforcement, and JWT.
- **Integration test** (`CrossTenantIsolationIT`) runs the full stack on **Testcontainers**
  (real Postgres + Redis) through the HTTP layer to prove tenant isolation.
- **JaCoCo** measures statement coverage (target ≥ 70% of business logic).
- **GitHub Actions** runs `mvn verify` for the API and lint + build for the web app.

## 9. Deployment

`docker compose up --build` starts Postgres, Redis, and the API as one stack. Flyway applies
the schema and plan seed on startup; configuration and secrets come from environment
variables. The API is stateless and horizontally scalable; Redis holds ephemeral
usage/rate-limit state and PostgreSQL is the system of record.

## 10. Trade-offs & next steps

- Role/organization are resolved from the DB on each request (one indexed lookup) rather
  than embedded in the token, so role/membership changes take effect immediately.
- Members are **soft-removed** (status `REMOVED`) to preserve audit history and free a seat
  without deleting the user row.
- Outbound email (password-reset + invitation links) is sent via `EmailService` over SMTP
  when `spring.mail.*` is configured; with no SMTP host it degrades gracefully to logging the
  action link, so local/dev flows stay testable. Delivery is best-effort — a mail outage is
  logged and never breaks the request.
- Future hardening: Postgres Row-Level Security as defense-in-depth, refresh-token reuse
  detection, HTML (multipart) email templates, and richer Stripe lifecycle handling
  (proration, `invoice.payment_failed` → `PAST_DUE` with grace).
