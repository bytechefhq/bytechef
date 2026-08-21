# Code workflows & custom components

Draft/publish lifecycle, the polyglot perform context, and declared connections.

Extracted from `CLAUDE.md` to keep that file within its size budget;
read this before working in the areas below.

### Editor draft/publish (custom components & code workflows, 2026-08-05)

Published artifacts are immutable via the editor paths; at most one mutable draft exists at a time.
Spec: `docs/superpowers/specs/2026-08-05-draft-publish-editors-design.md`.

- **Custom components**: `custom_component.status` (INT ordinal `Status {DRAFT, PUBLISHED}`, pinned)
  + `published_date`. Editor save updates a DRAFT in place (componentVersion re-read from the
  compiled definition); saving a PUBLISHED row spawns a new DRAFT row (the source-declared version must be
  strictly above the max; typed errors VERSION_NOT_BUMPED / DRAFT_ALREADY_EXISTS /
  VERSION_ALREADY_EXISTS). `publishCustomComponent` (facade + GraphQL mutation + agent/MCP tool)
  flips the draft. The handler registry filters `status == PUBLISHED` on BOTH list and fetch paths.
  One-draft-per-name is facade-enforced only — a concurrent-save race can create two drafts, after
  which `findByNameAndStatus` throws until one is deleted.
- **Code workflows** (automation + embedded mirror): editor save reconciles a mutable draft
  `code_workflow_container` in place via `CodeWorkflowContainerFacade.update`/7-arg `create`
  (returns `CodeWorkflowReconciliation`); after a publish, the next save mints a new container
  adopting the draft version's duplicated workflows through the ProjectWorkflow-uuid chain, so
  workflowIds (and test configurations) survive. Publish happens ONLY via the project/integration
  header publish. Editor-path saves never call `publishProject`/`publishIntegration`.
- **Upload/deploy paths publish immediately, by design** (CLI/REST deploy, embedded bridge): custom
  component uploads write PUBLISHED rows (rejected if a draft owns the version); project/integration
  deploys keep `deployInto`'s deploy-and-publish shape.
- **Bridge exclusion**: `__EMBEDDED_AUTOMATION__` catalog projects are filtered from
  `getCodeWorkflowProjects` and `updateCodeWorkflowSource` rejects them
  (EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE) — editor drafts would poison the bridge's one-deploy-back
  uuid carry-forward.
- **File-storage gotcha**: every `storeFile` writes a NEW physical blob even under an identical
  logical filename (`generateFilename=true`) — replaced-blob deletion must compare FileEntry URLs,
  never names.
- **Management MCP**: read viewers (`CodeCustomComponentViewerMcpContributorConfiguration`) plus
  flat WRITE tools (`CodeCustomComponentWriteMcpContributorConfiguration`: create/update/publish/
  delete custom component, create/update code workflow) — MCP clients author code directly,
  draft-safe; `publishProject` comes from the core management tool set.

### Perform context (custom components & code workflows, 2026-08-06)

- **Shared polyglot module**: CE `platform-component-polyglot` holds the strict sandbox
  (`PolyglotSandbox.newContext` — pinned restrictions with ONE carve-out: `allowCreateThread(true)`
  iff ruby is permitted, because TruffleRuby backs host-side hash iteration with fiber-based
  Enumerators), guest↔host marshalling (`PolyglotValues`), and the `context.component` proxy chain
  (`ContextProxyObject`/`ComponentProxyObject`/`ActionProxyObject` over the `ComponentActionInvoker`
  + `ComponentCatalog` seams). The script component, custom-component loader, and both code workflow
  loaders all consume it.
- **`SandboxPolicy.CONSTRAINED`** is applied to a perform context whose permitted languages are ALL
  in `PolyglotSandbox.CONSTRAINED_LANGUAGE_IDS` (`js`, `python` — which is every language the Script
  component actually registers; java/R/ruby are commented out). One language outside the set makes
  the whole context TRUSTED, because GraalVM applies one policy per context. Ceilings come from
  `bytechef.script.sandbox.*` (`max-cpu-time` 5m, `max-heap-memory` 512MB; empty lifts a ceiling,
  `enabled=false` is the kill switch back to TRUSTED). This buys resource-exhaustion protection — a
  `while(true){}` used to pin a worker thread indefinitely — NOT an isolated heap; that is
  `UNTRUSTED`, which needs polyglot isolate artifacts and cannot host js and python in one JVM
  (one isolate native library per process).
- **`PolyglotSandbox` owns its engines**, keyed by policy + permitted languages, because GraalVM
  requires a context and its engine to carry the SAME `SandboxPolicy`. Callers no longer pass one
  (the old per-loader `performEngine` fields are gone); every context it builds is a strict perform
  context, so the interop-cache clash that motivated separate engines — mixing strict and permissive
  contexts on ONE engine — cannot arise. Definition loading keeps its own permissive engine.
- **Never map guest aggregates with `Value.as(Map.class)` / `as(List.class)` / `as(TypeLiteral)` on a
  perform path.** Those are host object mappings of mutable target types, which EVERY policy above
  TRUSTED rejects — the failure is a `ClassCastException: Unsupported target type`, not a policy
  error, so it reads like a conversion bug. Walk the value instead: `PolyglotValues.copyToJavaValue`
  does, and deliberately checks hash entries before members (a python dict reports both; its contents
  are the hash entries) and keeps executable values callable before members (a guest function reports
  both; walking its members silently yields an empty map where a caller expects a `perform`).
