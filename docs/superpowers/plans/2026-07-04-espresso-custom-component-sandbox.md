# Espresso Custom Component Sandbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute Java custom components (definition + action performs) inside a GraalVM Espresso guest JVM, with a JSON-over-interop bridge and a narrow `@HostAccess.Export` ActionContext callback; classloader path kept behind `bytechef.component.custom-component.java-loader`.

**Architecture:** New guest-side module `platform-custom-component-guest-bridge` (plain Java against `component-api` + Jackson) does all walking inside the guest; host `ComponentHandlerEspressoEngine` boots contexts, parses definition JSON, rebuilds the definition with `ComponentDsl`, and exposes a `HostContextBridge` for log/http callbacks.

**Tech Stack:** GraalVM Polyglot 25.0.3 + `org.graalvm.espresso:polyglot:25.0.3` (guest API), Jackson, ComponentDsl.

**Spec:** `docs/superpowers/specs/2026-07-04-espresso-custom-component-sandbox-design.md`

## Global Constraints

- EE license header + `@version ee` on every file under `server/ee/`.
- Blank-line-before-control-statement and variable-modification spacing rules; camelCase test names; no `TODO:` comments; `Test` suffix only.
- Espresso contexts: `allowCreateThread(true)`, `allowNativeAccess(true)`, `allowIO(IOAccess.ALL)`, `allowPolyglotAccess(PolyglotAccess.ALL)`, `option("java.Polyglot", "true")`, `option("java.Classpath", ...)`. Never `allowAllAccess`; host access stays `HostAccess.EXPLICIT` (default) — only `@HostAccess.Export` methods reachable.
- `tasks.test { enableAssertions = false }` in any module whose tests boot Espresso (its internal asserts fire under `-ea`).
- New `bytechef.*` property MUST be a field in `ApplicationProperties` (strict binding, `ignoreUnknownFields = false`).
- Verification: `./gradlew spotlessApply` + module `check` tasks; Espresso E2E validated in a linux Docker container (local darwin-aarch64 cannot boot embedded Espresso).

---

### Task 1: Guest bridge module skeleton + `GuestParameters`

**Files:**
- Modify: `gradle/libs.versions.toml` — add `org-graalvm-espresso-polyglot = { module = "org.graalvm.espresso:polyglot", version.ref = "graalvm" }` next to the other graalvm entries; add Jackson alias reference if one does not already exist (check for an existing `com-fasterxml-jackson` alias first and reuse it).
- Modify: `settings.gradle.kts` — `include("server:ee:libs:platform:platform-custom-component:platform-custom-component-guest-bridge")` sorted within the platform-custom-component block.
- Create: `server/ee/libs/platform/platform-custom-component/platform-custom-component-guest-bridge/build.gradle.kts` — `api(project(":sdks:backend:java:component-api"))`, `implementation` Jackson databind, `compileOnly` espresso polyglot.
- Create: `.../guest/GuestParameters.java` (package `com.bytechef.ee.platform.customcomponent.guest`) — implements ALL `Parameters` methods over a `Map<String, ?>`:
  - Backing: `Map<String, ?> map` + a shared static `ObjectMapper` (configured with `JavaTimeModule` if available on classpath — plain registration via `findAndRegisterModules()`).
  - Scalar getters via a private `<T> T convert(Object value, Class<T> type)` using `objectMapper.convertValue`; date/time getters parse ISO strings via the same conversion; `getRequired*` throw `IllegalArgumentException("Required parameter '%s' is missing")` when absent.
  - `getFromPath`/`getMapFromPath`/`containsPath`: implement simple dot-path traversal (split on `.`, walk maps/list indices); TypeReference variants convert via `objectMapper.convertValue(value, objectMapper.constructType(typeReference.getType()))` — check `TypeReference` in component-api for its type-accessor method and adapt.
  - `getFileEntry`/`getFileEntries`/`getRequiredFileEntry` throw `UnsupportedOperationException("File entries are not supported in the Espresso sandbox; set bytechef.component.custom-component.java-loader=class-loader")`.
  - `toMap()` returns the backing map.
- Test: `.../guest/GuestParametersTest.java` — coercions (string→long, int→double, ISO string→LocalDateTime), defaults, required-missing throws, list/map with element types, dot-path access, toMap.

**Interfaces:**
- Produces: `GuestParameters(Map<String, ?>)` constructor + full `Parameters` contract. Task 4 consumes.

**Steps:** write failing test → run (module must compile once skeleton exists) → implement → green → commit `Add custom component guest bridge module with GuestParameters`.

---

### Task 2: `HostBridge` seam + `GuestActionContext` (json/encoder/converter/log + UOE stubs)

