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

import com.bytechef.ee.platform.ai.tool.usage.repository.AiToolUsageRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default {@link ToolUsageRecorder}. Persists each tool invocation as one row via Spring Data JDBC. Recording failures
 * are logged at ERROR and swallowed so a bookkeeping problem cannot break the end-user request. The DB write runs in a
 * {@link Propagation#REQUIRES_NEW} nested transaction so a failed insert rolls back only the usage row, never marking
 * the parent transaction rollback-only. The self-proxy resolved via {@link ObjectProvider} is required for that
 * propagation to take effect — a plain {@code this.save*()} call would bypass the AOP proxy.
 *
 * <p>
 * Lives in the platform shared module so any agent surface (AI Hub, future AI agent runtime, ad-hoc tool runs) can
 * reuse the same row shape and bookkeeping path. Cost estimation is pluggable via {@link ToolCostEstimator} —
 * deployments without a configured estimator get {@link BigDecimal#ZERO} costs (still useful for tracking call counts)
 * and emit a warning once.
 * </p>
 *
 * @author Ivica Cardic
 */
public class AiToolUsageRecorderImpl implements ToolUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiToolUsageRecorderImpl.class);

    /**
     * Tag values for the {@code bytechef.usage.recording.failures} counter. Without per-reason tags ops sees a single
     * number and cannot tell whether rows are being lost to upstream context-propagation bugs, downstream DB outages,
     * or REQUIRES_NEW proxy misconfiguration.
     */
    private static final String FAILURE_REASON_DB_FAILURE = "db_failure";
    private static final String FAILURE_REASON_PROXY_UNAVAILABLE = "proxy_unavailable";

    private final AiToolUsageRepository repository;
    private final ToolCostEstimator costEstimator;
    private final Clock clock;
    private final JsonMapper jsonMapper;
    private final @Nullable MeterRegistry meterRegistry;
    private final Map<String, Counter> recordingFailureCounters = new ConcurrentHashMap<>();
    private final @Nullable Counter selfProxyUnavailableCounter;
    private final AtomicBoolean selfProxyUnavailableLogged = new AtomicBoolean(false);
    private final ObjectProvider<AiToolUsageRecorderImpl> selfProvider;

    @SuppressFBWarnings({
        "EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"
    })
    public AiToolUsageRecorderImpl(
        AiToolUsageRepository repository, ToolCostEstimator costEstimator, JsonMapper jsonMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider, ObjectProvider<AiToolUsageRecorderImpl> selfProvider) {

        this.repository = repository;
        this.costEstimator = costEstimator;
        this.clock = Clock.systemUTC();
        this.jsonMapper = jsonMapper;
        this.selfProvider = selfProvider;

        this.meterRegistry = meterRegistryProvider.getIfAvailable();

        this.selfProxyUnavailableCounter = meterRegistry == null ? null : Counter
            .builder("bytechef.usage.recorder.proxy_unavailable_total")
            .description(
                "Number of times AiToolUsageRecorderImpl.self() fell back to `this` because the Spring proxy was "
                    + "not resolvable. Non-zero values in production mean @Transactional(REQUIRES_NEW) is silently "
                    + "disabled and a recording failure can roll the parent transaction back.")
            .register(meterRegistry);
    }

    @Override
    public void recordTool(
        ToolUsageContext context, String toolName, int unitCount, long durationMs,
        @Nullable Map<String, Object> metadata) {

        try {
            self().saveToolUsage(context, toolName, unitCount, durationMs, metadata);
        } catch (RuntimeException exception) {
            incrementRecordingFailureCounter(FAILURE_REASON_DB_FAILURE);

            log.error(
                "Failed to record tool usage for workspace={}, user={}, tool={}: {}",
                context.workspaceId(), context.userId(), toolName, exception.getMessage(), exception);
        }
    }

    /**
     * Persists one tool usage row in its own transaction so a billing entry survives even when the caller's transaction
     * later rolls back. Must remain {@code public} and must be invoked through {@link #self()} so the proxy applies —
     * see the class-level Javadoc.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveToolUsage(
        ToolUsageContext context, String toolName, int unitCount, long durationMs,
        @Nullable Map<String, Object> metadata) {

        int safeUnitCount = Math.max(unitCount, 0);
        BigDecimal cost = costEstimator.estimateToolCost(toolName, safeUnitCount);

        AiToolUsage usage = new AiToolUsage();

        usage.setWorkspaceId(context.workspaceId());
        usage.setUserId(context.userId());
        usage.setOwnerId(context.ownerId());
        usage.setToolName(toolName != null ? toolName : "");
        usage.setUnitCount(safeUnitCount);
        usage.setEstimatedCostUsd(cost);
        usage.setDurationMs((int) Math.min(Math.max(durationMs, 0), Integer.MAX_VALUE));
        usage.setMetadataJson(serializeMetadata(metadata));
        usage.setEnvironment(context.environment());
        usage.setCreatedAt(LocalDateTime.now(clock));

        repository.save(usage);
    }

    private AiToolUsageRecorderImpl self() {
        AiToolUsageRecorderImpl resolved = selfProvider.getIfAvailable();

        if (resolved == null || !AopUtils.isAopProxy(resolved)) {
            if (selfProxyUnavailableCounter != null) {
                selfProxyUnavailableCounter.increment();
            }

            incrementRecordingFailureCounter(FAILURE_REASON_PROXY_UNAVAILABLE);

            if (selfProxyUnavailableLogged.compareAndSet(false, true)) {
                log.warn(
                    "Self-proxy for AiToolUsageRecorderImpl is unavailable (resolved={}); falling back to `this`. "
                        + "In production this means @Transactional(REQUIRES_NEW) is NOT being applied to "
                        + "saveToolUsage and a recording failure could roll the parent transaction back. Verify "
                        + "proxyTargetClass=true and that the bean is exposed through AOP. Subsequent occurrences "
                        + "are tracked by the bytechef.usage.recorder.proxy_unavailable_total counter.",
                    resolved == null ? "null" : resolved.getClass()
                        .getName());
            }

            return this;
        }

        return resolved;
    }

    private void incrementRecordingFailureCounter(String reason) {
        if (meterRegistry == null) {
            return;
        }

        Counter counter = recordingFailureCounters.computeIfAbsent(
            reason,
            tag -> Counter.builder("bytechef.usage.recording.failures")
                .description(
                    "Number of AI tool usage rows that failed to persist. Non-zero values indicate missing "
                        + "billing data — the user request still succeeded, but the bookkeeping row was lost. "
                        + "Tagged by reason to distinguish DB outages from proxy misconfiguration.")
                .tag("reason", tag)
                .register(meterRegistry));

        counter.increment();
    }

    /**
     * Serialises {@code metadata} to JSON, or returns a sentinel JSON object that flags the failure when Jackson blows
     * up. Returning plain {@code null} would have made the persisted row indistinguishable from "no metadata was
     * provided" and the WARN log would be the only signal — the sentinel preserves the row's recoverability while a
     * tagged counter ({@code reason=metadata_serialization}) lets ops alert on rate.
     */
    private String serializeMetadata(@Nullable Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return jsonMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            log.warn("Failed to serialise tool usage metadata: {}", exception.getMessage());

            incrementRecordingFailureCounter("metadata_serialization");

            return "{\"_serialization_error\":\"" + exception.getClass()
                .getSimpleName() + "\"}";
        }
    }
}
