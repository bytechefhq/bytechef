# AI Hub Task Templates + Agents Surfacing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the six AI Hub changes in `docs/superpowers/specs/2026-08-12-ai-hub-task-templates-and-agents-design.md`: home-view resource panel, Personal Agents → Task Templates rename (client+server+DB), ClipboardListIcon, Context → More popup, Agents surfacing in AI Hub and `/automation/chats`.

**Architecture:** Mostly-mechanical rename executed by ordered `perl -pi` sweeps + `git mv` (2,200 occurrences / 149 files), with hand-written changes for the four behavioral items. The new `workspaceChatAgents` GraphQL query lives in `automation-ai-agent-graphql`/`-service`, which already replicates the chat-webhook-execution-id logic (see `AiAgentFacadeImpl` ~line 845 comment — replication there is established precedent).

**Tech Stack:** React 19 + TS (client, Vitest), Spring Boot / Spring GraphQL (server, JUnit 5), Liquibase, graphql-codegen.

## Global Constraints

- Branch `claude/ai-hub-resource-selection-863527`; NEVER amend existing commits; fresh commits only.
- Commit messages: `5XXX client - <desc>` for client-only, `5XXX <desc>` for server; end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- Client: `sort-keys` (alphabetical object keys), interfaces end `I`/`Props`, icons imported with `Icon` suffix, `twMerge` not `cn()`, named imports sorted.
- Server: EE files keep ByteChef Enterprise header + `@version ee`; blank line before control statements; enum values stored as INT ordinals — never reorder.
- DB decision (user-approved): edit init changelogs in place; table names `ai_hub_task_template`, `ai_hub_task_template_tool`, `ai_hub_task_template_resource`, `ai_hub_task_template_schedule`; FK column `ai_hub_task.ai_hub_task_template_id`.
- Icon decision (user-approved): `ClipboardListIcon` for Task Templates. `BotIcon` stays for AI Agents.
- Agents-page click (user-approved): start a chat task via `createWorkflowChatAiHubTask`, same as Workflow Chats rows.
- After renaming Liquibase changelog files: delete stale copies under `server/ee/libs/ai/ai-hub/ai-hub-service/build/resources/`.
- After any `.graphqls` schema change: `cd client && npx graphql-codegen` to regenerate `src/shared/middleware/graphql.ts`; commit operations and generated file separately from server schema changes.
- Verification floor per task: client tasks `cd client && npm run check`; server tasks `./gradlew spotlessApply` then compile/tests of touched modules (redirect gradle output to a file and grep `^> Task .* FAILED` — piping loses the exit code).

---

### Task 1: Home-view resource panel opens on resource selection

**Files:**
- Modify: `client/src/ee/pages/automation/ai-hub/AiHub.tsx` (lines ~44–56 gate, ~287–349 `mainBody`)
- Test: `client/src/ee/pages/automation/ai-hub/tests/AiHub.homeResourcePanel.test.tsx` (new)

**Interfaces:**
- Consumes: `useAiHubTabsStore` (`rightPanelOpen`, `openTabs`), existing `AiHubHomePanel`, `AiHubPanel`, `AiHubResourcePanel`.
- Produces: no new exports; behavior change only.

- [ ] **Step 1: Write the failing test.** Mirror the mock style of `client/src/ee/pages/automation/ai-hub/tests/AiHubPanel.test.tsx` (read it first; reuse its store-reset helpers). Mock the heavy children so the test only asserts composition:

```tsx
import AiHub from '@/ee/pages/automation/ai-hub/AiHub';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/AiHubHomePanel', () => ({default: () => <div data-testid="home-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubPanel', () => ({default: () => <div data-testid="task-panel" />}));
vi.mock('@/ee/pages/automation/ai-hub/AiHubResourcePanel', () => ({
    default: () => <div data-testid="resource-panel" />,
}));
vi.mock('@/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider', () => ({
    AiHubRuntimeProvider: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

describe('AiHub home view resource panel', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
        });
    });

    it('renders the resource panel on the home view when a tab is open', () => {
        aiHubTabsStore.setState({
            activeTabId: 'tab-1',
            openTabs: [{fileId: '1', id: 'tab-1', kind: 'file', name: 'a.txt', viewMode: 'editor'}],
            rightPanelOpen: true,
        });

        render(
            <MemoryRouter initialEntries={['/automation/ai-hub']}>
                <AiHub />
            </MemoryRouter>
        );

        expect(screen.getByTestId('home-panel')).toBeInTheDocument();
        expect(screen.getByTestId('resource-panel')).toBeInTheDocument();
    });

    it('keeps the plain home view when no tabs are open', () => {
        render(
            <MemoryRouter initialEntries={['/automation/ai-hub']}>
                <AiHub />
            </MemoryRouter>
        );

        expect(screen.getByTestId('home-panel')).toBeInTheDocument();
        expect(screen.queryByTestId('resource-panel')).not.toBeInTheDocument();
    });
});
```

