# Project Visibility — Design (Resource Visibility phase 2, first resource)

- **Date:** 2026-08-17
- **Branch:** `0_732`
- **Status:** Accepted — all ⚑ decisions reviewed. Decision 1 confirmed 2026-08-17; decisions 2-10 reviewed
  2026-08-19 (2 confirmed with grants clarified, 8 revised to add the header settings menu, rest confirmed
  as written). §16 records the outcome of each.
- **Builds on:** `2026-08-10-resource-visibility-design.md` (the "parent spec"), which shipped the
  resource-agnostic model and wired `Connection` as phase 1
- **Ticket:** 4750

## 1. Summary

Wire the automation **`Project`** into the resource-visibility model the parent spec introduced. A project
gains a `visibility` column (`PRIVATE` | `WORKSPACE`, default `WORKSPACE`), a `ResourceVisibilityPolicy`, and a
`ResourceVisibilityProvider`; its list surfaces filter through the existing `ResourceVisibilityResolver`
seam; EE gets owner-or-admin mutations to set visibility and to grant/revoke named users, backed by the
existing `resource_grant` table.

Two things go beyond a plain copy of the connection wiring, and are the reason this needs a spec rather
than just a plan:

1. **Inheritance.** The parent spec fixed that workflows have no visibility of their own — their reach *is*
   their project's, resolved at check time. This spec extends the same rule to **project deployments and
   jobs (executions)** — every deployment of a project, with no opt-out — and adds the small SPI hook that
   makes inheritance expressible: a provider may declare that its visibility record and grants live under a
   *different* resource type (`Project`).
2. **`hasWorkspaceScopeForProject`** and **`hasWorkflowScope`** — two further authorization entry points
   that today bypass `hasResourceScope` and therefore bypass visibility. Both are folded into the
   chokepoint. `hasWorkflowScope` was missed in the first draft of this spec and added 2026-08-19 after
   the Task 1 review surfaced it: EE resolves workflow → project → `hasWorkspaceScope` with no
   precondition, and it is live — `WorkflowEditorSpringAIAgent` calls it with `WORKFLOW_VIEW` to gate the
   copilot's workflow editor, so leaving it open would let a non-grantee open a workflow inside a
   `PRIVATE` project. It is closed by routing through `hasResourceScope(workflowId, "Workflow", scope)`,
   which needs the `WorkflowVisibilityProvider`, so it lands with the providers rather than with
   `hasWorkspaceScopeForProject`.

Everything else is the parent spec's §11 recipe applied to `project`.

## 2. Goals / non-goals

**Goals**

- A project owner (or workspace admin) can withhold a project from the workspace and hand it to named
  colleagues; the default stays "shared with workspace" in every edition.
- A hidden project is hidden *everywhere it would otherwise appear* inside the workspace UI: the projects
  list, workflow lists, deployments, executions, search results, and every by-id endpoint.
- List filtering and by-id authorization agree (parent spec §7 / risk 2), for the project **and** for
  every child resource that inherits from it.
- CE stays fully collaborative: `WORKSPACE` forced on write, no picker, no grants.

**Non-goals**

- `ORGANIZATION` for projects (parent spec §3.2 keeps it connection-only; the project model has no
  representation outside its workspace).
- Data tables, knowledge bases, files, skills — later phase-2 resources; nothing here blocks them.
- MCP servers and API collections (parent spec §11 excludes them pending the API-key question). See
  §16 for the residual interaction with a private project.
- Grants carrying permissions, group grants, cross-workspace grants, ownership transfer.
- Agent tools that list projects tenant-wide (`ProjectTools.listProjects/searchProjects`) — a
  pre-existing, visibility-independent gap (they ignore workspaces entirely) that belongs to the uniform
  tool-surface work, not here. ⚑
- Query-level (SQL) filtering. Per-workspace project counts are small; the in-memory `VisibilityRecord`
  seam is kept, as the parent spec's §6 anticipated. The one hand-built SQL list query
  (`CustomProjectRepositoryImpl.findAllProjects`) is the obvious later home for a predicate.

## 3. Model

### 3.1 Rungs, default, ownership

| | Value |
| --- | --- |
| Supported rungs | `PRIVATE`, `WORKSPACE` |
| Default (all editions) | `WORKSPACE` |
| Owner | `project.created_by` (login), resolved to a user id via `UserService.fetchUserByLogin` where an id is needed |
| Grants | `resource_grant` rows with `resource_type = "Project"` (EE only) |

`ProjectVisibilityPolicy` (`resourceType() = "Project"`, default `WORKSPACE`, supported `{PRIVATE,
WORKSPACE}`) is the second `ResourceVisibilityPolicy` in the repo. It is a `@Component` in
`automation-configuration-service`, picked up by the existing registry bean
(`ResourceVisibilityConfiguration` in `platform-connection-api`) — no registry change.

### 3.2 User-facing states

Identical to connections minus Organization:

| UI state | Storage |
| --- | --- |
| Shared with workspace *(default)* | `visibility = WORKSPACE` |
| Private | `visibility = PRIVATE`, no grants |
| Specific people | `visibility = PRIVATE` + one grant row per user |

Grants on a `WORKSPACE` project are inert and are not deleted on promotion (parent spec §3.3).

### 3.3 Resolution

Unchanged from the parent spec §3.4: admin → `visibility >= WORKSPACE` → owner (`created_by`) → grant →
deny. Both resolver implementations (`DefaultResourceVisibilityResolver` CE,
`ResourceVisibilityResolverImpl` EE) are already resource-type-agnostic; neither changes.

### 3.4 Editions

| | CE | EE | Embedded |
| --- | --- | --- | --- |
| Value written on create | `WORKSPACE`, forced | as requested (validated against the policy), default `WORKSPACE` | n/a — embedded has no workspace projects; the embedded automation bridge deploys under the `__EMBEDDED_AUTOMATION__` system-project marker and is ADMIN-only |
| Picker | hidden | shown | n/a |
| Grants | unavailable | available | n/a |

