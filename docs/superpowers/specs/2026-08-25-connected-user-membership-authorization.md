# Connected-user membership authorization

**Status:** design only — nothing implemented
**Edition:** Enterprise (embedded); the seam itself is Community
**Ticket:** 1051
**Date:** 2026-08-25

## Problem

`EmbeddedAutomationAuthorizationSkipFilter` wraps every request carrying an
`EmbeddedApiKeyAuthenticationToken` in `AutomationAuthorizationContext.callSkippingChecks(...)`
(`server/ee/libs/embedded/embedded-security-web/embedded-security-web-impl/src/main/java/com/bytechef/ee/embedded/security/web/filter/EmbeddedAutomationAuthorizationSkipFilter.java:50,57`),
disabling automation permission checks wholesale for the request's whole synchronous stack.

A parallel branch, `claude/embedded-permission-bypass-1051`, narrows that to a **restricted** mode:
tenant-admin, workspace-scoped, identity and ownership checks run for real again, but resource-scoped
checks — every check keyed on a single `Workflow`, `Connection`, `Project` or `Job` — are still
skipped outright.

That leaves the resource half open. A connected user may pass **any** id in the tenant to a
resource-gated operation and it is granted. The sharpest consequence is
`WorkflowNodeScriptFacadeImpl.testWorkflowNodeScript`
(`server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/WorkflowNodeScriptFacadeImpl.java:208`),
gated `hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')`: bypassed, it executes a node from
another workspace's workflow **using that workflow's stored connection** and returns the output.
Credential exercise without credential read.

**End state this design aims at:** the permission machinery answers a resource check for a connected
user from that user's own membership, so there is nothing left to skip.

## What already exists (verified)

### The membership data

One project per connected user per environment. `ConnectedUserProjectWorkflowManager.getOrCreateConnectedUserProject`
(`.../embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectWorkflowManager.java:151-166`)
creates a `Project` named `__EMBEDDED__<externalUserId>` in `Workspace.DEFAULT_WORKSPACE_ID` and a
`ConnectedUserProject(connected_user_id, project_id)` row pointing at it.
`ConnectUserProjectRepository.findFirstByEnvironmentAndExternalUserId`
(`.../repository/ConnectUserProjectRepository.java:35-43`) joins `connected_user` so the lookup is
(external id, environment) → the one project.

`ConnectedUserProjectWorkflow` (`.../embedded-configuration-api/.../domain/ConnectedUserProjectWorkflow.java:32`)
relates that project to individual `project_workflow` rows, and
`ConnectedUserProjectWorkflowConnection` (`.../domain/ConnectedUserProjectWorkflowConnection.java:32`)
records which connection each node uses.

Connections reach a connected user two ways, both already unioned in
`ConnectedUserConnectionFacadeImpl.getConnections`
(`.../facade/ConnectedUserConnectionFacadeImpl.java:72-97`): the `connection_id` of each
`IntegrationInstance` carrying that `connected_user_id`
(`.../domain/IntegrationInstance.java:32-36`, `IntegrationInstanceRepository.java:28`), and the
`connected_user_connection` relation.

### The pattern, five times — but not five of the same thing

`@SkipAutomationAuthorization` (`server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/SkipAutomationAuthorization.java:35`)
is applied to five types, and the brief's premise that each "enforces connected-user membership
itself" holds for only three of them:

| Type | Enforces what |
|---|---|
| `ConnectedUserProjectFacadeImpl:87` | Connected-user membership, on the paths that take an `externalUserId` |
| `ConnectedUserIntegrationFacadeImpl:84` | Connected-user identity + `EmbeddedPermissionEvaluator` on the integration's permission expression (`:203-204`, `:222-227`) |
| `ConnectedUserCodeWorkflowReferenceFacadeImpl:49` | Own project, plus catalog visibility via `validateCatalogWorkflowTemplateVisible` (`:122`, `:184`) |
| `ConnectedUserProjectWorkflowManager:57` | **Nothing.** It is a transactional persistence helper; its gate is its caller's |
| `AutomationWorkflowProjectFacadeImpl:60` | **Nothing.** It is the shared catalog facade; its admin gate lives in the per-controller `AutomationWorkflowProjectAdminFacade` behind `isTenantAdmin()` |

