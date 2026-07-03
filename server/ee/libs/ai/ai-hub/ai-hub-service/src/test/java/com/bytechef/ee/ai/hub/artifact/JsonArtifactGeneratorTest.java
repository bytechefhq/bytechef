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
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JsonArtifactGeneratorTest {

    @Test
    void testGenerateAcceptsValidJsonObject() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("config.json");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("config.json"), eq("application/json"), any(),
            eq(AssetFileFormat.JSON), any(), any(), any()))
                .thenReturn(saved);

        JsonArtifactGenerator generator = new JsonArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "config", "{\"a\":1,\"b\":[2,3]}", null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.JSON);
        assertThat(result.filename()).isEqualTo("config.json");
    }

    @Test
    void testGenerateAcceptsValidJsonArray() {
        // JSON arrays at the top level are valid JSON (RFC 7159). The validator must not assume the payload is
        // always an object — pin the array path so a regression that narrowed validation to objects only would fail
        // here.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("items.json");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(),
            eq(AssetFileFormat.JSON), any(), any(), any()))
                .thenReturn(saved);

        JsonArtifactGenerator generator = new JsonArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "items.json", "[1, 2, 3]", null));

        assertThat(result.assetFileId()).isEqualTo(42L);
    }

    @Test
    void testValidationRejectsMalformedJson() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        JsonArtifactGenerator generator = new JsonArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "bad.json", "{ unclosed: ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not well-formed");
    }

    @Test
    void testValidationRejectsTrailingCommaJson5Style() {
        // The Jackson default parser does NOT accept JSON5 trailing commas; if the LLM emits "[1,2,]" we want to
        // reject so the model self-corrects, rather than sneaking through and failing at the consumer's parser.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        JsonArtifactGenerator generator = new JsonArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "trail.json", "[1, 2, ]", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidationRejectsEmptyContent() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        JsonArtifactGenerator generator = new JsonArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "empty.json", "", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON content is empty");
    }
}