- **Espresso boots ONE context per JVM** (`ESPRESSO-SINGLE-CONTEXT`). A second — concurrent or
  sequential, any options, any engine — dies in guest `System.initPhase1` with `Object
  'Lsun/nio/cs/UTF_8;' ... does not have the expected shape`, and closing the first afterwards can
  SIGABRT the JVM. Since `loadJava` reads the definition in one context and closes it, and perform
  opens another, **java code workflow tasks and custom component actions cannot currently perform**;
  the three tests that covered it are `@Disabled`. Espresso itself works — the full production load
  path succeeds as the first context, on darwin-aarch64 included, so the older
  `ProjectHandlerPolyglotEngine` comment claiming the platform "cannot boot" Espresso is wrong, as is
  `assumeEspressoAvailable()`'s skip message. Note that guard spends the JVM's one context on a
  throwaway probe, so it makes the real load fail rather than detecting anything. The `CLASS_LOADER`
  java loader is unaffected and passing.
- **Script custom components**: `perform(inputParameters, connectionParameters, context)` where
  context = `{http, log}` ONLY — deliberately no component invocation. HTTP crosses the
  `HostContextBridge` as JSON.
- **Code workflow tasks**: `perform(context)` with
  `context.component.<componentName>.<actionName>(input, connectionName)` + `context.log`.
  Connection-by-name resolves from the task's wired connections first, then
  `CodeWorkflowConnectionResolver` against the connection store; dispatch is
  `ActionDefinitionService.executePerformForPolyglot` via `CodeWorkflowTaskContext` (which also
  implements `ComponentActionInvoker`). SDK: `TaskDefinition.PerformFunction.apply(TaskContext)`
  default-delegates to the zero-arg `apply()`, so legacy performs (JS `function () {}`, py/rb
  splats) keep working — engines always pass exactly one argument. Java classloader path hands the
  host `CodeWorkflowTaskContext` straight to user code; the Espresso path crosses a
  `CodeWorkflowHostBridge` (host, per loader module) ↔ `GuestTaskContext` (guest, SDK module
  `sdks/backend/java/workflow-guest-bridge`, on the guest classpath via the loaders' `guestSdk`
  configuration) as JSON, binding name `byteChefCodeWorkflowHostBridge`.
- Spec: `docs/superpowers/specs/2026-08-05-code-perform-context-design.md`.

### Declared connections (code workflows & custom components, 2026-08-06)

- **Code workflow tasks** declare connections in the SDK (`WorkflowDsl.task(...).connections(
  connection(componentName[, componentVersion], name))`) or script contract (task member
  `connections: [{componentName, componentVersion?, name}]`); both loader engines parse them and
  `CodeWorkflowContainerFacadeImpl` emits them into the task's **`extensions.connections` map**,
  keyed by the declared name (unpinned versions resolve to latest at save time). They then ride
  the platform's EXISTING connection machinery — do not invent a parallel path:
  `CodeWorkflowComponentConnectionFactory` (mirrors `ScriptComponentConnectionFactory`, resolves
  on the `codeWorkflow` component) → `ComponentConnectionFacade` → `WorkflowTask.connections` in
  the REST model → editor/test-configuration/deployment wiring → the perform action's
  `componentConnections` map, which `CodeWorkflowTaskContext` looks the name up in. There is NO
  by-name connection-store lookup (an earlier self-wiring resolver was removed): a name that is
  not wired fails exactly like the script component's. `CodeWorkflowTaskContext` reads
  `environmentId` from `ActionContextAware` and forwards it into `executePerformForPolyglot`.
  `TaskContext.connection(name)` returns a wired connection's parameters (all four
  execution paths; Espresso crosses it as JSON) for tasks that build requests themselves.
  Sources may declare `connections` as a LIST of `{componentName, componentVersion?, name}` or as a MAP
  keyed by connection name — both parse (the map mirrors the emitted definition's shape).
  Client: the code workflow source editor's header carries a **Test Configuration** button
  (`CodeWorkflowSourceEditor` → optional props from `WorkflowEditorLayout`, which owns the
  `WorkflowTestConfigurationDialog`), disabled via the same `testConfigurationDisabled` derivation
  the visual editor uses.
- **Custom components** declare connections via the single-file contract's `connection` member
  (`{baseUri?, authorizations?: [{type, authorizationUrl?, tokenUrl?, refreshUrl?, scopes?,
  apply?, properties?}], properties?}`), materialized host-side into a real `ConnectionDefinition`
  — all authorization types including OAuth2 (platform runs the flow). URL seams and `apply`
  accept guest functions, wrapped via the perform path's re-eval pattern; `apply` runs per
  outbound request (opt-in cost). Espresso `describeComponent` serializes a connection's static
  shape by invoking seams with null args — a dynamic lambda throws and the connection is reported
  `unsupported` (functions can't cross the guest boundary). Connection-level `properties` attach
  to each authorization (or an implicit CUSTOM authorization when none declared).
- Spec: `docs/superpowers/specs/2026-08-06-code-artifact-connections-design.md`.
