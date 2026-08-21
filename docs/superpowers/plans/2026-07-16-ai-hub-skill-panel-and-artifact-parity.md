# AI Hub Skill Panel, Delete Tool, and Artifact Parity — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the AI Hub agent delete a skill and open a skill in the resource panel, and give skills/data-tables/knowledge-bases full workflow-parity artifact recording (client hook + dedup-aware server layer).

**Architecture:** Mirror the existing `openDataTableTab` / `openWorkflowTab` tool + tab-store + client-hook pattern for a new `skill` kind. Add a `SKILL_REFERENCED` artifact kind to the Java and GraphQL enums. Add one dedup-aware server recorder method (the generic `record()` does not dedup and must not, because event-log kinds allow multiple rows) and use it as the server robustness layer for skill/data-table/KB open tools.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI `ToolCallback`, Spring Data JDBC, GraphQL (DGS-style schema + codegen), React 19 / TypeScript / Zustand, Vitest, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- EE files (`server/ee/**`) use the ByteChef Enterprise license header and a `@version ee` Javadoc tag.
- Enum values are JDBC INT ordinals — **append only**, never reorder/insert. Pinned by `EnumOrdinalStabilityTest`.
- Java: blank line before control statements and after a variable modification that a later statement uses; no trailing blank line before a class's closing brace; no `_`-prefixed private methods.
- Client: interface names end in `I`/`Props`; object keys sorted (`sort-keys`); named imports sorted; icons imported with the `Icon` suffix; use `twMerge` not `cn()`.
- Run `./gradlew spotlessApply` before server commits; `cd client && npm run check` before client commits.
- Commit message style: server `<ticket> <desc>`, client `<ticket> client - <desc>`. Use ticket `732`.
- The spec: `docs/superpowers/specs/2026-07-16-ai-hub-skill-panel-and-artifact-parity-design.md`.

---

## Task 1: Add `SKILL_REFERENCED` to both artifact-kind enums

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/task/AiHubTaskArtifactKind.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/ai-hub-artifact.graphqls`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-api/src/test/java/com/bytechef/ee/ai/hub/util/EnumOrdinalStabilityTest.java` (find the actual path with `git grep -l "class EnumOrdinalStabilityTest"`)

**Interfaces:**
- Produces: `AiHubTaskArtifactKind.SKILL_REFERENCED` (Java) and the `SKILL_REFERENCED` GraphQL enum value, appended last.

- [ ] **Step 1: Read the ordinal-stability test to see how kinds are pinned**

Run: `git grep -n "SKILL_REFERENCED\|TASK_REFERENCED\|ordinal" -- '*EnumOrdinalStabilityTest.java'`
Understand which assertion enumerates `AiHubTaskArtifactKind` values and their expected count/order.

- [ ] **Step 2: Update the pinning test to expect `SKILL_REFERENCED` as the new last value**

Add `SKILL_REFERENCED` at the end of the expected ordered list (and bump any expected `values().length`). Match the existing assertion style exactly.

- [ ] **Step 3: Run the test to verify it FAILS**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:test --tests '*EnumOrdinalStabilityTest*'`
Expected: FAIL — the enum does not yet contain `SKILL_REFERENCED`.

- [ ] **Step 4: Append `SKILL_REFERENCED` to the Java enum**

In `AiHubTaskArtifactKind.java`, after `TASK_REFERENCED`, add (keep the trailing comment convention):

```java
    TASK_REFERENCED,

    // Agent-opened / composer-referenced skill (SkillsTools archive). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    SKILL_REFERENCED