Additional mocks will be needed for hooks AiHub.tsx calls (`useAiHubTasksQuery`, `useResetAiHubStoresOnWorkspaceChange`, `useRecordReferencedArtifacts`, workspace/environment stores) — copy exactly what `AiHubPanel.test.tsx` / `AiHubTasksSidebar.test.tsx` already do for those.

- [ ] **Step 2: Run the test, verify it fails.** `cd client && npx vitest run src/ee/pages/automation/ai-hub/tests/AiHub.homeResourcePanel.test.tsx` — first test FAILS (`resource-panel` absent on home view).

- [ ] **Step 3: Implement the gate + layout change in `AiHub.tsx`.**

Add a subscription and widen the gate (replacing line ~48):

```tsx
const hasOpenTabs = useAiHubTabsStore((state) => state.openTabs.length > 0);

// The resource (right) panel shows for an active task AND on the home view the moment a composer
// pick opens a tab — "if I add a resource, I should see it immediately", before the first message
// creates the task. The home→task snapshot carry-over in setActiveTaskId keeps those tabs.
const showResourcePanel = rightPanelOpen && (hasActiveTask || hasOpenTabs);
```

Restructure `mainBody` so the split shell wraps BOTH views; only the left pane's content switches (delete the old `!hasActiveTask ? <AiHubHomePanel /> : (…)` fork):

```tsx
const mainBody = pendingUrlTask ? (
    /* …existing four-dot loader unchanged… */
) : (
    <div className="flex size-full">
        {showSidebarRail && <AiHubTasksSidebarRail onMouseEnter={() => setTasksSidebarPeeking(true)} />}

        <ResizablePanelGroup className="min-w-0 flex-1" orientation="horizontal">
            <ResizablePanel elementRef={chatPaneElementRef} minSize="25%">
                {hasActiveTask ? <AiHubPanel /> : <AiHubHomePanel />}
            </ResizablePanel>

            {/* …ResizableHandle and collapsible resource ResizablePanel exactly as today… */}
        </ResizablePanelGroup>
    </div>
);
```

Keep all existing comments about the flex animation; they still apply.

- [ ] **Step 4: Run the test file again — both tests PASS.** Then run the sibling suites that assert home/task view behavior: `npx vitest run src/ee/pages/automation/ai-hub`.

- [ ] **Step 5: Manual smoke.** With dev server running: on `/automation/ai-hub` home, pick a file / data table / workflow from the composer "+" — the right panel must slide open immediately with the tab; send a message and confirm the tabs survive into the created task.

- [ ] **Step 6: `npm run check`, then commit** (`git add` only the two files):

```bash
git commit -m "5XXX client - Open the AI Hub resource panel immediately on composer resource selection"
```

---

### Task 2: Server rename Personal Agent → Task Template (Java, GraphQL schema, Liquibase, MCP tool)

**Files:**
- Rename+modify (packages): `server/ee/libs/ai/ai-hub/ai-hub-api/src/{main,test}/java/com/bytechef/ee/ai/hub/personalagent/` → `…/tasktemplate/`; same under `ai-hub-service`, `ai-hub-graphql`, `ai-hub-rest` (wherever the package exists — verify with `fd`), and every `*PersonalAgent*` file under `server/ee/libs/ai/ai-hub/*/src/main/java/com/bytechef/ee/ai/hub/tool/`.
- Rename+modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/ai-hub-personal-agent.graphqls` → `ai-hub-task-template.graphqls`; `ai-hub-personal-agent-schedule.graphqls` → `ai-hub-task-template-schedule.graphqls`.
- Rename+modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_personal_agent_manager.txt` → `prompt_task_template_manager.txt`; `…/config/PersonalAgentManagerConfiguration.java` → `TaskTemplateManagerConfiguration.java` (+ its test).
- Rename+modify Liquibase (dir `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/`): `20260504000001_ai_hub_personal_agent_init.xml` → `20260504000001_ai_hub_task_template_init.xml`; same pattern for `20260504000002_ai_hub_task_add_personal_agent_id.xml` → `…_ai_hub_task_add_task_template_id.xml`, `20260505000002_…tool_init`, `20260506000001_…tool_add_config`, `20260513000004_…add_llm_model`, `20260516000001_…schedule_init`, `20260521000001_…resource_init`.
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerMcpContributorConfiguration.java` and `ManagerAgentType.java` (`personal_agent_manager` → `task_template_manager`).
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-api/…/task/AiHubTaskKind.java` (`PERSONAL_AGENT` → `TASK_TEMPLATE`, keep ordinal position 2), `AiHubTask.java` column `ai_hub_personal_agent_id` → `ai_hub_task_template_id`, `EnumOrdinalStabilityTest.java`.

