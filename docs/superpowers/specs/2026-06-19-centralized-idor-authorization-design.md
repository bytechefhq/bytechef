# Centralized IDOR / Resource-Authorization Layer — Design

- **Date:** 2026-06-19
- **Branch:** `0_732`
- **Status:** Approved (design)
- **Scope:** Gecko remediation tasks **T17** (central layer), **T18** (connections/credentials), **T19** (API keys & signing keys)
- **Foundation commit:** `8926d55329293d9e90eae6c8a400b9c8b5471128` — *"1051 Add ProjectWorkspacePermissionEvaluator and method-security wiring"*

## 1. Context & problem

The Gecko scan reports ~370 IDOR / broken-access-control findings (`gecko-security-report.md`,
consolidated in `gecko-remediation-tasks.md` as T17–T25). The dominant shape: a facade/controller
method accepts a caller-supplied numeric `id` and passes it straight to `repository.findById(id)` /
a mutation with no check that the caller owns (or shares a workspace with) that resource.

Commit `8926d55` already built the reusable spine for fixing this:

- `PermissionService` — the authorization engine (CE permissive no-op; EE enforces workspace
  roles/scopes). Bean name `"permissionService"`.
- `ProjectWorkspacePermissionEvaluator` — adapts `PermissionService` to Spring Security's
  `hasPermission(...)` SpEL built-in, routing on a `targetType` string token.
- `hasWorkspaceScopeForProject(projectId, scope)` — the proven IDOR-closing pattern: resolve the
  resource's **owning workspace** from a repository, then evaluate the scope against the caller's
  **workspace membership**. A non-member resolves to `false` → access denied.

This spec generalizes that pattern into a per-domain-contributed SPI so the remaining domains
(T18–T25) plug in mechanically, and wires the first, highest-severity set: secret-bearing resources
(connections, API keys, signing keys).

### Two axes of "IDOR" — what this layer does and does not cover

- **Cross-tenant** isolation is enforced separately by the tenant `search_path` / `TenantContext`
  (Gecko T4). `findById` only ever returns rows in the current tenant's schema. This layer is **not**
  responsible for cross-tenant isolation.
- **Cross-workspace / cross-user within a tenant** is what this layer addresses.

### Edition model (load-bearing)

- CE `PermissionServiceImpl` returns `true` for every `hasWorkspaceScope*` check by design — CE is a
  permissive, collaborative single-tenant edition. **Workspace-scope enforcement is therefore an
  EE-only effect.**
- `isCurrentUser(userId)` is **real in CE** (it checks the SecurityContext), so owner-isolation is
  achievable in CE without changing the permissive workspace model.
- Per CLAUDE.md, CE forces connection visibility to `PRIVATE` (creator-only) in
  `ConnectionFacadeImpl.create()`. So cross-user connection access is a genuine IDOR **in CE too** —
  which is why connections get owner-isolation in CE (see §5).

## 2. Goals / non-goals

**Goals**

1. A central, per-domain-extensible resource-authorization primitive (T17).
2. Close connection/credential IDOR (T18) and API-key/signing-key IDOR (T19).
3. Establish the pattern + tests so T20–T25 are mechanical sweeps.
4. Mark T17, T18, T19 done in `gecko-remediation-tasks.md`.

**Non-goals**

- T20–T25 domain sweeps (own spec→plan→impl cycles each).
- Embedded `ConnectedUser*` / `externalUserId` / `X-Instance-Id` trust model — that is **T23**.
- HMAC-signing capability tokens (approval/trigger-form) — **T24**.
- Any change to cross-tenant isolation (T4).

## 3. Architecture — the central layer (T17)

### 3.1 Ownership-resolution SPI

Each domain module contributes one bean mapping a resource `id` to its owning coordinates. The
resolver fails closed (returns "unknown" coordinates) when the resource does not exist.

```java
package com.bytechef.automation.configuration.security;   // CE api module

public interface ResourceOwnershipResolver {

    /** Resource-type discriminator, e.g. "Connection", "ApiKey", "SigningKey", "ApiClient". */
    String resourceType();

    /** Owning coordinates for the resource id; both fields empty if the resource is unknown. */
    ResourceOwner resolveOwner(long id);

    record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId) {

        static final ResourceOwner UNKNOWN = new ResourceOwner(OptionalLong.empty(), OptionalLong.empty());
    }
}
```