So the design generalises three enforcing facades, and must keep the two delegation helpers working
unchanged.

### The copilot precedent

`ConnectedUserProjectFacadeImpl.prepareCopilotChat` (`:451-454`) calls
`getConnectedUserProjectWorkflow(externalUserId, workflowUuid, environmentId)` purely for its throw
side-effect. That method (`:371-384`) is the chain this design generalises: own project →
`getLastProjectWorkflow(connectedUserProject.getProjectId(), workflowUuid)` (scoped to that project)
→ `getConnectedUserProjectWorkflow(cup.getId(), projectWorkflow.getId())` (throws when absent). Each
link fails closed, and `externalUserId` is supplied by the controller from
`SecurityUtils.getCurrentUserLogin()` (`.../embedded-configuration-rest-impl/.../ConnectedUserProjectWorkflowApiController.java:55,69,81`),
never from the request body.

### The edition boundary — narrower than the brief assumed

The brief states the constraint as "CE cannot import EE". The real shape is more specific and, in
one respect, more forgiving:

- The **EE** `PermissionServiceImpl` lives in `server/ee/libs/automation/automation-configuration/automation-configuration-service`.
  It is EE, but it is *automation* EE; connected users are *embedded* EE. Its `build.gradle.kts` has
  no embedded dependency, and adding one would invert the layering — `embedded-configuration-service`
  depends on `automation-configuration-api`, not the reverse
  (`.../embedded-configuration-service/build.gradle.kts:12,29`).
- `AutomationPermissionEvaluator` and `AutomationMethodSecurityExpressionRoot` — the two places where
  the skip flag is actually consulted for `hasPermission` and the SpEL built-ins — are **CE**, in
  `server/libs/automation/automation-configuration/automation-configuration-service`.
- The CE `PermissionServiceImpl` never reads `AutomationAuthorizationContext` at all. Its
  `hasResourceScope` (`.../service/PermissionServiceImpl.java:136-180`) is visibility + ownership only.

So the interface has exactly one viable home: **CE `automation-configuration-api`**, package
`com.bytechef.automation.configuration.security`, alongside `ResourceOwnershipResolver`,
`ResourceVisibilityProvider` and `ResourceEnvironmentResolver`. Both permission-service
implementations and both CE security classes can see it; `embedded-configuration-service` can
implement it.

**And the distributed-EE gap that bites `WorkflowVariablesResolver` does not exist here.** The module
that hosts the permission service and the module that would host the resolver are on exactly the same
two apps:

```
embedded-configuration-service        → server-app, ee/apps/configuration-app
automation-configuration-service (CE) → server-app, ee/apps/configuration-app
automation-configuration-service (EE) → server-app, ee/apps/configuration-app
```

An absent resolver bean therefore means one thing only: Community Edition. It never means "EE app
that happens to lack the module". *(One asymmetry, unresolved: `embedded-security-web` — the filter —
is on `server-app` only. Whether embedded traffic ever reaches `configuration-app` directly through
the API gateway is outside what I traced; see Open questions.)*

## The seam

### Interface

New file:
`server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/security/ResourceMembershipResolver.java`

```java
public interface ResourceMembershipResolver {

    enum Decision { NOT_APPLICABLE, GRANTED, DENIED }

    /**
     * Answers a resource-scoped check for the current principal, or NOT_APPLICABLE when this
     * resolver does not govern the current principal at all.
     */
    Decision resolve(Serializable id, String resourceType, String scope);

    /**
     * Whether the current principal is governed by this resolver — i.e. whether a check this
     * resolver returns NOT_APPLICABLE for (because it is workspace-scoped, or an unknown type)
     * must be DENIED rather than handed to the ordinary path.
     */
    boolean governsCurrentPrincipal();
}
```

Three-valued, and principal-driven: the resolver reads the caller from the `SecurityContext` itself
(as `EmbeddedIntegrationAuthorization.canAccessIntegration` already does on the parallel branch,
`.../embedded-configuration/embedded-configuration-service/.../security/EmbeddedIntegrationAuthorization.java:77`),
never from a method argument, so it cannot be satisfied by naming somebody else's external id.