## 4. Inheritance

### 4.1 Which resources inherit

| Resource type token | Id | Path to project | Inherits |
| --- | --- | --- | --- |
| `Workflow` | `String workflowId` | `ProjectService.getWorkflowProject(workflowId)` | yes (parent spec §11) |
| `ProjectWorkflow` | `long` | `ProjectWorkflowService` → project | yes |
| `ProjectDeployment` | `long` | `ProjectService.getProjectDeploymentProject(id)` | yes |
| `Job` | `long` | job → workflowId → project (as `JobOwnershipResolver` already does) | yes |

**The invariant: a child is exactly as visible as its project — never more, never less.** A `PRIVATE`
project with a workspace-visible deployment is not a state this model can represent, and it is not one
that would make sense: a deployment row displays its project's name, its workflows and its environment,
and a job's detail page shows the workflow itself. Hiding the project while listing its deployments and
executions would leak exactly what withholding the project was meant to withhold. **Every** deployment of
a project inherits — there is no per-deployment opt-out, no "published deployments are public" carve-out,
and no environment-based exception (a PRODUCTION deployment of a private project is as hidden as its DEV
sibling).

The parent spec's justification carries: **no child has its own column.** Reach is resolved at check time
from the project row, so there is no clamp rule, nothing to keep synchronized, and no incoherent state to
repair. Each of these resource types already has a `ResourceOwnershipResolver` walking the same path to
the project, so the visibility providers are mechanical.

### 4.1.1 What inheritance does *not* govern

Visibility is a **management-surface** control: who sees and administers the deployment. It is deliberately
not a runtime control.

- **Triggers keep firing, and webhook URLs keep serving.** `AbstractWebhookTriggerController#doProcessTrigger`
  and the request/app-event trigger controllers carry no `@PreAuthorize` and never consult
  `PermissionService` — they are anonymous by design, authenticated by the unguessable webhook URL, and are
  called by third-party systems that have no ByteChef identity at all. Making a project `PRIVATE` must not
  silently stop its integrations from running; that would turn a visibility setting into an outage.
  Withholding a project hides it from colleagues, it does not undeploy it.
- **Scheduled and event-driven runs continue**, for the same reason: the engine executes as the system, not
  as a viewer.
- **API collections and MCP servers are out of scope** (parent spec §11, pending the API-key question), so a
  key already issued against a workflow keeps working. See the risk in §16.

If "stop serving traffic" is what the user wants, the existing control is disabling or deleting the
deployment — a separate, explicit action with its own audit trail.

### 4.2 The SPI hook

`ResourceVisibilityProvider` today:

```java
String resourceType();
Optional<VisibilityRecord> fetchVisibility(long id);
```

Two problems for inheritance: (a) `Workflow` ids are Strings and `PermissionServiceImpl.isResourceVisible`
fails closed on non-`Number` ids before it ever asks a provider; (b) the EE resolver looks grants up under
the resource type it is handed, so a `Workflow` provider returning the project's record would have grants
looked up as `("Workflow", projectId)` and never match.

The interface gains two **default** methods; existing implementors are untouched:

```java
public interface ResourceVisibilityProvider {

    String resourceType();

    Optional<VisibilityRecord> fetchVisibility(long id);

    /**
     * Id-shape-agnostic entry point. The default handles numeric ids; providers for string-keyed
     * resources (workflows) override it. Non-numeric ids on the default fail closed.
     */
    default Optional<VisibilityRecord> fetchVisibility(Serializable id) {
        return id instanceof Number number ? fetchVisibility(number.longValue()) : Optional.empty();
    }

    /**
     * The resource type under which the returned record's visibility and grants are stored. A resource
     * that inherits its reach returns its parent's type ("Project") and its record's id is the parent's
     * id — so grant lookups resolve against the parent.
     */
    default String visibilityResourceType() {
        return resourceType();
    }
}
```

`PermissionServiceImpl.isResourceVisible` (both editions, currently verbatim twins) becomes:

```java
ResourceVisibilityProvider provider = resourceVisibilityProviders.get(resourceType);

if (provider == null) {
    return true;
}

return provider.fetchVisibility(id)                       // Serializable overload
    .map(record -> !resourceVisibilityResolver
        .filterVisibleIds(provider.visibilityResourceType(), 0L, List.of(record))
        .isEmpty())
    .orElse(false);
```

An inheriting provider therefore returns `new VisibilityRecord(project.getId(), project.getVisibility(),
project.getCreatedBy())` and `visibilityResourceType() = "Project"`. Owner check, admin check and grant
lookup all evaluate against the project — the single source of truth.

### 4.3 `hasWorkspaceScopeForProject`

`PermissionService.hasWorkspaceScopeForProject(projectId, scope)` (three copilot callers) resolves the
project's workspace and checks the workspace scope, bypassing `hasResourceScope` and hence visibility.
Both editions are changed to delegate: `hasWorkspaceScopeForProject(projectId, scope) ==
hasResourceScope(projectId, "Project", scope)`. In CE that swaps `isAuthenticated()` for the
visibility-preconditioned check; in EE it is behaviour-preserving for `WORKSPACE` projects and closes the
hole for `PRIVATE` ones. Its remote-client stub keeps throwing.

## 5. Schema

One new changeset in `automation-configuration-service`'s `includeAll` directory
(`config/liquibase/changelog/automation/configuration/`), mirroring the connection one:

```xml
<changeSet id="20260817000001-01" author="Ivica Cardic">
    <addColumn tableName="project">
        <column name="visibility" type="INT" defaultValueNumeric="1">
            <constraints nullable="false"/>
        </column>
    </addColumn>
    <comment>Projects default to WORKSPACE (1). PRIVATE (0) is opt-in via setProjectVisibility.</comment>
    <rollback>
        <dropColumn tableName="project" columnName="visibility"/>
    </rollback>
</changeSet>
```

