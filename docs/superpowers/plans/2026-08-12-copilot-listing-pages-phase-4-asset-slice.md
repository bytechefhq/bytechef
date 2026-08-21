# Copilot Listing Pages — Phase 4: Asset-File Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Copilot panel, AI Hub, and the management MCP server one shared asset-file
capability — listing, reading, generating, editing, cloning, and (new) fetching a file from a URL —
exposed on the Files listing page as a `Source.ASSET_FILE` slice.

**Architecture:** Follow the Data Table tri-surface template exactly. A CE
`AssetFileToolCallbacksFactory` in `automation-ai-tool` owns the read/write tool lists. A CE
`AssetFileAgentConfiguration` in `ai-copilot-service` builds two panel agents
(`asset_file_ask` / `asset_file_build`) and two sub-agent `ChatClient` beans. The BUILD client is
wrapped as an `asset_file_agent` delegate on both the management MCP server (workspace-scoped) and
the AI Hub root agent, replacing AI Hub's direct registration of asset-file write tools. One new
tool, `createAssetFileFromUrl`, downloads server-side so file bytes never enter the model context.

**Tech Stack:** Java 25 / Spring Boot 4, Spring AI `ToolCallback`, JDK `java.net.http.HttpClient`,
JUnit 5 + Mockito + AssertJ, React 19 / TypeScript, TanStack Query, Zustand.

## Global Constraints

- Base branch: `claude/copilot-phase3-manager-slices`; worktree
  `/Volumes/Data/bytechef/bytechef/.claude/worktrees/copilot-phase4` on branch
  `claude/copilot-phase4-asset-slice`. Never rebase onto `0_732` — it is rewritten frequently.
- **Never invent a ticket number in a commit message.** No commit in this phase carries one. Use
  the plain form: `client - <description>` for client changes, `<description>` for server changes.
- CE files carry the Apache 2.0 header (copy verbatim from a neighbouring file in the same module).
  Only `server/ee/**` files carry the ByteChef Enterprise header plus a `@version ee` Javadoc tag.
  Nothing in this phase creates an EE file.
- Verification runs module-scoped **`check`**, never bare `test`:
  `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check`. Redirect to a file and
  inspect `$?` plus `grep '^> Task .* FAILED'` — a piped Gradle run hides its exit code.
- Run `./gradlew spotlessApply` before every server commit; run `npm run check` from `client/`
  before every client commit.
- Java style: one blank line before control statements, one blank line between a variable
  modification and the statement that uses it, no trailing blank line before a class's closing brace,
  descriptive variable names (never `f`, `u`, `e2`), no `_` prefix on private methods.
- Test method names are camelCase with no underscores (`testCallRejectsPrivateAddress`, never
  `testCall_RejectsPrivateAddress`). This applies to private helpers in test sources too.
- Checkstyle forbids `TODO:` comments and empty blocks.
- Client: object keys sort alphabetically (`sort-keys`, not auto-fixable); named imports sort
  alphabetically inside `{}`; Lucide icons import with the `Icon` suffix; `useRef` variables end in
  `Ref`; hook order is `useState` → `useRef` → store hooks → other hooks → derived → `useEffect` →
  `return`.
- Tool callbacks never throw out of `call(...)`. Every failure path returns a JSON
  `{"error": "..."}` envelope. The outer `catch (RuntimeException)` logs at WARN with the stack and
  returns a class-name-only message so internal detail never reaches the model.
- A prompt must never name a tool that is not registered on that agent — the model will call it and
  the turn dies with "No ToolCallback found".

## Prerequisite from phase 3 (read before Task 3)

Phase 3's whole-branch review found that `ManagerSliceSpringAIAgent.toolContext(...)` wrote only the
`bytechef.agentTool.*` key family, while the tools on those slices read
`AutomationToolInvocationContext` (`bytechef.assetFile.*`) — so every tool resolved a null workspace
and failed the turn. That is fixed on `claude/copilot-phase3-manager-slices`, and **this phase's
branch must contain that fix before Task 3 is verified**, because every asset-file tool reads the
same `AutomationToolInvocationContext` family. Confirm with:

```bash
grep -n "TOOL_CONTEXT_WORKSPACE_ID_KEY" server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/agent/ManagerSliceSpringAIAgent.java
```

Expected: a match. If there is none, stop and report — the slice will compile and its tests will
pass, but every tool will fail at runtime.

**Known provenance gap, accepted for this phase.** That fix carries `workspaceId`, `userId` and
`environmentId` from the panel state, but not `threadId`, `sourceOrdinal` or `lastUserPrompt` —
the panel has no AI Hub task thread to source them from. Consequences for asset files created from
the Copilot panel: `generatedByAgentSource` and `generatedFromPrompt` are persisted as `null`, and
`ToolArtifactRecorder.record(...)` is skipped (every asset tool already guards on
`threadId != null`). Files still save correctly and appear on the page; they simply carry no
generator attribution. Do not attempt to synthesise a thread id to work around this.

## Scope correction (read before Task 1)

The design spec lists "FirecrawlTools (conditional)" as a BUILD-mode tool on this slice. **There is
no `FirecrawlTools` class in this repository.** The only Firecrawl code is
`FirecrawlWebScrapeService` (EE, `platform-api-connector-configuration-service`), consumed solely by
`ResearchToolCallback` in AI Hub. Wiring it into a CE slice would invert the CE→EE dependency
direction, which the spec's module-placement rule forbids.

The slice therefore ships **without** Firecrawl. `createAssetFileFromUrl` is the fetch capability,
and it is a strictly better fit: it persists bytes server-side rather than round-tripping page text
through the model. No prompt in this phase mentions Firecrawl.

## File Structure

**Create:**