```

- [ ] **Step 5: Append `SKILL_REFERENCED` to the GraphQL enum**

In `ai-hub-artifact.graphqls`, add `SKILL_REFERENCED` as the last value of `enum AiHubTaskArtifactKind`.

- [ ] **Step 6: Run the test to verify it PASSES**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:test --tests '*EnumOrdinalStabilityTest*'`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/ai/ai-hub/ai-hub-api server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/ai-hub-artifact.graphqls
git commit -m "732 Add SKILL_REFERENCED artifact kind (Java + GraphQL enums)"
```

---

## Task 2: Dedup-aware server reference recorder

**Files:**
- Modify: `AiHubTaskArtifactRecorder.java` (interface — `git grep -l "interface AiHubTaskArtifactRecorder"`)
- Modify: `AiHubTaskArtifactRecorderImpl.java`
- Modify: `AiHubTaskArtifactServiceImpl.java` + its interface `AiHubTaskArtifactService.java`
- Test: `AiHubTaskArtifactServiceImplTest.java` (or IntTest — check which exists with `git grep -l "recordReference" -- '*Test*.java'`)

**Interfaces:**
- Produces: `AiHubTaskArtifactRecorder.recordReference(String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName)` — resolves the task by `(threadId, userId)` and dedups on `(task, kind, artifactId)`; no-op when `userId`/task is missing. Backed by `AiHubTaskArtifactService.recordReferenceByThread(String threadId, Long userId, AiHubTaskArtifactKind kind, String artifactId, String artifactName)` returning the existing or newly-saved `AiHubTaskArtifact`.

- [ ] **Step 1: Write the failing service test**

In the service test, add (adapt setup to the existing test's fixtures for creating a task with a threadId+userId):

```java
@Test
void testRecordReferenceByThreadDedupsOnThreadKindArtifactId() {
    // given a task exists for (threadId, userId) — reuse the test's task-creation helper
    String threadId = existingThreadId;
    Long userId = existingUserId;

    aiHubTaskArtifactService.recordReferenceByThread(
        threadId, userId, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");
    aiHubTaskArtifactService.recordReferenceByThread(
        threadId, userId, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");

    List<AiHubTaskArtifact> artifacts =
        aiHubTaskArtifactService.listByTask(existingTaskId);

    assertThat(artifacts)
        .filteredOn(artifact -> artifact.getKind() == AiHubTaskArtifactKind.SKILL_REFERENCED)
        .hasSize(1);
}
```

- [ ] **Step 2: Run it to verify FAIL**

Run: `./gradlew <ai-hub-service module path>:test --tests '*AiHubTaskArtifactService*'`
Expected: FAIL — `recordReferenceByThread` does not exist.

- [ ] **Step 3: Add `recordReferenceByThread` to the service interface + impl**

Interface (`AiHubTaskArtifactService.java`):

```java
    AiHubTaskArtifact recordReferenceByThread(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind, String artifactId,
        String artifactName);
```

Impl (`AiHubTaskArtifactServiceImpl.java`) — resolve the task by `(threadId, userId)` like `record(...)` does, then dedup on `(taskId, kind.ordinal(), artifactId)` using the existing repository method `findFirstByTaskIdAndKindAndArtifactId` (the same query `recordReference` uses for non-workflow kinds):

```java
    @Override
    public AiHubTaskArtifact recordReferenceByThread(
        String threadId, @Nullable Long userId, AiHubTaskArtifactKind kind, String artifactId,
        String artifactName) {

        if (userId == null) {
            return null;
        }

        Optional<AiHubTask> taskOptional = taskRepository.findByThreadIdAndUserId(threadId, userId);

        if (taskOptional.isEmpty()) {
            return null;
        }

        AiHubTask task = taskOptional.get();

        Optional<AiHubTaskArtifact> existing =
            taskArtifactRepository.findFirstByTaskIdAndKindAndArtifactId(task.getId(), kind.ordinal(), artifactId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubTaskArtifact artifact = new AiHubTaskArtifact();

        artifact.setTaskId(task.getId());
        artifact.setKind(kind);
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName(artifactName);
        artifact.setEnvironment(task.getEnvironment());
        artifact.setCreatedAt(LocalDateTime.now(clock));
        artifact.setStatus(AiHubTaskArtifactStatus.APPLIED);

        return taskArtifactRepository.save(artifact);
    }
```

(Declare the method `@Nullable`-returning; add the `@org.jspecify.annotations.Nullable` import if not present.)

- [ ] **Step 4: Add `recordReference` to the recorder interface + impl**

Interface (`AiHubTaskArtifactRecorder.java`):

```java
    /**
     * Server robustness layer for reference-kind artifacts. Resolves the task by (threadId, userId) and
     * dedups on (task, kind, artifactId) so it collapses onto the client hook's row instead of duplicating.
     */
    void recordReference(
        String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName);
```

Impl (`AiHubTaskArtifactRecorderImpl.java`) — resolve the kind and delegate:

```java
    @Override
    public void recordReference(
        String threadId, @Nullable Long userId, String artifactKind, String artifactId, String artifactName) {

        AiHubTaskArtifactKind kind = AiHubTaskArtifactKind.valueOf(artifactKind);

        taskArtifactService.recordReferenceByThread(threadId, userId, kind, artifactId, artifactName);
    }
```

- [ ] **Step 5: Run the test to verify PASS**

Run: `./gradlew <ai-hub-service module path>:test --tests '*AiHubTaskArtifactService*'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/ai/ai-hub
git commit -m "732 Add dedup-aware recordReference server layer for reference artifacts"
```

---

## Task 3: `OpenSkillTabToolCallback`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/OpenSkillTabToolCallback.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/tool/OpenSkillTabToolCallbackTest.java`

**Interfaces:**
- Consumes: `AiHubTaskArtifactRecorder.recordReference(...)` (Task 2); `AiHubToolInvocationContext.fromToolContext(ToolContext)` → `threadId()`, `userId()`.
- Produces: tool `openSkillTab`, input `{skillId, name}`, output JSON `{opened, skillId, name}`.

- [ ] **Step 1: Write the failing test** (mirror `OpenDataTableTabToolCallbackTest`; read it first with `git grep -l OpenDataTableTabToolCallbackTest`)

```java
class OpenSkillTabToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsOpenedResult() {
        OpenSkillTabToolCallback callback = new OpenSkillTabToolCallback(null);

        String result = callback.call("{\"skillId\":\"7\",\"name\":\"Triage\"}", null);

        assertThat(result).contains("\"opened\":true").contains("\"skillId\":\"7\"").contains("Triage");
    }

    @Test
    void testCallReturnsToolErrorWhenSkillIdBlank() {
        OpenSkillTabToolCallback callback = new OpenSkillTabToolCallback(null);

        String result = callback.call("{\"skillId\":\"\",\"name\":\"Triage\"}", null);

        assertThat(result).contains("skillId is required");
    }

    @Test
    void testToolDefinitionName() {
        assertThat(new OpenSkillTabToolCallback(null).getToolDefinition().name()).isEqualTo("openSkillTab");
    }
}
```

- [ ] **Step 2: Run it to verify FAIL**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*OpenSkillTabToolCallbackTest*'`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Create `OpenSkillTabToolCallback`** (EE header + `@version ee`; mirror `OpenDataTableTabToolCallback` and its recordArtifact pattern from `OpenWorkflowTabToolCallback`)

```java
public class OpenSkillTabToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(OpenSkillTabToolCallback.class);

    private static final String DESCRIPTION = """
        Open an AI skill in the AI Hub resource panel so the user can see it.
        Call this after creating a skill or when referring to an existing skill.
        Use the skill id returned from createAiSkill or getAiSkills - never invent skill IDs.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "skillId": {"type": "string", "description": "Skill id"},
                "name": {"type": "string", "description": "Display name for the tab"}
            },
            "required": ["skillId", "name"]
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final @Nullable AiHubTaskArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenSkillTabToolCallback(@Nullable AiHubTaskArtifactRecorder artifactRecorder) {
        this.artifactRecorder = artifactRecorder;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("openSkillTab")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            OpenSkillTabInput input = jsonMapper.readValue(toolInput, OpenSkillTabInput.class);

            if (input.skillId() == null || input.skillId()
                .isBlank()) {
                return toolError("skillId is required");
            }

            if (input.name() == null || input.name()
                .isBlank()) {
                return toolError("name is required");
            }

            recordArtifact(toolContext, input);

            return jsonMapper.writeValueAsString(new OpenSkillTabOutput(true, input.skillId(), input.name()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, OpenSkillTabToolCallback.class, "openSkillTab", exception);
        }
    }

    private void recordArtifact(@Nullable ToolContext toolContext, OpenSkillTabInput input) {
        if (artifactRecorder == null) {
            return;
        }

        AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null || invocationContext.threadId() == null) {
            return;
        }

        try {
            artifactRecorder.recordReference(
                invocationContext.threadId(), invocationContext.userId(), "SKILL_REFERENCED", input.skillId(),
                input.name());
        } catch (RuntimeException exception) {
            log.warn("Failed to record skill artifact for openSkillTab (skillId={})", input.skillId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record OpenSkillTabInput(String skillId, String name) {
    }

    public record OpenSkillTabOutput(boolean opened, String skillId, String name) {
    }
}
```

- [ ] **Step 4: Run the test to verify PASS**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*OpenSkillTabToolCallbackTest*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/OpenSkillTabToolCallback.java server/ee/libs/ai/ai-hub/ai-hub-service/src/test
git commit -m "732 Add OpenSkillTabToolCallback"
```

---

## Task 4: Server robustness recorder on data-table & KB open tools

**Files:**
- Modify: `.../ee/ai/hub/tool/OpenDataTableTabToolCallback.java`
- Modify: `.../ee/ai/hub/tool/OpenKnowledgeBaseTabToolCallback.java`
- Test: the two callbacks' existing tests (or add them mirroring Task 3)

**Interfaces:**
- Consumes: `AiHubTaskArtifactRecorder.recordReference(...)`.
- Produces: both constructors now take `@Nullable AiHubTaskArtifactRecorder`.

- [ ] **Step 1: Write/extend the failing test** — assert that with a recorder passed, a valid `call` invokes `recordReference("DATA_TABLE_REFERENCED", ...)` (use a Mockito mock recorder + a `ToolContext` carrying a threadId; mirror how `OpenWorkflowTabToolCallbackTest` builds the tool context).

- [ ] **Step 2: Run it — FAIL** (constructor has no recorder param yet).

- [ ] **Step 3: Add the recorder** — give each callback a `@Nullable AiHubTaskArtifactRecorder` field + `@SuppressFBWarnings("EI_EXPOSE_REP2")` constructor, and a `recordArtifact(toolContext, input)` method identical in shape to Task 3's but recording `"DATA_TABLE_REFERENCED"` / `"KB_REFERENCED"` with `input.dataTableId()` / `input.knowledgeBaseId()`. Call `recordArtifact(...)` right before building the success output.

- [ ] **Step 4: Run the test — PASS.**

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool
git commit -m "732 Record data-table/KB reference artifacts server-side on open"
```

---

## Task 5: Register the tools + recorders in configuration; document in prompts

**Files:**
- Modify: `.../ee/ai/hub/config/AiHubConfiguration.java` (both registration sites: the null-recorder ASK site ~L283–286 and the recorder-enabled site ~L452–455)
- Modify: `.../ee/ai/hub/config/DataAnalystConfiguration.java` (only if it constructs `OpenDataTableTabToolCallback`)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`

**Interfaces:**
- Consumes: `OpenSkillTabToolCallback` (Task 3), updated DT/KB ctors (Task 4).

- [ ] **Step 1: Recorder-enabled site** — where `new OpenWorkflowTabToolCallback(aiHubTaskArtifactRecorder)` is added, change the DT/KB constructions to pass the recorder and add the skill tool:

```java
        toolCallbacks.add(new OpenDataTableTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenKnowledgeBaseTabToolCallback(aiHubTaskArtifactRecorder));
        toolCallbacks.add(new OpenSkillTabToolCallback(aiHubTaskArtifactRecorder));
```

- [ ] **Step 2: Null-recorder ASK site** — pass `null` (preserve the existing "ASK relies on the client hook" behavior) and add the skill tool with `null`:

```java
        toolCallbacks.add(new OpenDataTableTabToolCallback(null));
        toolCallbacks.add(new OpenKnowledgeBaseTabToolCallback(null));
        toolCallbacks.add(new OpenSkillTabToolCallback(null));
```

Add the `import com.bytechef.ee.ai.hub.tool.OpenSkillTabToolCallback;` (sorted with the other `Open*ToolCallback` imports).

- [ ] **Step 3: `DataAnalystConfiguration`** — if it does `new OpenDataTableTabToolCallback()`, update to pass the recorder it has access to, or `null` if none. (Grep: `git grep -n "new OpenDataTableTabToolCallback" -- '*DataAnalystConfiguration.java'`.)

- [ ] **Step 4: Prompts** — in `prompt_ai_hub_ask.txt` and `prompt_ai_hub_build.txt`, add next to the other `open*Tab` lines:

```
- openSkillTab({skillId, name}) — show an AI skill in the resource panel.
```

- [ ] **Step 5: Build the module to verify wiring compiles**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava`
Expected: SUCCESS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "732 Register openSkillTab + recorder-backed DT/KB open tools; document in prompts"
```

---

## Task 6: Re-enable the delete-skill tool

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/SkillsTools.java:113-128`
- Test: `SkillsToolsTest` if present (`git grep -l "class SkillsToolsTest"`); otherwise add a minimal one.

**Interfaces:**
- Produces: `@Tool String deleteAiSkill(long id)` returning a confirmation message (matches `deleteProject` / `deleteWorkflow`).

- [ ] **Step 1: Write the failing test** — `deleteAiSkill(5)` calls `aiSkillFacade.deleteAiSkill(5)` (Mockito verify) and returns a non-blank confirmation string containing `"5"`.

- [ ] **Step 2: Run it — FAIL** (returns `void`).

- [ ] **Step 3: Uncomment + change return type**

```java
    @Tool(description = "Delete an AI skill by its ID. Returns a confirmation message.")
    public String deleteAiSkill(
        @ToolParam(description = "The ID of the skill to delete") long id) {

        try {
            aiSkillFacade.deleteAiSkill(id);

            if (log.isDebugEnabled()) {
                log.debug("deleteAiSkill({}): Deleted skill", id);
            }

            return "Deleted skill " + id + ".";
        } catch (Exception e) {
            log.error("deleteAiSkill({}): Failed to delete skill", id, e);

            throw new ExecutionException(
                "Failed to delete skill: " + e.getMessage(), e, SkillToolErrorType.DELETE_SKILL);
        }
    }
```

- [ ] **Step 4: Run the test — PASS.**

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add -A server/libs/automation/automation-ai/automation-ai-tool
git commit -m "732 Re-enable deleteAiSkill agent tool (returns confirmation message)"
```

---

## Task 7: Regenerate GraphQL client types

**Files:**
- Modify (generated): `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Regenerate**

Run: `cd client && npx graphql-codegen`
Expected: `graphql.ts` now contains `SkillReferenced = 'SKILL_REFERENCED'` in the `AiHubTaskArtifactKind` enum.

- [ ] **Step 2: Verify**

Run: `cd client && git grep -n "SkillReferenced" src/shared/middleware/graphql.ts`
Expected: one or more matches.

- [ ] **Step 3: Commit**

```bash
cd client && git add src/shared/middleware/graphql.ts
git commit -m "732 client - Regenerate GraphQL types for SKILL_REFERENCED"
```

---

## Task 8: Add the `skill` tab kind + `openSkillTab` store action

**Files:**
- Modify: `client/src/pages/automation/ai-hub/stores/useAiHubTabsStore.ts`
- Test: `client/src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

**Interfaces:**
- Produces: `AiHubTabType` variant `{id: string; kind: 'skill'; name: string; skillId: string}`; store action `openSkillTab(skillId: string, name: string) => string`.

- [ ] **Step 1: Write the failing test** (mirror the data-table test):

```ts
it('opens a skill tab and dedups by skillId', () => {
    const {result} = renderHook(() => useAiHubTabsStore());

    let firstId = '';

    act(() => {
        firstId = result.current.openSkillTab('7', 'Triage');
    });

    expect(result.current.openTabs).toHaveLength(1);
    expect(result.current.openTabs[0]).toMatchObject({kind: 'skill', name: 'Triage', skillId: '7'});

    act(() => {
        result.current.openSkillTab('7', 'Triage');
    });

    expect(result.current.openTabs).toHaveLength(1);
    expect(result.current.activeTabId).toBe(firstId);
});
```

- [ ] **Step 2: Run it — FAIL.**

Run: `cd client && npx vitest run src/pages/automation/ai-hub/stores/tests/useAiHubTabsStore.test.ts`

- [ ] **Step 3: Add the type variant** — in `AiHubTabType`, add (keys sorted):

```ts
    | {id: string; kind: 'skill'; name: string; skillId: string}
```

- [ ] **Step 4: Add the action signature** to `AiHubTabsStateI` (alphabetical among the `open*` methods):

```ts
    openSkillTab: (skillId: string, name: string) => string;
```

- [ ] **Step 5: Implement `openSkillTab`** (mirror `openDataTableTab`, keys sorted):

```ts
                openSkillTab: (skillId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'skill'}> =>
                                tab.kind === 'skill' && tab.skillId === skillId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {...state, activeTabId: existing.id, rightPanelOpen: true};
                        }

                        const newTab: AiHubTabType = {id: getRandomId(), kind: 'skill', name, skillId};

                        tabIdToReturn = newTab.id;

                        return {...state, activeTabId: newTab.id, openTabs: [...state.openTabs, newTab], rightPanelOpen: true};
                    });

                    return tabIdToReturn;
                },