`governsCurrentPrincipal()` is what lets the filter go away. It answers "is the caller a connected
user" without CE knowing what an `EmbeddedApiKeyAuthenticationToken` is, and it is a property of the
principal rather than of a thread-local — so it survives a `SecurityUtils.runAs` or an async hop
that the current `SkipMode` thread-local does not.

### Implementation

New file:
`server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/security/ConnectedUserResourceMembershipResolver.java`,
`@Component @ConditionalOnEEVersion`.

### Consumption

Two call sites, mirroring exactly where the skip flag is read today:

1. `AutomationPermissionEvaluator` (`server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/security/AutomationPermissionEvaluator.java:45,68`)
   — every `hasPermission(...)` in a `@PreAuthorize`.
2. EE `PermissionServiceImpl.hasResourceScope` (`server/ee/.../service/PermissionServiceImpl.java:244-288`)
   — direct `@permissionService.…` SpEL calls and the internal delegations from
   `hasWorkflowScope`/`hasWorkspaceScopeForProject`.

Injected as `ObjectProvider<ResourceMembershipResolver>` and read with `getIfAvailable()`, not as a
constructor `List<...>` — CLAUDE.md records that adding a constructor collaborator to a scanned
`@Service` breaks other modules' hand-assembled `@SpringBootTest(classes = …)` contexts, and
`ObjectProvider` is this codebase's established optional-SPI shape.

### Precedence

This ordering is load-bearing and easy to get backwards:

```
1. resolver absent (CE)                → ordinary check
2. !governsCurrentPrincipal()          → ordinary check   (admin console, background threads)
3. resolve(...) == GRANTED             → grant
4. resolve(...) == DENIED              → deny
5. resolve(...) == NOT_APPLICABLE      → deny             (governed principal, ungoverned check kind)
```

Note what is **not** in the list: `AutomationAuthorizationContext.isSkipChecks(...)`. Once a
principal is governed, `@SkipAutomationAuthorization` no longer grants it anything — which is the
whole point, and is strictly stronger than the parallel branch's monotonic narrowing, because it
does not depend on a thread-local surviving the call stack.

The worked example that forces this ordering is `ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference`
(`:109-170`): a connected user legitimately reads an **admin-owned catalog** workflow there. Had the
design kept "full skip wins", that path would work by accident and the hole would reopen. Instead the
`Workflow` resolver below is required to grant visible catalog workflows explicitly, which is both
the correct answer and the reason the filter becomes deletable.

## The resolvers

The caller is `(externalUserId, environment)` from the `SecurityContext`; `cup` is
`ConnectedUserProjectService.fetchConnectUserProject(externalUserId, environment)`. **If `cup` is
absent, every answer is `DENIED`** — a connected user with no project owns nothing.

| Type | Predicate | Answered by | Not theirs |
|---|---|---|---|
| `Project` (long) | `id == cup.getProjectId()` | `ConnectedUserProjectService.fetchConnectUserProject` | `DENIED` |
| `Workflow` (String) | the workflow's project is `cup.getProjectId()`, **or** it is a published catalog workflow this connected user may see | `ProjectService.fetchWorkflowProject(workflowId)`; catalog arm reuses `ConnectedUserCodeWorkflowReferenceFacadeImpl.validateCatalogWorkflowTemplateVisible`'s predicate (`:184`), itself `AutomationWorkflowProjectFacadeImpl.getPublishedProjects(externalUserId, environment)` + `EmbeddedPermissionEvaluator` (`:286-292`) | `DENIED` |
| `ProjectWorkflow` (long) | `ProjectWorkflowService.getProjectWorkflow(id).getProjectId() == cup.getProjectId()` | `ProjectWorkflowService` | `DENIED` |
| `Connection` (long) | `id` ∈ connections of the caller's `IntegrationInstance` rows ∪ `connected_user_connection` rows | `IntegrationInstanceService` + `ConnectedUserConnectionService.getConnectionIds`, i.e. the same union `ConnectedUserConnectionFacadeImpl.getConnections` already builds (`:72-97`) **minus** the caller-supplied `connectionIds` argument | `DENIED` |
| `Job` (long) | `JobService.fetchJob(id).getWorkflowId()` then the `Workflow` predicate above | `JobService`, mirroring `JobOwnershipResolver.resolveOwner` (`.../automation-workflow-execution-service/.../security/JobOwnershipResolver.java:57`) | `DENIED` |
| anything else | — | — | `NOT_APPLICABLE` → denied by rule 5 |

