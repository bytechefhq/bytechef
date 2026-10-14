# MCP Server Catalog via GitHub Proxy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move ByteChef's catalog of hosted MCP servers into its own GitHub repo, serve it through `bytechef-github-proxy` as `GET /mcp-servers`, and expose it in ByteChef through a cached platform service and a GraphQL query — with no dependency on the official MCP Registry.

**Architecture:** Three repos. (1) `bytechef-mcp-server` holds `servers.json` (79 hand-curated entries). (2) The proxy gains a `mcp` package mirroring its `component`/`template` packages: a reader fetches the file from `raw.githubusercontent.com`, a parser validates entries, a controller caches the result with Caffeine + `SingleFlight` and serves the last good copy on failure. (3) ByteChef gains `McpServerCatalogProxyClient` in `platform-github-proxy-client`, a `McpServerCatalogService` in `platform-configuration-service` that keeps the last good catalog for an hour, a `@PreAuthorize`d facade, and a `mcpServerCatalog` GraphQL query.

**Tech Stack:** Java 25, Spring Boot 4 (proxy and server), Jackson 3 (`tools.jackson`), Caffeine, WireMock 3 (ByteChef tests), Spring GraphQL + `@GraphQlTest`, React client GraphQL codegen.

**Spec:** `docs/superpowers/specs/2026-10-14-mcp-server-catalog-via-github-proxy-design.md`

## Global Constraints

- Catalog content: the 79 n8n-derived entries from the current seed file; remove the 18 entries whose `sources` is exactly `["mcp-registry"]`; remove the `registryName`, `sources` and `generatedAt` fields; keep `description`. Top level `{"version": 1, "servers": [...]}`, sorted by `slug`, 2-space indent, trailing newline.
- Entry schema: `slug` (required, unique), `title` (required), `description`, `url` (required), `urlParameters`, `transportType` (required: `httpStreamable` | `sse`), `authentication` (required; `type` ∈ `oauth2` | `header` | `none`), `websiteUrl`, `component`.
- Proxy failure semantics: `GitHubUnavailableException` → **502** via the existing `createBadGatewayResponseEntity` pattern; last good copy served when one was ever loaded.
- Proxy properties: `mcp-servers.repo.owner` (`bytechefhq`), `mcp-servers.repo.name` (`bytechef-mcp-server`), `mcp-servers.repo.ref` (`master`), `mcp-servers.file` (`servers.json`), `mcp-servers.fetch.timeout.seconds` (60), `mcp-servers.cache.ttl.seconds` (3600), `mcp-servers.wait.seconds` (60), optional `github.token`.
- ByteChef naming: `McpServerCatalogEntry` / `McpServerCatalogAuthentication` / `McpServerCatalogHeader` / `McpServerCatalog` (client models), `*DTO` suffixes in `platform-configuration-api`, GraphQL types `McpServerCatalogEntry`, `McpServerCatalogAuthentication`, `McpServerCatalogHeader`, query `mcpServerCatalog`, schema file `mcp-server-catalog.graphqls`. **Never** `McpServer` — that name is taken by `platform-mcp`.
- ByteChef client timeouts: 5 s connect, 10 s read for this proxy client only.
- ByteChef service: in-memory last good catalog, refreshed at most once per hour (failure also counts as a refresh attempt), concurrent callers during a refresh get the current copy, empty list when nothing was ever loaded; WARN on failure, never throws to the caller.
- Facade guard: `@PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")`.
- No bundled fallback copy in ByteChef; delete the untracked `server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/`.
- Nothing is pushed in any repo. The catalog repo gets one local commit; the user publishes it.
- Repo conventions (ByteChef): subject-only commits, `client - ` prefix for client commits, no trailers; blank line before control statements; one test class per source class (`<Class>Test`/`<Class>IntTest`); controller tests are `@GraphQlTest` IntTests; `spotlessApply` before Java commits; GraphQL operation file and regenerated `graphql.ts` committed separately. Proxy repo commits: plain descriptive subject, no trailer.

## Review Focus

1. A `servers.json` whose `servers` is missing or not an array → the proxy must treat it as an unavailable source (502 / last good copy), not NPE (Task 2, `testParseRejectsMissingServersArray`).
2. A duplicate slug where the *second* entry is the valid-looking one → first wins, second skipped with a warning, no 500 (Task 2, `testParseKeepsFirstEntryForDuplicateSlug`).
3. Proxy up but returning `{"servers": []}` → ByteChef must cache the empty catalog as "good" and not hammer the proxy on every request (Task 6, `testEmptyCatalogIsCachedLikeAnyOther`).
4. Proxy answering 502 after a good load → ByteChef keeps serving the previous catalog, with one WARN per hour, not per request (Task 6, `testFailureKeepsPreviousCatalogAndDoesNotRetryWithinTheHour`).
5. A GraphQL caller with no `USER` authority → the facade rejects it (Task 7, `testGetMcpServerCatalogRequiresUserAuthority`).

## File Structure

**Repo `bytechef-mcp-server` (new, `/Volumes/Data/bytechef/bytechef-mcp-server`)**
- `servers.json` — the catalog.
- `README.md` — schema, provenance, contribution notes.

**Repo `bytechef-github-proxy` (`/Volumes/Data/bytechef/bytechef-github-proxy`, new branch `mcp-servers`)**
- Create `src/main/java/com/bytechef/workflows/proxy/mcp/McpServerCatalog.java` — record `(int version, List<JsonNode> servers)`.
- Create `.../mcp/McpServerCatalogParser.java` — pure JSON → `McpServerCatalog` with validation.
- Create `.../mcp/GitHubMcpServerReader.java` — fetch bytes from `raw.githubusercontent.com`, delegate to the parser.
- Create `src/main/java/com/bytechef/workflows/proxy/web/rest/McpServerController.java` — `GET /mcp-servers`.
- Tests: `McpServerCatalogParserTest`, `GitHubMcpServerReaderTest`, `McpServerControllerTest`; fixtures `src/test/resources/fixtures/mcp-servers.json`, `mcp-servers-invalid-entries.json`.
- Modify `README.md`.

**Repo `bytechef` (this worktree)**
- `server/libs/platform/platform-github-proxy-client`: create `McpServerCatalogProxyClient`, `internal/RestMcpServerCatalogProxyClient`, `model/McpServerCatalog`, `model/McpServerCatalogEntry`, `model/McpServerCatalogAuthentication`, `model/McpServerCatalogHeader`; modify `config/GitHubProxyClientConfiguration`, `build.gradle.kts` (WireMock test dep); test `RestMcpServerCatalogProxyClientTest`.
- `server/libs/platform/platform-configuration/platform-configuration-api`: create `dto/McpServerCatalogEntryDTO`, `dto/McpServerCatalogAuthenticationDTO`, `dto/McpServerCatalogHeaderDTO`, `service/McpServerCatalogService`, `facade/McpServerCatalogFacade`.
- `server/libs/platform/platform-configuration/platform-configuration-service`: create `service/McpServerCatalogServiceImpl`, `facade/McpServerCatalogFacadeImpl`; modify `build.gradle.kts`; tests `McpServerCatalogServiceTest`, `McpServerCatalogFacadeTest`.
- `server/libs/platform/platform-configuration/platform-configuration-graphql`: create `graphql/mcp-server-catalog.graphqls`, `web/graphql/McpServerCatalogGraphQlController`; modify `config/PlatformConfigurationGraphQlConfigurationSharedMocks`; test `McpServerCatalogGraphQlControllerIntTest`.
- `client/src/graphql/platform/mcp-server-catalog/mcpServerCatalog.graphql` (new) and regenerated `client/src/shared/middleware/graphql.ts`.
- `docs/content/docs/platform/use-bytechef/self-hosted/configuration/environment-variables.md:321`.
- Delete untracked `server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/`.

---

### Task 1: Catalog repo — `servers.json` and README