A resolver may populate `workspaceId` (workspace-mapped resources), `ownerUserId` (user-owned
resources), or **both** (e.g. an API key that is both user-owned and workspace-mapped).

### 3.2 Registry + two new `PermissionService` methods

`PermissionService` gains two methods. The implementations receive the resolvers as
`List<ResourceOwnershipResolver>` and index them by `resourceType()` (validated unique at startup).

```java
boolean hasResourceScope(String resourceType, long id, String scope);
boolean isResourceOwner(String resourceType, long id);
```

Edition behavior:

| Method | CE behavior | EE behavior |
|---|---|---|
| `hasResourceScope(type, id, scope)` | tenant admin → `true`; else resolver yields `ownerUserId` → `isCurrentUser(ownerUserId)`; **else `false`** (fail-closed) | tenant admin → `true`; else resolver yields `workspaceId` → `hasWorkspaceScope(workspaceId, scope)`; else `false` |
| `isResourceOwner(type, id)` | **`true`** (permissive — EE-only enforcement for now) | tenant admin → `true`; else resolver yields `ownerUserId` → `isCurrentUser(ownerUserId)`; else `false`. |

Notes:

- `hasResourceScope` CE fallback is **fail-closed `false`**, not permissive. Rationale: this is a
  security primitive whose whole purpose is to remove fail-open-by-omission; a predicate that returns
  `true` under uncertainty re-creates the IDOR (a resolver bug would silently degrade an owned secret
  to "anyone can read"). The empty-`ownerUserId` branch is only reached when the resource does not
  exist (deny is unambiguously correct) or is genuinely orphaned (null `createdBy`/`userId` from a
  system/seed insert, or a deleted owner) — rare, since `@CreatedBy`/`userId` populate on every normal
  insert. **Tenant-admin bypass** (added to the CE path here, symmetric with EE) is the escape hatch:
  an admin can always reach and reassign an orphaned resource, so fail-closed never permanently locks
  the tenant out.
- Collaborative resources that want CE-permissive behavior keep using the existing `WorkspaceScope` /
  `ProjectScope` tokens (which delegate to `hasWorkspaceScope*`, permissive in CE). The new
  `ResourceScope` token is for resources we *do* want owner-enforced in CE.
- `isResourceOwner` is **EE-only for now** per the per-domain ruling: CE returns `true`. This defers
  CE enforcement of platform API keys; SigningKey/ApiClient live in EE-only modules so the CE branch
  is moot for them.
- Tenant admin (`isTenantAdmin()`) short-circuits to `true` in EE for both methods, matching the
  existing `hasWorkspaceScope*` contract.
- DB/cache failures rethrow (HTTP 500), consistent with `withCheckErrorCounter` — never silently
  fail-closed on infra outage.

### 3.3 Evaluator tokens

`ProjectWorkspacePermissionEvaluator.hasPermission(auth, targetId, targetType, permission)` gains
prefix-parsing. A `targetType` containing `':'` is split into `resourceType:kind`:

```java
// targetType "Connection:ResourceScope" → resourceType="Connection", kind="ResourceScope"
int sep = targetType.indexOf(':');
if (sep > 0) {
    String resourceType = targetType.substring(0, sep);
    String kind = targetType.substring(sep + 1);

    return switch (kind) {
        case RESOURCE_SCOPE -> permissionService.hasResourceScope(resourceType, id, value);
        case RESOURCE_OWNER -> permissionService.isResourceOwner(resourceType, id);
        default -> false;
    };
}
// existing tokens (WorkspaceScope / ProjectScope / WorkspaceRole / User) unchanged
```

The skip-checks short-circuit (`AutomationAuthorizationContext.isSkipChecks()`) remains at the top,
unchanged.

## 4. Usage at facade call sites

```java
// workspace-mapped secret (EE workspace-scope, CE owner-isolation)
@PreAuthorize("hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_DELETE')")
void delete(long connectionId);

// user-owned secret, no workspace (EE owner-isolation, CE permissive for now)
@PreAuthorize("hasPermission(#id, 'ApiKey:ResourceOwner', 'SELF')")
void delete(long id);
```

