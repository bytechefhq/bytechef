# Copilot: AI-Generated Sample Output Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an EE-only AI affordance that generates a realistic sample-output JSON instance from a natural-language prompt and drops it into the node output-tab sample dialog, where the existing deterministic schema inference (the "Upload" button) turns it into the node's output schema (#2202).

**Architecture:** A new `generateSampleOutput` GraphQL mutation (prompt → JSON instance, parse-validated as object/array with one repair) in the existing `ai-copilot` modules, plus a surface-agnostic `SampleOutputCopilotBar` that fills the live `OutputTabSampleDataDialog` editor. Reuses the established Property Copilot patterns (gating `ai.copilot.enabled && ff-1570`, `WORKFLOW_VIEW` IDOR guard, `clean()` fence-strip, parse-validate + repair, metric counter). The dead `PropertyJsonSchemaBuilderSampleDataDialog` is intentionally NOT wired (see spec "Discovered constraint").

**Tech Stack:** Java 25 / Spring Boot, Spring AI `ChatModel`, Spring for GraphQL; React 19 + TypeScript, graphql-codegen + React Query, Zustand, Vitest. EE modules under `server/ee/libs/ai/ai-copilot/`.

**Spec:** `docs/superpowers/specs/2026-06-02-copilot-sample-output-design.md`

**Conventions reminder:** EE license header + `@version ee` Javadoc on every new file under `server/ee` (incl. tests). Client: `sort-keys` alphabetical, interfaces end `…PropsI`/`…Props`, sorted destructured imports, Lucide `…Icon` imports, `twMerge` (not `cn`), hook ordering (`useState`→`useRef`→store hooks→derived→`useEffect`→return), no `_`-prefixed methods, descriptive names. Java: one blank line before control statements; no gratuitous chaining.

---

## File Structure

**Backend (create):**
- `ai-copilot-api/.../copilot/sampleoutput/SampleOutputCopilotRequest.java`, `SampleOutputCopilotResult.java`, `SampleOutputCopilotGenerator.java`.
- `ai-copilot-service/.../copilot/sampleoutput/SampleOutputPromptBuilder.java`, `SampleOutputCopilotGeneratorImpl.java`.
- `ai-copilot-graphql/src/main/resources/graphql/sample-output-copilot.graphqls`.
- `ai-copilot-graphql/.../web/graphql/SampleOutputCopilotGraphQlController.java`.

**Frontend (create/modify):**
- Create `client/src/graphql/platform/copilot/generateSampleOutput.graphql`.
- Create `client/src/shared/components/copilot/useGenerateSampleOutput.ts`.
- Create `client/src/shared/components/copilot/SampleOutputCopilotBar.tsx` (+ `.test.tsx`).
- Modify `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx`.
- Modify `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx`.

---

## Part A — Backend: `generateSampleOutput` mutation

### Task 1: `ai-copilot-api` — request/result records + generator interface

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotRequest.java`
- Create: `.../sampleoutput/SampleOutputCopilotResult.java`
- Create: `.../sampleoutput/SampleOutputCopilotGenerator.java`

- [ ] **Step 1: Create the request record**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record SampleOutputCopilotRequest(String workflowId, String prompt, long environmentId) {
}
```

- [ ] **Step 2: Create the result record**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record SampleOutputCopilotResult(String value, boolean valid, String message) {
}
```

- [ ] **Step 3: Create the generator interface**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface SampleOutputCopilotGenerator {

    SampleOutputCopilotResult generate(SampleOutputCopilotRequest request);
}
```

- [ ] **Step 4: Compile + commit**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/
git commit -m "2202 Add SampleOutput copilot API types"
```

---

### Task 2: `SampleOutputPromptBuilder`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputPromptBuilder.java`
- Test: `.../src/test/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputPromptBuilderTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SampleOutputPromptBuilderTest {

    private final SampleOutputPromptBuilder promptBuilder = new SampleOutputPromptBuilder();

    @Test
    void testBuildAsksForJsonInstanceNotSchema() {
        String prompt = promptBuilder.build("an order with an id and a list of line items");

        assertThat(prompt).contains("an order with an id and a list of line items");
        assertThat(prompt).contains("JSON");
        assertThat(prompt).containsIgnoringCase("example");
        assertThat(prompt).doesNotContain("JSON Schema");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*SampleOutputPromptBuilderTest"`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for the Sample Output Copilot feature.
 *
 * @version ee
 * @author Ivica Cardic
 */
