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
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ChartArtifactGeneratorTest {

    @Test
    void testGenerateAcceptsValidBarChart() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("revenue.chart.json");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), eq("revenue.chart.json"), eq("application/json"), any(),
            eq(AssetFileFormat.CHART), any(), any(), any()))
                .thenReturn(saved);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec = """
            {
              "type": "bar",
              "title": "Q4 Revenue by Region",
              "xKey": "region",
              "yKeys": ["revenue", "cost"],
              "data": [
                {"region": "NA", "revenue": 1200, "cost": 800},
                {"region": "EU", "revenue": 900,  "cost": 600}
              ]
            }""";

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "revenue", spec, null));

        assertThat(result.format()).isEqualTo(AssetFileFormat.CHART);
        assertThat(result.filename()).isEqualTo("revenue.chart.json");
    }

    @Test
    void testGenerateDuplicatesPayloadIntoMetadataJson() {
        // Pin: the chart generator must duplicate the spec into metadata_json so the viewer can render without
        // re-fetching the file content. Without this, the Recharts mode in the viewer would have to issue a
        // separate download request just to read the spec — and the metadata_json column would be perpetually null
        // for chart rows, which the audit page would surface as a "missing metadata" warning.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("c.chart.json");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(),
            eq(AssetFileFormat.CHART), any(String.class), any(), any()))
                .thenReturn(saved);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec =
            "{\"type\":\"line\",\"xKey\":\"day\",\"yKeys\":[\"v\"],\"data\":[{\"day\":\"Mon\",\"v\":1}]}";

        generator.generate(new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "c.chart.json", spec, null));

        verify(facade).createFromAi(
            anyLong(), any(Integer.class), any(), any(), eq(spec),
            eq(AssetFileFormat.CHART), eq(spec), any(), any());
    }

    @Test
    void testGenerateAcceptsValidPieChart() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        AssetFile saved = mock(AssetFile.class);

        when(saved.getId()).thenReturn(42L);
        when(saved.getName()).thenReturn("share.chart.json");
        when(facade.createFromAi(
            anyLong(), any(Integer.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec = """
            {
              "type": "pie",
              "title": "Market share",
              "labelKey": "segment",
              "valueKey": "share",
              "data": [{"segment":"NA","share":42}, {"segment":"EU","share":31}]
            }""";

        GenerationResult result = generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "share", spec, null));

        assertThat(result.assetFileId()).isEqualTo(42L);
    }

    @Test
    void testValidationRejectsUnknownChartType() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "x.chart.json",
                "{\"type\":\"scatter\",\"xKey\":\"x\",\"yKeys\":[\"y\"],\"data\":[{\"x\":1,\"y\":2}]}", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Chart type must be one of");
    }

    @Test
    void testValidationRejectsRowMissingXKey() {
        // Pin the data-shape check: a row that drops the xKey would Recharts-render as a gap which the user reads
        // as "where did my data point go?" — pinning the rejection at the generator turns that into an LLM-
        // recoverable error rather than silent visual data loss.
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec = "{\"type\":\"bar\",\"xKey\":\"region\",\"yKeys\":[\"revenue\"],"
            + "\"data\":[{\"region\":\"NA\",\"revenue\":1200},{\"revenue\":900}]}";

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "x.chart.json", spec, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data[1] is missing the xKey 'region'");
    }

    @Test
    void testValidationRejectsRowMissingYKey() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec = "{\"type\":\"bar\",\"xKey\":\"region\",\"yKeys\":[\"revenue\",\"cost\"],"
            + "\"data\":[{\"region\":\"NA\",\"revenue\":1200,\"cost\":800},"
            + "{\"region\":\"EU\",\"revenue\":900}]}";

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "x.chart.json", spec, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data[1] is missing yKey 'cost'");
    }

    @Test
    void testValidationRejectsEmptyData() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "x.chart.json",
                "{\"type\":\"bar\",\"xKey\":\"x\",\"yKeys\":[\"y\"],\"data\":[]}", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-empty 'data' array");
    }

    @Test
    void testValidationRejectsPieRowMissingValueKey() {
        AssetFileFacade facade = mock(AssetFileFacade.class);
        AiHubTaskAssetFileService linkService = mock(AiHubTaskAssetFileService.class);

        ChartArtifactGenerator generator = new ChartArtifactGenerator(facade, linkService);

        String spec = "{\"type\":\"pie\",\"labelKey\":\"segment\",\"valueKey\":\"share\","
            + "\"data\":[{\"segment\":\"NA\",\"share\":42},{\"segment\":\"EU\"}]}";

        assertThatThrownBy(() -> generator.generate(
            new GenerationRequest(1L, 10L, 0, 7L, (short) 0, "x", "x.chart.json", spec, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pie data[1] is missing the valueKey 'share'");
    }
}