**Interfaces:**
- Produces (Task 3 depends on these exact names): GraphQL type `AiHubTaskTemplate`, queries `aiHubTaskTemplates` / `aiHubTaskTemplate`, mutations `createAiHubTaskTemplateTask`, `addAiHubTaskTemplateTool`, `addAiHubTaskTemplateResource`, etc. (mechanical `PersonalAgent`→`TaskTemplate` on every existing schema name); GraphQL enum value `TASK_TEMPLATE` on `AiHubTaskKind`.
- Produces: MCP tool name `task_template_manager`.

- [ ] **Step 1: Baseline.** `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:test :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/pre-rename.log 2>&1; grep -c "FAILED" /tmp/pre-rename.log` — record green baseline.

- [ ] **Step 2: `git mv` the package dirs, files, changelogs, prompt, graphqls files.** For each module that has the package: `git mv server/ee/libs/ai/ai-hub/<mod>/src/main/java/com/bytechef/ee/ai/hub/personalagent server/ee/libs/ai/ai-hub/<mod>/src/main/java/com/bytechef/ee/ai/hub/tasktemplate` (and `src/test`). Then rename each remaining `*PersonalAgent*`/`*personal_agent*`/`*personal-agent*` file found by `git ls-files | grep -iE 'personal.?agent' | grep ^server` with the corresponding TaskTemplate name.

- [ ] **Step 3: Ordered content sweep over `server/`** (specific → general; `rg -l` to scope, `perl -pi -e` to edit):

```bash
FILES=$(rg -l -iE 'personal.?agent' server --glob '!build' --glob '!*.class')
for f in $FILES; do perl -pi -e '
    s/AiHubPersonalAgent/AiHubTaskTemplate/g;
    s/aiHubPersonalAgent/aiHubTaskTemplate/g;
    s/AI_HUB_PERSONAL_AGENT/AI_HUB_TASK_TEMPLATE/g;
    s/PERSONAL_AGENT/TASK_TEMPLATE/g;
    s/PersonalAgent/TaskTemplate/g;
    s/personalAgent/taskTemplate/g;
    s/personalagent/tasktemplate/g;
    s/personal_agent/task_template/g;
    s/personal-agent/task-template/g;
    s/Personal Agent/Task Template/g;
    s/Personal agent/Task template/g;
    s/personal agent/task template/g;
' "$f"; done
```

- [ ] **Step 4: Fix what the sweep can't know.**
  - `AiHubTaskKind`: keep enum order `STANDARD, WORKFLOW_CHAT, TASK_TEMPLATE`; update the javadoc; update `EnumOrdinalStabilityTest` expected names.
  - Grep the Liquibase master/include wiring: `rg -l "includeAll|include " server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase` — if includes are explicit filenames, update them; if `includeAll`, nothing to do.
  - Constraint/index names inside the edited changelogs (`fk_…personal_agent…`, `idx_…`, `ck_…`) must all read `task_template` — verify with `rg -i 'personal' …/liquibase/`.
  - `rg -i 'personal' server --glob '!build'` must return ZERO code hits (allow only unrelated words — check each).
  - `find server/ee/libs/ai/ai-hub/ai-hub-service/build/resources -name '*personal_agent*' -delete` (stale changelog copies).

- [ ] **Step 5: Compile + module tests.** `./gradlew spotlessApply` then `./gradlew compileJava :server:ee:libs:ai:ai-hub:ai-hub-api:test :server:ee:libs:ai:ai-hub:ai-hub-service:test :server:libs:automation:automation-ai:automation-ai-tool:test > /tmp/post-rename.log 2>&1; grep "^> Task .* FAILED" /tmp/post-rename.log` — expect none.

- [ ] **Step 6: Commit** (server-side only; do NOT touch client yet — the client is temporarily broken against the new schema, which is fine because the client rename lands in the next commit):

```bash
git commit -m "5XXX Rename Personal Agents to Task Templates on the server incl. ai_hub_task_template tables"
```

Include this dev-DB data-carry-over snippet in the commit body (verbatim):

