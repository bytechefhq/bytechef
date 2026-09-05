# Data Syncs Under Projects — Design

## Summary

A Data Sync becomes a member of a user-visible Project, next to that project's workflows and agents. The dedicated
**Data Syncs** and **Data Sync Deployments** pages are removed. Data syncs are reached through a **Data Syncs** tab
on every project row of the Projects page, a **Data Syncs** tab in the project sidebar, a **Data Syncs** tab on every
deployment row of the Deployments page, and a **Data Syncs** sidebar filter on both pages. With the Data Syncs row
gone, the automation sidebar's **Build** group is dissolved and **Projects** becomes an ungrouped row.

This is the second application of the pattern "Agents Under Projects"
(`docs/superpowers/specs/2026-09-21-agents-under-projects-design.md`) established. It reuses the seams that change
introduced — `ProjectWorkflowType`, the typed `ProjectWorkflowService.addWorkflow`, `ProjectPublishPreListener`,
`ProjectDeleteEventListener`, `ProjectContentContributor`, `WorkflowUpdateGuard` — and builds in from the start the
follow-ups that change needed after it landed.

## Problem

Every Data Sync is already a project. `DataSyncFacadeImpl.createDataSync` creates a hidden `__DATA_SYNC__<uuid>`
system project holding one generated workflow; the sync's publish, versions and deployments are that project's
`ProjectVersion` and `ProjectDeployment` rows. Hiding this has the costs the agents change removed for agents:

- `SystemProjects.DATA_SYNC_NAME_PREFIX` is one of the prefixes every project, deployment and search listing filters,
  and `hasAnyDeployment` exists only to work around that filter.
- The facade calls raw `ProjectService` / `ProjectWorkflowService` and replicates `ProjectFacadeImpl.publishProject`
  (`publishProjectVersion`) to bypass `ProjectFacade` gates that reject hidden projects.
- The Data Sync Deployments page duplicates Project Deployments, and `DataSyncDeploymentDTO` re-exposes the project
  deployment's tags under another name.
- A sync and the workflows that consume its destination version and deploy on separate timelines.
- `getVersionWorkflowId` assumes one workflow per project version and `getDataSyncDeployments` assumes one deployment
  per project and environment (`fetchProjectDeployment(projectId, environment)`) — both true only of a hidden project.

## Goals

- One release unit: a sync, the agents and the workflows beside it publish, deploy and sync as one project.
- One authoring surface (Projects) and one deployment surface (Deployments).
- Creating a sync stays a single action; nobody has to create a project first.
- Every lesson the agents change learned after landing is in the first cut, not a follow-up.

## Non-goals

- No change to `DataSyncWorkflowGenerator`, the fixed node names, the five-step wizard's steps, the Mapping step or
  the Test step.
- No change to what a sync can express (one source, one destination, the field-mapper processor, manual or schedule).
- No redirects from the removed routes.
- No single-sync export/import UI or GraphQL (project export/import and git carry syncs; see below).
- No MCP / Copilot / AI Hub tools for data syncs.

## Decisions

Made by the user before this spec; recorded here, not reopened.

1. Each `data_sync` row owns one generated `project_workflow` of a new type `ProjectWorkflowType.DATA_SYNC`, appended
   as INT ordinal `2`. `data_sync.project_workflow_uuid` (not null, unique, stable across versions) links them.
   `data_sync.project_id` is dropped; the project is `project_workflow.project_id` for that uuid, as for agents (the
   user rejected denormalizing it there). `data_sync` is unreleased, so its init changelog is edited in place; the
   local dev database is fixed by hand, keeping its data.
2. Hidden `__DATA_SYNC__` projects go away. Syncs live in ordinary user-visible projects, alongside workflows and
   agents. A sync is created inside a chosen project or a new one.
3. No per-sync publish. Project publish regenerates the project's syncs through a `ProjectPublishPreListener`; an
   unfinished sync blocks the project publish with an error that names the sync. Project delete deletes its syncs
   through a `ProjectDeleteEventListener`. Sync delete is refused only while the sync's own workflow is enabled in a
   deployment.
4. Data sync tags are removed; `data_sync_tag` is dropped in place. Project tags are the only tags.
5. Client: three tabs — **Workflows | Agents | Data Syncs** — on Projects page rows, in the project sidebar and on
   Deployments page rows. The Data Syncs tab is always shown. Each tab has its own create button (tab row / empty
   state). The standalone Data Syncs and Data Sync Deployments pages are removed, with no redirects, and replaced by
   a **Data Syncs** sidebar filter on Projects and Deployments. The wizard page opens inside its project with the
   project sidebar, breadcrumb, `ProjectItemSelect` (gaining a Data Syncs group) and a settings menu with a Data Sync
   tab (edit, delete). Delete confirms with a dialog. Sidebar sync rows show "Edited <date>".
6. **Run now** lives only on the sync's row on the Deployments page. It keeps `runDataSyncDeployment`'s semantics — not
   `@Transactional` because the delegate is `Propagation.NEVER`; `DEPLOYMENT_NOT_OWNED`, `DEPLOYMENT_DISABLED` — with
   ownership redefined as "the deployment's project contains this sync's workflow".
7. From day one: deployment lookups handle several deployments per project and environment; duplicate,
   export/import and git sync carry data syncs through the content-contributor SPI; the workflow update guard refuses
   edits to `DATA_SYNC` workflows; every listing that excludes `AI_AGENT` excludes `DATA_SYNC` through one predicate;
   the project-workflow facade's `validateNotAiAgentWorkflow` is generalized.
