# Plan — Default non-privileged role on signup & social login (gecko T1)

Spec: `docs/superpowers/specs/2026-06-21-default-role-on-signup-social-design.md` (Option A approved).

## Steps

1. **Social (`CustomOAuth2UserService`)** — `findOrCreateSocialUser(..., AuthorityConstants.ADMIN)` →
   `AuthorityConstants.USER`, with an explanatory comment. ✅
2. **OIDC (`CustomOidcUserService`)** — `String defaultAuthority = AuthorityConstants.ADMIN;` →
   `AuthorityConstants.USER;`. SSO branch keeps `identityProvider.getDefaultAuthority()`. ✅
3. **`UserServiceImpl.registerUser`** — inject `TenantService`; grant ADMIN only when
   `tenantService.isMultiTenantEnabled() || countActiveUsers() == 0`, else ROLE_USER. Fail-closed
   `ifPresent` (missing authority row → no authorities, never silent admin). ✅
4. **Tests** —
   - `UserServiceRegistrationAuthorityTest` (new, unit): first single-tenant user → ADMIN;
     subsequent single-tenant user → USER; multi-tenant registrant → ADMIN; new social user →
     exactly the passed authority (USER). ✅
   - `UserServiceTotpLockoutTest` — add `TenantService` mock to the `new UserServiceImpl(...)`. ✅
5. **Context wiring fallout** (constructor gained `TenantService`): every test context that
   constructs `UserServiceImpl` now needs a `TenantService` bean —
   - `platform-user-service` `UserIntTestConfiguration` → explicit `SingleTenantService` bean +
     `tenant-single-service` test dep. ✅
   - `tenant-single-security-config` `SingleTenantUserDetailsServiceIntTest` → broaden scan to
     `com.bytechef.tenant.single` + `tenant-single-service` test dep. ✅
   - `platform-user-rest` `UserIntTestConfiguration` already scans `com.bytechef.tenant`. ✅

## Verification

- `:platform-user-service:check`, `:security-config:check`, `:platform-user-rest:check`,
  `:tenant-single-security-config:check` all green.
- Production app contexts (server-app, EE apps) already wire the real `TenantService`.

## Notes / known limitation

The two OAuth2 service `loadUser` methods aren't unit-tested directly — they call
`super.loadUser(...)` (real provider HTTP) with no override seam, and the codebase has no
precedent for testing them. The authority actually gets applied inside `findOrCreateSocialUser`,
which **is** pinned (`testNewSocialUserGetsPassedAuthority`). The literal `ADMIN → USER` swaps in
the services are guarded by review + comments.
