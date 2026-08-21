# Sample-Output Copilot — Design

- **Date:** 2026-07-09
- **Status:** Approved (design)
- **Author:** Ivica Cardic

## Summary

Replace the one-shot **"Generate with AI"** bar inside the *Upload Sample Output Data*
dialog with an embedded, side-by-side **CopilotPanel** chat. The chat builds and refines
the node's sample output JSON conversationally and applies the result directly into the
Monaco editor. The existing **Upload** button and its REST persistence path are untouched.

The one-shot generation feature is removed entirely — client and server (the EE
`sampleoutput` GraphQL package).

This introduces a new copilot **source / agent type** — `SAMPLE_OUTPUT` — cloned 1:1 from
the recently-added `JSON_SCHEMA_BUILDER` source, which is the closest analog (it also emits
structured JSON via a tool and applies it back into an editor without persisting server-side).

## Motivation

- The "Generate with AI" bar is a dead-end one-shot: type a description, press Generate,
  get a single JSON blob. It cannot iterate, ask clarifying questions, or refine.
- Every other AI-assisted surface in the workflow editor (workflow editor, code editor,
  workflow code editor, cluster element, JSON schema builder) uses the shared **CopilotPanel**
  chat pattern. Sample output is the last surface still on the bespoke one-shot bar.
- Consolidating onto the shared pattern removes bespoke GraphQL + service code and gives
  sample-output generation the same iterate/refine/apply loop as everything else.

## Non-goals

- No change to how sample output is **persisted**. The `WorkflowNodeTestOutput` REST stack
  (`uploadWorkflowNodeSampleOutput`) and the **Upload** button stay exactly as they are.
- No change to the shared copilot infrastructure (`CopilotPanel`, the three registries,
  `CopilotRuntimeProvider`, `CopilotApiController` dispatch mechanism). We only *add* a
  source to it.
- The shared feature flags `ff-1570` and `ai.copilot.enabled` are **not** removed — they
  gate many surfaces. Only their *uses inside the removed bar* go away.

## Chosen approach

Two design decisions were made up front:

1. **Panel placement: embedded, side-by-side.** The CopilotPanel renders *inside* the
   (widened, heightened) dialog, next to the Monaco editor — mirroring
   `PropertyJsonSchemaBuilderSheet`'s editor + `CopilotPanel` split. The dialog is made
   taller/wider so the two-column layout reads well.
2. **Chat modes: ASK + BUILD.** Exactly mirrors `JSON_SCHEMA_BUILDER`. `ASK` is
   conversational (no auto-apply); `BUILD` emits the `updateSampleOutput` tool and applies
   JSON to the editor. Two agent beans, two prompt files, the panel's mode toggle.

## Architecture

A copilot "source" is a thin routing key, not a monolith:

- The client `Source` enum value lowercases into the request URL
  `/api/platform/internal/ai/chat/sample_output` (built in `CopilotRuntimeProvider`).
- The EE `CopilotApiController` maps `sample_output` + a `Mode` (ASK/BUILD) to a Spring bean
  named `sample_output_ask` / `sample_output_build`, resolved from the `LocalAgent` map keyed
  by `agentId`.
- Apply-back is decoupled via three registries the screen participates in — the shared
  `CopilotPanel` never learns anything about sample output:
  - `useCopilotStateContributorRegistry` — contribute live `currentSampleOutput` + node context.
  - `useCopilotToolResultHandlerRegistry` — handle the named `updateSampleOutput` tool result.
  - `useCopilotPostTurnRegistry` — after each turn, apply the pending JSON and replace the raw
    tool dump with a short confirmation.
- The tool itself persists nothing: like `JsonSchemaTools.updateJsonSchema`, it validates the
  JSON and echoes `{"sampleOutput": …}`; the client applies it to the editor. Persistence
  remains the job of the existing **Upload** button.

## Detailed changes

### Server — add `SAMPLE_OUTPUT` (CE module `server/libs/ai/ai-copilot/`)

1. **`ai-copilot-api` → `com/bytechef/ai/copilot/util/Source.java`**
   Append `SAMPLE_OUTPUT` to the enum (append at end — ordinal stability convention).

2. **`ai-copilot-tool` → `com/bytechef/ai/copilot/tool/CopilotAgentType.java`**
   Append (at end): `SAMPLE_OUTPUT_ASK("sample_output_ask", false)`,
   `SAMPLE_OUTPUT_BUILD("sample_output_build", false)`,
   `SAMPLE_OUTPUT("sample_output", true)` (fallback),
   `SAMPLE_OUTPUT_AGENT("sample_output_agent", false)`.

