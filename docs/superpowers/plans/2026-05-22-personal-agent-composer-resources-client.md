# Personal Agent Composer Resources — §6 + Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface a personal agent's resources in its conversations' LLM context (§6), and give the agent-creation form a unified composer-style picker so users can attach all 8 resource kinds.

**Architecture:** Phase 1 is server-only — extend the existing per-turn `AiHubRoutingAgent` personal-agent overlay to merge the agent's resources into the AG-UI `state`'s `referencedResources` list, which the existing prompt renderer already turns into a Context block. Phase 2 is client — GraphQL operations + codegen, extract the composer's nested resource-picker into a shared `ResourcePickerMenu`, and replace the agent form's tools-only card with a unified resources card.

**Tech Stack:** Java 25 / Spring (EE — Enterprise license header, `@version ee` tag); React 19 + TypeScript, Vitest, GraphQL Code Generator.

**Prerequisite:** The server foundation (persistence, service, GraphQL schema/controller, task-spawn copy) is already merged on branch `0_732`. Spec: `docs/superpowers/specs/2026-05-21-personal-agent-composer-resources-design.md`.

**Module / Gradle paths:**
- `automation-ai-hub-service` → `:server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service`
- Client commands run from `client/`.

---

# Phase 1 — §6 server overlay

## Task 1: `mergeReferencedResources` static helper on `AiHubRoutingAgent`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgentTest.java`

The helper is a pure function: given the agent's resources and whatever `referencedResources` the client already put in `state`, produce the merged list. `AiHubSpringAIAgent.createSystemMessage` reads `state.get("referencedResources")` and `formatReferencedResources` (`AiHubSpringAIAgent.java:435`) reads each element as a `Map` with keys `kind`, `id`, `name` — so the agent entries must be `Map`s with exactly those keys.

- [ ] **Step 1: Write the failing test**

In `AiHubRoutingAgentTest.java`, add these imports if absent: `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource`, `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind`, `java.util.Map`, `static org.assertj.core.api.Assertions.assertThat`. Add:

```java
    @Test
    void testMergeReferencedResourcesAppendsAgentResourcesWithComposerKinds() {
        List<AiHubPersonalAgentResource> agentResources = List.of(
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup"),
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.DATA_TABLE, "dt-9", "Customers"));

        List<?> merged = AiHubRoutingAgent.mergeReferencedResources(agentResources, null);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0)).isEqualTo(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
        assertThat(merged.get(1)).isEqualTo(Map.of("kind", "dataTable", "id", "dt-9", "name", "Customers"));
    }

    @Test
    void testMergeReferencedResourcesKeepsClientEntriesAndDedupsByKindAndId() {
        // The client already @-mentioned wf-1 this turn; the agent also pins wf-1. The merged list keeps the
        // client entry and does NOT add a duplicate, but still appends the agent's other resource.
        List<Map<String, Object>> clientSent = List.of(
            Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));

        List<AiHubPersonalAgentResource> agentResources = List.of(
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup"),
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.FILE, "file-2", "notes.md"));

        List<?> merged = AiHubRoutingAgent.mergeReferencedResources(agentResources, clientSent);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0)).isEqualTo(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
        assertThat(merged.get(1)).isEqualTo(Map.of("kind", "file", "id", "file-2", "name", "notes.md"));
    }

    @Test
    void testMergeReferencedResourcesWithEmptyAgentResourcesReturnsClientEntriesUnchanged() {
        List<Map<String, Object>> clientSent = List.of(
            Map.of("kind", "file", "id", "file-5", "name", "spec.md"));

        List<?> merged = AiHubRoutingAgent.mergeReferencedResources(List.of(), clientSent);

        assertThat(merged).containsExactlyElementsOf(clientSent);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.agent.AiHubRoutingAgentTest"`
Expected: FAIL — compilation error, `mergeReferencedResources` does not exist.

- [ ] **Step 3: Implement the helper**

