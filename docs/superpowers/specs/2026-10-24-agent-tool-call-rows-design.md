# Agent tool call rows - design

Issue: #6553. Stacked on 6333 (#6336), which records cluster element calls per task execution.

## Intent

When an AI Agent runs, its task row is a black box: the test output and the Workflow Executions sheet show one row for the agent, and what each tool was asked, what it returned and what it logged is buried in the agent's own output and logs. Each tool call should be a row of its own under the agent's row, so a user can select it and read its Input, Output (or Error) and Logs exactly as they read a task's.

## Decisions (from the user)

- **One row per call.** A tool the model called three times gets three rows, in call order, under the agent's row.
- **Per-call logs.** Each call row has a Logs tab showing only the entries written while that call ran.
- **Size cap.** A call's input and output are stored capped at 64 KB each; a truncated value ends with a visible marker.
- **Branch.** Own issue and PR, stacked on 6333.
- **Sub-agents nest.** A Task Tool sub-agent's tool calls are rows under that sub-agent's call row, not siblings of it:
  agent > sub-agent call > the tools it called. Each call records its parent call.
- **Background calls freeze.** A background sub-agent can keep calling tools after its agent's task has finished and
  been saved. Its rows show the state at the moment the agent's task finished; a call still running then is marked as
  still running when the agent finished. Nothing is written to the task execution after it has been saved.

## Calls made while writing this (open to correction)

- **Suspended calls complete on resume.** A call suspended for a human-in-the-loop answer is never invoked again; the
  resumed agent passes the answer to the model as that call's result. On resume the call's row completes, with the
  answer as its output, instead of staying started forever.
- **Call ids are UUIDs**, not task execution id plus position: the recorder is process-local and does not know how
  many calls a resumed task execution already stored, so positional ids could collide.
- **Rows carry the tool name** as well as the element's workflow node name, because an element such as the MCP client
  exposes many tools under one node name (`mcpClient_1 · search_docs`).
- **Input travels with the start.** The capped input rides on the call's started event, so a long-running call already
  shows what it was asked; the output and error arrive with its end event.
- **The agent's own Logs tab is unchanged** and keeps showing every entry of its task execution; only call rows filter
  to their own entries.

## Scope

- **Elements:** the same ones 6333 colours: TOOLS elements and Task Tool sub-agents (including their tools and background runs). The model, memory, RAG and guardrails get no rows.
- **Surfaces:** the editor's test output panel (live and after the run) and the automation and embedded Workflow Executions sheets. Both use the shared `workflow-executions` components, so one task list change covers both.
- **Per call:** a UUID id, its parent call's id (a sub-agent's tools point at the sub-agent's call), the element's workflow node name, the tool name, status (started, completed, failed), start and end dates, input (the arguments the model sent), output (the result returned to the model) or error.

### Non-goals

- No rows for model calls, memory reads, RAG retrievals or guardrail checks.
- No change to the canvas colouring; it keeps reading the per-element aggregate.
- No retention or redaction beyond what task inputs and outputs already get. Call inputs and outputs are stored verbatim (capped), with the same sensitivity as task data; the docs warning on task data covers them too.

## Design

### Server

- **Recording.** The recorder keeps, per task execution, an ordered list of calls next to today's per-element aggregate. A call's start records its input; its end records its output, or its error when it threw. The observing tool callback and the sub-agent executor already see both: `call(toolInput)` and its returned string for tools, the `TaskCall` and its result for sub-agents. A sub-agent's tools are wrapped with the agent's context today, so they also need the sub-agent's current call id to record as their parent. A suspended call stays started until the job resumes, then completes with the human's answer as its output.
- **Cap.** Input and output are capped at 64 KB each when recorded, ending with a marker such as `... [truncated, 1.2 MB]`.
- **Storage.** The drained call list is written to the task execution's metadata under a new key beside `clusterElementCalls`, by the same post-output processor and finished hook, so failed agent tasks keep their calls too. A resumed task execution appends to the stored list. Calls that start or end after the drain (background sub-agents) are not written back; whatever was still running at the drain is stored as still running when the agent finished.
- **API.** `TaskExecutionDTO` / the task execution model in the OpenAPI spec gain the call list (id, parent call id, element workflow node name, tool name, status, dates, input, output, error). The id lets the client select a call and ask for its logs. Generated code goes in separate Generate commits.
- **Live stream.** The `cluster_element_*` SSE events carry the call's id and parent id; the started event also carries the capped input, and the completed or failed event the capped output or error. The test output adds a running row as soon as a call starts and fills it in when the call ends.
- **Logs.** `LogEntry` gains an optional element workflow node name and call id, set for entries written through a cluster element's context while a call runs; an entry written inside a sub-agent's tool call carries that tool call's id. The job logs filters (`jobFileLogs` and the editor's `editorJobFileLogs`) accept a call id, so a call row fetches only its entries; the agent's row keeps its unfiltered query. Tools backed by external providers (MCP, for instance) never write through a ByteChef context, so their rows have an empty Logs tab.

### Client

- **Task list.** `WorkflowExecutionsAccordionItem` renders an agent task's calls as nested rows, the way it nests loop iterations and subflow children, with a sub-agent's calls nested under the sub-agent's row: tool icon, element name, the tool name when it differs, a call number when the element was called more than once (`fetch_ok #2`), status icon and duration. A call still running when its agent finished says so instead of showing a spinner. Collapsed rows follow the same expand control as other nested rows.
- **Details.** Selecting a call row shows the same details pane as a task: header with the element's name and date, then Input, Output (or Error) and Logs tabs. Input and Output render with the existing JSON/text viewer; Logs uses `WorkflowExecutionLogsContent` filtered to the call.
- **Live.** During a test run, a started call appears as a running row under the agent and fills in when the call returns.
- **Canvas.** Unchanged, except that selecting a call row could later highlight its element; out of scope here.

## Left to the builder

- Exact metadata key and DTO/model names.
- Whether the call list replaces the aggregate on the wire (the canvas could derive status from the calls) or sits beside it. Keeping both is simpler for this change.
- How log entries and nested tool calls learn the current call id (a scope around the call, or passing it through the element context).
- How the resume path finds the suspended call to complete (for example, its id saved with the suspension's continue parameters).
- Marker wording for truncated values.
- Row styling details, within the existing task list look.
