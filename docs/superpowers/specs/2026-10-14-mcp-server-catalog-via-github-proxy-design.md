# MCP Server Catalog via GitHub Proxy — Design

Date: 2026-10-14
Status: Draft for review

## Goal

Make ByteChef's catalog of known hosted MCP servers a curated file that lives outside the ByteChef codebase, served
the same way workflow templates are: committed to a GitHub repo and delivered through `bytechef-github-proxy`.
ByteChef fetches it at runtime, so the catalog can change without a ByteChef release.

The catalog is ByteChef's own list, initially imported from n8n's catalog. ByteChef no longer depends on the
official MCP Registry (`https://registry.modelcontextprotocol.io`) in any way.

## Decisions

| Decision | Choice |
|---|---|
| Catalog content | The 79 servers imported from n8n's catalog. The 18 servers only the official registry listed are removed, as are the `registryName` and `sources` fields. The 72 `description` values (vendor-written text taken from the official registry) are kept. |
| Source of truth | `servers.json` at the root of the new repo `bytechefhq/bytechef-mcp-server`, branch `master`, edited by hand through PRs. No generator script. |
| Repo creation | A local repo is prepared at `/Volumes/Data/bytechef/bytechef-mcp-server` with one commit. The user creates the GitHub repo and pushes it. |
| Proxy API | A dedicated `GET /mcp-servers` endpoint, following the `/workflow-templates` pattern, not the generic raw passthrough. |
| ByteChef fallback | No bundled copy. ByteChef keeps the last good catalog in memory; with nothing cached it returns an empty catalog. Air-gapped installs point `GITHUB_PROXY_BASE_URL` at a mirror, as for workflow templates. |
| ByteChef scope | Proxy client method, a cached platform service, and one read-only GraphQL query. No UI work; a "pick a known server" picker is a separate follow-up. |
| Delivery | Proxy changes on a local branch in `bytechef-github-proxy`; the new repo as a local commit; ByteChef changes on the current branch. Nothing is pushed. |
| Naming | ByteChef already has a GraphQL `type McpServer` and domain class `McpServer` (the MCP servers ByteChef *hosts*, in `platform-mcp`). The catalog therefore uses `McpServerCatalogEntry` everywhere in ByteChef (records, GraphQL type, schema file `mcp-server-catalog.graphqls`, query `mcpServerCatalog`). |

## Current State

- `bytechef-github-proxy` (Spring Boot, port 6123, deployed by hand to `github-proxy.bytechef.io`) serves:
  - generic passthrough: `GET /gh/{owner}/{repo}/files` and `GET /gh/{owner}/{repo}/raw/...`;
  - dedicated, parsed and cached content: `GET /workflow-templates`, `GET /workflow-templates/{slug}` (from
    `bytechefhq/bytechef-workflows@master`) and `GET /integration/component-definitions`, `.../{name}`,
    `GET /integration/component-icons/{name}`.
  - Dedicated endpoints use a `GitHub*Reader` that fetches from `raw.githubusercontent.com` (optional
    `github.token`), a Caffeine cache, a `SingleFlight` loader, and `GitHubUnavailableException` → 502 Bad
    Gateway (`createBadGatewayResponseEntity`). Each reader owns its own `HttpClient`, fetch `Semaphore` and
    timeout; there is no shared fetch helper.
- ByteChef `server/libs/platform/platform-github-proxy-client` provides `GitHubProxyClient` (raw/files) and
  `WorkflowTemplateProxyClient` (typed), built in `GitHubProxyClientConfiguration` on top of `github-proxy.baseUrl`
  (`https://github-proxy.bytechef.io/` in `application.yml`; documented as `GITHUB_PROXY_BASE_URL`).
- Workflow templates are consumed by `PreBuiltTemplateService` (automation-configuration-service).
- `server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/servers.json` is an uncommitted seed
  file; nothing reads it.
- `bytechefhq/bytechef-mcp-server` does not exist on GitHub.

## Design

### 1. The catalog repo (`bytechef-mcp-server`)

Files:

- `servers.json`:

  ```json
  {
    "version": 1,
    "servers": [
      {
        "slug": "notion",
        "title": "Notion",
        "description": "Official Notion MCP server",
        "url": "https://mcp.notion.com/mcp",
        "transportType": "httpStreamable",
        "authentication": {"type": "oauth2", "dynamicClientRegistration": true},
        "websiteUrl": "https://developers.notion.com/docs/mcp",
        "component": "notion"
      }
    ]
  }
  ```

  Entry fields: `slug` (required, unique), `title` (required), `description`, `url` (required; may contain
  `{param}` placeholders), `urlParameters` (names of the placeholders), `transportType` (required:
  `httpStreamable` | `sse`), `authentication` (required), `websiteUrl`, `component` (matching ByteChef component
  name). `authentication.type` is `oauth2` | `header` | `none`; `oauth2` may carry `dynamicClientRegistration`,
  `clientIdMetadataDocument`, `authorizationUrl`, `tokenUrl`, `pkce`, `scopes`; `header` carries `headers`
  (`name`, `description`, `secret`).

  Content: the current seed file with the 18 `sources: ["mcp-registry"]`-only entries removed, the `registryName`,
  `sources` and `generatedAt` fields removed, entries sorted by `slug`, 2-space indentation, trailing newline.

- `README.md`: purpose, the schema above, the meaning of each authentication type, that the list was initially
  imported from n8n's catalog and is now curated here, and how to add or change a server (PR, keep slugs stable
  because they are identifiers, keep entries sorted).

- No generator script, CI or license file in this change.

### 2. Proxy: `GET /mcp-servers`

New package `com.bytechef.workflows.proxy.mcp`:

- `McpServerCatalog` — the parsed file: `int version`, `List<JsonNode> servers` (entries kept as JSON so unknown
  fields pass through).
- `GitHubMcpServerReader` — fetches `${mcp-servers.repo.owner:bytechefhq}/${mcp-servers.repo.name:bytechef-mcp-server}`
  at `${mcp-servers.repo.ref:master}`, file `${mcp-servers.file:servers.json}`, from `raw.githubusercontent.com`
  following the `GitHubComponentReader` pattern (own `HttpClient`, optional `github.token`, bounded fetch with
  `${mcp-servers.fetch.timeout.seconds:60}`). Validation:
  - malformed JSON or `version` other than `1` → `GitHubUnavailableException` (treated like an unavailable
    source), logged at ERROR with the cause;
  - an entry missing `slug`, `title`, `url` or `transportType`, with a `transportType` other than
    `httpStreamable`/`sse`, or with an `authentication.type` other than `oauth2`/`header`/`none` → skipped, WARN
    naming the slug or index and the reason;
  - duplicate `slug` → first wins, WARN.
- `McpServerController` — `GET /mcp-servers` → `200 {"servers": [...]}` with `Cache-Control: max-age=<ttl>`.
  Caffeine cache keyed by a constant, TTL `${mcp-servers.cache.ttl.seconds:3600}`, loaded through `SingleFlight`
  (wait `${mcp-servers.wait.seconds:60}`). On `GitHubUnavailableException`: serve the last good catalog if one was
  ever loaded (kept separately from the expiring cache entry), otherwise `502` through the existing
  `createBadGatewayResponseEntity` handling, as the other dedicated endpoints do.
- README: list the new endpoint and its properties.

### 3. ByteChef

**Proxy client** (`platform-github-proxy-client`):

- `McpServerCatalogProxyClient` interface: `List<McpServerCatalogEntry> getMcpServerCatalog()`.
- `RestMcpServerCatalogProxyClient` — `GET /mcp-servers`, `Accept: application/json`; a null body or missing
  `servers` is an `IllegalStateException`; HTTP errors propagate as `RestClientException`s.
- Model records in `com.bytechef.platform.githubproxy.client.model`, plain records like the existing
  `WorkflowTemplateSummary` (the module's Jackson 3 `ObjectMapper` does not fail on unknown properties, so no
  annotation is needed; a test pins that unknown fields are ignored): `McpServerCatalogEntry(slug, title,
  description, url, urlParameters, transportType, authentication, websiteUrl, component)`,
  `McpServerCatalogAuthentication(type, dynamicClientRegistration, clientIdMetadataDocument, authorizationUrl,
  tokenUrl, pkce, scopes, headers)`, `McpServerCatalogHeader(name, description, secret)`, and the wrapper
  `McpServerCatalog(servers)`.
- `GitHubProxyClientConfiguration` registers a `McpServerCatalogProxyClient` bean. Because the refresh runs inline
  on a user request (see Service), this client is bounded tighter than its siblings: 5 s connect timeout and a 10 s
  read timeout on its `JdkClientHttpRequestFactory`.

**Service** (`platform-configuration-service`; add `implementation(project(":server:libs:platform:platform-github-proxy-client"))`
to its `build.gradle.kts` — the proxy client depends only on Spring/Jackson, so no cycle):

