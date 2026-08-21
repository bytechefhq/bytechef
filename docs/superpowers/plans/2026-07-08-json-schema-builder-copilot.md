# JSON Schema Builder Copilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the existing `CopilotPanel` chat as a toggleable right-hand column of the JSON Schema Builder sheet, backed by a new `JSON_SCHEMA_BUILDER` chat agent that sees the live schema each turn and auto-applies full-schema updates back into the builder.

**Architecture:** Mirror the existing code-editor dialog integration end-to-end. Client: add a `Source.JSON_SCHEMA_BUILDER`, restructure the sheet into a flex row with a header toggle, drive a nested copilot conversation (save/reset/generate/setContext on open, restore on close), send the live schema each turn via a state contributor, and apply the agent's `updateJsonSchema` tool result back into the builder via the tool-result + post-turn registries. Server: add matching `Source`/`CopilotAgentType` members, a `JsonSchemaTools.updateJsonSchema` `@Tool`, a `JsonSchemaBuilderSpringAIAgent` + ASK/BUILD beans with prompt resources, and a controller routing branch.

**Tech Stack:** React 19 + TypeScript + Zustand + assistant-ui (`HttpAgent`/AG-UI); Java 25 + Spring Boot 4 + Spring AI (`ToolCallback`, `ChatClient`, `LocalAgent`). Client tests: Vitest + Testing Library. Server tests: JUnit 5 + Mockito.

## Global Constraints

- Client object keys must be in natural ascending (alphabetical) order (ESLint `sort-keys`, not auto-fixed).
- Client interface names end with `I` or `Props`; `useRef` vars end with `Ref`; Lucide icons imported with `Icon` suffix; use `twMerge` (not `cn()`).
- Vitest store mocks that reference module-scope refs must use `vi.hoisted(...)`.
- Java: one blank line before control statements and after a variable modification that precedes its use; no `_` prefix on private methods; descriptive names; EE files (`server/ee/`) use the ByteChef Enterprise header + `@version ee`, non-EE use Apache 2.0 header.
- Run before committing — server: `./gradlew spotlessApply` then `./gradlew check`; client (in `client/`): `npm run format` then `npm run check`.
- Copilot availability gate (reuse verbatim): `ai.copilot.enabled && ff-1570 && workflowId && workflowNodeName && environmentId`.
- New enum members are appended at the END of their enum (no ordinal-stability test pins these, but keep convention).
- New endpoint for this source: `POST /api/platform/internal/ai/chat/json_schema_builder` (client builds it as `Source[key].toLowerCase()`).

---

## File Structure

**Client (new/modified)**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts` — add `Source.JSON_SCHEMA_BUILDER`.
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.ts` — open/close + registries + contributor.
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx` — flex-row layout, header toggle, embed `CopilotPanel`.
- Tests: `...property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.test.ts`, `...PropertyJsonSchemaBuilderSheet.test.tsx`.

**Server (new/modified)**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java` — add `JSON_SCHEMA_BUILDER`.
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java` — add ASK/BUILD/fallback members.
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/JsonSchemaTools.java` — `@Tool updateJsonSchema`.
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/JsonSchemaBuilderSpringAIAgent.java` — agent class.
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_json_schema_builder_ask.txt` and `prompt_json_schema_builder_build.txt`.
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java` — prompt `@Value`s + ASK/BUILD agent beans.
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java` — routing branch.
- Tests: `JsonSchemaToolsTest.java`, plus extend the existing copilot config/controller tests as noted.

Build server first (so the endpoint exists to talk to), then client.

---

## Task 1: Server — add `JSON_SCHEMA_BUILDER` to `Source` and `CopilotAgentType`

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java:22`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`

**Interfaces:**
- Produces: `Source.JSON_SCHEMA_BUILDER`; `CopilotAgentType.JSON_SCHEMA_BUILDER_ASK("json_schema_builder_ask", false)`, `JSON_SCHEMA_BUILDER_BUILD("json_schema_builder_build", false)`, `JSON_SCHEMA_BUILDER("json_schema_builder", true)`, `JSON_SCHEMA_BUILDER_AGENT("json_schema_builder_agent", false)`.

