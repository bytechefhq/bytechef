# Copilot: Workflow Description + JSON Schema Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two EE-only AI-Copilot affordances — (a) generate a JSON Schema for `JSON_SCHEMA_BUILDER` properties from a natural-language prompt (#2109), and (b) one-click generate a workflow or node description by analyzing the definition (#2503).

**Architecture:** #2109 extends the existing Property Copilot (`generatePropertyValue` mutation) with a third `JSON_SCHEMA` mode and an inline prompt bar inside the JSON-schema-builder sheet that populates the builder. #2503 adds a new `generateWorkflowDescription` mutation in the same `ai-copilot` modules plus a reusable one-click button wired into `WorkflowDialog` and the node `DescriptionTab`. Both reuse `ff-1570` + `ai.copilot.enabled` gating and the existing IDOR auth guard.

**Tech Stack:** Java 25 / Spring Boot, Spring AI `ChatModel`, Spring for GraphQL, Spring Data; React 19 + TypeScript, graphql-codegen + React Query, Zustand, Vitest. EE modules under `server/ee/libs/ai/ai-copilot/`.

**Spec:** `docs/superpowers/specs/2026-06-02-copilot-workflow-description-and-json-schema-design.md`

**Conventions reminder:** EE license header + `@version ee` Javadoc on every new file under `server/ee` (including tests). Client: `sort-keys` alphabetical, interfaces end `…PropsI`/`…Props`, sorted destructured imports, Lucide `…Icon` imports, `twMerge` (not `cn`), hook ordering (`useState`→`useRef`→stores→derived→`useEffect`→return), no `_`-prefixed methods, descriptive variable names. Server: one blank line before control statements, no gratuitous method chaining.

---

## File Structure

**#2109 backend (modify):**
- `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotMode.java` — add `JSON_SCHEMA`.
- `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/property-copilot.graphqls` — add `JSON_SCHEMA` enum value.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilder.java` — `JSON_SCHEMA` branch.
- `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImpl.java` — `JSON_SCHEMA` path + JSON validate/repair.

**#2109 frontend (modify):**
- `client/src/shared/middleware/graphql-types.ts` + `graphql.ts` — regenerated (adds `PropertyCopilotMode.JsonSchema`).
- `client/src/pages/platform/workflow-editor/components/properties/Property.tsx` — pass copilot context props to `PropertyJsonSchemaBuilder`.
- `.../property-json-schema-builder/PropertyJsonSchemaBuilder.tsx` — forward props.
- `.../property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx` — render the bar.
- **Create** `.../property-json-schema-builder/JsonSchemaCopilotBar.tsx` (+ `.test.tsx`).

**#2503 backend (create):**
- `ai-copilot-api/.../copilot/workflow/WorkflowDescriptionCopilotRequest.java`, `WorkflowDescriptionCopilotResult.java`, `WorkflowDescriptionCopilotGenerator.java`.
- `ai-copilot-service/.../copilot/workflow/WorkflowDescriptionCopilotGeneratorImpl.java`, `WorkflowDescriptionPromptBuilder.java`.
- `ai-copilot-graphql/src/main/resources/graphql/workflow-description-copilot.graphqls`.
- `ai-copilot-graphql/.../web/graphql/WorkflowDescriptionCopilotGraphQlController.java`.

**#2503 frontend (create/modify):**
- **Create** `client/src/graphql/platform/copilot/generateWorkflowDescription.graphql`.
- **Create** `client/src/shared/components/copilot/useGenerateWorkflowDescription.ts`.
- **Create** `client/src/shared/components/copilot/CopilotGenerateDescriptionButton.tsx` (+ `.test.tsx`).
- Modify `client/src/shared/components/workflow/WorkflowDialog.tsx`.
- Modify `client/src/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab.tsx`.

---

## Part A — #2109 backend: Property Copilot `JSON_SCHEMA` mode

### Task 1: Add `JSON_SCHEMA` to `PropertyCopilotMode` (Java + GraphQL schema)

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotMode.java`
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/property-copilot.graphqls`

- [ ] **Step 1: Add the enum constant**

In `PropertyCopilotMode.java`, change the enum body to (append last):

```java
public enum PropertyCopilotMode {