- `McpServerCatalogService` / `McpServerCatalogServiceImpl.getMcpServerCatalog()`:
  - returns the in-memory catalog when it is younger than 1 hour (`Clock` injected for tests);
  - otherwise refreshes from `McpServerCatalogProxyClient` inline (bounded by the client's 5 s + 10 s timeouts);
    only one refresh runs at a time — concurrent callers during a refresh get the current in-memory copy (possibly
    empty) instead of waiting;
  - on failure keeps the previous copy (or empty list), logs WARN with the cause, and does not retry until the
    next hour (the failure time counts as the refresh time).

**API** (`platform-configuration-graphql`):

- `mcp-server-catalog.graphqls`: `type McpServerCatalogEntry { slug, title, description, url, urlParameters,
  transportType, authentication: McpServerCatalogAuthentication!, websiteUrl, component }`,
  `type McpServerCatalogAuthentication`, `type McpServerCatalogHeader`, and
  `extend type Query { mcpServerCatalog: [McpServerCatalogEntry!]! }`. (`type McpServer` already exists in
  `platform-mcp-graphql/mcp-server.graphqls` and both modules feed one schema.)
- `McpServerCatalogGraphQlController` → `McpServerCatalogFacade` → `McpServerCatalogService`. The facade method
  carries `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")`, exactly like
  `AiProviderCatalogFacadeImpl.getAiProviderCatalog`.
- Client: the module's `graphql/*.graphqls` path is already in `client/codegen.ts`; add an operation
  `client/src/graphql/platform/mcpServerCatalog.graphql` and regenerate `src/shared/middleware/graphql.ts`
  (operation and generated file committed separately). No component uses it yet.

**Cleanup and docs**

- Delete the uncommitted `server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/` directory.
- `docs/content/docs/platform/use-bytechef/self-hosted/configuration/environment-variables.md`: the
  `GITHUB_PROXY_BASE_URL` description mentions the MCP server catalog alongside workflow templates.

## Error Handling

| Situation | Proxy | ByteChef | User sees |
|---|---|---|---|
| Repo not published yet (GitHub 404) | 502 (nothing cached) | WARN, empty catalog | No catalog entries; manual MCP connections unaffected |
| GitHub down or slow | Last good copy, else 502 | Last good copy, else empty | Unchanged list, or empty on cold start |
| Malformed `servers.json` / unknown `version` | Last good copy, else 502; ERROR log | As above | Unchanged list |
| Some invalid entries | Skipped with WARN; rest served | Only valid entries | Valid servers |
| Proxy unreachable (firewall) | — | Last good copy / empty; WARN | Empty catalog; admin may set `GITHUB_PROXY_BASE_URL` to a mirror |
| Newer catalog fields than this ByteChef knows | Passed through | Ignored | Works |

The catalog is public data: no secrets are involved. Logs name repo, ref and cause, never the file body.

## Testing

**Proxy** (`bytechef-github-proxy`): fixture-based `GitHubMcpServerReaderTest` (valid file; entries missing
required fields, unknown transport, unknown auth type; duplicate slug; malformed JSON; unknown version) and
`McpServerControllerTest` modeled on `WorkflowTemplateControllerTest` (200 + `Cache-Control`; cache hit causes no
second fetch; last good copy served on failure; 502 when nothing was ever loaded). `./gradlew check` green.

**ByteChef**:

- `RestMcpServerCatalogProxyClientTest` (WireMock): parses the fixture; ignores unknown fields; 5xx throws.
- `McpServerCatalogServiceTest`: first call fetches; second call within the hour does not; refetch after the hour;
  failure keeps the previous copy; first-call failure returns empty and does not retry within the hour.
- `McpServerCatalogGraphQlControllerIntTest` (`@GraphQlTest`, like `WorkflowNodeScriptGraphQlControllerIntTest`):
  the query returns the facade's entries with nested authentication.
- Gates: `check` on every touched module; `npm run check` in `client/` (grep `Unhandled`).

**Catalog repo**: a one-off validation that `servers.json` parses, has 79 entries, unique slugs, and no
`registryName`/`sources` fields.

## Out of Scope

- The "pick a known server" UI.
- Creating or pushing `bytechefhq/bytechef-mcp-server`, deploying the proxy.
- A per-slug proxy endpoint, CI validation in the catalog repo, a JSON Schema file.
- Any use of the official MCP Registry.