- [ ] **Step 1: Add the `Source` member.** Edit `Source.java` so the enum line reads:

```java
public enum Source {

    WORKFLOW_EDITOR, CODE_EDITOR, CONVERTER, CLUSTER_ELEMENT, SKILLS, WORKFLOW_EXECUTION, WORKFLOW_CODE_EDITOR,
    JSON_SCHEMA_BUILDER
}
```

- [ ] **Step 2: Add the `CopilotAgentType` members.** In `CopilotAgentType.java`, append after `WORKFLOW_EXECUTION_AGENT("workflow_execution_agent", false)` (change its trailing `;` to `,`):

```java
    WORKFLOW_EXECUTION_AGENT("workflow_execution_agent", false),
    JSON_SCHEMA_BUILDER_ASK("json_schema_builder_ask", false),
    JSON_SCHEMA_BUILDER_BUILD("json_schema_builder_build", false),
    JSON_SCHEMA_BUILDER("json_schema_builder", true),
    JSON_SCHEMA_BUILDER_AGENT("json_schema_builder_agent", false);
```

- [ ] **Step 3: Compile.**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-api:compileJava :server:libs:ai:ai-copilot:ai-copilot-tool:compileJava -q`
Expected: BUILD SUCCESSFUL (empty output).

- [ ] **Step 4: Commit.**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java
git commit -m "Add JSON_SCHEMA_BUILDER copilot source and agent types"
```

---

## Task 2: Server — `JsonSchemaTools.updateJsonSchema` `@Tool`

The agent calls this tool to hand the client the updated schema. Unlike `ScriptTools.updateScriptComponentCode` (which mutates a workflow), the JSON schema lives in the client, so this tool only validates the schema is well-formed JSON and echoes it back; the client applies it via its tool-result handler (Task 9).

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/JsonSchemaTools.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/JsonSchemaToolsTest.java`

**Interfaces:**
- Consumes: `com.bytechef.commons.util.JsonUtils`.
- Produces: `JsonSchemaTools#updateJsonSchema(String schema)` returning a JSON string `{"schema": <object>}` (the tool-result content the client parses); throws-safe (returns an error JSON on invalid input).

- [ ] **Step 1: Write the failing test.**

```java
package com.bytechef.ai.copilot.tool;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class JsonSchemaToolsTest {

    private final JsonSchemaTools jsonSchemaTools = new JsonSchemaTools();

    @Test
    void testUpdateJsonSchemaEchoesValidSchema() {
        String result = jsonSchemaTools.updateJsonSchema("{\"type\":\"object\"}");

        assertTrue(result.contains("\"schema\""));
        assertTrue((boolean) JsonUtils.readMap(result).containsKey("schema"));
    }

    @Test
    void testUpdateJsonSchemaRejectsInvalidJson() {
        String result = jsonSchemaTools.updateJsonSchema("{not json");

        assertFalse((boolean) JsonUtils.readMap(result).containsKey("schema"));
        assertTrue(result.contains("error"));
    }
}
```

- [ ] **Step 2: Run it — expect failure.**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests "*JsonSchemaToolsTest*"`
Expected: FAIL — `JsonSchemaTools` does not exist (compile error).

- [ ] **Step 3: Implement `JsonSchemaTools`.**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ... (standard Apache 2.0 header) ...
 */

package com.bytechef.ai.copilot.tool;

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Exposes the "apply JSON schema" tool to the JSON Schema Builder copilot agent. The generated schema is applied on
 * the client (the schema is not persisted server-side), so this tool only validates and echoes it back.
 *
 * @author ByteChef
 */
public class JsonSchemaTools {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTools.class);

    @Tool(
        description = "Apply the complete, updated JSON Schema to the builder. Pass the entire schema object as a " +
            "JSON string; the previous schema is fully replaced.")
    public String updateJsonSchema(@ToolParam(description = "The complete updated JSON Schema as a JSON string") String schema) {
        try {
            Object parsed = JsonUtils.read(schema);

            return JsonUtils.write(Map.of("schema", parsed));
        } catch (RuntimeException exception) {
            log.warn("updateJsonSchema rejected invalid schema JSON: {}", exception.getMessage());

            return JsonUtils.write(Map.of("error", "Invalid JSON schema: " + exception.getMessage()));
        }
    }
}
```

