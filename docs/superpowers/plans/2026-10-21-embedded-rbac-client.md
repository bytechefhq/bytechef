# Embedded RBAC Client, E2E and Docs Implementation Plan (Plan C)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The client shows embedded navigation, pages and controls according to the member's embedded scopes in the selected environment. Embedded members can be managed from Embedded > Settings > Members. Playwright pins the behaviour end to end, and the docs describe it.

**Architecture:** Embedded scopes get their own store slice, loader and hooks, mirroring the automation ones (`usePermissionStore`, `useLoadWorkspaceScopes`, `useHasWorkspaceScope`, `useAccessibleEnvironmentIds`), fed by `myEmbeddedScopes(environment)`. Routes move from `[ADMIN]` to `[ADMIN, USER]`, with an embedded access guard, and every page gates its controls with `useHasEmbeddedScope`. The workflow editors reuse the existing read-only provider. Run sheets reuse `detailsHidden`.

**Tech Stack:** React 19, TypeScript, TanStack Query v5, Zustand, graphql-codegen, Vitest, Playwright, MDX docs. Plus one small Spring GraphQL addition (Task 3).

**Spec:** `docs/superpowers/specs/2026-10-19-embedded-rbac-design.md`. The server side is Plans A and B (`docs/superpowers/plans/2026-10-19-embedded-rbac-engine.md`, `docs/superpowers/plans/2026-10-20-embedded-rbac-gates.md`). Their "Plan C" and "Carried into Plan C" lists are binding.

## Global Constraints

- Client conventions from CLAUDE.md apply:
  - object keys in natural ascending order;
  - interfaces end in `I` or `Props`;
  - named imports sorted;
  - Lucide icons with the `Icon` suffix;
  - `twMerge`, not `cn()`;
  - `fieldset` for form groups;
  - `useMemo` instead of IIFEs;
  - `||` for JSX fallbacks;
  - hook order `useState` → `useRef` → store hooks → custom hooks → derived values → `useEffect`;
  - `useRef` names end in `Ref`;
  - no non-null assertion on an optional chain;
  - descriptive names.
- No explanatory inline comments. Commits are `1051 client - <desc>` (client), `1051 <desc>` (server), `1051 docs - <desc>` (docs), with no Co-Authored-By trailer.
- GraphQL operations go in `client/src/graphql/embedded/configuration/*.graphql`, and are regenerated with `cd client && npx graphql-codegen`. Operations and the generated file are committed separately.
- Fail closed. A scope that is loading, errored or unknown means "not allowed". CE and tenant admins are always allowed, exactly as `useHasWorkspaceScope` does.
- Embedded scopes are separate from workspace scopes. An embedded hook never reads `workspaceScopeStates`, and an automation hook never reads the embedded slice.
- Client-only gating is a failure. Every control hidden in this plan already has a server gate (Plans A and B). Never hide something the server allows, and never show something the server denies.
- Credentials the server withholds (connection credentials without the edit scope, instance configuration credentials without `INTEGRATION_INSTANCE_CONFIGURATION_EDIT`, the MCP secret key and URL without `MCP_EDIT`) are shown as "Hidden" with a short reason, never as empty editable fields. A save must never send back an empty credential map that would wipe stored values.
- Verification per task: `cd client && npm run check` (lint, typecheck, Vitest). Run `npm run format` first. Server tasks run the full `check` of every touched module with `DOCKER_HOST=unix:///Volumes/Data/Users/ivicac2/.orbstack/run/docker.sock`.
- Playwright runs against this branch:
  - a dedicated database `bytechef_e2e_<n>`;
  - the server on port 9556;
  - a free Vite port, with `PLAYWRIGHT_BASE_URL` set;
  - Mailpit running.
  Never touch another session's server or Vite instance. Stop your own processes by PID.

## Review Focus