In `AiHubRoutingAgent.java`: add imports `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResource`, `com.bytechef.ee.platform.aihub.personalagent.AiHubPersonalAgentResourceKind`, `java.util.ArrayList`, `java.util.HashSet`, `java.util.Set` (keep `java.util.List`, `java.util.Map` — already imported). Add these two static methods (place them just before the closing brace of the class, after `ensureState`):

```java
    /**
     * Merges a personal agent's resource template into whatever {@code referencedResources} the client already put
     * in the AG-UI state for this turn. Client-supplied entries are kept verbatim and first; each agent resource is
     * appended as a {@code {kind, id, name}} map unless an entry with the same {@code (kind, id)} is already present
     * (so an @-mention of an already-pinned resource is not doubled). The result is what
     * {@code AiHubSpringAIAgent.createSystemMessage} renders as the "Referenced Resources" Context block.
     *
     * <p>
     * Package-private + static so the unit test can pin the merge/dedup/kind-mapping behaviour directly, mirroring
     * {@code AiHubSpringAIAgent.appendAiHubPersonalAgentContext}.
     * </p>
     */
    static List<Object> mergeReferencedResources(
        List<AiHubPersonalAgentResource> agentResources, @Nullable Object existingReferencedResources) {

        List<Object> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Keep every client-supplied entry verbatim. Record the (kind, id) of the map-shaped ones so the agent's
        // resources dedup against them; non-map entries (defensive) are kept but don't participate in dedup.
        if (existingReferencedResources instanceof List<?> existing) {
            for (Object entry : existing) {
                merged.add(entry);

                if (entry instanceof Map<?, ?> entryMap) {
                    seen.add(entryMap.get("kind") + "|" + entryMap.get("id"));
                }
            }
        }

        for (AiHubPersonalAgentResource resource : agentResources) {
            String kind = composerKind(resource.getKind());
            String dedupeKey = kind + "|" + resource.getResourceId();

            if (seen.add(dedupeKey)) {
                merged.add(Map.of(
                    "kind", kind, "id", resource.getResourceId(), "name", resource.getResourceName()));
            }
        }

        return merged;
    }

    /**
     * Maps an {@link AiHubPersonalAgentResourceKind} to the lowercase wire string the AI Hub composer uses for the
     * same kind ({@code ReferencedResourceKindType} in {@code useAiHubComposerStore.ts}), so agent-pinned resources
     * and per-message @-mentions render homogeneously in the prompt.
     */
    private static String composerKind(AiHubPersonalAgentResourceKind kind) {
        return switch (kind) {
            case WORKFLOW -> "workflow";
            case FILE -> "file";
            case DATA_TABLE -> "dataTable";
            case KNOWLEDGE_BASE -> "knowledgeBase";
            case MCP_SERVER -> "mcpServer";
            case API_COLLECTION -> "apiCollection";
            case WORKFLOW_EXECUTION -> "workflowExecution";
            case TASK -> "task";
        };
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.agent.AiHubRoutingAgentTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgentTest.java
git commit -m "732 Add mergeReferencedResources helper to AiHubRoutingAgent"
```

---

## Task 2: Wire the resource overlay into `applyAiHubPersonalAgentOverlay`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgentTest.java`

`applyAiHubPersonalAgentOverlay` resolves the agent then sets instructions/title state keys, with an early `return` in the blank-instructions branch. The resource merge must run **before** that early return so it always happens. The agent's resources are read **live** every turn via `aiHubPersonalAgentService.listResources(...)`.

- [ ] **Step 1: Write the failing test**

In `AiHubRoutingAgentTest.java`, add a test mirroring the existing `testAiHubPersonalAgentTaskDispatchesToLlmAgentWithInstructionsOverlay` (read that test for the exact `RunAgentParameters.builder()` + `State` + task-mock setup pattern, including the `taskService.getWorkspaceId(...)` / `task.getUserId()` / `task.getAiHubPersonalAgentId()` stubs). Add:

