# PermissionEvaluator migration + embedded-skip — Design

- **Date:** 2026-05-30
- **Author:** Ivica Cardic
- **Status:** Draft (design)
- **Branch:** `0_732`

## 1. Problem & goals

Today, automation authorization is expressed in `@PreAuthorize` SpEL as **bean references** to a
named service:

```java
@PreAuthorize("@permissionService.hasProjectScope(#id, 'PROJECT_DELETE')")
```

Two changes are wanted:

1. **Adopt the standard Spring Security idiom.** Replace the `@permissionService.x(...)` bean-reference
   form with the built-in `hasPermission(...)` SpEL function backed by a custom
   `PermissionEvaluator`, so authorization reads in the canonical Spring way and is routed through a
   single, documented extension point.

2. **Skip automation permission checks when embedded delegates into automation.** Some embedded
   code (e.g. `AutomationWorkflowProjectFacadeImpl`) calls automation services that carry
   `@PreAuthorize`. In embedded execution there is no automation workspace/project membership for the
   acting principal, so those checks would deny. Embedded has already authorized the connected user at
   its own boundary; the automation-layer RBAC check must be **skipped for now** when the call
   originates from embedded.

These two goals are independent but converge: because *every* SpEL check is being routed through one
`PermissionEvaluator`, that evaluator becomes the single chokepoint where the embedded-skip is applied.

### Non-goals

- No change to the underlying RBAC semantics (workspace roles, project scopes, project roles, custom
  roles). All existing logic in `PermissionService` / `PermissionServiceImpl` (CE + EE) is preserved.
- No change to the 5 **direct Java callers** of `PermissionService` (cache eviction, `getMyProjectScopes`,
  `getMyWorkspaceRole`). They stay as direct bean calls.
- The embedded-skip is an interim, advisory bypass ("for now"). A future iteration may replace it with
  a positive embedded-principal authorization model. This spec does **not** design that.

## 2. Current state (inventory)

### 2.1 `PermissionService` interface
`server/libs/automation/automation-configuration/automation-configuration-api/.../service/PermissionService.java`

Gate methods (used from SpEL today):
- `boolean isTenantAdmin()`
- `boolean isCurrentUser(long userId)`
- `boolean hasWorkspaceRole(long workspaceId, String minimumRole)`
- `boolean hasProjectScope(long projectId, String scope)`
- `boolean hasProjectRole(long projectId, String minimumRole)`

Non-gate methods (direct Java callers, **unchanged** by this work):
- `Set<String> getMyProjectScopes(long projectId)` — also `@PreAuthorize("isAuthenticated()")`
- `String getMyWorkspaceRole(long workspaceId)` — also `@PreAuthorize("isAuthenticated()")`
- `evictProjectScopeCache`, `evictProjectScopeCaches`, `evictAllProjectScopeCache`
- typed default variants (`hasWorkspaceRoleTyped`, `hasProjectScopeTyped`, `hasProjectRoleTyped`)

### 2.2 Two edition-conditional implementations
- CE: `com.bytechef.automation.configuration.service.PermissionServiceImpl` (`@ConditionalOnCEVersion`) —
  all `hasXxx` return `true`; only `isTenantAdmin()` enforces a real authority.
- EE: `com.bytechef.ee.automation.configuration.service.PermissionServiceImpl` (`@ConditionalOnEEVersion`) —
  full RBAC, fail-closed error counters, scope cache.

Exactly one is loaded per edition, both registered as `@Service("permissionService")`.

### 2.3 Method security config
`server/libs/config/security-config/.../SecurityConfiguration.java` — `@EnableMethodSecurity(securedEnabled = true)`.
No existing `PermissionEvaluator`, no custom `MethodSecurityExpressionHandler`.

### 2.4 SpEL call sites to migrate (≈55)
CE/shared libs:
- `ProjectFacadeImpl` (×14), `ProjectServiceImpl` (×4), `ProjectWorkflowServiceImpl` (×3),
  `ProjectDeploymentServiceImpl` (×2)

EE libs:
- `ProjectUserGraphQlController` (×3), `ProjectUserServiceImpl` (×4), `WorkspaceServiceImpl` (×4),
  `WorkspaceUserServiceImpl` (×4), `CustomRoleServiceImpl` (×5), `WorkspaceFacadeImpl` (×1, combined OR),
  and the AI-Hub / AI-Gateway / AI-Observability / AI-Eval GraphQL controllers (×~14, all
  `hasWorkspaceRole(..., 'VIEWER')`)

(Exact line numbers enumerated during plan execution via a fresh grep — the count above is the
working estimate.)

## 3. Design

### 3.1 The evaluator — a thin adapter over `PermissionService`

A new class implements Spring's `PermissionEvaluator` and **delegates to the existing
`PermissionService`** so that all RBAC logic, CE/EE conditioning, fail-closed behavior, and error
metrics remain exactly where they are today. The evaluator carries no business logic.

