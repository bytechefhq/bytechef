# HTML Artifact Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an `HTML` artifact format to the command center so the agent can author self-contained interactive HTML+JS artifacts (countdown clocks, small games, custom widgets) that render in a sandboxed iframe with an in-app maximize toggle.

**Architecture:** Three layers, each touching exactly one extension point in the existing artifact pipeline. Persistence: append `HTML` to the `AssetFileFormat` enum. Generation: new `HtmlArtifactGenerator` registered with the existing `ArtifactGeneratorRegistry`, performs strict structural+external-refs validation and server-side CSP injection. Rendering: new `AiHubHtmlInteractivePane.tsx` dispatched from `AiHubFileViewer.tsx` when `format === 'HTML'`, iframe with `sandbox="allow-scripts"` and a maximize toggle.

**Tech Stack:** Java 25, Spring Boot 4, jsoup (HTML parser — already used elsewhere in the codebase via `libs.org.jsoup`), JUnit 5, Mockito, AssertJ. React 19, TypeScript 5.9, Vitest 4, Testing Library.

**Spec:** [docs/superpowers/specs/2026-05-08-html-artifact-format-design.md](../specs/2026-05-08-html-artifact-format-design.md)

---

## Task 1: Append `HTML` to `AssetFileFormat` enum

**Files:**
- Modify: `server/libs/automation/automation-asset-file/automation-asset-file-api/src/main/java/com/bytechef/automation/assetfile/domain/AssetFileFormat.java`
- Modify: `server/libs/automation/automation-asset-file/automation-asset-file-service/src/test/java/com/bytechef/automation/assetfile/domain/AssetFileFormatOrdinalStabilityTest.java`

The `format` column on `asset_file` stores the enum ordinal — append-only is mandatory, the stability test pins each ordinal. We add `HTML` at position 8.

- [ ] **Step 1: Update the ordinal stability test to expect HTML at position 8 (failing test)**

Edit `AssetFileFormatOrdinalStabilityTest.java`. Add the new entry to the `expected` map after `IMAGE`:

```java
expected.put("MARKDOWN", 0);
expected.put("CODE", 1);
expected.put("CSV", 2);
expected.put("JSON", 3);
expected.put("DOCX", 4);
expected.put("PPTX", 5);
expected.put("CHART", 6);
expected.put("IMAGE", 7);
expected.put("HTML", 8);
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:libs:automation:automation-asset-file:automation-asset-file-service:test --tests AssetFileFormatOrdinalStabilityTest
```

Expected: FAIL — the enum has only 8 values (0..7), the test now expects 9. Error mentions a missing `HTML` value.

- [ ] **Step 3: Append `HTML` to the enum**

Edit `AssetFileFormat.java`. The new value goes last:

```java
public enum AssetFileFormat {
    MARKDOWN,
    CODE,
    CSV,
    JSON,
    DOCX,
    PPTX,
    CHART,
    IMAGE,
    HTML;

    public static AssetFileFormat fromOrdinal(short ordinal) {
        AssetFileFormat[] values = values();

        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid AssetFileFormat ordinal: " + ordinal);
        }

        return values[ordinal];
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:libs:automation:automation-asset-file:automation-asset-file-service:test --tests AssetFileFormatOrdinalStabilityTest
```

Expected: PASS.

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-asset-file/automation-asset-file-api/src/main/java/com/bytechef/automation/assetfile/domain/AssetFileFormat.java \
        server/libs/automation/automation-asset-file/automation-asset-file-service/src/test/java/com/bytechef/automation/assetfile/domain/AssetFileFormatOrdinalStabilityTest.java
git commit -m "$(cat <<'EOF'
HTML Artifact - Append HTML to AssetFileFormat enum

