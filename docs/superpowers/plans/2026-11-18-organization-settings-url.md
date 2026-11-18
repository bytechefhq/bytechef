# Organization Settings URL Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve Organization settings once under `/settings/...`, move Memories into the Automation settings section, and redirect the old URLs.

**Architecture:** Two small pure modules decide the platform mode (URL first, then the persisted `usePlatformTypeStore` mode) and map old settings URLs to new ones. One menu builder produces the full settings menu for a mode with absolute hrefs; one `SettingsLayout` renders it for `/automation/settings`, `/embedded/settings` and `/settings`. `routes.tsx` and `App.tsx` consume these.

**Tech Stack:** React 19, react-router-dom (data router, `redirect` loaders), zustand, vitest + Testing Library.

**Spec:** `docs/superpowers/specs/2026-11-18-organization-settings-url-design.md`

## Global Constraints

- Client only; work in `/Volumes/Data/bytechef/bytechef-7598/client`.
- Organization page keys (relative to `/settings`): `users`, `billing`, `ai-providers`, `ai/skills`, `mcp-server`, `custom-components`, `api-connectors`, `notifications`, `identity-providers`, `registered-clients`, `admin-api-keys`, `audit-events`.
- Memories URL stays `/automation/settings/ai/memories`; no Memories entry in Embedded settings.
- Permissions (`PrivateRoute` authorities, `EEVersion`) and feature flags of every page are unchanged.
- CLAUDE.md client rules: sort-keys, `I`/`Props` interface suffix, sorted import destructures, `twMerge` not `cn` in new code, descriptive names.
- Commits: subject only, `7598 client - <Description>`, no trailers.

## Review Focus

- Old deep links with a suffix and query (`/embedded/settings/ai/skills/42?tagId=3`) must land on `/settings/ai/skills/42?tagId=3`.
- `/settings/...` opened in a fresh browser (no persisted mode) must render the Automation menu and sidebar, not an empty one.
- Feature-flagged items (API Connectors `ff-207`, Custom Components and Admin API Keys `ff-1024`, Git Configuration `ff-1039`, MCP Server `ff-2197`, Identity Providers `ff-1040`, Billing) must still hide with absolute hrefs.
- A section item (`/automation/settings/workspace-api-keys`) must not be highlighted as current on `/settings/admin-api-keys` and vice versa.
- `/embedded/settings/ai/memories` must not be swallowed by the Organization redirect (memories is not an Organization key) and must go to `/automation/settings/ai/memories`.

---

### Task 1: Platform mode and settings path helpers

**Files:**
- Create: `src/shared/navigation/platformMode.ts`, `src/shared/navigation/settingsPaths.ts`
- Test: `src/shared/navigation/tests/platformMode.test.ts`, `src/shared/navigation/tests/settingsPaths.test.ts`

**Interfaces:**
- Produces: `resolvePlatformMode(pathname: string, storedType: PlatformType | undefined): PlatformType`
- Produces: `ORGANIZATION_SETTINGS_PATH = '/settings'`, `ORGANIZATION_SETTINGS_PAGES: readonly string[]` (the keys above, in menu order), `AI_PROVIDERS_SETTINGS_PATH = '/settings/ai-providers'`, `AI_SKILLS_SETTINGS_PATH = '/settings/ai/skills'`, `BILLING_SETTINGS_PATH = '/settings/billing'`, `API_CONNECTORS_SETTINGS_PATH = '/settings/api-connectors'`, `MEMORIES_SETTINGS_PATH = '/automation/settings/ai/memories'`
- Produces: `getOrganizationSettingsRedirectPath(pathname: string, search: string): string | undefined`

- [ ] **Step 1: Write failing tests**

`platformMode.test.ts`: `/automation/projects` + EMBEDDED stored → AUTOMATION; `/embedded/connections` + AUTOMATION stored → EMBEDDED; `/settings/users` + EMBEDDED → EMBEDDED; `/settings/users` + undefined → AUTOMATION.

`settingsPaths.test.ts`:
- `('/automation/settings/users', '')` → `'/settings/users'`
- `('/embedded/settings/ai/skills/42', '?tagId=3')` → `'/settings/ai/skills/42?tagId=3'`
- `('/automation/settings/api-connectors/new/import', '')` → `'/settings/api-connectors/new/import'`
- `('/automation/settings/ai/memories', '')` → `undefined`
- `('/automation/settings/workspace-api-keys', '')` → `undefined`
- `('/automation/settings/users-extra', '')` → `undefined` (key must match a whole segment)