1. **A member with a role only in Production opens Embedded while Development is selected.** They see the "No access to Development" notice and the environment selector offers only Production, not a blank page or an error toast. Pinned in Task 2 (`EmbeddedEnvironmentAccessGuard` test) and Task 9 (per-environment spec).
2. **A Viewer opens the Edit dialog of a connection or instance configuration whose credentials were withheld.** The credential fields show "Hidden" and saving never sends empty credentials. Pinned in Task 6 (`ConnectionDialog` and configuration dialog tests).
3. **A member's role changes while they are signed in.** Their scopes refresh after the membership mutation (`invalidateMyEmbeddedScopeQueries`), and on the next scope fetch for other sessions, with no stale "allowed" state. Pinned in Task 1 (loader test) and Task 3 (members page test).
4. **A tenant admin with no embedded row.** Everything stays visible and enabled, exactly as today. Pinned in every page task's tests (`testATenantAdminSeesEveryControl`) and in Task 9.
5. **A non-member tenant user.** The Embedded nav is hidden and every `/embedded/...` URL shows the "not an embedded member" notice instead of firing queries that fail. Pinned in Task 2 and Task 9.

---

## Scope names used by the client

`EmbeddedScopeType` is a string-literal union of exactly the scopes the server enforces on the embedded side:

- `EMBEDDED_VIEW`, `EMBEDDED_MEMBER_MANAGE`, `EMBEDDED_SETTINGS_MANAGE`
- `INTEGRATION_CREATE`, `INTEGRATION_PUBLISH`, `INTEGRATION_SETTINGS`, `INTEGRATION_DELETE`
- `INTEGRATION_INSTANCE_CONFIGURATION_VIEW` / `_CREATE` / `_EDIT` / `_DELETE`
- `CONNECTED_USER_VIEW` / `_MANAGE` / `_DELETE`
- `APP_EVENT_VIEW` / `_CREATE` / `_EDIT` / `_DELETE`
- `WORKFLOW_VIEW` / `_CREATE` / `_EDIT` / `_DELETE`
- `EXECUTION_VIEW`, `EXECUTION_DETAILS_VIEW`, `EXECUTION_DELETE`
- `CONNECTION_VIEW` / `_EDIT` / `_DELETE`
- `MCP_VIEW` / `_CREATE` / `_EDIT` / `_DELETE`
- `API_KEY_VIEW` / `_CREATE` / `_DELETE`

Page gating table, used by Tasks 2, 4, 5 and 6. "Dev" means the scope is checked in Development (`environmentId = 0`). "Env" means the currently selected environment.

| Page / control | Scope |
|---|---|
| Embedded nav and every embedded route | member: any scope in any environment (the union is non-empty) |
| Integrations page, Integration editor, Automations (Hub) page and editor | `WORKFLOW_VIEW` Dev |
| Create integration / Hub project | `INTEGRATION_CREATE` (union) |
| Edit integration, tags, create/edit/delete/import workflow, Hub workflow edit | `WORKFLOW_CREATE` / `WORKFLOW_EDIT` / `WORKFLOW_DELETE` Dev |
| Publish integration / Hub project | `WORKFLOW_EDIT` Dev and `INTEGRATION_PUBLISH` |
| Delete integration / Hub project | `INTEGRATION_DELETE` |
| Workflow permission expression, Hub project settings (visibility) | `INTEGRATION_SETTINGS` |
| Configurations page | `INTEGRATION_INSTANCE_CONFIGURATION_VIEW` Env; create `_CREATE` Env; edit, enable, tags, version, workflow edit `_EDIT` Env; delete `_DELETE` Env |
| Connected Users page and sheet | `CONNECTED_USER_VIEW` Env; enable/disable user, instance, workflows, project workflows `_MANAGE` Env; delete user, instance, project workflow `_DELETE` Env |
| App Events | `APP_EVENT_VIEW`; create `_CREATE` (still Development only, as today); edit `_EDIT`; delete `_DELETE` |
| Executions | `EXECUTION_VIEW` Env; run details `EXECUTION_DETAILS_VIEW` Env (`detailsHidden`, no task fetch) |
| Connections | `CONNECTION_VIEW` Env; create and edit `CONNECTION_EDIT` Env; delete `CONNECTION_DELETE` Env |
| MCP Servers | `MCP_VIEW` Env; create `MCP_CREATE` Env; edit, enable, tags, components, tools `MCP_EDIT` Env; delete server `MCP_DELETE` Env; URL/secret shown only with `MCP_EDIT`; regenerate URL only for tenant admins |
| MCP integration configurations (in MCP pages) | `INTEGRATION_INSTANCE_CONFIGURATION_VIEW` / `_CREATE` / `_EDIT` / `_DELETE` Env (create also `MCP_EDIT`) |
| Settings > Signing Keys | `EMBEDDED_SETTINGS_MANAGE` Env |
| Settings > API Keys | `API_KEY_VIEW` Env; create and rename `API_KEY_CREATE` Env; delete `API_KEY_DELETE` Env |
| Settings > Members | `EMBEDDED_VIEW` (union); manage `EMBEDDED_MEMBER_MANAGE` in every environment |