- [ ] **Step 4: Run the test — expect pass.**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests "*JsonSchemaToolsTest*"`
Expected: PASS (2 tests). If `ObjectMapperSetupExtension` import path differs, match the one used by a neighboring tool test in the same module (`grep -r ObjectMapperSetupExtension server/libs/ai/ai-copilot`).

- [ ] **Step 5: Format + commit.**

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:spotlessApply -q
git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/JsonSchemaTools.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/JsonSchemaToolsTest.java
git commit -m "Add JsonSchemaTools updateJsonSchema copilot tool"
```

---

## Task 3: Server — prompt resources + `JsonSchemaBuilderSpringAIAgent` + ASK/BUILD beans

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_json_schema_builder_ask.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_json_schema_builder_build.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/JsonSchemaBuilderSpringAIAgent.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`

**Interfaces:**
- Consumes: `CopilotAgentType.JSON_SCHEMA_BUILDER_BUILD` (Task 1), `JsonSchemaTools` (Task 2), `Source.JSON_SCHEMA_BUILDER` (Task 1).
- Produces: two `LocalAgent` beans whose `agentId()` = `json_schema_builder_ask` and `json_schema_builder_build` (consumed by the controller in Task 4 via `localAgentMap`).

**Reference:** mirror `CodeEditorSpringAIAgent.java` and the `codeEditorAskSpringAIAgent`/`codeEditorBuildSpringAIAgent` beans in `CopilotConfiguration.java:177-225`.

- [ ] **Step 1: Author the ASK prompt** `prompt_json_schema_builder_ask.txt`:

```text
You are the JSON Schema Builder assistant inside ByteChef's low-code workflow editor.
You help the user understand, review, and refine a JSON Schema that describes the desired
structure of a value (for example an LLM response schema or a data output schema).

The user's current schema and the property it belongs to are provided in the State block
under "parameters" (keys: currentJsonSchema, propertyPath) and as top-level state keys.

In ASK mode you explain, critique, and answer questions about the schema. You do NOT modify
it — describe the change you would make and let the user ask you to apply it. Be concise and
concrete; refer to specific fields by their JSON path.
```

- [ ] **Step 2: Author the BUILD prompt** `prompt_json_schema_builder_build.txt`:

```text
You are the JSON Schema Builder assistant inside ByteChef's low-code workflow editor.
You help the user construct and refine a JSON Schema that describes the desired structure of
a value (for example an LLM response schema or a data output schema).

The user's current schema and the property it belongs to are provided in the State block
under "parameters" (keys: currentJsonSchema, propertyPath) and as top-level state keys.

When the user asks for a change, produce the COMPLETE updated JSON Schema (not a diff) and
apply it by calling the updateJsonSchema tool with the entire schema as a JSON string. The
schema must be a valid JSON Schema object. After applying, briefly summarise what changed.
Always start from the current schema in state rather than from scratch, unless the user asks
to replace it entirely.
```

- [ ] **Step 3: Create `JsonSchemaBuilderSpringAIAgent`.** Mirror `CodeEditorSpringAIAgent` but read the schema params without throwing. Read `CodeEditorSpringAIAgent.java` first and copy its structure (constructor, `toolContext`, `createSystemMessage`, and the `Builder` override), changing only `createSystemMessage`:

```java
/*
 * Copyright 2025 ByteChef
 * ... (Apache 2.0 header) ...
 */

package com.bytechef.ai.copilot.agent;

// ... same imports as CodeEditorSpringAIAgent (SystemMessage, State, Context, etc.) ...

public class JsonSchemaBuilderSpringAIAgent extends CopilotSpringAIAgent {

