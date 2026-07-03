/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that converts structured slide input into a {@code .pptx} file via Apache POI's
 * {@link XMLSlideShow}. Returns base64-encoded bytes of the generated presentation together with metadata (MIME type,
 * slide count, byte count) so callers can save the file as a workspace asset.
 *
 * <p>
 * The tool caps input at {@value #MAX_SLIDES} slides. If the caller supplies more slides, the excess is silently
 * dropped and a {@code truncated: true} flag is included in the response.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateSlideDeckToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        Produce a PowerPoint (.pptx) presentation from structured slide input.
        Returns base64-encoded pptx bytes. The caller saves the bytes as a
        workspace asset file.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "title": {"type": "string", "description": "Deck title (used for the title slide)"},
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

    private static final int MAX_SLIDES = 15;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createSlideDeck")
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
            Input input = jsonMapper.readValue(toolInput, Input.class);

            if (input.title() == null || input.title()
                .isBlank()) {
                return toolError("title is required and must not be blank");
            }

            if (input.slides() == null || input.slides()
                .isEmpty()) {
                return toolError("slides must be a non-empty array");
            }

            boolean truncated = input.slides()
                .size() > MAX_SLIDES;

            List<SlideInput> effectiveSlides = truncated ? input.slides()
                .subList(0, MAX_SLIDES) : input.slides();

            byte[] pptxBytes = buildPptx(input.title(), effectiveSlides);

            String base64 = Base64.getEncoder()
                .encodeToString(pptxBytes);

            Map<String, Object> result = new LinkedHashMap<>();

            result.put("pptxBytes", base64);
            result.put("mimeType",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            result.put("slideCount", effectiveSlides.size());
            result.put("byteCount", pptxBytes.length);
            result.put("truncated", truncated);

            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (IOException exception) {
            return toolError("Failed to build slide deck: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateSlideDeckToolCallback.class, "createSlideDeck", exception);
        }
    }

    private byte[] buildPptx(String title, List<SlideInput> slides) throws IOException {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFSlideLayout titleLayout = ppt.getSlideMasters()
                .get(0)
                .getLayout(SlideLayout.TITLE);

            XSLFSlide titleSlide = ppt.createSlide(titleLayout);

            XSLFTextShape titlePlaceholder = titleSlide.getPlaceholder(0);

            if (titlePlaceholder != null) {
                titlePlaceholder.setText(title);
            }

            for (SlideInput slideInput : slides) {
                XSLFSlideLayout contentLayout = ppt.getSlideMasters()
                    .get(0)
                    .getLayout(SlideLayout.TITLE_AND_CONTENT);

                XSLFSlide slide = ppt.createSlide(contentLayout);

                XSLFTextShape slideTitleShape = slide.getPlaceholder(0);

                if (slideTitleShape != null) {
                    slideTitleShape.setText(slideInput.title());
                }

                List<String> bullets = slideInput.bullets();

                if (bullets != null && !bullets.isEmpty()) {
                    XSLFTextShape bodyShape = slide.getPlaceholder(1);

                    if (bodyShape != null) {
                        bodyShape.clearText();

                        for (String bullet : bullets) {
                            XSLFTextParagraph paragraph = bodyShape.addNewTextParagraph();

                            paragraph.addNewTextRun()
                                .setText(bullet);
                        }
                    }
                }

                String notes = slideInput.notes();

                if (notes != null && !notes.isBlank()) {
                    XSLFNotes notesSlide = ppt.getNotesSlide(slide);

                    if (notesSlide != null) {
                        XSLFTextShape notesBody = notesSlide.getPlaceholder(1);

                        if (notesBody != null) {
                            notesBody.setText(notes);
                        }
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ppt.write(baos);

            return baos.toByteArray();
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record Input(String title, List<SlideInput> slides) {
        public Input {
            slides = slides == null ? List.of() : List.copyOf(slides);
        }
    }

    public record SlideInput(String title, @Nullable List<String> bullets, @Nullable String notes) {
        public SlideInput {
            bullets = bullets == null ? null : List.copyOf(bullets);
        }
    }

    public record Output(String pptxBytes, String mimeType, int slideCount, int byteCount) {
    }
}
