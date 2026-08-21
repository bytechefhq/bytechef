# Manager Subagent Relocation (Prep A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `mcp_manager` + `deployment_manager` (and their tool callbacks + shared scaffolding) into CE `automation-ai-tool`, `api_collection_manager` into EE `automation-ai-tool`, leaving `personal_agent_manager` in `ai-hub-service` — with the management MCP server and the ai_hub BUILD agent behaving identically.

**Architecture:** Pure relocation refactor. The three management managers are Spring `@Configuration` classes (each defines a subagent `ChatClient` `@Bean` + a static `create*ManagerToolCallback` factory) plus their constituent tool callbacks. They currently live under `com.bytechef.ee.ai.hub`. Because the app component-scans `com.bytechef` broadly and every relocated MCP/deployment tool callback imports only CE types, they can move to `com.bytechef.automation.ai.tool` (CE) with no behavior change. `api_collection_manager` depends on the EE apiplatform facade, so it lands in EE `automation-ai-tool`. One EE contributor bean splits into three (CE mcp+deployment, EE api_collection, slimmed ai-hub personal-only).

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (`ChatClient`, `ToolCallback`), Gradle Kotlin DSL, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- CE files (`server/libs/…`): Apache 2.0 license header; no `@version ee`; no `com.bytechef.ee.*` imports.
- EE files (`server/ee/…`): ByteChef Enterprise license header; `@version ee` Javadoc tag.
- Spotless picks the EE header by the `@version ee` **content**, not the path — CE files must not carry it.
- Keep Spring bean qualifier names identical across the move: `mcpManagerChatClient`, `deploymentManagerChatClient`, `apiCollectionManagerChatClient`, `personalAgentManagerChatClient`.
- Keep the `@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")` gate on every manager config and contributor.
- Keep `WorkspaceScopedManagerToolCallback` wrapping on the MCP surface and `ObjectProvider.ifAvailable` silent-skip.
- Test class names: drop `Impl`, use `Test` suffix (unit), `IntTest` (integration). Test methods camelCase, no underscores.
- Run `./gradlew spotlessApply` before every commit.

## Module reference

- CE tool module: `server/libs/automation/automation-ai/automation-ai-tool` — package `com.bytechef.automation.ai.tool`, build file `…/build.gradle.kts`.
- EE tool module: `server/ee/libs/automation/automation-ai/automation-ai-tool` — package `com.bytechef.ee.automation.ai.tool`.
- Source (ai-hub): `server/ee/libs/ai/ai-hub/ai-hub-service` — package root `com.bytechef.ee.ai.hub`, config in `…/config`, tools in `…/tool`, prompts in `src/main/resources`.

---

### Task 1: CE scaffolding — label enum + shared manager tool callbacks

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerAgentType.java`
- Move: `…/ee/ai/hub/tool/ManagerSubAgentToolCallback.java` → `…/automation/ai/tool/ManagerSubAgentToolCallback.java`
- Move: `…/ee/ai/hub/tool/WorkspaceScopedManagerToolCallback.java` → `…/automation/ai/tool/WorkspaceScopedManagerToolCallback.java`
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/build.gradle.kts`
- Modify: EE files still referencing the two moved classes (api_collection + personal managers, the old contributor) — update imports.

**Interfaces:**
- Produces: `com.bytechef.automation.ai.tool.ManagerAgentType` — enum `implements com.bytechef.ai.agent.tool.AgentType` with `MCP_MANAGER("mcp_manager")`, `DEPLOYMENT_MANAGER("deployment_manager")`, `API_COLLECTION_MANAGER("api_collection_manager")`; constructor `(String key, boolean fallback)` mirroring `AiHubAgentType`; `key()`/`isFallback()` identical to `AiHubAgentType`'s.
- Produces: `com.bytechef.automation.ai.tool.ManagerAgentTypeProvider` — `@AutoService(AgentTypeProvider.class)` returning `Set.of(ManagerAgentType.values())`.
- Produces: `com.bytechef.automation.ai.tool.ManagerSubAgentToolCallback(AgentType, ChatClient, String)` and `com.bytechef.automation.ai.tool.WorkspaceScopedManagerToolCallback(ManagerSubAgentToolCallback, WorkspaceService)` — same public API as today, new package.