**Files:**
- Create: `/Volumes/Data/bytechef/bytechef-mcp-server/servers.json`
- Create: `/Volumes/Data/bytechef/bytechef-mcp-server/README.md`
- Delete (untracked, no commit): `/Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a/server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/`

**Interfaces:**
- Produces: the catalog file shape every later task parses — `{"version": 1, "servers": [ {slug, title, description?, url, urlParameters?, transportType, authentication{type, dynamicClientRegistration?, clientIdMetadataDocument?, authorizationUrl?, tokenUrl?, pkce?, scopes?, headers?[{name, description, secret}]}, websiteUrl?, component?} ]}`.

- [ ] **Step 1: Build `servers.json` from the seed and verify it**

```bash
mkdir -p /Volumes/Data/bytechef/bytechef-mcp-server && cd /Volumes/Data/bytechef/bytechef-mcp-server && git init -q -b master
python3 -I - <<'EOF'
import json
seed = json.load(open('/Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a/server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/servers.json'))
servers = []
for entry in seed['servers']:
    if entry.get('sources') == ['mcp-registry']:
        continue
    entry = {key: value for key, value in entry.items() if key not in ('registryName', 'sources')}
    servers.append(entry)
servers.sort(key=lambda entry: entry['slug'])
assert len(servers) == 79, len(servers)
assert len({entry['slug'] for entry in servers}) == 79
for entry in servers:
    assert entry['transportType'] in ('httpStreamable', 'sse'), entry['slug']
    assert entry['authentication']['type'] in ('oauth2', 'header', 'none'), entry['slug']
    assert not {'registryName', 'sources'} & set(entry), entry['slug']
with open('servers.json', 'w') as out:
    json.dump({'version': 1, 'servers': servers}, out, indent=2, ensure_ascii=False)
    out.write('\n')
print('ok', len(servers))
EOF
python3 -I -c "import json; d=json.load(open('servers.json')); print(d['version'], len(d['servers']), d['servers'][0]['slug'], d['servers'][-1]['slug'])"
```

Expected: `ok 79`, then `1 79 airtable zoom-info`.

- [ ] **Step 2: Write `README.md`**

```markdown
# ByteChef MCP Server Catalog

`servers.json` lists hosted MCP servers that ByteChef offers in its MCP Client component. ByteChef reads it
through `bytechef-github-proxy` (`GET /mcp-servers`), so changes here reach ByteChef instances without a release.

The list was initially imported from n8n's public MCP server catalog and is now curated here by hand.

## Format

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

| Field | Required | Meaning |
|---|---|---|
| `slug` | yes | Stable identifier; never change it once published. |
| `title` | yes | Display name. |
| `description` | no | One-line description shown next to the title. |
| `url` | yes | MCP endpoint. May contain `{name}` placeholders the user fills in; list them in `urlParameters`. |
| `urlParameters` | no | Names of the placeholders in `url`. |
| `transportType` | yes | `httpStreamable` or `sse`. |
| `authentication` | yes | See below. |
| `websiteUrl` | no | Vendor documentation. |
| `component` | no | Name of the ByteChef component for the same product, if one exists. |

### `authentication`

- `{"type": "oauth2", "dynamicClientRegistration": true}` — OAuth2 with automatic client registration (RFC 7591);
  ByteChef discovers the authorization server and signs in with one click. Optional `clientIdMetadataDocument`,
  `authorizationUrl`, `tokenUrl`, `pkce`, `scopes` describe servers that do not support registration.
- `{"type": "header", "headers": [{"name": "Authorization", "description": "...", "secret": true}]}` — an API key
  sent as a request header.
- `{"type": "none"}` — no authentication.

## Contributing

Open a pull request that edits `servers.json`. Keep entries sorted by `slug`, keep `slug` values stable, and verify
the file parses:

```bash
python3 -c "import json; d=json.load(open('servers.json')); assert len({s['slug'] for s in d['servers']}) == len(d['servers']); print(len(d['servers']), 'servers')"
```
```

- [ ] **Step 3: Commit the catalog repo and remove the ByteChef seed**

```bash
cd /Volumes/Data/bytechef/bytechef-mcp-server && git add servers.json README.md && git commit -q -m "Add the MCP server catalog" && git log --oneline -1
rm -rf /Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a/server/libs/modules/components/mcp-client/src/main/resources/mcp-registry
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a && git status --short
```

Expected: one commit in the new repo; the ByteChef worktree shows no untracked `mcp-registry/` entry.

---

### Task 2: Proxy — `McpServerCatalog` and `McpServerCatalogParser`

**Files:**
- Create: `/Volumes/Data/bytechef/bytechef-github-proxy/src/main/java/com/bytechef/workflows/proxy/mcp/McpServerCatalog.java`
- Create: `.../mcp/McpServerCatalogParser.java`
- Create: `src/test/resources/fixtures/mcp-servers.json`, `src/test/resources/fixtures/mcp-servers-invalid-entries.json`
- Test: `src/test/java/com/bytechef/workflows/proxy/mcp/McpServerCatalogParserTest.java`

**Interfaces:**
- Produces: `record McpServerCatalog(int version, List<JsonNode> servers)`; `McpServerCatalogParser(ObjectMapper)` with `McpServerCatalog parse(byte[] json)` throwing `GitHubUnavailableException` for malformed JSON, missing/non-array `servers`, or `version != 1`.

Work on a new branch: `cd /Volumes/Data/bytechef/bytechef-github-proxy && git checkout -b mcp-servers`.

- [ ] **Step 1: Write fixtures and the failing test**

`src/test/resources/fixtures/mcp-servers.json`:

```json
{
  "version": 1,
  "servers": [
    {"slug": "notion", "title": "Notion", "url": "https://mcp.notion.com/mcp", "transportType": "httpStreamable",
     "authentication": {"type": "oauth2", "dynamicClientRegistration": true}, "component": "notion"},
    {"slug": "linear", "title": "Linear", "url": "https://mcp.linear.app/sse", "transportType": "sse",
     "authentication": {"type": "oauth2", "dynamicClientRegistration": true}, "futureField": "kept"}
  ]
}
```

`src/test/resources/fixtures/mcp-servers-invalid-entries.json`:

```json
{
  "version": 1,
  "servers": [
    {"slug": "ok", "title": "Ok", "url": "https://ok.example.com/mcp", "transportType": "httpStreamable", "authentication": {"type": "none"}},
    {"title": "No slug", "url": "https://x.example.com/mcp", "transportType": "httpStreamable", "authentication": {"type": "none"}},
    {"slug": "no-url", "title": "No url", "transportType": "httpStreamable", "authentication": {"type": "none"}},
    {"slug": "bad-transport", "title": "Bad", "url": "https://x.example.com/mcp", "transportType": "websocket", "authentication": {"type": "none"}},
    {"slug": "bad-auth", "title": "Bad", "url": "https://x.example.com/mcp", "transportType": "sse", "authentication": {"type": "basic"}},
    {"slug": "ok", "title": "Duplicate", "url": "https://dup.example.com/mcp", "transportType": "httpStreamable", "authentication": {"type": "none"}}
  ]
}
```

Test:

