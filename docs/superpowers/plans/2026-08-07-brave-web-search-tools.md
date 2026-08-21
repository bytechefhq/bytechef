# Brave Web Search Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `BraveWebSearchTools`, a second independently-toggleable server-side web search provider, and register it alongside `FirecrawlTools` on every copilot and AI Hub agent surface.

**Architecture:** A hand-rolled `@Component` in CE `platform-ai-tool` driving a `RestClient` against the Brave Search API, gated on `bytechef.ai.brave.enabled`. It exposes exactly one `@Tool` method, `braveWebSearch`. Fourteen bean methods that already inject `Optional<FirecrawlTools>` gain a parallel `Optional<BraveWebSearchTools>`. AI Hub's `ResearchConfiguration` relaxes from "requires Firecrawl" to "requires either provider" via `AnyNestedCondition`.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI (`@Tool` / `@ToolParam` / `ToolCallbacks`), Jackson 2 annotations, JUnit 5 + AssertJ + `MockRestServiceServer`, Gradle.

**Spec:** `docs/superpowers/specs/2026-08-07-brave-web-search-tools-design.md`

## Global Constraints

- **Copyright headers.** Files under `server/libs/` use the Apache 2.0 header. Files under `server/ee/` use the ByteChef Enterprise header **and** an `@version ee` Javadoc tag. Spotless picks the EE header by the `@version ee` *content*, not the path — copy the header verbatim from a neighbouring file in the same directory.
- **Author tag.** New classes get `@author Ivica Cardic` in the class Javadoc.
- **The tool method MUST be named `braveWebSearch`, not `webSearch`.** `ToolCallbacks.from(object)` derives tool names from method names, and `FirecrawlTools.webSearch` lands on the same `ChatClient`. A duplicate name is a build-time failure.
- **Variable naming.** No short or cryptic names, including lambda parameters and loop variables. `result`, not `r`.
- **Blank line before control statements** (`if`, `for`, `try`, …) except immediately after an opening `{`, and **blank line after a variable modification** that a following statement uses. See `CLAUDE.md`.
- **No trailing blank line** between the last member and a class's closing `}`.
- **Checkstyle:** test method names are camelCase with no underscores (`testBraveWebSearchClampsCount`, never `testBraveWebSearch_ClampsCount`). Empty blocks are forbidden — a comment alone does not satisfy the rule.
- **`ApplicationProperties` binding is strict.** Every new `bytechef.*` property must exist as a field with a getter and setter, or the app fails to start.
- `spring-boot-starter-test` is already on every subproject's test classpath from the root `build.gradle.kts`. **No `build.gradle.kts` changes are needed anywhere in this plan.**
- Run `./gradlew spotlessApply` before every commit.

## File Structure

| File | Responsibility |
| --- | --- |
| `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` | Add `Ai.Brave` nested class + `brave` field/getter/setter on `Ai`. |
| `server/apps/server-app/src/main/resources/config/application-bytechef.yml` | Ship the `bytechef.ai.brave` defaults, disabled. |
| `.../platform-ai-tool/src/main/java/com/bytechef/platform/ai/tool/exception/BraveToolErrorType.java` | **Create.** Error type for the single tool. |
| `.../platform-ai-tool/src/main/java/com/bytechef/platform/ai/tool/BraveWebSearchTools.java` | **Create.** The `@Tool` bean and its result records. |
| `.../platform-ai-tool/src/test/java/com/bytechef/platform/ai/tool/BraveWebSearchToolsTest.java` | **Create.** Unit tests over `MockRestServiceServer`. |
| `.../ai-copilot-service/.../config/CopilotConfiguration.java` | Wire 12 bean methods; convert 5 branching ones to the list shape. |
| `.../embedded-ai-copilot/.../config/EmbeddedCopilotConfiguration.java` | Wire 2 bean methods. |
| `.../ai-hub-service/.../config/ResearchConfiguration.java` | `AnyNestedCondition`, both providers optional, `brave_search` metering. |
| `.../ai-hub-service/src/test/.../config/ResearchConfigurationTest.java` | Cover Firecrawl-only, Brave-only, both. |
| 4 `prompt_*.txt` resources | Name the new tool; drop provider-specific wording. |