3. **`ai-copilot-tool` → new `com/bytechef/ai/copilot/tool/SampleOutputTools.java`**
   Clone of `JsonSchemaTools`. A `@Tool updateSampleOutput(String sampleOutput)` that parses
   the JSON string with `JsonUtils.read`, and returns `JsonUtils.write(Map.of("sampleOutput",
   parsed))`; on `RuntimeException` returns `{"error": "Invalid JSON: …"}`. No persistence.

4. **`ai-copilot-service` → new `com/bytechef/ai/copilot/agent/SampleOutputSpringAIAgent.java`**
   Clone of `JsonSchemaBuilderSpringAIAgent`. `createSystemMessage` injects the
   `currentSampleOutput` state key (plus `state` and `contexts`) into the system message.
   `toolContext` delegates to `CopilotToolContextUtils.toToolContext(input.state())`.

5. **`ai-copilot-service` → `com/bytechef/ai/copilot/config/CopilotConfiguration.java`**
   - Two prompt `Resource` fields + constructor `@Value` params:
     `classpath:prompt_sample_output_ask.txt`, `classpath:prompt_sample_output_build.txt`.
   - Bean `sampleOutputAskSpringAIAgent` — agentId `sample_output_ask`, ASK prompt, empty tools
     (via `wrapTools(securityContextRehydrator, List.of())`).
   - Bean `sampleOutputBuildSpringAIAgent` — agentId `sample_output_build`, BUILD prompt,
     tools `List.of(new SampleOutputTools())` wrapped via `wrapTools`.
   - Both follow the exact shape of the `jsonSchemaBuilderAsk/Build` beans (chatMemory,
     chatModel, state, `overrideChatClientResolver`).

6. **Two prompt files** under `ai-copilot-service/src/main/resources/`:
   `prompt_sample_output_ask.txt`, `prompt_sample_output_build.txt`. Modeled on the JSON
   schema builder prompts but instructing the agent to produce a realistic **sample output
   value** (concrete example JSON data, not a JSON *schema*) for the given workflow node, and
   in BUILD mode to call `updateSampleOutput` with the complete JSON.

7. **EE `ai-copilot-rest` →
   `com/bytechef/ee/ai/copilot/web/rest/CopilotApiController.java`**
   Add a routing branch: `else if (agentId.equals("sample_output"))` → `sample_output_build`
   when `Mode.BUILD`, else `sample_output_ask`.

### Client — add `SAMPLE_OUTPUT` source + wire the dialog

1. **`shared/components/copilot/stores/useCopilotStore.ts`**
   Append `SAMPLE_OUTPUT = 'SAMPLE_OUTPUT'` to the `Source` enum.

2. **New hook**
   `pages/platform/workflow-editor/components/node-details-tabs/output-tab/hooks/useSampleOutputCopilot.ts`
   Clone of `usePropertyJsonSchemaBuilderCopilot`. Responsibilities:
   - `handleCopilotOpen()` — `saveConversationState`, `resetMessages`,
     `generateConversationId`, `setContext({mode: MODE.ASK, parameters: {workflowId,
     workflowNodeName}, source: Source.SAMPLE_OUTPUT})`, open.
   - `handleCopilotClose()` — `restoreConversationState`, close.
   - Register state contributor → `{currentSampleOutput, workflowId, workflowNodeName}`
     (read live from a `sampleOutputRef`).
   - Register tool-result handler for `updateSampleOutput` → parse `{sampleOutput}`, stash in
     a `pendingRef`, call `onApply(json)` immediately.
   - Register post-turn for `Source.SAMPLE_OUTPUT` → if pending, append
     `✓ Applied the sample output.` to the last assistant message.

