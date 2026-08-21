# Per-environment workspace roles

**Status:** implemented (see As built)
**Edition:** Enterprise only
**Date:** 2026-08-19

## Problem

A workspace member holds exactly one role, and it applies everywhere. There is no way to
say "editor in Development, viewer in Production", or "no access to Production at all".

Three facts about the code as it stands:

- `workspace_user` is `(workspace_id, user_id, workspace_role, custom_role_id)`. There is
  nowhere to record that a role is environment-specific.
- No method on `PermissionService` takes an environment —
  `hasWorkspaceScope(workspaceId, scope)`, `hasWorkspaceScopeForProject(projectId, scope)`,
  `hasResourceScope(id, resourceType, scope)`.
- `environment` appears in no `@PreAuthorize` expression anywhere in the automation modules.

Environment is a data-partitioning key today: it selects *which* deployment, connection or
API key you act on, never *whether* you may act.

## Scope

**In:** an environment dimension on workspace membership, environment-aware permission
checks, and a promotion check against the target environment.

**Out:** the admin UI (its own phase, see Deferred), and `hasResourceScope`. Resource
visibility answers "may you see this thing"; environment answers "may you act here".
Conflating them would push environment into the `PRIVATE < WORKSPACE < ORGANIZATION`
ladder, which is a different axis.

**Organization stays implicit.** `ResourceVisibility.ORGANIZATION` is documented as "every
member of every workspace in the organization", and the organization is the tenant. No
`Organization` entity or table exists, and this design does not add one. Of the three
levels in ORG → WORKSPACE → ENV, only ENV is genuinely new: ORG is the tenant boundary that
already exists, and WORKSPACE already has membership rows.

**Environment stays an enum.** `Environment` is `DEVELOPMENT, STAGING, PRODUCTION` with no
table and no rows; `EnvironmentService` returns either the single configured value or all
three. Giving environments their own table — per-workspace, user-nameable — would change
every place the enum is used today, including deployments, connections and API keys. Not
part of this.

## Model

### Schema

`workspace_user` gains one nullable column:

```
environment INT NULL   -- Environment ordinal; NULL means "every environment"
```

The row already resolves to a role two ways — a built-in `workspace_role` ordinal or a
`custom_role_id` FK. Environment becomes part of the row's key, not part of the role.

Two partial unique indexes, because PostgreSQL treats NULLs as distinct and a plain
three-column unique constraint would permit unlimited NULL rows:

```sql
CREATE UNIQUE INDEX uk_workspace_user_implicit
    ON workspace_user (workspace_id, user_id) WHERE environment IS NULL;

CREATE UNIQUE INDEX uk_workspace_user_explicit
    ON workspace_user (workspace_id, user_id, environment) WHERE environment IS NOT NULL;
```

**No data migration.** Every existing row has `environment = NULL`, which is implicit mode,
which is exactly today's behaviour.

### Two modes per (workspace, user)

**Implicit** — a single row with `environment = NULL`. Its role applies in every
environment. This is what every existing member has.

**Explicit** — one or more rows, each naming an environment. **Only those environments are
reachable. An environment with no row is denied.** This is how "no access to Production" is
expressed: no Production row is written.

Omission is denial rather than fallback, which means adding a fourth environment later
denies by default instead of silently granting.

### Mode switching

Granting a member their first environment-specific role deletes their NULL row in the same
transaction.

Deleting their last environment-specific row removes them from the workspace. In explicit
mode a member's access *is* the set of environment rows, so removing the last one leaves
them reachable nowhere — which is the same state as not being a member. The alternative,
silently restoring a NULL row, would turn "revoke their last environment" into "grant them
every environment", which is the most dangerous possible reading of that action. Returning
a member to implicit mode is therefore an explicit operation that names the role to
restore, not a side effect of a delete.

Exactly one mode is represented at any moment. Two consequences worth stating: the
evaluator needs no precedence rule between a NULL row and an environment row, and a reader
of the table can never find two rows that disagree about the same environment.

Because the NULL row is gone in explicit mode, there is no "workspace role" left to bound
an environment role against. Each environment simply carries its own role. "Can an
environment row grant more than the workspace role" is not a question this model can ask.

## Resolution

`WorkspaceUserService` gains:

```java
Optional<ResolvedRole> fetchRole(long workspaceId, long userId, Environment environment);
```

1. An environment row for that environment → its role.
2. Otherwise the NULL row → its role.
3. Otherwise empty — denied.

