# Sandboxed Custom Component Execution via GraalVM Espresso

**Date:** 2026-07-04
**Status:** Approved

> **Addendum (2026-07-05):** The `bytechef.component.custom-component.java-loader` default changed from `espresso` to
> `class-loader` — the sandbox is opt-in. A matching flag was added for code workflows
> (`bytechef.workflow.code-workflow.java-loader`, also `class-loader` by default).
**Predecessor:** `2026-07-04-espresso-code-workflow-loaders-design.md` (code workflows; landed)

## Problem

Java custom components (`ComponentHandlerLoader`, EE) load through
`ComponentHandlerClassLoader` and run **inside the host JVM** with full
privileges. The code-workflow loaders already moved to a GraalVM Espresso
guest-JVM sandbox; custom components are the remaining — and riskier —
in-process execution path, because their actions run repeatedly at workflow
runtime, not just at deploy time.

Unlike code workflows (no-arg `perform()`), component actions execute as
`perform(Parameters inputParameters, Parameters connectionParameters,
ActionContext context)` — host objects must flow **into** the guest, and the
definition graph (properties, options, triggers) is orders of magnitude larger
than a project definition.

## Decisions (from design review)

1. **Bridge depth: full bridge including a narrow ActionContext** — actions
   execute in the guest with real input/connection parameters and a bounded
   `ActionContext` subset (http, json, log, encoder, converter).
2. **Classloader path is kept behind a flag** — `bytechef.component.custom-component.java-loader`
   (`espresso` default, `class-loader` opt-out) for deployments that need
   full-fidelity components until the bridge covers everything.

## Architecture: guest-side bridge jar, JSON across the boundary

The code-workflow loaders walk the guest object graph from the host via
interop member calls. That works for ~6 getters; it does not scale to
`Parameters` (65 methods), 12 property types, and `Context`'s nested
capability interfaces. Instead:

- A new **guest bridge** module (`platform-custom-component-guest-bridge`)
  ships plain-Java code that runs **inside** the guest JVM, compiled against
  `component-api` + Jackson. It is bundled into the loader's resources (like
  the code-workflow guest SDK jars) and appended to `java.Classpath` together
  with `component-api` and Jackson jars.
- Host ↔ guest crossings shrink to **three interop touchpoints**:
  1. `GuestComponentBridge.describeComponent(implClassName)` → one JSON string
     describing the full declarative definition.
  2. `GuestComponentBridge.executeActionPerform(implClassName, actionName,
     inputParametersJson, connectionParametersJson)` → result JSON string.
  3. A single **host callback object** (`HostContextBridge`) placed in the
     polyglot bindings; the guest imports it via the Espresso guest polyglot
     API (`com.oracle.truffle.espresso.polyglot.Polyglot.importObject` +
     `Polyglot.cast`, artifact `org.graalvm.espresso:polyglot`). Only methods
     annotated `@HostAccess.Export` are callable — that annotation set IS the
     sandbox boundary.
- Everything else (parameter typing, definition walking, JSON) is plain Java
  on one side or the other — unit-testable on any platform, including this
  darwin-aarch64 dev machine where Espresso cannot boot (embedded Espresso
  needs guest core libraries that ship for linux-amd64/darwin-amd64 only;
  verified empirically during the code-workflow phase).

### Espresso context options

Same as code workflows (`allowCreateThread`, `allowNativeAccess`,
`allowIO(IOAccess.ALL)` — Espresso runtime needs; host access stays at the
`HostAccess.EXPLICIT` default) plus:

- `java.Polyglot=true` — exposes the guest polyglot API so the bridge can
  import the host callback object.
- `.allowPolyglotAccess(PolyglotAccess.ALL)` — permits polyglot-bindings
  access from the guest.

## v1 scope

### Definition extraction (guest walks, host rebuilds via ComponentDsl)

Extracted to JSON and rebuilt host-side using `ComponentDsl` builders
(`ModifiableActionDefinition.perform(...)` overridden to route into Espresso):

- **Component**: name, title, description, version. Icon is read host-side
  from the JAR's `assets/sample.svg` entry (same contract as the classloader
  path).
- **Actions**: name, title, description, **properties** (recursive), perform
  (bridged execution).
- **Triggers**: name, title, description, type, properties — **metadata only,
  no execution** (all 13 trigger lifecycle functions out of scope).
- **Properties**: all declarative fields of the 12 property types — label,
  description, required, advancedOption, hidden, displayCondition,
  expressionEnabled, defaultValue/exampleValue (scalars), static options
  (label/value/description), min/max constraints, and recursive
  `properties`/`items` for OBJECT/ARRAY/FILE_ENTRY. **Dynamic** options
  functions, `PropertiesDataSource`, and `DYNAMIC_PROPERTIES` properties are
  dropped with a build-time warning entry in the JSON (`unsupported` list).

### Perform execution