---

### Task 1: Embedded scope plumbing

**Files:**
- Create GraphQL documents in `client/src/graphql/embedded/configuration/`:
  - `myEmbeddedScopes.graphql`
  - `embeddedUsers.graphql`
  - `addEmbeddedUser.graphql`
  - `updateEmbeddedUserRole.graphql`
  - `removeEmbeddedUser.graphql`
  - `setEmbeddedUserEnvironmentRole.graphql`
  - `removeEmbeddedUserEnvironmentRole.graphql`
  - `embeddedApiKeys.graphql`
  - `createEmbeddedApiKey.graphql`
  - `updateEmbeddedApiKey.graphql`
  - `deleteEmbeddedApiKey.graphql`

  Field names come from `embedded-user.graphqls` and `embedded-api-key.graphqls`.
- Regenerate: `client/src/shared/middleware/graphql.ts` and `graphql-types.ts`.
- Modify: `client/src/shared/stores/usePermissionStore.ts` (add an `embeddedScopeStates` slice keyed by environment id, plus `-1` for the union).
- Create:
  - `client/src/shared/hooks/useLoadEmbeddedScopes.ts`
  - `useHasEmbeddedScope.ts`
  - `useAccessibleEmbeddedEnvironmentIds.ts`
  - `useIsEmbeddedMember.ts`
- Modify:
  - `client/src/shared/queries/permissions.queries.ts` (`MY_EMBEDDED_SCOPES_QUERY_KEY`, `invalidateMyEmbeddedScopeQueries`)
  - `client/src/App.tsx` (mount the loader when `currentType === PlatformType.EMBEDDED`)
- Test: `client/src/shared/hooks/tests/{useHasEmbeddedScope,useLoadEmbeddedScopes,useAccessibleEmbeddedEnvironmentIds,useIsEmbeddedMember}.test.ts`.

**Interfaces (Produces):**

```ts
export type EmbeddedScopeType = /* the union above */;
export const useHasEmbeddedScope: (scope: EmbeddedScopeType, environmentId?: number) => boolean; // environmentId undefined = current env; -1 = union (any env)
export const useEmbeddedScopeState: (scope: EmbeddedScopeType, environmentId?: number) => {editionUnknown: boolean; error: boolean; granted: boolean; loading: boolean};
export const useAccessibleEmbeddedEnvironmentIds: () => number[] | undefined; // undefined when CE, tenant admin, or still loading
export const useIsEmbeddedMember: () => {loading: boolean; member: boolean}; // tenant admin or CE -> member true
export const UNION_ENVIRONMENT_ID = -1;
```

Loader behaviour, copied from `useLoadWorkspaceScopes`:
- It is gated on EE and an authenticated session.
- It loads the union (`environment: null`), the current environment and DEVELOPMENT.
- The first fetch error raises the toast `embedded-permissions-error`. A background refetch error keeps the last good scopes.
- The query key is `['MyEmbeddedScopes', {environment}]`.

