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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFile;
import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFileService;
import com.bytechef.ee.ai.hub.chat.AuthorshipAlreadyAssignedException;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MarkdownArtifactGeneratorTest {

    @Test
    void testGenerateAppendsMdExtensionWhenMissing() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("report.md");
        when(facade.createFromAi(
            eq(1L), eq(0), eq("report.md"), eq("text/markdown"), eq("# hello"),
            eq(AssetFileFormat.MARKDOWN), eq(null), eq((short) 0), any()))
                .thenReturn(saved);

        MarkdownArtifactGenerator generator = new MarkdownArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "summarise", "report", "# hello", null));

        assertThat(result.filename()).isEqualTo("report.md");
        assertThat(result.format()).isEqualTo(AssetFileFormat.MARKDOWN);
        assertThat(result.chatLinked()).isTrue();
        verify(linkService).recordAuthorship(7L, 42L);
    }

    @Test
    void testGenerateSkipsAuthorshipWhenChatIdMissing() {
        // First-turn invariant: the apply mutation can fire before the chat endpoint creates the chat row,
        // so the generator must persist the file with a null join rather than failing or fabricating a fake
        // chat id. Pin: linked=false and the link service is never called.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("orphan.md");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        MarkdownArtifactGenerator generator = new MarkdownArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, null, (short) 0, "x", "orphan", "x", null));

        assertThat(result.chatLinked()).isFalse();
        verify(linkService, never()).recordAuthorship(anyLong(), anyLong());
    }

    @Test
    void testGenerateLogsAndReturnsUnlinkedWhenAuthorshipAlreadyAssigned() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("dup.md");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);
        when(linkService.recordAuthorship(7L, 42L))
            .thenThrow(new AuthorshipAlreadyAssignedException(42L, 99L));

        MarkdownArtifactGenerator generator = new MarkdownArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "dup", "x", null));

        // The asset_file is persisted regardless; the join-failure path must not throw or roll back the write.
        assertThat(result.assetFileId()).isEqualTo(42L);
        assertThat(result.chatLinked()).isFalse();
    }

    @Test
    void testGenerateLeavesExtensionAloneWhenAlreadyPresent() {
        // A caller that explicitly asks for "notes.markdown" or "summary.md.bak" must not have ".md" double-appended.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("notes.markdown");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("notes.markdown"), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        MarkdownArtifactGenerator generator = new MarkdownArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "notes.markdown", "# hi", null));

        assertThat(result.filename()).isEqualTo("notes.markdown");
        // We pass-through the exact filename, no double extension.
        verify(facade).createFromAi(
            anyLong(), any(Integer.class), eq("notes.markdown"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testFormatIsMarkdown() {
        // Pin the format identity — the registry dispatches by this value, so a regression that returned a different
        // enum would re-route every "MARKDOWN" tool call to the wrong generator silently.
        MarkdownArtifactGenerator generator = new MarkdownArtifactGenerator(
            mock(AssetFileFacade.class), mock(AiHubChatAssetFileService.class));

        assertThat(generator.format()).isEqualTo(AssetFileFormat.MARKDOWN);
    }

    @SuppressWarnings("unused")
    private static AiHubChatAssetFile dummyJoin() {
        return mock(AiHubChatAssetFile.class);
    }
}