    // Copy the constructor and toolContext() verbatim from CodeEditorSpringAIAgent.

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Object parametersObject = state.get("parameters");
        Map<?, ?> parameters = parametersObject instanceof Map<?, ?> map ? map : Map.of();

        Object currentSchema = parameters.get("currentJsonSchema");

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%nCurrent JSON schema:%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, currentSchema, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    // Copy the Builder nested class from CodeEditorSpringAIAgent, renaming the return type to
    // JsonSchemaBuilderSpringAIAgent.
}
```

- [ ] **Step 4: Wire prompt `@Value`s + beans in `CopilotConfiguration`.** Add two constructor `@Value` params and two fields (mirroring `promptCodeEditorAskResource`):

```java
    private final Resource promptJsonSchemaBuilderAskResource;
    private final Resource promptJsonSchemaBuilderBuildResource;
    // in the constructor signature:
    @Value("classpath:prompt_json_schema_builder_ask.txt") Resource promptJsonSchemaBuilderAskResource,
    @Value("classpath:prompt_json_schema_builder_build.txt") Resource promptJsonSchemaBuilderBuildResource,
    // in the constructor body:
    this.promptJsonSchemaBuilderAskResource = promptJsonSchemaBuilderAskResource;

    this.promptJsonSchemaBuilderBuildResource = promptJsonSchemaBuilderBuildResource;
```

Add two beans (mirror `codeEditorAskSpringAIAgent`/`codeEditorBuildSpringAIAgent`, `CopilotConfiguration.java:177-225`). BUILD includes `new JsonSchemaTools()` in its tool list so the agent can apply; ASK omits it:

```java
    @Bean
    JsonSchemaBuilderSpringAIAgent jsonSchemaBuilderAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.JSON_SCHEMA_BUILDER.name() + "_" + Mode.ASK.name();

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptJsonSchemaBuilderAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of()))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    JsonSchemaBuilderSpringAIAgent jsonSchemaBuilderBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        String name = Source.JSON_SCHEMA_BUILDER.name() + "_" + Mode.BUILD.name();

        List<Object> tools = new ArrayList<>(List.of(new JsonSchemaTools()));

        return JsonSchemaBuilderSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptJsonSchemaBuilderBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }
```

- [ ] **Step 5: Compile the module.**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:compileJava -q`
Expected: BUILD SUCCESSFUL. Fix any missing imports on `JsonSchemaBuilderSpringAIAgent` by comparing against `CodeEditorSpringAIAgent`'s import block.

- [ ] **Step 6: Format + commit.**

```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:spotlessApply -q
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_json_schema_builder_ask.txt \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_json_schema_builder_build.txt \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/JsonSchemaBuilderSpringAIAgent.java \
        server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
git commit -m "Add JSON Schema Builder copilot agent, prompts, and beans"
```

---

## Task 4: Server — controller routing for `json_schema_builder`

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java` (the `if/else` chain around lines 92-124)

**Interfaces:**
- Consumes: `localAgentMap` keys `json_schema_builder_ask` / `json_schema_builder_build` (Task 3).

- [ ] **Step 1: Add the routing branch.** In the `chat` method's `if/else` chain, after the `skills` branch, add:

```java
        } else if (agentId.equals("json_schema_builder")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "json_schema_builder_build";
            } else {
                agentId = "json_schema_builder_ask";
            }
        }
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-rest:compileJava -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify agents are registered (integration check).** Run the copilot module's existing tests to confirm the new beans wire without breaking context load:

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:test -q`
Expected: existing tests still PASS (new beans construct under `bytechef.ai.copilot.enabled=true`).

- [ ] **Step 4: Commit.**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-rest/src/main/java/com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java
git commit -m "Route json_schema_builder copilot chat to ask/build agents"
```

---

