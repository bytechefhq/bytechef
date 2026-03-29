# Agent Eval: Run Dialog, Tool Simulation UI & Backend Wiring

**Date:** 2026-03-30
**Scope:** Agent evaluation Phase 2 — UI restructuring and tool simulation feature

## Overview

Three changes to the agent evaluation system:

1. Move test execution initiation from the Runs tab into a dialog triggered from the Tests tab
2. Add UI for managing tool simulations per scenario
3. Wire tool simulations into the backend execution pipeline
4. Minor: reposition "Add Test" button in Tests tab

## 1. "Add Test" Button Repositioning

**File:** `EvalsTestsTab.tsx`

- Remove the dashed-border "New Test" button that currently sits below the test list
- Add a "New Test" button with `bg-blue-600` styling in the top-right corner of the Tests tab, always visible when tests exist
- The empty-state centered "New Test" button remains unchanged

**Layout:**
```
[Tests tab content area]
  [top row: flex justify-between]
    (left: empty or future use)
    (right: blue "New Test" button with PlusIcon)
  [test cards list below]
```

## 2. Run Test Dialog

### New Component: `RunTestDialog`

**Location:** `components/tests/RunTestDialog.tsx`

**Trigger:** Play button on `AgentEvalTestCard` — currently navigates to Runs tab. Changed to open `RunTestDialog` with the test ID.

**Dialog contents:**
- Title: "Run Test — {testName}"
- Scenario checkboxes (fetched via `useAgentEvalTestQuery`, all selected by default)
- Global judge checkboxes (fetched via `useAgentJudgesQuery`, all selected by default)
- "Run Test" button (blue, disabled when no scenarios selected)

**On submit:**
1. Call `startAgentEvalRun` mutation with selected scenario IDs and judge IDs
2. Close dialog
3. Set `selectedTestId` in store
4. Switch `evalsTab` to `'runs'`
5. Set `selectedRunId` to the new run's ID (navigates to detail page)

**Props:**
```typescript
interface RunTestDialogProps {
    onClose: () => void;
    test: AgentEvalTestListItemType;
    workflowId: string;
    workflowNodeName: string;
}
```

### Runs Tab Changes

**File:** `EvalsRunsTab.tsx`

Remove from the Runs tab:
- Test selector dropdown
- Scenario checkboxes section
- Judge checkboxes section
- "Run Test" button
- `generateRunName()` function (moves to `RunTestDialog`)
- `SCENARIO_TYPE_COLORS` and `JUDGE_TYPE_COLORS` constants (move to `RunTestDialog`)

Keep in the Runs tab:
- Run history list (`AgentEvalRunList`) filtered by `selectedTestId`
- Run detail view (`AgentEvalRunDetail`)
- "No test selected" empty state
- Cancel run functionality
- Run polling logic

The Runs tab becomes a read-only history viewer. All run initiation happens through the dialog.

### Store Changes

No store changes needed. `selectedTestId` and `selectedRunId` are already in `useAiAgentEvalsStore`.

## 3. Tool Simulation UI

### In `AgentEvalScenarioRow` Expanded Area

Below the existing judges section, add a "Tool Simulations" section with the same visual pattern:

```
[Judges section - existing]
  [judge rows...]
  [+ Add Judge button]

[Tool Simulations section - new]
  [simulation rows...]
  [+ Add Tool Simulation button]
```

Each simulation row shows:
- Wrench icon (WrenchIcon from lucide-react)
- Tool name (bold)
- Response prompt (truncated, gray)
- If `simulationModel` is set: small badge showing model name
- Edit button (PencilIcon)
- Delete button (TrashIcon)

Empty state: "No tool simulations" centered text.

**Mutations used:** `useCreateAgentScenarioToolSimulationMutation`, `useUpdateAgentScenarioToolSimulationMutation`, `useDeleteAgentScenarioToolSimulationMutation` (already generated in `graphql.ts`).

### New Component: `CreateToolSimulationDialog`

**Location:** `components/tests/CreateToolSimulationDialog.tsx`

**Fields:**
- Tool Name (Input, required) — the name of the tool to intercept
- Response Prompt (Textarea, required) — instructions for generating the simulated response, or the verbatim response if no model is set
- Simulation Model (Input, optional) — when set, an LLM generates the response from the prompt; when empty, the prompt is returned verbatim

