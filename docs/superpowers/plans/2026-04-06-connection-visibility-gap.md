# Connection Visibility — Gap Remediation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining gaps between the Connection Visibility spec/plan and what shipped in commits `fc8e743e019`, `23c561567cf`, `e9976d888a7`, `7cb5d4d8439` (and their follow-up `2026-04-07-audit-connection-annotation`).

**Reference specs/plans:**
- `docs/superpowers/specs/2026-04-06-connection-visibility-phase1-design.md`
- `docs/superpowers/plans/2026-04-06-connection-visibility.md` (Phases 1, 2, 3)
- `docs/superpowers/specs/2026-04-07-audit-connection-annotation-design.md`
- `docs/superpowers/plans/2026-04-07-audit-connection-annotation.md`

**Scope audit (2026-04-13):** Phase 1 scoping, Phase 2 organization-connection flow, and most of Phase 3 (ConnectionStatus enum, reassignment facade + UI, audit annotation + aspect, `reassignAllConnections` manual audit loop) are **already implemented and merged**. The gaps below are the residual items.

**Execution order:** G1 → G2 (quick cleanups, non-blocking) before G3/G4 (runtime enforcement, larger). G5 UX is independent and can be picked up in parallel.

---

## G1. Audit publisher cleanup — **VERIFIED DONE**

The spec `2026-04-07-audit-connection-annotation-design.md` §4 calls for removing the redundant `ConnectionAuditPublisher` field from facades once the annotation covers their paths.

- [x] `WorkspaceConnectionFacadeImpl` — no longer references `ConnectionAuditPublisher` (grep: clean).
- [x] `OrganizationConnectionFacadeImpl` — no longer references `ConnectionAuditPublisher` (grep: clean).
- [x] `ConnectionReassignmentFacadeImpl` — legitimately retains the publisher for the `reassignAllConnections` loop (cannot use `@AuditConnection` due to self-invocation proxy limitation, per spec §147-149). Current references (field, import, constructor arg) all flow into the manual `publish(...)` call at lines 159-162.

No action needed.

---

## G2. `reassignAllConnections` per-item audit — **VERIFIED DONE**

Spec §147-149 requires a manual `connectionAuditPublisher.publish(CONNECTION_REASSIGNED, ...)` call per reassigned connection inside the loop (annotation alone is insufficient).

- [x] Already implemented: `ConnectionReassignmentFacadeImpl.reassignAllConnections` lines 159-162 loops `unresolvedConnections` and calls `connectionAuditPublisher.publish(CONNECTION_REASSIGNED, item.connectionId(), Map.of("newOwnerLogin", newOwnerLogin))`.

No action needed.

---

## G3. Workflow execution status validation — **DONE (core) / DEFERRED (extras)**

Phase 3 Task 22: when a connection is `PENDING_REASSIGNMENT` or `REVOKED`, workflows that reference it must not execute.

- [x] `ConnectionService.validateConnectionsActive(List<Long>)` added in `platform-connection-api` + `ConnectionServiceImpl` — throws `ConfigurationException(CONNECTION_NOT_ACTIVE)` with a descriptive message if any connection in the list is non-`ACTIVE`. Null-or-empty input short-circuits.
- [x] `ProjectDeploymentFacadeImpl.createProjectDeploymentWorkflowJob` now fetches the deployment workflow's connection IDs and invokes `validateConnectionsActive` *before* `principalJobFacade.createJob`. This covers user-initiated runs.
- [x] Compiles clean (`./gradlew :server:libs:platform:platform-connection:platform-connection-service:compileJava :server:libs:automation:automation-configuration:automation-configuration-service:compileJava`).
- [x] **Follow-up (done):** trigger-initiated coverage wired via a new `JobPrincipalAccessor.validateConnectionsForJob(jobPrincipalId, workflowUuid)` hook. `ProjectDeploymentJobPrincipalAccessor` resolves the deployment workflow's connection IDs and delegates to `ConnectionService.validateConnectionsActive`. EE `IntegrationJobPrincipalAccessor` is a documented no-op (embedded model doesn't carry user-owned connections with reassignment lifecycle). `TriggerCompletionHandler.handle` calls the hook right before `createJob`, so cron/webhook/listener triggers all short-circuit when a referenced connection is non-`ACTIVE`.
- [x] **Follow-up (done):** `WORKFLOW_PAUSED` value added to `ConnectionAuditEvent` enum. `ProjectDeploymentFacadeImpl.createProjectDeploymentWorkflowJob` catches `ConfigurationException` from validation, iterates the blocked connections, and publishes `WORKFLOW_PAUSED` via injected `ConnectionAuditPublisher` with `projectDeploymentId`, `workflowId`, and `connectionStatus` context data, then rethrows. `WORKFLOW_RESUMED` remains implicit (next successful run).
- [x] **Unit tests added:** `ConnectionServiceTest` covers `validateConnectionsActive` (all-active happy path, PENDING_REASSIGNMENT rejection with `CONNECTION_NOT_ACTIVE` error key, null/empty short-circuit). `ProjectDeploymentJobPrincipalAccessorTest` covers the trigger-side hook delegating to `ConnectionService.validateConnectionsActive` with the deployment-workflow's connection IDs.