- `Parameters` → `toMap()` host-side → JSON → guest `GuestParameters`
  (implements all 65 interface methods over the parsed map, Jackson-backed
  conversions, `getFileEntry`/`getFileEntries` throw
  `UnsupportedOperationException` in v1).
- Guest `GuestActionContext` implements `ActionContext`:
  - `json(...)`, `encoder(...)`, `converter(...)` — guest-local (Jackson /
    Base64), no host round-trip.
  - `log(...)` — host callback `HostContextBridge.log(level, message,
    exceptionMessage)` delegating to the live host `ActionContext` logger.
  - `http(...)` — guest builds a request DTO (method, url, headers,
    query params, body content+type, configuration subset: responseType,
    timeout, allowUnauthorizedCerts, followRedirect/followAllRedirects,
    disableAuthorization), serializes to JSON; host executes it through the
    **real** host `ActionContext.http(...)` (so platform auth/proxy behavior
    applies) and returns `{statusCode, headers, body}` JSON; guest wraps it in
    a `Response` adapter (`getBody(Class)` via Jackson). Body content types
    v1: JSON, RAW/plain string, FORM_URL_ENCODED. BINARY/FORM_DATA
    (FileEntry-based) unsupported.
  - Everything else (`file`, `xml`, `data`, `event`, `approval`, `suspend`,
    `mimeType`, `outputSchema`, `getTraceId` returns marshalled value,
    `isEditorEnvironment` marshalled boolean) — `UnsupportedOperationException`
    with a message naming the classloader flag, except the two marshalled
    scalars.
- Perform result → JSON in guest → host parses to `Map`/`List`/scalar.
- Fresh context per perform call (matches script/code-workflow semantics).

### Out of scope for v1 (classloader flag covers these)

- Trigger execution, dynamic options/properties, connection definitions
  (sandboxed components are connection-less in v1 — auth via manual headers
  from input parameters), cluster elements, unified API, file storage,
  XML/data/event/approval/suspend context areas, custom actions.

## Flag

`ApplicationProperties.Component` gains a nested `CustomComponent` section:

```yaml
bytechef:
  component:
    custom-component:
      java-loader: espresso   # or: class-loader
```

`CustomComponentFacadeImpl` and `CustomComponentDynamicComponentHandlerRegistry`
read it and pass the choice into `ComponentHandlerLoader.loadComponentHandler`.
JS/Python/Ruby routing is unchanged. The `JAVA -> "java"` commented line in
`getLanguageId` stays commented out — the Espresso path is a separate branch,
not a language-id mapping (Espresso consumes a JAR path, not script text).

## Modules

- **New:** `server/ee/libs/platform/platform-custom-component/platform-custom-component-guest-bridge`
  — guest-only code: `GuestComponentBridge`, `GuestParameters`,
  `GuestActionContext`, `HostBridge` (guest-side interface mirroring the
  exported host methods), DTOs. Depends on `component-api`,
  `jackson-databind`, `org.graalvm.espresso:polyglot` (compileOnly — Espresso
  injects the guest polyglot API when `java.Polyglot=true`).
- **Changed:** `platform-custom-component-loader` — adds
  `ComponentHandlerEspressoEngine` (context management, host-side JSON→
  `ComponentDsl` assembly, `HostContextBridge` with `@HostAccess.Export`
  methods, guest-bridge classpath bundling à la `GuestSdkClasspath`), and the
  flag switch in `ComponentHandlerLoader`. `ComponentHandlerClassLoader` stays.
- **Changed:** `app-config` (`ApplicationProperties`) — new nested section.
- **Changed:** facade + registry — flag plumbing.

## Testing

- **Guest bridge module (runs everywhere):** pure-JVM unit tests —
  `GuestParameters` type coercions, `GuestActionContext` json/encoder/log/http
  against a fake `HostBridge`, `describeComponent` JSON for a sample handler
  built with `ComponentDsl`, `executeActionPerform` round-trip.
- **Loader module:** JSON→`ComponentDsl` assembly tests (host-side, no
  Espresso); Espresso end-to-end fixture test (compile a fixture handler
  against `component-api` at test runtime, package with service entry, load +
  execute an action with parameters and a stubbed-context http call) —
  Espresso-availability assumption, skipped on darwin-aarch64.
- **Container verification:** end-to-end tests exercised in a linux Docker
  container (Docker amd64 emulation and native arm64 available on this
  machine), since local Espresso cannot boot.

## Risks

- Espresso guest polyglot import mechanics (`Polyglot.importObject`/`cast`)
  are exercised only in the container/CI runs; unit tests fake that seam.
- The `@HostAccess.Export` surface must stay minimal — every exported method
  is reachable by untrusted guest code with attacker-chosen string arguments.
  v1 exports: `log`, `httpExecute`, `getTraceId`, `isEditorEnvironment`.
- Feature regression relative to the classloader path is intentional and
  documented; the flag is the escape hatch.