## Task 5: Client — add `Source.JSON_SCHEMA_BUILDER`

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts:14-21`

**Interfaces:**
- Produces: `Source.JSON_SCHEMA_BUILDER = 'JSON_SCHEMA_BUILDER'` → endpoint `/api/platform/internal/ai/chat/json_schema_builder`.

- [ ] **Step 1: Add the member.** Edit the `Source` enum:

```ts
export enum Source {
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
    WORKFLOW_CODE_EDITOR = 'WORKFLOW_CODE_EDITOR',
    JSON_SCHEMA_BUILDER = 'JSON_SCHEMA_BUILDER',
}
```

- [ ] **Step 2: Typecheck + commit.**

Run (in `client/`): `npm run typecheck`
Expected: no new errors.

```bash
git add client/src/shared/components/copilot/stores/useCopilotStore.ts
git commit -m "client - Add JSON_SCHEMA_BUILDER copilot source"
```

---

## Task 6: Client — copilot hook (open/close, contributor, apply-back)

Encapsulate all copilot wiring for the sheet in one hook, mirroring `usePropertyCodeEditorDialog` + `usePropertyCodeEditorDialogToolbar`.

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.ts`
- Test: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.test.ts`

**Interfaces:**
- Consumes: `useCopilotStore` (`MODE`, `Source`, `saveConversationState`, `resetMessages`, `generateConversationId`, `setContext`, `restoreConversationState`), `useCopilotStateContributorRegistry`, `useCopilotToolResultHandlerRegistry`, `useCopilotPostTurnRegistry`, `parseJson`.
- Produces hook returning:
  ```ts
  interface UsePropertyJsonSchemaBuilderCopilotResultI {
      copilotPanelOpen: boolean;
      handleCopilotClose: () => void;
      handleCopilotOpen: () => void;
  }
  interface UsePropertyJsonSchemaBuilderCopilotParamsI {
      onSchemaApply: (schema: SchemaRecordType) => void;
      propertyPath?: string;
      schemaRef: {current: SchemaRecordType | undefined};
      title?: string;
      workflowId?: string;
      workflowNodeName?: string;
  }
  ```
  `schemaRef` is a ref so the contributor always reads the live schema without re-registering.

- [ ] **Step 1: Write the failing test** (context set on open, restored on close; tool result applies schema):

```ts
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {usePropertyJsonSchemaBuilderCopilot} from './usePropertyJsonSchemaBuilderCopilot';

describe('usePropertyJsonSchemaBuilderCopilot', () => {
    beforeEach(() => {
        useCopilotStore.setState({context: undefined, messages: []});
    });

    it('sets json-schema context on open and restores on close', () => {
        const saveSpy = vi.spyOn(useCopilotStore.getState(), 'saveConversationState');
        const restoreSpy = vi.spyOn(useCopilotStore.getState(), 'restoreConversationState');

        const {result} = renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply: vi.fn(),
                propertyPath: 'output',
                schemaRef: {current: {type: 'object'}},
                title: 'Response Schema',
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() => result.current.handleCopilotOpen());

        expect(saveSpy).toHaveBeenCalled();
        expect(useCopilotStore.getState().context).toMatchObject({
            mode: MODE.ASK,
            parameters: {propertyPath: 'output'},
            source: Source.JSON_SCHEMA_BUILDER,
        });
        expect(result.current.copilotPanelOpen).toBe(true);

        act(() => result.current.handleCopilotClose());

        expect(restoreSpy).toHaveBeenCalled();
        expect(result.current.copilotPanelOpen).toBe(false);
    });

    it('applies the schema from an updateJsonSchema tool result', () => {
        const onSchemaApply = vi.fn();

        renderHook(() =>
            usePropertyJsonSchemaBuilderCopilot({
                onSchemaApply,
                propertyPath: 'output',
                schemaRef: {current: undefined},
                workflowId: 'w1',
                workflowNodeName: 'node1',
            })
        );

        act(() =>
            useCopilotToolResultHandlerRegistry
                .getState()
                .runFor('updateJsonSchema', JSON.stringify({schema: {type: 'object'}}))
        );

        expect(onSchemaApply).toHaveBeenCalledWith({type: 'object'});
    });
});
```

- [ ] **Step 2: Run it — expect failure.**

Run (in `client/`): `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.test.ts`
Expected: FAIL — hook file does not exist.

- [ ] **Step 3: Implement the hook.**

```ts
import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {useCallback, useEffect, useRef, useState} from 'react';