```java
    @Test
    void testAiHubPersonalAgentTaskMergesAgentResourcesIntoState() throws AGUIException {
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(100L);
        when(task.getUserId()).thenReturn(3L);
        when(task.getAiHubPersonalAgentId()).thenReturn(42L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));
        when(taskService.getWorkspaceId(100L)).thenReturn(7L);

        AiHubPersonalAgent agent = mock(AiHubPersonalAgent.class);

        when(agent.getInstructions()).thenReturn("Be concise.");
        when(agent.getTitle()).thenReturn("Researcher");
        when(agent.hasLlmOverride()).thenReturn(false);

        AiHubPersonalAgentService aiHubPersonalAgentService = mock(AiHubPersonalAgentService.class);

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 3L)).thenReturn(Optional.of(agent));
        when(aiHubPersonalAgentService.listResources(42L)).thenReturn(List.of(
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup")));

        AiHubRoutingAgent routingAgent = new AiHubRoutingAgent(
            AGENT_ID, llmAgent, webhookBridgeAgent, taskService, assetFileFacade, aiHubPersonalAgentService);

        State state = new State();

        routingAgent.runAgent(
            RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .state(state)
                .build(),
            subscriber);

        Object referencedResources = state.get("referencedResources");

        assertThat(referencedResources).isInstanceOf(List.class);
        assertThat((List<?>) referencedResources)
            .containsExactly(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.agent.AiHubRoutingAgentTest.testAiHubPersonalAgentTaskMergesAgentResourcesIntoState"`
Expected: FAIL — `state.get("referencedResources")` is null because the overlay does not yet merge resources.

- [ ] **Step 3: Add the resource merge to `applyAiHubPersonalAgentOverlay`**

In `applyAiHubPersonalAgentOverlay`, the existing code has:

```java
        AiHubPersonalAgent agent = agentOptional.get();
        State state = ensureState(parameters);
```

Immediately after `State state = ensureState(parameters);`, add:

```java
        // §6: merge the agent's resource template (read live each turn) into the referencedResources state list so
        // AiHubSpringAIAgent.createSystemMessage renders them in the "Referenced Resources" Context block alongside
        // any resources the user @-mentioned this turn. Done before the blank-instructions early return below so an
        // agent with resources but no instructions still gets its resources injected.
        List<AiHubPersonalAgentResource> resources = aiHubPersonalAgentService.listResources(aiHubPersonalAgentId);

        if (!resources.isEmpty()) {
            state.set("referencedResources", mergeReferencedResources(resources, state.get("referencedResources")));
        }
```

