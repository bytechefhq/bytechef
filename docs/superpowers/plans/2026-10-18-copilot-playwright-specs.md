# Copilot Playwright Specs (#1570 scenarios) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Turn the #1570 live test scenarios (https://github.com/bytechefhq/bytechef/issues/1570#issuecomment-6242649809) into Playwright specs on `0_p_1570`.

**Architecture:** Two kinds of spec. Deterministic specs stub the Copilot's AG-UI stream (`POST /api/platform/internal/ai/chat/<agentId>`, `text/event-stream`) with `page.route` and a small event-script helper, so the client's real cards, pickers and round-trips are exercised without a model. `@live` specs drive the same flows, and the management MCP over Playwright's `request` context, against a real server and model; they skip unless `PLAYWRIGHT_LIVE=1`.

**Tech Stack:** `@playwright/test` 1.64, existing fixtures in `client/test/playwright/{fixtures,utils,pages}`.

**Spec:** the #1570 scenario comment above; design D13/D14 in `docs/superpowers/specs/2026-10-17-ai-hub-phase1-port-design.md` (on `0_p_ai_hub`).

## Global Constraints

- Branch `0_p_1570`, worktree `/Volumes/Data/bytechef/bytechef-worktrees/p1570`. Commit ticket `1570`, message `1570 client - <desc>`, no attribution lines.
- Client conventions: sort-keys, sorted import destructures, descriptive names, no `//` explanatory comments, no em dashes, no `waitForTimeout` in new code (wait on locators or responses).
- Deterministic specs must pass with no AI model configured. `@live` specs call `test.skip(!process.env.PLAYWRIGHT_LIVE, ...)` at describe level.
- Event payloads in the stub are copied from real server output (read the tool callbacks' result JSON and the AG-UI event classes under `spring-ai/spring-ag-ui`), not invented.

---

### Task 1: AG-UI stream stub helper

**Files:** Create `client/test/playwright/utils/agUiStreamStub.ts`; test it through Task 2's first spec.

**Interfaces:** `stubCopilotRun(page: Page, agentIdPattern: RegExp, turns: AgUiTurnI[]): Promise<{requests: AgUiRequestI[]}>` where each `AgUiTurnI` is an ordered list of events (`runStarted`, `textMessage(text)`, `toolCall(name, args, resultJson)`, `runFinished`, `runError(message)`), served one turn per request in order; `requests` records each request body (state, messages) so specs can assert what the client sent back. Event wire format exactly as the server emits it (`RUN_STARTED`, `TEXT_MESSAGE_START/CONTENT/END`, `TOOL_CALL_START/ARGS/END/RESULT`, `RUN_FINISHED`, `RUN_ERROR`, SSE `data:` lines).

- [ ] Write it; prove it with Task 2's first spec; commit `1570 client - Add an AG-UI stream stub for Copilot Playwright specs`.

### Task 2: Workflow editor Copilot pickers (scenarios 1-2)

**Files:** Create `client/test/playwright/tests/copilot/copilotPickers.spec.ts` (deterministic) using `projectTest`/`workflowTest` fixtures to open an empty workflow and the Copilot panel.

Tests (one per scenario bullet): select-connection card lists the connections from the `selectConnection` result, the turn ends (composer enabled, no spinner), picking one sends `User picked: <name> (ID: <id>)` as the next request's last message; create-connection card renders and its submit posts; select-property-option card lists options and the pick is sent with the value; ask-user-question card answer is sent as `User picked:` text; a picker followed by a newer message is dimmed and disabled; a connections list containing a negative id never shows that option; ASK mode request state carries `mode: 'ASK'` and the stubbed ASK turn's tools never include `selectConnection` (assert on the request the client sends and on the UI, not on server internals).

- [ ] Write, run `cd client && npx playwright test tests/copilot/copilotPickers.spec.ts`, commit `1570 client - Cover the Copilot's interactive pickers with Playwright specs`.

### Task 3: Delegate question rendering (scenario 4, client side)

**Files:** Create `client/test/playwright/tests/copilot/copilotDelegateQuestion.spec.ts`.

Tests: a `buildWorkflow` tool result whose payload is `{kind: 'ask-user-question', awaitingAnswer: true, questions: [...]}` renders the ask card (payload-kind fallback); answering sends one `User picked:` message; a `buildWorkflow` result that is prose starting with `{` renders as a normal tool result with no console warning.

- [ ] Write, run, commit `1570 client - Cover a delegated specialist's question in the Copilot`.

### Task 4: `@live` Copilot and MCP specs (scenarios 1, 3, 4, 5)

**Files:** Create `client/test/playwright/tests/copilot/copilotLive.spec.ts`, `client/test/playwright/tests/mcp/managementMcpLive.spec.ts`, `client/test/playwright/utils/mcpClient.ts` (JSON-RPC over the streamable HTTP endpoint `/api/management/{secretKey}/mcp`: `initialize`, `tools/list`, `tools/call`; secret key from `PLAYWRIGHT_MCP_SECRET_KEY`).

Tests: Copilot BUILD on a real model builds a Slack workflow and shows the select-connection card (two Slack connections created in `beforeAll` via the API); MCP `tools/list` has no `updateWorkflow` and `createProjectWorkflow`'s schema has `name`, not `definition`; `createProject` → `createProjectWorkflow` → `buildWorkflow` produces a workflow whose definition has a trigger and at least one task; an ambiguous `buildWorkflow` returns plain text containing a numbered option list and the re-invoke sentence, and calling again with the answer completes; `importWorkflow` with a small n8n JSON never returns a question; with `PLAYWRIGHT_COPILOT_DISABLED=1` set by the runner the MCP `tools/list` still works.

- [ ] Write; run once with `PLAYWRIGHT_LIVE=1` against a local server (record pass/fail per test in the report; a model-dependent failure is reported, not hidden by retries); commit `1570 client - Add live Copilot and management MCP Playwright specs`.

### Task 5: Runbook

**Files:** Modify `client/test/playwright/README.md`: how to run deterministic vs live, required env vars (`PLAYWRIGHT_LIVE`, `PLAYWRIGHT_MCP_SECRET_KEY`, server flags `bytechef.ai.copilot.enabled=true` and a model), and which #1570 scenario each spec covers.

- [ ] Write, commit `1570 docs - Explain how to run the Copilot Playwright specs`.