## 5. Per-domain wiring

| Resource | Owner shape | Token | CE | EE |
|---|---|---|---|---|
| **Connection** (T18) | workspace-mapped + `createdBy` | `'Connection:ResourceScope', 'CONNECTION_*'` | owner-isolation | workspace-scope |
| **WorkspaceApiKey** (T19) | workspace-mapped + `userId` | `'ApiKey:ResourceScope', 'API_KEY_*'` | owner-isolation | workspace-scope |
| **platform ApiKey** (T19) | `userId`, no workspace | `'ApiKey:ResourceOwner', 'SELF'` | permissive (deferred) | `isCurrentUser` |
| **SigningKey** (T19) | `userId`, no workspace (EE module) | `'SigningKey:ResourceOwner', 'SELF'` | n/a | `isCurrentUser` |
| **ApiClient** (T19) | `createdBy`, no workspace (EE module) | `'ApiClient:ResourceOwner', 'SELF'` | n/a | `isCurrentUser` |

### 5.1 Connections (T18)

- **Resolver:** `ConnectionOwnershipResolver` (resourceType `"Connection"`), in
  `automation-configuration-service`. `workspaceId` via
  `WorkspaceConnectionRepository.findByConnectionId(id)`; `ownerUserId` via the connection's
  `createdBy` login → resolve to userId (or expose `createdBy` comparison through `isCurrentUser`-by-login;
  see Open Questions).
- **Annotate** the currently-unchecked bare-`id` methods. Primary sites:
  `ConnectionFacadeImpl` `getConnection`/`delete`/`update`/`executeConnectionRefresh`,
  `ConnectionApiController` `getConnection`/`deleteConnection`/`updateConnection`,
  `ConnectionTagApiController.updateConnectionTags`.
- `getConnections(workspaceId, ...)` (list) already takes a `workspaceId` and applies
  `ConnectionVisibilityResolver`; gate it with `'WorkspaceScope', 'CONNECTION_VIEW'`.
- `ConnectionSearchAssetProvider.search` must scope results to the caller's connections (not the full
  platform set) — filter post-query by resolved access.
- `CONNECTION_VIEW/CREATE/EDIT/DELETE/USE` scopes already exist — **no enum change**.

### 5.2 API keys (T19)

- **Resolver:** `ApiKeyOwnershipResolver` (resourceType `"ApiKey"`), in
  `automation-configuration-service`. `workspaceId` via `WorkspaceApiKeyRepository.findByApiKeyId(id)`;
  `ownerUserId` via `ApiKey.getUserId()`.
- **WorkspaceApiKey** surfaces (`WorkspaceApiKeyFacadeImpl`, `WorkspaceApiKeyGraphQlController`):
  `'ApiKey:ResourceScope', 'API_KEY_*'`.
- **Platform ApiKey** surfaces (`ApiKeyFacadeImpl`, `ApiKeyGraphQlController`):
  `'ApiKey:ResourceOwner', 'SELF'`. `getAdminApiKeys` / `adminApiKeys` gated `'Tenant', 'ADMIN'`.
- **New scopes:** append `API_KEY_VIEW, API_KEY_CREATE, API_KEY_EDIT, API_KEY_DELETE` to the **end**
  of the EE `PermissionScope` enum (ordinal stability — enums are pinned by stability tests). Map them
  into the relevant built-in role scope sets.

### 5.3 Signing keys & API clients (T19, EE-only modules)

- **`SigningKeyOwnershipResolver`** (resourceType `"SigningKey"`) in `embedded-security-service`;
  `ownerUserId` via `SigningKey.getUserId()`. Annotate `SigningKeyServiceImpl` / `SigningKeyApiController`
  get/update/delete with `'SigningKey:ResourceOwner', 'SELF'`.
- **`ApiClientOwnershipResolver`** (resourceType `"ApiClient"`) in
  `automation-api-platform-configuration-service`; `ownerUserId` resolved from `ApiClient.createdBy`.
  Annotate `ApiClientServiceImpl` get/update/delete with `'ApiClient:ResourceOwner', 'SELF'`.

## 6. Module placement & dependencies

- The SPI interface (`ResourceOwnershipResolver`) lives in `automation-configuration-api` next to
  `PermissionService` so all editions/modules compile against it without an EE dependency.
