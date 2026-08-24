# Workspace scoping, resource visibility & sharing

The workspace_id column convention, the PRIVATE/WORKSPACE/ORGANIZATION model with grants, and per-environment workspace roles.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### Workspace scoping for platform entities

**New platform-package entities get a nullable `workspace_id BIGINT` column — not a
`workspace_<entity>` relation table.**

- The column is **nullable**, and the entity field is `Long` (never primitive `long`). Null is a real
  state: embedded has no workspace concept, and some entities use it for "global" scope (a
  `notification` with no workspace applies everywhere).
- If the entity later needs to be shared beyond its owning workspace, add a `visibility` column typed
  `ResourceVisibility` (`PRIVATE < WORKSPACE < ORGANIZATION`, in `platform-api`) and contribute a
  `ResourceVisibilityPolicy` declaring which rungs the resource supports and which one it is created
  with. Reach is **additive to a column** — it does not need a relation table, and it expresses
  ownership + reach, which membership rows cannot.
- **Exception — named-user grants.** A column expresses *reach* and is the right shape for it. It
  cannot express "these three specific people" without an unbounded array in a cell. Grants to
  individual users therefore live in the polymorphic `resource_grant` table (EE, see
  `platform-resource-grant`). That is the one sanctioned relation table for sharing; the rule above
  still governs reach. See `docs/superpowers/specs/2026-08-10-resource-visibility-design.md`.
- Create a `workspace_<entity>` relation table **only** for a genuinely many-to-many relationship with
  no owner concept — the `workspace_user` shape. The bar: *can the same row legitimately belong to two
  workspaces with equal standing, today, with an API that does it?*

**Six relation tables deliberately remain** (they are in release `v0.31.2`, so collapsing them would
mean data migrations against customer data for no user-facing benefit): `workspace_api_key`,
`workspace_connection`, `workspace_data_table`, `workspace_knowledge_base`, `workspace_mcp_server`, and
`workspace_user` (which is genuinely many-to-many and correct as-is). The resulting mixed state is
intentional, not drift.

Background and evidence: `docs/superpowers/specs/2026-07-25-workspace-relation-table-convention-revision.md`.
This revises the earlier `2026-05-06-workspace-relation-tables-design.md` for new work only.

### Resource Visibility & Sharing

A resource wired to this model carries a `visibility` column typed `ResourceVisibility` (`PRIVATE <
WORKSPACE < ORGANIZATION`, in `platform-api`, ordinals pinned by `ResourceVisibilityTest`). Every
resource is created **WORKSPACE-visible** — shared with its workspace unless its owner withholds it.
The model is resource-agnostic. Wired so far: **connections** (`PRIVATE`/`WORKSPACE`/`ORGANIZATION`)
and **projects** (`PRIVATE`/`WORKSPACE`; `ProjectVisibilityPolicy`, column `project.visibility`).
Workflows, project workflows, project deployments and jobs have **no column** — they inherit the
project's reach at check time via `ResourceVisibilityProvider`s whose `visibilityResourceType()`
returns `"Project"` and whose record id is the project id, so grants resolve against
`("Project", projectId)`.

**"Specific people"** is not a fourth stored value — it is `PRIVATE` plus rows in `resource_grant`
(EE, `platform-resource-grant`). A grant conveys visibility only; what the recipient may then do is
decided by the usual `PermissionScope`/`WorkspaceRole` machinery. Grants survive promotion so demoting
restores the previous audience, and are deleted with the resource because `resource_id` is
polymorphic and has no foreign key (connections in `WorkspaceConnectionFacadeImpl.delete`, projects in
EE `ProjectBeforeDeleteEventListener`).