```sql
INSERT INTO ai_hub_task_template SELECT * FROM ai_hub_personal_agent;
INSERT INTO ai_hub_task_template_tool SELECT * FROM ai_hub_personal_agent_tool;
INSERT INTO ai_hub_task_template_resource SELECT * FROM ai_hub_personal_agent_resource;
INSERT INTO ai_hub_task_template_schedule SELECT * FROM ai_hub_personal_agent_schedule;
UPDATE ai_hub_task SET ai_hub_task_template_id = ai_hub_personal_agent_id WHERE ai_hub_personal_agent_id IS NOT NULL;
-- then optionally: DROP TABLE ai_hub_personal_agent_schedule, ai_hub_personal_agent_resource,
-- ai_hub_personal_agent_tool, ai_hub_personal_agent; ALTER TABLE ai_hub_task DROP COLUMN ai_hub_personal_agent_id;
```

(Works because column order is identical — the init changelogs were renamed, not reshaped. The old `ai_hub_personal_agent_id` column survives on `ai_hub_task` because that table's own init wasn't re-run; the renamed add-column changelog adds the new column alongside it.)

---

### Task 3: Client rename Personal Agents → Task Templates + ClipboardListIcon

**Files:**
- Rename dir: `client/src/ee/pages/automation/ai-hub/personal-agents/` → `client/src/ee/pages/automation/ai-hub/task-templates/` (+ every `AiHubPersonalAgent*` file inside → `AiHubTaskTemplate*`, `useAiHubPersonalAgents.ts` → `useAiHubTaskTemplates.ts`).
- Rename dir: `client/src/graphql/ai/aihub/personal-agent/` → `client/src/graphql/ai/aihub/task-template/` (+ each `.graphql` file to the new operation name).
- Modify: `client/src/routes.tsx` (~lines 83–85, 1118+): route path `ai-hub/personal-agents` → `ai-hub/task-templates`, lazy imports.
- Modify: every other client file matched by `rg -l -iE 'personal.?agent' client/src` (AiHubTasksSidebar.tsx, AiHubHomePanel.tsx, AiHubPanel.tsx, ModelPicker.tsx + test, composer store/tests, useSwitchTask, etc.).
- Regenerate: `client/src/shared/middleware/graphql.ts` via `npx graphql-codegen` (server from Task 2 must be schema-source; codegen reads `.graphqls` paths in `client/codegen.ts` — verify the two renamed schema files are covered by its glob/list and update `codegen.ts` if listed explicitly).

**Interfaces:**
- Consumes: Task 2's GraphQL names (`aiHubTaskTemplates`, `createAiHubTaskTemplateTask`, kind `TASK_TEMPLATE`, …).
- Produces: route `/automation/ai-hub/task-templates`; components `AiHubTaskTemplates`, `AiHubTaskTemplatesList`, `AiHubTaskTemplateForm`; hook `useAiHubTaskTemplatesQuery`.

- [ ] **Step 1: `git mv`** the two dirs and each file inside to TaskTemplate names.

- [ ] **Step 2: Content sweep over `client/src`** — same ordered perl script as Task 2 Step 3 (run against `rg -l -iE 'personal.?agent' client/src`).

- [ ] **Step 3: Icon swap.** In the renamed `task-templates/` components, `AiHubTasksSidebar.tsx` (nav item ~line 728 and the kind badge ~line 267), and any Task-Template-specific `BotIcon` usage: replace `BotIcon` with `ClipboardListIcon` (update lucide imports, keep alphabetical order). Do NOT touch `BotIcon` where it means AI Agents (`ResourcePickerMenu.tsx` Agents branch, `AiHubAiAgentViewer`, composer agent handling). Decide per usage: `rg -n "BotIcon" client/src/ee/pages/automation/ai-hub client/src/shared/components/ai`.

- [ ] **Step 4: UI strings.** Verify the sweep produced: sidebar "Task Templates", page title "Task Templates", button "New Task Template" (the old label is "New Agent" — the sweep does NOT catch it; fix by hand: `rg -n '"New Agent"|>New Agent<' client/src`). Tooltip prose reads "Task template — your custom instructions…".

- [ ] **Step 5: Codegen + fixups.** `cd client && npx graphql-codegen` (requires Task 2 schema on disk — codegen reads local `.graphqls` files, not a running server; confirm in `codegen.ts`). Then `npm run typecheck` and chase residual identifiers.

- [ ] **Step 6: Verify zero leftovers:** `rg -iE 'personal.?agent' client/src` → no hits.

- [ ] **Step 7: `npm run check`** (lint + typecheck + tests — the renamed test files must pass).

- [ ] **Step 8: Manual smoke:** sidebar shows "Task Templates" with clipboard icon; create → edit → chat-with → delete a task template end-to-end; task rows of kind TASK_TEMPLATE show clipboard badge.

- [ ] **Step 9: Two commits** — (a) operations + code, (b) regenerated `graphql.ts`:

```bash
git commit -m "5XXX client - Rename Personal Agents to Task Templates with ClipboardListIcon"
git commit -m "5XXX client - Regenerate GraphQL client for the Task Template schema"
```

---

