/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeArtifactGeneratorTest {

    @Test
    void testGenerateMapsPyExtensionToPythonMimeType() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("script.py");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("script.py"), eq("text/x-python"), any(),
            eq(AssetFileFormat.CODE), any(), any(), any()))
                .thenReturn(saved);

        CodeArtifactGenerator generator = new CodeArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "script.py", "print('hi')", null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.CODE);
    }

    @Test
    void testGenerateFallsBackToTextPlainForUnknownExtension() {
        // The mime-type table is intentionally not exhaustive — Monaco does its own language detection client-side,
        // so an unknown extension is at most a wrong content-type on download. Pin the fallback so a future widening
        // of the table doesn't accidentally change the fallback shape (e.g. throwing instead of returning text/plain).
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("config.weirdext");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("config.weirdext"), eq("text/plain"), any(),
            eq(AssetFileFormat.CODE), any(), any(), any()))
                .thenReturn(saved);

        CodeArtifactGenerator generator = new CodeArtifactGenerator(facade, linkService);

        generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "config.weirdext", "...", null));
    }

    @Test
    void testGenerateLeavesExtensionlessFilenameAlone() {
        // Code intentionally has no default extension — adding one would force a guess about which language the LLM
        // meant. The file persists with text/plain mime; Monaco then handles language detection on the client side.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("Dockerfile");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("Dockerfile"), eq("text/plain"), any(),
            eq(AssetFileFormat.CODE), any(), any(), any()))
                .thenReturn(saved);

        CodeArtifactGenerator generator = new CodeArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "Dockerfile", "FROM alpine", null));

        assertThat(result.filename()).isEqualTo("Dockerfile");
    }

    @Test
    void testFormatIsCode() {
        CodeArtifactGenerator generator = new CodeArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));

        assertThat(generator.format()).isEqualTo(AssetFileFormat.CODE);
    }
}
