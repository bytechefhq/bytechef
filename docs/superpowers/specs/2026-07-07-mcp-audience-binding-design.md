# MCP Token Audience Binding (RFC 8707) — Design

**Date:** 2026-07-07
**Status:** Design — awaiting review
**Scope:** Hardening the MCP OAuth2 resource servers (automation, management, embedded) so an access token is bound to the *specific* MCP endpoint it was issued for, closing cross-endpoint / cross-tenant token replay. Picks up the "strict per-endpoint audience (RFC 8707) validation" item deferred from the Phase 2 / embedded specs.

## Problem

Today a token is bound to an **issuer** (signature + `iss`) and, for embedded-AS-minted tokens, a `tenant_id` claim — but **not to a specific endpoint**. `.validateAudienceClaim(false)` is set on the automation/management resource server, and nothing sets or checks `aud` anywhere. Consequently, if two endpoints trust the same issuer, a token minted for one is accepted at the other. For embedded, a token from tenant A's IdP could be replayed against tenant B's URL when both tenants trust the same IdP issuer.

## Decision: per-issuer opt-in (Option B)

Our own embedded authorization server we control end to end, so its tokens are always audience-bound. External IdPs may not honor RFC 8707 resource indicators, so enforcing audience for them unconditionally would break external-direct discovery — therefore external-issuer audience validation is **opt-in per issuer**.

## Mint side (embedded authorization server)

Generalize the existing resource extraction in `jwtTokenCustomizer` ([Oauth2AuthorizationServerConfiguration.java](../../../server/ee/libs/platform/platform-oauth2-authorization-server/src/main/java/com/bytechef/ee/platform/oauth2/authorizationserver/config/Oauth2AuthorizationServerConfiguration.java)): set the access token's `aud` claim to the RFC 8707 `resource` parameter value(s) on the token request. This works for every endpoint shape (`/api/automation/{secret}/mcp`, `/api/management/{secret}/mcp`, `/api/embedded/{secret}/mcp`) — the `aud` becomes the exact endpoint URL the client asked for. No change to the existing `tenant_id` / `authorities` claims.

If the token request carries no `resource`, no `aud` is minted (unchanged from today for non-MCP grants).

## Validate side

Audience is inherently **per-request** — the expected `aud` is the URL of the endpoint being called — so the check lives in the tenant-aware filter / embedded converter, not in a static `JwtDecoder` validator (the library's static `validateAudienceClaim` against a fixed `/api` path is too coarse for per-endpoint binding).

### Automation / management resource server

In `TenantAwareJwtAuthenticationFilter`, after signature validation and before establishing identity:

- **Our embedded-AS tokens** (issuer entry marked `self: true`): require `aud` to contain the current request's full endpoint URL. **Always enforced.**
- **External-issuer tokens**: if that issuer's config has `audience` set, require `aud` to contain that fixed value (matching how enterprise IdPs emit a configured audience identifier); if `audience` is unset, **skip** (preserves compatibility with IdPs lacking resource-indicator support).

New config on `McpResourceServerProperties.Issuer` (and its `ApplicationProperties` mirror):
- `self: boolean` (default `false`) — marks the ByteChef embedded AS issuer; triggers per-request endpoint-URL audience enforcement.
- `audience: String` (optional) — for an external issuer, the fixed audience value its tokens must carry; when set, enforced; when unset, skipped.

A mismatch → `401` (same as an untrusted issuer / missing scope today).

### Embedded resource server

- **Our embedded-AS tokens** (issuer == the embedded AS issuer): require `aud` to contain the current `/api/embedded/{secret}/mcp` URL. Enforced once the embedded endpoint trusts the AS issuer (SP-B). Since the embedded MCP tenant already comes from the URL secret, this adds per-endpoint binding on top of that.
- **External-direct IdP tokens**: **not enforced in this iteration** (documented follow-up below). Embedded already binds the tenant via the URL secret, so the residual risk is limited to two tenants sharing one IdP issuer; adding a per-IdP audience opt-in requires an `IdentityProvider` schema column and is deferred.

## Explicitly deferred (honest scope)

- Per-IdP `audience` opt-in for embedded **external-direct** tokens (needs an `IdentityProvider` column + UI). Until then, embedded external-direct relies on the URL-secret tenant binding, not `aud`.
- Consuming the `aud` on external IdP tokens that emit our endpoint URL directly (rather than a fixed string) — those tenants would configure `audience` to the endpoint URL, which is per-tenant; the fixed-string form covers the common case.

## Testing

- **Mint:** AS int test — token requested with `resource=<endpoint URL>` carries `aud=<endpoint URL>`; no `resource` → no `aud`.
- **Automation/management validate (unit):** filter rejects a `self`-issuer token whose `aud` lacks the request URL; accepts when it matches; for an external issuer, rejects when `audience` configured and absent from `aud`, skips when `audience` unset.
- **Automation/management validate (int):** existing resource-server int tests extended — a token minted for endpoint X is rejected at endpoint Y.
- **Embedded validate:** converter/int test — an embedded-AS token whose `aud` is another tenant's URL is rejected.
