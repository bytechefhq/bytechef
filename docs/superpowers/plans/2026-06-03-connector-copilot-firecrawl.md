# Connector Copilot: Firecrawl Doc Fetch (scrape + crawl) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route the API-connector OpenAPI-spec generator's documentation fetch through the existing `WebScrapeService` SPI (Firecrawl when enabled → JS-rendered markdown, Jsoup fallback otherwise) and add an opt-in multi-page **crawl** (`maxPages`) to the async preview path (#2500).

**Architecture:** `ApiConnectorAiServiceImpl` stops doing inline Jsoup/HttpClient fetching and instead depends on the `WebScrapeService` bean (`scrape(url)` for one page, `crawl(url, maxPages, [])` for many). Crawl is wired ONLY into the `@Async` preview method (it can block ~2 min, which is unsafe on a web thread); the synchronous path stays scrape-only. A `maxPages` knob threads from a new wizard "Crawl linked pages" control through the GraphQL `GenerateFromDocumentationInput` into the async call.

**Tech Stack:** Java 25 / Spring, Spring AI `ChatModel`, Spring for GraphQL; React 19 + TypeScript, react-hook-form, Zustand, graphql-codegen, Vitest. EE module `platform-api-connector`.

**Spec:** `docs/superpowers/specs/2026-06-03-connector-copilot-firecrawl-design.md`

**Conventions:** EE license header + `@version ee` on new `server/ee` files (incl. tests). Client: `sort-keys` alphabetical, interfaces `…PropsI`/`…I`, sorted imports, `twMerge`, no `_`-prefixed methods. Java: one blank line before control statements.

**Key facts (verified):**
- `WebScrapeService` (in `…apiconnector.configuration.service`, `-api` module): `ScrapeResult scrape(String)`, `CrawlResult crawl(String,int,List<String>)`, `String getProviderName()`. `ScrapeResult(boolean success, String content, String error)` + static `success/failure`. `CrawlResult(boolean success, String content, List<String> crawledUrls, String error)` + static `success/failure`.
- Exactly one `WebScrapeService` bean exists: `FirecrawlWebScrapeService` (`@ConditionalOnProperty bytechef.ai.firecrawl.enabled=true`) or `JsoupWebScrapeService` (`@ConditionalOnMissingBean(name="firecrawlWebScrapeService")`). Both implement real `scrape` + multi-page `crawl`.
- `ApiConnectorAiServiceImpl` ctor today: `(ApiConnectorGenerationJobService, ChatModel)`. It is `@ConditionalOnEEVersion @ConditionalOnProperty(bytechef.ai.copilot.enabled=true)`.
- Sync `generateFromDocumentation` mutation → `ApiConnectorFacadeImpl.generateFromDocumentation(name,url)` → `apiConnectorAiService.generateOpenApiSpecification(url)` (scrape-only, unchanged).
- Async preview mutation `startGenerateFromDocumentationPreview` → `apiConnectorAiService.generateOpenApiSpecificationAsync(jobId, url, userPrompt)`.

---

## File Structure

**Backend (modify):**
- `…/service/ApiConnectorAiService.java` (`-api`) — add `int maxPages` to the async method.
- `…/service/ApiConnectorAiServiceImpl.java` — inject `WebScrapeService`; scrape/crawl fetch; thread `maxPages`; drop Jsoup/HttpClient.
- `…/web/graphql/ApiConnectorGraphQlController.java` — `GenerateFromDocumentationInput.maxPages`; pass to async.
- `…/graphql/api-connector.graphqls` — `maxPages: Int` on `GenerateFromDocumentationInput`.
- **Test (create):** `…/service/ApiConnectorAiServiceImplTest.java`.

**Frontend (modify):**
- `…/middleware/graphql-types.ts`, `graphql.ts` — regenerated.
- `…/wizard/hooks/useApiConnectorWizardStore.ts` (+ `useApiConnectorWizard.ts`) — `maxPages` field.
- `…/wizard/ApiConnectorWizardDocUrlStep.tsx` — crawl control.
- `…/pages/hooks/useApiConnectorAiPage.ts` — pass `maxPages` into the mutation input.

---

## Part A — Backend

### Task 1: Add `maxPages` to the async interface method

**Files:**
- Modify: `server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-api/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiService.java`

- [ ] **Step 1: Change the async signature + Javadoc**

