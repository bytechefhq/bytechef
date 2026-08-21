# Brave Web Search Tools

**Date:** 2026-08-07
**Status:** Approved, not yet implemented

## Problem

`FirecrawlTools` (`server/libs/platform/platform-ai/platform-ai-tool`) is the only server-side web
access the copilot and AI Hub agents have. It is gated on a single operator property
(`bytechef.ai.firecrawl.enabled`), so a deployment without a Firecrawl subscription gives its agents
no web search at all — and in AI Hub it loses the research subagent entirely, because
`ResearchConfiguration` is `@ConditionalOnBean(FirecrawlTools.class)`.

ByteChef already talks to the Brave Search API, but only from
`AiAgentUtilsBraveWebSearchTool` — a canvas cluster element that wraps the community
`org.springaicommunity.agent.tools.BraveWebSearchTool` and reads its key from a per-node
**connection**. That credential lifetime is wrong for the agent surfaces, which need a
deployment-wide singleton bean.

This spec adds `BraveWebSearchTools`: a second, independently-toggleable web search provider on the
agent surfaces, shaped like `FirecrawlTools`.

## Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Implementation | Hand-rolled `RestClient` | Mirrors `FirecrawlTools`; full control of the tool schema the LLM sees; no spring-ai-community dependency in `platform-ai-tool`. |
| Coexistence | Both registered side by side | Each provider is independently toggleable; tool descriptions distinguish them. |
| Endpoints | Web search only | Matches what Brave is good at; keeps per-iteration schema token cost small. |
| Research subagent | Boots on **either** provider | A Brave-only deployment still gets research, degraded to snippet-level synthesis. |
| Metering | Yes, as `brave_search` | Parity with `firecrawl_search` in `ai_hub_tool_usage`. |

Explicitly out of scope:

- No domain-filter parameter. Brave honours `site:` operators inside the query string.
- No change to `AiAgentUtilsBraveWebSearchTool`. The canvas cluster element keeps its per-connection
  credential; it is a different surface with a different credential lifetime.
- No news, image, or summarizer endpoints. The summarizer in particular needs a Pro-tier
  subscription and a two-call polling handshake.

## Component 1 — `BraveWebSearchTools`

New class in `com.bytechef.platform.ai.tool` (CE, `platform-ai-tool`), alongside `FirecrawlTools`.

```java
@Component
@ConditionalOnProperty(name = "bytechef.ai.brave.enabled")
public class BraveWebSearchTools {

    private final RestClient restClient;

    public BraveWebSearchTools(ApplicationProperties applicationProperties, RestClient.Builder restClientBuilder) {
        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        this.restClient = restClientBuilder
            .baseUrl(brave.getBaseUrl())
            .defaultHeader("X-Subscription-Token", brave.getApiKey())
            .defaultHeader("Accept", "application/json")
            .build();
    }

    @Tool(description = "...")
    public BraveSearchResult braveWebSearch(
        @ToolParam(description = "...") String query,
        @ToolParam(required = false, description = "...") Integer count,
        @ToolParam(required = false, description = "...") String country,
        @ToolParam(required = false, description = "...") String freshness) { ... }
}
```

### The method name is load-bearing

The method is named `braveWebSearch`, **not** `webSearch`. `ToolCallbacks.from(object)` derives the
tool name from the method name. Under the side-by-side decision both classes land on the same
`ChatClient`, and two callbacks named `webSearch` is a duplicate-tool-name failure at build time.

Naming the method distinctly — rather than keeping `webSearch` and overriding via
`@Tool(name = "braveWebSearch")` — also keeps `ResearchConfiguration`'s metering map unambiguous,
because that map keys off callback names.

### Wire protocol

Differences from `FirecrawlTools`, all of which the implementation must respect:

- `GET /web/search?q=…` with query parameters, not `POST` with a JSON body.
- `X-Subscription-Token` header, not `Authorization: Bearer`.
- `count` clamps to Brave's **1–20** (Firecrawl's search allows 1–100).
- Results live at `web.results[]`.

