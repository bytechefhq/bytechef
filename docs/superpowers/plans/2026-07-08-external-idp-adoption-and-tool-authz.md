# External-IdP Adoption + Tool-Authz Completion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make external IdPs *easy* to adopt on automation/management and complete tool-level authorization. Two tracks:
- **Track 1 — Per-tenant IdPs for automation/management** (fixes the "external-IdP config burden", "no group→authority mapping", and "per-tenant external IdPs" gaps as one feature). Builds on the deferred spec `docs/superpowers/specs/2026-07-08-per-tenant-automation-management-external-idp-design.md`.
- **Track 2 — Tool-authz Phase 2 (admin UI) + Phase 3 (embedded) + broader gating.** Builds on `docs/superpowers/specs/2026-07-07-mcp-tool-authorization-design.md`.

**Architecture:** MCP OAuth2 is EE. Reuse the embedded per-tenant IdP model (`IdentityProvider` + `EmbeddedMcpTrustedIssuerResolver`) already built. Tenant is URL-anchored (done), so a per-tenant issuer resolver can scope to the request's tenant.

## Key insight (why this reduces burden, not relocates it)

Today an external IdP token must carry three ByteChef-specific things (`aud` = endpoint URL, `mcp:automation`/`mcp:management` scope, `authoritiesClaim`). **Per-tenant issuer trust removes two of them and a group map removes the third:**
- **`aud` becomes optional.** When an issuer is trusted *only for one tenant* (a per-tenant IdP record), a token from it can only validate at that tenant's endpoint — B doesn't trust A's issuer — so the shared-issuer replay that made audience mandatory can't happen. Audience stays as optional defense-in-depth (`validateMcpAudience`), not a requirement.
- **`scope` becomes optional.** The IdP record declares which surfaces it applies to (`automation`/`management` flags), so the token needn't carry `mcp:automation`/`mcp:management`.
- **Group→authority mapping** on the IdP record translates IdP group names → ByteChef authorities, so the IdP needn't emit exact ByteChef authority strings.

Net: a per-tenant automation/management IdP needs only a valid signature + a subject + its group claim. That's ordinary OIDC — no ByteChef-specific token shaping.

## Global Constraints

- EE conventions (`com.bytechef.ee.*`, EE header, `@version ee`, `@ConditionalOnEEVersion`) for MCP-auth code. `IdentityProvider` fields follow the existing CE pattern (`platform-user-api` + `platform-user-graphql` + client).
- **No behavior change for the `self` AS** — it stays a platform-wide issuer (static config) whose tokens map to ByteChef users (audience + revocation as today).
- **Backward compat** — static `issuers[]` external entries keep working during migration (hybrid); retire them only after per-tenant records cover the same trust.
- One concern per task; `test` + `testIntegration` + `spotlessApply` per task; never amend (parallel commits).

## Open questions — RESOLVED (recommendations; adjust at review)

1. **Retire vs hybrid `issuers[]`.** → **Hybrid.** Keep `issuers[]` for the `self` AS and as a fallback for external issuers; add per-tenant IdP records as the preferred external path. Full retirement is a later cleanup once no deployment relies on static external issuers.
2. **One IdP record per surface, or flags.** → **Flags.** Reuse the single `IdentityProvider` record with `mcp` (embedded, exists), plus new `mcpAutomation` / `mcpManagement` booleans. One record can serve several surfaces.
3. **Audience under per-tenant trust.** → **Optional** (reuse `validateMcpAudience`); required only for the `self` AS and any platform-wide (`issuers[]`) external issuer. The audience validator branches on issuer origin.
4. **Group→authority mapping shape.** → a per-IdP `Map<String,String>` (external group → ByteChef authority) persisted as a child relation `identity_provider_authority_mapping`, plus the existing direct `authoritiesClaim` passthrough when no mapping is configured.

---

## Track 1 — Per-tenant IdPs for automation/management

