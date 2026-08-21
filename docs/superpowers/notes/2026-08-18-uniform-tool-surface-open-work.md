# Open work — handoff for a fresh session

**Branch:** `claude/copilot-automation-listings-de1455` in worktree
`/Volumes/Data/bytechef/bytechef/.claude/worktrees/gracious-lewin-f68244`
**State at writing:** 55 commits ahead of `0_732`, **88 behind**, merge-base still on `0_732`
(so a plain rebase is valid — re-check before merging).

Six plans from `docs/superpowers/specs/2026-08-17-uniform-tool-surface-design.md` are complete.
Everything below is what those plans found and deliberately did not fix, plus one capability they removed.

Every item is stated with its evidence and a verification command. Items marked **CONFIRMED** were
re-verified against the tree on 2026-08-18. Items marked **VERIFY FIRST** were found earlier in the
session and may have been overtaken — check before writing code.

---

## A. Subagent question support

### A1. The ask capability is deleted, not dormant — CONFIRMED

Removed in `42203a8c0b3` ("Remove the unreachable subagent interactive-question stack"), 15 classes
and tests. The removal was correct: the gate was a compile-time-empty
`ASK_CAPABLE_AGENT_TYPE_KEYS = Set.of()`, and `SubAgentToolCallback` had zero production
constructions once the four CRUD delegates dissolved.

**What it did, precisely** — the phrasing matters, because it is not pause-and-resume:
the specialist calls `askUserQuestion`, **stops**, and is **re-delegated** with the answer. It
re-runs from the top, replaying its last 10 session events (`MAX_EVENTS`, a read filter). The
in-flight reasoning of the interrupted run is lost.

**Restoring it is not a revert.** Three separable pieces:
1. Recover the stack from `42203a8c0b3` (`SubAgentAskRelay`, `AskOutcome`, `SubagentAskChannel`,
   `SubagentAskChannelRelay`, `SubagentAskUserQuestionToolCallback`, `SubAgentAskToolContributor`).
2. Wire the relay into the **nine intelligent delegate callbacks** — these NEVER had it; the
   capability only ever served the four CRUD delegates. This is new work, and the largest part.
3. Repopulate `ASK_CAPABLE_AGENT_TYPE_KEYS` (`AiHubConfiguration#wrapDelegate`).

**Design constraints to carry in, all documented in CLAUDE.md:**
- The tool must return a **stop instruction**, never the payload — returning the payload makes the
  specialist read its own question back as the answer and invent the user's decision.
- `SubAgentAskRelay.runWithChannel` must return `AskOutcome(result, pendingQuestion)`; never expose a
  post-call `pending()` read. Delegate `ToolCallback`s are singletons serving concurrent
  delegations — any stash outliving the binding surfaces one user's question in another's chat.
- Known gaps, previously accepted: one question per delegation; nothing *enforces* that the
  specialist stops after asking (prompt guidance only); `truncateMessagesFrom` does not rewind
  specialist memory.

**Value check before building:** this buys *semantic* clarification ("did you mean #eng or a DM?").
It does **not** improve the connection flow — see A3.

### A2. MCP cannot render a question, and cannot raise one — CONFIRMED

`mcp-guidance-and-questions` Task 3 Steps 3–4, blocked as Ruling F. Two independent reasons the MCP
surface is structurally incapable:
1. The 1-arg `createMcpAgentToolCallback(ChatClient)` overload MCP contributors use passed `null`
   for the ask relay.
2. The specialist `ChatClient`s MCP injects never carry `askUserQuestion` — it is attached
   per-request by `SubAgentGuardrailedChatClient`, which wraps only on the hub surface.

**The blocker is a module cycle:** `SubAgentAskRelay`'s only implementation is EE `ai-hub-service`;
the callback lives in CE `automation-ai-tool`. CE cannot depend on EE. Needs a bean-based SPI seam —
absent-bean-means-off, the idiom the guardrails advisor and `OverrideChatClientResolver` use.

The plain-text renderer for this (`SubAgentQuestionRenderer`, `SubAgentQuestionFormatter`, tests) was
written and then deleted with the stack. Recover from `846f114` — it is a `git show`, not a no-op.

**Also pending:** `ManagementMcpServerConfiguration`'s instructions text had its
numbered-options question sentence deliberately dropped, because stating it today would be false.
Restore it *scoped to the delegates that can actually ask* once A1/A2 land.

