# Perform Context for Script Custom Components and Code Workflow Tasks — Design

Date: 2026-08-05
Status: Approved (design)

## Problem

Neither script custom components nor code workflow tasks receive anything at perform time:

- `ComponentHandlerPolyglotEngine.executePerform` calls the script action's `perform.apply(null)` —
  `inputParameters`, `connectionParameters`, and `ActionContext` are accepted and discarded
  (`PMD.UnusedFormalParameter`). The starter template's documented
  `perform: function (inputParameters, connectionParameters, context)` signature is unfulfilled.
- `CodeWorkflowTaskExecutor.executePerform` resolves the task's SDK `PerformFunction` and calls the
  zero-arg `apply()`. `CodeWorkflowPerformActionDefinition` has the full `MultipleConnectionsPerformFunction`
  seam (inputParameters, connection map, ActionContext) and forwards none of it.

Both surfaces are therefore unable to perform I/O or interact with the platform. Additionally, the
polyglot contexts for both engines are built with `Context.newBuilder().engine(engine).build()` — no
sandbox options at all — while the repo's reference sandbox (the script component's `PolyglotEngine`)
pins `HostAccess.NONE`, no IO, no host class lookup, etc.

## Reference model

`server/libs/modules/components/script/.../PolyglotEngine.java` is the only place a guest `perform`
receives a context today, and it is the model for this design:

- **Isolation**: `allowHostAccess(HostAccess.NONE)`, `allowHostClassLoading(false)`,
  `allowHostClassLookup(cn -> false)`, `allowNativeAccess(false)`, `allowCreateThread(false)`,
  `allowCreateProcess(false)`, `allowIO(IOAccess.NONE)`, `allowEnvironmentAccess(NONE)`,
  `allowPolyglotAccess(NONE)`.
- **Host/guest mapping**: `copyToGuestValue`/`copyToJavaValue` marshal values as proxies
  (`ProxyObject`, `ProxyArray`, `ProxyInstant`, …) — Java objects are never directly exposed.
- **Capability surface**: `ContextProxyObject` → `ComponentProxyObject` → `ActionProxyObject`
  (`ProxyExecutable`), letting the script call `context.component.<componentName>.<actionName>(input,
  connectionName?)`, dispatched through `ActionDefinitionService.executePerformForPolyglot`.

## Design

### A. Code workflow tasks — full component access (the script model)

Code workflow task `perform` becomes `perform(context)` where `context` is the script component's
proxy context: `context.component.<name>.<action>(inputMap, connectionName?)` can invoke any
component action, plus `context.log(level, message)`.

- **Extract the proxy machinery** from the script component into a shared CE module
  (`server/libs/platform/platform-component/platform-component-polyglot`): the three proxy classes,
  the `copyToGuestValue`/`copyToJavaValue`/`copyFromPolyglotContext` marshalling helpers, and the
  strict-sandbox `Context` builder. The script component's `PolyglotEngine` is refactored to consume
  it (behavior-identical); the code workflow path consumes the same classes.
- **Wiring**: the proxies need Spring collaborators (`ActionDefinitionService`,
  `ComponentDefinitionService`, an `ActionContext`/job-context factory). The loader engines are
  static and Spring-free, so `CodeWorkflowTaskExecutor` (a Spring `@Component`) constructs a
  `GuestContextFactory` and passes it through `ProjectHandlerLoader.loadProjectHandler(...)` /
  `IntegrationHandlerLoader` into the polyglot engines; `CodeWorkflowPerformActionDefinition` stops
  dropping its seam and forwards `inputParameters`, the connection map, and the `ActionContext` into
  `CodeWorkflowTaskExecutor.executePerform`.
- **SDK change (`sdks/backend/java/workflow-api`)** — binary-compatible via default methods, the
  pattern `ProjectHandler` already uses:

  ```java
  interface PerformFunction {
      default Object apply() { return null; }
      default Object apply(TaskContext context) { return apply(); }
  }
  ```

  plus a new `com.bytechef.workflow.definition.TaskContext` interface in the SDK exposing
  `Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)`
  and `void log(String level, String message)`. Existing zero-arg lambdas keep compiling and running;
  the engines invoke the one-arg variant.
- **Script languages** (JS/Python/Ruby): the engines pass the guest proxy context as the single
  argument — `perform: function (context) {...}`. Existing zero-arg JS functions and `*args` Python/
  Ruby lambdas tolerate the extra argument unchanged.
- **Java code workflows**: the classloader path receives a host-side `TaskContext` implementation
  directly. The Espresso path gets a guest-bridge extension mirroring the custom-component pattern
  (`HostBridge` gains `componentExecute(String requestJson)`; `GuestSdkClasspath` ships a guest-side
  `TaskContext` implementation) — same JSON-across-the-boundary discipline, Java never directly
  exposed.
- **Sandbox tightening**: `ProjectHandlerPolyglotEngine`/`IntegrationHandlerPolyglotEngine` contexts
  adopt the strict builder from the shared module (proxies work under `HostAccess.NONE`).

**Connections** for `context.component` calls: the script component resolves connection names from
the *task's* configured connections; code workflow tasks have no connection UI. v1 resolves the
`connectionName` argument against the workspace/environment connection store by connection name
(admin-authored code, admin-only deploy paths — acceptable authorization posture, and consistent
with the copilot/MCP write tools being admin-gated). Calls to connection-requiring components
without a resolvable name fail with a clear error.

