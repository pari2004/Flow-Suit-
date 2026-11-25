-- ============================================================================
-- FlowSuite — subscription plan seed (repeatable migration)
--
-- Plans are configuration data, never hardcoded in application logic (FR-40). This
-- repeatable migration re-runs automatically whenever its checksum changes, so editing
-- a limit/price/entitlement here and restarting is all it takes to update the catalog.
--
-- Prices are monthly, in whole Indian Rupees. project_limit = NULL means unlimited.
-- Entitlement flags are read by name (e.g. hasEntitlement('ADVANCED_ANALYTICS')) — never
-- by branching on the plan name.
-- ============================================================================

INSERT INTO plans (code, name, price_inr, seat_limit, project_limit, api_request_limit, entitlements, stripe_price_id, is_active)
VALUES
    ('FREE',         'Free',         0,   3,  2,    1000,   '{"ADVANCED_ANALYTICS": false}'::jsonb, NULL, TRUE),
    ('STARTER',      'Starter',      499, 10, 20,   10000,  '{"ADVANCED_ANALYTICS": false}'::jsonb, NULL, TRUE),
    ('PROFESSIONAL', 'Professional', 999, 50, NULL, 100000, '{"ADVANCED_ANALYTICS": true}'::jsonb,  NULL, TRUE)
ON CONFLICT (code) DO UPDATE SET
    name              = EXCLUDED.name,
    price_inr         = EXCLUDED.price_inr,
    seat_limit        = EXCLUDED.seat_limit,
    project_limit     = EXCLUDED.project_limit,
    api_request_limit = EXCLUDED.api_request_limit,
    entitlements      = EXCLUDED.entitlements,
    is_active         = EXCLUDED.is_active,
    updated_at        = now();
    -- stripe_price_id is intentionally NOT overwritten here: it is populated per
    -- environment in Phase 3 and must survive re-seeds.