Two deliberate choices worth stating:

**`Workflow` derives from the project, not from a `ConnectedUserProjectWorkflow` row.** The row-based
form is stricter on paper, but a project holds exactly one connected user's workflows, so it grants
nothing extra — and it avoids an ordering hazard: `ConnectedUserProjectWorkflowManager.createProjectWorkflow`
(`:112-136`) creates the `ProjectWorkflow` at `:119` before the `ConnectedUserProjectWorkflow` row at
`:128`, so any Workflow-keyed check between those two lines would deny under a row-based predicate.
The existing uuid-keyed entry points keep their row check as defence in depth; this is the
`hasPermission` predicate only.

**The `Connection` predicate deliberately drops the host-declared shared set.** `sharedConnectionIds`
arrives over the client-side `EMBED_INIT` `postMessage` handshake
(`client/src/ee/pages/embedded/shared/useEmbedHandshake.ts:8`), is passed straight through to
`ConnectionApi().getConnectedUserConnections({connectionIds})`
(`client/src/ee/shared/queries/embedded/connections.queries.ts:24-28`) and is added unconditionally
by the facade (`ConnectedUserConnectionFacadeImpl.getConnections:93`). The server holds no record of
it, so it cannot be part of a server-side membership predicate without one. See Open questions — this
is the single most likely source of a production regression.

## The complete check surface

Removing skip **fails closed**, so anything missed here becomes a 403. Counts below are from a
mechanical extraction of every `@PreAuthorize` in `server/libs` and `server/ee` (excluding `/test/`
and `/generated/`), filtered to the five resource-type tokens on the embedded path.

### Trace boundary — stated explicitly

**Followed:** `client/src/ee/pages/embedded/workflow-builder/**` (`WorkflowBuilder.tsx`,
`useWorkflowBuilder.ts`, `WorkflowBuilderHeader` and its hook) → `WorkflowEditorLayout` and the
query/mutation modules its subtree imports → the REST controllers and GraphQL controllers those
APIs hit → the facades and services carrying `@PreAuthorize`. Both the standalone builder route
(`client/src/ee/workflow-builder.tsx:28`) and the Automation Hub host
(`client/src/ee/pages/embedded/automation-hub/HubBuilderView.tsx:69`), which mounts the same
`WorkflowBuilder`. The whole `@PreAuthorize` corpus was extracted mechanically, so nothing is missing
from the *inventory*; what is judged, and therefore fallible, is the reachability column.

**Not followed:** the `/api/embedded/v1/**` public API surface; the hub's own catalog screens outside
the builder; the AI-copilot agent tool surface (`showCopilot={false}` in the embedded builder, but
`prepareCopilotChat` exists and the hub may differ); the chat/voice test panels; the code-workflow
detail panels (`ProjectCodeWorkflowDetail`, `IntegrationCodeWorkflowDetail`); OAuth2 connection
flows; and anything reached only through the SDK rather than the builder UI. **Stage 1 of the
migration exists precisely because this list cannot be closed by reading.**

The security chain in scope is `EmbeddedApiKeySecurityConfigurer`
(`.../embedded-security-web-impl/.../configurer/EmbeddedApiKeySecurityConfigurer.java:37-43`):
`/api/embedded/v{n}/**`, plus `/api/(automation|embedded|platform)/internal/**` and `/graphql` when
an `Authorization` header is present. So **GraphQL is in scope**, on the same chain.

### Inventory