`ResolvedRole` carries the same pair the row does: a `WorkspaceRole` or a custom role id.
It exists so callers do not re-implement the built-in-versus-custom branch.

## Permission checks

`PermissionService` gains environment-aware variants:

```java
boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment);
boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment);
```

The existing two-argument signatures remain and resolve against the NULL row, so no
existing call site changes and no guard silently shifts meaning.

**The environment is always passed explicitly. It is never read from `EnvironmentContext`.**
That thread-local holds the *source* environment during a promotion, and it is already
known to be lost on worker threads and in agent tool calls — the Copilot had to rehydrate
its security context through `toolContext()` for the same reason. An implicit read would
fail open in precisely the case this feature exists for.

## The scope cache

`WorkspaceScopeCacheService` caches the resolved scope set:

```java
@Cacheable(value = WORKSPACE_SCOPES_CACHE)
public Set<String> getWorkspaceScopes(long userId, long workspaceId)
```

Spring derives the key from the arguments, so today it is `(userId, workspaceId)`.

**The environment must become part of that key.** Without it, the first environment checked
for a user warms the cache and every later check on any other environment is served that
same scope set — a member who is viewer in Production would be answered with their
Development scopes for as long as the entry lives. That is silent privilege escalation, and
it would pass every unit test that exercises one environment at a time.

So `getWorkspaceScopes` takes the environment as a third argument, and
`evictWorkspaceScopeCache(userId, workspaceId)` evicts the entry for **every** environment
value rather than one. The environment set is the three enum values, so the eviction loop is
bounded and needs no prefix scan.

`WorkspaceScopeCacheKeyConsistencyTest` already exists and pins the current key behaviour; it
gains a case asserting that two environments do not share an entry.

## Promotion

There is no promote operation. Promotion is creating a `ProjectDeployment` in the target
environment, and the guard already sits on it:

```java
// today
@PreAuthorize("hasPermission(#projectDeploymentDTO.projectId, 'Project', 'WORKFLOW_EDIT')")
public long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO)
```

`ProjectDeploymentDTO` already carries `Environment environment`. The guard passes the
project id and ignores it.

The change is to pass the DTO instead of its id:

```java
@PreAuthorize("hasPermission(#projectDeploymentDTO, 'ProjectDeployment', 'WORKFLOW_EDIT')")
```

`AutomationPermissionEvaluator` recognises a `ProjectDeploymentDTO`, reads `projectId` and
`environment` from it, and checks the role in the **target** environment.

This is the whole reason the feature is coherent. If promotion checked the source
environment, "editor in Development, viewer in Production" would be decorative — anyone who
could edit in Development could put code into Production.

Spring's `PermissionEvaluator` fixes its two signatures and types `permission` as `Object`,
so a compound permission string like `'WORKFLOW_EDIT@PRODUCTION'` would also compile. It is
rejected here: it is stringly-typed, and it would have to be parsed in the one place that
must not be wrong.

The other two `createProjectDeployment` overloads carry no `@PreAuthorize` and are internal
callers. That matches the existing convention — the API facade owns authorization, the
shared facade is deliberately unguarded because runtime agent tools call it with no
security context.

## Edition split

CE's `PermissionServiceImpl.hasWorkspaceScope` returns `SecurityUtils.isAuthenticated()`.
CE has no authorization boundary between workspace members, so every member of a CE
workspace can already do everything.

**This work is Enterprise-only.** The CE implementation keeps returning `true` from the new
overloads. CE gains no column semantics to maintain and no behaviour change.

The EE implementation is
`server/ee/libs/automation/automation-configuration/automation-configuration-service/.../PermissionServiceImpl.java`.

## Testing

- **Resolution matrix**, table-driven over: implicit only; explicit covering the
  environment; explicit not covering it; neither. Assert the third case denies rather than
  falling back.
- **Regression**: a workspace whose rows are all NULL behaves identically to today across
  every existing guard. This is the test that protects every deployment on upgrade.
- **Promotion**: a member who is editor in Development and viewer in Production is refused
  when creating a deployment *into* Production, and permitted into Development. Assert the
  target decides, not the caller's current environment.
- **Mode switching**: after granting a first environment role the NULL row is gone; after
  deleting the last environment row the member has no rows at all and is denied everywhere.
  Assert both partial unique indexes hold under concurrent writes.