```java
package com.bytechef.workflows.proxy.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class McpServerCatalogParserTest {

    private final McpServerCatalogParser parser = new McpServerCatalogParser(new ObjectMapper());

    @Test
    void testParseKeepsValidEntriesAndUnknownFields() throws IOException {
        McpServerCatalog catalog = parser.parse(fixture("/fixtures/mcp-servers.json"));

        assertEquals(1, catalog.version());
        assertEquals(2, catalog.servers().size());
        assertEquals("kept", catalog.servers().get(1).path("futureField").asString(""));
    }

    @Test
    void testParseSkipsInvalidEntries() throws IOException {
        McpServerCatalog catalog = parser.parse(fixture("/fixtures/mcp-servers-invalid-entries.json"));

        assertEquals(1, catalog.servers().size());
        assertEquals("Ok", catalog.servers().getFirst().path("title").asString(""));
    }

    @Test
    void testParseKeepsFirstEntryForDuplicateSlug() throws IOException {
        McpServerCatalog catalog = parser.parse(fixture("/fixtures/mcp-servers-invalid-entries.json"));

        assertEquals("https://ok.example.com/mcp", catalog.servers().getFirst().path("url").asString(""));
    }

    @Test
    void testParseRejectsUnknownVersion() {
        GitHubUnavailableException exception = assertThrows(
            GitHubUnavailableException.class, () -> parser.parse(bytes("{\"version\":2,\"servers\":[]}")));

        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void testParseRejectsMissingServersArray() {
        assertThrows(GitHubUnavailableException.class, () -> parser.parse(bytes("{\"version\":1}")));
        assertThrows(GitHubUnavailableException.class, () -> parser.parse(bytes("{\"version\":1,\"servers\":{}}")));
    }

    @Test
    void testParseRejectsMalformedJson() {
        assertThrows(GitHubUnavailableException.class, () -> parser.parse(bytes("{not json")));
    }

    private byte[] fixture(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            return inputStream.readAllBytes();
        }
    }

    private static byte[] bytes(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `cd /Volumes/Data/bytechef/bytechef-github-proxy && ./gradlew test --tests '*McpServerCatalogParserTest' --offline -q`
Expected: compilation failure (classes missing).

- [ ] **Step 3: Implement the record and parser**

`McpServerCatalog.java`:

```java
package com.bytechef.workflows.proxy.mcp;

import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * The parsed MCP server catalog. Entries stay raw JSON so fields this proxy does not know pass through.
 *
 * @author Ivica Cardic
 */
public record McpServerCatalog(int version, List<JsonNode> servers) {

    public McpServerCatalog {
        servers = List.copyOf(servers);
    }
}
```

`McpServerCatalogParser.java`:

```java
package com.bytechef.workflows.proxy.mcp;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Validates servers.json: the file-level shape must be right or the catalog is unusable; a bad entry is skipped.
 *
 * @author Ivica Cardic
 */
@Component
public class McpServerCatalogParser {

    private static final Logger logger = LoggerFactory.getLogger(McpServerCatalogParser.class);

    private static final int SUPPORTED_VERSION = 1;
    private static final Set<String> TRANSPORT_TYPES = Set.of("httpStreamable", "sse");
    private static final Set<String> AUTHENTICATION_TYPES = Set.of("oauth2", "header", "none");
    private static final List<String> REQUIRED_FIELDS = List.of("slug", "title", "url", "transportType");

    private final ObjectMapper objectMapper;

    public McpServerCatalogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpServerCatalog parse(byte[] json) {
        JsonNode root;

        try {
            root = objectMapper.readTree(json);
        } catch (RuntimeException e) {
            throw new GitHubUnavailableException("servers.json is not valid JSON: " + e.getMessage(), e);
        }

        int version = root.path("version")
            .asInt(-1);

        if (version != SUPPORTED_VERSION) {
            throw new GitHubUnavailableException("Unsupported servers.json version: " + root.path("version"));
        }

        JsonNode serversNode = root.path("servers");

        if (!serversNode.isArray()) {
            throw new GitHubUnavailableException("servers.json has no servers array");
        }

        List<JsonNode> servers = new ArrayList<>();
        Set<String> slugs = new HashSet<>();
        int index = 0;

        for (JsonNode server : serversNode) {
            String reason = invalidReason(server);

            if (reason != null) {
                logger.warn("Skipping MCP server entry #{} ({}): {}", index, server.path("slug").asString("?"), reason);
            } else if (!slugs.add(server.path("slug").asString(""))) {
                logger.warn("Skipping MCP server entry #{}: duplicate slug {}", index, server.path("slug").asString(""));
            } else {
                servers.add(server);
            }

            index++;
        }

        return new McpServerCatalog(version, servers);
    }

    private static String invalidReason(JsonNode server) {
        if (!server.isObject()) {
            return "not an object";
        }

        for (String field : REQUIRED_FIELDS) {
            if (server.path(field).asString("").isBlank()) {
                return "missing " + field;
            }
        }

        if (!TRANSPORT_TYPES.contains(server.path("transportType").asString(""))) {
            return "unknown transportType " + server.path("transportType");
        }

        String authenticationType = server.path("authentication")
            .path("type")
            .asString("");

        if (!AUTHENTICATION_TYPES.contains(authenticationType)) {
            return "unknown authentication.type " + server.path("authentication").path("type");
        }

        return null;
    }
}
```

If `JsonNode.asString(String)` / `asInt(int)` do not exist on this Jackson 3 version, use the equivalents the existing readers use (`GitHubComponentReader` calls `.asString("")`).

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*McpServerCatalogParserTest' --offline -q`
Expected: 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bytechef/workflows/proxy/mcp src/test/java/com/bytechef/workflows/proxy/mcp src/test/resources/fixtures/mcp-servers.json src/test/resources/fixtures/mcp-servers-invalid-entries.json
git commit -m "Add the MCP server catalog parser"
```

---

### Task 3: Proxy — `GitHubMcpServerReader`

**Files:**
- Create: `/Volumes/Data/bytechef/bytechef-github-proxy/src/main/java/com/bytechef/workflows/proxy/mcp/GitHubMcpServerReader.java`
- Test: `src/test/java/com/bytechef/workflows/proxy/mcp/GitHubMcpServerReaderTest.java`

**Interfaces:**
- Consumes: `McpServerCatalogParser.parse(byte[])`, `GitHubUnavailableException`.
- Produces: `@Component GitHubMcpServerReader` with `McpServerCatalog read()`; package-private constructor `(String repoOwner, String repoName, String repoRef, String file, String gitHubToken, long fetchTimeoutSeconds, McpServerCatalogParser parser, HttpClient httpClient)` for tests.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.workflows.proxy.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class GitHubMcpServerReaderTest {

    private static final String CATALOG = """
        {"version":1,"servers":[{"slug":"notion","title":"Notion","url":"https://mcp.notion.com/mcp",
         "transportType":"httpStreamable","authentication":{"type":"oauth2"}}]}
        """;

    private final HttpClient httpClient = mock(HttpClient.class);
    private final GitHubMcpServerReader reader = new GitHubMcpServerReader(
        "bytechefhq", "bytechef-mcp-server", "master", "servers.json", "secret-token", 5,
        new McpServerCatalogParser(new ObjectMapper()), httpClient);

    @Test
    @SuppressWarnings("unchecked")
    void testReadFetchesRawFileWithTokenAndParsesIt() throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(CATALOG.getBytes(StandardCharsets.UTF_8));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        McpServerCatalog catalog = reader.read();

        assertEquals(1, catalog.servers().size());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);

        org.mockito.Mockito.verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest request = requestCaptor.getValue();

        assertEquals(
            "https://raw.githubusercontent.com/bytechefhq/bytechef-mcp-server/master/servers.json",
            request.uri().toString());
        assertEquals(Optional.of("token secret-token"), request.headers().firstValue("Authorization"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReadTreatsNon2xxAsUnavailable() throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        GitHubUnavailableException exception = assertThrows(GitHubUnavailableException.class, reader::read);

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReadWrapsTransportFailures() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("connection reset"));

        assertThrows(GitHubUnavailableException.class, reader::read);
    }
}
```

Replace the inline `org.mockito.Mockito.verify` with a static import. If Mockito cannot mock `HttpClient` directly in this project (it is an abstract class, which Mockito supports), follow `GitHubComponentReaderFetchTest`'s `FakeHttpClient` subclass approach instead.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests '*GitHubMcpServerReaderTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 3: Implement the reader**

