# Declared Connections for Code Workflows and Custom Components — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `docs/superpowers/specs/2026-08-06-code-artifact-connections-design.md` in the
decided order **B → A → C**: (B) the environment-aware by-name connection resolver +
`environmentId` threading, (A) SDK/script-contract connection declarations for code workflow tasks
emitted into the generated definition, (C) the custom-component `connection` member with
static-data authorizations plus function-valued `authorizationUrl`/`tokenUrl`/`refreshUrl`/`apply`
seams. Each phase lands independently.

**Reference implementations to mirror:** `ScriptComponentActionInvoker` (connection map matching,
`environmentId` forwarding, per-call `ActionContext`), `ComponentHandlerPolyglotEngine` (re-eval
pattern + `performEngine` separation), `CodeWorkflowContainerFacadeImpl.toArrayNode` (definition
emission), `ComponentDsl.authorization(...)` + `platform-oauth2` (what the materialized
`ConnectionDefinition` must carry for the platform to run OAuth flows).

## Global Constraints

- CE modules (`server/libs/`, `sdks/backend/`) use the Apache 2.0 header; EE modules
  (`server/ee/libs/`) use the ByteChef Enterprise header + `@version ee`.
- Backward compatibility: sources without declarations/`connection` members load byte-identically
  (regression-pin with existing loader tests); zero-arg and declaration-less shapes stay valid.
- Java style rules per CLAUDE.md; `./gradlew spotlessApply` on touched modules before each commit;
  judge gradle runs by redirecting to a file and grepping `^> Task .* FAILED`.
- Commit message `0 <description>` with the standard Claude trailers.
- TDD per task: failing test first, verify fail, implement, verify green, commit.

---

## Phase B — runtime resolution fixes

### Task 1: Environment-aware `CodeWorkflowConnectionResolver` bean + `environmentId` threading

**Files:**
- Create: resolver impl in `server/ee/libs/modules/components/code-workflow/` (e.g.
  `.../task/StoreCodeWorkflowConnectionResolver.java`, `@Component @ConditionalOnEEVersion`)
- Modify: `CodeWorkflowTaskContext` (forward `environmentId` into `executePerformForPolyglot`
  instead of `null`; obtain it the way `ScriptComponentActionInvoker`/`JobContextAware` does —
  INVESTIGATE at implementation time which seam carries it into this executor path and mirror it)
- Modify: `build.gradle.kts` (+ the connection service module the lookup needs)
- Test: `StoreCodeWorkflowConnectionResolverTest` (mocked connection service), extend
  `CodeWorkflowTaskContextTest` (environmentId mock-verify)

**Behavior:**
- Resolve the current environment (thread-local `EnvironmentContext` idiom) and workspace scope;
  query connections by name; filter to connections whose component matches `componentName`; map to
  `ComponentConnection` (id, connectionVersion, authorizationType, parameters) — the exact shape
  `executePerformForPolyglot` consumes. Miss → `Optional.empty()` (the context's existing error
  path fires).
- INVESTIGATE: the concrete service surface for by-name lookup (`ConnectionService` /
  facade — pick the narrowest one already visible to EE component modules) and whether tenant
  context is already bound on the worker thread (it is for the script path — mirror it).

**Steps:**
- [ ] Failing tests (resolver happy path, wrong-component miss, environmentId forwarded).
- [ ] Verify fail. Implement. Module tests green.
- [ ] Commit: `0 Resolve code workflow connections by name from the store for the current environment`.

---

## Phase A — declared connections for code workflow tasks

### Task 2: SDK `ConnectionRequirement` + `WorkflowDsl.connections(...)`

**Files:** `sdks/backend/java/workflow-api` — `TaskDefinition` (new
`Optional<List<? extends ConnectionRequirement>> getConnections()` default empty), new
`ConnectionRequirement` interface (`getComponentName()`, `OptionalInt getComponentVersion()`,
`getName()`), `WorkflowDsl` (`connection(componentName, name)` +
`connection(componentName, componentVersion, name)` factories, `task(...).connections(...)`).
Tests in the SDK module.

**Steps:**
- [ ] Failing tests (DSL round-trip both arities; task without connections → empty).
- [ ] Implement; compile `project-api`/`integration-api` dependents. Green.
- [ ] Commit: `0 Add declared connection requirements to the workflow SDK task definition`.

### Task 3: Script-contract parsing + definition emission

**Files:**
- Modify: `ProjectHandlerPolyglotEngine` + `IntegrationHandlerPolyglotEngine` (`load(...)` reads an
  optional per-task `connections` list — `{componentName, componentVersion?, name}` maps — into
  the task definitions; Java paths surface the SDK declarations via `getConnections()`)