interface UsePropertyJsonSchemaBuilderCopilotParamsI {
    onSchemaApply: (schema: SchemaRecordType) => void;
    propertyPath?: string;
    schemaRef: {current: SchemaRecordType | undefined};
    title?: string;
    workflowId?: string;
    workflowNodeName?: string;
}

interface UsePropertyJsonSchemaBuilderCopilotResultI {
    copilotPanelOpen: boolean;
    handleCopilotClose: () => void;
    handleCopilotOpen: () => void;
}

const APPLIED_MESSAGE = '✓ Applied the schema to the builder.';

export function usePropertyJsonSchemaBuilderCopilot({
    onSchemaApply,
    propertyPath,
    schemaRef,
    title,
    workflowId,
    workflowNodeName,
}: UsePropertyJsonSchemaBuilderCopilotParamsI): UsePropertyJsonSchemaBuilderCopilotResultI {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);

    const pendingSchemaRef = useRef<SchemaRecordType | null>(null);

    const handleCopilotOpen = useCallback(() => {
        const {context, generateConversationId, resetMessages, saveConversationState, setContext} =
            useCopilotStore.getState();

        saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...context,
            mode: MODE.ASK,
            parameters: {propertyPath, title, workflowId, workflowNodeName},
            source: Source.JSON_SCHEMA_BUILDER,
        });

        setCopilotPanelOpen(true);
    }, [propertyPath, title, workflowId, workflowNodeName]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();

        setCopilotPanelOpen(false);
    }, []);

    useEffect(() => {
        const unregisterContributor = useCopilotStateContributorRegistry.getState().register(() => ({
            currentJsonSchema: schemaRef.current,
            propertyPath,
            workflowId,
            workflowNodeName,
        }));

        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateJsonSchema', (content) => {
                const result = parseJson<{schema?: SchemaRecordType}>(content, 'updateJsonSchema result');

                if (result?.schema) {
                    pendingSchemaRef.current = result.schema;
                }
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.JSON_SCHEMA_BUILDER, () => {
            const schema = pendingSchemaRef.current;

            pendingSchemaRef.current = null;

            if (schema == null) {
                return;
            }

            onSchemaApply(schema);

            useCopilotStore.getState().appendToLastAssistantMessage(APPLIED_MESSAGE);
        });

        return () => {
            unregisterContributor();
            unregisterToolResult();
            unregisterPostTurn();
        };
    }, [onSchemaApply, propertyPath, schemaRef, workflowId, workflowNodeName]);

    return {copilotPanelOpen, handleCopilotClose, handleCopilotOpen};
}
```

Note: the tool-result handler in the test fires `onSchemaApply` synchronously via the post-turn callback only in the real flow. For the second test to pass without a post-turn run, ALSO apply directly in the tool-result handler when the panel is used stand-alone. Adjust the handler to apply immediately (auto-apply) AND stash for the post-turn message:

```ts
            .register('updateJsonSchema', (content) => {
                const result = parseJson<{schema?: SchemaRecordType}>(content, 'updateJsonSchema result');

                if (result?.schema) {
                    pendingSchemaRef.current = result.schema;

                    onSchemaApply(result.schema);
                }
            });
```
and in the post-turn callback drop the `onSchemaApply(schema)` line (keep only the applied-message append). This makes apply immediate (auto-apply, per design) and keeps the post-turn callback for the confirmation message.

- [ ] **Step 4: Run the tests — expect pass.**

Run (in `client/`): `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/usePropertyJsonSchemaBuilderCopilot.test.ts`
Expected: PASS (2 tests). If `parseJson`'s import path differs, confirm with `grep -rn "toToolResultDataPart" client/src/pages/.../usePropertyCodeEditorDialog.ts`.

- [ ] **Step 5: Format + commit.**

```bash
cd client && npm run format && cd ..
git add client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/hooks/
git commit -m "client - Add JSON schema builder copilot hook"
```

---

## Task 7: Client — embed `CopilotPanel` + header toggle in the sheet

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx`
- Test: `.../PropertyJsonSchemaBuilderSheet.test.tsx`

