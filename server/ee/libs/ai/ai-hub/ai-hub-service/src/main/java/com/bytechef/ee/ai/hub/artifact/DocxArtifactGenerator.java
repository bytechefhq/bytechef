/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFileService;
import com.bytechef.ee.ai.hub.chat.AuthorshipAlreadyAssignedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Persists a Word document built from a structured JSON spec the LLM produces. Spec shape:
 *
 * <pre>{@code { "title": "Q4 Revenue Report", "sections": [ {"heading": "Summary", "paragraphs": ["Revenue rose 12%.",
 * "Gross margin held steady."]}, {"heading": "Detail", "paragraphs": ["..."]} ] } }</pre>
 *
 * <p>
 * Title and headings render as styled paragraphs (bold, larger size); paragraphs as plain text. Bullet lists are
 * intentionally left out of v1 — the LLM produces unordered lists naturally inline as paragraphs starting with "• "
 * which renders fine in Word, and adding numbered/bulleted styling triples the spec surface for diminishing return. Add
 * when there's a concrete user request.
 * </p>
 *
 * <p>
 * Output is binary, so this generator does not extend {@link AbstractTextArtifactGenerator} — it persists via
 * {@code createBinaryFromAi} directly. The original spec is stored verbatim in {@code metadata_json} so the viewer can
 * offer "edit this section and regenerate" without re-prompting the LLM for the full document.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
public class DocxArtifactGenerator implements ArtifactGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocxArtifactGenerator.class);

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private final AssetFileFacade assetFileFacade;
    private final AiHubChatAssetFileService chatAssetFileService;

    public DocxArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubChatAssetFileService chatAssetFileService) {

        this.assetFileFacade = assetFileFacade;
        this.chatAssetFileService = chatAssetFileService;
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.DOCX;
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        DocxSpec spec = parseSpec(request.payload());

        byte[] bytes = renderDocx(spec);
        String filename = ensureExtension(request.filename());

        AssetFile saved = assetFileFacade.createBinaryFromAi(
            request.workspaceId(),
            request.environmentId(),
            filename,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            bytes,
            AssetFileFormat.DOCX,
            request.payload(),
            // metadataJson holds the original spec so the viewer can offer "regenerate from edits" without
            // a round-trip to the LLM. Distinct from request.metadataJson() which is reserved for an
            // explicit caller-supplied override; passing the spec through this slot is the default for DOCX
            // because the spec IS the regen-source-of-truth.
            request.generatedByAgentSource(),
            request.generatedFromPrompt());

        boolean linked = false;
        Long chatId = request.chatId();

        if (chatId != null) {
            try {
                chatAssetFileService.recordAuthorship(chatId, saved.getId());
                linked = true;
            } catch (AuthorshipAlreadyAssignedException exception) {
                log.warn(
                    "Generated DOCX asset_file {} already authored by another chat; skipping AUTHORED join "
                        + "for chat {}",
                    saved.getId(), chatId, exception);
            }
        }

        return new GenerationResult(saved.getId(), saved.getName(), AssetFileFormat.DOCX, linked);
    }

    private static DocxSpec parseSpec(String payload) {
        try {
            DocxSpec spec = JSON_MAPPER.readValue(payload, DocxSpec.class);

            if (spec == null) {
                throw new IllegalArgumentException("DOCX spec is empty");
            }

            return spec;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                "DOCX spec is not valid JSON or is missing required fields: " + exception.getMessage(), exception);
        }
    }

    /**
     * Renders the spec into XWPF bytes. Holds the document open inside a try-with-resources so the underlying OOXML
     * package descriptor is always closed even when the writer throws — leaking the descriptor manifests as a "package
     * not closed" warning at JVM shutdown rather than a per-request leak, but the closer-discipline matters the moment
     * we wire this into a long-running tool worker.
     */
    private static byte[] renderDocx(DocxSpec spec) {
        try (XWPFDocument doc = new XWPFDocument();
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            String title = spec.title();

            if (title != null && !title.isBlank()) {
                XWPFParagraph titleParagraph = doc.createParagraph();

                titleParagraph.setAlignment(ParagraphAlignment.CENTER);

                XWPFRun titleRun = titleParagraph.createRun();

                titleRun.setBold(true);
                titleRun.setFontSize(20);
                titleRun.setText(title);
            }

            List<DocxSection> sections = spec.sections();

            if (sections != null) {
                for (DocxSection section : sections) {
                    if (section == null) {
                        continue;
                    }

                    String heading = section.heading();

                    if (heading != null && !heading.isBlank()) {
                        XWPFParagraph headingParagraph = doc.createParagraph();
                        XWPFRun headingRun = headingParagraph.createRun();

                        headingRun.setBold(true);
                        headingRun.setFontSize(14);
                        headingRun.setText(heading);
                    }

                    List<String> paragraphs = section.paragraphs();

                    if (paragraphs != null) {
                        for (String paragraph : paragraphs) {
                            if (paragraph == null) {
                                continue;
                            }

                            XWPFParagraph bodyParagraph = doc.createParagraph();
                            XWPFRun bodyRun = bodyParagraph.createRun();

                            bodyRun.setText(paragraph);
                        }
                    }
                }
            }

            doc.write(output);

            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                "Failed to render DOCX document — POI write failure", exception);
        }
    }

    private static String ensureExtension(String filename) {
        if (filename.toLowerCase()
            .endsWith(".docx")) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            // Caller picked a non-.docx extension on purpose (e.g. ".doc.bak"). Don't override.
            return filename;
        }

        return filename + ".docx";
    }

    /**
     * Top-level structured spec the LLM emits. Tolerant by design — every field is nullable so a partial spec still
     * produces a syntactically valid document rather than a hard error. The spec is small enough that the field-level
     * tolerance never produces a meaningfully wrong document; whereas rejecting "title-only no sections" would block a
     * valid use case (cover page, single-section memo).
     */
    public record DocxSpec(@Nullable String title, @Nullable List<DocxSection> sections) {

        public DocxSpec {
            sections = sections == null ? null : List.copyOf(sections);
        }
    }

    public record DocxSection(@Nullable String heading, @Nullable List<String> paragraphs) {

        public DocxSection {
            paragraphs = paragraphs == null ? null : List.copyOf(paragraphs);
        }
    }
}
