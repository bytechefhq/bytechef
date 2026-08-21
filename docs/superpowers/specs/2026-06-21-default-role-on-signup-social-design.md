# Design — Stop granting `ROLE_ADMIN` by default on signup & social login (gecko T1)

- **Date:** 2026-06-21
- **Branch:** `0_732`
- **Task:** gecko T1 (3 findings, 2× CVSS 9.4–9.8)
- **Status:** APPROVED (Option A) — 2026-06-21

## Problem

Three provisioning paths assign `ROLE_ADMIN` to brand-new users:

1. **`CustomOAuth2UserService.loadUser`** (`security-config`) — passes a hard-coded
   `AuthorityConstants.ADMIN` to `findOrCreateSocialUser(...)` for **every** social login
   (GitHub/Google). `security-config/.../CustomOAuth2UserService.java:96`.
2. **`CustomOidcUserService.loadUser`** (`security-config`) — `defaultAuthority` is
   initialized to `AuthorityConstants.ADMIN` and only overridden when the registration is an
   SSO-configured `IdentityProvider` (`sso-<id>`). Plain (non-SSO) OIDC registrations fall
   through with ADMIN. `security-config/.../CustomOidcUserService.java:91`.
3. **`UserServiceImpl.registerUser`** (`platform-user-service`) — unconditionally adds
   `AuthorityConstants.ADMIN` to every self-registered user.
   `platform-user-service/.../UserServiceImpl.java:534`.

### Where the real escalation is

`findOrCreateSocialUser(..., autoProvision=true, ADMIN)` runs whenever social login or
(non-SSO) OIDC is enabled. Those users **join an existing instance/tenant** — there is no
bootstrap semantics and no "first user only" gate. Result: **anyone with a Google/GitHub
account (or an account at a configured OIDC issuer) who logs in becomes a full
`ROLE_ADMIN`** over the already-populated instance. This is the 9.4–9.8 privilege escalation.

### Why `registerUser` is different (and mostly defensible today)

`registerUser` is reached only by `AccountController.registerAccount` (`POST /api/register`,
`permitAll`). That controller already constrains it:

- **Single-tenant (`mono`):** registration is rejected once `countActiveUsers() > 0`
  ("Organization already exists"). So only the **first** user can ever self-register — the
  legitimate instance owner, who *should* be admin.
- **Multi-tenant:** each activated registration spins up a **new isolated tenant**
  (`AccountController.activateAccount` → `tenantService.createTenant()` → re-saves the user
  into it). The registrant is the owner/admin of *their own* tenant — not a cross-tenant
  escalation.

So the registration-path ADMIN grant is the correct bootstrap, but it is **unconditional**
in the service — it relies entirely on the controller's gating. That is fragile
defense-in-depth: a second caller, or a regression in the controller check, silently yields
unconditional admins.

## Goals

- Federated logins (social + non-SSO OIDC) default to a **non-privileged** role.
- Preserve admin-configured SSO `defaultAuthority` (already correct).
- Preserve legitimate **bootstrap** admin (single-tenant first user; multi-tenant tenant
  owner) — **no lockout regression**.
- Harden `registerUser` so the admin grant is no longer unconditional at the service tier.

## Non-goals

- Building an admin-invite/promotion UI (instance admins already manage roles via the
  existing user-admin surface).
- Changing the SSO `IdentityProvider.defaultAuthority` mechanism.
- Changing `permitAll` on `/api/register` or the sign-up feature flag.

## Design

### 1. `CustomOAuth2UserService` (social) — ADMIN → USER

`findOrCreateSocialUser(..., true, AuthorityConstants.ADMIN)` →
`... AuthorityConstants.USER)`. Social-login users join as `ROLE_USER`; an instance admin
promotes them through the existing user-admin surface.

### 2. `CustomOidcUserService` (OIDC) — default ADMIN → USER

`String defaultAuthority = AuthorityConstants.ADMIN;` → `AuthorityConstants.USER;`.
The SSO branch (`registrationId.startsWith("sso-")`) keeps
`identityProvider.getDefaultAuthority()` — admin-configured, unchanged. Only the non-SSO
fallthrough changes (ADMIN → USER).

### 3. `UserServiceImpl.registerUser` — conditional bootstrap admin

Replace the unconditional `findByName(ADMIN)` with: grant **ADMIN** only when the
registration is a genuine bootstrap, else **USER**:

```
boolean bootstrapAdmin = tenantService.isMultiTenantEnabled() || countActiveUsers() == 0;
String authorityName = bootstrapAdmin ? AuthorityConstants.ADMIN : AuthorityConstants.USER;
```

- **Single-tenant:** first active user → ADMIN (unchanged in practice — controller already
  blocks the rest); a 2nd+ user that somehow reaches the service → USER (new defense-in-depth).
- **Multi-tenant:** registrant → ADMIN of their own fresh tenant (unchanged). `registerUser`
  runs in the registration tenant context; multi-tenant registrants are always tenant owners,
  so the `isMultiTenantEnabled()` short-circuit preserves current behavior without depending
  on the registration-tenant `countActiveUsers()` value.

`UserServiceImpl` gains a `TenantService` constructor dependency (`tenant-api`, already on the
module path via the tenant abstraction). Fail-closed: if the ADMIN/USER authority row is
missing, the user is created with **no** authorities (same as today's `ifPresent`), never a
silent admin.

## The one open decision

**`registerUser` bootstrap admin — keep (Option A) or strip (Option B)?**

- **Option A (recommended):** keep bootstrap-admin with the `isMultiTenantEnabled() ||
  countActiveUsers()==0` gate. No lockout; defense-in-depth; minimal change. Self-hosted
  single-tenant and embedded multi-tenant both keep working out of the box.
- **Option B:** strip `registerUser` to `ROLE_USER` always and require a separate
  out-of-band admin bootstrap (seed/CLI/env). Stronger ("registration never grants admin")
  but **lock-out risk**: a fresh single-tenant production install (no seeded admin — the
  Liquibase admin seed is `dev`-context only) would have **no admin at all** after the owner
  self-registers. Needs a new bootstrap path → larger blast radius.

Recommendation: **Option A**. It removes the escalation (social/OIDC) and de-fangs the
unconditional grant, without inventing a new bootstrap mechanism or risking lockout.

## Test plan

- `CustomOAuth2UserServiceTest` / `CustomOidcUserServiceTest` (or extend existing): assert a
  new social/non-SSO-OIDC user is created with `ROLE_USER`; assert SSO keeps
  `identityProvider.getDefaultAuthority()`.
- `UserServiceImpl` registration: first user (single-tenant, `countActiveUsers()==0`) → ADMIN;
  with an existing active user → USER; multi-tenant → ADMIN. Mockito on `tenantService` +
  `authorityRepository` + `userRepository`.
- Negative: missing authority row → user has no authorities (never silently admin).

## Affected files

- `security-config/.../oauth2/CustomOAuth2UserService.java`
- `security-config/.../oauth2/CustomOidcUserService.java`
- `platform-user-service/.../UserServiceImpl.java` (+ constructor `TenantService`)
- Tests as above; possibly `platform-user-service` build dep on `tenant-api` (verify).