3. **`…/output-tab/OutputTabSampleDataDialog.tsx`**
   - Remove the `SampleOutputCopilotBar` import + its rendered block.
   - Widen + heighten the dialog (see Layout below); lay out a flex row: **CopilotPanel column
     | Monaco editor column**.
   - Use `useSampleOutputCopilot` (passing `onApply`, `sampleOutputRef`, `workflowId`,
     `workflowNodeName`); drive `handleCopilotOpen/Close` from the dialog open lifecycle.
   - Render `<CopilotPanel source={Source.SAMPLE_OUTPUT} open={…} onClose={handleCopilotClose}
     className="… border-l …"/>` — mirroring `PropertyJsonSchemaBuilderSheet`.
   - Gate the chat column on `ai.copilot.enabled` + `ff-1570`. When copilot is unavailable,
     fall back to the editor-only dialog (current behavior, narrower).
   - Prune the now-unused `environmentId` prop; keep/thread `workflowNodeName` for the
     copilot context (add it if the parent doesn't already pass it).

4. **`…/output-tab/OutputTab.tsx`**
   Update the `OutputTabSampleDataDialog` usage: drop `environmentId`, ensure `workflowId` and
   `workflowNodeName` are passed.

### Removals — "Generate with AI"

**Client:**
- `shared/components/copilot/SampleOutputCopilotBar.tsx` (+ `.test.tsx`)
- `shared/components/copilot/useGenerateSampleOutput.ts`
- `graphql/platform/copilot/generateSampleOutput.graphql`
- Re-run `cd client && npx graphql-codegen` to drop `GenerateSampleOutput*` from
  `graphql.ts` / `graphql-types.ts` (do not hand-edit generated files).

**Server (EE `server/ee/libs/ai/ai-copilot/`), whole `sampleoutput` package:**
- `ai-copilot-graphql`: `web/graphql/SampleOutputCopilotGraphQlController.java`,
  `resources/graphql/sample-output-copilot.graphqls`, and the controller test.
- `ai-copilot-api`: `sampleoutput/SampleOutputCopilotGenerator.java`,
  `SampleOutputCopilotRequest.java`, `SampleOutputCopilotResult.java`.
- `ai-copilot-service`: `sampleoutput/SampleOutputCopilotGeneratorImpl.java`,
  `SampleOutputPromptBuilder.java`, and both service tests.

**Kept:** the dialog shell, Monaco editor, Upload button,
`useUploadSampleOutputRequestMutation`, `WorkflowNodeTestOutputApi`, and the entire
`WorkflowNodeTestOutput` REST controller/facade/service persistence stack.

## Layout / dialog sizing

Custom tailwind tokens already exist:
`max-w-output-tab-sample-data-dialog-width` and `min-h-output-tab-sample-data-dialog-height`.

- Widen `max-w-output-tab-sample-data-dialog-width` so two columns (chat + editor) fit.
- Raise `min-h-output-tab-sample-data-dialog-height` so the chat has vertical room — this is
  the "make the dialog taller so CopilotPanel makes sense" requirement.
- The editor column keeps the existing Monaco setup and placeholder; the chat column is the
  `CopilotPanel` with a left border (`border-l border-l-border/50`), matching the JSON schema
  builder split.
- Editor-only fallback (copilot unavailable) uses the original narrower sizing.

## Testing

- **Client:** adapt/replace `OutputTabSampleDataDialog.test.tsx` — drop the AI-bar assertions;
  add coverage that the copilot column renders when copilot is available and is absent
  otherwise, and that an `updateSampleOutput` tool result applies JSON into the editor state.
  Add a `useSampleOutputCopilot.test.ts` mirroring the JSON schema builder copilot hook test
  (open/close conversation lifecycle, contributor/tool-result/post-turn registration).
  Run `cd client && npm run check`.
- **Server:** add `SampleOutputToolsTest` and `SampleOutputSpringAIAgentTest` (or the minimal
  bean-wiring assertions mirroring the JSON schema builder agent tests). Ensure the
  `EnumOrdinalStabilityTest`-style pins (if any cover `Source`/`CopilotAgentType`) still pass
  with the appended values. Remove the deleted `sampleoutput` package tests. Run
  `./gradlew spotlessApply` then `./gradlew check`.

## Risks / notes

- **Shared flags.** `ff-1570` and `ai.copilot.enabled` gate many surfaces — only remove their
  uses in the deleted bar, never the flags.
- **Enum ordinals.** Append new `Source` / `CopilotAgentType` members at the end.
- **Codegen, not hand-edits.** Removing the GraphQL operation + schema and re-running codegen
  is the clean path for the generated client/server GraphQL artifacts.
- **Prop pruning.** `environmentId` was only feeding the old bar; the new copilot needs
  `workflowId` + `workflowNodeName` instead. Verify no other consumer of the dialog relies on
  `environmentId`.
- **EE vs CE split.** The new agent/tool/config live in CE `ai-copilot` (Apache header,
  mirroring `JsonSchemaBuilder`); only the REST dispatch branch and the removed `sampleoutput`
  GraphQL package are EE.