```java
public class ProjectWorkspacePermissionEvaluator implements PermissionEvaluator {

    // targetType discriminators
    static final String PROJECT_SCOPE  = "ProjectScope";
    static final String PROJECT_ROLE   = "ProjectRole";
    static final String WORKSPACE_ROLE = "WorkspaceRole";
    static final String USER           = "User";

    // sentinel identifiers for the non-object checks
    static final String TENANT         = "Tenant";   // 2-arg object form
    static final String ADMIN          = "ADMIN";     // permission for TENANT
    static final String SELF           = "SELF";      // permission for USER

    private final PermissionService permissionService;

    // id + type form: hasPermission(#id, 'ProjectScope', 'PROJECT_DELETE')
    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        long id = ((Number) targetId).longValue();
        String value = String.valueOf(permission);

        return switch (targetType) {
            case PROJECT_SCOPE  -> permissionService.hasProjectScope(id, value);
            case PROJECT_ROLE   -> permissionService.hasProjectRole(id, value);
            case WORKSPACE_ROLE -> permissionService.hasWorkspaceRole(id, value);
            case USER           -> SELF.equals(value) && permissionService.isCurrentUser(id);
            default             -> false;   // unknown type fails closed
        };
    }

    // object form: hasPermission('Tenant', 'ADMIN')  — the target-less tenant-admin check
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return TENANT.equals(targetDomainObject) && ADMIN.equals(String.valueOf(permission))
            && permissionService.isTenantAdmin();
    }
}
```

Design notes:
- The evaluator injects the `PermissionService` **interface**; Spring supplies the CE or EE bean per
  edition. The evaluator itself needs **no** `@ConditionalOnXEVersion`.
- Unknown `targetType` → `false` (fail closed). An unknown 2-arg target → `false`.
- The constants exist for documentation/reuse; SpEL annotations must still use **string literals**
  (SpEL cannot reference these constants), so the migration uses the literal values.

### 3.2 Annotation migration — `hasPermission(...)` everywhere, sentinels for the odd two

Every gate becomes a `hasPermission(...)` call. The scope-vs-role distinction is carried by
`targetType`, not by an opaque permission string.

| Today (`@permissionService.…`) | After (native `hasPermission`) |
|---|---|
| `hasProjectScope(#id, 'PROJECT_DELETE')` | `hasPermission(#id, 'ProjectScope', 'PROJECT_DELETE')` |
| `hasProjectRole(#id, 'ADMIN')` | `hasPermission(#id, 'ProjectRole', 'ADMIN')` |
| `hasWorkspaceRole(#wsId, 'EDITOR')` | `hasPermission(#wsId, 'WorkspaceRole', 'EDITOR')` |
| `isCurrentUser(#id)` | `hasPermission(#id, 'User', 'SELF')` |
| `isTenantAdmin()` | `hasPermission('Tenant', 'ADMIN')` |
| `isTenantAdmin() or isCurrentUser(#id)` | `hasPermission('Tenant', 'ADMIN') or hasPermission(#id, 'User', 'SELF')` |

After migration, `@permissionService.*` appears in **zero** SpEL expressions. The `permissionService`
bean remains only for the 5 direct Java callers and as the evaluator's delegate.

### 3.3 Wiring — one global expression handler

`@EnableMethodSecurity` consumes a single `MethodSecurityExpressionHandler` bean. We contribute one
that wires in the evaluator:

```java
@Bean
static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(permissionEvaluator);
    return handler;
}
```

- **`static` @Bean** — method-security infrastructure initializes early; a non-static bean risks
  premature initialization of the configuration class.
- All existing built-ins (`isAuthenticated()`, `hasAuthority(...)`, etc.) keep working; we only *add*
  a permission evaluator to the default handler.

**Placement:** evaluator + this `@Bean` live in **`automation-configuration-service`** (CE shared
module). It already depends on `automation-configuration-api` for the `PermissionService` interface,
and every app that evaluates these `@PreAuthorize` annotations already loads it. `security-config`
stays untouched (no layering inversion into automation).

**Verification item (plan):** confirm no deployable app evaluates these annotations *without* loading
`automation-configuration-service`. If such an app exists, `hasPermission` would resolve against the
default handler (no evaluator) and **deny** — a fail-closed regression to catch. Also confirm there is
no pre-existing `MethodSecurityExpressionHandler` bean elsewhere (there must be exactly one).

### 3.4 Embedded-skip — annotation + aspect over a thread-local

A thread-local context mirrors the existing `TenantContext` pattern, with a wrapper that guarantees
`finally` cleanup:

```java
public final class AutomationAuthorizationContext {

    private static final ThreadLocal<Boolean> SKIP_CHECKS = ThreadLocal.withInitial(() -> false);

    public static boolean isSkipChecks() {
        return Boolean.TRUE.equals(SKIP_CHECKS.get());
    }

    public static <V> V callSkippingChecks(Callable<V> callable) { /* set true, try/finally restore */ }

    public static void runSkippingChecks(Runnable runnable)       { /* set true, try/finally restore */ }

    private AutomationAuthorizationContext() {}
}
```

**Read point:** the evaluator (§3.1), top of both `hasPermission` overloads — the single chokepoint
now that all SpEL routes through it.