| Path | Responsibility |
|---|---|
| `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/CreateAssetFileFromUrlToolCallback.java` | Download a URL server-side, validate scheme/host/size/mime, persist as an AI-attributed asset file |
| `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/CreateAssetFileFromUrlToolCallbackTest.java` | Unit tests for the new tool |
| `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AssetFileToolCallbacksFactory.java` | Read/write tool lists shared by all three surfaces |
| `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/AssetFileToolCallbacksFactoryTest.java` | Pins the exact tool-name sets |
| `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/AssetFileAgentConfiguration.java` | Panel ASK/BUILD agents + sub-agent `ChatClient` beans |
| `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_asset_file_ask.txt` | ASK prompt |
| `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_asset_file_build.txt` | BUILD prompt |
| `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/AssetFileAgentConfigurationTest.java` | Pins bean ids and prompt-vs-tool consistency |
| `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AssetFileAgentToolCallback.java` | `asset_file_agent` delegate |

**Modify:**

| Path | Change |
|---|---|
| `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java` | Add `ASSET_FILE` |
| `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java` | Add `ASSET_FILE_ASK` / `ASSET_FILE_BUILD` / `ASSET_FILE` / `ASSET_FILE_AGENT` |
| `server/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts` | Add `automation-asset-file-api` dependency |
| `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java` | Contribute `asset_file_agent` to MCP, workspace-scoped |
| `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java` | Register `asset_file_agent`; drop direct write-tool registrations |
| `client/src/shared/components/copilot/stores/useCopilotStore.ts` | Add `ASSET_FILE` to `Source` |
| `client/src/pages/automation/asset-files/AssetFiles.tsx` | `CopilotButton` + post-turn invalidation |

---

### Task 1: `createAssetFileFromUrl` tool

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/CreateAssetFileFromUrlToolCallback.java`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/CreateAssetFileFromUrlToolCallbackTest.java`

**Interfaces:**
- Consumes: `AssetFileFacade.createFromAi(Long, int, String, String, String, AssetFileFormat, String, Short, String)` and
  `AssetFileFacade.createBinaryFromAi(Long, int, String, String, byte[], AssetFileFormat, String, Short, String)`;
  `AutomationToolInvocationContext.fromToolContext(ToolContext)` / `.resolveEnvironmentOrDefault(...)`;
  `ToolArtifactRecorder.record(String, Long, String, String, String)`;
  `UrlValidator.validate(String, Set<String>)` throwing `UrlValidationException`.
- Produces: `public CreateAssetFileFromUrlToolCallback(AssetFileFacade, @Nullable ToolArtifactRecorder)`
  and a package-private test constructor
  `CreateAssetFileFromUrlToolCallback(AssetFileFacade, @Nullable ToolArtifactRecorder, HttpClient, int, Set<String>)`.
  Tool name: `createAssetFileFromUrl`.

**Design notes the implementer must not re-litigate:**

- **Why a new tool at all.** The only existing binary ingress is `createBinaryAssetFile`, which
  requires the model to emit the whole file as inline base64 — for anything beyond a tiny icon that
  is unusable, and its 64 MB guard exists precisely to stop a hallucinating model exhausting heap.
  Downloading server-side removes the payload from the context window entirely.
- **Redirects are refused, not followed.** `UrlValidator`'s own Javadoc flags that the JDK
  `HttpClient` has no DNS-resolver hook, so a followed redirect would land on an unvalidated host.
  Build the client with `Redirect.NEVER` and return a typed error naming the `Location` value so the
  model can retry with the final URL. This is a deliberate, documented refusal — not an oversight.
- **Two-stage size guard**, mirroring `CreateBinaryAssetFileToolCallback`: reject on
  `Content-Length` when the server sends one, then read through a bounded loop that aborts past the
  cap. The bounded read is the load-bearing check — `Content-Length` can lie or be absent on a
  chunked response.
- **Text vs binary dispatch on the normalized mime type**, so both branches persist through an
  AI-attributed facade method. `createFromUpload` is stream-based but records `USER_UPLOAD`, which
  would be a false audit trail for a model-initiated download.

- [ ] **Step 1: Write the failing tests**

Create the test file. Copy the Apache header verbatim from
`CreateBinaryAssetFileToolCallback.java` in the same module.