- Modify: `CodeWorkflowContainerFacadeImpl.toArrayNode` (emit `"connections": [...]` into the task
  JSON when declared — sibling of the `getParameters()` TODO, which stays untouched)
- Tests: loader engine tests per language (declared → parsed; absent → empty, regression pin);
  facade emission test.

**Steps:**
- [ ] Failing tests. Implement (automation, transpose embedded). Green.
- [ ] Commit: `0 Emit declared code workflow task connections into the generated definition`.

### Task 4: Deploy-time validation warning (self-wiring, no override)

**Files:** the code workflow deploy/publish path (INVESTIGATE:
`AutomationWorkflowProjectCodeWorkflowFacadeImpl` / `CodeWorkflowContainerFacade` seam) — on
deploy/publish, WARN-log (do not fail) any declared connection name with no store connection in
the target environment. No client work in this plan (introspection surfacing is a follow-up).

**Steps:**
- [ ] Failing test (mock store: missing name → warning recorded/logged; present → silent).
- [ ] Implement. Green.
- [ ] Commit: `0 Warn on deploy when a declared code workflow connection name is absent`.

---

## Phase C — custom component `connection` member

### Task 5: Parse + register the declarative connection (static data)

**Files:**
- Modify: `ComponentHandlerPolyglotEngine.load` (read optional `connection` member: `baseUri`,
  `authorizations` [{type, authorizationUrl, tokenUrl, refreshUrl, scopes}], `properties` — same
  property-map shape as action properties) → build a `ModifiableConnectionDefinition` via
  `ComponentDsl.connection(...)`/`authorization(...)` with constant seams; attach to the component
  definition. INVESTIGATE: how the custom-component registry exposes connection definitions
  (mirror whatever the Java classloader path already does — it supports connections today).
- Tests: engine test (declared → registered ConnectionDefinition with auth type/properties/
  baseUri); no-connection source regression pin.

**Steps:**
- [ ] Failing tests. Implement. Green.
- [ ] Commit: `0 Script custom components declare connections in the single-file contract`.

### Task 6: Function-valued seams (`authorizationUrl`/`tokenUrl`/`refreshUrl`/`apply`)

**Files:** `ComponentHandlerPolyglotEngine` — seam values that are guest-executable materialize as
host wrappers: re-eval `(languageId, script)` in a fresh strict-sandbox context (the existing
`performEngine`), navigate to `connection.authorizations[i].<seam>`, execute with
`copyToGuestValue(connectionParameters)`, validate shape (string for URL seams; map with optional
`headers`/`queryParameters` for `apply` mapped onto the DSL's `ApplyResponse`), error naming the
seam on wrong shape. String constants keep the constant path.

**Steps:**
- [ ] Failing tests: function tokenUrl computes from connectionParameters; apply decorates
      headers and overrides the type default; wrong-shape errors name the seam; constants still work.
- [ ] Implement. Green.
- [ ] Commit: `0 Custom component connections support guest function url and apply seams`.

### Task 7: Espresso describe path serializes static connections

**Files:** `GuestComponentBridge.describeComponent` (serialize a declared connection's static
shape instead of adding `"connection"` to `unsupported`; definitions carrying real lambdas keep
`unsupported`), `ComponentHandlerEspressoEngine` (rebuild the connection from the JSON). Tests
mirror the existing describe tests.

**Steps:**
- [ ] Failing tests. Implement. Green.
- [ ] Commit: `0 Espresso custom components describe static connection definitions`.

### Task 8: Templates, docs, prompts + full verification

- Starter template `connection` example (commented); `custom-components.mdx` +
  `code-workflows.mdx` contract updates (including the new `connections` task member and the
  self-wiring-by-name story); `prompt_custom_component_build.txt` +
  both code workflow build prompts; CLAUDE.md perform-context subsection extended.
- CreateEmpty facade tests as the template gate; then the full sweep: spotless, repo
  `compileJava compileTestJava --continue`, all touched module `check` tasks, spec cross-read →
  update the spec's implementation notes.
- [ ] Commit: `0 Teach templates, docs, and prompts the declared connection contracts`.
- [ ] Commit (if needed): `0 Declared connections: verification fixes`.

---

## Self-review notes

- Task 1 is the only piece the already-shipped perform-context contract is waiting on — land it
  first and the existing docs become true.
- Task ordering: 1 ∥ 2 are independent; 3 needs 2; 4 needs 3; 5 → 6 → 7 sequential; 8 last.
- Risk concentrations: Task 5's registry integration (custom-component connection exposure differs
  from built-ins — investigate the classloader Java path's existing behavior first), Task 6's
  `ApplyResponse` mapping, Task 1's environment/workspace context availability on the executor
  thread (mirror the script path's `JobContextAware`).
