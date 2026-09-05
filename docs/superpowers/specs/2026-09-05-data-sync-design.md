# Data Sync — design

Date: 2026-09-05
Status: approved in conversation, awaiting spec review

> Superseded on placement, publish, deployments and tags by
> `docs/superpowers/specs/2026-09-22-data-syncs-under-projects-design.md`, which moves Data Sync off the
> hidden per-sync project onto ordinary projects and drops `data_sync_tag`; the model, wizard and generated
> workflow described below are otherwise unchanged.

## Summary

Data Sync is a Build surface for one-source-to-one-destination data transfer, edited through a copy of the
Data Stream simple editor (the Source / Destination / Mapping / Test wizard that today lives inside the
cluster element editor dialog). The dialog is being retired, and the wizard would go with it; this feature
gives the wizard a standalone home and a proper domain behind it.

The model is the Agents model: domain rows are the source of truth, a hidden system project holds one
generated draft workflow, every mutation regenerates that draft, Publish snapshots it as a project version,
and a Deploy page pins a version per environment. Precedents: `AiAgentWorkflowGenerator` /
`AiAgentFacadeImpl` (aggregate + regenerate + publish), `KnowledgeBaseSourceWorkflowGenerator` (a
`schedule/v1/cron` → `dataStream/v1/stream` workflow with a manual-run path).

## Decisions log

| Question | Decision |
|---|---|
| What owns source, destination and mapping? | Domain rows, like Agents. The generator renders the whole draft on every mutation. |
| How does the wizard save? | Per-field autosave. Each change replaces the element's whole parameters map through one mutation. |
| Can a scheduled sync run on demand? | Yes, always. Run now on the deployment row regardless of trigger type. |
| Publish / deploy lifecycle? | Mirror Agents: Publish, versions, deployments per environment. |
| What happens to the dialog wizard? | Copied, not moved. The dialog's Data Stream editor stays untouched. |
| Edition / placement | CE, `server/libs/automation/automation-data-sync/`. |
| Processor | Always `dataStreamProcessor/v1/fieldMapper`; auto-map is computed client side and saved like any mapping edit. |

## Domain model

Module `server/libs/automation/automation-data-sync/` with the Agents three-module split: `-api` (domain,
DTOs, facade interface, services), `-service` (impls, repositories, generator, Liquibase changelog),
`-graphql` (controllers, schema). Apache header.

### `data_sync` (`DataSync.java`)

- `name` — slug, `^[a-z0-9_-]{1,64}$`, enforced in `setName` and by a DB check constraint `ck_data_sync_name_slug`,
  unique per workspace (`uniqueName(slugify(title), workspaceId)` as in `createAgent`).
- `title`, `description`.
- `workspace_id BIGINT` nullable, field `Long` (workspace-column rule, no relation table).
- `project_id` — FK to the hidden backing project.
- `uuid`.
- `trigger_type INT` — ordinal enum `TriggerType { MANUAL, SCHEDULE }`; append-only.
- `trigger_parameters` JSON — for `SCHEDULE`, the cadence fields in the exact shape the agent schedule channel
  stores (`frequency`, `time`, `dayOfWeek`, `dayOfMonth`, `timezone`, …) so the client cadence helper
  (`agentScheduleCron.ts`) round-trips unchanged. Null for `MANUAL`.
- Audit columns (`created_by`, `created_date`, `last_modified_by`, `last_modified_date`, `version`).

### `data_sync_element` (`DataSyncElement.java`)

- `data_sync_id` — FK, cascade delete.
- `kind INT` — ordinal enum `Kind { SOURCE, DESTINATION, PROCESSOR }`; append-only.
- `component_name`, `component_version`, `operation_name` (the cluster element name).
- `parameters` JSON, `connection_id` nullable.
- Unique index on `(data_sync_id, kind)` — one row per kind.

Identity lives in columns rather than inside `parameters` (the one deliberate departure from
`ai_agent_element`) so the list page can show "Salesforce → Postgres" per row without parsing JSON.
The processor row is created lazily on the first mapping edit and always points at
`dataStreamProcessor` / `1` / `fieldMapper`.

### `data_sync_tag`

The `ai_agent_tag` join shape: `data_sync_id` + `tag_id`, composite PK, `data_sync_id` cascade-deletes. A relation
table because tags are genuinely many-to-many.

### System project

