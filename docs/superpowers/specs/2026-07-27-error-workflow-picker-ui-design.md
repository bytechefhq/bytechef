# Error-workflow picker UI (project default + per-workflow override)

**Status:** Designed, not implemented. Completes the error-workflow feature
(`2026-07-26-workflow-error-handler-design.md`): the config surface it shipped without.

## Server (the missing half of the API)

- New facade method + `ROLE_ADMIN`-consistent GraphQL mutation
  `updateProjectWorkflowErrorWorkflow(projectWorkflowId, errorProjectWorkflowId, disabled)` setting
  the per-workflow override columns (`project_workflow.error_project_workflow_id`,
  `error_workflow_disabled`). Non-null references validate through the existing
  `ErrorWorkflowConfigurationValidator` (same project, carries the trigger, not self); clearing
  skips validation; persistence via a dedicated single-field service method (the
  `updateErrorWorkflow` precedent — never the whitelist `update(Project)` path).
- A query exposing, per project: the current default, each workflow's override/disabled state, and
  the **eligible handlers** — workflows in the project whose definition carries a
  `workflow/newWorkflowError` trigger (version-agnostic match via `WorkflowNodeType.ofType`, the
  validator's own comparison).

## Client

- **Project settings**: "Error workflow" picker listing eligible handlers only (plus "None");
  empty-state hint when the project has none ("add a New Workflow Error trigger to a workflow").
- **Workflow settings** (per workflow): three-state control — Inherit project default (null) /
  Override with picker (eligible handlers minus the workflow itself) / Disabled. Mirrors the
  resolver semantics exactly: explicit disable beats inherited default.
- Validation failures surface the server's `IllegalArgumentException` message via the global toast
  (no per-mutation onError unless state must reset).
- Conventions: sort-keys, `*I`/`Props` naming, `twMerge`, hook ordering, GraphQL codegen flow
  (`client/codegen.ts` schema + operation files + `npx graphql-codegen`), `npm run check` green.

## Out of scope

- Cross-project handlers (spec'd out originally); editor-canvas affordances beyond settings;
  embedded (feature is automation-only).
