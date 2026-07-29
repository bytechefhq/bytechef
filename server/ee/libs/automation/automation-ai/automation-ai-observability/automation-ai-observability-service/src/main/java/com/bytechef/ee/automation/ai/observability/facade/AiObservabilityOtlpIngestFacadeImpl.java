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
 * Converts {@link OtelSpanBatch} into persisted {@link AiObservabilityTrace} / {@link AiObservabilitySpan} rows. Span
 * deduplication on {@code (traceId, externalSpanId)} keeps retried OTLP batches from double-counting tokens / costs.
 *
 * <p>
 * {@code ingest} is intentionally NOT {@code @Transactional} so a single poison-pill span does not roll back peers —
 * the partial-success contract advertised by {@link OtlpIngestResult}. Each service-level {@code create} call enters
 * its own transaction.
 *
 * <p>
 * <b>Orphan-trace handling.</b> If trace creation succeeds and span insert then fails (non-dedup), we explicitly clean
 * up the just-created trace via {@link AiObservabilityTraceRepository#deleteById(Object)} so dashboards do not show
 * "ghost" traces with zero observable activity. If the cleanup itself fails (transient DB outage), the orphan is picked
 * up by the existing {@code deleteOlderThan} cron — the trace becomes invisible to user-facing queries within the
 * configured retention window. Wrapping trace+span in a single outer {@code TransactionTemplate} was rejected: the
 * inner service {@code @Transactional} methods join the outer transaction (PROPAGATION_REQUIRED default), so a
 * {@link DuplicateKeyException} from the inner method marks the outer transaction rollback-only; on commit Spring
 * raises {@code UnexpectedRollbackException} instead of letting our dedup catch return successfully — which would
 * mis-classify legitimate dedup retries as REJECTED_PERSIST_FAILED.
 *
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

    // Names of the unique indexes that legitimately produce DuplicateKeyException on the idempotency path.
    // SPAN_DEDUP_INDEX is a partial unique index (scoped via WHERE external_span_id IS NOT NULL); see
    // 00000000000012_ai_observability_span_add_external_span_id.xml. TRACE_DEDUP_INDEX is a plain (non-partial)
    // unique index on ai_observability_trace (workspace_id, external_trace_id); the uq_ prefix follows the
    // standard naming convention so operators triaging constraint-violation incidents can locate it via the
    // uq_ / ux_ prefix scan. Any DuplicateKeyException whose message references a different index/constraint
    // is a real schema violation (e.g., a future NOT-NULL or FK addition) and must propagate as
    // REJECTED_PERSIST_FAILED rather than being silently bucketed as DEDUPLICATED.
    private static final String SPAN_DEDUP_INDEX = "uq_ai_obs_span_trace_external";
    private static final String TRACE_DEDUP_INDEX = "uq_ai_obs_trace_ext_trace_id";

    // Word-boundary patterns for the substring fallback. Plain String#contains would silently match a future
    // index named "uq_ai_obs_span_trace_externalv2" against the original constant, mis-classifying a real
    // schema violation as DEDUPLICATED. \b anchors prevent that.
    private static final Pattern SPAN_DEDUP_INDEX_PATTERN =
        AiGatewayConstraintMatchers.wordBoundaryPattern(SPAN_DEDUP_INDEX);
    private static final Pattern TRACE_DEDUP_INDEX_PATTERN =
        AiGatewayConstraintMatchers.wordBoundaryPattern(TRACE_DEDUP_INDEX);

    // Per-request rejection-log cap: a 1000-span batch failing identically would otherwise emit 1000 warn
    // lines all carrying the same exception class. Matches the OtlpProtobufMapperImpl per-batch cap.
    private static final int PER_REQUEST_REJECTION_LOG_CAP = 5;

    // Upper bound on the positional retrieval.documents.{i}.document.content scan in resolveRetrievalOutput.
    // Guards against an unbounded loop if a malformed exporter emits a pathologically long contiguous run of
    // indexed document attributes.
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
        // Direct repository for orphan-trace cleanup. The service interface has no single-id delete (its
        // delete API is retention-driven via deleteOlderThan); rather than carve out a service method just
        // for this cleanup, we use the repository. The cleanup is a narrow, self-contained operation.
        this.aiObservabilityTraceRepository = aiObservabilityTraceRepository;
        // Span repository so cleanupJustCreatedTrace can detect concurrent adoption — another exporter that
        // raced past the trace create-commit and successfully wrote its own span owns the trace, and our
        // cleanup must NOT delete it (the FK violation would only burn a misleading ERROR log).
        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
        this.otlpCostResolver = otlpCostResolver;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    /**
     * Ingests one OTLP batch.
     *
     * <p>
     * Counter invariant for normal completion (no JVM {@link Error}):
     * {@code accepted + deduplicated + rejected == batch.spans().size() + batch.rejectedSpans()}. If a JVM Error
     * (OutOfMemoryError, StackOverflowError) escapes mid-loop, the counters reflect the rejected span before the Error
     * is rethrown so any caller inspecting partial state through a debugger or wrapping catch sees a self-consistent
     * count up to that point.
     */
    @Override
    public OtlpIngestResult ingest(Long workspaceId, OtelSpanBatch batch) {
        if (batch.droppedSpans() > 0) {
            incrementDroppedCounter(batch.droppedSpans());
        }

        // Hoist batch.spans() once — this list is referenced repeatedly in the per-span loop and the dedup-summary
        // log line; calling the record getter inside the loop header was a CLAUDE.md method-chaining hit and a
        // micro-perf hit on hot batches. Locals also make the tail-end log line clearer (was: batch.spans().size()).
        List<OtelGenAiSpan> batchSpans = batch.spans();
        int batchSpanCount = batchSpans.size();

        int accepted = 0;
        int deduplicated = 0;
        int rejected = 0;
        int loggedRejections = 0;
        List<RejectionDetail> reasons = new ArrayList<>();
        List<RejectionDetail> warnings = buildMapperWarnings(batch.mapperWarnings());

        // Aggregate mapper-rejected spans into one RejectionDetail rather than N copies — for a 10k-span batch
        // with mass mapper rejection, identical rows train operators to treat distinct entries as noise.
        if (batch.rejectedSpans() > 0) {
            rejected += batch.rejectedSpans();

            reasons.add(RejectionDetail.withoutIndex(
                RejectionCode.REJECTED_BY_MAPPER,
                batch.rejectedSpans() + " span(s) rejected by mapper (malformed boundary input)"));

            incrementCounter("REJECTED_BY_MAPPER", batch.rejectedSpans());
        }

        for (int spanIndex = 0; spanIndex < batchSpanCount; spanIndex++) {
            OtelGenAiSpan otelSpan = batchSpans.get(spanIndex);
            // systemAttr() returns null for both missing and non-CharSequence-typed values — both route
            // through the same REJECTED_NO_SYSTEM bucket below. The non-string-shape case (e.g. a malformed
            // exporter emitting gen_ai.system as a one-element ARRAY_VALUE) used to silently land
            // "[openai]" as the provider; null-on-type-mismatch in OtelGenAiSpan.readStringAttr makes the
            // span surface here as missing-system instead.
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
                // JVM-level distress (OOM, StackOverflow). Increment the rejected counter and add a detail row
                // before propagating so any caller inspecting partial state sees a self-consistent count, then
                // rethrow so the JVM can shed load.
                rejected++;

                reasons.add(RejectionDetail.at(
                    spanIndex, RejectionCode.REJECTED_INTERNAL,
                    otelSpan.spanIdHex() + ": " + AiGatewayThrowables.summarize(error)));

                incrementCounter("REJECTED_INTERNAL");

                throw error;
            }
        }

        // Surface the truncation explicitly so operators reading the log can see "we hit the cap" rather than
        // wondering why mass rejections from a misbehaving exporter only produced 5 lines. The rejected /
        // counter / detail counts above are exact — only the per-line breadcrumb logging is capped.
        if (rejected > loggedRejections) {
            log.warn(
                "OTLP ingest emitted {} additional rejection(s) in this request beyond the log cap (cap={})",
                rejected - loggedRejections, PER_REQUEST_REJECTION_LOG_CAP);
        }

        // Per-batch INFO summary when any spans were deduplicated. The per-span DEBUG line at persistSpan() is
        // off in production, so operators tailing INFO have no breadcrumb when an exporter retry storm causes
        // a 30% drop in apparent span count — they would have to remember the bytechef_ai_otlp_spans_ingested
        // {status=DEDUPLICATED} counter exists. One per-batch line is small in steady-state but immediately
        // visible during the stuck-exporter scenario where dedups suddenly account for the bulk of a batch.
        if (deduplicated > 0) {
            log.info(
                "OTLP ingest deduplicated {}/{} span(s) for workspaceId={} (idempotent retry path)",
                deduplicated, batchSpanCount, workspaceId);
        }

        return OtlpIngestResult.forBatch(batch, accepted, deduplicated, rejected, reasons, warnings);
    }

    /**
     * Wraps free-form mapper warning strings into typed {@link RejectionDetail} entries with the
     * {@link RejectionCode#MAPPER_RESOURCE_DECODE_WARNING} code. The OTLP API module emits warnings as plain strings to
     * avoid a dependency cycle (otlp-api would otherwise need to import the gateway-api module that already depends on
     * it); the conversion happens here at the seam so the wire-facing {@code OtlpIngestResult.warnings} list still
     * exposes structured codes that dashboards can branch on.
     */
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

    /**
     * Persists one span (creating its parent trace on first sight). Returns {@code true} if a new span row was created
     * and {@code false} if the span was a duplicate (idempotent retry path). On a non-dedup span failure where the
     * trace was just created in this call, the orphan trace is explicitly deleted so dashboards do not show "ghost"
     * traces with zero observable activity.
     */
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
                // Non-dedup constraint violation. If the trace was just created in this call, this leaves an
                // orphan — clean it up. Then propagate so the caller buckets as REJECTED_PERSIST_FAILED.
                cleanupJustCreatedTrace(resolution);

                throw duplicateKeyException;
            }

            // SPAN_DEDUP_INDEX hit: the (trace_id, external_span_id) tuple was already persisted, meaning the
            // parent trace existed before this call. No orphan possible — the trace is reachable via at least
            // one already-committed span.
            log.debug(
                "OTLP span dedup hit for traceId={} externalSpanId={} (already persisted)",
                trace.getId(), otelSpan.spanIdHex());

            return false;
        } catch (RuntimeException nonDedupFailure) {
            // Span insert failed for any reason other than dedup (DataIntegrityViolation on a different
            // constraint, transient connection error, etc.). Clean up the just-created trace so we don't
            // leave a ghost.
            cleanupJustCreatedTrace(resolution);

            throw nonDedupFailure;
        }
    }

    /**
     * Types a span as {@link AiObservabilitySpanType#RETRIEVAL} when the OpenInference {@code openinference.span.kind}
     * attribute reads {@code RETRIEVER} (case-insensitive); every other span keeps the existing
     * {@link AiObservabilitySpanType#GENERATION} default.
     */
    private AiObservabilitySpanType resolveSpanType(OtelGenAiSpan otelSpan) {
        String kind = otelSpan.spanKindAttr();

        if (kind != null && kind.equalsIgnoreCase("RETRIEVER")) {
            return AiObservabilitySpanType.RETRIEVAL;
        }

        return AiObservabilitySpanType.GENERATION;
    }

    /**
     * Joins OpenInference {@code retrieval.documents.{i}.document.content} attributes into a single text block. Falls
     * back to {@code null} when no such attributes are present so the caller keeps the span's {@code outputBody}.
     */
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

    /**
     * Deletes the just-created orphan trace if {@link TraceResolution#justCreated()} is {@code true}. Cleanup failures
     * are logged but never propagated — the caller's original exception is the load-bearing one. Long-lived orphans
     * (cleanup-failure path) are eventually picked up by the existing {@code deleteOlderThan} retention cron.
     */
    private void cleanupJustCreatedTrace(TraceResolution resolution) {
        if (!resolution.justCreated()) {
            return;
        }

        Long orphanId = resolution.trace()
            .getId();

        // Concurrent-adoption guard: between our trace create-commit and our span insert failure, another
        // exporter can have observed the trace via findByExternalTraceId, written its own span, and made the
        // trace healthy. fk_ai_obs_span_trace has no ON DELETE CASCADE, so deleteById then fails with an FK
        // violation, the catch fires ERROR + increments the counter, and dashboards alert for a row that is
        // perfectly fine. The pre-flight existsByTraceId leaves adopted traces alone (the originally-failed
        // span we attempted to insert is the rejected one; the adopting writer owns the trace now).
        //
        // The pre-flight is wrapped in its own try/catch: a transient DB outage during the adoption check
        // would otherwise propagate uncaught, masking the original load-bearing span-insert failure that the
        // caller is in the middle of handling. A failed adoption check is treated as "skip cleanup" (the
        // safe default — leaving an orphan is recoverable via the deleteOlderThan retention cron, while
        // wrongly deleting an adopted trace is not) and the operator-facing signal is preserved via the
        // ORPHAN_CLEANUP_FAILED counter so dashboards alert on cleanup-failure spikes regardless of which
        // step failed.
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
            // ERROR (not WARN): a stuck deleteById indicates DB connection-pool pressure or a row lock that
            // is itself worth alerting on independently of the original span-insert failure. The orphan
            // trace remains invisible to user-facing queries until the deleteOlderThan retention cron runs,
            // so the recovery path is unchanged — the severity bump is about giving ops a discoverable
            // signal for the underlying DB issue. The companion ORPHAN_CLEANUP_FAILED_METRIC counter lets
            // dashboards alert on cleanup-failure spikes independently of log retention. The pre-flight
            // existsByTraceId guard above means this branch should NOT fire for healthy concurrent-adoption
            // races — anything reaching here is a genuine DB problem.
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

    /**
     * Resolves the trace by external id. Returns a {@link TraceResolution} carrying both the trace and a flag
     * indicating whether the trace was created in THIS call (vs. found pre-existing or retrieved after losing a
     * concurrent-insert race). Only the "just created in this call" case needs orphan cleanup if a downstream span
     * insert fails — pre-existing traces and concurrent-race winners are owned by other actors.
     */
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
            // Concurrent insert lost the race on (workspace_id, external_trace_id); re-fetch the winner.
            // Narrowed to TRACE_DEDUP_INDEX so an unrelated future unique constraint does not silently
            // re-fetch under the wrong invariant. The race-winner trace belongs to another thread — flag
            // justCreated=false so cleanupJustCreatedTrace leaves it alone.
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

    // Workspace id is intentionally NOT a tag on either counter — high-cardinality per-workspace breakdowns blow
    // the Micrometer registry's memory budget; the per-workspace dimension belongs in logs / traces. Matches the
    // AiGatewayMetrics convention applied across the gateway so a future contributor adding workspaceId here
    // would silently start a meter explosion that only manifests under multi-tenant load.
    private void incrementCounter(String statusTag) {
        incrementCounter(statusTag, 1);
    }

    // The accepted+deduplicated+rejected == batch.size()+rejectedSpans() invariant must hold even if the meter
    // registry is closed mid-batch (JVM shutdown) or hits a cardinality cap. Without the inner try/catch, a
    // RuntimeException from Counter.builder(...).register(...).increment() inside the per-span try block at line
    // ~221 would escape into the surrounding RuntimeException catch — double-counting the same span as both
    // ACCEPTED and REJECTED_PERSIST_FAILED. Counter failures are operationally noisy but not data-correctness
    // affecting; degrade gracefully so the per-span hot path keeps running.
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
