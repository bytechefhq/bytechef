/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.tool.CreateSlideDeckToolCallback.SlideInput;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CreateSlideDeckToolCallbackTest {

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallBuildsTheDeckAndSavesItAsAWorkspaceFile() throws IOException {
        when(assetFileFacade.createBinaryFromAi(
            eq(3L), eq(2), eq("q3-roadmap.pptx"), eq(CreateSlideDeckToolCallback.MIME_TYPE), any(), any(), any(),
            any(), any())).thenReturn(assetFile());

        JsonNode node = jsonMapper.readTree(
            newCallback().call(
                """
                    {"title":"Q3 roadmap","filename":"q3-roadmap","slides":[
                        {"title":"Goals","bullets":["Ship it","Measure it"],"notes":"Keep it short"},
                        {"title":"Risks"}]}""",
                toolContext()));

        assertThat(node.get("fileId")
            .asString()).isEqualTo("21");
        assertThat(node.get("slideCount")
            .asInt()).isEqualTo(2);
        assertThat(node.get("truncated")
            .asBoolean()).isFalse();

        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);

        verify(assetFileFacade).createBinaryFromAi(
            eq(3L), eq(2), eq("q3-roadmap.pptx"), eq(CreateSlideDeckToolCallback.MIME_TYPE), bytesCaptor.capture(),
            any(), any(), any(), any());

        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(bytesCaptor.getValue()))) {
            List<XSLFSlide> slides = slideShow.getSlides();

            assertThat(slides).hasSize(3);
            assertThat(slides.get(1)
                .getTitle()).isEqualTo("Goals");
        }

        verify(artifactRecorder).record("thread-1", 7L, "FILE_CREATED", "21", "q3-roadmap.pptx");
    }

    @Test
    void testCallDropsSlidesBeyondTheMaximum() {
        when(assetFileFacade.createBinaryFromAi(any(), eq(2), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(assetFile());

        String slides = String.join(
            ",", IntStream.rangeClosed(1, CreateSlideDeckToolCallback.MAX_SLIDES + 2)
                .mapToObj(index -> "{\"title\":\"Slide " + index + "\"}")
                .toList());

        JsonNode node = jsonMapper.readTree(
            newCallback().call("{\"title\":\"Long deck\",\"slides\":[" + slides + "]}", toolContext()));

        assertThat(node.get("slideCount")
            .asInt()).isEqualTo(CreateSlideDeckToolCallback.MAX_SLIDES);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
    }

    @Test
    void testCallReturnsErrorWhenTitleIsBlank() {
        assertThat(newCallback().call("{\"title\":\" \",\"slides\":[{\"title\":\"A\"}]}", toolContext()))
            .contains("title is required");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallReturnsErrorWhenSlidesAreEmpty() {
        assertThat(newCallback().call("{\"title\":\"Deck\",\"slides\":[]}", toolContext()))
            .contains("slides must be a non-empty array");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallReturnsErrorWithoutAWorkspace() {
        assertThat(newCallback().call("{\"title\":\"Deck\",\"slides\":[{\"title\":\"A\"}]}"))
            .contains("Workspace context unavailable");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testBuildPptxWritesATitleSlidePlusOneSlidePerInput() throws IOException {
        byte[] pptxBytes = CreateSlideDeckToolCallback.buildPptx(
            "Deck", List.of(new SlideInput("One", List.of("a", "b"), null), new SlideInput("Two", null, "notes")));

        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(pptxBytes))) {
            assertThat(slideShow.getSlides()).hasSize(3);
        }
    }

    @Test
    void testResolveFilenameAddsThePptxExtension() {
        assertThat(CreateSlideDeckToolCallback.resolveFilename("deck")).isEqualTo("deck.pptx");
        assertThat(CreateSlideDeckToolCallback.resolveFilename("Deck.PPTX")).isEqualTo("Deck.PPTX");
        assertThat(CreateSlideDeckToolCallback.resolveFilename(null)).startsWith("slide-deck-")
            .endsWith(".pptx");
    }

    private CreateSlideDeckToolCallback newCallback() {
        return new CreateSlideDeckToolCallback(assetFileFacade, artifactRecorder);
    }

    private static AssetFile assetFile() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(21L);
        assetFile.setName("q3-roadmap.pptx");

        return assetFile;
    }

    private static ToolContext toolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }
}
