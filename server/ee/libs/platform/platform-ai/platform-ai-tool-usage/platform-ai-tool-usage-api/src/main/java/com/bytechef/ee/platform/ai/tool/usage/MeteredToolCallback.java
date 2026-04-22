/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * {@link ToolCallback} decorator that delegates to a wrapped callback and, on a successful (non-error) JSON response,
 * writes one row to {@link ToolUsageRecorder}. The unit count is fixed per decorator instance (constructor-injected);
 * the decorator extracts a metadata map from the parsed tool input via a pluggable function so per-tool record details
 * (e.g. captured query string) stay close to the tool itself.
 *
 * <p>
 * Two pluggable resolvers keep this class free of any agent-platform–specific types:
 * </p>
 *
 * <ul>
 * <li>{@code contextResolver} — translates the Spring AI {@link ToolContext} into the workspace/user/owner identifiers
 * the recorder needs. Caller supplies the mapping; if it returns {@code null} the call is skipped (and a per-tool
 * counter is incremented) so a missing context does not silently produce orphaned rows.</li>
 * <li>{@code metadataExtractor} — maps the parsed tool input JSON to a free-form metadata map. {@code null} or empty
 * results are persisted as no metadata.</li>
 * </ul>
 *
 * <p>
 * Recording is best-effort: any failure inside the recorder is logged at WARN; the tool's response is returned to the
 * caller unchanged.
 * </p>
 *
 * @author Ivica Cardic
 */
public class MeteredToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(MeteredToolCallback.class);

    /**
     * Latches once per JVM the first time {@link #recordSafely} skips a tool call because the caller-supplied
     * {@code contextResolver} returned {@code null}. Pairs with the {@code bytechef.usage.context_unavailable_total}
     * counter so a non-zero rate alerts in metrics while the WARN-once keeps log volume bounded.
     */
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
        // Don't wrap usageRecorder.recordTool — implementations are expected to have their own catch + counter.
        // Wrapping here would absorb the throw before any downstream counter ever increments, so we only guard the
        // prep work that does NOT have its own failure counter.
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
            // Fire the unified failures counter so prep-stage drops are visible on the same dashboard panel as
            // DB-stage drops in the recorder implementation.
            Metrics.counter("bytechef.usage.recording.failures", "reason", "tool_prep_failure")
                .increment();

            log.warn(
                "Failed to prepare tool usage record for tool={}: {}", toolName, exception.getMessage(), exception);

            return;
        }

        usageRecorder.recordTool(context, toolName, unitCount, durationMs, metadata);
    }

    /**
     * True when the wrapped tool's JSON response carries a top-level {@code error} field. Top-level only — keeps benign
     * substrings like {@code "errorCount":0} from skewing billing. Unparseable responses are treated as errors (skip
     * billing) so a regression that breaks tool JSON output doesn't silently charge for failed calls;
     * {@code tool_response_unparseable_total} alerts ops to fix the regression.
     *
     * <p>
     * Blank responses are NOT treated as errors. A tool legitimately returning an empty string (e.g. "no rows matched",
     * "no items found") is indistinguishable from a crash if both are routed through the skip-billing arm — the agent
     * sees a "no result" response and the workspace gets a free call. Instead, blank responses increment
     * {@code bytechef.usage.tool_response_blank_total} so a sustained rate is observable but the call is still billed
     * under its tool name. If a tool genuinely fails it should return a typed {@code {"error": ...}} payload — the
     * typed error path is what the LLM and the cost dashboards both rely on.
     * </p>
     */
    private boolean isErrorResponse(String response) {
        if (response == null) {
            return true;
        }

        if (response.isBlank()) {
            // Observable but billed: a blank-but-not-null response is a tool's affirmative "I have nothing to
            // report" rather than a failure. Pair with the counter so dashboards catch tools that drift toward
            // returning blanks instead of typed empty structures (e.g. "[]" or {"items": []}).
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
            // Log at WARN so silently-broken metadata extraction surfaces in production logs instead of just
            // producing usage rows with metadata=null indistinguishable from rows that genuinely have no metadata.
            // Pair the WARN with a counter so a sustained rate is observable in metrics in addition to logs.
            Metrics.counter("bytechef.usage.metadata_extraction_failures", "toolName", toolName)
                .increment();

            log.warn(
                "Failed to extract metadata for tool={} — recording row without metadata: {}",
                toolName, exception.getMessage());

            return null;
        }
    }

    /**
     * Convenience metadata extractor that copies a single string property from the input JSON when present.
     */
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