**Edit mode:** Same pattern as `CreateJudgeDialog` — receives `editData` prop, switches to update mutation.

**Props:**
```typescript
interface CreateToolSimulationDialogProps {
    agentEvalScenarioId: string;
    editData?: {
        id: string;
        responsePrompt: string;
        simulationModel?: string | null;
        toolName: string;
    };
    onClose: () => void;
    onCreate: (toolName: string, responsePrompt: string, simulationModel?: string) => void;
    onUpdate?: (id: string, toolName?: string, responsePrompt?: string, simulationModel?: string) => void;
}
```

### GraphQL Operations

The `.graphql` operation files for tool simulations already exist:
- `createAgentScenarioToolSimulation.graphql`
- `updateAgentScenarioToolSimulation.graphql`
- `deleteAgentScenarioToolSimulation.graphql`

The `agentEvalTests.graphql` query already fetches `toolSimulations` on scenarios. No new GraphQL files needed.

## 4. Backend Tool Simulation Wiring

### Execution Flow

```
AgentEvalRunExecutor.executeScenario()
  -> load tool simulations for scenario
  -> pass to aiAgentTestFacade.executeAiAgentAction() as new parameter
  -> actionDefinitionFacade.executePerform() receives simulations
  -> agent component intercepts matching tool calls
     -> if simulationModel is null: return responsePrompt verbatim
     -> if simulationModel is set: use ChatClient to generate response
```

### Interface Changes

**`AiAgentTestFacade`** — add `toolSimulations` parameter:
```java
Object executeAiAgentAction(
    String workflowId, String workflowNodeName, long environmentId,
    String conversationId, String message, List<Object> attachments,
    Map<String, ToolSimulationConfig> toolSimulations);
```

Where `ToolSimulationConfig` is a simple record:
```java
record ToolSimulationConfig(String responsePrompt, @Nullable String simulationModel) {}
```

**`AiAgentTestFacadeImpl`** — pass tool simulations through to `actionDefinitionFacade.executePerform()` via the parameters map (e.g., as a `__toolSimulations` key in `evaluatedParameters`).

### AgentEvalRunExecutor Changes

**In `executeScenario()`:**
1. Load simulations: `agentScenarioToolSimulationService.getAgentScenarioToolSimulations(scenario.getId())`
2. Build a `Map<String, ToolSimulationConfig>` keyed by tool name
3. Pass to `aiAgentTestFacade.executeAiAgentAction()`

**In `executeMultiTurnScenario()`:**
Same — load simulations once before the turn loop, pass on each `executeAiAgentAction()` call.

### Tool Call Interception

The agent component handler checks the `__toolSimulations` parameter before executing each tool call:
- If the tool name has a matching simulation entry:
  - No model: return `responsePrompt` as the tool result
  - With model: build a ChatClient for the simulation model, send a prompt like "Given this tool call input: {input}, generate a realistic response following these instructions: {responsePrompt}", return the generated response
- If no match: execute the real tool as normal

### Dependencies

`AgentEvalRunExecutor` needs `AgentScenarioToolSimulationService` injected (add to constructor).

## File Change Summary

### New Files
- `client/src/pages/.../ai-agent-evals/components/tests/RunTestDialog.tsx`
- `client/src/pages/.../ai-agent-evals/components/tests/CreateToolSimulationDialog.tsx`

### Modified Files (Client)
- `EvalsTestsTab.tsx` — reposition "Add Test" button
- `AgentEvalTestCard.tsx` — play button opens `RunTestDialog` instead of switching tabs
- `AgentEvalScenarioRow.tsx` — add tool simulation section in expanded area
- `EvalsRunsTab.tsx` — remove run initiation controls, keep history view
- `useAgentEvalsRunsTab.ts` — simplify (remove start-related logic if fully moved)

### Modified Files (Server)
- `AgentEvalRunExecutor.java` — load and pass tool simulations
- `AiAgentTestFacade.java` — add `toolSimulations` parameter
- `AiAgentTestFacadeImpl.java` — pass simulations to execution
- Agent component handler — intercept tool calls with simulations
