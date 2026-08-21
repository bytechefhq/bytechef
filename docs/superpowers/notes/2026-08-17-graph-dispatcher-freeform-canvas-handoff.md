# Handoff — Graph dispatcher free-form canvas (ByteChef, branch `0_732`)

**Kickoff prompt for the new session:**

> Read `docs/superpowers/notes/2026-08-17-graph-dispatcher-freeform-canvas-handoff.md`, then the spec and both
> plans it references. Execute Plan A (server) first with `superpowers:subagent-driven-development`, task by task;
> when Plan A is green, continue with Plan B (client). Branch `0_732`, fresh commits only.

**Repo:** branch `0_732`; the user commits in parallel on this branch — never amend, never rebase it, always fresh commits; linear history, no merge commits.

## What was decided (this session, 2026-08-17)

The user wants the `graph/v1` task dispatcher rendered as a **free-form canvas inside a box**: nodes placed and connected by dragging, the box auto-sizes, the surrounding flow reflows. Analysis, design and plans are DONE and committed — the next session **executes**.

Key decisions (user, all recorded in the spec's decisions log):
- Free-form graph inside the box; the current lane rendering is "awkward" and is being replaced.
- **No task list per node** — a graph node IS one task.
- **DSL changes** (`graph/v1` is unreleased — not on `master`, not in `v1.1.5` — so the shape is replaced in place, no migration): `nodes` = plain task list; `transitions: [{from, to, condition?}]`; `to` may be an expression (dynamic/LLM routing); evaluation = conditional edges in declared order, then the unconditional default; routers dropped.
- Still executed by **Atlas** via the existing `task-dispatchers/graph` module; nothing under `server/libs/atlas/` changes.

## Artifacts (read these first — do not duplicate their content)

- Spec: `docs/superpowers/specs/2026-08-17-graph-dispatcher-freeform-canvas-design.md` (commit `9972f13f279`)
- **Plan A (server, do first):** `docs/superpowers/plans/2026-08-17-graph-dispatcher-edge-list-dsl.md` — 7 tasks in `server/libs/modules/task-dispatchers/graph` + one arm of `WorkflowNodeOutputFacadeImpl` + docs.
- **Plan B (client, depends on A):** `docs/superpowers/plans/2026-08-17-graph-dispatcher-freeform-canvas.md` — 10 tasks in `client/src/pages/platform/workflow-editor`.
- Both plans committed in `0536f736fbd`.
- Superseded (for context only): `docs/superpowers/specs/2026-08-02-graph-task-dispatcher-design.md` (engine half still valid), `2026-08-06-graph-dispatcher-topological-lane-ordering-design.md` (fully superseded).

## State of the tree

- Nothing implemented yet. Pre-existing unrelated modified files in the working tree (CONTRIBUTING.md, `FunctionSignature.extension.ts`, gradle/config/security files, an untracked test) belong to the user — do not stage them.
- Client dev server may be running on 5173; the API server on 9555 was NOT running this session (start with `./gradlew -p server/apps/server-app bootRun` after `docker compose -f server/docker-compose.dev.infra.yml up -d` when a manual checkpoint needs it).

## Facts learned that the plans rely on (verified in code this session)

- Container "boxes" in the editor are NOT nodes today: two 2px ghost bars + smoothstep edges; ELK compound frames are dropped before nodes are emitted; no `parentId`/`extent` anywhere on the main canvas; `nodesConnectable={false}` and every `<Handle isConnectable={false}>` — Plan B adds the app's first `onConnect`.
- Drag exists (global lock, `nodesLocked` default true) but positions are re-normalized by `applySavedPositions` (last layout pass); `saveWorkflowNodesPosition.updateTaskPositions` does not walk `parameters.nodes` yet.
- Server: only `WorkflowNodeOutputFacadeImpl` (~line 314) special-cases `"nodes"` outside the graph module; no copilot/MCP prompt hardcodes the graph shape (definitions are server-driven). `DeferredEvaluationParameterKeys.register(GRAPH + "/", NODES)` lives in `GraphTaskDispatcherConfiguration` (Plan A adds `TRANSITIONS`).
- `useLayout.getTasksStructuralFingerprint` currently hashes graph `next` expressions (`collectGraphNextExpressions`) — Plan B replaces it with transitions + member positions.
- ELK default hierarchy handling is SEPARATE_CHILDREN; `org.eclipse.elk.fixed` exists in the bundle but the chosen design does not use it (frame is a sized leaf so both engines behave identically).

## Suggested skills for the next session

- `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` — to execute Plan A, then Plan B, task by task with review checkpoints.
- `superpowers:using-git-worktrees` — if isolating the implementation from the user's parallel commits (verify + cherry-pick back; see memory note on worktree stranding).
- `superpowers:test-driven-development` — every task in both plans is written red→green.
- `superpowers:verification-before-completion` — Gradle results must be checked via a log file and `$?` (never through a pipe); client via `cd client && npm run check`.
- `superpowers:requesting-code-review` after each plan lands.

## Conventions to keep in mind (from CLAUDE.md, enforced)

- Commit messages: server `732 <description>`, client `732 client - <description>`; only stage files touched by the task.
- Java: blank line before control statements and after variable modification; test names camelCase, no underscores; no `TODO` comments; `./gradlew spotlessApply` before commit.
- Client: ESLint `sort-keys`, interface names end `I`/`Props`, refs end `Ref`, `twMerge` not `cn`, Lucide `*Icon` imports, `vi.hoisted` for mock refs; hook ordering rule.
