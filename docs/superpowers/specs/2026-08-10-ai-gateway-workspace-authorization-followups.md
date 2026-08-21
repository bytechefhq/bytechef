# AI Gateway workspace authorization — open follow-ups

**Date:** 2026-08-10
**Status:** Open
**Origin:** Surfaced by review during the models.dev model catalog work (merged into `0_732`, commits `ac3eff9717e..38c1039591f`). None of these are caused by that work; all were found while verifying it.

## 0. Authorization tests enumerate methods by name, so a new unguarded method passes

`WorkspaceAiGatewayModelFacadeAuthorizationTest`,
`WorkspaceAiGatewayProviderFacadeAuthorizationTest`,
`WorkspaceAiGatewayRoutingPolicyFacadeAuthorizationTest` and `AiEvalRuleFacadeAuthorizationTest` all
assert the exact `@PreAuthorize` expression for a hardcoded list of method names. They catch a
loosened or removed annotation on a method they name — but a **newly added** facade method carrying
no annotation at all passes every one of them.

`PreAuthorizeAnnotationTest` in `automation-configuration-service` shows the better form: a
`getDeclaredMethods()` sweep asserting every public interface-implementing method carries the
annotation. Applying that shape across the AI gateway's authorization tests would make them
self-maintaining.

Not a regression — every one of these tests matches the pre-existing convention. Recorded because the
gap is systemic rather than specific to any one facade.

## 1. Provider and routing-policy workspace facades have no caller-binding check — FIXED 2026-08-10

**Fixed in `22637e61b1e` / `ee810243b21`.** Reads on both facades now use
`hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')`; all nine writes (including
`testWorkspaceProviderConnection`, which reveals the stored `apiKey` and triggers outbound traffic)
require `ROLE_ADMIN`; all five `verifyWorkspaceOwnership` sites survive. Reflective authorization
tests added for both facades, and the client pages gate their write controls on
`authenticated && ROLE_ADMIN`. Reviewed independently: `#workspaceId` binds on impl and interface,
and an unresolved binding fails **closed** (`null` → `ResourceOwner.unknown()` → empty workspace →
`false`, no exception, no grant).

The original description follows, for context.

`WorkspaceAiGatewayModelFacadeImpl` was fixed first. Its siblings were not, until now.

`WorkspaceAiGatewayProviderFacadeImpl` gates `createWorkspaceProvider`, `deleteWorkspaceProvider`,
`getWorkspaceProviders`, `updateWorkspaceProvider`, and `testWorkspaceProviderConnection` with
`@PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")` only (lines 75, 94, 105, 111,
163). `workspaceId` arrives from the GraphQL argument, so nothing binds it to the caller.

`verifyWorkspaceOwnership` does not close this. It proves *resource ∈ workspaceId*; it never proves
*caller ∈ workspaceId*. The two questions read alike and the method name suggests the second, which
is why the gap survived earlier review.

**Exploit shape, same as the one closed on models:** a user in workspace 2 calls
`workspaceAiGatewayProviders(workspaceId: 1)` — also `USER`-only — to enumerate workspace 1's
providers and their ids, then calls `updateWorkspaceAiGatewayProvider` with `workspaceId: 1`. The
ownership check passes because the provider genuinely belongs to workspace 1, and the write lands.

Consequences are worse here than for models: `updateWorkspaceProvider` writes `apiKey`, `baseUrl`,
and `enabled`. Cross-tenant credential overwrite, not just pricing.

The routing-policy workspace facade has the same shape (not re-verified in detail).

**The correct form already exists in the same package** —
`AiGatewayWorkspaceSettingsFacadeImpl:42`, `AiGatewayRequestLogFacadeImpl:48`,
`AiEvalRuleFacadeImpl:79` all use
`@PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")`, which resolves
through `AutomationPermissionEvaluator` → `PermissionServiceImpl.hasResourceScope` to a genuine
per-user workspace-role check.

**Pattern applied to models, for reference:** reads got the `hasPermission` gate; writes got
`ROLE_ADMIN`; `verifyWorkspaceOwnership` was kept as the resource↔workspace half. Pinned by
`WorkspaceAiGatewayModelFacadeAuthorizationTest`, which asserts the exact expression strings
reflectively — necessary because `WorkspaceAiGatewayModelFacadeTest` constructs the impl directly
and never exercises method security at all.

## 2. The `hasPermission` read gate is EE-only — by design, NOT an open issue

Reviewed 2026-08-10 and deliberately closed as working-as-intended. Recorded so it is not reopened
as a latent hole.

CE `PermissionServiceImpl`
(`server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/service/PermissionServiceImpl.java`)
is uniformly permissive: `hasWorkspaceScope` and `hasWorkspaceScopeForProject` return
`SecurityUtils.isAuthenticated()`, `isResourceOwner` returns `isAuthenticated()`, and
`hasResourceScope` falls through to `resourceOwner.workspaceId().isPresent()` — true for any
authenticated caller once the resource resolves.

That is the seam that lets CE compile without workspace RBAC, which is an EE feature. CE has no
workspace membership or role model for a caller to be bound to, so there is nothing for
`hasPermission` to check. Tightening one facade's method would be inconsistent with the four sibling
facades using the same expression and would not produce isolation anyway, because the data model
underneath it does not exist in CE.

Making this real is a product decision — "should CE have workspace RBAC?" — not a code fix in the AI
gateway. The EE implementation (`hasResourceScope` → `hasWorkspaceScope`, with an `isTenantAdmin()`
short-circuit) is where the enforcement genuinely lives.

## 3. `ROLE_ADMIN` is global, so admins still cross workspaces

Accepted when the models gate was chosen: writes require `ROLE_ADMIN`, which is a tenant-global
authority, so a system admin can act on any workspace. This matches the platform-level
`AiGatewayModelFacade` posture. Recorded so it is a known property rather than a surprise.

## 4. Client shows controls the server now rejects

The AI Gateway Models page has no role gating, so non-admin workspace members see create / edit /
delete / reset-to-catalog controls that fail with `AccessDenied` at submit. A consequence of moving
writes to `ROLE_ADMIN`, not a defect in it.

## 5. `0_732` does not compile — FIXED 2026-08-10

`AiAutoMemoryService` declared `listAllOwners(long, int, AiAutoMemoryType)` (line 120) but
`RemoteAiAutoMemoryServiceClient` did not implement it, so
`platform-ai-auto-memory-remote-client:compileJava` failed.

Unrelated to the model catalog work — that branch changed zero files under either auto-memory tree.
Fixed by adding the stub throwing `UnsupportedOperationException`, per the EE remote-client
convention in `CLAUDE.md`. `compileJava` on that module is green.

## 6. Latent test-configuration pattern worth watching

`AiGatewayModelFacadeAsyncProxyTest` and `AiEvalExecutorAsyncProxyTest` each had a nested
`@Configuration` sitting in a package that `AiGatewayIntTestConfiguration` component-scans, so their
mock beans leaked into every integration-test context. The first collided
(`BeanDefinitionOverrideException` on `aiGatewayModelCatalogReconciler`, breaking all 14 integration
test classes); the second was dormant. Both fixed by removing the stereotype annotation — explicit
`AnnotationConfigApplicationContext.register()` still processes `@Bean` methods as a lite config
class.

`@TestConfiguration` does **not** fix this: its scan-exclusion applies only to the test class
currently executing, not to other classes on the scanned classpath. Verified empirically.

Any future nested test `@Configuration` under a scanned package has the same failure mode.