**CRITICAL — AgentType key collision:** `ManagerSubAgentToolCallback` uses `agentType.key()` and `CurrentAgentContext.callWith(agentType, …)`, and `AgentTypeProvider` keys must be **unique across all providers**. So the three constants MUST be **removed from `AiHubAgentType`** in this task (they only appear in the moving configs + their tests) at the same time they are added to `ManagerAgentType`. `AiHubAgentType` retains `PERSONAL_AGENT_MANAGER` and all non-manager values. The CE `ManagerAgentTypeProvider` replaces those three keys in the registry.

- [ ] **Step 1: Read the AgentType interface and AiHubAgentType** to copy the exact method set the label enum must satisfy.

Run: `sed -n '1,60p' server/libs/ai/ai-api/src/main/java/com/bytechef/ai/agent/tool/AgentType.java server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/AiHubAgentType.java`

- [ ] **Step 2: Create `ManagerAgentType`** (Apache header, CE package) implementing `AgentType`, with the three manager constants using the exact string names `"mcp_manager"`, `"deployment_manager"`, `"api_collection_manager"` and the same secondary field value the AiHubAgentType constants pass (`false`). Implement every `AgentType` method exactly as `AiHubAgentType` does.

- [ ] **Step 3: Move the two scaffolding classes** with `git mv`, then rewrite their `package` line to `com.bytechef.automation.ai.tool` and swap the Apache header in (strip the EE header + any `@version ee`).

```bash
git mv server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/ManagerSubAgentToolCallback.java \
       server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerSubAgentToolCallback.java
git mv server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/tool/WorkspaceScopedManagerToolCallback.java \
       server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/WorkspaceScopedManagerToolCallback.java
```

- [ ] **Step 4: Add the CE build dependency** on `ai-api` (for `AgentType`) and `ai-mcp-server-api` is NOT needed here (added in Task 5). Add to `automation-ai-tool/build.gradle.kts` implementation block:

```kotlin
    implementation(project(":server:libs:ai:ai-api"))
```

(`WorkspaceService` is `com.bytechef.automation.configuration.service` — already covered by the existing `automation-configuration-api` dependency; verify it resolves at compile.)

- [ ] **Step 5: Update EE importers.** `git grep -l "com.bytechef.ee.ai.hub.tool.ManagerSubAgentToolCallback\|com.bytechef.ee.ai.hub.tool.WorkspaceScopedManagerToolCallback"` and rewrite each import to the new `com.bytechef.automation.ai.tool.*` package. (Callers: `ApiCollectionManagerConfiguration`, `PersonalAgentManagerConfiguration`, `AiHubManagerMcpContributorConfiguration`, and any test — some resolve in later tasks; fix all that remain in ai-hub now.) Ensure `ai-hub-service/build.gradle.kts` has `implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))` (add if absent).

- [ ] **Step 6: Compile CE tool module + ai-hub-service.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Move manager subagent scaffolding to CE automation-ai-tool"
```

---

### Task 2: Relocate `mcp_manager` to CE `automation-ai-tool`

**Files:**
- Move (config): `…/ee/ai/hub/config/McpManagerConfiguration.java` → `…/automation/ai/tool/McpManagerConfiguration.java`
- Move (prompt): `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_mcp_manager.txt` → `server/libs/automation/automation-ai/automation-ai-tool/src/main/resources/prompt_mcp_manager.txt`
- Move (7 tool callbacks): `CreateMcpServerToolCallback`, `ListMcpServersToolCallback`, `UpdateMcpServerToolCallback`, `CreateMcpProjectToolCallback`, `CloneMcpProjectToolCallback`, `ListMcpProjectWorkflowsToolCallback`, `UpdateMcpProjectWorkflowParametersToolCallback` — from `…/ee/ai/hub/tool/` → `…/automation/ai/tool/`
- Modify: `automation-ai-tool/build.gradle.kts`

**Interfaces:**
- Consumes: `ManagerAgentType`, `ManagerSubAgentToolCallback` (Task 1).
- Produces: `com.bytechef.automation.ai.tool.McpManagerConfiguration.createMcpManagerToolCallback(ChatClient)` (static) + `@Bean mcpManagerChatClient` — same names, new package.

- [ ] **Step 1: `git mv` all 8 java files + the prompt** to the CE module/package paths above.

- [ ] **Step 2: Rewrite each moved file's `package` to `com.bytechef.automation.ai.tool`, swap in the Apache header** (remove EE header + `@version ee`), and update intra-set imports (they reference each other and `ManagerSubAgentToolCallback`/`ManagerAgentType` now in the same CE package — drop stale `com.bytechef.ee.ai.hub.tool.*` imports). In `McpManagerConfiguration`, replace `AiHubAgentType.MCP_MANAGER` → `ManagerAgentType.MCP_MANAGER` and drop the `AiHubAgentType` import.

- [ ] **Step 3: Add CE build deps** for the MCP facade/service/domain the callbacks use (`com.bytechef.automation.ai.mcp.facade|service|domain`, `com.bytechef.platform.mcp.domain`, `com.bytechef.atlas.configuration.service.WorkflowService`, `com.bytechef.platform.ai.tool.constant`, `com.bytechef.platform.component.constant`). Add to `automation-ai-tool/build.gradle.kts` (verify exact CE project paths with `git grep -l "name = \"automation-ai-mcp` settings.gradle.kts` first):

