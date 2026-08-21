# MCP Guidance and Clean-Text Questions — Implementation Plan (spec step 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give external MCP clients (Claude Desktop, Cursor) the sequencing knowledge that panels
get from a system prompt — via the protocol's `instructions` field and tool-description
preconditions — and make subagent questions come back as clean text instead of a JSON envelope.

**Prerequisites:** none. This plan is independent of the catalog and may land before or after it.
If the rename plan has NOT landed yet, use the current tool names in the instructions text and
descriptions, and note in the rename plan's Task 2 that these strings are additional prompt-family
reference sites (the rename grep will find them).

**Tech Stack:** Java 25 / Spring Boot 4, MCP Java SDK (`McpServer.async` builder), Spring AI.

## Global Constraints

- Work on `0_732`; user commits in parallel — fresh commits, never amend,
  `git commit -m "..." -- <paths>`.
- Commit messages `732 <description>`.
- CE Apache 2.0 headers; EE Enterprise header + `@version ee`.
- Java style per CLAUDE.md; log-file verification, never piped tails.
- The panel/hub JSON question contract is **client-load-bearing** (`toToolResultDataPart` renders
  the choice card from it). Nothing in this plan may alter what panels and AI Hub receive.

---

### Task 1: Server `instructions`

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java`

The builder currently sets only `.serverInfo("mcp-server", "1.0.0")`. Add `.instructions(...)`
(verify the exact builder method name against the MCP SDK version on the classpath; it is the
field returned in the `initialize` result).

- [ ] **Step 1:** Add the instructions text. Draft (adjust tool names to whatever is live on the
  branch):

  > ByteChef management server. Two kinds of tools: ordinary tools are deterministic CRUD —
  > call them directly; intelligent tools (buildWorkflow, importWorkflow, configureClusterElement,
  > writeScript, buildCodeWorkflow, buildCustomComponent, authorSkill, debugWorkflowExecution) run
  > an inner AI agent and may take minutes — call them for judgment work, not for CRUD.
  > To build a workflow: createProject (if needed) → createProjectWorkflow → buildWorkflow with the
  > workflowId and a plain-language instruction. The intelligent tools re-read current state on
  > every call, so iterate by calling them again with the next instruction.
  > If an intelligent tool's result is a question with numbered options, present it to the user and
  > call the SAME tool again with the chosen answer as the instruction — the agent resumes with its
  > context intact.
  > Most tools require workspace context: if a tool returns workspace_required with candidates,
  > retry with one of the listed workspaceId values.

- [ ] **Step 2:** Compile the module (log, `$?`, FAILED grep). There is no unit seam for the
  builder; verification is manual (below).
- [ ] **Step 3:** Commit with pathspec.

### Task 2: Preconditions in tool descriptions

**Files:** the DESCRIPTION constants of the nine intelligent delegate callbacks (post-catalog: the
descriptions live where the definitions are built — re-derive with
`git grep -n "DESCRIPTION" -- 'server/**/*AgentToolCallback.java'` and the contributor classes).

- [ ] **Step 1:** For each intelligent tool whose use has a precondition, append one sentence to its
  description. Minimum set:
  - `buildWorkflow`: "The workflow must already exist — create it with createProjectWorkflow
    first, then pass its workflowId."
  - `importWorkflow`: same shape (target workflow id required — see the narrowing plan; if that
    plan has not landed, describe today's contract instead and leave a note there).
  - `buildCustomComponent` / `buildCodeWorkflow` / `authorSkill`: name the create-first tool if the
    narrowed contract requires an existing artifact; otherwise state what the tool creates.
  - `configureClusterElement` / `writeScript` / `debugWorkflowExecution`: name the id each needs
    and where it comes from.
- [ ] **Step 2:** Descriptions are read by every surface's model, not just MCP — keep each to one
  or two sentences so the pinned-list token cost stays flat.
- [ ] **Step 3:** Compile + affected module checks; commit with pathspec.

### Task 3: Clean-text questions on MCP

**The seam (do not sniff JSON):** the delegate callback runs the delegation through
`SubAgentAskRelay.runWithChannel`, which returns `AskOutcome(result, pendingQuestion)` — the
callback knows structurally whether a question was raised. Today the ask branch returns the JSON
envelope as the tool result on every surface.

**Files:**
- Modify: the delegate callback classes that carry the ask relay (re-derive:
  `git grep -ln "SubAgentAskRelay\|AskOutcome" -- 'server/**/*.java' | grep -v test`)
- Modify: the MCP contributor configs (CE `ToolCallbackContributorConfiguration`, EE
  `AutomationCopilotMcpContributorConfiguration`, `EmbeddedCopilotMcpContributorConfiguration`) or,
  post-catalog, the single callback-decorator argument at each MCP `catalog.getAll` call site.
- Test: unit test on the formatter.

- [ ] **Step 1:** Add a question renderer to the delegate callback — an enum constructor parameter,
  default `JSON` (existing behaviour, zero call-site churn):
  ```java
  public enum SubAgentQuestionRenderer {
      JSON, PLAIN_TEXT
  }
  ```
  In the ask branch, `PLAIN_TEXT` formats `pendingQuestion` as:
  ```
  The <toolName> agent needs a decision before continuing:

  <question text>

  1. <option label> — <option description>
  2. ...

  Present these options to the user. Then call <toolName> again with the chosen answer as the
  instruction — the agent will resume where it left off.
  ```
  The final sentence is load-bearing: the re-delegation contract is implicit in the panel flow, and
  without stating it an external client answers the user and never calls back.
- [ ] **Step 2:** Unit-test the formatter against a two-option question envelope: assert the
  numbered options, the question text, and the re-invoke sentence appear; assert `JSON` mode is
  byte-identical to today's payload.
- [ ] **Step 3:** Pass `PLAIN_TEXT` at the MCP construction sites only. Panels and AI Hub keep the
  default — grep the hub and panel sites afterward to confirm neither passes the new parameter.
- [ ] **Step 4:** Module checks; commit with pathspec.

## Manual verification (running backend + a real MCP client)

1. Connect Claude Desktop (or `npx @modelcontextprotocol/inspector`) to
   `/api/management/{secretKey}/mcp`; confirm `initialize` carries the instructions and tools/list
   shows the preconditions in descriptions.
2. Ask for something that makes a specialist raise a question (e.g. an MCP-server setup with an
   ambiguous project). Confirm the tool result is readable text with numbered options, and that
   answering leads the client to re-invoke the same tool.
3. Open a Copilot panel and trigger a specialist question — the choice card still renders (proves
   the JSON contract untouched).

## Self-review notes

- **Not in scope:** MCP progress notifications for long-running intelligent calls. Recorded in the
  spec as known work; it needs transport-level support and deserves its own plan once the SDK
  exposes it cleanly.
- The instructions text and descriptions are the rename plan's blast radius — whichever plan lands
  second must re-run its name grep over these strings.