```java
package com.bytechef.workflows.proxy.mcp;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import com.github.mizosoft.methanol.Methanol;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fetches servers.json from the catalog repository and hands it to the parser.
 *
 * @author Ivica Cardic
 */
@Component
public class GitHubMcpServerReader {

    private final String repoOwner;
    private final String repoName;
    private final String repoRef;
    private final String file;
    private final String gitHubToken;
    private final long fetchTimeoutSeconds;
    private final McpServerCatalogParser parser;
    private final HttpClient httpClient;

    @Autowired
    public GitHubMcpServerReader(
        @Value("${mcp-servers.repo.owner:bytechefhq}") String repoOwner,
        @Value("${mcp-servers.repo.name:bytechef-mcp-server}") String repoName,
        @Value("${mcp-servers.repo.ref:master}") String repoRef,
        @Value("${mcp-servers.file:servers.json}") String file,
        @Value("${github.token:}") String gitHubToken,
        @Value("${mcp-servers.fetch.timeout.seconds:60}") long fetchTimeoutSeconds,
        McpServerCatalogParser parser) {

        this(repoOwner, repoName, repoRef, file, gitHubToken, fetchTimeoutSeconds, parser, newClient(fetchTimeoutSeconds));
    }

    GitHubMcpServerReader(
        String repoOwner, String repoName, String repoRef, String file, String gitHubToken,
        long fetchTimeoutSeconds, McpServerCatalogParser parser, HttpClient httpClient) {

        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.repoRef = repoRef;
        this.file = file;
        this.gitHubToken = gitHubToken.isBlank() ? null : gitHubToken;
        this.fetchTimeoutSeconds = fetchTimeoutSeconds;
        this.parser = parser;
        this.httpClient = httpClient;
    }

    public McpServerCatalog read() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(rawUrl()))
            .GET()
            .timeout(Duration.ofSeconds(fetchTimeoutSeconds))
            .header("Accept", "application/json");

        if (gitHubToken != null) {
            requestBuilder.header("Authorization", "token " + gitHubToken);
        }

        HttpResponse<byte[]> response;

        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new GitHubUnavailableException("Failed to fetch the MCP server catalog: " + e.getMessage(), e);
        }

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            throw new GitHubUnavailableException("Failed to fetch the MCP server catalog: HTTP " + status);
        }

        return parser.parse(response.body());
    }

    private String rawUrl() {
        return String.format(
            "https://raw.githubusercontent.com/%s/%s/%s/%s", encode(repoOwner), encode(repoName), encode(repoRef),
            encode(file));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static HttpClient newClient(long timeoutSeconds) {
        return Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .headersTimeout(Duration.ofSeconds(timeoutSeconds))
            .readTimeout(Duration.ofSeconds(timeoutSeconds))
            .requestTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }
}
```

An `InterruptedException` inside `send` is caught by the generic `catch`; re-set the interrupt flag there (`if (e instanceof InterruptedException) Thread.currentThread().interrupt();`) before throwing.

- [ ] **Step 4: Run the test**

Run: `./gradlew test --tests '*GitHubMcpServerReaderTest' --offline -q`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/bytechef/workflows/proxy/mcp/GitHubMcpServerReader.java src/test/java/com/bytechef/workflows/proxy/mcp/GitHubMcpServerReaderTest.java
git commit -m "Add the GitHub MCP server catalog reader"
```

---

### Task 4: Proxy — `GET /mcp-servers` controller, README

**Files:**
- Create: `/Volumes/Data/bytechef/bytechef-github-proxy/src/main/java/com/bytechef/workflows/proxy/web/rest/McpServerController.java`
- Test: `src/test/java/com/bytechef/workflows/proxy/web/rest/McpServerControllerTest.java`
- Modify: `/Volumes/Data/bytechef/bytechef-github-proxy/README.md` (endpoint list near the top; "Configuration (env vars)" section at line ~113)

**Interfaces:**
- Consumes: `GitHubMcpServerReader.read()`, `SingleFlight<T>` (package-private, same package), `McpServerCatalog`.
- Produces: `GET /mcp-servers` → `200 {"servers": [...]}` with headers `X-Cache: HIT|MISS`, `Cache-Control: public, max-age=<ttl>`, `X-Content-Type-Options: nosniff`; `502 text/plain` when nothing was ever loaded and GitHub fails. Constructor `McpServerController(GitHubMcpServerReader reader, ObjectMapper objectMapper, long cacheTtlSeconds, long waitSeconds)`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.workflows.proxy.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import com.bytechef.workflows.proxy.mcp.GitHubMcpServerReader;
import com.bytechef.workflows.proxy.mcp.McpServerCatalog;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

class McpServerControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GitHubMcpServerReader reader = mock(GitHubMcpServerReader.class);

    private McpServerCatalog catalog(String slug) {
        return new McpServerCatalog(
            1, List.of(OBJECT_MAPPER.readTree("{\"slug\":\"" + slug + "\",\"title\":\"T\"}")));
    }

    @Test
    void testListServesCatalogWithCacheHeaders() {
        when(reader.read()).thenReturn(catalog("notion"));

        McpServerController controller = new McpServerController(reader, OBJECT_MAPPER, 3600L, 30L);

        ResponseEntity<?> response = controller.listMcpServers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("MISS", response.getHeaders().getFirst("X-Cache"));
        assertEquals("public, max-age=3600", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertTrue(body(response).contains("\"servers\":[{\"slug\":\"notion\""));
    }

    @Test
    void testSecondCallIsServedFromCache() {
        when(reader.read()).thenReturn(catalog("notion"));

        McpServerController controller = new McpServerController(reader, OBJECT_MAPPER, 3600L, 30L);

        controller.listMcpServers();

        ResponseEntity<?> second = controller.listMcpServers();

        assertEquals("HIT", second.getHeaders().getFirst("X-Cache"));
        verify(reader, times(1)).read();
    }

    @Test
    void testLastGoodCatalogIsServedWhenGitHubFails() {
        when(reader.read())
            .thenReturn(catalog("notion"))
            .thenThrow(new GitHubUnavailableException("down"));

        McpServerController controller = new McpServerController(reader, OBJECT_MAPPER, 0L, 30L);

        controller.listMcpServers();

        ResponseEntity<?> response = controller.listMcpServers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("STALE", response.getHeaders().getFirst("X-Cache"));
        assertTrue(body(response).contains("notion"));
    }

    @Test
    void testBadGatewayWhenNothingWasEverLoaded() {
        when(reader.read()).thenThrow(new GitHubUnavailableException("down"));

        McpServerController controller = new McpServerController(reader, OBJECT_MAPPER, 3600L, 30L);

        ResponseEntity<?> response = controller.listMcpServers();

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(body(response).contains("down"));
    }

    private static String body(ResponseEntity<?> response) {
        Object body = response.getBody();

        return body instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(body);
    }
}
```

A TTL of `0` makes the Caffeine entry expire immediately, which is how the stale test forces a reload on the second call.

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests '*McpServerControllerTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 3: Implement the controller**

