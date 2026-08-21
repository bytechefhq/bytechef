# Relocate base MCP OAuth2 to CE (external-IdP federation stays EE) — Design

**Date:** 2026-07-08
**Status:** Implemented (2026-07-08). Base resource server + MCP API-key auth relocated to CE `platform-security-web-impl`; embedded authorization server relocated to CE `platform-oauth2-authorization-server`; external-IdP federation stays EE behind the `McpFederatedIssuerAuthenticator` SPI. CE-only build proven by `McpOAuth2ResourceServerCeOnlyIntTest`. Scope note: per the implementer's decision, MCP **API-key** auth was also moved to CE in this pass (the spec's "already-CE API-key" framing was inaccurate — it was in EE), fully realizing the "CE = API-key + ByteChef-AS OAuth2" line.
**Reverses:** parts of #6-era "Relocate ... to EE" — specifically the *base* OAuth2 pieces. Consistent with #6, which already moved MCP **API-key** auth to CE.

## Decision (locked)

Split the MCP OAuth2 stack along the free/paid line:

- **CE** — the embedded **authorization server** (ByteChef's self AS mints tokens: metadata, DCR, auth-code/PKCE) **and** the **resource server** that validates **self-issuer** JWTs (signature/iss/expiry, the `mcp:automation`/`mcp:management` scope, RFC 8707 audience binding, per-request user revocation). CE users get ByteChef-issued OAuth2 for MCP, alongside the already-CE API-key auth.
- **EE** — **external-IdP federation**: bring-your-own-Okta/Entra. The per-tenant `IdentityProvider` surface flags, `McpTenantIssuerResolver`, per-tenant trust, group→authority mapping, and tenant-aware discovery stay `com.bytechef.ee.*`. This is the enterprise capability, and it is the T1.x work already built there.

The free/paid line becomes coherent: **CE = API-key + ByteChef-AS OAuth2; EE = bring-your-own-IdP federation + tool-authz.**

## Why this is feasible

No EE-only dependency. The OAuth2 stack is Spring Security OAuth2 + `org.springaicommunity:mcp-server-security` + platform services; the multi-tenant services it relies on (`tenant-api`, schema-per-tenant) are already CE. The mechanical relocation is the same as #6's API-key move (package `com.bytechef.ee.*` → `com.bytechef.*`, Apache header, drop `@ConditionalOnEEVersion`), just larger.

## The hard part: base and federation are woven together

The T1.3 work deliberately wove per-tenant federation **into** several base RS classes. A clean CE/EE split therefore needs **extension points (SPIs)** so the base RS stands alone in CE and EE plugs federation in. Three woven seams:

### 1. `IssuerLocationMcpJwtDecoderFactory` — trust gate
Base: trust an issuer iff it is statically configured. Federation: also trust it if the request tenant's `McpTenantTrustContext` lists it.
**Split:** move `McpTenantTrustContext` (a neutral request-scoped thread-local, no federation logic) to **CE**. The CE decoder factory reads it — empty when EE is absent, so no behavior change for CE-only. The EE `McpTenantTrustResolutionFilter` (which *populates* it) stays EE.

### 2. `TenantAwareJwtAuthenticationFilter` — post-decode policy
Base path (static issuer): scope + mandatory audience + user revocation + `map(...)`. Federation path (per-tenant issuer): no scope, optional audience, `mapTenantIssuer(...)`.
**Split:** the CE filter keeps the static path. Introduce a CE SPI:
```java
interface McpFederatedIssuerAuthenticator {   // CE, optional
    // Returns the identity for a non-statically-configured (per-tenant IdP) issuer, or empty to reject.
    Optional<McpJwtIdentity> authenticate(Jwt jwt, HttpServletRequest request, String urlTenantId);
}
```
The CE filter, when the issuer is **not** in static config, delegates to the SPI if present, else 401. EE provides the impl (the current per-tenant branch: `McpTenantTrustContext` lookup + `mapTenantIssuer`). CE-only builds have no bean → non-static issuers rejected, exactly the pre-T1.3 behavior.

### 3. Discovery entry point + metadata
Base: challenge points at the static `/.well-known/oauth-protected-resource/api` metadata (self AS + static issuers). Federation: challenge points at the per-endpoint, tenant-aware metadata advertising the tenant's IdPs.
**Split:** CE ships the base discovery filter + the static metadata chain. EE **overrides** the entry point with the tenant-aware one (`McpTenantProtectedResourceMetadataAuthenticationEntryPoint`) and adds the tenant metadata controller/chain (already EE-shaped). Wire via an optional bean the CE contributor picks up if present.

## File split map

**Move to CE** (`com.bytechef.*`, Apache header, drop `@ConditionalOnEEVersion`):
- Embedded AS module `platform-oauth2-authorization-server` → CE counterpart: the AS config, consent SPA controller, DCR/registered-client pieces, `jwtTokenCustomizer` (self `tenant_id`/scope minting). *External-IdP federation entry points on the AS stay EE.*
- Base RS classes in `platform-security-web-impl` → CE `platform-security-web-impl`: `MultiIssuerJwtDecoder`, `IssuerLocationMcpJwtDecoderFactory`, `McpJwtDecoderFactory`, `McpBearerTokenResolver`, `McpResourceServerProperties`, `McpAudienceValidator` (self/static methods), `McpJwtIdentity`, `McpJwtIdentityMapper.map(...)` (static path), `TenantAwareJwtAuthenticationFilter` (static path + SPI hook), `McpOAuth2ResourceServerSecurityConfigurerContributor`, `McpJwtTenantSecurityConfigurer`(base filter wiring), `McpDiscoveryAuthenticationFilter`, `McpDiscoverySecurityConfigurer(Contributor)` (base entry point), `McpProtectedResourceMetadataSecurityConfiguration`, `McpTenantTrustContext`.
- New CE SPI: `McpFederatedIssuerAuthenticator`.

**Stays EE** (`com.bytechef.ee.*`):
- `McpTenantIssuerResolver`, `McpTenantIssuer`, `McpTenantIssuerResolverConfiguration`, `McpTenantIssuerCacheEvictionListener`, `McpTenantTrustResolutionFilter`, `McpJwtIdentityMapper.mapTenantIssuer(...)` (extract to an EE mapper or keep the method on a CE mapper the EE authenticator calls), `McpAudienceValidator.isTenantAudienceValid(...)`, the EE `McpFederatedIssuerAuthenticator` impl, `McpTenantProtectedResourceMetadata{Resolver,Controller,AuthenticationEntryPoint}`, `McpTenantDiscoverySecurityConfiguration`, and all embedded external-IdP federation (`EmbeddedMcpTrustedIssuerResolver`, the OAuth2 converter's federation, embedded tenant discovery).
- The `IdentityProvider` surface flags + `authoritiesClaim` + `authorityMappings` are already **CE** (platform-user) — the *data model* is shared; only the *federation behavior* is EE. No change.

**Split within a class** (base→CE, federation branch→EE SPI): `TenantAwareJwtAuthenticationFilter`, `IssuerLocationMcpJwtDecoderFactory` (via CE `McpTenantTrustContext`), the discovery contributor (via optional EE entry-point bean), `McpAudienceValidator`, `McpJwtIdentityMapper`.

## Edition reasoning to preserve

- CE-only build: static-issuer RS works; non-static issuers rejected (no SPI bean); discovery advertises the self AS. No EE types referenced.
- EE build: `@ConditionalOnEEVersion` federation beans present → the SPI + tenant discovery activate. Behavior identical to today.
- Tests: every base test moves to CE with the class; every federation test stays EE. The IntTest configs that today wire the whole thing in one module now span CE base + EE federation — split accordingly.

## Non-goals

- No functional change to how tokens are validated or how federation behaves — this is a relocation + SPI extraction only.
- Tool-authz stays as-is (CE evaluator + EE per-tenant surfaces).
- The `IdentityProvider` data model does not move (already CE).

## Effort

Larger than #6's API-key move. Roughly: AS module relocation (~14 files) + base-RS relocation (~18 files) + SPI extraction of the 5 woven classes (the delicate part — needs its own careful tasks and IntTests on both CE-only and EE builds) + splitting the IntTest configs. A #6-style task-per-concern plan, with an explicit **CE-only smoke IntTest** proving base OAuth2 works without any EE bean on the classpath.

## Recommendation

Proceed, but land the **SPI extraction first** (seams #1–#3) as behavior-preserving refactors *while everything is still EE* — so each seam is independently testable — **then** do the mechanical EE→CE relocation. That ordering de-risks the woven parts before the package churn. The plan follows this ordering.
