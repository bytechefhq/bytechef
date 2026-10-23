# Cluster element call colouring - design

Issue: #6333 (Colour the executed path and live task status on the workflow canvas). Builds on PR #6336.

## Intent

The workflow canvas already colours task nodes during an editor test run (blue while running, green or red when
done, with a duration badge) and colours the stored run on the read-only canvas of the workflow execution sheet. The
nodes inside a cluster root stay grey, so when an AI Agent misbehaves the canvas cannot answer the first question a
builder asks: which tools did it call, and which of them failed? Today that answer is buried in the agent's logs.

This feature colours the cluster elements a cluster root *calls* - for an AI Agent, its tools - the same way task
nodes are coloured, live in the editor and afterwards for deployed runs.

## Goals

- During an editor test run, a tool node turns blue with a spinner when the agent calls it, then green on success or
  red on failure, with a duration badge. The wire from the root's Tools slot to that tool takes the same colour.
  Exactly the look task nodes and edges have today.
- Tools the agent never called stay uncoloured.
- A tool with any failed call in the run is red, even when the agent recovered and its own task finished green. Each
  node reports its own outcome; a later successful call of the same tool does not turn it green.
- A call is failed when it throws, except for a tool suspension. A call that returns normally is green whatever the
  result says: an HTTP 4xx/5xx, an MCP result flagged as an error or a component error payload are successful calls.
- Sub-agents are coloured all the way down: the Task Tool element, each sub-agent it ran (its SUBAGENT slot children)
  and each sub-agent's own tools, including sub-agents that run in the background and finish after the Task Tool call
  returned.
- A simulated tool call (`TOOL_SIMULATIONS` in an editor test run) is coloured exactly like a real call.
- In the workflow execution sheets (deployed runs; the automation sheet and the embedded EE sheet), the read-only
  canvas draws a cluster root the way the editor does - the frame with its slots, the round element nodes and their
  wires - and colours its tools from the stored run.
- Works for both AI Agent chat actions: `chat` and `streamChat`.

## Non-goals

- Colouring model, chat memory, RAG, guardrail or other always-run elements. They run on every agent call, so a
  colour carries no information. Only elements the root chooses to call are coloured: tools, MCP tools, the Task Tool
  and the sub-agents and tools beneath it.
- Inspecting tool results to decide success. Failure is "threw", nothing provider-specific.
- Changing other read-only canvases (template previews, `ReadOnlyWorkflowSheet`, deployment workflow panels, API
  collection, integration instance and MCP server screens). They keep the icon row.
- A call count or call timeline per tool. Duration shown is the call's (see "Left to the builder" for repeat calls).
- Changing how task nodes, the trigger node or task dispatchers are coloured.
- Showing tool inputs/outputs on the canvas. The execution panels and logs keep that role.

## Anti-goals (would make this a failure even if it works)

- Matching a call to a node by tool name. Tool names are model-facing, can be renamed through the `toolName`
  property, collide across elements, and one element (an MCP client) can expose many tools. The mapping must come
  from the element the tool callback was built from.
- Colours leaking between workflows or runs. Node states are keyed by node name and outlive a workflow; the existing
  guard (`useWorkflowTestNodeStates`, states scoped to the workflow that ran) must cover element states too.
- A tool left spinning blue after a run is stopped, fails or the stream errors. The existing RUNNING sweep
  (`clearRunningNodeStates`, the result backfill) must cover element states.
- Colouring that only works for `streamChat`. The non-streaming `chat` action must behave the same.

## Constraints

- Cluster elements are identified by their workflow node name (`ClusterElement.getWorkflowNodeName()`, e.g.
  `httpClient_1`). These are unique within a workflow, the same namespace as task names, and are what the canvas uses
  as node identity (`data.workflowNodeName`). Element states can therefore share the task node-state map.
- The AI Agent builds its tool callbacks per element in `AbstractAiAgentChatAction.getToolCallbacks`, which loops over
  `clusterElementMap.getClusterElements(TOOLS)` and calls `ClusterElementToolCallbacks.build(clusterElement, ...)`.
  That loop is where each callback can be bound to its element's workflow node name. Callbacks are already wrapped
  (`createObservableToolCallback`, simulation and suspension wrappers), so a further wrapper fits the existing shape.
  The observer must sit outside the simulation wrapper (`createSimulationAwareToolCallback`) so simulated calls are
  reported like real ones.