---

### Task 1: `BraveWebSearchTools`

Everything needed for the tool to exist and be callable: the properties it binds, the error type it throws, the class itself, and its tests.

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`
- Create: `server/libs/platform/platform-ai/platform-ai-tool/src/main/java/com/bytechef/platform/ai/tool/exception/BraveToolErrorType.java`
- Create: `server/libs/platform/platform-ai/platform-ai-tool/src/main/java/com/bytechef/platform/ai/tool/BraveWebSearchTools.java`
- Test: `server/libs/platform/platform-ai/platform-ai-tool/src/test/java/com/bytechef/platform/ai/tool/BraveWebSearchToolsTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces:
  - `com.bytechef.platform.ai.tool.BraveWebSearchTools` — `@Component`, constructor `(ApplicationProperties, RestClient.Builder)`.
  - `BraveWebSearchTools#braveWebSearch(String query, Integer count, String country, String freshness)` returning `BraveWebSearchTools.BraveSearchResult`.
  - `BraveWebSearchTools.BraveSearchResult(String query, List<BraveSearchResultItem> results)` (public record).
  - `BraveWebSearchTools.BraveSearchResultItem(String title, String url, String description, String age)` (public record).
  - `com.bytechef.platform.ai.tool.exception.BraveToolErrorType.WEB_SEARCH`.
  - `ApplicationProperties.Ai#getBrave()` returning `ApplicationProperties.Ai.Brave` with `getApiKey()`, `getBaseUrl()`, `isEnabled()` and matching setters.

- [ ] **Step 1: Add the `Brave` properties class**

In `ApplicationProperties.java`, insert this class immediately **before** `public static class Firecrawl {` (currently line 1212). The nested classes in `Ai` are not alphabetical — placing `Brave` next to the sibling it mirrors is the intent.

```java
        /**
         * Brave Search API configuration.
         */
        public static class Brave {

            /**
             * Brave Search API key
             */
            private String apiKey;

            /**
             * Brave Search API base URL
             */
            private String baseUrl = "https://api.search.brave.com/res/v1";

            /**
             * Whether Brave Search is enabled
             */
            private boolean enabled;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

```

- [ ] **Step 2: Add the `brave` field, getter, and setter on `Ai`**

The fields, getters, and setters of `Ai` **are** alphabetical. Insert `brave` between `autoMemory` and `copilot` in all three places.

Field list (currently line 690) becomes:

```java
        private AutoMemory autoMemory = new AutoMemory();
        private Brave brave = new Brave();
        private Copilot copilot = new Copilot();
```

Getter — insert between `getAutoMemory()` and `getCopilot()`:

```java
        public Brave getBrave() {
            return brave;
        }

```

Setter — insert between `setAutoMemory(...)` and `setCopilot(...)`:

```java
        public void setBrave(Brave brave) {
            this.brave = brave;
        }

```

- [ ] **Step 3: Add the yaml defaults**