`freshness` accepts Brave's `pd` / `pw` / `pm` / `py` tokens or a `YYYY-MM-DDtoYYYY-MM-DD` range;
the tool passes it through unvalidated and lets Brave reject bad values.

### Result types

Public records carry `@JsonProperty` + `@JsonPropertyDescription` so the LLM sees a documented
schema, and `@SuppressFBWarnings("EI")` as `FirecrawlTools` does:

- `BraveSearchResult(String query, List<BraveSearchResultItem> results)`
- `BraveSearchResultItem(String title, String url, String description, String age)`

Package-private wire records are `@JsonIgnoreProperties(ignoreUnknown = true)` — Brave's payload is
far wider than what we want to expose:

- `BraveSearchResponse(WebResults web)`
- `WebResults(List<WebResult> results)`
- `WebResult(String title, String url, String description, String age)`

### Error handling

Same shape as `FirecrawlTools`: debug-log on entry and on result count, error-log with the
parameters, and rethrow as `ExecutionException` with a new error type.

New `com.bytechef.platform.ai.tool.exception.BraveToolErrorType extends AbstractErrorType` with a
single `WEB_SEARCH = new BraveToolErrorType(100)`. `AbstractErrorType` keys by class, so reusing
error key `100` does not collide with `FirecrawlToolErrorType`.

Missing or blank API key is **not** handled in the tool. The `@ConditionalOnProperty` gate means an
operator who sets `enabled: true` without a key gets a 401 from Brave surfaced as an
`ExecutionException` — the same failure mode Firecrawl already has.

## Component 2 — Configuration properties

`ApplicationProperties` binding is strict, so the class is mandatory, not optional.

Add `ApplicationProperties.Ai.Brave` modelled directly on `Ai.Firecrawl`:

| Field | Default |
| --- | --- |
| `apiKey` | none |
| `baseUrl` | `https://api.search.brave.com/res/v1` |
| `enabled` | `false` |

Add the `brave` field plus getter and setter on `Ai`, positioned alphabetically — before `copilot`,
matching how that class already orders its members.

Add to `server/apps/server-app/src/main/resources/config/application-bytechef.yml`, alphabetically
before `copilot`:

```yaml
bytechef:
  ai:
    brave:
      base-url: https://api.search.brave.com/res/v1
      enabled: false
```

## Component 3 — Agent wiring

Fourteen bean methods inject `Optional<FirecrawlTools>` today: twelve in `CopilotConfiguration`
(CE, `ai-copilot-service`) and two in `EmbeddedCopilotConfiguration` (EE, `embedded-ai-copilot`).
Every one of them gains an `Optional<BraveWebSearchTools>` parameter.

**Nine sites** already build a mutable list. They gain one line:

```java
firecrawlTools.ifPresent(tools::add);
braveWebSearchTools.ifPresent(tools::add);
```

**Five sites** — the ASK `*SubAgentChatClient` beans in `CopilotConfiguration`:
`codeEditorAskSubAgentChatClient`, `workflowEditorAskSubAgentChatClient`,
`clusterElementAskSubAgentChatClient`, `skillsAskSubAgentChatClient`, and
`workflowExecutionAskSubAgentChatClient` — instead branch:

```java
if (firecrawlTools.isPresent()) {
    builder.defaultTools(a, b, c, firecrawlTools.get());
} else {
    builder.defaultTools(a, b, c);
}
```

A second `Optional` turns that into four branches. Convert these five to the list-building shape the
other nine already use:

```java
List<Object> tools = new ArrayList<>(List.of(a, b, c));

firecrawlTools.ifPresent(tools::add);
braveWebSearchTools.ifPresent(tools::add);

builder.defaultTools(tools.toArray());
```

`ChatClient.Builder.defaultTools(Object...)` accepts this directly. This is a targeted cleanup in
code the change already touches, not a drive-by refactor of unrelated call sites.