@Component
public class SampleOutputPromptBuilder {

    public String build(String prompt) {
        StringBuilder builder = new StringBuilder();

        builder.append(
            "You generate a single realistic example data instance in JSON based on the user's description. ")
            .append("Return example data (a concrete JSON object or array with sample values), NOT a JSON ")
            .append("Schema.\n\n");
        builder.append("User request: ")
            .append(prompt)
            .append("\n\n");
        builder.append(
            "Return ONLY the JSON instance. No explanation, no markdown, no code fences.");

        return builder.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*SampleOutputPromptBuilderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputPromptBuilder.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputPromptBuilderTest.java
git commit -m "2202 Add SampleOutputPromptBuilder"
```

---

### Task 3: `SampleOutputCopilotGeneratorImpl`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotGeneratorImpl.java`
- Test: `.../src/test/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotGeneratorImplTest.java`

Validates the model output parses as a JSON **object or array** via a standalone `tools.jackson` `ObjectMapper` (the precedent set by `PropertyCopilotGeneratorImpl`, which avoids the Spring-context requirement of `JsonUtils` in unit tests). One repair retry, mirroring the JSON_SCHEMA path.

- [ ] **Step 1: Write the failing tests**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SampleOutputCopilotGeneratorImplTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static ChatResponse buildChatResponse(String text) {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();
    }

    @SuppressWarnings("unchecked")
    private SampleOutputCopilotGeneratorImpl generatorReturning(String... llmTexts) {
        ChatModel chatModel = mock(ChatModel.class);

        if (llmTexts.length == 1) {
            when(chatModel.call(any(Prompt.class))).thenReturn(buildChatResponse(llmTexts[0]));
        } else {
            when(chatModel.call(any(Prompt.class))).thenReturn(
                buildChatResponse(llmTexts[0]), buildChatResponse(llmTexts[1]));
        }

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new SampleOutputCopilotGeneratorImpl(
            chatModel, new SampleOutputPromptBuilder(), meterRegistryProvider);
    }

    @Test
    void testGenerateObjectStripsFencesAndValidates() {
        SampleOutputCopilotGeneratorImpl generator = generatorReturning(
            "```json\n{\"id\":1,\"items\":[]}\n```");

        SampleOutputCopilotResult result = generator.generate(
            new SampleOutputCopilotRequest("wf1", "order", 0));

        assertThat(result.value()).isEqualTo("{\"id\":1,\"items\":[]}");
        assertThat(result.valid()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    void testGenerateArrayIsValid() {
        SampleOutputCopilotGeneratorImpl generator = generatorReturning("[{\"id\":1},{\"id\":2}]");

        SampleOutputCopilotResult result = generator.generate(
            new SampleOutputCopilotRequest("wf1", "list of orders", 0));

        assertThat(result.value()).isEqualTo("[{\"id\":1},{\"id\":2}]");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testGenerateInvalidThenRepaired() {
        SampleOutputCopilotGeneratorImpl generator = generatorReturning(
            "not json", "{\"id\":1}");

        SampleOutputCopilotResult result = generator.generate(
            new SampleOutputCopilotRequest("wf1", "order", 0));

        assertThat(result.value()).isEqualTo("{\"id\":1}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testGenerateStillInvalidAfterRepairReturnsInvalid() {
        SampleOutputCopilotGeneratorImpl generator = generatorReturning("nope", "still not json");

        SampleOutputCopilotResult result = generator.generate(
            new SampleOutputCopilotRequest("wf1", "order", 0));

        assertThat(result.value()).isEqualTo("still not json");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*SampleOutputCopilotGeneratorImplTest"`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.sampleoutput;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class SampleOutputCopilotGeneratorImpl implements SampleOutputCopilotGenerator {

    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    private final ChatModel chatModel;
    private final SampleOutputPromptBuilder promptBuilder;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public SampleOutputCopilotGeneratorImpl(
        ChatModel chatModel, SampleOutputPromptBuilder promptBuilder,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.chatModel = chatModel;
        this.promptBuilder = promptBuilder;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public SampleOutputCopilotResult generate(SampleOutputCopilotRequest request) {
        String prompt = promptBuilder.build(request.prompt());

        String value = clean(call(prompt));

        if (isValidJsonInstance(value)) {
            record("success");

            return new SampleOutputCopilotResult(value, true, null);
        }

        String repaired = clean(call(prompt +
            "\n\nThe previous attempt was not valid JSON. Return ONLY a valid JSON object or array."));

        if (isValidJsonInstance(repaired)) {
            record("success");

            return new SampleOutputCopilotResult(repaired, true, null);
        }

        record("invalid_json");

        return new SampleOutputCopilotResult(
            repaired, false, "The generated sample output could not be parsed; please review it.");
    }

    private String call(String promptText) {
        return chatModel.call(new Prompt(promptText))
            .getResult()
            .getOutput()
            .getText();
    }

    private static boolean isValidJsonInstance(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            JsonNode node = JSON_OBJECT_MAPPER.readTree(value);

            return node.isObject() || node.isArray();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("```[a-zA-Z]*", "")
            .strip();
    }

    private void record(String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_sample_output_copilot_generate")
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*SampleOutputCopilotGeneratorImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotGeneratorImpl.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/sampleoutput/SampleOutputCopilotGeneratorImplTest.java
git commit -m "2202 Add SampleOutputCopilotGeneratorImpl with object/array validation"
```

---

### Task 4: GraphQL schema + `SampleOutputCopilotGraphQlController`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/sample-output-copilot.graphqls`
- Create: `.../web/graphql/SampleOutputCopilotGraphQlController.java`
- Test: `.../src/test/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlControllerTest.java`

- [ ] **Step 1: Create the GraphQL schema**

```graphql
extend type Mutation {
    generateSampleOutput(input: GenerateSampleOutputInput!): GenerateSampleOutputPayload!
}

input GenerateSampleOutputInput {
    workflowId: ID!
    prompt: String!
    environmentId: Int!
}

type GenerateSampleOutputPayload {
    value: String!
    valid: Boolean!
    message: String
}
```

- [ ] **Step 2: Write the failing controller test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.sampleoutput.SampleOutputCopilotGenerator;
import com.bytechef.ee.ai.copilot.sampleoutput.SampleOutputCopilotResult;
import com.bytechef.ee.ai.copilot.web.graphql.SampleOutputCopilotGraphQlController.GenerateSampleOutputInput;
import com.bytechef.ee.ai.copilot.web.graphql.SampleOutputCopilotGraphQlController.GenerateSampleOutputPayload;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SampleOutputCopilotGraphQlControllerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final SampleOutputCopilotGenerator generator = mock(SampleOutputCopilotGenerator.class);

    private final SampleOutputCopilotGraphQlController controller = new SampleOutputCopilotGraphQlController(
        permissionService, projectWorkflowService, Optional.of(generator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasProjectScope(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> controller.generateSampleOutput(input()))
            .isInstanceOf(AccessDeniedException.class);

        verify(generator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasProjectScope(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(generator.generate(any())).thenReturn(new SampleOutputCopilotResult("{\"id\":1}", true, null));

        GenerateSampleOutputPayload payload = controller.generateSampleOutput(input());

        assertThat(payload.value()).isEqualTo("{\"id\":1}");
        assertThat(payload.valid()).isTrue();
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static GenerateSampleOutputInput input() {
        return new GenerateSampleOutputInput("wf1", "order", 0);
    }
}
```

- [ ] **Step 3: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:test --tests "*SampleOutputCopilotGraphQlControllerTest"`
Expected: FAIL — controller class missing.

- [ ] **Step 4: Implement the controller**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.sampleoutput.SampleOutputCopilotGenerator;
import com.bytechef.ee.ai.copilot.sampleoutput.SampleOutputCopilotRequest;
import com.bytechef.ee.ai.copilot.sampleoutput.SampleOutputCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Sample Output Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class SampleOutputCopilotGraphQlController {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final SampleOutputCopilotGenerator sampleOutputCopilotGenerator;

    public SampleOutputCopilotGraphQlController(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<SampleOutputCopilotGenerator> sampleOutputCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.sampleOutputCopilotGenerator = sampleOutputCopilotGenerator.orElse(null);
    }

    @MutationMapping
    public GenerateSampleOutputPayload generateSampleOutput(@Argument GenerateSampleOutputInput input) {
        if (sampleOutputCopilotGenerator == null) {
            throw new IllegalStateException("Sample Output Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before generating (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(input.workflowId())
            .getProjectId();

        if (!permissionService.hasProjectScope(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + input.workflowId());
        }

        SampleOutputCopilotResult result = sampleOutputCopilotGenerator.generate(
            new SampleOutputCopilotRequest(input.workflowId(), input.prompt(), input.environmentId()));

        return new GenerateSampleOutputPayload(result.value(), result.valid(), result.message());
    }

    @SuppressFBWarnings("EI")
    public record GenerateSampleOutputInput(String workflowId, String prompt, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateSampleOutputPayload(String value, boolean valid, String message) {
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:test --tests "*SampleOutputCopilotGraphQlControllerTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/sample-output-copilot.graphqls \
        server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlController.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/SampleOutputCopilotGraphQlControllerTest.java
git commit -m "2202 Add generateSampleOutput GraphQL mutation"
```

---

## Part B — Frontend: sample-output AI bar

### Task 5: GraphQL op + hook

**Files:**
- Create: `client/src/graphql/platform/copilot/generateSampleOutput.graphql`
- Modify (generated): `client/src/shared/middleware/graphql-types.ts`, `graphql.ts`
- Create: `client/src/shared/components/copilot/useGenerateSampleOutput.ts`

- [ ] **Step 1: Create the GraphQL operation**

```graphql
mutation generateSampleOutput($input: GenerateSampleOutputInput!) {
    generateSampleOutput(input: $input) {
        value
        valid
        message
    }
}
```

- [ ] **Step 2: Regenerate types**

Run: `cd client && npx graphql-codegen`
(If `useGenerateSampleOutputMutation` does not appear in `graphql.ts`, re-run `node_modules/.bin/graphql-codegen` — codegen can serve a stale cache on first run.)

Verify: `cd client && grep -n "useGenerateSampleOutputMutation" src/shared/middleware/graphql.ts`
Expected: present.

- [ ] **Step 3: Create the hook**

```typescript
import {GenerateSampleOutputInput, useGenerateSampleOutputMutation} from '@/shared/middleware/graphql';

export function useGenerateSampleOutput() {
    const {isPending, mutateAsync} = useGenerateSampleOutputMutation();

    const generate = async (input: GenerateSampleOutputInput) => {
        const data = await mutateAsync({input});

        return data.generateSampleOutput;
    };

    return {generate, isPending};
}
```

- [ ] **Step 4: Type-check + commit**

Run: `cd client && npm run typecheck`
Expected: no errors.

```bash
git add client/src/graphql/platform/copilot/generateSampleOutput.graphql \
        client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts \
        client/src/shared/components/copilot/useGenerateSampleOutput.ts
git commit -m "2202 client - Add generateSampleOutput op and hook"
```

---

### Task 6: `SampleOutputCopilotBar`

**Files:**
- Create: `client/src/shared/components/copilot/SampleOutputCopilotBar.tsx`
- Create: `client/src/shared/components/copilot/SampleOutputCopilotBar.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import SampleOutputCopilotBar from './SampleOutputCopilotBar';

const {generateMock} = vi.hoisted(() => ({generateMock: vi.fn()}));

vi.mock('./useGenerateSampleOutput', () => ({
    useGenerateSampleOutput: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

describe('SampleOutputCopilotBar', () => {
    beforeEach(() => {
        generateMock.mockReset();
    });

    it('generates a sample and applies the raw JSON string', async () => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({message: null, valid: true, value: '{"id":1}'});

        render(
            <SampleOutputCopilotBar
                currentEditorIsEmpty={true}
                environmentId={1}
                onApply={onApply}
                workflowId="wf1"
            />
        );

        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'an order'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await vi.waitFor(() => expect(onApply).toHaveBeenCalledWith('{"id":1}'));
    });

    it('renders nothing when workflowId is undefined', () => {
        const {container} = render(
            <SampleOutputCopilotBar
                currentEditorIsEmpty={true}
                environmentId={1}
                onApply={vi.fn()}
                workflowId={undefined}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
```

- [ ] **Step 2: Run to verify fail**

Run: `cd client && npx vitest run src/shared/components/copilot/SampleOutputCopilotBar.test.tsx`
Expected: FAIL — module missing.

- [ ] **Step 3: Implement**

```tsx
import Button from '@/components/Button/Button';
import {Textarea} from '@/components/ui/textarea';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Loader2Icon, SparklesIcon} from 'lucide-react';
import {useState} from 'react';

import {useGenerateSampleOutput} from './useGenerateSampleOutput';

interface SampleOutputCopilotBarPropsI {
    currentEditorIsEmpty: boolean;
    environmentId: number;
    onApply: (value: string) => void;
    workflowId?: string;
}

const SampleOutputCopilotBar = ({
    currentEditorIsEmpty,
    environmentId,
    onApply,
    workflowId,
}: SampleOutputCopilotBarPropsI) => {
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const [error, setError] = useState<string | null>(null);
    const [prompt, setPrompt] = useState('');

    const {generate, isPending} = useGenerateSampleOutput();

    if (!ai.copilot.enabled || !ff1570 || !workflowId) {
        return null;
    }

    const handleGenerate = async () => {
        setError(null);

        try {
            const result = await generate({environmentId, prompt, workflowId});

            if (!result.valid) {
                setError(result.message ?? 'The generated sample output could not be validated.');

                return;
            }

            if (!currentEditorIsEmpty && !window.confirm('Replace the current sample data?')) {
                return;
            }

            onApply(result.value);
        } catch (generateError) {
            setError(generateError instanceof Error ? generateError.message : 'Generation failed.');
        }
    };

    return (
        <div className="flex flex-col gap-1 rounded-md border border-input bg-surface-neutral-primary p-2">
            <div className="flex items-center gap-1 text-sm font-medium text-content-neutral-primary">
                <SparklesIcon className="size-4" />

                <span>Generate with AI</span>
            </div>

            <Textarea
                className="min-h-14 resize-none text-sm"
                onChange={(event) => setPrompt(event.target.value)}
                placeholder="Describe the sample output you want…"
                value={prompt}
            />

            <div className="flex justify-end">
                <Button
                    disabled={isPending || prompt.trim().length === 0}
                    icon={isPending ? <Loader2Icon className="animate-spin" /> : undefined}
                    onClick={handleGenerate}
                    size="xs"
                >
                    {isPending ? 'Generating…' : 'Generate'}
                </Button>
            </div>

            {error && <span className="text-xs text-destructive">{error}</span>}
        </div>
    );
};

export default SampleOutputCopilotBar;
```

- [ ] **Step 4: Run to verify pass**

Run: `cd client && npx vitest run src/shared/components/copilot/SampleOutputCopilotBar.test.tsx`
Expected: PASS (2 tests).

- [ ] **Step 5: Lint + commit**

Run: `cd client && npx eslint src/shared/components/copilot/SampleOutputCopilotBar.tsx`
Expected: clean.

```bash
git add client/src/shared/components/copilot/SampleOutputCopilotBar.tsx \
        client/src/shared/components/copilot/SampleOutputCopilotBar.test.tsx
git commit -m "2202 client - Add SampleOutputCopilotBar"
```

---

### Task 7: Wire the bar into `OutputTabSampleDataDialog` (+ thread ids from `OutputTab`)

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx`

- [ ] **Step 1: Add optional props to the dialog and render the bar**

In `OutputTabSampleDataDialog.tsx`, add the import (sorted):

```typescript
import SampleOutputCopilotBar from '@/shared/components/copilot/SampleOutputCopilotBar';
```

Extend the props interface (keys alphabetical):

```typescript
interface OutputTabSampleDataDialogProps {
    environmentId?: number;
    onClose: () => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
    workflowId?: string;
}
```

Destructure the new props (alphabetical):

```typescript
const OutputTabSampleDataDialog = ({
    environmentId,
    onClose,
    onUpload,
    open,
    placeholder,
    workflowId,
}: OutputTabSampleDataDialogProps) => {
```

Render the bar directly above the editor wrapper `<div className="relative mt-4 ...">`:

```tsx
                {environmentId !== undefined && (
                    <SampleOutputCopilotBar
                        currentEditorIsEmpty={rawValue.trim().length === 0}
                        environmentId={environmentId}
                        onApply={(value) => {
                            setRawValue(value);

                            try {
                                setParsedValue(JSON.parse(value));
                            } catch {
                                setParsedValue(undefined);
                            }
                        }}
                        workflowId={workflowId}
                    />
                )}
```

- [ ] **Step 2: Pass ids from `OutputTab`**

In `OutputTab.tsx`, add the env-store import (sorted):

```typescript
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
```

Add the store hook near the other hooks (respect ordering — with other store hooks):

```typescript
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
```

Pass both props at the `<OutputTabSampleDataDialog ... />` render site (alphabetical among existing props):

```tsx
                    <OutputTabSampleDataDialog
                        environmentId={currentEnvironmentId}
                        onClose={() => setShowUploadDialog(false)}
                        onUpload={handleSampleDataDialogUpload}
                        open={showUploadDialog}
                        placeholder={placeholder || sampleOutput}
                        workflowId={workflowId}
                    />
```

- [ ] **Step 3: Type-check + run the dialog's existing test (ensure no regression)**

Run: `cd client && npm run typecheck`
Expected: no errors.

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.test.tsx`
Expected: PASS (the existing test renders the dialog without `environmentId`, so the bar stays hidden — no regression).

- [ ] **Step 4: Lint + commit**

Run: `cd client && npx eslint src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx`
Expected: clean.

```bash
git add client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog.tsx \
        client/src/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab.tsx
git commit -m "2202 client - Wire AI sample-output bar into output-tab sample dialog"
```

---

## Part C — Verification

### Task 8: Full server + client checks

- [ ] **Step 1: Server format + module checks**

```bash
./gradlew spotlessApply
```
Then revert any reformatting in files NOT part of this change (e.g. pre-existing drift), staging only your own files. Then:

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:test \
          :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:check
```
Expected: api + graphql `check` SUCCESSFUL; service `test` SUCCESSFUL. (Note: `:ai-copilot-service:check` has a KNOWN pre-existing PMD `EmptyCatchBlock` failure in `agent/WorkflowExecutionSpringAIAgent.java`, unrelated to this work — run `:ai-copilot-service:test` instead of `:check` for the service module, or expect that single pre-existing violation.)

- [ ] **Step 2: Client full check**

```bash
cd client && npm run check
```
Expected: lint + typecheck + tests pass.

- [ ] **Step 3: Commit any spotless reformatting of your files**

```bash
git add -A
git commit -m "2202 Apply spotless formatting" || echo "nothing to format"
```

---

## Self-Review

**Spec coverage:**
- Backend api/service/prompt/generator (object-or-array validate + repair, metric) → Tasks 1–3. ✓
- Mutation + IDOR guard → Task 4. ✓
- op/hook, surface-agnostic bar, wire into the live output-tab dialog → Tasks 5–7. ✓
- Dead `PropertyJsonSchemaBuilderSampleDataDialog` intentionally excluded (spec "Discovered constraint"); bar built reusable so a future live surface needs no backend change. ✓
- Gating `ff-1570` + `ai.copilot.enabled` → enforced in `SampleOutputCopilotBar`. ✓
- EE conventions / verification → headers on every new server file; Task 8. ✓

**Type consistency:** `GenerateSampleOutputInput(workflowId, prompt, environmentId)` matches the GraphQL input, the Java record, the request record, and the client hook input `{environmentId, prompt, workflowId}`. Result `{value, valid, message}` matches `SampleOutputCopilotResult`, the payload, and the bar's `result.valid`/`result.value`/`result.message` usage. `onApply(string)` matches both the bar and the dialog's `setRawValue(value)`. ✓

**Placeholder scan:** every code step has full code; commands have expected output; the one environmental nuance (service `:check` pre-existing PMD) is called out explicitly with the workaround. ✓
