# Espresso-based Java Code-Workflow Loading + Embedded Integration Loader

**Date:** 2026-07-04
**Status:** Approved

> **Addendum (2026-07-05):** After implementation, the in-process classloader path was restored behind
> `bytechef.workflow.code-workflow.java-loader` (`class-loader` default / `espresso` opt-in), mirroring the
> custom-component flag — whose default also changed to `class-loader`. `ProjectHandlerClassLoader` is back and a new
> `IntegrationHandlerClassLoader` covers the embedded side. The Espresso path and everything else in this spec is
> unchanged; the sandbox is now opt-in rather than default.

## Problem

Code workflows uploaded as Java JARs are loaded through `ProjectHandlerClassLoader`
(an `IsolatingClassLoader`) and therefore execute **inside the host JVM** with full
host privileges. JavaScript, Python, and Ruby uploads run through GraalVM polyglot
contexts with no host access — a real sandbox. Java should have the same trust
model.

Separately, the embedded (integration) side has no code-workflow loader at all:
`CodeWorkflowTaskExecutor` only handles `PlatformType.AUTOMATION` and carries a
`// TODO integration` comment, even though `IntegrationHandler` and
`IntegrationDsl` already exist in `sdks/backend/embedded/integration-api`.

## Goals

1. Load and execute Java code-workflow JARs inside a GraalVM **Espresso** guest
   JVM (`org.graalvm.polyglot:java`, dependency already declared) with no host
   access — same sandbox posture as the script languages.
2. Add `embedded-code-workflow-loader` mirroring the automation loader, wired
   into `CodeWorkflowTaskExecutor`'s `EMBEDDED` branch, supporting all four
   languages.

## Non-Goals (follow-ups)

- **Custom components** (`ComponentHandlerLoader` in
  `platform-custom-component-loader`): stays on the classloader path in this
  change. Its runtime surface passes host objects (`Parameters`,
  `ActionContext`) *into* the guest on every action/trigger perform, which
  needs its own bridging design. This spec deliberately covers only code
  workflows, whose perform is no-arg.
- Upload-side facade for embedded code workflows (no
  `IntegrationCodeWorkflowFacade` exists yet); this spec only delivers the
  loader and executor wiring.

## Background: how GraalVM handles Java

Espresso ("Java on Truffle") executes **bytecode, not source**. There is no
`context.eval("java", sourceString)`; instead a context is built with
`.option("java.Classpath", ...)` and classes are resolved through
`context.getBindings("java")`. The guest JVM has its own heap and classes —
guest `ProjectHandler` is *not* the host's `ProjectHandler` class — so results
cross the boundary as interop `Value`s.

Consequence: unlike `IsolatingClassLoader` (host as parent classloader), the
guest resolves SDK classes (`ProjectDsl`, `WorkflowDefinition`, ...) only from
its own classpath. The server must supply them (see SDK bundling below).

## Design

### 1. Automation loader (`automation-code-workflow-loader`)

- `ProjectHandlerLoader.loadProjectHandler(url, language)`:
  - `JAVA` routes to a new Espresso path in `ProjectHandlerPolyglotEngine`
    (`getLanguageId` gains `case JAVA -> "java"`; the loader passes the JAR
    file path instead of `Files.readString`).
  - `cacheKey`/`cacheManager` parameters are removed (they only served the
    classloader cache). Call sites updated: `ProjectCodeWorkflowFacadeImpl`,
    `CodeWorkflowTaskExecutor` (whose now-unused `CacheManager` field is
    removed too).
- `ProjectHandlerClassLoader` is deleted; the `class-loader-api` dependency is
  dropped from the module.

### 2. Espresso loading mechanics (JAVA path)

Upload format is unchanged (`Language.JAVA("jar")`).

1. Host reads the impl class name from the JAR entry
   `META-INF/services/com.bytechef.automation.project.ProjectHandler`
   (embedded: `META-INF/services/com.bytechef.embedded.integration.IntegrationHandler`).
   Missing entry → `IllegalArgumentException` naming the expected path.
2. Context construction, on the engine class's shared `Engine`:

   ```java
   Context.newBuilder("java")
       .engine(engine)
       .option("java.Classpath", uploadedJarPath + File.pathSeparator + guestSdkClasspath)
       .build()
   ```

   No `allowAllAccess`/`allowHostAccess` grants — the guest cannot touch host
   classes, filesystem access is confined to the guest JDK/classpath, matching
   the script languages' closed-sandbox defaults.