```

- [ ] **Step 6: Run the test — PASS.**

- [ ] **Step 7: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/stores
git commit -m "732 client - Add skill tab kind and openSkillTab store action"
```

---

## Task 9: Make `AiSkillDetail` embeddable (accept a `skillId` prop)

**Files:**
- Modify: `client/src/pages/automation/ai/skills/components/AiSkillDetail.tsx`
- Test: `client/src/pages/automation/ai/skills/components/tests/AiSkillDetail.test.tsx`

**Interfaces:**
- Produces: `AiSkillDetail` accepts an optional `AiSkillDetailProps` `{skillId?: string}`; when provided it uses that id instead of the route param, and renders read-only (no route-dependent chrome).

- [ ] **Step 1: Read the component** to see how it currently obtains the skill id (route param via `useParams`) and fetches files.

Run: `sed -n '132,180p' client/src/pages/automation/ai/skills/components/AiSkillDetail.tsx`

- [ ] **Step 2: Write/extend the failing test** — render `<AiSkillDetail skillId="7" />` and assert it fetches skill `7` without a router param (mock the skill query hook; assert it's called with `7`).

- [ ] **Step 3: Add the prop**

```tsx
interface AiSkillDetailProps {
    skillId?: string;
}

const AiSkillDetail = ({skillId: skillIdProp}: AiSkillDetailProps = {}) => {
    const {skillId: skillIdParam} = useParams();
    const skillId = skillIdProp ?? skillIdParam;
    // ...existing body, using `skillId`; when skillIdProp is set, omit route-only affordances (e.g. breadcrumb/back).
```

Guard any route-only UI behind `!skillIdProp`.

- [ ] **Step 4: Run the test — PASS.** `cd client && npx vitest run src/pages/automation/ai/skills/components/tests/AiSkillDetail.test.tsx`

- [ ] **Step 5: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai/skills
git commit -m "732 client - Let AiSkillDetail accept a skillId prop for embedding"
```

---

## Task 10: Render the skill tab in the resource panel

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubResourcePanel.tsx`

- [ ] **Step 1: Read the panel's tab-kind switch** (`git grep -n "kind === 'dataTable'\|kind === 'workflow'" AiHubResourcePanel.tsx`).

- [ ] **Step 2: Add a `skill` branch** rendering the embeddable viewer:

```tsx
    if (activeTab.kind === 'skill') {
        return <AiSkillDetail skillId={activeTab.skillId} />;
    }
```

Add the sorted import `import AiSkillDetail from '@/pages/automation/ai/skills/components/AiSkillDetail';`.

- [ ] **Step 3: Typecheck**

Run: `cd client && npm run typecheck`
Expected: no errors (the `skill` kind is now handled; the discriminated union is exhaustive).

- [ ] **Step 4: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/AiHubResourcePanel.tsx
git commit -m "732 client - Render skill tab in the AI Hub resource panel"
```

---

## Task 11: Handle the `openSkillTab` tool-call in the runtime provider

**Files:**
- Modify: `client/src/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider.tsx` (add branch after the `openKnowledgeBaseTab` branch ~L639)
- Modify: the tab-result validators file (where `validateOpenDataTableTabResult` lives — `git grep -l validateOpenDataTableTabResult`)

- [ ] **Step 1: Add `validateOpenSkillTabResult`** mirroring `validateOpenDataTableTabResult` (fields `opened`, `skillId`, `name`; return `{opened: true, skillId, name}` or `{opened: false, error}`).

- [ ] **Step 2: Add the runtime-provider branch**

```tsx
            } else if (toolCallName === 'openSkillTab') {
                const raw = parseJson<unknown>(event.content, 'openSkillTab result');

                if (raw === null) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                const parsed = validateOpenSkillTabResult(raw);

                if (parsed === null) {
                    surfaceTabOpenFailure(toolCallName, 'unparseable result', event.content);

                    return;
                }

                if (!parsed.opened) {
                    surfaceTabOpenFailure(toolCallName, parsed.error);

                    return;
                }

                aiHubTabsStore.getState().openSkillTab(parsed.skillId, parsed.name);
            }
