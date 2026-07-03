/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateSlideDeckToolCallbackTest {

    private static final JsonMapper jsonMapper = new JsonMapper();
    private static final CreateSlideDeckToolCallback callback = new CreateSlideDeckToolCallback();

    @Test
    void testToolDefinitionExposesCreateSlideDeckName() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("createSlideDeck");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("slides");
        assertThat(definition.inputSchema()).contains("title");
    }

    @Test
    void testCallProducesValidPptxWithMatchingSlideCount() throws Exception {
        String input = """
            {
                "title": "Test Deck",
                "slides": [
                    {"title": "Slide Alpha", "bullets": ["Point one", "Point two"]},
                    {"title": "Slide Beta",  "bullets": ["Item A", "Item B"], "notes": "Speaker note here"},
                    {"title": "Slide Gamma", "bullets": ["Only bullet"]}
                ]
            }""";

        String result = callback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("slideCount")
            .asInt()).isEqualTo(3);
        assertThat(node.get("truncated")
            .asBoolean()).isFalse();
        assertThat(node.get("mimeType")
            .asText())
                .isEqualTo("application/vnd.openxmlformats-officedocument.presentationml.presentation");

        byte[] pptxBytes = Base64.getDecoder()
            .decode(node.get("pptxBytes")
                .asText());

        assertThat(pptxBytes).startsWith((byte) 'P', (byte) 'K', (byte) 0x03, (byte) 0x04);

        Set<String> zipEntries = new HashSet<>();
        StringBuilder xmlContents = new StringBuilder();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pptxBytes))) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                zipEntries.add(entry.getName());

                if (entry.getName()
                    .endsWith(".xml")) {
                    xmlContents.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }

                zip.closeEntry();
            }
        }

        assertThat(zipEntries).anyMatch(entryName -> entryName.startsWith("ppt/slides/slide"));

        String xmlText = xmlContents.toString();

        assertThat(xmlText).contains("Slide Alpha");
        assertThat(xmlText).contains("Slide Beta");
        assertThat(xmlText).contains("Slide Gamma");
    }

    @Test
    void testCallReturnsErrorOnEmptySlides() throws Exception {
        String input = """
            {
                "title": "Empty Deck",
                "slides": []
            }""";

        String result = callback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("slides");
    }

    @Test
    void testCallReturnsErrorOnBlankTitle() throws Exception {
        String input = """
            {
                "title": "   ",
                "slides": [{"title": "Slide One"}]
            }""";

        String result = callback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("title");
    }

    @Test
    void testCallCapsSlidesAtFifteen() throws Exception {
        StringBuilder slidesJson = new StringBuilder("[");

        for (int slideIndex = 1; slideIndex <= 20; slideIndex++) {
            if (slideIndex > 1) {
                slidesJson.append(",");
            }

            slidesJson.append("{\"title\": \"Slide ")
                .append(slideIndex)
                .append("\"}");
        }

        slidesJson.append("]");

        String input = "{\"title\": \"Big Deck\", \"slides\": " + slidesJson + "}";

        String result = callback.call(input);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("slideCount")
            .asInt()).isEqualTo(15);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
    }
}