| Module | Sites | Reached by the builder |
|---|---|---|
| `platform-configuration` (facades + services) | 34 | **34** — 33 `Workflow`-keyed, 1 `isAuthenticated()` |
| `platform-configuration` (`EditorLogFileStorageReaderImpl`) | 3 | 3 — `Job`/`EXECUTION_VIEW`, via `EditorLogFileGraphQlController` |
| `platform-configuration` (GraphQL, `Connection`-keyed) | 5 | 5 — `ClusterElement{DynamicProperties,Field,Option}`, `WorkflowTestConfiguration` ×2 |
| `automation-configuration-service` (CE) | 42 | ~20 (see below) |
| `automation-configuration-graphql` (CE) | 2 | 2 — `ProjectWorkflowGraphQlController:115,129` |
| `automation-workflow-execution-service` | 2 | 2 — `ProjectWorkflowExecutionFacadeImpl:141,169`, `Job`/`EXECUTION_VIEW` |
| `platform-component-log` | 4 | 4 — `LogFileStorageImpl:56,66,83,89`, `Job`-keyed |
| `platform-workflow-validator` | 1 | 1 — `WorkflowValidatorFacadeImpl:120` |
| `automation-configuration-service` (EE) | 11 | 0 — sharing/git, all `isResourceOwner`/`hasResourceRole` (workspace-scoped) |
| `automation-configuration-rest` (EE) | 4 | 0 — `ProjectGitApiController` |
| **Total inventoried** | **108** | **~71** |

The 34 `platform-configuration` facade sites break down as: `WorkflowNodeDescriptionFacadeImpl` 2,
`WorkflowNodeDynamicPropertiesFacadeImpl` 2, `WorkflowNodeOutputFacadeImpl` 5,
`WorkflowNodeParameterFacadeImpl` 9 (8 `Workflow`-keyed + `getDisplayConditions` on
`isAuthenticated()`), `WorkflowNodeScriptFacadeImpl` 4, `WorkflowNodeTestOutputFacadeImpl` 5,
`WorkflowTestConfigurationFacadeImpl` 5, `WorkflowNodeTestOutputServiceImpl` 2.

The reached CE `automation-configuration-service` subset:

- `ProjectWorkflowFacadeImpl` — `addWorkflow:149` (`Project`/`WORKFLOW_CREATE`),
  `deleteWorkflow:177`, `getProjectWorkflow(long):333` (`ProjectWorkflow`),
  `getProjectWorkflow(String):344`, `getProjectWorkflows:395`, `updateWorkflow:543`,
  `updateWorkflowErrorWorkflow:565`, `duplicateWorkflow:216`
- `ProjectWorkflowServiceImpl` — `addWorkflow:53`, `delete:219`, `publishWorkflow:238`
- `ProjectFacadeImpl` — `publishProject:701`, `getProject:311`, `getProjectRow:334`,
  `getProjectVersions:540` (reached via `ConnectedUserProjectFacadeImpl.publishProjectWorkflow`)
- `ProjectServiceImpl` — `publishProject:185`, `update:211/225`
- `ProjectDeploymentServiceImpl` — `create:52`, `update:156` (`Project`/`DEPLOYMENT_PUSH`), reached
  from the publish path's `projectDeploymentFacade.createProjectDeployment` /
  `updateProjectDeployment`; those two `ProjectDeploymentFacadeImpl` overloads carry no
  `@PreAuthorize` of their own
- `WebhookTriggerTestApiFacadeImpl` — `enableTrigger:43`, `disableTrigger:49`; injected directly into
  the editor as `webhookTriggerTestApi: new WebhookTriggerTestApi()`
  (`client/src/ee/pages/embedded/workflow-builder/WorkflowBuilder.tsx:105`)

Plus the `ProjectDeploymentDTO` branch of `AutomationPermissionEvaluator.hasPermission(target,
permission)` (`ProjectDeploymentFacadeImpl:160`), reached from the same publish path. The parallel
branch's long comment there says the real fix is to authorise against the connected user's own
membership; **that is this design**, and once `ResourceMembershipResolver` governs the principal that
branch is answered by the `Project` predicate, so the comment's "do not re-litigate without that
predicate in hand" condition is satisfied.

### GraphQL

