# Dual-transport MCP servers (Streamable HTTP + SSE) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve each of the three MCP servers (management, automation, embedded) over both Streamable HTTP (always on) and HTTP+SSE (gated by a config flag), backed by the same transport-agnostic tool filtering.

**Architecture:** `FilterableMcpAsyncServer` already builds transport-agnostic request/notification handlers; only its session-factory wiring is streamable-specific. Split "build handlers" from "attach to a transport", then attach the same core to both a `WebMvcStreamableServerTransportProvider` and a `WebMvcSseServerTransportProvider`. Each app-type config gains an SSE provider bean + router + security allow-list entry, gated by `bytechef.ai.mcp.server.sse.enabled` (default true).

**Tech Stack:** Java 25, Spring Boot 4, MCP SDK 2.0.0 (`mcp-core`, `mcp-spring-webmvc`), JUnit 5, Spring GraphQL test / MockMvc.

## Spike outcome (Task 1) — REVISED APPROACH

The Task 1 spike (resolved by reading `WebMvcSseServerTransportProvider` source) found the shared-provider-with-templated-endpoint approach **not viable**: `buildEndpointUrl` advertises `baseUrl + messageEndpoint + "?sessionId=…"` using `messageEndpoint` **verbatim** (no path-variable substitution), and the `McpTransportContext` is extracted **only** on the `/message` request (`handleSseConnection` never calls the `contextExtractor`). So per-secret multi-tenancy requires **one `WebMvcSseServerTransportProvider` per secretKey** with *concrete* endpoints.

Revised design (supersedes Tasks 4-7 below where they conflict):

