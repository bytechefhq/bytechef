# External-IdP Tenant Anchoring for MCP Endpoints — Design

**Date:** 2026-07-08
**Status:** Implemented (2026-07-08)
**Scope:** How the MCP endpoints (automation, management, embedded) should establish the ByteChef **tenant** when the presented credential is an **external IdP** OAuth2 token — i.e. a token ByteChef did not mint. Records a non-obvious finding: the automation/management "tenant from claim" model and the embedded "tenant from URL" model are two solutions to the *same* problem, each missing a different guard.

## Problem

Every MCP request must run in exactly one ByteChef tenant. Where that tenant comes from depends on the credential:

| Path | Tenant anchor | Trust basis |
|---|---|---|
| API key / signing-key (all three surfaces) | `TenantKey.parse(secret)` | the secret **is** a ByteChef-issued, unguessable, tenant-stamped bearer key |
| Automation / management **OAuth2** | the token's `tenantClaim` (`McpJwtIdentityMapper`) | whatever the issuer stamps |
| Embedded **external-direct OAuth2** | the URL path secret (`TenantKey.parse`) | ByteChef-issued URL secret |

The API-key rows are safe by construction — the credential encodes the tenant. The two OAuth2 rows are where an **external IdP** (a customer's Okta/Entra/etc., which ByteChef does not control) meets a multi-tenant endpoint, and each has an unclosed gap:

- **Automation/management (claim anchor):** `McpJwtIdentityMapper` reads the tenant from a configured claim and **uses it directly** — there is no check that *this issuer is allowed to assert that tenant*. A shared external IdP (one IdP serving several ByteChef tenants), or a user who can influence the claim, can assert a tenant they should not reach. Audience binding does **not** help — it binds the endpoint, not the tenant identity. → **Missing: per-issuer tenant scoping.**
- **Embedded external-direct (URL anchor):** the tenant comes from the URL `TenantKey`, but the token proves only "a trusted issuer signed this," not "for this endpoint." If two tenants trust the same issuer, an A-issued token is signature-valid at B's URL and adopts tenant B. → **Missing: audience binding** (RFC 8707; per-issuer opt-in, deferred in the 2026-07-07 audience spec).

Both reduce to one problem: **an issuer-trust credential meeting a multi-tenant endpoint.** They differ only in anchor, and each anchor needs its own guard.

## Core insight

An **external IdP token is an identity assertion, not a tenancy assertion.** The IdP knows *who the user is*; it does not (reliably) know *which ByteChef tenant* they act in. Therefore tenancy must be anchored to something **ByteChef controls**, not to a claim an external party fills in.

ByteChef already controls such a thing on every surface: the **URL path secret is a ByteChef-minted `TenantKey`** (`McpServerGraphQlController` sets `mcpServer.secretKey = TenantKey.of()`; embedded keys are `TenantKey`s). This is the trustworthy anchor. The IdP token should be treated as identity-only.

## Recommended design: unify on the URL `TenantKey` anchor + audience

Treat all three surfaces the same for external-IdP OAuth2:

1. **Tenant = URL `TenantKey`.** Resolve the tenant by parsing the ByteChef-issued path secret (as the API-key path and embedded already do), **not** from an IdP-controlled claim. The IdP token authenticates the *user* (subject → ByteChef user / connected user) and supplies authorities; it does not assert the tenant.
2. **Audience binding.** Because the URL is now authoritative for tenancy, the token must be bound to *this* endpoint (RFC 8707 `aud` ⊇ endpoint URL), so a token minted for one tenant's endpoint cannot be replayed at another. Per-issuer opt-in (an external IdP that cannot emit `aud` degrades to today's behavior, documented as reduced assurance).
3. **Issuer trust stays a gate, not a tenancy source.** A configured/trusted issuer means "we accept this IdP's *identity* assertions," never "we accept its *tenant* assertions."

This collapses the two half-covered models into one: ByteChef owns tenancy (URL secret), the IdP owns identity, and audience keeps the token pinned to its endpoint.

### Application per surface

**All three** MCP endpoints carry a per-tenant `TenantKey` in the URL path secret, so the URL anchor applies uniformly (an earlier draft wrongly excluded management):

- **Automation:** the MCP server secret is `TenantKey.of()` (`McpServerGraphQlController`).
- **Embedded external-direct:** already uses the URL anchor. Add per-IdP audience (opt-in field on `IdentityProvider` + converter check).
- **Management:** the `mcp.server` secret is `TenantKey.of()` per tenant (`ManagementMcpServerServiceImpl`). It is a `Property.Scope.PLATFORM` property, but ByteChef is **schema-per-tenant** (`BaseDataSource` sets `search_path` per tenant, and the `property` table is unique on `key` *within* a schema), so `PLATFORM` means "tenant-wide," **not** "cross-tenant global" — the secret is per-tenant. So management uses the URL anchor exactly like automation.

Uniform rule for all three, implemented in `TenantAwareJwtAuthenticationFilter` + `McpAudienceValidator`: **tenant from the URL `TenantKey`; a token whose `tenant_id` claim disagrees is rejected; audience is required for every issuer** (the token's `aud` must contain the endpoint URL, or the issuer's configured fixed audience). An external IdP token carries no tenant claim and must emit the endpoint URL as its `aud`.

## The fork (decision needed before a plan)

The recommended design changes automation/management's tenant source for external issuers. If that is too large a shift, the narrower alternative keeps the claim anchor but closes its specific gap:

- **Option A — URL anchor + audience (recommended, unifying).** Automation/management external issuers stop trusting `tenantClaim`; tenant comes from the URL `TenantKey`; audience enforced. One model across all surfaces. Larger change; the `self` (embedded-AS) path keeps its claim (ByteChef mints it) or also moves to URL — TBD.
- **Option B — keep claim anchor, add per-issuer tenant scoping.** Add `Issuer.allowedTenants` (or bind an issuer to a single tenant); `McpJwtIdentityMapper` rejects a token whose asserted tenant is not in the issuer's allowlist (`self` unrestricted). Smaller, config-only, preserves today's flow — but leaves tenancy delegated to a *scoped* external IdP rather than removed from its control.

**Recommendation:** Option A as the north star (it removes tenancy from the external IdP entirely and unifies the surfaces), with Option B acceptable as an interim if a full anchor switch is out of scope. Both still want audience; A depends on it.

## Non-goals / deferred

- Implementation (a plan follows once the fork is chosen).
- Changing the API-key / signing-key paths (already tenant-bound by construction).
- Changing the `self` embedded-AS issuer's behavior beyond what the chosen option requires (ByteChef mints its `tenant_id`, so it is already trustworthy).

## Open questions

1. **Option A vs B** (the fork above).
2. Under Option A, does the `self` issuer also move to the URL anchor, or keep its minted `tenant_id` claim (they agree by construction, so it is a consistency/simplicity call)?
3. Audience for external issuers is opt-in; what is the assurance story for an IdP that cannot emit RFC 8707 `aud` (document as reduced, or refuse external issuers without it)?