**Interfaces:**
- Consumes: `usePropertyJsonSchemaBuilderCopilot` (Task 6), `CopilotPanel`, the availability gate (Global Constraints).

- [ ] **Step 1: Write the failing test** (toggle button shows when gated on; clicking opens the panel). Mock the copilot hook and stores:

```tsx
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

const {handleCopilotOpen} = vi.hoisted(() => ({handleCopilotOpen: vi.fn()}));

vi.mock('./hooks/usePropertyJsonSchemaBuilderCopilot', () => ({
    usePropertyJsonSchemaBuilderCopilot: () => ({
        copilotPanelOpen: false,
        handleCopilotClose: vi.fn(),
        handleCopilotOpen,
    }),
}));
vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => <div data-testid="copilot-panel" />}));
vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (s: unknown) => unknown) => selector({ai: {copilot: {enabled: true}}}),
}));
vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({useFeatureFlagsStore: () => () => true}));

import PropertyJsonSchemaBuilderSheet from './PropertyJsonSchemaBuilderSheet';

describe('PropertyJsonSchemaBuilderSheet copilot toggle', () => {
    it('opens the copilot when the toggle is clicked', async () => {
        const user = userEvent.setup();

        render(
            <PropertyJsonSchemaBuilderSheet
                environmentId={1}
                propertyPath="output"
                title="Response Schema"
                workflowId="w1"
                workflowNodeName="node1"
            />
        );

        await user.click(screen.getByRole('button', {name: /copilot/i}));

        expect(handleCopilotOpen).toHaveBeenCalled();
    });
});
```

- [ ] **Step 2: Run it — expect failure.**

Run (in `client/`): `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.test.tsx`
Expected: FAIL — no copilot toggle button.

- [ ] **Step 3: Implement the layout change.** In `PropertyJsonSchemaBuilderSheet.tsx`:
  1. Add imports:
     ```tsx
     import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
     import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
     import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
     import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
     import {Button} from '@/components/ui/button';
     import {SparklesIcon} from 'lucide-react';
     import {usePropertyJsonSchemaBuilderCopilot} from './hooks/usePropertyJsonSchemaBuilderCopilot';
     ```
  2. Inside the component, after `localSchema`:
     ```tsx
     const schemaRef = useRef(localSchema);

     schemaRef.current = localSchema;

     const ai = useApplicationInfoStore((state) => state.ai);
     const ff1570 = useFeatureFlagsStore()('ff-1570');

     const copilotAvailable = Boolean(
         ai.copilot.enabled && ff1570 && workflowId && workflowNodeName && environmentId !== undefined
     );

     const {copilotPanelOpen, handleCopilotClose, handleCopilotOpen} = usePropertyJsonSchemaBuilderCopilot({
         onSchemaApply: handleSchemaChange,
         propertyPath,
         schemaRef,
         title,
         workflowId,
         workflowNodeName,
     });
     ```
  3. Widen the sheet when the panel is open: change `SheetContent`'s `sm:max-w-(--breakpoint-lg)` to
     ```tsx
     className={twMerge(
         'top-3 right-4 bottom-4 flex h-auto w-11/12 flex-row gap-0 rounded-md bg-surface-neutral-secondary p-0',
         copilotPanelOpen ? 'sm:max-w-(--breakpoint-xl)' : 'sm:max-w-(--breakpoint-lg)'
     )}
     ```
     (add `import {twMerge} from 'tailwind-merge';`). Note `flex-col` → `flex-row`.
  4. Wrap the existing `<Tabs>` block in a left column and add the panel as a sibling:
     ```tsx
     <div className="flex min-w-0 flex-1 flex-col">
         <Tabs /* ...unchanged... */>
             {/* header + body unchanged, EXCEPT add the toggle button in the header's controls div */}
         </Tabs>
     </div>

     {copilotAvailable && (
         <CopilotPanel
             className="h-full rounded-r-md border-l border-l-border/50"
             onClose={handleCopilotClose}
             open={copilotPanelOpen}
             source={Source.JSON_SCHEMA_BUILDER}
         />
     )}
     ```
  5. In the header controls `<div className="flex items-center gap-1">`, before `<SheetCloseButton />`, add (only when available):
     ```tsx
     {copilotAvailable && (
         <Button aria-label="Copilot" onClick={handleCopilotOpen} size="icon" variant="ghost">
             <SparklesIcon className="size-4" />
         </Button>
     )}
     ```

