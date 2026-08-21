# Embedded Code Workflows Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the shipped embedded automation code-workflows feature along its three independently
shippable dimensions: distributed EE (real remote clients instead of throwing stubs), CLI (`bytechef embedded
code-workflow deploy|list`), and the admin console UI (kind badge, deploy action, read-only references panel).

**Architecture:** Distributed EE re-exposes the three real facade beans that already live in
`embedded-configuration-service` over `/remote/...` REST controllers hosted by `configuration-app`, following the
`automation-task-remote-rest` / `RemoteApprovalTaskFacadeClient` precedent exactly — only the methods the
webhook-app bridge controllers actually call get real implementations; every other method on the same three
interfaces stays a throwing stub. The CLI gets a new generated client module against the embedded-configuration
internal OpenAPI spec (the deploy endpoint lives under `/api/embedded/internal/...`, a different document than the
two client modules the CLI already has) plus a small server-side change so deploy returns trigger-validation
warnings instead of a bare 204. The admin UI adds a `codeWorkflowProject` boolean to the existing
`AutomationWorkflowProject` GraphQL type (the DTO already carries it), a multipart-upload deploy dialog modeled on
the existing custom-component upload flow, and a new admin-only references query that joins
`connected_user_project_workflow` back to the connected user owning each reference.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Spring GraphQL, Spring Shell (CLI), React 19.2 /
TypeScript 5.9 (client), JUnit 5, Mockito, AssertJ, MockRestServiceServer.

## Global Constraints

- Specs: `docs/superpowers/specs/2026-07-27-embedded-code-workflows-completion-design.md` (read before Task 1) and
  `docs/superpowers/specs/2026-07-27-embedded-automation-code-workflows-design.md` (the feature this plan
  completes — read for the bridge/reference-mode/dangling model this plan builds on top of).
- Three sub-projects, independently shippable, done here in dependency order: distributed EE (Tasks 1-3), CLI
  (Tasks 4-5), admin UI (Tasks 6-8). The admin UI depends on nothing from Tasks 1-5.
- Distributed EE: implement ONLY the methods the webhook-app bridge controllers (`RequestTriggerApiController`,
  `AppEventTriggerApiController`) actually invoke — `AutomationWorkflowProjectFacade.getPublishedProjects()` (the
  no-arg overload only), `ConnectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows` and
  `.getOrCreateReference`, `ConnectedUserProjectFacade.copyWorkflowTemplate`. Every other method on these three
  interfaces stays `throw new UnsupportedOperationException();` — do not implement methods nobody calls yet.
- The capability guard in `RequestTriggerApiController`/`AppEventTriggerApiController`
  (`AtomicBoolean automationBridgeUnsupportedLogged`, catch `UnsupportedOperationException`, log-once + degrade)
  stays untouched. It still protects every method this plan does not implement, and it is what makes this rollout
  safe on deployments where configuration-app is unreachable — remove nothing from it.
- EE code (`server/ee/`) uses the ByteChef Enterprise license header (not Apache 2.0), and every class gets the
  `@version ee` Javadoc tag. The header is selected by file CONTENT, not path — apply it to every file under
  `server/ee/`, including tests.
- CE code (client-side TypeScript, and any CLI code under `cli/`) uses no license header (matches existing files
  in those trees).
- Client conventions from CLAUDE.md apply throughout Tasks 7-8: ESLint `sort-keys` (object keys alphabetical),
  interface names ending `I`/`Props`, `twMerge` (never `cn()`), import-destructure alphabetical sort, hook
  ordering (`useState` → `useRef` → custom store hooks → other hooks → derived values → `useEffect` → `return`).
  Run `npm run check` (from `client/`) before committing any client change.
- Run `./gradlew spotlessApply` before every server-side commit. Check Gradle results by redirecting to a file and
  testing `$?` — a run piped into `tail` reports the filter's exit code, not Gradle's:
  `./gradlew <task> > /tmp/out.log 2>&1; echo $?`.
- Mockito gotcha: unstubbed wrapper-returning methods (`Long`, `Integer`) return `0`, not `null`. Stub
  `thenReturn(null)` explicitly wherever a null must be observed (relevant to Task 2's tests, which branch on a
  nullable `Environment`/reference fields).
- The generated OpenAPI interface at
  `embedded-configuration-rest-api/generated/src/main/java/.../AutomationProjectCodeWorkflowApi.java` has no wired
  Gradle regeneration task in this module (unlike the CLI clients, which document `generateClient` explicitly in
  their `build.gradle.kts`). Task 3 edits both `openapi.yaml` and this generated interface by hand, keeping them in
  sync — the same way this file has been hand-maintained since its last codegen pass.

---

## File Structure

**Created:**
- `.../embedded-configuration-remote-rest/.../remote/web/rest/facade/RemoteAutomationWorkflowProjectFacadeController.java`
- `.../embedded-configuration-remote-rest/.../remote/web/rest/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeController.java`
- `.../embedded-configuration-remote-rest/.../remote/web/rest/facade/RemoteConnectedUserProjectFacadeController.java`
- `.../embedded-configuration-remote-client/.../remote/client/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest.java`
- `cli/clients/embedded-configuration-internal/build.gradle.kts` — new Gradle module
- `cli/commands/embedded/.../EmbeddedCodeWorkflowCommand.java`
- `.../embedded-configuration-graphql/.../graphql/connected-user-code-workflow-reference.graphqls`
- `.../embedded-configuration-graphql/.../web/graphql/ConnectedUserCodeWorkflowReferenceGraphQlController.java`
- `client/src/graphql/embedded/configuration/connectedUserCodeWorkflowReferences.graphql`
- `client/src/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog.tsx`
- `client/src/ee/pages/embedded/automation-workflows/CodeWorkflowProjectDetail.tsx`
- `client/src/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel.tsx`

