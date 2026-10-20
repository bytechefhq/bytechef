# AI Hub Workflow Chats (Phase 1b) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port `0_732`'s workflow chats onto `0_p_ai_hub`: a hub chat bound to a deployed chat-trigger workflow, whose turns run the workflow through the webhook executor instead of an LLM.

**Architecture:** Server first: a `kind` column and `WORKFLOW_CHAT` kind on `AiHubChat`; a facade listing of chat workflows gated by the `WORKFLOW_VIEW` scope; `AiHubRoutingAgent` replaces the two LLM agents as the `ai_hub_ask` / `ai_hub_build` `LocalAgent`s and routes bridged chats to `WebhookBridgeAgent`, which streams the workflow's output through `AgUiStreamBridge` and stops the job through the phase 1 run registry's stop signal. Then the client: the picker's "Workflow chats" cascade, chat creation, the composer and sidebar differences, and the `ask-workflow-question` resume. Playwright specs close it.

**Tech Stack:** Java 25 / Spring Boot 4 / Spring AI / AG-UI `LocalAgent`, Caffeine and Redis caches, Liquibase, JUnit 5 + Mockito + AssertJ, Gradle; React 19 / TypeScript / TanStack Query / Vitest / Playwright.

**Spec:** `docs/superpowers/specs/2026-10-19-ai-hub-workflow-chats-design.md`. Source of every ported file: branch `0_732`, read only with `git show 0_732:<path>` (referred to as SOURCE below; its working tree at `/Volumes/Data/bytechef/bytechef` belongs to another session and is never touched). Read the SOURCE file before porting; copy, then trim to what the task names. Phase 1 context: `docs/agents/ai-hub.md`.

## Global Constraints