- The two new `PermissionService` methods are added to the interface; implemented in **both** the CE
  `PermissionServiceImpl` (`automation-configuration-service`) and EE `PermissionServiceImpl`
  (`server/ee/.../automation-configuration-service`).
- Resolvers are discovered via Spring `List<ResourceOwnershipResolver>` injection. Each lives in the
  module that owns its relation repo / entity, contributing zero new dependencies to
  `PermissionService` itself (the registry depends only on the SPI). This is the whole point of the
  SPI over fattening `PermissionService`.
- A resolver bean absent at runtime (module not on classpath) simply means that `resourceType` is
  unregistered → `hasResourceScope`/`isResourceOwner` return `false` (fail-closed) for it.

## 7. Edition behavior matrix (summary)

| Token | CE | EE |
|---|---|---|
| `X:ResourceScope` | admin bypass; else owner-isolation if owner known, else deny (fail-closed) | admin bypass; else workspace-scope |
| `X:ResourceOwner` | allow (deferred) | admin bypass; else `isCurrentUser` |
| existing `WorkspaceScope`/`ProjectScope` | allow (permissive) | workspace-scope |

## 8. Testing strategy

- **Resolver unit tests** per resolver: known id → correct coordinates; unknown id → `UNKNOWN`
  (fail-closed).
- **Facade authorization tests** mirroring `AiEvalScoreFacadeAuthorizationTest` /
  `PreAuthorizeAnnotationTest`: assert each annotated method denies a non-owner / non-member caller
  and permits the owner/member; assert tenant-admin bypass.
- **Evaluator wiring test** extending `PermissionEvaluatorWiringIntTest` and
  `ProjectWorkspacePermissionEvaluatorTest`: the new `ResourceScope` / `ResourceOwner` tokens route to
  the right `PermissionService` method, prefix parsing is correct, unknown kind → `false`.
- **CE vs EE behavior tests**: `hasResourceScope` CE owner-branch + fail-closed fallback; `isResourceOwner`
  CE permissive vs EE enforced.
- EE test classes carry the `@version ee` Javadoc tag (Spotless header trigger).

## 9. Marking done

On landing, check off in `gecko-remediation-tasks.md`:

- **T17** — central resource-authorization layer (SPI + registry + evaluator tokens + tests).
- **T18** — connections/credentials.
- **T19** — API keys & signing keys.

Add a one-line note under each pointing to this spec. Findings not covered here (embedded
`ConnectedUser*` connection paths) stay under their owning task (T23) and are explicitly noted as
out of scope so the checkmarks are not over-claimed.

## 10. Risks & open questions

1. **`createdBy` is a login string, not a userId.** `isCurrentUser` takes a `long userId`. For
   Connection/ApiClient owner resolution we either (a) resolve `createdBy` login → userId in the
   resolver, or (b) add an `isCurrentUser(String login)` overload to `PermissionService`. Decide in the
   plan; (a) keeps the interface unchanged and is preferred.
2. **`ConnectionSearchAssetProvider` / list endpoints** require *result filtering*, not a single
   pass/deny gate — these are not pure `@PreAuthorize` sites and need per-row scoping. Treat as a
   distinct work item within T18.
3. **`ClusterElementDefinitionFacadeImpl`** loads connections by id for dynamic-property/option
   execution deep in component context. Gating it with `@PreAuthorize` may be too coarse (it runs in
   editor flows already gated upstream). Verify the call path; may defer to a connection-load check in
   `ConnectionService` rather than the cluster facade.
4. **GraphQL vs facade annotation site.** Prefer annotating the **facade/service** (single-caller)
   per the established controller-auth→facades rule (see project memory), not the GraphQL controller,
   unless the method is shared across controllers.
5. **Built-in role → scope mapping** for the new `API_KEY_*` scopes must be added to `BuiltInRoleScopes`
   (EE) or non-admins lose API-key access entirely.

## 11. Sequencing after this spec

T17 + T18 + T19 land together (this cycle). T20–T25 each become their own spec→plan→impl cycle reusing
this layer; each adds one resolver + a sweep of `@PreAuthorize` annotations + authorization tests, and
checks off its task.