```

Add the sorted import for `validateOpenSkillTabResult`.

- [ ] **Step 3: Typecheck + run any runtime-provider tests.** `cd client && npm run typecheck`

- [ ] **Step 4: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/runtime-providers
git commit -m "732 client - Handle openSkillTab tool-call in AI Hub runtime provider"
```

---

## Task 12: Record skill artifacts in the client hook

**Files:**
- Modify: `client/src/pages/automation/ai-hub/tasks/hooks/useRecordReferencedArtifacts.ts`

- [ ] **Step 1: Add the mapping** to `KIND_TO_ARTIFACT_KIND` (keys sorted):

```ts
    skill: AiHubTaskArtifactKind.SkillReferenced,
```

- [ ] **Step 2: Add the `resolveArtifactKey` case**

```ts
        case 'skill':
            return {artifactId: tab.skillId, kind: KIND_TO_ARTIFACT_KIND.skill};
```

- [ ] **Step 3: Update the header comment** — the "scoped to the four kinds" note now includes skills (five kinds). Edit the comment to say files, workflows, data tables, knowledge bases, and skills.

- [ ] **Step 4: Typecheck.** `cd client && npm run typecheck`

- [ ] **Step 5: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/tasks/hooks/useRecordReferencedArtifacts.ts
git commit -m "732 client - Record SKILL_REFERENCED artifacts when a skill tab opens"
```

---

## Task 13: Sidebar render + reopen for `SKILL_REFERENCED`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx`
- Test: add a sidebar test asserting a `SKILL_REFERENCED` artifact renders and, on click, calls `openSkillTab`.

