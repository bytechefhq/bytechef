<!-- Load this when working on project or workflow create, update, duplicate, delete, publish or restore - on the
     Automation side (ProjectFacadeImpl, ProjectWorkflowFacadeImpl) or the EE embedded side
     (AutomationProjectFacadeImpl, IntegrationFacadeImpl, IntegrationWorkflowFacadeImpl). -->

# Project versioning and audit dates

User docs: `docs/content/docs/platform/automation/build/workflows/projects.mdx` and
`docs/content/docs/platform/automation/deploy/workflows.mdx`.

Each rule has an ID. A change that touches one of these paths must keep the rule's test green. A rule whose
status is **gap** is the intended behavior and is not implemented yet: do not describe it as current behavior in
user docs.

## Model

- A **project** has one row and a list of **project versions** (integer, starting at 1, incrementing by one).
  The latest version is always the **DRAFT**; every earlier one is **PUBLISHED** and immutable.
- A **project workflow** row links a project version to a workflow. Each version has its own workflow rows: the
  draft's workflows are separate copies of the published version's workflows, not shared rows.
- `createdDate`/`lastModifiedDate` (and `createdBy`/`lastModifiedBy`) are stamped by Spring Data auditing. On
  insert both dates come from one clock read, so they are equal; every later save re-stamps `lastModifiedDate`.

## A project's dates behave like a directory's

A project is to its workflows what a file-system directory is to its files:

- **`createdDate`** is the directory's birth time. It is set when the project is created and moves only when a new
  draft is born (publish) or an older one is brought back (restore).
- **`lastModifiedDate`** is the directory's `mtime`. It changes when the *list* of workflows in the draft changes: a
  workflow is added, removed, or renamed. It takes the same instant as the workflow change that caused it.
- **Editing a workflow's content** is editing a file: it changes the workflow's `lastModifiedDate`, never the
  project's, because the list of workflows did not change.
- **Changing the project's own attributes** (name, description, category, tags) moves the project's
  `lastModifiedDate` to now. The project has no separate `ctime`, so this deliberately departs from the analogy (D-1).

## Rules

| ID | Action | Rule |
|---|---|---|
| PV-1 | Create project | Creates version 1 as DRAFT. Project `createdDate == lastModifiedDate == now`. |
| PV-2 | Add workflow (create, import, from template, restore of one missing workflow) | Workflow `createdDate == lastModifiedDate == now`. Project `lastModifiedDate` = the workflow's `createdDate`. |
| PV-3 | Edit workflow content | Workflow `lastModifiedDate = now`. Project dates unchanged. |
| PV-4 | Rename workflow (label changes) | Workflow `lastModifiedDate = now`. Project `lastModifiedDate` = the workflow's `lastModifiedDate`. |
| PV-5 | Duplicate workflow | Creates a copy in the source workflow's project, labelled before its first save, so copy `createdDate == lastModifiedDate == now`. The copy is an added workflow, so PV-2 applies to the project. |
| PV-6 | Delete workflow | Project `lastModifiedDate = now`. |
| PV-7 | Edit project attributes | Project `lastModifiedDate = now`. |
| PV-8 | Publish project | The DRAFT version N becomes PUBLISHED with `publishedDate`; a new DRAFT version N+1 is added. Project `createdDate == lastModifiedDate ==` version N's `publishedDate` (the new draft is born at that instant). Every workflow of version N is duplicated into N+1, then the copy's `createdDate`, `createdBy`, `lastModifiedDate` and `lastModifiedBy` are set to the source workflow's, so the draft keeps the workflows' history. |
| PV-9 | Restore version N into the draft | The draft's workflows are replaced by fresh copies of version N's workflows (fresh dates). Project dates are set to the ones the draft had when version N was the draft, derived from publish dates: `createdDate` = version N-1's `publishedDate` (version N's own for N = 1), `lastModifiedDate` = version N's `publishedDate`. |

## Status and enforcement

All rules hold for Automation projects (CE `ProjectFacadeImpl`/`ProjectWorkflowFacadeImpl`, EE
`AutomationProjectFacadeImpl`). Embedded integrations (`IntegrationFacadeImpl`, `IntegrationWorkflowFacadeImpl`) are not
covered yet.

| ID | Enforced by |
|---|---|
| PV-1 | `ProjectFacadeIntTest.testCreateProjectStartsAtDraftVersionOneWithEqualDates` |
| PV-2 | `ProjectFacadeIntTest.testAddWorkflowSetsProjectLastModifiedDateToTheWorkflowCreatedDate` |
| PV-3 | `ProjectFacadeIntTest.testEditWorkflowContentKeepsProjectLastModifiedDate` |
| PV-4 | `ProjectFacadeIntTest.testRenameWorkflowSetsProjectLastModifiedDateToTheWorkflowLastModifiedDate`, `AutomationProjectFacadeIntTest.testUpdateProjectWorkflowRenameSetsProjectLastModifiedDateToTheWorkflowLastModifiedDate` |
| PV-5 | `ProjectWorkflowFacadeIntTest.testDuplicateWorkflowLabelsTheCopyBeforeItsFirstSave`, `ProjectFacadeIntTest.testDuplicateWorkflowSetsProjectLastModifiedDateToTheCopyCreatedDate` |
| PV-6 | `ProjectFacadeIntTest.testDeleteWorkflowMovesProjectLastModifiedDate` |
| PV-7 | `ProjectFacadeIntTest.testUpdateProjectMovesProjectLastModifiedDate` |
| PV-8 | `ProjectFacadeIntTest.testPublishProjectSetsProjectDatesToThePublishedDate`, `ProjectFacadeIntTest.testPublishProjectKeepsWorkflowAuditFieldsInNextDraft`, `WorkflowServiceIntTest.testDuplicateWorkflowPreservingAuditKeepsSourceAuditFields` |
| PV-9 | `ProjectFacadeIntTest.testRestoreProjectVersionSetsProjectDatesFromThePublishDates`, `AutomationProjectFacadeIntTest.testRestoreProjectVersionSetsProjectDatesFromThePublishDates` |

## Decisions

- **D-1 (2026-11-18)** Editing project attributes moves `lastModifiedDate` to now.
- **D-2 (2026-11-18)** Publish sets the project's `createdDate` and `lastModifiedDate` to the published version's
  `publishedDate`. Consequence: after the first publish the project's original creation date is no longer stored
  anywhere. Under team discussion; revisit with how versions are maintained.
- **D-3 (2026-11-18)** Restore derives the project's dates from publish dates (PV-9) rather than snapshotting them on
  `project_version`, so no schema change was needed. Exact only because PV-8 uses one instant for both.

## Notes

- PV-8 keeps the published version pointing at the original workflow rows; `ProjectWorkflowServiceImpl.publishWorkflow`
  moves the existing project workflow row to N+1 with the copy and inserts a new row for N with the original.
- PV-8 preserves audit fields through `WorkflowService.duplicateWorkflowPreservingAudit`, which overwrites them with
  `WorkflowCrudRepository.updateAuditFields` after the insert and refreshes the workflow cache. Auditing cannot be
  bypassed on a normal save: `modifyOnCreation` stamps all four fields on insert.
- A project save always stamps its own clock read, so PV-2, PV-4, PV-8 and PV-9 go through direct updates:
  `ProjectService.updateLastModified` and `ProjectRepository.updateDates`. These do not bump the project's `@Version`,
  so an open edit dialog's optimistic lock is not invalidated by adding a workflow.