---

## G4. User-removal → `markConnectionsPendingReassignment` glue — **DONE**

The mutation + facade method exist (`ConnectionReassignmentFacadeImpl.markConnectionsPendingReassignment`) but nothing currently triggers them when a user is removed from a workspace.

- [x] `WorkspaceUserRemovedEvent(long workspaceId, String userLogin)` added at `automation-configuration-api/.../event/WorkspaceUserRemovedEvent.java`.
- [x] `WorkspaceUserRemovalListener` added at `automation-configuration-service/.../listener/WorkspaceUserRemovalListener.java` — `@TransactionalEventListener(phase = AFTER_COMMIT)` that invokes `connectionReassignmentFacade.markConnectionsPendingReassignment(workspaceId, userLogin)`.
- [x] Compiles clean.
- [x] **Producer added:** `WorkspaceUserFacade.removeUserFromWorkspace(workspaceId, userLogin)` in `automation-configuration-api`/`-service` publishes `WorkspaceUserRemovedEvent` via `ApplicationEventPublisher`. The method is admin-only and validated (`Assert.hasText` on `userLogin`).
- [x] **GraphQL mutation added:** `removeWorkspaceUser(workspaceId: ID!, userLogin: String!): Boolean!` in new `workspace-user.graphqls` + `WorkspaceUserGraphQlController` with `@PreAuthorize(ADMIN)`.
- [x] **Unit test:** `WorkspaceUserFacadeTest` asserts the event is published with correct payload and rejects empty `userLogin`.
- [ ] Client: surface the reassignment dialog (already built — `ConnectionReassignmentDialog.tsx`) before confirming removal if the user owns workspace connections (deferred UI work; the backend mutation is ready to be wired to a Settings → Members remove button).
- [x] **Unit test added:** `WorkspaceUserRemovalListenerTest` verifies the listener delegates to `ConnectionReassignmentFacade.markConnectionsPendingReassignment(workspaceId, userLogin)` on `WorkspaceUserRemovedEvent`. Full integration test (with DB-backed reassignment) is deferred until the producer endpoint exists.

---

## G5. Grouped connection picker UI — **DONE (tests pending)**

Phase 1 plan calls for a picker grouped by visibility scope. The current `ConnectionTabConnectionSelect.tsx` renders a flat select.

- [x] `ConnectionI.visibility` is already returned by the existing query (confirmed in `workflowEditorProvider.tsx`).
- [x] `ConnectionTabConnectionSelect.tsx` now renders options under Radix `SelectGroup` + `SelectLabel` headers: PRIVATE → PROJECT → WORKSPACE → ORGANIZATION. `useMemo` computes the grouped structure; groups with no connections are hidden.
- [x] `ConnectionScopeBadge` rendered next to each option (color-coded icon + label per visibility).
- [x] Existing behaviours preserved: selection, "Create new connection" CTA, clear-button, environment badge, tag list.
- [ ] Update/extend `ConnectionTabConnectionSelect.test.tsx` to assert grouped rendering (deferred).

---

## Done criteria

- [ ] `./gradlew spotlessApply check testIntegration` green for affected `platform-connection-*` and `automation-configuration-*` modules.
- [ ] `cd client && npm run check` green.
- [ ] Every gap item above is either implemented or explicitly marked `WONTFIX` with rationale appended in this plan.