**Modified:**
- `.../embedded-configuration-remote-client/.../facade/RemoteAutomationWorkflowProjectFacadeClient.java`
- `.../embedded-configuration-remote-client/.../facade/RemoteConnectedUserCodeWorkflowReferenceFacadeClient.java`
- `.../embedded-configuration-remote-client/.../facade/RemoteConnectedUserProjectFacadeClient.java`
- `.../embedded-configuration-api/.../facade/AutomationWorkflowProjectCodeWorkflowFacade.java`
- `.../embedded-configuration-service/.../facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- `.../embedded-configuration-rest-impl/openapi.yaml`
- `.../embedded-configuration-rest-api/generated/.../AutomationProjectCodeWorkflowApi.java`
- `.../embedded-configuration-rest-impl/.../web/rest/AutomationProjectCodeWorkflowApiController.java`
- `cli/settings additions` → `settings.gradle.kts`
- `cli/commands/embedded/build.gradle.kts`
- `cli/README.md`
- `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md`
- `.../embedded-configuration-graphql/.../graphql/automation-workflow-project.graphqls`
- `client/src/graphql/embedded/configuration/automationWorkflowProjects.graphql`
- `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectListItem.tsx`
- `docs/content/docs/automation/*` embedded bridge page (drop degrade caveats) — see Task 8 note.

---

### Task 1: Remote-rest controllers exposing the three real facades

**Files:**
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-rest/src/main/java/com/bytechef/ee/embedded/configuration/remote/web/rest/facade/RemoteAutomationWorkflowProjectFacadeController.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-rest/src/main/java/com/bytechef/ee/embedded/configuration/remote/web/rest/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeController.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-rest/src/main/java/com/bytechef/ee/embedded/configuration/remote/web/rest/facade/RemoteConnectedUserProjectFacadeController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-rest/src/test/java/com/bytechef/ee/embedded/configuration/remote/web/rest/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest.java`

**Interfaces:**
- Consumes: `AutomationWorkflowProjectFacade` (`.getPublishedProjects()`), `ConnectedUserCodeWorkflowReferenceFacade`
  (`.getConnectedUserWorkflows`, `.getOrCreateReference`), `ConnectedUserProjectFacade`
  (`.copyWorkflowTemplate`) — all already real beans in `embedded-configuration-service`, already on
  `configuration-app`'s classpath (it depends on both `embedded-configuration-service` and
  `embedded-configuration-remote-rest`).
- Produces: `GET /remote/automation-workflow-project-facade/get-published-projects`,
  `GET /remote/connected-user-code-workflow-reference-facade/get-connected-user-workflows/{connectedUserId}`,
  `POST /remote/connected-user-code-workflow-reference-facade/get-or-create-reference`,
  `POST /remote/connected-user-project-facade/copy-workflow-template`. Task 2's remote clients call exactly
  these four paths.

`configuration-app`'s `build.gradle.kts` already lists
`:server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-rest` and
`:server:ee:libs:embedded:embedded-configuration:embedded-configuration-service` (lines 96 and 98) — no new
module wiring is needed, only new controller classes.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RemoteConnectedUserCodeWorkflowReferenceFacadeController.class)
class RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Test
    void testGetConnectedUserWorkflowsReturnsFacadeResult() throws Exception {
        when(connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(42L))
            .thenReturn(List.of(new ConnectedUserProjectWorkflow()));

        mockMvc.perform(get("/remote/connected-user-code-workflow-reference-facade/get-connected-user-workflows/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetOrCreateReferenceReturns409OnMissingConnection() throws Exception {
        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            eq("ext-1"), eq("uuid-1"), any(Environment.class)))
            .thenThrow(new MissingConnectionException("slack"));

        mockMvc.perform(
            post("/remote/connected-user-code-workflow-reference-facade/get-or-create-reference")
                .param("externalUserId", "ext-1")
                .param("catalogWorkflowUuid", "uuid-1")
                .param("environment", "PRODUCTION"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.missingConnectionComponentName").value("slack"));
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-rest:test --tests "*RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class RemoteConnectedUserCodeWorkflowReferenceFacadeController`.

- [ ] **Step 3: Write the three controllers**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link AutomationWorkflowProjectFacade} to remote callers. Only {@code getPublishedProjects()} is exposed —
 * the embedded-webhook bridge controllers are its only remote caller today, and they only invoke the no-arg overload.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/automation-workflow-project-facade")
public class RemoteAutomationWorkflowProjectFacadeController {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public RemoteAutomationWorkflowProjectFacadeController(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-published-projects",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<AutomationWorkflowProjectDTO>> getPublishedProjects() {
        return ResponseEntity.ok(automationWorkflowProjectFacade.getPublishedProjects());
    }
}
```

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link ConnectedUserCodeWorkflowReferenceFacade} to remote callers -- the embedded-webhook bridge's read and
 * provisioning path for automation-bridge references. {@link MissingConnectionException} is translated to HTTP 409
 * with the missing component name in the body, mirroring how {@code RequestTriggerApiController} already reports it
 * to its own caller, so {@link RemoteConnectedUserCodeWorkflowReferenceFacadeClient} can reconstruct the exception on
 * the other side of the wire instead of losing the distinction as a generic 5xx.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/connected-user-code-workflow-reference-facade")
public class RemoteConnectedUserCodeWorkflowReferenceFacadeController {

    private final ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserCodeWorkflowReferenceFacadeController(
        ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade) {

        this.connectedUserCodeWorkflowReferenceFacade = connectedUserCodeWorkflowReferenceFacade;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-connected-user-workflows/{connectedUserId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<ConnectedUserProjectWorkflow>> getConnectedUserWorkflows(
        @PathVariable long connectedUserId) {

        return ResponseEntity.ok(
            connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/get-or-create-reference",
        produces = {
            "application/json"
        })
    public ResponseEntity<?> getOrCreateReference(
        @RequestParam String externalUserId, @RequestParam String catalogWorkflowUuid,
        @RequestParam Environment environment) {

        try {
            ConnectedUserProjectWorkflow reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, catalogWorkflowUuid, environment);

            return ResponseEntity.ok(reference);
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        }
    }
}
```

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link ConnectedUserProjectFacade} to remote callers. Only {@code copyWorkflowTemplate} is exposed -- the
 * embedded-webhook bridge's implicit visual-template provisioning path is its sole remote caller today.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/connected-user-project-facade")
public class RemoteConnectedUserProjectFacadeController {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserProjectFacadeController(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/copy-workflow-template",
        produces = {
            "application/json"
        })
    public ResponseEntity<String> copyWorkflowTemplate(
        @RequestParam String externalUserId, @RequestParam String workflowUuid,
        @RequestParam Environment environment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.copyWorkflowTemplate(externalUserId, workflowUuid, environment));
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-rest:test --tests "*RemoteConnectedUserCodeWorkflowReferenceFacadeControllerTest*" > /tmp/t1.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-rest
git commit -m "Expose the automation-bridge facades over /remote for configuration-app

Only the four methods the embedded-webhook bridge controllers actually call are
exposed: getPublishedProjects(), getConnectedUserWorkflows,
getOrCreateReference, and copyWorkflowTemplate. MissingConnectionException
becomes an HTTP 409 with the missing component name in the body so the caller
can reconstruct it instead of losing the distinction as a generic 5xx."
```

---

### Task 2: Real remote-client implementations + wiring test

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client/src/main/java/com/bytechef/ee/embedded/configuration/remote/client/facade/RemoteAutomationWorkflowProjectFacadeClient.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client/src/main/java/com/bytechef/ee/embedded/configuration/remote/client/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeClient.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client/src/main/java/com/bytechef/ee/embedded/configuration/remote/client/facade/RemoteConnectedUserProjectFacadeClient.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client/src/test/java/com/bytechef/ee/embedded/configuration/remote/client/facade/RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest.java`

**Interfaces:**
- Consumes: the four `/remote/...` paths from Task 1.
- Produces: `RemoteAutomationWorkflowProjectFacadeClient.getPublishedProjects()`,
  `RemoteConnectedUserCodeWorkflowReferenceFacadeClient.getConnectedUserWorkflows`/`.getOrCreateReference`,
  `RemoteConnectedUserProjectFacadeClient.copyWorkflowTemplate` — all now return real data instead of throwing.
  This is what makes `RequestTriggerApiController`/`AppEventTriggerApiController`'s `try` blocks stop hitting
  `UnsupportedOperationException` for these four call sites in `webhook-app`; every other method on these three
  interfaces is untouched and keeps throwing.

- [ ] **Step 1: Write the failing wiring test**

This is a hermetic unit test — no Spring context, no real load-balancer bean — using `RestClient.builder()` bound
to `MockRestServiceServer` and wrapped directly in `LoadBalancedRestClient`, the same pattern
`AbstractRestClientHeadersTest` already uses to unit-test `AbstractRestClient` subclasses without a full context.

```java
package com.bytechef.ee.embedded.configuration.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest {

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId("public");
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testGetConnectedUserWorkflowsHitsTheRemoteRestPath() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-connected-user-workflows/42"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        List<ConnectedUserProjectWorkflow> result = client.getConnectedUserWorkflows(42L);

        assertThat(result).isEmpty();

        server.verify();
    }

    @Test
    void testGetOrCreateReferenceTranslates409IntoMissingConnectionException() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://configuration-app");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();

        server.expect(
            requestTo("http://configuration-app/remote/connected-user-code-workflow-reference-facade"
                + "/get-or-create-reference?externalUserId=ext-1&catalogWorkflowUuid=uuid-1&environment=PRODUCTION"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"missingConnectionComponentName\":\"slack\"}"));

        RemoteConnectedUserCodeWorkflowReferenceFacadeClient client =
            new RemoteConnectedUserCodeWorkflowReferenceFacadeClient(new LoadBalancedRestClient(builder));

        assertThatThrownBy(() -> client.getOrCreateReference("ext-1", "uuid-1", Environment.PRODUCTION))
            .isInstanceOf(MissingConnectionException.class)
            .hasFieldOrPropertyWithValue("componentName", "slack");
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client:test --tests "*RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest*" > /tmp/t2.log 2>&1; echo $?`
Expected: non-zero — both methods still throw `UnsupportedOperationException`, the mock server never gets called.

- [ ] **Step 3: Implement the four methods**

In `RemoteAutomationWorkflowProjectFacadeClient.java`, add the constants and replace `getPublishedProjects()`:

```java
    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String AUTOMATION_WORKFLOW_PROJECT_FACADE = "/remote/automation-workflow-project-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteAutomationWorkflowProjectFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(AUTOMATION_WORKFLOW_PROJECT_FACADE + "/get-published-projects")
                .build(),
            new ParameterizedTypeReference<>() {});
    }
```

(imports: `com.bytechef.ee.remote.client.LoadBalancedRestClient`, `org.springframework.core.ParameterizedTypeReference`)

In `RemoteConnectedUserCodeWorkflowReferenceFacadeClient.java`:

```java
    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE =
        "/remote/connected-user-code-workflow-reference-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserCodeWorkflowReferenceFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE + "/get-connected-user-workflows/"
                        + connectedUserId)
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        try {
            return loadBalancedRestClient.post(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(CONNECTED_USER_CODE_WORKFLOW_REFERENCE_FACADE + "/get-or-create-reference")
                    .queryParam("externalUserId", externalUserId)
                    .queryParam("catalogWorkflowUuid", catalogWorkflowUuid)
                    .queryParam("environment", environment)
                    .build(),
                null, ConnectedUserProjectWorkflow.class);
        } catch (HttpClientErrorException.Conflict conflict) {
            Map<String, String> body = conflict.getResponseBodyAs(new ParameterizedTypeReference<>() {});

            throw new MissingConnectionException(body.get("missingConnectionComponentName"));
        }
    }
```

(imports: `com.bytechef.ee.remote.client.LoadBalancedRestClient`, `org.springframework.core.ParameterizedTypeReference`,
`org.springframework.web.client.HttpClientErrorException`, `java.util.Map`)

Leave `deleteReference`, `enableReference`, `markDanglingReferences` on this class throwing — they have no
remote caller today.

In `RemoteConnectedUserProjectFacadeClient.java`:

```java
    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String CONNECTED_USER_PROJECT_FACADE = "/remote/connected-user-project-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserProjectFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public String copyWorkflowTemplate(String externalUserId, String workflowUuid, Environment environment) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(CONNECTED_USER_PROJECT_FACADE + "/copy-workflow-template")
                .queryParam("externalUserId", externalUserId)
                .queryParam("workflowUuid", workflowUuid)
                .queryParam("environment", environment)
                .build(),
            null, String.class);
    }
```

Leave the remaining 14 methods on this class throwing.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client:test --tests "*RemoteConnectedUserCodeWorkflowReferenceFacadeClientTest*" > /tmp/t2.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 5: Verify webhook-app still boots**

Run: `./gradlew :server:ee:apps:webhook-app:test --tests "*WebhookApplicationIntTest*" > /tmp/t2b.log 2>&1; echo $?`
Expected: `0` — confirms the now-implemented methods don't break dependency wiring, and the capability guard around
the still-throwing methods is unaffected (nothing in this task changes those code paths).

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration/embedded-configuration-remote-client
git commit -m "Implement the 4 automation-bridge remote-client methods webhook-app calls

getPublishedProjects, getConnectedUserWorkflows, getOrCreateReference, and
copyWorkflowTemplate now call configuration-app for real; every other method on
these three interfaces is untouched and still throws
UnsupportedOperationException, preserving the capability guard's log-once +
degrade behavior for topologies that never wire configuration-app in. 409 from
get-or-create-reference is translated back into MissingConnectionException so
RequestTriggerApiController's existing catch block keeps working unmodified."
```

---

### Task 3: Deploy endpoint returns trigger-validation warnings

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacade.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeImpl.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-api/generated/src/main/java/com/bytechef/ee/embedded/configuration/web/rest/AutomationProjectCodeWorkflowApi.java`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/src/main/java/com/bytechef/ee/embedded/configuration/web/rest/AutomationProjectCodeWorkflowApiController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/AutomationWorkflowProjectCodeWorkflowFacadeTest.java` (existing — extend)

**Interfaces:**
- Consumes: nothing new.
- Produces: `AutomationWorkflowProjectCodeWorkflowFacade.save(byte[], Language): List<String>` (was `void`); HTTP
  response for `POST /automation/projects/deploy` becomes `200` with body
  `AutomationProjectCodeWorkflowDeployResultModel { warnings: List<String> }` (was `204` empty). Task 4's CLI
  `deploy` command reads `warnings` and prints each one.

`warnIfNotPubliclyInvocable` in the impl today only logs a WARN; deploy-time trigger validation stays advisory
(never rejects the deploy), it just also becomes visible to the caller instead of only to server logs.

- [ ] **Step 1: Write the failing test**

Add to the existing `AutomationWorkflowProjectCodeWorkflowFacadeTest`. `fakeProjectDefinitionBytes("acme-billing")`
(the fixture `testFirstDeployCreatesTheCatalogProject` already uses) deploys a single "charge" workflow with tasks
but no triggers at all, so it already exercises the WARN-only code path today — this test just captures the
return value instead of only the log line:

```java
    @Test
    void testSaveReturnsWarningForWorkflowWithNoPubliclyInvocableTrigger() {
        Mockito.when(automationWorkflowProjectFacade.fetchProjectIdByName("acme-billing"))
            .thenReturn(Optional.empty());
        Mockito.when(automationWorkflowProjectFacade.createProject(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.isNull(), Mockito.eq(List.of()), Mockito.isNull()))
            .thenReturn(100L);

        Project project = new Project();

        project.setId(100L);

        Mockito.when(projectService.getProject(100L))
            .thenReturn(project);

        CodeWorkflowContainer container = codeWorkflowContainer(Map.of("charge", "wf-1"));

        Mockito.when(codeWorkflowContainerFacade.create(
            Mockito.eq("acme-billing"), Mockito.any(), Mockito.any(), Mockito.eq(Language.JAVASCRIPT),
            Mockito.any(), Mockito.eq(PlatformType.AUTOMATION)))
            .thenReturn(container);

        List<String> warnings = facade.save(fakeProjectDefinitionBytes("acme-billing"), Language.JAVASCRIPT);

        Assertions.assertEquals(1, warnings.size());
        Assertions.assertTrue(warnings.getFirst()
            .contains("will not be invocable"));
    }
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*AutomationWorkflowProjectCodeWorkflowFacadeTest*" > /tmp/t3.log 2>&1; echo $?`
Expected: non-zero — `save` still returns `void`.

- [ ] **Step 3: Change the facade signature and collect warnings**

In `AutomationWorkflowProjectCodeWorkflowFacade.java`:

```java
    List<String> save(byte[] bytes, Language language);
```

In `AutomationWorkflowProjectCodeWorkflowFacadeImpl.save`, change the return type to `List<String>` and change
only the trailing loop (every line before it — loading the definition, resolving/creating the catalog project,
carrying uuids forward, publishing, and computing `currentUuids`/`previousUuids` for
`markDanglingReferences` — is untouched):

```java
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> save(byte[] bytes, Language language) {
        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java code workflows is disabled",
                CodeWorkflowErrorType.JAVA_CODE_WORKFLOW_UPLOAD_DISABLED);
        }

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName(projectDefinition.getName())
            .orElseGet(() -> automationWorkflowProjectFacade.createProject(
                projectDefinition.getName(),
                projectDefinition.getDescription()
                    .orElse(null),
                null, List.of(), null));

        Project project = projectService.getProject(projectId);

        Map<String, UUID> previousWorkflowUuidsByName = fetchPreviousWorkflowUuidsByName(project.getId());

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(), language,
            bytes, PlatformType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        for (Map.Entry<String, String> entry : codeWorkflowContainer.getWorkflowNameIds()
            .entrySet()) {

            ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
                project.getId(), project.getLastProjectVersion(), entry.getValue());

            UUID previousUuid = previousWorkflowUuidsByName.get(entry.getKey());

            if (previousUuid != null) {
                projectWorkflow.setUuid(previousUuid);

                projectWorkflowService.update(projectWorkflow);
            }
        }

        projectService.publishProject(project.getId(), null, false);

        Set<String> currentUuids = projectWorkflowService
            .getProjectWorkflows(project.getId(), project.getLastProjectVersion())
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Set<String> previousUuids = previousWorkflowUuidsByName.values()
            .stream()
            .map(UUID::toString)
            .collect(Collectors.toSet());

        connectedUserCodeWorkflowReferenceFacade.markDanglingReferences(project.getId(), previousUuids, currentUuids);

        List<String> warnings = new ArrayList<>();

        for (WorkflowDefinition workflowDefinition : projectDefinition.getWorkflows()) {
            warnIfNotPubliclyInvocable(projectDefinition.getName(), workflowDefinition, warnings);
        }

        return warnings;
    }
```

(add import `java.util.ArrayList`)

Change `warnIfNotPubliclyInvocable` to append to the passed-in list in addition to logging:

```java
    private void warnIfNotPubliclyInvocable(
        String projectName, WorkflowDefinition workflowDefinition, List<String> warnings) {

        List<? extends TriggerDefinition> triggerDefinitions = workflowDefinition.getTriggers()
            .orElseGet(List::of);

        boolean publiclyInvocable = triggerDefinitions.stream()
            .anyMatch(AutomationWorkflowProjectCodeWorkflowFacadeImpl::isPubliclyInvocableTrigger);

        if (!publiclyInvocable) {
            String warning = "Workflow '%s' in deployed automation code workflow project '%s' declares neither a "
                + "request trigger nor an app-event trigger; it will not be invocable through the embedded public "
                + "endpoints".formatted(workflowDefinition.getName(), projectName);

            log.warn(warning);

            warnings.add(warning);
        }
    }
```

- [ ] **Step 4: Update the OpenAPI spec and generated interface**

In `openapi.yaml`, replace the `deployAutomationProjectCodeWorkflow` response and add the schema:

```yaml
      responses:
        "200":
          description: "Successful operation."
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/AutomationProjectCodeWorkflowDeployResult"
```

Add the schema alongside the other embedded-configuration schemas:

```yaml
    AutomationProjectCodeWorkflowDeployResult:
      type: "object"
      description: "The result of deploying an automation code workflow into the embedded catalog."
      properties:
        warnings:
          description: "Deploy-time trigger validation warnings; a deployed workflow is still usable even with warnings."
          type: "array"
          items:
            type: "string"
```

In the generated `AutomationProjectCodeWorkflowApi.java`, hand-edit the default method's return type and add the
model import (mirroring the shape of every other `*Model`-returning operation already in this package):

```java
    default ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        @Parameter(name = "projectFile", description = "The file of a code-native automation project.") @RequestPart(value = "projectFile", required = false) MultipartFile projectFile
    ) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
```

(add `import com.bytechef.ee.embedded.configuration.web.rest.model.AutomationProjectCodeWorkflowDeployResultModel;`
and hand-write the small generated-style model record/class alongside the other `*Model` classes in the same
`generated/.../model` directory, following their existing shape.)

- [ ] **Step 5: Update the controller**

```java
    @Override
    public ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        MultipartFile projectFile) {

        List<String> warnings;

        try {
            warnings = automationWorkflowProjectCodeWorkflowFacade.save(
                projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(new AutomationProjectCodeWorkflowDeployResultModel().warnings(warnings));
    }
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*AutomationWorkflowProjectCodeWorkflowFacadeTest*" :server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl:compileJava > /tmp/t3.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Return deploy-time trigger warnings from the code-workflow deploy endpoint

Deploy stays advisory -- a workflow missing a request/app-event trigger still
deploys -- but the warning is now returned to the caller instead of only logged
server-side, so the admin UI and CLI can surface it instead of a silent 204."
```

---

### Task 4: CLI internal client module and `embedded code-workflow deploy`

**Files:**
- Create: `cli/clients/embedded-configuration-internal/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `cli/commands/embedded/build.gradle.kts`
- Create: `cli/commands/embedded/src/main/java/com/bytechef/cli/command/embedded/EmbeddedCodeWorkflowCommand.java`
- Modify: `cli/commands/embedded/src/main/java/com/bytechef/cli/command/embedded/EmbeddedConfigurationClientFactory.java`
- Test: `cli/commands/embedded/src/test/java/com/bytechef/cli/command/embedded/EmbeddedCodeWorkflowCommandTest.java`

**Interfaces:**
- Consumes: `POST /api/embedded/internal/automation/projects/deploy` (Task 3's response shape).
- Produces: `bytechef embedded code-workflow deploy --file <artifact> [--language JAVA|JS|PYTHON|RUBY]`.

The existing `cli/clients/embedded-configuration` module targets the PUBLIC spec
(`embedded-configuration-public-rest/openapi.yaml`), which has no `/internal/...` paths. The deploy endpoint lives
in a different document (`embedded-configuration-rest-impl/openapi.yaml`, server url `/api/embedded/internal`), so
a sibling client module is needed — mirroring the `automation-configuration` / `embedded-configuration` module
pair exactly, just pointed at the internal spec.

- [ ] **Step 1: Register the new client module**

In `settings.gradle.kts`, beside the existing three `cli:clients:*` entries:

```kotlin
include("cli:clients:embedded-configuration-internal")
```

- [ ] **Step 2: Add the module's build file**

```kotlin
plugins {
    id("com.bytechef.java-library-conventions")
    alias(libs.plugins.org.openapi.generator)
}

val generateClient by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("java")
    library.set("native")
    inputSpec.set("${rootDir}/server/ee/libs/embedded/embedded-configuration/embedded-configuration-rest/embedded-configuration-rest-impl/openapi.yaml")
    outputDir.set("$projectDir/generated")
    apiPackage.set("com.bytechef.cli.client.embeddedconfigurationinternal.api")
    modelPackage.set("com.bytechef.cli.client.embeddedconfigurationinternal.model")
    invokerPackage.set("com.bytechef.cli.client.embeddedconfigurationinternal")
    modelNameSuffix.set("Model")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useTags" to "true",
            "hideGenerationTimestamp" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

// Generated client sources are committed; regenerate manually with the `generateClient` task
// when openapi.yaml changes (mirrors cli/clients/embedded-configuration).

listOf("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest").forEach { taskName ->
    tasks.matching { it.name == taskName }
        .configureEach { enabled = false }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")

    implementation("org.apache.httpcomponents:httpmime:4.5.14")
}
```

- [ ] **Step 3: Generate the client**

Run: `./gradlew :cli:clients:embedded-configuration-internal:generateClient > /tmp/t4.log 2>&1; echo $?`
Expected: `0`. Confirms `AutomationProjectCodeWorkflowApi` and
`AutomationProjectCodeWorkflowDeployResultModel` land under `cli/clients/embedded-configuration-internal/generated`.

- [ ] **Step 4: Write the failing command test**

```java
package com.bytechef.cli.command.embedded;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.cli.core.error.CliException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmbeddedCodeWorkflowCommandTest {

    @Test
    void testDeployRejectsAMissingFile() {
        EmbeddedCodeWorkflowCommand command = new EmbeddedCodeWorkflowCommand();

        command.setConfigPath(Path.of("/nonexistent/config"));
        command.setEnvironmentVariables(Map.of());

        CliException exception = assertThrows(
            CliException.class,
            () -> command.deploy(
                "/nonexistent/project.js", null, "default", "http://localhost:8080", "token", "PRODUCTION"));

        org.junit.jupiter.api.Assertions.assertEquals(1, exception.exitCode());
    }
}
```

- [ ] **Step 5: Run it to verify it fails**

Run: `./gradlew :cli:commands:embedded:test --tests "*EmbeddedCodeWorkflowCommandTest*" > /tmp/t4b.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class EmbeddedCodeWorkflowCommand`.

- [ ] **Step 6: Add the dependency and factory method**

In `cli/commands/embedded/build.gradle.kts`, add:

```kotlin
    implementation(project(":cli:clients:embedded-configuration-internal"))
```

In `EmbeddedConfigurationClientFactory.java`, add:

```java
    static com.bytechef.cli.client.embeddedconfigurationinternal.ApiClient internalApiClient(CliConfig config) {
        com.bytechef.cli.client.embeddedconfigurationinternal.ApiClient apiClient =
            new com.bytechef.cli.client.embeddedconfigurationinternal.ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config, "/api/embedded/internal"));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        return apiClient;
    }

    static com.bytechef.cli.client.embeddedconfigurationinternal.api.AutomationProjectCodeWorkflowApi
        automationProjectCodeWorkflowApi(CliConfig config) {

        return new com.bytechef.cli.client.embeddedconfigurationinternal.api.AutomationProjectCodeWorkflowApi(
            internalApiClient(config));
    }

    static CliException toCliException(com.bytechef.cli.client.embeddedconfigurationinternal.ApiException exception) {
        int status = exception.getCode();

        if (status == 401 || status == 403) {
            return new CliException(2, "Authentication failed (HTTP " + status + ").");
        }

        return new CliException(1, "Request failed (HTTP " + status + ").");
    }
```

- [ ] **Step 7: Write the command**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.embedded;

import com.bytechef.cli.client.embeddedconfigurationinternal.ApiException;
import com.bytechef.cli.client.embeddedconfigurationinternal.model.AutomationProjectCodeWorkflowDeployResultModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

/**
 * Commands for deploying and listing automation code workflows served through the embedded bridge (deploy-once,
 * reference-per-user catalog projects) -- the admin-only counterpart to the connected-user-scoped
 * {@code embedded integration} commands.
 *
 * @author Ivica Cardic
 */