(The literal key `"referencedResources"` matches what `AiHubSpringAIAgent.createSystemMessage` reads at `AiHubSpringAIAgent.java:280` — deliberately not a new `AiHubStateKeys` constant, to keep this change to one file.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests "com.bytechef.ee.automation.aihub.agent.AiHubRoutingAgentTest"`
Expected: PASS — the new test plus every pre-existing routing test (the existing personal-agent tests don't stub `listResources`; Mockito returns an empty list for unstubbed methods, so the `!resources.isEmpty()` guard makes the merge a no-op for them).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgent.java server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/agent/AiHubRoutingAgentTest.java
git commit -m "732 Merge personal agent resources into LLM context each turn"
```

- [ ] **Step 6: Format + module check**

Run: `./gradlew spotlessApply && ./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check`
Expected: BUILD SUCCESSFUL. If `spotlessApply` modified files outside `AiHubRoutingAgent.java` / `AiHubRoutingAgentTest.java`, revert those (`git checkout -- <file>`) — they are unrelated pre-existing formatting drift. Commit any in-scope formatting change with `git commit -m "732 Apply spotless formatting"`.

---

# Phase 2 — client

## Task 3: Client GraphQL operations + codegen

**Files:**
- Create: `client/src/graphql/ai/aihub/personal-agent/addAiHubPersonalAgentResource.graphql`
- Create: `client/src/graphql/ai/aihub/personal-agent/removeAiHubPersonalAgentResource.graphql`
- Modify: `client/src/graphql/ai/aihub/personal-agent/aiHubPersonalAgent.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Create `addAiHubPersonalAgentResource.graphql`**

```graphql
mutation addAiHubPersonalAgentResource($input: AddAiHubPersonalAgentResourceInput!) {
    addAiHubPersonalAgentResource(input: $input) {
        id
        aiHubPersonalAgentId
        kind
        resourceId
        resourceName
        createdAt
    }
}
```

- [ ] **Step 2: Create `removeAiHubPersonalAgentResource.graphql`**

```graphql
mutation removeAiHubPersonalAgentResource($workspaceId: ID!, $id: ID!) {
    removeAiHubPersonalAgentResource(workspaceId: $workspaceId, id: $id)
}
```

- [ ] **Step 3: Add the `resources` selection to `aiHubPersonalAgent.graphql`**

In `aiHubPersonalAgent.graphql`, inside the `aiHubPersonalAgent(...) { ... }` selection, immediately after the `tools { ... }` block, add:

```graphql
        resources {
            id
            aiHubPersonalAgentId
            kind
            resourceId
            resourceName
            createdAt
        }
```

- [ ] **Step 4: Regenerate the GraphQL client**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` updates with `AddAiHubPersonalAgentResourceInput`, `AiHubPersonalAgentResourceKind`, `useAddAiHubPersonalAgentResourceMutation`, `useRemoveAiHubPersonalAgentResourceMutation`, and the `resources` field on the agent query types.

- [ ] **Step 5: Verify typecheck of generated code**

Run: `cd client && npx tsc --noEmit src/shared/middleware/graphql.ts` — or `npm run typecheck` (note: a pre-existing unrelated failure for `@/ee/shared/middleware/embedded/configuration/public` may appear; confirm no NEW errors mention `AiHubPersonalAgentResource`).

- [ ] **Step 6: Commit**

```bash
git add client/src/graphql/ai/aihub/personal-agent/ client/src/shared/middleware/graphql.ts
git commit -m "732 client - Add personal agent resource GraphQL operations"
```

---

## Task 4: Client hooks + `AiHubPersonalAgentI` interface

**Files:**
- Modify: `client/src/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents.ts`

- [ ] **Step 1: Add the `AiHubPersonalAgentResourceI` interface**

After the existing `AiHubPersonalAgentToolI` interface, add (keys alphabetically ordered per the project's `sort-keys` ESLint rule):

```typescript
// Per-agent resource template row. Each entry is a (kind, resourceId, resourceName) reference the agent has
// pre-declared; the backend copies these onto every task the agent spawns and the routing agent also injects them
// into the LLM context each turn. `kind` is the GraphQL AiHubPersonalAgentResourceKind enum value.
export interface AiHubPersonalAgentResourceI {
    aiHubPersonalAgentId: number;
    createdAt: string;
    id: number;
    kind: string;
    resourceId: string;
    resourceName: string;
}
```

- [ ] **Step 2: Add `resources` to `AiHubPersonalAgentI`**

In the `AiHubPersonalAgentI` interface, add the field (alphabetically — after `name`, before `schedule`):

```typescript
    resources: AiHubPersonalAgentResourceI[];
```

- [ ] **Step 3: Map `resources` in `toAgent`**

In the `toAgent` function, after the `tools: (agent.tools ?? []).map(...)` block, add a `resources` mapping:

```typescript
        resources: (agent.resources ?? []).map((resource) => ({
            aiHubPersonalAgentId: Number(resource.aiHubPersonalAgentId),
            createdAt: resource.createdAt != null ? new Date(Number(resource.createdAt)).toISOString() : '',
            id: Number(resource.id),
            kind: resource.kind,
            resourceId: resource.resourceId,
            resourceName: resource.resourceName,
        })),
```

- [ ] **Step 4: Add the two mutation hooks**

After `useRemoveAiHubPersonalAgentToolMutation`, add (mirroring it — bulk-invalidate list + detail by workspace). Add `AddAiHubPersonalAgentResourceInput`, `useAddAiHubPersonalAgentResourceMutation as useGeneratedAddAiHubPersonalAgentResourceMutation`, `useRemoveAiHubPersonalAgentResourceMutation as useGeneratedRemoveAiHubPersonalAgentResourceMutation` to the `@/shared/middleware/graphql` import block:

```typescript
export function useAddAiHubPersonalAgentResourceMutation() {
    const queryClient = useQueryClient();

    return useGeneratedAddAiHubPersonalAgentResourceMutation({
        onError: (error) => reportMutationError('Add personal agent resource', error as Error),
        onSuccess: (_data, variables) => {
            const addInput = (variables as {input: AddAiHubPersonalAgentResourceInput}).input;
            const workspaceId = Number(addInput.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: AiHubPersonalAgentsKeys.detail(Number(addInput.aiHubPersonalAgentId), workspaceId),
            });
        },
    });
}

export function useRemoveAiHubPersonalAgentResourceMutation() {
    const queryClient = useQueryClient();

    return useGeneratedRemoveAiHubPersonalAgentResourceMutation({
        onError: (error) => reportMutationError('Remove personal agent resource', error as Error),
        onSuccess: (_data, variables) => {
            const removeVars = variables as {id: string; workspaceId: string};
            const workspaceId = Number(removeVars.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'detail'],
            });
        },
    });
}
```

- [ ] **Step 5: Verify**

Run: `cd client && npm run typecheck` — confirm no new errors referencing `useAiHubPersonalAgents`.

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents.ts
git commit -m "732 client - Add personal agent resource hooks"
```

---

## Task 5: Extract `ResourcePickerMenu` from `AiHubComposer`

**Files:**
- Create: `client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx`
- Modify: `client/src/pages/automation/ai-hub/composer/AiHubComposer.tsx`

This task extracts the composer's nested "Search resources…" dropdown into a reusable component. **It must not change composer behaviour** — the composer's existing tests (`client/src/pages/automation/ai-hub/composer/tests/`) are the regression gate.

**`ResourcePickerMenu` component contract:**

```typescript
import {ReferencedResourceKindType} from '@/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {ReactNode} from 'react';

export interface ResourcePickerSelectionI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
    // Workflow-only: present so a caller can open a workflow tab. Undefined for the other 7 kinds.
    projectId?: string;
    projectWorkflowId?: number;
}

// A caller-supplied Tools branch. ResourcePickerMenu covers only the 8 reference kinds; Tools differs per
// context (composer vs. agent form), so each caller plugs its own. When omitted, the menu shows only 8 kinds.
export interface ResourcePickerToolsBranchI {
    // Rendered as the "Tools" CommandItem in the root menu. `onEnter` drills the menu into the tools branch.
    renderRootItem: (onEnter: () => void) => ReactNode;
    // Rendered as the full menu body when the tools branch is active. `onBack` returns to the root menu.
    renderBranch: (onBack: () => void) => ReactNode;
}

export interface ResourcePickerMenuPropsI {
    workspaceId: number;
    environmentId: number;
    // The Popover trigger (the composer's "+" button, the agent form's "Add" button).
    trigger: ReactNode;
    // Fired when the user picks one of the 8 reference-kind resources. The menu closes itself after.
    onSelect: (selection: ResourcePickerSelectionI) => void;
    toolsBranch?: ResourcePickerToolsBranchI;
}
```

- [ ] **Step 1: Create `ResourcePickerMenu.tsx` by moving the picker out of `AiHubComposer.tsx`**

`ResourcePickerMenu` owns: the `Popover`+`Command` shell; the picker state (`open`, `search`,
`debouncedSearch`, `menuPath`, the 8 `*ShowCount` states); the per-kind query hooks and
`useAllWorkspaceWorkflows` helper; all `filtered*` / `visible*` / `filteredWorkflowProjects` /
`selectedWorkflowProject` memos; `handleSearchChange`; `handleOpenChange`; and the JSX for search-mode +
browse-mode for the **8 reference kinds** (workflows, files, dataTables, knowledgeBases, workflowExecutions,
mcpServers, apiCollections, tasks). Move the constants `SECTION_INITIAL_CAP`, `SECTION_EXPAND_INCREMENT`,
`PROJECT_LIMIT`, the `WorkflowItemI` interface, the `MenuPathType` (minus the `'tools'` variants — see
below), and `useAllWorkspaceWorkflows`.

For the **Tools** root entry and branch: `ResourcePickerMenu` keeps a `['tools']` value in its `MenuPathType` but does NOT render tool content itself — when `menuPath[0] === 'tools'` it renders `props.toolsBranch?.renderBranch(() => setMenuPath([]))`; in the root menu it renders `props.toolsBranch?.renderRootItem(() => setMenuPath(['tools']))`. The `['tools', string]` second-level path and the `toolableComponents` query / `dialogTarget` stay OUT of `ResourcePickerMenu` entirely (caller concern).

When a reference-kind item is picked, `ResourcePickerMenu` calls `props.onSelect({id, kind, name, ...})` then closes itself (`setOpen(false)`, clears search) — it does NOT write to any store or open tabs.

- [ ] **Step 2: Refactor `AiHubComposer.tsx` to consume `ResourcePickerMenu`**

`AiHubComposer` keeps: `referencedResources` subscription (badge count), `taskId`, `dialogTarget` state,
`TaskToolDialog`, `useAiHubTaskToolableComponentsQuery`, `toolableComponents`/`selectedToolableComponent`
memos, `handleSelect`, `handleSelectWorkflow`. It renders `<ResourcePickerMenu trigger={<the + button>}
workspaceId={...} environmentId={...} onSelect={...} toolsBranch={...} />`. The `onSelect` callback routes
to the existing `handleSelect` / `handleSelectWorkflow` (workflow when `selection.projectId` is present).
The `toolsBranch.renderBranch` renders the composer's existing two-level Tools component→tool drill-down
that sets `dialogTarget`.

- [ ] **Step 3: Verify composer behaviour is unchanged**

Run: `cd client && npm run test -- src/pages/automation/ai-hub/composer`
Expected: all composer tests PASS unchanged.
Run: `cd client && npm run typecheck` — confirm no new errors.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/resource-picker/ResourcePickerMenu.tsx client/src/pages/automation/ai-hub/composer/AiHubComposer.tsx
git commit -m "732 client - Extract shared ResourcePickerMenu from AiHubComposer"
```

---

## Task 6: `ResourcePickerMenu` test

**Files:**
- Create: `client/src/pages/automation/ai-hub/resource-picker/tests/ResourcePickerMenu.test.tsx`

- [ ] **Step 1: Write the test**

Cover, with the per-kind GraphQL/query hooks mocked (follow the mocking pattern in `client/src/pages/automation/ai-hub/composer/tests/`): (a) opening the picker shows the 8 root kind entries; (b) the Tools root item appears only when `toolsBranch` is supplied; (c) drilling into "Files" lists files and picking one fires `onSelect` with `{kind: 'file', id, name}` and closes the menu; (d) typing in the search box filters across kinds. Keep assertions behavioural (rendered text, `onSelect` mock calls).

- [ ] **Step 2: Run the test**

Run: `cd client && npm run test -- src/pages/automation/ai-hub/resource-picker`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/resource-picker/tests/ResourcePickerMenu.test.tsx
git commit -m "732 client - Add ResourcePickerMenu test"
```

---

## Task 7: `AiHubPersonalAgentResourcesCard`

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentResourcesCard.tsx`
- Delete: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentToolsCard.tsx` (replaced)

This replaces `AiHubPersonalAgentToolsCard` with a card titled "Resources this agent can use:" whose single "Add" button opens `ResourcePickerMenu` (8 reference kinds) with a `toolsBranch` supplying the agent form's existing component→action Tools picker. It keeps the discriminated edit-mode / create-mode props pattern of the old card.

- [ ] **Step 1: Build the component**

Reuse the structure of the existing `AiHubPersonalAgentToolsCard.tsx` (read it first). Keep:
- The discriminated props: edit mode (`aiHubPersonalAgentId`, `tools`, `resources`, `workspaceId`) vs. create mode (`onAddPendingTool`/`onRemovePendingTool`/`pendingTools` plus new `onAddPendingResource`/`onRemovePendingResource`/`pendingResources`).
- The `ToolRow` rendering and `AiHubPersonalAgentToolConfigDialog` wiring (unchanged — Tools keep Configure).
- Add a `ResourceRow` (icon by kind + `resourceName` + remove control; no Configure).
- The "Add" button opens `ResourcePickerMenu`. Its `onSelect` adds a resource (edit mode → `useAddAiHubPersonalAgentResourceMutation`; create mode → `onAddPendingResource`). Its `toolsBranch` renders the existing component→action drill-down; selecting an action adds a tool (edit → `useAddAiHubPersonalAgentToolMutation`; create → `onAddPendingTool`).
- Render attached items as one list grouped by kind (Tools first, then the 8 reference kinds), each row with a remove control.

**Kind normalization (required).** `ResourcePickerMenu.onSelect` emits the composer's lowercase
`ReferencedResourceKindType` (`'workflow'`, `'dataTable'`, …). The GraphQL `addAiHubPersonalAgentResource`
mutation and the persisted `AiHubPersonalAgentResourceI.kind` use the SCREAMING_SNAKE
`AiHubPersonalAgentResourceKind` (`'WORKFLOW'`, `'DATA_TABLE'`, …). Normalize at the `onSelect` boundary —
define a module-level map in this file and convert immediately, so `pendingResources`, the mutation input,
the persisted `resources`, and `ResourceRow` all uniformly use the SCREAMING_SNAKE form:

```typescript
const COMPOSER_KIND_TO_RESOURCE_KIND: Record<ReferencedResourceKindType, string> = {
    apiCollection: 'API_COLLECTION',
    dataTable: 'DATA_TABLE',
    file: 'FILE',
    knowledgeBase: 'KNOWLEDGE_BASE',
    mcpServer: 'MCP_SERVER',
    task: 'TASK',
    workflow: 'WORKFLOW',
    workflowExecution: 'WORKFLOW_EXECUTION',
};
```

`ResourceRow` keys its lucide icon off this SCREAMING_SNAKE kind — map each to an icon matching what `AiHubComposer` uses for that kind (`WORKFLOW`→`WorkflowIcon`, `FILE`→`FileTextIcon`, `DATA_TABLE`→`DatabaseIcon`, `KNOWLEDGE_BASE`→`VectorSquareIcon`, `MCP_SERVER`→`ServerIcon`, `API_COLLECTION`→`LinkIcon`, `WORKFLOW_EXECUTION`→`HistoryIcon`, `TASK`→`ClockIcon`).

`pendingResources` shape (create mode, mirrors `AiHubPersonalAgentPendingToolI`) — `kind` holds the SCREAMING_SNAKE `AiHubPersonalAgentResourceKind` value:

```typescript
export interface AiHubPersonalAgentPendingResourceI {
    kind: string;
    resourceId: string;
    resourceName: string;
}
```

- [ ] **Step 2: Delete the old tools card**

`git rm client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentToolsCard.tsx` — but FIRST confirm its only importer is `AiHubPersonalAgentForm.tsx` (Task 8 updates that import): `grep -rn AiHubPersonalAgentToolsCard client/src`. If anything else imports it, stop and report.

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck` — expect errors only in `AiHubPersonalAgentForm.tsx` (fixed in Task 8).

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentResourcesCard.tsx client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentToolsCard.tsx
git commit -m "732 client - Add AiHubPersonalAgentResourcesCard"
```

---

## Task 8: Wire `AiHubPersonalAgentResourcesCard` into `AiHubPersonalAgentForm`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx`

- [ ] **Step 1: Replace the tools card usage**

In `AiHubPersonalAgentForm.tsx`: change the import from `AiHubPersonalAgentToolsCard` to `AiHubPersonalAgentResourcesCard` (and import `AiHubPersonalAgentPendingResourceI`). Add `pendingResources` state alongside `pendingTools` (`const [pendingResources, setPendingResources] = useState<AiHubPersonalAgentPendingResourceI[]>([])`) with `handleAddPendingResource` / `handleRemovePendingResource` mirroring `handleAddPendingTool` / `handleRemovePendingTool` (dedup by `(kind, resourceId)`).

Replace the two `<AiHubPersonalAgentToolsCard .../>` render branches (edit and create) with `<AiHubPersonalAgentResourcesCard .../>`, passing `resources={agent.resources}` (edit) and the pending-resource handlers (create).

- [ ] **Step 2: Bulk-attach pending resources on create**

In `handleSave`, in the create branch after the `pendingTools` bulk-attach loop, add an analogous loop
over `pendingResources` calling `addResourceMutation.mutateAsync({input: {aiHubPersonalAgentId:
createdAgentId, kind, resourceId, resourceName, workspaceId: String(currentWorkspaceId)}})`, aggregating
failures into the same `failedAttaches`-style partial-failure toast. Add `const addResourceMutation =
useAddAiHubPersonalAgentResourceMutation();` near the other mutation hooks, and include
`addResourceMutation.isPending` in `isSubmitDisabled` / the saving-label condition.

- [ ] **Step 3: Verify**

Run: `cd client && npm run typecheck` — confirm no errors in `AiHubPersonalAgentForm.tsx`.

- [ ] **Step 4: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/AiHubPersonalAgentForm.tsx
git commit -m "732 client - Wire resources card into personal agent form"
```

---

## Task 9: `AiHubPersonalAgentResourcesCard` test

**Files:**
- Create: `client/src/pages/automation/ai-hub/personal-agents/tests/AiHubPersonalAgentResourcesCard.test.tsx`

- [ ] **Step 1: Write the test**

With the resource/tool mutation hooks and `ResourcePickerMenu` mocked, cover: (a) edit mode renders existing `tools` and `resources` rows; (b) picking a reference-kind resource in edit mode fires `useAddAiHubPersonalAgentResourceMutation`; (c) removing a resource fires `useRemoveAiHubPersonalAgentResourceMutation`; (d) create mode routes picks to `onAddPendingResource` and renders `pendingResources` rows. Follow the Zustand/hook-mocking and `vi.hoisted` patterns already used in `client/src/pages/automation/ai-hub/personal-agents/tests/`.

- [ ] **Step 2: Run the test**

Run: `cd client && npm run test -- src/pages/automation/ai-hub/personal-agents`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add client/src/pages/automation/ai-hub/personal-agents/tests/AiHubPersonalAgentResourcesCard.test.tsx
git commit -m "732 client - Add AiHubPersonalAgentResourcesCard test"
```

---

## Final verification

- [ ] **Step 1: Client checks**

Run: `cd client && npm run format && npm run check`
Expected: lint + typecheck + tests pass. **Known pre-existing failure:** `npm run typecheck` reports 4 errors for a missing `@/ee/shared/middleware/embedded/configuration/public` module — this is unrelated in-flight work on branch `0_732`, not caused by this feature. Confirm no NEW failures reference personal-agent / resource-picker files. Stage only files this plan touched (`git add` per the lists above) — do not `git add -A` (the branch carries unrelated untracked files).

- [ ] **Step 2: Server check**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit any formatting changes**

```bash
git add <only the personal-agent / resource-picker / composer files reformatted>
git commit -m "732 client - Apply formatting"
```

(Skip if `npm run format` produced no changes.)

---

## Notes for the implementer

- **Spec §6 is resolved server-side** — there is intentionally NO client-side resource-hydration code. The agent's resources reach the LLM via Task 1–2 (the router overlay). The client only needs the form UI to *manage* the agent's resource list.
- **`ai-hub-personal-agent.graphqls` already has** the `AiHubPersonalAgentResource` type, the `AiHubPersonalAgentResourceKind` enum, the `resources` field, and the two mutations — committed with the server work. Task 3 only adds the *client operation* `.graphql` files and runs codegen.
- The original spec's "Tools stays per-context" decision is why Task 5's `ResourcePickerMenu` covers 8 kinds and takes a `toolsBranch` slot rather than 9.