```java
package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class CreateAssetFileFromUrlToolCallbackTest {

    private static final Set<String> ALLOWED_TEST_HOSTS = Set.of("files.example.com");

    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);

    @Test
    void testGetToolDefinitionName() {
        CreateAssetFileFromUrlToolCallback toolCallback =
            new CreateAssetFileFromUrlToolCallback(assetFileFacade, null);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("createAssetFileFromUrl");
    }

    @Test
    void testCallStoresTextResponseThroughCreateFromAi() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(7L);
        assetFile.setName("report.csv");

        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/csv; charset=utf-8", "id,name\n1,alpha\n".getBytes(StandardCharsets.UTF_8)),
            64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/report.csv\"}", workspaceToolContext());

        assertThat(result).contains("\"id\":7")
            .contains("report.csv");

        verify(assetFileFacade).createFromAi(
            eq(1L), anyInt(), eq("report.csv"), eq("text/csv"), eq("id,name\n1,alpha\n"), any(), any(), any(),
            any());
    }

    @Test
    void testCallStoresBinaryResponseThroughCreateBinaryFromAi() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(9L);
        assetFile.setName("chart.png");

        when(assetFileFacade.createBinaryFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "image/png", new byte[] {
                1, 2, 3
            }), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/chart.png\"}", workspaceToolContext());

        assertThat(result).contains("\"id\":9");

        verify(assetFileFacade).createBinaryFromAi(
            eq(1L), anyInt(), eq("chart.png"), eq("image/png"), any(), any(), any(), any(), any());
    }

    @Test
    void testCallHonoursExplicitFilename() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(11L);
        assetFile.setName("notes.md");

        when(assetFileFacade.createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(assetFile);

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/markdown", "# hi".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        toolCallback.call(
            "{\"url\":\"https://files.example.com/raw\",\"filename\":\"notes.md\"}", workspaceToolContext());

        verify(assetFileFacade).createFromAi(
            anyLong(), anyInt(), eq("notes.md"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCallRejectsPrivateAddress() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "secret".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"http://127.0.0.1:8080/admin\"}", workspaceToolContext());

        assertThat(result).contains("error");

        verify(assetFileFacade, never()).createFromAi(
            anyLong(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCallRejectsUnsupportedMimeType() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "application/x-msdownload", new byte[] {
                4
            }), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/setup.exe\"}", workspaceToolContext());

        assertThat(result).contains("Unsupported mime type");
    }

    @Test
    void testCallRejectsOversizePayload() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "abcdefghij".getBytes(StandardCharsets.UTF_8)), 4);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/big.txt\"}", workspaceToolContext());

        assertThat(result).contains("exceeds");
    }

    @Test
    void testCallRejectsRedirect() throws Exception {
        HttpResponse<InputStream> response = stubResponse(302, "text/plain", new byte[0]);

        when(response.headers()).thenReturn(
            HttpHeaders.of(Map.of("location", List.of("https://elsewhere.example.com/x")), (name, value) -> true));

        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(response, 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/moved\"}", workspaceToolContext());

        assertThat(result).contains("redirect");
    }

    @Test
    void testCallWithoutWorkspaceContextReturnsError() throws Exception {
        CreateAssetFileFromUrlToolCallback toolCallback = newToolCallback(
            stubResponse(200, "text/plain", "hello".getBytes(StandardCharsets.UTF_8)), 64 * 1024 * 1024);

        String result = toolCallback.call(
            "{\"url\":\"https://files.example.com/a.txt\"}", new ToolContext(Map.of()));

        assertThat(result).contains("Workspace context unavailable");
    }

    @SuppressWarnings("unchecked")
    private CreateAssetFileFromUrlToolCallback newToolCallback(
        HttpResponse<InputStream> response, int maxDownloadBytes) throws Exception {

        HttpClient httpClient = mock(HttpClient.class);

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);

        return new CreateAssetFileFromUrlToolCallback(
            assetFileFacade, null, httpClient, maxDownloadBytes, ALLOWED_TEST_HOSTS);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<InputStream> stubResponse(int statusCode, String contentType, byte[] body)
        throws IOException {

        HttpResponse<InputStream> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(new ByteArrayInputStream(body));
        when(response.headers()).thenReturn(
            HttpHeaders.of(Map.of("content-type", List.of(contentType)), (name, value) -> true));

        return response;
    }

    private ToolContext workspaceToolContext() {
        return new ToolContext(
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 1L));
    }
}
```

Note: `stubResponse` declares `throws IOException` only so the stubbing calls compose with the
mocked `send`; if the compiler reports it as unnecessary, drop the clause. Verify
`AssetFile` exposes no-arg construction plus `setId`/`setName`; if it does not, build the fixture the
way `CreateBinaryAssetFileToolCallbackTest` does and mirror that.

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*CreateAssetFileFromUrlToolCallbackTest' > /tmp/p4t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED|error:' /tmp/p4t1.log | head -20
```

Expected: compilation failure — `CreateAssetFileFromUrlToolCallback` does not exist.

- [ ] **Step 3: Implement the tool**

Create `CreateAssetFileFromUrlToolCallback.java` with the Apache header copied verbatim from
`CreateBinaryAssetFileToolCallback.java`.

```java
package com.bytechef.automation.ai.tool;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidator;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that downloads a public URL server-side and stores the response as a workspace asset
 * file. Keeps file bytes out of the model's context entirely — the alternative, {@code createBinaryAssetFile}, requires
 * the model to emit the whole payload as inline base64.
 *
 * <p>
 * Redirects are refused rather than followed: {@link UrlValidator} runs before the request, and the JDK
 * {@link HttpClient} exposes no resolver hook, so a followed hop would reach an unvalidated host. The tool returns a
 * typed error naming the {@code Location} value so the agent can retry with the final URL.
 *
 * @author Ivica Cardic
 */
public class CreateAssetFileFromUrlToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateAssetFileFromUrlToolCallback.class);

    /**
     * Upper bound on a single download, mirroring {@link CreateBinaryAssetFileToolCallback#MAX_BINARY_BYTES}.
     */
    static final int MAX_DOWNLOAD_BYTES = 64 * 1024 * 1024;

    private static final Set<String> TEXT_MIME_TYPES = Set.of(
        "text/markdown", "text/csv", "text/plain", "application/json",
        "text/javascript", "text/x-python", "text/x-java",
        "text/html", "text/css", "text/yaml", "application/xml");

    private static final Set<String> BINARY_MIME_TYPES = Set.of(
        "image/png", "image/jpeg", "image/webp", "image/gif",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private static final String DESCRIPTION = """
        Download a file from a public http(s) URL and save it into the user's workspace files. Use this
        when the user asks you to fetch, grab, or import something from a link — the bytes are downloaded
        on the server, so large files cost nothing in this conversation. Supported content types are the
        text formats (markdown, csv, plain, json, xml, html, css, yaml, and source code) plus png, jpeg,
        webp, gif, pdf, and pptx. URLs that redirect are refused: read the error and retry with the final
        URL. Private, loopback, and internal addresses are refused.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "url": {"type": "string", "description": "Public http or https URL to download"},
                "filename": {"type": "string", "description": "Optional filename override; defaults to the last URL path segment"},
                "description": {"type": "string", "description": "Optional short description"}
            },
            "required": ["url"]
        }""";

    private final AssetFileFacade facade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;
    private final HttpClient httpClient;
    private final int maxDownloadBytes;
    private final Set<String> allowedHosts;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateAssetFileFromUrlToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this(
            facade, artifactRecorder,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
            MAX_DOWNLOAD_BYTES, Set.of());
    }

    /**
     * Test-friendly constructor: a stubbed {@link HttpClient} keeps the tests offline, a small byte bound drives the
     * rejection path without allocating megabytes, and {@code allowedHosts} lets a fixture host bypass the SSRF
     * resolver so the happy-path tests do not depend on DNS.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    CreateAssetFileFromUrlToolCallback(
        AssetFileFacade facade, @Nullable ToolArtifactRecorder artifactRecorder, HttpClient httpClient,
        int maxDownloadBytes, Set<String> allowedHosts) {

        this.facade = facade;
        this.artifactRecorder = artifactRecorder;
        this.httpClient = httpClient;
        this.maxDownloadBytes = maxDownloadBytes;
        this.allowedHosts = Set.copyOf(allowedHosts);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createAssetFileFromUrl")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            CreateAssetFileFromUrlInput input = jsonMapper.readValue(toolInput, CreateAssetFileFromUrlInput.class);

            String url = input.url();

            if (url == null || url.isBlank()) {
                return toolError("url is required");
            }

            try {
                UrlValidator.validate(url, allowedHosts);
            } catch (UrlValidationException exception) {
                return toolError("Refused to download from this URL: " + exception.getMessage());
            }

            URI uri;

            try {
                uri = URI.create(url);
            } catch (IllegalArgumentException exception) {
                return toolError("Malformed url: " + exception.getMessage());
            }

            AutomationToolInvocationContext invocationContext =
                AutomationToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable - open this chat from the Files panel of a workspace.");
            }

            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<InputStream> response =
                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();

            if (statusCode >= 300 && statusCode < 400) {
                String location = response.headers()
                    .firstValue("location")
                    .orElse("(no Location header)");

                return toolError(
                    "The URL issued a redirect to %s. Redirects are not followed; retry with the final URL."
                        .formatted(location));
            }

            if (statusCode < 200 || statusCode >= 300) {
                return toolError("Download failed with HTTP status " + statusCode);
            }

            Optional<String> declaredLength = response.headers()
                .firstValue("content-length");

            if (declaredLength.isPresent()) {
                long declaredBytes;

                try {
                    declaredBytes = Long.parseLong(declaredLength.get());
                } catch (NumberFormatException exception) {
                    declaredBytes = -1L;
                }

                if (declaredBytes > maxDownloadBytes) {
                    return toolError(
                        "Download exceeds the maximum allowed size of " + maxDownloadBytes + " bytes");
                }
            }

            String mimeType = response.headers()
                .firstValue("content-type")
                .map(CreateAssetFileFromUrlToolCallback::normalizeMimeType)
                .orElse("application/octet-stream");

            boolean textMimeType = TEXT_MIME_TYPES.contains(mimeType);

            if (!textMimeType && !BINARY_MIME_TYPES.contains(mimeType)) {
                return toolError("Unsupported mime type: %s".formatted(mimeType));
            }

            byte[] data;

            try (InputStream inputStream = response.body()) {
                data = readBounded(inputStream, maxDownloadBytes);
            }

            if (data == null) {
                return toolError("Download exceeds the maximum allowed size of " + maxDownloadBytes + " bytes");
            }

            String filename = resolveFilename(input.filename(), uri);
            int environment = AutomationToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            AssetFile created;

            if (textMimeType) {
                created = facade.createFromAi(
                    workspaceId, environment, filename, mimeType, new String(data, StandardCharsets.UTF_8), null,
                    null, invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());
            } else {
                created = facade.createBinaryFromAi(
                    workspaceId, environment, filename, mimeType, data, null, null,
                    invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());
            }

            String threadId = invocationContext.threadId();

            if (artifactRecorder != null && threadId != null) {
                artifactRecorder.record(
                    threadId, invocationContext.userId(), "FILE_CREATED", String.valueOf(created.getId()),
                    created.getName());
            }

            return jsonMapper.writeValueAsString(
                new CreateAssetFileFromUrlOutput(
                    created.getId(),
                    created.getName(),
                    "/api/automation/internal/asset-files/%d/content".formatted(created.getId()),
                    created.getSizeBytes()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IOException exception) {
            return toolError("Download failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            return toolError("Download was interrupted");
        } catch (RuntimeException exception) {
            // Catch-all so a transient DB outage or file-storage 5xx returns a recoverable tool error instead of
            // aborting the whole agent run. The message omits exception.getMessage() to keep internal detail away
            // from the model.
            log.warn("createAssetFileFromUrl failed: {}", exception.toString(), exception);

            return toolError("createAssetFileFromUrl failed (" + exception.getClass()
                .getSimpleName() + ")");
        }
    }

    private static byte @Nullable [] readBounded(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;

        while ((read = inputStream.read(chunk)) != -1) {
            total += read;

            if (total > maxBytes) {
                return null;
            }

            buffer.write(chunk, 0, read);
        }

        return buffer.toByteArray();
    }

    private static String normalizeMimeType(String contentTypeHeader) {
        int separatorIndex = contentTypeHeader.indexOf(';');
        String mimeType = separatorIndex < 0 ? contentTypeHeader : contentTypeHeader.substring(0, separatorIndex);

        return mimeType.trim()
            .toLowerCase(Locale.ROOT);
    }

    private static String resolveFilename(@Nullable String requestedFilename, URI uri) {
        if (requestedFilename != null && !requestedFilename.isBlank()) {
            return requestedFilename.trim();
        }

        String path = uri.getPath();

        if (path != null) {
            int slashIndex = path.lastIndexOf('/');
            String lastSegment = slashIndex < 0 ? path : path.substring(slashIndex + 1);

            if (!lastSegment.isBlank()) {
                return lastSegment;
            }
        }

        return "download";
    }

    private String toolError(String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (JacksonException exception) {
            return "{\"error\":\"serialization failure\"}";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateAssetFileFromUrlInput(String url, String filename, String description) {
    }

    public record CreateAssetFileFromUrlOutput(long id, String name, String downloadUrl, long sizeBytes) {
    }
}
```

Confirm the exact package and constructor of `UrlValidationException` before compiling; if it lives
in a different package than `UrlValidator`, fix the import. If `UrlValidator.validate` throws an
unchecked type that is not `UrlValidationException`, catch whatever it actually throws.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*CreateAssetFileFromUrlToolCallbackTest' > /tmp/p4t1.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p4t1.log
```

Expected: `exit=0`, no FAILED tasks.

- [ ] **Step 5: Run the module check**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check > /tmp/p4t1c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p4t1c.log
```

Expected: `exit=0`. If SpotBugs flags `CT_CONSTRUCTOR_THROW` on the test class, declare the test
class `final` (the precedent is `MultiIssuerJwtDecoderTest`).

- [ ] **Step 6: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src
git commit -m "Add the createAssetFileFromUrl asset-file tool"
```

---

### Task 2: `AssetFileToolCallbacksFactory`

**Files:**
- Create: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/AssetFileToolCallbacksFactory.java`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/AssetFileToolCallbacksFactoryTest.java`

**Interfaces:**
- Consumes: the six existing asset tool callbacks plus `CreateAssetFileFromUrlToolCallback` from Task 1.
- Produces: `public AssetFileToolCallbacksFactory(AssetFileFacade, @Nullable ToolArtifactRecorder)`,
  `public List<ToolCallback> readToolCallbacks()`, `public List<ToolCallback> writeToolCallbacks()`.

The factory sits in `com.bytechef.automation.ai.tool` — the same package as the tools it builds.
`DataTableToolCallbacksFactory` lives in a `datatable` sub-package only because its tools already did;
relocating six asset classes here would be pure churn.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class AssetFileToolCallbacksFactoryTest {

    private final AssetFileToolCallbacksFactory factory =
        new AssetFileToolCallbacksFactory(mock(AssetFileFacade.class), null);

    @Test
    void testReadToolCallbacks() {
        assertThat(toolNames(factory.readToolCallbacks()))
            .containsExactlyInAnyOrder("listAssetFiles", "getAssetFileContent");
    }

    @Test
    void testWriteToolCallbacks() {
        assertThat(toolNames(factory.writeToolCallbacks()))
            .containsExactlyInAnyOrder(
                "listAssetFiles", "getAssetFileContent", "createAssetFile", "createBinaryAssetFile",
                "updateAssetFileContent", "cloneAssetFile", "createAssetFileFromUrl");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*AssetFileToolCallbacksFactoryTest' > /tmp/p4t2.log 2>&1; echo "exit=$?"
```

Expected: compilation failure — `AssetFileToolCallbacksFactory` does not exist.

- [ ] **Step 3: Implement the factory**

Apache header copied verbatim from a neighbouring file.

```java
package com.bytechef.automation.ai.tool;

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the asset-file tool-callback lists shared by the Copilot panel agents, the AI Hub
 * {@code asset_file_agent} subagent, and the management MCP server. Read list feeds ASK; write list feeds BUILD.
 *
 * @author Ivica Cardic
 */