- **Thread-local independence**: the promotion check succeeds with `EnvironmentContext`
  unset or holding a different environment.

## Deferred

**Admin UI — no longer deferred.** This was originally left to a later phase, on the
grounds that the model and the checks should be reviewed first. Reversed while writing the
plan: without it there is no way to create an environment row except by calling
`WorkspaceUserService` from Java, so the feature would ship complete and unreachable. The
implementation plan covers it in two tasks — a GraphQL surface
(`setWorkspaceUserEnvironmentRole`, `removeWorkspaceUserEnvironmentRole`, plus `environment`
on the `WorkspaceUser` type) and the members dialog.

One UI requirement is worth stating at the design level because it is easy to get wrong:
removing a member's **last** environment role removes them from the workspace, so that
control must say so before it acts. A per-row delete that silently ends someone's
membership is the wrong affordance for what looks like a demotion.

**Custom roles per environment.** `custom_role_id` rides along in the same row and so is
already per-environment under this design. Whether the custom-role editor should expose
that is a UI question, deferred with the UI.

**A `NONE` role value.** Omission is denial, so a role meaning "no access" is not needed.
If the admin UI later wants denial to be visible in the table rather than inferred from an
absent row, a `NONE` value could be added — but it would be a UI affordance, not a model
change, and the evaluator would treat it identically to an absent row.

## Decisions log

| Decision | Chosen | Why the alternative was rejected |
|---|---|---|
| ORG level | Stays implicit (= tenant) | A real `Organization` entity changes tenancy itself and would migrate every existing workspace under a synthesized org — a separate subsystem, not this one. |
| ENV storage | Nullable column on `workspace_user` | A separate override table adds a second lookup to every check and a join in the evaluator, to express something the row's key already can. |
| Missing environment row | Denied, in explicit mode | Falling back to a workspace role would make "no access to Production" inexpressible, which was the motivating case. |
| Mode coexistence | The NULL row is replaced | Keeping it "but ignored" lets two rows disagree with only the evaluator knowing which wins. |
| Deleting the last environment row | Removes membership | Restoring a NULL row would turn "revoke their last environment" into "grant them every environment". |
| Completeness | List only what you grant | Requiring all three environments plus a `NONE` role makes adding a fourth environment a migration. |
| Promotion check | Target environment | Checking the source makes the whole split decorative. |
| Environment source | Passed explicitly | `EnvironmentContext` is a thread-local holding the *source* environment at promotion time, and is already known to be lost on worker threads. |
| Scope cache key | Includes the environment | Leaving it at `(userId, workspaceId)` serves one environment's scopes for another — silent privilege escalation that single-environment tests cannot catch. |

## As built

Recorded after implementation. Where the design or its plan assumed something that turned out not
to exist, the assumption is corrected here rather than quietly dropped.

**A unique constraint already forbade the model.** `202604061200060` had added
`uk_workspace_user_workspace_user` UNIQUE on `(workspace_id, user_id)` — the exact shape explicit
mode has to violate. Neither the design nor the plan mentions it. The changelog drops it, which is
not a weakening: `uk_workspace_user_implicit` re-imposes the same rule over the NULL rows, which is
every row that existed before this work. `RbacMigrationsIntTest` asserted that constraint and was not
on the plan's list of protected tests; it now asserts the two partial indexes.

**The promotion guard uses the two-argument `hasPermission` form.** The design proposed
`hasPermission(#projectDeploymentDTO, 'ProjectDeployment', 'WORKFLOW_EDIT')`. That is the
three-argument SpEL form, and `SecurityExpressionRoot` casts its first argument to `Serializable`
(spring-security-core 7.0.5, line 298) — `ProjectDeploymentDTO` is a plain record, so every promotion
would have thrown `ClassCastException`; and past the cast, the four-argument evaluator method
dispatches to `hasResourceScope` and never reaches the promotion branch. The two-argument form takes
the object as itself, dispatches on the real type instead of a string, and needs no `Serializable`
marker on a record whose components are not serializable. That method previously failed closed
unconditionally, so it gained the `AutomationAuthorizationContext.isSkipChecks()` short circuit its
sibling already had — without it, agent tool calls creating deployments would have been denied.
`PromotionGuardExpressionRoutingTest` evaluates the real annotation string through a real Spring
expression handler, closing the gap between "the pinned string is right" and "the string reaches the
promotion branch"; reverting the guard fails it.

