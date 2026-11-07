/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
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
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiToolUsageRecorderImpl implements ToolUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiToolUsageRecorderImpl.class);

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
