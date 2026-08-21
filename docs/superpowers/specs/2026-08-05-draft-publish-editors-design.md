# Draft/Publish for Custom Component and Code Workflow Editors — Design

Date: 2026-08-05
Status: Approved (design), pending implementation plan

## Problem

The two code editors have no real draft/publish separation today:

- **Custom component editor** (`client/src/ee/pages/settings/platform/custom-components/CustomComponentDetail.tsx`)
  has only a Save button. Save (`CustomComponentFacadeImpl.updateCustomComponentSource`) overwrites the
  existing `custom_component` row in place: the previous source blob is orphaned, and `component_version`
  is never re-read from the recompiled definition, so a `.version(2)` bump made in the editor is silently
  ignored (the stored file is named `name_2.js` while the row still says `component_version = 1`). The
  upload path (`save(byte[], Language)`) honours version bumps by creating a new `(name, component_version)`
  row — the two paths disagree.
- **Code workflow editor** (`CodeWorkflowSourceEditor.tsx`) fuses save and publish: every save runs
  `ProjectCodeWorkflowFacadeImpl.deployInto`, which mints a brand-new `code_workflow_container` and then
  calls `ProjectServiceImpl.publishProject` directly — bypassing `ProjectFacadeImpl.publishProject`, so
  git sync and the version-description flow never run. There is no way to save work-in-progress without
  publishing a project version. The embedded surface (`IntegrationCodeWorkflowFacadeImpl`) mirrors this.

## Goals and core invariant

**Published artifacts are immutable; at most one mutable draft exists at a time.**

1. Save creates or updates a **draft**; it never mints a published version and never touches a published row.
2. Publish is a separate, explicit action.
3. Editing a published version creates a **new draft version** — never an in-place edit of the published one.
4. Save updates the existing draft DB row in place (no row-per-save accumulation, no orphaned blobs).
5. Running workflows always use the last-published version; drafts are invisible to the runtime.
6. Applies to both automation (project) and embedded (integration) code workflow surfaces.

Out of scope (deliberate): viewing/restoring the source of old published versions, a "discard draft"
action, per-save history, test-running draft custom components inside workflows.

## Part 1 — Custom components

### Data model

New columns on `custom_component` (module `platform-custom-component-configuration-service`):

- `status INT NOT NULL` — enum `Status { DRAFT, PUBLISHED }`, INT ordinal, append-only (pinned by an
  enum-ordinal stability test per repo convention).
- `published_date TIMESTAMP NULL`.

The schema is released (present in `v0.31.2`), so this is a **new changeset**, never an init edit.
Backfill: existing rows get `status = PUBLISHED` (preserves current behavior — everything that exists
today is live) and `published_date = last_modified_date` (best available approximation, informational only).

The `uk_custom_component_name (name, component_version)` unique key is unchanged. A draft is simply a row
with `status = DRAFT` and a higher `component_version` than the published rows of the same name.
**Invariant: at most one DRAFT row per component name**, enforced in the facade (not the DB — the unique
key already prevents duplicate versions; the one-draft rule is a business rule with a typed error).

### Save semantics (`updateCustomComponentSource`)

Save always compiles the source first (unchanged — drafts must compile to be saved) and re-reads
`(name, version)` from the compiled definition. Rename is still rejected (`SOURCE_RENAME_UNSUPPORTED`).

- **Target row is DRAFT** → update that row in place: store the new `FileEntry`, **delete the replaced
  blob**, update `component_version` from the compiled definition (this fixes the version bug), and copy
  title/description/icon as today. A version change on a draft is allowed as long as it doesn't collide
  with an existing row of the same name (unique-key rule; collision → typed error `VERSION_ALREADY_EXISTS`).