`SystemProjects.DATA_SYNC_NAME_PREFIX = "__DATA_SYNC__"`, added to `NAME_PREFIXES` so the project is excluded
from Projects and Deployments listings. Creating a sync creates the project, the entity, and one draft
workflow from the generator (mirroring `createAgent`). Visibility is inherited from the project; the sync is
created WORKSPACE-visible.

### Delete rules

Delete is refused while any deployment of the project exists (`hasAnyDeployment`, per environment). Otherwise
element and tag rows cascade at DB level, and the project is deleted through the existing project-delete
cleanup path.

## Workflow generation

`DataSyncWorkflowGenerator.generate(dataSync, elements)` in `-service`'s `util` package. Static, deterministic
(`LinkedHashMap`, `JsonUtils.write`, no clock or random reads). Fixed shape:

```
trigger_1 (manual/v1/trigger | schedule/v1/cron) ─▶ dataStream_1 (dataStream/v1/stream)
                                                       clusterElements:
                                                         source:      <component>/v<n>/<operation>   name source_1
                                                         destination: <component>/v<n>/<operation>   name destination_1
                                                         processor:   dataStreamProcessor/v1/fieldMapper name processor_1  (only when the row exists)
```

- `label` = sync title; `description` = "Auto-generated by Data Sync. Edits via the Data Sync UI.";
  `metadata.dataSyncId` = owner id (the `knowledgeBaseSourceId` convention); `inputs` = `[]`.
- `MANUAL` → `manual/v1/trigger`, no parameters. `SCHEDULE` → `schedule/v1/cron` with `expression` and `timezone`
  computed from `trigger_parameters` by the same server-side cadence translation the agent schedule channel
  uses. No never-fires sentinel is needed.
- A missing source or destination row emits no entry for that slot. The draft is not runnable then; publish
  validation is the gate.
- Node names are fixed (`trigger_1`, `dataStream_1`, `source_1`, `destination_1`, `processor_1`) so the client
  addresses options, display-condition and test lookups without discovery.
- Each element's `connectionId` is emitted in the task's `connections` block keyed by its node name, and
  `buildConnectionRefs(...)` exposes the same traversal for the test-connection sync.

### Regeneration

Every facade mutation calls `regenerateAndSaveWorkflow`: load the project's last-version workflow, rewrite its
definition, then sync each element's `connection_id` into the draft's `WorkflowTestConfiguration` for every
`Environment` (the agent `syncTestConnections` pattern). The row change and the regeneration share one
transaction, so the draft can never disagree with the rows.

### Run now

- `MANUAL` syncs: the deployment row's existing "Run workflow manually" action covers it, no new server path.
- `SCHEDULE` syncs: one facade method `runDeployment(projectDeploymentId)` starts the deployed workflow's job
  directly through the same job-creation path the manual-run endpoint uses, with no trigger involvement.
  Refused with a typed error when the deployment is disabled.
- The client shows one Run now button and picks the path by trigger type.

### Draft Test

The wizard's Test step calls the existing workflow-node test-output endpoint against `draftWorkflowId` and
`dataStream_1`. `draftWorkflowId` is on the DTO exactly like the agent's.

## Facade and GraphQL

### `DataSyncFacade` (`-api`, impl in `-service`)

`@PreAuthorize` on the facade (the agent convention), with permissions `DATA_SYNC_VIEW`, `DATA_SYNC_EDIT`,
`DATA_SYNC_DELETE`, `DATA_SYNC_PUBLISH` registered beside the `AGENT_*` ones.

- `create(title, description, workspaceId)`, `update(id, title, description)`, `delete(id)`, `get(id)`, `list(workspaceId)`
- `updateTrigger(id, triggerType, triggerParameters)`
- `setElement(id, kind, componentName, componentVersion, operationName, parameters, connectionId)` — upsert on
  `(id, kind)`. Changing the source or destination component replaces the row wholesale **and drops the
  processor row**, since a mapping over a different side is meaningless.
- `updateElementParameters(elementId, parameters, connectionId)` — whole-map replace; the per-field autosave
  target; no side effects beyond regeneration.
- `publish(id, description)`, `getVersions(id)`, `getDraftWorkflowId(id)`
- `getDeployments(workspaceId)`, `runDeployment(projectDeploymentId)`
- `getTags(workspaceId)`, `updateTags(id, tags)`, `getDeploymentTags(workspaceId)`, `updateDeploymentTags(id, tags)`

Every by-id operation resolves the sync through the workspace-visible path, so an id from another workspace
reads as missing.

### Publish validation

