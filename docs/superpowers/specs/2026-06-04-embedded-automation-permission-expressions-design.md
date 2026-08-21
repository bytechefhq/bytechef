# Permission Expressions for Embedded Automation Projects & Workflows — Design

- **Date:** 2026-06-04
- **Branch:** `0_732`
- **Status:** Approved (design); pending implementation plan

## Summary

Extend the existing SpEL **permission expression** feature — today available only on
`Integration` and `IntegrationWorkflow` (embedded) — to **embedded automation workflow
projects and workflows**, surfaced through `AutomationWorkflowProjectDialog.tsx`
(project level) and `AutomationWorkflowDialog.tsx` (workflow level).

A permission expression is a SpEL expression evaluated against the connected user
(`metadata`, `externalId`, `email`, `name`, `environment`) by
`EmbeddedPermissionEvaluator`. A blank expression is visible; an expression that
evaluates false hides the resource; an evaluation error fails closed (hidden).

## Background / Current State

- **Permission expressions already work end-to-end for Integration / IntegrationWorkflow:**
  - `permission_expression TEXT` columns on `integration` and `integration_workflow`
    (migration `20240604183170_embedded_configuration_added_permission_expression.xml`).
  - Project-level expression is **folded into the single `updateIntegration` write**
    (commit `2aedc014bc3`) to avoid an optimistic-lock 409 from a second write.
  - Workflow-level expression uses a **dedicated** GraphQL mutation
    `updateIntegrationWorkflowPermissionExpression` because it lives on the
    `IntegrationWorkflow` join entity, separate from the workflow definition.
  - `ConnectedUserIntegrationFacadeImpl` filters visibility via
    `embeddedPermissionEvaluator.evaluate(permissionExpression, connectedUser)`
    (`isIntegrationVisible` for integrations, `filterWorkflows` for workflows).

- **Embedded "automation workflow projects" are not a separate entity.** They are
  standard **core** automation `Project` / `ProjectWorkflow` rows whose name carries the
  `__EMBEDDED_AUTOMATION__` marker prefix (`AutomationWorkflowProjectFacadeImpl`). The
  core `Project` / `ProjectWorkflow` entities have **no** `permissionExpression` today.

- **`AutomationWorkflowDialog` is create-only today.** `AutomationWorkflows.tsx` never
  passes a `workflow` prop; `handleWorkflowDialogSubmit` builds a fresh definition and
  calls `createAutomationWorkflowProjectWorkflow(projectId, definition)`. Existing
  workflows open in the editor — there is **no metadata (label/description) update
  mutation** for embedded automation workflows.

## Decisions (approved)

1. **Scope:** Full stack — DB, entities, GraphQL, facade, connected-user visibility
   filtering, and both dialogs.
2. **Levels:** Both project-level (whole-project visibility) and workflow-level
   (per-workflow visibility), mirroring Integration + IntegrationWorkflow.
3. **Storage:** **Inline `permission_expression` column on the core `project` and
   `project_workflow` tables.** Rationale: embedded automation reuses these core
   entities; mirrors how Integration stores it inline; the column is nullable and simply
   unused for non-embedded automation. CE footprint is tiny (column + entity
   field/getter/setter only) — no new CE service methods; the EE facade load-set-saves
   via existing `projectService.update` / `projectWorkflowService.update`. Alternative
   (EE sidecar tables keyed by id) rejected as needless plumbing.
4. **Write pattern:** **Fold** the project expression into `create/updateAutomationWorkflowProject`
   (single write, like Integration). Use a **dedicated** mutation for the workflow
   expression and pass it on workflow create (like IntegrationWorkflow, which lives on a
   join entity).
5. **Workflow editing:** Add a real **Edit Workflow** entry point in the workflow list
   that opens `AutomationWorkflowDialog` pre-filled, so an existing workflow's
   label/description/permission expression are all editable.

## Architecture

### Backend — Core / CE (Apache license header)

1. **Liquibase migration** in the `automation-configuration-service` changelog: add
   nullable `permission_expression TEXT` to `project` and `project_workflow`.
2. **`Project.java`** and **`ProjectWorkflow.java`** (automation-configuration-api):
   add `@Column("permission_expression") private String permissionExpression;` with
   getter/setter. No service-layer changes — the existing `update(...)` paths persist
   the new field.

### Backend — EE (ByteChef Enterprise header, `@version ee` on every file incl. tests)

3. **`automation-workflow-project.graphqls`:**
   - `AutomationWorkflowProject` → add `permissionExpression: String`.
   - `AutomationWorkflowProjectWorkflowTemplate` → add `permissionExpression: String`.
   - `createAutomationWorkflowProject(name, description, category, tags, permissionExpression: String): ID!`
   - `updateAutomationWorkflowProject(id, name, description, category, tags, permissionExpression: String): Boolean!`
   - `createAutomationWorkflowProjectWorkflow(projectId, definition, permissionExpression: String): ID!`
   - **New** `updateAutomationWorkflowProjectWorkflow(workflowUuid: ID!, label: String!, description: String): Boolean!` (metadata)
   - **New** `updateAutomationWorkflowProjectWorkflowPermissionExpression(workflowUuid: ID!, permissionExpression: String): Boolean!`
4. **`AutomationWorkflowProjectGraphQlController`:** add the new arguments to the
   existing mutations and the two new mutation mappings, delegating to the facade.
