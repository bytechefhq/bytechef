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
            AiScoreWorkspaceBoundaryException boundary = collapseToBoundary(
                workspaceId, AiScoreTargetType.TRACE, traceId, notFound);

            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} trace {} reason={}",
                workspaceId, traceId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (AiScoreWorkspaceBoundaryException boundary) {
            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} trace {} reason={}",
                workspaceId, traceId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (Error error) {
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
            log.error(
                "Cross-workspace score write rejected (singleton): workspaceId={} span {} reason={}",
                workspaceId, spanId, AiGatewayThrowables.summarize(boundary), boundary);

            throw boundary;
        } catch (Error error) {
            log.error(
                "Score write hit JVM Error (singleton): workspaceId={} span {} reason={}",
                workspaceId, spanId, AiGatewayThrowables.summarize(error), error);

            throw error;
        }

        emitRecordedEvent(persisted, workspaceId);
        incrementCounters(persisted);

        return AiExternalScoreResult.accepted(persisted.getId());
    }

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

                log.error(
                    "Cross-workspace score write rejected (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(exception), exception);
            } catch (AiScoreTargetNotFoundException exception) {
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.NOT_FOUND, targetDescription, exception));

                log.info(
                    "Score target not found (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(exception));
            } catch (IllegalArgumentException exception) {
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

                String exceptionType = exception.getClass()
                    .getSimpleName();

                log.warn(
                    "Score write failed (batch): workspaceId={} {} type={} reason={}",
                    workspaceId, targetDescription, exceptionType,
                    AiGatewayThrowables.summarize(exception), exception);
            } catch (Error error) {
                rejectedCount++;
                rejectionReasons.add(
                    rejectionDetail(itemIndex, RejectionCode.BATCH_INTERNAL_ERROR, targetDescription, error));

                log.error(
                    "Score write hit JVM Error (batch): workspaceId={} {} reason={}",
                    workspaceId, targetDescription, AiGatewayThrowables.summarize(error), error);

                throw error;
            }
        }

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

    private static RejectionDetail rejectionDetail(
        int itemIndex, RejectionCode code, String targetDescription, Throwable exception) {

        return RejectionDetail.at(itemIndex, code, targetDescription + ": " + AiGatewayThrowables.summarize(exception));
    }

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

    private AiObservabilityTrace loadTrace(Long callerWorkspaceId, Long traceId) {
        try {
            return aiObservabilityTraceService.getTrace(traceId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw AiScoreTargetNotFoundException.forTarget(
                callerWorkspaceId, AiScoreTargetType.TRACE, traceId, illegalArgumentException);
        }
    }

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

    private static void assertSameWorkspace(
        Long callerWorkspaceId, Long targetWorkspaceId, AiScoreTargetType targetType, Long targetId) {

        if (!Objects.equals(callerWorkspaceId, targetWorkspaceId)) {
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

        AiEvalScoreSource source = score.getSource();
        AiEvalScoreDataType dataType = score.getDataType();

        try {
            Counter.builder(METRIC_RECORDED)
                .tag("source", source.name())
                .tag("data_type", dataType.name())
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug(
                "Failed to increment {} (source={}, data_type={}); continuing",
                METRIC_RECORDED, source, dataType, meterFailure);
        }

        if (dataType == AiEvalScoreDataType.NUMERIC) {
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

    private void incrementListenerFailedCounter() {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(METRIC_LISTENER_FAILED)
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException meterFailure) {
            log.debug("Failed to increment {}; continuing", METRIC_LISTENER_FAILED, meterFailure);
        }
    }
}
