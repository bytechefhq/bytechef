# Per-Tenant External IdPs for Automation/Management MCP — Design

**Date:** 2026-07-08
**Status:** Implemented — server functional core (2026-07-08). See "Implementation status" below for what shipped and what is deferred.
**Trigger:** A multi-tenant deployment that wants each tenant to bring its **own** external IdP for the automation/management MCP endpoints (self-service, at runtime) — the way embedded tenants already do.

## Implementation status (2026-07-08)

The **server functional core** shipped on `claude/competent-euler-fb5f4b` (plan `docs/superpowers/plans/2026-07-08-external-idp-adoption-and-tool-authz.md`, Track 1):

- **T1.1** — `IdentityProvider` gained `mcpAutomation` / `mcpManagement` surface flags and an `identity_provider_authority_mapping` child table (external group → ByteChef authority), with a Liquibase migration and a DB round-trip IntTest. Commit `e7656af5afe`.
- **T1.2** — a shared, surface-aware `McpTenantIssuerResolver` (platform-security-web EE) returns the enabled IdP issuers a tenant trusts for a given surface. Commit `409b1bddfd0`.
- **T1.3** — the automation/management resource server now trusts a tenant's own IdP issuers (union with the static issuers). An early `McpTenantTrustResolutionFilter` resolves the tenant from the URL secret and publishes the tenant's applicable IdP issuers to a request-scoped `McpTenantTrustContext`; the JWT decoder factory consults it to admit a per-tenant token for decoding, and the tenant-aware filter re-makes the authoritative per-tenant trust decision. Per-tenant tokens follow a relaxed path (surface authorized by the IdP flag → no scope; audience optional unless the IdP opts in; authorities from the group→authority map). Static issuers keep the strict path (scope + mandatory audience + user revocation). IntTest proves a signature-only + group-claim token authenticates and the same issuer is rejected on another tenant's endpoint. Commit `0f8f41698d7`.

**Also shipped:** the **client admin UI** — `mcpAutomation`/`mcpManagement` exposed over the IdentityProvider GraphQL type/input (commit `b7708c4`); the IdP dialog gained automation/management surface checkboxes + a group→authority mapping editor (shown when any MCP surface is enabled), the audience-validation option now applies to all surfaces, and the table shows per-surface badges (commit `b7c2b74`).

**Follow-on hardening (shipped 2026-07-08):**

- **Tenant-aware discovery on automation/management** (commit `c9aa708`) — an unauthenticated request now gets a per-endpoint RFC 9728 challenge whose `resource_metadata` resolves to a document advertising the tenant's IdPs for that surface (plus ByteChef's issuers), so a generic MCP client can auto-discover the tenant's IdP, matching embedded. New `McpTenantProtectedResourceMetadata{Resolver,Controller,AuthenticationEntryPoint}` + a dedicated permit-all chain ordered ahead of the static metadata chain.
- **Per-tenant issuer resolution cached** (commit `be69425`) — `McpTenantIssuerResolver` caches per (tenant, surface) with a short 30s TTL, so the trust filter and per-tenant token path no longer query the database each request.
- **Embedded delegation** (commit `c48dfa2`) — `EmbeddedMcpTrustedIssuerResolver` now delegates to the shared `McpTenantIssuerResolver` (surface EMBEDDED). Behavior change: embedded trust now requires the IdP's `mcp` flag, consistent with automation/management (previously any enabled IdP was trusted on embedded).
- **Configurable groups claim** (commit `1b2a3c7`/`ee2d97c`) — `IdentityProvider.authoritiesClaim` (nullable column + migration) lets a provider name the token claim carrying groups (blank = conventional OIDC `groups`); threaded through `McpTenantIssuer`, the mapper, embedded, GraphQL, and the IdP dialog.

## Problem

Automation/management external issuers are configured as **static, platform-wide** `issuers[]` application properties (`McpResourceServerProperties` / `ApplicationProperties.Oauth2.ResourceServer`). This has two limits:

1. **No self-service UX** — a tenant admin can't add its own IdP; an operator must edit config and redeploy.
2. **No per-tenant issuer trust** — *every* tenant trusts the *same* issuers. There's no "issuer X is trusted only for tenant A."

Embedded avoids both: its trusted issuers are per-tenant `IdentityProvider` **DB records** managed through the identity-providers settings UI (`EmbeddedMcpTrustedIssuerResolver` reads the current tenant's enabled providers).

### Is (2) a security must-fix?

No — the per-tenant **URL `TenantKey` secret is the primary isolation** (unguessable bearer credential; you can't reach a tenant's endpoint or mint an `aud` for it without that tenant's secret). Per-tenant issuer trust is **defense-in-depth** on top: it would matter if a tenant's URL secret leaked *and* you wanted the issuer boundary as a second gate. So this feature is a capability + hardening upgrade, not a correctness fix.

## Design: unify onto the embedded per-tenant `IdentityProvider` model

The URL-anchor work (2026-07-08) makes this clean: automation/management now resolve the tenant from the URL **before** issuer trust is evaluated, so the resolver can consult *that tenant's* IdPs.

1. **External issuer trust becomes per-tenant.** For automation/management, resolve trusted external issuers from the current tenant's enabled `IdentityProvider` records (reuse/generalize `EmbeddedMcpTrustedIssuerResolver` into a shared per-tenant issuer resolver), instead of the static `issuers[]` external entries.
2. **Keep static config only for the operator-level `self` AS.** The ByteChef embedded authorization server is one platform-wide issuer (not per-tenant); it stays in `issuers[]` (or a single dedicated property). Retire external issuers from `issuers[]`.
3. **Reuse the existing IdP surface.** `IdentityProvider` already carries `issuerUri`, `mcp` flag, `validateMcpAudience`, `authoritiesClaim` (via `scopes`/claims), and a management UI. Add whatever automation/management need (e.g. an `authoritiesClaim`/`tenantClaim`-equivalent, an `mcp:automation`/`mcp:management` applicability flag) so one IdP record can serve embedded and/or automation/management per tenant.
4. **Authorities mapping per IdP** — the resolver supplies the issuer's `authoritiesClaim` + static authorities from the IdP record instead of the static `Issuer` config, so `McpJwtIdentityMapper` reads them the same way.

Net: one per-tenant IdP model across all three surfaces; `issuers[]` shrinks to the single `self` AS entry. This delivers self-service UX **and** per-tenant issuer trust in one move, and removes the "platform-wide external trust" defense-in-depth gap.

## Interactions / prerequisites

- **URL anchor (done)** — required; the tenant must be known before issuer trust so the resolver can scope to it. Automation/management already do this.
- **Audience (done)** — unchanged; still required per issuer. External IdP records reuse `validateMcpAudience`.
- **`self` handling (done, unified hardening)** — the `self` AS stays a static/global issuer and remains the only issuer whose principal is a ByteChef user (relevant to mid-token revocation).

## Non-goals / open questions (resolve when picked up)

- Whether to keep `issuers[]` for external issuers as a fallback (hybrid) or fully retire it.
- The exact `IdentityProvider` fields automation/management need beyond embedded's (tenant/authorities claim naming).
- Whether a single IdP record applies to multiple MCP surfaces (flags) or one record per surface.
- Migration for any existing static external `issuers[]` config → per-tenant IdP records.

## Recommendation

~~Deferred.~~ **Built** (server functional core, 2026-07-08) — see "Implementation status" above. The DB fields, shared resolver, migration, and resource-server wiring shipped; the admin UI and the two non-functional cleanups remain deferred until a concrete deployment needs them.