```java
package com.bytechef.workflows.proxy.web.rest;

import com.bytechef.workflows.proxy.GitHubUnavailableException;
import com.bytechef.workflows.proxy.mcp.GitHubMcpServerReader;
import com.bytechef.workflows.proxy.mcp.McpServerCatalog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Serves the MCP server catalog from the bytechef-mcp-server repository, cached, with the last good copy as a
 * fallback when GitHub is unavailable.
 *
 * @author Ivica Cardic
 */
@RestController
public class McpServerController {

    private static final Logger logger = LoggerFactory.getLogger(McpServerController.class);

    private static final String CATALOG_KEY = "catalog";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String NOSNIFF = "nosniff";

    private final GitHubMcpServerReader reader;
    private final ObjectMapper objectMapper;
    private final long cacheMaxAgeSeconds;
    private final Cache<String, McpServerCatalog> catalogCache;
    private final AtomicReference<McpServerCatalog> lastGoodCatalog = new AtomicReference<>();
    private final SingleFlight<McpServerCatalog> singleFlight;

    public McpServerController(
        GitHubMcpServerReader reader, ObjectMapper objectMapper,
        @Value("${mcp-servers.cache.ttl.seconds:3600}") long cacheTtlSeconds,
        @Value("${mcp-servers.wait.seconds:60}") long waitSeconds) {

        this.reader = reader;
        this.objectMapper = objectMapper;
        this.cacheMaxAgeSeconds = cacheTtlSeconds;
        this.singleFlight = new SingleFlight<>(waitSeconds);
        this.catalogCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
            .build();
    }

    @GetMapping("/mcp-servers")
    public ResponseEntity<?> listMcpServers() {
        McpServerCatalog cached = catalogCache.getIfPresent(CATALOG_KEY);

        if (cached != null) {
            return createJsonResponseEntity(cached, "HIT");
        }

        try {
            McpServerCatalog catalog = singleFlight.get("the MCP server catalog", this::load);

            return createJsonResponseEntity(catalog, "MISS");
        } catch (GitHubUnavailableException e) {
            McpServerCatalog stale = lastGoodCatalog.get();

            if (stale != null) {
                logger.warn("Serving the last good MCP server catalog: {}", e.getMessage());

                return createJsonResponseEntity(stale, "STALE");
            }

            return createBadGatewayResponseEntity(e.getMessage());
        } catch (RuntimeException e) {
            return createBadGatewayResponseEntity("Failed to build the MCP server catalog: " + e.getMessage());
        }
    }

    private McpServerCatalog load() {
        McpServerCatalog catalog = reader.read();

        catalogCache.put(CATALOG_KEY, catalog);
        lastGoodCatalog.set(catalog);

        return catalog;
    }

    private ResponseEntity<byte[]> createJsonResponseEntity(McpServerCatalog catalog, String cacheStatus) {
        byte[] json = objectMapper.writeValueAsBytes(Map.of("servers", catalog.servers()));

        return ResponseEntity.ok()
            .header("X-Cache", cacheStatus)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + cacheMaxAgeSeconds)
            .header(X_CONTENT_TYPE_OPTIONS, NOSNIFF)
            .contentType(MediaType.APPLICATION_JSON)
            .body(json);
    }

    private static ResponseEntity<String> createBadGatewayResponseEntity(String message) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .header(X_CONTENT_TYPE_OPTIONS, NOSNIFF)
            .contentType(MediaType.TEXT_PLAIN)
            .body(HtmlUtils.htmlEscape(message) + "\n");
    }
}
```

- [ ] **Step 4: Run the controller test and the whole proxy build**

Run: `./gradlew test --tests '*McpServerControllerTest' --offline -q && ./gradlew check --offline -q`
Expected: 4 tests pass; `check` BUILD SUCCESSFUL.

- [ ] **Step 5: Document the endpoint and properties in README.md**

After the "Fetch raw file bytes" bullet list in the endpoint section add:

```markdown
- **MCP server catalog** (curated list from `bytechefhq/bytechef-mcp-server`, parsed, validated and cached):
  ```
  GET /mcp-servers
  ```
  Responds `{"servers": [...]}`; serves the last good copy with `X-Cache: STALE` when GitHub is unavailable.
```

In "Configuration (env vars)" add a row/lines for: `MCP_SERVERS_REPO_OWNER` (`bytechefhq`), `MCP_SERVERS_REPO_NAME` (`bytechef-mcp-server`), `MCP_SERVERS_REPO_REF` (`master`), `MCP_SERVERS_FILE` (`servers.json`), `MCP_SERVERS_FETCH_TIMEOUT_SECONDS` (60), `MCP_SERVERS_CACHE_TTL_SECONDS` (3600), `MCP_SERVERS_WAIT_SECONDS` (60), matching the format the section already uses for the other properties.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/bytechef/workflows/proxy/web/rest/McpServerController.java src/test/java/com/bytechef/workflows/proxy/web/rest/McpServerControllerTest.java README.md
git commit -m "Serve the MCP server catalog at /mcp-servers"
```

---

### Task 5: ByteChef — `McpServerCatalogProxyClient`

**Files:**
- Create: `server/libs/platform/platform-github-proxy-client/src/main/java/com/bytechef/platform/githubproxy/client/McpServerCatalogProxyClient.java`
- Create: `.../client/internal/RestMcpServerCatalogProxyClient.java`
- Create: `.../client/model/McpServerCatalog.java`, `McpServerCatalogEntry.java`, `McpServerCatalogAuthentication.java`, `McpServerCatalogHeader.java`
- Modify: `.../client/config/GitHubProxyClientConfiguration.java`
- Modify: `server/libs/platform/platform-github-proxy-client/build.gradle.kts`
- Test: `server/libs/platform/platform-github-proxy-client/src/test/java/com/bytechef/platform/githubproxy/client/internal/RestMcpServerCatalogProxyClientTest.java`

**Interfaces (Produces):**

```java
public interface McpServerCatalogProxyClient {
    List<McpServerCatalogEntry> getMcpServerCatalog();   // throws RestClientException on HTTP errors,
                                                         // IllegalStateException on an empty/invalid body
}
public record McpServerCatalog(List<McpServerCatalogEntry> servers) {}
public record McpServerCatalogEntry(
    String slug, String title, String description, String url, List<String> urlParameters, String transportType,
    McpServerCatalogAuthentication authentication, String websiteUrl, String component) {}
public record McpServerCatalogAuthentication(
    String type, Boolean dynamicClientRegistration, Boolean clientIdMetadataDocument, String authorizationUrl,
    String tokenUrl, Boolean pkce, List<String> scopes, List<McpServerCatalogHeader> headers) {}
public record McpServerCatalogHeader(String name, String description, Boolean secret) {}
```

Work from `/Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a`.

- [ ] **Step 1: Add the WireMock test dependency**

In `platform-github-proxy-client/build.gradle.kts` add `testImplementation(libs.org.wiremock.wiremock)`.

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.platform.githubproxy.client.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class RestMcpServerCatalogProxyClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private RestMcpServerCatalogProxyClient client;

    @BeforeEach
    void beforeEach() {
        client = new RestMcpServerCatalogProxyClient(
            RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build());
    }

    @Test
    void testGetMcpServerCatalogParsesEntriesAndIgnoresUnknownFields() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"servers":[{"slug":"notion","title":"Notion","url":"https://mcp.notion.com/mcp",
                     "transportType":"httpStreamable","authentication":{"type":"oauth2","dynamicClientRegistration":true,
                     "futureAuthField":1},"component":"notion","futureField":"x"}]}
                    """)));

        List<McpServerCatalogEntry> entries = client.getMcpServerCatalog();

        assertThat(entries).hasSize(1);

        McpServerCatalogEntry entry = entries.getFirst();

        assertThat(entry.slug()).isEqualTo("notion");
        assertThat(entry.transportType()).isEqualTo("httpStreamable");
        assertThat(entry.authentication().type()).isEqualTo("oauth2");
        assertThat(entry.authentication().dynamicClientRegistration()).isTrue();
        assertThat(entry.description()).isNull();
    }

    @Test
    void testGetMcpServerCatalogFailsOnServerError() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(aResponse().withStatus(502)));

        assertThatThrownBy(client::getMcpServerCatalog).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void testGetMcpServerCatalogRejectsBodyWithoutServers() {
        wireMock.stubFor(get(urlEqualTo("/mcp-servers")).willReturn(
            aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));

        assertThatThrownBy(client::getMcpServerCatalog).isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-github-proxy-client:test --tests '*RestMcpServerCatalogProxyClientTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 4: Implement models, interface, client and bean**

Models (package `com.bytechef.platform.githubproxy.client.model`, Apache header copied from `WorkflowTemplateSummary.java`, `@SuppressFBWarnings("EI")` on records holding lists, exactly the record signatures in Interfaces above).

`McpServerCatalogProxyClient.java`:

```java
package com.bytechef.platform.githubproxy.client;

import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.util.List;

public interface McpServerCatalogProxyClient {

    /**
     * Fetches the curated MCP server catalog. Throws a {@code RestClientException} when the proxy answers with an
     * error status and an {@code IllegalStateException} when the body has no {@code servers} array.
     */
    List<McpServerCatalogEntry> getMcpServerCatalog();
}
```

`RestMcpServerCatalogProxyClient.java`:

```java
package com.bytechef.platform.githubproxy.client.internal;