**Deferred (explicitly out of scope for v1)**: passing workflow inputs / prior-task outputs into
task `perform` (requires giving the SDK's empty `Parameter` marker a real shape, emitting
`taskDefinition.getParameters()` into the generated task JSON — closing the existing `// TODO` — and
letting Atlas evaluate `${...}` expressions there). The one-argument `TaskContext` signature is
forward-compatible with adding an `input` argument later via another default method.

### B. Script custom components — inputs, connection, and http/log context

`ComponentHandlerPolyglotEngine.executePerform` fulfills the already-documented signature
`perform(inputParameters, connectionParameters, context)`:

- `inputParameters`/`connectionParameters` marshalled with the shared `copyToGuestValue` helpers
  (plain guest maps).
- `context` is a guest proxy exposing `http(requestMap) -> {statusCode, headers, body}` and
  `log(level, message)`, backed by the existing `HostContextBridge` contract
  (`method`/`url`/`headers`/`queryParameters`/`body`/`configuration` request keys — the exact
  `executeRequest` contract the Espresso path already supports). NO component-invocation access:
  custom components are components; composition happens in workflows.
- Sandbox: the context builder adopts the strict shared builder (`HostAccess.NONE` etc. — proxies
  don't need host access; the current no-options builder is replaced).
- The Java-jar (Espresso/classloader) custom-component path is unchanged — it already receives the
  full `GuestActionContext`.
- Starter template + docs + agent prompts updated to show a working `context.http` example.

### C. Non-goals

- No inter-component calls from custom components.
- No workflow-input plumbing into code workflow tasks (deferred, see above).
- No change to visual workflows or the script *component* behavior (it is refactored onto the shared
  module but stays behavior-identical, pinned by its existing tests).
- The in-process `CLASS_LOADER` Java paths keep their current (unsandboxed) posture — Espresso
  remains the opt-in isolation for Java; this design does not change that trade-off.

## Testing

- Shared-module unit tests: marshalling round-trips, sandbox options pinned (a test asserting the
  builder's restrictions, mirroring the script component's explicit-lockdown comment).
- Script component regression: existing `PolyglotEngine` tests keep passing after extraction.
- Custom components: engine test invoking a JS action that reads inputs and calls `context.http`
  against a mocked bridge; log capture; signature-tolerance for old zero-arg performs.
- Code workflows: engine tests for JS/Python/Ruby task perform receiving `context` and invoking a
  mocked component action + connection-name resolution failure case; SDK default-method compat test
  (old zero-arg lambda still runs); Java classloader path test; Espresso bridge test mirroring
  `GuestComponentBridgeTest`.

## Decisions (resolved at review, 2026-08-05)

1. **Connections**: v1 resolves the `connectionName` argument against the workspace/environment
   connection store by name (admin-authored code on admin-only deploy paths).
2. **`TaskContext.component(...)` shape**: positional —
   `Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)`
   (guest languages call `context.component.<name>.<action>(input, connectionName?)` via the proxy
   chain; the positional SDK method is what the Java path and the Espresso guest bridge implement).
3. **Java code workflows are in v1** — including the Espresso guest-bridge extension
   (`componentExecute` on the bridge + a guest-side `TaskContext` shipped via `GuestSdkClasspath`),
   confirmed explicitly ("including java code workflow via sdk").

## Implementation notes (2026-08-06)

Implemented as designed, with three deliberate refinements:

1. **No `GuestContextFactory` threading through the loaders.** The design sketched passing a
   context factory through `loadProjectHandler(...)`/`loadIntegrationHandler(...)`. Instead the
   loaders' `TaskDefinition.getPerform()` implementations override `apply(TaskContext)` directly —
   the context arrives at invocation time through the SDK seam the executor already calls
   (`performFunction.apply(taskContext)`). Loader signatures are unchanged, and the definition-time
   loading paths (which never invoke perform) are untouched by construction.
2. **`PerformFunction.apply()` stays abstract** rather than becoming a default method. Making both
   methods default (as sketched) would have destroyed the functional-interface property that lets
   existing zero-arg lambdas compile. The context-consuming lambda form is provided by the separate
   `ContextPerformFunction` interface plus a `WorkflowDsl.task(...).perform(...)` overload,
   disambiguated by arity.
3. **Ruby-only sandbox carve-out.** `PolyglotSandbox.newContext` sets `allowCreateThread(true)` iff
   `ruby` is among the permitted languages: TruffleRuby implements host-side iteration of guest
   hashes with a fiber-backed `Enumerator`, and fibers require thread creation — with it denied,
   any Ruby hash argument to a `context.component` call fails with "fibers not allowed with
   allowCreateThread(false)". Every other restriction is unchanged for Ruby, and both behaviors
   (Ruby hash iteration works; JS host-class lookup still denied) are pinned by
   `PolyglotSandboxTest`.

The Espresso-path engine tests are guarded by the repo's standard skip-if-Espresso-unavailable
assumption and therefore only execute on platforms where embedded Espresso can boot
(linux amd64/aarch64, darwin-amd64) — e.g. CI, not darwin-aarch64 dev machines.