In `server/apps/server-app/src/main/resources/config/application-bytechef.yml`, insert `brave` **before** `copilot` (that file's keys are alphabetical):

```yaml
bytechef:
  ai:
    brave:
      base-url: https://api.search.brave.com/res/v1
      enabled: false
    copilot:
      enabled: false
```

Only the `brave:` block is new — `copilot:` is shown for placement.

- [ ] **Step 4: Create the error type**

Create `BraveToolErrorType.java` with the Apache 2.0 header copied verbatim from the sibling `FirecrawlToolErrorType.java`, then:

```java
package com.bytechef.platform.ai.tool.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class BraveToolErrorType extends AbstractErrorType {

    public static final BraveToolErrorType WEB_SEARCH = new BraveToolErrorType(100);

    private BraveToolErrorType(int errorKey) {
        super(BraveToolErrorType.class, errorKey);
    }
}
```

Error key `100` does not collide with `FirecrawlToolErrorType.WEB_SEARCH` — `AbstractErrorType` keys by class as well as by int.

- [ ] **Step 5: Write the failing tests**

Create `BraveWebSearchToolsTest.java` with the Apache 2.0 header copied from `ComponentToolsTest.java` in the same test tree.

`MockRestServiceServer.bindTo(RestClient.Builder)` installs a mock request factory **into the builder**, so bind first and hand the same builder to the constructor. The constructor's `.baseUrl(...)` and `.defaultHeader(...)` calls mutate that same builder and are still honoured.

```java
package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
class BraveWebSearchToolsTest {

    private static final String SEARCH_URL = "https://api.search.brave.com/res/v1/web/search";

    private static final String RESPONSE_BODY = """
        {
          "type": "search",
          "web": {
            "type": "search",
            "results": [
              {
                "title": "ByteChef",
                "url": "https://bytechef.io",
                "description": "Open-source automation platform.",
                "age": "3 days ago",
                "family_friendly": true
              }
            ]
          }
        }
        """;

    @Test
    void testBraveWebSearchMapsResults() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Subscription-Token", "test-key"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.query()).isEqualTo("bytechef");
        assertThat(result.results()).hasSize(1);

        BraveWebSearchTools.BraveSearchResultItem item = result.results()
            .getFirst();

        assertThat(item.title()).isEqualTo("ByteChef");
        assertThat(item.url()).isEqualTo("https://bytechef.io");
        assertThat(item.description()).isEqualTo("Open-source automation platform.");
        assertThat(item.age()).isEqualTo("3 days ago");

        server.verify();
    }

    @Test
    void testBraveWebSearchClampsCountToBraveMaximum() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&count=20"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", 50, null, null);

        server.verify();
    }

    @Test
    void testBraveWebSearchClampsCountToBraveMinimum() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&count=1"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", 0, null, null);

        server.verify();
    }

    @Test
    void testBraveWebSearchPassesOptionalParameters() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef&country=DE&freshness=pw"))
            .andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        braveWebSearchTools.braveWebSearch("bytechef", null, "DE", "pw");

        server.verify();
    }

    @Test
    void testBraveWebSearchReturnsEmptyResultsWhenWebSectionAbsent() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withSuccess("{\\"type\\":\\"search\\"}", MediaType.APPLICATION_JSON));

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.results()).isEmpty();
    }

    @Test
    void testBraveWebSearchReturnsEmptyResultsWhenBodyIsEmpty() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withSuccess());

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        BraveWebSearchTools.BraveSearchResult result = braveWebSearchTools.braveWebSearch(
            "bytechef", null, null, null);

        assertThat(result.query()).isEqualTo("bytechef");
        assertThat(result.results()).isEmpty();
    }

    @Test
    void testBraveWebSearchWrapsHttpFailure() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder)
            .build();

        server.expect(requestTo(SEARCH_URL + "?q=bytechef"))
            .andRespond(withServerError());

        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), restClientBuilder);

        assertThatThrownBy(() -> braveWebSearchTools.braveWebSearch("bytechef", null, null, null))
            .isInstanceOf(ExecutionException.class);
    }

    @Test
    void testToolCallbackIsNamedBraveWebSearch() {
        BraveWebSearchTools braveWebSearchTools = new BraveWebSearchTools(
            applicationProperties(), RestClient.builder());

        ToolCallback[] toolCallbacks = ToolCallbacks.from(braveWebSearchTools);

        assertThat(toolCallbacks).hasSize(1);
        assertThat(
            toolCallbacks[0].getToolDefinition()
                .name()).isEqualTo("braveWebSearch");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai.Brave brave = applicationProperties.getAi()
            .getBrave();

        brave.setApiKey("test-key");

        return applicationProperties;
    }
}
```

`testToolCallbackIsNamedBraveWebSearch` is the regression test for the naming constraint in Global Constraints — it is the only thing standing between a rename and a runtime duplicate-tool-name failure.

- [ ] **Step 6: Run the tests to verify they fail**

```bash
./gradlew :server:libs:platform:platform-ai:platform-ai-tool:test --tests '*BraveWebSearchToolsTest' > /tmp/brave-test.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/brave-test.log
```

Expected: compilation failure — `BraveWebSearchTools` does not exist.

- [ ] **Step 7: Implement `BraveWebSearchTools`**

Create the file with the Apache 2.0 header copied verbatim from `FirecrawlTools.java`, then:

```java
package com.bytechef.platform.ai.tool;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.exception.BraveToolErrorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Spring AI tool for Brave Search integration.
 *
 * <p>
 * The tool method is deliberately named {@code braveWebSearch} rather than {@code webSearch}: Spring AI derives tool
 * names from method names, and this bean is registered on the same agents as {@link FirecrawlTools}, whose search
 * method is already called {@code webSearch}.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(name = "bytechef.ai.brave.enabled")
public class BraveWebSearchTools {

    private static final int MAX_COUNT = 20;
    private static final int MIN_COUNT = 1;

    private static final Logger log = LoggerFactory.getLogger(BraveWebSearchTools.class);

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

    @Tool(
        description = "Search the web with the Brave Search API. Returns ranked results with title, URL, description and age. Use this for fast, broad web search; it returns snippets only and does not fetch full page content.")
    public BraveSearchResult braveWebSearch(
        @ToolParam(description = "The search query") String query,
        @ToolParam(required = false, description = "Number of results to return (1-20, default 20)") Integer count,
        @ToolParam(
            required = false,
            description = "ISO country code for geo-targeting (e.g., 'US', 'DE', 'JP')") String country,
        @ToolParam(
            required = false,
            description = "Freshness filter: 'pd' past day, 'pw' past week, 'pm' past month, 'py' past year, or a 'YYYY-MM-DDtoYYYY-MM-DD' range") String freshness) {

        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    "braveWebSearch({}, {}, {}, {}): Performing Brave search for query: {}", query, count, country,
                    freshness, query);
            }

            BraveSearchResponse response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/web/search")
                        .queryParam("q", query);

                    if (count != null) {
                        uriBuilder.queryParam("count", Math.min(Math.max(count, MIN_COUNT), MAX_COUNT));
                    }

                    if (country != null) {
                        uriBuilder.queryParam("country", country);
                    }

                    if (freshness != null) {
                        uriBuilder.queryParam("freshness", freshness);
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .body(BraveSearchResponse.class);

            if (response == null || response.web() == null || response.web()
                .results() == null) {

                return new BraveSearchResult(query, List.of());
            }

            List<BraveSearchResultItem> results = response.web()
                .results()
                .stream()
                .map(webResult -> new BraveSearchResultItem(
                    webResult.title() != null ? webResult.title() : "",
                    webResult.url() != null ? webResult.url() : "",
                    webResult.description() != null ? webResult.description() : "",
                    webResult.age() != null ? webResult.age() : ""))
                .toList();

            if (log.isDebugEnabled()) {
                log.debug(
                    "braveWebSearch({}, {}, {}, {}): Found {} search results for query: {}", query, count, country,
                    freshness, results.size(), query);
            }

            return new BraveSearchResult(query, results);
        } catch (Exception e) {
            log.error(
                "braveWebSearch({}, {}, {}, {}): Failed to perform Brave search for query: {}", query, count, country,
                freshness, query, e);

            throw new ExecutionException(
                "Failed to perform Brave search: " + e.getMessage(), e, BraveToolErrorType.WEB_SEARCH);
        }
    }

    @SuppressFBWarnings("EI")
    public record BraveSearchResult(
        @JsonProperty("query") @JsonPropertyDescription("The search query that was performed") String query,
        @JsonProperty("results") @JsonPropertyDescription("List of search results with title, URL, description and age") List<BraveSearchResultItem> results) {
    }

    @SuppressFBWarnings("EI")
    public record BraveSearchResultItem(
        @JsonProperty("title") @JsonPropertyDescription("Title of the search result") String title,
        @JsonProperty("url") @JsonPropertyDescription("URL of the search result") String url,
        @JsonProperty("description") @JsonPropertyDescription("Description of the search result") String description,
        @JsonProperty("age") @JsonPropertyDescription("Human readable age of the page, e.g. '3 days ago'") String age) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BraveSearchResponse(WebResults web) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResults(List<WebResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WebResult(String title, String url, String description, String age) {
    }
}
```

- [ ] **Step 8: Run the tests to verify they pass**

```bash
./gradlew spotlessApply
./gradlew :server:libs:platform:platform-ai:platform-ai-tool:test --tests '*BraveWebSearchToolsTest' > /tmp/brave-test.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/brave-test.log
```

Expected: `exit=0`, no `FAILED` task lines, all eight tests pass.

Never judge this from a piped `tail`/`grep` — the pipeline's exit code is the filter's. Redirect to a file, check `$?` on its own line, then grep the file.

If `testBraveWebSearchPassesOptionalParameters` fails on the query string, the cause is parameter **order**, not correctness: `requestTo` matches the URL literally. The implementation appends `q`, then `count`, then `country`, then `freshness`; make the expectation match that order rather than reordering the implementation.

- [ ] **Step 9: Commit**

```bash
git add server/libs/config/app-config server/apps/server-app/src/main/resources/config/application-bytechef.yml server/libs/platform/platform-ai/platform-ai-tool
git commit -m "732 Add BraveWebSearchTools web search tool"
```

---

### Task 2: Wire Brave into `CopilotConfiguration` (CE)

Twelve bean methods inject `Optional<FirecrawlTools>`. Seven already build a mutable list; five branch on `isPresent()` and are converted to the list shape first, so a second provider does not turn them into four branches.

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java`

**Interfaces:**
- Consumes: `com.bytechef.platform.ai.tool.BraveWebSearchTools` (Task 1).
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Add the import**

Alongside the existing `import com.bytechef.platform.ai.tool.FirecrawlTools;` (line 65), add:

```java
import com.bytechef.platform.ai.tool.BraveWebSearchTools;
```

Place it in correct alphabetical position — `BraveWebSearchTools` sorts before `ComponentTools`, `FirecrawlTools`, etc. within the `com.bytechef.platform.ai.tool` group. Spotless will not reorder imports for you; get it right or `./gradlew check` fails on Checkstyle.

- [ ] **Step 2: Update the seven list-shape bean methods**

These methods already look like:

```java
        List<Object> tools = new ArrayList<>(List.of(/* ... */));

        firecrawlTools.ifPresent(tools::add);
```

For each of `codeEditorAskSpringAIAgent`, `workflowCodeEditorAskSpringAIAgent`, `workflowCodeEditorBuildSpringAIAgent`, `clusterElementAskSpringAIAgent`, `workflowEditorAskSpringAIAgent`, `skillsAskSpringAIAgent`, and `workflowExecutionAskSpringAIAgent`:

1. Add `Optional<BraveWebSearchTools> braveWebSearchTools` to the parameter list, immediately after the existing `Optional<FirecrawlTools> firecrawlTools` parameter.
2. Add one line directly beneath the existing `ifPresent` call:

```java
        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);
