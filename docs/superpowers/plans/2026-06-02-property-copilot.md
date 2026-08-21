# Property Copilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an EE-only, single-shot, mode-aware "Property Copilot": a `✦` trigger above a workflow property opens a popover where the user describes what they want, and the field is filled with text containing `${datapill}` references (text mode) or a validated SpEL `=formula` (formula mode), grounded in the previous step outputs available to that field.

**Architecture:** A focused EE GraphQL mutation `generatePropertyValue` (mirroring `generateSpecification`) backed by a `PropertyCopilotGenerator` service that (1) grounds on `WorkflowNodeOutputFacade` previous-node outputs + sample values, (2) for formula mode also grounds on the evaluator function catalog, (3) calls a Spring AI `ChatModel` once, (4) validates/repairs formulas via the SpEL `Evaluator` against the sample outputs as context. The client adds an inline popover (trigger in the controls row above the field) that inserts the result into the existing `PropertyMentionsInputEditor`.

**Tech Stack:** Java 25 / Spring Boot, Spring GraphQL, Spring AI (`ChatModel`), JUnit 5 + Mockito; React 19 + TypeScript, TanStack React Query, GraphQL codegen, Vitest.

---

## Spec reference

`docs/superpowers/specs/2026-06-02-property-copilot-design.md`

## Conventions

- Repo root: `/Volumes/Data/bytechef/bytechef`. Server commands from root; client commands from `client/`.
- **Commit message convention for this feature:** `client - copilot - <desc>` for client commits; `0_732 copilot - <desc>` for server commits.
- EE files use the ByteChef Enterprise license header and a `@version ee` Javadoc tag (see existing files in the same modules for the exact header text — copy it verbatim).
- Server build: `./gradlew :module:path:compileJava` for a single module; `./gradlew spotlessApply` before server commits.
- Single client test: `cd client && npx vitest run <path>`. Single client typecheck/lint: `npx tsc --noEmit` / `npx eslint <path>`.
- Git hygiene: stage only the files each task lists, by explicit path. NEVER `git add -A` — the repo has unrelated pre-existing changes.

## Grounding facts (verified — use these exact APIs)

- **Previous outputs facade:** `com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade`
  - `List<WorkflowNodeOutputDTO> getPreviousWorkflowNodeOutputs(String workflowId, String lastWorkflowNodeName, long environmentId)`
  - `Map<String, ?> getPreviousWorkflowNodeSampleOutputs(String workflowId, String lastWorkflowNodeName, long environmentId)`
  - `WorkflowNodeOutputDTO` has `String workflowNodeName()` and `Object getSampleOutput()`.
- **Formula validation:** `com.bytechef.evaluator.Evaluator.evaluate(Map<String,?> map, Map<String,?> context, boolean lenient)`. With `lenient=false` it throws on an invalid/unresolvable expression. Validate `{"value": "=formula"}` against the sample-outputs map as context.
- **Function catalog:** inject `List<com.bytechef.evaluator.EvaluatorFunctionDefinitionFactory>`; each has `getDefinitions()` returning `List<EvaluatorFunctionDefinition>` with `name()`, `title()`, `description()`.
- **One-shot LLM call (pattern from `ApiConnectorAiServiceImpl`):**
  `String text = chatModel.call(new Prompt(promptString)).getResult().getOutput().getText();`
- **EE module template:** `server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-graphql` (controller `ApiConnectorGraphQlController`, `.graphqls` using `extend type Mutation`, `build.gradle.kts`).
- **Existing ai-copilot modules:** `ai-copilot-api`, `ai-copilot-rest`, `ai-copilot-service` under `server/ee/libs/ai/ai-copilot/`. `ai-copilot-service` already depends on `platform-configuration-api` and has `ChatModel` available; `WorkflowEditorSpringAIAgent` there already calls `workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(...)`.
- **Datapill path format (client):** `${nodeName.field.subfield}`, arrays `[0]` (see `DataPill.buildMentionId`). The model must emit references in this form.
- **Gating:** `@ConditionalOnEEVersion` (= `bytechef.edition=ee`) + `@ConditionalOnProperty(prefix="bytechef.ai.copilot", name="enabled", havingValue="true")`. Client: `useApplicationInfoStore` `ai.copilot.enabled` + the copilot feature flag (see `CopilotButton`).

## File structure

**Server — `ai-copilot-api` (new files):**
- `PropertyCopilotMode.java` — enum `TEXT, FORMULA`.
- `PropertyCopilotRequest.java` — record `(String prompt, PropertyCopilotMode mode, String workflowId, String workflowNodeName, String propertyPath, String propertyType, long environmentId)`.
- `PropertyCopilotResult.java` — record `(String value, boolean valid, String message)`.
- `PropertyCopilotGenerator.java` — interface `PropertyCopilotResult generate(PropertyCopilotRequest request)`.

**Server — `ai-copilot-service` (new files):**
- `PropertyCopilotPromptBuilder.java` — pure prompt assembly.
- `PropertyCopilotGeneratorImpl.java` — grounding + LLM + formula validation/repair + metric.

