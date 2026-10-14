# Embedded Catalog Code Projects: Create, Edit and Publish In-App

**Date:** 2026-10-11
**Scope:** "New Code Project" on Embedded → Automation Workflows, with a draft/publish lifecycle for catalog code
projects that the upload path shares.

## Goal

Embedded → Automation Workflows gets **New Code Project**, like Projects and Integrations already have. An admin
creates a catalog code project (JavaScript or Python, from the starter template), edits it in the code editor, and
publishes it to connected users. Edits stay a draft until Publish. Upload ("Import Code Project" and the public deploy
API) keeps working and still publishes immediately.

Constraints:

- A workflow's uuid is what every connected user's reference points at. No path may change the uuid of a workflow
  that keeps its name, or references dangle.
- Publishing must fire `AutomationWorkflowProjectPublishedEvent`, which drives
  `ConnectedUserReferenceRolloutManager.rollOut`.
- No Java authoring in-app (the other two pages do not offer it).

## Current state

- **Automation code projects** (`ProjectCodeWorkflowFacadeImpl`) already have the lifecycle we want. `saveDraft`
  writes the source into the draft version: it updates the draft's `CodeWorkflowContainer` in place, or creates one
  reusing workflow ids by name (`resolveDraftWorkflowNameIds`), then adds or removes the draft's `ProjectWorkflow`
  rows. Publishing is the generic `ProjectFacadeImpl.publishProject`, which moves the draft's rows to the next version
  keeping their uuids and duplicates the workflows.
- **Catalog upload** (`AutomationWorkflowProjectCodeWorkflowFacadeImpl.save`) writes a fresh container straight into
  the current version, copies uuids over by name (`fetchPreviousWorkflowUuidsByName`), publishes at the service level,
  and leaves the new draft version **empty** (no rows, no container). Two problems follow today:
    - The project menu's Publish is not gated for code projects. On an uploaded project it publishes an empty version
      and rolls it out to every reference.
    - The admin list renders draft workflows, so an uploaded code project shows none.
- `ProjectCodeWorkflowFacadeImpl.updateCodeWorkflowSource` refuses catalog projects (`isEmbeddedBridgeProject`). Its
  name check expects the declared source name to equal the project name, but a catalog project's stored name is
  `__EMBEDDED_AUTOMATION__<name>`, and its publish path would skip the rollout event.
- The embedded editor route `/embedded/automation-workflows/:workflowUuid/editor` renders only the visual canvas, so a
  code catalog workflow opens as a canvas of `codeWorkflow/v1/perform` tasks.

## Design

### 1. Shared draft service

New `ProjectCodeWorkflowDraftService` (EE `automation-configuration-service`) holding the draft logic that is private
to `ProjectCodeWorkflowFacadeImpl` today:

- `saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language)`: updates the
  draft version's container in place when one exists, or creates one reusing workflow ids by name, then applies the
  added and removed workflows to the draft version's `ProjectWorkflow` rows (deleting removed workflows and notifying
  `WorkflowPreDeleteListener`s, as today).
- The uuid fallback (section 3).

The name check stays in each caller; the service does not know which declared name a project expects.

`ProjectCodeWorkflowFacadeImpl` delegates to the service with no behaviour change, and keeps refusing catalog projects.

### 2. Catalog facade

`AutomationWorkflowProjectCodeWorkflowFacade` gains, next to `save`, three methods keyed by catalog
(automation-workflow-project) id, so no automation `Project` id reaches embedded callers. All carry
`@PreAuthorize("hasAuthority(ADMIN)")`, like `save`.

- `long createEmptyCodeWorkflow(String name, Language language, String description, String category,
  List<String> tags, String permissionExpression)`: rejects Java and Ruby, validates the name (no blank name, no quote,
  backslash or newline, as for integrations), rejects a name already used by a catalog project
  (`CODE_WORKFLOW_ALREADY_EXISTS`), creates the project through the existing marker convention
  (`__EMBEDDED_AUTOMATION__<name>` plus its `AutomationWorkflowProject` row), fills the starter template with `name`,
  and saves it as an unpublished draft. Returns the catalog project id.
- `String getCodeWorkflowSource(long automationWorkflowProjectId)` and the stored language: the latest container's
  source.
- `void updateCodeWorkflowSource(long automationWorkflowProjectId, String content)`: rejects Java, loads the
  definition (`SOURCE_LOAD_FAILED` on failure, draft unchanged), requires the declared name to equal the project name
  without the marker (`CODE_WORKFLOW_NAME_MISMATCH`; renaming is not supported), then `saveDraft`.

The starter template is the automation one (`code-workflow-templates/starter.js` / `.py`).

### 3. Upload, publish and uuid continuity

- **Upload (`save`)** keeps its endpoints, Java/Ruby checks and warnings. Internally: find or create the catalog
  project, `saveDraft` with the uploaded source, then the catalog `publishProject`. After an upload the new draft holds
  a copy of the uploaded workflows, so the editor continues from it. Upload overwrites any unpublished editor changes
  (upload wins); the Import Code Project dialog says so.