```

- [ ] **Step 3: Convert the five branching bean methods**

`codeEditorAskSubAgentChatClient`, `workflowEditorAskSubAgentChatClient`, `clusterElementAskSubAgentChatClient`, `skillsAskSubAgentChatClient`, and `workflowExecutionAskSubAgentChatClient` currently read:

```java
        if (firecrawlTools.isPresent()) {
            builder.defaultTools(
                readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools,
                firecrawlTools.get());
        } else {
            builder.defaultTools(
                readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools);
        }
```

Add `Optional<BraveWebSearchTools> braveWebSearchTools` after the `Optional<FirecrawlTools>` parameter, then replace the whole `if`/`else` with — using `codeEditorAskSubAgentChatClient`'s own tool list as the example:

```java
        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);

        builder.defaultTools(tools.toArray());
```

`ChatClient.Builder.defaultTools(Object...)` accepts the array directly.

**Each method's tool list differs** — use the right one. The list is exactly that method's current `else` branch (the one *without* `firecrawlTools.get()`):

| Method | `List.of(...)` contents |
| --- | --- |
| `codeEditorAskSubAgentChatClient` | `readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools` |
| `workflowEditorAskSubAgentChatClient` | `readProjectTools, readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools, workflowInstructionTools` |
| `clusterElementAskSubAgentChatClient` | `readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools, workflowInstructionTools` |
| `skillsAskSubAgentChatClient` | `readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools, workflowInstructionTools` |
| `workflowExecutionAskSubAgentChatClient` | `workflowExecutionTools, readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools` |

The blank line before `builder.defaultTools(...)` is required by the blank-line-after-variable-modification rule.

`ArrayList` and `List` are already imported in this file; `Optional` is too.

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew spotlessApply
./gradlew :server:libs:ai:ai-copilot:ai-copilot-service:compileJava > /tmp/copilot-compile.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/copilot-compile.log
```

