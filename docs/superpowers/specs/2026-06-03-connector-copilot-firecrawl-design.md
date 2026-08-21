# Connector Copilot: Firecrawl Doc Fetch (scrape + crawl) — Design

- **Date:** 2026-06-03
- **Status:** Approved (design)
- **Edition:** EE only
- **Author:** Ivica Cardic
- **Issue:** [#2500](https://github.com/bytechefhq/bytechef/issues/2500) (AI Copilot for connectors)

## Context

#2500's two stated tasks — *generate an OpenAPI spec from API documentation* and *expose it in the
connector builder UI* — are **already implemented** on this branch:

- `ApiConnectorAiServiceImpl` (EE, `bytechef.ai.copilot.enabled`) generates an OpenAPI 3.0 spec from a
  documentation URL via `ChatModel`, with async job tracking + cancellation
  (`generateOpenApiSpecificationAsync`).
- The `ApiConnectorAiPage` wizard drives doc-URL + user-prompt → generate → endpoint selection → save.
- The spec is turned into a connector by `OpenApiGenerator` (wrapping the CLI
  `ComponentInitOpenApiGenerator`).

The one real weakness: the doc fetch is a **single-page** `HttpClient` GET parsed by **Jsoup**, which
fails on (a) JavaScript-rendered doc sites (Jsoup sees an empty shell) and (b) multi-page docs.

Meanwhile the module **already has the right abstraction, fully built but unused by the AI service**:

- `WebScrapeService` SPI (`-api` module): `scrape(url) → ScrapeResult{success,content,error}`,
  `crawl(url, maxPages, includePatterns) → CrawlResult{success,content,crawledUrls,error}`,
  `getProviderName()`.
- `FirecrawlWebScrapeService` (`@ConditionalOnProperty bytechef.ai.firecrawl.enabled=true`) — markdown
  scrape + multi-page crawl (with up to ~2-minute polling).
- `JsoupWebScrapeService` (`@ConditionalOnMissingBean(name="firecrawlWebScrapeService")`) — the fallback.
- So **exactly one** `WebScrapeService` bean exists at runtime (Firecrawl when enabled, Jsoup
  otherwise); a plain `WebScrapeService` injection resolves unambiguously. A sibling `WebScrapeTool`
  already injects it this way.

This work **finishes the wiring**: route the AI service's doc fetch through `WebScrapeService`, and add
opt-in multi-page crawl to the async preview path.

### Correctness constraint (load-bearing)

`FirecrawlWebScrapeService.pollCrawlStatus`'s Javadoc states crawl **must only run on async/background
threads** (it blocks up to `MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS` ≈ 2 min). Therefore:

- **Crawl is wired only into the `@Async` preview path** (`generateOpenApiSpecificationAsync`).
- The **synchronous** `generateOpenApiSpecification(url)` (reached by the sync
  `generateFromDocumentation` mutation on a web request thread) stays **scrape-only**.

## Goal

Make the connector spec generator fetch documentation through `WebScrapeService` — gaining
JS-rendered-doc support (Firecrawl) with automatic Jsoup fallback — and let the user opt into
**multi-page crawl** in the wizard for docs whose endpoints span many pages.

EE-only, gated on the existing `bytechef.ai.copilot.enabled` (+ `bytechef.ai.firecrawl.enabled` to
activate the Firecrawl provider).

## Non-goals (deferred)

- Changing the OpenAPI-generation system prompt or the connector-from-spec generation.
- Crawl on the synchronous path (web-thread hazard) — scrape-only there.
- Exposing `includePatterns` in the UI (pass empty list for v1; Firecrawl crawls from the root URL).
- A new feature flag.

## Backend design

1. **`ApiConnectorAiServiceImpl`** (`platform-api-connector-configuration-service`):
   - Constructor-inject `WebScrapeService` (replaces the private `HttpClient` field). Remove the inline
     Jsoup/HttpClient `fetchDocumentation` and the `org.jsoup.*` / `java.net.http.*` imports.
   - New private `String fetchDocumentation(String url)` → `webScrapeService.scrape(url)`; on
     `ScrapeResult.success()` return `content()`, else throw
     `ConfigurationException(..., INVALID_API_CONNECTOR_DEFINITION)`.
   - New private `String fetchDocumentation(String url, int maxPages)` → when `maxPages > 1`
     `webScrapeService.crawl(url, maxPages, List.of())` (use `CrawlResult.content()` / throw on
     failure), else delegate to the single-page `fetchDocumentation(url)`.
   - `generateOpenApiSpecification(String url)` (sync) → uses the scrape-only `fetchDocumentation(url)`.
   - `generateOpenApiSpecificationAsync(jobId, url, userInstructions, maxPages)` → uses
     `fetchDocumentation(url, maxPages)`; keep the existing cancellation checks and
     markProcessing/Completed/Failed flow.
2. **`ApiConnectorAiService`** interface (`-api`): add `int maxPages` to
   `generateOpenApiSpecificationAsync(...)`. Leave `generateOpenApiSpecification(String)` unchanged.
3. **GraphQL** (`ApiConnectorGraphQlController` + `api-connector.graphqls`): add `maxPages: Int` to
   `GenerateFromDocumentationInput` (+ the Java record). `startGenerateFromDocumentationPreview` passes
   `input.maxPages()` (treat null/≤0 as `1`) into the async call. The sync `generateFromDocumentation`
   mutation is unchanged (scrape-only).

## Frontend design

- **Wizard doc-URL step** (`ApiConnectorWizardDocUrlStep`): add an opt-in control — a checkbox
  **"Crawl linked documentation pages"**; when checked, a small number input for max pages (default
  `10`, min `2`). Off → `maxPages = 1` (scrape).
- **`useApiConnectorAiPage`**: thread `maxPages` into the `startGenerateFromDocumentationPreview`
  mutation input. Regenerate GraphQL types.
- No change to the polling/endpoint-selection/save steps.

## Error handling

- Scrape/crawl failure → `ConfigurationException(INVALID_API_CONNECTOR_DEFINITION)` (sync) or, on the
  async path, caught and recorded via `markAsFailed(jobId, message)` (existing behavior).
- Firecrawl disabled → `JsoupWebScrapeService` serves `scrape`; its `crawl` (if it implements only a
  degenerate crawl) is provider-defined — when only Jsoup is present and `maxPages > 1`, behavior is
  whatever `JsoupWebScrapeService.crawl` returns (verify during planning; if Jsoup crawl is
  single-page, document that multi-page requires Firecrawl).

## Testing

- **Backend:** new `ApiConnectorAiServiceImplTest` mocking `WebScrapeService`, `ChatModel`,
  `ApiConnectorGenerationJobService`:
  - sync `generateOpenApiSpecification` → `scrape` success → cleaned spec; scrape failure → throws.
  - async with `maxPages = 1` → `scrape`; `maxPages > 1` → `crawl`; success → `markAsCompleted`;
    fetch failure → `markAsFailed`; cancellation short-circuits.
- **Frontend:** `ApiConnectorWizardDocUrlStep` crawl toggle renders + drives `maxPages`; hook passes
  `maxPages` to the mutation.

## Architecture summary (units & boundaries)

| Unit | Responsibility | Depends on |
|------|----------------|------------|
| `ApiConnectorAiServiceImpl.fetchDocumentation(url[, maxPages])` | fetch docs via the SPI (scrape/crawl) | `WebScrapeService` |
| `ApiConnectorAiService.generateOpenApiSpecificationAsync(…, maxPages)` | async generate honoring crawl | service impl |
| `GenerateFromDocumentationInput.maxPages` + controller | carry the crawl knob | GraphQL |
| `ApiConnectorWizardDocUrlStep` crawl control | user opts into multi-page | `useApiConnectorAiPage` |

The change is additive and self-contained: it swaps a private fetch for an existing SPI and threads one
optional `maxPages` knob into the async preview path. No change to spec generation or connector building.