**Files:**
- Create: `.../guest/HostBridge.java` — guest-side **interface** mirroring the host's exported methods:

```java
public interface HostBridge {

    String httpExecute(String requestJson);

    boolean isEditorEnvironment();

    void log(String level, String message, String exceptionMessage);
}
```

- Create: `.../guest/GuestActionContext.java` — implements `ActionContext`:
  - Constructor `(HostBridge hostBridge, String traceId)`.
  - `json(fn)` → guest-local `GuestJson` (Jackson: `read` variants incl. the JSONPath-taking overloads throw `UnsupportedOperationException` for path-based reads in v1 unless trivially implementable via dot-walk; `write` → `writeValueAsString`).
  - `encoder(fn)` → guest-local Base64 impl of all 6 methods.
  - `converter(fn)` → `objectMapper.convertValue`-backed `canConvert`/`value`; `string(String)` returns the input.
  - `log(consumer)` → `GuestLog` implementing all 15 methods, formatting `String.format`-style args (`{}` SLF4J placeholders replaced sequentially) and delegating to `hostBridge.log(level, message, exception == null ? null : exception.toString())`.
  - `getTraceId()` returns the constructor value; `isEditorEnvironment()` delegates to the bridge.
  - `http(fn)` wired in Task 3; until then throws.
  - `file`/`xml`/`mimeType`/`outputSchema`/`data`/`event`/`approval`/`suspend`/`escaper` → `UnsupportedOperationException` naming the flag.
  - `ContextFunction`/`ContextConsumer` invocation: wrap checked exceptions in `RuntimeException`.
- Test: `.../guest/GuestActionContextTest.java` — json read/write round-trip, encoder base64, converter, log captures level+formatted message through a recording fake `HostBridge`, unsupported areas throw with flag-naming message.

**Interfaces:**
- Produces: `GuestActionContext(HostBridge, String)`; `HostBridge` shape that Task 5's host export set must match method-for-method.

**Steps:** failing test → implement → green → commit `Add guest ActionContext adapter with host bridge seam`.

---

### Task 3: Guest HTTP adapter (request DTO → host callback → Response wrapper)

**Files:**
- Create: `.../guest/GuestHttp.java` — implements `Context.Http`:
  - `get/post/put/patch/delete/head/exchange` return a `GuestExecutor` accumulating url, method, headers (`Map<String,List<String>>`), query params, `Http.Body`, and configuration (from `Http.ConfigurationBuilder.build()` — read supported fields: responseType, timeout, allowUnauthorizedCerts, followRedirect, followAllRedirects, disableAuthorization; inspect the built `Configuration` getters in component-api and copy what exists).
  - `execute()` → serialize `HttpRequestDto` record to JSON (Jackson): `{method, url, headers, queryParameters, body: {content, contentType, mimeType}, configuration: {...}}`. Body content types v1: `JSON` (content serialized as JSON value), `RAW` (string), `FORM_URL_ENCODED` (map). `BINARY`/`FORM_DATA` → `UnsupportedOperationException`.
  - Response JSON `{statusCode, headers, body}` (body = raw JSON value or string) → `GuestResponse` implementing `Http.Response` (`getBody(Class)` via `convertValue`; `getFirstHeader`/`getHeader` helpers).
- Modify: `GuestActionContext.http(fn)` → `fn.apply(new GuestHttp(hostBridge))`.
- Test: extend `GuestActionContextTest` or new `GuestHttpTest` — fake `HostBridge` capturing request JSON and returning a canned response; assert DTO fields for a `post(url).header(...).queryParameter(...).body(Body.of(Map.of(...)))` chain and response accessors.

**Interfaces:**
- Produces: request/response JSON shape (documented above) that Task 5's `HostContextBridge.httpExecute` must parse/produce.

**Steps:** failing test → implement → green → commit `Add guest HTTP adapter for sandboxed components`.

---

### Task 4: `GuestComponentBridge` (describe + execute entrypoints)

**Files:**
- Create: `.../guest/GuestComponentBridge.java` — static entrypoints invoked by the host over interop:

```java
public static String describeComponent(String implClassName)
public static String executeActionPerform(
    String implClassName, String actionName, String inputParametersJson,
    String connectionParametersJson, String traceId)
```

  - `describeComponent`: `Class.forName(implClassName).getDeclaredConstructor().newInstance()` → `ComponentHandler.getDefinition()` → walk to a `ComponentDto` (records defined in `.../guest/dto/`): name, title, description, version; actions (name/title/description + `PropertyDto` tree); triggers (name/title/description/type name + properties); `unsupported` list collecting dropped features (dynamic options, dynamic properties, connection, cluster elements — detected and named). `PropertyDto` fields: name, type (enum name), label, description, required, advancedOption, hidden, displayCondition, expressionEnabled, defaultValue, exampleValue, options (`OptionDto` label/value/description), minValue/maxValue (integer/number), properties (children), items (array). Walk uses only interface getters (`Property.*Property` casts by `getType()` switch). Serialize with Jackson.
  - `executeActionPerform`: instantiate handler, find action by name, `actionDefinition.getPerform()` → cast to the single-abstract `PerformFunction` (same cast pattern the platform uses; if absent throw `IllegalArgumentException`), build `GuestParameters` from each JSON (`readValue(..., Map.class)`), build `GuestActionContext(importHostBridge(), traceId)`, invoke, serialize result to JSON (null → `"null"`).
  - `importHostBridge()`: isolated in one small method —

```java
private static HostBridge importHostBridge() {
    Object imported = com.oracle.truffle.espresso.polyglot.Polyglot.importObject("byteChefHostBridge");

    return com.oracle.truffle.espresso.polyglot.Polyglot.cast(HostBridge.class, imported);
}
```

    Guard: when the polyglot API is unavailable (plain-JVM unit tests), a package-private static `HostBridge hostBridgeOverride` field takes precedence — tests set it; production leaves it null.
- Test: `.../guest/GuestComponentBridgeTest.java` — a sample `ComponentHandler` built with `ComponentDsl` (string/integer/object properties, static options, one trigger, a perform that reads parameters + calls `context.log` and `context.http` against the override fake bridge and returns a map); assert `describeComponent` JSON content (parse with Jackson, check fields incl. `unsupported`) and `executeActionPerform` result JSON.

**Interfaces:**
- Produces: the two static entrypoints + the definition JSON schema Task 5 parses; polyglot binding name `byteChefHostBridge`.

**Steps:** failing test → implement → green → commit `Add guest component bridge entrypoints`.

---

### Task 5: Host side — `ComponentHandlerEspressoEngine` + guest classpath bundling + `HostContextBridge`

**Files:**
- Modify: `server/ee/libs/platform/platform-custom-component/platform-custom-component-loader/build.gradle.kts`:
  - `guestBridge` configuration → `guestBridge(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-guest-bridge"))` (transitively brings component-api + Jackson jars); `processResources` copies into `META-INF/guest-sdk/custom-component/` + `index.txt` (same pattern as code workflows); `tasks.test { enableAssertions = false }`; add Jackson databind implementation dep for host-side parsing.
- Create: `.../loader/GuestBridgeClasspath.java` — copy of the code-workflow `GuestSdkClasspath` pattern with prefix `META-INF/guest-sdk/custom-component/`.
- Create: `.../loader/HostContextBridge.java` — final class, constructor `(ActionContext actionContext)`:

```java
@HostAccess.Export
public String httpExecute(String requestJson) { ... }

@HostAccess.Export
public boolean isEditorEnvironment() { ... }

@HostAccess.Export
public void log(String level, String message, String exceptionMessage) { ... }
```

  - `httpExecute`: Jackson-parse the request DTO; build the call via the REAL `actionContext.http(http -> http.exchange(url, method)...)` applying headers/query params/body/configuration; serialize `{statusCode, headers, body}` back. Body reconstruction: contentType JSON → `Body.of((Map) content, BodyContentType.JSON)` / list variant; RAW → `Body.of(string)`; FORM_URL_ENCODED → map variant.
  - `log`: switch on level → `actionContext.log(log -> log.info(message))` etc.; append exceptionMessage when present.
- Create: `.../loader/ComponentHandlerEspressoEngine.java`:
  - `static ComponentHandler load(Path jarPath)` — service entry `META-INF/services/com.bytechef.component.ComponentHandler` read host-side (reuse the pattern from `ProjectHandlerPolyglotEngine.readServiceImplementationClassName`); icon read host-side from jar entry `assets/sample.svg` (nullable); context per call; invoke `describeComponent`; Jackson-parse; assemble with `ComponentDsl`: `component(name).title(...).version(...).actions(...)` — each action a `ModifiableActionDefinition` via `action(name)` + `.properties(toDslProperties(...))` + `.perform((inputParameters, connectionParameters, context) -> executePerform(...))`; triggers via `trigger(name)` metadata + properties (no execution functions). Wrap final definition to add the icon (`AbstractComponentDefinitionWrapper` subclass, same as classloader path).
  - `toDslProperties(List<PropertyDto>)`: switch on type name → `ComponentDsl.string/integer/number/bool/date/dateTime/time/object/array/fileEntry` with declarative fields + recursive children; static options via `ComponentDsl.option(label, value, description)` (value typed by property kind).
  - `static Object executePerform(...)`: fresh context; `polyglotContext.getPolyglotBindings().putMember("byteChefHostBridge", new HostContextBridge(context))`; input/connection `Parameters.toMap()` → Jackson JSON; invoke `executeActionPerform`; parse result JSON to Object (Map/List/scalar/null).
  - Context builder: options per Global Constraints, classpath = uploadedJar + `GuestBridgeClasspath.get()`; same `IllegalStateException` platform message as the code-workflow engines; shared static `Engine`.