    TEXT, FORMULA, JSON_SCHEMA
}
```

- [ ] **Step 2: Add the GraphQL enum value**

In `property-copilot.graphqls`, change the `PropertyCopilotMode` enum to:

```graphql
enum PropertyCopilotMode {
    TEXT
    FORMULA
    JSON_SCHEMA
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotMode.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/property-copilot.graphqls
git commit -m "2109 Add JSON_SCHEMA value to PropertyCopilotMode"
```

---

### Task 2: `PropertyCopilotPromptBuilder` — `JSON_SCHEMA` branch

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilder.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilderTest.java`

- [ ] **Step 1: Write the failing test**

Append this test method to `PropertyCopilotPromptBuilderTest`:

```java
    @Test
    void testBuildJsonSchemaModeAsksForSchemaOnlyAndOmitsOutputs() {
        PropertyCopilotRequest request = new PropertyCopilotRequest(
            "an order with an id and a list of line items", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2",
            "responseSchema", "STRING", 0);

        String prompt = promptBuilder.build(request, "trigger_1: {\"city\":\"paris\"}\n", "");

        assertThat(prompt).contains("an order with an id and a list of line items");
        assertThat(prompt).contains("JSON Schema");
        assertThat(prompt).doesNotContain("trigger_1");
        assertThat(prompt).doesNotContain("${");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*PropertyCopilotPromptBuilderTest"`
Expected: FAIL — the JSON_SCHEMA branch does not yet exist, so the prompt contains the outputs / `${` instruction.

- [ ] **Step 3: Implement the branch**

Replace the body of `PropertyCopilotPromptBuilder.build(...)` so the JSON_SCHEMA mode is handled before the outputs are appended:

```java
    public String build(PropertyCopilotRequest request, String availableOutputs, String functionCatalog) {
        StringBuilder builder = new StringBuilder();

        if (request.mode() == PropertyCopilotMode.JSON_SCHEMA) {
            builder.append(
                "You generate a JSON Schema (draft 2020-12) describing the structure the user wants.\n\n");
            builder.append("User request: ")
                .append(request.prompt())
                .append("\n\n");
            builder.append(
                "Return ONLY a single JSON Schema object. It must be valid JSON with a top-level \"type\" " +
                    "(usually \"object\") and a \"properties\" map. No explanation, no markdown, no code fences.");

            return builder.toString();
        }

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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*PropertyCopilotPromptBuilderTest"`
Expected: PASS (all three tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilder.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotPromptBuilderTest.java
git commit -m "2109 Add JSON_SCHEMA prompt branch to PropertyCopilotPromptBuilder"
```

---

### Task 3: `PropertyCopilotGeneratorImpl` — `JSON_SCHEMA` path with parse-validate + one repair

**Files:**
- Modify: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImpl.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImplTest.java`

- [ ] **Step 1: Write the failing tests**

Append to `PropertyCopilotGeneratorImplTest`:

```java
    @Test
    void testJsonSchemaModeStripsFencesAndValidates() {
        PropertyCopilotGeneratorImpl generator = generatorReturning(
            "```json\n{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}\n```");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", 0));

        assertThat(result.value()).isEqualTo("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}");
        assertThat(result.valid()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testJsonSchemaModeInvalidThenRepaired() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse bad = buildChatResponse("not json at all");
        ChatResponse good = buildChatResponse("{\"type\":\"object\",\"properties\":{}}");

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(bad, good);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider);

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", 0));

        assertThat(result.value()).isEqualTo("{\"type\":\"object\",\"properties\":{}}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testJsonSchemaModeStillInvalidAfterRepairReturnsInvalid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("definitely not json");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", 0));

        assertThat(result.value()).isEqualTo("definitely not json");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*PropertyCopilotGeneratorImplTest"`
Expected: FAIL — JSON_SCHEMA currently falls into the non-FORMULA branch and returns `valid=true` verbatim, so the invalid-JSON tests fail (and the repair test sees only one model call).

- [ ] **Step 3: Implement the JSON_SCHEMA path**

In `PropertyCopilotGeneratorImpl`, add the import:

```java
import com.bytechef.commons.util.JsonUtils;
```

Then change `generate(...)` so the JSON_SCHEMA mode is handled before the existing TEXT/FORMULA logic. Replace the early part of the method:

```java
    @Override
    public PropertyCopilotResult generate(PropertyCopilotRequest request) {
        String availableOutputs = buildAvailableOutputs(request);
        String functionCatalog =
            request.mode() == PropertyCopilotMode.FORMULA ? buildFunctionCatalog() : "";

        String prompt = promptBuilder.build(request, availableOutputs, functionCatalog);

        if (request.mode() == PropertyCopilotMode.JSON_SCHEMA) {
            return generateJsonSchema(request, prompt);
        }

        String value = clean(call(prompt));

        if (request.mode() != PropertyCopilotMode.FORMULA) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        // ... existing FORMULA logic unchanged ...
```

Add these two private methods (place them after `generate`):

```java
    private PropertyCopilotResult generateJsonSchema(PropertyCopilotRequest request, String prompt) {
        String value = clean(call(prompt));

        if (isValidJsonObject(value)) {
            record(request, "success");

            return new PropertyCopilotResult(value, true, null);
        }

        String repaired = clean(call(prompt +
            "\n\nThe previous attempt was not valid JSON. Return ONLY a valid JSON Schema object."));

        if (isValidJsonObject(repaired)) {
            record(request, "success");

            return new PropertyCopilotResult(repaired, true, null);
        }

        record(request, "invalid_json");

        return new PropertyCopilotResult(
            repaired, false, "The generated JSON schema could not be parsed; please review it.");
    }

    private static boolean isValidJsonObject(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            JsonUtils.read(value, java.util.Map.class);

            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
```

(Note: `Map` is already imported at the top of the file via `java.util.Map`; if the existing import is present, use `Map.class` instead of the fully-qualified form.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*PropertyCopilotGeneratorImplTest"`
Expected: PASS (all tests, including the original TEXT/FORMULA ones).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImpl.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/property/PropertyCopilotGeneratorImplTest.java
git commit -m "2109 Add JSON_SCHEMA generation path with parse-validate and repair"
```

---

### Task 4: GraphQL controller test for `JSON_SCHEMA` mode

**Files:**
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/PropertyCopilotGraphQlControllerTest.java`

The controller already forwards `mode`; this test locks in that `JSON_SCHEMA` flows through unchanged.

- [ ] **Step 1: Write the test**

Append to `PropertyCopilotGraphQlControllerTest`:

```java
    @Test
    void testGenerateJsonSchemaModeForwardsToGenerator() {
        givenWorkflowProject(42L);

        when(permissionService.hasProjectScope(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(propertyCopilotGenerator.generate(any()))
            .thenReturn(new PropertyCopilotResult("{\"type\":\"object\"}", true, null));

        GeneratePropertyValuePayload payload = controller.generatePropertyValue(new GeneratePropertyValueInput(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "n1", "responseSchema", "STRING", 0));

        assertThat(payload.value()).isEqualTo("{\"type\":\"object\"}");
        assertThat(payload.valid()).isTrue();
    }
```

- [ ] **Step 2: Run + verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:test --tests "*PropertyCopilotGraphQlControllerTest"`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/PropertyCopilotGraphQlControllerTest.java
git commit -m "2109 Test JSON_SCHEMA mode flows through PropertyCopilot GraphQL controller"
```

---

## Part B — #2109 frontend: inline schema-generation bar

### Task 5: Regenerate GraphQL types (adds `PropertyCopilotMode.JsonSchema`)

**Files:**
- Modify (generated): `client/src/shared/middleware/graphql-types.ts`, `client/src/shared/middleware/graphql.ts`

- [ ] **Step 1: Run codegen**

Run: `cd client && npx graphql-codegen`
Expected: regenerates without error.

- [ ] **Step 2: Verify the enum value exists**

Run: `cd client && grep -n "JsonSchema" src/shared/middleware/graphql-types.ts`
Expected: shows `JsonSchema = 'JSON_SCHEMA'` inside the `PropertyCopilotMode` enum.

- [ ] **Step 3: Commit**

```bash
git add client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts
git commit -m "2109 client - Regenerate GraphQL types for JSON_SCHEMA copilot mode"
```

---

### Task 6: Thread copilot context into `PropertyJsonSchemaBuilder` → sheet

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/properties/Property.tsx` (~line 851)
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder.tsx`
- Modify: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx`

These are plumbing-only edits; the bar itself arrives in Task 7. Type-check is the gate.

- [ ] **Step 1: Add optional copilot props to `PropertyJsonSchemaBuilderSheet`**

In `PropertyJsonSchemaBuilderSheet.tsx`, extend the props interface (keep keys alphabetical) and pass them to the bar (added Task 7). For now just widen the interface:

```typescript
interface PropertyJsonSchemaBuilderSheetProps {
    environmentId?: number;
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    propertyPath?: string;
    schema?: SchemaRecordType;
    title?: string;
    workflowId?: string;
    workflowNodeName?: string;
}
```

Destructure the new props in the component signature (alphabetical):

```typescript
const PropertyJsonSchemaBuilderSheet = ({
    environmentId,
    onChange,
    onClose,
    propertyPath,
    schema,
    title,
    workflowId,
    workflowNodeName,
}: PropertyJsonSchemaBuilderSheetProps) => {
```

- [ ] **Step 2: Add the same optional props to `PropertyJsonSchemaBuilder` and forward them**

In `PropertyJsonSchemaBuilder.tsx`, add to `PropertyJsonSchemaBuilderProps` (alphabetical):

```typescript
    environmentId?: number;
```
```typescript
    propertyPath?: string;
    workflowId?: string;
    workflowNodeName?: string;
```

Destructure them in the `forwardRef` callback params, and pass them through to the sheet:

```tsx
                    <PropertyJsonSchemaBuilderSheet
                        environmentId={environmentId}
                        onChange={onChange}
                        onClose={() => setShowPropertyJsonSchemaBuilder(false)}
                        propertyPath={propertyPath}
                        schema={schema}
                        title={title}
                        workflowId={workflowId}
                        workflowNodeName={workflowNodeName}
                    />
```

- [ ] **Step 3: Pass context from `Property.tsx`**

In `Property.tsx`, locate the `<PropertyJsonSchemaBuilder ... />` block (~line 852) and add these props (the surrounding component already has `workflow`, `currentNode`, `currentEnvironmentId`, `calculatedPath`, `name` in scope — verified against the `PropertyCopilotButton` usage at ~line 247):

```tsx
                            environmentId={currentEnvironmentId}
                            propertyPath={calculatedPath ?? name}
                            workflowId={workflow.id as string}
                            workflowNodeName={currentNode?.name}
```

(Insert in alphabetical position among the existing props.)

- [ ] **Step 4: Type-check**

Run: `cd client && npm run typecheck`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/Property.tsx \
        client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilder.tsx \
        client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx
git commit -m "2109 client - Thread copilot context into JSON schema builder sheet"
```

---

### Task 7: `JsonSchemaCopilotBar` + integrate into the sheet (with replace-confirm)

**Files:**
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/JsonSchemaCopilotBar.tsx`
- Create: `client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/JsonSchemaCopilotBar.test.tsx`
- Modify: `.../property-json-schema-builder/PropertyJsonSchemaBuilderSheet.tsx`

- [ ] **Step 1: Write the failing test**

Create `JsonSchemaCopilotBar.test.tsx`:

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import JsonSchemaCopilotBar from './JsonSchemaCopilotBar';

const {generateMock} = vi.hoisted(() => ({generateMock: vi.fn()}));

vi.mock('../property-copilot/useGeneratePropertyValue', () => ({
    useGeneratePropertyValue: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

describe('JsonSchemaCopilotBar', () => {
    beforeEach(() => {
        generateMock.mockReset();
    });

    it('generates a schema and applies the parsed object', async () => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({message: null, valid: true, value: '{"type":"object"}'});

        render(
            <JsonSchemaCopilotBar
                currentSchemaIsEmpty={true}
                environmentId={1}
                onApply={onApply}
                propertyPath="responseSchema"
                workflowId="wf1"
                workflowNodeName="node1"
            />
        );

        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'an order'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await vi.waitFor(() => expect(onApply).toHaveBeenCalledWith({type: 'object'}));
    });

    it('renders nothing when copilot context is incomplete', () => {
        const {container} = render(
            <JsonSchemaCopilotBar
                currentSchemaIsEmpty={true}
                environmentId={1}
                onApply={vi.fn()}
                propertyPath="responseSchema"
                workflowId={undefined}
                workflowNodeName="node1"
            />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/JsonSchemaCopilotBar.test.tsx`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement `JsonSchemaCopilotBar.tsx`**

```tsx
import Button from '@/components/Button/Button';
import {Textarea} from '@/components/ui/textarea';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {PropertyCopilotMode} from '@/shared/middleware/graphql-types';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Loader2Icon, SparklesIcon} from 'lucide-react';
import {useState} from 'react';

import {useGeneratePropertyValue} from '../property-copilot/useGeneratePropertyValue';

interface JsonSchemaCopilotBarPropsI {
    currentSchemaIsEmpty: boolean;
    environmentId: number;
    onApply: (schema: SchemaRecordType) => void;
    propertyPath: string;
    workflowId?: string;
    workflowNodeName?: string;
}

const JsonSchemaCopilotBar = ({
    currentSchemaIsEmpty,
    environmentId,
    onApply,
    propertyPath,
    workflowId,
    workflowNodeName,
}: JsonSchemaCopilotBarPropsI) => {
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const [error, setError] = useState<string | null>(null);
    const [prompt, setPrompt] = useState('');

    const {generate, isPending} = useGeneratePropertyValue();

    if (!ai.copilot.enabled || !ff1570 || !workflowId || !workflowNodeName) {
        return null;
    }

    const handleGenerate = async () => {
        setError(null);

        try {
            const result = await generate({
                environmentId,
                mode: PropertyCopilotMode.JsonSchema,
                prompt,
                propertyPath,
                propertyType: 'STRING',
                workflowId,
                workflowNodeName,
            });

            let parsed: SchemaRecordType;

            try {
                parsed = JSON.parse(result.value);
            } catch {
                setError(result.message ?? 'The generated schema was not valid JSON.');

                return;
            }

            if (!currentSchemaIsEmpty && !window.confirm('Replace the current schema?')) {
                return;
            }

            onApply(parsed);
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
                placeholder="Describe the structure you want…"
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

export default JsonSchemaCopilotBar;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd client && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/JsonSchemaCopilotBar.test.tsx`
Expected: PASS.

- [ ] **Step 5: Render the bar in the sheet**

In `PropertyJsonSchemaBuilderSheet.tsx`, import the bar:

```typescript
import JsonSchemaCopilotBar from './JsonSchemaCopilotBar';
```

Inside the scrollable body `<div className="flex-1 space-y-4 overflow-y-auto p-3">`, above the `Note`/`TabsContent`, render the bar when context is present:

```tsx
                        {environmentId !== undefined && propertyPath && (
                            <JsonSchemaCopilotBar
                                currentSchemaIsEmpty={
                                    !localSchema?.properties ||
                                    Object.keys(localSchema.properties).length === 0
                                }
                                environmentId={environmentId}
                                onApply={handleSchemaChange}
                                propertyPath={propertyPath}
                                workflowId={workflowId}
                                workflowNodeName={workflowNodeName}
                            />
                        )}
```

(`localSchema` and `handleSchemaChange` already exist in the sheet. The `SchemaRecordType` is a record, so `.properties` access is via index; if TypeScript complains, use `(localSchema as {properties?: Record<string, unknown>})?.properties`.)

- [ ] **Step 6: Type-check + run sheet/bar tests**

Run: `cd client && npm run typecheck && npx vitest run src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/`
Expected: no type errors; tests PASS.

- [ ] **Step 7: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/properties/components/property-json-schema-builder/
git commit -m "2109 client - Add inline AI schema generation bar to JSON schema builder"
```

---

## Part C — #2503 backend: workflow / node description generation

### Task 8: `ai-copilot-api` — request/result records + generator interface

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionCopilotRequest.java`
- Create: `.../copilot/workflow/WorkflowDescriptionCopilotResult.java`
- Create: `.../copilot/workflow/WorkflowDescriptionCopilotGenerator.java`

- [ ] **Step 1: Create the request record**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record WorkflowDescriptionCopilotRequest(String workflowId, String workflowNodeName, long environmentId) {
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

package com.bytechef.ee.ai.copilot.workflow;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record WorkflowDescriptionCopilotResult(String value) {
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

package com.bytechef.ee.ai.copilot.workflow;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowDescriptionCopilotGenerator {

    WorkflowDescriptionCopilotResult generate(WorkflowDescriptionCopilotRequest request);
}
```

- [ ] **Step 4: Compile + commit**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:compileJava`
Expected: BUILD SUCCESSFUL.

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ee/ai/copilot/workflow/
git commit -m "2503 Add WorkflowDescription copilot API types"
```

---

### Task 9: `WorkflowDescriptionPromptBuilder`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionPromptBuilder.java`
- Test: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionPromptBuilderTest.java`

The builder takes the workflow definition JSON and an optional node name. To stay robust against definition-shape changes, it passes the raw definition to the model rather than parsing it.

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowDescriptionPromptBuilderTest {

    private final WorkflowDescriptionPromptBuilder promptBuilder = new WorkflowDescriptionPromptBuilder();

    @Test
    void testBuildWholeWorkflowPrompt() {
        String prompt = promptBuilder.build("{\"label\":\"Sync\",\"tasks\":[]}", null);

        assertThat(prompt).contains("Sync");
        assertThat(prompt).contains("workflow");
        assertThat(prompt).doesNotContain("single step");
    }

    @Test
    void testBuildNodePrompt() {
        String prompt = promptBuilder.build("{\"tasks\":[{\"name\":\"node1\"}]}", "node1");

        assertThat(prompt).contains("node1");
        assertThat(prompt).contains("single step");
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowDescriptionPromptBuilderTest"`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import org.springframework.stereotype.Component;

/**
 * Builds prompts for the Workflow Description Copilot feature.
 *
 * @version ee
 * @author Ivica Cardic
 */
@Component
public class WorkflowDescriptionPromptBuilder {

    public String build(String workflowDefinition, String workflowNodeName) {
        StringBuilder builder = new StringBuilder();

        if (workflowNodeName == null) {
            builder.append(
                "You write a concise, human-readable description of an automation workflow based on its " +
                    "JSON definition (its triggers and tasks). Describe what the workflow does in 1-3 " +
                    "sentences. Return ONLY the description text, no preamble, no markdown, no code fences.\n\n");
            builder.append("Workflow definition:\n")
                .append(workflowDefinition);

            return builder.toString();
        }

        builder.append(
            "You write a short note describing a single step (node) of an automation workflow, based on the " +
                "workflow's JSON definition. Describe what the step named '")
            .append(workflowNodeName)
            .append("' does in 1-2 sentences. Return ONLY the note text, no preamble, no markdown, no code ")
            .append("fences.\n\n");
        builder.append("Workflow definition:\n")
            .append(workflowDefinition);

        return builder.toString();
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowDescriptionPromptBuilderTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionPromptBuilder.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionPromptBuilderTest.java
git commit -m "2503 Add WorkflowDescriptionPromptBuilder"
```

---

### Task 10: `WorkflowDescriptionCopilotGeneratorImpl`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionCopilotGeneratorImpl.java`
- Test: `.../src/test/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionCopilotGeneratorImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
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
class WorkflowDescriptionCopilotGeneratorImplTest {

    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static ChatResponse buildChatResponse(String text) {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();
    }

    @SuppressWarnings("unchecked")
    private WorkflowDescriptionCopilotGeneratorImpl generatorReturning(String definition, String llmText) {
        ChatModel chatModel = mock(ChatModel.class);

        when(chatModel.call(any(Prompt.class))).thenReturn(buildChatResponse(llmText));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn(definition);
        when(workflowService.getWorkflow(any())).thenReturn(workflow);

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new WorkflowDescriptionCopilotGeneratorImpl(
            chatModel, workflowService, new WorkflowDescriptionPromptBuilder(), meterRegistryProvider);
    }

    @Test
    void testGenerateWholeWorkflowDescriptionStripsFences() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning(
            "{\"label\":\"Sync\"}", "```\nSyncs records nightly.\n```");

        WorkflowDescriptionCopilotResult result = generator.generate(
            new WorkflowDescriptionCopilotRequest("wf1", null, 0));

        assertThat(result.value()).isEqualTo("Syncs records nightly.");
    }

    @Test
    void testGenerateNodeDescription() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning(
            "{\"tasks\":[{\"name\":\"node1\"}]}", "Sends a Slack message.");

        WorkflowDescriptionCopilotResult result = generator.generate(
            new WorkflowDescriptionCopilotRequest("wf1", "node1", 0));

        assertThat(result.value()).isEqualTo("Sends a Slack message.");
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowDescriptionCopilotGeneratorImplTest"`
Expected: FAIL — class missing.

- [ ] **Step 3: Implement**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
public class WorkflowDescriptionCopilotGeneratorImpl implements WorkflowDescriptionCopilotGenerator {

    private final ChatModel chatModel;
    private final WorkflowService workflowService;
    private final WorkflowDescriptionPromptBuilder promptBuilder;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public WorkflowDescriptionCopilotGeneratorImpl(
        ChatModel chatModel, WorkflowService workflowService, WorkflowDescriptionPromptBuilder promptBuilder,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.chatModel = chatModel;
        this.workflowService = workflowService;
        this.promptBuilder = promptBuilder;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    public WorkflowDescriptionCopilotResult generate(WorkflowDescriptionCopilotRequest request) {
        Workflow workflow = workflowService.getWorkflow(request.workflowId());

        String prompt = promptBuilder.build(workflow.getDefinition(), request.workflowNodeName());

        String value = clean(call(prompt));

        record(request.workflowNodeName() == null ? "workflow" : "node");

        return new WorkflowDescriptionCopilotResult(value);
    }

    private String call(String promptText) {
        return chatModel.call(new Prompt(promptText))
            .getResult()
            .getOutput()
            .getText();
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("```", "")
            .strip();
    }

    private void record(String scope) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_workflow_description_copilot_generate")
            .tag("scope", scope)
            .tag("outcome", "success")
            .register(meterRegistry)
            .increment();
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-service:test --tests "*WorkflowDescriptionCopilotGeneratorImplTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionCopilotGeneratorImpl.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ee/ai/copilot/workflow/WorkflowDescriptionCopilotGeneratorImplTest.java
git commit -m "2503 Add WorkflowDescriptionCopilotGeneratorImpl"
```

---

### Task 11: GraphQL schema + `WorkflowDescriptionCopilotGraphQlController`

**Files:**
- Create: `server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/workflow-description-copilot.graphqls`
- Create: `.../web/graphql/WorkflowDescriptionCopilotGraphQlController.java`
- Test: `.../src/test/java/com/bytechef/ee/ai/copilot/web/graphql/WorkflowDescriptionCopilotGraphQlControllerTest.java`
- Modify: `ai-copilot-graphql/build.gradle.kts` (add `ai-copilot-service`? NO — controller depends only on `ai-copilot-api`. No build change needed.)

- [ ] **Step 1: Create the GraphQL schema**

```graphql
extend type Mutation {
    generateWorkflowDescription(input: GenerateWorkflowDescriptionInput!): GenerateWorkflowDescriptionPayload!
}

input GenerateWorkflowDescriptionInput {
    workflowId: ID!
    workflowNodeName: String
    environmentId: Int!
}

type GenerateWorkflowDescriptionPayload {
    value: String!
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
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionInput;
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionPayload;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowDescriptionCopilotGraphQlControllerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowDescriptionCopilotGenerator generator = mock(WorkflowDescriptionCopilotGenerator.class);

    private final WorkflowDescriptionCopilotGraphQlController controller =
        new WorkflowDescriptionCopilotGraphQlController(
            permissionService, projectWorkflowService, Optional.of(generator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasProjectScope(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> controller.generateWorkflowDescription(input()))
            .isInstanceOf(AccessDeniedException.class);

        verify(generator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasProjectScope(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(generator.generate(any())).thenReturn(new WorkflowDescriptionCopilotResult("Syncs records."));

        GenerateWorkflowDescriptionPayload payload = controller.generateWorkflowDescription(input());

        assertThat(payload.value()).isEqualTo("Syncs records.");
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static GenerateWorkflowDescriptionInput input() {
        return new GenerateWorkflowDescriptionInput("wf1", null, 0);
    }
}
```

- [ ] **Step 3: Run to verify fail**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:test --tests "*WorkflowDescriptionCopilotGraphQlControllerTest"`
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
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Workflow Description Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGraphQlController {

    private static final String WORKFLOW_VIEW_SCOPE = "WORKFLOW_VIEW";

    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowDescriptionCopilotGenerator workflowDescriptionCopilotGenerator;

    public WorkflowDescriptionCopilotGraphQlController(
        PermissionService permissionService, ProjectWorkflowService projectWorkflowService,
        Optional<WorkflowDescriptionCopilotGenerator> workflowDescriptionCopilotGenerator) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowDescriptionCopilotGenerator = workflowDescriptionCopilotGenerator.orElse(null);
    }

    @MutationMapping
    public GenerateWorkflowDescriptionPayload generateWorkflowDescription(
        @Argument GenerateWorkflowDescriptionInput input) {

        if (workflowDescriptionCopilotGenerator == null) {
            throw new IllegalStateException("Workflow Description Copilot is not enabled");
        }

        // Authorize: the workflowId is client-supplied, so verify the current user may view the owning
        // project before reading its definition (IDOR / cross-tenant guard).
        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(input.workflowId())
            .getProjectId();

        if (!permissionService.hasProjectScope(projectId, WORKFLOW_VIEW_SCOPE)) {
            throw new AccessDeniedException("Access denied to workflow " + input.workflowId());
        }

        WorkflowDescriptionCopilotResult result = workflowDescriptionCopilotGenerator.generate(
            new WorkflowDescriptionCopilotRequest(
                input.workflowId(), input.workflowNodeName(), input.environmentId()));

        return new GenerateWorkflowDescriptionPayload(result.value());
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionInput(String workflowId, String workflowNodeName, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionPayload(String value) {
    }
}
```

- [ ] **Step 5: Run to verify pass**

Run: `./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:test --tests "*WorkflowDescriptionCopilotGraphQlControllerTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/resources/graphql/workflow-description-copilot.graphqls \
        server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/main/java/com/bytechef/ee/ai/copilot/web/graphql/WorkflowDescriptionCopilotGraphQlController.java \
        server/ee/libs/ai/ai-copilot/ai-copilot-graphql/src/test/java/com/bytechef/ee/ai/copilot/web/graphql/WorkflowDescriptionCopilotGraphQlControllerTest.java
git commit -m "2503 Add generateWorkflowDescription GraphQL mutation"
```

---

## Part D — #2503 frontend: one-click description button

### Task 12: GraphQL op + hook

**Files:**
- Create: `client/src/graphql/platform/copilot/generateWorkflowDescription.graphql`
- Modify (generated): `client/src/shared/middleware/graphql-types.ts`, `graphql.ts`
- Create: `client/src/shared/components/copilot/useGenerateWorkflowDescription.ts`

- [ ] **Step 1: Create the GraphQL operation**

```graphql
mutation generateWorkflowDescription($input: GenerateWorkflowDescriptionInput!) {
    generateWorkflowDescription(input: $input) {
        value
    }
}
```

- [ ] **Step 2: Regenerate types**

Run: `cd client && npx graphql-codegen`
Expected: generates `useGenerateWorkflowDescriptionMutation` + `GenerateWorkflowDescriptionInput`.

Verify: `cd client && grep -n "useGenerateWorkflowDescriptionMutation" src/shared/middleware/graphql.ts`
Expected: present.

- [ ] **Step 3: Create the hook**

```typescript
import {GenerateWorkflowDescriptionInput, useGenerateWorkflowDescriptionMutation} from '@/shared/middleware/graphql';

export function useGenerateWorkflowDescription() {
    const {isPending, mutateAsync} = useGenerateWorkflowDescriptionMutation();

    const generate = async (input: GenerateWorkflowDescriptionInput) => {
        const data = await mutateAsync({input});

        return data.generateWorkflowDescription;
    };

    return {generate, isPending};
}
```

- [ ] **Step 4: Type-check + commit**

Run: `cd client && npm run typecheck`
Expected: no errors.

```bash
git add client/src/graphql/platform/copilot/generateWorkflowDescription.graphql \
        client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts \
        client/src/shared/components/copilot/useGenerateWorkflowDescription.ts
git commit -m "2503 client - Add generateWorkflowDescription op and hook"
```

---

### Task 13: `CopilotGenerateDescriptionButton`

**Files:**
- Create: `client/src/shared/components/copilot/CopilotGenerateDescriptionButton.tsx`
- Create: `client/src/shared/components/copilot/CopilotGenerateDescriptionButton.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CopilotGenerateDescriptionButton from './CopilotGenerateDescriptionButton';

const {generateMock} = vi.hoisted(() => ({generateMock: vi.fn()}));

vi.mock('./useGenerateWorkflowDescription', () => ({
    useGenerateWorkflowDescription: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true,
}));

describe('CopilotGenerateDescriptionButton', () => {
    beforeEach(() => {
        generateMock.mockReset();
    });

    it('generates and applies the value', async () => {
        const onApply = vi.fn();

        generateMock.mockResolvedValue({value: 'Syncs records nightly.'});

        render(
            <CopilotGenerateDescriptionButton
                environmentId={1}
                onApply={onApply}
                workflowId="wf1"
            />
        );

        fireEvent.click(screen.getByRole('button', {name: /generate with ai/i}));

        await vi.waitFor(() => expect(onApply).toHaveBeenCalledWith('Syncs records nightly.'));
        expect(generateMock).toHaveBeenCalledWith({environmentId: 1, workflowId: 'wf1', workflowNodeName: undefined});
    });

    it('renders nothing when copilot disabled context (no workflowId)', () => {
        const {container} = render(
            <CopilotGenerateDescriptionButton environmentId={1} onApply={vi.fn()} workflowId={undefined} />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
```

- [ ] **Step 2: Run to verify fail**

Run: `cd client && npx vitest run src/shared/components/copilot/CopilotGenerateDescriptionButton.test.tsx`
Expected: FAIL — module missing.

- [ ] **Step 3: Implement**

```tsx
import Button from '@/components/Button/Button';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Loader2Icon, SparklesIcon} from 'lucide-react';

import {useGenerateWorkflowDescription} from './useGenerateWorkflowDescription';

interface CopilotGenerateDescriptionButtonPropsI {
    environmentId: number;
    onApply: (value: string) => void;
    workflowId?: string;
    workflowNodeName?: string;
}

const CopilotGenerateDescriptionButton = ({
    environmentId,
    onApply,
    workflowId,
    workflowNodeName,
}: CopilotGenerateDescriptionButtonPropsI) => {
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const {generate, isPending} = useGenerateWorkflowDescription();

    if (!ai.copilot.enabled || !ff1570 || !workflowId) {
        return null;
    }

    const handleGenerate = async () => {
        const result = await generate({environmentId, workflowId, workflowNodeName});

        onApply(result.value);
    };

    return (
        <Button
            aria-label="Generate with AI"
            disabled={isPending}
            icon={isPending ? <Loader2Icon className="animate-spin" /> : <SparklesIcon />}
            onClick={handleGenerate}
            size="iconXs"
            variant="ghost"
        />
    );
};

export default CopilotGenerateDescriptionButton;
```

- [ ] **Step 4: Run to verify pass**

Run: `cd client && npx vitest run src/shared/components/copilot/CopilotGenerateDescriptionButton.test.tsx`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/copilot/CopilotGenerateDescriptionButton.tsx \
        client/src/shared/components/copilot/CopilotGenerateDescriptionButton.test.tsx
git commit -m "2503 client - Add CopilotGenerateDescriptionButton"
```

---

### Task 14: Wire button into `WorkflowDialog` (workflow-level, edit mode only)

**Files:**
- Modify: `client/src/shared/components/workflow/WorkflowDialog.tsx`

- [ ] **Step 1: Add imports**

At the top (respect sorted imports):

```typescript
import CopilotGenerateDescriptionButton from '@/shared/components/copilot/CopilotGenerateDescriptionButton';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
```

- [ ] **Step 2: Read the current environment id**

Inside the component, after the existing hooks (respect hook ordering — place with other store hooks, before `form`):

```typescript
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
```

- [ ] **Step 3: Render the button next to the Description label**

Replace the Description `FormItem`'s `FormLabel` line so the label and button share a row, and wire `onApply` to `form.setValue`. The `description` `FormField` render becomes:

```tsx
                        render={({field}) => (
                            <FormItem>
                                <div className="flex items-center justify-between">
                                    <FormLabel>Description</FormLabel>

                                    {workflow?.id && (
                                        <CopilotGenerateDescriptionButton
                                            environmentId={currentEnvironmentId}
                                            onApply={(value) =>
                                                form.setValue('description', value, {shouldDirty: true})
                                            }
                                            workflowId={workflow.id}
                                        />
                                    )}
                                </div>

                                <FormControl>
                                    <Textarea
                                        placeholder="Cute description of your project deployment"
                                        {...field}
                                        onKeyDown={handleOnKeyDown}
                                    />
                                </FormControl>

                                <FormMessage />
                            </FormItem>
                        )}
```

- [ ] **Step 4: Type-check + run existing WorkflowDialog tests (if any)**

Run: `cd client && npm run typecheck`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add client/src/shared/components/workflow/WorkflowDialog.tsx
git commit -m "2503 client - Add AI generate-description button to WorkflowDialog"
```

---

### Task 15: Wire button into node `DescriptionTab` (Notes)

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab.tsx`

The Notes `Textarea` is uncontrolled (`defaultValue` + `key`). On apply, set the DOM value via a ref and reuse the existing `handleNotesChange` save path (it reads `event.target.value`).

- [ ] **Step 1: Add imports**

```typescript
import CopilotGenerateDescriptionButton from '@/shared/components/copilot/CopilotGenerateDescriptionButton';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
```

Also add `useRef` to the existing `react` import.

- [ ] **Step 2: Add the ref + env id (respect hook ordering)**

After the store hooks, before the debounced callbacks:

```typescript
    const notesTextareaRef = useRef<HTMLTextAreaElement>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
```

- [ ] **Step 3: Attach the ref + render the button**

Change the Notes `fieldset` to put the label and button on one row, attach the ref, and wire `onApply`:

```tsx
            <fieldset className="space-y-1">
                <div className="flex items-center justify-between">
                    <Label>Notes</Label>

                    <CopilotGenerateDescriptionButton
                        environmentId={currentEnvironmentId}
                        onApply={(value) => {
                            if (notesTextareaRef.current) {
                                notesTextareaRef.current.value = value;
                            }

                            handleNotesChange({
                                target: {value},
                            } as ChangeEvent<HTMLTextAreaElement>);
                        }}
                        workflowId={workflow.id}
                        workflowNodeName={currentNode?.workflowNodeName}
                    />
                </div>

                <Textarea
                    className="bg-white"
                    defaultValue={workflowTaskOrTrigger?.description}
                    key={`${currentNode?.componentName}-${workflowTaskOrTrigger?.type}_nodeNotes`}
                    name="nodeNotes"
                    onChange={handleNotesChange}
                    placeholder="Write some notes for yourself..."
                    ref={notesTextareaRef}
                    rows={6}
                />
            </fieldset>
```

(`workflow.id` is available from `useWorkflowDataStore`; `ChangeEvent` is already imported. If `workflow.id` may be undefined typed, cast `workflow.id as string` — the button itself no-ops when `workflowId` is falsy.)

- [ ] **Step 4: Type-check**

Run: `cd client && npm run typecheck`
Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add client/src/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab.tsx
git commit -m "2503 client - Add AI generate-description button to node Description tab"
```

---

## Part E — Full verification

### Task 16: Run full server + client checks

- [ ] **Step 1: Server format + checks for touched modules**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:ai:ai-copilot:ai-copilot-api:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-service:check \
          :server:ee:libs:ai:ai-copilot:ai-copilot-graphql:check
```
Expected: BUILD SUCCESSFUL (Spotless applies EE headers via `@version ee`; Checkstyle/PMD/SpotBugs pass).

- [ ] **Step 2: Client full check**

```bash
cd client && npm run check
```
Expected: lint + typecheck + tests all pass.

- [ ] **Step 3: Final commit (any spotless reformatting)**

```bash
git add -A
git commit -m "2503 2109 Apply spotless formatting" || echo "nothing to format"
```

---

## Self-Review

**Spec coverage:**
- #2109 backend mode + prompt + validation → Tasks 1-3; controller passthrough → Task 4. ✓
- #2109 inline bar, populate builder, replace-with-confirm, gating, threaded context → Tasks 5-7. ✓
- #2503 api/service/prompt/generator → Tasks 8-10; mutation + IDOR guard → Task 11. ✓
- #2503 op/hook, reusable button, WorkflowDialog (edit-only), DescriptionTab (ref + save) → Tasks 12-15. ✓
- Feature flag `ff-1570` + `ai.copilot.enabled` → enforced in `JsonSchemaCopilotBar` and `CopilotGenerateDescriptionButton`. ✓
- EE conventions / verification → headers on every new server file; Task 16. ✓

**Type consistency:** `PropertyCopilotMode.JsonSchema` (generated TS) ↔ `JSON_SCHEMA` (GraphQL/Java); `WorkflowDescriptionCopilotRequest(workflowId, workflowNodeName, environmentId)` matches generator + controller + hook input `{environmentId, workflowId, workflowNodeName}`; `generate(...)` return `{value}` matches `WorkflowDescriptionCopilotResult`. `onApply` signatures: bar applies `SchemaRecordType`, description button applies `string`. ✓

**Placeholder scan:** every code step contains full code; commands have expected output. The one judgment call (`SchemaRecordType.properties` index access) includes the exact fallback cast to use. ✓

**Known follow-up (not blocking):** `WorkflowDialog` is reused in embedded/automation contexts; the button only renders in EE + flag-on + edit-mode, so CE/embedded are unaffected. If `useEnvironmentStore` is not mounted in a given embedded route, `currentEnvironmentId` falls back to its store default (the button still calls with that id; server auth validates access regardless).