### Task 1.1: IdP model — surface flags + group→authority mapping
**Files:** `IdentityProvider` (+ `IdentityProviderAuthorityMapping` child), Liquibase migration, `platform-user-graphql` controller + schema, client ops + dialog + hook (mirror the `mcp`/`validateMcpAudience` flags).
- [ ] Add `mcp_automation` / `mcp_management` BOOLEAN columns + `identity_provider_authority_mapping(identity_provider_id, external_group, authority)` child table.
- [ ] Domain accessors + `getAuthorityMappings()` (Set/Map); service round-trip int test.
- [ ] GraphQL + client: two checkboxes + a group→authority mapping editor in the IdP dialog (OIDC-only). `npm run check`.

### Task 1.2: Shared per-tenant issuer resolver
**Files:** generalize `EmbeddedMcpTrustedIssuerResolver` into a shared `McpTenantIssuerResolver` (platform EE) that, for the current tenant, returns enabled IdPs applicable to a given surface (`mcp`/`mcpAutomation`/`mcpManagement`), each with its issuer URI, audience-required flag, authorities claim, and group→authority map.
- [ ] Extract + generalize; embedded delegates to it (surface = embedded). Unit tests.

### Task 1.3: Automation/management resource server consults per-tenant IdPs
**Files:** `McpOAuth2ResourceServerSecurityConfigurerContributor`, `MultiIssuerJwtDecoder` wiring, `TenantAwareJwtAuthenticationFilter`, `McpJwtIdentityMapper`, `McpAudienceValidator`.
- [ ] The trusted-issuer set for a request = static `issuers[]` ∪ the request tenant's applicable IdP issuers (tenant known from the URL anchor).
- [ ] Identity mapping for a per-tenant IdP token: tenant from URL (unchanged); authorities from the IdP's group→authority map (falling back to `authoritiesClaim`); **no scope required** (surface authorized by the IdP flag) ; **audience optional** unless the IdP sets `validateMcpAudience`.
- [ ] Keep `self` + static external `issuers[]` on the strict path (scope + mandatory audience + user revocation).
- [ ] Unit + IntTest: a per-tenant automation IdP token with only signature + subject + group claim authenticates; the same issuer is rejected on another tenant's endpoint.

### Task 1.4: Verify + docs
- [ ] Whole-repo compile; edition reasoning; update the spec status to Implemented.

---

## Track 2 — Tool-authz Phase 2 (UI) + Phase 3 (embedded)

### Task 2.1: Admin UI for tool authorization (Phase 2)
**Files:** `platform-mcp-graphql` (or the MCP server settings GraphQL) + client MCP-server settings.
- [ ] GraphQL: expose/set `McpServer.enforceToolAuthorization` + per-`McpComponent` `requiredAuthorities`.
- [ ] Client: a toggle on the MCP server + a per-component authorities editor. `npm run check`.

### Task 2.2: Embedded tool authorization (Phase 3)
**Files:** `EmbeddedMcpServerConfiguration` toolFilter + contextExtractor.
- [ ] Capture the connected-user / IdP-claim authorities in the embedded `contextExtractor`; enforce per-component authorization in the embedded `toolFilter` using `McpToolAuthorizationEvaluator` (mirror the automation server). Int test.

### Task 2.3: Broader gating (optional, same pass or follow-up)
- [ ] Decide gating for automation **project/workspace** tools and management **built-in** tools (currently ungated). If in scope: attach required authorities to those tool sources; else document why they stay ungated (project = project RBAC; management = `mcp:management` scope).

### Task 2.4: Verify
- [ ] Whole-repo compile; int tests; update the tool-authz spec status.

## Sequencing

Track 1 and Track 2 are independent. Recommend **Track 1 first** (it unblocks real external-IdP adoption, the higher-friction gap), then Track 2. Each task is independently testable and committable.

## Self-review checklist
- `self` AS + static external issuers keep the strict path (scope + mandatory audience + revocation).
- Per-tenant IdP tokens need no ByteChef-specific token shaping (no `aud`/scope required; groups mapped server-side).
- A per-tenant issuer is rejected on other tenants' endpoints (per-tenant trust).
- Tool-authz enforcement + component authorities are settable from the UI on all applicable servers.
