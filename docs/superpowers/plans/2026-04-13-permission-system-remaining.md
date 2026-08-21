# Permission System — Remaining Work

Tracks the gaps still open after the 2026-04-13 remediation session against
[`2026-04-06-permission-system.md`](2026-04-06-permission-system.md). Closed
items in that plan are not repeated here — see git log for what shipped.

## Status snapshot

| Task | Status | Note |
|------|--------|------|
| 8 — `PermissionServiceImpl` unit tests | ✅ Done | 20 tests in `PermissionServiceImplTest` |
| 11 — `ProjectUser` integration test | ⚠️ Partial | Mock-based `ProjectUserServiceImplTest` (13 tests) — full Spring Boot int test still missing |
| 12 — Auto-assign project creator as ADMIN | ✅ Done | `ProjectCreatedEvent` + EE listener |
| 13 — Auto-assign workspace creator as ADMIN | ✅ Done | Inline in EE `WorkspaceServiceImpl.create` |
| 20 — `@PreAuthorize` rollout to remaining services | ⚠️ Partial | MCP / agent / deployment services still unguarded |
| 24 — Client permission-based button visibility | ⚠️ Partial | Hooks used in only 4 components |
| 25 — Backfill migration | ✅ Done | `202604061200050_..._backfill_project_user.xml` |

## Open work

### Task 11 — Full integration test for `ProjectUserService`

**Why it matters:** the unit test verifies business rules but does not exercise
`@PreAuthorize` (requires Spring proxy) or actual SQL persistence. Without int
coverage, regressions in the security wiring or repository queries can ship
unnoticed.

**What's required:**
- New `ProjectUserServiceIntTest` under
  `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/test/java/.../service/`
- Stand up int-test infra in the EE module (currently has none):
  - `IntTestConfiguration` class
  - `application-testint.yml`
  - `testImplementation(":server:libs:test:test-int-support")` + Postgres
    Testcontainer wiring
  - Liquibase change-log discovery for the EE migrations
- Cover: persistence round-trip, last-admin protection under real transactions,
  `@PreAuthorize` enforcement with `@WithMockUser` (admin bypass + scoped user
  rejection)

### Task 20 — `@PreAuthorize` rollout to remaining services

**Already guarded:** `ProjectServiceImpl` (4), `ProjectWorkflowServiceImpl` (3),
`ProjectDeploymentServiceImpl` (2), `ConnectionServiceImpl` (4), EE
`WorkspaceUserServiceImpl` (3), EE `ProjectUserServiceImpl` (4).

**Still unguarded — work per-service, commit per-service** (the plan calls this
out explicitly because each addition breaks integration tests that don't supply
a security context):

1. **`WorkspaceMcpServerService`** —
   `assignMcpServerToWorkspace`, `removeMcpServerFromWorkspace` need
   workspace-scope checks; `getWorkspaceMcpServers` needs at least
   `hasWorkspaceRole(.., 'VIEWER')`.
2. **`WorkspaceConnectionFacade`** — connection create/update/delete on the
   workspace boundary.
3. **`ProjectDeploymentService`** — verify `enableProjectDeployment`,
   `createProjectDeployment`, deletion paths all carry the right scope
   (currently 2 methods are guarded; audit the rest).
4. **MCP project services** (`McpProjectService`, `McpProjectWorkflowService`,
   etc.) — currently unguarded.
5. **Agent services** under `automation-ai/` — `WorkspaceAiAgentService`,
   `WorkspaceAiToolService`, agent-execution facades.
6. **Knowledge-base services** under `automation-knowledge-base/` — document
   CRUD, chunk operations.

**Per-service workflow:**
- Add annotations.
- Run that module's `test` + `testIntegration`.
- For every breakage, add `@WithMockUser(authorities = "ROLE_ADMIN")` (int
  tests) or set `SecurityContextHolder` in `@BeforeEach` (unit tests).
- Single-purpose commit: `2311 Add @PreAuthorize to <ServiceName>`.

### Task 24 — Client permission-based button visibility

**Already gated:** `ProjectShareDialog`, `WorkflowShareDialog`,
`ProjectUsersDialog`, `ProjectTabButtons` (4 files).

**Still rendering unconditionally — sweep by area:**

- **Workflow editor toolbar** — edit, delete, duplicate, toggle, run, publish.
  Wire `useHasProjectScope('WORKFLOW_EDIT' / 'WORKFLOW_DELETE' /
  'WORKFLOW_TOGGLE' / 'DEPLOYMENT_PUSH')`.
- **Project/workflow list pages** — create / delete / duplicate buttons.
- **Connections** — create, edit, delete, reassign actions
  (`CONNECTION_CREATE` / `CONNECTION_EDIT` / `CONNECTION_DELETE`).
- **Deployments** — enable/disable/run buttons (`DEPLOYMENT_PUSH`,
  `DEPLOYMENT_TOGGLE`).
- **Agents / MCP** — agent create, agent execute, MCP server assign / remove
  (`AGENT_CREATE`, `AGENT_EXECUTE`, `MCP_MANAGE`).
- **Knowledge base** — document CRUD.
- **Workspace settings** — invite, role-change, remove member; covered for
  workspace dialogs but verify settings menu items.

**Pattern to follow:**

```tsx
const canEdit = useHasProjectScope(projectId, 'WORKFLOW_EDIT');

return canEdit ? <Button onClick={...}>Edit</Button> : null;
```

For multi-action menus, hide the menu item rather than disabling — disabled
items invite "why can't I click this?" support tickets. Where the whole
component is admin-only, prefer hiding the entry-point (e.g., settings tab)
over hiding individual buttons.

## Suggested ordering when picking this up

1. **Task 20 service-by-service**, smallest blast radius first
   (`WorkspaceMcpServerService` → `WorkspaceConnectionFacade` →
   `ProjectDeploymentService` → agents).
2. **Task 24** can run in parallel chunks per UI area; no test fallout.
3. **Task 11** last — int-test infra is a side-quest that can wait until
   another EE service module needs it too.