public class AssetFileToolCallbacksFactory {

    private final AssetFileFacade assetFileFacade;
    private final @Nullable ToolArtifactRecorder artifactRecorder;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AssetFileToolCallbacksFactory(
        AssetFileFacade assetFileFacade, @Nullable ToolArtifactRecorder artifactRecorder) {

        this.assetFileFacade = assetFileFacade;
        this.artifactRecorder = artifactRecorder;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListAssetFilesToolCallback(assetFileFacade));
        toolCallbacks.add(new GetAssetFileContentToolCallback(assetFileFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateAssetFileToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new CreateBinaryAssetFileToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new UpdateAssetFileContentToolCallback(assetFileFacade, artifactRecorder));
        toolCallbacks.add(new CloneAssetFileToolCallback(assetFileFacade));
        toolCallbacks.add(new CreateAssetFileFromUrlToolCallback(assetFileFacade, artifactRecorder));

        return toolCallbacks;
    }
}
```

- [ ] **Step 4: Run the tests and the module check**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:automation:automation-ai:automation-ai-tool:check > /tmp/p4t2c.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p4t2c.log
```

Expected: `exit=0`.

- [ ] **Step 5: Commit**

```bash
git add server/libs/automation/automation-ai/automation-ai-tool/src
git commit -m "Add AssetFileToolCallbacksFactory for the asset-file slice"
```

---

### Task 3: Asset-file slice configuration, prompts, and enums

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-api/src/main/java/com/bytechef/ai/copilot/util/Source.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/CopilotAgentType.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/AssetFileAgentConfiguration.java`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_asset_file_ask.txt`
- Create: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_asset_file_build.txt`
- Test: `server/libs/ai/ai-copilot/ai-copilot-service/src/test/java/com/bytechef/ai/copilot/config/AssetFileAgentConfigurationTest.java`

**Interfaces:**
- Consumes: `AssetFileToolCallbacksFactory` (Task 2); `ManagerSliceSpringAIAgent.builder()`;
  `RehydrateContextToolCallback.wrap(ToolCallback, SecurityContextRehydrator)`;
  `OverrideChatClientResolver`.
- Produces: Spring beans named exactly `assetFileToolCallbacksFactory`,
  `assetFileAskSpringAIAgent`, `assetFileBuildSpringAIAgent`, `assetFileAskSubAgentChatClient`,
  `assetFileBuildSubAgentChatClient`. Agent ids `asset_file_ask` and `asset_file_build`.
  The `@Qualifier` names in Task 4 depend on these bean names verbatim.

**Design notes:**

- Reuse `ManagerSliceSpringAIAgent` — it is already generic (its `createSystemMessage` appends the
  shared additional-rules block plus the `State:`/`Context:` dump, and `toolContext` delegates to
  `CopilotToolContextUtils`). No new agent subclass is needed.
- Agent ids are derived, never literal: `Source.ASSET_FILE.name() + "_" + Mode.ASK.name()`, then
  `.toLowerCase()`. The same lowercased string is simultaneously the bean's agent id, the value
  `CopilotApiController` routes to, and the `CopilotAgentType` key.
- Only the panel agents' tool lists get `RehydrateContextToolCallback.wrap(...)`; the sub-agent
  `ChatClient` beans take the raw factory lists, exactly as `DataTableAgentConfiguration` does.

- [ ] **Step 1: Add the enum values**

In `Source.java`, append `ASSET_FILE` to the enum constant list (after `API_COLLECTION`).

In `CopilotAgentType.java`, insert before the `API_COLLECTION_ASK` group (keeping the file's
existing grouping style) — note the trailing semicolon stays on the last constant:

```java
    ASSET_FILE_ASK("asset_file_ask", false),
    ASSET_FILE_BUILD("asset_file_build", false),
    ASSET_FILE("asset_file", true),
    ASSET_FILE_AGENT("asset_file_agent", false),
