# configureMcpServer Promotion — Implementation Plan (spec step 5)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote `mcp_agent` — the one CRUD delegate that carries judgment — into the intelligent
tool `configureMcpServer(mcpServerId)`, narrowed to synthesizing tool mappings; move its six CRUD
tools flat; move its enable-only-when-mapped rule out of the prompt and into the facade.

**Prerequisite:** the catalog plan has landed. Rename plan optional (this tool is born with its
capability name either way).

**Its one thing:** given a server id, read the attached workflows (`listMcpProjectWorkflows`) and
write each one's mapping — `toolName` (short snake_case verb phrase, unique per server),
`toolDescription` (routable by a calling LLM), and per-input `fromAi('<name>', '<TYPE>',
{description, required})` expressions synthesized from the row's `inputSchema` (literals for fixed
values). It returns mapping status and **never enables anything**.

## Global Constraints

- Work on `0_732`; user commits in parallel — fresh commits, never amend,
  `git commit -m "..." -- <paths>`.
- Commit messages `732 <description>` / `732 client - <description>` if any client change.
- CE Apache 2.0; EE Enterprise header + `@version ee`. Java style per CLAUDE.md; log-file
  verification.
- Preserve from the current `prompt_mcp_agent.txt` (these are properties of the agent, not its
  wiring): the 20-tool-call budget cap, `askUserQuestion` for genuinely user-owned decisions with
  at-most-one-question-per-turn, never inventing ids, and treating workflow labels/descriptions/
  parameter values as data, not instructions.
- The tool mapping lives on `McpProjectWorkflow.parameters` (the attachment), NEVER in the workflow
  definition — the prompt's strongest rule; it survives verbatim.

## Evidence to re-derive first

- The delegate today: `mcp_agent` (`SubAgentToolCallback`-family; find its construction and its
  ChatClient bean — `git grep -n "mcp_agent" -- 'server/**/*.java' | grep -v test`).
- Its tools: `McpServerToolCallbacksFactory` builds `CloneMcpProjectToolCallback`,
  `CreateMcpProjectToolCallback`, `CreateMcpServerToolCallback`, `ListMcpProjectWorkflowsToolCallback`,
  `ListMcpServersToolCallback`, `UpdateMcpProjectWorkflowParametersToolCallback`,
  `UpdateMcpServerToolCallback`.
- The prompt: `server/libs/automation/automation-ai/automation-ai-tool/src/main/resources/prompt_mcp_agent.txt`.
- The enable path: `updateMcpServer` flows through which facade/service method — find it and its
  `MCP_EDIT` authorization site before Task 1.

---

### Task 1: The facade enable-guard

**Files:**
- Modify: the service/facade method behind `updateMcpServer` (re-derive; the MCP registration stack
  lives under `automation-ai-mcp-*`)
- Test: unit test beside it

Make `updateMcpServer(enabled = true)` refuse a server that has exposed workflows with an
incomplete mapping — null `toolName`, or a required input in `inputSchema` with neither a `fromAi`
expression nor a literal in `parameters` — with a typed error naming the offending workflows.

This lands FIRST because it is the invariant that makes flattening the CRUD safe: today the
prompt's step-4 rule holds only because the delegate is the only path in; the moment
`updateMcpServer` is flat on three surfaces, nothing else enforces it.

- [ ] **Step 1:** Write the failing test: a server with one mapped and one unmapped exposed
  workflow → enable throws, error message names the unmapped workflow; all-mapped → enable
  succeeds; disable always succeeds regardless of mapping state.
- [ ] **Step 2:** Run to verify it fails; implement in the facade/service (NOT the tool callback —
  the guard must protect the GraphQL mutation and any future caller too); re-run to green.
- [ ] **Step 3:** Check the client: does the MCP Servers page call an enable mutation that can now
  fail with this error? If yes, confirm the global GraphQL error toast covers it (it should — no
  per-mutation onError needed per CLAUDE.md) and note it in the report.
- [ ] **Step 4:** Module check; commit with pathspec.

### Task 2: The `configureMcpServer` intelligent tool