- Sub-agents do not go through that loop. The Task Tool (`AiAgentUtilsTaskTool`) is the TOOLS element; the sub-agents
  are its SUBAGENT slot children, each with a nested TOOLS slot whose callbacks are built in `buildSubagentToolCallbacks`
  via `ClusterElementToolCallbacks.build` (see also `AiAgentUtilsSubagentTool`, `ClusterElementSubagentExecutor`).
  Background sub-agents run on another executor (`TaskRepository`, `TaskOutputTool`), so their reports arrive from other
  threads and possibly after the Task Tool call, or even the agent task, has returned. The observer has to cover these
  paths too, and the slot filter on the client must admit SUBAGENT children and nested TOOLS slots.
- Tool failures reach the callback as thrown exceptions (`AiAgentToolFacade`, `AbstractToolFacade` catch nothing), so
  "failed" is observable with a try/catch around the call.
- Today only `streamChat` emits a per-call event (`tool_execution`, via its `SseTransport`, after the call returns,
  keyed by tool name). Neither action emits anything when a call starts. Chat memory tool callbacks
  (`concatToolCallbacks`) belong to the memory element, not TOOLS, and are not coloured.
- A tool call that suspends the job (approval, ask-user, wait) is not a failure. `ToolSuspension.isSuspendedToolResult`
  and `isToolSuspensionFailure` already distinguish these; such a call must not end red. It ends uncoloured or keeps its
  running state until resumed (builder's call, see below), never failed.
- Editor test runs stream over `WorkflowTestApiController`'s SSE emitter. `TestWorkflowExecutorImpl` already forwards
  `task_started` / `task_completed` / `task_failed` and `progress` snapshots (ported in this PR from 0_732), and
  component code can already push events onto the job's stream through the job's `SseStreamBridge`
  (`WebhookSseStreamBridge` maps an `__eventType` key to the SSE event name).
- Task executions persist a `metadata` map (`task_execution.metadata`, TEXT), but the platform task-execution API
  model (`TaskExecutionModel`, client `TaskExecution`) does not expose it. The execution sheet builds node states from
  stored task executions in `buildNodeStatesFromTaskExecutions` and feeds them through
  `WorkflowExecutionOverlayContext`.
- On the read-only canvas a cluster root renders as `ReadOnlyNode`, which shows its elements only as a row of small
  icons (`extractClusterElementIcons`, `+N` overflow). The editor renders the root as a cluster frame
  (`ClusterFrameShell`, `ClusterRootNode`, `AiAgentNode`, `layoutClusterFrames`). `useLayout` converts every task node
  to `readonly` when given `readOnlyWorkflow`.
- `ActionContext` is public SDK (`sdks/backend/java/component-api`). Its `Event` interface has only
  `publishActionProgressEvent`. Adding to it is a public API change; prefer a platform-internal path if one is clean.
- Distributed (EE microservice) deployments run the agent on a worker and the editor test run on the server-app. In
  single-tenant distributed mode the editor test path still runs in-process through `JobSyncExecutor`; live events
  only have to work on that path. Stored calls must work wherever the agent runs, because they travel with the task
  execution.
- Client conventions in `CLAUDE.md` apply (sort-keys, `I`/`Props` interface suffixes, `twMerge`, hook ordering), and no
  explanatory inline comments: rationale goes in commit messages.

## Decided

- **Look:** identical to task nodes - blue + spinner while in flight, green/red + duration badge after, wire coloured
  the same. Reuse `WorkflowTestNodeStatus` and `getTestNodeStateBorderClassName` rather than a new visual.
- **Scope:** TOOLS-slot elements (component tools, MCP tool providers, the Task Tool), plus the sub-agents the Task
  Tool runs and their own TOOLS elements, background sub-agents included.
- **Failure rule:** a call fails when it throws, suspensions excepted; returned error-shaped results are successes.
  Any failed call in the run makes the element red; the agent's own colour is unaffected.
- **Simulated calls:** coloured like real calls.
- **Surfaces:** editor test runs (live) and the workflow execution sheets (stored).
- **Execution sheet layout:** on the automation and embedded execution sheets only, the read-only canvas draws
  cluster roots as the editor's frame, coloured as above, behind an opt-in to the read-only layout. Every other
  read-only canvas keeps `ReadOnlyNode`'s icon row. *(My call: matches "deployments" and keeps the change off
  template previews and configuration screens.)*