```

- [ ] **Step 2: Add the Gradle dependency**

`ai-copilot-service` does not currently see `AssetFileFacade`. In
`server/libs/ai/ai-copilot/ai-copilot-service/build.gradle.kts`, add this line to the
`dependencies` block, keeping the block's existing alphabetical ordering among the
`project(...)` entries:

```kotlin
    implementation(project(":server:libs:automation:automation-asset-file:automation-asset-file-api"))
```

- [ ] **Step 3: Write the prompts**

`prompt_asset_file_ask.txt`:

```
You are the Files assistant in ByteChef, embedded in the workspace Files page.

You help the user find and read files stored in their workspace. Use `listAssetFiles` to see what
exists (id, name, mimeType, sizeBytes, createdDate) and `getAssetFileContent` to read a text file
by id. Only text files under 1 MB can be read; for anything else, describe what you know from the
listing instead of guessing at contents.

You are READ-ONLY. Never create, modify, clone, or delete a file. If the user asks you to write,
edit, or fetch a file, explain what you would do and tell them to switch to Build mode.

Be concise. Cite file names and ids. If workspace context is unavailable, say so.
```

`prompt_asset_file_build.txt`:

```
You are the Files assistant in ByteChef, embedded in the workspace Files page. You can read the
user's workspace files and create or change them.

Reading:
- `listAssetFiles` returns up to 50 files with their id, name, mimeType, sizeBytes and createdDate.
- `getAssetFileContent` reads a text file by id. Text mimes only, 1 MB limit.

Writing:
- `createAssetFile` writes a new text file you have composed — reports, specs, CSVs, JSON, markdown
  notes, or source code. Pick a filename whose extension matches the mime type.
- `updateAssetFileContent` replaces an existing text file's content. Read the current content first
  and supply the complete new content; this overwrites rather than patches.
- `createAssetFileFromUrl` downloads a public http(s) URL server-side and stores the result. Use it
  whenever the user gives you a link to a file — never try to reproduce a downloaded file's bytes
  yourself. Redirecting URLs are refused; read the error and retry with the final URL.
- `createBinaryAssetFile` stores binary content you already hold as base64. Prefer
  `createAssetFileFromUrl` when the bytes live behind a URL.
- `cloneAssetFile` copies a file into another environment of the same workspace, for example
  promoting a reviewed report to PRODUCTION.

Rules:
- Always call `listAssetFiles` before acting on an existing file. Never invent a file id.
- Before overwriting a file with `updateAssetFileContent`, state which file you are about to
  overwrite and wait for the user to confirm in their next message.
- Never claim a file was saved unless the tool returned an id.
- Report the resulting file name and id after every write.

Be concise. If workspace context is unavailable, say so.
```

Both prompts name only tools this slice registers. `askUserQuestion` is deliberately absent — it is
not registered on panel agents, and naming it would kill the turn; the confirm step is plain text
plus waiting for the next message.

- [ ] **Step 4: Write the configuration**

Create `AssetFileAgentConfiguration.java` in `com.bytechef.ai.copilot.config`, modelled on
`DataTableAgentConfiguration` in the same package. Apache header copied verbatim from it.

```java
package com.bytechef.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ManagerSliceSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.automation.ai.tool.AssetFileToolCallbacksFactory;
import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the asset-file Copilot panel source agents ({@code asset_file_ask}/{@code asset_file_build}) and the
 * asset-file subagent {@link ChatClient} beans consumed by the AI Hub root agents and the management MCP server.
 * Lives in CE because {@link AssetFileToolCallbacksFactory} and {@link AssetFileFacade} are CE; the optional
 * {@link ToolArtifactRecorder} hook is a CE SPI that the EE AI Hub recorder plugs into when present.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class AssetFileAgentConfiguration {

    @Value("classpath:prompt_asset_file_ask.txt")
    private Resource promptAssetFileAskResource;

    @Value("classpath:prompt_asset_file_build.txt")
    private Resource promptAssetFileBuildResource;

    private final State state = new State();

    @Bean
    AssetFileToolCallbacksFactory assetFileToolCallbacksFactory(
        AssetFileFacade assetFileFacade, ObjectProvider<ToolArtifactRecorder> toolArtifactRecorderProvider) {

        return new AssetFileToolCallbacksFactory(
            assetFileFacade, toolArtifactRecorderProvider.getIfAvailable());
    }

    @Bean
    ManagerSliceSpringAIAgent assetFileAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.ASSET_FILE.name() + "_" + Mode.ASK.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAssetFileAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ManagerSliceSpringAIAgent assetFileBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        String name = Source.ASSET_FILE.name() + "_" + Mode.BUILD.name();

        return ManagerSliceSpringAIAgent.builder()
            .agentId(name.toLowerCase())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptAssetFileBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, assetFileToolCallbacksFactory.writeToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    ChatClient assetFileAskSubAgentChatClient(
        ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptAssetFileAskResource))
            .defaultToolCallbacks(assetFileToolCallbacksFactory.readToolCallbacks())
            .build();
    }

    @Bean
    ChatClient assetFileBuildSubAgentChatClient(
        ChatModel chatModel, AssetFileToolCallbacksFactory assetFileToolCallbacksFactory) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptAssetFileBuildResource))
            .defaultToolCallbacks(assetFileToolCallbacksFactory.writeToolCallbacks())
            .build();
    }

    private List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> toolCallbacks) {

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback toolCallback : toolCallbacks) {
            wrapped.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrapped;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read asset file prompt resource: " + resource.getDescription(), exception);
        }
    }
}
```

- [ ] **Step 5: Write the configuration test**

Model it on `DataTableAgentConfigurationTest` if one exists in the same package; otherwise on the
phase-2 `ProjectAgentConfigurationTest`. Declare the class `final` to pre-empt SpotBugs
`CT_CONSTRUCTOR_THROW`. It must assert:

1. Every tool name that appears in `prompt_asset_file_build.txt` is registered on
   `writeToolCallbacks()` — read the prompt resource from the classpath, extract every
   backtick-quoted identifier, and assert set containment. This is the guard against the
   "No ToolCallback found" turn-killer.
2. The same check for `prompt_asset_file_ask.txt` against `readToolCallbacks()`.
3. `Source.ASSET_FILE.name().toLowerCase() + "_ask"` equals `CopilotAgentType.ASSET_FILE_ASK.key()`,
   and likewise for BUILD — pinning the derived-id contract.

- [ ] **Step 6: Run the checks**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:check :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-api:check > /tmp/p4t3.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p4t3.log
```