- Keep Task 2 (property) and Task 3 (`FilterableMcpAsyncServer.attachStreamable`/`attachSse`) as written — the core split is still needed; the registry calls `attachSse` per provider.
- **Drop the builder `sseTransportProvider(...)` method** (old Task 4). The config builds the filtering core (streamable attached, as today) and passes that core instance to a registry, which calls `core.attachSse(perSecretProvider)`.
- **New Task 4 — `McpSseProviderRegistry`** (in `platform-mcp-server-support`): given a `Function<String, WebMvcSseServerTransportProvider>` provider factory, it lazily creates one provider per secretKey (bounded Caffeine cache, `maximumSize` + `expireAfterAccess`, removal listener calls the provider's graceful shutdown) and exposes `ServerResponse route(ServerRequest request, String secretKey)` that delegates to the per-secret provider's `getRouterFunction()`.
- **Tasks 5-7 (configs)** register a single dispatcher `RouterFunction` per app-type (gated), e.g. `GET /api/automation/{secretKey}/sse` and `POST /api/automation/{secretKey}/message`, both delegating to `registry.route(request, request.pathVariable("secretKey"))`. The per-secret provider factory builds a provider with concrete endpoints `/api/automation/<secret>/sse` + `/message`, `keepAliveInterval(30s)`, the app-type `contextExtractor` (may hardcode the secret from the closure), and `core.attachSse(provider)`. Management builds a standard `McpServer.async(provider)` per secret instead of the filtering core.
- **Task 8 (security)** unchanged (broaden the regex to `(mcp|sse|message)`).
- **Task 9 (int test)** unchanged in intent; drives `/api/automation/{secret}/sse` → advertised `/message` → `tools/list`.

## Global Constraints

- New `bytechef.*` property MUST be a field on the central `ApplicationProperties` (`server/libs/config/app-config/.../ApplicationProperties.java`).
- Streamable HTTP transport is NEVER gated; only SSE is gated. Default is both-on (`matchIfMissing = true`).
- Keep-alive stays OFF on streamable (issue #5389); enable it only on SSE.
- Apache 2.0 license header on all touched CE files; EE files (`server/ee/**`) use the ByteChef Enterprise header + `@version ee` (embedded config only).
- Do not duplicate the filtering logic — one core, two wirings.
- Blank-line-before-control-statement and no-`_`-prefix Java style rules apply.

---

### Task 1: SPIKE — verify SSE context threading and advertised message endpoint

**Why first:** The whole config approach depends on two unknowns: (a) does `WebMvcSseServerTransportProvider` advertise a usable `messageEndpoint` when the endpoint path contains a `{secretKey}` path variable, and (b) does the `McpTransportContext` (secret) captured by the `contextExtractor` on the `/sse` GET reach the tool filter on the `/message` POST? If both hold, one shared SSE provider per app-type works (Tasks 4-6 as written). If not, we fall back to a per-secret provider registry (documented at the end of this task).

**Files:**
- Create: `server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/SseTransportContextSpikeIntTest.java`

**Interfaces:**
- Consumes: `WebMvcSseServerTransportProvider` (`org.springframework.ai.mcp.server.webmvc.transport`), `McpServerTransportProvider.setSessionFactory(McpServerSession.Factory)`, `McpServerSession` ctor `(String id, Duration, McpServerTransport, McpInitRequestHandler, Map<String,McpRequestHandler<?>>, Map<String,McpNotificationHandler>, Supplier<Mono<Void>>, JsonSchemaValidator)`.
- Produces: a documented decision — "shared provider works" or "need per-secret providers" — recorded in a comment at the top of the spike test and referenced by Tasks 4-6.

- [ ] **Step 1: Write a MockMvc-based spike test**

Stand up a minimal Spring MVC context with a `WebMvcSseServerTransportProvider` whose `sseEndpoint("/mcp-spike/{secretKey}/sse")` and `messageEndpoint("/mcp-spike/{secretKey}/message")` carry a path variable, and a `contextExtractor` that reads the `secretKey` path variable into `McpTransportContext`. Install a session factory whose `tools/list` handler returns the value of `exchange.transportContext().get("secretKey")` as a single tool name.

```java
// Register the provider's getRouterFunction() as a RouterFunction bean.
// 1. GET /mcp-spike/abc123/sse  -> capture the SSE 'endpoint' event body.
// 2. Assert the advertised message endpoint contains "abc123" (NOT the literal "{secretKey}").
// 3. POST initialize + notifications/initialized + tools/list to the advertised endpoint.
// 4. Assert the returned tool name equals "abc123" (secret threaded from /sse to /message).
```

- [ ] **Step 2: Run the spike**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:test --tests "*SseTransportContextSpikeIntTest"`
Expected: either PASS (shared provider viable) or a clear failure showing the advertised endpoint contains `{secretKey}` literally, or the secret is null on `/message`.

- [ ] **Step 3: Record the decision**

Add a top-of-file comment: `// SPIKE RESULT (2026-07-14): shared SSE provider per app-type is viable — advertised endpoint substitutes the real secret and transportContext threads to /message.` OR `// SPIKE RESULT: NOT viable — Tasks 4-6 must use the per-secret provider registry fallback (see plan Task 1).`

**Fallback if the spike fails (per-secret provider registry):** instead of one SSE provider bean per app-type, add a `McpSseProviderRegistry` that lazily creates one `WebMvcSseServerTransportProvider` per `secretKey` with concrete (non-templated) `sseEndpoint`/`messageEndpoint`, each attached to the same filtering core, and a dispatcher `RouterFunction` that routes `/api/{base}/{secretKey}/sse|message` to the matching provider's router. If the spike passes, ignore this fallback.

- [ ] **Step 4: Commit**

```bash
git add server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/SseTransportContextSpikeIntTest.java
git commit -m "5393 Spike: verify MCP SSE transport context threading"
```

---

### Task 2: Add the `bytechef.ai.mcp.server.sse.enabled` property

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (the `Ai.Mcp.Server` static class, ~line 1461)

**Interfaces:**
- Produces: `applicationProperties.getAi().getMcp().getServer().getSse().isEnabled()` (boolean, default `true`). Property path `bytechef.ai.mcp.server.sse.enabled`.

- [ ] **Step 1: Add a nested `Sse` class with `enabled` (default true) to `Ai.Mcp.Server`**

Inside `public static class Server` (after the existing `enabled` field/accessors), add:

```java
            private Sse sse = new Sse();

            public Sse getSse() {
                return sse;
            }

            public void setSse(Sse sse) {
                this.sse = sse;
            }

            /**
             * HTTP+SSE transport settings. The Streamable HTTP transport is always enabled; SSE is opt-out.
             */
            public static class Sse {

                /**
                 * Whether the MCP servers additionally expose the HTTP+SSE transport.
                 */
                private boolean enabled = true;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
```

- [ ] **Step 2: Compile app-config**

Run: `./gradlew :server:libs:config:app-config:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "5393 Add bytechef.ai.mcp.server.sse.enabled property"
```

---

### Task 3: Split `FilterableMcpAsyncServer` into build-core + attach

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-server-support/src/main/java/com/bytechef/platform/mcp/server/FilterableMcpAsyncServer.java`
- Test: `server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/FilterableMcpAsyncServerTest.java`

**Interfaces:**
- Consumes: `McpStreamableServerTransportProvider`, `McpServerTransportProvider`, `DefaultMcpStreamableServerSessionFactory`, `McpServerSession`, `McpInitRequestHandler`.
- Produces:
  - `FilterableMcpAsyncServer(McpJsonMapper, Implementation, ServerCapabilities, String instructions, Duration requestTimeout, JsonSchemaValidator, boolean validateToolInputs, Function<...> toolFilter, List<AsyncResourceSpecification> resources, List<String> protocolVersions)` — builds handler maps, attaches nothing.
  - `void attachStreamable(McpStreamableServerTransportProvider provider)`
  - `void attachSse(McpServerTransportProvider provider)`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testAttachStreamableInstallsSessionFactory() {
    McpStreamableServerTransportProvider provider = mock(McpStreamableServerTransportProvider.class);
    when(provider.protocolVersions()).thenReturn(List.of("2024-11-05"));

    FilterableMcpAsyncServer server = new FilterableMcpAsyncServer(
        McpJsonDefaults.getMapper(), new McpSchema.Implementation("t", "1"),
        McpSchema.ServerCapabilities.builder().tools(true).build(), null, Duration.ofSeconds(10),
        McpJsonDefaults.getSchemaValidator(), false, exchange -> List.of(), List.of(), List.of("2024-11-05"));

    server.attachStreamable(provider);

    verify(provider).setSessionFactory(any(McpStreamableServerSession.Factory.class));
}

@Test
void testAttachSseInstallsSessionFactory() {
    McpServerTransportProvider provider = mock(McpServerTransportProvider.class);

    FilterableMcpAsyncServer server = new FilterableMcpAsyncServer(
        McpJsonDefaults.getMapper(), new McpSchema.Implementation("t", "1"),
        McpSchema.ServerCapabilities.builder().tools(true).build(), null, Duration.ofSeconds(10),
        McpJsonDefaults.getSchemaValidator(), false, exchange -> List.of(), List.of(), List.of("2024-11-05"));

    server.attachSse(provider);

    verify(provider).setSessionFactory(any(McpServerSession.Factory.class));
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:test --tests "*FilterableMcpAsyncServerTest"`
Expected: FAIL (constructor signature mismatch / `attachSse` not defined).

- [ ] **Step 3: Refactor the constructor and add attach methods**

Change the constructor to drop the `transportProvider` parameter, add a trailing `List<String> protocolVersions` parameter, store `requestTimeout`, `requestHandlers`, `notificationHandlers`, `jsonSchemaValidator`, `protocolVersions` as fields, and REMOVE the in-constructor `transportProvider.setSessionFactory(...)` call. Then add:

```java
    private final Duration requestTimeout;
    private final Map<String, McpRequestHandler<?>> requestHandlers;
    private final Map<String, McpNotificationHandler> notificationHandlers;

    // in constructor, after building the maps:
    this.requestTimeout = requestTimeout;
    this.requestHandlers = prepareRequestHandlers();
    this.notificationHandlers = prepareNotificationHandlers();
    this.protocolVersions = protocolVersions;

    @SuppressFBWarnings("EI")
    public void attachStreamable(McpStreamableServerTransportProvider provider) {
        provider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(
            requestTimeout, this::asyncInitializeRequestHandler, requestHandlers, notificationHandlers,
            sessionId -> Mono.empty(), jsonSchemaValidator));
    }

    @SuppressFBWarnings("EI")
    public void attachSse(McpServerTransportProvider provider) {
        provider.setSessionFactory(sessionTransport -> new McpServerSession(
            UUID.randomUUID().toString(), requestTimeout, sessionTransport, this::asyncInitializeRequestHandler,
            requestHandlers, notificationHandlers, Mono::empty, jsonSchemaValidator));
    }
```

Add imports: `io.modelcontextprotocol.spec.McpServerSession`, `io.modelcontextprotocol.spec.McpServerTransportProvider`, `java.util.UUID`. Keep `protocolVersions` field (already present).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:test --tests "*FilterableMcpAsyncServerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-mcp/platform-mcp-server-support/src/main/java/com/bytechef/platform/mcp/server/FilterableMcpAsyncServer.java server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/FilterableMcpAsyncServerTest.java
git commit -m "5393 Split FilterableMcpAsyncServer into build-core plus attach"
```

---

### Task 4: Teach `FilterableMcpServerBuilder` to attach to both transports

**Files:**
- Modify: `server/libs/platform/platform-mcp/platform-mcp-server-support/src/main/java/com/bytechef/platform/mcp/server/FilterableMcpServerBuilder.java`
- Test: `server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/FilterableMcpServerBuilderTest.java`

**Interfaces:**
- Consumes: Task 3's `FilterableMcpAsyncServer` constructor + `attachStreamable`/`attachSse`.
- Produces:
  - `FilterableMcpServerBuilder(McpStreamableServerTransportProvider streamable)` — unchanged constructor (streamable required).
  - `FilterableMcpServerBuilder sseTransportProvider(McpServerTransportProvider sse)` — optional.
  - `FilterableMcpAsyncServer build()` — builds the core (sourcing `protocolVersions` from the streamable provider), calls `attachStreamable(streamable)`, and `attachSse(sse)` when the SSE provider was set.

- [ ] **Step 1: Write the failing test**

```java
@Test
void testBuildAttachesBothWhenSseSet() {
    McpStreamableServerTransportProvider streamable = mock(McpStreamableServerTransportProvider.class);
    when(streamable.protocolVersions()).thenReturn(List.of("2024-11-05"));
    McpServerTransportProvider sse = mock(McpServerTransportProvider.class);

    new FilterableMcpServerBuilder(streamable)
        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
        .sseTransportProvider(sse)
        .build();

    verify(streamable).setSessionFactory(any(McpStreamableServerSession.Factory.class));
    verify(sse).setSessionFactory(any(McpServerSession.Factory.class));
}

@Test
void testBuildAttachesOnlyStreamableWhenSseAbsent() {
    McpStreamableServerTransportProvider streamable = mock(McpStreamableServerTransportProvider.class);
    when(streamable.protocolVersions()).thenReturn(List.of("2024-11-05"));

    new FilterableMcpServerBuilder(streamable)
        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
        .build();

    verify(streamable).setSessionFactory(any(McpStreamableServerSession.Factory.class));
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:test --tests "*FilterableMcpServerBuilderTest"`
Expected: FAIL (`sseTransportProvider` not defined).

- [ ] **Step 3: Add the SSE provider field + wire `build()`**

```java
    private McpServerTransportProvider sseTransportProvider;

    @SuppressFBWarnings("EI")
    public FilterableMcpServerBuilder sseTransportProvider(McpServerTransportProvider sseTransportProvider) {
        this.sseTransportProvider = sseTransportProvider;

        return this;
    }

    public FilterableMcpAsyncServer build() {
        FilterableMcpAsyncServer server = new FilterableMcpAsyncServer(
            McpJsonDefaults.getMapper(), serverInfo, serverCapabilities, instructions, requestTimeout,
            McpJsonDefaults.getSchemaValidator(), validateToolInputs, toolFilter, resourceSpecifications,
            transportProvider.protocolVersions());

        server.attachStreamable(transportProvider);

        if (sseTransportProvider != null) {
            server.attachSse(sseTransportProvider);
        }

        return server;
    }
```

Add import: `io.modelcontextprotocol.spec.McpServerTransportProvider`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:test --tests "*FilterableMcpServerBuilderTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-mcp/platform-mcp-server-support/src/main/java/com/bytechef/platform/mcp/server/FilterableMcpServerBuilder.java server/libs/platform/platform-mcp/platform-mcp-server-support/src/test/java/com/bytechef/platform/mcp/server/FilterableMcpServerBuilderTest.java
git commit -m "5393 FilterableMcpServerBuilder attaches to both transports"
```

---

### Task 5: Automation config — add gated SSE provider + router, attach filtering

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java`

**Interfaces:**
- Consumes: Task 4's `sseTransportProvider(...)`, the property from Task 2.
- Produces: beans `automationWebMvcSseServerTransportProvider` and `automationMcpSseRouterFunction` (gated), and `automationMcpAsyncServer` now attaches the SSE provider when present.

- [ ] **Step 1: Add a gated SSE transport provider bean**

Add (mirroring the streamable bean, same `contextExtractor`):

```java
    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.mcp.server.sse", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    WebMvcSseServerTransportProvider automationWebMvcSseServerTransportProvider() {
        return WebMvcSseServerTransportProvider.builder()
            .sseEndpoint("/api/automation/{secretKey}/sse")
            .messageEndpoint("/api/automation/{secretKey}/message")
            .keepAliveInterval(Duration.ofSeconds(30))
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.mcp.server.sse", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    RouterFunction<ServerResponse> automationMcpSseRouterFunction(
        WebMvcSseServerTransportProvider automationWebMvcSseServerTransportProvider) {
        return automationWebMvcSseServerTransportProvider.getRouterFunction();
    }
```

Re-add `import java.time.Duration;` and add `import org.springframework.ai.mcp.server.webmvc.transport.WebMvcSseServerTransportProvider;`, `import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;`.

> If the Task 1 spike chose the fallback, replace this bean pair with the `McpSseProviderRegistry` + dispatcher router described in Task 1, keyed by secretKey.

- [ ] **Step 2: Attach the SSE provider in the server bean**

Change `automationMcpAsyncServer(...)` to accept the optional SSE provider and pass it to the builder:

```java
    @Bean
    FilterableMcpAsyncServer automationMcpAsyncServer(
        /* existing params... */
        ObjectProvider<WebMvcSseServerTransportProvider> sseTransportProvider) {

        FilterableMcpServerBuilder builder = new FilterableMcpServerBuilder(
            automationWebMvcStreamableHttpServerTransportProvider())
            .serverInfo("automation-mcp-server", "1.0.0")
            .capabilities(/* unchanged */)
            .toolFilter(/* unchanged */);

        sseTransportProvider.ifAvailable(builder::sseTransportProvider);

        return builder.build();
    }
```

Add `import org.springframework.beans.factory.ObjectProvider;` (already present in this file per its imports).

- [ ] **Step 3: Compile**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java
git commit -m "5393 Expose automation MCP server over SSE"
```

---

### Task 6: Embedded config — add gated SSE provider + router, attach filtering

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/config/EmbeddedMcpServerConfiguration.java`

**Interfaces:**
- Consumes: Task 4's builder, Task 2's property.
- Produces: gated `embeddedWebMvcSseServerTransportProvider` + `embeddedMcpSseRouterFunction`, SSE attached to `embeddedMcpAsyncServer`.

- [ ] **Step 1: Mirror Task 5 for embedded**

Same two gated beans, using `sseEndpoint("/api/embedded/{secretKey}/sse")` and `messageEndpoint("/api/embedded/{secretKey}/message")`, and the embedded `contextExtractor` (which also sets `AUTHORITIES`, `ENVIRONMENT`, `EXTERNAL_USER_ID` from the request — copy the existing streamable `contextExtractor` body verbatim). EE header + `@version ee` already present; keep them.

- [ ] **Step 2: Attach SSE provider in `embeddedMcpAsyncServer`** (same `ObjectProvider<WebMvcSseServerTransportProvider>` + `ifAvailable` pattern as Task 5).

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/embedded/embedded-ai/embedded-ai-mcp-server/src/main/java/com/bytechef/ee/embedded/ai/mcp/server/config/EmbeddedMcpServerConfiguration.java
git commit -m "5393 Expose embedded MCP server over SSE"
```

---

### Task 7: Management config — add gated SSE provider + router (standard server)

**Files:**
- Modify: `server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java`

**Interfaces:**
- Consumes: the SDK's standard `McpServer.async(provider)`, Task 2's property.
- Produces: gated `webMvcSseServerTransportProvider` + `managementMcpSseRouterFunction` + a gated `managementMcpSseServer` built with `McpServer.async(sseProvider)` reusing the same tool registration as the existing streamable `mcpAsyncServer`.

- [ ] **Step 1: Add gated SSE provider + router** (`/api/management/{secretKey}/sse` + `/message`), mirroring Task 5's bean pair but with the management `contextExtractor`/endpoint.

- [ ] **Step 2: Add a gated SSE `McpAsyncServer`** built the same way as the existing `mcpAsyncServer` bean but passing the SSE provider. Extract the shared server-spec construction (tools/capabilities) into a private helper so streamable and SSE register identical tools (DRY).

- [ ] **Step 3: Compile**

Run: `./gradlew :server:libs:ai:ai-mcp:ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/ai/ai-mcp/ai-mcp-server/src/main/java/com/bytechef/ai/mcp/server/config/ManagementMcpServerConfiguration.java
git commit -m "5393 Expose management MCP server over SSE"
```

---

### Task 8: Permit SSE paths in the three security configurers

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/main/java/com/bytechef/automation/ai/mcp/server/security/web/configurer/AutomationMcpServerSecurityConfigurer.java`
- Modify: the management security configurer (`server/libs/ai/ai-mcp/.../security/.../ManagementMcpServerSecurityConfigurer.java` or equivalent)
- Modify: the embedded security configurer (`server/ee/libs/embedded/embedded-ai/.../security/.../EmbeddedMcpServerSecurityConfigurer.java`)

**Interfaces:**
- Consumes: `McpApiKeyHttpConfigurer`, the per-app `PATH_PATTERN` regex + `McpApiKeyAuthenticationConverter(base)`.

- [ ] **Step 1: Broaden the automation `PATH_PATTERN` regex to include sse + message**

```java
    private static final String PATH_PATTERN = "^/api/automation/.+/(mcp|sse|message)";
```

- [ ] **Step 2: Do the same for management and embedded**

Management: `"^/api/management/.+/(mcp|sse|message)"`. Embedded: `"^/api/embedded/.+/(mcp|sse|message)"`. (Locate the exact `PATH_PATTERN` constant in each; keep the existing `McpApiKeyAuthenticationConverter(base)` unchanged — the secret is still the path segment after the base.)

- [ ] **Step 3: Compile all three modules**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:compileJava :server:libs:ai:ai-mcp:ai-mcp-server:compileJava :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-mcp-server/.../AutomationMcpServerSecurityConfigurer.java server/libs/ai/ai-mcp/.../ManagementMcpServerSecurityConfigurer.java server/ee/libs/embedded/embedded-ai/.../EmbeddedMcpServerSecurityConfigurer.java
git commit -m "5393 Permit MCP SSE and message paths in security configurers"
```

---

### Task 9: Integration test — automation MCP over SSE end-to-end

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/AutomationMcpSseIntTest.java`

**Interfaces:**
- Consumes: the running automation SSE endpoints from Task 5.

- [ ] **Step 1: Write the end-to-end SSE test**

Boot the automation MCP server slice (mirror the existing streamable MCP int test's Spring setup), then: open `GET /api/automation/{secretKey}/sse`, read the `endpoint` event, POST `initialize` → `notifications/initialized` → `tools/list` to the advertised message endpoint with a known `secretKey`, and assert the tool set matches what `buildToolSpecifications` returns for that secret. Assert an unknown secret yields an empty tool list (parity with streamable filtering).

- [ ] **Step 2: Run it**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-mcp-server:testIntegration --tests "*AutomationMcpSseIntTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-mcp-server/src/test/java/com/bytechef/automation/ai/mcp/server/AutomationMcpSseIntTest.java
git commit -m "5393 Add automation MCP SSE integration test"
```

---

### Task 10: Full verification + docs

- [ ] **Step 1: Spotless + checks on all touched modules**

Run:
```bash
./gradlew spotlessApply
./gradlew :server:libs:platform:platform-mcp:platform-mcp-server-support:check :server:libs:config:app-config:checkstyleMain :server:libs:automation:automation-ai:automation-ai-mcp-server:check :server:libs:ai:ai-mcp:ai-mcp-server:check :server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server:check
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Document the property**

Add `bytechef.ai.mcp.server.sse.enabled` (default true) to `server/apps/server-app/src/main/resources/config/application-bytechef.yml` if MCP defaults are documented there, with a comment noting SSE is the legacy transport and can be disabled.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "5393 Format and document MCP SSE transport toggle"
```

## Self-Review Notes

- **Spec coverage:** property (Task 2 = spec §1), transport-agnostic core (Tasks 3-4 = spec §2), per-app wiring (Tasks 5-7 = spec §3), security (Task 8 = spec §4), keep-alive on SSE only (Tasks 5-7 bean = spec §5), testing (Tasks 3,4,9 = spec §Testing), the SSE-endpoint spike (Task 1 = spec §Top open question). All covered.
- **Ticket:** commits reference #5393 per the user's instruction to fix that issue with this work. Confirm #5393 is the correct tracking issue before pushing (see note in the execution handoff).