INT ordinal, `NOT NULL`, default `1`. Every existing row becomes `WORKSPACE`, which is the pre-visibility
behaviour. No grant-table change; `resource_grant` is already polymorphic.

`Project` gets `private int visibility = ResourceVisibility.WORKSPACE.ordinal()` with a bounds-checked
`getVisibility()`/plain `setVisibility(ResourceVisibility)` exactly as `Connection` does. `ProjectDTO`
gains a non-null `visibility` component (record header, three constructors, `Builder`, `toProject()`).
`ProjectDTO.toProject()` copies it; the service's `update` path does **not** — visibility changes only via
`updateVisibility` (§9).

## 6. Module placement

| Artefact | Module | Edition |
| --- | --- | --- |
| `project.visibility` changeset | `automation-configuration-service` | CE |
| `Project.visibility`, `ProjectDTO.visibility` | `automation-configuration-api` | CE |
| `ProjectVisibilityPolicy` | `automation-configuration-service` (`security` package, beside the ownership resolvers) | CE |
| `ProjectVisibilityProvider`, `WorkflowVisibilityProvider`, `ProjectWorkflowVisibilityProvider`, `ProjectDeploymentVisibilityProvider` | `automation-configuration-service` `security` package | CE |
| `JobVisibilityProvider` | `automation-workflow-execution-service` `security` package (beside `JobOwnershipResolver`) | CE |
| `ResourceVisibilityProvider` default methods | `automation-configuration-api` | CE |
| `ProjectService.updateVisibility` (+ remote client stub) | `automation-configuration-api` / `-service` / EE `-remote-client` | CE / EE |
| `filterVisible` in `ProjectFacadeImpl`, `ProjectDeploymentFacadeImpl`, search providers | respective CE modules | CE |
| `ProjectSharingFacade` (+Impl) | EE `automation-configuration-api` / `-service` | EE |
| `project-sharing.graphqls`, `ProjectSharingGraphQlController` | EE `automation-configuration-graphql` | EE |
| Grant cleanup on delete | EE `ProjectBeforeDeleteEventListener` | EE |
| Audit events | CE `ProjectAuditEvent` (new constants), published from the EE facade via the CE `ProjectAuditPublisher` | CE / EE |

A **separate** `ProjectSharingFacade` rather than an EE subclass of `ProjectFacadeImpl` — the connection
precedent subclassed because the EE workspace-connection facade already existed; `ProjectFacadeImpl` has no
EE subclass and is large enough that adding one just for four methods would obscure it.

## 7. Read path (lists)

Every point below already applies the `SystemProjects` filter; visibility is applied at the same point,
through one private helper per facade that maps `Project` → `VisibilityRecord(id, visibility, createdBy)`
and calls `resourceVisibilityResolver.filterVisibleIds("Project", workspaceId, records)`.

| Surface | Where | Note |
| --- | --- | --- |
| Projects list (`getWorkspaceProjects`, `getProjects` tenant-wide) | `ProjectFacadeImpl.getProjects(private overload)` | one place feeds both public methods |
| Workflow lists (`getWorkspaceProjectWorkflows`, `getWorkspaceLatestProjectWorkflows`) | `ProjectFacadeImpl.getVisibleWorkspaceProjectIds` (already named for this) + the latest-workflows path | executions' no-filter case rides on `getWorkspaceProjectWorkflows` |
| Deployments list | `ProjectDeploymentFacadeImpl.getWorkspaceProjectDeployments` next to `filterOutSystemProjectDeployments` | drop deployments whose project is not visible; the projects are already loaded there |
| Executions page with an explicit `projectId` / `projectDeploymentId` / `workflowId` filter | `ProjectWorkflowExecutionFacadeImpl.getWorkflowExecutionsPage` | the id must pass `hasResourceScope(id, <type>, "EXECUTION_VIEW")`, else `AccessDeniedException` ⚑ — matches by-id semantics; the client never sends an id it cannot see |
| GraphQL `projects` / `project(id)` | `ProjectGraphQlController` | today calls `ProjectService` directly, skipping every facade gate; `project(id)` delegates to `projectFacade.getProject(id)`, `projects()` runs the resolver filter |
| Search | `ProjectSearchAssetProvider`, `WorkflowSearchAssetProvider`, `ProjectDeploymentSearchAssetProvider` | filter through the resolver (it resolves against the current principal, so the unscoped `workspaceId` these providers pass is fine); the workspace pass in `AutomationSearchFacadeImpl` stays |
| Project tags | `ProjectTagFacadeImpl` | tags of hidden projects would leak names of tags only — left as-is ⚑ |

AI-agent hidden `__AI_AGENT__` projects and other system projects are unaffected: they are created
`WORKSPACE` and are filtered by name prefix before visibility ever runs.

## 8. By-id path

Registering the five providers is all that the ~60 existing `hasPermission(#id, 'Project'|'Workflow'|
'ProjectWorkflow'|'ProjectDeployment'|'Job', …)` gates need — `isResourceVisible` becomes a precondition
for each. `ProjectService.publishProject`'s service-level `@PreAuthorize` is covered the same way.

**`ProjectOwnershipResolver` must also start returning `ownerUserId`** (resolved from `created_by` like
`ConnectionOwnershipResolver`), because the sharing mutations are gated
`@permissionService.isResourceOwner('Project', #projectId) || hasResourceRole(#projectId, 'Project', 'ADMIN')`
and `isResourceOwner` reads `ownerUserId`. Its Javadoc currently says it omits the owner "so CE treats
projects as shared" — that reasoning is superseded: once a `Project` visibility provider is registered, CE
`hasResourceScope` takes the "visibility already decided" branch and never consults `ownerUserId`. **The
resolver change and the provider registration land in the same commit**; landing the resolver first would
owner-isolate every CE project for one commit.