**Server — new module `ai-copilot-graphql`:**
- `build.gradle.kts`, settings registration.
- `resources/graphql/property-copilot.graphqls`.
- `web/graphql/PropertyCopilotGraphQlController.java` + co-located `GeneratePropertyValueInput`/`GeneratePropertyValuePayload` records + `GraphQlPropertyCopilotMode` enum.

**Client — new:**
- `src/graphql/platform/copilot/generatePropertyValue.graphql` + regenerated `graphql.ts`.
- `useGeneratePropertyValue.ts` hook.
- `PropertyCopilotButton.tsx`, `PropertyCopilotPopover.tsx` (+ tests).

**Client — modified:**
- `Property.tsx` — render the trigger in the controls row after `PropertyInputTypeSwitch`; insert result into the field.

---

## Task 1: API types in `ai-copilot-api`

**Files (create):** under `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/property/`
- `PropertyCopilotMode.java`, `PropertyCopilotRequest.java`, `PropertyCopilotResult.java`, `PropertyCopilotGenerator.java`

- [ ] **Step 1: Create the enum, records, and interface**

`PropertyCopilotMode.java`:
```java
/*
 * <COPY the ByteChef Enterprise license header verbatim from another file in ai-copilot-api>
 */

package com.bytechef.ee.ai.copilot.property;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum PropertyCopilotMode {
    TEXT, FORMULA
}
```

`PropertyCopilotRequest.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PropertyCopilotRequest(
    String prompt, PropertyCopilotMode mode, String workflowId, String workflowNodeName, String propertyPath,
    String propertyType, long environmentId) {
}
```

`PropertyCopilotResult.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PropertyCopilotResult(String value, boolean valid, String message) {
}
```

`PropertyCopilotGenerator.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface PropertyCopilotGenerator {

    PropertyCopilotResult generate(PropertyCopilotRequest request);
}
```