- [ ] **Step 2: Run, expect FAIL** — `npx vitest run src/shared/navigation/tests` (modules missing)
- [ ] **Step 3: Implement both modules.** Redirect: match `^/(automation|embedded)/settings/(<key>)(/.*)?$` for each key, return `/settings/<key><rest><search>`.
- [ ] **Step 4: Run, expect PASS**
- [ ] **Step 5: Commit** `7598 client - Add platform mode and Organization settings path helpers`

### Task 2: Settings menu builder and layout

**Files:**
- Create: `src/shared/layout/settingsNavItems.ts`, `src/shared/layout/SettingsLayout.tsx`
- Modify: `src/shared/layout/Settings.tsx` (visibility checks by page key)
- Test: `src/shared/layout/tests/settingsNavItems.test.ts`, `src/shared/layout/tests/SettingsLayout.test.tsx`, existing `src/shared/layout/Settings.test.tsx`

**Interfaces:**
- Consumes: Task 1 constants and `resolvePlatformMode`
- Produces: `getSettingsNavItems(mode: PlatformType): SettingsNavItemI[]`; `SettingsLayout` (no props) rendering `<Settings sidebarNavItems={getSettingsNavItems(resolvePlatformMode(pathname, currentType))} />`

Menu content (all hrefs absolute):
- AUTOMATION: `Current Workspace` heading, `/automation/settings/git-configuration` "Git Configuration", `/automation/settings/workspace-api-keys` "API Keys", `/automation/settings/ai/memories` "Memories", `Organization` heading, `/automation/settings/workspaces` "Workspaces", then Organization items.
- EMBEDDED: `/embedded/settings/signing-keys` "Signing Keys", `/embedded/settings/api-keys` "API Keys", `Organization` heading, Organization items.
- Organization items: Users, Billing, AI group (Providers `/settings/ai-providers`, Skills `/settings/ai/skills`), MCP Server, Custom Components, API Connectors, Notifications, Identity Providers, OAuth2 Clients (`registered-clients`), Admin API Keys, Audit Events — titles as today.

- [ ] **Step 1: Write failing tests**
  - builder: AUTOMATION contains `/automation/settings/ai/memories` and no Memories under the AI group; EMBEDDED contains no `ai/memories` href; every Organization href starts with `/settings/`.
  - `Settings.test.tsx`: add a case — with `ff-207` off, `{href: '/settings/api-connectors'}` is hidden; on, it shows. Add a case — on `/settings/admin-api-keys`, `/automation/settings/workspace-api-keys` "API Keys" is not current.
  - `SettingsLayout.test.tsx`: at `/settings/users` with stored EMBEDDED, "Signing Keys" renders; with no stored mode, "Current Workspace" renders; at `/automation/settings/users` with stored EMBEDDED, "Current Workspace" renders.
- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Implement.** In `Settings.tsx` compare `settingsPageKey(href)` (href after the last `/settings/`, or href itself if relative) against the existing keys.
- [ ] **Step 4: Run `npx vitest run src/shared/layout`, expect PASS**
- [ ] **Step 5: Commit** `7598 client - Build one settings menu per mode with absolute links`

### Task 3: Routes

**Files:**
- Modify: `src/routes.tsx`

**Interfaces:**
- Consumes: `SettingsLayout`, `getOrganizationSettingsRedirectPath`, `ORGANIZATION_SETTINGS_PAGES`, `AI_SKILLS_SETTINGS_PATH`, `MEMORIES_SETTINGS_PATH`

