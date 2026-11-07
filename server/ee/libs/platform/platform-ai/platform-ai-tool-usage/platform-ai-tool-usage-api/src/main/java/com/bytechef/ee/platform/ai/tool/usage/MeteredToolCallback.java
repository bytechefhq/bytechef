/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Metrics;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MeteredToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(MeteredToolCallback.class);

    private static final AtomicBoolean CONTEXT_UNAVAILABLE_LOGGED = new AtomicBoolean(false);

    private final ToolCallback delegate;
    private final String toolName;
    private final int unitCount;
    private final ToolUsageRecorder usageRecorder;
    private final Function<ToolContext, ToolUsageContext> contextResolver;
    private final Function<JsonNode, Map<String, Object>> metadataExtractor;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MeteredToolCallback(
        ToolCallback delegate, String toolName, int unitCount, ToolUsageRecorder usageRecorder,
        Function<ToolContext, ToolUsageContext> contextResolver,
        Function<JsonNode, Map<String, Object>> metadataExtractor, JsonMapper jsonMapper) {

        this.delegate = delegate;
        this.toolName = toolName;
        this.unitCount = unitCount;
        this.usageRecorder = usageRecorder;
        this.contextResolver = contextResolver;
        this.metadataExtractor = metadataExtractor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        long startNanos = System.nanoTime();

        String response = delegate.call(toolInput, toolContext);

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;

        recordSafely(toolInput, response, durationMs, toolContext);

        return response;
    }

    private void recordSafely(String toolInput, String response, long durationMs, @Nullable ToolContext toolContext) {
        ToolUsageContext context;
        Map<String, Object> metadata;

        try {
            if (isErrorResponse(response)) {
                return;
            }

            context = contextResolver.apply(toolContext);

            if (context == null) {
                Metrics.counter("bytechef.usage.context_unavailable_total", "toolName", toolName)
                    .increment();

                if (CONTEXT_UNAVAILABLE_LOGGED.compareAndSet(false, true)) {
                    log.warn(
                        "Skipping tool usage record: contextResolver returned null (toolName={}). This message logs " +
                            "once per JVM; the bytechef.usage.context_unavailable_total counter tracks ongoing rate.",
                        toolName);
                }

                return;
            }

            metadata = extractMetadata(toolInput);
        } catch (RuntimeException exception) {
            Metrics.counter("bytechef.usage.recording.failures", "reason", "tool_prep_failure")
                .increment();

            log.warn(
                "Failed to prepare tool usage record for tool={}: {}", toolName, exception.getMessage(), exception);

            return;
        }

        usageRecorder.recordTool(context, toolName, unitCount, durationMs, metadata);
    }

    private boolean isErrorResponse(String response) {
        if (response == null) {
            return true;
        }

        if (response.isBlank()) {
            Metrics.counter("bytechef.usage.tool_response_blank_total", "toolName", toolName)
                .increment();

            return false;
        }

        try {
            JsonNode root = jsonMapper.readTree(response);

            if (root == null || !root.isObject()) {
                return false;
            }

            JsonNode errorNode = root.get("error");

            return errorNode != null && !errorNode.isNull();
        } catch (JacksonException exception) {
            Metrics.counter("tool_response_unparseable_total", "toolName", toolName)
                .increment();

            log.warn(
                "Failed to parse tool response as JSON for tool={} — treating as error and skipping billing row: {}",
                toolName, exception.getMessage());

            return true;
        }
    }

    private Map<String, Object> extractMetadata(String toolInput) {
        if (metadataExtractor == null || toolInput == null || toolInput.isBlank()) {
            return null;
        }

        try {
            JsonNode root = jsonMapper.readTree(toolInput);

            Map<String, Object> result = metadataExtractor.apply(root);

            return result == null || result.isEmpty() ? null : result;
        } catch (JacksonException exception) {
            Metrics.counter("bytechef.usage.metadata_extraction_failures", "toolName", toolName)
                .increment();

            log.warn(
                "Failed to extract metadata for tool={} — recording row without metadata: {}",
                toolName, exception.getMessage());

            return null;
        }
    }

    public static Function<JsonNode, Map<String, Object>> singleStringField(String fieldName) {
        return root -> {
            JsonNode node = root.get(fieldName);

            if (node == null || node.isNull() || !node.isTextual()) {
                return Map.of();
            }

            Map<String, Object> result = new LinkedHashMap<>();

            result.put(fieldName, node.asText());

            return result;
        };
    }
}