Adds HTML at ordinal 8 (append-only per the enum contract). Pins the
ordinal in AssetFileFormatOrdinalStabilityTest so a future reorder
fails fast.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: Add jsoup dependency to automation-ai-hub-service

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts`

The HTML validator parses with jsoup. The dependency is declared in the version catalog as `libs.org.jsoup` (already used by `text-helper` and `platform-api-connector-configuration-service`). One line in `build.gradle.kts`.

- [ ] **Step 1: Add jsoup to the dependencies block**

Edit `build.gradle.kts`. Insert at the top of the `implementation` declarations, alphabetically before `methanol`:

```kotlin
dependencies {
    implementation(libs.com.github.mizosoft.methanol)
    implementation(libs.org.apache.poi.poi.ooxml)
    implementation(libs.org.jsoup)
    implementation(libs.org.springaicommunity.spring.ai.agent.utils)
    // ... rest unchanged
```

- [ ] **Step 2: Verify the module still compiles**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/build.gradle.kts
git commit -m "$(cat <<'EOF'
HTML Artifact - Add jsoup dependency to ai-hub-service

Needed by the upcoming HtmlArtifactGenerator validator. jsoup is already
on the catalog (libs.org.jsoup) and used by text-helper.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: HtmlArtifactGenerator skeleton + happy path

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGenerator.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGeneratorTest.java`

Skeleton extends `AbstractTextArtifactGenerator`. No validation rules yet — those land in Task 4. CSP injection happens here so the happy-path test can verify a valid input gets the meta tag inserted before persistence.

- [ ] **Step 1: Write the happy-path failing test**

Create `HtmlArtifactGeneratorTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.automation.aihub.task.AiHubTaskAssetFileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class HtmlArtifactGeneratorTest {

    private static final String VALID_HTML = """
        <!doctype html>
        <html>
          <head><title>Hi</title></head>
          <body><h1>Hello</h1><script>console.log('ok')</script></body>
        </html>
        """;

    @Test
    void testFormatIsHtml() {
        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubTaskAssetFileService.class));

        assertThat(generator.format()).isEqualTo(AssetFileFormat.HTML);
    }

    @Test
    void testGenerateHappyPathPersistsAsTextHtmlWithCspInjected() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("widget.html");

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("widget.html"), eq("text/html"), contentCaptor.capture(),
            eq(AssetFileFormat.HTML), any(), any(), any()))
                .thenReturn(saved);

        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "make a widget", "widget.html", VALID_HTML, null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.HTML);
        assertThat(result.assetFileId()).isEqualTo(42L);
        assertThat(result.taskLinked()).isTrue();

        String persisted = contentCaptor.getValue();

        assertThat(persisted)
            .contains("Content-Security-Policy")
            .contains("connect-src 'none'")
            .contains("script-src 'unsafe-inline'");
    }

    @Test
    void testGenerateAppendsHtmlExtensionWhenMissing() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("widget.html");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("widget.html"), eq("text/html"), any(),
            eq(AssetFileFormat.HTML), any(), any(), any()))
                .thenReturn(saved);

        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(facade, linkService);

        generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "widget", VALID_HTML, null));

        // Captured filename via the eq("widget.html") matcher above proves the extension was appended.
    }

    @Test
    void testGenerateRejectsNonHtmlContent() {
        // Sanity guard: a request whose content isn't recognisable HTML must surface a validation error
        // rather than landing as a malformed asset_file row. Detailed structural rules are pinned in
        // Task 4's tests.
        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubTaskAssetFileService.class));

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", "not html", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail (compile error)**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests HtmlArtifactGeneratorTest
```

Expected: FAIL — `HtmlArtifactGenerator` does not exist.

- [ ] **Step 3: Create HtmlArtifactGenerator with skeleton + minimal happy-path validation**

Create `HtmlArtifactGenerator.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.automation.aihub.task.AiHubTaskAssetFileService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Persists a self-contained interactive HTML artifact.
 *
 * <p>
 * Two responsibilities beyond the base {@link AbstractTextArtifactGenerator}:
 * </p>
 * <ol>
 * <li><strong>Strict structural validation</strong> — full HTML doc required, no external script/stylesheet/media
 * sources, no nested browsing contexts, no external form actions, no {@code <base>} tag, 1 MB cap. The validator
 * collects every violation in one pass so the LLM can fix the whole batch on the next turn.</li>
 * <li><strong>Server-side CSP injection</strong> — a {@code Content-Security-Policy} meta tag is inserted as the
 * first child of {@code <head>} before persistence. The LLM cannot defeat this; if the artifact already has its own
 * CSP meta, ours sits in front and browsers honour the intersection of stacked policies.</li>
 * </ol>
 *
 * <p>
 * Combined with the viewer's iframe sandbox ({@code sandbox="allow-scripts"} — no same-origin), the artifact runs
 * interactive JS but cannot reach ByteChef session state, navigate the parent, or phone home.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.ai-hub", name = "enabled", havingValue = "true")
public class HtmlArtifactGenerator extends AbstractTextArtifactGenerator {

    private static final String CSP_META =
        "<meta http-equiv=\"Content-Security-Policy\" content=\""
            + "default-src 'none'; "
            + "script-src 'unsafe-inline'; "
            + "style-src 'unsafe-inline' data:; "
            + "img-src data:; "
            + "font-src data:; "
            + "media-src data:; "
            + "connect-src 'none'; "
            + "base-uri 'none'; "
            + "frame-ancestors 'none';"
            + "\">";

    private static final int MAX_CONTENT_BYTES = 1_000_000;

    public HtmlArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        super(assetFileFacade, taskAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.HTML;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        return "text/html";
    }

    @Override
    protected String defaultExtension() {
        return "html";
    }

    @Override
    protected void validate(GenerationRequest request) {
        Document parsed = parseStrict(request.payload());

        // Sentinel rule for the skeleton — a payload that doesn't parse to a recognisable HTML document fails.
        // Detailed structural rules are layered in by HtmlArtifactValidator in the next task.
        if (parsed.head() == null || parsed.body() == null) {
            throw new IllegalArgumentException("HTML artifact must include <head> and <body> elements.");
        }
    }

    /**
     * Override of the base persistence flow so we can inject the CSP meta tag <em>after</em> validation but
     * <em>before</em> the size cap and persistence. Bypassing the base class' final {@code generate} is intentional;
     * the CSP injection is integral to the format and must run on every successful path.
     */
    @Override
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public final GenerationResult generate(GenerationRequest request) {
        validate(request);

        Document doc = parseStrict(request.payload());

        injectCsp(doc);

        String injected = doc.outerHtml();

        if (injected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException(
                "HTML artifact exceeds the 1 MB size cap after CSP injection.");
        }

        return super.generate(new GenerationRequest(
            request.workspaceId(),
            request.userId(),
            request.environmentId(),
            request.taskId(),
            request.generatedByAgentSource(),
            request.generatedFromPrompt(),
            request.filename(),
            injected,
            request.metadataJson()));
    }

    private static Document parseStrict(String html) {
        // Parser.htmlParser() is the default tolerant parser. We feed empty baseUri because the artifact must be
        // self-contained — relative URLs resolved against a base would defeat the no-external-refs rule.
        return Jsoup.parse(html, "", Parser.htmlParser());
    }

    private static void injectCsp(Document doc) {
        Element head = doc.head();

        // Place CSP as head's first child so any author-supplied CSP meta lands AFTER ours; browsers
        // intersect stacked CSPs, so the LLM cannot weaken our policy with its own.
        head.prepend(CSP_META);
    }
}
```

Note: the `super.generate(...)` call after `validate` would re-run `validate`. Update the base class flow handling — instead of overriding `generate`, override `resolveMetadataJson` and `validate` only, and inject CSP via a *content-rewrite* hook on the base class.

After looking at the base class flow more carefully, the cleaner path is to NOT override `generate`. Instead:

- Replace the override of `generate` above with a protected hook on `AbstractTextArtifactGenerator` that subclasses can use to rewrite the payload between `validate` and `assetFileFacade.createFromAi`. Default implementation returns the payload unchanged.

To keep this task focused on TDD, **use the override approach above for now** (works correctly because `super.generate` will call `validate` again on the *injected* content, and a CSP-injected document still passes the skeleton's "head and body present" check). Task 4 will refine this when the strict validation rules are added.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests HtmlArtifactGeneratorTest
```

Expected: PASS for all four tests.

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGenerator.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGeneratorTest.java
git commit -m "$(cat <<'EOF'
HTML Artifact - Add HtmlArtifactGenerator skeleton with CSP injection

Skeleton validates that the payload parses as HTML with head+body and
injects the Content-Security-Policy meta tag as the first child of head
before persistence. Strict structural rules (no external refs, no nested
browsing contexts, etc.) layer in next.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: Strict structural validation rules

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactValidator.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGenerator.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGeneratorTest.java`

Extract the validation into a dedicated class so each rule has a clear home and the generator stays small. The validator collects all violations in one pass and throws a single `IllegalArgumentException` listing them — the LLM sees the whole batch and can fix everything in one revision.

- [ ] **Step 1: Write failing tests for each rule**

Append these tests to `HtmlArtifactGeneratorTest.java`:

```java
@Test
void testGenerateRejectsFragmentMissingDoctype() {
    HtmlArtifactGenerator generator = newGenerator();

    String fragment = "<html><head></head><body>x</body></html>";

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", fragment, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("doctype");
}

@Test
void testGenerateRejectsExternalScriptSrc() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html><head></head>
        <body><script src="https://evil.example/x.js"></script></body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("external script");
}

@Test
void testGenerateAllowsDataUriScriptSrc() {
    AssetFileFacade facade = mock(AssetFileFacade.class);
    AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

    AssetFile saved = mock(AssetFile.class);
    when(saved.getId()).thenReturn(42L);
    when(saved.getName()).thenReturn("x.html");
    when(facade.createFromAi(
        anyLong(), any(Integer.class), eq("x.html"), eq("text/html"), any(),
        eq(AssetFileFormat.HTML), any(), any(), any()))
            .thenReturn(saved);

    HtmlArtifactGenerator generator = new HtmlArtifactGenerator(facade, linkService);

    String html = """
        <!doctype html><html><head></head>
        <body><script src="data:application/javascript;base64,Y29uc29sZS5sb2coJ2hpJyk="></script></body></html>
        """;

    GenerationResult result = generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null));

    assertThat(result.format()).isEqualTo(AssetFileFormat.HTML);
}

@Test
void testGenerateRejectsExternalStylesheetLink() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html>
        <head><link rel="stylesheet" href="https://cdn.example/s.css"></head>
        <body>x</body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("external stylesheet");
}

@Test
void testGenerateRejectsExternalImgSrc() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html><head></head>
        <body><img src="https://tracker.example/p.gif"></body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("external image");
}

@Test
void testGenerateRejectsNestedIframe() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html><head></head>
        <body><iframe src="about:blank"></iframe></body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nested browsing context");
}

@Test
void testGenerateRejectsAbsoluteFormAction() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html><head></head>
        <body><form action="https://exfil.example/post"></form></body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("external form action");
}

@Test
void testGenerateRejectsBaseTag() {
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html>
        <head><base href="https://attacker.example/"></head>
        <body>x</body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("<base>");
}

@Test
void testGenerateRejectsContentOverOneMegabyte() {
    HtmlArtifactGenerator generator = newGenerator();

    StringBuilder big = new StringBuilder("<!doctype html><html><head></head><body>");

    while (big.length() < 1_100_000) {
        big.append("aaaaaaaaaa");
    }

    big.append("</body></html>");

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", big.toString(), null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1 MB");
}

@Test
void testGenerateReportsAllViolationsInOneError() {
    // Multi-violation discipline: a message listing every violation lets the LLM fix the whole batch on
    // the next turn instead of round-tripping per rule.
    HtmlArtifactGenerator generator = newGenerator();

    String html = """
        <!doctype html><html>
        <head>
          <base href="https://attacker.example/">
          <link rel="stylesheet" href="https://cdn.example/s.css">
        </head>
        <body>
          <iframe src="about:blank"></iframe>
          <img src="https://tracker.example/p.gif">
        </body></html>
        """;

    assertThatThrownBy(() -> generator.generate(
        new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .satisfies(thrown -> {
                String message = thrown.getMessage();

                assertThat(message)
                    .contains("<base>")
                    .contains("external stylesheet")
                    .contains("nested browsing context")
                    .contains("external image");
            });
}

private HtmlArtifactGenerator newGenerator() {
    return new HtmlArtifactGenerator(mock(AssetFileFacade.class), mock(AiHubTaskAssetFileService.class));
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests HtmlArtifactGeneratorTest
```

Expected: FAIL on the new tests (skeleton validator only checks head/body presence).

- [ ] **Step 3: Create HtmlArtifactValidator**

Create `HtmlArtifactValidator.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.aihub.artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Strict validator for HTML artifacts. Walks the parsed document tree once, accumulating every violation, then throws a
 * single {@link IllegalArgumentException} listing all of them. Single-pass collection lets the LLM fix the whole batch
 * on the next turn instead of round-tripping per rule.
 *
 * <p>
 * Rules pin the structural and "no external refs" invariants the spec requires. The CSP meta tag and the iframe sandbox
 * provide runtime backstops, so a future relaxation here would not silently expose users — but a relaxation should be
 * deliberate and live alongside the rule it weakens.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class HtmlArtifactValidator {

    private HtmlArtifactValidator() {
    }

    static void validate(Document document) {
        List<String> violations = new ArrayList<>();

        checkDoctype(document, violations);
        checkRequiredElements(document, violations);
        checkScripts(document, violations);
        checkLinkRels(document, violations);
        checkMediaSources(document, violations);
        checkNestedBrowsingContexts(document, violations);
        checkFormActions(document, violations);
        checkBaseTag(document, violations);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "HTML artifact failed validation:\n  - " + String.join("\n  - ", violations));
        }
    }

    private static void checkDoctype(Document document, List<String> violations) {
        boolean hasHtmlDoctype = false;

        for (Node node : document.childNodes()) {
            if (node instanceof DocumentType doctype && "html".equalsIgnoreCase(doctype.name())) {
                hasHtmlDoctype = true;

                break;
            }
        }

        if (!hasHtmlDoctype) {
            violations.add("missing <!doctype html> declaration");
        }
    }

    private static void checkRequiredElements(Document document, List<String> violations) {
        if (document.selectFirst("html") == null) {
            violations.add("missing <html> element");
        }

        if (document.head() == null) {
            violations.add("missing <head> element");
        }

        if (document.body() == null) {
            violations.add("missing <body> element");
        }
    }

    private static void checkScripts(Document document, List<String> violations) {
        for (Element script : document.select("script[src]")) {
            String src = script.attr("src");

            if (!isAllowedAssetUri(src)) {
                violations.add("external script src not allowed: '" + truncate(src) + "' (use inline <script> or data: URI)");
            }
        }
    }

    private static void checkLinkRels(Document document, List<String> violations) {
        for (Element link : document.select("link")) {
            String rel = link.attr("rel").toLowerCase(Locale.ROOT);
            String href = link.attr("href");

            if ("stylesheet".equals(rel)) {
                if (!isAllowedAssetUri(href)) {
                    violations.add("external stylesheet not allowed: '" + truncate(href) + "' (use inline <style> or data: URI)");
                }
            } else if (!rel.isBlank()) {
                violations.add("<link rel=\"" + rel + "\"> not allowed (only stylesheet links permitted)");
            }
        }
    }

    private static void checkMediaSources(Document document, List<String> violations) {
        String[] tags = {"img", "source", "video", "audio", "track"};

        for (String tag : tags) {
            for (Element element : document.select(tag)) {
                checkMediaAttribute(element, "src", "external " + label(tag) + " src", violations);
                checkMediaAttribute(element, "srcset", "external " + label(tag) + " srcset", violations);
                checkMediaAttribute(element, "poster", "external " + label(tag) + " poster", violations);
            }
        }
    }

    private static void checkMediaAttribute(Element element, String attribute, String label, List<String> violations) {
        if (!element.hasAttr(attribute)) {
            return;
        }

        String value = element.attr(attribute);

        if (!isAllowedAssetUri(value)) {
            violations.add(label + " not allowed: '" + truncate(value) + "' (use data: URI)");
        }
    }

    private static void checkNestedBrowsingContexts(Document document, List<String> violations) {
        Elements nested = document.select("iframe, frame, frameset, embed, object, applet");

        if (!nested.isEmpty()) {
            violations.add("nested browsing context tags not allowed: " + nested.size() + " element(s) found (iframe/frame/embed/object/applet)");
        }
    }

    private static void checkFormActions(Document document, List<String> violations) {
        for (Element form : document.select("form[action]")) {
            String action = form.attr("action");

            if (isAbsoluteUrl(action)) {
                violations.add("external form action not allowed: '" + truncate(action) + "'");
            }
        }
    }

    private static void checkBaseTag(Document document, List<String> violations) {
        if (!document.select("base").isEmpty()) {
            violations.add("<base> tag not allowed (would re-anchor relative URLs)");
        }
    }

    private static boolean isAllowedAssetUri(String value) {
        if (value == null) {
            return true;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return true;
        }

        return trimmed.toLowerCase(Locale.ROOT).startsWith("data:");
    }

    private static boolean isAbsoluteUrl(String value) {
        if (value == null) {
            return false;
        }

        String trimmed = value.trim().toLowerCase(Locale.ROOT);

        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
            || trimmed.startsWith("//");
    }

    private static String label(String tag) {
        return switch (tag) {
            case "img" -> "image";
            case "source" -> "media source";
            case "video" -> "video";
            case "audio" -> "audio";
            case "track" -> "track";
            default -> tag;
        };
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }

        return value.length() > 80 ? value.substring(0, 77) + "..." : value;
    }
}
```

- [ ] **Step 4: Wire the validator into HtmlArtifactGenerator**

Replace the body of `validate(GenerationRequest)` and the override of `generate` in `HtmlArtifactGenerator.java` so the flow is:

1. Parse strictly.
2. Run `HtmlArtifactValidator.validate(doc)` — collects every violation, throws once.
3. Inject CSP.
4. Re-serialize, check size cap.
5. Delegate to `super.generate` with the rewritten payload (the base flow's `validate` re-runs but is fine on the post-injection content because injection only adds a meta tag — none of the rules trigger on it).

```java
@Override
protected void validate(GenerationRequest request) {
    Document parsed = parseStrict(request.payload());

    HtmlArtifactValidator.validate(parsed);
}

@Override
public final GenerationResult generate(GenerationRequest request) {
    validate(request);

    Document doc = parseStrict(request.payload());

    injectCsp(doc);

    String injected = doc.outerHtml();

    if (injected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
        throw new IllegalArgumentException(
            "HTML artifact exceeds the 1 MB size cap after CSP injection.");
    }

    return super.generate(new GenerationRequest(
        request.workspaceId(),
        request.userId(),
        request.environmentId(),
        request.taskId(),
        request.generatedByAgentSource(),
        request.generatedFromPrompt(),
        request.filename(),
        injected,
        request.metadataJson()));
}
```

- [ ] **Step 5: Run the tests to verify all pass**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests HtmlArtifactGeneratorTest
```

Expected: all 14 tests PASS.

- [ ] **Step 6: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactValidator.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGenerator.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/artifact/HtmlArtifactGeneratorTest.java
git commit -m "$(cat <<'EOF'
HTML Artifact - Add strict structural validation

HtmlArtifactValidator walks the parsed doc once, collects every
violation, and throws a single IllegalArgumentException listing all of
them so the LLM can fix the whole batch on the next turn. Rules: full
doc structure, no external script/stylesheet/media srcs, no nested
browsing contexts, no external form actions, no <base> tag, 1 MB cap.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: Wire HTML into `GenerateArtifactToolCallback`

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/GenerateArtifactToolCallback.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/GenerateArtifactToolCallbackTest.java`

The schema's format enum needs `HTML` added so the LLM can pick it. The description gains the steering line so the LLM doesn't reach for HTML when CHART/MARKDOWN would do.

- [ ] **Step 1: Write failing test for HTML format dispatch**

Append to `GenerateArtifactToolCallbackTest.java`:

```java
@Test
void testCallDispatchesHtmlFormatToRegistry() throws Exception {
    ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
    AiHubTaskService taskService = mock(AiHubTaskService.class);

    when(registry.generate(eq(AssetFileFormat.HTML), any(GenerationRequest.class)))
        .thenReturn(new GenerationResult(99L, "widget.html", AssetFileFormat.HTML, false));

    GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

    String html = "<!doctype html><html><head></head><body><h1>x</h1></body></html>";

    String result = callback.call(
        "{\"format\":\"HTML\",\"filename\":\"widget.html\",\"content\":" + jsonMapper.writeValueAsString(html) + "}",
        toolContext());

    JsonNode node = jsonMapper.readTree(result);

    assertThat(node.get("format").asText()).isEqualTo("HTML");
    verify(registry).generate(eq(AssetFileFormat.HTML), any(GenerationRequest.class));
}

@Test
void testToolDescriptionAdvertisesHtmlInSchemaEnum() {
    ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
    AiHubTaskService taskService = mock(AiHubTaskService.class);

    GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

    String schema = callback.getToolDefinition().inputSchema();

    assertThat(schema).contains("\"HTML\"");
}

@Test
void testToolDescriptionContainsHtmlSteeringLine() {
    // The steering line is the only nudge keeping the LLM from reaching for HTML when CHART/MARKDOWN
    // would do. Pin it so a refactor of the description doesn't silently drop the guidance.
    ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
    AiHubTaskService taskService = mock(AiHubTaskService.class);

    GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

    String description = callback.getToolDefinition().description();

    assertThat(description)
        .contains("CHART for data visualization")
        .contains("HTML only for interactive apps");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests GenerateArtifactToolCallbackTest
```

Expected: FAIL on the three new tests — the schema enum lacks `HTML` and the description lacks the steering line.

- [ ] **Step 3: Update the tool description, schema, and dispatch**

Edit `GenerateArtifactToolCallback.java`:

Update `DESCRIPTION`:

```java
private static final String DESCRIPTION = """
    Author a file artifact (markdown report, code file, csv/json data file, etc.) and persist it
    in the user's workspace. The file appears as a clickable chip in chat and in the Generated tab
    of the Files panel; the user can open it in the file viewer or download it. Use this when the
    user asks for a deliverable they will reference later — not for ephemeral preview text.

    format is one of MARKDOWN, CODE, CSV, JSON, DOCX, PPTX, CHART, IMAGE, HTML. content is the literal
    artifact body for text formats; for binary formats (DOCX/PPTX/CHART/IMAGE) the format-specific
    generator interprets it (typically a JSON spec). metadataJson is optional structured metadata
    the viewer reads back (e.g. chart spec) — leave null when not applicable.

    Use CHART for data visualization. Use HTML only for interactive apps that cannot be expressed as
    a chart, table, or markdown report (e.g. a countdown clock, a small game, a custom interactive
    widget). HTML artifacts must be self-contained: no external script/stylesheet/image references —
    inline everything or use data: URIs.""";
```

Update `INPUT_SCHEMA` — add `"HTML"` to the format enum array:

```java
private static final String INPUT_SCHEMA =
    """
        {
            "type": "object",
            "properties": {
                "format": {
                    "type": "string",
                    "enum": ["MARKDOWN", "CODE", "CSV", "JSON", "DOCX", "PPTX", "CHART", "IMAGE", "HTML"],
                    "description": "Logical format of the artifact"
                },
                "filename": {
                    "type": "string",
                    "description": "Display filename. Extension may be auto-appended for some formats."
                },
                "content": {
                    "type": "string",
                    "description": "Artifact body. Literal text for MARKDOWN/CODE/CSV/JSON/HTML; format-specific JSON spec for the rest."
                },
                "metadataJson": {
                    "type": "string",
                    "description": "Optional structured metadata. Leave null when not applicable."
                }
            },
            "required": ["format", "filename", "content"]
        }""";
```

No other changes are needed to the dispatch logic — it already routes by `AssetFileFormat.valueOf(input.format())` which will resolve `"HTML"` once the enum has it.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:test --tests GenerateArtifactToolCallbackTest
```

Expected: all tests PASS (existing six + three new ones).

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/main/java/com/bytechef/ee/automation/aihub/tool/GenerateArtifactToolCallback.java \
        server/ee/libs/automation/automation-ai-hub/automation-ai-hub-service/src/test/java/com/bytechef/ee/automation/aihub/tool/GenerateArtifactToolCallbackTest.java
git commit -m "$(cat <<'EOF'
HTML Artifact - Wire HTML format into generateArtifact tool

Adds HTML to the schema enum and an LLM steering line to the tool
description: use CHART for data viz, HTML only for interactive apps
that can't be expressed as a chart/table/markdown report. Dispatch
already routes by enum-resolved format so no callback-side changes.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: `AiHubHtmlInteractivePane` component + tests

**Files:**
- Create: `client/src/pages/automation/ai-hub/AiHubHtmlInteractivePane.tsx`
- Create: `client/src/pages/automation/ai-hub/tests/AiHubHtmlInteractivePane.test.tsx`

The interactive pane wraps the artifact in a sandboxed iframe with `sandbox="allow-scripts"` (no same-origin) and exposes a maximize toggle. Maximize state is component-local; Esc collapses.

- [ ] **Step 1: Write the failing tests**

Create `client/src/pages/automation/ai-hub/tests/AiHubHtmlInteractivePane.test.tsx`:

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import AiHubHtmlInteractivePane from '../AiHubHtmlInteractivePane';

const HTML = '<!doctype html><html><head></head><body><h1>Hi</h1></body></html>';

describe('AiHubHtmlInteractivePane', () => {
    it('renders an iframe with sandbox="allow-scripts" and srcDoc', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        const iframe = screen.getByTitle('widget.html') as HTMLIFrameElement;

        expect(iframe).toBeInTheDocument();
        expect(iframe).toHaveAttribute('sandbox', 'allow-scripts');
        expect(iframe.getAttribute('srcdoc') || iframe.getAttribute('srcDoc')).toBe(HTML);
    });

    it('maximizes via the toolbar button and applies the fixed-overlay class', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        const button = screen.getByRole('button', {name: /maximize/i});

        fireEvent.click(button);

        const wrapper = screen.getByTestId('html-pane-wrapper');

        expect(wrapper.className).toMatch(/fixed/);
        expect(wrapper.className).toMatch(/inset-0/);
        expect(wrapper.className).toMatch(/z-50/);
    });

    it('collapses via Escape after maximizing', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        fireEvent.click(screen.getByRole('button', {name: /maximize/i}));

        const wrapper = screen.getByTestId('html-pane-wrapper');

        expect(wrapper.className).toMatch(/fixed/);

        fireEvent.keyDown(document, {key: 'Escape'});

        expect(wrapper.className).not.toMatch(/fixed/);
    });

    it('shows minimize button after maximize', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        fireEvent.click(screen.getByRole('button', {name: /maximize/i}));

        expect(screen.getByRole('button', {name: /minimize/i})).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubHtmlInteractivePane.test.tsx
```

Expected: FAIL — module does not exist.

- [ ] **Step 3: Create the component**

Create `client/src/pages/automation/ai-hub/AiHubHtmlInteractivePane.tsx`:

```tsx
import {Button} from '@/components/ui/button';
import {MaximizeIcon, MinimizeIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface AiHubHtmlInteractivePanePropsI {
    content: string;
    name: string;
}

const AiHubHtmlInteractivePane = ({content, name}: AiHubHtmlInteractivePanePropsI) => {
    const [maximized, setMaximized] = useState(false);

    useEffect(() => {
        if (!maximized) {
            return;
        }

        const handler = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                setMaximized(false);
            }
        };

        document.addEventListener('keydown', handler);

        return () => document.removeEventListener('keydown', handler);
    }, [maximized]);

    return (
        <div
            className={twMerge(
                'flex size-full flex-col bg-surface-main',
                maximized && 'fixed inset-0 z-50'
            )}
            data-testid="html-pane-wrapper"
        >
            <div className="flex items-center justify-end border-b border-stroke-neutral-secondary px-2 py-1">
                <Button
                    aria-label={maximized ? 'Minimize' : 'Maximize'}
                    onClick={() => setMaximized((current) => !current)}
                    size="sm"
                    variant="ghost"
                >
                    {maximized ? <MinimizeIcon className="size-4" /> : <MaximizeIcon className="size-4" />}
                </Button>
            </div>

            <iframe
                className="size-full flex-1 border-0"
                sandbox="allow-scripts"
                srcDoc={content}
                title={name}
            />
        </div>
    );
};

export default AiHubHtmlInteractivePane;
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubHtmlInteractivePane.test.tsx
```

Expected: all four tests PASS.

- [ ] **Step 5: Run lint + typecheck**

```bash
cd client && npm run lint && npm run typecheck
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubHtmlInteractivePane.tsx \
        client/src/pages/automation/ai-hub/tests/AiHubHtmlInteractivePane.test.tsx
git commit -m "$(cat <<'EOF'
client - HTML Artifact - Add AiHubHtmlInteractivePane

Iframe with sandbox="allow-scripts" (no same-origin) plus an in-app
maximize toggle wrapped around it. Maximize state is component-local;
Escape collapses. Maximize is orthogonal to the editor/preview/split
viewModes elsewhere — applied on top of whichever pane the viewer
chose.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Wire dispatch in `AiHubFileViewer`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/AiHubFileViewer.tsx`
- Modify: `client/src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx`

Format-discriminated dispatch goes ahead of the mime-only path so user-uploaded HTML files (mime `text/html`, no `format`) keep using the existing `sandbox=""` branch while AI-generated HTML artifacts (`format = HTML`) get the trusted-generation pane with scripts allowed.

- [ ] **Step 1: Write the failing dispatch test**

Append to `AiHubFileViewer.test.tsx`:

```tsx
describe('AiHubFileViewer with HTML format', () => {
    it('routes to the html interactive pane when format=HTML', async () => {
        // Pin the format-discriminated dispatch: a file whose `format` is HTML must render via the
        // interactive pane (sandbox="allow-scripts") rather than the generic text/html iframe path
        // (sandbox=""). Without the format check, an AI-generated interactive artifact would render
        // as a static HTML page with scripts blocked — defeating the purpose of the format.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {assetFile: {format: 'HTML', metadataJson: null}},
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('../hooks/useFileContent', () => ({
            default: () => ({
                content: '<!doctype html><html><head></head><body><h1>App</h1></body></html>',
                loading: false,
                mimeType: 'text/html',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: HtmlViewer} = await import('../AiHubFileViewer');

        render(<HtmlViewer fileId="42" name="widget.html" viewMode="preview" />);

        const iframe = screen.getByTitle('widget.html') as HTMLIFrameElement;

        expect(iframe).toHaveAttribute('sandbox', 'allow-scripts');
    });

    it('falls back to the generic text/html branch when format is null (user-uploaded HTML)', async () => {
        // Trust is conferred by the format column, not the mime type. A user-uploaded HTML file has
        // mime=text/html, format=null and continues to render with sandbox="" (no scripts) so we
        // do not grant scripts to untrusted content.
        vi.resetModules();

        vi.doMock('@/shared/middleware/graphql', () => ({
            useGetAssetFileQuery: () => ({
                data: {assetFile: {format: null, metadataJson: null}},
                error: null,
                isLoading: false,
            }),
        }));

        vi.doMock('../hooks/useFileContent', () => ({
            default: () => ({
                content: '<!doctype html><html><head></head><body>uploaded</body></html>',
                loading: false,
                mimeType: 'text/html',
            }),
        }));

        vi.doMock('@/shared/components/MonacoEditorWrapper', () => ({
            default: ({value}: {value: string}) => <div data-testid="monaco-editor">{value}</div>,
        }));

        const {default: UploadedHtmlViewer} = await import('../AiHubFileViewer');

        render(<UploadedHtmlViewer fileId="100" name="upload.html" viewMode="preview" />);

        const iframe = screen.getByTitle('upload.html') as HTMLIFrameElement;

        expect(iframe).toHaveAttribute('sandbox', '');
    });
});
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx
```

Expected: FAIL on the new HTML format test (router does not check format=HTML yet).

- [ ] **Step 3: Add the dispatch branch to AiHubFileViewer**

Edit `AiHubFileViewer.tsx`. Add the import at the top (sorted alphabetically with siblings):

```tsx
import AiHubChartPane from './AiHubChartPane';
import AiHubHtmlInteractivePane from './AiHubHtmlInteractivePane';
import useFileContent from './hooks/useFileContent';
```

Then in the `AiHubFileViewer` component body, after the `if (format === 'CHART')` block and before `if (IMAGE_MIME_TYPES.has(...))`, insert:

```tsx
if (format === 'HTML') {
    if (viewMode === 'editor') {
        return <EditorPane content={content} fileId={fileId} mimeType={mimeType} name={name} />;
    }

    if (viewMode === 'preview') {
        return <AiHubHtmlInteractivePane content={content} name={name} />;
    }

    return (
        <div className="flex size-full">
            <div className="size-full flex-1 border-r">
                <EditorPane content={content} fileId={fileId} mimeType={mimeType} name={name} />
            </div>

            <div className="size-full flex-1">
                <AiHubHtmlInteractivePane content={content} name={name} />
            </div>
        </div>
    );
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd client && npx vitest run src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx
```

Expected: all tests PASS — both the existing chart/markdown/image/pptx and the two new HTML cases.

- [ ] **Step 5: Run lint + typecheck**

```bash
cd client && npm run lint && npm run typecheck
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add client/src/pages/automation/ai-hub/AiHubFileViewer.tsx \
        client/src/pages/automation/ai-hub/tests/AiHubFileViewer.test.tsx
git commit -m "$(cat <<'EOF'
client - HTML Artifact - Wire format=HTML to interactive pane in viewer

Format-discriminated dispatch goes ahead of the mime-only path so AI-
generated HTML artifacts (format=HTML) render via the interactive pane
with sandbox="allow-scripts", while user-uploaded HTML files (format
null, mime text/html) continue to render via the existing sandbox=""
branch with scripts blocked. Trust is conferred by the format column,
not the mime type.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: Integration check

**Files:** none changed — verification only.

End-to-end smoke check that the agent path produces a renderable artifact.

- [ ] **Step 1: Run the full server check for the touched modules**

```bash
./gradlew :server:libs:automation:automation-asset-file:automation-asset-file-service:check \
          :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-service:check
```

Expected: BUILD SUCCESSFUL. All unit tests pass; spotless, checkstyle, PMD, SpotBugs all clean.

- [ ] **Step 2: Run the client checks**

```bash
cd client && npm run check
```

Expected: lint, typecheck, and Vitest all pass.

- [ ] **Step 3: Manual smoke test (developer-driven, non-automated)**

Boot the dev stack:

```bash
cd server && docker compose -f docker-compose.dev.infra.yml up -d
cd .. && ./gradlew -p server/apps/server-app bootRun
cd client && npm run dev
```

Log in as `admin@localhost.com` / `admin`, open AI Hub, send a prompt: "Make a countdown clock to next New Year's Eve as an HTML artifact." Expected: the agent calls `generateArtifact` with `format: HTML`; a chip appears in chat; clicking it opens the resource panel rendering the live, ticking countdown; the maximize button on the toolbar expands the iframe to cover the workspace area; Escape collapses.

If the agent reaches for `CODE` format with a `.html` extension instead of `HTML`, the steering line in the tool description needs strengthening — file a follow-up rather than weakening it inline. Do not patch around an LLM steering issue at the validator layer.

- [ ] **Step 4: Commit only if any spotless drift was applied**

```bash
git status
# If clean, no commit needed.
```

---

## Self-review checklist

The plan covers every section of the spec:

| Spec section | Plan task |
|---|---|
| §3.1 Persistence (enum append + ordinal pin) | Task 1 |
| §3.2 Generation (HtmlArtifactGenerator + CSP injection) | Task 3, Task 4 |
| §3.3 Rendering (interactive pane + dispatch) | Task 6, Task 7 |
| §4 Validator rules | Task 4 |
| §5 CSP injection | Task 3 (skeleton), Task 4 (final wiring) |
| §6 Tool input schema + steering | Task 5 |
| §7 Filename auto-extension | Task 3 (`defaultExtension()`) |
| §8 Telemetry | Deferred — call out in summary; existing artifact pipeline lacks per-format counters today, adding them sits in a separate observability task. |
| §9 Tests | Each task has its own test step. |
| §10 Risks | Encoded in Task 4 (validator rules) + Task 7 (format-discriminated trust dispatch). |
| §11 Module placement | Each task names exact paths. |
| §12 Edge cases (first-turn race, user-uploaded HTML, CE) | First-turn race is inherited from `AbstractTextArtifactGenerator` — no plan task needed. User-uploaded HTML covered in Task 7. CE without ai-hub inherits the `@ConditionalOnProperty` gate from the `@Component` annotation — no plan task needed. |
| §13 Implementation sequence | Matches the task order: enum → dependency → generator skeleton → strict validation → tool wire-in → React pane → React dispatch → integration. |

**Note on telemetry deferral:** §8 of the spec calls for `bytechef_artifact_generated{format}` and `bytechef_artifact_generation_rejected{format,reason}` counters. After scanning the existing artifact code, no per-format counters exist today on any of the eight existing formats. Adding them as part of this plan would either (a) wedge an unrelated observability change into an HTML-only feature, or (b) instrument only HTML, leaving the others uncovered. Both are wrong. Recommend filing a separate small task to add per-format counters across all generators uniformly; this plan does not block on it.
