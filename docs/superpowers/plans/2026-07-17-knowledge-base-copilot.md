# Knowledge Base Copilot Implementation Plan (Slice 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the Domain Copilot pattern (proven by Slice 1, Context Store — all on this branch) to Knowledge Base: ASK/BUILD Copilot on the KB detail page + a `knowledge_base_agent` AI-Hub subagent. Design: `docs/superpowers/specs/2026-07-17-domain-copilot-context-store-kb-datatable-design.md`.

**Architecture:** Mirror the Context Store slice, whose files are the in-repo templates: move 5 KB `ToolCallback` classes from `ai-hub-service` to shared EE `automation-ai-tool` (package `knowledgebase`), swap `AiHubToolInvocationContext` → `AgentToolInvocationContext`; introduce a shared `ToolMutationArtifactRecorder` interface (+ ai-hub adapter) so the 2 artifact-recording mutation tools keep their AI-Hub audit trail; add `deleteKnowledgeBase`; factory (read/write); `KnowledgeBaseSpringAIAgent` + prompts; EE `KnowledgeBaseAgentConfiguration`; `KnowledgeBaseAgentToolCallback` dual-registered (in-app ai_hub ASK/BUILD **and** MCP contributor — Slice 1's regression lesson); `knowledge_base` branch in `CopilotApiController`; frontend trigger on `KnowledgeBase.tsx`.

**Tech Stack:** Java 21 / Spring AI / Gradle; React + TS + vitest.

## Global Constraints

- Run everything from the worktree root `/Volumes/Data/bytechef/bytechef/.claude/worktrees/context-store-copilot`.
- Shared tools go in `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/knowledgebase/`; Enterprise License header (copy from any `contextstore/` sibling).
- Tools read workspace/env via `AgentToolInvocationContext.fromToolContext(toolContext)`; errors via `ToolErrors.toolError(...)`/`ToolErrors.runtimeFailure(...)`.
- `OpenKnowledgeBaseTabToolCallback` STAYS in ai-hub-service (depends on `AiHubTaskArtifactRecorder`) and stays registered flat on both ai_hub agents.
- Gradle verification always fresh: `--rerun-tasks`. Checkstyle (`checkstyleMain`/`checkstyleTest`) must pass for touched modules — watch `UnusedImports` and `MethodLength` (150) on `AiHubConfiguration` bean methods.
- Backend `Source` (`ai-copilot-api`) and frontend `Source` (`useCopilotStore.ts`) stay in sync: both gain `KNOWLEDGE_BASE`.
- In-repo templates (Slice 1): `contextstore/` tool classes & factory & tests; `ContextStoreSpringAIAgent`; `prompt_context_store_{ask,build}.txt`; `ContextStoreAgentConfiguration`; `ContextStoreAgentToolCallback`(+Test); the `context_store` controller branch; `ContextStoreSources.tsx` copilot wiring & test. When this plan says "mirror X", read X on this branch and replicate faithfully with KB substitutions.

---

## Task 1: Add `KNOWLEDGE_BASE` to both Source enums

**Files:** `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java`; `client/src/shared/components/copilot/stores/useCopilotStore.ts`.

Mirror commit `c31f0d0c288` (Slice 1 Task 1): append `KNOWLEDGE_BASE` to the backend enum list and `KNOWLEDGE_BASE = 'KNOWLEDGE_BASE',` to the frontend enum.

- [ ] Step 1: Both edits.
- [ ] Step 2: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-api:compileJava -q` → BUILD SUCCESSFUL.
- [ ] Step 3: Commit `feat(copilot): add KNOWLEDGE_BASE source enum on both surfaces`.

---

## Task 2: Move 5 KB tool callbacks to the shared lib + `ToolMutationArtifactRecorder`

**Files:**
- Move (git mv) from `server/ee/libs/ai/ai-hub/ai-hub-service/.../tool/` to `.../automation-ai-tool/.../tool/knowledgebase/`: `ListKnowledgeBasesToolCallback`, `QueryKnowledgeBaseToolCallback`, `AddKnowledgeBaseDocumentToolCallback`, `DeleteKnowledgeBaseDocumentToolCallback`, `CloneKnowledgeBaseToolCallback` (+ existing tests: `ListKnowledgeBasesToolCallbackTest`, `QueryKnowledgeBaseToolCallbackTest`; the other three have no tests).
- Create: `.../automation-ai-tool/.../tool/ToolMutationArtifactRecorder.java` (shared interface).
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/.../tool/AiHubToolMutationArtifactRecorder.java` (adapter).
- Modify: `automation-ai-tool/build.gradle.kts` (+2 deps), `AiHubConfiguration.java` (repoint imports + adapted constructor args).

**Interfaces:**
- Produces: 5 relocated classes in `com.bytechef.ee.automation.ai.tool.knowledgebase` (TOOL_NAMEs unchanged: `listKnowledgeBases`, `queryKnowledgeBase`, `addKnowledgeBaseDocument`, `deleteKnowledgeBaseDocument`, `cloneKnowledgeBase`); `ToolMutationArtifactRecorder` with a minimal method set derived from the existing `recordArtifact` call sites (kind passed as `String`, plus `conversationId` and whatever payload args the call sites use); `AiHubToolMutationArtifactRecorder(AiHubTaskArtifactService)` mapping kind string → `AiHubTaskArtifactKind.valueOf(...)`.

- [ ] Step 1: Add deps to `automation-ai-tool/build.gradle.kts`:
```kotlin
implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-api"))
implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-api"))
```
- [ ] Step 2: `git mv` the 5 classes (+2 tests); change packages; swap `AiHubToolInvocationContext` → `AgentToolInvocationContext` (all 5 read it).
- [ ] Step 3: Rework the two artifact-recording tools (`AddKnowledgeBaseDocumentToolCallback`, `DeleteKnowledgeBaseDocumentToolCallback`): read the exact `taskArtifactService.recordArtifact(...)` call shapes and `threadId()` usages FIRST; define `ToolMutationArtifactRecorder` (shared lib, package `com.bytechef.ee.automation.ai.tool`) to carry exactly those calls with the kind as a `String`; constructor arg becomes `@Nullable ToolMutationArtifactRecorder` (replacing `AiHubTaskArtifactService`); `invocationContext.threadId()` → `invocationContext.conversationId()` (same underlying value — `AiHubSpringAIAgent.toolContext()` populates it; null on the Copilot panel path, so recording naturally no-ops there). Guard: record only when recorder != null && conversationId != null (preserve the existing null-thread guard semantics).
- [ ] Step 4: Create `AiHubToolMutationArtifactRecorder` in ai-hub-service implementing the interface via `AiHubTaskArtifactService` (`AiHubTaskArtifactKind.valueOf(kind)`); Enterprise header.
- [ ] Step 5: Repoint `AiHubConfiguration`: imports of the 5 moved classes → `com.bytechef.ee.automation.ai.tool.knowledgebase.*`; at the two flat call sites passing `taskArtifactService` to Add/Delete-document tools, pass `new AiHubToolMutationArtifactRecorder(taskArtifactService)` instead. Keep all registrations otherwise intact (flat removal is Task 8). `OpenKnowledgeBaseTabToolCallback` untouched.
- [ ] Step 6: Update moved tests' packages/context construction (`AgentToolInvocationContext.builder()...toToolContext()`), mirroring the `contextstore` test ports.
- [ ] Step 7: Fresh build: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:test :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava --rerun-tasks -q` → BUILD SUCCESSFUL; then both modules' checkstyle targets → clean.
- [ ] Step 8: Commit `refactor(knowledge-base): move copilot tool callbacks to shared automation-ai-tool lib`.

---

## Task 3: Add the new `deleteKnowledgeBase` tool

**Files:** Create `.../tool/knowledgebase/DeleteKnowledgeBaseToolCallback.java` + test.

Mirror `DeleteContextStoreToolCallback`(+Test) with ONE deliberate difference: `WorkspaceKnowledgeBaseFacade.deleteWorkspaceKnowledgeBase(Long knowledgeBaseId)` takes only the id (authorization enforced by its `@PreAuthorize hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_EDIT')`), so the tool does NOT read workspace from the invocation context — it parses `{id}`, calls the facade, returns `{"deleted":true,"id":...}`. DESCRIPTION: irreversible cascade (deletes all documents), always confirm with the user first. Tests (TDD, fail-first): delete calls facade with the id; missing id → `id is required`; facade `IllegalArgumentException` surfaces as toolError.

- [ ] Step 1: Failing test → run (`--rerun-tasks`, `--tests "*DeleteKnowledgeBaseToolCallbackTest"`) → FAIL.
- [ ] Step 2: Implement → test PASS + module `checkstyleMain checkstyleTest` clean.
- [ ] Step 3: Commit `feat(knowledge-base): add deleteKnowledgeBase tool`.

---

## Task 4: `KnowledgeBaseToolCallbacksFactory`

**Files:** Create `.../tool/knowledgebase/KnowledgeBaseToolCallbacksFactory.java` + test.

Mirror `ContextStoreToolCallbacksFactory`(+Test). Constructor deps (verify each against the real moved-class constructors): `WorkspaceKnowledgeBaseFacade`, `KnowledgeBaseFacade`, `KnowledgeBaseService`, `KnowledgeBaseDocumentFacade`, `KnowledgeBaseDocumentService`, `@Nullable ToolMutationArtifactRecorder`.
- `readToolCallbacks()`: `listKnowledgeBases`, `queryKnowledgeBase`.
- `writeToolCallbacks()`: read + `addKnowledgeBaseDocument`, `deleteKnowledgeBaseDocument`, `cloneKnowledgeBase`, `deleteKnowledgeBase`.
Test asserts membership (read excludes `deleteKnowledgeBase`/`addKnowledgeBaseDocument`; write includes them).

- [ ] TDD steps as in Slice 1 Task 4; fresh test + checkstyle; commit `feat(knowledge-base): add tool-list factory for ASK/BUILD`.

---

## Task 5: `KnowledgeBaseSpringAIAgent` + prompts

**Files:** Create `server/libs/ai/ai-copilot/ai-copilot-service/.../agent/KnowledgeBaseSpringAIAgent.java`; `.../resources/prompt_knowledge_base_ask.txt`; `.../resources/prompt_knowledge_base_build.txt`.

Agent class: byte-for-byte mirror of `ContextStoreSpringAIAgent` (itself a mirror of `SkillsSpringAIAgent`) with only the class name changed. Prompts (working defaults, product refines later):

`prompt_knowledge_base_ask.txt`:
```
You are the Knowledge Base assistant in ByteChef, embedded in the Knowledge Base detail page.

You help the user understand and query a Knowledge Base: what knowledge bases exist, their documents,
and the content inside them. Use `listKnowledgeBases` to discover knowledge bases and
`queryKnowledgeBase` to retrieve relevant content for a question (retrieval-augmented lookup).

You are READ-ONLY. Never add, delete, or clone anything. If the user asks to change something,
explain what you would change and tell them to switch to Build mode.

Be concise. Cite knowledge base and document names/ids. If workspace context is unavailable, say so.
```

`prompt_knowledge_base_build.txt`:
```
You are the Knowledge Base builder in ByteChef, embedded in the Knowledge Base detail page.

You can inspect AND modify Knowledge Bases. Available actions: add a document
(`addKnowledgeBaseDocument`), delete a document (`deleteKnowledgeBaseDocument`), clone a knowledge
base (`cloneKnowledgeBase`), and delete an entire knowledge base (`deleteKnowledgeBase`).

Always call `listKnowledgeBases` (and `queryKnowledgeBase` where helpful) first to ground yourself.
Before any irreversible action (delete document, delete knowledge base), state exactly what will be
removed and get the user's explicit confirmation in the conversation before calling the tool.

Be concise; report the ids you created or changed.
```

- [ ] Create all three; fresh `ai-copilot-service:compileJava --rerun-tasks` + `checkstyleMain` clean; commit `feat(copilot): add knowledge base agent class + ask/build prompts`.

---

## Task 6: EE `KnowledgeBaseAgentConfiguration`

**Files:** Create `server/ee/libs/ai/ai-hub/ai-hub-service/.../config/KnowledgeBaseAgentConfiguration.java`. (No build.gradle change — the `ai-copilot-service` dep was added in Slice 1; KB facades are OSS modules ai-hub-service already depends on.)

Mirror `ContextStoreAgentConfiguration` bean-for-bean: same gate `@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")`, same `readPrompt`/`wrapToolCallbacks` helpers, substituting: `KnowledgeBaseToolCallbacksFactory` bean (recorder supplied via `ObjectProvider<AiHubTaskArtifactService>` → `new AiHubToolMutationArtifactRecorder(...)` when available, else null — the service is absent when only copilot is enabled); `knowledgeBaseAskSpringAIAgent`/`knowledgeBaseBuildSpringAIAgent` (agentIds `knowledge_base_ask`/`knowledge_base_build` from `Source.KNOWLEDGE_BASE.name() + "_" + Mode.X.name()` lowercased); `knowledgeBaseAskSubAgentChatClient`/`knowledgeBaseBuildSubAgentChatClient`.

- [ ] Create; fresh `ai-hub-service:compileJava --rerun-tasks` + `checkstyleMain` clean; commit `feat(copilot): add EE KnowledgeBaseAgentConfiguration (source agents + subagent clients)`.

---

## Task 7: `KnowledgeBaseAgentToolCallback` + `CopilotAgentType` entries

**Files:** Create `server/libs/ai/ai-copilot/ai-copilot-tool/.../tool/KnowledgeBaseAgentToolCallback.java` + test; modify `CopilotAgentType.java`.

Mirror `ContextStoreAgentToolCallback`(+Test — including `@ExtendWith(ObjectMapperSetupExtension.class)`): tool name `knowledge_base_agent`, `CopilotAgentType.KNOWLEDGE_BASE_AGENT` in `CurrentAgentContext.callWith`, DESCRIPTION:
```
Delegate a user request about Knowledge Bases to a specialised Knowledge Base subagent.
A Knowledge Base stores documents whose content is indexed for retrieval. The subagent owns
listing, querying, and (in build mode) adding/deleting documents and cloning/deleting knowledge
bases. Prefer calling it over reasoning about knowledge bases directly. Returns a synthesised
markdown report or a summary of the mutations performed.
```
`CopilotAgentType` additions: `KNOWLEDGE_BASE_ASK("knowledge_base_ask", false), KNOWLEDGE_BASE_BUILD("knowledge_base_build", false), KNOWLEDGE_BASE("knowledge_base", true), KNOWLEDGE_BASE_AGENT("knowledge_base_agent", false),`.

- [ ] TDD; fresh test + checkstyle; commit `feat(copilot): add knowledge_base_agent subagent tool callback`.

---

## Task 8: Wire delegation everywhere; remove flat KB tools

**Files:** `ToolCallbackContributorConfiguration.java` (OSS); `CopilotApiController.java` (EE rest); `AiHubConfiguration.java` (EE).

- [ ] Step 1 (contributor/MCP): mirror the contextStore lines — `@Qualifier("knowledgeBaseBuildSubAgentChatClient") ObjectProvider<ChatClient> knowledgeBaseProvider` + `ifAvailable(... new KnowledgeBaseAgentToolCallback(chatClient))` + import.
- [ ] Step 2 (controller): add after the `context_store` branch:
```java
} else if (agentId.equals("knowledge_base")) {
    if (Mode.valueOf((String) mode) == Mode.BUILD) {
        agentId = "knowledge_base_build";
    } else {
        agentId = "knowledge_base_ask";
    }
}
```
- [ ] Step 3 (in-app dual registration — Slice 1 lesson, do NOT skip): in `AiHubConfiguration`, mirror the contextStore wiring end-to-end: new param `knowledgeBaseSubAgentChatClientProvider` in `registerCopilotSubAgentToolCallbacks` (+ `ProgressReportingToolCallback(new KnowledgeBaseAgentToolCallback(chatClient), "knowledge_base_agent")` in its body); `@Qualifier("knowledgeBaseAskSubAgentChatClient")` param on the ASK bean and `...Build...` on the BUILD bean; both call sites pass them (position mirroring contextStore's).
- [ ] Step 4 (remove flat KB tools): in both ASK and BUILD bean bodies remove the `ListKnowledgeBases`/`QueryKnowledgeBase`/`AddKnowledgeBaseDocument`/`DeleteKnowledgeBaseDocument`/`CloneKnowledgeBase` registrations; KEEP `OpenKnowledgeBaseTabToolCallback` on both. Remove imports/params that become unused (KB facades/services, and the Task 2 adapter usage if now unused at flat sites — the adapter class itself stays for Task 6's factory bean).
- [ ] Step 5: Fresh compile of the three touched modules (`--rerun-tasks`) → BUILD SUCCESSFUL; `ai-hub-service:checkstyleMain` → 0 (watch MethodLength); `ai-hub-service:test` → all pass (adapt any test asserting flat KB tools to expect `knowledge_base_agent`; report which).
- [ ] Step 6: Commit `refactor(ai-hub): delegate knowledge base to knowledge_base_agent subagent; drop flat tools`.

---

## Task 9: Frontend trigger + post-turn invalidation on the KB detail page

**Files:** `client/src/pages/automation/knowledge-base/KnowledgeBase.tsx`; `client/src/pages/automation/knowledge-base/components/KnowledgeBaseHeader.tsx` (add a `right` slot — it has none today); test `client/src/pages/automation/knowledge-base/tests/KnowledgeBase.test.tsx` (exists; mock-everything style).

Mirror `ContextStoreSources.tsx`'s copilot wiring (the Slice 1 template): `openCopilot` calls `setContext({mode: MODE.ASK, parameters: {knowledgeBaseId: id}, source: Source.KNOWLEDGE_BASE})` + `setCopilotPanelOpen(true)`; button gated on `ai.copilot.enabled` (`useApplicationInfoStore`); parameters carry `{knowledgeBaseId}` ONLY (env/workspace auto-injected by `CopilotRuntimeProvider`). Post-turn: `useCopilotPostTurnRegistry.register(Source.KNOWLEDGE_BASE, ...)` in a `useEffect` with cleanup, invalidating `['knowledgeBase']` and `['knowledgeBases']` (TanStack v5 prefix-match covers `['knowledgeBase', {id}]` — same convention as sibling mutation handlers; verify with a grep of `invalidateQueries` under `knowledge-base*/`). `KnowledgeBaseHeader` gains an optional `right?: ReactNode` prop rendered in its layout (match `DataTableHeader.tsx`'s right-slot shape); `KnowledgeBase.tsx` passes the button through it. TDD: failing test first (click "Ask Copilot" → `setContext` args + panel open), following the existing test file's mock pattern (it mocks `KnowledgeBaseHeader` — the test may need that mock widened to render `right`, mirroring how the ContextStoreSources test widened its Button mock; do NOT weaken existing assertions).

- [ ] Failing test → implement → `npm --prefix client test -- KnowledgeBase` all pass → `npx tsc --project client/tsconfig.json --noEmit` clean → commit `feat(knowledge-base): add copilot trigger + post-turn invalidation to detail page`.

---

## Task 10: Full-slice verification

- [ ] `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:test :server:libs:ai:ai-copilot:ai-copilot-tool:test :server:libs:ai:ai-copilot:ai-copilot-service:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:test :server:ee:libs:ai:ai-hub:ai-hub-service:checkstyleMain -q` → BUILD SUCCESSFUL.
- [ ] `npm --prefix client test -- KnowledgeBase` + `npx tsc --noEmit` → clean.
- [ ] Note in ledger anything not verifiable without booting the app.

## Self-Review

Spec coverage: shared tools+recorder (T2), delete tool (T3), factory (T4), agent+prompts (T5), EE config (T6), subagent callback+enum (T7), dual registration+controller+flat removal (T8), frontend (T1, T9), tests throughout, verification (T10). ✅ Type consistency: `knowledge_base_ask`/`knowledge_base_build`/`knowledge_base_agent`, `Source.KNOWLEDGE_BASE`, factory/bean names used identically across tasks. Placeholders: none — "mirror X" always names a real file on this branch.