EE `hasResourceScope` reads only `workspaceId` from the ownership resolver, so nothing tightens there.

## 9. Write path

### 9.1 Create

`ProjectFacadeImpl.createProject` (CE): if the edition is CE, force `WORKSPACE`; in EE, take
`projectDTO.visibility()` (null → policy default) and validate with
`resourceVisibilityPolicyRegistry.supports("Project", visibility)`. Same `eeEdition` `@Value` +
`validateEdition` pattern as `ConnectionFacadeImpl`. Duplicate, import, import-template and
"create from template" write the **default** (`WORKSPACE`), not the source's value ⚑ — the copy is a new
resource created by the actor; if they want it private they set it after.

### 9.2 `ProjectSharingFacade` (EE)

```java
void setProjectVisibility(long workspaceId, long projectId, ResourceVisibility visibility);
void grantProjectAccess(long workspaceId, long projectId, long userId);
void revokeProjectAccess(long workspaceId, long projectId, long userId);
List<Long> getProjectGrants(long workspaceId, long projectId);
```

- All four: `@PreAuthorize("@permissionService.isResourceOwner('Project', #projectId) ||
  @permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')")`, annotated on the **facade** per
  convention; the GraphQL controller is pure argument mapping.
- `setProjectVisibility`: policy check (`supports`), project-belongs-to-workspace check, then
  `projectService.updateVisibility(projectId, visibility)`. **No "not while deployed" rule** ⚑ — for
  connections that rule exists because a deployment *uses* the credential and would be left pointing at
  something its operator can no longer see. A project's deployments inherit its visibility (§4.1, confirmed),
  so narrowing takes them with it and leaves nothing dangling; and because inheritance is a management-surface
  control only (§4.1.1), the deployments keep running throughout.
- `grantProjectAccess`: project-belongs-to-workspace, grantee is an active workspace member — non-member
  rejected with the *same* error as an unknown project so user ids cannot be enumerated (parent §8.2);
  then `resourceGrantService.grant("Project", projectId, userId)` (idempotent by `ON CONFLICT`).
- `revokeProjectAccess`: no membership check on the way out; hard delete.
- `getProjectGrants`: `@Transactional(readOnly = true)`, same owner-or-admin gate — plain viewers must not
  learn the audience.
- Authorize before existence/usage validation; collapse validation failures into one error type
  (`ProjectErrorType`, new constant if none fits).

### 9.3 Delete

EE `ProjectBeforeDeleteEventListener.onBeforeDelete` gains `resourceGrantService.deleteGrants("Project",
projectId)`. This is the established project-cleanup pattern (listener, not FK cascade — the grant table
has no FK to purge by).

## 10. Audit & metrics

`ProjectAuditEvent` gains `PROJECT_VISIBILITY_CHANGED`, `PROJECT_ACCESS_GRANTED`, `PROJECT_ACCESS_REVOKED`,
published through the existing `ProjectAuditPublisher.publish(event, projectId, additionalData)` with
`toVisibility` / `targetUserId` payload keys — the same keys `ConnectionAuditEvent` documents.

No new metric ⚑. `bytechef_connection_create{visibility}` exists to watch the credential-sharing mix,
which is the parent spec's risk 1; a project's visibility mix carries no comparable risk. Adding one later
is a three-line change in `createProject`.

## 11. API surface

**REST (`automation-configuration-rest-impl/openapi.yaml`)** — `Project` gains
`visibility: enum [PRIVATE, WORKSPACE]` (read on every project; accepted on create, ignored on update). The
generated client (`client/src/shared/middleware/automation/configuration`) is regenerated.