@org.springframework.stereotype.Component
public class EmbeddedCodeWorkflowCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> environmentVariables = System.getenv();

    @Command(name = "embedded code-workflow deploy", description = "Deploy a code workflow into the embedded catalog.")
    public void deploy(
        @Option(longName = "file", required = true) String file,
        @Option(longName = "language") String language,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        File projectFile = new File(file);

        if (!projectFile.exists()) {
            throw new CliException(1, "Project file not found: " + file);
        }

        CliConfig config = resolve(profile, host, token, environment);

        try {
            AutomationProjectCodeWorkflowDeployResultModel result =
                EmbeddedConfigurationClientFactory.automationProjectCodeWorkflowApi(config)
                    .deployAutomationProjectCodeWorkflow(projectFile);

            System.out.println("Project deployed.");

            List<String> warnings = result.getWarnings();

            if (warnings != null) {
                for (String warning : warnings) {
                    System.out.println("WARNING: " + warning);
                }
            }
        } catch (ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private CliConfig resolve(String profile, String host, String token, String environment) {
        return EmbeddedSupport.resolve(configPath, environmentVariables, profile, host, token, environment);
    }
}
```

(The `--language` option is accepted for symmetry with the spec's stated flag, but is not sent — the server infers
language from the uploaded file's extension per `Language.of(originalFilename)`; it stays for forward-compat if a
future server revision needs an explicit override.)

- [ ] **Step 8: Run the test to verify it passes**

Run: `./gradlew :cli:commands:embedded:test --tests "*EmbeddedCodeWorkflowCommandTest*" > /tmp/t4b.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 9: Commit**

```bash
git add cli settings.gradle.kts
git commit -m "Add bytechef embedded code-workflow deploy

New cli/clients/embedded-configuration-internal module targets the internal
embedded-configuration OpenAPI spec (the deploy endpoint lives under
/api/embedded/internal, a different document than the public client the CLI
already generates from). Prints any deploy-time trigger-validation warnings
Task 3 now returns instead of the previous silent 204."
```

---

### Task 5: `embedded code-workflow list` + docs

**Files:**
- Modify: `cli/commands/embedded/src/main/java/com/bytechef/cli/command/embedded/EmbeddedCodeWorkflowCommand.java`
- Modify: `cli/commands/embedded/src/main/java/com/bytechef/cli/command/embedded/EmbeddedConfigurationClientFactory.java`
- Modify: `cli/README.md`
- Modify: `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md`

**Interfaces:**
- Consumes: `GET /api/embedded/v1/automation/projects` (`getFrontendProjects`, `jwtBearerAuth`, already generated
  in `cli/clients/embedded-configuration` — its committed generated model is stale relative to the server's
  `kind` field added by the shipped feature and must be regenerated).
- Produces: `bytechef embedded code-workflow list [--output table]`.

No new server endpoint is needed for `list` — `getFrontendProjects` (`operationId` in
`embedded-configuration-public-rest/openapi.yaml`) already returns every catalog project's `name`, `kind`
(`COPY`/`REFERENCE`), and `workflowTemplates[].label`, with no `externalUserId` scoping, using the same
`Authorization: Bearer` flow every other CLI command already uses. It is already wired into the CLI's
`embedded-configuration` client module (`AutomationWorkflowProjectApi`); the committed generated sources
just need regenerating to pick up the `kind` field.

- [ ] **Step 1: Regenerate the existing public client**

Run: `./gradlew :cli:clients:embedded-configuration:generateClient > /tmp/t5.log 2>&1; echo $?`
Expected: `0`. Confirm the field landed:

```bash
grep -c "kind" cli/clients/embedded-configuration/generated/src/main/java/com/bytechef/cli/client/embeddedconfiguration/model/AutomationWorkflowProjectModel.java
```
Expected: at least `1`.

- [ ] **Step 2: Add the factory accessor**

```java
    static AutomationWorkflowProjectApi automationWorkflowProjectApi(CliConfig config) {
        return new AutomationWorkflowProjectApi(apiClient(config));
    }
```

(Already present per the earlier factory listing — confirm it exists; add only if missing.)

- [ ] **Step 3: Add the `list` command**

```java
    @Command(name = "embedded code-workflow list", description = "List catalog projects in the embedded automation bridge.")
    public void list(
        @Option(longName = "output", defaultValue = "json") String output,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment);

        try {
            new OutputRenderer(System.out).render(
                EmbeddedConfigurationClientFactory.automationWorkflowProjectApi(config)
                    .getFrontendProjects(null),
                output);
        } catch (com.bytechef.cli.client.embeddedconfiguration.ApiException e) {
            throw EmbeddedConfigurationClientFactory.toCliException(e);
        }
    }
```

(add `import com.bytechef.cli.core.output.OutputRenderer;`)

- [ ] **Step 4: Update `cli/README.md`**

Under `## Embedded commands`, add a `Code workflows (admin-only)` subsection with `deploy`/`list` examples,
mirroring the existing subsection banners' style.

- [ ] **Step 5: Update the plugin skill**

In `claude-code-plugin/bytechef-dev/skills/bytechef-code-workflow/SKILL.md`, replace the **Bridge** section's
prose-only deploy mention with a CLI-first example, curl retained as the no-CLI fallback:

```markdown
Deploy through the CLI once it's configured (`bytechef configure ...`):

\`\`\`bash
bytechef embedded code-workflow deploy --file my-project.js
bytechef embedded code-workflow list --output table
\`\`\`

No CLI available? The same deploy is a plain multipart POST (see **Deploying** above for the
`/api/automation/v1/projects/deploy` shape) — the bridge variant is
`POST /api/embedded/internal/automation/projects/deploy`, admin session/token only.
```

Also swap the **Deploying** section's plain-automation curl example for the CLI form — an
`automation project deploy` command already exists (`AutomationProjectCommand.deploy`, hitting the public
`POST /api/automation/v1/projects/deploy`), so per the plugin's own CLI-first convention this example should not
stay curl-only:

```markdown
\`\`\`bash
bytechef automation project deploy --project-file my-code-project.js --workspace-id 1049
\`\`\`

No CLI available? Same endpoint over curl:

\`\`\`bash
curl -sf -X POST "$BYTECHEF_BASE_URL/api/automation/v1/projects/deploy" \
  -H "Authorization: Bearer $BYTECHEF_API_KEY" \
  -F "workspaceId=1049" \
  -F "projectFile=@my-code-project.js"
\`\`\`
```

The embedded-integration curl example stays curl-only (no `embedded integration deploy` CLI command exists and
adding one is out of scope for this plan — only the automation-bridge code-workflow surface gets a CLI command
here).

- [ ] **Step 6: Verify the whole CLI module compiles**

Run: `./gradlew :cli:commands:embedded:compileJava :cli:cli-app:compileJava > /tmp/t5b.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Commit**

```bash
git add cli claude-code-plugin
git commit -m "Add bytechef embedded code-workflow list and CLI-first bridge docs

Reuses the existing public getFrontendProjects endpoint (name/kind/workflows,
no externalUserId scoping) instead of adding a new admin listing endpoint --
the CLI client just needed regenerating to pick up the kind field the shipped
feature already returns."
```

---

### Task 6: Admin GraphQL — kind field and references query

**Files:**
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/automation-workflow-project.graphqls`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/resources/graphql/connected-user-code-workflow-reference.graphqls`
- Modify: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/repository/ConnectedUserProjectWorkflowRepository.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-api/src/main/java/com/bytechef/ee/embedded/configuration/dto/ConnectedUserCodeWorkflowReferenceDTO.java`
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/main/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserCodeWorkflowReferenceAdminFacade.java` (interface + impl, one file each)
- Create: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-graphql/src/main/java/com/bytechef/ee/embedded/configuration/web/graphql/ConnectedUserCodeWorkflowReferenceGraphQlController.java`
- Test: `server/ee/libs/embedded/embedded-configuration/embedded-configuration-service/src/test/java/com/bytechef/ee/embedded/configuration/facade/ConnectedUserCodeWorkflowReferenceAdminFacadeTest.java`

**Interfaces:**
- Consumes: `AutomationWorkflowProjectDTO.codeWorkflowProject()` (already populated), `ProjectWorkflowService`,
  `ConnectedUserService`.
- Produces: `AutomationWorkflowProject.codeWorkflowProject: Boolean!` GraphQL field; GraphQL query
  `connectedUserCodeWorkflowReferences(catalogWorkflowUuids: [ID!]!): [ConnectedUserCodeWorkflowReference!]!`,
  admin-gated the same way `AutomationWorkflowProjectAdminFacade` already is (facade wraps the
  `@SkipAutomationAuthorization`-carrying real facade behind `isTenantAdmin()`), so this new facade follows the
  same wrap-with-`isTenantAdmin()` shape rather than a controller-layer `@PreAuthorize`. This is intentionally NOT
  the same SPI Task 1/2 touch (`ConnectedUserCodeWorkflowReferenceFacade`) — it is a new, admin-only, repository-
  backed read path that never needs a remote-client implementation because `embedded-configuration-graphql`
  controllers are `@ConditionalOnCoordinator` and run wherever the real `embedded-configuration-service` beans
  already live (configuration-app / the monolith), same as `AutomationWorkflowProjectGraphQlController`.

- [ ] **Step 1: Add the `codeWorkflowProject` field to the existing type**

In `automation-workflow-project.graphqls`, add one line to `type AutomationWorkflowProject`:

```graphql
type AutomationWorkflowProject {
    id: ID!
    name: String!
    description: String
    categoryId: ID
    tagIds: [ID!]!
    published: Boolean!
    version: Int!
    lastPublishedVersion: Int
    permissionExpression: String
    codeWorkflowProject: Boolean!
    workflowTemplates: [AutomationWorkflowProjectWorkflowTemplate!]!
}
```

No controller change needed — `automationWorkflowProjects()` already returns `AutomationWorkflowProjectDTO`
records straight from `automationWorkflowProjectFacade.getProjects()`, and GraphQL-Java's default
`PropertyDataFetcher` resolves `codeWorkflowProject()` off the record with zero extra code, the same way every
other unmapped field on this type already resolves.

- [ ] **Step 2: Write the failing facade test**

```java
package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectedUserCodeWorkflowReferenceAdminFacadeTest {

    @Mock
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Mock
    private ConnectedUserService connectedUserService;

    @InjectMocks
    private ConnectedUserCodeWorkflowReferenceAdminFacadeImpl connectedUserCodeWorkflowReferenceAdminFacade;

    @Test
    void testGetReferencesJoinsBackToTheOwningConnectedUser() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setCatalogWorkflowUuid("uuid-1");
        reference.setEnabled(true);
        reference.setDangling(false);

        when(connectedUserProjectWorkflowRepository.findAllByCatalogWorkflowUuidIn(Set.of("uuid-1")))
            .thenReturn(List.of(reference));

        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId("ext-1");
        connectedUser.setEnvironment(0);

        when(connectedUserService.getConnectedUserByProjectWorkflow(reference))
            .thenReturn(connectedUser);

        List<ConnectedUserCodeWorkflowReferenceDTO> result =
            connectedUserCodeWorkflowReferenceAdminFacade.getReferences(Set.of("uuid-1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .externalUserId()).isEqualTo("ext-1");
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserCodeWorkflowReferenceAdminFacadeTest*" > /tmp/t6.log 2>&1; echo $?`
Expected: non-zero, `cannot find symbol: class ConnectedUserCodeWorkflowReferenceAdminFacadeImpl`.

- [ ] **Step 3: Add the repository query**

In `ConnectedUserProjectWorkflowRepository.java`:

```java
    @Query("""
        SELECT cupw.*
        FROM connected_user_project_workflow cupw
        WHERE cupw.catalog_workflow_uuid IN (:catalogWorkflowUuids)
        """)
    List<ConnectedUserProjectWorkflow> findAllByCatalogWorkflowUuidIn(
        @Param("catalogWorkflowUuids") Set<String> catalogWorkflowUuids);
```

- [ ] **Step 4: Write the DTO**

```java
package com.bytechef.ee.embedded.configuration.dto;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserCodeWorkflowReferenceDTO(
    String catalogWorkflowUuid, String externalUserId, boolean enabled, boolean dangling, String danglingReason) {
}
```

- [ ] **Step 5: Write the interface and the facade**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import java.util.List;
import java.util.Set;

/**
 * Admin-only read seam over automation-bridge references, keyed by catalog workflow rather than by connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserCodeWorkflowReferenceAdminFacade {

    List<ConnectedUserCodeWorkflowReferenceDTO> getReferences(Set<String> catalogWorkflowUuids);
}
```

```java
package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Admin-only read seam over automation-bridge references, joined back to the connected user each reference belongs
 * to -- the direction {@link com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade}
 * deliberately does not serve (it reads per connected user, not per catalog workflow). Gated the same way
 * {@code AutomationWorkflowProjectAdminFacade} already is: a plain {@code isTenantAdmin()} guard rather than a role
 * literal, since the embedded admin console has no {@code ROLE_ADMIN} authority of its own.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ConnectedUserCodeWorkflowReferenceAdminFacadeImpl implements ConnectedUserCodeWorkflowReferenceAdminFacade {

    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public ConnectedUserCodeWorkflowReferenceAdminFacadeImpl(
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserService connectedUserService) {

        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserService = connectedUserService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<ConnectedUserCodeWorkflowReferenceDTO> getReferences(Set<String> catalogWorkflowUuids) {
        return connectedUserProjectWorkflowRepository.findAllByCatalogWorkflowUuidIn(catalogWorkflowUuids)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    private ConnectedUserCodeWorkflowReferenceDTO toDTO(ConnectedUserProjectWorkflow reference) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUserByProjectWorkflow(reference);

        return new ConnectedUserCodeWorkflowReferenceDTO(
            reference.getCatalogWorkflowUuid(), connectedUser.getExternalId(), reference.isEnabled(),
            reference.isDangling(), reference.getDanglingReason());
    }
}
```

(the interface `ConnectedUserCodeWorkflowReferenceAdminFacade` declares just `getReferences`; add
`ConnectedUserService.getConnectedUserByProjectWorkflow(ConnectedUserProjectWorkflow)` as a small new service
method that resolves through `connected_user_project` the same way
`ConnectedUserProjectWorkflowRepository.findAllByConnectedUserId` already joins in the other direction — reuse
that repository's join style rather than inventing a new one.)

- [ ] **Step 6: Wire the GraphQL schema and controller**

```graphql
type ConnectedUserCodeWorkflowReference {
    catalogWorkflowUuid: ID!
    externalUserId: String!
    enabled: Boolean!
    dangling: Boolean!
    danglingReason: String
}

extend type Query {
    connectedUserCodeWorkflowReferences(catalogWorkflowUuids: [ID!]!): [ConnectedUserCodeWorkflowReference!]!
}
```

```java
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserCodeWorkflowReferenceGraphQlController {

    private final ConnectedUserCodeWorkflowReferenceAdminFacade connectedUserCodeWorkflowReferenceAdminFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserCodeWorkflowReferenceGraphQlController(
        ConnectedUserCodeWorkflowReferenceAdminFacade connectedUserCodeWorkflowReferenceAdminFacade) {

        this.connectedUserCodeWorkflowReferenceAdminFacade = connectedUserCodeWorkflowReferenceAdminFacade;
    }

    @QueryMapping
    public List<ConnectedUserCodeWorkflowReferenceDTO> connectedUserCodeWorkflowReferences(
        @Argument List<String> catalogWorkflowUuids) {

        return connectedUserCodeWorkflowReferenceAdminFacade.getReferences(Set.copyOf(catalogWorkflowUuids));
    }
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew :server:ee:libs:embedded:embedded-configuration:embedded-configuration-service:test --tests "*ConnectedUserCodeWorkflowReferenceAdminFacadeTest*" :server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql:compileJava > /tmp/t6.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 8: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/embedded/embedded-configuration
git commit -m "Add kind field and admin references query for the code-workflow catalog

codeWorkflowProject needed only a schema addition -- the DTO already carried it.
The references query is a new admin-only read path (never remoted; graphql
controllers here run wherever the real service beans already live), joining
connected_user_project_workflow back to its owning connected user -- the
opposite direction from ConnectedUserCodeWorkflowReferenceFacade, which reads
per connected user, not per catalog workflow."
```

---

### Task 7: Admin UI — kind badge and deploy dialog

**Files:**
- Modify: `client/src/graphql/embedded/configuration/automationWorkflowProjects.graphql`
- Modify: `client/src/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectListItem.tsx`
- Create: `client/src/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog.tsx`
- Test: `client/src/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog.test.tsx`

**Interfaces:**
- Consumes: `codeWorkflowProject` field from Task 6; `POST /api/embedded/internal/automation/projects/deploy`
  (Task 3's warnings response).
- Produces: kind badge on each catalog project row; a "Deploy code workflow" action reachable from the catalog
  page's toolbar, redeploy = the same dialog reused for an existing code project (same admin-name-match
  idempotency the server already implements).

- [ ] **Step 1: Add the field to the query**

In `automationWorkflowProjects.graphql`, add `codeWorkflowProject` to the `automationWorkflowProjects` selection
set (alongside `permissionExpression`). Run `cd client && npx graphql-codegen` afterward to regenerate
`useAutomationWorkflowProjectsQuery`'s result type.

- [ ] **Step 2: Write the failing dialog test**

```tsx
import DeployCodeWorkflowDialog from '@/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('DeployCodeWorkflowDialog', () => {
    test('posts the selected file and shows returned warnings', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            json: () => Promise.resolve({warnings: ['Workflow "x" has no publicly invocable trigger']}),
            ok: true,
        });

        vi.stubGlobal('fetch', fetchMock);

        const onClose = vi.fn();

        render(<DeployCodeWorkflowDialog onClose={onClose} />);

        const file = new File(['export default {}'], 'my-project.js', {type: 'text/javascript'});
        const input = screen.getByLabelText(/project file/i);

        fireEvent.change(input, {target: {files: [file]}});
        fireEvent.click(screen.getByRole('button', {name: /deploy/i}));

        await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
            '/api/embedded/internal/automation/projects/deploy',
            expect.objectContaining({method: 'POST'})
        ));

        expect(await screen.findByText(/no publicly invocable trigger/i)).toBeInTheDocument();
    });
});
```

- [ ] **Step 3: Run it to verify it fails**

Run: `cd client && npx vitest run src/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog.test.tsx > /tmp/t7.log 2>&1; echo $?`
Expected: non-zero — the component does not exist yet.

- [ ] **Step 4: Write the dialog**

Modeled on `useUploadCustomComponentDialog.ts`'s raw `fetch` + `FormData` + `X-XSRF-TOKEN` admin-session upload
pattern (this endpoint sits under the admin-cookie surface, not the connected-user bearer-token API, exactly as
the plugin skill's existing note about `/api/embedded/internal/integrations/deploy` already documents).

```tsx
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {getCookie} from '@/shared/util/cookie-utils';
import {useState} from 'react';

interface DeployCodeWorkflowDialogProps {
    onClose: () => void;
    onDeployed?: () => void;
}

const DeployCodeWorkflowDialog = ({onClose, onDeployed}: DeployCodeWorkflowDialogProps) => {
    const [file, setFile] = useState<File | null>(null);
    const [deploying, setDeploying] = useState(false);
    const [warnings, setWarnings] = useState<string[]>([]);
    const [error, setError] = useState<string | null>(null);

    const handleDeploy = async () => {
        if (!file) {
            return;
        }

        setDeploying(true);
        setError(null);

        const formData = new FormData();

        formData.append('projectFile', file);

        try {
            const response = await fetch('/api/embedded/internal/automation/projects/deploy', {
                body: formData,
                headers: {
                    'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                },
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Deploy failed: ${response.statusText}`);
            }

            const result: {warnings?: string[]} = await response.json();

            setWarnings(result.warnings || []);

            onDeployed?.();
        } catch (deployError) {
            setError(deployError instanceof Error ? deployError.message : 'Deploy failed');
        } finally {
            setDeploying(false);
        }
    };

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Deploy Code Workflow</DialogTitle>

                        <DialogDescription>
                            Upload a code-native automation project. Redeploying an existing project name updates it
                            in place for every connected user referencing it.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <label className="flex flex-col gap-1 text-sm" htmlFor="deploy-code-workflow-file">
                    Project file

                    <input
                        accept=".jar,.js,.py,.rb"
                        id="deploy-code-workflow-file"
                        onChange={(event) => setFile(event.target.files?.[0] || null)}
                        type="file"
                    />
                </label>

                {warnings.length > 0 && (
                    <ul className="space-y-1 text-sm text-warning">
                        {warnings.map((warning) => (
                            <li key={warning}>{warning}</li>
                        ))}
                    </ul>
                )}

                {error && <p className="text-sm text-destructive">{error}</p>}

                <DialogFooter>
                    <Button
                        disabled={!file || deploying}
                        label={deploying ? 'Deploying...' : 'Deploy'}
                        onClick={handleDeploy}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default DeployCodeWorkflowDialog;
```

- [ ] **Step 5: Add the kind badge and the toolbar action**

In `AutomationWorkflowProjectListItem.tsx`, add a `Badge` next to the existing project-name row when
`project.codeWorkflowProject` is true:

```tsx
{project.codeWorkflowProject && (
    <Badge label="Reference" styleType="secondary-outline" />
)}
```

Wire a "Deploy code workflow" `DropdownMenuItem` (mirroring the existing `UploadIcon`-based import item already in
this file) that opens `DeployCodeWorkflowDialog`, in the page-level component that owns `AutomationWorkflowProjectList`.

- [ ] **Step 6: Run the test to verify it passes**

Run: `cd client && npx vitest run src/ee/pages/embedded/automation-workflows/components/DeployCodeWorkflowDialog.test.tsx > /tmp/t7.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Run the full client check and commit**

```bash
cd client && npm run check
git add client
git commit -m "Add kind badge and Deploy code workflow action to the catalog page

Deploy reuses the admin-session cookie + X-XSRF-TOKEN upload pattern already
established for custom-component uploads -- this endpoint sits behind the admin
cookie surface, not the connected-user bearer-token API. Redeploy is the same
dialog against an existing project name; the server already treats that as an
idempotent update, not a duplicate."
```

---

### Task 8: Admin UI — code catalog project detail + references panel

**Files:**
- Create: `client/src/graphql/embedded/configuration/connectedUserCodeWorkflowReferences.graphql`
- Create: `client/src/ee/pages/embedded/automation-workflows/CodeWorkflowProjectDetail.tsx`
- Create: `client/src/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel.tsx`
- Test: `client/src/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel.test.tsx`
- Modify: `docs/content/docs/automation/*` embedded bridge page — drop the "degrades to 404 in distributed
  deployments" caveat now that Tasks 1-2 make it work.

**Interfaces:**
- Consumes: `connectedUserCodeWorkflowReferences(catalogWorkflowUuids: [ID!]!)` from Task 6.
- Produces: a read-only References panel; no admin mutation of user references, matching the spec's stated v1 scope.

- [ ] **Step 1: Add the GraphQL operation**

```graphql
query connectedUserCodeWorkflowReferences($catalogWorkflowUuids: [ID!]!) {
    connectedUserCodeWorkflowReferences(catalogWorkflowUuids: $catalogWorkflowUuids) {
        catalogWorkflowUuid
        dangling
        danglingReason
        enabled
        externalUserId
    }
}
```

Run `cd client && npx graphql-codegen` to generate `useConnectedUserCodeWorkflowReferencesQuery`.

- [ ] **Step 2: Write the failing panel test**

```tsx
import ReferencesPanel from '@/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel';
import * as graphql from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {describe, expect, test, vi} from 'vitest';

describe('ReferencesPanel', () => {
    test('renders a dangling badge for a dangling reference', () => {
        vi.spyOn(graphql, 'useConnectedUserCodeWorkflowReferencesQuery').mockReturnValue({
            data: {
                connectedUserCodeWorkflowReferences: [
                    {
                        catalogWorkflowUuid: 'uuid-1',
                        dangling: true,
                        danglingReason: 'Workflow removed on redeploy',
                        enabled: false,
                        externalUserId: 'ext-1',
                    },
                ],
            },
        } as ReturnType<typeof graphql.useConnectedUserCodeWorkflowReferencesQuery>);

        const queryClient = new QueryClient();

        render(
            <QueryClientProvider client={queryClient}>
                <ReferencesPanel catalogWorkflowUuids={['uuid-1']} />
            </QueryClientProvider>
        );

        expect(screen.getByText('ext-1')).toBeInTheDocument();
        expect(screen.getByText(/dangling/i)).toBeInTheDocument();
        expect(screen.getByText('Workflow removed on redeploy')).toBeInTheDocument();
    });
});
```

- [ ] **Step 3: Run it to verify it fails**

Run: `cd client && npx vitest run src/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel.test.tsx > /tmp/t8.log 2>&1; echo $?`
Expected: non-zero — the component does not exist yet.

- [ ] **Step 4: Write the panel**

```tsx
import Badge from '@/components/Badge/Badge';
import {useConnectedUserCodeWorkflowReferencesQuery} from '@/shared/middleware/graphql';

interface ReferencesPanelProps {
    catalogWorkflowUuids: string[];
}

const ReferencesPanel = ({catalogWorkflowUuids}: ReferencesPanelProps) => {
    const {data} = useConnectedUserCodeWorkflowReferencesQuery({catalogWorkflowUuids});

    const references = data?.connectedUserCodeWorkflowReferences || [];

    if (references.length === 0) {
        return <p className="text-sm text-muted-foreground">No connected users reference this workflow yet.</p>;
    }

    return (
        <table className="w-full text-sm">
            <thead>
                <tr className="text-left text-muted-foreground">
                    <th>External user</th>
                    <th>Enabled</th>
                    <th>Status</th>
                </tr>
            </thead>

            <tbody>
                {references.map((reference) => (
                    <tr key={`${reference.catalogWorkflowUuid}-${reference.externalUserId}`}>
                        <td>{reference.externalUserId}</td>
                        <td>{reference.enabled ? 'Yes' : 'No'}</td>
                        <td>
                            {reference.dangling ? (
                                <Badge
                                    label={`Dangling: ${reference.danglingReason || 'unknown reason'}`}
                                    styleType="destructive-outline"
                                />
                            ) : (
                                <Badge label="OK" styleType="success-outline" />
                            )}
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
};

export default ReferencesPanel;
```

- [ ] **Step 5: Wire the detail page**

`CodeWorkflowProjectDetail.tsx` renders the project's workflow list (reusing
`AutomationWorkflowProjectWorkflowList`'s existing row shape) with a `ReferencesPanel` per workflow, reachable
from `AutomationWorkflowProjectListItem`'s project-name click when `project.codeWorkflowProject` is true (routes
to the detail page instead of the existing inline `Collapsible` used for visual-template projects).

- [ ] **Step 6: Run the test to verify it passes**

Run: `cd client && npx vitest run src/ee/pages/embedded/automation-workflows/components/code-workflow-project-detail/ReferencesPanel.test.tsx > /tmp/t8.log 2>&1; echo $?`
Expected: `0`.

- [ ] **Step 7: Update docs and run the full client check**

Drop the "degrades to 404 in distributed deployments" caveat from the embedded bridge's user-facing docs page
(now that Tasks 1-2 implement it) and from the `CLAUDE.md` "MCP servers and workflows-as-tools" / bridge entry if
one exists referencing the degrade behavior.

```bash
cd client && npm run check
```

- [ ] **Step 8: Commit**

```bash
git add client docs CLAUDE.md
git commit -m "Add code catalog project detail page with a read-only references panel

Lists every connected user referencing each workflow -- external user id,
enabled, dangling(+reason) -- served by the admin-only query from Task 6. No
admin mutation of user references in v1: fixing a dangling or disabled
reference remains the customer backend's job through the public API."
```

---

## Not in this plan

- **Admin mutation of connected-user references.** The References panel is read-only per the spec; enabling,
  disabling, or deleting a specific user's reference from the admin console is out of scope.
- **`bytechef embedded code-workflow` provision/enable commands.** Connected-user operations belong to customer
  backends through the public API, not the operator CLI, per the spec.
- **Remote implementations for the other ~40 methods** across `AutomationWorkflowProjectFacade`,
  `ConnectedUserCodeWorkflowReferenceFacade`, and `ConnectedUserProjectFacade`. They stay throwing stubs until a
  real distributed caller needs them, per the established partial-implementation pattern.
- **Per-user version pinning, per-user bespoke code deploys, admin catalog authoring of code templates users
  copy** — out of scope per the base feature's own spec.