- Work in the worktree created by Task 0 on branch `0_p_ai_hub_wf`; `0_p_ai_hub` itself is checked out in `/Volumes/Data/bytechef/bytechef-master`, where the user runs the app, and is only fast-forwarded by Task 13.
- Commit ticket `732`. Server: `732 <desc>`; client: `732 client - <desc>`. No `Co-Authored-By` trailer, no attribution footer. Commit when green; never push.
- Files under `server/ee/` carry the ByteChef Enterprise license header and `@version ee`; everything else Apache 2.0.
- No explanatory inline comments; the why goes in the commit message. Ported SOURCE Javadoc may stay; ported SOURCE `//` rationale comments are dropped. No em dashes in prose, messages or comments; the one em dash allowed is inside the UI label string `${projectName} — ${workflowLabel}`.
- Checkstyle: test method names camelCase without underscores, including private helpers; no `TODO:`; no empty blocks. Spotless: `./gradlew spotlessApply` before every server commit; `cd client && npm run format` before every client commit.
- Verification is `./gradlew <module>:check > <log> 2>&1` on one line, `echo "EXIT=$?"` on the next line, then `grep '^> Task .* FAILED' <log>`; never pipe Gradle into `tail`/`grep`. Client: `cd client && npm run check`.
- Never port: `AGENT_CHAT`, `aiAgentId`, `createAgentChat`, `workspaceChatAgents`, `RunChatWorkflowToolCallback`, `workflowStreamHandler.ts`, `AiHubWorkflowExecutionViewer.tsx`, attachments (`AssetFileSystemFacade`, `BridgedFileEntry`, `MAX_ATTACHMENT_BYTES`, `extractAttachments`, `promoteAttachments`, `recordAttachmentFailure`), `hasSuspendingTask`, `AiAgentSseEventType.APPROVAL_REQUEST`, `ProjectVisibilityFilter`, `hasWorkspaceScopeInEnvironmentId`, `WorkspaceAccessGuard`, visibility/participation/sharing fields.
- Every new bean stays behind `@ConditionalOnAiHub` (never SOURCE's `@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled")`).
- Keep phase 1's one-run-per-chat rule in `InFlightAiHubRunRegistry`; `WorkflowChatGuard` carries no in-flight lock.

## Review Focus

1. A turn on a workflow chat whose deployment was disabled after the chat was created: the user gets a run error naming the disabled workflow, and the chat is free for the next message. Pinned in Task 6.
2. Stop clicked before the workflow's `start` event has delivered the job id: the job is stopped the moment the id arrives, not left running. Pinned in Task 6.
3. An `ask_user_question` event without a `resumeUrl`: the turn ends with a run error, and the next message starts a fresh run instead of waiting forever. Pinned in Task 5.
4. A workflow whose output has no extractable text (empty or non-text body): the turn completes with the fallback reply, the hub transcript gets the user turn, and nothing throws. Pinned in Task 6.
5. A workflow chat whose workflow is no longer deployed: the sidebar shows the stored title (no live label), and a turn gets a run error rather than a 500. Pinned in Tasks 6 and 9.

---

### Task 0: Worktree

**Files:** none in the repo.

- [ ] **Step 1: Create the worktree on a new branch from `0_p_ai_hub`**

Run from `/Volumes/Data/bytechef/bytechef-master`:
```bash
git worktree add -b 0_p_ai_hub_wf /Volumes/Data/bytechef/bytechef-worktrees/ai-hub-wf 0_p_ai_hub
```
Expected: `HEAD is now at de3c229946d 732 docs - Give workflow chats a conversation id distinct from the hub session` (or the current tip of `0_p_ai_hub`).

- [ ] **Step 2: Confirm the baseline compiles**

Run in the new worktree: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava > /tmp/ai-hub-wf-baseline.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=0`.

All later tasks run in `/Volumes/Data/bytechef/bytechef-worktrees/ai-hub-wf`.

---

### Task 1: Chat kind, columns and GraphQL type

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/chat/AiHubChatKind.java` (SOURCE, same path, without `AGENT_CHAT`)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260502000001_ai_hub_chat_add_workflow_chat_kind.xml` (SOURCE, same path, same changeSet id and author, XML comment dropped)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/chat/AiHubChat.java` (SOURCE's `kind`, `workflowExecutionId`, `projectDeploymentId` columns and accessors only)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/ai-hub-chat.graphqls` (`kind: AiHubChatKind!`, `workflowExecutionId: String`, `projectDeploymentId: Long` on `AiHubChat`; `enum AiHubChatKind { STANDARD WORKFLOW_CHAT }`)
- Modify: the changelog master that includes the aihub folder, only if it lists files explicitly (check `includeAll` first; SOURCE needs no master change)
- Test: `server/ee/libs/ai/ai-hub/ai-hub-api/src/test/java/com/bytechef/ee/ai/hub/chat/AiHubChatKindTest.java`

**Interfaces:**
- Produces: `enum AiHubChatKind { STANDARD, WORKFLOW_CHAT; boolean isWebhookBridged() }` (ordinals 0 and 1); `AiHubChat.getKind(): AiHubChatKind` (default `STANDARD`), `setKind(AiHubChatKind)`, `getWorkflowExecutionId(): String`, `setWorkflowExecutionId(String)`, `getProjectDeploymentId(): Long`, `setProjectDeploymentId(Long)`.

- [ ] **Step 1: Write the failing test**

```java
@Test
void testOrdinalsMatchTheStoredValues() {
    assertThat(AiHubChatKind.STANDARD.ordinal()).isZero();
    assertThat(AiHubChatKind.WORKFLOW_CHAT.ordinal()).isEqualTo(1);
}

@Test
void testOnlyWorkflowChatIsWebhookBridged() {
    assertThat(AiHubChatKind.WORKFLOW_CHAT.isWebhookBridged()).isTrue();
    assertThat(AiHubChatKind.STANDARD.isWebhookBridged()).isFalse();
}

@Test
void testNewChatIsStandard() {
    assertThat(new AiHubChat(7L).getKind()).isEqualTo(AiHubChatKind.STANDARD);
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:test --tests '*AiHubChatKindTest*' > /tmp/t1.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`, compilation error on `AiHubChatKind`.

- [ ] **Step 3: Port the enum, the entity columns and the changelog; add the GraphQL fields**

`kind` is stored as `int` via `EnumOrdinals.fromOrdinal`, as in SOURCE. The GraphQL controller needs no change: Spring GraphQL maps the getters.

- [ ] **Step 4: Run the module checks**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:check :server:ee:libs:ai:ai-hub:ai-hub-graphql:check :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/t1.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t1.log`.
Expected: `EXIT=0`, no FAILED lines (the schema parity tests in ai-hub-graphql still pass).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub
git commit -m "732 Add the workflow chat kind to AI Hub chats"
```

---

### Task 2: Chat workflow listing in the facade

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/domain/HostedChatTriggers.java` (SOURCE, same path)
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/test/java/com/bytechef/platform/configuration/domain/HostedChatTriggersTest.java` (SOURCE, same path)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-api/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacade.java` (`ChatWorkflow` record and two methods)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeImpl.java` (move the listing in from the controller, SOURCE's shape minus `projectVisibilityFilter` and the `isGenerated` filter)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/java/com/bytechef/automation/configuration/web/graphql/ProjectDeploymentWorkflowGraphQlController.java` (`workspaceChatWorkflows` delegates; delete the inlined listing, `hasHostedChatTrigger` and the local `ChatWorkflow` record)
- Modify: `server/libs/automation/automation-configuration/automation-configuration-graphql/src/main/resources/graphql/project-deployment.graphqls` (`ChatWorkflow` gains `projectWorkflowId: ID!` and `workflowId: ID!`)
- Create: `server/libs/automation/automation-configuration/automation-configuration-service/src/test/java/com/bytechef/automation/configuration/facade/ProjectDeploymentFacadeChatWorkflowTest.java` (SOURCE, same path, visibility and generated-type cases dropped)

**Interfaces:**
- Produces: `ProjectDeploymentFacade.ChatWorkflow(long projectDeploymentId, long projectId, String projectName, long projectWorkflowId, String workflowExecutionId, String workflowId, String workflowLabel)`; `List<ChatWorkflow> getWorkspaceChatWorkflows(long workspaceId, @Nullable Long environmentId)` (null lists every environment); `boolean isWorkspaceChatWorkflow(long workspaceId, String workflowExecutionId)` (true when the id is in the all-environment listing; no parsing of the id). Both carry `@PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")`.
- `HostedChatTriggers.hasHostedChatTrigger(List<WorkflowTrigger>)` and `isChatTriggerType(@Nullable String)` as in SOURCE.

- [ ] **Step 1: Port `HostedChatTriggersTest` and write the failing facade tests**

Port SOURCE's `ProjectDeploymentFacadeChatWorkflowTest`, keeping its mocks. Add:

```java
@Test
void testIsWorkspaceChatWorkflowMatchesAcrossEnvironments() {
    // deployments mocked for environment null (all), one hosted chat workflow with execution id "exec-1"
    assertThat(facade.isWorkspaceChatWorkflow(WORKSPACE_ID, "exec-1")).isTrue();
    assertThat(facade.isWorkspaceChatWorkflow(WORKSPACE_ID, "not-an-id")).isFalse();
}

@Test
void testListingCarriesProjectWorkflowIdAndWorkflowId() {
    ChatWorkflow chatWorkflow = facade.getWorkspaceChatWorkflows(WORKSPACE_ID, ENVIRONMENT_ID).getFirst();
    assertThat(chatWorkflow.projectWorkflowId()).isEqualTo(PROJECT_WORKFLOW_ID);
    assertThat(chatWorkflow.workflowId()).isEqualTo(WORKFLOW_ID);
}
```

`isWorkspaceChatWorkflow("not-an-id")` must not throw: it compares strings against the listing.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-service:test --tests '*ProjectDeploymentFacadeChatWorkflowTest*' > /tmp/t2.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`, compilation error on `getWorkspaceChatWorkflows`.

- [ ] **Step 3: Implement**

Move the controller's listing body into `ProjectDeploymentFacadeImpl.getWorkspaceChatWorkflows`; `environmentId == null` passes `null` as the environment to `projectDeploymentService.getProjectDeployments(false, environment, null, null, workspaceId)`. Replace the controller's private `hasHostedChatTrigger` with `HostedChatTriggers.hasHostedChatTrigger`. The controller keeps the query mapping and delegates. Add the two schema fields.

- [ ] **Step 4: Run the module checks**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:check :server:libs:automation:automation-configuration:automation-configuration-api:check :server:libs:automation:automation-configuration:automation-configuration-service:check :server:libs:automation:automation-configuration:automation-configuration-graphql:check > /tmp/t2.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t2.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-configuration server/libs/automation/automation-configuration
git commit -m "732 List chat workflows in the project deployment facade"
```

---

### Task 3: Workflow chat creation, bulk archive and cancel

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/chat/AiHubChatService.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/chat/AiHubChatServiceImpl.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/chat/repository/AiHubChatRepository.java` (SOURCE's `findByWorkspaceIdAndUserIdAndEnvironmentAndKindInAndStatus` query)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/java/com/bytechef/ee/ai/hub/web/graphql/AiHubChatGraphQlController.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/ai-hub-chat.graphqls` (SOURCE's `createWorkflowChatAiHubChat`, `bulkArchiveWorkflowChatAiHubChats`, `cancelWorkflowChatTurn` mutations)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-graphql/build.gradle.kts` (add `automation-configuration-api` if missing)
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/chat/AiHubChatServiceTest.java`, `server/ee/libs/ai/ai-hub/ai-hub-graphql/src/test/java/com/bytechef/ee/ai/hub/web/graphql/AiHubChatGraphQlControllerTest.java`

**Interfaces:**
- Consumes: Task 1's kind and columns; Task 2's `ProjectDeploymentFacade.isWorkspaceChatWorkflow`.
- Produces: `AiHubChat createWorkflowChat(long workspaceId, long userId, int environment, String workflowExecutionId, long projectDeploymentId, @Nullable String title)` (always a new row, random UUID thread id, `kind = WORKFLOW_CHAT`, title set when non-blank); `int bulkArchiveWorkflowChatAiHubChats(long workspaceId, long userId, int environment)`; `boolean cancelWorkflowChatTurn(long chatId, long requesterWorkspaceId, long requesterUserId)` which returns `cancelAiHubRun(chatId, requesterWorkspaceId, requesterUserId, null)`.
- GraphQL: `createWorkflowChatAiHubChat(workspaceId: ID!, environment: Int!, workflowExecutionId: String!, projectDeploymentId: ID!, title: String): AiHubChat!`, `bulkArchiveWorkflowChatAiHubChats(workspaceId: ID!, environment: Int!): Int!`, `cancelWorkflowChatTurn(workspaceId: ID!, id: ID!): Boolean!`.

- [ ] **Step 1: Write the failing tests**

Service:
```java
@Test
void testCreateWorkflowChatStampsKindAndBinding() {
    AiHubChat chat = service.createWorkflowChat(WORKSPACE_ID, USER_ID, 0, "exec-1", 42L, "Acme — Support");
    assertThat(chat.getKind()).isEqualTo(AiHubChatKind.WORKFLOW_CHAT);
    assertThat(chat.getWorkflowExecutionId()).isEqualTo("exec-1");
    assertThat(chat.getProjectDeploymentId()).isEqualTo(42L);
    assertThat(chat.getTitle()).isEqualTo("Acme — Support");
    assertThat(chat.getThreadId()).isNotBlank();
}

@Test
void testCancelWorkflowChatTurnGoesThroughTheRunRegistry() {
    // chat participable, registry.cancel(threadId, null) stubbed true
    assertThat(service.cancelWorkflowChatTurn(CHAT_ID, WORKSPACE_ID, USER_ID)).isTrue();
    verify(inFlightRunRegistry).cancel(THREAD_ID, null);
}

@Test
void testBulkArchiveArchivesOnlyActiveWorkflowChats() {
    // repository returns two WORKFLOW_CHAT rows; expect two updateStatusUnlessDeleted(ARCHIVED) calls and 2 returned
}
```

Controller:
```java
@Test
void testCreateWorkflowChatRefusesAnIdOutsideTheWorkspaceListing() {
    when(projectDeploymentFacade.isWorkspaceChatWorkflow(1L, "exec-x")).thenReturn(false);
    assertThatThrownBy(() -> controller.createWorkflowChatAiHubChat(1L, 0, "exec-x", 42L, null))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(chatService);
}

@Test
void testCreateWorkflowChatRequiresTheWorkflowViewScope() {
    when(permissionService.hasWorkspaceRole(1L, "VIEWER")).thenReturn(true);
    when(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).thenReturn(false);
    assertThatThrownBy(() -> controller.createWorkflowChatAiHubChat(1L, 0, "exec-1", 42L, null))
        .isInstanceOf(AccessDeniedException.class);
}
```

Use whatever exception type `AiHubWorkspaceAccess.verifyViewer` throws today for the second test's expectation, so both refusals surface the same way.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubChatServiceTest*' :server:ee:libs:ai:ai-hub:ai-hub-graphql:test --tests '*AiHubChatGraphQlControllerTest*' > /tmp/t3.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`, compilation errors on the new methods.

- [ ] **Step 3: Implement**

Controller order for `createWorkflowChatAiHubChat`: `AiHubWorkspaceAccess.verifyViewer`, then `permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")`, then `projectDeploymentFacade.isWorkspaceChatWorkflow(workspaceId, workflowExecutionId)`, then the service call. `bulkArchive` and `cancelWorkflowChatTurn` use `verifyViewer` like their neighbours. Service `delete` is untouched here (Task 6 adds the resume-slot clear).

- [ ] **Step 4: Run the module checks**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:check :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:ee:libs:ai:ai-hub:ai-hub-graphql:check > /tmp/t3.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t3.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub
git commit -m "732 Create, bulk archive and cancel workflow chats"
```

---

### Task 4: Registries, guard, metrics, caches and module dependencies

**Files:**
- Create (SOURCE, same paths, under `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/`): `agent/WorkflowChatJobRegistry.java`, `agent/WebhookResumeRegistry.java`, `agent/WorkflowChatGuard.java`, `metric/WorkflowChatMetrics.java`
- Create (SOURCE tests, same paths): `WorkflowChatJobRegistryTest`, `WebhookResumeRegistryTest`, `WorkflowChatGuardTest`
- Modify: `server/libs/config/cache-config/src/main/java/com/bytechef/cache/config/CacheConfiguration.java` (SOURCE's three constants and registrations, Redis and Caffeine: `WebhookResumeRegistry.resumeUrls` 30 min, `WorkflowChatJobRegistry.jobIds` 30 min, `WorkflowChatGuard.lastTurn` 5 min; no `inFlight`)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/build.gradle.kts` (add `platform-webhook-api`, `atlas-execution-api`, `atlas-configuration-api`, `platform-component-api`, `platform-configuration-api`, `:sdks:backend:java:component-api`, `platform-ai-api`, `io.micrometer:micrometer-core`; match the existing `implementation(project(...))` style)

**Interfaces:**
- Produces: `WorkflowChatJobRegistry(CacheManager)` with `register(long chatId, long jobId)`, `clear(long chatId)`, `@Nullable Long get(long chatId)`; `WebhookResumeRegistry(CacheManager)` with `register(long chatId, String resumeUrl)`, `@Nullable String consume(long chatId)`, plus new `clear(long chatId)`; `WorkflowChatGuard(CacheManager)` with `AdmissionResult tryAdmit(long chatId)` where `AdmissionResult` is `Admit` or `RateLimited(Duration cooldownRemaining)` only, `RATE_LIMIT_MIN_INTERVAL_MS = 2_000L`, `LAST_TURN_CACHE_NAME` only; `WorkflowChatMetrics(ObjectProvider<MeterRegistry>)` as in SOURCE.

- [ ] **Step 1: Port the three tests; adjust `WorkflowChatGuardTest`**

Drop the concurrency-blocked and `release` cases. Add:
```java
@Test
void testRegistryHasNoInFlightCache() {
    assertThat(WorkflowChatGuard.class.getDeclaredFields())
        .extracting(Field::getName).doesNotContain("IN_FLIGHT_CACHE_NAME");
}
```
Add to `WebhookResumeRegistryTest`:
```java
@Test
void testClearDropsThePendingResumeUrl() {
    registry.register(5L, "http://resume");
    registry.clear(5L);
    assertThat(registry.consume(5L)).isNull();
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*WorkflowChat*' --tests '*WebhookResumeRegistry*' > /tmp/t4.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`, compilation errors.

- [ ] **Step 3: Port the classes, trim the guard, register the caches, add the dependencies**

Each class is `@Component` + `@ConditionalOnAiHub`.

- [ ] **Step 4: Run the module checks**

Run: `./gradlew :server:libs:config:cache-config:check :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/t4.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t4.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/cache-config server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "732 Add the workflow chat registries, rate limit and metrics"
```

---

### Task 5: `AgUiStreamBridge`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AgUiStreamBridge.java` (SOURCE, same path)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/AgUiStreamBridgeTest.java` (SOURCE, same path)

**Interfaces:**
- Consumes: Task 4's registries and guard.
- Produces: `AgUiStreamBridge(AgentSubscriber subscriber, String messageId, String threadId, String runId, long chatId, WebhookResumeRegistry resumeRegistry, WorkflowChatJobRegistry jobRegistry, @Nullable JobFacade jobFacade, LongConsumer onJobId, Runnable onFinalize)` implementing `SseStreamBridge`. Compared with SOURCE: the `WorkflowChatGuard` parameter and its `release` calls are gone (no lock to release); `onJobId` is called once with the job id captured from the `{event: start, payload: {jobId}}` event right after `jobRegistry.register`; the `APPROVAL_REQUEST` branch is removed; `onComplete` / `onError` clear the job registry and run `onFinalize`, as in SOURCE. `String accumulatedAssistantText()` stays.

- [ ] **Step 1: Port the test and adjust**

Drop the approval-request and guard-release cases. Add:
```java
@Test
void testStartEventReportsTheJobId() {
    AtomicLong seen = new AtomicLong();
    AgUiStreamBridge bridge = newBridge(seen::set);
    bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", "77")));
    assertThat(seen.get()).isEqualTo(77L);
    verify(jobRegistry).register(CHAT_ID, 77L);
}

@Test
void testAskUserQuestionWithoutResumeUrlEndsTheRunWithAnError() {
    bridge.onEvent(Map.of("ask_user_question", Map.of("question", "Which account?")));
    // a RunErrorEvent reached the subscriber, the registry has no resume url, onFinalize ran
}
```
If SOURCE's bridge only logs a missing `resumeUrl` and keeps streaming, make it emit the run error as the second test says; this pins Review Focus 3.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AgUiStreamBridgeTest*' > /tmp/t5.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`.

- [ ] **Step 3: Port the bridge with the interface changes above**

- [ ] **Step 4: Run the module check**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/t5.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t5.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "732 Bridge workflow SSE events onto the AG-UI stream"
```

---

### Task 6: `WebhookBridgeAgent`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/WebhookBridgeAgent.java` (SOURCE, same path)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/WorkflowChatTurnStopper.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/chat/AiHubChatServiceImpl.java` (`delete` also calls `webhookResumeRegistry.clear(chatId)`; inject `ObjectProvider<WebhookResumeRegistry>`)
- Create (SOURCE, same paths): `WebhookBridgeAgentTest`, `WebhookBridgeAgentResumeTest`, `WebhookBridgeAgentTenantContextTest`; new `WorkflowChatTurnStopperTest`

**Interfaces:**
- Consumes: Task 5's bridge; Task 4's registries, guard, metrics; `WebhookWorkflowExecutor` (`stream(WorkflowExecutionId, WebhookRequest, SseStreamBridge): CompletableFuture<Void>`, `executeSync(WorkflowExecutionId, WebhookRequest): @Nullable Object` on this branch, `hasStreamingTask`, `isWorkflowDisabled`, `getWebhookTriggerFlags`); `InFlightAiHubRunRegistry.stopRequested(String threadId): Mono<Void>`; `AiHubChatService.findByThreadId`.
- Produces: `WebhookBridgeAgent(WebhookWorkflowExecutor, AiHubChatService, WebhookResumeRegistry, JsonMapper, WorkflowChatMetrics, WorkflowChatJobRegistry, AiHubSessionMemory, WorkflowChatGuard, @Nullable JobFacade, InFlightAiHubRunRegistry) extends LocalAgent`, with `protected void run(RunAgentInput, AgentSubscriber)` as in SOURCE.
- `WorkflowChatTurnStopper(long chatId, WorkflowChatJobRegistry jobRegistry, @Nullable JobFacade jobFacade)` with `void onStopRequested()` and `void onJobId(long jobId)`: `JobFacade.stopJob` runs exactly once, as soon as both have happened in either order; `jobRegistry.clear(chatId)` after the stop; both methods idempotent; no-op without a `JobFacade`.
- Request body (SOURCE's `buildWebhookRequest`): `message`, `chatId` = thread id, `conversationId` = `"workflow-chat:" + threadId`, `attachments` = empty list; the same `message`, `chatId`, `conversationId` in `parameters`.

- [ ] **Step 1: Port the three tests; write `WorkflowChatTurnStopperTest`; add the new cases**

Drop every attachment, `hasSuspendingTask` and approval case. Whether or not SOURCE already covers them, `WebhookBridgeAgentTest` must contain these three (Review Focus 1, 4, 5):

```java
@Test
void testDisabledWorkflowEndsWithARunErrorAndFreesTheChat() {
    when(executor.isWorkflowDisabled(any())).thenReturn(true);
    // run; a RunErrorEvent mentioning "disabled" reached the subscriber; onRunFinalized was called; no job registered
}

@Test
void testEmptyOutputCompletesWithTheFallbackReply() {
    when(executor.executeSync(any(), any())).thenReturn(Map.of());
    // run; the assistant text is SOURCE's fallback sentence; the hub session got the user turn; no exception
}

@Test
void testUnresolvableWorkflowExecutionIdEndsWithARunError() {
    // chat bound to "garbage"; WorkflowExecutionId.parse throws; a RunErrorEvent reached the subscriber, nothing else
}
```

Then the new cases:

```java
@Test
void testStopBeforeJobIdStopsTheJobWhenItArrives() {
    stopper.onStopRequested();
    verifyNoInteractions(jobFacade);
    stopper.onJobId(77L);
    verify(jobFacade).stopJob(77L);
    verify(jobRegistry).clear(CHAT_ID);
}

@Test
void testStopAfterJobIdStopsAtOnceAndOnlyOnce() {
    stopper.onJobId(77L);
    stopper.onStopRequested();
    stopper.onStopRequested();
    verify(jobFacade, times(1)).stopJob(77L);
}

@Test
void testRequestBodyCarriesConversationIdAndChatId() {
    // run one turn on a chat with thread id "t-1"; capture the WebhookRequest passed to the executor
    Map<String, ?> content = (Map<String, ?>) captured.getBody().getContent();
    assertThat(content).containsEntry("conversationId", "workflow-chat:t-1").containsEntry("chatId", "t-1");
    assertThat(captured.getParameters()).containsEntry("conversationId", List.of("workflow-chat:t-1"));
}

@Test
void testStopSignalReachesTheStopper() {
    // registry.stopRequested("t-1") returns a Sinks.one() Mono; emit it after the start event; expect stopJob
}

@Test
void testSyncPathCallsExecuteSyncDirectly() {
    when(executor.executeSync(any(), any())).thenReturn(Map.of("message", "done"));
    // run; expect the assistant text "done" and no CompletionException handling needed
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*WebhookBridgeAgent*' --tests '*WorkflowChatTurnStopper*' > /tmp/t6.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`.

- [ ] **Step 3: Port the agent**

In `run()`, after the chat is resolved and admitted: create a `WorkflowChatTurnStopper`, subscribe `inFlightRunRegistry.stopRequested(threadId)` to `stopper::onStopRequested`, hand `stopper::onJobId` to the bridge's `onJobId`, and dispose the subscription in `onFinalize`. The sync path calls `executeSync` directly and treats a `RuntimeException` like SOURCE treats an unwrapped `CompletionException`. `WorkflowChatGuard.tryAdmit` only rate-limits; a `RateLimited` result emits SOURCE's friendly run error.

- [ ] **Step 4: Run the module check**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/t6.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t6.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "732 Run workflow chat turns through the webhook bridge agent"
```

---

### Task 7: Routing agent, bean wiring and per-turn checks

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/agent/AiHubRoutingAgent.java` (SOURCE, same path, without the attachment upload block and `AssetFileSystemFacade`)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` (`@Bean(defaultCandidate = false)` on `aiHubAskSpringAIAgent` and `aiHubBuildSpringAIAgent`; new beans `webhookBridgeAgent` (`@ConditionalOnBean(WebhookWorkflowExecutor.class)`), `aiHubAskRoutingAgent`, `aiHubBuildRoutingAgent` as in SOURCE minus the asset facade)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-rest/src/main/java/com/bytechef/ee/ai/hub/web/rest/AiHubApiController.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-rest/build.gradle.kts` (add `automation-configuration-api` only if the controller needs it; it does not: the scope check uses `PermissionService`)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/AiHubRoutingAgentTest.java` (SOURCE, same path)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/agent/AiHubTurnToolDispatchTest.java` (its `List<LocalAgent>` holds the two routing agents)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-rest/src/test/java/com/bytechef/ee/ai/hub/web/rest/AiHubApiControllerTest.java`

**Interfaces:**
- Consumes: Task 6's agent; `AiHubChatService.findByThreadId`; `PermissionService.hasWorkspaceScope`.
- Produces: `AiHubRoutingAgent(String agentId, AiHubSpringAIAgent llmAgent, @Nullable WebhookBridgeAgent webhookBridgeAgent, AiHubChatService chatService)`; `runAgent` routes `WORKFLOW_CHAT` to the bridge (a run error when the bridge bean is absent) and everything else to the LLM agent, as in SOURCE.
- Controller `chat(...)` order: `rejectConnectedUser`; user id; `workspaceId` read from state (400 when missing); `hasWorkspaceRole(workspaceId, "VIEWER")` or 403; thread id, environment id; `resolveChat`; then if `chat.getKind().isWebhookBridged()`: `hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW")` or 403; else resolve the mode and require `EDITOR` for `BUILD` (today's rule). `resolveAgent(mode)` still picks `ai_hub_ask` / `ai_hub_build`, which are now the routers.

- [ ] **Step 1: Port `AiHubRoutingAgentTest`; add controller tests**

```java
@Test
void testWorkflowChatTurnRequiresTheWorkflowViewScope() {
    // chat for the thread has kind WORKFLOW_CHAT; hasWorkspaceRole VIEWER true; hasWorkspaceScope WORKFLOW_VIEW false
    assertThatThrownBy(() -> controller.chat(parameters))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode").isEqualTo(HttpStatus.FORBIDDEN);
    verify(inFlightRunRegistry, never()).tryStart(any(), any());
}

@Test
void testWorkflowChatTurnSkipsTheBuildEditorRule() {
    // mode BUILD in state, kind WORKFLOW_CHAT, VIEWER true, EDITOR false, WORKFLOW_VIEW true
    assertThat(controller.chat(parameters)).isNotNull();
}

@Test
void testStandardChatBuildTurnStillRequiresEditor() {
    // mode BUILD, kind STANDARD, VIEWER true, EDITOR false -> 403
}
```
Add to `AiHubTurnToolDispatchTest` an assertion that the agent map built from the routers has exactly the ids `ai_hub_ask` and `ai_hub_build`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubRoutingAgentTest*' --tests '*AiHubTurnToolDispatchTest*' :server:ee:libs:ai:ai-hub:ai-hub-rest:test --tests '*AiHubApiControllerTest*' > /tmp/t7.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`.

- [ ] **Step 3: Implement**

Keep `AiHubPinnedToolCallbacks` and every other `@Qualifier("aiHubAskSpringAIAgent")` injection working: `defaultCandidate = false` only removes the LLM agents from the unqualified `List<LocalAgent>`.

- [ ] **Step 4: Run the module checks and compile the app**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:ee:libs:ai:ai-hub:ai-hub-rest:check > /tmp/t7.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t7.log`.
Expected: `EXIT=0`, no FAILED lines.
Then run `./gradlew :server:apps:server-app:compileJava > /tmp/t7b.log 2>&1` and `echo "EXIT=$?"`. Expected `EXIT=0`. (The controller tests pin the duplicate-id hazard; a full boot happens in Task 12.)

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub
git commit -m "732 Route workflow chat turns to the webhook bridge"
```

---

### Task 8: Hub tools, prompt and docs

**Files:**
- Create (SOURCE, same paths under `.../ee/ai/hub/tool/`): `ListChatWorkflowsToolCallback.java`, `CreateWorkflowChatToolCallback.java`, `OpenWorkflowChatTabToolCallback.java`
- Create (SOURCE tests): `CreateWorkflowChatToolCallbackTest`, `OpenWorkflowChatTabToolCallbackTest`; new `ListChatWorkflowsToolCallbackTest`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` (ASK: `OpenWorkflowChatTabToolCallback` after `openWorkflowTab`; BUILD: `OpenWorkflowChatTabToolCallback`, `ListChatWorkflowsToolCallback`, `CreateWorkflowChatToolCallback` in that order after `openWorkflowTab`)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt` (new section "Workflow chats" after "Completing a built workflow": `listChatWorkflows` discovers chat workflows; `createWorkflowChat({workflowExecutionTriggerId, projectDeploymentId, title?})` returns `{threadId, chatId, title}`; then `openWorkflowChatTab({threadId})` switches the user to it; never construct or edit workflows here)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt` (one line: `openWorkflowChatTab({threadId})` opens an existing workflow chat the user names)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/AiHubConfigurationToolSetTest.java`
- Modify: `docs/agents/ai-hub.md` ("Tool sets and the three doors": the three tools; new paragraph "Workflow chats" naming the router, the bridge, the `WORKFLOW_VIEW` rule, and the conversation id)

**Interfaces:**
- Consumes: Task 2's facade; Task 3's `createWorkflowChat`; `AiHubToolInvocationContext(workspaceId, userId, sourceOrdinal, lastUserPrompt, environmentId, threadId, ownerUserId)`.
- Produces: `ListChatWorkflowsToolCallback(ProjectDeploymentFacade)` named `listChatWorkflows`, input `{projectId?}`, result items `{projectDeploymentId, projectId, workflowExecutionTriggerId, workflowLabel}` from `getWorkspaceChatWorkflows(workspaceId, environmentId)` of the invocation context (no `streaming` flag: it needed `WorkflowFacade.hasSseStreamResponse` per row and nothing consumes it here); `CreateWorkflowChatToolCallback(AiHubChatService, ProjectDeploymentFacade)` named `createWorkflowChat`, SOURCE's input schema, refusing with a tool error when `isWorkspaceChatWorkflow` is false; `OpenWorkflowChatTabToolCallback()` as in SOURCE.

- [ ] **Step 1: Port the two tests, write the listing test, extend the tool-set test**

```java
@Test
void testCreateWorkflowChatRefusesAnUnknownWorkflow() {
    when(projectDeploymentFacade.isWorkspaceChatWorkflow(1L, "exec-x")).thenReturn(false);
    String result = callback.call("{\"workflowExecutionTriggerId\":\"exec-x\",\"projectDeploymentId\":\"42\"}", context);
    assertThat(result).contains("not a chat workflow");
    verifyNoInteractions(chatService);
}

@Test
void testListChatWorkflowsUsesTheTurnEnvironment() {
    callback.call("{}", contextWithEnvironment(1L));
    verify(projectDeploymentFacade).getWorkspaceChatWorkflows(WORKSPACE_ID, 1L);
}
```
Tool-set test: ASK `contains("openWorkflowChatTab")` and `doesNotContain("listChatWorkflows", "createWorkflowChat")`; BUILD `contains("openWorkflowChatTab", "listChatWorkflows", "createWorkflowChat")`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*ToolCallbackTest*' --tests '*AiHubConfigurationToolSetTest*' > /tmp/t8.log 2>&1` then `echo "EXIT=$?"`.
Expected: `EXIT=1`.

- [ ] **Step 3: Port the tools, pin them, write the prompt sections and the docs paragraph**

- [ ] **Step 4: Run the module check**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/t8.log 2>&1`, then `echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/t8.log`.
Expected: `EXIT=0`, no FAILED lines.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-hub docs/agents/ai-hub.md
git commit -m "732 Let the hub agent list, create and open workflow chats"
```

---

### Task 9: Client API, GraphQL documents, launchers and live label

**Files:**
- Modify: `client/src/graphql/ai/aihub/chat/aiHubChats.graphql`, `createAiHubChat.graphql`, `updateAiHubChat.graphql`, `generateAiHubChatTitle.graphql` (select `kind`, `workflowExecutionId`, `projectDeploymentId`)
- Create: `client/src/graphql/ai/aihub/chat/createWorkflowChatAiHubChat.graphql` (SOURCE minus `aiAgentId`, `visibility`, `participation`, `ownerUserId`, `ownerName`, `isOwner`)
- Modify: `client/src/graphql/automation/configuration/workspaceChatWorkflows.graphql` (add `projectWorkflowId`, `workflowId`)
- Regenerate: `client/src/shared/middleware/graphql.ts` (`cd client && npx graphql-codegen`), committed separately
- Modify: `client/src/ee/pages/automation/ai-hub/chats/api/chats.api.ts` (`ChatKindType = 'STANDARD' | 'WORKFLOW_CHAT'`, `isWebhookBridgedChat(kind)`, `kind`, `workflowExecutionId: string | null`, `projectDeploymentId: string | null` on `AiHubChatI`, mapped in `toChat`)
- Create: `client/src/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers.ts` (SOURCE minus agent chats)
- Create: `client/src/ee/pages/automation/ai-hub/chats/hooks/useLiveWorkflowLabel.ts` (SOURCE, same path)
- Modify: `client/src/ee/pages/automation/ai-hub/AiHubHomePanel.tsx`, `AiHubPanel.tsx` (SOURCE's wiring of `workflowChats` / `onSelectWorkflowChat` into `ModelPicker`)
- Test: `client/src/ee/pages/automation/ai-hub/chats/api/tests/chats.api.test.ts`, new `client/src/ee/pages/automation/ai-hub/hooks/tests/useAiHubChatLaunchers.test.tsx`, new `client/src/ee/pages/automation/ai-hub/chats/hooks/tests/useLiveWorkflowLabel.test.tsx`, `client/src/ee/pages/automation/ai-hub/tests/AiHubPanel.test.tsx`

**Interfaces:**
- Consumes: Task 3's mutation; Task 2's listing fields; `ModelPickerWorkflowChatI` and the `workflowChats` / `onSelectWorkflowChat(workflowExecutionId, projectDeploymentId, label)` props already on `client/src/shared/components/ai/model-picker/ModelPicker.tsx`.
- Produces: `useAiHubChatLaunchers(): {onSelectWorkflowChat, workflowChats}`; the label is `` `${projectName} — ${workflowLabel}` ``; selecting one calls `createWorkflowChatAiHubChat` with the label as `title` and navigates to `/automation/ai-hub/chats/${id}`; `useLiveWorkflowLabel(chat): string | null` (null when the chat is not bridged or its workflow is not in the listing).

- [ ] **Step 1: Write the failing tests**

```ts
it('maps the kind and workflow binding', () => {
    expect(toChat({...graphQlChat, kind: 'WORKFLOW_CHAT', projectDeploymentId: '42', workflowExecutionId: 'exec-1'}))
        .toMatchObject({kind: 'WORKFLOW_CHAT', projectDeploymentId: '42', workflowExecutionId: 'exec-1'});
});

it('treats only WORKFLOW_CHAT as webhook bridged', () => {
    expect(isWebhookBridgedChat('WORKFLOW_CHAT')).toBe(true);
    expect(isWebhookBridgedChat('STANDARD')).toBe(false);
    expect(isWebhookBridgedChat(undefined)).toBe(false);
});

it('labels workflow chats as "project — workflow" and creates a chat on select', async () => {
    // workspaceChatWorkflows mocked with one row; select it; expect the mutation called with title 'Acme — Support'
    // and navigate called with '/automation/ai-hub/chats/9'
});

it('returns null for a workflow chat whose workflow is no longer listed', () => { /* Review Focus 5 */ });
```

- [ ] **Step 2: Run them to verify they fail**

Run: `cd client && npx vitest run src/ee/pages/automation/ai-hub/chats/api src/ee/pages/automation/ai-hub/hooks src/ee/pages/automation/ai-hub/chats/hooks/tests/useLiveWorkflowLabel.test.tsx`
Expected: FAIL (missing exports).

- [ ] **Step 3: Implement**

Commit the `.graphql` documents, then codegen as its own commit, then the TypeScript.

- [ ] **Step 4: Run the client check**

Run: `cd client && npm run check`. Expected: PASS.

- [ ] **Step 5: Commit (three commits)**

```bash
git add client/src/graphql && git commit -m "732 client - Add the workflow chat GraphQL documents"
git add client/src/shared/middleware/graphql.ts && git commit -m "732 client - Regenerate GraphQL types for workflow chats"
git add client/src && git commit -m "732 client - Launch workflow chats from the provider picker"
```

---

### Task 10: Workflow chat behaviour in the chat view

**Files (all under `client/src/ee/pages/automation/ai-hub/`):**
- Modify: `composer/AiHubChatComposer.tsx` (SOURCE's `isWorkflowChat` gating: no model picker, no `@` mentions, no attachments, workflow badge in the header; `handleCancelTurn` sends `cancelAiHubRun` for every kind, so no `useCancelWorkflowChatTurnMutation`)
- Modify: `messages/AiHubThread.tsx` (`showSuggestions` false for workflow chats), `AiHub.tsx` (`useRecordReferencedArtifacts(..., !isWorkflowChat)`), `artifacts/useAiHubArtifactsCard.ts`, `chats/hooks/useRecordReferencedArtifacts.ts` (SOURCE's enabled flag)
- Modify: `chats/AiHubChatsSidebar.tsx` (workflow icon for bridged chats; tooltip shows `useLiveWorkflowLabel` when non-null)
- Modify: `runtime-providers/AiHubRuntimeProvider.tsx` (SOURCE's `ask-workflow-question` custom event handling and the `openWorkflowChatTab` tool-result chat switch; skip automatic title generation for bridged chats; nothing from `runChatWorkflow` / `workflowStreamHandler`)
- Test: `composer/tests/AiHubChatComposer.test.tsx`, `chats/tests/AiHubChatsSidebar.test.tsx`, `runtime-providers/tests/AiHubRuntimeProvider.test.tsx`, `chats/hooks/tests/useRecordReferencedArtifacts.test.tsx` (SOURCE's workflow-chat cases, trimmed to this branch)

**Interfaces:**
- Consumes: Task 9's `isWebhookBridgedChat`, `useLiveWorkflowLabel`; the existing `useCancelAiHubRunMutation` and `cancelChatRunIfStreaming` (unchanged: already one call per Stop).

- [ ] **Step 1: Write the failing tests**

```ts
it('hides the model picker, mentions and attachments in a workflow chat and shows the badge', () => { ... });
it('sends one cancelAiHubRun on Stop for a workflow chat', () => {
    // expect cancelAiHubRun called once with {id, runId, workspaceId}; no other mutation
});
it('renders a workflow icon and the live label tooltip for a workflow chat', () => { ... });
it('routes ask-workflow-question into the question card and resumes on answer', () => { ... });
it('switches to the chat named by an openWorkflowChatTab result', () => { ... });
it('does not generate a title for a workflow chat', () => { ... });
```

- [ ] **Step 2: Run them to verify they fail**

Run: `cd client && npx vitest run src/ee/pages/automation/ai-hub`
Expected: the new cases FAIL.

- [ ] **Step 3: Port the hunks**

Use `git diff 0_p_ai_hub 0_732 -- <file>` per file and take only the hunks the Files list names.

- [ ] **Step 4: Run the client check**

Run: `cd client && npm run check`. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src
git commit -m "732 client - Show workflow chats as bridged chats in the hub"
```

---

### Task 11: Deterministic Playwright spec

**Files:**
- Create: `client/test/playwright/tests/ai-hub/aiHubWorkflowChats.spec.ts` (mock pattern of `aiHubChats.spec.ts` and `client/test/playwright/fixtures/aiHubChats.ts`)
- Modify: `client/test/playwright/fixtures/aiHubChats.ts` (mocks for `workspaceChatWorkflows`, `createWorkflowChatAiHubChat`, chats with `kind`)
- Modify: `docs/agents/ai-hub.md` ("Playwright specs" list)

- [ ] **Step 1: Write the spec**

Cases: the picker shows a "Workflow chats" cascade with the mocked label and its search filter narrows it; selecting it calls `createWorkflowChatAiHubChat` and navigates to the new chat; in that chat the model picker, `@` mention and attachment button are absent and the workflow badge is present; a streaming turn's Stop sends exactly one `cancelAiHubRun`; the sidebar row has the workflow icon.

- [ ] **Step 2: Run it**

Run: `cd client && npx playwright test tests/ai-hub/aiHubWorkflowChats.spec.ts`
Expected: all cases pass.

- [ ] **Step 3: Commit**

```bash
git add client/test/playwright docs/agents/ai-hub.md
git commit -m "732 client - Cover workflow chats with a Playwright spec"
```

---

### Task 12: Live Playwright spec (controller runs it)

**Files:**
- Create: `client/test/playwright/tests/ai-hub/fixtures/workflow-chat-memory.json` (a project workflow: `chat/v1/newChatRequest` trigger in hosted mode; an `aiAgent` task on an Anthropic model with the built-in Chat Memory (version 2) cluster element whose `conversationId` is `${trigger_1.conversationId}` and the ask-user-question utility tool; system prompt: "Remember the user's name when told. When asked to plan a trip, first ask which city with the ask-user-question tool, then answer in one sentence.")
- Modify: `client/test/playwright/tests/ai-hub/aiHubLive.spec.ts` (new describe "Workflow chats", after the deployments describe, using its API helpers to import the fixture into a project, deploy and enable it in DEVELOPMENT, and delete it afterwards)

- [ ] **Step 1: Write the describe**

Steps: open the hub, pick the workflow from the "Workflow chats" cascade, send "My name is Mira." and expect a reply; send "What is my name?" and expect "Mira" in the reply (the workflow's own memory across turns); send "Plan my trip." and expect the question card, answer "Lisbon" and expect a reply mentioning Lisbon (resume); send a long request and click Stop, expect the turn to end and a new message to be accepted.

- [ ] **Step 2: Run it with the live keys**

The controller runs this with the model key and the e2e server setup from phase 1 (`branch-e2e-run-setup`), never a subagent. Expected: the describe passes.

- [ ] **Step 3: Commit**

```bash
git add client/test/playwright
git commit -m "732 client - Add the live workflow chat spec"
```

---

### Task 13: Fast-forward `0_p_ai_hub`

- [ ] **Step 1: Check the user's tree is clean**

Run in `/Volumes/Data/bytechef/bytechef-master`: `git status --short | wc -l`. Expected: `0`. If not, stop and ask.

- [ ] **Step 2: Fast-forward and remove the worktree**

```bash
git merge --ff-only 0_p_ai_hub_wf
git worktree remove /Volumes/Data/bytechef/bytechef-worktrees/ai-hub-wf
git branch -d 0_p_ai_hub_wf
```
Expected: `0_p_ai_hub` at the last commit of Task 12; no push.
