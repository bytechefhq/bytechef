/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

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
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFileService;
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
            mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));

        assertThat(generator.format()).isEqualTo(AssetFileFormat.HTML);
    }

    @Test
    void testGenerateHappyPathPersistsAsTextHtmlWithCspInjected() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

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

        String persisted = contentCaptor.getValue();

        assertThat(persisted)
            .contains("Content-Security-Policy")
            .contains("connect-src 'none'")
            .contains("script-src 'unsafe-inline'");
    }

    @Test
    void testGenerateAppendsHtmlExtensionWhenMissing() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

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

        // The eq("widget.html") matcher above proves the extension was appended; if it weren't,
        // the createFromAi stub would not have matched and the call would have returned null.
    }

    @Test
    void testGenerateRejectsNonHtmlContent() {
        // Sanity guard: a request whose content isn't recognisable HTML must surface a validation error
        // rather than landing as a malformed asset_file row. Detailed structural rules are pinned in
        // Chat 4's tests.
        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", "not html", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGenerateRejectsContentOverOneMegabyteWithActionableMessage() {
        // Pin the actionable error message — the LLM can choose between trimming an inlined data URI
        // and reauthoring the artifact only when it knows the actual byte count vs. the 1 MB cap.
        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));

        StringBuilder big = new StringBuilder("<!doctype html><html><head></head><body>");

        while (big.length() < 1_100_000) {
            big.append("aaaaaaaaaa");
        }

        big.append("</body></html>");

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", big.toString(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 MB size cap")
                .hasMessageContaining("actual size is")
                .hasMessageContaining("bytes");
    }

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
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

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

    @Test
    void testGenerateRejectsFramesetTag() {
        // <frameset> is in the rejection set but was missing from the error-message parenthetical
        // before this fix; pin both the rejection and the message wording.
        HtmlArtifactGenerator generator = newGenerator();

        String html = """
            <!doctype html><html><head></head>
            <frameset><frame src="about:blank"></frameset></html>
            """;

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nested browsing context")
                .hasMessageContaining("frameset");
    }

    @Test
    void testGenerateRejectsExternalUrlInMultiEntrySrcset() {
        // Smuggle test: a srcset that leads with a data: candidate but includes an external URL as the
        // second entry must still be rejected. Without per-candidate validation, the leading data: prefix
        // would pass a naive startsWith check.
        HtmlArtifactGenerator generator = newGenerator();

        String html = """
            <!doctype html><html><head></head>
            <body>
              <img src="data:image/gif;base64,abc"
                   srcset="data:image/gif;base64,abc 1x, https://tracker.example/img.jpg 2x">
            </body></html>
            """;

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("external image srcset")
                .hasMessageContaining("tracker.example");
    }

    @Test
    void testGenerateAllowsAllDataUriSrcset() {
        // Sanity guard: a fully-data-URI multi-entry srcset must pass. Otherwise the new per-candidate
        // validator could become overzealous.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

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
            <body>
              <img src="data:image/gif;base64,abc"
                   srcset="data:image/gif;base64,abc 1x, data:image/png;base64,xyz 2x">
            </body></html>
            """;

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.HTML);
    }

    @Test
    void testGenerateInjectsCspBeforeAnyAuthorSuppliedCsp() {
        // Stacked CSPs are evaluated as the intersection by browsers — the LLM cannot weaken our policy
        // by authoring a more permissive one of its own. Pin the prepend ordering so a future refactor
        // switching to append (which would let the LLM-authored CSP win on permissive directives) fails
        // the build instead of silently inverting the security boundary.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("x.html");

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("x.html"), eq("text/html"), contentCaptor.capture(),
            eq(AssetFileFormat.HTML), any(), any(), any()))
                .thenReturn(saved);

        HtmlArtifactGenerator generator = new HtmlArtifactGenerator(facade, linkService);

        String html = """
            <!doctype html>
            <html>
              <head>
                <meta http-equiv="Content-Security-Policy" content="default-src *">
                <title>Author CSP</title>
              </head>
              <body><h1>x</h1></body>
            </html>
            """;

        generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "x.html", html, null));

        String persisted = contentCaptor.getValue();

        int ourCspIndex = persisted.indexOf("connect-src 'none'");
        int authorCspIndex = persisted.indexOf("default-src *");

        assertThat(ourCspIndex).isGreaterThan(-1);
        assertThat(authorCspIndex).isGreaterThan(-1);
        assertThat(ourCspIndex).isLessThan(authorCspIndex);
    }

    private HtmlArtifactGenerator newGenerator() {
        return new HtmlArtifactGenerator(mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));
    }
}
