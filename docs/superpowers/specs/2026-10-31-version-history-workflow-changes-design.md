# Version History: Workflow Changes per Version — Design

Issue: [#6849](https://github.com/bytechefhq/bytechef/issues/6849)

## Intent

Today the three version history panels show only a version number, status, published date and description. To find
out what changed in a version, a user exports two versions and compares them by hand, or restores one and looks. We
want each version to say which workflows changed, and let the user open any changed workflow to see exactly what
changed: a structured list of changes, the full definition diff, and the canvas with the changed nodes highlighted.

Affected panels:

- Automation → Project: `client/src/pages/automation/project/components/ProjectVersionHistorySheet.tsx`
- Embedded → Integration: `client/src/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet.tsx`
- Embedded → Automation Workflows: `client/src/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionHistorySheet.tsx`

## Goals

1. Each version row (published and Draft) can be expanded to show the workflows **added**, **removed** or
   **modified** compared with the previous version, with a summary line such as
   "3 workflows changed (1 added, 2 modified)". Unchanged workflows are not listed.
2. Clicking a changed workflow opens a **new dialog** showing:
   - a **structured change list** (decided by the user: shown above / alongside the raw diff),
   - a **side-by-side diff of the workflow definition** (pretty-printed JSON),
   - a **read-only canvas** with added/removed/modified/moved nodes highlighted (decided by the user: in this ticket).
3. The Draft version shows unpublished changes against the last published version.
4. Sensitive parameter values are masked by key name in everything sent to the client.
5. The diff is computed once, on the server, in the platform layer, and reused by all three panels.

## Non-goals

- Picking an arbitrary baseline version to compare with (always "previous version"; user decided).
- Restoring or exporting a single workflow from the dialog.
- Diffing deployments / instance configurations / connections.
- Masking by component property definitions (password controls); key-name masking only (user decided).

## Anti-goals (would make this a failure even if it works)

- Leaking a secret typed into a parameter (e.g. an `Authorization` header value) through the diff, the change list
  or the canvas tab.
- Reporting noise as change: key reordering or formatting differences in the stored definition must not show up as
  modifications.
- Slowing down opening the version history sheet: nothing is diffed until a version row is expanded.
- Breaking the existing Project panel behaviour (selecting a published version to restore, delete, export).

## Decisions

### Baseline (user decided: previous version)

- Published version N is compared with the **highest existing version below N** (versions can be deleted, so this
  is not necessarily N−1).
- The Draft version (the current, highest version) is compared with the **last published version**.
- If there is no earlier version, every workflow is reported as **added**.

### Matching workflows across versions

Workflows are matched by their stable `uuid`:

- projects (automation and embedded automation workflows): `ProjectWorkflow.uuid` via
  `ProjectWorkflowService.getProjectWorkflows(projectId, projectVersion)`;
- integrations: `IntegrationWorkflow.uuid` via `IntegrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion)`.

Present only in the newer version → ADDED; only in the older → REMOVED; in both with a non-empty structural diff →
MODIFIED; otherwise unchanged (omitted).

### Shared platform diff service (builder places the module)

A platform-layer service (suggested: `platform-configuration-api` interface + `platform-configuration-service`
implementation, next to `WorkflowTrigger`/`ClusterElementMap`, which already understand trigger and cluster-element
structure) with roughly:

```
WorkflowDefinitionDiff diff(@Nullable String oldDefinition, @Nullable String newDefinition)
```

returning:

- `changeType`: ADDED | REMOVED | MODIFIED | UNCHANGED
- `changes`: list of structural changes (below)
- `oldDefinition` / `newDefinition`: normalized, masked, pretty-printed JSON (for the diff pane and the canvas)

Callers (project and integration facades) only pair workflows by uuid and call this.

### Structural diff (user decided: build it)

Nodes are keyed by task `name` across the **whole tree**, not by position, so a task that moves keeps its identity.
This relies on the editor keeping node names unique within a workflow (it does: names like `httpClient_1` are
generated per workflow). A name present twice in one definition is treated positionally for that name only.
A change entry carries roughly: `kind` (ADDED | REMOVED | MODIFIED | MOVED), `target` (WORKFLOW | TRIGGER | TASK |
CLUSTER_ELEMENT | INPUT | OUTPUT), `name`, `type` (e.g. `httpClient/v1/get`), `label`, the parent path, and for
MODIFIED a list of `valueChanges {path, oldValue, newValue}` (values as JSON, masked).

Covered:

- workflow `label` / `description` changes;
- trigger added / removed / changed (type or parameters);
- tasks added / removed / modified (type, label, parameters, other task-level fields) / moved;
- **nested tasks** inside task dispatchers: the walker recurses through every parameter value (maps and lists at
  any depth) and treats any map with a string `name` and a string `type` as a task node. This is deliberately not
  "a list of task maps", because dispatchers nest differently: condition uses `caseTrue`/`caseFalse` lists, loop and
  each use an `iteratee` list, parallel uses `tasks`, branch uses `cases[{key, tasks}]` plus `default`, and fork-join
  uses `branches` as a list of lists. New dispatchers are covered without a per-dispatcher table. A nested task's own
  parameters are diffed without re-reporting its child task lists;
- **cluster elements** (the `clusterElements` extension, single element or list) as child nodes of their parent task;
- `inputs` and `outputs` definitions (inputs keyed by `name`).

MOVED means the node's parent container changed, or its relative order among siblings that exist in both versions
changed. Exact ordering algorithm is the builder's call.

### Masking (user decided: by key name)

A value is masked when its key matches (case-insensitive, ignoring `_`/`-`) a sensitive set such as `password`,
`secret`, `clientSecret`, `token`, `accessToken`, `refreshToken`, `apiKey`, `authorization`, `privateKey`,
`credentials`. Masking applies to the change list, both JSON sides and the canvas definitions. If a masked value
changed, the change is still reported with both sides shown as `••••••` ("changed, value hidden"). Builder's call:
the exact key set, and whether it extends `ObfuscateUtils`' key sets or lives in the diff service. Values that are
expressions (`${...}`) are not masked.

### Normalization

A stored definition is JSON or YAML (`Workflow.Format`), so both sides are parsed according to their own format,
masked and re-serialized as **JSON with sorted object keys** and consistent indentation; arrays keep their order
(task order is meaningful). This is what the JSON diff pane shows, so formatting-only differences in storage, and a
JSON-to-YAML format change alone, do not show up.

### API (GraphQL; builder may adjust names)

Shared types in a platform-level schema file:

```graphql
enum WorkflowChangeType { ADDED REMOVED MODIFIED }
enum WorkflowNodeChangeKind { ADDED REMOVED MODIFIED MOVED }
enum WorkflowNodeChangeTarget { WORKFLOW TRIGGER TASK CLUSTER_ELEMENT INPUT OUTPUT }

type VersionWorkflowChangeSummary {
    workflowUuid: String!
    label: String
    changeType: WorkflowChangeType!
    changeCount: Int!            # number of WorkflowDefinitionChange entries
}

type WorkflowDefinitionValueChange {
    path: String!                # e.g. "parameters.headers.Accept"
    oldValue: String             # JSON, masked
    newValue: String             # JSON, masked
}

type WorkflowDefinitionChange {
    kind: WorkflowNodeChangeKind!
    target: WorkflowNodeChangeTarget!
    name: String                 # task / trigger / cluster element / input name
    type: String                 # e.g. "httpClient/v1/get"
    label: String
    parentPath: String           # e.g. "condition_1.caseTrue" (null for top-level)
    valueChanges: [WorkflowDefinitionValueChange!]!
}

type VersionWorkflowChange {
    workflowUuid: String!
    label: String
    changeType: WorkflowChangeType!
    baseVersion: Int
    version: Int!
    changes: [WorkflowDefinitionChange!]!
    oldDefinition: String
    newDefinition: String
}
```

Two queries per owner, so the summary does not ship definitions:

- automation project: `projectVersionWorkflowChanges(projectId, projectVersion)` / `projectVersionWorkflowChange(projectId, projectVersion, workflowUuid)`;
- integration: `integrationVersionWorkflowChanges(...)` / `integrationVersionWorkflowChange(...)`;
- embedded automation workflow project: `automationWorkflowProjectVersionWorkflowChanges(...)` /
  `automationWorkflowProjectVersionWorkflowChange(...)`.

The pairing logic for projects (pick baseline version, pair `ProjectWorkflow`s by uuid, call the diff service) lives
in one **unguarded** helper in `automation-configuration-service` that both the CE `ProjectFacade` and the EE
`AutomationWorkflowProjectFacade` call. The EE facade must not call the CE facade: it runs under
`@SkipAutomationAuthorization`, so the CE facade's `hasPermission` check would not fit its callers.

Authorization (user decided), as `@PreAuthorize` on the facade methods, never on services or controllers:

- automation project: `hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')`, as `ProjectWorkflowFacadeImpl` uses;
- integration: `isTenantAdmin()`;
- embedded automation workflow project: `isTenantAdmin()` (its facade is `@SkipAutomationAuthorization`, so the
  method-level guard is the only check).

The existing version-list queries keep their current guards; tightening them is out of scope. Tests prove access is
denied without the permission/role.

The summary result also reports the baseline version so the client can label "V3 → V4".

### Client

Shared components (suggested under `client/src/shared/components/version-workflow-changes/`), driven by a fetch
hook passed in by each panel so they don't know which owner they serve:

- **VersionWorkflowChanges**: summary line + rows (label, ADDED/REMOVED/MODIFIED badge, change count). Loads lazily
  when the version row is expanded. Empty state: "No workflow changes."
- **WorkflowVersionChangesDialog**: title = workflow label, subtitle "V3 → V4" (Draft: "V4 → Draft"). Three tabs:
  1. **Changes**: structured list grouped by node (trigger first, then tasks in tree order), each with its
     component icon, name, kind badge, and old → new values for modified fields.
  2. **Definition**: Monaco `DiffEditor` (`@monaco-editor/react`, already a dependency), side-by-side, read-only,
     JSON language, theme follows light/dark.
  3. **Canvas**: read-only `WorkflowEditor` (as used by `ReadOnlyWorkflowSheet` via `readOnlyWorkflow`) with
     changed nodes highlighted: added = green, modified = amber, moved = blue, removed = red. A toggle switches
     between the newer version (shows added/modified/moved) and the older version (shows removed/modified/moved),
     since removed nodes only exist on the old canvas (my call). The canvas is fed from the **masked definitions**
     returned by the API: the client builds the `readOnlyWorkflow` (`tasks`, `triggers`, `definition`) from the
     parsed JSON, so no unmasked structure is ever sent and the API needs no parsed task/trigger types (my call;
     the builder must check the definition's task/trigger shape is what `useLayout` expects). Highlighting is passed into the editor via an optional
     prop/context mapping node name → change kind; how it reaches node rendering is the builder's call, but it
     must not touch the editable editor's stores or behaviour.
  ADDED workflows show an empty left side; REMOVED an empty right side and the old canvas only.

Panel integration:

- **Project panel**: each card gets an expand control (chevron) that reveals the changes list below the
  description. Clicks inside the list must not select the row for restore. The Draft row is expandable too.
- **Integration** and **Automation Workflows** panels: the existing accordion content shows the description plus the
  changes list. The Draft accordion item, currently disabled, becomes expandable.

## Testing

- Server unit tests for the diff service: added / removed / modified / moved tasks, nested task dispatchers
  (condition, loop, branch, each, parallel), cluster elements (single and list), trigger changes, inputs/outputs,
  label/description, masking (including masked-value changes and nested sensitive keys), normalization (key order
  and formatting produce no change), ADDED/REMOVED whole workflows.
- Server tests for each facade's pairing: previous-existing-version baseline with a deleted version in between,
  Draft vs last published, first version.
- GraphQL controller tests as `@GraphQlTest` IntTests, following the existing `ProjectGraphQlControllerIntTest`
  and `AutomationWorkflowProjectGraphQlController` test conventions.
- Facade authorization tests proving each new query is denied without `WORKFLOW_VIEW` / tenant admin, following the
  existing facade authorization test pattern.
- Client Vitest: the changes list (lazy load, summary counts, empty state), the dialog tabs, the three panels
  (expanding a row loads changes, clicking a workflow opens the dialog, Project row selection unaffected).

## Left to the builder

- Exact module/package placement within the platform layer and DTO names.
- Ordering algorithm for MOVED detection.
- The final sensitive key set and its home.
- How highlighting is plumbed into the read-only `WorkflowEditor`.
- Dialog sizing and tab component choice (follow existing dialog/tab components in `client/src/components`).
