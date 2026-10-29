# AI Hub Port, Plan B: the Hub on `0_p_ai_hub` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port a minimal AI Hub from `0_732` onto `0_p_ai_hub`: persisted chats, an ASK/BUILD agent that manages projects, workflows and deployments through the same tools as the Copilot and the management MCP, workflow artifacts, the tool approval gate, and a two-pane page with a workflow-editor tab, made the default automation page.

**Architecture:** Four EE Gradle modules under `server/ee/libs/ai/ai-hub/` (`ai-hub-api`, `ai-hub-service`, `ai-hub-rest`, `ai-hub-graphql`) keep `0_732`'s file names and packages, trimmed to the spec's scope. The agent is a trimmed `AiHubSpringAIAgent` with a static tool list built from the beans Plan A put on `0_p_1570` (`ProjectTools`, `ProjectWorkflowLifecycleTools`, `DeploymentToolCallbacksFactory`, `InteractivePickerToolCallbacks`, `IntelligentToolCatalog`'s `buildWorkflow` / `importWorkflow`), every callback passing through one wrap site. The client is a trimmed copy of `0_732`'s `client/src/ee/pages/automation/ai-hub/` plus an `EmbeddableWorkflowEditor` for the one tab kind.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, AG-UI (`spring-ai:spring-ag-ui:*`), Spring Data JDBC, Liquibase, Testcontainers; React 19, TypeScript, zustand, `@ag-ui/client`, TanStack Query, GraphQL codegen, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-17-ai-hub-phase1-port-design.md` (D1-D12, Acceptance). Plan A (`2026-10-17-ai-hub-port-a-0p1570-prerequisites.md`) is merged into this branch. SOURCE = branch `0_732`, working tree `/Volumes/Data/bytechef/bytechef`; every "port X" step means: read SOURCE's file, copy it to the same relative path (rename only where stated), then trim exactly what the spec section named in the step says to drop. The spec's per-file trim lists are the requirement; this plan does not repeat them.

## Global Constraints

- Branch `0_p_ai_hub`, worktree `/Volumes/Data/bytechef/bytechef-worktrees/ai-hub`. Plan A's commits are already in it; never edit them.
- Commit ticket `732` for everything. Server: `732 <desc>`; client: `732 client - <desc>`. No `Co-Authored-By`, no attribution footer.
- Files under `server/ee/` carry the ByteChef Enterprise license header and `@version ee`. Client EE pages live under `client/src/ee/`.
- No explanatory inline comments; ported Javadoc may stay; ported `//` rationale comments are dropped. No em dashes in anything newly written.
- Enum ordinals persisted as INT: `AiHubChatArtifactKind` is ported whole and never reordered; `AiHubChatStatus`, `AiHubToolApproval.Status`, `.ToolKind`, `AiHubToolApprovalRule.Mode` keep `0_732`'s order.
- New entities have a nullable `workspace_id BIGINT`; no relation tables.
- Hub beans, controllers and the client nav/route are gated on `bytechef.ai.hub.enabled` AND `bytechef.ai.copilot.enabled` (spec D2); `info.ai.hub.enabled` reports the AND.
- The hub never holds `ProjectWorkflowTools`; it holds `ProjectWorkflowLifecycleTools` (spec anti-goal; pinned in Task 7).
- Roles exactly as spec D4: chat access owner or tenant `ADMIN` authority; ASK requires workspace `VIEWER`, BUILD requires `EDITOR`; rule list `VIEWER`, rule create/delete tenant `ADMIN`; approvals resolved by the chat owner.
- Client conventions: sort-keys ascending, interfaces end in `I`/`Props`, sorted import destructures, `useRef` names end in `Ref`, lucide icons with `Icon` suffix, `twMerge` not `cn`, descriptive names, hook order per `CLAUDE.md`.
- Verification: Gradle output to a log file, `$?` on its own line, then `grep '^> Task .* FAILED'`; never pipe Gradle. Client: `cd client && npm run format && npm run check`. Testcontainers needs `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` on this machine.

## Review Focus

1. Two browser tabs on the same chat send at once: the second `POST /internal/ai/chat/ai_hub` must get 409 (one turn per chat), not a second concurrent run on the same session. Pinned in Task 8.
2. An approval resolved after its 24h expiry, or twice (double click): the second resolve returns the existing decision and never executes the tool twice; an expired one is `EXPIRED` and does nothing. Pinned in Task 5.
3. A chat id from another user (or another workspace) in the URL or in `threadId`: every GraphQL read/mutation and the REST turn endpoint deny it (403 or not-found shape), never leak the title or transcript. Pinned in Tasks 3 and 8.
4. `buildWorkflow` asks a question mid-BUILD: the hub turn ends (awaitingAnswer), the card renders, the answer re-delegates, and no approval or second question is lost. Pinned in Tasks 7 and 13.
5. Hub enabled but copilot disabled: startup fails fast with a message naming both properties (no silently tool-less BUILD). Pinned in Task 1.

---

### Task 1: Module skeleton, wiring and the hub gate

**Files:**
- Create: `server/ee/libs/ai/ai-hub/{ai-hub-api,ai-hub-service,ai-hub-rest,ai-hub-graphql}/build.gradle.kts` (from SOURCE, dependencies cut to the IN list below)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-api/src/main/java/com/bytechef/ee/ai/hub/config/ConditionalOnAiHub.java`
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubJdbcRepositoryConfiguration.java` (SOURCE, `basePackages` = `com.bytechef.ee.ai.hub.chat.repository`, `com.bytechef.ee.ai.hub.approval.repository`; drop `CostEstimationProperties`) and its `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `settings.gradle.kts` (four `include("server:ee:libs:ai:ai-hub:...")` after line 588), `server/apps/server-app/build.gradle.kts` (four `implementation(project(...))` after line 273), `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (`bytechef.ai.hub.enabled: false` beside `copilot`), `application-dev.yml` (`bytechef.ai.hub.enabled: true`)
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubInfoContributor.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/ConditionalOnAiHubTest.java`

**Interfaces:**
- Produces: `@ConditionalOnAiHub` meta-annotation = `@ConditionalOnExpression("${bytechef.ai.hub.enabled:false} and ${bytechef.ai.copilot.enabled:false}")` (target TYPE, METHOD). Every hub `@Configuration`, controller and `@Service`/`@Component` in later tasks carries it (repositories excepted: they are harmless and needed by the IntTest).
- Produces: `AiHubInfoContributor` (unconditional `InfoContributor`) contributes `ai.hub.enabled = hubEnabled && copilotEnabled` read from `Environment`, so the client sees the effective flag (spec D9). It must merge into the existing `ai` info map without dropping `ai.copilot` / `ai.knowledgeBase`; if the `/info` shape makes merging awkward, add `info.ai.hub.enabled` to `application.yml` as a SpEL-free placeholder for `bytechef.ai.hub.enabled` instead and AND the copilot flag on the client, and say which you did.
- Dependencies, IN only (spec D1, survey "Build deps"): api: `platform-api`, `platform-configuration-api`, `automation-configuration-api`, `automation-ai-tool`, `spring-data-jdbc`, `spring-ai-commons`; service: `spring-ag-ui` (`servers:spring`, `packages:core`, `packages:server`, `integrations:spring-ai`, `utils:json`), `org-springaicommunity-spring-ai-session`, `chat-memory-builtin`, `caffeine`, `spring-webmvc`, `spring-boot-autoconfigure`, `spring-data-jdbc`, `spring-security-core`, `spring-tx`, `auto-service`, `commons-util`, `tenant-api`, `platform-user-api`, `ai-api`, CE `ai-copilot-api`/`-tool`/`-service`, EE `platform-ai-agent-api`, `components:ai:llm`; rest: hub api+service, `spring-ag-ui`, `tenant-api`, `platform-user-api`; graphql: hub api+service, `platform-configuration-graphql`, `user-api`. Test: `test-support`, `testcontainers-postgresql`.

- [ ] **Step 1: Write the failing test**

```java
class ConditionalOnAiHubTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(GatedConfiguration.class);

    @Test
    void testBeanLoadsOnlyWhenHubAndCopilotAreEnabled() {
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=true")
            .run(context -> assertThat(context).hasBean("gatedBean"));
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=true", "bytechef.ai.copilot.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean("gatedBean"));
        contextRunner.withPropertyValues("bytechef.ai.hub.enabled=false", "bytechef.ai.copilot.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean("gatedBean"));
    }

    @Test
    void testInfoContributorReportsTheEffectiveFlag() {
        // AiHubInfoContributor with hub=true, copilot=false contributes ai.hub.enabled=false
    }

    @Configuration
    @ConditionalOnAiHub
    static class GatedConfiguration {

        @Bean
        String gatedBean() {
            return "gated";
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*ConditionalOnAiHubTest' > /tmp/b1.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1` (project does not exist / class missing).

- [ ] **Step 3: Create the modules, the annotation, `AiHubInfoContributor`, the JDBC auto-config and the wiring**

Empty `src/main/java` package roots are fine; Gradle must configure all four modules.

- [ ] **Step 4: Run the test and `./gradlew :server:apps:server-app:compileJava`**

Expected: both `EXIT=0`.

- [ ] **Step 5: Commit**

`git commit -m "732 Add the AI Hub modules and gate them on the hub and copilot toggles"`

---

### Task 2: Database and entities

**Files:**
- Create (single collapsed init changesets, contexts as SOURCE): `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/config/liquibase/changelog/automation/aihub/20260423000001_ai_hub_chat_init.xml`, `20260423000002_ai_hub_chat_artifact_init.xml`, `20260902000001_ai_hub_tool_approval_rule_init.xml`, `20260902000002_ai_hub_tool_approval_init.xml` (keep SOURCE file names and changeSet ids so `0_732` and this line agree; columns exactly as spec D8)
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` (one `includeAll` for `classpath:config/liquibase/changelog/automation/aihub`, `contextFilter="mono or configuration or multitenant"`, after the last `automation/` include)
- Port to `ai-hub-api` (`chat/`, `approval/`, `util/`, `exception/`): `AiHubChat` (trimmed per D4: no kind/workflowExecutionId/projectDeploymentId/aiAgentId/visibility/participation), `AiHubChatStatus`, `AiHubChatArtifact`, `AiHubChatArtifactKind` (whole), `AiHubChatArtifactStatus`, `AiHubToolApproval`, `AiHubToolApprovalRule`, `util/EnumOrdinals`, `util/Mode`, `util/Source`, `util/AiHubStateKeys`, `exception/{ForbiddenException,ConflictException,NotFoundException,TitleGenerationFailedException}`
- Port to `ai-hub-service`: `chat/repository/AiHubChatRepository` (trimmed per spec D4 survey: drop shared/private-candidate/agent-chat/kind queries), `chat/repository/AiHubChatArtifactRepository`, `approval/repository/AiHubToolApprovalRepository`, `approval/repository/AiHubToolApprovalRuleRepository`
- Test: `ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/chat/AiHubChatRepositoryIntTest.java` (from SOURCE, trimmed), new `approval/AiHubToolApprovalRepositoryIntTest.java`, `util/EnumOrdinalStabilityTest.java` (from SOURCE, trimmed to the four ported enums), `src/test/resources/config/application-testint.yml` (from SOURCE)

**Interfaces:**
- Produces: the entities and repository method names as on SOURCE minus the dropped queries. `AiHubChatRepository.findByThreadId(String)`, `findAllByUserIdAndWorkspaceIdAndEnvironmentAndStatus...` (keep SOURCE's list-query name and paging signature), `AiHubToolApprovalRepository.findFirstByChatIdAndStatus(long, int)`, `findAllByChatId(long)`, `deleteByChatId(long)`.
- `Source` (hub util): keep SOURCE values and order (`WORKFLOW_EDITOR, CODE_EDITOR, CLUSTER_ELEMENT, FILES, AI_HUB`).

- [ ] **Step 1: Port the IntTests and the ordinal test**

`AiHubChatRepositoryIntTest`: save a chat, find by thread id, list by user/workspace/environment/status ordered by `updated_at desc`, archive changes the listing, deleting a chat cascades its artifacts (FK `ON DELETE CASCADE`). `AiHubToolApprovalRepositoryIntTest`: save a PENDING approval, `findFirstByChatIdAndStatus` finds it, `deleteByChatId` removes it, the rule table's unique index rejects a duplicate `(workspace_id, tool_kind, component_name, tool_name)`. `EnumOrdinalStabilityTest`: pins each ported enum's `name() -> ordinal()` map exactly as SOURCE (copy SOURCE's expected maps for these enums).

- [ ] **Step 2: Run to verify they fail**

Run: `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock ./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:testIntegration --tests '*RepositoryIntTest' :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*EnumOrdinalStabilityTest' > /tmp/b2.log 2>&1; echo "EXIT=$?"`
Expected: `EXIT=1`.

- [ ] **Step 3: Write the changelogs, port the entities and repositories, add the include**
- [ ] **Step 4: Run the same command**, expect `EXIT=0`.
- [ ] **Step 5: Commit**

`git add` the changelogs, `master.xml`, entities, repositories, tests. `git commit -m "732 Add the AI Hub chat, artifact and tool approval tables"`

---

### Task 3: Chat service, session memory, titles and access

**Files:**
- Port to `ai-hub-api`: `chat/AiHubChatService` (trimmed per D4), `chat/AiHubChatAccessPolicy`
- Port to `ai-hub-service`: `chat/AiHubChatServiceImpl` (trimmed per D4 and the survey's cut list), `memory/AiHubSessionMemory` (as SOURCE), `chat/TitleGenerationService` (plain `ChatClient`, no guardrails advisor), `chat/AiHubChatAccessPolicyImpl` (owner or tenant `ADMIN` authority, ~30 lines)
- Test: `chat/AiHubChatServiceTest.java`, `chat/TitleGenerationServiceTest.java`, `chat/AiHubChatAccessPolicyTest.java` (from SOURCE, trimmed)

**Interfaces:**
- Consumes: Task 2 entities and repositories.
- Produces: `AiHubChatService` methods `create(...)`, `list(...)`, `loadMessages(long chatId)`, `patch(long chatId, AiHubChatPatch patch)`, `delete(long chatId)` (deletes artifacts, approvals via `AiHubToolApprovalService.deleteByChat` from Task 5 through an `ObjectProvider` to avoid a cycle, and purges the session), `truncateMessagesFrom(long chatId, int fromIndex)`, `appendAssistantMessage(...)`, `findByThreadId(String)`, `getWorkspaceId(long)`, `getById(long)`, `getViewable(long)`, `getManageable(long)`; records `AiHubChatPatch`, `AiHubChatMessage` (no `authorUserId`). Signatures as SOURCE minus removed parameters. `AiHubChatAccessPolicy.canView(AiHubChat)`, `canParticipate`, `canManage`. `AiHubSessionMemory` bean name `aiHubSessionMemory`, `SESSION_USER_ID` constant as SOURCE.

- [ ] **Step 1: Port and trim the three tests; add**

```java
@Test
void testOtherUsersChatIsNotViewable() {
    // chat owned by user 1; current user 2 without ADMIN authority
    assertThatThrownBy(() -> chatService.getViewable(chatId)).isInstanceOf(ForbiddenException.class);
}

@Test
void testAdminCanViewAnyChat() { ... }

@Test
void testDeletingAChatDeletesItsApprovalsAndSession() { ... }
```

- [ ] **Step 2: Run to verify they fail** (`./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*AiHubChat*' --tests '*TitleGeneration*' > /tmp/b3.log 2>&1; echo "EXIT=$?"`, expect `EXIT=1`).
- [ ] **Step 3: Port and trim the four classes.**
- [ ] **Step 4: Run the tests**, expect `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Add the AI Hub chat service, session memory, titles and owner access"`

---

### Task 4: Workflow artifacts

**Files:**
- Port to `ai-hub-api`: `chat/AiHubChatArtifactService` (trimmed per D5)
- Port to `ai-hub-service`: `chat/AiHubChatArtifactServiceImpl` (drop the workspace listing), `tool/AiHubChatArtifactRecorder` (extends `ToolArtifactRecorder`), `chat/AiHubChatArtifactRecorderImpl` (no `MeterRegistry`), `chat/WorkflowArtifactRecorderImpl` (implements Plan A's `WorkflowArtifactRecorder`)
- Test: `chat/AiHubChatArtifactServiceTest.java`, `chat/AiHubChatArtifactRecorderTest.java`, `chat/WorkflowArtifactRecorderImplTest.java`, `chat/AiHubChatArtifactKindWireFormatTest.java` (SOURCE, trimmed)

**Interfaces:**
- Consumes: `com.bytechef.automation.ai.tool.WorkflowArtifactRecorder#recordWorkflowArtifact(@Nullable ToolContext, boolean created, String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName)`, `ToolArtifactRecorder` (both Plan A); `AgentToolInvocationContext` (conversationId = thread id).
- Produces: `AiHubChatArtifactService.recordWorkflowArtifact(String threadId, Long userId, AiHubChatArtifactKind kind, ...)`, `recordReference(...)`, `recordReferenceByThread(...)`, `deleteReference(long artifactId)` (only `WORKFLOW_REFERENCED`), `listByChat(long chatId)`, `countByChat(long chatId)`; `AiHubChatArtifactRecorder.recordWorkflowReference(String threadId, @Nullable Long userId, String workflowId, long projectId, @Nullable Long projectWorkflowId, String workflowName)`.

- [ ] **Step 1: Port the tests; add**

```java
@Test
void testWorkflowArtifactWithoutAThreadIsIgnored() {
    // ToolContext without a conversation id (management MCP): no row, no exception
}

@Test
void testDeleteReferenceRefusesNonReferenceKinds() {
    assertThatThrownBy(() -> service.deleteReference(createdArtifactId)).isInstanceOf(ForbiddenException.class);
}
```

- [ ] **Step 2: Run to verify they fail**, `EXIT=1`.
- [ ] **Step 3: Port the classes.**
- [ ] **Step 4: Run**, `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Record the workflows an AI Hub chat creates, updates and opens"`

---

### Task 5: Tool approval gate

**Files:**
- Port to `ai-hub-api/approval/`: `AiHubToolApprovalService`, `AiHubToolApprovalRuleService`, `AiHubToolApprovalRuleFacade` (no `componentName` argument or forced CATALOG), `AiHubToolApprovalPolicy`, `AiHubToolApprovalFacade`, `AiHubToolApprovalDefaults`
- Port to `ai-hub-service/approval/`: `AiHubToolApprovalServiceImpl` (no audit, no metrics), `AiHubToolApprovalRuleServiceImpl`, `AiHubToolApprovalRuleFacadeImpl` (roles per Global Constraints via `PermissionService.hasWorkspaceRole(workspaceId, "VIEWER")` and the `ADMIN` authority; rejects COMPONENT kind), `AiHubToolApprovalPolicyImpl` (`applyCatalogRules` only), `AiHubApprovalGate`, `AiHubApprovalGateToolCallback` (always CATALOG; implements Plan A's `DelegatingToolCallback`), `AiHubToolApprovalFacadeImpl` (`findCallback` = PINNED lookup over the two agents' `pinnedToolCallbacks()`; continuation kept, via Task 8's streamer and registry, injected through `ObjectProvider` so this task's tests mock them)
- Port to `ai-hub-service/agent/`: `NonEmptyToolCallback` (implements `DelegatingToolCallback`), `AiHubToolCallbackWrappers`
- Test: `approval/AiHubToolApprovalPolicyTest.java`, `AiHubApprovalGateToolCallbackTest.java`, `AiHubToolApprovalServiceTest.java`, `AiHubToolApprovalRuleFacadeTest.java`, `AiHubToolApprovalFacadeTest.java`, `AiHubToolApprovalEnumOrdinalTest.java`, `agent/NonEmptyToolCallbackTest.java` (SOURCE, trimmed to CATALOG / pinned paths)

**Interfaces:**
- Consumes: `com.bytechef.component.ai.llm.tool.DelegatingToolCallback` (Plan A), `RehydrateContextToolCallback` and `SecurityContextRehydrator` (existing).
- Produces: `AiHubToolCallbackWrappers.wrap(ToolCallback, SecurityContextRehydrator, AiHubApprovalGate)` and `wrap(ToolCallback, SecurityContextRehydrator)` (no gate); `AiHubToolApprovalFacade.resolve(long approvalId, boolean approved, @Nullable String comment)` returning `Resolution(AiHubToolApproval approval, boolean continuationStarted, @Nullable String runId)`; `AiHubToolApprovalService.supersedePending(long chatId)`, `deleteByChat(long chatId)`.

- [ ] **Step 1: Port the tests; add the Review Focus pins**

```java
@Test
void testResolvingTwiceExecutesTheToolOnce() {
    facade.resolve(approvalId, true, null);
    facade.resolve(approvalId, true, null);

    verify(gatedDelegate, times(1)).call(anyString(), any());
}

@Test
void testExpiredApprovalNeverExecutes() {
    // approval expiresAt in the past
    Resolution resolution = facade.resolve(approvalId, true, null);

    assertThat(resolution.approval().getStatus()).isEqualTo(AiHubToolApproval.Status.EXPIRED);
    verify(gatedDelegate, never()).call(anyString(), any());
}

@Test
void testDefaultsGateTheInScopeDestructiveTools() {
    assertThat(List.of("deleteProject", "deleteWorkflow", "deleteProjectDeployment", "rollbackProjectDeployment", "promoteWorkflow"))
        .allMatch(AiHubToolApprovalDefaults::isGatedByDefault);
    assertThat(AiHubToolApprovalDefaults.isGatedByDefault("listProjects")).isFalse();
}

@Test
void testPersistenceFailureFailsClosed() { ... }  // SOURCE has it; keep
```

- [ ] **Step 2: Run to verify they fail**, `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*Approval*' --tests '*NonEmptyToolCallback*' > /tmp/b5.log 2>&1; echo "EXIT=$?"`, `EXIT=1`.
- [ ] **Step 3: Port the classes.**
- [ ] **Step 4: Run**, `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Gate destructive AI Hub tools behind an owner approval"`

---

### Task 6: Hub tool context, agent type and `openWorkflowTab`

**Files:**
- Port to `ai-hub-api`: `tool/AiHubToolInvocationContext` (aliases Plan A's four `AutomationToolInvocationContext` keys)
- Port to `ai-hub-service/tool/`: `AiHubAgentType` (`AI_HUB_ASK("ai_hub_ask")`, `AI_HUB_BUILD("ai_hub_build")` only), `AiHubAgentTypeProvider` (`@AutoService`), `OpenWorkflowTabToolCallback`
- Test: `tool/AiHubToolInvocationContextTest.java`, `tool/OpenWorkflowTabToolCallbackTest.java` (SOURCE)

**Interfaces:**
- Consumes: `AutomationToolInvocationContext.TOOL_CONTEXT_{USER_ID,SOURCE_ORDINAL,LAST_USER_PROMPT,THREAD_ID}_KEY` (Plan A), `AiHubChatArtifactRecorder` (Task 4).
- Produces: tool `openWorkflowTab(workflowId, projectId, projectWorkflowId, name)` whose result JSON matches what the client validator expects (keep SOURCE's result shape verbatim; Task 12 consumes it).

- [ ] **Step 1: Port the two tests**; add `testOpenWorkflowTabRecordsAReference` (verify `recordWorkflowReference` with the thread id and user from the context) if SOURCE lacks it.
- [ ] **Step 2: Run to verify they fail**, `EXIT=1`.
- [ ] **Step 3: Port the classes.**
- [ ] **Step 4: Run**, `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Let the AI Hub agent open a workflow in the resource panel"`

---

### Task 7: The agent and its configuration

**Files:**
- Port to `ai-hub-service/agent/`: `AiHubSpringAIAgent` (trimmed per spec D2 section map; keep the hub-local nested `OverrideChatClientResolver`; override `endsTurn` for `awaitingAnswer` and `awaitingApproval`), `AiHubAgentTenantBinder`, `AiHubChatClientResolver` (trimmed to ~90 lines, `@ConditionalOnAiHub`), `NonEmptyMessagesAdvisor`
- Port to `ai-hub-service/config/`: `AiHubConfiguration` (`@ConditionalOnAiHub`; beans `aiHubAskSpringAIAgent`, `aiHubBuildSpringAIAgent`, `aiHubSessionMemory` only; static tool lists per spec D3; fail fast in a `@PostConstruct`/`SmartInitializingSingleton` if BUILD lacks `buildWorkflow`, message naming `bytechef.ai.hub.enabled` and `bytechef.ai.copilot.enabled`)
- Rewrite: `ai-hub-service/src/main/resources/prompt_ai_hub_ask.txt`, `prompt_ai_hub_build.txt` (spec D9; keep SOURCE's "Completing a built workflow" checklist and "Tool approvals" section; the workflow-building paragraph says what `ManagementMcpServerConfiguration.INSTRUCTIONS` says)
- Modify: `server/apps/server-app/src/test/java/com/bytechef/server/config/WorkflowContentSinglePathTest.java` (add `AiHubConfiguration.class` to `BUILD_WORKFLOW_SURFACES`; update the class Javadoc so it names only the surfaces in the list)
- Test: `agent/AiHubSpringAIAgentTest.java`, `AiHubSpringAIAgentTenantContextTest.java`, `AiHubToolContextModelPropagationTest.java`, `AiHubChatClientResolverTest.java`, `NonEmptyMessagesAdvisorTest.java`, `config/AiHubConfigurationDeploymentFlatCrudToolCallbacksTest.java`, `config/PropertyOptionsToolWiringTest.java` (SOURCE, trimmed), new `config/AiHubConfigurationToolSetTest.java`

**Interfaces:**
- Consumes (Plan A / existing): `ReadProjectTools`, `ReadProjectWorkflowTools`, `ProjectTools`, `ProjectWorkflowLifecycleTools`, `DeploymentToolCallbacksFactory.readToolCallbacks()/writeToolCallbacks()`, `InteractivePickerToolCallbacks.read()/write()`, `IntelligentToolCatalog.getByNames(Set.of("buildWorkflow","importWorkflow"), IntelligentToolVariant.BUILD, identity, (callback, definition) -> new WorkspaceScopedSubAgentToolCallback(callback, workspaceFacade))` (4-arg, JSON renderer), `SecurityContextRehydrator`, `CatalogChatClientResolver`, `ChatModel`; Tasks 3, 5, 6.
- Produces: `LocalAgent` beans with ids `ai_hub_ask`, `ai_hub_build`; `AiHubSpringAIAgent#pinnedToolCallbacks()` (unwrapped list) consumed by Task 5's facade.

- [ ] **Step 1: Write `AiHubConfigurationToolSetTest`**

```java
@Test
void testAskToolsAreReadOnly() {
    assertThat(toolNames(askAgent)).contains("listProjects", "getWorkflow", "listProjectDeployments",
        "listConnectionsForComponent", "askUserQuestion", "openWorkflowTab")
        .doesNotContain("createProject", "deleteProject", "createProjectDeployment", "selectConnection",
            "buildWorkflow", "updateWorkflow");
}

@Test
void testBuildToolsAuthorThroughBuildWorkflowOnly() {
    assertThat(toolNames(buildAgent)).contains("createProject", "createProjectWorkflow", "deleteWorkflow",
        "createProjectDeployment", "promoteWorkflow", "selectConnection", "createConnection",
        "buildWorkflow", "importWorkflow", "openWorkflowTab")
        .doesNotContain("updateWorkflow");
}

@Test
void testEveryBuildToolPassesThroughTheApprovalWrapper() { ... }  // each callback's outermost type is RehydrateContextToolCallback over AiHubApprovalGateToolCallback

@Test
void testStartupFailsWhenBuildWorkflowIsMissing() {
    // catalog returns no buildWorkflow: context fails, message contains both property names
}

@Test
void testAgentEndsTurnOnAwaitingAnswerAndAwaitingApproval() { ... }
```

`toolNames` reads `getToolDefinition().name()` of the agent's tool callbacks (use the accessor SOURCE's `AiHubSpringAIAgentTest` uses).

- [ ] **Step 2: Port and trim the other tests; run all to verify they fail**, `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/b7.log 2>&1; echo "EXIT=$?"`, `EXIT=1`.
- [ ] **Step 3: Port the classes, rewrite the prompts, extend `WorkflowContentSinglePathTest`.**
- [ ] **Step 4: Run the module tests and `./gradlew :server:apps:server-app:test --tests '*WorkflowContentSinglePathTest'`**, both `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Add the AI Hub ASK and BUILD agents on the shared project, workflow and deployment tools"`

---

### Task 8: Streaming, in-flight runs and the REST endpoint

**Files:**
- Port to `ai-hub-service/agent/`: `AiHubChatStreamer`, `InFlightAiHubRunRegistry`, `AiHubRunState` (as SOURCE)
- Port to `ai-hub-rest/web/rest/`: `AiHubApiController` (only `POST /ai/chat/ai_hub` and `GET /ai/chat/ai_hub/{threadId}/attach`; `@ConditionalOnEEVersion @ConditionalOnCoordinator @ConditionalOnAiHub`; workspace check per Global Constraints roles; no `recordTurn`), `TurnInFlightException` (plain 409)
- Test: `agent/InFlightAiHubRunRegistryTest.java`, `agent/AiHubRunStateTest.java`, `web/rest/AiHubApiControllerTest.java` (SOURCE, trimmed)

**Interfaces:**
- Consumes: agents (Task 7), `AiHubChatService` (Task 3), `AiHubToolApprovalService.supersedePending` (Task 5), `PermissionService`.
- Produces: `InFlightAiHubRunRegistry.tryStart(String threadId, ...)`, `.cancel(String threadId)`, `.attach(String threadId)`; `AiHubChatStreamer.stream(...)` (SOURCE signatures). Task 5's continuation now resolves real beans.

- [ ] **Step 1: Port the tests; add**

```java
@Test
void testSecondConcurrentTurnOnTheSameChatIs409() { ... }

@Test
void testTurnOnAnotherUsersChatIsForbidden() { ... }

@Test
void testBuildModeRequiresEditorRole() {
    // hasWorkspaceRole(workspaceId, "EDITOR") false, mode BUILD -> 403; mode ASK with VIEWER -> accepted
}

@Test
void testNewTurnSupersedesPendingApprovals() { ... }
```

- [ ] **Step 2: Run to verify they fail**, `EXIT=1`.
- [ ] **Step 3: Port the classes.**
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test :server:ee:libs:ai:ai-hub:ai-hub-rest:test > /tmp/b8.log 2>&1; echo "EXIT=$?"`, `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Stream AI Hub turns with one run per chat and approval continuations"`

---

### Task 9: GraphQL API

**Files:**
- Port to `ai-hub-graphql/web/graphql/`: `AiHubChatGraphQlController` (trimmed per D4), `AiHubChatArtifactGraphQlController` (no admin listing), `AiHubToolApprovalGraphQlController`, `AiHubToolApprovalRuleGraphQlController`, `AiHubGraphQlExceptionResolver`; all `@ConditionalOnAiHub`
- Port to `ai-hub-graphql/src/main/resources/graphql/`: `ai-hub-chat.graphqls` (trimmed per D4, no `AiHubChatKind`, visibility, participation, owner/author names), `ai-hub-artifact.graphqls` (no page/admin query), `ai-hub-tool-approval.graphqls` (as SOURCE)
- Test: `web/graphql/AiHubChatGraphQlControllerTest.java`, `AiHubToolApprovalGraphQlControllerTest.java`, `AiHubToolApprovalRuleGraphQlControllerTest.java`, `AiHubGraphQlExceptionResolverTest.java` (SOURCE, trimmed); new `GraphQlSchemaLoadsTest` that builds a `GraphQlSource` from the three schemas plus the platform base schema and asserts no unresolved types

**Interfaces:**
- Produces (schema): queries `aiHubChats`, `aiHubChatMessages`, `aiHubChatArtifactsByAiHubChat`, `aiHubToolApprovals`, `aiHubToolApprovalRules`, `aiHubToolApprovalDefaultToolNames`; mutations `createAiHubChat`, `updateAiHubChat`, `generateAiHubChatTitle`, `deleteAiHubChat`, `truncateAiHubChatMessages`, `appendAiHubChatAssistantMessage`, `cancelAiHubRun`, `recordReferencedAiHubChatArtifact`, `deleteAiHubChatArtifact`, `resolveAiHubToolApproval`, `createAiHubToolApprovalRule`, `deleteAiHubToolApprovalRule`. Field and argument names as SOURCE minus the removed fields. Task 10 generates the client from these.

- [ ] **Step 1: Port the tests; add** a controller test that `aiHubChatMessages` for another user's chat yields a GraphQL error classified `FORBIDDEN` and no data.
- [ ] **Step 2: Run to verify they fail**, `EXIT=1`.
- [ ] **Step 3: Port the controllers and schemas.**
- [ ] **Step 4: Run** `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-graphql:check > /tmp/b9.log 2>&1; echo "EXIT=$?"` and the boot check `DOCKER_HOST=... ./gradlew :server:apps:server-app:testIntegration --tests '*ServerApplicationIntTest' > /tmp/b9b.log 2>&1; echo "EXIT=$?"`, both `EXIT=0`.
- [ ] **Step 5: Commit** `git commit -m "732 Add the AI Hub chat, artifact and tool approval GraphQL API"`

---

### Task 10: Client GraphQL operations and shared chat additions

**Files:**
- Modify: `client/codegen.ts` (add `'../server/ee/libs/ai/ai-hub/ai-hub-graphql/src/main/resources/graphql/*.graphqls'`)
- Port: `client/src/graphql/ai/aihub/chat/*.graphql` (the ten chat operations in Task 9's list; drop removed fields), `artifact/{recordReferencedAiHubChatArtifact,deleteAiHubChatArtifact}.graphql`, `tool-approval/*.graphql` (all six)
- Regenerate: `client/src/shared/middleware/graphql.ts` (`cd client && npx graphql-codegen`)
- Port: `client/src/shared/components/ai-chat/messages/ToolApprovalRequestMessage.tsx`, `client/src/shared/components/ai-chat/approvalResolutionContext.ts` (`resolveApproval` optional)
- Modify: `client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts` (add the `tool-approval-request` fallback branch, SOURCE lines 194-212), `aiChatDataComponents.tsx` (`'tool-approval-request'` entry), `client/src/shared/stores/useApplicationInfoStore.tsx` (`ai.hub.enabled`, parsed like `ai.knowledgeBase`)
- Test: `ToolApprovalRequestMessage.test.tsx` (SOURCE), extend `toToolResultDataPart.test.ts` and the application info store test

- [ ] **Step 1: Write the tests** (`toToolResultDataPart` maps a `tool-approval-request` payload from any tool name to `data-tool-approval-request` with `approvalId`, `toolName`, `arguments`, `expiresAt`; a malformed one without numeric `approvalId` stays `undefined`; the store parses `ai.hub.enabled: 'true'` to `true`).
- [ ] **Step 2: Run** `cd client && npx vitest run src/shared/components/ai-chat src/shared/stores`, expect failures.
- [ ] **Step 3: Implement; run codegen.**
- [ ] **Step 4: `npm run format && npm run check`**, pass.
- [ ] **Step 5: Commit (two commits, per the GraphQL convention)**

`git commit -m "732 client - Add the AI Hub GraphQL operations"` (operations + `codegen.ts`), then `git commit -m "732 client - Regenerate the GraphQL client"` (`graphql.ts`), then `git commit -m "732 client - Render tool approval requests in the shared chat"` (the rest).

---

### Task 11: Embeddable workflow editor

**Files:**
- Create: `client/src/pages/platform/workflow-editor/EmbeddableWorkflowEditor.tsx` (from SOURCE, per spec D10: no `WorkflowDataStoreProvider`, `useGetConnectionTagsQuery()` without a workspace id, no `connectionTagsQueryKey`; reset the node-details, data-pill and test-chat singleton stores on `projectWorkflowId` change)
- Modify: `client/src/pages/platform/workflow-editor/WorkflowEditorLayout.tsx`, `components/WorkflowEditor.tsx`, `hooks/useWorkflowEditorCanvas.ts` (`fitViewOnWorkflowChange`), `client/src/pages/automation/project/components/project-header/ProjectHeader.tsx` (`showPublishDeploy`, `showWorkflowSelect`, `onWorkflowChange`, embedded gating), `client/src/shared/hooks/useWorkflowTestRunGuard.ts` (SOURCE's ref fix)
- Test: `EmbeddableWorkflowEditor.test.tsx` (new), extend `ProjectHeader` and `useWorkflowEditorCanvas` tests if present (SOURCE has them; port)

**Interfaces:**
- Produces: `EmbeddableWorkflowEditor({projectId: number, projectWorkflowId: number, showPublishDeploy?: boolean})` default export; Task 13's `AiHubWorkflowViewer` renders it.

- [ ] **Step 1: Write the tests** (renders the editor for a stubbed project workflow; `ProjectHeader` with `embedded` hides the breadcrumb and settings menu and shows Publish only with `showPublishDeploy`; `fitViewOnWorkflowChange` calls `fitView` once after nodes initialise; switching `projectWorkflowId` resets the three stores).
- [ ] **Step 2: Run, expect failures.**
- [ ] **Step 3: Implement.** The full project page must behave exactly as before: every new prop defaults to today's behaviour.
- [ ] **Step 4: `npm run format && npm run check`**, pass.
- [ ] **Step 5: Commit** `git commit -m "732 client - Add an embeddable workflow editor for the AI Hub resource panel"`

---

### Task 12: Hub stores and runtime

**Files (all under `client/src/ee/pages/automation/ai-hub/`, ported per spec D10):**
- `stores/useAiHubTabsStore.ts` (workflow kind only), `stores/useAiHubStore.ts` (no voice), `stores/useAiHubSettingsStore.ts`, `runtime-providers/stores/useAiHubRunStateStore.ts`, `runtime-providers/stripLeakedToolMarkup.ts`, `runtime-providers/inFlightRunClient.ts` (only `attachToInFlightRun`), `runtime-providers/AiHubRuntimeProvider.tsx` (the IN ranges listed in the spec survey; ~1,100-1,200 code lines), `hooks/useResetAiHubChatOnEnvironmentChange.ts`, `hooks/useResetAiHubStoresOnWorkspaceChange.ts`, `chats/api/chats.api.ts`, `chats/stores/useAiHubChatsStore.ts`, `chats/pendingToolApproval.ts`
- Tests: SOURCE's tests for each kept file, trimmed (`runtime-providers/tests/*`, `stores/tests/*`, `chats/tests/*` that cover these files)

**Interfaces:**
- Consumes: Task 10 operations and `ApprovalResolutionContext`; shared `useAiChatToolCallStore` with Plan A's `resetForChat` / `failRunningInChat`; `useReportQueryError`.
- Produces: `AiHubRuntimeProvider({children, chatId})`, `useAiHubTabsStore` with `openWorkflowTab({name, projectId, projectWorkflowId, workflowId})`, `closeTab`, `setActiveTab`, `rightPanelOpen`, `chatsSidebarCollapsed`; consumed by Task 13.

- [ ] **Step 1: Port the tests; add**: an `askUserQuestion` result from `buildWorkflow` adds a `data-ask-user-question` part and marks the run idle (turn ended); an `openWorkflowTab` result opens one tab and a second one for the same workflow focuses it; a `buildWorkflow` result invalidates `ProjectWorkflowKeys` for its project.
- [ ] **Step 2: Run, expect failures.**
- [ ] **Step 3: Port and trim.**
- [ ] **Step 4: `npm run format && npm run check`**, pass.
- [ ] **Step 5: Commit** `git commit -m "732 client - Add the AI Hub runtime and stores"`

---

### Task 13: Hub page, chats sidebar, composer, messages, artifacts and resource panel

**Files (under `client/src/ee/pages/automation/ai-hub/`, per spec D10 per-file trims):**
- `AiHub.tsx`, `AiHubPanel.tsx`, `AiHubResourcePanel.tsx`, `AiHubHomePanel.tsx`, `AiHubChatsSidebarToggle.tsx`, `AiHubErrorBoundary.tsx`, `AiHubWorkflowViewer.tsx`, `WorkflowTabLabel.tsx`
- `chats/AiHubChatsSidebar.tsx`, `chats/AiHubChatActionDialogs.tsx`, `chats/hooks/{useAiHubChatActions,useChats,useRecordReferencedArtifacts,useSwitchChat,useTruncateChatMessages}.ts`
- `composer/AiHubChatComposer.tsx` (reduced: input, `ModeSwitch`, model picker slot, Send/Stop, approval placeholder)
- `messages/{AiHubThread,AiHubMessage,AiHubMessageContent,AiHubSuggestionChips,AiHubToolCallRenderer}.tsx`
- `artifacts/{AiHubArtifactsCard,AiHubArtifactRow,artifactIcons}.tsx`, `artifacts/{useAiHubArtifactsCard,artifactOpen}.ts`
- Tests: SOURCE's tests for each kept file, trimmed; Playwright `client/test/playwright/tests/ai-hub/` specs that touch only sidebar, new chat and mode toggle

- [ ] **Step 1: Port the tests; add**: opening `/automation/ai-hub/chats/<id>` for a chat the API returns FORBIDDEN shows the not-found state, not a blank thread; the artifacts card lists WORKFLOW_CREATED / UPDATED / REFERENCED rows and clicking one opens the workflow tab; the composer is disabled while a turn runs and Stop calls `cancelAiHubRun`.
- [ ] **Step 2: Run, expect failures.**
- [ ] **Step 3: Port and trim.**
- [ ] **Step 4: `npm run format && npm run check`**, pass.
- [ ] **Step 5: Commit** `git commit -m "732 client - Add the AI Hub page with chats, artifacts and the workflow tab"`

---

### Task 14: Tool approvals settings, routes, navigation and the default page

**Files:**
- Port: `client/src/ee/pages/automation/ai-hub/settings/ToolApprovals.tsx` (`canManage` from the `ADMIN` authority), `settings/components/{AddToolApprovalRuleDialog,BuiltInToolApprovalsCard,WorkspaceToolApprovalRulesCard}.tsx`
- Modify: `client/src/routes.tsx` (lazy `AiHub` and `AiHubToolApprovalsPage`; `ai-hub` and `ai-hub/chats/:chatId` beside the automation `chats` route wrapped `PrivateRoute` > `EEVersion` > `LazyLoadWrapper hasLeftSidebar`; `ai-hub/tool-approvals` in `currentWorkspaceSettingsRoutes` under a nested `{items: [...], title: 'AI Hub'}` group; the automation index loader redirects to `ai-hub` when `edition === EE && ai.hub.enabled`, else `projects`), `client/src/App.tsx` (hub entry first in `automationNavigation`, icon not `MessagesSquareIcon`; filter on EE and `ai.hub.enabled`), `client/src/shared/layout/Settings.tsx` (filter `ai-hub/tool-approvals` the same way)
- Test: `settings/tests/ToolApprovals.test.tsx` (SOURCE), new `routes` test for the index redirect (hub on EE → `ai-hub`; CE or hub off → `projects`), `App` nav filter test, `Settings` nav filter test

- [ ] **Step 1: Write the tests.** The redirect test drives the index route's `loader` directly with the application info and edition stores set.
- [ ] **Step 2: Run, expect failures.**
- [ ] **Step 3: Implement.** Confirm the application info store is loaded before the router evaluates the loader (spec D10); if it is not, await it in the loader the same way the existing `projects` loader reads `environmentStore`.
- [ ] **Step 4: `npm run format && npm run check`**, pass.
- [ ] **Step 5: Commit** `git commit -m "732 client - Make the AI Hub the default automation page and add its tool approval settings"`

---

### Task 15: Deep-dive doc

**Files:**
- Create: `docs/agents/ai-hub.md` (spec D12: chats, the tool sets and the three-doors parity contract, the one wrap site and the approval gate, the artifact recorders, the gate on both toggles, the default-page redirect, the deferred phases by name with pointers to `0_732`'s specs)
- Modify: `CLAUDE.md` (one line under "Agent skills", matching the other `docs/agents/*` pointers)

- [ ] **Step 1: Write the doc** (under 150 lines; no em dashes; every class it names exists on this branch: check with `grep -r`).
- [ ] **Step 2: Commit** `git commit -m "732 docs - Describe the AI Hub as it exists on this line"`

---

### Task 15b: Playwright specs for the #6377 scenarios

**Spec:** the scenario comment on https://github.com/bytechefhq/bytechef/issues/6377 (sections 1-9). Reuse `client/test/playwright/utils/agUiStreamStub.ts` from `0_p_1570` (Copilot specs plan, Task 1); point it at `/api/platform/internal/ai/chat/ai_hub`.

**Files:** Create under `client/test/playwright/tests/ai-hub/`: `aiHubNavigation.spec.ts` (section 1), `aiHubChats.spec.ts` (2), `aiHubBuild.spec.ts` (3: stubbed `createProject`/`createProjectWorkflow`/`buildWorkflow`/`openWorkflowTab` results, pickers, delegate question, artifacts card), `aiHubWorkflowTab.spec.ts` (4, against a real project created through the API), `aiHubApprovals.spec.ts` (6: stubbed `tool-approval-request` payload, approve/reject via the real `resolveAiHubToolApproval` mutation stubbed with `page.route` on `/graphql`, settings page rules), `aiHubModesAndRoles.spec.ts` (7: ASK request carries `mode: 'ASK'`, VIEWER login cannot pick BUILD, a second concurrent send shows the in-flight message from a stubbed 409, Stop calls `cancelAiHubRun`), `aiHubLive.spec.ts` (`@live`: sections 3, 5, 6, 8 end to end on a real model, including the three-door parity comparison with the MCP client and the Copilot); keep and update `aiHub.spec.ts` from SOURCE for the shell assertions. Section 9 (configuration) is a `@live` test that needs a server restart per setting; implement it as a `test.describe` that reads `PLAYWRIGHT_HUB_CONFIG` (`on`, `hub-off`, `copilot-off`) and asserts the nav/route state for that run.

- [ ] Write the deterministic specs; run `cd client && npx playwright test tests/ai-hub`; pass with no model configured.
- [ ] Write `aiHubLive.spec.ts`; run with `PLAYWRIGHT_LIVE=1` during Task 16's acceptance; record per-test results.
- [ ] Commit `732 client - Cover the AI Hub scenarios with Playwright specs` (and update `client/test/playwright/README.md`).

### Task 16: Whole-branch verification and acceptance

- [ ] **Step 1: Server**

Run: `DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock ./gradlew :server:ee:libs:ai:ai-hub:ai-hub-api:check :server:ee:libs:ai:ai-hub:ai-hub-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:testIntegration :server:ee:libs:ai:ai-hub:ai-hub-rest:check :server:ee:libs:ai:ai-hub:ai-hub-graphql:check :server:apps:server-app:test :server:apps:server-app:testIntegration --tests '*ServerApplicationIntTest' --continue > /tmp/planB-check.log 2>&1; echo "EXIT=$?"`, then `grep '^> Task .* FAILED' /tmp/planB-check.log`. Then `./gradlew compileJava compileTestJava --continue` the same way.
Expected: `EXIT=0`, no matches.

- [ ] **Step 2: Client** `cd client && npm run check`, pass.

- [ ] **Step 3: Manual acceptance** (spec Acceptance 2-7) against a local server (`bytechef.ai.hub.enabled=true`, copilot enabled, a configured model) and the Vite client, driven with the Playwright MCP: record each item's result (pass / fail with a screenshot path) in the hand-back. Acceptance 3 (parity across MCP, Copilot and hub) is run by hand and recorded the same way.

- [ ] **Step 4: Report.** Do not push. List the commits (`git log --oneline cb974653950..HEAD`).