5. **`AutomationWorkflowProjectFacade` / `...FacadeImpl`:**
   - `createProject` / `updateProject`: set `permissionExpression`. **Null-guard** on
     update — the existing `handleUpdateTags` path omits the arg (`null`) and must not
     clobber a stored expression. The dialog always sends a string (`""` = explicit
     clear, persisted as `null`); a `null` argument means "leave unchanged".
   - `createProjectWorkflow(projectId, definition, permissionExpression)`: set the
     expression on the newly created `ProjectWorkflow`.
   - **New** `updateProjectWorkflow(workflowUuid, label, description)`: merge
     label/description into the `Workflow` definition via `workflowService` (already
     injected).
   - **New** `updateProjectWorkflowPermissionExpression(workflowUuid, expr)`: load the
     `ProjectWorkflow`, set the field, `projectWorkflowService.update(...)`. Writes
     `project_workflow`, never the `workflow` definition row — so it cannot collide with
     editor saves (no optimistic-lock 409).
   - `toDTO`: populate `permissionExpression` at both project level (from `Project`) and
     workflow-template level (from each `ProjectWorkflow`).
6. **DTOs:** add `permissionExpression` to `AutomationWorkflowProjectDTO` and the
   workflow-template DTO used in `workflowTemplates`.
7. **`ConnectedUserProjectFacadeImpl`** (visibility enforcement — the point of the
   feature): inject `EmbeddedPermissionEvaluator`; filter returned projects by
   `evaluate(project.getPermissionExpression(), connectedUser)` and filter each
   project's workflows by the matching `ProjectWorkflow.permissionExpression`, mirroring
   `ConnectedUserIntegrationFacadeImpl.isIntegrationVisible` / `filterWorkflows`.
   Fail-closed behaviour is inherited from the evaluator.

### Frontend (EE, `client/src/ee/...`)

8. **`AutomationWorkflowProjectDialog.tsx`:** add a `permissionExpression` `Textarea`
   FormField (placeholder e.g. `metadata['plan'] == 'pro'`), default from
   `project?.permissionExpression`, add to `AutomationWorkflowProjectFormValuesI`.
9. **`AutomationWorkflowDialog.tsx`:** add the same field, default from
   `workflow?.permissionExpression`; the pre-existing `isEditMode` branch becomes live.
10. **`AutomationWorkflows.tsx`:**
    - Project create/update: pass `permissionExpression` (always a string; `""` clears).
    - Workflow create: pass `permissionExpression` to the create mutation.
    - Add edit-workflow state + handler; render `AutomationWorkflowDialog` in edit mode;
      on edit submit call `updateAutomationWorkflowProjectWorkflow` (label/description)
      **and** `updateAutomationWorkflowProjectWorkflowPermissionExpression`, then
      invalidate.
11. **Workflow list** (`AutomationWorkflowProjectList` + row item): add an **Edit**
    action wired to `onEditWorkflow`.
12. **GraphQL operations** (`client/src/graphql/embedded/configuration/`): select
    `permissionExpression` on the project and its `workflowTemplates`; add the new arg to
    the create/update project and create-workflow operations; add
    `updateAutomationWorkflowProjectWorkflow.graphql` and
    `updateAutomationWorkflowProjectWorkflowPermissionExpression.graphql`; regenerate via
    `cd client && npx graphql-codegen`.

## Data Flow

- **Author (embedding customer) sets expression** → dialog → GraphQL mutation → facade →
  `Project` / `ProjectWorkflow` (`permission_expression` column).
- **Connected end user lists projects/workflows** → `ConnectedUserProjectFacadeImpl` →
  `EmbeddedPermissionEvaluator.evaluate(expr, connectedUser)` decides visibility per
  project and per workflow.

## Error Handling

- Blank/null expression → visible (default).
- Expression evaluates false → hidden.
- Expression throws during evaluation → hidden (fail closed), logged with the connected
  user's external id (existing `EmbeddedPermissionEvaluator` behaviour, unchanged).
- Project `updateProject` null-guard prevents tag-only updates from wiping a stored
  expression.

## Testing

- **Backend (EE, `@version ee`):**
  - Facade: project create/update fold + null-guard (tag update preserves expression;
    `""` clears); `createProjectWorkflow` sets expression; the two new workflow mutations.
  - Connected-user filtering: project visible/hidden by expression; workflow visible/
    hidden by expression; fail-closed on a malformed expression.
- **Frontend:** both dialogs render and persist the field; edit-workflow flow fires both
  mutations; `npm run check` (lint + typecheck + tests) before commit.
- **Codegen:** regenerate `graphql.ts`; commit operations and generated file separately.

## Out of Scope

- Permission expressions for non-embedded (standard) automation projects — the column
  exists but is unused there; no UI.
- Changes to `EmbeddedPermissionEvaluator` semantics or its context variables.
- Bulk migration of existing automation projects (all start with `null` = visible).

## References

- Integration feature commits: `2aedc014bc3`, `6a6c7e481d8`, `b06d28b13dd`,
  `f1074c8eae4`, `9094b6bedc1`.
- `EmbeddedPermissionEvaluator` (embedded-configuration-service security package).
- `ConnectedUserIntegrationFacadeImpl` (`isIntegrationVisible`, `filterWorkflows`) — the
  pattern mirrored by `ConnectedUserProjectFacadeImpl`.