- **Mapping:** by the element's workflow node name, bound where the callbacks are built. One element that provides
  several tool callbacks colours as one node.

## Shape of the solution (my proposal; the builder may refine)

1. **Server, agent.** In `getToolCallbacks`, wrap every callback built from a TOOLS element in an observer that knows
   the element's workflow node name and reports `started`, `completed` or `failed` (with error message and start/end
   time) for each call. Apply it in both actions regardless of whether a `ToolExecutionListener` is present.
2. **Server, live path.** During an editor test run, those reports become SSE events on the job's stream, named
   `cluster_element_started`, `cluster_element_completed`, `cluster_element_failed`, with a payload carrying `name`
   (workflow node name), the owning task's name, `status`, `startDate`, `endDate` and `error`.
3. **Server, stored path.** The per-element outcome of the run (status, first start, last end, error of a failed call)
   is saved with the agent's task execution and exposed on the task-execution API model, so the execution sheet can
   read it. Regenerate the OpenAPI model and client in separate `Generate` commits.
4. **Client, live.** `useWorkflowTestStream` handles the three new events by writing element states into the existing
   node-state map, applying the failure rule (a failed element is not overwritten by a later success).
   `clearRunningNodeStates` and the result backfill already sweep RUNNING entries; extend the backfill to read the
   stored per-element outcome from the result so the end state is right even if a live event was missed.
5. **Client, canvas.** Cluster element nodes and the Tools-slot wires read the node-state map the way task nodes and
   edges do (`useWorkflowTestNodeStates`, `useExecutedEdgeStatus` / `getExecutedEdgeStatus`). Non-TOOLS elements are
   never coloured even if a state with their name exists.
6. **Client, execution sheet.** `buildNodeStatesFromTaskExecutions` adds element states from the stored outcome. The
   read-only layout renders cluster roots as frames (reusing the editor's frame components in a non-interactive mode)
   instead of `ReadOnlyNode`'s icon row, inside the existing `WorkflowExecutionOverlayContext`.

## Left to the builder

- How the agent's observer reaches the SSE stream: the job's `SseStreamBridge` via the action context, an application
  event like `TaskProgressedApplicationEvent` picked up by `TestWorkflowExecutorImpl`, or another platform-internal
  route. It must work for `chat`, which has no `SseTransport` today, and must not change the public SDK unless there is
  no clean alternative.
- Where the stored outcome lives: `task_execution.metadata` under a dedicated key, or task output, or a new structure -
  and the field name on the API model. It must not change the agent's output that downstream tasks reference
  (`${aiAgent_1}`).
- Duration shown for a tool called more than once: total, last call, or first start to last end.
- What a suspended tool call looks like while the job is suspended (running, or uncoloured), as long as it is never red.
- Whether the read-only frame is a shared component with the editor's or a read-only variant, and how much of the
  editor frame chrome (lock, reset, add buttons) it drops - none of it may be interactive.
- How a background sub-agent that finishes after the agent's task execution is stored lands in the stored outcome
  (late update of the stored record, or recorded as still running), as long as a sub-agent that later fails is not
  left green.
- Whether other cluster roots with a TOOLS slot (if any besides the AI Agent and its sub-agents) get the same observer
  in this change or later.
- Test strategy; the existing patterns are `TestWorkflowExecutorTest` for stream events, the AI Agent action tests for
  callback wrapping, and `useWorkflowTestStream.test.ts`, `getExecutedEdgeStatus.test.ts`,
  `buildNodeStatesFromTaskExecutions.test.ts` on the client.

## Verification

- Unit tests as above, plus `./gradlew check` on every touched server module and `npm run check` in `client/`.
- A live run against a branch server: an AI Agent with a real model and three tools where the prompt makes it call
  one that succeeds and one that fails, never the third. In the editor, watch the called tools turn blue then
  green/red with the third staying plain; then deploy, trigger it, and check the execution sheet draws the frame with
  the same colours. This needs an LLM provider API key on the branch server.