**Cache eviction cannot use `SimpleKey`.** The read path falls through to the globally-configured
`TenantKeyGenerator`, storing under `"<tenantId>_<userId>_<workspaceId>[_<environment>]"`. Eviction
goes through `TenantCacheKeyUtils.getKey`, as the existing method's own comment already warned.

**The cached method resolves through the repository, not `fetchRole`.** `WorkspaceUserServiceImpl`
depends on `PermissionService`, which depends on `WorkspaceScopeCacheService`, so injecting the
service there would close a cycle. The precedence rule is mirrored with a comment saying why it is
duplicated.

**The GraphQL enum is `EnvironmentEnum`.** `platform-configuration-graphql` already declares
`type Environment { id, name }`, and Spring GraphQL merges every `graphql/*.graphqls` on the
classpath into one schema, so adding an `enum Environment` would be a duplicate type and would fail
schema assembly at boot.

**The two mutations are guarded on the service.** The design left authorization unstated and the plan
said to copy a `@PreAuthorize` from `addWorkspaceUser`, which has none — this controller's own Javadoc
records that authorization lives on `WorkspaceUserService`. Both writes carry
`WORKSPACE_MEMBER_MANAGE`, pinned in `PreAuthorizeAnnotationTest` and
`PreAuthorizeProxyEnforcementIntTest`, because that test is an explicit per-method allowlist rather
than a scan and would not have caught the omission. Both also evict the scope cache and write an
audit event, as every sibling mutation does.

**The members UI is the routed page, not the dialog.** `WorkspaceUsersDialog` and `WorkspaceUsers`
are different components; the plan named the dialog but its own named test file renders the page
(`routes.tsx:206`). The dialog still needed a fix, and a correctness one: it renders one row per
membership row, so an explicit-mode member appeared three times, and its role select writes through
`updateWorkspaceUserRole`, which resolves the member's row via `findByUserIdAndWorkspaceId` and would
therefore have edited an arbitrary environment's role with nothing on screen saying which. Rows
carrying an environment are read-only there.

**Explicit mode was initially unusable, and the follow-up landed with it.** Per this design the
two-argument `getWorkspaceScopes` reads the implicit row explicitly, and a member in explicit mode has
none — so on first landing they held no scopes under any guard that names no environment, which is
every guard except promotion. That was fail-closed but left the feature complete and half-reachable,
so the widening the design deferred was completed rather than left as a follow-up. It took three
parts, and none of them is safe alone:

- **The environment-unaware read became a union** across the environments a member can reach.
- **Membership mutations split by reach.** The union on its own is an escalation: a member who
  administers only Development could grant a workspace-wide role, which takes effect in Production
  too. `addWorkspaceUser`, `inviteWorkspaceUser`, `updateWorkspaceUserRole`, `assignCustomRole` and
  `removeWorkspaceUser` now require `WORKSPACE_MEMBER_MANAGE` in *every* environment;
  `setEnvironmentRole` and `removeEnvironmentRole` require it in the environment they name. Both are
  new SpEL functions on `AutomationMethodSecurityExpressionRoot`, because Spring fixes
  `hasPermission`'s two shapes and neither carries an environment.
- **By-id checks resolve the resource's own environment** through the new opt-in
  `ResourceEnvironmentResolver` SPI, wired into `hasResourceScope`. Four types contribute one —
  `ProjectDeployment`, `Connection`, `McpServer`, `ApiKey` — covering the 33 guards on them with no
  annotation changes. A type with no resolver keeps exactly the check it had, which is right: a
  project or a workflow definition does not live in an environment.

**The last-admin guard did not cover the new dimension.** `validateNotLastAdmin` counts ADMIN rows
for the whole workspace, which cannot see the failure this feature introduces: the sole admin grants
themselves ADMIN in Development, their implicit row is deleted, and Staging and Production are left
with nobody able to administer them while the count still reads one. Both per-environment writes now
compute which environments the member would stop administering and refuse if any would be stranded,
counting a null-environment row as administering every environment. Self-demotion is refused on the
same dimension. Tenant admins bypass both, as they do for the workspace-wide guards.

**Custom roles per environment shipped too.** `custom_role_id` rides in the same row as the
environment, so the model already supported it; only the surface refused. `setEnvironmentRole` takes
the same XOR pair as `addWorkspaceUser`. This also closed a defect in the members UI, whose
per-environment select listed custom roles while the handler cast the selection to a built-in role.
