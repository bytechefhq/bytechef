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
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import com.bytechef.ee.ai.hub.task.AuthorshipAlreadyAssignedException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Persists a PowerPoint deck built from a structured JSON spec the LLM produces. Spec shape:
 *
 * <pre>{@code { "title": "Q4 Review", "slides": [ {"title": "Cover", "bullets": []}, {"title": "Revenue", "bullets":
 * ["+12% YoY", "Subscription drove growth", "Enterprise lagged"]} ] } }</pre>
 *
 * <p>
 * Layout is intentionally simple: every slide is a title text-box at the top + a bullets text-box below. Real master
 * slide / theme support is out of scope for v1 — POI's master-slide handling adds significant complexity for
 * incremental win, and the dominant use case (a deck the user opens in Keynote/PowerPoint and themes manually) does not
 * benefit from server-side themes anyway. The original spec is stored in {@code metadata_json} so the future
 * "regenerate with theme X" flow can replay against an updated layout policy without going back to the LLM.
 * </p>
 *
 * <p>
 * Slide dimensions follow the POI default (10in × 7.5in, 4:3). 16:9 is the modern default but flipping to it would
 * require setting {@code ppt.setPageSize(...)} which is correct but cosmetic — defer until a user actually asks for
 * widescreen, then make it a spec field.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
public class PptxArtifactGenerator implements ArtifactGenerator {

    private static final Logger log = LoggerFactory.getLogger(PptxArtifactGenerator.class);

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    /**
     * Slide canvas geometry (POI default 10x7.5 inches at 72 DPI = 720x540 EMU points). Title takes the top 60pt; the
     * bullets box fills the rest of the canvas with 36pt of side padding.
     */
    private static final int SLIDE_WIDTH = 720;
    private static final int SLIDE_HEIGHT = 540;
    private static final int TITLE_HEIGHT = 60;
    private static final int SIDE_PADDING = 36;

    private final AssetFileFacade assetFileFacade;
    private final AiHubTaskAssetFileService taskAssetFileService;

    public PptxArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        this.assetFileFacade = assetFileFacade;
        this.taskAssetFileService = taskAssetFileService;
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.PPTX;
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        PptxSpec spec = parseSpec(request.payload());

        byte[] bytes = renderPptx(spec);
        String filename = ensureExtension(request.filename());

        AssetFile saved = assetFileFacade.createBinaryFromAi(
            request.workspaceId(),
            request.environmentId(),
            filename,
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            bytes,
            AssetFileFormat.PPTX,
            request.payload(),
            request.generatedByAgentSource(),
            request.generatedFromPrompt());

        boolean linked = false;
        Long taskId = request.taskId();

        if (taskId != null) {
            try {
                taskAssetFileService.recordAuthorship(taskId, saved.getId());
                linked = true;
            } catch (AuthorshipAlreadyAssignedException exception) {
                log.warn(
                    "Generated PPTX asset_file {} already authored by another task; skipping AUTHORED join "
                        + "for task {}",
                    saved.getId(), taskId, exception);
            }
        }

        return new GenerationResult(saved.getId(), saved.getName(), AssetFileFormat.PPTX, linked);
    }

    private static PptxSpec parseSpec(String payload) {
        try {
            PptxSpec spec = JSON_MAPPER.readValue(payload, PptxSpec.class);

            if (spec == null) {
                throw new IllegalArgumentException("PPTX spec is empty");
            }

            return spec;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                "PPTX spec is not valid JSON: " + exception.getMessage(), exception);
        }
    }

    private static byte[] renderPptx(PptxSpec spec) {
        try (XMLSlideShow ppt = new XMLSlideShow();
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            List<PptxSlide> specSlides = spec.slides();
            List<PptxSlide> slides = specSlides == null ? List.of() : specSlides;

            if (slides.isEmpty()) {
                // Empty deck still produces a valid file with one blank slide. A zero-slide pptx is technically
                // valid but Keynote in particular silently treats it as corrupt — this gives the user something
                // useful to see while the model regenerates.
                ppt.createSlide();
            } else {
                for (PptxSlide slide : slides) {
                    if (slide == null) {
                        continue;
                    }

                    XSLFSlide xslfSlide = ppt.createSlide();

                    String slideTitle = slide.title();

                    if (slideTitle != null && !slideTitle.isBlank()) {
                        XSLFTextBox titleBox = xslfSlide.createTextBox();

                        titleBox.setAnchor(new Rectangle(SIDE_PADDING, SIDE_PADDING,
                            SLIDE_WIDTH - 2 * SIDE_PADDING, TITLE_HEIGHT));

                        XSLFTextParagraph titleParagraph = titleBox.addNewTextParagraph();

                        XSLFTextRun titleRun = titleParagraph.addNewTextRun();

                        titleRun.setBold(true);
                        titleRun.setFontSize(28d);
                        titleRun.setText(slideTitle);
                    }

                    List<String> bullets = slide.bullets();

                    if (bullets != null && !bullets.isEmpty()) {
                        XSLFTextBox bulletsBox = xslfSlide.createTextBox();

                        int bulletsTop = SIDE_PADDING + TITLE_HEIGHT + SIDE_PADDING;

                        bulletsBox.setAnchor(new Rectangle(SIDE_PADDING, bulletsTop,
                            SLIDE_WIDTH - 2 * SIDE_PADDING, SLIDE_HEIGHT - bulletsTop - SIDE_PADDING));

                        for (String bullet : bullets) {
                            if (bullet == null) {
                                continue;
                            }

                            XSLFTextParagraph bulletParagraph = bulletsBox.addNewTextParagraph();

                            // setBullet(true) is sufficient for an unordered bullet — auto-numbering would force a
                            // numbered list (1, 2, 3) and POI rejects startAt=0 anyway. Numbered lists can be a
                            // future spec field once we have a concrete user need.
                            bulletParagraph.setBullet(true);
                            bulletParagraph.setTextAlign(TextParagraph.TextAlign.LEFT);

                            XSLFTextRun bulletRun = bulletParagraph.addNewTextRun();

                            bulletRun.setFontSize(18d);
                            bulletRun.setText(bullet);
                        }
                    }
                }
            }

            ppt.write(output);

            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                "Failed to render PPTX deck — POI write failure", exception);
        }
    }

    private static String ensureExtension(String filename) {
        if (filename.toLowerCase()
            .endsWith(".pptx")) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename;
        }

        return filename + ".pptx";
    }

    public record PptxSpec(@Nullable String title, @Nullable List<PptxSlide> slides) {

        public PptxSpec {
            slides = slides == null ? null : List.copyOf(slides);
        }
    }

    public record PptxSlide(@Nullable String title, @Nullable List<String> bullets) {

        public PptxSlide {
            bullets = bullets == null ? null : List.copyOf(bullets);
        }
    }
}