Expected: `exit=0`.

- [ ] **Step 7: Commit**

```bash
git add server/libs/ai/ai-copilot
git commit -m "Add the asset file copilot slice"
```

---

### Task 4: Delegate, MCP contribution, and AI Hub swap

**Files:**
- Create: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AssetFileAgentToolCallback.java`
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/ToolCallbackContributorConfiguration.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/AiHubConfiguration.java`

**Interfaces:**
- Consumes: bean `assetFileBuildSubAgentChatClient` (Task 3); `WorkspaceScopedSubAgentToolCallback`;
  `CopilotAgentType.ASSET_FILE_AGENT`.
- Produces: tool name `asset_file_agent`.

**Design notes:**

- The spec calls for "a new contributor" on the MCP surface. Adding to the existing
  `ToolCallbackContributorConfiguration` is the same contribution with less machinery: that bean is
  already *the* copilot sub-agent delegate contributor, and `asset_file_agent` is a copilot slice
  delegate. A separate contributor class would duplicate the `WorkspaceScopedSubAgentToolCallback`
  wrap for no gain.
- **The workspace wrap is mandatory on MCP.** An unwrapped delegate forwards `Map.of()` as its tool
  context, so every inner tool hits the "Workspace context unavailable" branch — the exact
  pre-existing defect phase 0 fixed for the other delegates. `asset_file_agent` gets the wrap from
  day one.
- **AI Hub keeps its read tools pinned.** `listAssetFiles` and `getAssetFileContent` stay directly on
  the root ASK and BUILD agents: the hub prompts document them, and a demoted read tool would break
  those turns. Only the *write* tools move behind the delegate, matching the spec's module-placement
  rule. `cloneAssetFile` leaves the searchable BUILD catalog for the same reason — it is now reachable
  through `asset_file_agent`.
- The generative one-shots (`ImageGeneratorConfiguration`, `SlideBuilderConfiguration`) keep their own
  `createBinaryAssetFile`. That is their artifact pipe, explicitly out of scope.

- [ ] **Step 1: Create the delegate**

Copy `DataTableAgentToolCallback.java` from the same package and adapt it: tool name
`asset_file_agent`, agent type `CopilotAgentType.ASSET_FILE_AGENT`, field and constructor parameter
named `assetFileChatClient`, input record `AssetFileAgentInput`. Keep the
`CurrentAgentContext.callWith(...)` wrapper and the verbatim `toolContext.getContext()` →
`.toolContext(forwardedContext)` forwarding unchanged — that forwarding is how workspace context
reaches the leaf tools through a delegate. Description:

```
Delegate a user request about workspace files to a specialised Files subagent.
Workspace files are the user's stored documents and assets: text files (markdown, csv, json,
code) and binaries (images, pdf, pptx). The subagent owns listing and reading files, and (in
build mode) generating new text files, editing existing ones, downloading a file from a public
URL, storing binary content, and cloning a file into another environment. Prefer calling it over
reasoning about files directly. Returns a synthesised report or a summary of the changes made.
```

- [ ] **Step 2: Contribute it to the MCP management surface**

In `ToolCallbackContributorConfiguration.copilotAgentToolCallbackContributor`, add one parameter
(keeping the existing parameter ordering convention) and one `ifAvailable` block:

```java
        @Qualifier("assetFileBuildSubAgentChatClient") ObjectProvider<ChatClient> assetFileProvider,
```

```java
            assetFileProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new AssetFileAgentToolCallback(chatClient), workspaceService)));
```

Add the `AssetFileAgentToolCallback` import in alphabetical position.

- [ ] **Step 3: Swap the AI Hub wiring**

In `AiHubConfiguration`:

1. In the method that registers sub-agent delegates (the one containing the
   `dataTableSubAgentChatClientProvider.ifAvailable(...)` block near line 970), add an
   `assetFileSubAgentChatClientProvider` parameter and an analogous block:

```java
        assetFileSubAgentChatClientProvider.ifAvailable(
            chatClient -> toolCallbacks.add(
                new ProgressReportingToolCallback(
                    new AssetFileAgentToolCallback(
                        wrapDelegate(
                            chatClient, CopilotAgentType.ASSET_FILE_AGENT.key(), aiGuardrails, aiGuardrailMetrics,
                            workspaceSystemPrompts, aiHubSessionMemory)),
                    "asset_file_agent")));
```

   Thread the new provider through every call site of that method, mirroring how
   `dataTableBuildSubAgentChatClientProvider` is threaded (declared with
   `@Qualifier("assetFileBuildSubAgentChatClient")` on the outer `@Bean` method around line 399 and
   passed down around line 505).

2. Remove the BUILD-agent direct registrations (around line 539):
   `new CreateAssetFileToolCallback(...)` and `new UpdateAssetFileContentToolCallback(...)`.
   **Keep** the two read registrations on both ASK and BUILD.

3. Remove `new CloneAssetFileToolCallback(assetFileFacade)` from the searchable BUILD catalog
   (around line 711).