import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalog;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public final class RestMcpServerCatalogProxyClient implements McpServerCatalogProxyClient {

    private final RestClient restClient;

    public RestMcpServerCatalogProxyClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    @Override
    public List<McpServerCatalogEntry> getMcpServerCatalog() {
        McpServerCatalog mcpServerCatalog = restClient.get()
            .uri("/mcp-servers")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(McpServerCatalog.class);

        if (mcpServerCatalog == null || mcpServerCatalog.servers() == null) {
            throw new IllegalStateException("Empty response from the MCP server catalog proxy");
        }

        return mcpServerCatalog.servers();
    }
}
```

`GitHubProxyClientConfiguration.java` — add:

```java
    @Bean
    McpServerCatalogProxyClient mcpServerCatalogProxyClient(GitHubProxyClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();

        return new RestMcpServerCatalogProxyClient(restClient);
    }
```

- [ ] **Step 5: Run the module check**

Run: `./gradlew :server:libs:platform:platform-github-proxy-client:check --offline -q`
Expected: BUILD SUCCESSFUL, 3 new tests green.

- [ ] **Step 6: Commit**

```bash
./gradlew :server:libs:platform:platform-github-proxy-client:spotlessApply --offline -q
git add server/libs/platform/platform-github-proxy-client
git commit -m "Add the MCP server catalog proxy client"
```

---

### Task 6: ByteChef — DTOs and `McpServerCatalogService`

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/dto/McpServerCatalogEntryDTO.java`, `McpServerCatalogAuthenticationDTO.java`, `McpServerCatalogHeaderDTO.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/service/McpServerCatalogService.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/service/McpServerCatalogServiceImpl.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/build.gradle.kts`
- Test: `server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/service/McpServerCatalogServiceTest.java`

**Interfaces:**
- Consumes: `McpServerCatalogProxyClient.getMcpServerCatalog()` and the `McpServerCatalog*` client records (Task 5).
- Produces:

```java
public record McpServerCatalogEntryDTO(
    String slug, String title, String description, String url, List<String> urlParameters, String transportType,
    McpServerCatalogAuthenticationDTO authentication, String websiteUrl, String component) {}
public record McpServerCatalogAuthenticationDTO(
    String type, Boolean dynamicClientRegistration, Boolean clientIdMetadataDocument, String authorizationUrl,
    String tokenUrl, Boolean pkce, List<String> scopes, List<McpServerCatalogHeaderDTO> headers) {}
public record McpServerCatalogHeaderDTO(String name, String description, Boolean secret) {}
public interface McpServerCatalogService { List<McpServerCatalogEntryDTO> getMcpServerCatalog(); }
```

`McpServerCatalogServiceImpl` constructors: `@Autowired (McpServerCatalogProxyClient)` and package-private `(McpServerCatalogProxyClient, Clock)`; constant `REFRESH_INTERVAL = Duration.ofHours(1)`.

- [ ] **Step 1: Add dependencies**