Every GraphQL controller in `platform-configuration` either carries its own `@PreAuthorize` (the 5
`Connection`-keyed ones) or delegates to a gated facade — verified for
`WorkflowNodeScriptGraphQlController`, `WorkflowNodeParameterGraphQlController`,
`WorkflowNodeTestOutputGraphQlController` and `EditorLogFileGraphQlController`. Three delegate to
**ungated** facades and are therefore not part of this surface at all: `ComponentConnectionGraphQlController`
→ `ComponentConnectionFacadeImpl`, the options path → `WorkflowNodeOptionFacadeImpl`, and
`WorkflowGraphQLController`. See §"What this does not close".

## Migration order

Deleting the filter is last. Each stage ships and reverts independently.

**Stage 0 — land the restricted mode (in flight).** `claude/embedded-permission-bypass-1051` is the
base this builds on: `SkipMode.RESOURCE_SCOPED_ONLY`, `CheckKind`, monotonic narrowing, and
`SkipMode` carried across the copilot tool hand-off rather than a boolean. Nothing here duplicates
it. *Proves safe:* that branch's own tests, plus a manual builder session.

**Stage 1 — introduce the seam in shadow mode.** Add `ResourceMembershipResolver` and
`ConnectedUserResourceMembershipResolver`. Wire both consumption points to *call* the resolver, log
the answer, and **discard it** — the restricted skip still decides. Every `DENIED` and every
governed-`NOT_APPLICABLE` is logged at WARN with resource type, id and scope.

This stage is the point of the whole sequence. The reachability column in §"The complete check
surface" is a judgement, and a wrong judgement becomes a 403. Shadow mode converts the enumeration
from something I asserted into something the running system measures. *Proves safe:* it changes no
decision. *Gate to stage 2:* a full builder session — create, edit every node type, configure test
connections, run, inspect logs, publish, and the same again inside the Automation Hub — produces zero
unexpected WARNs. Any WARN is either a resolver bug or a missing predicate; fix and repeat.

> **What actually shipped, 2026-08-25.** Stages 2–4 below were written before the flag was dropped.
> They are kept as the reasoning of the day; the paragraphs after each record what landed. The short
> version: there is no flag, Stage 3 does not exist, and Stage 4 landed partially and correctly.