```kotlin
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
```

Resolve each concrete project path against `settings.gradle.kts`; add only those not already present. If any dep pulls an EE module, STOP — that contradicts the zero-EE-import finding; re-inspect the offending callback.

- [ ] **Step 4: Compile CE tool module.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava -q`
Expected: BUILD SUCCESSFUL. Fix missing-dependency compile errors by adding the precise CE project dep the error names.

- [ ] **Step 5: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Relocate mcp_manager subagent to CE automation-ai-tool"
```

---

### Task 3: Relocate `deployment_manager` to CE `automation-ai-tool`

**Files:**
- Move (config): `…/ee/ai/hub/config/DeploymentManagerConfiguration.java` → `…/automation/ai/tool/DeploymentManagerConfiguration.java`
- Move (prompt): `prompt_deployment_manager.txt` → CE `automation-ai-tool` resources
- Move (7 tool callbacks): `CreateProjectDeploymentToolCallback`, `UpdateProjectDeploymentToolCallback`, `DeleteProjectDeploymentToolCallback`, `ListProjectDeploymentsToolCallback`, `PromoteWorkflowToolCallback`, `RollbackProjectDeploymentToolCallback`, `ToggleProjectDeploymentToolCallback` → CE package

**Interfaces:**
- Consumes: `ManagerAgentType`, `ManagerSubAgentToolCallback`.
- Produces: `com.bytechef.automation.ai.tool.DeploymentManagerConfiguration.createDeploymentManagerToolCallback(ChatClient)` + `@Bean deploymentManagerChatClient`.

- [ ] **Step 1: `git mv` the 8 java files + prompt** to CE paths.

- [ ] **Step 2: Repackage + Apache header + `AiHubAgentType.DEPLOYMENT_MANAGER` → `ManagerAgentType.DEPLOYMENT_MANAGER`**; drop stale EE imports. The deployment callbacks use `com.bytechef.automation.configuration.facade|service|domain|dto` (CE) — already covered by `automation-configuration-api`; verify.

- [ ] **Step 3: Compile CE tool module.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava -q`
Expected: BUILD SUCCESSFUL (add any missing CE dep the error names, e.g. `platform-configuration-api` if a domain type is referenced).

- [ ] **Step 4: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Relocate deployment_manager subagent to CE automation-ai-tool"
```

---

### Task 4: Relocate `api_collection_manager` to EE `automation-ai-tool`

**Files:**
- Move (config): `…/ee/ai/hub/config/ApiCollectionManagerConfiguration.java` → `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/ApiCollectionManagerConfiguration.java`
- Move (prompt): `prompt_api_collection_manager.txt` → EE `automation-ai-tool` resources
- Move (3 tool callbacks): `CreateApiCollectionToolCallback`, `CloneApiCollectionToolCallback`, `ListApiCollectionsToolCallback` → EE package `com.bytechef.ee.automation.ai.tool`
- Modify: `server/ee/libs/automation/automation-ai/automation-ai-tool/build.gradle.kts`

**Interfaces:**
- Consumes: `com.bytechef.automation.ai.tool.ManagerAgentType`, `ManagerSubAgentToolCallback`, `WorkspaceScopedManagerToolCallback` (CE — EE may import CE).
- Produces: `com.bytechef.ee.automation.ai.tool.ApiCollectionManagerConfiguration.createApiCollectionManagerToolCallback(ChatClient)` + `@Bean apiCollectionManagerChatClient`.