- **Target row is PUBLISHED** → the row is never modified. Instead:
  - If a DRAFT row for this name already exists → reject with typed error `DRAFT_ALREADY_EXISTS`
    (payload includes the draft's id/version so the client can offer to navigate there).
  - Otherwise the compiled version must be **strictly greater** than the highest existing
    `component_version` for the name; if not → typed error `VERSION_NOT_BUMPED` ("bump `.version(n)` in
    the source to start a new draft").
  - On success a **new row** is created with `status = DRAFT` carrying the compiled definition. The
    mutation returns the (possibly new) row so the client can navigate to it.

### Publish semantics (new)

New GraphQL mutation `publishCustomComponent(id: ID!)` (schema enum values SCREAMING_SNAKE_CASE;
`@PreAuthorize` ADMIN on the facade, matching the other custom-component operations):

- Valid only on a DRAFT row → flips `status` to `PUBLISHED`, stamps `published_date`, fires a new
  `CUSTOM_COMPONENT_PUBLISHED` audit event. Publishing a PUBLISHED row → typed error.
- Older published versions of the same name are untouched and continue to coexist — the same
  `(name, componentVersion)` model the upload path already implements.

### Runtime visibility

Wherever custom components are loaded for the component registry / workflow editor / execution, the
existing `enabled = true` filter is extended to `enabled = true AND status = PUBLISHED`. Drafts never
appear in component pickers and never execute. The settings list (`CustomComponentListItem`) shows all
rows with a status badge.

Consequence (accepted): `createEmptyCustomComponent` now creates a DRAFT row, so a newly created custom
component is unusable in workflows until first published. That is the point of drafts; the migration
marks all pre-existing rows PUBLISHED so nothing already in use disappears.

### Upload path — deliberate exception

`deployCustomComponentInternal` / `CustomComponentFacadeImpl.save(byte[], Language)` (CLI deploy) is
**unchanged**: it creates-or-updates rows directly as `PUBLISHED` (new rows get `status = PUBLISHED`,
`published_date = now`). Rationale: the deploy API is an artifact-replacement tool (like re-pushing a
container tag) and the CLI dev loop depends on re-deploying the same version repeatedly. The
immutability invariant is an *editor* contract. If an upload targets a name that has a DRAFT row with
the same version → typed error (the draft owns that version number).

### Client (custom components)

- `CustomComponentDetailHeader`: Save button unchanged in behavior; new **Publish** button, enabled only
  when the row is a DRAFT **and** the buffer is not dirty (tooltip: "Save first" / "Already published").
  Status badge (Draft/Published) next to the title.
- Save on a PUBLISHED row that returns a new draft row id → navigate to the new draft's route and
  invalidate the relevant queries. `VERSION_NOT_BUMPED` / `DRAFT_ALREADY_EXISTS` surface via the global
  GraphQL error toast (no per-mutation `onError` unless navigation to an existing draft is offered).
- List page shows the status badge; the enable toggle is unchanged (an enabled draft still doesn't run —
  `enabled` and `status` are independent axes).

## Part 2 — Code workflows

No schema changes. The existing entities already carry what we need: `ProjectCodeWorkflow` /
`IntegrationCodeWorkflow` join rows are stamped with the project/integration version, and
`ProjectVersion` already has the `DRAFT`/`PUBLISHED` state machine.

### Save semantics (`updateCodeWorkflowSource` / `updateIntegrationCodeWorkflowSource`)

Save compiles the source (unchanged) and then, instead of today's `deployInto` (container + publish):

- **A draft container exists** (latest container's join row is stamped with the project's current DRAFT
  version) → **update it in place**: replace the container's source blob (delete the old one), keep the
  container row and its UUID, and reconcile the generated workflows **by workflow name**:
  - matched name → update the existing Atlas `Workflow` definition in place, **preserving the
    `workflowId` and the code-workflow UUID** (this keeps `WorkflowTestConfiguration`, connections, and
    embedded references stable across draft saves);
  - new name → create workflow + `ProjectWorkflow` row in the draft version;
  - dropped name → remove the workflow and its `ProjectWorkflow` row from the draft version.
- **No draft container** (latest container belongs to a published version, or the project has none yet)
  → mint a new container stamped with the current DRAFT version. If the preceding publish duplicated
  `ProjectWorkflow` rows into the draft version (the standard facade-publish behavior), reconcile against
  them by name exactly as above rather than creating parallel rows — the duplicated rows become the
  draft's rows.

Save never creates a `ProjectVersion`, never publishes. `NewCodeWorkflowDialog` /
`createCodeWorkflow` follows the same path: creating a code workflow yields a draft, not a published
version (behavior change from today).

### Publish semantics

The existing header `PublishPopover` → REST `publishProject` → **`ProjectFacadeImpl.publishProject`**
is the one and only publish path; the `projectService.publishProject(...)` call is removed from the
editor save path. This restores git sync and the version-description flow for code workflow projects.
The container attached to the version being published is thereby frozen (nothing will ever write to it
again — the next save mints a new draft container). Embedded mirrors this with the integration header
publish and `IntegrationCodeWorkflowFacadeImpl`.

### Upload/deploy path — deliberate exception

`ProjectCodeWorkflowFacadeImpl.save(byte[], Language)` (the REST/CLI deploy endpoint) **keeps its
current save-and-publish semantics via `deployInto`**, unchanged. Rationale: "deploy" is an explicit
release action, and the embedded automation bridge's uuid carry-forward and dangling-reference logic
(`fetchPreviousWorkflowUuidsByName`, which looks exactly one deploy back) depends on the current
container-per-deploy shape. Only the editor path changes. Same for the embedded deploy endpoint.

### Known interaction — embedded-bridge catalog projects

The code editor's draft/publish container flow and the embedded automation bridge's uuid
carry-forward share the same `project_code_workflow` table with no mutual awareness. A catalog
project deployed through the embedded bridge (marked `__EMBEDDED_AUTOMATION__`, see "Embedded
automation code workflow bridge" in `CLAUDE.md`) is, from the editor's point of view, an ordinary
code-workflow project — nothing today excludes it from the editor's project list. If an operator
opens such a project in the code editor and saves, the save mints a new draft container per the
rules above and inserts it into `project_code_workflow` exactly like any other editor save. The
bridge's own `fetchPreviousWorkflowUuidsByName` (see `CLAUDE.md`) only looks one deploy back when
carrying workflow uuids forward across a redeploy; an editor-inserted draft container in between
two bridge deploys is invisible to that one-deploy lookback, so the next bridge redeploy can mint
a fresh uuid for a workflow whose uuid didn't actually need to change. Any connected-user reference
row pinned to the old uuid (`ConnectedUserProjectWorkflow.catalogWorkflowUuid`) then dangles
permanently — dangling references never self-heal (see `CLAUDE.md`), so this failure mode requires
manual de-provision/re-provision to recover, same as the redeploy-drops-a-workflow case. The editor
therefore excludes bridge catalog projects: `getCodeWorkflowProjects` filters marker-named projects
out of the code editor's project list, and `updateCodeWorkflowSource` rejects saves on them with the
typed error `EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE` — bridge catalog projects are only writable
through the bridge's own deploy endpoint.

### Editor read path

`getProjectCodeWorkflow` (latest container) remains the editor's read source. After a publish and
before the first save, the editor shows the just-published source; editing and saving it creates the
new draft container per the rules above — the published container itself is never written.

## Error handling summary

| Error | Where | Behavior |
|---|---|---|
| `VERSION_NOT_BUMPED` | custom component save on PUBLISHED row | reject; client explains version bump |
| `DRAFT_ALREADY_EXISTS` | custom component save on PUBLISHED row | reject; client offers navigation to draft |
| `VERSION_ALREADY_EXISTS` | custom component draft save changing version onto a taken version | reject |
| `SOURCE_RENAME_UNSUPPORTED` | both editors | unchanged |
| `CODE_WORKFLOW_NAME_MISMATCH` | code workflow save | unchanged |
| Compile failure | both editors | unchanged — a draft must compile to be saved |

All new backend code under `server/ee/` uses the ByteChef Enterprise license header and `@version ee`.

## Testing

- **Custom components (unit)**: facade branches — draft in-place update (blob deleted, version
  re-read), published→new-draft creation, `VERSION_NOT_BUMPED`, `DRAFT_ALREADY_EXISTS`,
  `VERSION_ALREADY_EXISTS`, publish flip + audit event, publish-on-published rejection, upload path
  creating PUBLISHED rows and rejecting a draft-owned version. Enum-ordinal stability test for `Status`.
- **Custom components (registry)**: drafts excluded from component listing/resolution; enabled draft
  still excluded.
- **Code workflows (unit)**: draft container updated in place (same UUID, blob replaced), workflow
  reconciliation by name (update/add/remove, workflowId + uuid preserved), new-container branch after
  publish (reuses duplicated ProjectWorkflow rows), save no longer creates a ProjectVersion, deploy
  path still publishes. Mirror the automation tests on the embedded facade.
- **Liquibase**: verified through an existing module `*IntTest` (Testcontainers builds the schema from
  scratch), not via the `liquibase` bootRun profile.
- **Client (vitest)**: Publish button enable/disable matrix (draft/published × dirty/clean), navigation
  on save returning a new draft id, status badges.

## Migration and rollout

- One new changeset in `platform-custom-component-configuration-service`: add `status` +
  `published_date`, backfill `PUBLISHED` / `last_modified_date`.
- No data migration for code workflows — existing containers all belong to published versions, so the
  first editor save after upgrade naturally takes the "mint new draft container" branch.
- Behavior changes to announce: new custom components and new code workflows start as drafts and must
  be explicitly published before they run.