```java
    /**
     * Asynchronously generates an OpenAPI specification from API documentation URL.
     *
     * @param jobId            the job ID to track progress
     * @param documentationUrl the URL to the API documentation
     * @param userPrompt       optional user instructions for endpoint selection
     * @param maxPages         when greater than 1, crawl up to this many linked pages; otherwise scrape one page
     */
    void generateOpenApiSpecificationAsync(String jobId, String documentationUrl, String userPrompt, int maxPages);
```

(Leave `generateOpenApiSpecification(String documentationUrl)` unchanged — scrape-only.)

- [ ] **Step 2: Compile (will fail until impl + controller update — that's fine, do them in Tasks 2–3)**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-api:compileJava`
Expected: BUILD SUCCESSFUL (interface-only change compiles on its own).

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-api/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiService.java
git commit -m "2500 Add maxPages to async OpenAPI generation"
```

---

### Task 2: `ApiConnectorAiServiceImpl` — fetch via `WebScrapeService` (scrape + crawl)

**Files:**
- Modify: `…/platform-api-connector-configuration-service/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImpl.java`
- Test: `…/platform-api-connector-configuration-service/src/test/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.CrawlResult;
import com.bytechef.ee.platform.apiconnector.configuration.service.WebScrapeService.ScrapeResult;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorAiServiceImplTest {

    private final ApiConnectorGenerationJobService jobService = mock(ApiConnectorGenerationJobService.class);
    private final ChatModel chatModel = mock(ChatModel.class);
    private final WebScrapeService webScrapeService = mock(WebScrapeService.class);

    private final ApiConnectorAiServiceImpl service =
        new ApiConnectorAiServiceImpl(jobService, chatModel, webScrapeService);

    private void givenLlmReturns(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    @Test
    void testGenerateScrapesAndCleansResponse() {
        when(webScrapeService.scrape("https://docs.example.com"))
            .thenReturn(ScrapeResult.success("# API docs"));
        givenLlmReturns("```yaml\nopenapi: \"3.0.0\"\n```");

        String specification = service.generateOpenApiSpecification("https://docs.example.com");

        assertThat(specification).isEqualTo("openapi: \"3.0.0\"");
    }

    @Test
    void testGenerateThrowsWhenScrapeFails() {
        when(webScrapeService.scrape(any())).thenReturn(ScrapeResult.failure("boom"));

        assertThatThrownBy(() -> service.generateOpenApiSpecification("https://docs.example.com"))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testAsyncWithMaxPagesOneScrapes() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.scrape("https://docs.example.com"))
            .thenReturn(ScrapeResult.success("# API docs"));
        givenLlmReturns("openapi: \"3.0.0\"");

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 1);

        verify(webScrapeService).scrape("https://docs.example.com");
        verify(webScrapeService, never()).crawl(any(), anyInt(), anyList());
        verify(jobService).markAsCompleted(eq("j1"), any());
    }

    @Test
    void testAsyncWithMaxPagesGreaterThanOneCrawls() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.crawl("https://docs.example.com", 5, List.of()))
            .thenReturn(CrawlResult.success("# combined", List.of("https://docs.example.com")));
        givenLlmReturns("openapi: \"3.0.0\"");

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 5);

        verify(webScrapeService).crawl("https://docs.example.com", 5, List.of());
        verify(jobService).markAsCompleted(eq("j1"), any());
    }

    @Test
    void testAsyncMarksFailedWhenFetchFails() {
        when(jobService.isCancellationRequested("j1")).thenReturn(false);
        when(webScrapeService.scrape(any())).thenReturn(ScrapeResult.failure("boom"));

        service.generateOpenApiSpecificationAsync("j1", "https://docs.example.com", null, 1);

        verify(jobService).markAsFailed(eq("j1"), any());
        verify(jobService, never()).markAsCompleted(any(), any());
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:test --tests "*ApiConnectorAiServiceImplTest"`
Expected: FAIL — the 3-arg constructor and the new async signature don't exist yet.

- [ ] **Step 3: Update the implementation**

In `ApiConnectorAiServiceImpl.java`:

(a) Remove these now-unused imports: `java.io.IOException`, `java.net.URI`, `java.net.http.HttpClient`, `java.net.http.HttpRequest`, `java.net.http.HttpResponse`, `org.jsoup.Jsoup`, `org.jsoup.nodes.Document`. Keep `java.time.Duration`? No — only used by the old HttpClient; remove it too. Add `import java.util.List;`.

(b) Replace the field + constructor:

```java
    private final ApiConnectorGenerationJobService apiConnectorGenerationJobService;
    private final ChatModel chatModel;
    private final WebScrapeService webScrapeService;

    @SuppressFBWarnings("EI")
    public ApiConnectorAiServiceImpl(
        ApiConnectorGenerationJobService apiConnectorGenerationJobService, ChatModel chatModel,
        WebScrapeService webScrapeService) {

        this.apiConnectorGenerationJobService = apiConnectorGenerationJobService;
        this.chatModel = chatModel;
        this.webScrapeService = webScrapeService;
    }
```

(c) Replace the whole `fetchDocumentation(String)` method (the Jsoup/HttpClient body) with these two methods:

```java
    private String fetchDocumentation(String documentationUrl) {
        WebScrapeService.ScrapeResult result = webScrapeService.scrape(documentationUrl);

        if (!result.success()) {
            throw new ConfigurationException(
                "Failed to fetch documentation: " + result.error(),
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return result.content();
    }

    private String fetchDocumentation(String documentationUrl, int maxPages) {
        if (maxPages <= 1) {
            return fetchDocumentation(documentationUrl);
        }

        WebScrapeService.CrawlResult result = webScrapeService.crawl(documentationUrl, maxPages, List.of());

        if (!result.success()) {
            throw new ConfigurationException(
                "Failed to crawl documentation: " + result.error(),
                ApiConnectorErrorType.INVALID_API_CONNECTOR_DEFINITION);
        }

        return result.content();
    }
```

(d) Change the async method signature to accept `maxPages` and use the crawl-aware fetch:

```java
    @Override
    @Async
    public void generateOpenApiSpecificationAsync(
        String jobId, String documentationUrl, String userInstructions, int maxPages) {

        log.debug("Starting async OpenAPI generation for job {}", jobId);

        apiConnectorGenerationJobService.markAsProcessing(jobId);

        try {
            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled before processing started", jobId);

                return;
            }

            String documentationContent = fetchDocumentation(documentationUrl, maxPages);

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after fetching documentation", jobId);

                return;
            }

            String promptMessage = String.format(
                "Analyze the following API documentation and generate an OpenAPI 3.0 specification:\n\n%s",
                documentationContent);

            if (userInstructions != null && !userInstructions.isBlank()) {
                promptMessage = promptMessage + "\n\nUser instructions:\n" + userInstructions;
            }

            Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser: " + promptMessage);

            String response = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

            if (apiConnectorGenerationJobService.isCancellationRequested(jobId)) {
                log.debug("Job {} was cancelled after AI generation", jobId);

                return;
            }

            String specification = cleanOpenApiResponse(response);

            apiConnectorGenerationJobService.markAsCompleted(jobId, specification);

            log.debug("Job {} completed successfully", jobId);
        } catch (Exception exception) {
            log.error("Job {} failed", jobId, exception);

            apiConnectorGenerationJobService.markAsFailed(jobId, exception.getMessage());
        }
    }
```

(The sync `generateOpenApiSpecification(String)` keeps calling `fetchDocumentation(documentationUrl)` — unchanged body otherwise.)

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:test --tests "*ApiConnectorAiServiceImplTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImpl.java \
        server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/test/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImplTest.java
git commit -m "2500 Fetch documentation via WebScrapeService with optional crawl"
```

---

### Task 3: GraphQL — `maxPages` on `GenerateFromDocumentationInput`

**Files:**
- Modify: `…/platform-api-connector-configuration-graphql/src/main/resources/graphql/api-connector.graphqls`
- Modify: `…/platform-api-connector-configuration-graphql/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/web/graphql/ApiConnectorGraphQlController.java`

- [ ] **Step 1: Add `maxPages` to the GraphQL input**

In `api-connector.graphqls`, the `GenerateFromDocumentationInput` block (around line 118) — add the field:

```graphql
input GenerateFromDocumentationInput {
    name: String!
    documentationUrl: String!
    icon: String
    userPrompt: String
    maxPages: Int
}
```

- [ ] **Step 2: Add `maxPages` to the Java record + pass it to the async call**

In `ApiConnectorGraphQlController.java`, update the record (around line 253):

```java
    public record GenerateFromDocumentationInput(
        String name, String documentationUrl, String icon, String userPrompt, Integer maxPages) {
    }
```

In `startGenerateFromDocumentationPreview` (around line 159-171), pass a normalized maxPages (null/≤0 → 1):

```java
        int maxPages = input.maxPages() == null ? 1 : Math.max(1, input.maxPages());

        apiConnectorAiService.generateOpenApiSpecificationAsync(
            job.getId(), input.documentationUrl(), input.userPrompt(), maxPages);
```

(The sync `generateFromDocumentation` mutation is unchanged — it still calls the facade's scrape-only path.)

- [ ] **Step 3: Compile**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-graphql:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-graphql/src/main/resources/graphql/api-connector.graphqls \
        server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-graphql/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/web/graphql/ApiConnectorGraphQlController.java
git commit -m "2500 Add maxPages to GenerateFromDocumentationInput and async preview"
```

---

## Part B — Frontend

### Task 4: Wizard crawl control + thread `maxPages`

**Files:**
- Modify (generated): `client/src/shared/middleware/graphql-types.ts`, `graphql.ts`
- Modify: `client/src/ee/pages/settings/platform/api-connectors/components/wizard/hooks/useApiConnectorWizardStore.ts`
- Modify: `client/src/ee/pages/settings/platform/api-connectors/components/wizard/hooks/useApiConnectorWizard.ts`
- Modify: `client/src/ee/pages/settings/platform/api-connectors/components/wizard/ApiConnectorWizardDocUrlStep.tsx`
- Modify: `client/src/ee/pages/settings/platform/api-connectors/pages/hooks/useApiConnectorAiPage.ts`

- [ ] **Step 1: Regenerate GraphQL types**

Run: `cd client && npx graphql-codegen`
(If `maxPages` doesn't appear on `GenerateFromDocumentationInput` in `graphql-types.ts`, re-run `node_modules/.bin/graphql-codegen`.)

Verify: `cd client && grep -n "maxPages" src/shared/middleware/graphql-types.ts`
Expected: shows `maxPages?: ... Int ...` on the input type.

- [ ] **Step 2: Add `maxPages` to the wizard store**

In `useApiConnectorWizardStore.ts`, mirror the existing `userPrompt` field: add `maxPages: number` to the state type (alphabetical), default it to `1` in the initial state, and add a `setMaxPages: (maxPages: number) => void` action that does `set({maxPages})`. (Follow the exact shape of the existing `userPrompt`/`setUserPrompt` pair in that file.)

- [ ] **Step 3: Add `maxPages` to the wizard form**

In `useApiConnectorWizard.ts`: add `maxPages: number;` to `WizardFormDataI` (alphabetical), read `maxPages` from `useApiConnectorWizardStore()`, default it in `defaultValues` as `maxPages: maxPages || 1`, and in the form-subscription effect (where it syncs `userPrompt` etc.) add:

```typescript
                if (values.maxPages !== undefined) {
                    setMaxPages(values.maxPages);
                }
```

(Add `setMaxPages` to the store destructuring at the top, matching `setUserPrompt`.)

- [ ] **Step 4: Add the crawl control to the doc-URL step**

In `ApiConnectorWizardDocUrlStep.tsx`, add imports:

```typescript
import {Checkbox} from '@/components/ui/checkbox';
import {useState} from 'react';
```

Inside the component, after `const {control, form} = useApiConnectorWizard('docUrl');`, derive the current value and a local toggle:

```typescript
    const maxPages = form.watch('maxPages');

    const [crawlEnabled, setCrawlEnabled] = useState((maxPages ?? 1) > 1);
```

Add this `FormField` block right after the `userPrompt` field's `FormField` (before the trailing `<p>` helper text):

```tsx
                <FormField
                    control={control}
                    name="maxPages"
                    render={({field}) => (
                        <FormItem>
                            <div className="flex items-center gap-2">
                                <Checkbox
                                    checked={crawlEnabled}
                                    id="crawl-enabled"
                                    onCheckedChange={(checked) => {
                                        const enabled = checked === true;

                                        setCrawlEnabled(enabled);
                                        field.onChange(enabled ? 10 : 1);
                                    }}
                                />

                                <FormLabel className="!mt-0" htmlFor="crawl-enabled">
                                    Crawl linked documentation pages
                                </FormLabel>
                            </div>

                            {crawlEnabled && (
                                <FormControl>
                                    <Input
                                        max={50}
                                        min={2}
                                        onChange={(event) => field.onChange(Number(event.target.value) || 2)}
                                        type="number"
                                        value={field.value ?? 10}
                                    />
                                </FormControl>
                            )}

                            <FormMessage />
                        </FormItem>
                    )}
                />
```

- [ ] **Step 5: Pass `maxPages` into the mutation input**

In `useApiConnectorAiPage.ts`, read `maxPages` from the store (next to `documentationUrl`/`userPrompt` — around line 36/52) and add it to the `startGenerationMutation.mutate({input: {...}})` object (around line 301):

```typescript
                    maxPages,
```

(Insert in alphabetical position among the input keys: `documentationUrl`, `maxPages`, `name`, `userPrompt`. Ensure `maxPages` is destructured from `useApiConnectorWizardStore()` alongside the others.)

- [ ] **Step 6: Type-check + lint**

Run: `cd client && npm run typecheck && npx eslint src/ee/pages/settings/platform/api-connectors/components/wizard/ApiConnectorWizardDocUrlStep.tsx src/ee/pages/settings/platform/api-connectors/components/wizard/hooks/useApiConnectorWizard.ts src/ee/pages/settings/platform/api-connectors/components/wizard/hooks/useApiConnectorWizardStore.ts src/ee/pages/settings/platform/api-connectors/pages/hooks/useApiConnectorAiPage.ts`
Expected: clean.

- [ ] **Step 7: Commit**

```bash
git add client/src/shared/middleware/graphql-types.ts client/src/shared/middleware/graphql.ts \
        client/src/ee/pages/settings/platform/api-connectors/
git commit -m "2500 client - Add crawl (maxPages) control to connector doc-URL wizard step"
```

---

## Part C — Verification

### Task 5: Full server + client checks

- [ ] **Step 1: Server format + checks**

```bash
./gradlew spotlessApply
```
Revert any reformatting in files NOT part of this change (stage only your own). Then:

```bash
./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-api:check \
          :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:test \
          :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-graphql:check
```
Expected: api + graphql `check` SUCCESSFUL; service `test` SUCCESSFUL. (If `:…-service:check` surfaces an unrelated pre-existing static-analysis failure, run `:…-service:test` for that module and note it.)

- [ ] **Step 2: Client full check**

```bash
cd client && npm run check
```
Expected: lint + typecheck + tests pass.

- [ ] **Step 3: Commit any spotless reformatting of your files**

```bash
git add -A
git commit -m "2500 Apply spotless formatting" || echo "nothing to format"
```

---

## Self-Review

**Spec coverage:**
- Route fetch through `WebScrapeService` (scrape) → Task 2. ✓
- Multi-page crawl on async path only; sync stays scrape-only → Task 2 (`fetchDocumentation(url,maxPages)` used only by async; sync uses `fetchDocumentation(url)`). ✓
- `maxPages` threaded interface → impl → GraphQL → client → Tasks 1,2,3,4. ✓
- Wizard crawl control (off → maxPages 1 → scrape) → Task 4. ✓
- Graceful degradation (Firecrawl vs Jsoup) → automatic via the single `WebScrapeService` bean (no code branch needed; both impls support scrape+crawl). ✓
- Error handling: scrape/crawl failure → `ConfigurationException` (sync) / `markAsFailed` (async) → Task 2 + its tests. ✓
- EE conventions / verification → header on the new test; Task 5. ✓

**Type consistency:** `generateOpenApiSpecificationAsync(String,String,String,int)` matches interface (Task 1), impl (Task 2), controller call (Task 3). `GenerateFromDocumentationInput.maxPages` is `Integer` (Java record, nullable) ↔ `Int` (GraphQL) ↔ `maxPages?: number` (client) — controller normalizes null→1. `WebScrapeService.scrape/crawl` + `ScrapeResult.success()/content()/error()` and `CrawlResult.success()/content()/error()` match the verified record shapes. Client `maxPages` store field default `1`.

**Placeholder scan:** every code step has full code; the store/form field additions (Task 4 Steps 2–3) say "mirror the existing `userPrompt` field" and give the exact additions (state field, default, setter, subscription block) because that file's surrounding boilerplate must match its own style — no logic is left unspecified.