- [ ] **Step 1: Write the failing tests.** Mirror `useHasWorkspaceScope.test.ts` case for case. Then add:
  - `testTheUnionAnswersAScopeHeldInAnyEnvironment`
  - `testAWorkspaceScopeIsNeverReadForAnEmbeddedScope`: workspace slice has `WORKFLOW_EDIT`, embedded slice empty, expect false
  - `testANonMemberIsNotAMember`: union loaded and empty
  - `testInvalidatingRefetchesEveryEmbeddedScopeQuery`
- [ ] **Step 2: Run them and confirm they fail.** `cd client && npx vitest run src/shared/hooks/tests/useHasEmbeddedScope.test.ts`
- [ ] **Step 3: Implement.** Add the documents and run `npx graphql-codegen`, then add the store slice, the hooks and the App mount.
- [ ] **Step 4: Run `npm run format && npm run check`.**
- [ ] **Step 5: Commit**, in two commits:
  - `1051 client - Add embedded membership, scope and API key GraphQL operations` (documents and generated files)
  - `1051 client - Load embedded scopes and answer them per environment`

### Task 2: Routes, navigation, settings and the environment guard

**Files:**
- Modify:
  - `client/src/routes.tsx`: every embedded route and the embedded settings routes go from `[ADMIN]` to `[ADMIN, USER]`; the `embedded` route gets `element: <EmbeddedEnvironmentAccessGuard/>`.
  - `client/src/App.tsx`: in `filteredEmbeddedNavigation`, each entry needs its page's view scope (gating table) in the current environment; Integrations and Automations need `WORKFLOW_VIEW` in Development and keep the existing "Development only" rule; non-members see none.
  - `client/src/shared/layout/Settings.tsx`: remove `/embedded/settings/api-keys` and `/embedded/settings/signing-keys` from `TENANT_ADMIN_NAV_ITEM_HREFS` and gate them by scope; add a Members item.
  - `client/src/shared/layout/EmbeddedIndexRedirect.tsx`: go to the first page the member can view.
  - `client/src/shared/layout/app-sidebar/AppSidebar.tsx`: for embedded, use `useAccessibleEmbeddedEnvironmentIds()`.
- Create: `client/src/shared/layout/EmbeddedEnvironmentAccessGuard.tsx`, mirroring `AutomationEnvironmentAccessGuard`:
  - a non-member gets the title `No access to Embedded` and the message `You are not an embedded member. Ask an embedded admin or a tenant admin to add you.`;
  - a member with no role in the current environment gets `No access to <Env>` and `You have no role in this environment. Switch to an environment you have access to, or ask an embedded admin for access.`;
  - `/embedded/account` stays exempt.
- Test: rewrite the embedded parts of `routes.test.tsx`, `App.test.tsx`, `Settings.test.tsx`, `EmbeddedIndexRedirect.test.tsx` and `AppSidebar.test.tsx`; create `EmbeddedEnvironmentAccessGuard.test.tsx`.

- [ ] **Step 1: Write the failing tests:**
  - `testEveryEmbeddedRouteAdmitsUsers`: `it.each` over the embedded paths, expecting `[ADMIN, USER]`
  - `testANonMemberSeesNoEmbeddedNavigation`
  - `testAMemberSeesOnlyPagesTheyCanView`
  - `testTheGuardShowsTheEnvironmentNoticeForAProductionOnlyMember`
  - `testTheGuardShowsTheMembershipNoticeForANonMember`
  - `testTheEnvironmentSelectorOffersOnlyAccessibleEmbeddedEnvironments`
  - `testSigningKeysNeedEmbeddedSettingsManage`
  - `testTheIndexRedirectsToTheFirstViewablePage`
  - `testATenantAdminSeesEveryEmbeddedEntry`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run `npm run format && npm run check`.**
- [ ] **Step 5: Commit** `1051 client - Open embedded routes and navigation to members by scope and environment`

### Task 3: Embedded > Settings > Members