**Stage 2 — make it authoritative behind a flag.** Add `bytechef.embedded.connected-user-authorization.enforce`
(default `false`; a real field on `ApplicationProperties`, per CLAUDE.md's strict-binding rule). With
the flag on, the precedence list above decides and the restricted skip is not consulted for governed
principals. *Proves safe:* stage 1's WARN count is zero over a full session, plus integration tests
asserting that a connected user is denied another connected user's `workflowId`, `connectionId`,
`projectId` and `jobId` on `testWorkflowNodeScript` specifically. *Reverts by:* config.

> **Landed without the flag.** The flag's only real value was letting one build enforce in staging
> while not enforcing in production. This product ships everything at once, so it bought nothing but
> config-revert, at the cost of a permanent second path through an authorization decision. Rules 1
> and 2 of the precedence list — resolver absent (CE), and principal not governed — do the
> containment the flag would have done, per-principal rather than per-deployment, and cannot be
> misconfigured or drift between environments. Merging is therefore enforcing.

**Stage 3 — default the flag on.** *Proves safe:* stage 2 has run enforcing in a staging tenant with
real embedded traffic. *Reverts by:* config.

> **Does not exist.** With no flag, Stage 2 *is* enforcement.

**Stage 4 — remove the scaffolding.** Delete the flag and the shadow logging. Delete
`EmbeddedAutomationAuthorizationSkipFilter` and its registration
(`EmbeddedApiKeySecurityConfigurer:50`). Delete `SkipMode.RESOURCE_SCOPED_ONLY`,
`callSkippingResourceScopedChecks`, `CheckKind`, `checkKindForResourceType` and the monotonic-narrowing
rule — with a principal-driven resolver none of them have a job left. `AutomationAuthorizationContext`
survives in its original two-state form for `@SkipAutomationAuthorization`, which after this governs
only non-connected-user principals. *Proves safe:* stages 2–3 have been enforcing for a release, and
`SkipAutomationAuthorizationAspectIntTest` plus the connected-user denial tests pass with the filter
gone.

> **Landed partially, and that is the correct end state.** The filter and its registration are gone.
> The rest of the scaffolding stays, because this paragraph's premise — that the filter is its only
> user — is false: the copilot arms restricted skip at `WorkflowEditorSpringAIAgent` and via the mode
> `CopilotToolContextUtils` carries into `RehydrateContextToolCallback`, on a `ForkJoinPool.commonPool`
> worker (`LocalAgent.runAgent` calls `CompletableFuture.runAsync` with no executor). `TenantContext`
> is a plain, non-inheritable `ThreadLocal` defaulting to `"public"`, so on that thread
> `fetchConnectedUser` queries the wrong schema, finds nothing, and — with the filter gone — would
> 403 the copilot in any multi-tenant deployment. `SkipMode.RESOURCE_SCOPED_ONLY` goes when the
> tenant reaches those threads, not before; `STATE_TENANT_ID` is already captured on the request
> thread and simply never applied there, so the fix is small and is its own ticket.
>
> Two corrections to the paragraph above, for anyone re-reading it: the shadow logging survives as
> `ResourceMembershipDenialLog` — every line it prints is now a real denial rather than a silent
> grant — and the "gate to stage 2" builder session was never run, because dropping the flag removed
> the stage boundary it gated. The reachability question it would have answered was instead settled
> by enumeration: every resource-scoped read of the skip state is textually dominated by a
> `ResourceMembershipDecider.decide(...)` call, so the filter was already inert for a governed
> principal before it was deleted.

**Only stage 4 makes the filter deletable, and only because the seam is principal-driven rather than
thread-local-driven.** A request-scoped marker would have to survive; a principal does not have to.

## What this does not close

A check that does not exist cannot be strengthened. These stay open and belong to the separate
`hasPermission` rollout, not here:

- **Running a workflow.** `WorkflowTestApiController`
  (`server/libs/platform/platform-workflow/platform-workflow-test/platform-workflow-test-rest/src/main/java/com/bytechef/platform/workflow/test/web/rest/WorkflowTestApiController.java`)
  has **no `@PreAuthorize` anywhere** — not on `startWorkflowTest:222`, `attachWorkflowTest:108` or
  `stopWorkflowTest:183`. Any authenticated principal can start, attach to, or stop a test run of any
  workflow id. This is a larger hole than the one this design closes and it is untouched by it.
- **Listing another connected user's connections.** `ConnectionApiController.getConnectedUserConnections`
  (`.../embedded-configuration-rest-impl/.../ConnectionApiController.java:78-88`) takes
  `connectedUserId` as a client-supplied parameter, carries no `@PreAuthorize`, and its facade
  carries none either.
- **Ungated `platform-configuration` facades:** `ComponentConnectionFacadeImpl`,
  `WorkflowNodeOptionFacadeImpl`, `WorkflowFacadeImpl`.
- **Workspace-scoped checks** are already handled by the parallel branch's restricted mode; this
  design changes their answer for a governed principal from "denied by mode" to "denied by
  principal", which is the same answer by a sturdier route.

## Risks and open questions

**1. Should `Job` access derive from workflow ownership or be tracked separately?**
*Recommendation: derive from the workflow.* An embedded job's workflow always belongs to the
connected user's project, `JobService.fetchJob` gives the workflow id in one query, and it mirrors
`JobOwnershipResolver`'s existing traversal so the two cannot disagree about which jobs are
reachable. A `connected_user_job` table would add a write to the hot execution path and a second
source of truth for the same fact. The cost is one extra hop per `Job` check; `EditorLogFileStorageReader`
and `ProjectWorkflowExecutionFacade` are editor-panel reads, not hot paths.

**2. Should the five `@SkipAutomationAuthorization` facades delegate to the new seam?**
*Recommendation: no — but for a different reason per group.* The two delegation helpers
(`ConnectedUserProjectWorkflowManager`, `AutomationWorkflowProjectFacadeImpl`) enforce nothing today
and should keep enforcing nothing; their gates are elsewhere. The three enforcing facades keep their
hand-rolled checks as defence in depth: they are uuid-keyed and identity-keyed, which the seam is
not, and each of them checks something the seam does not know about — integration permission
expressions, catalog visibility, workflow-uuid-to-project scoping. Collapsing them into the seam
would make the seam's contract much wider than "may this principal touch this resource id".

**3. `sharedConnectionIds` will break.** This is the highest-probability regression. Today a host app
declares shared connection ids over an unauthenticated client-side `postMessage` and the server
honours them. Under the new `Connection` predicate they are not membership, so every node configured
against a shared connection starts failing `hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')`
on the five GraphQL sites. *Recommendation:* before stage 2, give the shared set a server-side
record — the natural home is the signed embed JWT (it is already the trust anchor for the connected
user's identity) or a per-signing-key allowlist — and have the `Connection` predicate consult it.
Until that exists, stage 2 must not be enabled for any tenant using shared connections. Stage 1's
shadow logging will name them.

**4. Does embedded traffic reach `configuration-app` directly?** `embedded-security-web` is on
`server-app` only, so `configuration-app` has the permission service and the embedded modules but no
skip filter. Either embedded requests never terminate there, or they already enforce full RBAC there
today. I did not trace the API-gateway routing and cannot say which. It matters because the
principal-driven seam would start governing that path where the thread-local never did. *Recommendation:*
resolve before stage 2 by reading `server/ee/apps/api-gateway-app`'s route configuration.

**5. `getOrCreateConnectedUserProject` creates.** The membership lookup that the resolver performs
must use `fetchConnectUserProject`, never the `getOrCreate` variant
(`ConnectedUserProjectWorkflowManager:151`). An authorization predicate that creates a `Project` row
as a side effect would be a write on the read path and, worse, would let an unknown external id
provision itself a project by failing a permission check. Stated here because the surrounding code
reaches for `getOrCreate` almost everywhere.

**6. The catalog arm of the `Workflow` predicate is a behaviour widening, not a narrowing.** Under
today's skip a connected user can read any workflow, so the catalog path works by accident. Making
the catalog arm explicit means the resolver now depends on `EmbeddedPermissionEvaluator` and the
project `permissionExpression` — a second, expression-driven authorization model inside what is
otherwise a membership predicate. *Recommendation:* accept it, and reuse
`ConnectedUserCodeWorkflowReferenceFacadeImpl.validateCatalogWorkflowTemplateVisible`'s logic rather
than re-deriving it, so the two cannot drift. Flagged because it is the one place where the seam
stops being a simple ownership question.

**7. Performance.** Every resource-scoped check for a governed principal now costs at least one
lookup of `ConnectedUserProject` by (external id, environment), plus one type-specific query.
`WorkflowNodeParameterFacadeImpl` alone is hit on every keystroke-driven parameter update.
*Recommendation:* memoize the `ConnectedUserProject` lookup per request (the resolver is a singleton
reading the `SecurityContext`, so a request-scoped cache or a `ThreadLocal` cleared by a filter is
the shape); measure in stage 1, where the resolver already runs on every check with its answer
discarded — that stage is a free performance probe as well as a correctness one.

## Decisions log

- **Interface in CE `automation-configuration-api`, not EE anywhere.** Only home visible to both
  permission-service implementations and to `AutomationPermissionEvaluator`, and the only direction
  `embedded-configuration-service` already depends on.
- **Principal-driven, not thread-local-driven.** Makes the filter deletable, and survives async
  hand-offs that a `ThreadLocal` does not.
- **Three-valued `Decision` plus `governsCurrentPrincipal()`, not a boolean.** A boolean cannot
  distinguish "not my principal" from "my principal, denied"; conflating them either fails open in CE
  or fails closed for every admin request.
- **Resolver answer takes precedence over `@SkipAutomationAuthorization`.** The alternative reopens
  the hole through embedded's own facades.
- **`Workflow` membership derives from the project, not from `ConnectedUserProjectWorkflow` rows.**
  Same grant set, no create-ordering hazard.
- **Shadow mode before enforcement.** The check surface cannot be closed by reading; measure it.
