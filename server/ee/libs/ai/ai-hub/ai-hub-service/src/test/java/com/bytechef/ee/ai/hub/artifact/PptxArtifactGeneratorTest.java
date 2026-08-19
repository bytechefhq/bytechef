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
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PptxArtifactGeneratorTest {

    @Test
    void testGenerateRendersSlidesWithTitleAndBullets() throws Exception {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("deck.pptx");
        when(facade.createBinaryFromAi(
            anyLong(), any(Integer.class), eq("deck.pptx"),
            eq("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            any(byte[].class), eq(AssetFileFormat.PPTX), any(), any(), any()))
                .thenReturn(saved);

        PptxArtifactGenerator generator = new PptxArtifactGenerator(facade, linkService);

        String spec = """
            {
              "title": "Q4 Review",
              "slides": [
                {"title": "Cover", "bullets": []},
                {"title": "Revenue", "bullets": ["+12% YoY", "Subscription drove growth"]}
              ]
            }""";

        generator.generate(new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "deck", spec, null));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        org.mockito.Mockito.verify(facade)
            .createBinaryFromAi(
                anyLong(), any(Integer.class), any(), any(), bytesCaptor.capture(),
                any(), any(), any(), any());

        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(bytesCaptor.getValue()))) {
            List<XSLFSlide> slides = ppt.getSlides();

            assertThat(slides).hasSize(2);

            String slide1Text = textOf(slides.get(0));
            String slide2Text = textOf(slides.get(1));

            assertThat(slide1Text).contains("Cover");
            assertThat(slide2Text).contains("Revenue");
            assertThat(slide2Text).contains("+12% YoY");
            assertThat(slide2Text).contains("Subscription drove growth");
        }
    }

    @Test
    void testGenerateProducesBlankSlideWhenSlidesEmpty() throws Exception {
        // A zero-slide PPTX is technically valid OOXML but Keynote treats it as corrupt. Pin: an empty slides list
        // produces a single blank slide rather than a zero-slide deck.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("blank.pptx");
        when(facade.createBinaryFromAi(
            anyLong(), any(Integer.class), any(), any(), any(byte[].class), any(), any(), any(), any()))
                .thenReturn(saved);

        PptxArtifactGenerator generator = new PptxArtifactGenerator(facade, linkService);

        generator.generate(new GenerationRequest(
            1L, 10L, 0, 7L, (short) 0, "x", "blank.pptx", "{\"title\":\"empty\",\"slides\":[]}", null));

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        org.mockito.Mockito.verify(facade)
            .createBinaryFromAi(
                anyLong(), any(Integer.class), any(), any(), bytesCaptor.capture(),
                any(), any(), any(), any());

        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(bytesCaptor.getValue()))) {
            assertThat(ppt.getSlides()).hasSize(1);
        }
    }

    @Test
    void testGenerateAppendsPptxExtension() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("preso.pptx");
        when(facade.createBinaryFromAi(
            anyLong(), any(Integer.class), eq("preso.pptx"), any(), any(byte[].class), any(), any(), any(), any()))
                .thenReturn(saved);

        PptxArtifactGenerator generator = new PptxArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(new GenerationRequest(
            1L, 10L, 0, 7L, (short) 0, "x", "preso", "{\"slides\":[]}", null));

        assertThat(result.filename()).isEqualTo("preso.pptx");
    }

    @Test
    void testGenerateRejectsMalformedSpec() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        PptxArtifactGenerator generator = new PptxArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "bad.pptx", "{ unclosed:", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PPTX spec is not valid JSON");
    }

    private static String textOf(XSLFSlide slide) {
        StringBuilder builder = new StringBuilder();

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                builder.append(textShape.getText())
                    .append('\n');
            }
        }

        return builder.toString();
    }
}