**Files:**
- Server:
  - add `addEmbeddedUserByEmail(email: String!, role: WorkspaceRole!): EmbeddedUser!` to `embedded-user.graphqls`;
  - add `EmbeddedUserFacade.addEmbeddedUserByEmail(String email, WorkspaceRole role)`, gated `hasEmbeddedScopeInEveryEnvironment('EMBEDDED_MEMBER_MANAGE')` and audited as `EMBEDDED_USER_ADDED`. It resolves an existing tenant user by email through the ungated `UserService`, then delegates to `addEmbeddedUser`. An unknown email throws the same not-found error for every caller, so it reveals nothing beyond "no such user".
- Client:
  - create `client/src/ee/pages/settings/embedded/members/EmbeddedMembers.tsx`, `components/EmbeddedAddMemberDialog.tsx` and `components/EmbeddedMemberEnvironmentRoles.tsx`, reusing the automation `WorkspaceUserEnvironmentRoles` component if its props fit, otherwise a thin copy;
  - add the GraphQL document `addEmbeddedUserByEmail.graphql` and regenerate;
  - add the route `embedded/settings/members` with `[ADMIN, USER]` and the nav item (Task 2's list).
- Test:
  - `EmbeddedMembers.test.tsx`;
  - `EmbeddedUserGraphQlControllerTest` and `EmbeddedUserFacadeIntTest` cases for the new mutation;
  - pin the gate in `PreAuthorizeAnnotationTest`.

Behaviour mirrors `WorkspaceUsers.tsx`:
- rows come from `embeddedUsers`, and tenant admins show as `inherited` and cannot be edited;
- the role select is embedded-wide or per environment, inline;
- "Add member" asks for an email and a role;
- the remove button opens an AlertDialog.

`canManageMembers` is `EMBEDDED_MEMBER_MANAGE` in all three environments. After every mutation, call `invalidateMyEmbeddedScopeQueries` and refetch `embeddedUsers`. Errors are shown through the global interceptor; there is no per-mutation `onError` unless the form needs a reset.

- [ ] **Step 1: Write the failing tests.**
  - Server:
    - `testAnEmbeddedAdminAddsAnExistingUserByEmail`
    - `testAnUnknownEmailIsRefusedWithTheSameErrorForEveryone`
    - `testAnEmbeddedEditorCannotAdd`
  - Client:
    - `testAViewerSeesMembersButNoManageControls`
    - `testAnAdminAddsAMemberByEmailAndTheListRefreshes`
    - `testSettingAProductionRoleTurnsTheMemberPerEnvironment`
    - `testTenantAdminsAreListedAsInheritedAndNotEditable`
    - `testScopesAreInvalidatedAfterARoleChange`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement**, server first, then codegen, then the client.
- [ ] **Step 4: Run the server module `check`s** (EECFG, the configuration GraphQL module), **then `npm run format && npm run check`.**
- [ ] **Step 5: Commit**, in three commits:
  - `1051 Add an embedded member by email`
  - `1051 client - Add the embedded add-member-by-email operation`
  - `1051 client - Add the Embedded Members settings page`

### Task 4: Integrations, Hub templates and their editors

**Files:**
- Modify the Integrations page and the components named in the inventory (`client/src/ee/pages/embedded/integrations/**`), the Integration editor (`client/src/ee/pages/embedded/integration/**`), the Automations page (`client/src/ee/pages/embedded/automation-workflows/**`) and the Hub editor (`client/src/ee/pages/embedded/automation-workflow/**`).
- Create `client/src/ee/pages/embedded/shared/hooks/useEmbeddedWorkflowReadOnly.ts`: returns `!useHasEmbeddedScope('WORKFLOW_EDIT', 0)` (Development).
- Create `useEmbeddedWorkflowViewOnlyNotice.ts`, mirroring `useProjectWorkflowViewOnlyNotice`.
- Tests: extend the existing component tests and add `Integrations.test.tsx` and `IntegrationReadOnly.test.tsx`.

Rules:
- Both editors wrap in `WorkflowEditorReadOnlyProvider readOnly={useEmbeddedWorkflowReadOnly()}`.
- Both headers pass `readOnly` to `WorkflowActionsButton` and show the same `View only` badge as `ProjectHeader`. Extract it into a small shared `WorkflowViewOnlyBadge` component used by all three headers.
- Every create, edit, delete, import, publish and settings control follows the gating table.
- The Hub project dialog's visibility switch and the workflow permission expression field need `INTEGRATION_SETTINGS`.

- [ ] **Step 1: Write the failing tests:**
  - `testAViewerSeesIntegrationsWithoutCreateEditPublishOrDelete`
  - `testAnEditorBuildsWorkflowsButCannotDeleteTheIntegration`
  - `testOnlyAnAdminEditsThePermissionExpression`
  - `testTheIntegrationEditorOpensReadOnlyWithoutDevelopmentWorkflowEdit`
  - `testTheHubEditorOpensReadOnlyWithoutDevelopmentWorkflowEdit`
  - `testTheViewOnlyBadgeShowsInBothEmbeddedEditors`
  - `testATenantAdminSeesEveryControl`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run `npm run format && npm run check`.**
- [ ] **Step 5: Commit** `1051 client - Gate embedded integrations, Hub templates and their editors on embedded scopes`

### Task 5: Configurations, connected users, app events and executions

**Files:** the pages and components under:
- `client/src/ee/pages/embedded/integration-instance-configurations/**`
- `connected-users/**`
- `app-events/**`
- `workflow-executions/**`

These are listed with file and line in the inventory. Tests: extend the existing tests and add `ConnectedUsers.test.tsx`, `AppEvents.test.tsx` and `IntegrationInstanceConfigurations.test.tsx`. For the run sheet, add `WorkflowExecutionSheetContent.test.tsx`.

Rules:
- Gating follows the table.
- The embedded run sheet passes `detailsHidden={!useHasEmbeddedScope('EXECUTION_DETAILS_VIEW')}` and enables the task-execution query only when details are visible, mirroring the automation `WorkflowExecutionSheetContent`.
- The embedded editors' test output (`WorkflowExecutionsTestOutput`) needs no gate: running a test already requires `WORKFLOW_EDIT` in Development, and Editors hold `EXECUTION_DETAILS_VIEW`.

- [ ] **Step 1: Write the failing tests:**
  - `testAProductionEditorEditsOnlyProductionConfigurations`
  - `testAViewerSeesConnectedUsersWithoutEnableOrDelete`
  - `testOnlyAnAdminDeletesAConnectedUser`
  - `testAppEventCreateStaysDevelopmentOnlyAndNeedsAppEventCreate`
  - `testAViewerRunSheetHidesDetailsAndFetchesNoTaskData`
  - `testATenantAdminSeesEveryControl`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run `npm run format && npm run check`.**
- [ ] **Step 5: Commit** `1051 client - Gate embedded configurations, connected users, app events and runs on embedded scopes`

### Task 6: Connections, MCP, signing keys, API keys and withheld credentials

**Files:**
- Embedded Connections page and list item.
- The shared `client/src/shared/components/connection/ConnectionDialog.tsx`, together with its parameter components and the instance configuration dialog's connection step.
- The embedded MCP pages, plus the shared automation MCP components `McpComponentToolListItem.tsx:100` and `McpComponentListItemDropdownMenu.tsx:72`: replace the `embedded ||` / `!embedded &&` bypass with an embedded-scope check, and keep the automation branch on `useHasWorkspaceScope`.
- `McpServerList.tsx` / `McpServerConfigurationCode` (URL and secret).
- The Signing Keys page and table.
- `client/src/ee/pages/settings/embedded/api-keys/ApiKeys.tsx`: repoint to the `embeddedApiKeys` / `createEmbeddedApiKey(input: CreateEmbeddedApiKeyInput)` / `updateEmbeddedApiKey(input: UpdateEmbeddedApiKeyInput)` / `deleteEmbeddedApiKey(id)` operations from Task 1.
- Shared `ApiKeyTable.tsx`: add a `canEdit` prop for the rename button, defaulting to `true`.

**Rules:**

*Gating*
- Follow the gating table.
- The automation Workspace API Keys page passes `canEdit` from `API_KEY_CREATE`, which matches the server.

*Withheld credentials*
- When the server withholds credentials, the connection record arrives with empty `parameters`, `authorizationParameters` and `connectionParameters`, and the caller lacks the edit scope.
- In that case the dialog shows each credential field as read-only "Hidden" text with the hint `Only members who can edit this connection can see its credentials.`
- Saving never sends those maps. This applies to automation connections as well: automation now withholds the same way, so check the automation connection dialog path too.
- The instance configuration dialog does the same with `INTEGRATION_INSTANCE_CONFIGURATION_EDIT`.

*MCP URL and secret*
- Without `MCP_EDIT`, the MCP URL shows `Hidden` with the hint `Only members who can edit this server can see its URL.`
- Hide the regenerate button unless the caller is a tenant admin.

*API key secret*
- The API key table no longer renders the full secret in the list.
- The secret is shown once on create, as automation does today.
- If the list's `secretKey` comes back obfuscated, render it as returned.

**Steps**

- [ ] **Step 1: Write the failing tests:**
  - `testAViewerSeesConnectionCredentialsAsHiddenAndSavingSendsNoCredentials`
  - `testAnEditorSeesAndEditsConnectionCredentials`
  - `testAutomationConnectionDialogAlsoShowsHiddenCredentials`
  - `testEmbeddedMcpNoLongerBypassesTheScopeCheck`
  - `testAViewerSeesTheMcpUrlAsHidden`
  - `testOnlyATenantAdminRegeneratesTheMcpUrl`
  - `testSigningKeysNeedEmbeddedSettingsManage`
  - `testEmbeddedApiKeysUseTheEmbeddedOperations`
  - `testAViewerHasNoApiKeysPage` (API_KEY_VIEW is an Editor scope)
  - `testATenantAdminSeesEveryControl`
- [ ] **Step 2: Run them and confirm they fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run `npm run format && npm run check`.**
- [ ] **Step 5: Commit**, in two commits:
  - `1051 client - Gate embedded connections, MCP, signing keys and API keys on embedded scopes`
  - `1051 client - Show withheld credentials as hidden and never send them back empty`

### Task 7: Playwright fixtures for embedded members

**Files:**
- Create: `client/test/playwright/fixtures/embeddedRoles.ts`, which exports `embeddedRolesTest`. It provides worker-scoped fixtures:
  - `adminApi`.
  - `embeddedRoleUsers`: users for `VIEWER`, `EDITOR` and `ADMIN` with an embedded-wide role, `PRODUCTION_EDITOR` (Editor in Production only), `DEVELOPMENT_EDITOR` (Editor in Development only), and an `OUTSIDER` (tenant user, not a member). Users are created with the existing `createRoleUser`/`createAccount` helpers, and roles are granted through the GraphQL membership mutations.
  - `embeddedData`: an integration with one workflow, a Development and a Production instance configuration, a connected user in each, an embedded connection in each, and an embedded MCP server in each, all created as the admin.
  - Teardown deletes everything it created, including the signing key that `connectedUserOwnership.spec.ts` currently leaks.
- Create `client/test/playwright/utils/embeddedRbacUtils.ts` with:
  - `embeddedInternalApi<T>(api, method, path, data?)` (prefix `/api/embedded/internal`);
  - `setEmbeddedRole(adminApi, userId, role)`;
  - `setEmbeddedEnvironmentRole(adminApi, userId, environment, role)`;
  - `openAsEmbeddedRoleUser(browser, baseURL, roleUser, environmentId)`.

- [ ] **Step 1:** Write `fixtures.smoke.spec.ts` under `tests/embedded-rbac/`. It asserts that every fixture user can sign in and that `myEmbeddedScopes` returns the expected scope set for each role.
- [ ] **Step 2:** Run it against a branch server and confirm it passes. Server setup per the Global Constraints.
- [ ] **Step 3: Commit** `1051 client - Add Playwright fixtures for embedded members and data`

### Task 8: Playwright specs for embedded RBAC

**Files:** create in `client/test/playwright/tests/embedded-rbac/`:
- `builtInRoles.spec.ts` (Viewer, Editor, Admin, and a tenant admin across every embedded page's controls, following the gating table)
- `perEnvironment.spec.ts` (Production-only and Development-only Editors: environment selector, guard notice, configurations, connections, MCP, editor read-only state)
- `serverGates.spec.ts` (an embedded `GATE_CASES` list in the same shape as `tests/rbac/serverGates.spec.ts`, one case per scope family, denied for Viewers and allowed for the minimum role)
- `membership.spec.ts` (add by email, per-environment roles, remove, inherited tenant admins, scope refresh after a change)
- `readOnlyEditor.spec.ts` (Integration and Hub editors)
- `runDetails.spec.ts` (Viewer sees the run without details and no task request goes out; Editor sees details)
- `credentials.spec.ts` (Viewer gets hidden connection, configuration and MCP credentials; Editor sees them; network responses carry no credentials for the Viewer)

Also modify `tests/rbac/serverGates.spec.ts`: remove the stale embedded entries in `TENANT_ADMIN_CASES`, which are now scope-gated, and keep the MCP secret rotation as tenant-admin only.

- [ ] **Step 1: Write the specs.**
- [ ] **Step 2: Run** `npx playwright test tests/embedded-rbac tests/rbac tests/audit` against a fresh branch server (Global Constraints). Every embedded RBAC spec must pass, and so must `tests/rbac` (including `connectedUserOwnership.spec.ts`) and `tests/audit`. Record the counts.
- [ ] **Step 3: Commit** `1051 client - Add embedded RBAC Playwright scenarios`

### Task 9: Docs

**Files:**
- Create: `docs/content/docs/platform/embedded/administration/users-and-roles.mdx`. Model it on `platform/settings/users.mdx`, using `<EEBadge />`, `<Callout>`, absolute links and a "See also" section. Sections:
  - Embedded members
  - Built-in roles
  - Permission scopes (catalogue table `| Scope | Minimum role |`)
  - Per-environment roles (`| Action | Result |` table)
  - What follows the selected environment
  - Workflow editors (read-only)
  - Run details
  - Credentials (what each role sees)
  - Embedded API keys are shared
  - What stays tenant admin only (rotating an MCP server's secret)
  - Notable rules (no last-Admin rule, an Admin may step down, Hub and connected users' projects are not visible to workspace roles)
- Modify:
  - `docs/content/docs/platform/embedded/meta.json` (list the page under Administration);
  - `docs/content/docs/platform/settings/users.mdx` (link to it, and state that automation connection credentials need `CONNECTION_EDIT`);
  - the embedded signing-keys and api-keys docs pages (who can use them).

- [ ] **Step 1: Write the page and the links.**
- [ ] **Step 2: Run the docs build or lint the repo uses** (check `docs/package.json`), and confirm the page renders and the navigation lists it.
- [ ] **Step 3: Commit** `1051 docs - Describe embedded users, roles and permissions`

### Task 10: Ticket and final checks

- [ ] **Step 1:** Run `cd client && npm run check` on the branch head. Then run the full `check` of every server module Plan C touched, and the Playwright suites from Task 8 once more on a fresh database.
- [ ] **Step 2:** Update #1051:
  - add embedded test scenarios in the same format as the automation scenarios (one per Playwright spec);
  - add an "Embedded" block to the Acceptance Criteria;
  - add a "Results after adding the embedded client, scenarios and docs (date)" block with commits and counts.
  Write the body to the scratchpad and send it with `gh issue edit 1051 -R bytechefhq/bytechef --body-file`.
- [ ] **Step 3:** If this branch changed nothing, there is no commit.

---

## Out of this plan

- A per-environment embedded Admin removing themselves past the every-environment gate. The Members page hides "Leave" for them. A server change would be needed to allow it, so it is noted for the user.
- Embedded custom roles and per-customer access: spec non-goals.
- Plan A and B parked minors (query caching, configuration-app 500s, audit event naming, tag lists).
