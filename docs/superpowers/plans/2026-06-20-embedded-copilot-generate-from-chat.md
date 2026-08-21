# Embedded Copilot "Generate from Chat" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a conversational, streaming "Generate from Chat" experience to embedded mode — a connected end-user builds one workflow by chatting with the real Copilot agent, with no workflow editor shown.

**Architecture:** A single new public, frontend-only, SSE endpoint under `/api/embedded/v1` runs the existing `workflow_editor_build` agent over the AG-UI protocol (mirroring the web client's `CopilotPanel`). A new self-contained React component in `@bytechef/embedded-react` creates a skeleton workflow up front via the existing create endpoint, then drives the SSE chat against that `workflowUuid`. The skeleton's `workflowUuid` anchors the conversation; the server resolves the authoritative `workflowId`, authorization, tenant, principal, and allowed components per turn.

**Tech Stack:** Java 25 / Spring Boot 4, `com.agui` (spring-ag-ui) server lib, `SseEmitter`; React 19 / TypeScript, `@ag-ui/client` `HttpAgent`, `@assistant-ui/react`, `zustand`.

## Global Constraints

- All new/modified **server files under `server/ee/`** use the **ByteChef Enterprise license header** (not Apache) and carry a `@version ee` Javadoc tag — including test classes. (Spotless selects the header by file content `@version ee`, so the tag must be present.)
- Java: one blank line before control statements and after a variable modification that precedes its use; no trailing blank line before a class's closing `}`; no `_`-prefixed private methods; descriptive variable names (no single letters).
- The new endpoint must be reachable at a path matching `^/api/embedded/v1/.+` so `EmbeddedApiKeySecurityConfigurer` authenticates it (embedded JWT → `externalUserId` via `SecurityUtils.fetchCurrentUserLogin()`). All browser-facing methods are annotated `@CrossOrigin`.
- The endpoint is **frontend-only** — no `externalUserId`-in-path backend variant.
- Mode is **BUILD-only**; agent id is `workflow_editor_build` = `(Source.WORKFLOW_EDITOR + "_" + Mode.BUILD).toLowerCase()`.
- The server **never trusts** client-supplied `workflowId`, tenant, user, or principal. It overrides `workflowId` in the AG-UI state with the server-resolved value and injects tenant/principal itself.
- Client SDK: interface names end in `I` or `Props`; named imports sorted alphabetically within `{}`; object keys in ascending order; `useRef` vars end in `Ref`; icons imported with the `Icon` suffix; conditional classes merged with `twMerge` (not `cn`). Run `npm run check` in the SDK before any client commit.
- Commit message convention: server `<ticket> <desc>`; client `<ticket> client - <desc>`. No ticket number is assigned for this work — use a short descriptive message and end every commit with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer. Make **fresh commits** (never `--amend` on branch `0_732`); stage only files this task touched.

## Deviation from spec (intentional)

The spec proposed an `EmbeddedCopilotChatService` (ai-copilot-api/-service) to wrap agent selection + state injection. This plan instead does that **inline in the new controller**, exactly like the existing `CopilotApiController.chat(...)`. Rationale: single caller (YAGNI), it matches the referenced template, and a cross-module service would force `agui-server` + `SseEmitter`/`spring-webmvc` dependencies onto the deliberately thin `ai-copilot-api` module. The state-key knowledge it touches (`CopilotStateKeys`, `Mode`, `Source`, `TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY`) is all already public API consumed cross-module today.

## File structure

**Server — modify**
- `…/embedded-configuration-api/.../facade/ConnectedUserProjectFacade.java` — add `prepareCopilotChat(...)`.
- `…/embedded-configuration-api/.../dto/CopilotChatContextDTO.java` — **new** record (workflowId + allowedComponentNames).
- `…/embedded-configuration-service/.../facade/ConnectedUserProjectFacadeImpl.java` — implement `prepareCopilotChat(...)`.
- `…/embedded-configuration-public-rest/build.gradle.kts` — add deps.

**Server — new**
- `…/embedded-configuration-public-rest/.../public_/web/rest/ConnectedUserCopilotApiController.java` — SSE `/chat` endpoint.
- `…/embedded-configuration-service/.../facade/ConnectedUserProjectFacadeCopilotChatTest.java` — facade unit test (the existing facade has no test class; this focused class tests only the new method).
- `…/embedded-configuration-public-rest/.../public_/web/rest/ConnectedUserCopilotApiControllerIntTest.java` — controller slice test.

**Client SDK — new** (`sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/`)
- `constants.ts` — minimal skeleton definition + endpoint path helpers.
- `api.ts` — `createSkeletonWorkflow(...)`.
- `store.ts` — zustand chat store + AG-UI subscriber wiring.
- `EmbeddedCopilotRuntimeProvider.tsx` — `@ag-ui/client` runtime.
- `thread.tsx` — vendored assistant-ui Thread (adapted, no ModeSwitch/ModelPicker).
- `dataComponents.tsx` — `ask-user-question` data-part.
- `EmbeddedWorkflowChat.tsx` — public component.
- `index.ts` — re-exports.
- `__tests__/EmbeddedWorkflowChat.test.tsx`, `__tests__/api.test.ts`.

**Client SDK — modify**
- `src/main.ts` — export `EmbeddedWorkflowChat` + props type.
- `package.json` — new dependencies.

**Sample app — new/modify** (`bytechef-embedded-sample-app/front-end`)
- `src/app/automations/chat/page.tsx` — **new** page.
- `src/lib/api.ts` — confirm/extend helper for navigation (reuse existing).
- `src/app/automations/page.tsx` — add "Generate from Chat" menu item.

---

## Task 1: Facade `prepareCopilotChat` + DTO

Adds the server method that authorizes the chat against the connected user, resolves the latest `workflowId`, and computes allowed components — reusing existing building blocks (`getConnectedUserProjectWorkflow` for the ownership/IDOR check, `projectWorkflowService.getLastWorkflowId`, and the package-private `resolveAllowedComponentNames`).

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/CopilotChatContextDTO.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeImpl.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeCopilotChatTest.java`

**Interfaces:**
- Consumes (existing): `ConnectedUserProjectFacade.getConnectedUserProjectWorkflow(String externalUserId, String workflowUuid, Long environmentId)`; `ProjectWorkflowService.getLastWorkflowId(String workflowUuid) -> String`; `ConnectedUserProjectFacadeImpl.resolveAllowedComponentNames(Environment) -> Set<String>` (package-private); `EnvironmentService.getEnvironment(...)`.
- Produces: `CopilotChatContextDTO(String workflowId, Set<String> allowedComponentNames)`; `ConnectedUserProjectFacade.prepareCopilotChat(String externalUserId, String workflowUuid, Environment environment) -> CopilotChatContextDTO`.

- [ ] **Step 1: Create the DTO record**

`CopilotChatContextDTO.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import java.util.Set;

/**
 * Server-resolved context for an embedded Copilot chat turn: the authoritative latest workflow id for the chat's
 * workflow uuid and the component names the connected user's embedded environment permits the agent to use.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record CopilotChatContextDTO(String workflowId, Set<String> allowedComponentNames) {
}
```

- [ ] **Step 2: Declare the facade method (write the failing test first)**

Create `ConnectedUserProjectFacadeCopilotChatTest.java` (EE header + `@version ee`). It tests the impl directly with mocked collaborators. Note: the impl constructor has many parameters; pass mocks for the ones used by the method under test and `null`/`Mockito.mock(...)` for the rest. Inspect the current constructor in `ConnectedUserProjectFacadeImpl` to fill the argument list.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectFacadeCopilotChatTest {

    @Test
    void testPrepareCopilotChatResolvesWorkflowIdAndAllowedComponents() {
        // Arrange a partially-mocked ConnectedUserProjectFacadeImpl. Stub:
        //   - getConnectedUserProjectWorkflow("ext-1", "uuid-1", 1L) -> a DTO (ownership check passes)
        //   - projectWorkflowService.getLastWorkflowId("uuid-1") -> "wf-99"
        //   - resolveAllowedComponentNames(Environment.PRODUCTION) -> Set.of("slack", "gmail")
        // Use Mockito spy on the impl so resolveAllowedComponentNames (package-private) can be stubbed,
        // or stub the underlying ConnectedUserService/IntegrationInstanceConfiguration collaborators it reads.

        ProjectWorkflowService projectWorkflowService = org.mockito.Mockito.mock(ProjectWorkflowService.class);

        when(projectWorkflowService.getLastWorkflowId(eq("uuid-1"))).thenReturn("wf-99");

        ConnectedUserProjectFacadeImpl facade = TestFacadeFactory.withAllowedComponents(
            projectWorkflowService, Set.of("slack", "gmail"));

        CopilotChatContextDTO context = facade.prepareCopilotChat("ext-1", "uuid-1", Environment.PRODUCTION);

        assertThat(context.workflowId()).isEqualTo("wf-99");
        assertThat(context.allowedComponentNames()).containsExactlyInAnyOrder("slack", "gmail");
    }
}
```

> Helper `TestFacadeFactory.withAllowedComponents(...)` is a tiny private static helper you add at the bottom of the test class that constructs the impl with a Mockito spy whose `resolveAllowedComponentNames` is stubbed and whose `getConnectedUserProjectWorkflow` returns a stub DTO. Implement it to match the current `ConnectedUserProjectFacadeImpl` constructor arity (read the constructor first).

- [ ] **Step 3: Run the test to verify it fails (method not yet declared)**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserProjectFacadeCopilotChatTest" -i`
Expected: compile failure / FAIL — `prepareCopilotChat` does not exist.

- [ ] **Step 4: Add the method to the facade interface**

In `ConnectedUserProjectFacade.java`, add the import `com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;` and the method (alphabetical position after `getConnectedUserProjectWorkflows`):

```java
    CopilotChatContextDTO prepareCopilotChat(String externalUserId, String workflowUuid, Environment environment);
```

- [ ] **Step 5: Implement the method in the impl**

In `ConnectedUserProjectFacadeImpl.java` (the impl already holds `projectWorkflowService`, `environmentService`, and the package-private `resolveAllowedComponentNames`):

```java
    @Override
    public CopilotChatContextDTO prepareCopilotChat(
        String externalUserId, String workflowUuid, Environment environment) {

        // Ownership / IDOR guard: throws if this workflow uuid is not owned by the connected user.
        getConnectedUserProjectWorkflow(externalUserId, workflowUuid, (long) environment.ordinal());

        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        return new CopilotChatContextDTO(workflowId, allowedComponentNames);
    }
```

Add imports `com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;` and `java.util.Set;` if not present.

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserProjectFacadeCopilotChatTest" -i`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/CopilotChatContextDTO.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacade.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeImpl.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserProjectFacadeCopilotChatTest.java
git commit -m "Add prepareCopilotChat facade for embedded Copilot chat

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: SSE `/copilot/chat` controller

Adds the only new server endpoint. Hand-written (SSE can't be code-generated). Mirrors `CopilotApiController.chat(...)` state injection but anchored on the embedded `workflowUuid` and frontend JWT auth.

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserCopilotApiController.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/build.gradle.kts`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserCopilotApiControllerIntTest.java`

**Interfaces:**
- Consumes: `ConnectedUserProjectFacade.prepareCopilotChat(...) -> CopilotChatContextDTO` (Task 1); `EnvironmentService.getEnvironment(String)`; `AgUiService.runAgent(LocalAgent, AgUiParameters) -> SseEmitter`; `List<LocalAgent>` (keyed by `getAgentId()`); `CopilotStateKeys.{STATE_TENANT_ID, STATE_AUTHENTICATION}`; `Mode`, `Source`; `TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY`.
- Produces: `POST /api/embedded/v1/automation/workflows/{workflowUuid}/copilot/chat` → `SseEmitter` (AG-UI event stream).

- [ ] **Step 1: Add build dependencies**

In `embedded-configuration-public-rest/build.gradle.kts`, inside `dependencies { … }` add (alongside the existing `implementation(...)` lines):

```kotlin
    implementation("org.springframework:spring-webmvc")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:servers:spring"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-tool"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
```

- [ ] **Step 2: Write the failing controller slice test**

Create `ConnectedUserCopilotApiControllerIntTest.java` (EE header + `@version ee`). Use the MockMvc web slice (the module already has `spring-boot-starter-webmvc-test` + `spring-security-test`). Mock `ConnectedUserProjectFacade`, `EnvironmentService`, `AgUiService`, and provide one `LocalAgent` whose `getAgentId()` is `"workflow_editor_build"`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
// Use the same web-slice setup the other *ApiControllerIntTest classes in this module use; if they
// use @SpringBootTest(webEnvironment = RANDOM_PORT), match that and adjust the request calls.
class ConnectedUserCopilotApiControllerIntTest {

    @Test
    @WithMockUser(username = "ext-user-1")
    void testCopilotChatAuthorizesResolvesStateAndRunsBuildAgent() throws Exception {
        // GIVEN prepareCopilotChat returns workflowId "wf-99" + allowed {"slack"}
        // WHEN POST .../workflows/uuid-1/copilot/chat with a minimal AgUiParameters body and X-Environment
        // THEN response content type is text/event-stream, prepareCopilotChat was called with ("ext-user-1","uuid-1",PRODUCTION),
        //      and agUiService.runAgent was invoked with the workflow_editor_build agent.
        //
        // Assertions to encode:
        //   verify(connectedUserProjectFacade).prepareCopilotChat("ext-user-1", "uuid-1", Environment.PRODUCTION);
        //   verify(agUiService).runAgent(eq(buildAgent), any(AgUiParameters.class));
        //   and capture the AgUiParameters to assert state contains:
        //     workflowId == "wf-99", mode == "BUILD", autonomous == false,
        //     allowedComponentNames == Set.of("slack"), STATE_TENANT_ID present.
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    void testCopilotChatReturns403ForForeignWorkflowUuid() throws Exception {
        // GIVEN prepareCopilotChat throws (ownership check fails)
        // WHEN POST .../workflows/foreign-uuid/copilot/chat
        // THEN status is 4xx (403) and agUiService.runAgent is never called.
    }
}
```

> Fill the test bodies using the same bootstrap style as the sibling `ConnectedUserProjectWorkflowApiControllerIntTest` (or whatever `*IntTest` exists in this module). Read one existing controller test in this module first and copy its `@SpringBootTest`/`MockMvc` wiring. Use `org.mockito.ArgumentCaptor<AgUiParameters>` to assert injected state.

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:testIntegration --tests "*ConnectedUserCopilotApiControllerIntTest" -i`
Expected: FAIL/compile error — controller does not exist.

- [ ] **Step 4: Implement the controller**

`ConnectedUserCopilotApiController.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Streams an interactive, BUILD-mode Copilot chat turn for an embedded connected user over the AG-UI protocol. The
 * conversation is anchored on a workflow uuid created up front by the SDK; this endpoint resolves the authoritative
 * workflow id, authorizes ownership, and injects tenant/principal/allowed-component state the agent's tools need.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ConnectedUserCopilotApiController {

    private final AgUiService agUiService;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final EnvironmentService environmentService;
    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI")
    public ConnectedUserCopilotApiController(
        AgUiService agUiService, ConnectedUserProjectFacade connectedUserProjectFacade,
        EnvironmentService environmentService, List<LocalAgent> localAgents) {

        this.agUiService = agUiService;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.environmentService = environmentService;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    @PostMapping(
        value = "/automation/workflows/{workflowUuid}/copilot/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public SseEmitter copilotChat(
        @PathVariable("workflowUuid") String workflowUuid, @RequestBody AgUiParameters agUiParameters,
        @RequestHeader(value = "X-Environment", required = false) @Nullable EnvironmentModel xEnvironment) {

        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");
        Environment environment = getEnvironment(xEnvironment);

        CopilotChatContextDTO context =
            connectedUserProjectFacade.prepareCopilotChat(externalUserId, workflowUuid, environment);

        State state = agUiParameters.getState();
        Map<String, Object> stateMap = state.getState();

        // Server-authoritative state — never trust client-supplied values.
        stateMap.put("workflowId", context.workflowId());
        stateMap.put("mode", Mode.BUILD.name());
        stateMap.put("autonomous", false);
        stateMap.put(CopilotStateKeys.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null) {
            stateMap.put(CopilotStateKeys.STATE_AUTHENTICATION, authentication);
        }

        Set<String> allowedComponentNames = context.allowedComponentNames();

        if (allowedComponentNames != null && !allowedComponentNames.isEmpty()) {
            stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        String agentId = (Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name()).toLowerCase();

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            throw new IllegalStateException("Workflow editor BUILD agent not available: " + agentId);
        }

        return agUiService.runAgent(localAgent, agUiParameters);
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(@Nullable EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
```

> If `jakarta.annotation.Nullable` is not on the classpath here, use `org.jspecify.annotations.Nullable` (as `CopilotApiController` does) and add the import accordingly.

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:testIntegration --tests "*ConnectedUserCopilotApiControllerIntTest" -i`
Expected: PASS.

- [ ] **Step 6: Compile the module + format**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest:compileJava spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/build.gradle.kts \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/main/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserCopilotApiController.java \
        server/ee/libs/embedded/embedded-configuration/embedded-configuration-public-rest/src/test/java/com/bytechef/ee/embedded/configuration/public_/web/rest/ConnectedUserCopilotApiControllerIntTest.java
git commit -m "Add embedded Copilot SSE chat endpoint

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: SDK dependencies, constants, and skeleton-create API

Adds the new runtime dependencies and the small, unit-testable pieces: the skeleton definition constant, path helpers, and the `createSkeletonWorkflow` fetch call.

**Files:**
- Modify: `sdks/frontend/embedded/library/react/package.json`
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/constants.ts`
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/api.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/__tests__/api.test.ts`

**Interfaces:**
- Produces:
  - `DEFAULT_BASE_URL: string`, `SKELETON_WORKFLOW_DEFINITION: string`, `copilotChatUrl(baseUrl, workflowUuid): string`, `createWorkflowUrl(baseUrl): string` (in `constants.ts`).
  - `createSkeletonWorkflow(params: {baseUrl: string; environment: string; jwtToken: string}): Promise<string>` returning the new `workflowUuid` (in `api.ts`).

- [ ] **Step 1: Add dependencies to package.json**

Add to `dependencies` (create the block if absent; keep keys alphabetical). Use versions matching the web client / automation-chat SDK (`@ag-ui/client` ^0.0.54, `@ag-ui/core` ^0.0.54, `@assistant-ui/react` ^0.14.12, `@assistant-ui/react-markdown` ^0.14.1, `zustand` ^5, `lucide-react`, `tailwind-merge`). Verify exact versions by reading `sdks/frontend/automation/chat/library/package.json` and matching them.

```json
    "dependencies": {
        "@ag-ui/client": "^0.0.54",
        "@ag-ui/core": "^0.0.54",
        "@assistant-ui/react": "^0.14.12",
        "@assistant-ui/react-markdown": "^0.14.1",
        "lucide-react": "^0.400.0",
        "tailwind-merge": "^2.5.0",
        "zustand": "^5.0.14"
    },
```

Then run `cd sdks/frontend/embedded/library/react && npm install`.

- [ ] **Step 2: Create constants.ts**

```ts
export const DEFAULT_BASE_URL = 'https://app.bytechef.io';

// Minimal valid skeleton. The agent overwrites it on the first BUILD turn; it only needs to be a
// definition the create endpoint accepts so we get a workflowUuid to anchor the conversation.
export const SKELETON_WORKFLOW_DEFINITION = JSON.stringify({
    description: '',
    label: 'New Workflow',
    tasks: [],
});

export const createWorkflowUrl = (baseUrl: string): string => `${baseUrl}/api/embedded/v1/automation/workflows`;

export const copilotChatUrl = (baseUrl: string, workflowUuid: string): string =>
    `${baseUrl}/api/embedded/v1/automation/workflows/${workflowUuid}/copilot/chat`;
```

> Confirm the skeleton shape the create endpoint expects by checking `buildInitialDefinition` in `ConnectedUserProjectFacadeImpl` (server). Match its required top-level keys.

- [ ] **Step 3: Write the failing api.test.ts**

```ts
import {afterEach, describe, expect, it, vi} from 'vitest';
import {createSkeletonWorkflow} from '../api';

describe('createSkeletonWorkflow', () => {
    afterEach(() => vi.restoreAllMocks());

    it('POSTs the skeleton definition with auth + environment headers and returns the workflowUuid', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            text: () => Promise.resolve('uuid-123'),
        });

        vi.stubGlobal('fetch', fetchMock);

        const workflowUuid = await createSkeletonWorkflow({
            baseUrl: 'https://app.example.com',
            environment: 'PRODUCTION',
            jwtToken: 'jwt-abc',
        });

        expect(workflowUuid).toBe('uuid-123');

        const [url, init] = fetchMock.mock.calls[0];

        expect(url).toBe('https://app.example.com/api/embedded/v1/automation/workflows');
        expect(init.method).toBe('POST');
        expect(init.headers.Authorization).toBe('Bearer jwt-abc');
        expect(init.headers['X-Environment']).toBe('PRODUCTION');
    });

    it('throws when the response is not ok', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok: false, status: 401, text: () => Promise.resolve('')}));

        await expect(
            createSkeletonWorkflow({baseUrl: 'https://x', environment: 'PRODUCTION', jwtToken: 'bad'})
        ).rejects.toThrow();
    });
});
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `cd sdks/frontend/embedded/library/react && npx vitest run src/components/embedded-workflow-chat/__tests__/api.test.ts`
Expected: FAIL — `../api` not found.

- [ ] **Step 5: Implement api.ts**

```ts
import {SKELETON_WORKFLOW_DEFINITION, createWorkflowUrl} from './constants';

interface CreateSkeletonWorkflowParamsI {
    baseUrl: string;
    environment: string;
    jwtToken: string;
}

export const createSkeletonWorkflow = async ({
    baseUrl,
    environment,
    jwtToken,
}: CreateSkeletonWorkflowParamsI): Promise<string> => {
    const response = await fetch(createWorkflowUrl(baseUrl), {
        body: JSON.stringify({definition: SKELETON_WORKFLOW_DEFINITION}),
        headers: {
            Authorization: `Bearer ${jwtToken}`,
            'Content-Type': 'application/json',
            'X-Environment': environment,
        },
        method: 'POST',
    });

    if (!response.ok) {
        throw new Error(`Failed to create workflow: ${response.status}`);
    }

    return (await response.text()).replaceAll('"', '').trim();
};
```

> The create endpoint returns the uuid as a JSON string body (it's `ResponseEntity<String>`); strip surrounding quotes. Verify the exact response shape against `createFrontendProjectWorkflow` and adjust the parse if it returns a raw string.

- [ ] **Step 6: Run the test to verify it passes**

Run: `cd sdks/frontend/embedded/library/react && npx vitest run src/components/embedded-workflow-chat/__tests__/api.test.ts`
Expected: PASS (both cases).

- [ ] **Step 7: Commit**

```bash
git add sdks/frontend/embedded/library/react/package.json \
        sdks/frontend/embedded/library/react/package-lock.json \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/constants.ts \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/api.ts \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/__tests__/api.test.ts
git commit -m "1051 client - Add embedded Copilot chat SDK deps + skeleton-create API

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

> Replace `1051` with the active ticket number if one is assigned; otherwise drop the number and use `client - …`.

---

## Task 4: Chat store + AG-UI runtime provider

Vendors the AG-UI streaming runtime into the SDK, adapted from `client/src/shared/components/copilot/runtime-providers/CopilotRuntimeProvider.tsx`. State payload is minimal (`workflowUuid`, `mode: 'BUILD'`); the server fills the rest.

**Files:**
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/store.ts`
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/EmbeddedCopilotRuntimeProvider.tsx`

**Interfaces:**
- Consumes: `copilotChatUrl(baseUrl, workflowUuid)` (Task 3); `HttpAgent` from `@ag-ui/client`; `AgentSubscriber` from `@ag-ui/core`; `useExternalStoreRuntime`, `AssistantRuntimeProvider` from `@assistant-ui/react`.
- Produces:
  - `createChatStore()` → a zustand store with `{messages, conversationId, appendToLastAssistantMessage, addMessage, resetMessages, generateConversationId}` (shape mirrors web `useCopilotStore`).
  - `<EmbeddedCopilotRuntimeProvider baseUrl jwtToken environment workflowUuid onRunFinished>{children}</EmbeddedCopilotRuntimeProvider>`.

- [ ] **Step 1: Implement store.ts**

Port the message/conversation slice of `client/src/shared/components/copilot/stores/useCopilotStore.ts` — keep only `messages`, `conversationId`, `addMessage`, `appendToLastAssistantMessage`, `resetMessages`, `generateConversationId`. Drop `context`, `selectedLlm*`, `savedState`. Use `ThreadMessageLike` from `@assistant-ui/react` for message typing. (Read the web store first; reproduce its `appendToLastAssistantMessage` semantics exactly.)

- [ ] **Step 2: Implement EmbeddedCopilotRuntimeProvider.tsx**

Adapt `CopilotRuntimeProvider.tsx`:
- `new HttpAgent({agentId: 'workflow_editor', headers: {Authorization: 'Bearer ' + jwtToken, 'X-Environment': environment}, threadId: conversationId, url: copilotChatUrl(baseUrl, workflowUuid)})`.
- Per turn: `agent.setState({mode: 'BUILD', workflowUuid})` then `agent.runAgent({runId: getRandomId()}, subscriber)`.
- `AgentSubscriber`: `onTextMessageContentEvent` → `appendToLastAssistantMessage`; `onToolCall*` per the web pattern; `onRunErrorEvent` → humanize (strip `com.…` FQCNs) and append; `onRunFinishedEvent` → call `onRunFinished()`.
- Wrap children in `AssistantRuntimeProvider` with `useExternalStoreRuntime` bound to the store.
- Remove all workspace/environmentId/model-picker/state-contributor-registry code — none applies in embedded.

> This is an adaptation of an existing file, not new invention. Keep variable names descriptive; sort named imports alphabetically; `useRef` vars end in `Ref`.

- [ ] **Step 3: Typecheck**

Run: `cd sdks/frontend/embedded/library/react && npm run typecheck` (or `tsc --noEmit` if no script).
Expected: no errors in the new files.

- [ ] **Step 4: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/store.ts \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/EmbeddedCopilotRuntimeProvider.tsx
git commit -m "client - Add embedded Copilot AG-UI runtime + chat store

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Vendored Thread UI + data components

Vendors a focused assistant-ui Thread and the `ask-user-question` data-part. No ModeSwitch, no ModelPicker.

**Files:**
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/thread.tsx`
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/dataComponents.tsx`

**Interfaces:**
- Consumes: `@assistant-ui/react` primitives; `@assistant-ui/react-markdown`.
- Produces: `Thread` (props: `{suggestions?: string[]; dataComponents?: Record<string, DataMessagePartComponent>}`); `embeddedChatDataComponents` (`{'ask-user-question': …}`).

- [ ] **Step 1: Port thread.tsx**

Copy `sdks/frontend/automation/chat/library/src/components/assistant-ui/thread.tsx` into the new path and trim to what's needed: message list, markdown text, composer with send/cancel + attachments, optional suggestions. Remove voice (`MicButton`, voice props) and any automation-chat-specific store coupling — this Thread renders from the assistant-ui runtime context provided by Task 4. Use `twMerge` for class merging; icons with the `Icon` suffix.

> Prefer porting the automation-chat Thread (already SDK-shaped, no app-internal imports) over the web client Thread (which imports `@/` app paths).

- [ ] **Step 2: Implement dataComponents.tsx**

```tsx
import type {DataMessagePartProps} from '@assistant-ui/react';

interface AskUserQuestionDataI {
    question: string;
    options?: string[];
}

const AskUserQuestionMessage = ({data}: DataMessagePartProps<AskUserQuestionDataI>) => (
    <div className="rounded-md border border-border p-3 text-sm">{data.question}</div>
);

export const embeddedChatDataComponents = {
    'ask-user-question': (props: DataMessagePartProps<AskUserQuestionDataI>) => <AskUserQuestionMessage {...props} />,
};
```

> Match the real `ask-user-question` data shape by reading `client/src/shared/components/ai-chat/messages/aiChatDataComponents.tsx` and its `AskUserQuestionMessage`; reproduce the relevant fields. Keep v1 minimal (render the question + options as plain text/buttons that submit back through the composer). Connection pickers are out of scope (see spec non-goals).

- [ ] **Step 3: Typecheck**

Run: `cd sdks/frontend/embedded/library/react && npm run typecheck`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/thread.tsx \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/dataComponents.tsx
git commit -m "client - Add embedded Copilot chat Thread UI + data components

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: `EmbeddedWorkflowChat` public component + export

Wires it together: create skeleton on mount, render Thread once `workflowUuid` resolves, fire `onWorkflowReady` once on first run-finish.

**Files:**
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/EmbeddedWorkflowChat.tsx`
- Create: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/index.ts`
- Modify: `sdks/frontend/embedded/library/react/src/main.ts`
- Test: `sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/__tests__/EmbeddedWorkflowChat.test.tsx`

**Interfaces:**
- Consumes: `createSkeletonWorkflow` (Task 3); `EmbeddedCopilotRuntimeProvider` (Task 4); `Thread`, `embeddedChatDataComponents` (Task 5); `DEFAULT_BASE_URL` (Task 3).
- Produces: `EmbeddedWorkflowChat` (default export of `index.ts`) + `EmbeddedWorkflowChatPropsI`.

- [ ] **Step 1: Write the failing component test**

```tsx
import {render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import EmbeddedWorkflowChat from '../EmbeddedWorkflowChat';

const {createSkeletonWorkflowMock} = vi.hoisted(() => ({createSkeletonWorkflowMock: vi.fn()}));

vi.mock('../api', () => ({createSkeletonWorkflow: createSkeletonWorkflowMock}));
vi.mock('../EmbeddedCopilotRuntimeProvider', () => ({
    EmbeddedCopilotRuntimeProvider: ({children}: {children: React.ReactNode}) => <div data-testid="runtime">{children}</div>,
}));
vi.mock('../thread', () => ({Thread: () => <div data-testid="thread" />}));

describe('EmbeddedWorkflowChat', () => {
    afterEach(() => vi.clearAllMocks());

    it('creates a skeleton workflow on mount then renders the Thread', async () => {
        createSkeletonWorkflowMock.mockResolvedValue('uuid-xyz');

        render(<EmbeddedWorkflowChat environment="PRODUCTION" jwtToken="jwt-1" />);

        await waitFor(() => expect(screen.getByTestId('thread')).toBeInTheDocument());

        expect(createSkeletonWorkflowMock).toHaveBeenCalledWith(
            expect.objectContaining({environment: 'PRODUCTION', jwtToken: 'jwt-1'})
        );
    });
});
```

> Follow the `vi.hoisted` mock-factory pattern (per CLAUDE.md) so the mock refs exist before the `vi.mock` factories run.

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd sdks/frontend/embedded/library/react && npx vitest run src/components/embedded-workflow-chat/__tests__/EmbeddedWorkflowChat.test.tsx`
Expected: FAIL — component not found.

- [ ] **Step 3: Implement EmbeddedWorkflowChat.tsx**

```tsx
import {useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {createSkeletonWorkflow} from './api';
import {DEFAULT_BASE_URL} from './constants';
import {EmbeddedCopilotRuntimeProvider} from './EmbeddedCopilotRuntimeProvider';
import {embeddedChatDataComponents} from './dataComponents';
import {Thread} from './thread';

export interface EmbeddedWorkflowChatPropsI {
    baseUrl?: string;
    className?: string;
    description?: string;
    environment?: 'DEVELOPMENT' | 'PRODUCTION' | 'STAGING';
    jwtToken: string;
    onWorkflowReady?: (workflowUuid: string) => void;
    suggestions?: string[];
    title?: string;
}

const EmbeddedWorkflowChat = ({
    baseUrl = DEFAULT_BASE_URL,
    className,
    environment = 'PRODUCTION',
    jwtToken,
    onWorkflowReady,
    suggestions,
}: EmbeddedWorkflowChatPropsI) => {
    const [workflowUuid, setWorkflowUuid] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    const readyFiredRef = useRef(false);

    useEffect(() => {
        let cancelled = false;

        createSkeletonWorkflow({baseUrl, environment, jwtToken})
            .then((uuid) => {
                if (!cancelled) {
                    setWorkflowUuid(uuid);
                }
            })
            .catch((createError) => {
                if (!cancelled) {
                    setError(createError instanceof Error ? createError.message : 'Failed to start chat');
                }
            });

        return () => {
            cancelled = true;
        };
    }, [baseUrl, environment, jwtToken]);

    if (error) {
        return <div className={twMerge('p-4 text-sm text-red-600', className)}>{error}</div>;
    }

    if (!workflowUuid) {
        return <div className={twMerge('p-4 text-sm', className)}>Starting…</div>;
    }

    return (
        <div className={twMerge('flex h-full flex-col', className)}>
            <EmbeddedCopilotRuntimeProvider
                baseUrl={baseUrl}
                environment={environment}
                jwtToken={jwtToken}
                onRunFinished={() => {
                    if (!readyFiredRef.current) {
                        readyFiredRef.current = true;

                        onWorkflowReady?.(workflowUuid);
                    }
                }}
                workflowUuid={workflowUuid}
            >
                <Thread dataComponents={embeddedChatDataComponents} suggestions={suggestions} />
            </EmbeddedCopilotRuntimeProvider>
        </div>
    );
};

export default EmbeddedWorkflowChat;
```

- [ ] **Step 4: Create index.ts**

```ts
export {default} from './EmbeddedWorkflowChat';
export type {EmbeddedWorkflowChatPropsI} from './EmbeddedWorkflowChat';
```

- [ ] **Step 5: Export from main.ts**

Add to `src/main.ts` (keep statements grouped with the existing imports/exports):

```ts
import EmbeddedWorkflowChat from './components/embedded-workflow-chat';

export {EmbeddedWorkflowChat};
export type {EmbeddedWorkflowChatPropsI} from './components/embedded-workflow-chat';
```

- [ ] **Step 6: Run the component test + full check**

Run: `cd sdks/frontend/embedded/library/react && npx vitest run src/components/embedded-workflow-chat && npm run check`
Expected: tests PASS; lint + typecheck clean. Fix any sort-keys / import-order / naming lint errors manually (ESLint `--fix` does not auto-fix `sort-keys`).

- [ ] **Step 7: Build the library**

Run: `cd sdks/frontend/embedded/library/react && npm run build`
Expected: BUILD succeeds; `EmbeddedWorkflowChat` present in `dist/main.d.ts`.

- [ ] **Step 8: Commit**

```bash
git add sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/EmbeddedWorkflowChat.tsx \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/index.ts \
        sdks/frontend/embedded/library/react/src/components/embedded-workflow-chat/__tests__/EmbeddedWorkflowChat.test.tsx \
        sdks/frontend/embedded/library/react/src/main.ts
git commit -m "client - Add EmbeddedWorkflowChat component + export

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Sample app "Generate from Chat" page + menu item

Adds the demo page and the dropdown entry beside "Generate from Prompt". The sample app lives in the sibling repo `bytechef-samples/bytechef-embedded-sample-app/front-end`.

**Files:**
- Create: `bytechef-samples/bytechef-embedded-sample-app/front-end/src/app/automations/chat/page.tsx`
- Modify: `bytechef-samples/bytechef-embedded-sample-app/front-end/src/app/automations/page.tsx`

**Interfaces:**
- Consumes: `EmbeddedWorkflowChat` from `@bytechef/embedded-react` (Task 6); the sample app's existing JWT/baseUrl/environment config and post-generate navigation (as used by `generate-workflow-dialog.tsx`).

- [ ] **Step 1: Confirm how the sample app gets jwtToken/baseUrl/environment + how it consumes the SDK**

Read `bytechef-samples/bytechef-embedded-sample-app/front-end/src/lib/api.ts` and `generate-workflow-dialog.tsx` to find how `generateWorkflow` obtains the token/baseUrl and how it navigates to the created workflow (`onWorkflowGenerated`). Reuse the same sources. If the SDK is linked via yalc (per project memory), ensure the new export is available: `cd sdks/frontend/embedded/library/react && npm run build && yalc push`.

- [ ] **Step 2: Create the page**

`src/app/automations/chat/page.tsx`:

```tsx
'use client';

import {useRouter} from 'next/navigation';
import {EmbeddedWorkflowChat} from '@bytechef/embedded-react';
// Reuse the sample app's existing config source for these (see generate-workflow-dialog.tsx / lib/api.ts).
import {BASE_URL, ENVIRONMENT, JWT_TOKEN} from '@/lib/config';

export default function GenerateFromChatPage() {
    const router = useRouter();

    return (
        <div className="mx-auto flex h-[calc(100vh-8rem)] max-w-2xl flex-col">
            <EmbeddedWorkflowChat
                baseUrl={BASE_URL}
                environment={ENVIRONMENT}
                jwtToken={JWT_TOKEN}
                onWorkflowReady={(workflowUuid) => router.push(`/automations/${workflowUuid}`)}
            />
        </div>
    );
}
```

> Replace `BASE_URL`/`ENVIRONMENT`/`JWT_TOKEN`/`@/lib/config` and the `router.push` target with the sample app's actual config accessors and route shape discovered in Step 1.

- [ ] **Step 3: Add the menu item**

In `src/app/automations/page.tsx`, beside the existing "Generate from Prompt" item (around line 141), add:

```tsx
                <DropdownMenuItem onClick={() => router.push('/automations/chat')}>
                  <MessageSquareIcon className="h-4 w-4 mr-2" />
                  Generate from Chat
                </DropdownMenuItem>
```

Add `MessageSquareIcon` to the `lucide-react` import (alphabetical) and ensure `useRouter` is available in this file (it may already be).

- [ ] **Step 4: Run the sample app and verify manually**

Run: `cd bytechef-samples/bytechef-embedded-sample-app/front-end && npm run dev`
Verify: the Automations dropdown shows "Generate from Chat"; clicking it opens the chat page; sending a message streams a response; on completion the app navigates to the new workflow.

- [ ] **Step 5: Commit (in the sample app repo)**

```bash
cd bytechef-samples/bytechef-embedded-sample-app
git add front-end/src/app/automations/chat/page.tsx front-end/src/app/automations/page.tsx
git commit -m "Add Generate from Chat page + menu item

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

> The sample app is a separate git repo — commit there, not in the main `bytechef` repo.

---

## Self-review

**Spec coverage**
- AG-UI transport end-to-end → Tasks 2 (server SSE via `agUiService.runAgent`), 4 (`@ag-ui/client` runtime). ✓
- Skeleton up front via existing `createFrontendProjectWorkflow` → Task 3 (`createSkeletonWorkflow`), Task 6 (mount). ✓
- Same `workflowUuid` refined; server-authoritative `workflowId` per turn → Task 1 (`prepareCopilotChat`), Task 2 (state override). ✓
- BUILD-only, no ModeSwitch/ModelPicker → Task 2 (`mode=BUILD` fixed), Task 4/5 (omitted). ✓
- New dedicated controller under `/api/embedded/v1`, frontend-only, `@CrossOrigin` → Task 2. ✓
- Vendored Thread + AG-UI runtime in `@bytechef/embedded-react` → Tasks 4–6. ✓
- Auth: JWT→externalUserId, IDOR guard, tenant + `STATE_AUTHENTICATION` injection → Tasks 1–2. ✓
- v1 scope cut (connection pickers out; `ask-user-question` in) → Task 5. ✓
- Demo page + "Generate from Chat" menu item → Task 7. ✓
- Tests: facade unit, controller slice, SDK api + component → Tasks 1, 2, 3, 6. ✓
- EE header + `@version ee` on new server files → Global Constraints + each server task. ✓
- Error handling (skeleton-create failure page state, foreign uuid 403, humanized run error) → Task 6, Task 2, Task 4. ✓

**Note on `EmbeddedCopilotChatService`:** intentionally dropped in favor of inline controller logic (see "Deviation from spec"). The spec's "keep internals out of embedded module" goal is met differently — by reusing already-public state-key/`TaskTools` constants rather than a wrapper service.

**Type consistency:** `prepareCopilotChat` returns `CopilotChatContextDTO(String workflowId, Set<String> allowedComponentNames)` in Tasks 1 and 2; `createSkeletonWorkflow(params): Promise<string>` consistent in Tasks 3 and 6; `EmbeddedCopilotRuntimeProvider` props (`baseUrl`, `environment`, `jwtToken`, `workflowUuid`, `onRunFinished`) consistent in Tasks 4 and 6; `embeddedChatDataComponents` consistent in Tasks 5 and 6.

**Open items deferred to execution** (flagged inline, not placeholders): exact sample-app config accessors + route shape (Task 7 Step 1); exact create-endpoint response parse (Task 3 Step 5); exact `ask-user-question` data shape (Task 5 Step 2); matching the existing module's `*IntTest` bootstrap style (Task 2 Step 2).