Refused (typed `DataSyncErrorType`) when: the source or destination row is missing; either declares a
connection requirement but has no `connection_id`; a `SCHEDULE` trigger's cadence does not translate to a cron.
Like `publishAgent`, publish regenerates the draft first, then publishes the project version.

### `data-sync.graphqls`

```graphql
extend type Query {
    dataSync(id: ID!): DataSync
    dataSyncs(workspaceId: ID!): [DataSync!]!
    dataSyncDeployments(workspaceId: ID!): [DataSyncDeployment!]!
    dataSyncDeploymentTags(workspaceId: ID!): [Tag!]!
    dataSyncTags(workspaceId: ID!): [Tag!]!
    dataSyncVersions(id: ID!): [DataSyncVersion!]!
}

extend type Mutation {
    createDataSync(input: CreateDataSyncInput!): DataSync!
    updateDataSync(input: UpdateDataSyncInput!): DataSync!
    updateDataSyncTrigger(input: UpdateDataSyncTriggerInput!): Boolean!
    setDataSyncElement(input: SetDataSyncElementInput!): DataSyncElement!
    updateDataSyncElement(input: UpdateDataSyncElementInput!): Boolean!
    deleteDataSync(id: ID!): Boolean!
    publishDataSync(id: ID!, description: String): Int!
    runDataSyncDeployment(projectDeploymentId: ID!): Boolean!
    updateDataSyncTags(input: UpdateDataSyncTagsInput!): Boolean!
    updateDataSyncDeploymentTags(input: UpdateDataSyncDeploymentTagsInput!): Boolean!
}

enum DataSyncTriggerType { MANUAL, SCHEDULE }
enum DataSyncElementKind { SOURCE, DESTINATION, PROCESSOR }

type DataSync {
    id: ID!, name: String!, title: String!, description: String, workspaceId: ID, projectId: ID!, uuid: String!,
    triggerType: DataSyncTriggerType!, triggerParameters: Map,
    elements: [DataSyncElement!]!, tags: [Tag!]!,
    unpublishedChanges: Boolean!, lastPublishedVersion: Int!, publishedDate: String, lastModifiedDate: String,
    draftWorkflowId: String!, visibility: ResourceVisibility!
}
type DataSyncElement {
    id: ID!, kind: DataSyncElementKind!, componentName: String!, componentVersion: Int!, operationName: String!,
    parameters: Map, connectionId: ID
}
type DataSyncVersion { version: Int!, description: String, publishedDate: String, status: String! }
type DataSyncDeployment {
    id: ID!, name: String!, dataSyncId: ID!, dataSyncTitle: String!, projectId: ID!, environmentId: Int!,
    enabled: Boolean!, projectVersion: Int!, triggerType: DataSyncTriggerType!, tags: [Tag!], lastExecutionDate: String
}

input CreateDataSyncInput { title: String!, description: String, workspaceId: ID! }
input UpdateDataSyncInput { id: ID!, title: String, description: String }
input UpdateDataSyncTriggerInput { id: ID!, triggerType: DataSyncTriggerType!, triggerParameters: Map }
input SetDataSyncElementInput {
    dataSyncId: ID!, kind: DataSyncElementKind!, componentName: String!, componentVersion: Int!,
    operationName: String!, parameters: Map, connectionId: ID
}
input UpdateDataSyncElementInput { id: ID!, parameters: Map, connectionId: ID }
input UpdateDataSyncTagsInput { id: ID!, tags: [TagInput!] }
input UpdateDataSyncDeploymentTagsInput { id: ID!, tags: [TagInput!] }
```

Deployments are created and toggled through the existing project deployment REST API on the hidden project,
as agent deployments are.

## Client

### Navigation and routes

- `Data Syncs` joins the automation **Build** group directly after Agents in
  `client/src/shared/navigation/navigationItems.ts` (`/automation/data-syncs`, `ArrowLeftRightIcon`).
- `Data Sync Deployments` joins **Deploy** directly after Agent Deployments (`/automation/data-sync-deployments`).
- `developmentOnlyRoutes.ts` gains the pair (Data Syncs → Data Sync Deployments), which yields both the row hiding
  outside DEVELOPMENT and the redirect guard from one list.
- Routes: `/automation/data-syncs`, `/automation/data-syncs/:dataSyncId`, `/automation/data-sync-deployments`.

### Pages

`client/src/pages/automation/data-syncs/` and `client/src/pages/automation/data-sync-deployments/`, mirroring the
agent files one for one.