8. `developmentOnlyRoutes.ts` stays the single source for Development-only surfaces (CLAUDE.md, "Sidebar navigation
   groups").
9. The automation sidebar's **Build** group is removed. It holds Projects and Data Syncs today; with Data Syncs gone,
   Projects becomes a plain ungrouped row in the group's former position, with the same icon and route, still
   Development-only. The embedded Build group is untouched.
10. Process: design → plan → subagent implementation in this worktree, landed by fast-forward.

## Current state

### Server — `server/libs/automation/automation-data-sync/`

| File | Today | Change |
|------|-------|--------|
| `…-api/…/domain/DataSync.java` | `projectId` (`long`), `dataSyncTags` (`@MappedCollection`), `getTagIds` | Drop `projectId` and tags; add `projectWorkflowUuid` (`UUID`) |
| `…-api/…/domain/DataSyncTag.java` | tag join entity | Delete |
| `…-api/…/domain/DataSyncElement.java` | element row | Unchanged |
| `…-api/…/dto/DataSyncDTO.java` | carries tags, visibility, draft id | Add `projectId` (resolved) and `projectWorkflowUuid`; drop tags |
| `…-api/…/dto/DataSyncDeploymentDTO.java` | per hidden-project deployment, with tags | One row per deployment deploying the sync's workflow; drop tags |
| `…-api/…/dto/DataSyncVersionDTO.java` | project versions | Unchanged (project versions, as `aiAgentVersions`) |
| `…-api/…/exception/DataSyncErrorType.java` | typed errors | Keep all; `DATA_SYNC_HAS_DEPLOYMENTS` now means "enabled in a deployment" |
| `…-api/…/facade/DataSyncFacade.java` | 16 methods | See "Facade surface" |
| `…-api/…/service/DataSyncService.java` | CRUD + `update(id, tagIds)` | Drop the tag overload; add `getProjectId`, `getProjectIds`, `getProjectDataSyncs` |
| `…-service/…/facade/DataSyncFacadeImpl.java` | hidden-project lifecycle, replicated publish | Rewritten onto real projects (below) |
| `…-service/…/repository/DataSyncRepository.java` | `findByProjectId`, `findByWorkspaceId` | Replace `findByProjectId` with the `project_workflow` subquery forms `AiAgentRepository` uses |
| `…-service/…/security/DataSyncOwnershipResolver.java` | `data_sync.workspace_id` | Unchanged |
| `…-service/…/security/DataSyncVisibilityProvider.java` | `DataSync::getProjectId` → project | Resolve the project through `project_workflow_uuid` |
| `…-service/…/security/DataSyncElement{Ownership,Visibility}*.java` | via parent sync | Follow the parent's new resolution |
| `…-service/…/util/DataSyncWorkflowGenerator.java` | draft generator | Unchanged |
| `…-service/…/liquibase/…/00000000000001_automation_data_sync_init.xml` | `project_id` FK, `data_sync_tag` | Edited in place (see Schema) |
| `…-graphql/…/DataSyncGraphQlController.java`, `graphql/data-sync.graphqls` | 6 queries, 10 mutations | See "GraphQL" |
| new `…-service/…/event/DataSyncProject{PublishPreListener,DeleteEventListener,ContentContributor}.java` | — | New, mirroring the `AiAgentProject*` trio |

### Server — outside the module

- `automation-configuration-api/…/domain/ProjectWorkflowType.java` — `WORKFLOW, AI_AGENT`. Gains `DATA_SYNC` and the
  predicate below.
- `automation-configuration-api/…/domain/SystemProjects.java` — `DATA_SYNC_NAME_PREFIX` deleted and removed from
  `NAME_PREFIXES`; class Javadoc ("a uuid for Data Sync") updated. Also the Javadoc mentions in
  `ProjectFacade.getProjectRow` and `ProjectWorkflowFacadeImpl.deleteProjectDeploymentWorkflows`.
- `automation-configuration-service/…/workflow/AiAgentWorkflowUpdateGuard.java` — generalized (below).
- `automation-configuration-service/…/facade/ProjectWorkflowFacadeImpl.java` — `validateNotAiAgentWorkflow` (four
  call sites) generalized; the two `== WORKFLOW` listing filters use the predicate.
- `== WORKFLOW` / `== AI_AGENT` sites switched to the predicate: `ProjectFacadeImpl` (three),
  `ProjectDeploymentFacadeImpl` (chat workflows), `SubflowDataSourceImpl`, `ProjectSearchAssetProvider`,
  `WorkflowSearchAssetProvider`, `ProjectWorkflowGraphQlController`, `McpProjectWorkflowGraphQlController`, EE
  `ProjectGitFacadeImpl`.
- EE `automation-configuration-service/…/security/scope/DataSyncPermissionScope{,Provider}.java` — unchanged; every
  scope is kept (see Permissions).
- EE `ResourceEnvironmentResolverCoverageTest` — its DataSync justification names `hasAnyDeployment`; reword.

### Client

| Path | Today | Change |
|------|-------|--------|
| `shared/navigation/navigationItems.ts` | Build group = Projects + Data Syncs; Deploy has Data Syncs | Build group removed for automation; Projects ungrouped; both Data Syncs rows removed |
| `shared/navigation/developmentOnlyRoutes.ts` | `/automation/data-syncs → /automation/data-sync-deployments` | Entry removed; syncs are covered by `/automation/projects` |
| `routes.tsx` | `data-syncs`, `data-syncs/:dataSyncId`, `data-sync-deployments` | Removed; new `projects/:projectId/data-syncs/:dataSyncId` |
| `pages/automation/data-syncs/DataSyncs.tsx`, `DataSyncDetail.tsx` (+ tests) | list and detail pages | Delete; detail becomes `pages/automation/project/ProjectDataSync.tsx` |
| `pages/automation/data-syncs/components/data-sync-list/*`, `DataSyncsLeftSidebarNav.tsx`, `DataSyncsFilterTitle.tsx` | list page parts | Delete |
| `pages/automation/data-syncs/components/detail/DataSyncDetailHeader.tsx` | own Publish, per-sync versions, delete | Rebuilt on `AgentDetailHeader`'s shape (below) |
| `pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu.tsx`, `DataSyncDialog.tsx` | row menu, create/edit dialog | Kept; dialog gains a project select; menu gains a delete confirmation |
| `pages/automation/data-syncs/components/wizard/*`, `hooks/*`, `utils/*` | wizard | Kept unchanged except query invalidation |
| `pages/automation/data-sync-deployments/**` | deployments page | Delete; Run now moves to a Deployments-page row |
| `graphql/automation/data-sync/*.graphql` | 16 operations | 5 deleted, 3 changed (see "GraphQL client") |
| `project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogBasicStep.tsx` | `agentOptions` / `DeployableAgentI` entity picker | Deleted — its only caller is `DataSyncDeployments.tsx` |

## Design

### Model

- `ProjectWorkflowType` becomes `WORKFLOW, AI_AGENT, DATA_SYNC` — `DATA_SYNC` appended (ordinal `2`), never
  reordered. `project_workflow.type` is a plain `INT` with no check constraint
  (`20260921100000_automation_configuration_added_project_workflow_type.xml`), and `ProjectWorkflow.getType()` reads
  it through `toEnum(ProjectWorkflowType.values(), …)`, so the new value needs no schema change.
- **The generated-type predicate.** `ProjectWorkflowType` gains:

  ```java
  /** Whether rows of this type are generated from a feature's own configuration and edited only through it. */
  public boolean isGenerated() {
      return this != WORKFLOW;
  }
  ```

  Every site that today tests `== WORKFLOW` (to list user workflows) or `== AI_AGENT` (to skip agent workflows) tests
  `!isGenerated()` / `isGenerated()` instead, so a future generated type needs no hunt through listings. A grep for
  `ProjectWorkflowType.AI_AGENT` outside the agent module and the guard's message table must come back empty.
- `data_sync.project_workflow_uuid` (`${uuid_type}`, not null, unique `uk_data_sync_project_workflow_uuid`). The
  project is resolved with the queries `AiAgentRepository` already uses, copied into `DataSyncRepository`:
  `findAllByProjectId` (`project_workflow_uuid IN (SELECT uuid FROM project_workflow WHERE project_id = :projectId)`),
  `findProjectIdByProjectWorkflowUuid`, and the batch `findProjectIdsByProjectWorkflowUuids`.
- The sync's workflow in a given version is `ProjectWorkflowService.fetchProjectWorkflow(projectId, projectVersion,
  uuid)`; `getVersionWorkflowId(projectId, projectVersion)` and its one-workflow-per-version assumption are deleted.
- `data_sync.workspace_id` stays and must equal the project's workspace (enforced at create/import).
- `data_sync.uuid` stays; it no longer names a project but remains the sync's stable identity.

### Schema

- **Unreleased, verified.** `git ls-tree -r --name-only v0.33.2 | grep -c automation-data-sync` returns `0` (v0.33.2
  is the latest release tag). The init changelog was introduced by `84987d260d7` ("Add the Data Sync domain, schema,
  repositories and services"), and `git merge-base --is-ancestor 84987d260d7 master` exits `1` for `master`,
  `origin/master` and `upstream/master`. The table has never shipped, so its init changelog is edited in place.
- `00000000000001_automation_data_sync_init.xml`, edited in place:
  - `data_sync`: remove `project_id` and `fk_data_sync_project`; add `project_workflow_uuid` (`${uuid_type}`, not null,
    unique `uk_data_sync_project_workflow_uuid`).
  - Remove `data_sync_tag` — its `createTable`, primary key, both foreign keys and its `dropTable` in `<rollback>`.
  - `data_sync_element` is unchanged.
- No changeset is added anywhere: `project_workflow.type` already exists.
- The in-place edit changes the changeset's checksum. Other worktrees that still carry the old init changelog will
  clash with a database migrated from this one; that is the known cost of in-place edits, not something to solve here.

### Server behaviour

#### Create

`createDataSync(title, description, workspaceId, @Nullable Long projectId)`:

- `projectId` absent → create an ordinary project named after the title, suffixing ` (2)`, ` (3)`… on a collision in
  the workspace — `AiAgentFacadeImpl.createAgentProject` / `uniqueProjectName`, lifted into a shared helper only if the
  plan finds a natural home; otherwise copied. No prefix, and the description is the sync's own.
- `projectId` present → the project must be in the workspace, not a system project and visible to the caller
  (`AiAgentFacadeImpl.getWorkspaceProject`); otherwise `IllegalArgumentException`.
- Add a placeholder workflow with `ProjectWorkflowService.addWorkflow(projectId, lastVersion, workflowId,
  ProjectWorkflowType.DATA_SYNC)`, store its uuid on the row, save the row, then regenerate the definition from the
  saved row — the ordering `createAgent` uses, because the uuid column is not null and the definition's
  `metadata.dataSyncId` needs the row id.

#### Permissions

- The `DATA_SYNC_VIEW` / `DATA_SYNC_CREATE` / `DATA_SYNC_EDIT` / `DATA_SYNC_DELETE` / `DATA_SYNC_PUBLISH` scopes all stay
  and keep their ranks (VIEWER / EDITOR ×4). None collapses into `WORKFLOW_*`, so a custom role can still grant data
  sync access without workflow access or the reverse.
- Adding a sync to a project also passes that project's `WORKFLOW_CREATE` gate, which `ProjectWorkflowService.addWorkflow`
  carries, exactly as adding an agent does.
- `DATA_SYNC_PUBLISH` keeps a meaning without a per-sync publish: it gates `prepareProjectPublish(projectId)`
  (`hasPermission(#projectId, 'Project', 'DATA_SYNC_PUBLISH')`). The pre-listener calls it only for projects that
  contain syncs, so publishing a project with syncs needs `DEPLOYMENT_PUSH` (on `ProjectService.publishProject`) plus
  `DATA_SYNC_PUBLISH`, and publishing any other project is unaffected. The agent counterpart uses `WORKFLOW_EDIT`
  because agents never had a publish scope.
- New lifecycle methods and their gates: `deleteProjectDataSyncs(projectId)` — `PROJECT_DELETE` on `Project` (as
  `deleteProjectAgents`); `copyProjectDataSyncs(source, target, workspaceId)` and `importDataSync(workspaceId, json,
  projectId)` — `DATA_SYNC_CREATE` on `Workspace`; `exportDataSync(id)` — `DATA_SYNC_VIEW` by id;
  `updateDataSyncFromExport(id, json)` — `DATA_SYNC_EDIT` by id.
- Visibility stays derived from the project (`DataSyncVisibilityProvider` returns the project's record under
  `"Project"`); only the traversal changes. There is no per-sync visibility.
- `runDataSyncDeployment` keeps `DATA_SYNC_EDIT` by id.

#### Facade surface

`DataSyncFacade` after the change (17 methods, each pinned by `DataSyncFacadeAuthorizationTest`):
`createDataSync` (gains `Long projectId`), `updateDataSync`, `deleteDataSync`, `getDataSync`, `getDataSyncs`,
`updateDataSyncTrigger`, `setDataSyncElement`, `updateDataSyncElement`, `getDataSyncVersions`, `getDataSyncDeployments`,
`runDataSyncDeployment`, `prepareProjectPublish`, `deleteProjectDataSyncs`, `copyProjectDataSyncs`, `exportDataSync`,
`importDataSync`, `updateDataSyncFromExport`.

Removed: `publishDataSync`, `getDataSyncTags`, `updateDataSyncTags`, `getDataSyncDeploymentTags`,
`updateDataSyncDeploymentTags`, and the private `publishProjectVersion` / `hasAnyDeployment` / `getVersionWorkflowId`.

#### Listings

Generated workflows stay out of every list of user workflows, through `isGenerated()` (see Model):
`ProjectWorkflowFacadeImpl.getProjectWorkflows()` and `getProjectWorkflows(projectId)`,
`ProjectFacadeImpl.getWorkspaceLatestProjectWorkflows`, `getOrdinaryProjectWorkflows` (used by duplicate and export)
and the `includeAllFields` project listing, `ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows`,
`SubflowDataSourceImpl.getSubWorkflows` (call-workflow options), both search asset providers,
`ProjectWorkflowGraphQlController`, `McpProjectWorkflowGraphQlController` (MCP pickers) and EE `ProjectGitFacadeImpl`.
`ProjectWorkflowFacadeImpl.getProjectVersionWorkflows` stays unfiltered: it feeds the deployment dialog, which must
see generated workflows to deploy them — a sync's workflow appears there labelled with the sync's title.

`ProjectFacadeImpl`'s `projectWorkflowIds` therefore counts user workflows only; the client counts syncs from
`dataSyncs`, as it counts agents from `aiAgents`.

#### Edit guard

`AiAgentWorkflowUpdateGuard` is generalized in place rather than given a sibling: it lives in
`automation-configuration-service`, already reads the `project_workflow` row, and one guard means one lookup per
editor write. It is renamed `GeneratedProjectWorkflowUpdateGuard` and refuses any `isGenerated()` row:

```java
projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId)
    .filter(projectWorkflow -> projectWorkflow.getType().isGenerated())
    .ifPresent(projectWorkflow -> {
        throw new IllegalArgumentException(generatedWorkflowMessage(workflowId, projectWorkflow.getType()));
    });
```

The message names the owner — "Workflow %s is generated by an AI agent; edit it through the agent" / "… generated by
a Data Sync; edit it through the Data Sync". `ProjectWorkflowFacadeImpl.validateNotAiAgentWorkflow` becomes
`validateNotGeneratedWorkflow` with the same test and message, sharing one message helper with the guard. The guard
covers `WorkflowNodeParameterFacadeImpl` (including the `/internal` node-parameter endpoint) and the EE embedded
`AutomationWorkflowProjectFacadeImpl`; the facade check covers the project-workflow update, delete, duplicate and
the remaining by-id write.

#### Publish

- `publishDataSync` and its GraphQL mutation are removed. Publishing the project publishes its syncs.
- `DataSyncProjectPublishPreListener implements ProjectPublishPreListener`, `@Lazy DataSyncFacade`: returns at once
  when `dataSyncService.getProjectDataSyncs(projectId)` is empty; otherwise calls
  `dataSyncFacade.prepareProjectPublish(projectId)`, which runs `validateForPublish` and then
  `regenerateAndSaveWorkflow` for each sync.
- Validation messages name the sync by title — "Data Sync 'Orders to warehouse' has no source.", "… has no
  destination.", "Element SOURCE of Data Sync '…' needs a connection.", "Data Sync '…' is scheduled but has no cron
  expression." — with the existing `DataSyncErrorType` codes, so the global fetch interceptor's toast tells the user
  which sync blocked the publish, whichever surface published (project header, sync header, workflow editor, MCP
  `publishProject`).
- The listener regenerates definitions **in place** and never adds or removes `project_workflow` rows:
  `ProjectFacadeImpl.publishProject` snapshots the draft's workflow list before `ProjectService.publishProject` runs
  the listeners, and duplicates exactly that list.
- `getDataSyncVersions` lists the project's versions, as `getAgentVersions` does. A sync added after the project's
  last publish has no row in that version and reports `lastPublishedVersion = 0`, `unpublishedChanges = true`
  (`toAgentDTO`'s rule).

#### Deployments and Run now

- **Several deployments per project and environment, everywhere.** No data sync code calls
  `fetchProjectDeployment(projectId, environment)`. Deployment reads go through
  `projectDeploymentService.getAllProjectDeployments(projectId)` (the unfiltered read) and pick out the sync's row with
  `projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, uuid)` — the
  `AiAgentFacadeImpl.projectDeployments` / `agentDeploymentWorkflows` pair, copied.
- `getDataSyncDeployments(workspaceId)` returns one `DataSyncDeploymentDTO` per (visible sync, deployment of its project
  whose version contains the sync's workflow and has a `project_deployment_workflow` row for it). Fields: deployment
  `id`, `name`, `dataSyncId`, `dataSyncTitle`, `projectId`, `environmentId`, `enabled` (the deployment's own flag),
  `projectVersion`, `workflowId`, `triggerType`, `lastExecutionDate`. `tags` is dropped. `triggerType` is read from
  the **deployed** workflow's trigger (`schedule/v1/cron` → `SCHEDULE`, otherwise `MANUAL`), not from the current row,
  so the Scheduled filter follows what is deployed — the reasoning the agents filter applies to deployed triggers.
- `runDataSyncDeployment(id, projectDeploymentId)`, still not `@Transactional`:
  1. `workflowId = fetchProjectWorkflowWorkflowId(projectDeploymentId, sync.projectWorkflowUuid)`; empty →
     `DEPLOYMENT_NOT_OWNED` ("Deployment N does not deploy Data Sync 'title'"). This is the new ownership test: the
     deployment's project, in the deployed version, contains this sync's workflow. It also rejects a deployment of the
     right project pinned to a version from before the sync existed.
  2. Deployment disabled, **or** the sync's own `project_deployment_workflow` row disabled → `DEPLOYMENT_DISABLED`.
     The second half is new: in a multi-workflow project the per-workflow switch is how a sync is turned off, and
     running a switched-off sync by hand would contradict it.
  3. `projectDeploymentFacade.createProjectDeploymentWorkflowJob(projectDeploymentId, workflowId)`.
- Per-sync enablement is `ProjectDeploymentWorkflow.enabled` on the sync's workflow, toggled through the existing
  REST mutation. Deployment tags are the project deployment's own.

#### Delete

- `deleteDataSync(id)`: refused with `DATA_SYNC_HAS_DEPLOYMENTS` only while the sync's workflow is enabled in any
  deployment of its project (every environment, every deployment — `isEnabledInAnyDeployment`, copied from the
  agent facade). Otherwise it deletes the row (elements cascade), then for every project version the sync's
  `project_workflow` row, the `WorkflowPreDeleteListener`s, every `project_deployment_workflow` row for that workflow
  (a disabled row outlives the refusal), the workflow, and finally its test configurations — `deleteAgent`'s body.
  The project is never deleted.
- `DataSyncProjectDeleteEventListener implements ProjectDeleteEventListener` (`@Lazy DataSyncFacade`) calls
  `deleteProjectDataSyncs(projectId)`, which deletes the project's `data_sync` rows; `ProjectFacadeImpl.deleteProject`
  then removes deployments and workflows as for any project. Nothing references a sync from outside its project, so
  there is no refusal case (unlike sub-agents).

#### Duplicate, export/import, git

`DataSyncProjectContentContributor implements ProjectContentContributor`, `@Lazy` on its facade/service/project
collaborators for the reason `AiAgentProjectContentContributor` documents.

- **Content directory** `data-syncs/`; one file per sync, `data-syncs/<name>.json`, where `<name>` is the slug
  (`^[a-z0-9_-]{1,64}$`, so always a safe file name). Only top-level `.json` entries are read.
- **Document** (`exportDataSync`, pretty-printed, enums by name, element order SOURCE, DESTINATION, PROCESSOR):

  ```json
  {
    "exportVersion": 1,
    "name": "orders-to-warehouse",
    "title": "Orders to warehouse",
    "description": null,
    "triggerType": "SCHEDULE",
    "triggerParameters": {"frequencyKind": "daily", "timeOfDay": "06:00", "expression": "0 0 6 * * ?", "timezone": "UTC"},
    "elements": [
      {"kind": "SOURCE", "componentName": "salesforce", "componentVersion": 1, "operationName": "…", "parameters": {}},
      {"kind": "DESTINATION", "componentName": "postgresql", "componentVersion": 1, "operationName": "…", "parameters": {}},
      {"kind": "PROCESSOR", "componentName": "dataStreamProcessor", "componentVersion": 1, "operationName": "fieldMapper", "parameters": {"mappings": []}}
    ]
  }
  ```

  The generated workflow is not exported: it is derived, and import regenerates it.
- **Connections** are never exported — a connection id means nothing in another workspace, and the agent document
  omits them for the same reason. Import leaves each element's `connection_id` null, so an imported sync whose
  components need a connection blocks the project publish (naming the sync) until one is picked — the same step a
  workflow's deployment connections need after import.
- **Duplicate** (`onProjectDuplicated` → `copyProjectDataSyncs`) stays in the workspace, so it copies everything,
  connections included, into new rows with new names (`uniqueName`), new generated workflows in the duplicate's draft
  version and freshly synced test connections.
- **Import** (`importProjectContent`) calls `importDataSync(workspaceId, json, projectId)` per file: the exported name
  seeds `uniqueName`, the processor is still validated as the field mapper, elements are created, the draft generated.
- **Git pull** (`pullProjectContent`) matches each file to a project sync by name, then by a unique title (the
  agent contributor's `findAgent` rule) and applies it through `updateDataSyncFromExport`: title, description and
  trigger replaced; each element upserted by kind; an element whose component, version and operation are unchanged
  keeps its current `connection_id`, a changed one gets null; a kind absent from the file is deleted (so a pulled
  sync with no processor drops the local mapping). Unmatched files are imported; syncs without a file are left
  alone, as workflows missing from the repository are. Git push needs nothing new — EE `ProjectGitFacadeImpl` already
  writes every contributor's files and excludes generated workflows.

#### GraphQL

`data-sync.graphqls`:

- `DataSync`: add `projectWorkflowUuid: String!`; `projectId: ID!` stays, now resolved; drop `tags`.
- `DataSyncDeployment`: drop `tags`; `triggerType` is the deployed trigger (see above).
- `CreateDataSyncInput`: add `projectId: ID`.
- Remove `dataSyncTags`, `dataSyncDeploymentTags`, `publishDataSync`, `updateDataSyncTags`,
  `updateDataSyncDeploymentTags` and the inputs only they used.
- Kept unchanged: `dataSync`, `dataSyncs`, `dataSyncDeployments`, `dataSyncVersions`, `updateDataSync`,
  `updateDataSyncTrigger`, `setDataSyncElement`, `updateDataSyncElement`, `deleteDataSync`, `runDataSyncDeployment`.

Deployments are created and toggled through the existing project deployment REST API, now on an ordinary project.

### Client

#### Navigation

- `navigationItems.ts` — automation: the `{group: 'Build'}` entries go; `Projects` (`/automation/projects`,
  `FolderIcon`) stays at the same position (after Approval Tasks, before Connections) with no `group`. The Data Syncs
  row (`/automation/data-syncs`) and the Deploy group's Data Syncs row (`/automation/data-sync-deployments`) are
  deleted, and `ArrowLeftRightIcon` / `RefreshCwIcon` imports go if nothing else uses them. The embedded Build group
  stays, so `NAVIGATION_GROUP_ICONS.Build` (`HammerIcon`) stays.
- Projects keeps being hidden outside DEVELOPMENT: `App.tsx` hides rows by `isDevelopmentOnlyHref`, which is keyed by
  href, not group. A one-item group would still have rendered a collapsible labelled "Build" when expanded (CLAUDE.md:
  a labeled group of one keeps its section), which is why it becomes a plain row rather than a group of one.
- `developmentOnlyRoutes.ts` — delete `{fallbackHref: '/automation/data-sync-deployments', href:
  '/automation/data-syncs'}`. The sync page lives at `/automation/projects/:projectId/data-syncs/:dataSyncId` and the
  Data Syncs filter at `/automation/projects?dataSyncs=…`, both covered by the `/automation/projects` →
  `/automation/deployments` entry. `developmentOnlyRoutes.test.ts` gains
  `['/automation/projects/12/data-syncs/34', '/automation/deployments']` in its detail-route table.
- `AppSidebar.test.tsx` uses its own fixture and asserts no real group; no navigation test names the automation Build
  group. The plan re-greps for `'Build'` in tests before claiming that.

#### Routes

- Removed: `data-syncs`, `data-syncs/:dataSyncId`, `data-sync-deployments` and the three lazy imports.
- Added: `projects/:projectId/data-syncs/:dataSyncId` → `pages/automation/project/ProjectDataSync.tsx`.
- `pages/automation/data-syncs/utils/getDataSyncPath.ts` — `/automation/projects/${projectId}/data-syncs/${id}`,
  the `getAgentPath` twin, used by every link to a sync.

#### Projects page

- `ProjectList.tsx`: `ProjectListTabType` becomes `'agents' | 'dataSyncs' | 'workflows'`; the tab strip reads
  **Workflows (N) | Agents (M) | Data Syncs (K)**, K counted from `useDataSyncs()` grouped by `projectId`. The tab-row
  create button follows the agents rule: shown only while the active tab lists something.
- New `projects/components/project-data-sync-list/`: `ProjectDataSyncList` (empty state: `ArrowLeftRightIcon`
  size-24, "No data syncs in this project", `ProjectDataSyncCreationActions placement="emptyState"`),
  `ProjectDataSyncListItem` (title links to `getDataSyncPath`, a "Source → Destination" component line, trigger
  summary via `describeTrigger`, published version, `DataSyncsLeftSidebarDropdownMenu`), `ProjectDataSyncCreationActions`
  (`placement: 'emptyState' | 'tabRow'`, a single **New Data Sync** / **Create Data Sync** button — there is no
  single-sync import, so no dropdown). Rows keep the shared list-row rhythm (`min-h-8`, 8px gap, `min-h-7`).
- `ProjectListItem.tsx` computes the sync count beside the agent count where it shows counts.
- **Data Syncs filter.** `pages/automation/data-syncs/components/DataSyncsFilterLeftSidebarNav.tsx`, built on
  `LeftSidebarFilterNav` like `AgentsFilterLeftSidebarNav`: title "Data Syncs", items **All Data Syncs** and
  **Scheduled**, search param `?dataSyncs=all|scheduled` read by `getDataSyncsFilter`. The Agents and Data Syncs
  filters are mutually exclusive — each nav's links delete the other's param — because each picks the tab every row
  opens on and a row opens on one tab. On Projects, the filter keeps projects holding a sync (Scheduled: a sync whose
  current `triggerType` is `SCHEDULE`) and opens rows on the Data Syncs tab; `ProjectsFilterTitle` and
  `preservedSearchParams` carry whichever content filter is active. With the filter on and no matching project, the
  empty state offers **Create Data Sync**, opening `DataSyncDialog` unlocked (project select defaulting to a new
  project).

#### Project sidebar

- `ProjectsLeftSidebar` gains `currentDataSyncId`; tabs **Workflows (N) | Agents (M) | Data Syncs (K)**, the active tab
  initialised from whichever current id is set. The Data Syncs tab has a full-width **New Data Sync** button (locked
  to the browsed project, falling back to the page's project, as for agents) above the list.
- New `projects-sidebar/components/ProjectDataSyncsList.tsx`, shaped like `ProjectAgentsList`: a badge row with the
  source and destination component icons (the counterpart of the agent row's channel badges and the workflow row's
  component icons), the title, **Edited <date>**, and `DataSyncsLeftSidebarDropdownMenu`. "Edited" reads
  `DataSync.lastModifiedDate`, which the server fills from the draft workflow's `lastModifiedDate` (falling back to the
  row's): element edits never touch the `data_sync` row, but every mutation regenerates the draft, so the workflow's
  date is the one that moves — and it is also what workflow rows show.
- **Icon rule**, applied consistently: rows in the Projects page and Deployments page tab lists carry a leading
  size-4 `text-content-neutral-secondary` type icon — `WorkflowIcon`, `BotIcon`, and `ArrowLeftRightIcon` for syncs
  (the retired nav row's icon). Sidebar rows carry no leading type icon, as sidebar agent rows do not; their badge row
  identifies them. `ProjectItemSelect` uses the same three icons for its three groups.

#### Data sync page

- `pages/automation/project/ProjectDataSync.tsx`, the `ProjectAgent.tsx` shell: 355px `ProjectsLeftSidebar`
  (`currentDataSyncId`), then a header and `DataSyncWizard` full width below it. No test panel — the wizard's own Test
  step stays the test surface.
- `DataSyncDetailHeader`, rebuilt on `AgentDetailHeader`: `leading` = `LeftSidebarButton` + `ProjectBreadcrumb` whose
  `itemSelect` is `ProjectItemSelect` with `currentDataSyncId` and the sync's title; **Publish** (`PublishPopover` →
  `usePublishProjectMutation`, publishing the project) and **Deploy** (`ProjectDeploymentDialog` for the project) as a
  segmented group; the version pill and history sheet from `dataSyncVersions`; `SettingsMenu` with
  `firstTab={{ariaLabel: 'Data Sync tab', label: 'Data Sync', value: 'dataSync', content}}` holding **Edit** (opens
  `DataSyncDialog`) and **Delete**, through a new `useDataSyncActions` hook (the `useAgentActions` twin). No Run now.
- `ProjectItemSelect` gains a **Data Syncs** group listing the project's syncs (from `useDataSyncs`) and a
  `currentDataSyncId` prop. Its radio values become typed — `workflow:<projectWorkflowId>`, `agent:<id>`,
  `dataSync:<id>` — because agent ids, sync ids and project-workflow ids come from separate sequences that all start
  at 1050, and today a workflow is matched first, so an agent whose id equals a workflow's `projectWorkflowId` is
  unreachable from the switcher. A third id space makes that likelier; prefixing removes it.
- **Delete confirmation.** New `DeleteDataSyncAlertDialog` (the `DeleteAgentAlertDialog` twin, "This will permanently
  delete the data sync <title>."), used by every delete entry point: the sidebar row menu (shared with the Projects
  page row) and the page's Data Sync settings tab. After deleting the sync the page is standing on, navigate to the
  project's first workflow, else to `/automation/projects`.
- `DataSyncDialog` gains the project select `AgentDialog` has (hidden when opened with a locked `projectId`; default
  "New project named after this data sync"); on success it navigates to `getDataSyncPath` and invalidates project
  queries when a project was created. `invalidateDataSyncQueries` additionally invalidates `ProjectKeys` on create and
  delete.

#### Deployments page

- `ProjectDeploymentList.tsx`: `ProjectDeploymentListTabType` gains `'dataSyncs'`; tabs **Workflows | Agents | Data
  Syncs (K)**, K from `useDataSyncDeployments()` (`dataSyncDeployments` query) grouped by project deployment id. The
  Workflows tab additionally leaves out the syncs' workflows, recognised by `workflowUuid` against the syncs'
  `projectWorkflowUuid` set, beside the existing agent exclusion. `ProjectDeploymentListItem`'s counter reads
  "N workflows · M agents · K data syncs".
- New `project-deployments/components/project-deployment-data-sync-list/`: `ProjectDeploymentDataSyncList` (empty:
  "This deployment has no data syncs.") and `ProjectDeploymentDataSyncListItem` — `ArrowLeftRightIcon`, title, trigger
  summary, the enable `Switch` on the sync's `ProjectDeploymentWorkflow` (`useEnableProjectDeploymentWorkflowMutation`,
  as the agent row does), last execution, and **Run now** (`PlayIcon` button → `runDataSyncDeployment`), enabled only
  while both the deployment and the sync's row are enabled.
- The Data Syncs filter on Deployments keeps deployments that deploy a sync (Scheduled: `triggerType === SCHEDULE`, the
  deployed trigger) and opens rows on the Data Syncs tab. `ProjectDeploymentFilterTitle` shows it.

#### GraphQL client

- Deleted: `dataSyncTags.graphql`, `dataSyncDeploymentTags.graphql`, `publishDataSync.graphql`,
  `updateDataSyncTags.graphql`, `updateDataSyncDeploymentTags.graphql`.
- Changed: `createDataSync.graphql` (`projectId`), `dataSync.graphql` / `dataSyncs.graphql` (`projectWorkflowUuid`, no
  `tags`), `dataSyncDeployments.graphql` (no `tags`).
- Regenerate `src/shared/middleware/graphql.ts` with `npx graphql-codegen`; operations and generated file are committed
  separately.

#### Removed

The Data Syncs and Data Sync Deployments nav rows, the automation Build group, the three routes,
`pages/automation/data-sync-deployments/`, `DataSyncs.tsx`, `DataSyncDetail.tsx`, `data-sync-list/`,
`DataSyncsLeftSidebarNav`, `DataSyncsFilterTitle`, the tag UI, and `ProjectDeploymentDialogBasicStep`'s
`agentOptions` / `agentOptionsLabel` / `DeployableAgentI` path. `pages/automation/data-syncs/` stays as the home of
the dialog, row menu, wizard, hooks and utils.

### Documentation

- `.agents/data-sync.md`: entity model (`project_workflow_uuid`, no `project_id`, no tags), placement in ordinary
  projects, publish via the pre-listener and the `DATA_SYNC_PUBLISH` gate, delete rules, the contributor file format,
  Run now's ownership rule, the new client surfaces.
- `.agents/agents.md`: the client section's two-tab wording becomes three tabs.
- `docs/superpowers/specs/2026-09-05-data-sync-design.md`: a one-line note at the top pointing here for placement,
  publish, deployments and tags.
- `CLAUDE.md`, "Sidebar navigation groups":
  - "Current groups: automation Build / Deploy / Monitor / AI / Resources; …" becomes "automation Deploy / Monitor /
    AI / Resources; embedded Build / Configure / Monitor / Resources", and the ungrouped automation rows gain
    **Projects** ("AI Hub (Chats in CE …), Approval Tasks, Projects and Connections").
  - "Both Build groups are Development-only" becomes "The embedded Build group and the automation Projects row are
    Development-only"; the list of pairs drops "Data Syncs → Data Sync Deployments" and names data syncs among what
    the Projects entry covers ("Projects — including the Agents and Data Syncs tabs and the agents and data syncs
    inside a project — → Deployments").
- `SystemProjects` and the `ProjectFacade` / `ProjectWorkflowFacadeImpl` Javadoc mentions of `__DATA_SYNC__`.

### Migration of local data

Never shipped as a changeset and never run automatically. For a PostgreSQL dev database that already applied the old
init changeset, this keeps every sync: each hidden project becomes an ordinary project named after its sync, its
workflow is marked `DATA_SYNC`, and the schema is brought to the edited changelog.

```sql
BEGIN;

ALTER TABLE data_sync ADD COLUMN project_workflow_uuid UUID;

-- Each hidden project holds exactly one project workflow uuid across its versions; the scalar subquery fails
-- loudly if one does not.
UPDATE data_sync
SET project_workflow_uuid = (
    SELECT DISTINCT project_workflow.uuid FROM project_workflow
    WHERE project_workflow.project_id = data_sync.project_id);

UPDATE project_workflow SET type = 2
WHERE uuid IN (SELECT project_workflow_uuid FROM data_sync);

ALTER TABLE data_sync ALTER COLUMN project_workflow_uuid SET NOT NULL;
ALTER TABLE data_sync ADD CONSTRAINT uk_data_sync_project_workflow_uuid UNIQUE (project_workflow_uuid);

UPDATE project
SET name = data_sync.title, description = data_sync.description
FROM data_sync
WHERE data_sync.project_id = project.id;

ALTER TABLE data_sync DROP CONSTRAINT fk_data_sync_project;
ALTER TABLE data_sync DROP COLUMN project_id;

DROP TABLE data_sync_tag;

UPDATE databasechangelog SET md5sum = NULL
WHERE filename LIKE '%automation_data_sync_init.xml';

COMMIT;
```

Afterwards `SELECT id, name FROM project WHERE name LIKE '\_\_DATA\_SYNC\_\_%' ESCAPE '\';` must return no rows. Any
survivor is an orphan hidden project with no `data_sync` row; delete it with the agents spec's cleanup script
(Schema section), with the prefix swapped. Project names are not unique, so a renamed project may share a name with
an existing one; rename it by hand if that matters. `DATA_SYNC_PUBLISH` stays, so no `custom_role_scope` row goes
stale.

## Trade-offs accepted

- Editing a sync and publishing bumps the whole project's version and redeploys its workflows. A user who wants an
  independent release cadence keeps one sync per project — what the default create flow produces.
- An unfinished sync blocks every publish of its project, including one started from the workflow editor to ship an
  unrelated workflow. The error names the sync, so the fix is one click away; the alternative — silently leaving the
  sync out of the version — would let a version exist in which the sync's workflow and rows disagree.
- Imported syncs arrive without connections and block the project publish until reconnected.
- "Data Syncs" loses its top-level sidebar slot. If discoverability suffers, a nav row linking to
  `/automation/projects?dataSyncs=all` is a one-line addition.
- Per-sync visibility and sync tags are gone; both are per project.

## Risks

- **Data Sync has never run end to end against a live app.** Its pieces are unit- and IntTest-covered, but no one has
  created, published, deployed and run a sync on a running server. This change moves every lifecycle edge (publish,
  deploy, run, delete, duplicate, import, git). Boots against the shared dev database are off-limits, so verification
  is by Testcontainers IntTests (below) that exercise each edge against a real schema built from the edited
  changelog. A manual pass on a throwaway database is the user's call and is not part of the plan.
- The migration SQL has not been run; it is written against the init changelog as it stands and must be read before
  use.
- Three tabs in the 355px project sidebar with counts may not fit ("Workflows (12) | Agents (3) | Data Syncs (4)").
  Verify at that width in the client phase; if it truncates, the fallback is counts as badges inside the triggers,
  applied to all three tabs, not a shorter label for one.
- EE environment promotion (`automation-promotion`) copies project deployments across environments by lineage uuid. A
  sync's workflow is an ordinary member of the deployment there and should promote with it, but no promotion test
  covers a generated workflow; this spec adds none (out of scope) and records the gap.
- Tests across `automation-configuration` use `SystemProjects.DATA_SYNC_NAME_PREFIX` as a sample system prefix
  (`ProjectDeploymentFacadeTest`, `ProjectFacadeRowVisibilityTest`, `ProjectDeploymentServiceSystemProjectIntTest`,
  `ProjectServiceIntTest`, `SystemProjectsTest`, `WorkflowDeleteCascadeIntTest`, `DataSyncServiceIntTest`). They switch to
  `KNOWLEDGE_BASE_NAME_PREFIX` or `CONTEXT_STORE_NAME_PREFIX`; missing one fails compilation, not silently.
- Worktrees that still carry the old init changelog will checksum-clash on a database migrated from this one.

## Phasing

1. **Server model.** Enum value and predicate, schema edit, repository and service, facade rewrite onto real projects,
   guard generalization, listing predicate, the three listeners/contributor, GraphQL changes. The old client keeps
   compiling against the trimmed schema only after phase 2, so phases 1 and 2 land together.
2. **Client move.** Navigation and Build group, routes, Projects tabs and filter, project sidebar tab, sync page shell
   and header, `ProjectItemSelect`, Deployments tab and Run now, dialogs, removals, GraphQL regeneration.
3. **Docs.** `.agents/data-sync.md`, `.agents/agents.md`, CLAUDE.md, the original spec's pointer.

## Testing

Server (each new test watched failing before it is trusted):

- `DataSyncFacadeIntTest` (Testcontainers), rewritten: create with and without `projectId`; a project with two syncs,
  an agent and a workflow — each sync resolves its own workflow across a project publish; an unfinished sync blocks
  the project publish with its title in the message; delete leaves the project, the sibling sync and the workflows;
  delete refused while enabled in a deployment and allowed once disabled, removing the disabled
  `project_deployment_workflow` row; project delete removes the syncs; two deployments of one project in one
  environment are both listed by `getDataSyncDeployments` and neither lookup throws
  `IncorrectResultSizeDataAccessException`. `runDataSyncDeployment` with `ProjectDeploymentFacade` mocked: resolves the
  sync's workflow id among several, refuses a foreign deployment and a deployment pinned to a pre-sync version
  (`DEPLOYMENT_NOT_OWNED`), refuses a disabled deployment and a disabled sync row (`DEPLOYMENT_DISABLED`), and is
  called outside a transaction.
- `DataSyncProjectContentContributorIntTest`: duplicate copies syncs with connections into the duplicate project;
  export writes `data-syncs/<name>.json` without connection ids; import recreates them without connections; git pull
  updates by name, then unique title, keeps connections of unchanged elements, imports unmatched files and leaves
  unmatched syncs alone.
- `DataSyncProjectPublishPreListenerTest`: skips a project without syncs (no facade call); calls
  `prepareProjectPublish` otherwise.
- `DataSyncFacadeAuthorizationTest`: 17 methods pinned by exact signature and expression, including
  `prepareProjectPublish` on `DATA_SYNC_PUBLISH` and `deleteProjectDataSyncs` on `PROJECT_DELETE`.
- `DataSyncVisibilityProvidersTest` / `DataSyncOwnershipResolversTest`: project resolved through
  `project_workflow_uuid`; a sync whose workflow is gone resolves to no visibility record (fail closed).
- `GeneratedProjectWorkflowUpdateGuardTest` (renamed from `AiAgentWorkflowUpdateGuardTest`): refuses `AI_AGENT` and
  `DATA_SYNC` with the owner in the message, allows `WORKFLOW` and unknown ids. `ProjectWorkflowFacade` tests: the
  same for update/delete/duplicate.
- Listing tests proving `DATA_SYNC` workflows are absent from project workflow lists, workspace latest workflows,
  call-workflow options, both search providers, the MCP picker and the git push, and present in
  `getProjectVersionWorkflows`; a project formerly named with the `__DATA_SYNC__` prefix is no longer special-cased.
- `ProjectGitFacadeTest` (EE): pushes and pulls `data-syncs/` files through the contributor.
- `DataSyncWorkflowGeneratorTest`: unchanged snapshots.

Client:

- `ProjectList.test.tsx`: three tabs, Data Syncs tab present with zero syncs, per-tab create button placement,
  Data Syncs filter opening rows on the Data Syncs tab.
- `ProjectsLeftSidebar.test.tsx` and new `ProjectDataSyncsList.test.tsx`: tab initialisation from `currentDataSyncId`,
  badge row, Edited line, no leading type icon.
- `ProjectItemSelect.test.tsx`: three groups; an agent and a workflow with equal numeric ids navigate to the right
  target.
- New `ProjectDataSync.test.tsx`: page shell, breadcrumb, settings Data Sync tab, delete confirmation and navigation
  after delete.
- New `ProjectDeploymentDataSyncListItem.test.tsx`: Run now disabled unless both flags are on; calls
  `runDataSyncDeployment` with the sync and deployment ids. `ProjectDeploymentList.test.tsx`: sync workflows leave the
  Workflows tab.
- `DataSyncsFilterLeftSidebarNav` test: toggling, preserving other params, clearing `agents`.
- `developmentOnlyRoutes.test.ts`: the sync detail route; no `/automation/data-syncs` entry.
- Deleted with their subjects: `DataSyncs.test.tsx`, `DataSyncDetail.test.tsx`, `DataSyncListItem.test.tsx`,
  `DataSyncDeployments.test.tsx`, `DataSyncDeploymentListItem.test.tsx`. Store and router mocks use `vi.hoisted`.

## Out of scope

- Single-sync export/import in the UI or GraphQL, and MCP / Copilot / AI Hub tools for data syncs.
- Environment promotion tests for generated workflows (recorded as a risk).
- Changes to the generator, wizard steps, Mapping or Test step, and the retired dialog wizard.
- Multiple sources or destinations per sync; new trigger types.
- Embedded edition surfaces.
- A `type` field on the REST `ProjectWorkflow` / `ProjectDeploymentWorkflow` models; the client keeps recognising
  generated workflows by uuid, as it does for agents.
- Any changeset outside `automation-data-sync`, and any automatic migration of existing dev data.