**GraphQL (EE `project-sharing.graphqls`)**, mirroring `connection-sharing.graphqls` exactly, including its
as-shipped return types (`Boolean!` / `[Long!]!`, not the parent spec's `Connection!` / `[User!]!`):

```graphql
extend type Query {
    projectGrants(workspaceId: ID!, projectId: ID!): [Long!]!
}

extend type Mutation {
    setProjectVisibility(workspaceId: ID!, projectId: ID!, visibility: ResourceVisibility!): Boolean!
    grantProjectAccess(workspaceId: ID!, projectId: ID!, userId: ID!): Boolean!
    revokeProjectAccess(workspaceId: ID!, projectId: ID!, userId: ID!): Boolean!
}
```

`ResourceVisibility` is already declared in CE `connection.graphqls`. The CE `project.graphqls` `Project`
type gains `visibility: ResourceVisibility!`.

## 12. Client

**Reuse, generalized.** `ConnectionVisibilityPicker` and `ConnectionScopeBadge` are already resource-agnostic
in behaviour (they speak plain `'PRIVATE' | 'WORKSPACE' | 'ORGANIZATION'` strings and a `grantedUserIds`
array). They move to `client/src/shared/components/visibility/` as `ResourceVisibilityPicker` /
`ResourceVisibilityBadge` (tests move with them; connection call sites re-import) ⚑. Nothing about their
props changes; `showOrganizationOption` simply is not passed for projects.

**Surfaces** ⚑ — mirror connections, nothing more:

- **`ProjectDialog` (create only)**: picker rendered when `useIsVisibilityEditionEnabled()` (EE) and
  `!project?.id`, inside a `visibility` form field, `grantedUserIds={[]}` (grants need an id). Edit mode
  shows the read-only badge with the same "change from the list" hint the connection dialog uses.
- **`ProjectListItem`**: the badge next to the project name is the `DropdownMenuTrigger`; the picker is
  the content. Grants are diffed (revoke removed, grant added), never replaced. Members come from the same
  `workspaceUsers` query the connection item uses. Successful mutations invalidate `ProjectKeys.projects`
  (and `ProjectKeys.project(id)`).
- **Project header settings menu**: no entry — the list item is the single edit surface, as for
  connections.

**Naming collision, avoided:** `client/src/pages/automation/project/components/ProjectShareDialog.tsx`
is the public template-export "share" feature. Nothing new is called "share"; the new UI is "visibility",
consistent with the connection wording.

**New GraphQL documents** under `client/src/graphql/automation/configuration/`: `projectGrants`,
`setProjectVisibility`, `grantProjectAccess`, `revokeProjectAccess`; regenerate `graphql.ts`.

## 13. Testing

- **Policy:** `ORGANIZATION` rejected for `"Project"`; default is `WORKSPACE`. (`ResourceVisibilityPolicyRegistry` behaviour, one test.)
- **Providers:** each of the five returns the *project's* record and `visibilityResourceType() ==
  "Project"`; unknown id → empty; `WorkflowVisibilityProvider` handles a `String` id and fails closed on a
  `Number`.
- **`isResourceVisible` SPI hook, both `PermissionServiceImpl`s** — the anti-drift tests, written first:
  a member holding `WORKFLOW_VIEW` is denied on a colleague's `PRIVATE` project by id and allowed once
  granted; the same through `'Workflow'` (string id) and `'ProjectDeployment'` — the grant is on
  `("Project", projectId)` and must be honoured for the child. `hasWorkspaceScopeForProject` on a
  `PRIVATE` project denies a non-owner in both editions.
- **List/by-id agreement:** `ProjectFacadeImpl.getWorkspaceProjects` hides the same project
  `getProject(id)` denies; deployments and executions lists hide the children of that project.
- **Sharing facade authorization (EE):** grant/revoke/set/list denied to a non-owner non-admin; allowed
  to owner and to workspace `ADMIN`; cross-workspace grantee rejected with the unknown-project error;
  `getProjectGrants` denied to a plain viewer; duplicate grant → one row, no error.
- **Create:** CE persists `WORKSPACE` when `PRIVATE` is requested; EE persists the request; duplicate of
  a `PRIVATE` project is `WORKSPACE` **and carries zero grants** (source grants are not copied).
- **Delete:** grants purged.
- **Client:** picker/badge tests move unchanged; `ProjectListItem` renders the badge only in EE and diffs
  grants; `ProjectDialog` shows the picker only for create in EE; the project header settings menu opens the
  same visibility dialog and is absent in CE.
- EE test classes carry `@version ee`.

## 14. Documentation to amend

- **`CLAUDE.md` "Resource Visibility & Sharing"** — "connections are the only resource wired to it so
  far" becomes "connections and projects (with workflows, project workflows, deployments and jobs
  inheriting the project's)". Add the `visibilityResourceType()` inheritance rule and the
  "provider + ownership-resolver change land together" note.
- **`docs/superpowers/specs/2026-08-10-resource-visibility-design.md` §11** — mark `Project` as
  implemented by this spec; note the SPI hook.
- **`openapi.yaml:1253`** (connection `visibility` description) — stale: still says "setting WORKSPACE
  requires ROLE_ADMIN"; fix while adding the project field.

## 15. Rollout constraints

- Developer `0_732` databases: a new additive changeset — no checksum break, no manual step.
- Order inside the plan: SPI hook + `PermissionServiceImpl` change → column/entity/policy → providers
  **together with** the ownership-resolver change → list filters → EE facade/GraphQL/audit → client. Each
  step compiles and passes on its own; nothing user-visible changes until the client step because the
  default is `WORKSPACE`.

## 15.1 As-built: distributed EE is covered except for jobs (added 2026-08-19, Task 4 review; **corrected 2026-08-20, Task 10 fix round 1**)

> **Correction.** The first version of this section claimed `configuration-app` "carries neither of those
> modules" and concluded that the whole project family goes unrestricted in distributed EE. That is
> **false**, and it was a generalisation of the narrower Task 4 finding (that `configuration-app` lacks
> the *execution* module) into a statement about all the providers. The corrected text follows; the
> headline survives, the mechanism does not.

**"Hiding a project hides its EXECUTIONS" is a monolith-only guarantee. The rest of the family is
enforced in distributed EE.**

Four of the five providers — `ProjectVisibilityProvider`, `ProjectWorkflowVisibilityProvider`,
`ProjectDeploymentVisibilityProvider`, `WorkflowVisibilityProvider` — are unconditional `@Component`s in
CE `automation-configuration-service`. `configuration-app`, where `PermissionServiceImpl` runs in
distributed EE, **does** carry that module (`server/ee/apps/configuration-app/build.gradle.kts` pulls the
CE module at line 33 and the EE one at line 86), and `ConfigurationApplication` scans `com.bytechef`. So
`"Project"`, `"ProjectWorkflow"`, `"ProjectDeployment"` and `"Workflow"` all resolve a provider there and
carry their visibility precondition exactly as in the monolith.

The exception is `JobVisibilityProvider`, which ships in `automation-workflow-execution-service` — a
module `configuration-app` does not carry. A resource type with **no registered provider is unrestricted
by visibility**, so `"Job"` is unrestricted in that app.

**`execution-app` does not leak in compensation.** It serves the automation executions REST surface
(`automation-workflow-execution-rest` + `-service`) but carries
`automation-configuration-remote-client` rather than `-service`. Two consequences, both read off the
code rather than reasoned:

- the unfiltered executions list derives its workflow ids from
  `projectFacade.getWorkspaceProjectWorkflows(workspaceId)`, and
  `RemoteProjectFacadeClient.getWorkspaceProjectWorkflows` is an `UnsupportedOperationException` stub;
- every explicit `projectId` / `workflowId` / `projectDeploymentId` filter goes through
  `ProjectWorkflowExecutionFacadeImpl.requireResourceScope` →
  `permissionService.hasResourceScope`, and `RemotePermissionServiceClient` returns `false` for every
  method, so each is denied with `AccessDeniedException`.

`ProjectVisibilityFilter` is on that app's classpath transitively via the remote client, and its
`getIfAvailable() == null` branch returns `Set.of()` — fail-closed — but in practice the facade throws or
denies before the filter is reached. What has **not** been observed on a running distributed deployment
is the end-to-end behaviour of the executions page against a `configuration-app` that hides the project;
the two statements above are established from the build files and the call chain, which is why the
residual risk is stated as "jobs have no provider in `configuration-app`" rather than as a claim about
what a user sees.

Distributed EE remains a deployment shape where several management-surface features degrade — the same
topology already cannot resolve an error workflow, cannot recover orphaned jobs, and cannot record
channel-born agent conversations (CLAUDE.md catalogues each). This is one narrow instance of that, not
the wholesale absence the earlier draft described.

Two consequences to carry forward:

- The remaining gap belongs to module topology, not to this design. Closing it means giving
  `configuration-app` either `JobVisibilityProvider` or a remote equivalent — a deployment-architecture
  change, not a visibility change.
- `ProjectVisibilityFilter` sits in `automation-configuration-api` (so `-graphql` can use it) while its
  `ResourceVisibilityResolver` collaborator lives only in `-service`. Six apps carry the former without
  the latter — ai-copilot, connection, coordinator, execution, webhook and worker; `configuration-app` is
  the one EE app that carries both, and api-gateway, config-server, runtime-job and scheduler carry
  neither (re-derived 2026-08-20 by resolving each app's `runtimeClasspath`, after the count was written
  as five). That made the filter a **startup** failure rather than a degraded one until it was changed
  to resolve the collaborator optionally and fail closed. Any future component placed in `-api` needs the
  same check: `-api` reaches apps that `-service` does not.

## 16. Decisions made without review (⚑) and risks

All ten were reviewed. Decision 1 was confirmed 2026-08-17; decisions 2-10 on 2026-08-19 — **2** confirmed
(with "no grants copied" made explicit), **8** revised to add the project header settings menu, and
**3, 4, 5, 6, 7, 9, 10** confirmed as written. The ⚑ marks are struck where the wording changed and stand
as historical elsewhere.

| # | Decision | Alternative | Why this one |
| --- | --- | --- | --- |
| 1 | ~~⚑~~ **Confirmed 2026-08-17.** Deployments and jobs inherit, not just workflows — and every deployment does, with no per-deployment or per-environment opt-out (§4.1) | workflows only (parent §11 minimum) | a private project with a workspace-visible deployment is incoherent: the deployment row shows the project's name, workflows and environment. Listing them leaks exactly what withholding the project withheld |
| 2 | ~~⚑~~ **Confirmed 2026-08-19.** Duplicate/import write the default `WORKSPACE` **and copy no grants** — the copy gets fresh, standard permissions and keeps nothing of the source's sharing | copy the source's visibility | the copy is a new resource by a new actor; a private source copied by a grantee would otherwise become a private project owned by the grantee |
| 3 | No "not while deployed" rule on narrowing to `PRIVATE` | mirror connections | deployments inherit; nothing dangles |
| 4 | Explicit execution filters on a hidden id → `AccessDeniedException` | empty page | by-id semantics; the client cannot produce such an id |
| 5 | Search results filtered in the three asset providers | leave search | private project names would appear in global search |
| 6 | Agent tools `ProjectTools.list/searchProjects` out of scope | filter them | already tenant-wide across workspaces — a separate, larger fix |
| 7 | Rename picker/badge to `ResourceVisibility*` under `shared/components/visibility/` | duplicate for projects | second consumer; connection-specific name would mislead |
| 8 | ~~⚑~~ **Revised 2026-08-19.** Client surfaces = create dialog + list-item badge dropdown **+ the project header settings menu** | create dialog + list item only | discoverability from inside a project outweighs the second sync point; the header menu already hosts Error Workflow, so the pattern exists |
| 9 | No `bytechef_project_create{visibility}` metric | add it | no risk to watch; trivial to add later |
| 10 | Project tags not filtered | filter | tags are workspace-level metadata; only tag names could leak |

**Risks**

- **API collections / MCP servers over a private project.** Excluded resources (parent §11) still list
  their own entities; a member may see an API collection whose project link then 404s. Accepted until
  the API-key question is decided; the collection page must tolerate a denied project fetch (it already
  does for deleted projects).
- **Two checks that must not drift** — now five resource types wide. The `isResourceVisible` tests in
  §13 are the guard, and they must cover the inheriting types, not just `Project`.
- **CE ordering hazard** (§8): resolver `ownerUserId` without the provider owner-isolates CE projects.
  Same commit.
- **Remote (distributed EE) apps**: `RemoteProjectServiceClient.updateVisibility` is a stub like its
  siblings; the sharing facade lives in configuration-app, so this is consistent with the rest of the
  project facade.

## 17. As-built: the `PermissionService` entry-point audit (added 2026-08-20, Task 10)

**Why this table exists.** This plan closed THREE authorization entry points; the spec named only ONE.
`hasWorkspaceScopeForProject(long, String)` was designed in (§4.3); `hasWorkflowScope` was found while
reviewing Task 1; the three-argument `hasWorkspaceScopeForProject(long, String, Environment)` was found
by the Task 2 implementer. A fourth, `hasResourceScope`, was already carrying the precondition from phase
1 — so FOUR entry points carry it today, of which three were closed here. Two of those three were
discovered after the design rather than by it — that is a discovery process, not a design, and the next
gap should not be found by a customer.

**And the table's premise held only for `PermissionService`.** The final whole-branch review found two
read methods on `ProjectWorkflowFacadeImpl` — `getProjectWorkflow(long)` and the no-argument
`getProjectWorkflows()` — carrying no gate of any kind, alongside eleven siblings that do. Both predate
this branch, which is not a defence: before it there was no `PRIVATE` state for them to defeat. They are
closed in the same shapes this plan already uses — the by-id read takes
`hasPermission(#projectWorkflowId, 'ProjectWorkflow', 'WORKFLOW_VIEW')`, and the listing, having no id to
gate on, takes a single batched `ProjectVisibilityFilter.visibleProjectIds` call
(`ProjectWorkflowFacadeVisibilityTest`). The lesson for this table is that enumerating one SPI's surface
does not enumerate the facades that reach it: a method with no annotation at all never appears in a
table of what the annotations route to.

So the whole public surface of `PermissionService` (`automation-configuration-api`) is enumerated below.
**Extend this table when adding a method.** A method may sit in the "correct to skip" column only for a
stated reason: it is not resource-keyed, the caller is the resource's own owner, or it is a deliberate
management posture.

Everything in the `@PreAuthorize` SpEL surface funnels here, so the table is the complete picture:
`AutomationPermissionEvaluator.hasPermission(id, type, permission)` → `hasResourceScope`;
`hasPermission(ProjectDeploymentDTO, permission)` → the 3-arg `hasWorkspaceScopeForProject`; and
`AutomationMethodSecurityExpressionRoot`'s built-ins (`isCurrentUser`, `isTenantAdmin`,
`isResourceOwner`, `hasWorkspaceScopeInEveryEnvironment`, `hasWorkspaceScopeInEnvironment`) are thin
delegations to the methods below — but not all of them to a same-named one, so read the delegation
rather than the name: `hasWorkspaceScopeInEnvironment(workspaceId, scope, environment)` delegates to
`hasWorkspaceScope(long, String, Environment)` (there is no interface method of its own name), and
`isResourceOwner(long id, String resourceType)` delegates to `isResourceOwner(String resourceType, long
id)` with the arguments in the opposite order.

**Which edition each justification describes (added in fix round 1).** The interface has two
implementations and they are not symmetrical. CE is a documented permissive pass-through with no
authorization boundary between workspace members: `hasWorkspaceRole`, both `hasWorkspaceScope` overloads,
`hasWorkspaceScopeInEveryEnvironment`, `isResourceOwner` and `hasResourceRole` are all literally
`return SecurityUtils.isAuthenticated();` there, the cache evictions are no-ops, `getMyWorkspaceScopes`
returns an empty set and `getMyWorkspaceRole` returns `"ADMIN"`. Only the four methods marked **yes**
below, plus `isCurrentUser` and `isTenantAdmin`, have a CE body that does real work. So each row states
which edition its justification is about; where a row says **both**, the reasoning holds in CE and EE for
different reasons and both are given.

| Method | By-id? | Goes through `isResourceVisible`? | If not, why that is correct |
| --- | --- | --- | --- |
| `evictWorkspaceScopeCache(long userId, long workspaceId)` | no | no | Not resource-keyed. Cache maintenance; grants nothing and answers nothing. |
| `evictWorkspaceScopeCaches(Collection<UserWorkspacePair>)` | no | no | Not resource-keyed. A `default` method looping over the above, which is what CE runs; EE overrides it with one batched cache eviction. |
| `evictAllWorkspaceScopeCache()` | no | no | Not resource-keyed. |
| `hasWorkspaceRole(long workspaceId, String minimumRole)` | no | no | Not resource-keyed — answers the caller's own standing in a workspace. There is no resource whose reach could be consulted. |
| `hasWorkspaceScope(long workspaceId, String scope)` | no | no | Not resource-keyed. Same as above. |
| `hasWorkspaceScope(long workspaceId, String scope, Environment)` | no | no | Not resource-keyed. Same as above, per environment. |
| `hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope)` | no | no | Not resource-keyed. Same as above, intersected over environments. |
| `hasWorkspaceScopeForProject(long projectId, String scope)` | **yes** (`Project`) | **yes** | — Both editions delegate to `hasResourceScope(projectId, "Project", scope)`. |
| `hasWorkspaceScopeForProject(long projectId, String scope, Environment)` | **yes** (`Project`) | **yes** | — CE discards the `Environment` and calls `hasResourceScope(projectId, "Project", scope)` directly, exactly as its 2-arg sibling does (CE has no per-environment roles, so nothing is lost); EE inlines `isResourceVisible(projectId, "Project")` so it can keep the explicit `Environment` the promotion caller passes. |
| `hasResourceScope(Serializable id, String resourceType, String scope)` | **yes** (any) | **yes** | — This is where the precondition lives (phase 1, parent spec §7). |
| `hasResourceRole(long id, String resourceType, String minimumRole)` | **yes** (any) | **no** | **Deliberate management posture — the one intentional omission. Describes EE**, whose body resolves the owning workspace and then asks `hasWorkspaceRole`; the CE body is `return SecurityUtils.isAuthenticated();` and both callers (`WorkspaceConnectionFacadeImpl`, `ProjectSharingFacadeImpl`) are EE-only, so CE never reaches it in a gate that matters. See §17.1. |
| `hasWorkflowScope(String workflowId, String scope)` | **yes** (`Workflow`) | **yes** | — Routes through `hasResourceScope(workflowId, "Workflow", scope)`; `WorkflowVisibilityProvider` redirects the lookup to the owning project. |
| `getMyWorkspaceScopes(long workspaceId)` | no | no | Not resource-keyed — returns the caller's own scope names. |
| `getMyWorkspaceRole(long workspaceId)` | no | no | Not resource-keyed — returns the caller's own role name. |
| `isCurrentUser(long userId)` | no | no | Not resource-keyed — an identity comparison against the authenticated principal. |
| `isResourceOwner(String resourceType, long id)` | **yes** (any) | **no** | **The caller is the resource's own owner — describes EE**, whose body resolves `ownerUserId` and compares it to the current user; a `true` answer means the current user created the resource, and `PRIVATE` is by definition "the owner plus grantees", so wherever this returns `true` the resource is already visible. Adding the precondition could therefore only turn a `true` into a `false`, and precisely where it must not: the sharing facades' `isResourceOwner(...) \|\| hasResourceRole(...)` gates exist so an owner can repair a resource they have withheld. **In CE** the body is `return SecurityUtils.isAuthenticated();` — no ownership is consulted at all, consistent with CE having no boundary between workspace members, and the sharing facades that use this token are EE-only. |
| `isTenantAdmin()` | no | no | Not resource-keyed. Also the short-circuit that every other check consults first. |

The nested `record UserWorkspacePair(long userId, long workspaceId)` is a value carrier for the cache
eviction API and has no authorization surface.

### 17.2 The endpoint-keyed counterpart: `VisibilityBearingSurfaceAuditTest` (added 2026-08-20)

The table above is **method-keyed**, and that axis has a structural blind spot: authorization only happens if
something calls the machinery, so a facade method with no `@PreAuthorize` and no filter never reaches
`PermissionService` and cannot appear in a table of what its methods route to. That is exactly how
`ProjectWorkflowFacadeImpl.getProjectWorkflow(long)` and `getProjectWorkflows()` survived to the final review.

`VisibilityBearingSurfaceAuditTest`
(`automation-configuration-graphql/src/test/java/com/bytechef/automation/configuration/security/`) inverts the
axis. It reflectively enumerates the public methods of the CE facade implementations
(`com.bytechef.automation.configuration.facade`) and the CE GraphQL controllers
(`com.bytechef.automation.configuration.web.graphql`), and requires every method whose signature mentions one of
the five visibility-bearing types — or a DTO projecting one — to carry `@PreAuthorize`, to be a GraphQL field
resolver (`@SchemaMapping`/`@BatchMapping`, whose parent a root field already authorized), or to appear in an
exemption map with a reason naming what protects it instead. Its own javadoc carries the scope statement and what
it deliberately does not cover; three further assertions keep it from rotting — no stale exemption, every DTO in
the shared package classified, and the scan actually reaching both layers.

Read the two together: this section's table answers "is the lock well made?", the test answers "is there a lock on
every door?". Extend the table when adding a `PermissionService` method; the test needs no extending, but an
exemption added to silence a genuine finding converts it into the opposite of what it is for.

Four findings it produced on landing, all since fixed. Two it covers directly: the GraphQL `projects()` and
`workspaceProjectDeployments()` listings, neither gated nor workspace-scoped. Two it surfaced without covering:
`ProjectApiController.getProjectVersions(Long)`, a REST by-id read straight through to `ProjectService` and
invisible to a type-keyed rule because REST returns generated `*Model` types, now reading through a guarded facade
method; and `ProjectDeploymentWorkflowGraphQlController.workspaceChatWorkflows(Long, Long)`, which returned a
purpose-built projection record with no gate and no project-visibility filter. The latter was the more instructive
one: it had neither because the listing was assembled *in the controller* out of six services, past the facade
where a gate would live. It now reads through `ProjectDeploymentFacade.getWorkspaceChatWorkflows`, which carries
`hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')` and filters through `ProjectVisibilityFilter`. Its live
client consumer — the AI Hub chat launchers — tolerates a shorter list; every consumer's degraded state is
enumerated on that facade method.

A fifth surface, `projectDeploymentWorkflow(String id)`, was found later by review rather than by this test, and
the reason is worth recording: the test keys on a list of visibility-bearing *type names*, and the domain type
`ProjectDeploymentWorkflow` was absent from it while only DTOs were checked for classification. A type-keyed audit
is exactly as complete as its type list, and nothing forced that list to be complete.

### 17.1 Decision: `hasResourceRole` deliberately has no visibility precondition

**This section describes the EE implementation.** There, `hasResourceRole(long id, String resourceType,
String minimumRole)` resolves the resource's owning workspace through its `ResourceOwnershipResolver` and
then asks `hasWorkspaceRole`. It carries no visibility precondition, and it is live for a type that HAS a
registered provider — `'Connection'` through `WorkspaceConnectionFacadeImpl`'s owner-or-admin gates on
`setConnectionVisibility`, `grantConnectionAccess`, `revokeConnectionAccess` and `getConnectionGrants`.

The CE body is `return SecurityUtils.isAuthenticated();` — it resolves nothing and grants every
authenticated caller, which is CE's documented permissive posture rather than a second omission to
justify. Both sharing facades that use this token are EE-only, so the CE body is not reached from any
sharing gate. Everything below is therefore about EE.

This is the documented **owner-or-admin sharing-management posture**, not an oversight: a workspace
admin must be able to repair the sharing of a resource they cannot themselves see. A resource is
withheld precisely so it is invisible to the workspace; if the admin arm of that gate were gated on
visibility in turn, a `PRIVATE` resource whose owner has left the company would be unrecoverable by
anyone.

**Task 7's project sharing SpEL relies on this posture.** `ProjectSharingFacadeImpl` uses the identical
`@permissionService.isResourceOwner('Project', #projectId) || @permissionService.hasResourceRole(
#projectId, 'Project', 'ADMIN')` pair on all four of its methods. Adding a visibility precondition to
`hasResourceRole` would silently make a private project unshareable by an admin — which is the operation
those methods exist to perform.

The reach of the posture is bounded: the gate still requires the workspace-`ADMIN` role in the
resource's OWN workspace, and it authorizes only the sharing-management surface — no read of the
resource's contents follows from it. The one other caller,
`KnowledgeBaseDocumentApiFacadeImpl`, names a resource type with no registered
`ResourceVisibilityProvider`, so no precondition would apply there today in any case.

**If a future resource wants the precondition**, the fix is not to add it inside `hasResourceRole` (that
would break every sharing facade at once) but to introduce a second, precondition-carrying method and
migrate the call sites that want it — the same shape as the two `hasWorkspaceScopeForProject` overloads.