- **List** — `DataSyncs.tsx`, `components/data-sync-list/DataSyncList.tsx`, `DataSyncListItem.tsx`,
  `DataSyncsLeftSidebarNav.tsx` (tag filter), `DataSyncDialog.tsx` (create: title, description), `hooks/useDataSyncs.ts`.
  The row follows the shared list-row rhythm (`min-h-8` / `gap-y-2` / `min-h-7`) and shows source → destination
  component titles with icons, the trigger summary (Manual, or cadence text from the agent helper), tags,
  published version and date.
- **Detail** — `DataSyncDetail.tsx` with a header (title, Publish, dropdown with version history and delete).
  The body is the copied wizard plus one Trigger step: **Trigger, Source, Destination, Mapping, Test**. Step nav,
  footer and step layouts are copied from `cluster-element-editor/data-stream-editor/` into
  `data-syncs/components/wizard/`.
- **Deployments** — `DataSyncDeployments.tsx`, copied from Agent Deployments: published-sync picker feeding the
  existing `ProjectDeploymentDialog`, `DataSyncDeploymentListItem` with enable switch, tags, Run now and last
  execution date. Run now calls the existing manual-run path for `MANUAL` and `runDataSyncDeployment` for
  `SCHEDULE`.

### Wizard wiring

- One hook per side, `useDataSyncElementStep(kind)`, replaces `useClusterElementStep`. It reads the element row
  from the `dataSync` query, lists candidate components by `clusterElementsCount.source` /
  `clusterElementsCount.destination` from the component definitions query the agent pages already use, and on
  component or operation change calls `setDataSyncElement`.
- Properties render in form mode (`Properties` with `control` / `controlPath` inside `ClusterElementProvider`
  carrying `inputParameters` and `connectionId`, `formDisplayConditions` for conditional fields — the
  `ComponentConfigDialog` arrangement). A debounced `watch` on the form sends `updateDataSyncElement` with the
  whole parameters map; the query invalidates so the next options lookup runs against the regenerated draft.
- Connection is a plain workspace connection select filtered to the element's component, saving
  `connectionId` on the same mutation. `ConnectionTab` is not used.
- Mapping keeps the auto-map button: it resolves source and destination field options through the options API
  against `draftWorkflowId` and the fixed node names, builds the mapping list client side, and saves through
  `updateDataSyncElement` on the processor row (creating the row through `setDataSyncElement` on first use).
- Test is the copied `DataStreamTestStep` with `draftWorkflowId` and `dataStream_1` passed as props.
- Trigger reuses `AgentScheduleFrequencyFields` and `agentScheduleCron.ts` behind a Manual / Scheduled radio,
  saving through `updateDataSyncTrigger`. The caller renders the frequency selector itself (the helper does not).

### GraphQL client

Operations in `client/src/graphql/automation/data-sync/`; regenerate `src/shared/middleware/graphql.ts` with
`npx graphql-codegen`; commit operations and generated file separately.

## Error handling

- Facade by-id operations resolve through the workspace-visible path; foreign ids read as missing.
- Publish and run-now errors are typed (`DataSyncErrorType`) and surface via the global fetch interceptor as
  toasts; the client adds no per-mutation `onError`.
- Row change and regeneration share one transaction; a generator failure rolls the row change back.
- Run now on a disabled deployment is refused with a typed error.

## Testing

Server:
- `DataSyncWorkflowGeneratorTest` — golden-JSON snapshots for manual, schedule, missing processor, missing
  destination; identical-input determinism.
- `DataSyncFacadeIntTest` (Testcontainers) — create makes the hidden project and draft; `setElement` on the source
  drops the processor; `updateElementParameters` regenerates and syncs test connections into every environment;
  publish refuses each invalid state and succeeds otherwise; delete refuses with a live deployment; the hidden
  project is absent from ordinary project listings.
- `DataSyncGraphQlControllerIntTest` — schema round trip.
- Each test is watched failing (stubbed generator / reverted guard) before it is trusted.

Client:
- `DataSyncs.test.tsx`, `DataSyncListItem.test.tsx`, `DataSyncDetail.test.tsx`,
  `useDataSyncElementStep.test.ts` (component-change cascade, debounced whole-map save),
  `DataSyncTriggerStep.test.tsx` (manual / schedule round trip), `DataSyncDeployments.test.tsx` (Run now path
  choice). Store and router mocks use `vi.hoisted`.

## Out of scope

Export and import; environment promotion; embedded edition; Copilot and MCP tools; per-element visibility;
multiple sources or destinations in one sync; any change to the existing dialog wizard or the box header's
"Switch to DataStream editor" button.