- [ ] **Step 2: Compile the module**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Spotless + commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/property/
git commit -m "0_732 copilot - Add Property Copilot API types"
```

---

## Task 2: `PropertyCopilotPromptBuilder` (pure, TDD)

**Files:** under `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/`
- Create: `PropertyCopilotPromptBuilder.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilderTest.java`

The builder turns a request + grounding strings into one prompt. It is deterministic and unit-testable with no Spring context.

- [ ] **Step 1: Write the failing test**

`PropertyCopilotPromptBuilderTest.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PropertyCopilotPromptBuilderTest {

    private final PropertyCopilotPromptBuilder promptBuilder = new PropertyCopilotPromptBuilder();

    @Test
    void testBuildTextModeIncludesPromptAndOutputsAndPillInstruction() {
        PropertyCopilotRequest request = new PropertyCopilotRequest(
            "greet the customer by first name", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", 0);

        String prompt = promptBuilder.build(request, "trigger_1: {\"firstName\":\"Ada\"}\n", "");

        assertThat(prompt).contains("greet the customer by first name");
        assertThat(prompt).contains("trigger_1");
        assertThat(prompt).contains("${");
        assertThat(prompt).doesNotContain("function");
    }

    @Test
    void testBuildFormulaModeIncludesFunctionCatalogAndFormulaInstruction() {
        PropertyCopilotRequest request = new PropertyCopilotRequest(
            "uppercase the city", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", 0);

        String prompt = promptBuilder.build(
            request, "trigger_1: {\"city\":\"paris\"}\n", "- upperCase(value): converts to upper case\n");

        assertThat(prompt).contains("uppercase the city");
        assertThat(prompt).contains("upperCase");
        assertThat(prompt).contains("=");
        assertThat(prompt).contains("trigger_1");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.property.PropertyCopilotPromptBuilderTest"`
Expected: FAIL (compilation error — `PropertyCopilotPromptBuilder` does not exist).

- [ ] **Step 3: Implement the builder**

`PropertyCopilotPromptBuilder.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class PropertyCopilotPromptBuilder {

    public String build(PropertyCopilotRequest request, String availableOutputs, String functionCatalog) {
        StringBuilder builder = new StringBuilder();

        builder.append(
            "You generate the value for a single workflow property based on the user's request.\n\n");
        builder.append("User request: ")
            .append(request.prompt())
            .append("\n\n");
        builder.append("Target property: ")
            .append(request.propertyPath());

        if (request.propertyType() != null) {
            builder.append(" (type ")
                .append(request.propertyType())
                .append(")");
        }

        builder.append("\n\nAvailable previous step outputs (reference these as ${nodeName.path}):\n")
            .append(availableOutputs)
            .append("\n");

        if (request.mode() == PropertyCopilotMode.FORMULA) {
            builder.append("Available functions (use ONLY these):\n")
                .append(functionCatalog)
                .append("\n");
            builder.append(
                "Return ONLY a single SpEL expression beginning with '='. Reference outputs as " +
                    "${nodeName.path}. Use only the listed functions. No explanation, no code fences.");
        } else {
            builder.append(
                "Return ONLY the literal text value for the property, embedding references to the " +
                    "outputs inline as ${nodeName.path} where appropriate. No explanation, no code fences.");
        }

        return builder.toString();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.property.PropertyCopilotPromptBuilderTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilder.java server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilderTest.java
git commit -m "0_732 copilot - Add Property Copilot prompt builder"
```

---

## Task 3: `PropertyCopilotGeneratorImpl` (TDD with mocks)

**Files:** under `server/ee/libs/ai/ai-copilot/ai-copilot-service/...`
- Create: `.../property/PropertyCopilotGeneratorImpl.java`
- Test: `.../property/PropertyCopilotGeneratorImplTest.java`

**Dependencies to add** to `server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts` (if not already present): the evaluator API and micrometer.
- `implementation(project(":server:libs:core:evaluator:evaluator-api"))`
- `implementation("io.micrometer:micrometer-core")`
(Confirm `platform-configuration-api` is already a dependency — it is.)

Behavior:
- Build grounding outputs string from `getPreviousWorkflowNodeOutputs` (each `workflowNodeName(): getSampleOutput()`), and obtain the sample-outputs `Map` from `getPreviousWorkflowNodeSampleOutputs` for formula validation context.
- For FORMULA mode, build the function catalog string from the factories.
- Call `chatModel`, get text, strip code fences/whitespace.
- FORMULA mode: ensure it starts with `=`; validate via `Evaluator.evaluate(Map.of("value", value), sampleOutputs, false)`. On exception, one repair attempt (append the error + "fix it"); re-validate. If still invalid, `valid=false` + message.
- Record `bytechef_property_copilot_generate{mode,outcome}`.

- [ ] **Step 1: Write the failing test**

`PropertyCopilotGeneratorImplTest.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.ObjectProvider;

class PropertyCopilotGeneratorImplTest {

    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);
    private final Evaluator evaluator = mock(Evaluator.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @SuppressWarnings("unchecked")
    private PropertyCopilotGeneratorImpl generatorReturning(String llmText) {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(llmText))));

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong()))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider);
    }

    @Test
    void testTextModeReturnsValueVerbatim() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("Hello ${trigger_1.firstName}");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "greet", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", 0));

        assertThat(result.value()).isEqualTo("Hello ${trigger_1.firstName}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testFormulaModeStripsFencesAndValidates() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("```\n=upperCase(${trigger_1.city})\n```");
        // evaluator does not throw -> valid
        when(evaluator.evaluate(any(), any(), eq(false))).thenReturn(Map.of("value", "PARIS"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "uppercase city", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", 0));

        assertThat(result.value()).isEqualTo("=upperCase(${trigger_1.city})");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testFormulaModeInvalidThenRepaired() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse bad = new ChatResponse(List.of(new Generation(new AssistantMessage("=bogus("))));
        ChatResponse good = new ChatResponse(List.of(new Generation(new AssistantMessage("=concat(${a})"))));

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(bad, good);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        // first validate throws, second succeeds
        when(evaluator.evaluate(any(), any(), eq(false)))
            .thenThrow(new RuntimeException("parse error"))
            .thenReturn(Map.of("value", "x"));

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider);

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "concat a", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", 0));

        assertThat(result.value()).isEqualTo("=concat(${a})");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testFormulaModeStillInvalidAfterRepairReturnsInvalid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("=bogus(");

        when(evaluator.evaluate(any(), any(), eq(false))).thenThrow(new RuntimeException("parse error"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "x", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", 0));

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotБlank();
    }
}
```
> NOTE for implementer: fix the obvious typo `isNotБlank()` → `isNotBlank()` (a guard to confirm you read the test). Also adjust imports if your Spring AI version's `ChatResponse`/`Generation`/`AssistantMessage` constructors differ — verify against the version on the classpath and adapt the mock construction minimally while keeping the assertions identical.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.property.PropertyCopilotGeneratorImplTest"`
Expected: FAIL (compilation — impl does not exist).

- [ ] **Step 3: Implement the generator**

`PropertyCopilotGeneratorImpl.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.property;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.EvaluatorFunctionDefinition;
import com.bytechef.evaluator.EvaluatorFunctionDefinitionFactory;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class PropertyCopilotGeneratorImpl implements PropertyCopilotGenerator {

    private final ChatModel chatModel;
    private final Evaluator evaluator;
    private final PropertyCopilotPromptBuilder promptBuilder;
    private final List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public PropertyCopilotGeneratorImpl(
        ChatModel chatModel, Evaluator evaluator, PropertyCopilotPromptBuilder promptBuilder,
        List<EvaluatorFunctionDefinitionFactory> evaluatorFunctionDefinitionFactories,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.chatModel = chatModel;
        this.evaluator = evaluator;
        this.promptBuilder = promptBuilder;
        this.evaluatorFunctionDefinitionFactories = evaluatorFunctionDefinitionFactories;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public PropertyCopilotResult generate(PropertyCopilotRequest request) {
        String availableOutputs = buildAvailableOutputs(request);
        String functionCatalog =
            request.mode() == PropertyCopilotMode.FORMULA ? buildFunctionCatalog() : "";

        String prompt = promptBuilder.build(request, availableOutputs, functionCatalog);
        String value = clean(call(prompt));

        if (request.mode() != PropertyCopilotMode.FORMULA) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        if (!value.startsWith("=")) {
            value = "=" + value;
        }

        Map<String, ?> context = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        if (isValidFormula(value, context)) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        String repaired = clean(call(prompt +
            "\n\nThe previous attempt was not a valid expression. Return a corrected single '=' expression."));

        if (!repaired.startsWith("=")) {
            repaired = "=" + repaired;
        }

        if (isValidFormula(repaired, context)) {
            record(request, "success");

            return new PropertyCopilotResult(repaired, true, null);
        }

        record(request, "invalid_formula");

        return new PropertyCopilotResult(
            repaired, false, "The generated formula could not be validated; please review it.");
    }

    private String buildAvailableOutputs(PropertyCopilotRequest request) {
        List<WorkflowNodeOutputDTO> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            request.workflowId(), request.workflowNodeName(), request.environmentId());

        StringBuilder builder = new StringBuilder("\n");

        for (WorkflowNodeOutputDTO output : outputs) {
            builder.append(output.workflowNodeName())
                .append(": ")
                .append(output.getSampleOutput())
                .append("\n");
        }

        return builder.toString();
    }

    private String buildFunctionCatalog() {
        StringBuilder builder = new StringBuilder();

        for (EvaluatorFunctionDefinitionFactory factory : evaluatorFunctionDefinitionFactories) {
            for (EvaluatorFunctionDefinition definition : factory.getDefinitions()) {
                builder.append("- ")
                    .append(definition.name())
                    .append(": ")
                    .append(definition.description())
                    .append("\n");
            }
        }

        return builder.toString();
    }

    private String call(String prompt) {
        return chatModel.call(new Prompt(prompt))
            .getResult()
            .getOutput()
            .getText();
    }

    private boolean isValidFormula(String value, Map<String, ?> context) {
        try {
            evaluator.evaluate(Map.of("value", value), context, false);

            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("```", "")
            .strip();
    }

    private void record(PropertyCopilotRequest request, String outcome) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_property_copilot_generate")
            .tag("mode", request.mode()
                .name())
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "com.bytechef.ee.ai.copilot.property.PropertyCopilotGeneratorImplTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImpl.java server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImplTest.java server/ee/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts
git commit -m "0_732 copilot - Add Property Copilot generator service"
```

---

## Task 4: New `ai-copilot-graphql` module + mutation

**Files:**
- Create module dir `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/`
  - `build.gradle.kts`
  - `src/main/resources/graphql/property-copilot.graphqls`
  - `src/main/java/com/bytechef/ee/ai/copilot/web/graphql/PropertyCopilotGraphQlController.java`
- Modify: `settings.gradle.kts` (register the module)
- Modify: every app `build.gradle.kts` that already depends on `platform-api-connector-configuration-graphql` (mirror the dependency)
- Modify: `client/codegen.ts` (add the schema glob)

- [ ] **Step 1: `build.gradle.kts`** (mirror the api-connector-graphql module)

`server/ee/libs/ai/ai-copilot/ai-copilot-graphql/build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
}
```

- [ ] **Step 2: Register the module in `settings.gradle.kts`**

Add immediately after the line `include("server:ee:libs:ai:ai-copilot:ai-copilot-service")`:
```kotlin
include("server:ee:libs:ai:ai-copilot:ai-copilot-graphql")
```

- [ ] **Step 3: GraphQL schema**

`server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/property-copilot.graphqls`:
```graphql
extend type Mutation {
    generatePropertyValue(input: GeneratePropertyValueInput!): GeneratePropertyValuePayload!
}

enum PropertyCopilotMode {
    TEXT
    FORMULA
}

input GeneratePropertyValueInput {
    prompt: String!
    mode: PropertyCopilotMode!
    workflowId: ID!
    workflowNodeName: String!
    propertyPath: String!
    propertyType: String
    environmentId: Int!
}

type GeneratePropertyValuePayload {
    value: String!
    valid: Boolean!
    message: String
}
```

- [ ] **Step 4: Controller (+ co-located input/payload types)**

`.../web/graphql/PropertyCopilotGraphQlController.java`:
```java
/*
 * <EE license header>
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class PropertyCopilotGraphQlController {

    private final PropertyCopilotGenerator propertyCopilotGenerator;

    public PropertyCopilotGraphQlController(PropertyCopilotGenerator propertyCopilotGenerator) {
        this.propertyCopilotGenerator = propertyCopilotGenerator;
    }

    @MutationMapping
    public GeneratePropertyValuePayload generatePropertyValue(@Argument GeneratePropertyValueInput input) {
        PropertyCopilotResult result = propertyCopilotGenerator.generate(new PropertyCopilotRequest(
            input.prompt(), PropertyCopilotMode.valueOf(input.mode()
                .name()),
            input.workflowId(), input.workflowNodeName(), input.propertyPath(), input.propertyType(),
            input.environmentId()));

        return new GeneratePropertyValuePayload(result.value(), result.valid(), result.message());
    }

    public enum GraphQlPropertyCopilotMode {
        TEXT, FORMULA
    }

    public record GeneratePropertyValueInput(
        String prompt, GraphQlPropertyCopilotMode mode, String workflowId, String workflowNodeName,
        String propertyPath, String propertyType, int environmentId) {
    }

    public record GeneratePropertyValuePayload(String value, boolean valid, String message) {
    }
}
```
> NOTE: Spring GraphQL maps the `PropertyCopilotMode` enum argument to a Java enum; the controller's nested `GraphQlPropertyCopilotMode` mirrors the schema enum and is converted to the API `PropertyCopilotMode`. If Spring GraphQL binds the schema enum directly to `PropertyCopilotMode`, simplify by using that type directly — verify during the boot test and adjust.

- [ ] **Step 5: Add the schema glob to `client/codegen.ts`**

After the line ending `.../platform-api-connector-configuration-graphql/src/main/resources/graphql/*.graphqls',` add:
```ts
        '../server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 6: Register the module as an app dependency**

Run: `grep -rln "platform-api-connector-configuration-graphql" --include=build.gradle.kts server | sort`
For EACH `build.gradle.kts` listed (these are the apps/aggregators that expose graphql controllers), add a sibling dependency line mirroring its existing api-connector-graphql line, e.g.:
```kotlin
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-graphql"))
```
(Match the exact `implementation(...)`/`api(...)` form used on the api-connector-graphql line in that file.)

- [ ] **Step 7: Compile the new module + an app**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:compileJava`
Expected: BUILD SUCCESSFUL.
Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-copilot/ai-copilot-graphql settings.gradle.kts client/codegen.ts $(grep -rl "platform-api-connector-configuration-graphql" --include=build.gradle.kts server)
git commit -m "0_732 copilot - Add Property Copilot GraphQL mutation"
```

---

## Task 5: Client GraphQL operation + codegen

**Files:**
- Create: `client/src/graphql/platform/copilot/generatePropertyValue.graphql`
- Modify (generated): `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Operation**

`client/src/graphql/platform/copilot/generatePropertyValue.graphql`:
```graphql
mutation generatePropertyValue($input: GeneratePropertyValueInput!) {
    generatePropertyValue(input: $input) {
        value
        valid
        message
    }
}
```

- [ ] **Step 2: Codegen**

Run: `cd client && npx graphql-codegen`
Expected: completes; `src/shared/middleware/graphql.ts` modified.

- [ ] **Step 3: Verify hook generated**

Run: `cd client && grep -n "useGeneratePropertyValueMutation" src/shared/middleware/graphql.ts`
Expected: a match.

- [ ] **Step 4: Typecheck + commit**

Run: `cd client && npx tsc --noEmit` → PASS.
```bash
git add client/src/graphql/platform/copilot/generatePropertyValue.graphql client/src/shared/middleware/graphql.ts
git commit -m "client - copilot - Add generatePropertyValue GraphQL operation"
```

---

## Task 6: `useGeneratePropertyValue` hook

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/useGeneratePropertyValue.ts`
- Test: same dir, `useGeneratePropertyValue.test.ts`

- [ ] **Step 1: Write the failing test**
```ts
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useGeneratePropertyValue} from './useGeneratePropertyValue';

const mutateAsyncMock = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useGeneratePropertyValueMutation: () => ({isPending: false, mutateAsync: mutateAsyncMock}),
}));

describe('useGeneratePropertyValue', () => {
    beforeEach(() => mutateAsyncMock.mockReset());

    it('calls the mutation with the built input and returns the payload', async () => {
        mutateAsyncMock.mockResolvedValue({generatePropertyValue: {message: null, valid: true, value: '=x'}});

        const {result} = renderHook(() => useGeneratePropertyValue());

        const payload = await result.current.generate({
            environmentId: 0,
            mode: 'FORMULA',
            prompt: 'x',
            propertyPath: 'p',
            propertyType: 'STRING',
            workflowId: 'wf1',
            workflowNodeName: 'n1',
        });

        expect(mutateAsyncMock).toHaveBeenCalledWith({
            input: {
                environmentId: 0,
                mode: 'FORMULA',
                prompt: 'x',
                propertyPath: 'p',
                propertyType: 'STRING',
                workflowId: 'wf1',
                workflowNodeName: 'n1',
            },
        });
        expect(payload).toEqual({message: null, valid: true, value: '=x'});
    });
});
```

- [ ] **Step 2: Run → FAIL** (`cd client && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-copilot/useGeneratePropertyValue.test.ts`).

- [ ] **Step 3: Implement**
```ts
import {GeneratePropertyValueInput, useGeneratePropertyValueMutation} from '@/shared/middleware/graphql';

export function useGeneratePropertyValue() {
    const {isPending, mutateAsync} = useGeneratePropertyValueMutation();

    const generate = async (input: GeneratePropertyValueInput) => {
        const data = await mutateAsync({input});

        return data.generatePropertyValue;
    };

    return {generate, isPending};
}
```
> If the generated input type is not named `GeneratePropertyValueInput`, use the exact generated name (grep `graphql.ts`).

- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit**
```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/useGeneratePropertyValue.ts client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/useGeneratePropertyValue.test.ts
git commit -m "client - copilot - Add useGeneratePropertyValue hook"
```

---

## Task 7: `PropertyCopilotPopover` component

**Files:** under `client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/`
- Create: `PropertyCopilotPopover.tsx`
- Test: `PropertyCopilotPopover.test.tsx`

The popover renders a prompt textarea + Generate button, calls `onGenerate(prompt)`, shows a loading state while pending, and an error message on failure. It does NOT itself call the hook (kept presentational for testability); the button wrapper wires the hook.

- [ ] **Step 1: Write the failing test**
```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import PropertyCopilotPopover from './PropertyCopilotPopover';

describe('PropertyCopilotPopover', () => {
    it('submits the typed prompt via onGenerate', () => {
        const onGenerate = vi.fn();

        render(<PropertyCopilotPopover error={null} pending={false} onGenerate={onGenerate} />);

        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet the user'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        expect(onGenerate).toHaveBeenCalledWith('greet the user');
    });

    it('disables generate while pending and shows a busy label', () => {
        render(<PropertyCopilotPopover error={null} pending={true} onGenerate={vi.fn()} />);

        expect(screen.getByRole('button', {name: /generating/i})).toBeDisabled();
    });

    it('renders an error message', () => {
        render(<PropertyCopilotPopover error="boom" pending={false} onGenerate={vi.fn()} />);

        expect(screen.getByText('boom')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Implement** (use existing UI primitives — `Button` from `@/components/ui/button`, `Textarea` from `@/components/ui/textarea`; `twMerge` for classes; `SparklesIcon` from lucide-react). Keep hooks order per CLAUDE.md.
```tsx
import {Button} from '@/components/ui/button';
import {Textarea} from '@/components/ui/textarea';
import {useState} from 'react';

interface PropertyCopilotPopoverProps {
    error: string | null;
    pending: boolean;
    onGenerate: (prompt: string) => void;
}

const PropertyCopilotPopover = ({error, pending, onGenerate}: PropertyCopilotPopoverProps) => {
    const [prompt, setPrompt] = useState('');

    return (
        <div className="flex w-80 flex-col gap-2 p-1">
            <Textarea
                className="min-h-16 text-sm"
                onChange={(event) => setPrompt(event.target.value)}
                placeholder="Describe what this field should contain…"
                value={prompt}
            />

            {error && <span className="text-xs text-destructive">{error}</span>}

            <div className="flex justify-end">
                <Button
                    disabled={pending || prompt.trim().length === 0}
                    onClick={() => onGenerate(prompt)}
                    size="sm"
                >
                    {pending ? 'Generating…' : 'Generate'}
                </Button>
            </div>
        </div>
    );
};

export default PropertyCopilotPopover;
```
> Verify the import paths for `Button`/`Textarea` against the codebase (grep an existing usage). Adjust if they differ.

- [ ] **Step 4: Run → PASS. Then eslint + prettier the two files.**
- [ ] **Step 5: Commit**
```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotPopover.tsx client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotPopover.test.tsx
git commit -m "client - copilot - Add PropertyCopilotPopover component"
```

---

## Task 8: `PropertyCopilotButton` (trigger + gating + wiring)

**Files:** under `.../property-copilot/`
- Create: `PropertyCopilotButton.tsx`
- Test: `PropertyCopilotButton.test.tsx`

The button renders the `✦` trigger inside a Popover (use the existing `Popover`/`PopoverTrigger`/`PopoverContent` primitives), is hidden unless EE + `ai.copilot.enabled`, wires `useGeneratePropertyValue`, and calls an `onApply(value)` prop with the generated value (insert is the parent's responsibility). On a result with `valid === false`, it still calls `onApply` but surfaces `message` as the error text.

- [ ] **Step 1: Write the failing test**
```tsx
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCopilotButton from './PropertyCopilotButton';

const generateMock = vi.fn();
const copilotEnabledMock = vi.fn();

vi.mock('./useGeneratePropertyValue', () => ({
    useGeneratePropertyValue: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: copilotEnabledMock()}}}),
}));

describe('PropertyCopilotButton', () => {
    beforeEach(() => {
        generateMock.mockReset();
        copilotEnabledMock.mockReset().mockReturnValue(true);
    });

    const baseProps = {
        environmentId: 0,
        mode: 'TEXT' as const,
        onApply: vi.fn(),
        propertyPath: 'p',
        propertyType: 'STRING',
        workflowId: 'wf1',
        workflowNodeName: 'n1',
    };

    it('is hidden when copilot is disabled', () => {
        copilotEnabledMock.mockReturnValue(false);

        render(<PropertyCopilotButton {...baseProps} />);

        expect(screen.queryByLabelText(/copilot/i)).not.toBeInTheDocument();
    });

    it('generates and applies the value', async () => {
        generateMock.mockResolvedValue({message: null, valid: true, value: 'Hi ${n1.name}'});
        const onApply = vi.fn();

        render(<PropertyCopilotButton {...baseProps} onApply={onApply} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await waitFor(() => expect(onApply).toHaveBeenCalledWith('Hi ${n1.name}'));
        expect(generateMock).toHaveBeenCalledWith(expect.objectContaining({mode: 'TEXT', prompt: 'greet'}));
    });
});
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Implement** (verify Popover + `useApplicationInfoStore` selector shape against the codebase; adjust the gating selector to the real store API — grep `CopilotButton.tsx` for the exact gating).
```tsx
import {Button} from '@/components/ui/button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {SparklesIcon} from 'lucide-react';
import {useState} from 'react';

import PropertyCopilotPopover from './PropertyCopilotPopover';
import {useGeneratePropertyValue} from './useGeneratePropertyValue';

interface PropertyCopilotButtonProps {
    environmentId: number;
    mode: 'TEXT' | 'FORMULA';
    onApply: (value: string) => void;
    propertyPath: string;
    propertyType?: string;
    workflowId: string;
    workflowNodeName: string;
}

const PropertyCopilotButton = ({
    environmentId,
    mode,
    onApply,
    propertyPath,
    propertyType,
    workflowId,
    workflowNodeName,
}: PropertyCopilotButtonProps) => {
    const copilotEnabled = useApplicationInfoStore((state) => state.ai?.copilot?.enabled);

    const [error, setError] = useState<string | null>(null);
    const [open, setOpen] = useState(false);

    const {generate, isPending} = useGeneratePropertyValue();

    if (!copilotEnabled) {
        return null;
    }

    const handleGenerate = async (prompt: string) => {
        setError(null);

        try {
            const result = await generate({
                environmentId,
                mode,
                prompt,
                propertyPath,
                propertyType,
                workflowId,
                workflowNodeName,
            });

            onApply(result.value);

            if (result.valid) {
                setOpen(false);
            } else {
                setError(result.message ?? 'The generated value could not be validated.');
            }
        } catch (generateError) {
            setError(generateError instanceof Error ? generateError.message : 'Generation failed.');
        }
    };

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button aria-label="Ask copilot" size="icon" variant="ghost">
                    <SparklesIcon className="size-4" />
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="w-auto p-2">
                <PropertyCopilotPopover error={error} onGenerate={handleGenerate} pending={isPending} />
            </PopoverContent>
        </Popover>
    );
};

export default PropertyCopilotButton;
```

- [ ] **Step 4: Run → PASS; eslint + prettier the two files.**
- [ ] **Step 5: Commit**
```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotButton.tsx client/src/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotButton.test.tsx
git commit -m "client - copilot - Add PropertyCopilotButton trigger"
```

---

## Task 9: Wire the trigger + insertion into `Property.tsx`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/properties/Property.tsx`

The trigger renders in the controls row directly after `PropertyInputTypeSwitch` (around Property.tsx:284–292), only for mention-capable controls. `onApply(value)` sets the property value the same way a manual edit does (reuse the existing value-set path the editor already uses on change/save), so the result renders pills / enters formula mode and is undoable. `mode` is `'FORMULA'` when the field is in formula mode, else `'TEXT'`. `workflowId`, `workflowNodeName`, `propertyPath`, `propertyType`, `environmentId` come from the existing property context/stores.

- [ ] **Step 1: Read the controls-row region and value-set path**

Read `Property.tsx` around the `PropertyInputTypeSwitch` usages and identify: (a) the JSX row where the trigger goes, (b) the function used to set/persist the field value on change (the same one used for manual edits / the mentions editor's `onValueChange`/`saveProperty`), (c) how the current workflow id, node name, property path, environment id, and formula-mode flag are available in this component. Document these before editing.

- [ ] **Step 2: Add the trigger to the controls row**

In the controls row after `PropertyInputTypeSwitch`, render (only for mention-capable controls — reuse the same `mentionInput`/control-type guard the file already computes):
```tsx
{mentionInput && currentComponent && workflow.id && currentNode?.name && (
    <PropertyCopilotButton
        environmentId={environmentId}
        mode={isFormulaMode ? 'FORMULA' : 'TEXT'}
        onApply={handleCopilotApply}
        propertyPath={path ?? name}
        propertyType={type}
        workflowId={workflow.id as string}
        workflowNodeName={currentNode.name}
    />
)}
```
Add the import:
```tsx
import PropertyCopilotButton from '@/pages/platform/workflow-editor/components/properties/components/property-copilot/PropertyCopilotButton';
```
(Use the actual variable names found in Step 1 for environment id, formula-mode flag, path, type, node, and workflow. If `environmentId` is not directly available here, obtain it from the same store the rest of the editor uses — identify in Step 1.)

- [ ] **Step 3: Implement `handleCopilotApply`**

Define a handler that sets the field value through the SAME path a manual edit uses (identified in Step 1). For a value entering formula mode, set the field's formula-mode state if the value starts with `=` (mirror how typing `=` toggles it). Concretely, route the value into the mentions editor's value-set/save path so `${...}` refs serialize to pills.
```tsx
const handleCopilotApply = (value: string) => {
    // setValue/onValueChange/saveProperty — use the exact function identified in Step 1.
    handlePropertyValueApply(value);
};
```
> This step's exact code depends on Step 1's findings; the implementer fills in the real value-set call. If the wiring is non-trivial (e.g., requires reaching into the editor ref), STOP and report DONE_WITH_CONCERNS describing what the value-set path looks like, so the controller can confirm the approach.

- [ ] **Step 4: Typecheck + lint**

Run: `cd client && npx tsc --noEmit` → no new errors; `npx eslint src/pages/platform/workflow-editor/components/properties/Property.tsx` → clean.

- [ ] **Step 5: Manual-reasoning check (no automated UI test for Property.tsx)**

Property.tsx is a large integration component; do not add a full render test. Instead confirm by reading that: trigger appears only for mention-capable controls; `mode` reflects formula state; `onApply` routes through the existing value-set path. Note this reasoning in the report.

- [ ] **Step 6: Commit**
```bash
git add client/src/pages/platform/workflow-editor/components/properties/Property.tsx
git commit -m "client - copilot - Wire Property Copilot trigger into property controls row"
```

---

## Task 10: Full checks

- [ ] **Step 1: Server** — compile the touched modules + spotless/check.
Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava :server:ee:libs:ai:ai-copilot:ai-copilot-service:test :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:compileJava :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL; generator tests pass.
Run: `./gradlew spotlessApply`

- [ ] **Step 2: Boot smoke (schema loads)** — start the server app long enough to confirm the GraphQL schema parses with the new types (no startup schema error). If a full boot is impractical in the worker environment, instead run any existing GraphQL schema/wiring test in the repo over the aggregated schema, or document that boot verification is pending and must be done manually.

- [ ] **Step 3: Client** — `cd client && npm run check`.
Expected: PASS for the new/changed files. (The pre-existing `AiHubChatComposer.tsx` Prettier issue is unrelated; do not fix it. If `npm run check` is red ONLY due to that file, run the stages individually — `prettier --check` on changed files, `eslint src`, `tsc --noEmit`, `vitest run <copilot dirs>` — and confirm those pass.)

- [ ] **Step 4: Manual smoke (documented)** — In EE with `bytechef.ai.copilot.enabled=true`: open a workflow property, click `✦`, type a request in text mode → field fills with text + pills; switch to formula mode (`=`) → generates a `=formula`; an invalid formula surfaces the validation message. Not automated.

- [ ] **Step 5: Final commit (only if Step 1/3 required formatting fixes)**
```bash
git add <only the files you reformatted>
git commit -m "client - copilot - Fix formatting for Property Copilot"
```

---

## Self-Review (plan author)

- **Spec coverage:** mutation → Task 4; generator/grounding/validation/repair → Task 3; prompt grounding incl. function catalog → Tasks 2–3; previous-output awareness → Task 3 via `WorkflowNodeOutputFacade`; inline popover + trigger-above-field-after-dynamic-toggle → Tasks 7–9; direct editable insert → Task 9; mode-aware → Tasks 8–9; EE gating → Tasks 3,4,8; metrics → Task 3; client operation/hook → Tasks 5–6; testing matrix → Tasks 2,3,6,7,8.
- **Placeholder scan:** the only intentionally-deferred specifics are Task 9 Step 1/3 (the exact `Property.tsx` value-set path) and Spring AI mock constructor shapes (Task 3) — both are explicit "read X, then fill in / adapt" investigation steps with a STOP-and-report fallback, not vague hand-waving. The deliberate `isNotБlank()` typo in Task 3 is a read-the-test guard with an explicit correction note.
- **Type consistency:** `PropertyCopilotMode {TEXT,FORMULA}`, `PropertyCopilotRequest`, `PropertyCopilotResult`, `PropertyCopilotGenerator.generate` consistent across Tasks 1/3/4; GraphQL `generatePropertyValue(input)` → payload `{value,valid,message}` consistent across Tasks 4/5/6; client `generate(input)` input shape consistent across Tasks 6/8.
- **Known risks flagged in-plan:** Spring GraphQL enum binding (Task 4 note), Spring AI response-constructor version drift (Task 3 note), Property.tsx value-set wiring (Task 9 STOP-and-report), app-dependency registration via grep (Task 4 Step 6), boot/schema verification (Task 10 Step 2).
```
