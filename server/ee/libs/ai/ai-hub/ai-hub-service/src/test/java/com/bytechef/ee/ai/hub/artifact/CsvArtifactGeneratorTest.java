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
import static org.mockito.Mockito.verify;
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
class CsvArtifactGeneratorTest {

    @Test
    void testGenerateAcceptsConsistentRows() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("data.csv");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("data.csv"), eq("text/csv"), any(),
            eq(AssetFileFormat.CSV), any(), any(), any()))
                .thenReturn(saved);

        CsvArtifactGenerator generator = new CsvArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "data", "name,age\nalice,30\nbob,25", null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.CSV);
        assertThat(result.filename()).isEqualTo("data.csv");
    }

    @Test
    void testValidationRejectsMismatchedColumnCount() {
        // Probe-oracle for LLM truncation: a CSV where the model dropped a value mid-row would otherwise land in
        // the user's workspace and surface as an import error days later. Pin the row-count check so a regression
        // that loosens it surfaces here.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        CsvArtifactGenerator generator = new CsvArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(
                1L, 10L, 0, 7L, (short) 0, "x", "data.csv", "name,age,city\nalice,30\nbob,25,nyc", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CSV row 2 has 2 columns; expected 3");
    }

    @Test
    void testValidationAllowsQuotedCommas() {
        // RFC 4180: a quoted field with embedded commas counts as one column. Without the quote-aware parser, a
        // legitimate "Doe, John" cell would be miscounted as two columns and falsely rejected. This test pins that
        // the validator does not regress to a naive split.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("contacts.csv");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(),
            eq(AssetFileFormat.CSV), any(), any(), any()))
                .thenReturn(saved);

        CsvArtifactGenerator generator = new CsvArtifactGenerator(facade, linkService);

        GenerationResult result = generator.generate(
            new GenerationRequest(
                1L, 10L, 0, 7L, (short) 0, "x", "contacts.csv",
                "name,note\n\"Doe, John\",vip\n\"Smith, Jane\",new", null));

        assertThat(result.assetFileId()).isEqualTo(42L);
    }

    @Test
    void testValidationAllowsEmbeddedNewlinesInQuotedField() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("notes.csv");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(),
            any(), any(), any(), any()))
                .thenReturn(saved);

        CsvArtifactGenerator generator = new CsvArtifactGenerator(facade, linkService);

        // Two rows, both 2 columns. The newline inside the quoted "line1\nline2" cell must NOT split the row.
        GenerationResult result = generator.generate(
            new GenerationRequest(
                1L, 10L, 0, 7L, (short) 0, "x", "notes.csv",
                "id,body\n1,\"line1\nline2\"\n2,plain", null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.CSV);

        verify(facade).createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(),
            eq(AssetFileFormat.CSV), any(), any(), any());
    }

    @Test
    void testValidationRejectsEmptyContent() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubChatAssetFileService linkService = mock(AiHubChatAssetFileService.class);

        CsvArtifactGenerator generator = new CsvArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "empty.csv", "", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV content is empty");
    }
}