### Task 4: Sidebar Context → More popup

**Files:**
- Modify: `client/src/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx` (Collapsible block lines ~739–789, imports, `contextExpanded` state ~line 678)
- Test: `client/src/ee/pages/automation/ai-hub/tasks/tests/AiHubTasksSidebar.test.tsx` (extend)

**Interfaces:**
- Consumes: existing `DropdownMenu*` primitives already imported in the file; routes `/automation/ai/memories`, `/automation/settings/ai-hub/connectors`, `/automation/ai/skills`.
- Produces: none.

- [ ] **Step 1: Extend the sidebar test** with a failing case (mirror existing render helpers in that test file):

```tsx
it('opens Memories, Connectors and Skills from the More popup', async () => {
    renderSidebar(); // existing helper in this test file

    expect(screen.queryByText('Context')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', {name: /more/i}));

    expect(await screen.findByRole('menuitem', {name: /memories/i})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: /connectors/i})).toBeInTheDocument();
    expect(screen.getByRole('menuitem', {name: /skills/i})).toBeInTheDocument();
});
```

- [ ] **Step 2: Run it — FAILS** (`Context` still rendered).

- [ ] **Step 3: Implement.** Delete the `Collapsible` import + `contextExpanded` state + whole Collapsible block; replace with:

```tsx
{/* "More" — AI Hub-wide resources that aren't scoped to a single task. A right-side popup
  * (instead of the old inline "Context" collapsible) keeps the task list from being pushed
  * down when the group is open. */}

<DropdownMenu>
    <DropdownMenuTrigger
        className={twMerge(menuItemClass(isOnMemories || isOnConnectors || isOnSkills), 'justify-between')}
    >
        <span className="flex items-center gap-2">
            <BlocksIcon className="size-4 shrink-0 text-muted-foreground" />

            <span>More</span>
        </span>

        <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
    </DropdownMenuTrigger>

    <DropdownMenuContent align="start" side="right">
        <DropdownMenuItem asChild>
            <Link to="/automation/ai/memories">
                <BrainIcon />
                Memories
            </Link>
        </DropdownMenuItem>

        <DropdownMenuItem asChild>
            <Link to="/automation/settings/ai-hub/connectors">
                <LinkIcon />
                Connectors
            </Link>
        </DropdownMenuItem>

        <DropdownMenuItem asChild>
            <Link to="/automation/ai/skills">
                <HexagonIcon />
                Skills
            </Link>
        </DropdownMenuItem>
    </DropdownMenuContent>
</DropdownMenu>
```

Remove now-unused imports (`Collapsible*`, `ChevronDownIcon` if unused elsewhere).

- [ ] **Step 4: Test passes; run the sidebar suite.** `npx vitest run src/ee/pages/automation/ai-hub/tasks`.

- [ ] **Step 5: `npm run check`; commit:**

```bash
git commit -m "5XXX client - Replace the AI Hub sidebar Context section with a More popup menu"
```

---

### Task 5: `workspaceChatAgents` GraphQL query (server)

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-api/src/main/java/com/bytechef/automation/ai/agent/facade/AiAgentFacade.java`
- Create: `server/libs/automation/automation-ai/automation-ai-agent/automation-ai-agent-api/src/main/java/com/bytechef/automation/ai/agent/dto/ChatAgentDTO.java`
- Modify: `…/automation-ai-agent-service/…/facade/AiAgentFacadeImpl.java`
- Modify: `…/automation-ai-agent-graphql/…/web/graphql/AiAgentGraphQlController.java`
- Modify: `…/automation-ai-agent-graphql/src/main/resources/graphql/ai-agent.graphqls`
- Test: `…/automation-ai-agent-graphql/src/test/java/com/bytechef/automation/ai/agent/web/graphql/AiAgentGraphQlControllerTest.java` (extend)

**Interfaces:**
- Consumes: `AiAgentService.getAgents(workspaceId)`, `ProjectDeploymentService.fetchProjectDeployment(projectId, environment)`, `ProjectDeploymentWorkflowService`, `WorkflowService`, `TriggerDefinitionService` — all already injected in `AiAgentFacadeImpl`.
- Produces (Tasks 6–7 depend on):

```graphql
extend type Query {
    workspaceChatAgents(workspaceId: ID!, environmentId: ID!): [ChatAgent!]!
}

