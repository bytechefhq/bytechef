# Workflow execution simulation (dry-run validator)

**Date:** 2026-07-24
**Status:** Design — for review
**Author:** Ivica Cardic

## Motivation

Static rules and the advisory `WorkflowValidator` catch structural mistakes, but AI-built workflows
still ship subtle gaps that only surface at run time — a task referencing an output field that
doesn't exist, a missing required input, an expression that can't resolve, a mis-wired step. The
idea: **run the built definition without making any real external calls**, and let the engine's own
DAG execution reveal the gap at the exact failing task. The result (COMPLETED, or FAILED-at-task
with a concrete reason) feeds back to the builder — human or AI — to fix.

## Goal

- A **dry-run** of a workflow definition that executes the real Atlas DAG (expressions, data flow,
  task dispatchers) but **substitutes component I/O** so no API/DB/connection call is made.
- Returns structured **findings**: overall outcome + per-task failures with reasons.
- **Shared and surface-agnostic** (hard requirement, see below).
- No component code changes, no Mockito, no reflection, no code generation.

## Non-goals

- Full data-value fidelity (sample outputs are representative, not the real payload).
- Validating that chosen values are valid *dynamic options* — that is a separate, connected
  concern (see §Dynamic options).
- Replacing real test-run execution; this is a fast pre-flight, not a substitute for a real run.

## Hard requirement: shared, surface-agnostic

The simulator takes a **workflow definition in** and returns **findings out** — it must not know or
care who built it. It lives in the **platform / automation-workflow layer** (not in `ai-hub`), and
is invocable by every workflow-building surface:

- the in-editor copilot (`workflow_editor` agent),
- the AI Hub BUILD agent,
- the management MCP `workflow_editor` agent,
- the embedded workflow-editor copilot,
- and directly by the editor UI ("simulate" action).

Exposed as a facade + a `@Tool` (e.g. `simulateWorkflow`) that the building agents can call, and
optionally a REST/GraphQL entry for the editor. This mirrors the placement decisions from the
Prep A/B/C relocations: shared build-time capabilities belong in the shared layer, consumed by all
surfaces — never siloed in AI Hub.

## Mechanism

Run the definition through `JobSyncExecutor` (in-memory, synchronous) with a **`dryRun`** flag
threaded from the job into the component-action/trigger task handlers. At the component boundary
(`ActionDefinitionFacade` / the action task handler), `dryRun` **replaces `executePerform(...)`**
with a **simulated-output resolver** instead of invoking the component's `perform()`.

### Simulated-output resolver — the transport-agnostic backbone

For each action, resolve output **without invoking the component's I/O**, in this order:

1. the action's declared static output — `ActionDefinition.getOutputDefinition()` =
   `{ outputSchema, sampleOutput, placeholder }` — prefer `sampleOutput`, else `placeholder`, else
2. a value **synthesized from the output schema**, else
3. an empty object.

This is **transport-agnostic**: it works identically for HTTP, **JDBC**, AWS SDK, filesystem, SMTP,
brokers — because it never opens a socket, DB connection, or SDK client. It is the backbone; every
component is covered at least at this tier. Triggers resolve the same way (declared sample trigger
output).

The Atlas engine then runs the whole DAG on these substituted outputs — evaluating expressions,
resolving task inputs, running conditions/loops/branches — so **structural gaps fail at the exact
task** that hits them.

### Fidelity tiers (opt-in, above the backbone)

- **Input-derived output functions** (compute schema from `inputParameters`, touch no I/O) can be
  **run for real** against any context — full fidelity, genuinely zero calls.
- **API-probing output functions** (HTTP GET a sample to infer schema) can be run against a
  **simulation HTTP client** (the "mock like in tests" seam) — but only for **HTTP** components, and
  fidelity equals what the stub returns: empty stub → minimal schema; **recorded cassettes** →
  real shape. This is an **opt-in HTTP-only** layer, valuable for deep field-reference validation on
  the components you care about; it does not generalize to JDBC/SDK (each transport would need a
  bespoke stub — explicitly out of scope).
- **Raising fidelity for any node** without the HTTP trick is the declarative lever: add a static
  `sampleOutput` to the action (one DSL line, additive, no logic change).

## Node-type policy (open decisions)

- **Script / polyglot tasks (JS/Python/Ruby via GraalVM):** they run *real code*, not a component
  API call — a script may itself do HTTP. Decision: run pure-logic scripts (deterministic, exercise
  the data flow) vs. stub them. Default proposal: **run them**, and document that a script making an
  external call breaks the no-real-calls guarantee.
- **AI-agent / LLM cluster elements:** would make real model calls. Proposal: **stub** them with a
  representative sample output in dry-run.
- **Task dispatchers (condition/loop/branch/each/parallel):** run for real on substituted data —
  structure is exercised; the branch taken may differ from production (documented limitation).

## Dynamic options (explicitly out of scope here)

Dynamic `options` functions are **design-time**, not execution-time — `perform()` never invokes
them, so a dry-run never touches them. Validating that a property value is a *valid* option is a
**separate, connected** capability that needs the real option list (a live call or a recorded
cassette) — it cannot be done against an offline stub (an empty list would flag every value as
invalid). That concern is already best addressed at **build time** via `lookupPropertyOptions`,
which is now wired onto every workflow-building agent (interactive copilot, the copilot
workflow-editor subagent, AI Hub, and the management MCP `workflow_editor` agent). A post-build
connected re-validation could be a later, separate spec.

## Findings contract

`simulateWorkflow(definition) → SimulationResult` with:
- `outcome`: COMPLETED | FAILED
- `failedTaskName`, `failedTaskType`, `reason` (the engine's error) when FAILED
- a list of `warnings` (e.g. "action X had no declared output — used empty fallback; downstream
  field references were not validated") so the caller knows where fidelity was degraded — never a
  silent gap.

## Risks / limits

- **Structural validation is strong; data-value validation is partial** — sample outputs are
  representative, so value-dependent branches aren't faithfully exercised.
- **Actions with no declared output** degrade to placeholder/empty — surfaced as a warning.
- **Determinism:** the sync path must run reproducibly; no wall-clock/random dependence in the
  substitution.

## Rollout

Additive: a new dry-run flag + resolver at the component boundary, a shared simulate facade/tool,
and per-surface wiring. No schema change, no impact on real runs (flag defaults off).
