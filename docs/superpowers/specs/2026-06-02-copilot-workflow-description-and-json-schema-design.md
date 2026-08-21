# Copilot: Workflow Description + JSON Schema Generation — Design

- **Date:** 2026-06-02
- **Status:** Approved (design)
- **Edition:** EE only
- **Author:** Ivica Cardic
- **Issues:** [#2503](https://github.com/bytechefhq/bytechef/issues/2503) (workflow description),
  [#2109](https://github.com/bytechefhq/bytechef/issues/2109) (JSON schema for `JSON_SCHEMA_BUILDER`)

## Context

Two AI-Copilot enhancements that both build on the **Property Copilot** infrastructure
(`2026-06-02-property-copilot-design.md`): the EE-only, single-shot `generatePropertyValue`
GraphQL mutation with a mode switch (`TEXT` / `FORMULA`), the `useGeneratePropertyValue` client
hook, and the `PropertyCopilotButton` / `PropertyCopilotPopover` UI.

They are grouped because they share gating, conventions, and (for #2109) the exact same mutation
and hook. They differ in surface and grounding:

- **#2109** is a property value — the `RESPONSE_SCHEMA` string whose `controlType` is
  `JSON_SCHEMA_BUILDER`. It extends `generatePropertyValue` with a third mode, `JSON_SCHEMA`.
- **#2503** is *not* a property value. It reads the whole workflow (or one node) definition, has no
  `propertyPath`, and produces free-form prose. It gets its own mutation,
  `generateWorkflowDescription`, in the same `ai-copilot` modules.

### Existing infrastructure this builds on (verified on the working branch)

- **Property Copilot backend** (`server/ee/libs/ai/ai-copilot/`):
  - `ai-copilot-api`: `PropertyCopilotMode {TEXT, FORMULA}`, `PropertyCopilotRequest`,
    `PropertyCopilotResult`, `PropertyCopilotGenerator`.
  - `ai-copilot-service`: `PropertyCopilotGeneratorImpl` (calls `org.springframework.ai.chat.model.ChatModel`,
    cleans fences, validates FORMULA via `Evaluator` with one repair retry, records the
    `bytechef_property_copilot_generate` counter), `PropertyCopilotPromptBuilder`.
  - `ai-copilot-graphql`: `PropertyCopilotGraphQlController` (`generatePropertyValue`), schema
    `property-copilot.graphqls`. The controller authorizes by resolving the owning project from the
    client-supplied `workflowId` via `ProjectWorkflowService.getWorkflowProjectWorkflow(...).getProjectId()`
    and checking `PermissionService.hasProjectScope(projectId, "WORKFLOW_VIEW")` (IDOR guard).
  - Gating: `@ConditionalOnEEVersion` + `@ConditionalOnProperty(bytechef.ai.copilot.enabled=true)`;
    GraphQL controller also `@ConditionalOnCoordinator`.
- **Property Copilot frontend**:
  - GraphQL op `client/src/graphql/platform/copilot/generatePropertyValue.graphql`; generated
    `PropertyCopilotMode` enum + `useGeneratePropertyValueMutation` in `src/shared/middleware/graphql.ts`
    / `graphql-types.ts`.
  - `useGeneratePropertyValue` hook; `PropertyCopilotButton` (Sparkles trigger + popover, gated on
    `ai.copilot.enabled && ff-1570`); `PropertyCopilotPopover` (prompt textarea, plain-text preview,
    Replace/Insert).
  - Wired in `Property.tsx` (~line 247) for mention inputs, mode `Formula`/`Text`.
- **JSON Schema Builder** (the #2109 surface):
  - `Property.tsx` (~line 851) renders `PropertyJsonSchemaBuilder` for `controlType === 'JSON_SCHEMA_BUILDER'`.
  - `PropertyJsonSchemaBuilder` shows an "Open Response Template" button → `PropertyJsonSchemaBuilderSheet`.
  - `PropertyJsonSchemaBuilderSheet` has Designer (`JsonSchemaBuilder`) + Code Editor (Monaco) tabs,
    synchronized through `handleSchemaChange(newSchema)` (calls `onChange` → debounced persist).
  - The backing component property is `RESPONSE_SCHEMA` in `LLMConstants` (control type
    `JSON_SCHEMA_BUILDER`, type STRING, shown when `responseFormat == JSON`).
- **Workflow description surfaces** (the #2503 surfaces):
  - Workflow-level: `client/src/shared/components/workflow/WorkflowDialog.tsx` — a react-hook-form
    `Textarea` bound to `workflow.description`, persisted into the workflow definition JSON on Save.
  - Node-level: `client/src/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab.tsx`
    — the "Notes" `Textarea` bound to `task/trigger.description`, persisted via `saveWorkflowDefinition`
    / `saveClusterElementFieldChange` / `saveTaskDispatcherSubtaskFieldChange` (debounced).
  - Server: `WorkflowService.getWorkflow(String id)` returns the `Workflow` domain object whose
    `getDefinition()` is the workflow JSON (triggers + tasks).
- **Edition gating mirror (client):** `ai.copilot.enabled` from `useApplicationInfoStore` + `ff-1570`
  from `useFeatureFlagsStore`, exactly as `PropertyCopilotButton` / `CopilotButton`.

## Goals

1. **#2109** — On a `JSON_SCHEMA_BUILDER` property, the user opens the builder sheet, types a
   natural-language prompt ("an order with id, customer name, and a list of line items…"), and the
   AI generates a JSON Schema that populates the Designer + Code Editor directly for review/edit.
2. **#2503** — One click generates a human-readable description by analyzing the existing definition:
   the whole workflow (in `WorkflowDialog`) or a single node (in `DescriptionTab`). The result lands
   in the editable description/notes field.

Both are EE-only, single-shot, gated on `ai.copilot.enabled && ff-1570`.

## Non-goals (deferred)

- Multi-turn refinement / streaming.
- Generating descriptions from a free-form prompt (#2503 is "analyze the definition", no prompt box).
- Schema generation outside the `JSON_SCHEMA_BUILDER` sheet (no header Sparkles popover).
- Reusing / modifying the chat `CopilotPanel`.
- A new feature flag (reuse `ff-1570`).

---

## Feature #2109 — JSON Schema generation

### Backend (extend Property Copilot)

1. **`PropertyCopilotMode`**: add `JSON_SCHEMA` (Java enum + `property-copilot.graphqls` enum). Append
   last to keep ordering stable.
2. **`PropertyCopilotPromptBuilder`**: add a `JSON_SCHEMA` branch. It instructs the model to return
   **only** a JSON Schema object (Draft 2020-12) describing the structure the user wants, no prose, no
   code fences. It does **not** append the "available previous step outputs" or the function catalog
   (neither is relevant to output-shape definition) — those stay TEXT/FORMULA-only.
3. **`PropertyCopilotGeneratorImpl`**: add a `JSON_SCHEMA` path parallel to FORMULA's repair loop:
   - call the model, `clean()` (strip ``` fences / whitespace);
   - validate the result parses as a JSON **object** (via the platform JSON util); if it does →
     `PropertyCopilotResult(value, true, null)`;
   - if not, one repair retry ("The previous attempt was not valid JSON. Return only a valid JSON
     Schema object."); if it parses → success; else `PropertyCopilotResult(value, false, "The
     generated JSON schema could not be parsed; please review it.")`.
   - Record `bytechef_property_copilot_generate` with `mode=JSON_SCHEMA`, `outcome=success|invalid_json`.
   - Validation is **parse-only** (valid JSON object) for v1 — not full JSON-Schema meta-validation;
     the builder tolerates partial schemas and the user reviews the result.

No controller change is required: `generatePropertyValue` already accepts `mode`, and the existing
IDOR guard covers the schema property because it is reached with the same `workflowId`.

### Frontend

1. **Generated `PropertyCopilotMode`**: regenerate graphql types after the `.graphqls` enum change so
   the client enum gains `JsonSchema`.
2. **Thread context into the builder**: `Property.tsx` (~851) passes `workflowId`, `workflowNodeName`
   (`currentNode.name`), `environmentId` (`currentEnvironmentId`), and `propertyPath`
   (`calculatedPath ?? name`) into `PropertyJsonSchemaBuilder`, which forwards them to
   `PropertyJsonSchemaBuilderSheet`. The button/sheet only render the AI affordance when these are
   present and `currentNode?.name` exists (parity with `PropertyCopilotButton`'s `workflow.id &&
   currentNode?.name` guard).
3. **Inline "Generate with AI" bar** in `PropertyJsonSchemaBuilderSheet` (new small component, e.g.
   `JsonSchemaCopilotBar`):
   - gated on `ai.copilot.enabled && ff-1570` (renders nothing otherwise);
   - a prompt textarea + Generate button using the existing `useGeneratePropertyValue` hook with
     `mode: JSON_SCHEMA`, `propertyType: 'STRING'`;
   - on success: `JSON.parse(result.value)` → if it throws, show `result.message` / a parse error and
     do nothing; else apply via `handleSchemaChange(parsed)` (populates Designer + Code Editor);
   - **Replace-with-confirm**: if the current schema is non-empty (has `properties` / non-trivial
     content) when Generate succeeds, show a small confirm ("Replace the current schema?") before
     calling `handleSchemaChange`. Generating into an empty/default schema applies with no confirm.
   - placed at the top of the sheet body, above the tabs content; lives inside the sheet so the
     Designer/Code-Editor act as the live preview (no separate text preview).

### #2109 tests

- Backend: `PropertyCopilotGeneratorImplTest` — JSON_SCHEMA success, repair-then-success,
  repair-fail → `valid=false`; metric tag `mode=JSON_SCHEMA`. `PropertyCopilotGraphQlControllerTest`
  — `generatePropertyValue` with `JSON_SCHEMA` returns the payload (auth path unchanged).
- Frontend: `JsonSchemaCopilotBar` — gated rendering, generate → parse → `handleSchemaChange`,
  invalid-JSON handling, replace-confirm path; extend the sheet test for the threaded props.

---

## Feature #2503 — Workflow / node description generation

### Backend (new, in the `ai-copilot` modules)

1. **`ai-copilot-api`**:
   - `WorkflowDescriptionCopilotRequest(String workflowId, String workflowNodeName, long environmentId)`
     — `workflowNodeName == null` ⇒ whole-workflow description; non-null ⇒ that node's notes.
   - `WorkflowDescriptionCopilotResult(String value)`.
   - `WorkflowDescriptionCopilotGenerator` interface.
2. **`ai-copilot-service`**:
   - `WorkflowDescriptionCopilotGeneratorImpl` (`@Service @ConditionalOnEEVersion
     @ConditionalOnProperty(bytechef.ai.copilot.enabled=true)`): injects `WorkflowService`, `ChatModel`,
     `WorkflowDescriptionPromptBuilder`, `ObjectProvider<MeterRegistry>`. Reads
     `workflowService.getWorkflow(workflowId).getDefinition()`; for a node, extracts that node's
     entry (trigger or task — by `name`) from the definition. Calls the model, `clean()`s, returns
     the text. Records `bytechef_workflow_description_copilot_generate` counter with
     `scope=workflow|node`, `outcome=success`.
   - `WorkflowDescriptionPromptBuilder`: whole-workflow prompt summarizes triggers + tasks (types,
     labels, ordering) into a concise paragraph describing what the workflow does; node prompt
     summarizes the single node's component/type/label/params into a short note. Both: "return only
     the description text, no preamble, no fences".
3. **`ai-copilot-graphql`**:
   - New schema `workflow-description-copilot.graphqls`:
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
   - `WorkflowDescriptionCopilotGraphQlController` (`@Controller @ConditionalOnEEVersion
     @ConditionalOnCoordinator`): same IDOR guard as `PropertyCopilotGraphQlController` (resolve
     projectId from `workflowId`, require `WORKFLOW_VIEW`); delegates to the generator;
     `Optional<WorkflowDescriptionCopilotGenerator>` → `IllegalStateException` when copilot disabled.

### Frontend

1. **GraphQL op** `client/src/graphql/platform/copilot/generateWorkflowDescription.graphql` +
   regenerate types → `useGenerateWorkflowDescriptionMutation`. Thin hook
   `useGenerateWorkflowDescription`.
2. **`CopilotGenerateDescriptionButton`** (new, reusable): Sparkles icon button with loading spinner;
   gated on `ai.copilot.enabled && ff-1570` (renders nothing otherwise); props
   `{environmentId, workflowId, workflowNodeName?, onApply(value), disabled?}`; one click → mutate →
   `onApply(result.value)`; surfaces errors through the existing global fetch-interceptor toast.
3. **Wire into `WorkflowDialog`**: render the button next to the Description `FormLabel`, **edit-mode
   only** (`workflow?.id` present — a new unsaved workflow has nothing to analyze); `workflowNodeName`
   omitted; `onApply` → `form.setValue('description', value)`. Source `environmentId` from the
   environment store used elsewhere (resolve during implementation; e.g. `useEnvironmentStore` /
   `currentEnvironmentId`).
4. **Wire into `DescriptionTab`**: render the button next to the "Notes" `Label`; `workflowNodeName =
   currentNode.workflowNodeName`; `onApply` sets the (currently uncontrolled) Notes textarea via a
   ref and invokes the existing `handleNotesChange` persistence path (re-using its
   task/cluster/dispatcher routing). `environmentId` from the same source `Property.tsx` uses
   (`currentEnvironmentId`).

### #2503 tests

- Backend: `WorkflowDescriptionCopilotGeneratorImplTest` (whole-workflow + node, definition parsing,
  clean(), metric tags); `WorkflowDescriptionCopilotGraphQlControllerTest` (success, auth-denied,
  disabled → `IllegalStateException`).
- Frontend: `CopilotGenerateDescriptionButton` (gated rendering, click → mutate → onApply, error
  path); `WorkflowDialog` edit-mode-only rendering + `setValue`; `DescriptionTab` ref-set + save.

---

## Cross-cutting

- **Feature flag:** reuse `ff-1570` + `ai.copilot.enabled` for every new affordance.
- **Conventions:** EE license header + `@version ee` Javadoc on all new `server/ee` files (including
  tests — Spotless picks the header by content). Client: `sort-keys`, interface `…PropsI`/`…Props`,
  sorted destructured imports, Lucide `…Icon` imports, `twMerge` (no `cn`), hook ordering, no
  `_`-prefixed methods. Server: blank-line-before-control-statements, no method chaining beyond the
  allowed DSLs, `spotlessApply`.
- **Verification:** server `./gradlew spotlessApply check`; client `npm run check` (lint + typecheck
  + tests).

## Architecture summary (units & boundaries)

| Unit | Responsibility | Depends on |
|------|----------------|------------|
| `PropertyCopilotMode.JSON_SCHEMA` | new mode token | — |
| `PropertyCopilotPromptBuilder` (JSON_SCHEMA branch) | schema-generation prompt | request |
| `PropertyCopilotGeneratorImpl` (JSON_SCHEMA path) | call model + parse-validate + repair | ChatModel, JSON util |
| `JsonSchemaCopilotBar` (client) | prompt → generate → confirm → `handleSchemaChange` | `useGeneratePropertyValue` |
| `WorkflowDescriptionCopilotGenerator(Impl)` | read definition → prompt → text | WorkflowService, ChatModel |
| `WorkflowDescriptionPromptBuilder` | whole-workflow / node prompt | definition |
| `WorkflowDescriptionCopilotGraphQlController` | mutation + IDOR guard | generator, Permission/ProjectWorkflow |
| `CopilotGenerateDescriptionButton` (client) | one-click generate → `onApply` | `useGenerateWorkflowDescription` |

Each unit has one purpose, a narrow interface, and is independently testable. The two features touch
disjoint files except for the shared gating constants and the `ai-copilot-graphql` module
(independent schema files).
