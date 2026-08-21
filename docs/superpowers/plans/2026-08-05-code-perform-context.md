# Perform Context for Script Components and Code Workflow Tasks — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give script custom components a real `perform(inputParameters, connectionParameters, context)` (http/log) and code workflow tasks a `perform(context)` with full component access (`context.component.<name>.<action>(input, connectionName)`), per `docs/superpowers/specs/2026-08-05-code-perform-context-design.md`.

**Architecture:** Extract the script component `PolyglotEngine`'s sandbox + marshalling + proxy machinery into a shared CE module; both EE engines consume it. SDK `PerformFunction` gains a default-method `apply(TaskContext)`; Java code workflows get it on both classloader and Espresso paths (bridge extension). All guest access crosses narrow, JSON/proxy-marshalled boundaries — Java is never directly exposed.

**Tech Stack:** GraalVM polyglot (Proxy*, HostAccess), Spring, JUnit 5 + Mockito. Key reference files: `server/libs/modules/components/script/src/main/java/com/bytechef/component/script/engine/PolyglotEngine.java` (proxies, marshalling, strict sandbox), `server/ee/libs/platform/platform-custom-component/platform-custom-component-loader/.../HostContextBridge.java` + `.../guest/GuestComponentBridge.java` (bridge pattern).

## Global Constraints

- CE modules under `server/libs/` use the Apache 2.0 header; EE modules under `server/ee/` use the ByteChef Enterprise header + `@version ee` on new classes. SDK modules (`sdks/backend/`) use the Apache header.
- The script *component*'s observable behavior must not change — its existing tests are the regression pin for the extraction.
- Sandbox posture is pinned by tests: every polyglot (non-Espresso) guest context built from the shared module must set exactly the restrictions listed in the spec's Reference model.
- Backward compatibility is a hard requirement: existing zero-arg `PerformFunction` lambdas (Java jars AND script `function () {}` / `lambda *args:`) keep working.
- Java style rules per CLAUDE.md (blank line before control statements, blank line after variable mutation, no trailing blank line, camelCase test names, no `Impl` in test class names). `./gradlew spotlessApply` on touched modules before each commit; judge gradle runs by redirecting to a file and grepping `^> Task .* FAILED`.
- Commit message `0 <description>` with trailers:
  `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` and
  `Claude-Session: https://claude.ai/code/session_01D8EFzG3BVniqYrWXYTe3rw`
- New settings.gradle.kts entries for new modules; register new Spring Data-free modules normally (no auto-config needed unless beans are declared).

---

### Task 1: Shared polyglot module — extract sandbox, marshalling, and proxies