**Interfaces:**
- Consumes: `useAiHubTabsStore().openSkillTab` (Task 8).

- [ ] **Step 1: Read the two `DATA_TABLE_REFERENCED` blocks** — the icon/label block (~L115–124) and the quick-open block (~L308–321).

- [ ] **Step 2: Write the failing test** — mock the tabs store's `openSkillTab`; render the sidebar with a `SKILL_REFERENCED` artifact `{artifactId:'7', artifactName:'Triage'}`; click it; assert `openSkillTab('7', 'Triage')` was called.

- [ ] **Step 3: Add the icon/label branch** (mirror the data-table icon branch; use a Lucide `*Icon`, e.g. `SparklesIcon` or the skill icon used on the settings page):

```tsx
    if (kind === 'SKILL_REFERENCED') {
        return <SparklesIcon className="size-4 text-muted-foreground" />;
    }
```

- [ ] **Step 4: Add the quick-open branch**

```tsx
    if (artifact.kind === 'SKILL_REFERENCED') {
        aiHubTabsStore.getState().openSkillTab(artifact.artifactId, artifact.artifactName);

        return;
    }
```

- [ ] **Step 5: Run the test — PASS.**

- [ ] **Step 6: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/tasks/AiHubTasksSidebar.tsx
git commit -m "732 client - Render and reopen SKILL_REFERENCED artifacts in the task sidebar"
```

---

## Task 14: Replay `openSkillTab` on task switch

**Files:**
- Modify: `client/src/pages/automation/ai-hub/tasks/hooks/useSwitchTask.ts` (near the `openDataTableTab` / `openKnowledgeBaseTab` replay at ~L64–71)

- [ ] **Step 1: Add a replay branch** for `SKILL_REFERENCED` artifacts that pushes an `openSkillTab` tool-call replay entry (mirror the `openDataTableTab` entry shape exactly):

```ts
            } else if (artifact.kind === 'SKILL_REFERENCED') {
                toolCalls.push({
                    args: {name: artifact.artifactName, skillId: artifact.artifactId},
                    toolName: 'openSkillTab',
                });
```

(Match the exact object shape used by the neighboring `openDataTableTab` push — keys sorted.)

- [ ] **Step 2: Typecheck + run the hook's tests.** `cd client && npm run typecheck`

- [ ] **Step 3: Commit**

```bash
cd client && npm run check
git add src/pages/automation/ai-hub/tasks/hooks/useSwitchTask.ts
git commit -m "732 client - Replay openSkillTab for SKILL_REFERENCED artifacts on task switch"
```

---

## Task 15: Full-stack verification

- [ ] **Step 1: Server checks for touched modules**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:ee:libs:ai:ai-hub:ai-hub-api:check :server:libs:automation:automation-ai:automation-ai-tool:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

- [ ] **Step 3: Manual smoke (optional, if a dev stack is up)** — in the AI Hub BUILD chat, ask the agent to open a skill; confirm the skill renders in the right panel and appears once in the task sidebar; click the sidebar entry to reopen; ask the agent to delete a skill and confirm it's gone.

- [ ] **Step 4: Final commit if any formatting/fixups remain**

```bash
git status
```

---

## Self-review notes (coverage against the spec)

- Spec §Server 1 (delete) → Task 6. §Server 2 (both enums) → Task 1. §Server 3 (OpenSkillTab) → Task 3.
  §Server 4 (dedup recorder) → Task 2. §Server 5 (DT/KB recorder) → Task 4. §Server 6 (config) + 7
  (prompts) → Task 5.
- Spec §Client 8 (codegen) → Task 7. §Client 9 (store) → Task 8. §Client 10 (AiSkillDetail) → Task 9.
  §Client 11 (panel) → Task 10. §Client 12 (runtime provider) → Task 11. §Client 13 (hook) → Task 12.
  §Client 14 (sidebar) → Task 13. §Client 15 (useSwitchTask) → Task 14.
- Spec §Testing → folded into each task (TDD) + Task 15.
