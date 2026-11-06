/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.gateway.cost.OtlpCostResolver;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanStatus;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayConstraintMatchers;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.platform.ai.observability.facade.OtlpIngestResult;
import com.bytechef.ee.platform.ai.observability.facade.RejectionCode;
import com.bytechef.ee.platform.ai.observability.facade.RejectionDetail;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityTraceRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiObservabilityOtlpIngestFacadeImpl implements AiObservabilityOtlpIngestFacade {

    private static final Logger log = LoggerFactory.getLogger(AiObservabilityOtlpIngestFacadeImpl.class);

    private static final String METRIC = "bytechef_ai_otlp_spans_ingested";
    private static final String DROPPED_METRIC = "bytechef_ai_otlp_spans_dropped_non_genai";
    private static final String ORPHAN_CLEANUP_FAILED_METRIC = "bytechef_ai_otlp_orphan_cleanup_failed";

    private static final String SPAN_DEDUP_INDEX = "uq_ai_obs_span_trace_external";
    private static final String TRACE_DEDUP_INDEX = "uq_ai_obs_trace_ext_trace_id";

    private static final Pattern SPAN_DEDUP_INDEX_PATTERN =
        AiGatewayConstraintMatchers.wordBoundaryPattern(SPAN_DEDUP_INDEX);
    private static final Pattern TRACE_DEDUP_INDEX_PATTERN =
        AiGatewayConstraintMatchers.wordBoundaryPattern(TRACE_DEDUP_INDEX);

    private static final int PER_REQUEST_REJECTION_LOG_CAP = 5;

    private static final int MAX_RETRIEVAL_DOCUMENTS = 100;

    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilityTraceRepository aiObservabilityTraceRepository;
    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;
    private final OtlpCostResolver otlpCostResolver;
    private final MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AiObservabilityOtlpIngestFacadeImpl(
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceRepository aiObservabilityTraceRepository,
        AiObservabilitySpanRepository aiObservabilitySpanRepository,
        OtlpCostResolver otlpCostResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceRepository = aiObservabilityTraceRepository;
        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
        this.otlpCostResolver = otlpCostResolver;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch) {
        if (batch.droppedSpans() > 0) {
            incrementDroppedCounter(batch.droppedSpans());
        }

        List<OtelGenAiSpan> batchSpans = batch.spans();
        int batchSpanCount = batchSpans.size();

        int accepted = 0;
        int deduplicated = 0;
        int rejected = 0;
        int loggedRejections = 0;
        List<RejectionDetail> reasons = new ArrayList<>();
        List<RejectionDetail> warnings = buildMapperWarnings(batch.mapperWarnings());

        if (batch.rejectedSpans() > 0) {
            rejected += batch.rejectedSpans();

            reasons.add(RejectionDetail.withoutIndex(
                RejectionCode.REJECTED_BY_MAPPER,
                batch.rejectedSpans() + " span(s) rejected by mapper (malformed boundary input)"));

            incrementCounter("REJECTED_BY_MAPPER", batch.rejectedSpans());
        }

        for (int spanIndex = 0; spanIndex < batchSpanCount; spanIndex++) {
            OtelGenAiSpan otelSpan = batchSpans.get(spanIndex);
            String system = otelSpan.systemAttr();

            if (system == null) {
                rejected++;
                reasons.add(RejectionDetail.at(
                    spanIndex, RejectionCode.REJECTED_NO_SYSTEM,
                    otelSpan.spanIdHex() + ": missing gen_ai.system attribute"));

                incrementCounter("REJECTED_NO_SYSTEM");

                if (loggedRejections < PER_REQUEST_REJECTION_LOG_CAP) {
                    log.warn(
                        "OTLP span ingest rejected (missing gen_ai.system) workspaceId={} spanId={}",
                        workspaceId, otelSpan.spanIdHex());

                    loggedRejections++;
                }

                continue;
            }

            OtelSpanStatus rawStatus = otelSpan.status();

            if (rawStatus == null) {
                rejected++;
                reasons.add(RejectionDetail.at(
                    spanIndex, RejectionCode.REJECTED_NO_STATUS, otelSpan.spanIdHex() + ": missing span status"));

                incrementCounter("REJECTED_NO_STATUS");

                if (loggedRejections < PER_REQUEST_REJECTION_LOG_CAP) {
                    log.warn(
                        "OTLP span ingest rejected (missing status) workspaceId={} spanId={}",
                        workspaceId, otelSpan.spanIdHex());

                    loggedRejections++;
                }

                continue;
            }

            String statusTag = rawStatus.name();

            try {
                if (persistSpan(workspaceId, otelSpan, system)) {
                    accepted++;

                    incrementCounter(statusTag);
                } else {
                    deduplicated++;

                    incrementCounter("DEDUPLICATED");
                }
            } catch (RuntimeException exception) {
                rejected++;

                String summary = AiGatewayThrowables.summarize(exception);

                reasons.add(RejectionDetail.at(
                    spanIndex, RejectionCode.REJECTED_PERSIST_FAILED, otelSpan.spanIdHex() + ": " + summary));

                if (loggedRejections < PER_REQUEST_REJECTION_LOG_CAP) {
                    log.warn(
                        "OTLP span ingest rejected workspaceId={} spanId={} reason={}",
                        workspaceId, otelSpan.spanIdHex(), summary, exception);

                    loggedRejections++;
                }

                incrementCounter("REJECTED_PERSIST_FAILED");
            } catch (Error error) {
                rejected++;

                reasons.add(RejectionDetail.at(
                    spanIndex, RejectionCode.REJECTED_INTERNAL,
                    otelSpan.spanIdHex() + ": " + AiGatewayThrowables.summarize(error)));

                incrementCounter("REJECTED_INTERNAL");

                throw error;
            }
        }

        if (rejected > loggedRejections) {
            log.warn(
                "OTLP ingest emitted {} additional rejection(s) in this request beyond the log cap (cap={})",
                rejected - loggedRejections, PER_REQUEST_REJECTION_LOG_CAP);
        }

        if (deduplicated > 0) {
            log.info(
                "OTLP ingest deduplicated {}/{} span(s) for workspaceId={} (idempotent retry path)",
                deduplicated, batchSpanCount, workspaceId);
        }

        return OtlpIngestResult.forBatch(batch, accepted, deduplicated, rejected, reasons, warnings);
    }

    private static List<RejectionDetail> buildMapperWarnings(List<String> mapperWarningMessages) {
        if (mapperWarningMessages.isEmpty()) {
            return List.of();
        }

        List<RejectionDetail> result = new ArrayList<>(mapperWarningMessages.size());

        for (String warningMessage : mapperWarningMessages) {
            result.add(RejectionDetail.withoutIndex(RejectionCode.MAPPER_RESOURCE_DECODE_WARNING, warningMessage));
        }

        return result;
    }

    private boolean persistSpan(Long workspaceId, OtelGenAiSpan otelSpan, String system) {
        TraceResolution resolution = resolveOrCreateTrace(workspaceId, otelSpan);
        AiObservabilityTrace trace = resolution.trace();

        AiObservabilitySpan span = new AiObservabilitySpan(trace.getId(), resolveSpanType(otelSpan));

        span.setExternalSpanId(otelSpan.spanIdHex());
        span.setName(otelSpan.name());
        span.setProvider(system);
        span.setModel(Optional.ofNullable(otelSpan.responseModelAttr())
            .orElse(otelSpan.requestModelAttr()));
        span.setStartTime(otelSpan.startTime());
        span.setInputTokens(otelSpan.inputTokensAttr());
        span.setOutputTokens(otelSpan.outputTokensAttr());
        span.setInput(otelSpan.inputBody());
        span.setOutput(otelSpan.outputBody());

        if (span.getType() == AiObservabilitySpanType.RETRIEVAL) {
            String retrievalOutput = resolveRetrievalOutput(otelSpan);

            if (retrievalOutput != null) {
                span.setOutput(retrievalOutput);
            }
        }

        BigDecimal cost = otlpCostResolver.computeCost(
            span.getModel(), otelSpan.inputTokensAttr(), otelSpan.outputTokensAttr());

        span.setCost(cost);
        span.close(otelSpan.endTime(), toSpanStatus(otelSpan.status()));

        try {
            aiObservabilitySpanService.create(span);

            return true;
        } catch (DuplicateKeyException duplicateKeyException) {
            if (!matchesConstraint(duplicateKeyException, SPAN_DEDUP_INDEX, SPAN_DEDUP_INDEX_PATTERN)) {
                cleanupJustCreatedTrace(resolution);

                throw duplicateKeyException;
            }

            log.debug(
                "OTLP span dedup hit for traceId={} externalSpanId={} (already persisted)",
                trace.getId(), otelSpan.spanIdHex());

            return false;
        } catch (RuntimeException nonDedupFailure) {
            cleanupJustCreatedTrace(resolution);

            throw nonDedupFailure;
        }
    }

    private AiObservabilitySpanType resolveSpanType(OtelGenAiSpan otelSpan) {
        String kind = otelSpan.spanKindAttr();

        if (kind != null && kind.equalsIgnoreCase("RETRIEVER")) {
            return AiObservabilitySpanType.RETRIEVAL;
        }

        return AiObservabilitySpanType.GENERATION;
    }

    private String resolveRetrievalOutput(OtelGenAiSpan otelSpan) {
        List<String> documents = new ArrayList<>();

        for (int index = 0; index < MAX_RETRIEVAL_DOCUMENTS; index++) {
            String content = otelSpan.attributes()
                .get("retrieval.documents." + index + ".document.content") instanceof CharSequence charSequence
                    ? charSequence.toString()
                    : null;

            if (content == null) {
                break;
            }

            documents.add(content);
        }

        if (documents.isEmpty()) {
            return null;
        }

        return String.join("\n\n", documents);
    }

    private void cleanupJustCreatedTrace(TraceResolution resolution) {
        if (!resolution.justCreated()) {
            return;
        }

        Long orphanId = resolution.trace()
            .getId();

        boolean adopted;

        try {
            adopted = aiObservabilitySpanRepository.existsByTraceId(orphanId);
        } catch (RuntimeException existsCheckFailure) {
            log.error(
                "Adoption check (existsByTraceId) failed for orphan trace {} during cleanup; skipping deletion to " +
                    "avoid removing a possibly-adopted trace. The trace will be reaped by the deleteOlderThan " +
                    "retention cron — investigate the DB-side cause (connection pool exhaustion, row lock).",
                orphanId, existsCheckFailure);

            incrementOrphanCleanupFailedCounter();

            return;
        }

        if (adopted) {
            log.debug(
                "Skipping orphan-trace cleanup for {} — trace was concurrently adopted by another span writer " +
                    "between our create-commit and span-insert failure",
                orphanId);

            return;
        }

        try {
            aiObservabilityTraceRepository.deleteById(orphanId);

            log.debug("Cleaned up orphan trace {} after span insert failure", orphanId);
        } catch (RuntimeException cleanupFailure) {
            log.error(
                "Failed to clean up orphan trace {} after span insert failure; " +
                    "the trace will be removed by the deleteOlderThan retention cron — investigate the DB-side " +
                    "cause (connection pool exhaustion, row lock) before it cascades into other writes",
                orphanId, cleanupFailure);

            incrementOrphanCleanupFailedCounter();
        }
    }

    private static boolean matchesConstraint(
        DuplicateKeyException exception, String constraintName, Pattern wordBoundaryPattern) {

        return AiGatewayConstraintMatchers.matchesConstraint(exception, constraintName, wordBoundaryPattern);
    }

    private TraceResolution resolveOrCreateTrace(Long workspaceId, OtelGenAiSpan otelSpan) {
        Optional<AiObservabilityTrace> existing =
            workspaceAiObservabilityTraceService.findByExternalTraceId(workspaceId, otelSpan.traceIdHex());

        if (existing.isPresent()) {
            return new TraceResolution(existing.get(), false);
        }

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        trace.setExternalTraceId(otelSpan.traceIdHex());
        trace.setName(otelSpan.name());

        try {
            workspaceAiObservabilityTraceService.createInWorkspace(trace, workspaceId);

            return new TraceResolution(trace, true);
        } catch (DuplicateKeyException duplicateKeyException) {
            if (!matchesConstraint(duplicateKeyException, TRACE_DEDUP_INDEX, TRACE_DEDUP_INDEX_PATTERN)) {
                throw duplicateKeyException;
            }

            AiObservabilityTrace winner =
                workspaceAiObservabilityTraceService.findByExternalTraceId(workspaceId, otelSpan.traceIdHex())
                    .orElseThrow(() -> new IllegalStateException(
                        "Trace insert rejected by unique constraint on (workspace_id, external_trace_id) " +
                            "but re-fetch returned empty",
                        duplicateKeyException));

            return new TraceResolution(winner, false);
        }
    }

    private record TraceResolution(AiObservabilityTrace trace, boolean justCreated) {
    }

    private AiObservabilitySpanStatus toSpanStatus(OtelSpanStatus status) {
        return switch (status) {
            case OK, UNSET -> AiObservabilitySpanStatus.COMPLETED;
            case ERROR -> AiObservabilitySpanStatus.ERROR;
        };
    }

    private void incrementCounter(String statusTag) {
        incrementCounter(statusTag, 1);
    }

    private void incrementCounter(String statusTag, int amount) {
        if (meterRegistry == null || amount <= 0) {
            return;
        }

        try {
            Counter.builder(METRIC)
                .tag("status", statusTag)
                .register(meterRegistry)
                .increment(amount);
        } catch (RuntimeException meterFailure) {
            log.debug("Counter increment failed (status={}, amount={})", statusTag, amount, meterFailure);
        }
    }

    private void incrementDroppedCounter(int count) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(DROPPED_METRIC)
                .register(meterRegistry)
                .increment(count);
        } catch (RuntimeException meterFailure) {
            log.debug("Dropped-counter increment failed (count={})", count, meterFailure);
        }
    }

    private void incrementOrphanCleanupFailedCounter() {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(ORPHAN_CLEANUP_FAILED_METRIC)
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug("Orphan-cleanup-failed counter increment failed", meterFailure);
        }
    }
}
