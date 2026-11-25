-- ============================================================================
-- FlowSuite — initial schema (V1)
--
-- This migration is the authoritative DDL. JPA entities map onto these tables and
-- Hibernate runs in `validate` mode, so the two must stay in lockstep.
--
-- Conventions:
--   * UUID primary keys, defaulted with gen_random_uuid() for SQL-level inserts.
--   * Enums are VARCHAR + CHECK constraints (JPA-friendly native-enum equivalent).
--   * Timestamps are timestamptz; the app writes UTC.
--   * Every tenant-owned table carries organization_id (FK -> organizations) and is
--     indexed on it; cross-tenant isolation is enforced in the app by always filtering
--     on the authenticated user's resolved organization_id.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- provides gen_random_uuid() on older PG

-- ---------------------------------------------------------------------------
-- organizations (tenant root)
-- ---------------------------------------------------------------------------
CREATE TABLE organizations (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

-- ---------------------------------------------------------------------------
-- users (global identity)
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    email          VARCHAR(320) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------------------
-- plans (configuration/reference data — NOT tenant-scoped)
-- ---------------------------------------------------------------------------
CREATE TABLE plans (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    code              VARCHAR(32) NOT NULL,
    name              VARCHAR(100) NOT NULL,
    price_inr         INTEGER     NOT NULL,
    seat_limit        INTEGER     NOT NULL,
    project_limit     INTEGER,                 -- NULL = unlimited
    api_request_limit INTEGER     NOT NULL,
    entitlements      JSONB       NOT NULL DEFAULT '{}'::jsonb,
    stripe_price_id   VARCHAR(255),
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_plans PRIMARY KEY (id),
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT ck_plans_code CHECK (code IN ('FREE', 'STARTER', 'PROFESSIONAL'))
);

-- ---------------------------------------------------------------------------
-- memberships (user <-> org, carries role). One org per user.
-- ---------------------------------------------------------------------------
CREATE TABLE memberships (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_memberships PRIMARY KEY (id),
    CONSTRAINT fk_memberships_org  FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id)         REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT uq_memberships_user UNIQUE (user_id),                      -- one org per user
    CONSTRAINT uq_memberships_org_user UNIQUE (organization_id, user_id),
    CONSTRAINT ck_memberships_role   CHECK (role   IN ('OWNER', 'ADMIN', 'MANAGER', 'MEMBER')),
    CONSTRAINT ck_memberships_status CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED', 'REMOVED'))
);
CREATE INDEX idx_memberships_org ON memberships (organization_id);

-- ---------------------------------------------------------------------------
-- subscriptions (one per org)
-- ---------------------------------------------------------------------------
CREATE TABLE subscriptions (
    id                     UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id        UUID        NOT NULL,
    plan_id                UUID        NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    stripe_customer_id     VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    current_period_start   TIMESTAMPTZ,
    current_period_end     TIMESTAMPTZ,
    trial_ends_at          TIMESTAMPTZ,
    grace_period_ends_at   TIMESTAMPTZ,
    cancel_at_period_end   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_org  FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id)         REFERENCES plans (id)         ON DELETE RESTRICT,
    CONSTRAINT uq_subscriptions_org UNIQUE (organization_id),
    CONSTRAINT ck_subscriptions_status CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_subscriptions_plan ON subscriptions (plan_id);

-- ---------------------------------------------------------------------------
-- projects
-- ---------------------------------------------------------------------------
CREATE TABLE projects (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_id   UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_org     FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_projects_creator FOREIGN KEY (created_by_id)   REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX idx_projects_org ON projects (organization_id);

-- ---------------------------------------------------------------------------
-- customers (optionally linked to a project in the same org)
-- ---------------------------------------------------------------------------
CREATE TABLE customers (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(320),
    phone           VARCHAR(50),
    company         VARCHAR(255),
    project_id      UUID,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_id   UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT fk_customers_org     FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_customers_project FOREIGN KEY (project_id)      REFERENCES projects (id)      ON DELETE SET NULL,
    CONSTRAINT fk_customers_creator FOREIGN KEY (created_by_id)   REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX idx_customers_org     ON customers (organization_id);
CREATE INDEX idx_customers_project ON customers (project_id);

-- ---------------------------------------------------------------------------
-- tasks
-- ---------------------------------------------------------------------------
CREATE TABLE tasks (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    project_id      UUID        NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'TODO',
    assignee_id     UUID,
    created_by_id   UUID,
    due_date        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_org      FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_project  FOREIGN KEY (project_id)      REFERENCES projects (id)      ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id)     REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT fk_tasks_creator  FOREIGN KEY (created_by_id)   REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);
CREATE INDEX idx_tasks_org      ON tasks (organization_id);
CREATE INDEX idx_tasks_project  ON tasks (project_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);

-- ---------------------------------------------------------------------------
-- invitations
-- ---------------------------------------------------------------------------
CREATE TABLE invitations (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    email           VARCHAR(320) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    token_hash      VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by_id   UUID,
    expires_at      TIMESTAMPTZ NOT NULL,
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_invitations PRIMARY KEY (id),
    CONSTRAINT fk_invitations_org     FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_inviter FOREIGN KEY (invited_by_id)   REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT uq_invitations_token UNIQUE (token_hash),
    CONSTRAINT ck_invitations_role   CHECK (role   IN ('OWNER', 'ADMIN', 'MANAGER', 'MEMBER')),
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);
CREATE INDEX idx_invitations_org       ON invitations (organization_id);
CREATE INDEX idx_invitations_org_email ON invitations (organization_id, email);

-- ---------------------------------------------------------------------------
-- audit_logs (append-only; no updated_at)
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    actor_id        UUID,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(100),
    target_id       VARCHAR(255),
    metadata        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_org   FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id)        REFERENCES users (id)         ON DELETE SET NULL
);
CREATE INDEX idx_audit_logs_org_created ON audit_logs (organization_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------------
-- password_reset_tokens
-- ---------------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

-- ---------------------------------------------------------------------------
-- api_usage (per-org, per-billing-period request counter)
-- ---------------------------------------------------------------------------
CREATE TABLE api_usage (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    organization_id UUID        NOT NULL,
    period_start    DATE        NOT NULL,
    request_count   BIGINT      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_api_usage PRIMARY KEY (id),
    CONSTRAINT fk_api_usage_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT uq_api_usage_org_period UNIQUE (organization_id, period_start)
);
CREATE INDEX idx_api_usage_org ON api_usage (organization_id);