Expected: `exit=0`, no FAILED tasks.

- [ ] **Step 5: Confirm every site was updated**

```bash
grep -c "Optional<FirecrawlTools> firecrawlTools" server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
grep -c "Optional<BraveWebSearchTools> braveWebSearchTools" server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
grep -c "firecrawlTools.isPresent()" server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
```

Expected: `12`, `12`, and `0`.

- [ ] **Step 6: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/java/com/bytechef/ai/copilot/config/CopilotConfiguration.java
git commit -m "732 Register Brave web search tool on the copilot agents"
```

---

### Task 3: Wire Brave into `EmbeddedCopilotConfiguration` (EE)

**Files:**
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotConfiguration.java`

**Interfaces:**
- Consumes: `com.bytechef.platform.ai.tool.BraveWebSearchTools` (Task 1).
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Add the import and update both bean methods**

Add `import com.bytechef.platform.ai.tool.BraveWebSearchTools;` in alphabetical position next to the existing `FirecrawlTools` import (line 28).

Both `workflowEditorEmbeddedAskSpringAIAgent` (parameter at line 119) and `workflowExecutionEmbeddedAskSpringAIAgent` (parameter at line 234) already use the list shape. For each: add `Optional<BraveWebSearchTools> braveWebSearchTools` immediately after the `Optional<FirecrawlTools> firecrawlTools` parameter, and add one line beneath the existing `ifPresent`:

```java
        firecrawlTools.ifPresent(tools::add);
        braveWebSearchTools.ifPresent(tools::add);
```

This file is under `server/ee/` and already carries the Enterprise header and `@version ee` tag — do not touch either.

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:embedded:embedded-ai:embedded-ai-copilot:compileJava > /tmp/embedded-compile.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/embedded-compile.log
```

Expected: `exit=0`, no FAILED tasks.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/java/com/bytechef/ee/embedded/ai/copilot/config/EmbeddedCopilotConfiguration.java
git commit -m "732 Register Brave web search tool on the embedded copilot agents"
```

---

### Task 4: `ResearchConfiguration` boots on either provider (EE)

Today the AI Hub research subagent does not exist without Firecrawl. Make it exist when **either** provider is present, and meter Brave calls as `brave_search`.

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/ResearchConfiguration.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/config/ResearchConfigurationTest.java`

**Interfaces:**
- Consumes: `com.bytechef.platform.ai.tool.BraveWebSearchTools` (Task 1).
- Produces: `ResearchConfiguration#researchChatClient(ChatModel, Optional<FirecrawlTools>, Optional<BraveWebSearchTools>, ObjectProvider<ToolUsageRecorder>, ObjectProvider<AiHubTaskService>, JsonMapper, Resource)`.

- [ ] **Step 1: Write the failing tests**

Replace `testResearchChatClientIsBuilt` in `ResearchConfigurationTest.java` with three cases, and add the two imports `com.bytechef.platform.ai.tool.BraveWebSearchTools` and `java.util.Optional`. Leave the two `ResearchToolCallback` tests untouched.

```java
    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuiltWithFirecrawlOnly() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.of(mock(FirecrawlTools.class)), Optional.empty(),
                mock(ObjectProvider.class), mock(ObjectProvider.class), new JsonMapper(), promptResource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuiltWithBraveOnly() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.empty(), Optional.of(mock(BraveWebSearchTools.class)),
                mock(ObjectProvider.class), mock(ObjectProvider.class), new JsonMapper(), promptResource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuiltWithBothProviders() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.of(mock(FirecrawlTools.class)),
                Optional.of(mock(BraveWebSearchTools.class)), mock(ObjectProvider.class), mock(ObjectProvider.class),
                new JsonMapper(), promptResource()));
    }

    private static Resource promptResource() {
        return new ByteArrayResource(
            "You are a research assistant.".getBytes(StandardCharsets.UTF_8), "test prompt_research.txt");
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*ResearchConfigurationTest' > /tmp/research-test.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED|error:" /tmp/research-test.log
```