In `platform-configuration-service/build.gradle.kts` add `implementation(project(":server:libs:platform:platform-github-proxy-client"))`. (`platform-api` and `spring-security-core` are added in Task 7, where the facade needs them.)

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class McpServerCatalogServiceTest {

    private static final McpServerCatalogEntry NOTION = new McpServerCatalogEntry(
        "notion", "Notion", "Official Notion MCP server", "https://mcp.notion.com/mcp", null, "httpStreamable",
        new McpServerCatalogAuthentication("oauth2", true, null, null, null, null, null, null),
        "https://developers.notion.com/docs/mcp", "notion");

    private McpServerCatalogProxyClient proxyClient;
    private MutableClock clock;
    private McpServerCatalogServiceImpl service;

    @BeforeEach
    void beforeEach() {
        proxyClient = mock(McpServerCatalogProxyClient.class);
        clock = new MutableClock(Instant.parse("2026-10-14T10:00:00Z"));
        service = new McpServerCatalogServiceImpl(proxyClient, clock);
    }

    @Test
    void testFirstCallFetchesAndMapsTheCatalog() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        List<McpServerCatalogEntryDTO> entries = service.getMcpServerCatalog();

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().slug()).isEqualTo("notion");
        assertThat(entries.getFirst().authentication().dynamicClientRegistration()).isTrue();
    }

    @Test
    void testSecondCallWithinTheHourDoesNotFetchAgain() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(59));
        service.getMcpServerCatalog();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    @Test
    void testCatalogIsRefreshedAfterAnHour() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of(NOTION));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(61));
        service.getMcpServerCatalog();

        verify(proxyClient, times(2)).getMcpServerCatalog();
    }

    @Test
    void testEmptyCatalogIsCachedLikeAnyOther() {
        when(proxyClient.getMcpServerCatalog()).thenReturn(List.of());

        assertThat(service.getMcpServerCatalog()).isEmpty();
        assertThat(service.getMcpServerCatalog()).isEmpty();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    @Test
    void testFailureKeepsPreviousCatalogAndDoesNotRetryWithinTheHour() {
        when(proxyClient.getMcpServerCatalog())
            .thenReturn(List.of(NOTION))
            .thenThrow(new ResourceAccessException("proxy down"));

        service.getMcpServerCatalog();
        clock.advance(Duration.ofMinutes(61));

        assertThat(service.getMcpServerCatalog()).hasSize(1);

        clock.advance(Duration.ofMinutes(30));

        assertThat(service.getMcpServerCatalog()).hasSize(1);

        verify(proxyClient, times(2)).getMcpServerCatalog();
    }

    @Test
    void testFirstCallFailureReturnsEmptyAndDoesNotRetryWithinTheHour() {
        when(proxyClient.getMcpServerCatalog()).thenThrow(new ResourceAccessException("proxy down"));

        assertThat(service.getMcpServerCatalog()).isEmpty();
        clock.advance(Duration.ofMinutes(30));
        assertThat(service.getMcpServerCatalog()).isEmpty();

        verify(proxyClient, times(1)).getMcpServerCatalog();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
```

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*McpServerCatalogServiceTest' --offline -q`
Expected: compilation failure.

- [ ] **Step 4: Implement DTOs, interface and service**

DTO records in `platform-configuration-api` `dto` package with the signatures above (Apache header, `@SuppressFBWarnings("EI")` where a record holds a `List`).

`McpServerCatalogService.java` (api, `service` package):

```java
public interface McpServerCatalogService {

    /**
     * The curated catalog of hosted MCP servers. Never throws: when the catalog cannot be fetched the last good
     * copy is returned, or an empty list when none was ever fetched.
     */
    List<McpServerCatalogEntryDTO> getMcpServerCatalog();
}
```

`McpServerCatalogServiceImpl.java`:

```java
package com.bytechef.platform.configuration.service;

import com.bytechef.platform.configuration.dto.McpServerCatalogAuthenticationDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogHeaderDTO;
import com.bytechef.platform.githubproxy.client.McpServerCatalogProxyClient;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogAuthentication;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogEntry;
import com.bytechef.platform.githubproxy.client.model.McpServerCatalogHeader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpServerCatalogServiceImpl implements McpServerCatalogService {

    private static final Logger log = LoggerFactory.getLogger(McpServerCatalogServiceImpl.class);

    static final Duration REFRESH_INTERVAL = Duration.ofHours(1);

    private final Clock clock;
    private final McpServerCatalogProxyClient mcpServerCatalogProxyClient;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile List<McpServerCatalogEntryDTO> catalog = List.of();
    private volatile Instant lastRefreshAttempt;

    @Autowired
    public McpServerCatalogServiceImpl(McpServerCatalogProxyClient mcpServerCatalogProxyClient) {
        this(mcpServerCatalogProxyClient, Clock.systemUTC());
    }

    McpServerCatalogServiceImpl(McpServerCatalogProxyClient mcpServerCatalogProxyClient, Clock clock) {
        this.mcpServerCatalogProxyClient = mcpServerCatalogProxyClient;
        this.clock = clock;
    }

    @Override
    public List<McpServerCatalogEntryDTO> getMcpServerCatalog() {
        if (isRefreshDue() && refreshLock.tryLock()) {
            try {
                if (isRefreshDue()) {
                    refresh();
                }
            } finally {
                refreshLock.unlock();
            }
        }

        return catalog;
    }

    private boolean isRefreshDue() {
        Instant attempt = lastRefreshAttempt;

        return attempt == null || Duration.between(attempt, clock.instant())
            .compareTo(REFRESH_INTERVAL) >= 0;
    }

    private void refresh() {
        lastRefreshAttempt = clock.instant();

        try {
            catalog = mcpServerCatalogProxyClient.getMcpServerCatalog()
                .stream()
                .map(McpServerCatalogServiceImpl::toDTO)
                .toList();
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Unable to refresh the MCP server catalog; keeping {} cached entries: {}", catalog.size(),
                runtimeException.getMessage());
        }
    }

    private static McpServerCatalogEntryDTO toDTO(McpServerCatalogEntry entry) {
        return new McpServerCatalogEntryDTO(
            entry.slug(), entry.title(), entry.description(), entry.url(), entry.urlParameters(),
            entry.transportType(), toDTO(entry.authentication()), entry.websiteUrl(), entry.component());
    }

    private static McpServerCatalogAuthenticationDTO toDTO(McpServerCatalogAuthentication authentication) {
        if (authentication == null) {
            return null;
        }

        List<McpServerCatalogHeaderDTO> headers = authentication.headers() == null
            ? null
            : authentication.headers()
                .stream()
                .map(McpServerCatalogServiceImpl::toDTO)
                .toList();

        return new McpServerCatalogAuthenticationDTO(
            authentication.type(), authentication.dynamicClientRegistration(),
            authentication.clientIdMetadataDocument(), authentication.authorizationUrl(), authentication.tokenUrl(),
            authentication.pkce(), authentication.scopes(), headers);
    }

    private static McpServerCatalogHeaderDTO toDTO(McpServerCatalogHeader header) {
        return new McpServerCatalogHeaderDTO(header.name(), header.description(), header.secret());
    }
}
```

- [ ] **Step 5: Run the module check**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-api:check :server:libs:platform:platform-configuration:platform-configuration-service:check --offline -q`
Expected: BUILD SUCCESSFUL, 6 new tests green.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-configuration/platform-configuration-api server/libs/platform/platform-configuration/platform-configuration-service
git commit -m "Add the cached MCP server catalog service"
```

---

### Task 7: ByteChef — facade, GraphQL schema and controller

**Files:**
- Create: `server/libs/platform/platform-configuration/platform-configuration-api/src/main/java/com/bytechef/platform/configuration/facade/McpServerCatalogFacade.java`
- Create: `server/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/platform/configuration/facade/McpServerCatalogFacadeImpl.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-service/build.gradle.kts`
- Create: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/resources/graphql/mcp-server-catalog.graphqls`
- Create: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/main/java/com/bytechef/platform/configuration/web/graphql/McpServerCatalogGraphQlController.java`
- Modify: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/test/java/com/bytechef/platform/configuration/web/graphql/config/PlatformConfigurationGraphQlConfigurationSharedMocks.java`
- Test: `server/libs/platform/platform-configuration/platform-configuration-service/src/test/java/com/bytechef/platform/configuration/facade/McpServerCatalogFacadeTest.java`
- Test: `server/libs/platform/platform-configuration/platform-configuration-graphql/src/test/java/com/bytechef/platform/configuration/web/graphql/McpServerCatalogGraphQlControllerIntTest.java`

**Interfaces:**
- Consumes: `McpServerCatalogService.getMcpServerCatalog()` and the DTOs (Task 6); `AuthorityConstants.USER` from `com.bytechef.platform.security.constant.AuthorityConstants` (`platform-api`).
- Produces: `McpServerCatalogFacade { List<McpServerCatalogEntryDTO> getMcpServerCatalog(); }`; GraphQL query `mcpServerCatalog: [McpServerCatalogEntry!]!`.

- [ ] **Step 1: Write the failing tests**

`McpServerCatalogFacadeTest` (service module):

```java
package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.service.McpServerCatalogService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class McpServerCatalogFacadeTest {

    @Test
    void testGetMcpServerCatalogDelegatesToTheService() {
        McpServerCatalogService service = mock(McpServerCatalogService.class);
        McpServerCatalogEntryDTO entry = new McpServerCatalogEntryDTO(
            "notion", "Notion", null, "https://mcp.notion.com/mcp", null, "httpStreamable", null, null, null);

        when(service.getMcpServerCatalog()).thenReturn(List.of(entry));

        assertThat(new McpServerCatalogFacadeImpl(service).getMcpServerCatalog()).containsExactly(entry);
    }

    @Test
    void testGetMcpServerCatalogRequiresUserAuthority() throws NoSuchMethodException {
        Method method = McpServerCatalogFacadeImpl.class.getMethod("getMcpServerCatalog");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains(AuthorityConstants.USER);
    }
}
```

`McpServerCatalogGraphQlControllerIntTest` (graphql module):

```java
package com.bytechef.platform.configuration.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.dto.McpServerCatalogAuthenticationDTO;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.facade.McpServerCatalogFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class,
    McpServerCatalogGraphQlController.class
})
@GraphQlTest(
    controllers = McpServerCatalogGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformConfigurationGraphQlConfigurationSharedMocks
class McpServerCatalogGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private McpServerCatalogFacade mcpServerCatalogFacade;

    @Test
    void testMcpServerCatalogReturnsEntriesWithAuthentication() {
        when(mcpServerCatalogFacade.getMcpServerCatalog()).thenReturn(List.of(
            new McpServerCatalogEntryDTO(
                "notion", "Notion", "Official Notion MCP server", "https://mcp.notion.com/mcp", null,
                "httpStreamable",
                new McpServerCatalogAuthenticationDTO("oauth2", true, null, null, null, null, null, null),
                "https://developers.notion.com/docs/mcp", "notion")));

        graphQlTester.document("""
            query {
                mcpServerCatalog {
                    slug
                    title
                    url
                    transportType
                    authentication {
                        type
                        dynamicClientRegistration
                    }
                    component
                }
            }
            """)
            .execute()
            .path("mcpServerCatalog[0].slug")
            .entity(String.class)
            .isEqualTo("notion")
            .path("mcpServerCatalog[0].authentication.type")
            .entity(String.class)
            .isEqualTo("oauth2")
            .path("mcpServerCatalog[0].authentication.dynamicClientRegistration")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
}
```

Add `McpServerCatalogFacade.class` to the `@MockitoBean(types = {...})` list in `PlatformConfigurationGraphQlConfigurationSharedMocks`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:test --tests '*McpServerCatalogFacadeTest' :server:libs:platform:platform-configuration:platform-configuration-graphql:testIntegration --tests '*McpServerCatalogGraphQlControllerIntTest' --offline -q`
Expected: compilation failures.

- [ ] **Step 3: Implement facade, schema and controller**

`platform-configuration-service/build.gradle.kts`: add `implementation("org.springframework.security:spring-security-core")` and `implementation(project(":server:libs:platform:platform-api"))`.

`McpServerCatalogFacade.java` (api, `facade` package):

```java
public interface McpServerCatalogFacade {

    List<McpServerCatalogEntryDTO> getMcpServerCatalog();
}
```

`McpServerCatalogFacadeImpl.java` (service module, `facade` package):

```java
package com.bytechef.platform.configuration.facade;

import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.service.McpServerCatalogService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class McpServerCatalogFacadeImpl implements McpServerCatalogFacade {

    private final McpServerCatalogService mcpServerCatalogService;

    public McpServerCatalogFacadeImpl(McpServerCatalogService mcpServerCatalogService) {
        this.mcpServerCatalogService = mcpServerCatalogService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<McpServerCatalogEntryDTO> getMcpServerCatalog() {
        return mcpServerCatalogService.getMcpServerCatalog();
    }
}
```

`mcp-server-catalog.graphqls`:

```graphql
extend type Query {
    mcpServerCatalog: [McpServerCatalogEntry!]!
}

type McpServerCatalogEntry {
    slug: String!
    title: String!
    description: String
    url: String!
    urlParameters: [String!]
    transportType: String!
    authentication: McpServerCatalogAuthentication!
    websiteUrl: String
    component: String
}

type McpServerCatalogAuthentication {
    type: String!
    dynamicClientRegistration: Boolean
    clientIdMetadataDocument: Boolean
    authorizationUrl: String
    tokenUrl: String
    pkce: Boolean
    scopes: [String!]
    headers: [McpServerCatalogHeader!]
}

type McpServerCatalogHeader {
    name: String!
    description: String
    secret: Boolean
}
```

`McpServerCatalogGraphQlController.java`:

```java
package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.dto.McpServerCatalogEntryDTO;
import com.bytechef.platform.configuration.facade.McpServerCatalogFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@ConditionalOnCoordinator
class McpServerCatalogGraphQlController {

    private final McpServerCatalogFacade mcpServerCatalogFacade;

    @SuppressFBWarnings("EI")
    McpServerCatalogGraphQlController(McpServerCatalogFacade mcpServerCatalogFacade) {
        this.mcpServerCatalogFacade = mcpServerCatalogFacade;
    }

    @QueryMapping
    public List<McpServerCatalogEntryDTO> mcpServerCatalog() {
        return mcpServerCatalogFacade.getMcpServerCatalog();
    }
}
```

- [ ] **Step 4: Run tests and checks**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-service:check :server:libs:platform:platform-configuration:platform-configuration-graphql:check :server:libs:platform:platform-configuration:platform-configuration-graphql:testIntegration --offline -q`
Expected: BUILD SUCCESSFUL; the facade test and the GraphQL IntTest pass; existing GraphQL IntTests still pass with the schema addition.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply --offline -q
git add server/libs/platform/platform-configuration
git commit -m "Expose the MCP server catalog through a guarded facade and GraphQL query"
```

---

### Task 8: ByteChef client operation, generated types and docs

**Files:**
- Create: `client/src/graphql/platform/mcp-server-catalog/mcpServerCatalog.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts` (and `client/src/shared/middleware/graphql-types*` if codegen writes there)
- Modify: `docs/content/docs/platform/use-bytechef/self-hosted/configuration/environment-variables.md:321`

**Interfaces:**
- Consumes: the `mcpServerCatalog` query (Task 7).
- Produces: a generated `useMcpServerCatalogQuery` hook for a future picker. No component uses it in this change.

- [ ] **Step 1: Add the operation**

`client/src/graphql/platform/mcp-server-catalog/mcpServerCatalog.graphql`:

```graphql
query mcpServerCatalog {
    mcpServerCatalog {
        slug
        title
        description
        url
        urlParameters
        transportType
        authentication {
            type
            dynamicClientRegistration
            clientIdMetadataDocument
            authorizationUrl
            tokenUrl
            pkce
            scopes
            headers {
                name
                description
                secret
            }
        }
        websiteUrl
        component
    }
}
```

Commit it alone:

```bash
git add client/src/graphql/platform/mcp-server-catalog/mcpServerCatalog.graphql
git commit -m "client - Add the mcpServerCatalog GraphQL operation"
```

- [ ] **Step 2: Regenerate and verify**

Run: `cd client && npm run codegen && npm run check`
Expected: `graphql.ts` gains `McpServerCatalogQuery` types and `useMcpServerCatalogQuery`; `git status` shows only generated files changed; lint, typecheck and tests pass (grep the vitest output for `Unhandled`).

Commit the generated output alone:

```bash
git add client/src/shared/middleware
git commit -m "client - Regenerate GraphQL types for the MCP server catalog"
```

- [ ] **Step 3: Update the docs line**

In `environment-variables.md` line 321, change the `GITHUB_PROXY_BASE_URL` description to: `Base URL of the ByteChef GitHub proxy used to fetch pre-built workflow templates and the MCP server catalog. Point it at a private mirror when the instance cannot reach the public proxy. Note: this property is top-level (`github-proxy.baseUrl`), not under the `bytechef.*` prefix.`

```bash
git add docs/content/docs/platform/use-bytechef/self-hosted/configuration/environment-variables.md
git commit -m "docs - Mention the MCP server catalog in the GitHub proxy variable description"
```

---

### Task 9: Gates

- [ ] **Step 1: Proxy**

```bash
cd /Volumes/Data/bytechef/bytechef-github-proxy && ./gradlew check --offline -q && git log --oneline master..mcp-servers
```

Expected: BUILD SUCCESSFUL; three commits on `mcp-servers`.

- [ ] **Step 2: ByteChef server**

```bash
cd /Volumes/Data/bytechef/bytechef/.claude/worktrees/n8n-mcp-registry-c4f43a && ./gradlew spotlessCheck \
  :server:libs:platform:platform-github-proxy-client:check \
  :server:libs:platform:platform-configuration:platform-configuration-api:check \
  :server:libs:platform:platform-configuration:platform-configuration-service:check \
  :server:libs:platform:platform-configuration:platform-configuration-graphql:check \
  :server:libs:platform:platform-configuration:platform-configuration-graphql:testIntegration --offline -q
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: ByteChef client**

```bash
cd client && npm run check 2>&1 | tee /tmp/client-check.log | tail -5; grep -c Unhandled /tmp/client-check.log
```

Expected: pass; `0` unhandled errors.

- [ ] **Step 4: Catalog repo**

```bash
cd /Volumes/Data/bytechef/bytechef-mcp-server && python3 -c "import json; d=json.load(open('servers.json')); assert d['version']==1 and len(d['servers'])==79 and len({s['slug'] for s in d['servers']})==79 and not any('registryName' in s or 'sources' in s for s in d['servers']); print('ok')" && git log --oneline
```

Expected: `ok`; one commit.

- [ ] **Step 5: Boot smoke (if port 9555 is free)**

Start `./gradlew -p server/apps/server-app bootRun --offline` in the background, wait for `Started`, grep the log for `APPLICATION FAILED TO START`, `UnsatisfiedDependencyException`, `NoSuchBeanDefinitionException` and GraphQL schema errors (`SchemaProblem`, `type McpServer`), then stop it. If the port is held by another process, report that instead of stopping it.

---

## Self-Review Notes

- **Spec coverage:** §1 catalog repo → Task 1; §2 proxy (parser, reader, controller, properties, README) → Tasks 2–4; §3 ByteChef (client + bean + timeouts → Task 5; DTOs + service → Task 6; facade + GraphQL + guard → Task 7; client operation + codegen + docs → Task 8); error-handling table → Tasks 2, 4, 6; testing section → each task's tests plus Task 9 gates; out-of-scope items untouched.
- **Type consistency:** `McpServerCatalogEntry`/`McpServerCatalogAuthentication`/`McpServerCatalogHeader`/`McpServerCatalog` (client models, Task 5) feed `McpServerCatalogEntryDTO`/`McpServerCatalogAuthenticationDTO`/`McpServerCatalogHeaderDTO` (Task 6) which the facade (Task 7) and GraphQL types (same component names) return. `getMcpServerCatalog()` is the method name on client, service and facade. Proxy `McpServerCatalog(int version, List<JsonNode> servers)` is a different type in a different repo.
- **Deployment note:** ByteChef works against a proxy that does not yet serve `/mcp-servers` (404 → `HttpClientErrorException` → WARN, empty catalog) and against a repo that is not yet published (proxy 502 → same). Nothing is pushed by this plan; the user publishes `bytechef-mcp-server`, deploys the proxy, and merges the ByteChef branch.