4. Delete the now-unused imports (`CreateAssetFileToolCallback`, `UpdateAssetFileContentToolCallback`,
   `CloneAssetFileToolCallback`) and add `AssetFileAgentToolCallback`. Leave the
   `GetAssetFileContentToolCallback` / `ListAssetFilesToolCallback` imports in place.

5. Update the two explanatory comments that describe the old arrangement (the "cloneAssetFile is
   demoted to the searchable catalog" line and the BUILD-mode asset comment) so they describe the
   delegate instead. A stale comment here is worse than none — it tells the next reader the write
   tools are still pinned.

6. Update the AI Hub BUILD prompt: it must no longer name `createAssetFile`,
   `updateAssetFileContent`, or `cloneAssetFile` as directly callable, and should instead direct
   file-writing work to `asset_file_agent`. Locate the prompt resource
   (`prompt_ai_hub_build*.txt` under `ai-hub-service/src/main/resources`) and grep it for those
   three names. **This step is not optional** — a prompt naming an unregistered tool makes the model
   call it and the turn dies with "No ToolCallback found". If an existing test pins the prompt text,
   update it too.

- [ ] **Step 4: Verify**

```bash
./gradlew spotlessApply > /dev/null 2>&1; ./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:check :server:libs:ai:ai-copilot:ai-copilot-service:check :server:ee:libs:ai:ai-hub:ai-hub-service:check > /tmp/p4t4.log 2>&1; echo "exit=$?"; grep -E '^> Task .* FAILED' /tmp/p4t4.log
```

Expected: `exit=0`.

Then grep-verify no orphan references remain:

```bash
grep -rn "CreateAssetFileToolCallback\|UpdateAssetFileContentToolCallback\|CloneAssetFileToolCallback" server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/
```

Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add server/libs/ai/ai-copilot server/ee/libs/ai/ai-hub
git commit -m "Expose the asset file slice as asset_file_agent on MCP and AI Hub"
```

---

### Task 5: Files listing page

**Files:**
- Modify: `client/src/shared/components/copilot/stores/useCopilotStore.ts`
- Modify: `client/src/pages/automation/asset-files/AssetFiles.tsx`

**Interfaces:**
- Consumes: `CopilotButton` (`{source, parameters?, mode?}`), `useCopilotPostTurnRegistry` whose
  `register(source, callback)` returns an unregister function.
- Produces: `Source.ASSET_FILE = 'ASSET_FILE'`.

- [ ] **Step 1: Add the Source value**

In `useCopilotStore.ts`, add to the `Source` enum after `API_COLLECTION` (the enum is grouped by
surface, not alphabetised — follow the existing order):

```ts
    ASSET_FILE = 'ASSET_FILE',
```

- [ ] **Step 2: Wire the page**

In `AssetFiles.tsx`:

Add imports in alphabetical position within their groups:

```ts
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
```

Add the store hook alongside the existing store hooks (before the derived values), keeping the
established hook order:

```ts
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);
```

Add this `useEffect` with the file's other effects, immediately before the `return`:

```ts
    /*
     * Refresh the file list after a BUILD-mode copilot turn creates, downloads, or edits a file, so the
     * page reflects it without a manual reload. Prefix-only key: the listing query is keyed by
     * {environment, mimeTypePrefix, tagIds, workspaceId}, and a copilot turn can change any of those
     * dimensions, so invalidating the bare prefix is the correct breadth here.
     */
    useEffect(() => {
        return registerPostTurn(Source.ASSET_FILE, () => {
            void queryClient.invalidateQueries({queryKey: ['GetAssetFiles']});
        });
    }, [queryClient, registerPostTurn]);
```

Add the button to `toolbarRight`, immediately after `<EnvironmentSelect />` and before the
`{!showCenteredEmpty && (` block, and change the wrapper's `gap-2` to `gap-1` to match every other
listing page:

```tsx
    const toolbarRight = (
        <div className="flex items-center gap-1">
            <EnvironmentSelect />

            <CopilotButton source={Source.ASSET_FILE} />

            {!showCenteredEmpty && (
```

The button sits outside the `!showCenteredEmpty` guard deliberately: on an empty workspace, "fetch
this URL into my files" is exactly the turn a user wants, so hiding the button there would remove
the one affordance that can populate the page.

- [ ] **Step 3: Verify**

```bash
cd client && npm run check > /tmp/p4t5.log 2>&1; echo "exit=$?"; tail -20 /tmp/p4t5.log
```

Expected: `exit=0`.

- [ ] **Step 4: Commit**

```bash
git add client/src
git commit -m "client - Add Copilot to the Files listing page"
```

---

## Self-review notes

**Spec coverage.** Every phase-4 item in
`docs/superpowers/specs/2026-08-12-copilot-automation-listing-pages-design.md` maps to a task:
the `ASSET_FILE` slice (Tasks 2–3), `createAssetFileFromUrl` (Task 1), the `asset_file_agent` MCP
contribution with mandatory workspace scoping (Task 4), the module-placement rule moving write tools
out of `AiHubConfiguration` (Task 4), the prompt pair (Task 3), the client `Source` sync and page
wiring (Task 5), and row 10 of the listing-page table (Task 5). Two documented deviations: Firecrawl
is dropped (no such class; see the scope correction above), and the MCP contribution joins the
existing copilot contributor rather than a new class (Task 4 rationale).

**Deferred, not in this phase.** Generative one-shots stay AI-Hub-only per explicit instruction.
`listAssetFiles` remains absent from `ViewerToolMcpContributorConfiguration` — MCP clients reach it
through `asset_file_agent` instead, which is the tri-surface design's whole point.

**Cross-task type consistency.** Bean names in Task 3 (`assetFileBuildSubAgentChatClient`) are
consumed verbatim by the `@Qualifier` strings in Task 4. Factory method names
`readToolCallbacks()` / `writeToolCallbacks()` are fixed in Task 2 and used unchanged in Task 3.
Tool name `createAssetFileFromUrl` is fixed in Task 1 and asserted in Task 2's test.