**Files:**
- Create module: `server/libs/platform/platform-component/platform-component-polyglot/` (`build.gradle.kts`, register in `settings.gradle.kts` near the other platform-component modules)
- Create: `.../platform-component-polyglot/src/main/java/com/bytechef/platform/component/polyglot/PolyglotSandbox.java`
- Create: `.../polyglot/PolyglotValues.java`
- Create: `.../polyglot/ComponentContextProxies.java` (or split into the three proxy classes — implementer's choice, one file per class preferred)
- Modify: `server/libs/modules/components/script/src/main/java/com/bytechef/component/script/engine/PolyglotEngine.java` (consume the shared code; delete the duplicated private members)
- Modify: `server/libs/modules/components/script/build.gradle.kts` (+ dependency on the new module)
- Test: `.../platform-component-polyglot/src/test/java/com/bytechef/platform/component/polyglot/PolyglotSandboxTest.java`, `PolyglotValuesTest.java`

**Interfaces (later tasks consume these EXACT shapes):**
- `PolyglotSandbox.newContext(Engine engine, String... permittedLanguages)` → `org.graalvm.polyglot.Context` carrying exactly: `allowHostAccess(HostAccess.NONE)`, `allowHostClassLoading(false)`, `allowHostClassLookup(cn -> false)`, `allowNativeAccess(false)`, `allowCreateThread(false)`, `allowCreateProcess(false)`, `allowIO(IOAccess.NONE)`, `allowEnvironmentAccess(EnvironmentAccess.NONE)`, `allowPolyglotAccess(PolyglotAccess.NONE)`. Copy the lockdown comment from `PolyglotEngine.java:236-240`.
- `PolyglotValues.copyToGuestValue(Object value, String languageId)`, `copyToJavaValue(Value value)`, `copyFromPolyglotContext(Object value)` — extracted verbatim from `PolyglotEngine.java:115-233` (public static).
- The three proxy classes extracted from `PolyglotEngine.java:291-566` (`ContextProxyObject`, `ComponentProxyObject`, `ActionProxyObject`), generalized ONLY as follows: their Spring collaborators (`ApplicationContext` today) are replaced by a constructor-injected `ComponentActionInvoker` functional seam:
  ```java
  public interface ComponentActionInvoker {
      Object invoke(String componentName, String actionName, Map<String, ?> input, String connectionName)
          throws Exception;
  }
  ```
  `ActionProxyObject`'s body dispatches through it. A second constructor-injected seam `ComponentCatalog` (`boolean hasComponent(String name)`, `boolean hasAction(String componentName, String actionName)`) replaces the direct `ComponentDefinitionService` lookups used by `hasMember`. The script component provides adapters preserving today's behavior exactly.

**Steps:**
- [ ] **Step 1:** Write failing tests: `PolyglotSandboxTest.testContextRestrictionsPinned` (build a context for `js`, assert a host-class-lookup attempt and `Java.type` access fail; assert eval of pure JS works), `PolyglotValuesTest` round-trip tests for map/list/instant/byte[] (port assertions from any existing script engine tests that cover marshalling; write new ones otherwise).
- [ ] **Step 2:** Run them (module doesn't exist → create module skeleton first so the test compiles, then FAIL on missing classes).
- [ ] **Step 3:** Extract the code from `PolyglotEngine.java` into the new module per the interfaces above. Then refactor `PolyglotEngine` to delegate: its `getContext`→`PolyglotSandbox.newContext`, its marshalling → `PolyglotValues`, its proxies → shared classes with adapters implementing `ComponentActionInvoker` (wrapping today's `jobContextAware.toActionContext` + `actionDefinitionService.executePerformForPolyglot` logic) and `ComponentCatalog`.
- [ ] **Step 4:** Run new-module tests AND the whole script component module test task — both green.
- [ ] **Step 5:** Commit: `0 Extract shared polyglot sandbox, marshalling, and component proxies from the script engine`.

---

### Task 2: SDK — `TaskContext` + `PerformFunction` default methods

**Files:**
- Create: `sdks/backend/java/workflow-api/src/main/java/com/bytechef/workflow/definition/TaskContext.java`
- Modify: `sdks/backend/java/workflow-api/src/main/java/com/bytechef/workflow/definition/TaskDefinition.java`
- Test: `sdks/backend/java/workflow-api/src/test/java/com/bytechef/workflow/definition/PerformFunctionCompatibilityTest.java` (create test dir if absent)

**Interfaces:**
```java
public interface TaskContext {

    Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception;

    void log(String level, String message);
}
```
`TaskDefinition.PerformFunction` becomes:
```java
interface PerformFunction {

    default Object apply() {
        return null;
    }

    default Object apply(TaskContext context) throws Exception {
        return apply();
    }
}
```
(Javadoc: engines invoke `apply(TaskContext)`; zero-arg implementations keep working. NOTE: `getPerform()` stays as-is.)

**Steps:**
- [ ] **Step 1:** Failing test: a zero-arg lambda assigned to `PerformFunction` still runs via `apply(context)` (returns its zero-arg result); a context-consuming implementation receives the passed context; `WorkflowDsl.task("x").perform(...)` accepts both shapes.
- [ ] **Step 2:** Verify fail (TaskContext missing).
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run `:sdks:backend:java:workflow-api:test` + compile `:sdks:backend:automation:project-api` and `:sdks:backend:embedded:integration-api` — green.
- [ ] **Step 5:** Commit: `0 Add TaskContext and backward-compatible PerformFunction context variant to the workflow SDK`.

---

### Task 3: Connection-by-name resolution + host TaskContext implementation

**Files:**
- Create: `server/ee/libs/modules/components/code-workflow/src/main/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskContext.java` (host-side `TaskContext` impl)
- Modify: `server/ee/libs/modules/components/code-workflow/src/main/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskExecutor.java`
- Modify: `.../action/definition/CodeWorkflowPerformActionDefinition.java`
- Modify: `server/ee/libs/modules/components/code-workflow/build.gradle.kts` (+ platform-component-polyglot, + whatever connection-service module the resolver needs)
- Test: `.../task/CodeWorkflowTaskContextTest.java`, extend `CodeWorkflowTaskExecutor` tests (create if absent)

**Interfaces (Tasks 4-6 consume):**
- `CodeWorkflowTaskExecutor.executePerform(String codeWorkflowContainerUuid, String workflowName, String taskName, PlatformType type, Parameters inputParameters, Map<String, ComponentConnection> componentConnections, ActionContext actionContext)` — extended signature; `CodeWorkflowPerformActionDefinition.perform` forwards its full seam instead of dropping it.
- `CodeWorkflowTaskContext implements TaskContext` — `component(...)` resolves the connection: first from the forwarded `componentConnections` map by name, else by connection name lookup against the connection store for the current environment (inject the same service surface the script `ActionProxyObject` path uses — investigate `executePerformForPolyglot`'s connection parameter contract and mirror it; a missing/unresolvable name on a connection-requiring component throws `IllegalArgumentException` naming the connection). Dispatches through `ActionDefinitionService.executePerformForPolyglot(...)`. `log(...)` delegates to the forwarded `ActionContext.log`.
- Executor builds `CodeWorkflowTaskContext` per invocation and (for now) still calls `performFunction.apply()` — the context is HANDED DOWN in Task 4/5 when the loaders learn to accept it; in THIS task, change the executor's call to `performFunction.apply(taskContext)` for the classloader-loaded Java path only if trivially possible; otherwise keep apply() and leave invocation switching entirely to Tasks 4-6. (Bias: keep this task about seam-forwarding + the TaskContext class.)

**Steps:**
- [ ] **Step 1:** Failing tests: `CodeWorkflowTaskContextTest` — component() dispatches with resolved connection (mocked resolver + invoker), unresolvable name throws with the connection name in the message, log delegates. Executor test — the new signature threads inputParameters/connections/context (mock verify).
- [ ] **Step 2:** Verify fail.
- [ ] **Step 3:** Implement; update `CodeWorkflowPerformActionDefinition.perform` to pass `inputParameters`, the connection map, and `actionContext`.
- [ ] **Step 4:** Module tests green.
- [ ] **Step 5:** Commit: `0 Forward the code workflow perform seam and add the host TaskContext with connection-by-name resolution`.

---

### Task 4: Script custom components — inputs + http/log context + strict sandbox

**Files:**
- Modify: `server/ee/libs/platform/platform-custom-component/platform-custom-component-loader/src/main/java/com/bytechef/ee/platform/customcomponent/loader/ComponentHandlerPolyglotEngine.java`
- Modify: `.../loader/HostContextBridge.java` (constructor visibility only if needed — reuse as-is)
- Modify: `platform-custom-component-loader/build.gradle.kts` (+ platform-component-polyglot)
- Test: new `.../loader/ComponentHandlerPolyglotEngineTest.java`

**Behavior:**
- `executePerform` builds the guest context via `PolyglotSandbox.newContext(engine, languageId)`.
- Marshal `inputParameters.toMap()` and `connectionParameters.toMap()` with `PolyglotValues.copyToGuestValue(..., languageId)`.
- Build a guest `context` as a `ProxyObject` with two members: `http` — a `ProxyExecutable` taking one request map argument, converting it via `copyToJavaValue`, serializing to JSON, calling `HostContextBridge.httpExecute`, parsing the response JSON, returning it via `copyToGuestValue`; `log` — `ProxyExecutable(level, message)` → `HostContextBridge.log(level, message, null)`. The bridge instance is constructed from the (previously discarded) `ActionContext` parameter.
- Invoke `perform` with the three guest arguments. Old zero-arg guest functions must still work (JS ignores extra args; Python/Ruby `*args` splats tolerate them — pin with a test using a zero-arg JS function).

**Steps:**
- [ ] **Step 1:** Failing tests: JS action reads `inputParameters.foo` and returns it; JS action calls `context.http({method:'GET', url:...})` against a mocked `ActionContext` whose `http` executor returns a canned response — assert the guest sees `{statusCode, headers, body}`; JS action calls `context.log('INFO', 'x')` — verify via mocked ActionContext logger; legacy zero-arg action still runs.
- [ ] **Step 2:** Verify fail (`perform.apply(null)` today ignores everything).
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Module tests green (including any existing loader tests).
- [ ] **Step 5:** Commit: `0 Script custom components receive inputs, connection parameters, and an http/log context in a strict sandbox`.

---

### Task 5: Code workflow polyglot engines — context-bearing perform (automation + embedded)

**Files:**
- Modify: `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerLoader.java` + `ProjectHandlerPolyglotEngine.java`
- Modify: `server/ee/libs/embedded/embedded-code-workflow-loader/.../IntegrationHandlerLoader.java` + `IntegrationHandlerPolyglotEngine.java`
- Modify: both loaders' `build.gradle.kts` (+ platform-component-polyglot)
- Modify: `CodeWorkflowTaskExecutor` (pass the TaskContext through the loader call; switch invocation to the context variant)
- Test: new engine tests in both loader modules + executor test update

**Behavior:**
- `loadProjectHandler(...)`/`loadIntegrationHandler(...)` gain an overload accepting a `TaskContext` supplier (or the TaskContext itself) threaded to `PolyglotTaskDefinition.getPerform()`; the zero-arg overload keeps existing behavior (definition-time loading in the configuration facades continues to use it — they never invoke perform).
- Script path `executePerform`: guest context = a `ProxyObject` with members `component` (the shared `ComponentContextProxies` chain backed by a `ComponentActionInvoker` that delegates to the passed `TaskContext.component(...)`) and `log` (ProxyExecutable → `TaskContext.log`). Invoke the guest perform function with this single argument. Sandbox via `PolyglotSandbox.newContext`.
- Java classloader path: call `performFunction.apply(taskContext)` directly (host-side TaskContext).
- Zero-arg legacy performs still work on all paths (pin with tests).
- Note the Espresso Java path stays context-less until Task 6 — its `invokeMember("apply")` remains temporarily.

**Steps:**
- [ ] **Step 1:** Failing tests per module: JS task perform calls `context.component.mock.doIt({x:1}, 'conn')` — assert the injected invoker received `("mock","doIt",{x=1},"conn")` and its return value round-trips to the caller; python + ruby happy-path variants; JS zero-arg legacy task still runs; classloader Java path receives the host TaskContext (use a tiny in-test jar or a stubbed ProjectHandler — mirror how existing loader tests build fixtures; if jar fixtures are impractical, test through a fake `ProjectHandler` on the classloader path).
- [ ] **Step 2:** Verify fail.
- [ ] **Step 3:** Implement automation, then transpose to embedded (full parallel, no shared abstraction across the two EE modules — repo convention).
- [ ] **Step 4:** Both loader modules + code-workflow component module tests green.
- [ ] **Step 5:** Commit: `0 Code workflow task perform receives a component-capable context in script and classloader paths`.

---

### Task 6: Espresso bridge for Java code workflows

**Files:**
- Modify: `ProjectHandlerPolyglotEngine.getJavaContext` + `executeJavaPerform` (automation) and the embedded twin: add `allowExperimentalOptions(true)`, `allowPolyglotAccess(PolyglotAccess.ALL)`, `option("java.Polyglot", "true")`; put a host bridge into polyglot bindings; guest resolves it.
- Create: host bridge class (mirror `HostContextBridge` shape) `CodeWorkflowHostBridge` in each loader module (or one shared in a common loader-support location — implementer judgment, favor duplication per repo convention) with `@HostAccess.Export` methods: `String componentExecute(String requestJson)` (`{componentName, actionName, input, connectionName}` → TaskContext.component → JSON result) and `void log(String level, String message)`.
- Create: guest-side SDK support in `sdks/backend/java/workflow-api` (new `com.bytechef.workflow.guest` package or a new small `workflow-guest-bridge` SDK module mirroring `platform-custom-component-guest-bridge`): `GuestTaskContext implements TaskContext` resolving the bridge via `Polyglot.importObject("byteChefCodeWorkflowHostBridge")` with the same `ForeignHostBridge`-style interop wrapper; values cross as JSON.
- Modify: both loaders' `build.gradle.kts` guestSdk wiring if a new module is added.
- Test: bridge tests mirroring `GuestComponentBridgeTest` (host-side: bridge JSON contracts; guest-side: `GuestTaskContext` against a stub bridge), plus an engine test that the Espresso path now invokes `apply(TaskContext)` with a working `component(...)` round-trip (skip-if-Espresso-unavailable guard if the repo's existing Espresso tests use one — check `ComponentHandlerEspressoEngineTest` for the pattern and mirror it).

**Steps:**
- [ ] **Step 1:** Failing tests per the above (host bridge JSON contract tests are plain-JVM; engine test follows the existing Espresso test guard pattern).
- [ ] **Step 2:** Verify fail.
- [ ] **Step 3:** Implement automation, transpose embedded.
- [ ] **Step 4:** Loader modules + SDK tests green.
- [ ] **Step 5:** Commit: `0 Java code workflows receive TaskContext through an Espresso guest bridge`.

---

### Task 7: Templates, docs, prompts

**Files:**
- Modify: `custom-component-templates/starter.js` — perform body demonstrates `context.http` (commented example) and returns inputs-derived value.
- Modify: `code-workflow-templates/starter.{js,py,rb}` + `integration-code-workflow-templates/starter.{js,py,rb}` — task perform takes `context`; a commented `context.component` example.
- Modify: `docs/content/docs/enterprise/extensibility/custom-components.mdx` (single-file contract: perform signature now real; http example) and `code-workflows.mdx` (task perform contract: `perform(context)`, `context.component` example, connection-by-name note).
- Modify: agent prompts `prompt_custom_component_{ask,build}.txt`, `prompt_code_workflow_{ask,build}.txt` — teach the new contracts (custom components: context.http/log, still no component calls; code workflows: context.component with connection names).
- Modify: CLAUDE.md — short subsection under the draft/publish section documenting the perform-context contracts + shared polyglot module.
- Test: existing template-loading tests (CreateEmpty tests) must still pass — templates must still compile through the real loaders.

**Steps:**
- [ ] **Step 1:** Update templates; run the CreateEmpty facade tests in automation-configuration-service, embedded-configuration-service, and platform-custom-component-configuration-service (they load starters through the real engines — this is the compile gate).
- [ ] **Step 2:** Update docs + prompts + CLAUDE.md.
- [ ] **Step 3:** Commit: `0 Teach templates, docs, and agent prompts the perform context contracts`.

---

### Task 8: Full verification

- [ ] **Step 1:** `./gradlew spotlessApply`, then `./gradlew compileJava compileTestJava --continue` (file + grep FAILED).
- [ ] **Step 2:** Test tasks with `--continue`, grep FAILED: platform-component-polyglot, script component module, workflow-api SDK, platform-custom-component-loader, automation-code-workflow-loader, embedded-code-workflow-loader, code-workflow component module, automation-configuration-service, embedded-configuration-service.
- [ ] **Step 3:** Spec cross-read (`2026-08-05-code-perform-context-design.md`) section by section → evidence table; flag gaps.
- [ ] **Step 4:** Commit any verification fixes: `0 Perform context: verification fixes`.

---

## Self-review notes

- Task ordering: 1 (shared module) → 2 (SDK) are prerequisites for 3-6; 3 before 5 (executor signature); 5 before 6 (Espresso switches the remaining path). 4 depends only on 1.
- The definition-time loading paths (configuration facades compiling sources on save) never invoke perform, so the loader overloads keep them untouched — pinned by the existing CreateEmpty/Source facade tests plus Task 7's template gate.
- Risk concentrations for reviewers: proxy-extraction behavior drift (Task 1 — script tests are the pin), guest function invocation shapes across three languages (Tasks 4-5), Espresso context options (Task 6 — `java.Polyglot` requires `allowExperimentalOptions`, see `ComponentHandlerEspressoEngine:361`).