- [ ] **Step 1: `git mv` the 4 java files + prompt** to EE `automation-ai-tool` paths.

- [ ] **Step 2: Repackage to `com.bytechef.ee.automation.ai.tool`, keep the EE header + `@version ee`.** Replace `AiHubAgentType.API_COLLECTION_MANAGER` → `com.bytechef.automation.ai.tool.ManagerAgentType.API_COLLECTION_MANAGER`; import the CE scaffolding from `com.bytechef.automation.ai.tool.*`.

- [ ] **Step 3: Add EE build deps** — CE `automation-ai-tool` (for the scaffolding + label) and the EE apiplatform facade if not already present:

```kotlin
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:ee:libs:automation:automation-apiplatform:automation-apiplatform-configuration:automation-apiplatform-configuration-api"))
```

(Confirm the apiplatform project path in `settings.gradle.kts`.)

- [ ] **Step 4: Compile EE tool module.**

Run: `./gradlew :server:ee:libs:automation:automation-ai:automation-ai-tool:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Relocate api_collection_manager subagent to EE automation-ai-tool"
```

---

### Task 5: Split the MCP contributor three ways

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerMcpContributorConfiguration.java` (CE) — contributes `mcp_manager` + `deployment_manager`.
- Create: `server/ee/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/ee/automation/ai/tool/ApiCollectionManagerMcpContributorConfiguration.java` (EE) — contributes `api_collection_manager`.
- Modify: `…/ee/ai/hub/config/AiHubManagerMcpContributorConfiguration.java` — slim to `personal_agent_manager` only.
- Modify: `automation-ai-tool/build.gradle.kts` (CE) — add `ai-mcp-server-api` (the SPI).
- Modify: `automation-ai-tool/build.gradle.kts` (EE) — add `ai-mcp-server-api` if absent.

**Interfaces:**
- Consumes: `McpServerToolCallbackContributor` (`com.bytechef.ai.mcp.server.spi`), `WorkspaceScopedManagerToolCallback`, `WorkspaceService`, and the moved `create*ManagerToolCallback` factories + qualified `ChatClient` beans.
- Produces: three `McpServerToolCallbackContributor` beans whose union equals today's four-manager contribution.

- [ ] **Step 1: Read the current `AiHubManagerMcpContributorConfiguration`** to copy the exact wrapping (`toWorkspaceScoped`, `ifAvailable`, gating).

- [ ] **Step 2: Write the CE contributor** (Apache header, `@ConditionalOnProperty bytechef.ai.hub.enabled=true`) injecting `@Qualifier("mcpManagerChatClient")` + `@Qualifier("deploymentManagerChatClient")` `ObjectProvider<ChatClient>` and `WorkspaceService`, adding each (when available) wrapped in `WorkspaceScopedManagerToolCallback` via the moved factories.

- [ ] **Step 3: Write the EE contributor** (EE header + `@version ee`, same gate) for `apiCollectionManagerChatClient`.

- [ ] **Step 4: Slim `AiHubManagerMcpContributorConfiguration`** to inject only `personalAgentManagerChatClient` and contribute the single personal-agent manager; remove the mcp/deployment/api_collection injections and their now-removed imports.

- [ ] **Step 5: Add the SPI dep** `implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server-api"))` to both CE and EE `automation-ai-tool` build files (verify path).