**Visibility is a precondition of `hasResourceScope`**, in both editions — not a filter running beside
it. Without that, a member holding `CONNECTION_EDIT` would pass the by-id check for a connection the
list correctly hides. In CE this replaces owner-isolation *only* for resource types that registered a
`ResourceVisibilityProvider`; API keys and other user-owned resources keep it.
EE is pinned by `PermissionServiceVisibilityTest` (an EE-only class) and CE by the CE
`PermissionServiceResourceTest`; both are the regression guard, one per edition. Four entry points carry
the precondition: `hasResourceScope`, both `hasWorkspaceScopeForProject` overloads and
`hasWorkflowScope`. The whole `PermissionService` surface is enumerated and classified in
`docs/superpowers/specs/2026-08-17-project-visibility-design.md` §17 — extend that table when adding a
method rather than reasoning about it case by case; three of the four were closed by the project-
visibility plan (`hasResourceScope` came from phase 1) and two of those three were found after the
design rather than by it. The one deliberate omission is `hasResourceRole`, which is the owner-or-admin
sharing-management posture (an admin must be able to repair the sharing of a resource they cannot
themselves see). **That table covers the SPI, not the facades that reach it** — a facade method with no
annotation at all cannot appear in a table of what annotations route to, which is how
`ProjectWorkflowFacadeImpl`'s `getProjectWorkflow(long)` and no-argument `getProjectWorkflows()` stayed
unguarded until the whole-branch review.

#### Connections

- **CE**: `ConnectionFacadeImpl.create()` force-writes `WORKSPACE`. No picker, no grants — CE has no
  authorization boundary between workspace members, so everything is workspace-public.
- **EE**: the picker offers Shared with workspace / Private / Specific people. No `ROLE_ADMIN` gate on
  `WORKSPACE` — it is the default, so gating it would fail every ordinary create. `ORGANIZATION` is
  **not** offered here: it is reached through `createOrganizationConnection`, and
  `setConnectionVisibility` rejects it. (`ConnectionVisibilityPicker` can render an Organization
  option behind `showOrganizationOption`, but no caller passes it today.)
- **Embedded**: force-written `PRIVATE`, unchanged. An embedded connection belongs to a connected user,
  not a workspace member, so workspace reach would be wrong in a way that crosses customers.

**What sharing exposes.** `WORKSPACE` grants *use plus existence*, not *read plus write*: both REST
controllers obfuscate `authorizationParameters` and null `parameters`, so a member can never extract a
credential. Credentials *can* be replaced after creation —
`ConnectionFacade.replaceAuthorizationParameters`, reached through
`WorkspaceConnectionFacade.updateConnectionCredentials` — but only by the connection's **owner or an
admin** (`isResourceOwner || hasResourceRole(…, 'ADMIN')`, the sharing-management posture; the same check
`ConnectionServiceImpl.validateOwnerOrAdmin` already applies to every parameter write). `CONNECTION_EDIT`
alone renames and retags, and still cannot repoint a shared connection. A member can run a workflow
against a colleague's account; they can neither extract nor repoint the credential.

**GraphQL mutations** (owner-or-admin, annotated on the facade so they protect every caller):
- `setConnectionVisibility(workspaceId, connectionId, visibility)` — rejects `ORGANIZATION` (set
  through `createOrganizationConnection`) and refuses to narrow to `PRIVATE` while an active
  deployment uses the connection.
- `grantConnectionAccess` / `revokeConnectionAccess(workspaceId, connectionId, userId)` — grantee must
  be a member of the owning workspace; rejection reuses the unknown-connection error so user ids
  cannot be enumerated. Grant is idempotent via `ON CONFLICT DO NOTHING`, not a caught
  `DuplicateKeyException` — PostgreSQL aborts the transaction on a constraint violation, so catching it
  still fails at commit.
- `connectionGrants(workspaceId, connectionId)` — owner-or-admin; a plain viewer must not learn who
  else a connection was handed to.

**Audit**: `CONNECTION_VISIBILITY_CHANGED`, `CONNECTION_ACCESS_GRANTED`, `CONNECTION_ACCESS_REVOKED`.
The first and last are `strictAudit` — both can remove access.

**Metrics**: `bytechef_connection_create` (Counter), tagged `visibility`, wired via
`ObjectProvider<MeterRegistry>` so lightweight app variants without actuator start cleanly. Only
`PRIVATE` and `WORKSPACE` are ever emitted: the counter lives in
`WorkspaceConnectionFacadeImpl.incrementCreateCounter` — its own description reads "Connections
created via the workspace facade" — and `createOrganizationConnection` is a separate EE GraphQL
controller that never reaches it, while `setConnectionVisibility` rejects `ORGANIZATION` outright.

#### Projects (resource visibility phase 2)