- **Publish** is the existing catalog `publishProject`: rows move to the next version keeping their uuids, workflows
  are duplicated, and `AutomationWorkflowProjectPublishedEvent` fires. Rollout behaviour is unchanged: references move
  to the new version, and references to workflows removed from the source are marked dangling.
- **uuid fallback.** When `saveDraft` adds a workflow whose name existed in the last **published** version, the new
  draft row takes that published uuid. This covers catalog projects uploaded before this change (their draft is
  empty), and it means a workflow removed and later re-added by name gets its old uuid back. This replaces
  `fetchPreviousWorkflowUuidsByName`. Automation code projects get the same rule; they have no references, so for them
  it only steadies uuids across uploads.
- **Publish guard.** The catalog `publishProject` refuses a code project whose draft version has no code container,
  with a new `CodeWorkflowErrorType`, which closes the empty-version rollout for older projects until their next edit
  or upload.

### 4. API

GraphQL in `embedded-configuration-graphql`, delegating to the catalog facade:

```graphql
extend type Mutation {
    createAutomationWorkflowProjectCodeWorkflow(
        name: String!, language: CodeWorkflowLanguage!, description: String, category: String,
        tags: [String!], permissionExpression: String): ID!
    updateAutomationWorkflowProjectCodeWorkflowSource(
        automationWorkflowProjectId: ID!, content: String!): Boolean!
}

extend type Query {
    automationWorkflowProjectCodeWorkflowSource(automationWorkflowProjectId: ID!):
        AutomationWorkflowProjectCodeWorkflowSource!
}

type AutomationWorkflowProjectCodeWorkflowSource {
    content: String!
    language: CodeWorkflowLanguage!
}
```

`category` is a name, as in `createAutomationWorkflowProject`; new categories and tags are created on the fly.
Publishing uses the existing `publishAutomationWorkflowProject` mutation. No REST or public API change; the public
deploy endpoint follows the new upload behaviour.

### 5. Client

- **Menu.** New Project ▾ on Embedded → Automation Workflows: **New Project / New Code Project / Import Code
  Project**, header and empty state, same order and icons as Projects.
- **Dialog.** `NewAutomationWorkflowProjectCodeWorkflowDialog`, modelled on `NewCodeWorkflowDialog`: Language
  (JavaScript, Python), Name, Description, Category, Tags, Permission Expression, using the catalog page's pickers. On
  success it invalidates the project list and navigates to the editor.
- **Editor page.** Route `/embedded/automation-workflows/projects/:automationWorkflowProjectId/code`, component
  `AutomationWorkflowProjectCodeWorkflowDetail`: a thin wrapper around `CodeWorkflowSourceEditor` using the new query
  and mutation. A header bar shows the project name, a back link, and **Publish** (the existing publish mutation),
  disabled while unsaved edits exist; success toasts that the change is rolling out to connected users. The route is
  keyed by catalog project id because one source covers all of the project's workflows; the visual route is untouched.
- **Entry points.** For rows with `codeWorkflowProject = true`, clicking the project or any of its workflows opens the
  code route instead of the canvas, and the row menu gets **Edit Source**. Visual projects are unchanged.
- **Unchanged.** Reference badge, View References, the Import dialog (description text only), the Publish menu item.

## Errors

All are `ConfigurationException`s with error types, shown by the global GraphQL error toast.

| Case | Error type |
|------|------------|
| Catalog project name already used | `CODE_WORKFLOW_ALREADY_EXISTS` |
| Invalid name | the integrations' name validation |
| Java or Ruby requested | `LANGUAGE_NOT_SUPPORTED` |
| Source fails to load (draft unchanged) | `SOURCE_LOAD_FAILED` |
| Declared name differs from the project name | `CODE_WORKFLOW_NAME_MISMATCH` |
| Publishing a code project with no draft source | new `CodeWorkflowErrorType` |

## Testing

Server (IntTests against Postgres where database state matters; one test class per source class):

- `ProjectCodeWorkflowDraftServiceIntTest`: in-place update keeps workflow ids; a new draft reuses ids by name; a
  removed workflow is deleted from the draft; the fallback restores published uuids on an empty draft, including a
  workflow removed and re-added by name.
- `AutomationWorkflowProjectCodeWorkflowFacadeIntTest` (extended): create leaves an unpublished draft with the starter
  workflow; update saves a draft and leaves the published version untouched; upload over a pending draft publishes the
  upload; upload → edit → publish keeps every workflow uuid; an older project (empty draft) keeps its uuids through
  the first edit; publishing a code project with no draft source is refused.
- `ConnectedUserReferenceRolloutManagerIntTest`: a reference survives an in-app edit and publish, moves to the new
  version and keeps its inputs.
- Authorization: the new facade methods are admin-only, pinned as `save` is.
- `ProjectCodeWorkflowFacadeImpl`'s existing tests pass unchanged after the delegation refactor.

Client (Vitest): the dialog (validation, create call, navigation); the menu order on Automation Workflows; the detail
page (Save calls the update mutation, Publish disabled while edits are unsaved); the list item (code projects route to
the code editor and show Edit Source, visual projects do not).

## Out of scope

Java authoring; renaming through the source; discarding a draft; a draft-versus-published diff; "unpublished
changes" badges.
