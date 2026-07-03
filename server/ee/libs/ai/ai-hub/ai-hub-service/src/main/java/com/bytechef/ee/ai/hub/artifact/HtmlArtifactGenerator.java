/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import java.nio.charset.StandardCharsets;
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
 * sources, no nested browsing contexts, no external form actions, no {@code <base>} tag, 1 MB cap. All rules are
 * enforced by {@code HtmlArtifactValidator} in a single pass that collects every violation before throwing.</li>
 * <li><strong>Server-side CSP injection</strong> — a {@code Content-Security-Policy} meta tag is inserted as the first
 * child of {@code <head>} before persistence. The LLM cannot defeat this; if the artifact already has its own CSP meta,
 * ours sits in front and browsers honour the intersection of stacked policies.</li>
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
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class HtmlArtifactGenerator extends AbstractTextArtifactGenerator {

    private static final String CSP_META =
        "<meta http-equiv=\"Content-Security-Policy\" content=\""
            + "default-src 'none'; "
            + "script-src 'unsafe-inline' data:; "
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

    /**
     * Strict validator: parses the payload and delegates to {@link HtmlArtifactValidator}, which walks the document
     * tree once and accumulates every violation before throwing. Single-pass collection lets the LLM fix the whole
     * batch on the next turn instead of round-tripping per rule.
     */
    @Override
    protected void validate(GenerationRequest request) {
        Document parsed = parseStrict(request.payload());

        HtmlArtifactValidator.validate(parsed);
    }

    /**
     * Injects the Content-Security-Policy meta tag as the first child of {@code <head>} before persistence. The base
     * class calls this hook after validation and before writing the content to the {@code asset_file} row. A size cap
     * is enforced here so the persisted content (post-injection) stays within 1 MB.
     */
    @Override
    protected String transformPayload(GenerationRequest request) {
        Document doc = parseStrict(request.payload());

        injectCsp(doc);

        String injected = doc.outerHtml();

        if (injected.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new IllegalArgumentException(
                "HTML artifact exceeds the 1 MB size cap after CSP injection: actual size is "
                    + injected.getBytes(StandardCharsets.UTF_8).length + " bytes.");
        }

        return injected;
    }

    private static Document parseStrict(String html) {
        // Empty baseUri because the artifact must be self-contained — relative URLs resolved against a base
        // would defeat the no-external-refs rule introduced in Task 4.
        return Jsoup.parse(html, "", Parser.htmlParser());
    }

    private static void injectCsp(Document doc) {
        Element head = doc.head();

        // Place CSP as head's first child so any author-supplied CSP meta lands AFTER ours; browsers
        // intersect stacked CSPs, so the LLM cannot weaken our policy with its own.
        head.prepend(CSP_META);
    }
}