### A3. The connection selector — NOT broken, do not "fix" it — CONFIRMED

Verified across all three surfaces. The builder subagent has never had a picker and must not get one:

| Surface | Pickers |
|---|---|
| AI Hub main agent | `SelectConnectionToolCallback` + `CreateConnectionToolCallback` pinned, `AiHubConfiguration:511-512` |
| Copilot panel agents (ask + build) | those two + `ListConnectionsForComponentToolCallback`, `CopilotConfiguration#interactivePickerToolCallbacks:414` |
| `buildWorkflow` subagent | **none, by design** — `CopilotConfiguration:874` |

The subagent's own builder carries the rationale as a comment: *"Not the interactive
askUserQuestion/select picker — a one-shot subagent can't ask + resume."* It gets
`LookupComponentPropertyOptionsToolCallback` instead — fetch real values, choose one itself.

The seam is clean and should stay: **subagent authors the requirement, parent resolves the
instance.** The definition's `connections` block declares only `componentName`/`componentVersion`;
no connection id ever enters it. `ProjectWorkflowTools.saveWorkflowTestConnection` states the
handoff in its own description ("call AFTER the user selects a connection").

---

## B. Confirmed bugs

### B1. `searchTask` does not exist — CONFIRMED
Embedded prompts name `searchTask(query, type)`; the registered tool is `searchTasks`
(`TaskTools.java:337`). Files: `prompt_workflow_editor_embedded_ask.txt:8`,
`prompt_workflow_editor_embedded_build.txt:18,95,132`.
```
grep -rn 'searchTask(' server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/resources
```

### B2. `getClusterElementInstructions` does not exist — CONFIRMED
Prompts name the singular; the registered tool is `getClusterElementsInstructions`
(`WorkflowInstructionTools.java:68`, and `workflowInstructionTools` IS on those agents).
Files: `prompt_workflow_editor_build.txt:37`, `prompt_converter_build.txt:50`,
`prompt_workflow_editor_embedded_build.txt:36`.

### B3. `prompt_skills_build.txt` names three unregistered tools — CONFIRMED
The prompt names `searchActions`, `getProperties`, `getActionDefinition` — all three live in
`ComponentTools`, which is **not** on the `authorSkill` BUILD bean
(`CopilotConfiguration:1071-1073` registers `skillsTools, readProjectTools,
readProjectWorkflowTools, workflowValidatorTools, workflowInstructionTools`).
Fix is a judgement call: add `ComponentTools` to the bean, or strike the three from the prompt.

### B4. `createContextStoreSource` bypasses its own guarded facade — CONFIRMED
`CreateContextStoreSourceToolCallback.java:29` (javadoc) and `:45` (the LLM-visible DESCRIPTION)
both claim "Workspace admin role required".

The gate DOES exist — `ContextStoreSourceFacadeImpl:70` carries
`@PreAuthorize(hasAuthority(ADMIN))` on `createContextStoreSource`, one of 7 guards in that impl, and
`ContextStoreSourceGraphQlController:86` goes through it. But the callback calls the **shared**
`WorkspaceContextStoreSourceFacade.create` directly (`:176`), which is correctly unguarded per the
API-facade/shared-facade convention. So the claim is true of the GraphQL path and false of the tool
path.

**Fix: point the callback at the guarded API facade** — swap `WorkspaceContextStoreSourceFacade` for
`ContextStoreSourceFacade` in the constructor, call `createContextStoreSource(...)`. No new
annotation, no new policy; the description becomes true. Viable because this tool is catalog-demoted
and catalog tools are security-context-rehydration-wrapped by `ToolSearchAdvisorConfiguration`,
which exists so `@PreAuthorize` facades work from tools.

**Before landing:** confirm the management MCP surface's rehydrated principal carries the ADMIN
authority — the rehydration wrapping is a property of the CATALOG path, and the MCP wrappers forward
workspace id but never `userId` (see B5). If it does not, this turns a working MCP call into an
authorization failure.

### B5. MCP wrappers never forward `userId` — CONFIRMED (pre-existing, carried forward)
`WorkspaceScopedSubAgentToolCallback` / `WorkspaceScopedFlatToolCallback` resolve and forward the
workspace id under both key families but never the user id. Tools reading a user id get nothing on
the management MCP surface. Scope the blast radius before fixing.

---

## C. Claims to re-verify before acting