type ChatAgent {
    aiAgentId: ID!
    agentName: String!
    agentTitle: String!
    projectDeploymentId: ID!
    workflowExecutionId: String!
    workflowLabel: String!
}
```

- [ ] **Step 1: DTO + facade signature.**

```java
@SuppressFBWarnings("EI_EXPOSE_REP")
public record ChatAgentDTO(
    long aiAgentId, String agentName, String agentTitle, long projectDeploymentId, String workflowExecutionId,
    String workflowLabel) {
}
```

`AiAgentFacade`: `List<ChatAgentDTO> getWorkspaceChatAgents(Long workspaceId, long environmentId);`

- [ ] **Step 2: Failing controller test.** Extend `AiAgentGraphQlControllerTest` following its existing `@QueryMapping` test style (mock facade, assert mapping):

```java
@Test
void testWorkspaceChatAgents() {
    List<ChatAgentDTO> chatAgents = List.of(
        new ChatAgentDTO(7L, "support-agent", "Support Agent", 11L, "automation:11:uuid:trigger_1", "Chat"));

    when(aiAgentFacade.getWorkspaceChatAgents(1L, 2L)).thenReturn(chatAgents);

    List<ChatAgentDTO> result = aiAgentGraphQlController.workspaceChatAgents(1L, 2L);

    assertEquals(chatAgents, result);
}
```

- [ ] **Step 3: Run — FAILS** (method missing). `./gradlew :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-graphql:test`

- [ ] **Step 4: Implement.** Facade impl (reusing the module's replicated trigger-resolution approach — see the ~line 845 comment; mirror `hasHostedChatTrigger` + static-webhook resolution from `ProjectDeploymentWorkflowGraphQlController` lines 286–343, adapted):

```java
@Override
@PreAuthorize("isAuthenticated()")
public List<ChatAgentDTO> getWorkspaceChatAgents(Long workspaceId, long environmentId) {
    Environment environment = environmentService.getEnvironment(environmentId);

    List<ChatAgentDTO> chatAgents = new ArrayList<>();

    for (AiAgent agent : agentService.getAgents(workspaceId)) {
        projectDeploymentService.fetchProjectDeployment(agent.getProjectId(), environment)
            .filter(ProjectDeployment::isEnabled)
            .ifPresent(projectDeployment -> collectAgentChatWorkflows(agent, projectDeployment, chatAgents));
    }

    return chatAgents;
}
```

`collectAgentChatWorkflows` loads the deployment's enabled `ProjectDeploymentWorkflow`s, their `Workflow`s, filters on the chat-trigger predicate (type starts with `chat/`, first trigger `mode` absent or 1), resolves the `WorkflowExecutionId.of(PlatformType.AUTOMATION, deploymentId, projectWorkflowUuid, triggerName)` for the first STATIC_WEBHOOK non-manual trigger, and adds `new ChatAgentDTO(agent.getId(), agent.getName(), agent.getTitle(), deploymentId, workflowExecutionId.toString(), workflowLabel)`. If `environmentService` isn't yet injected in the facade, add it (constructor injection, matching existing fields). Controller:

```java
@QueryMapping
public List<ChatAgentDTO> workspaceChatAgents(@Argument long workspaceId, @Argument long environmentId) {
    return agentFacade.getWorkspaceChatAgents(workspaceId, environmentId);
}
```

Schema: add the `ChatAgent` type + query to `ai-agent.graphqls` (SCREAMING_SNAKE_CASE only applies to enums; field names camelCase as above).

- [ ] **Step 5: Tests pass; spotless; commit.**

```bash
./gradlew spotlessApply :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-graphql:test :server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service:test
git commit -m "5XXX Add workspaceChatAgents query surfacing agent chat workflows"
```

---

### Task 6: AI Hub Agents page + sidebar entry

**Files:**
- Create: `client/src/graphql/automation/agent/workspaceChatAgents.graphql`
- Create: `client/src/ee/pages/automation/ai-hub/tasks/AiHubAgents.tsx`
- Create: `client/src/ee/pages/automation/ai-hub/tasks/AiHubAgentsList.tsx`
- Test: `client/src/ee/pages/automation/ai-hub/tasks/tests/AiHubAgentsList.test.tsx`
- Modify: `client/src/routes.tsx` (lazy import + route `ai-hub/agents` next to the `ai-hub/workflow-chats` route), `client/src/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx` (nav item above Workflow Chats)
- Regenerate: `client/src/shared/middleware/graphql.ts`

**Interfaces:**
- Consumes: `workspaceChatAgents` (Task 5), `useCreateWorkflowChatAiHubTaskMutation`, `AiHubTasksKeys`, `aiHubStore`/`aiHubTasksStore` (same trio `WorkflowChatsList.handleSelect` uses).
- Produces: route `/automation/ai-hub/agents`; `useWorkspaceChatAgentsQuery` (codegen).

- [ ] **Step 1: GraphQL operation + codegen.**

```graphql
query workspaceChatAgents($workspaceId: ID!, $environmentId: ID!) {
    workspaceChatAgents(workspaceId: $workspaceId, environmentId: $environmentId) {
        agentName
        agentTitle
        aiAgentId
        projectDeploymentId
        workflowExecutionId
        workflowLabel
    }
}
```

`cd client && npx graphql-codegen` → `useWorkspaceChatAgentsQuery` exists.

- [ ] **Step 2: Failing list test** (mirror `AiHubTasksSidebar.test.tsx` provider setup; mock `useWorkspaceChatAgentsQuery` and the create mutation):

```tsx
it('lists agents and starts a chat task on click', async () => {
    mockWorkspaceChatAgents([
        {
            agentName: 'support-agent',
            agentTitle: 'Support Agent',
            aiAgentId: '7',
            projectDeploymentId: '11',
            workflowExecutionId: 'exec-1',
            workflowLabel: 'Chat',
        },
    ]);

    renderAgentsList();

    const row = await screen.findByRole('button', {name: /support agent/i});

    await userEvent.click(row);

    expect(createWorkflowChatTaskMock).toHaveBeenCalledWith(
        expect.objectContaining({projectDeploymentId: '11', title: 'Support Agent', workflowExecutionId: 'exec-1'})
    );
});
```

- [ ] **Step 3: Run — FAILS (module not found).**

- [ ] **Step 4: Implement `AiHubAgentsList.tsx`** — copy `WorkflowChatsList.tsx` and reduce: flat list (no per-project grouping), `BotIcon` rows, `handleSelect` identical to `WorkflowChatsList.handleSelect` but with `title: agent.agentTitle`; keep the archived-badge logic keyed on `workflowExecutionId`; empty state:

```tsx
<p className="text-base font-semibold">No agents yet</p>
<p className="mt-1 max-w-md text-sm text-muted-foreground">
    Create an agent with a chat channel to talk to it here.