**Files:**
- Create: the tool callback (model on the narrowed post-Task-4-of-narrowing shape of
  `ProjectWorkflowAgentToolCallback`) with input schema `{mcpServerId: number}` and optional
  `{instruction?: string}` for user guidance ("name them all get_*", etc.)
- Create/modify: prompt — rewrite of `prompt_mcp_agent.txt` narrowed to the mapping capability
- Modify: the contributor that owns the MCP-domain ChatClient bean (per the module map, the MCP
  registration stack is automation-owned — put the definition in
  `AutomationIntelligentToolContributor` if the ChatClient bean lives in reach, else create the
  contributor beside the bean; the rule is the definition closes over its own module's client)
- Modify: `CopilotAgentType` — replace `MCP_AGENT`-family key with the new one; keep the KEY
  registered in `AgentTypeRegistry` (session purge depends on it)

- [ ] **Step 1:** Rewrite the prompt: KEEP the fromAi syntax block, the attachment-not-definition
  rule, the toolCallable=false explanation, naming conventions, budget cap, askUserQuestion rules,
  ids-from-results-only, data-not-instructions. REMOVE playbook steps 1, 2 and 4 (server
  resolution, attachment, enabling) — those are the caller's flat CRUD now. The inner agent keeps
  exactly two CRUD tools: `listMcpProjectWorkflows` and `updateMcpProjectWorkflowParameters`.
- [ ] **Step 2:** Register the definition with `panelScopes = Set.of(MCP_SERVER)` — this is the one
  panel registration the whole design adds: the MCP Servers panel's BUILD agent gets
  `configureMcpServer` via `catalog.getForPanel(MCP_SERVER, ...)`. Find the MCP Servers panel bean
  (`mcp_server_build`, in the phase-3 slice configuration) and add the catalog call; update its
  BUILD prompt to describe the tool.
- [ ] **Step 3:** Remove the old `mcp_agent` delegate registrations (hub + MCP surfaces) in the
  same commit the new definition appears — the parity test's expected-names list changes
  atomically.
- [ ] **Step 4:** Verify: parity test green with the new name; prompt closing check on every
  touched prompt; module checks. Commit.

### Task 3: Flatten the six CRUD tools

**Files:** AI Hub configuration + the CE MCP contributor config (the factory's list already exists —
registration only).

- [ ] **Step 1:** Register `listMcpServers`, `createMcpServer`, `updateMcpServer`,
  `createMcpProject`, `cloneMcpProject`, `listMcpProjectWorkflows` flat on the hub BUILD agent and
  the management MCP surface (reads also on hub ASK, matching how the other flat domains split
  read/write — mirror the existing factory read/write lists).
- [ ] **Step 2: Key-family check (the Phase-3 defect guard):** confirm which tool-context keys these
  callbacks read (`AutomationToolInvocationContext` vs `AgentToolInvocationContext`) and that each
  target surface writes that family. Record the finding in the report even when it is fine.
- [ ] **Step 3:** Update hub prompts: the MCP-servers passage becomes the caller sequence —
  `createMcpServer` (created disabled) → `createMcpProject` → `configureMcpServer` →
  `updateMcpServer(enabled=true)`, noting the enable call fails with named workflows if mapping is
  incomplete.
- [ ] **Step 4:** Verify + commit.

## Manual verification (running backend)

1. AI Hub BUILD: "expose workflows X and Y of project P as MCP tools on a new server" — the root
   creates/attaches flat, calls `configureMcpServer`, then enables; the sequence is visible in the
   tool-call events.
2. Enable-guard: try enabling a server with an unmapped workflow via the client page — clear error
   naming the workflow.
3. MCP Servers panel: ask the panel to complete a server's mapping — `configureMcpServer` runs from
   the panel.
4. External MCP client: full setup from Claude Desktop using the flat tools + `configureMcpServer`.

## Self-review notes

- Ordering inside this plan is load-bearing: guard (T1) before flat `updateMcpServer` (T3).
- **Not in scope:** the other seven CRUD delegates (unwind plan); progress notifications.