- **CE**: `ProjectFacadeImpl.applyCreateVisibility` force-writes `WORKSPACE` and logs a requested
  `PRIVATE` rather than honouring it. **EE**: takes the request, defaulting to the policy default and
  rejecting an unsupported rung against `ProjectVisibilityPolicy.supportedVisibilities()`, which omits
  `ORGANIZATION` because a project belongs to exactly one workspace and the model has no way to express
  one that reaches outside it. `ORGANIZATION` is therefore never persisted in either edition, but by
  different means — EE consults the policy, CE never reaches it (the force-write returns first), and the
  REST enum cannot express the request in the first place. Only two paths write the column — create, and
  EE's `setProjectVisibility`; `updateProject` deliberately cannot change it.
- **Inheritance is a management-surface control only.** A `PRIVATE` project's deployments, webhooks,
  triggers and schedules keep serving traffic exactly as before. Making a project private hides it,
  its workflows, its deployments and its executions from the workspace's *lists and by-id reads* — it is
  not a way to undeploy or pause anything.
- **`ProjectVisibilityFilter` (`automation-configuration-api`) is the single `Project → VisibilityRecord`
  mapping** every list surface uses (projects, workflows, deployments, executions, search, GraphQL). It
  sits in `-api`, not `-service`, because `automation-configuration-graphql` depends only on `-api`; it
  resolves its `ResourceVisibilityResolver` through an `ObjectProvider` and fails closed, since six
  distributed EE apps carry `-api` without `-service` (ai-copilot, connection, coordinator, execution,
  webhook, worker — configuration-app is the one that carries both).
- **`hasWorkspaceScopeForProject` delegates to `hasResourceScope(projectId, "Project", scope)`** in both
  editions (the 3-arg EE overload inlines the same precondition so it can keep the explicit
  `Environment`). `hasWorkflowScope` routes through `hasResourceScope(workflowId, "Workflow", …)`, where
  `WorkflowVisibilityProvider` redirects the lookup to the owning project.
- **`ProjectOwnershipResolver` returns `ownerUserId` (from `created_by`) ONLY because
  `ProjectVisibilityProvider` is registered** — CE `hasResourceScope` then takes the visibility branch
  instead of owner-isolating. The two must never be split across commits: a resolver with an owner but
  no provider hides every project from everyone but its creator in CE.
- **"Hiding a project hides its EXECUTIONS" is monolith-only — the rest of the family is not.** Four of
  the five providers (`ProjectVisibilityProvider`, `ProjectWorkflowVisibilityProvider`,
  `ProjectDeploymentVisibilityProvider`, `WorkflowVisibilityProvider`) are unconditional `@Component`s in
  CE `automation-configuration-service`, which `configuration-app` **does** carry — so in distributed EE
  the project, project-workflow, deployment and workflow types are enforced there exactly as in the
  monolith. Only `JobVisibilityProvider` is missing: it ships in `automation-workflow-execution-service`,
  which `configuration-app` does not carry, so `"Job"` has no registered provider in that app and is
  unrestricted by visibility there. This is narrower than an earlier draft of this bullet (and of the
  design spec's §15.1) claimed — do not restate it as "configuration-app carries no providers".
  `execution-app`, which serves the automation executions REST surface, does not leak in compensation:
  it carries `automation-configuration-remote-client` rather than `-service`, so the unfiltered
  executions list throws (`RemoteProjectFacadeClient.getWorkspaceProjectWorkflows` is an
  `UnsupportedOperationException` stub) and every explicit project/workflow/deployment filter is denied
  (`RemotePermissionServiceClient` returns `false` for every check).
- **No "not while deployed" rule** when narrowing a project to `PRIVATE` (connections have one) —
  deployments inherit, so narrowing takes them along and nothing dangles.
- **Duplicating or importing a project produces a `WORKSPACE` copy carrying NO grants** — a copy is a new
  resource by a new actor, so it starts with standard permissions rather than the source's audience.
- **EE sharing** lives on the separate `ProjectSharingFacade` (owner-or-admin, annotated on the impl),
  GraphQL `project-sharing.graphqls`: `setProjectVisibility` / `grantProjectAccess` /
  `revokeProjectAccess` / `projectGrants`. The three enumeration-relevant failures — unknown project,
  project not in the named workspace, grantee not a member of it — all collapse to the same
  `ProjectErrorType.INVALID_PROJECT` so user ids and project ids cannot be probed. An unsupported rung is
  a different, non-enumerating failure and keeps its own `UNSUPPORTED_VISIBILITY`.
- **Audit**: `PROJECT_VISIBILITY_CHANGED`, `PROJECT_ACCESS_GRANTED`, `PROJECT_ACCESS_REVOKED` (via
  `ProjectAuditPublisher`). No create metric — spec decision ⚑9.
- **Client**: three surfaces, all EE-gated through `useVisibilityFeatureEnabled` /
  `useIsVisibilityEditionEnabled` — the create dialog's picker (reach only; a project that does not exist
  yet has no id to grant against), the project list item's visibility badge dropdown, and the project
  header settings menu's **`Visibility`** item. That menu item is labelled Visibility, not "Share": it
  already hosts two outward-publishing "Share" items (Share project, Share with Community) that mean
  something else entirely. `ResourceVisibilityPicker`/`ResourceVisibilityBadge` live in
  `client/src/shared/components/visibility/` and are shared with connections.