### C1. `getTaskDispatcherInstructions` vs `getTaskDispatcherBuildInstructions` — VERIFY FIRST
Both methods exist: `TaskDispatcherTools.java:297` (plain) and `TaskTools.java:238` (Build variant).
So a prompt naming the plain one is only wrong if `TaskDispatcherTools` is absent from the agent that
loads it. **Check per-agent** — this may not be a bug at all.

### C2. `simulateWorkflow` "unregistered" — VERIFY FIRST, likely stale
`SimulationTools.java:46` defines it and `simulationTools` IS in the `buildWorkflow` subagent's
`defaultTools(...)`. The original finding probably concerned a different agent. Re-derive before
touching.

---

## D. Product calls (not engineering decisions)

### D1. `buildIntegrationWorkflow` lifecycle creep
It still carries the lifecycle surface `buildWorkflow` shed during narrowing. Removing it has **no
substitute surface** in the embedded EE tree, so narrowing it would remove capability rather than
relocate it. Needs a product decision on whether embedded gets the substitute surface first.

---

## E. Test and guard gaps

### E1. Nothing asserts a definition's `name()` matches the name its own `ToolCallback` advertises
This is the duplication that made the rename plan touch 44 files instead of 5: each tool name lives
in **two** places — `SimpleIntelligentToolDefinition("buildWorkflow", ...)` in the contributor AND a
hardcoded default in the callback's short constructor (e.g. `ProjectWorkflowAgentToolCallback:81`).
A future divergence is invisible to `IntelligentToolSurfaceParityTest`. Add the assertion there.

### E2. No integration test has ever executed in this work
`ai-hub-service:testIntegration` and `ai-mcp-server:testIntegration` fail at **context load** with
"Could not find a valid Docker environment" — Testcontainers cannot reach the socket from the
sandboxed test JVM, though `docker info` exits 0 on the host. No assertion ever failed. Resolve the
sandbox/Docker path, then run them.

### E3. Nothing has run against a live backend
True of all six plans, including the catalog they build on. The batched visual pass was deferred and
never happened.

---

## F. Merge-back

Gated on explicit approval. Procedure (0_732 keeps linear history — never a merge commit):
```
git -C <worktree> rev-list --count 0_732..HEAD     # was 55
git -C <worktree> rev-list --count HEAD..0_732     # was 88 and growing
git -C <worktree> merge-base --is-ancestor $(git merge-base HEAD 0_732) 0_732
```
If that last check passes, rebase normally. If it fails, `0_732` was rewritten — use
`git rebase --onto 0_732 <old-branch-point>`, never a plain rebase.

Then **from the main checkout**: `git merge --ff-only claude/copilot-automation-listings-de1455`.
Verify by **content diff** against the worktree (empty `git diff`), not by ancestry.

After any rebase, both of these — a clean merge is not a correct one:
```
./gradlew compileJava compileTestJava --continue
cd client && npm run check
```
Known pre-existing client failures, NOT from this branch (both reproduce on the base commit):
`useApplicationInfoStore.test.ts` typecheck error, and a prettier failure on `AiHubChatsSidebar.tsx`.

---

## Suggested order

1. **B1–B3** — small, independent, no design needed.
2. **B4** — a facade swap; sequence with B5, confirm the MCP principal carries ADMIN first.
2. **E1** — one assertion, prevents the class of bug B1–B3 belong to.
3. **A2's SPI seam** — unblocks MCP question rendering AND is a prerequisite for A1's MCP coverage.
   Recover the formatter from `846f114` rather than rewriting.
4. **A1** — the largest item; wiring the nine intelligent delegates is new work, not a revert.
5. **C1, C2** — re-derive; may evaporate.
6. **D1** — needs the product call before any code.
7. **E2/E3, F** — verification and landing.

## Verification discipline (carried from the session)

- Gradle/npm: redirect to a log file, check `$?` on its own line, then
  `grep '^> Task .* FAILED'`. Never judge a piped `tail`, and never a wrapper's exit code.
- `ObjectProvider.ifAvailable()` swallows missing beans — trust the parity test, not reading, for
  every registration move.
- Commits: fresh only, never amend. `git commit -m "732 <description>" -- <paths>` with the message
  flag BEFORE the pathspec. Client: `"732 client - <description>"`.
- IDE diagnostics went stale repeatedly this session; every reported unresolved type was disproved by
  a clean `compileJava compileTestJava --continue`. Trust the compiler.
