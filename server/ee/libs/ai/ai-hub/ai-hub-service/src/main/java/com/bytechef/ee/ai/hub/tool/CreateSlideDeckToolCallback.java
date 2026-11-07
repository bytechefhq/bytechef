/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateSlideDeckToolCallback implements ToolCallback {

    static final String TOOL_NAME = "createSlideDeck";

    static final int MAX_SLIDES = 15;

    static final String MIME_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String DESCRIPTION = """
        Produce a PowerPoint (.pptx) presentation from structured slide input and save it as a
        workspace file. Returns the saved file's fileId and name, the slide count, and whether
        slides beyond the maximum of 15 were dropped.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "title": {"type": "string", "description": "Deck title (used for the title slide)"},
                    "filename": {"type": "string", "description": "Descriptive filename ending in .pptx (e.g. q3-roadmap.pptx)"},
                    "slides": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "title":   {"type": "string"},
                                "bullets": {"type": "array", "items": {"type": "string"}},
                                "notes":   {"type": "string", "description": "Optional speaker notes"}
                            },
                            "required": ["title"]
                        }
                    }
                },
                "required": ["title", "slides"]
            }""";

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final AssetFileFacade assetFileFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateSlideDeckToolCallback(
        AssetFileFacade assetFileFacade, @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.artifactRecorder = artifactRecorder;
        this.assetFileFacade = assetFileFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            SlideDeckInput input = JsonUtils.read(toolInput, SlideDeckInput.class);

            String title = input.title();

            if (title == null || title.isBlank()) {
                return ToolErrors.toolError("title is required and must not be blank");
            }

            List<SlideInput> slides = input.slides();

            if (slides.isEmpty()) {
                return ToolErrors.toolError("slides must be a non-empty array");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return ToolErrors.toolError("Workspace context unavailable - open this chat from a workspace.");
            }

            boolean truncated = slides.size() > MAX_SLIDES;

            List<SlideInput> effectiveSlides = truncated ? slides.subList(0, MAX_SLIDES) : slides;

            byte[] pptxBytes = buildPptx(title, effectiveSlides);

            AssetFile assetFile = assetFileFacade.createBinaryFromAi(
                invocationContext.workspaceId(),
                AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                resolveFilename(input.filename()), MIME_TYPE, pptxBytes, null, null, invocationContext.sourceOrdinal(),
                invocationContext.lastUserPrompt());

            String fileId = String.valueOf(assetFile.getId());

            if (artifactRecorder != null && invocationContext.threadId() != null) {
                artifactRecorder.record(
                    invocationContext.threadId(), invocationContext.userId(), "FILE_CREATED", fileId,
                    assetFile.getName());
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("fileId", fileId);
            result.put("name", assetFile.getName());
            result.put("mimeType", MIME_TYPE);
            result.put("slideCount", effectiveSlides.size());
            result.put("byteCount", pptxBytes.length);
            result.put("truncated", truncated);

            return JsonUtils.write(result);
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (IOException exception) {
            return ToolErrors.runtimeFailure(
                CreateSlideDeckToolCallback.class, TOOL_NAME, new IllegalStateException(exception));
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(CreateSlideDeckToolCallback.class, TOOL_NAME, exception);
        }
    }

    static byte[] buildPptx(String title, List<SlideInput> slides) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            List<XSLFSlideMaster> slideMasters = slideShow.getSlideMasters();

            XSLFSlideMaster slideMaster = slideMasters.getFirst();

            XSLFSlide titleSlide = slideShow.createSlide(slideMaster.getLayout(SlideLayout.TITLE));

            setText(titleSlide.getPlaceholder(0), title);

            XSLFSlideLayout contentLayout = slideMaster.getLayout(SlideLayout.TITLE_AND_CONTENT);

            for (SlideInput slideInput : slides) {
                XSLFSlide slide = slideShow.createSlide(contentLayout);

                setText(slide.getPlaceholder(0), slideInput.title());

                addBullets(slide.getPlaceholder(1), slideInput.bullets());

                String notes = slideInput.notes();

                if (notes != null && !notes.isBlank()) {
                    XSLFNotes notesSlide = slideShow.getNotesSlide(slide);

                    if (notesSlide != null) {
                        setText(notesSlide.getPlaceholder(1), notes);
                    }
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            slideShow.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    static String resolveFilename(@Nullable String filename) {
        if (filename == null || filename.isBlank()) {
            return "slide-deck-" + LocalDateTime.now()
                .format(FILENAME_FORMATTER) + ".pptx";
        }

        String trimmedFilename = filename.trim();

        return trimmedFilename.toLowerCase(Locale.ROOT)
            .endsWith(".pptx") ? trimmedFilename : trimmedFilename + ".pptx";
    }

    private static void addBullets(@Nullable XSLFTextShape bodyShape, @Nullable List<String> bullets) {
        if (bodyShape == null || bullets == null || bullets.isEmpty()) {
            return;
        }

        bodyShape.clearText();

        for (String bullet : bullets) {
            XSLFTextParagraph paragraph = bodyShape.addNewTextParagraph();

            paragraph.addNewTextRun()
                .setText(bullet);
        }
    }

    private static void setText(@Nullable XSLFTextShape textShape, @Nullable String text) {
        if (textShape != null && text != null) {
            textShape.setText(text);
        }
    }

    public record SlideDeckInput(@Nullable String title, @Nullable String filename, List<SlideInput> slides) {

        public SlideDeckInput {
            slides = slides == null ? List.of() : List.copyOf(slides);
        }
    }

    public record SlideInput(@Nullable String title, @Nullable List<String> bullets, @Nullable String notes) {

        public SlideInput {
            bullets = bullets == null ? null : List.copyOf(bullets);
        }
    }
}