</p>
```

**Implement `AiHubAgents.tsx`** — copy `WorkflowChats.tsx`, title "Agents", tooltip: "Each row is an agent with a chat channel. Clicking one starts a task that chats with the agent's deployed workflow."

**Route** (in `routes.tsx`, adjacent to the workflow-chats entries):

```tsx
const AiHubAgentsPage = lazy(() => import('@/ee/pages/automation/ai-hub/tasks/AiHubAgents'));
// …
{element: <LazyLoadWrapper><AiHubAgentsPage /></LazyLoadWrapper>, path: 'ai-hub/agents'},
```

(match the exact wrapper the sibling `ai-hub/workflow-chats` route uses).

**Sidebar nav item** in `AiHubTasksSidebar.tsx`, ABOVE Workflow Chats (Task Templates link stays above it per current order):

```tsx
const isOnAgents = pathname.startsWith('/automation/ai-hub/agents');
// …
<Link className={menuItemClass(isOnAgents)} to="/automation/ai-hub/agents">
    <BotIcon className="size-4 shrink-0 text-muted-foreground" />

    <span>Agents</span>
</Link>
```

- [ ] **Step 5: Tests pass; `npm run check`; manual smoke** (agent with chat channel deployed → appears on page → click → chat task opens; the same workflow does NOT appear under Workflow Chats).

- [ ] **Step 6: Two commits** (code, then regenerated `graphql.ts`):

```bash
git commit -m "5XXX client - Add Agents page and sidebar entry to the AI Hub"
git commit -m "5XXX client - Regenerate GraphQL client for workspaceChatAgents"
```

---

### Task 7: `/automation/chats` sidebar lists Agents

**Files:**
- Modify: `client/src/pages/automation/chats/components/ChatsSidebar.tsx`
- Test: `client/src/pages/automation/chats/components/ChatsSidebar.test.tsx` (create if absent; check `client/src/pages/automation/chats` for an existing test dir first)

**Interfaces:**
- Consumes: `useWorkspaceChatAgentsQuery` (Task 6 codegen), existing `LeftSidebarNav`/`LeftSidebarNavItem`.

- [ ] **Step 1: Failing test** — mock both queries; assert an "Agents" group renders above project groups with a row per agent linking to `/automation/chats/{workflowExecutionId}`:

```tsx
it('renders an Agents group above project chat groups', async () => {
    mockWorkspaceChatWorkflows([
        {
            projectDeploymentId: '21',
            projectId: '2',
            projectName: 'CRM',
            projectWorkflowId: '31',
            workflowExecutionId: 'exec-9',
            workflowId: 'wf-9',
            workflowLabel: 'Support intake',
        },
    ]);
    mockWorkspaceChatAgents([
        {
            agentName: 'support-agent',
            agentTitle: 'Support Agent',
            aiAgentId: '7',
            projectDeploymentId: '11',
            workflowExecutionId: 'exec-1',
            workflowLabel: 'Chat',
        },
    ]);

    renderChatsSidebar();

    const groups = await screen.findAllByRole('heading');

    expect(groups[0]).toHaveTextContent('Agents');
    expect(screen.getByText('Support Agent')).toHaveAttribute('href', '/automation/chats/exec-1');
});
```

(Adapt the `heading`/link-role assertions to what `LeftSidebarNav` actually renders — check its DOM in the component source before finalizing.)

- [ ] **Step 2: Run — FAILS.**

- [ ] **Step 3: Implement.** In `ChatsSidebar`, add the query and render before the project groups:

```tsx
const {data: chatAgentsData} = useWorkspaceChatAgentsQuery({
    environmentId: String(currentEnvironmentId),
    workspaceId: String(currentWorkspaceId),
});

