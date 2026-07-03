/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTaskAssetFileService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Persists a chart specification — not a rendered image. The spec is the artifact: it lives in the asset_file content
 * (so the user can download it as JSON for portability), is duplicated into {@code metadata_json} for fast viewer
 * access, and is rendered client-side via Recharts (already a dep). This avoids pulling JFreeChart or a headless
 * renderer into the server image, and means the chart re-renders crisply at any size without a round-trip to a raster.
 *
 * <p>
 * Supported shapes (all driven from the {@code type} field):
 * </p>
 *
 * <pre>{@code { "type": "bar" | "line" | "area", "title": "Q4 Revenue by Region", "xKey": "region", "yKeys":
 * ["revenue", "cost"], "data": [ {"region": "NA", "revenue": 1200, "cost": 800}, {"region": "EU", "revenue": 900,
 * "cost": 600} ] }
 *
 * { "type": "pie", "title": "Market share", "labelKey": "segment", "valueKey": "share", "data":
 * [{"segment":"NA","share":42}, {"segment":"EU","share":31}] } }</pre>
 *
 * <p>
 * The validator checks: type is one of the supported values; data is non-empty; for bar/line/area, every row carries
 * the xKey AND every yKey; for pie, every row carries the labelKey AND valueKey. A row missing a key would render as a
 * gap in Recharts which the user would see as "what happened to my data?" — failing fast at generation time turns that
 * into an LLM-recoverable error instead.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ChartArtifactGenerator extends AbstractTextArtifactGenerator {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private static final Set<String> SUPPORTED_TYPES = Set.of("bar", "line", "area", "pie");

    private static final TypeReference<Map<String, Object>> CHART_SPEC_TYPE = new TypeReference<>() {};

    public ChartArtifactGenerator(
        AssetFileFacade assetFileFacade, AiHubTaskAssetFileService taskAssetFileService) {

        super(assetFileFacade, taskAssetFileService);
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.CHART;
    }

    @Override
    protected String mimeType(GenerationRequest request) {
        return "application/json";
    }

    @Override
    protected String defaultExtension() {
        return "chart.json";
    }

    @Override
    protected String resolveMetadataJson(GenerationRequest request) {
        // The spec IS the chart — duplicate into metadata_json so the viewer can read it without re-fetching the
        // file content. A caller-supplied request.metadataJson() override (rare for chart) would lose the chart
        // shape, so we explicitly prefer the payload here.
        return request.payload();
    }

    @Override
    protected void validate(GenerationRequest request) {
        Map<String, Object> spec;

        try {
            spec = JSON_MAPPER.readValue(request.payload(), CHART_SPEC_TYPE);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                "Chart spec is not valid JSON: " + exception.getMessage(), exception);
        }

        if (spec == null) {
            throw new IllegalArgumentException("Chart spec is empty");
        }

        Object typeValue = spec.get("type");

        if (!(typeValue instanceof String type) || !SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                "Chart type must be one of " + SUPPORTED_TYPES + "; got: " + typeValue);
        }

        Object dataValue = spec.get("data");

        if (!(dataValue instanceof List<?> dataList) || dataList.isEmpty()) {
            throw new IllegalArgumentException("Chart spec must include a non-empty 'data' array");
        }

        if ("pie".equals(type)) {
            validatePieRows(spec, dataList);
        } else {
            validateAxisRows(type, spec, dataList);
        }
    }

    /**
     * For bar/line/area: every row must carry the {@code xKey} field and each {@code yKey}. A missing key would
     * Recharts-render as a gap rather than a hard error, which the user sees as "where did my July number go?" —
     * failing here turns that into an LLM-recoverable validation error.
     */
    private static void validateAxisRows(String type, Map<String, Object> spec, List<?> dataList) {
        Object xKeyValue = spec.get("xKey");

        if (!(xKeyValue instanceof String xKey) || xKey.isBlank()) {
            throw new IllegalArgumentException(
                "Chart spec for type '" + type + "' must include a non-blank 'xKey'");
        }

        Object yKeysValue = spec.get("yKeys");

        if (!(yKeysValue instanceof List<?> yKeysList) || yKeysList.isEmpty()) {
            throw new IllegalArgumentException(
                "Chart spec for type '" + type + "' must include a non-empty 'yKeys' array");
        }

        for (int i = 0; i < dataList.size(); i++) {
            Object row = dataList.get(i);

            if (!(row instanceof Map<?, ?> rowMap)) {
                throw new IllegalArgumentException(
                    "Chart data[" + i + "] must be an object; got: " + (row == null ? "null" : row.getClass()
                        .getSimpleName()));
            }

            if (!rowMap.containsKey(xKey)) {
                throw new IllegalArgumentException(
                    "Chart data[" + i + "] is missing the xKey '" + xKey + "'");
            }

            for (Object yKeyEntry : yKeysList) {
                if (!(yKeyEntry instanceof String yKey) || yKey.isBlank()) {
                    throw new IllegalArgumentException(
                        "Chart yKeys must all be non-blank strings; got: " + yKeyEntry);
                }

                if (!rowMap.containsKey(yKey)) {
                    throw new IllegalArgumentException(
                        "Chart data[" + i + "] is missing yKey '" + yKey + "'");
                }
            }
        }
    }

    private static void validatePieRows(Map<String, Object> spec, List<?> dataList) {
        Object labelKeyValue = spec.get("labelKey");
        Object valueKeyValue = spec.get("valueKey");

        if (!(labelKeyValue instanceof String labelKey) || labelKey.isBlank()) {
            throw new IllegalArgumentException("Pie chart spec must include a non-blank 'labelKey'");
        }

        if (!(valueKeyValue instanceof String valueKey) || valueKey.isBlank()) {
            throw new IllegalArgumentException("Pie chart spec must include a non-blank 'valueKey'");
        }

        for (int i = 0; i < dataList.size(); i++) {
            Object row = dataList.get(i);

            if (!(row instanceof Map<?, ?> rowMap)) {
                throw new IllegalArgumentException(
                    "Pie data[" + i + "] must be an object; got: "
                        + (row == null ? "null" : row.getClass()
                            .getSimpleName()));
            }

            if (!rowMap.containsKey(labelKey)) {
                throw new IllegalArgumentException(
                    "Pie data[" + i + "] is missing the labelKey '" + labelKey + "'");
            }

            if (!rowMap.containsKey(valueKey)) {
                throw new IllegalArgumentException(
                    "Pie data[" + i + "] is missing the valueKey '" + valueKey + "'");
            }
        }
    }
}