- [ ] **Step 6: Compile all three modules.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:compileJava :server:ee:libs:automation:automation-ai:automation-ai-tool:compileJava :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Split manager MCP contributor: CE mcp+deployment, EE api_collection, ai-hub personal-only"
```

---

### Task 6: Rewire the ai_hub BUILD agent factory imports

**Files:**
- Modify: `…/ee/ai/hub/config/AiHubConfiguration.java` — update the four `create*ManagerToolCallback` static references + imports to the new homes.

**Interfaces:**
- Consumes: the moved factories (`com.bytechef.automation.ai.tool.McpManagerConfiguration`, `…DeploymentManagerConfiguration`, `com.bytechef.ee.automation.ai.tool.ApiCollectionManagerConfiguration`) and the unchanged `com.bytechef.ee.ai.hub.config.PersonalAgentManagerConfiguration`.

- [ ] **Step 1: Update the imports** in `AiHubConfiguration` for `McpManagerConfiguration`, `DeploymentManagerConfiguration`, `ApiCollectionManagerConfiguration` to their new packages; leave `PersonalAgentManagerConfiguration` as-is. The static call sites (`McpManagerConfiguration.createMcpManagerToolCallback(...)`, etc.) are unchanged by class name.

- [ ] **Step 2: Ensure `ai-hub-service/build.gradle.kts` depends on EE `automation-ai-tool`** (for `ApiCollectionManagerConfiguration`) and CE `automation-ai-tool` (Task 1). Add the EE dep if absent:

```kotlin
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-tool"))
```

- [ ] **Step 3: Compile ai-hub-service.**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: spotlessApply + commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Point ai_hub BUILD agent at relocated manager factories"
```

---

### Task 7: Relocate tests + full verification

**Files:**
- Move: any `McpManager*Test` / `DeploymentManager*Test` → CE `automation-ai-tool` test tree; `ApiCollectionManager*Test` → EE test tree (repackage + header per destination).
- Modify: `…/ee/ai/hub/config/AiHubManagerMcpContributorConfigurationTest.java` — assert only `personal_agent_manager` remains.
- Create: `ManagerMcpContributorConfigurationTest` (CE) asserting mcp+deployment contributed + workspace-scoped + skipped when the `ChatClient` bean is absent.
- Create: `ApiCollectionManagerMcpContributorConfigurationTest` (EE) asserting api_collection contributed.

- [ ] **Step 1: Locate existing manager tests.** Run: `git grep -l "McpManagerConfiguration\|DeploymentManagerConfiguration\|ApiCollectionManagerConfiguration" -- '*Test.java'`

- [ ] **Step 2: Move + repackage each existing manager test** to the module matching its subject; fix imports to the new production packages; apply the destination's license header.

- [ ] **Step 3: Slim `AiHubManagerMcpContributorConfigurationTest`** to the personal-agent-only expectation (remove mcp/deployment/api_collection assertions).

- [ ] **Step 4: Write the two new contributor tests** (CE + EE) mirroring the existing test's mock-ChatClient + `ObjectProvider` style: assert the contributor returns the expected manager tool(s), each wrapped in `WorkspaceScopedManagerToolCallback`, and returns empty when the provider is unavailable.

- [ ] **Step 5: Run the affected module test suites + a broad compile.**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test :server:ee:libs:automation:automation-ai:automation-ai-tool:test :server:ee:libs:ai:ai-hub:ai-hub-service:test -q`
Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 6: Verify no dangling references** to the old packages.

Run: `git grep -n "com.bytechef.ee.ai.hub.tool.ManagerSubAgentToolCallback\|com.bytechef.ee.ai.hub.tool.WorkspaceScopedManagerToolCallback\|ee.ai.hub.config.McpManagerConfiguration\|ee.ai.hub.config.DeploymentManagerConfiguration\|ee.ai.hub.config.ApiCollectionManagerConfiguration"`
Expected: no matches.

- [ ] **Step 7: spotlessApply + final commit.**

```bash
./gradlew spotlessApply -q
git add -A
git commit -m "732 Relocate + add manager contributor tests for Prep A"
```

---

## Self-Review

**Spec coverage:** every spec §"What moves" row maps to a task (scaffolding → T1; mcp_manager → T2; deployment_manager → T3; api_collection_manager → T4; contributor fan-out → T5; BUILD agent → T6; tests → T7). Gate + workspace-scope preservation is a Global Constraint checked in T5. The `AgentType` decouple is T1's `ManagerAgentType`.

**Placeholder scan:** dependency `project(...)` paths are marked "verify against settings.gradle.kts" because exact module coordinates must be confirmed at execution — not placeholders for logic, but real paths to resolve. All logic steps are concrete.

**Type consistency:** `ManagerAgentType` (T1) is consumed by name in T2/T3/T4; `create*ManagerToolCallback` factory names are unchanged from the originals and reused verbatim in T5/T6; qualifier bean names are pinned in Global Constraints and reused in T5.

**Known execution risk:** if a relocated callback fails CE compile due to a hidden EE type, fall back to EE `automation-ai-tool` for that whole manager (spec §Risks) and record the blocking dependency.