const chatAgents = chatAgentsData?.workspaceChatAgents ?? [];
// …above the workflowsByProject map():
{chatAgents.length > 0 && (
    <LeftSidebarNav
        body={
            <>
                {chatAgents.map((chatAgent) => (
                    <LeftSidebarNavItem
                        disabled={isRunning && workflowExecutionId !== chatAgent.workflowExecutionId}
                        item={{
                            current: workflowExecutionId === chatAgent.workflowExecutionId,
                            id: chatAgent.workflowExecutionId,
                            name: chatAgent.agentTitle,
                        }}
                        key={chatAgent.workflowExecutionId}
                        toLink={`/automation/chats/${chatAgent.workflowExecutionId}`}
                    />
                ))}
            </>
        }
        title="Agents"
    />
)}
```

Also update the empty-state condition (`workflowsByProject.size === 0 && chatAgents.length === 0`).

- [ ] **Step 4: Test passes; `npm run check`; manual smoke on `http://localhost:5173/automation/chats`.**

- [ ] **Step 5: Commit:**

```bash
git commit -m "5XXX client - List agents in the automation chats sidebar"
```

---

### Task 8: Full verification sweep

- [ ] **Step 1:** `./gradlew spotlessApply` → `git status` clean of formatting diffs (commit any as `5XXX Apply spotless formatting` if produced).
- [ ] **Step 2:** `./gradlew check > /tmp/gradle-check.log 2>&1; grep "^> Task .* FAILED" /tmp/gradle-check.log` — expect empty.
- [ ] **Step 3:** `cd client && npm run check` — green.
- [ ] **Step 4:** Restart the dev server against a reset DB (`docker compose -f server/docker-compose.dev.infra.yml down -v` optional) OR run the data-carry-over SQL from Task 2; walk the manual checklist from the spec's Testing section (six items).
- [ ] **Step 5:** Report results honestly per item; no completion claims without command output.

---

## ADDENDUM — 2026-08-12 evening pivot (user-directed, supersedes conflicting text above)

Naming pivot and page restructure, decided after Tasks 1–3 landed:

1. **Chat Templates, not Task Templates**: `TaskTemplate`→`ChatTemplate` in every form (Java, GraphQL, client, MCP tool `chat_template_manager`, route `/automation/ai-hub/chat-templates`, DB `ai_hub_chat_template*`, kind `CHAT_TEMPLATE`).
2. **Full Task→Chat domain rename** (user chose full depth incl. code + DB): `AiHubTask*`→`AiHubChat*` Java/GraphQL/client, package `ee.ai.hub.task`→`ee.ai.hub.chat`, DB `ai_hub_task`, `ai_hub_task_artifact`, `ai_hub_task_tool`, `ai_hub_task_asset_file`→`ai_hub_chat*` (edit init changelogs in place, same approach as before), routes `/ai-hub/tasks/:taskId`→`/ai-hub/chats/:chatId`, UI strings "New Chat", "Chats", "Search chats...". Platform/Atlas "task" concepts (TaskDispatcher, listTasks MCP tools, workflow tasks) are OUT of scope — only AiHub-owned identifiers.
3. **Pages removed**: the Workflow Chats page + its sidebar nav item are deleted; the planned Agents page (Task 6) is CANCELLED. The sidebar keeps: New Chat, Chat Templates, More (popup), Chats list.
4. **Provider popup (ModelPicker) becomes the chat launcher**: cascades "Chat Templates" (renamed from "Personal agents"), "Workflow Chats" (capitalization fix), and NEW "Agent Chats" (lists deployed AI agents with chat channels via `workspaceChatAgents`; picking one runs the same createWorkflowChat flow with the agent's chat workflow, title = agent title). Workflow Chats cascade must EXCLUDE agent-backed workflows (they currently leak in — see user screenshot showing AIAGENT project groups).
5. Task 5 (`workspaceChatAgents` server query) unchanged. Task 7 (/automation/chats Agents group) unchanged. Task 4 (More popup) unchanged.