## Component 4 — `ResearchConfiguration` (EE, ai-hub)

### Conditional

`@ConditionalOnBean` **ANDs** its values; there is no `anyOf`. "Either provider" needs the
`AnyNestedCondition` idiom:

```java
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Conditional(ResearchConfiguration.OnAnyWebSearchToolsCondition.class)
public class ResearchConfiguration {

    static class OnAnyWebSearchToolsCondition extends AnyNestedCondition {

        OnAnyWebSearchToolsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(FirecrawlTools.class)
        static class OnFirecrawlTools {
        }

        @ConditionalOnBean(BraveWebSearchTools.class)
        static class OnBraveWebSearchTools {
        }
    }
}
```

`REGISTER_BEAN` is required: bean-presence conditions cannot be evaluated during configuration-class
parsing.

### Bean method

`researchChatClient` takes `Optional<FirecrawlTools>` and `Optional<BraveWebSearchTools>` in place of
the current mandatory `FirecrawlTools`. It concatenates whichever `ToolCallbacks.from(...)` arrays
exist and runs the existing metering loop over the union — the loop body does not change.

### Metering map

`mapFirecrawlToolName` is renamed `mapMeteredToolName` and gains one case:

```java
return switch (toolName) {
    case "webSearch" -> "firecrawl_search";
    case "webpageScrape" -> "firecrawl_scrape";
    case "braveWebSearch" -> "brave_search";
    default -> null;
};
```

The argument-field extractor needs no change: it already defaults to `"query"` for anything that is
not `websiteMap` or `webpageScrape`, and Brave's parameter is `query`.

### Known degradation

A Brave-only deployment can **search but not fetch pages**. The research subagent boots and works,
but synthesises from search snippets rather than page bodies. This is the accepted trade of the
"boot on either provider" decision. It is documented in `prompt_research.txt` rather than left
silent, so the model states its own limitation instead of over-claiming.

## Component 5 — Prompts

| File | Change |
| --- | --- |
| `ai-copilot-service/src/main/resources/prompt_research.txt` | "You have Firecrawl tools…" → provider-neutral, plus the snippet-only caveat when no scrape tool is present. |
| `ai-hub-service/src/main/resources/prompt_ai_hub_build.txt` | Line 77 names Firecrawl in the research tool description; make it provider-neutral. |
| `ai-copilot-service/src/main/resources/prompt_workflow_editor_ask.txt` | Line 25 lists `webSearch, websiteMap and webpageScrape`; add `braveWebSearch`. |
| `embedded-ai-copilot/src/main/resources/prompt_workflow_editor_embedded_ask.txt` | Line 29, same change. |

## Testing

**New `BraveWebSearchToolsTest`** (`platform-ai-tool`), using `MockRestServiceServer` bound to the
`RestClient.Builder` handed to the constructor:

- Happy path — request hits `/web/search` with `q`, and results map to `BraveSearchResultItem`.
- `count` clamping — `50` is sent as `20`, `0` as `1`.
- Optional parameters — `country` and `freshness` are omitted from the query string when null.
- Empty `web.results` returns an empty list, not null.
- Null response body returns an empty result rather than throwing.
- HTTP error maps to `ExecutionException` carrying `BraveToolErrorType.WEB_SEARCH`.
- Tool name — `ToolCallbacks.from(braveWebSearchTools)` yields a callback named `braveWebSearch`,
  pinning the collision-avoidance decision above.

**`ResearchConfigurationTest`** (ai-hub) updates to the new signature and gains cases for
Firecrawl-only, Brave-only, and both — each asserting the client builds.

**No integration test.** Both classes are thin HTTP adapters over a third-party API; a Testcontainers
run would add nothing that `MockRestServiceServer` does not already cover.

## Verification

- `./gradlew spotlessApply`
- `./gradlew :server:libs:platform:platform-ai:platform-ai-tool:test`
- `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test`
- `./gradlew compileJava compileTestJava --continue`
