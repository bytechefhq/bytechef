# Dual-transport MCP servers (Streamable HTTP + SSE)

**Date:** 2026-07-14
**Status:** Design approved, pending implementation plan

## Problem

ByteChef exposes three MCP servers — **management** (`ai-mcp-server`), **automation**
(`automation-ai-mcp-server`), and **embedded** (`embedded-ai-mcp-server`) — each over a single
**Streamable HTTP** transport (`WebMvcStreamableServerTransportProvider`). Some MCP clients only
speak the older **HTTP+SSE** transport. We want every MCP server reachable over both transports
concurrently, so a client can connect via whichever it supports.

Related context: keep-alive pings were disabled on the streamable transports (issue #5389) because
the MCP SDK's `KeepAliveScheduler` does not support server-initiated pings on Streamable HTTP
sessions (a streamable session has no persistent server→client stream unless the client holds an
open listening GET/SSE stream). Keep-alive **does** work for the classic SSE transport, which keeps
a persistent server-push stream for the session's lifetime.

## Goal

Each of the three MCP app-types serves the **same tools** over:

- **Streamable HTTP** — always on (the modern, primary transport). Unchanged.
- **HTTP+SSE** — on by default, gated by a config flag so it can be disabled where not needed.

Same secretKey multi-tenancy and the same per-request tool filtering must apply on both transports.

## Key finding: the tool filtering is transport-agnostic

`FilterableMcpAsyncServer` (in `platform-mcp/platform-mcp-server-support`) is ByteChef's
per-request, URL-secret tool filter — the core of how the automation and embedded servers scope
tools by `secretKey` carried in the `McpTransportContext`. The standard SDK `McpAsyncServer` serves
a single static tool list, so ByteChef composes the SDK's public primitives instead.

Inspection of the class shows that **all filtering logic is transport-agnostic**: the
`requestHandlers`/`notificationHandlers` maps, the init handler, capabilities, protocol versions,
`toolFilter`, and resource handlers operate only on `McpAsyncServerExchange`. The **only**
streamable-specific coupling is the session-factory wiring at the end of the constructor:

```java
transportProvider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(
    requestTimeout, this::asyncInitializeRequestHandler, requestHandlers, notificationHandlers,
    sessionId -> Mono.empty(), this.jsonSchemaValidator));
```

Both transports' session factories consume the **same inputs** (verified against SDK 2.0.0 sources):

- **Streamable:** `setSessionFactory(McpStreamableServerSession.Factory)` — built via
  `DefaultMcpStreamableServerSessionFactory(timeout, init, requestHandlers, notificationHandlers, …)`.
- **SSE:** `setSessionFactory(McpServerSession.Factory)` — where `Factory.create(transport)` returns
  `new McpServerSession(id, timeout, transport, init, requestHandlers, notificationHandlers, onClose)`.

`McpRequestHandler` and `McpNotificationHandler` are transport-agnostic. Therefore the same handler
maps and init handler feed **both** factories. Supporting SSE is a bounded extraction, not a fork or
a duplicated filter.

`WebMvcSseServerTransportProvider.Builder` mirrors the streamable builder: same
`contextExtractor(McpTransportContextExtractor<ServerRequest>)` (so secretKey/authorities extraction
ports directly), same `keepAliveInterval(Duration)`, plus `sseEndpoint(String)` +
`messageEndpoint(String)` for its two routes. No new dependency is required — both providers ship in
`mcp-spring-webmvc` (already on the classpath).

## Design

### 1. Config property

Add a boolean field to the existing `ApplicationProperties.Ai.Mcp` (per the strict-binding rule that
every `bytechef.*` property must be a field on the central `ApplicationProperties`):

- Property: `bytechef.ai.mcp.sse.enabled`
- Type: `boolean`, default `true`

SSE beans are registered with
`@ConditionalOnProperty(prefix = "bytechef.ai.mcp.sse", name = "enabled", havingValue = "true", matchIfMissing = true)`.
Streamable beans are never gated.

### 2. Transport-agnostic filtering core

Refactor `FilterableMcpAsyncServer` so the handler-building (request handlers, notification handlers,
init handler, capabilities, protocol versions, `toolFilter`, resources) is constructed independently
of any transport, then attached to a provider through one of two methods:

- `attachStreamable(McpStreamableServerTransportProvider provider)` →
  `provider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(…))` (today's behavior,
  preserved).
- `attachSse(WebMvcSseServerTransportProvider provider)` →
  `provider.setSessionFactory(transport -> new McpServerSession(id, timeout, transport, init,
  requestHandlers, notificationHandlers, onClose))`.

`FilterableMcpServerBuilder` builds the core once and attaches it to whichever providers are present
(streamable always, SSE when enabled). One core, two thin wirings — the filtering logic itself is not
duplicated or forked.

`protocolVersions` currently comes from the streamable provider (`transportProvider.protocolVersions()`).
The core must source protocol versions in a transport-agnostic way (from whichever provider(s) it is
attached to, or a shared constant) so it does not depend on a single provider instance.

The **management** server uses the SDK's standard `McpServer.async(provider)` (no filtering), so its
SSE support is a straightforward `McpServer.async(sseProvider)` alongside the existing streamable
server — it does not touch the filtering core.

### 3. Per-app-type wiring

Each of the three configurations gains, guarded by the flag:

- An SSE transport-provider bean (`WebMvcSseServerTransportProvider`) with `sseEndpoint` and
  `messageEndpoint` carrying the `{secretKey}` path variable and the **same** `contextExtractor` as
  the streamable provider.
- Its `RouterFunction<ServerResponse>` bean (registers the SSE + message routes).
- The filtering core (automation/embedded) or a standard `McpServer.async` (management) attached to
  the SSE provider.

Endpoint scheme (example, automation):

| Transport | Endpoint(s) |
| --- | --- |
| Streamable HTTP (unchanged) | `POST/GET /api/automation/{secretKey}/mcp` |
| HTTP+SSE | `GET /api/automation/{secretKey}/sse` + `POST /api/automation/{secretKey}/message` |

Management and embedded follow the same `/sse` + `/message` scheme under their respective base paths.

### 4. Security

Each app-type's security configurer (e.g. `AutomationMcpServerSecurityConfigurer`) must permit the
two new SSE paths with the same authorization as the existing streamable path.

### 5. Keep-alive per transport

- Enable `keepAliveInterval(30s)` on the **SSE** providers — functional there (persistent stream),
  keeps idle SSE streams warm.
- Keep it **off** on streamable (the #5389 fix stands).

The bounded `spring.mvc.async.request-timeout: 10m` applies to all async requests; SSE keep-alive
keeps the stream active within that window.

## Testing

- **Unit:** the extracted filtering core attaches to both session-factory types and filters
  identically; a `tools/list`/`tools/call` filter test independent of transport.
- **Integration (per app-type):** an SSE integration test — open the SSE stream with a `secretKey`,
  send `initialize` + `tools/list` on the message endpoint, assert the filtered tool set — mirroring
  the existing streamable integration tests.
- **Security:** assert the new SSE and message paths are permitted with a valid secret and rejected
  without.

## Top open question (spike during planning)

SSE advertises its message endpoint to the client in the initial `endpoint` SSE event. We must
confirm `WebMvcSseServerTransportProvider` substitutes the real `{secretKey}` (not the literal
template) into that advertised URL, and that the `contextExtractor` populates the secret per
`message` POST so per-request filtering works. The API surface supports the approach; this specific
path-variable/templating behavior needs a short spike. If the provider does not substitute path
variables into the advertised endpoint, the fallback is a per-secret provider instance or a small
routing shim that rewrites the advertised message URL — to be decided from the spike result.

## Non-goals

- No per-`McpServer`-entity transport field or per-entity transport enforcement (options B/C from
  brainstorming). Both transports are simply available; the client chooses.
- No change to per-client MCP protocol semantics — each client speaks one transport end-to-end.
- No change to existing streamable behavior beyond the internal `FilterableMcpAsyncServer` refactor.