- Test: `.../loader/ComponentHandlerEspressoEngineDslAssemblyTest.java` — host-side only: feed a hand-written definition JSON string into the parsing/assembly method (make it package-private static taking the JSON + icon) and assert the resulting `ComponentDefinition` (properties types/labels/options, action perform present, trigger metadata). No Espresso needed.

**Interfaces:**
- Consumes: JSON schemas + binding name from Tasks 3-4.
- Produces: `ComponentHandlerEspressoEngine.load(Path)` for Task 6.

**Steps:** failing assembly test → implement → green → commit `Add Espresso engine for custom component loading`.

---

### Task 6: Flag + wiring (`ApplicationProperties`, loader switch, facade/registry)

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` — inside the existing `Component` class add nested `CustomComponent` with `private JavaLoader javaLoader = JavaLoader.ESPRESSO;` and `public enum JavaLoader { CLASS_LOADER, ESPRESSO }` + getters/setters, plus `private CustomComponent customComponent = new CustomComponent();` accessor pair on `Component`.
- Modify: `.../loader/ComponentHandlerLoader.java`:

```java
public static ComponentHandler loadComponentHandler(
    URL url, Language language, JavaLoader javaLoader, String cacheKey, CacheManager cacheManager) {

    try {
        return switch (language) {
            case JAVA -> javaLoader == JavaLoader.ESPRESSO
                ? ComponentHandlerEspressoEngine.load(toLocalPath(url))
                : loadJavaComponentHandler(url, cacheKey, cacheManager);
            case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotComponentHandler(url, language);
        };
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

  plus a `toLocalPath` helper (same as the code-workflow loaders). Add `app-config` dependency to the loader module for the enum (check module dependency direction: if `app-config` is too heavy for the loader, define the enum in the loader and map in callers — prefer the loader-owned enum `ComponentHandlerLoader.JavaLoader` and map from `ApplicationProperties.Component.CustomComponent.JavaLoader` in the callers to keep the loader Spring-free).
- Modify: `CustomComponentFacadeImpl` (2 call sites) and `CustomComponentDynamicComponentHandlerRegistry` (1 call site) — inject `ApplicationProperties`, map the enum, pass through.
- Test: loader-level test asserting JAVA + `CLASS_LOADER` still routes to the classloader (existing jar fixture via classloader loads — build fixture with `ComponentDsl` compiled at test runtime like other fixtures, or simpler: assert Espresso branch selected for `ESPRESSO` via exception-on-missing-service for a jar without service entry, and classloader branch selected via its distinct error). Config-binding test not required (strict binding fails app start if wrong; `compileJava` + existing app config tests cover).

**Steps:** failing test → implement → green → run `./gradlew compileJava` (whole server; catches the changed loader signature call sites) → commit `Gate Java custom component loading behind java-loader flag`.

---

### Task 7: Espresso end-to-end fixture test + full verification

**Files:**
- Test: `.../loader/ComponentHandlerEspressoEngineTest.java` — fixture handler source compiled at test runtime against `component-api` (CodeSource classpath trick from the code-workflow tests), defining one action with a string property + perform that reads `inputParameters.getRequiredString("name")`, logs, and returns a greeting map; packaged with `META-INF/services/com.bytechef.component.ComponentHandler` + `assets/sample.svg`. Assert definition (name/version/title, property, icon) and `perform` result through a real host `ActionContext` mock (Mockito: `http`/`log` lambdas invoked with recording fakes). Espresso-availability assumption guard (shared helper duplicated from code-workflow tests).
- Verification steps:
  1. `./gradlew spotlessApply`
  2. `check` on: guest-bridge, custom-component loader, app-config, custom-component-configuration-service, custom-component-handler modules
  3. `./gradlew compileJava`
  4. Docker linux container run of the guest-bridge tests + both Espresso E2E suites (code-workflow + custom-component):
     `docker run --rm --platform linux/amd64 -v "$PWD":/work -v bytechef-gradle-cache:/root/.gradle -w /work eclipse-temurin:25-jdk ./gradlew --no-daemon <module>:test` (try native arm64 first; fall back to amd64 if Espresso needs bitcode libs).
- Commit `Add Espresso end-to-end coverage for sandboxed custom components` + any formatting fallout.