### Per-environment workspace roles (EE)

A workspace member holds either **one implicit role** (a `workspace_user` row with
`environment IS NULL`, applying everywhere — what every pre-existing member has) or **one row per
environment**, where an absent row is a denial. Never both: `setEnvironmentRole` deletes the implicit
row in the same transaction, and two partial unique indexes (`uk_workspace_user_implicit`,
`uk_workspace_user_explicit`) enforce it — the older `uk_workspace_user_workspace_user` constraint was
dropped because it forbade the shape outright. Removing a member's last environment row removes them
from the workspace; it does NOT restore an implicit row, which would turn "revoke their last
environment" into "grant them every environment".

**The environment is always an explicit argument, never read from `EnvironmentContext`.** That
thread-local holds the *source* environment during a promotion and is lost on worker threads and in
agent tool calls, so an implicit read fails open in exactly the case the feature exists for.

**Three checks, and the safety of the first depends on the other two.** Breaking any one silently
re-opens an escalation:

1. `hasWorkspaceScope(workspaceId, scope)` — environment-unaware — returns the implicit row's scopes,
   or for an explicit-mode member the **union** across the environments they can reach. Most guarded
   operations name no environment, so without the union an explicit-mode member holds nothing anywhere.
2. Operations taking effect in **every** environment at once use
   `hasWorkspaceScopeInEveryEnvironment` (`addWorkspaceUser`, `inviteWorkspaceUser`,
   `updateWorkspaceUserRole`, `assignCustomRole`, `removeWorkspaceUser`). Without this, (1) lets a
   member who administers only Development grant themselves Production.
3. Operations acting **in one** environment check that environment:
   `hasWorkspaceScopeInEnvironment(..., #environment)` for the per-environment writes, the
   `ResourceEnvironmentResolver` SPI for by-id guards, and `ProjectDeploymentDTO`'s own environment
   for promotion.

Both new expressions are SpEL functions on `AutomationMethodSecurityExpressionRoot`, not
`hasPermission` overloads — Spring fixes `hasPermission`'s two shapes and neither carries an
environment. Promotion's guard is `hasPermission(#projectDeploymentDTO, 'WORKFLOW_EDIT')`, the
**two**-argument form: the three-argument form casts its first argument to `Serializable`, which that
record is not, and routes to a method that never reaches the promotion branch.
`PromotionGuardExpressionRoutingTest` pins the whole path by evaluating the real annotation.

**`ResourceEnvironmentResolver`** (`automation-configuration-api`) is opt-in per resource type;
`ProjectDeployment`, `Connection`, `McpServer` and `ApiKey` contribute one. A type with no resolver
keeps the environment-unaware check, which is correct — a project, a workflow definition or a data
table does not live in an environment. A resolver returning empty falls back rather than denying.

**The scope cache key includes the environment.** Without it the first environment checked warms the
entry and every later one is served those scopes — silent privilege escalation that any
single-environment test passes. Eviction loops all three enum values and builds keys through
`TenantCacheKeyUtils.getKey`, never `SimpleKey`, or it no-ops against the tenant-prefixed read key.

**Admin protection is per environment.** `validateNotLastAdmin` counts workspace-wide and cannot see
the new failure: the sole admin moves themselves to Development-only and the other environments are
stranded while the count still reads one. The per-environment writes refuse to strand an environment,
counting a null-environment row as administering every one. Tenant admins bypass, as they do
workspace-wide.

CE is unaffected — every new overload returns `isAuthenticated()`, since CE has no authorization
boundary between workspace members. Spec:
`docs/superpowers/specs/2026-08-19-per-environment-workspace-roles-design.md` (see its As built
section).