Expected: compilation failure — `researchChatClient` still takes a bare `FirecrawlTools`.

- [ ] **Step 3: Replace the class-level conditional**

`@ConditionalOnBean` **ANDs** its values; there is no `anyOf` attribute. OR semantics require `AnyNestedCondition`.

Replace `@ConditionalOnBean(FirecrawlTools.class)` on the class with:

```java
@Conditional(ResearchConfiguration.OnAnyWebSearchToolsCondition.class)
```

and add this as the last member of the class:

```java
    /**
     * Matches when at least one web search provider bean is present. {@code @ConditionalOnBean} ANDs its values, so
     * "either provider" needs a nested-condition composite.
     */
    static class OnAnyWebSearchToolsCondition extends AnyNestedCondition {

        OnAnyWebSearchToolsCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(FirecrawlTools.class)
        static class OnFirecrawlTools {

            private OnFirecrawlTools() {
            }
        }

        @ConditionalOnBean(BraveWebSearchTools.class)
        static class OnBraveWebSearchTools {

            private OnBraveWebSearchTools() {
            }
        }
    }
```

`ConfigurationPhase.REGISTER_BEAN` is mandatory — bean-presence cannot be evaluated during configuration-class parsing. The private no-arg constructors exist so the marker classes are not empty blocks, which Checkstyle forbids.

New imports: `com.bytechef.platform.ai.tool.BraveWebSearchTools`, `java.util.Optional`, `org.springframework.boot.autoconfigure.condition.AnyNestedCondition`, `org.springframework.context.annotation.Conditional`. `ConditionalOnBean` is already imported.

- [ ] **Step 4: Take both providers as `Optional` and union their callbacks**

Change the `researchChatClient` signature's second parameter and add a third:

```java
        Optional<FirecrawlTools> firecrawlTools,
        Optional<BraveWebSearchTools> braveWebSearchTools,
```

Replace the single `ToolCallbacks.from(firecrawlTools)` line with a union built before the metering loop:

```java
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        firecrawlTools.ifPresent(tools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(tools))));
        braveWebSearchTools.ifPresent(tools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(tools))));
```

Then change the loop header to iterate the union and size the output list from it:

```java
        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback callback : toolCallbacks) {
```

The loop body is unchanged apart from Step 5.

- [ ] **Step 5: Add `brave_search` to the metering map**

Rename `mapFirecrawlToolName` to `mapMeteredToolName`, rename its parameter, add the Brave case, and update the single call site in the loop (`String meteredToolName = mapMeteredToolName(name);`):

```java
    /**
     * Maps a web search tool's {@code @Tool} method name to the canonical {@code tool_name} stored in
     * {@code ai_hub_tool_usage}. Returns {@code null} for cheap or non-instrumented tools.
     */
    private static String mapMeteredToolName(String toolName) {
        return switch (toolName) {
            case "webSearch" -> "firecrawl_search";
            case "webpageScrape" -> "firecrawl_scrape";
            case "braveWebSearch" -> "brave_search";
            default -> null;
        };
    }
```

The argument-field extractor in the loop needs **no** change: it already falls through to `"query"` for anything that is not `websiteMap` or `webpageScrape`, and Brave's parameter is `query`.

- [ ] **Step 6: Update the class Javadoc**

The class Javadoc currently says the subagent is "pre-loaded with Firecrawl tools" and that it is "guarded by `@ConditionalOnBean(FirecrawlTools.class)`". Replace both sentences:

```java
 * The research subagent is a dedicated {@link ChatClient} pre-loaded with whichever web search tools the deployment
 * has configured and the {@code prompt_research.txt} system prompt. Its isolated context means the parent ai_hub
 * agent never sees the browsing transcript — it only receives the final synthesised markdown report.
```

and