3. Definition extraction via interop:
   `getBindings("java").getMember(implClassName).newInstance()` →
   `invokeMember("getDefinition")`, then walk the guest graph with plain
   member invocations — guest `Optional` unwrapped via
   `invokeMember("orElse", (Object) null)`, guest `List` via `size()`/`get(i)`,
   scalars via `Value.as(...)`. All data is **fully materialized into the
   existing host-side `Polyglot*Definition` records before the context
   closes** (no `Value` may escape a closed context).
4. `perform()` semantics mirror the script path: each
   `PerformFunction.apply()` opens a fresh context, re-instantiates the
   handler, navigates to the workflow/task by name, invokes
   `getPerform().apply()`, and converts the result to a host value. Perform
   results must be interop-friendly (primitives, strings, lists, maps) — the
   same constraint the script languages already have.

### 3. Bundled guest SDK jars

Uploaded JARs stay thin (compiled against the SDK, not shaded). Each loader
module bundles the SDK jars the guest needs:

- automation: `sdks:backend:automation:project-api` +
  `sdks:backend:java:workflow-api`
- embedded: `sdks:backend:embedded:integration-api` +
  `sdks:backend:java:workflow-api`

(Both SDK modules have no third-party dependencies; `project-api` and
`integration-api` each depend only on `workflow-api`.)

Build: a dedicated `guestSdk` Gradle configuration + a copy step wired into
`processResources` places the jars under `META-INF/guest-sdk/` in the loader
module's resources.

Runtime: a `GuestSdkClasspath` helper extracts the resource jars once to a
cached temp directory and returns the classpath fragment appended to
`java.Classpath`. Extraction is idempotent and process-lifetime cached.

### 4. Embedded loader module

New module `server:ee:libs:embedded:embedded-code-workflow-loader`, package
`com.bytechef.ee.embedded.codeworkflow.loader` (EE license header +
`@version ee` on every file):

- `IntegrationHandlerLoader.loadIntegrationHandler(url, language)` — same
  shape as the automation loader, polyglot-only for all four languages (no
  classloader variant ever exists).
- `IntegrationHandlerPolyglotEngine` — mirrors `ProjectHandlerPolyglotEngine`
  with integration-specific members: `componentName`, `componentVersion`,
  `multipleInstances`, plus the shared `description`/`category`/`tags`/
  `version`/`workflows`. `PolyglotIntegrationDefinition` implements
  `IntegrationDefinition`.
- `CodeWorkflowTaskExecutor` gains the `PlatformType.EMBEDDED` branch calling
  it (replacing the `// TODO integration` comment); the `code-workflow`
  component module adds the new loader as a dependency.
- Registered in `settings.gradle.kts`.

### 5. Error handling

- Missing `META-INF/services` entry → `IllegalArgumentException` with the
  expected entry path and JAR name.
- Espresso context-creation failure (unsupported platform) → wrapped in an
  exception pointing at GraalVM Espresso platform support (Linux amd64/
  aarch64, macOS aarch64; Windows experimental).
- Loaders keep the existing behavior of wrapping checked exceptions in
  `RuntimeException`.

### 6. Testing

Neither loader module has tests today; both get unit tests:

- **JAVA path**: build a fixture JAR at test runtime (`ToolProvider`-driven
  `javac` compiling a minimal handler against the SDK on the test classpath,
  packaged with a `META-INF/services` entry), then assert definition
  extraction and a `perform()` round-trip through Espresso.
- **Script path**: one JavaScript-based test per engine to lock the shared
  extraction logic (name/description/version/workflows/tasks + perform).
- Executor wiring: test that `PlatformType.EMBEDDED` routes to the embedded
  loader.

## Risks / accepted costs

- **Espresso footprint**: `org.graalvm.polyglot:java` ships a guest JDK
  (hundreds of MB of runtime resources). The dependency is already declared in
  these modules, so this cost is accepted.
- **Startup latency**: first Espresso context takes seconds. Acceptable at
  deploy/load time; `perform()` per-call context creation matches existing
  script-language behavior.
- **Behavior change (intended)**: uploaded Java code can no longer reach host
  classes, Spring beans, or host statics. That is the sandbox goal, but any
  existing uploaded JAR relying on host access will stop working.
- **Platform support**: Espresso standalone does not cover every OS/arch;
  unsupported platforms fail at context creation with a clear message.