- [ ] **Step 1: Restructure.**
  - Remove the Memories route from `platformSettingsRoutes.children`; add it to the `/automation/settings` children. Delete `platformSettingsRoutes.navItems`, `currentWorkspaceSettingsRoutes.navItems` and `organizationSettingsNavItem` (moved to the builder).
  - Add a top-level `settings` route next to `automation`/`embedded` under `/`: `element: <SettingsLayout />`, children `platformSettingsRoutes.children`, index redirect to `users`, `errorElement: <ErrorPage />`.
  - `/automation/settings` and `/embedded/settings`: `element: <SettingsLayout />`; replace `...platformSettingsRoutes.children` with one redirect route per key, `path: '<key>/*'`, loader `({request}) => redirect(getOrganizationSettingsRedirectPath(url.pathname, url.search)!)`. Under `/embedded/settings` add `ai/memories` → `MEMORIES_SETTINGS_PATH`.
  - Legacy `/automation/ai/skills...` loaders redirect to `AI_SKILLS_SETTINGS_PATH` (and `${AI_SKILLS_SETTINGS_PATH}/${skillId}`).
- [ ] **Step 2: Run `npx vitest run src/routes.loader.test.ts src/routes.environment.test.ts`, expect PASS**
- [ ] **Step 3: Commit** `7598 client - Serve Organization settings under /settings and redirect the old URLs`

### Task 4: App shell mode

**Files:**
- Modify: `src/App.tsx`

- [ ] **Step 1:** Compute `const platformMode = resolvePlatformMode(location.pathname, currentType)` (from `usePlatformTypeStore`). Use it for `navigation` selection, `useLoadWorkspaceScopes` and the `TrialBanner` condition. Keep the effect that stores the mode only for URL-derived modes (unchanged). Getting-started checks keep their path conditions.
- [ ] **Step 2: Run `npx vitest run src/shared/layout src/shared/navigation`, expect PASS**
- [ ] **Step 3: Commit** `7598 client - Keep the current mode's sidebar on Organization settings`

### Task 5: Links, Skills base path and Memories

**Files:**
- Modify: `src/config/useFetchInterceptor.ts`, `src/shared/layout/TrialBanner.tsx`, `src/shared/components/copilot/CopilotPanel.tsx`, `src/shared/components/ai/model-picker/ModelPicker.tsx`, `src/pages/automation/knowledge-bases/components/KnowledgeBasesEmptyState.tsx`, `src/pages/platform/workflow-editor/nodes/ClusterFrameShell.tsx`, `src/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader.tsx`, `src/pages/platform/cluster-element-editor/ai-agent-editor/components/AiAgentHeader.tsx`, `src/ee/pages/settings/platform/api-connectors/components/ApiConnectorCreateMenu.tsx`, `src/ee/pages/settings/platform/api-connectors/pages/hooks/useImportApiConnector.ts`, Skills hooks/components using `getAiSkillsBasePath`, `src/pages/automation/ai/memories/Memories.tsx`
- Delete: `src/pages/automation/ai/skills/utils/getAiSkillsBasePath.ts` and its test
- Test: update the existing tests listed by `grep -rn "/automation/settings/\(billing\|ai-providers\|ai/skills\|api-connectors\)\|/embedded/settings/ai" src`, and `Memories.test.tsx`

- [ ] **Step 1: Update tests first** to expect `/settings/...` paths; Skills tests expect `/settings/ai/skills` from any mount; Memories: replace the Embedded test with "lists the selected workspace memories wherever it is rendered" (`renderAt('/embedded/settings/ai/memories')` → workspace 7).
- [ ] **Step 2: Run, expect FAIL**
- [ ] **Step 3: Replace hard-coded paths with Task 1 constants; replace `getAiSkillsBasePath(...)` with `AI_SKILLS_SETTINGS_PATH`; drop the `/embedded/` branch and `DEFAULT_WORKSPACE_ID` use in `Memories.tsx`.**
- [ ] **Step 4: `grep -rn "/automation/settings/\(users\|billing\|ai-providers\|ai/skills\|mcp-server\|custom-components\|api-connectors\|notifications\|identity-providers\|registered-clients\|admin-api-keys\|audit-events\)" src` returns no hits outside redirect tests**
- [ ] **Step 5: Run the touched tests, expect PASS**
- [ ] **Step 6: Commit** `7598 client - Link to Organization settings under /settings`

### Task 6: Verify

- [ ] `npm run format`, then `npm run check` (lint, typecheck, tests): all pass.
- [ ] Run the dev client against the dev server and check: `/settings/users` from Automation and from Embedded keeps the matching sidebar and menu; `/automation/settings/audit-events`, `/embedded/settings/ai/skills` redirect; Memories shows under Current Workspace and opens `/automation/settings/ai/memories`; Embedded menu has no Memories.