- [ ] **Step 4: Run the test — expect pass.**

Run (in `client/`): `npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.test.tsx`
Expected: PASS. Confirm the `Button` import matches the sheet's existing button usage (`@/components/ui/button` vs `@/components/Button/Button` — check which the codebase's other sheets use; `JsonSchemaCopilotBar` uses `@/components/Button/Button`).

- [ ] **Step 5: Full client check + commit.**

Run (in `client/`): `npm run check`
Expected: lint + typecheck + tests pass (fix any `sort-keys`/import-order issues manually).

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx \
        client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.test.tsx
git commit -m "client - Embed copilot chat panel in JSON schema builder sheet"
```

---

## Task 8: End-to-end verification

**Files:** none (manual + automated verification).

- [ ] **Step 1: Server up with copilot enabled.** In `server/apps/server-app/src/main/resources/config/application-local.yml`, ensure `bytechef.ai.copilot.enabled: true` and an AI provider/gateway is configured (the user's selected `gateway.enabled` lines are relevant here). Start infra + server per CLAUDE.md.

- [ ] **Step 2: Drive it.** In the workflow editor, open a property that uses the JSON Schema Builder (e.g. an LLM Response Schema), click the **Copilot** (Sparkles) toggle. Confirm: the panel opens on the right, the sheet widens, and a fresh thread starts. Ask "add a `price` number field"; confirm the agent calls `updateJsonSchema` and the builder pills update live. Close the panel; confirm the main workflow-editor conversation is intact (restore worked).

- [ ] **Step 3: Full suites.**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test :server:libs:ai:ai-copilot:ai-copilot-service:test`
Run (in `client/`): `npm run check`
Expected: all pass.

- [ ] **Step 4: Final commit (if any verification fixups).**

```bash
git add -A
git commit -m "Verify JSON schema builder copilot end-to-end"
```

---

## Self-review notes

- **Spec coverage:** layout/toggle (Task 7) ✓; lifecycle save/restore (Task 6) ✓; live schema each turn via contributor (Task 6) ✓; new JSON_SCHEMA source client+server (Tasks 5,1) ✓; auto-apply full schema via tool result (Tasks 2,6) ✓; new schema-aware agent + prompt + tool (Tasks 2,3) ✓; controller routing (Task 4) ✓; keep existing "Generate with AI" bar (untouched — left in the sheet body) ✓; error handling for invalid schema (Task 2 tool + `parseJson` guard in Task 6) ✓; testing client+server ✓.
- **Ordinals:** enums appended at end (Tasks 1,5); no stability test pins them.
- **Type consistency:** tool result shape `{schema: ...}` is produced by `JsonSchemaTools.updateJsonSchema` (Task 2) and consumed by the hook's `parseJson<{schema?: SchemaRecordType}>` (Task 6); tool name `updateJsonSchema` matches in both. Source name `JSON_SCHEMA_BUILDER` → path `json_schema_builder` is consistent across client enum (Task 5), server enum (Task 1), agent ids (Task 3), and controller branch (Task 4).
- **Open risk to confirm during Task 3:** `CopilotSpringAIAgent`/`SpringAIAgent.Builder` field names (`systemMessage`, `systemMessageProvider`) — copy them verbatim from `CodeEditorSpringAIAgent` when implementing.
