/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreValue;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchItem;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.platform.ai.gateway.dto.ScoreTarget;
import com.bytechef.ee.platform.ai.gateway.dto.ScoreValue;
import com.bytechef.ee.platform.ai.gateway.event.AiScoreRecordedEvent;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.facade.RejectionCode;
import com.bytechef.ee.platform.ai.observability.facade.RejectionDetail;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Persists externally-produced eval scores (Ragas, DeepEval, TruLens, custom pipelines) against a caller-owned trace or
 * span. Workspace ownership is enforced by loading the target trace (or the span's trace) and comparing workspace ids
 * before any write — cross-tenant writes raise {@link AiScoreWorkspaceBoundaryException}, mapped to HTTP 403 at the web
 * layer.
 *
 * <p>
 * Every score write — single-target or batch row — is persisted inside its own short transaction via
 * {@link TransactionTemplate}, so a single failure (cross-workspace target, missing target, data-type/value mismatch,
 * or a database-level rejection like {@code DataIntegrityViolationException}) does not poison subsequent rows. Per-row
 * failures are counted in {@code rejectedCount} and surfaced in {@code rejectionReasons}.
 *
 * <p>
 * The {@link AiScoreRecordedEvent} is published <em>after</em> the persistence transaction commits — moving it outside
 * {@link TransactionTemplate#execute} means a buggy listener no longer rolls back the write and gets bucketed as
 * {@code PERSIST_FAILED}. A listener failure stays in {@code acceptedCount} on the response (the row is in the DB) and
 * is surfaced via a dedicated {@code listenerFailedCount} bucket plus a per-row {@link RejectionDetail} with code
 * {@link com.bytechef.ee.platform.ai.observability.facade.RejectionCode#LISTENER_FAILED} so operators can alert on
 * notification-side pain. Reclassifying these rows as rejected would conflate "wrote and notified", "wrote but no
 * notify", and "did not write" — clients deduping on {@code acceptedCount} would resubmit and the row would persist
 * twice (no unique index on {@code ai_eval_score} stops the second write).
 *
 * <p>
 * Critical-failure symmetry: the {@code recordBatch} loop catches {@link Error} for the persistence step (mirroring the
 * OTLP ingest facade's pattern) so an OOM / StackOverflow during a single row's write increments the {@code rejected}
 * counter and adds a detail row before propagating. Without that branch, the batch counters would silently
 * desynchronize from {@code items.size()} when the JVM was already in distress.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "EI"
})
public class AiExternalScoreFacadeImpl implements AiExternalScoreFacade {

    private static final Logger log = LoggerFactory.getLogger(AiExternalScoreFacadeImpl.class);

    private static final String METRIC_RECORDED = "bytechef_ai_score_recorded";
    private static final String METRIC_VALUE = "bytechef_ai_score_value";
    private static final String METRIC_LISTENER_FAILED = "bytechef_ai_score_listener_failed";

    private final WorkspaceAiEvalScoreService workspaceAiEvalScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    public AiExternalScoreFacadeImpl(
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilitySpanService aiObservabilitySpanService,
        ApplicationEventPublisher applicationEventPublisher,
        ObjectMapper objectMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        PlatformTransactionManager transactionManager) {

        this.workspaceAiEvalScoreService = workspaceAiEvalScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public AiExternalScoreResult recordTraceScore(Long workspaceId, Long traceId, AiExternalScoreRequest request) {
        AiEvalScore persisted;

        try {
            persisted = transactionTemplate.execute(
                status -> persistOne(workspaceId, new ScoreTarget.Trace(traceId), request));
        } catch (AiScoreTargetNotFoundException notFound) {
            // Inline boundary logging mirrors recordBatch — letting AiGatewayExceptionHandler do it works for
            // REST callers but loses the security-ops ERROR signal for any non-REST caller (scheduled task,
            // copilot, SDK loop). The handler's log is duplicative for REST callers but harmless; the cost of
            // a duplicate ERROR line is far smaller than the cost of a silent cross-tenant probe.
            AiScoreWorkspaceBoundaryException boundary = collapseToBoundary(
                workspaceId, AiScoreTargetType.TRACE, traceId, notFound);

            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} trace {} reason={}",
                workspaceId, traceId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (AiScoreWorkspaceBoundaryException boundary) {
            // A genuine workspace mismatch (caller targeted a trace owned by another workspace) lands here
            // unmodified — the not-found→boundary collapse above only fires when the trace is missing.
            // Without this branch the only ERROR signal for a cross-tenant write from a non-REST caller
            // would be the @ExceptionHandler at the web layer, which non-REST callers never reach. Symmetry
            // with the recordBatch loop's catch keeps the security dashboard consistent across entry points.
            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} trace {} reason={}",
                workspaceId, traceId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (Error error) {
            // JVM-level distress (OOM, StackOverflow). The class Javadoc advertises critical-failure symmetry
            // with the OTLP ingest facade and the recordBatch loop already catches Error; without this branch
            // a non-REST singleton caller (scheduled task, copilot loop) would emit no security-ops ERROR
            // signal on the way out — only the JVM stack trace would surface, and only if the caller logged
            // it. Mirror the batch path's discipline so dashboards stay consistent across entry points.
            log.error(
                "Score write hit JVM Error (singleton): workspaceId={} trace {} reason={}",
                workspaceId, traceId, AiGatewayThrowables.summarize(error), error);

            throw error;
        }

        emitRecordedEvent(persisted, workspaceId);
        incrementCounters(persisted);

        return AiExternalScoreResult.accepted(persisted.getId());
    }

    @Override
    public AiExternalScoreResult recordSpanScore(Long workspaceId, Long spanId, AiExternalScoreRequest request) {
        AiEvalScore persisted;

        try {
            persisted = transactionTemplate.execute(
                status -> persistOne(workspaceId, new ScoreTarget.Span(spanId), request));
        } catch (AiScoreTargetNotFoundException notFound) {
            AiScoreWorkspaceBoundaryException boundary = collapseToBoundary(
                workspaceId, AiScoreTargetType.SPAN, spanId, notFound);

            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} span {} reason={}",
                workspaceId, spanId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (AiScoreWorkspaceBoundaryException boundary) {
            // See recordTraceScore: the genuine-mismatch path needs its own catch so non-REST callers
            // still emit the security-ops ERROR signal.
            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} span {} reason={}",
                workspaceId, spanId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (Error error) {
            // See recordTraceScore: critical-failure symmetry with the recordBatch path.
            log.error(
                "Score write hit JVM Error (singleton): workspaceId={} span {} reason={}",
                workspaceId, spanId, AiGatewayThrowables.summarize(error), error);

            throw error;
        }

        emitRecordedEvent(persisted, workspaceId);
        incrementCounters(persisted);

        return AiExternalScoreResult.accepted(persisted.getId());
    }

    /**
     * Collapses a 404-class not-found into the same 403-class boundary failure shape used for wrong-workspace targets.
     * The two outcomes must be indistinguishable on the wire — distinguishability lets a workspace-A caller enumerate
     * workspace-B trace/span ids by probing the 404-vs-403 delta. The original cause is preserved for server-side
     * debugging but never reaches the client (the boundary handler only renders the message). Structured fields on the
     * boundary exception carry the workspace + target context for log appenders without leaking it via the message.
     */
    private static AiScoreWorkspaceBoundaryException collapseToBoundary(
        Long workspaceId, AiScoreTargetType targetType, Long targetId, AiScoreTargetNotFoundException notFound) {

        return AiScoreWorkspaceBoundaryException.forTarget(workspaceId, targetType, targetId, notFound);
    }

    @Override
    public AiExternalScoreBatchResult recordBatch(Long workspaceId, AiExternalScoreBatchRequest request) {
        int acceptedCount = 0;
        int rejectedCount = 0;
        int listenerFailedCount = 0;
        List<RejectionDetail> rejectionReasons = new ArrayList<>();
        List<AiEvalScore> persistedRows = new ArrayList<>();

        List<AiExternalScoreBatchItem> items = request.scores();

        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            AiExternalScoreBatchItem item = items.get(itemIndex);

            AiExternalScoreRequest itemRequest = new AiExternalScoreRequest(
                item.name(), item.value(), item.dataType(), item.comment(), item.source(), item.metadata());

            // Dispatch on the sealed ScoreTarget — both-null and both-set cases are already rejected by the DTO
            // compact constructor (which delegates to ScoreTarget.of). Pulling target() here gives the loop a
            // typed reference that the persistOne dispatch and the targetDescription string both share, so a
            // future variant added to ScoreTarget without updating both sites fails the compiler. No try/catch:
            // target() is deterministic from the validated record fields — if construction passed, every later
            // call returns the same value without throwing.
            ScoreTarget target = item.target();

            String targetDescription = switch (target) {
                case ScoreTarget.Trace trace -> "trace " + trace.traceId();
                case ScoreTarget.Span span -> "span " + span.spanId();
            };

            try {
                AiEvalScore persisted = transactionTemplate.execute(
                    status -> persistOne(workspaceId, target, itemRequest));

                persistedRows.add(persisted);

                acceptedCount++;
            } catch (AiScoreWorkspaceBoundaryException exception) {
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.WORKSPACE_BOUNDARY, targetDescription, exception));

                // Cross-tenant write attempt — ERROR so security-ops dashboards see it. Silent absorption
                // would train operators to ignore the only signal a real attack produces.
                log.error(
                    "Cross-workspace score write rejected (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(exception), exception);
            } catch (AiScoreTargetNotFoundException exception) {
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.NOT_FOUND, targetDescription, exception));

                // 404-class — INFO suffices; clients passing stale ids is normal.
                log.info(
                    "Score target not found (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(exception));
            } catch (IllegalArgumentException exception) {
                // Caller-side validation failure. Today the only known IAE source inside persistOne is
                // serializeMetadata, which wraps a Jackson failure in IllegalArgumentException. The catch is
                // INTENTIONALLY broader: a future contributor adding another caller-shape IAE inside persistOne
                // (e.g. negative id translation) lands here as VALIDATION_FAILED, which is the right bucket for
                // payload-shape problems. Distinct from PERSIST_FAILED so a client retrying because the response
                // said "PERSIST_FAILED" does not retry against a database that never failed — value-shape errors
                // stay rejected on every retry and need different remediation (fix the payload). NOTE: a non-
                // validation IAE (e.g. an internal id-translation bug throwing IAE for the wrong reason) would
                // also bucket here. If that becomes a real concern, introduce a typed sentinel (mirroring
                // MissingModelException in AiEvalExecutor) and dispatch on instanceof.
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.VALIDATION_FAILED, targetDescription, exception));

                log.info(
                    "Score validation failed (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(exception));
            } catch (RuntimeException exception) {
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.PERSIST_FAILED, targetDescription, exception));

                // Anything else is a real bug or DB failure: WARN with class name + stack.
                String exceptionType = exception.getClass()
                    .getSimpleName();

                log.warn(
                    "Score write failed (batch): workspaceId={} {} type={} reason={}",
                    workspaceId, targetDescription, exceptionType,
                    AiGatewayThrowables.summarize(exception), exception);
            } catch (Error error) {
                // JVM-level distress (OOM, StackOverflow). Mirror the OTLP ingest facade's pattern: increment
                // the rejected counter and add a detail row before propagating, so any caller inspecting
                // partial state through a debugger or wrapping catch sees a self-consistent count up to the
                // failure. Without this branch, accepted/rejected counts would silently desync from
                // items.size() under JVM distress.
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.BATCH_INTERNAL_ERROR, targetDescription, error));

                log.error(
                    "Score write hit JVM Error (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(error), error);

                throw error;
            }
        }

        // Emit recorded events + counters AFTER all transactions have committed. A buggy listener now cannot
        // roll back any persistence work. The row stays in acceptedCount (it's in the DB); the failure is
        // surfaced via the dedicated listenerFailedCount bucket plus a per-row RejectionDetail. Reclassifying
        // it as rejected would let a client deduping on acceptedCount resubmit the same row — there's no
        // unique index on ai_eval_score to absorb the duplicate, so it would persist twice.
        for (AiEvalScore persisted : persistedRows) {
            try {
                emitRecordedEvent(persisted, workspaceId);
                incrementCounters(persisted);
            } catch (RuntimeException listenerFailure) {
                listenerFailedCount++;
                rejectionReasons.add(RejectionDetail.withoutIndex(
                    RejectionCode.LISTENER_FAILED,
                    "score id=" + persisted.getId() + ": " + AiGatewayThrowables.summarize(listenerFailure)));

                log.warn(
                    "Score recorded-event listener failed for persisted score id={}: {}",
                    persisted.getId(), AiGatewayThrowables.summarize(listenerFailure), listenerFailure);

                incrementListenerFailedCounter();
            }
        }

        return new AiExternalScoreBatchResult(acceptedCount, rejectedCount, listenerFailedCount, rejectionReasons);
    }

    /**
     * Builds a stable, non-null {@link RejectionDetail}. {@code exception.getMessage()} can be null for many Spring
     * {@code DataIntegrityViolationException}s; routing through {@link AiGatewayThrowables#summarize} falls back to the
     * class name and avoids the literal {@code "trace 123: null"} a naive {@code getMessage()} would emit. The
     * structured {@code code} field carries the rejection class so dashboards branch without prose-matching.
     */
    private static RejectionDetail rejectionDetail(
        int itemIndex, RejectionCode code, String targetDescription, Throwable exception) {

        return RejectionDetail.at(itemIndex, code, targetDescription + ": " + AiGatewayThrowables.summarize(exception));
    }

    /**
     * Shared persistence path for single-target methods and per-row batch dispatch. Dispatches on the sealed
     * {@link ScoreTarget} — the trace variant resolves the trace directly; the span variant resolves the span first
     * then loads its parent trace to authorize the workspace. Always called inside a
     * {@link TransactionTemplate}-managed transaction. Returns the persisted entity so the caller can fire the
     * {@link AiScoreRecordedEvent} after commit (decoupling listener failures from rollback).
     */
    private AiEvalScore persistOne(Long workspaceId, ScoreTarget target, AiExternalScoreRequest request) {
        Long resolvedTraceId;
        Long resolvedSpanId;

        switch (target) {
            case ScoreTarget.Span spanTarget -> {
                AiObservabilitySpan span = loadSpan(workspaceId, spanTarget.spanId());
                AiObservabilityTrace trace = loadTrace(workspaceId, span.getTraceId());

                assertSameWorkspace(workspaceId, workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()),
                    AiScoreTargetType.SPAN, spanTarget.spanId());

                resolvedTraceId = span.getTraceId();
                resolvedSpanId = spanTarget.spanId();
            }
            case ScoreTarget.Trace traceTarget -> {
                AiObservabilityTrace trace = loadTrace(workspaceId, traceTarget.traceId());

                assertSameWorkspace(
                    workspaceId, workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()),
                    AiScoreTargetType.TRACE,
                    traceTarget.traceId());

                resolvedTraceId = traceTarget.traceId();
                resolvedSpanId = null;
            }
        }

        AiEvalScore score = buildScore(resolvedTraceId, resolvedSpanId, request);

        return workspaceAiEvalScoreService.createInWorkspace(score, workspaceId);
    }

    /**
     * Loads a trace, translating the not-found {@link IllegalArgumentException} from the underlying service into
     * {@link AiScoreTargetNotFoundException} with the cause preserved. Cause preservation matters: the underlying
     * service throws {@code IllegalArgumentException} for both "not found" AND future validation failures (negative id,
     * malformed format), so dropping the cause would let a validation bug masquerade as a 404 in logs and lead ops to
     * chase ghost "missing target" tickets when the real failure was a malformed id.
     */
    private AiObservabilityTrace loadTrace(Long callerWorkspaceId, Long traceId) {
        try {
            return aiObservabilityTraceService.getTrace(traceId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw AiScoreTargetNotFoundException.forTarget(
                callerWorkspaceId, AiScoreTargetType.TRACE, traceId, illegalArgumentException);
        }
    }

    /**
     * Loads a span, translating the not-found {@link IllegalArgumentException} into
     * {@link AiScoreTargetNotFoundException} with the cause preserved. See {@link #loadTrace} for rationale.
     */
    private AiObservabilitySpan loadSpan(Long callerWorkspaceId, Long spanId) {
        try {
            return aiObservabilitySpanService.getSpan(spanId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw AiScoreTargetNotFoundException.forTarget(
                callerWorkspaceId, AiScoreTargetType.SPAN, spanId, illegalArgumentException);
        }
    }

    private AiEvalScore buildScore(Long traceId, Long spanId, AiExternalScoreRequest request) {
        String serializedMetadata = serializeMetadata(request.metadata());

        // Dispatch on the sealed ScoreValue rather than the enum dataType: a future AiEvalScoreDataType variant
        // added without a corresponding ScoreValue permit fails at compile time, instead of slipping through an
        // unreachable-default branch and surfacing as a runtime IllegalStateException in production.
        AiEvalScore score = switch (request.scoreValue()) {
            case ScoreValue.Numeric numeric -> AiEvalScore.numeric(
                traceId, request.name(), AiEvalScoreSource.EXTERNAL,
                numeric.value(), request.source(), serializedMetadata);
            case ScoreValue.Bool bool -> AiEvalScore.bool(
                traceId, request.name(), AiEvalScoreSource.EXTERNAL,
                bool.value(), request.source(), serializedMetadata);
            case ScoreValue.Categorical categorical -> AiEvalScore.categorical(
                traceId, request.name(), AiEvalScoreSource.EXTERNAL,
                categorical.value(), request.source(), serializedMetadata);
        };

        if (spanId != null) {
            score.setSpanId(spanId);
        }

        if (request.comment() != null) {
            score.setComment(request.comment());
        }

        return score;
    }

    /**
     * Fires the recorded event for one persisted score. Called outside the persistence transaction so listener failures
     * do not roll back the write — they surface as a distinct rejection bucket instead.
     */
    private void emitRecordedEvent(AiEvalScore persisted, Long workspaceId) {
        applicationEventPublisher.publishEvent(new AiScoreRecordedEvent(
            persisted.getId(),
            workspaceId,
            persisted.getTraceId(),
            persisted.getSpanId(),
            persisted.getName(),
            persisted.getDataType(),
            persisted.getSource(),
            persisted.getSourceIdentifier(),
            Instant.now()));
    }

    /**
     * Workspace boundary check — fail-closed. {@link Objects#equals(Object, Object)} treats null operands as not-equal
     * so a null {@code targetWorkspaceId} (from a future schema change relaxing the NOT-NULL on the trace/span
     * workspace_id, or from a hypothetical orphaned row) throws the boundary exception with diagnostic context instead
     * of NPE-ing into a 500. {@code callerWorkspaceId} arrives from the authenticated request header and is enforced
     * non-null upstream — but defending the comparison itself keeps the failure mode loud regardless of which side
     * becomes null first.
     *
     * <p>
     * The exception message is uniform ("Caller is not authorized for X") so the wrong-workspace path produces the same
     * wire shape as the not-found path collapsed by {@link #collapseToBoundary}. Distinguishable messages would leak
     * whether a target exists in another workspace and what its workspace id is. The full diagnostic context (caller
     * and target workspace ids) is emitted to the security log at the throw site instead.
     */
    private static void assertSameWorkspace(
        Long callerWorkspaceId, Long targetWorkspaceId, AiScoreTargetType targetType, Long targetId) {

        if (!Objects.equals(callerWorkspaceId, targetWorkspaceId)) {
            // Diagnostic context (caller/target workspace ids) is intentionally NOT in the exception message —
            // a wire-leaking message would let workspace-A enumerate workspace-B target ids by reading the
            // 403 body. Structured fields on the exception carry the same context server-side for log
            // appenders. Three call sites log this ERROR with workspace + target context: recordBatch's
            // per-row catch, recordTraceScore/recordSpanScore's singleton catch (so non-REST callers like
            // scheduled tasks and copilot loops still emit the signal), and the
            // AiGatewayExceptionHandler.handleWorkspaceBoundary at the web layer. The throw site stays
            // log-free so test fixtures can assert on a single ERROR event per attempted write.
            throw AiScoreWorkspaceBoundaryException.forTarget(callerWorkspaceId, targetType, targetId);
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException jacksonException) {
            throw new IllegalArgumentException("metadata is not serializable as JSON", jacksonException);
        }
    }

    private void incrementCounters(AiEvalScore score) {
        if (meterRegistry == null) {
            return;
        }

        // Workspace id is intentionally NOT a tag — high-cardinality breakdowns belong in logs / traces, not
        // Micrometer (matches the AiGatewayMetrics convention which excludes workspace from counter tags).
        AiEvalScoreSource source = score.getSource();
        AiEvalScoreDataType dataType = score.getDataType();

        try {
            Counter.builder(METRIC_RECORDED)
                .tag("source", source.name())
                .tag("data_type", dataType.name())
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            // Counters are observability for the recordBatch post-commit emit loop. A registry-side failure
            // here must not propagate into catch (RuntimeException listenerFailure) and rebucket the row as
            // LISTENER_FAILED — that would blame applicationEventPublisher for a registry problem and corrupt
            // the dashboard alerting on "listener health".
            log.debug(
                "Failed to increment {} (source={}, data_type={}); continuing",
                METRIC_RECORDED, source, dataType, meterFailure);
        }

        if (dataType == AiEvalScoreDataType.NUMERIC) {
            // Score `name` is caller-controlled (per-request, unbounded by the API) so it is intentionally NOT a
            // tag — same discipline as the recorded counter above. A misbehaving or malicious external scorer
            // submitting many unique names would otherwise explode the metric series, denial-of-service the
            // Micrometer registry, and leak memory until restart. Per-name breakdowns belong in logs / traces.
            // Read via typed view: a corrupt NUMERIC row with a null value column will throw IllegalStateException
            // (loud), instead of being silently dropped from the histogram (where the absence is invisible).
            AiEvalScoreValue typedValue = score.getTypedValue();

            if (typedValue instanceof AiEvalScoreValue.Numeric numeric) {
                BigDecimal numericValue = numeric.value();
                double recordedValue = numericValue.doubleValue();

                try {
                    DistributionSummary.builder(METRIC_VALUE)
                        .register(meterRegistry)
                        .record(recordedValue);
                } catch (RuntimeException meterFailure) {
                    log.debug(
                        "Failed to record {} for score {}; continuing",
                        METRIC_VALUE, score.getId(), meterFailure);
                }
            }
        }
    }

    /**
     * Counter for {@link AiScoreRecordedEvent} listener failures. Per-row breadcrumb already lands in
     * {@code rejectionReasons} and a WARN log fires per failure, but operator dashboards need an aggregate series to
     * alert on "the listener is broken right now" without scraping logs. Untagged because the meaningful dimensions
     * (workspace, score id) are too high-cardinality for Micrometer.
     */
    private void incrementListenerFailedCounter() {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(METRIC_LISTENER_FAILED)
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            // The listener-failed counter itself failing must not propagate out of recordBatch — subsequent
            // rows in the persisted-rows loop would never get their event fired and never receive a
            // LISTENER_FAILED detail row.
            log.debug("Failed to increment {}; continuing", METRIC_LISTENER_FAILED, meterFailure);
        }
    }
}