**Set point:** the embedded→automation delegation boundary, applied at the **public-method level** of
the embedded facades (not per automation-service call, which would be dozens of interleaved wraps).
Mechanism — a marker annotation plus one aspect:

```java
@Retention(RUNTIME) @Target({TYPE, METHOD})
public @interface SkipAutomationAuthorization {}
```

```java
@Aspect
@Component
@ConditionalOnEEVersion
public class SkipAutomationAuthorizationAspect {

    @Around("@within(...SkipAutomationAuthorization) || @annotation(...SkipAutomationAuthorization)")
    public Object around(ProceedingJoinPoint pjp) {
        return AutomationAuthorizationContext.callSkippingChecks(pjp::proceed);
    }
}
```

Annotate **only `AutomationWorkflowProjectFacadeImpl`** (class-level annotation → all its public
methods run their entire body inside the skip window). Other embedded facades are intentionally left
out of scope for now; if a different embedded bridge later needs the skip, it just adds the same
annotation — no further infrastructure required.

The aspect lives in an EE embedded module so the skip exists **only** on embedded code paths.

### 3.5 Fail-open containment (security analysis)

A thread-local skip is **fail-open**: if it were set and not cleared, automation authorization would
silently pass. Three disciplines contain that risk:

1. **Guaranteed cleanup** — `callSkippingChecks` / the aspect's `try/finally` always restore the prior
   value, even on exception.
2. **Narrow scope** — set only inside the annotated embedded facade methods, never in a servlet
   filter or request-wide boundary. One embedded operation's worth of work, no more.
3. **No async propagation** — a plain `ThreadLocal` does not cross thread boundaries; embedded
   config-time delegation is synchronous, so the skip cannot leak onto worker/async threads. (If a
   future caller fans this work onto another thread, the skip simply will not apply there — fail
   *closed*, the safe direction.)

The skip is also EE-only by construction (aspect is `@ConditionalOnEEVersion`, and CE already returns
`true` from every gate anyway), so it changes behavior on EE embedded paths exclusively.

## 4. Components & files

New (in `automation-configuration-service`):
- `ProjectWorkspacePermissionEvaluator implements PermissionEvaluator`
- A `@Configuration` (or addition to an existing one) contributing the static
  `methodSecurityExpressionHandler` bean

New (in `core`/`platform` shared so both the evaluator and EE embedded can see it):
- `AutomationAuthorizationContext` (thread-local + wrappers)
- `@SkipAutomationAuthorization` marker annotation

New (in an EE `embedded-*` module):
- `SkipAutomationAuthorizationAspect` (`@ConditionalOnEEVersion`)

Modified:
- ≈55 `@PreAuthorize` annotations migrated from `@permissionService.x(...)` to `hasPermission(...)`
- `AutomationWorkflowProjectFacadeImpl` annotated (class-level) with `@SkipAutomationAuthorization`

Unchanged:
- `PermissionService` interface + both `PermissionServiceImpl`s (CE/EE)
- The 5 direct Java callers
- `SecurityConfiguration`

## 5. Testing

- **Evaluator unit tests:** each `targetType` dispatches to the correct `PermissionService` method;
  unknown type → `false`; 2-arg `Tenant/ADMIN` → `isTenantAdmin`; `User/SELF` → `isCurrentUser`;
  skip-context active → `true` without touching `permissionService` (verify with a Mockito
  `verifyNoInteractions`).
- **Expression-handler wiring test:** a thin `@SpringBootTest` slice asserting a single
  `MethodSecurityExpressionHandler` bean is present and has the evaluator set; a method annotated with
  a migrated `@PreAuthorize` denies/permits as expected.
- **Skip context tests:** `callSkippingChecks` sets then restores; restores on exception; nested calls
  restore correctly; value does not leak to a freshly spawned thread.
- **Aspect integration test (EE):** invoking an annotated embedded facade method with a non-admin /
  non-member principal succeeds (would otherwise deny); a non-annotated automation service called
  directly with the same principal still denies.
- **Regression sweep:** existing `@PreAuthorize` behavior on the migrated call sites is preserved
  (admin bypass, viewer/editor hierarchy, project scope grants) — rely on existing
  `PermissionServiceImpl` EE tests plus a sampling of controller/service security tests.

## 6. Open decisions for plan kickoff

1. **Exact module for `AutomationAuthorizationContext` + marker annotation** — must be visible to both
   the evaluator (`automation-configuration-service`) and the EE embedded aspect. Candidate: a small
   shared `platform-security`/`core` location. Resolve when wiring the modules.
2. **Set-point breadth** — RESOLVED: annotate **only** `AutomationWorkflowProjectFacadeImpl`
   (class-level). Other embedded bridge facades are out of scope for this iteration.
3. **`isTenantAdmin` form** — keep as the 2-arg `hasPermission('Tenant', 'ADMIN')` sentinel (chosen),
   vs. native `hasAuthority('ADMIN')`. Chosen: sentinel, for uniformity ("`hasPermission` for
   everything").