```java
 * The configuration is guarded by {@link OnAnyWebSearchToolsCondition} so it registers when either
 * {@link FirecrawlTools} or {@link BraveWebSearchTools} is present, and deployments with neither continue to boot
 * normally without the research tool. A Brave-only deployment can search but not fetch page bodies, so its reports
 * are synthesised from search snippets.
```

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*ResearchConfigurationTest' > /tmp/research-test.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/research-test.log
```

Expected: `exit=0`, no FAILED tasks, five tests pass.

- [ ] **Step 8: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service
git commit -m "732 Boot the AI Hub research subagent on either web search provider"
```

---

### Task 5: Prompts and full verification

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_research.txt` (line 5)
- Modify: `server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources/prompt_workflow_editor_ask.txt` (line 25)
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt` (line 77)
- Modify: `server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/resources/prompt_workflow_editor_embedded_ask.txt` (line 29)

**Interfaces:**
- Consumes: the tool name `braveWebSearch` (Task 1).
- Produces: nothing.

- [ ] **Step 1: Make `prompt_research.txt` provider-neutral**

Line 5 currently reads:

> research. You have Firecrawl tools for searching the public web and for fetching specific

Replace that sentence with wording that names neither vendor and states the snippet-only limitation:

> research. You have web search tools for searching the public web, and — when the deployment provides
> one — a tool for fetching specific pages. If no page-fetching tool is available to you, say so in your
> report and synthesise from search result snippets rather than claiming to have read the pages.

Keep the surrounding lines and the file's existing line wrapping.

- [ ] **Step 2: Make `prompt_ai_hub_build.txt` provider-neutral**

Line 77 currently reads:

> that uses Firecrawl to search and fetch public web pages. Returns a structured

Replace `Firecrawl` with `web search tools`:

> that uses web search tools to search and fetch public web pages. Returns a structured

- [ ] **Step 3: Name the Brave tool in both workflow-editor prompts**

In `prompt_workflow_editor_ask.txt` line 25 and `prompt_workflow_editor_embedded_ask.txt` line 29, both of which read:

> - For information that you can't find in the context, use tools like webSearch, websiteMap and webpageScrape to access the web. For example, when there are no instructions in context on how to create a connection.

change the tool list to:

> - For information that you can't find in the context, use tools like webSearch, braveWebSearch, websiteMap and webpageScrape to access the web. Not all of them are available in every deployment — use whichever you have. For example, when there are no instructions in context on how to create a connection.

- [ ] **Step 4: Full verification**

```bash
./gradlew spotlessApply
./gradlew compileJava compileTestJava --continue > /tmp/brave-compile-all.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/brave-compile-all.log
./gradlew :server:libs:platform:platform-ai:platform-ai-tool:test :server:ee:libs:ai:ai-hub:ai-hub-service:test --continue > /tmp/brave-test-all.log 2>&1; echo "exit=$?"; grep -E "^> Task .* FAILED" /tmp/brave-test-all.log
```

Expected: `exit=0` for both, no FAILED task lines.

Do **not** grep for `error:` in these logs — it matches module paths such as `:server:libs:core:error:`. Grep `^> Task .* FAILED` instead.

- [ ] **Step 5: Confirm no stale Firecrawl-only wording survives**

```bash
grep -rn "Firecrawl" --include="*.txt" server/libs/ai server/ee/libs/ai server/ee/libs/embedded | grep -v "/build/"
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add server/libs/ai/ai-copilot/ai-copilot-service/src/main/resources server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources server/ee/libs/embedded/embedded-ai/embedded-ai-copilot/src/main/resources
git commit -m "732 Name the Brave web search tool in the agent prompts"
```

---

## Manual smoke test (optional, needs a Brave API key)

Not part of any task's gate — the unit tests cover the contract. Run it if you have a key and want end-to-end confidence:

1. Add to `server/apps/server-app/src/main/resources/config/application-bytechef.yml` (or an env var):
   `bytechef.ai.brave.enabled: true` and `bytechef.ai.brave.api-key: <key>`.
2. `cd server && docker compose -f docker-compose.dev.infra.yml up -d && cd ..`
3. `./gradlew -p server/apps/server-app bootRun`
4. Open the workflow editor copilot and ask a question that needs current web information. Confirm the
   agent calls `braveWebSearch` and that both it and `webSearch` are offered when Firecrawl is also enabled.
