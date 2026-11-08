/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchItem;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.platform.ai.gateway.event.AiScoreRecordedEvent;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.RejectionCode;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiExternalScoreFacadeTest {

    @Test
    void testRecordTraceScorePersistsAndEmitsEvent() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 1L);

        when(traceService.getTrace(1L)).thenReturn(trace);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);

        AiEvalScore persistedScore = AiEvalScore.numeric(
            1L, "faithfulness", AiEvalScoreSource.EXTERNAL, new BigDecimal("0.87"),
            "ragas@0.2.3", "{\"run_id\":\"abc\"}");

        ReflectionTestUtils.setField(persistedScore, "id", 99L);

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong()))
            .thenReturn(persistedScore);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", new BigDecimal("0.87"), AiEvalScoreDataType.NUMERIC,
            null, "ragas@0.2.3", Map.of("run_id", "abc"));

        AiExternalScoreResult result = facade.recordTraceScore(42L, 1L, request);

        assertThat(result).isEqualTo(new AiExternalScoreResult.Accepted(99L));

        verify(publisher).publishEvent(any(AiScoreRecordedEvent.class));
    }

    @Test
    void testRecordTraceScoreRejectsCrossWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 1L);

        when(traceService.getTrace(1L)).thenReturn(trace);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(99L);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        assertThatThrownBy(() -> facade.recordTraceScore(42L, 1L, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class);
    }

    @Test
    void testRecordTraceScoreCollapsesNotFoundOntoBoundaryException() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        IllegalArgumentException rootCause = new IllegalArgumentException("trace 999 not found");

        when(traceService.getTrace(999L)).thenThrow(rootCause);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        assertThatThrownBy(() -> facade.recordTraceScore(42L, 999L, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessage("Caller is not authorized for trace 999")
            .hasCauseInstanceOf(AiScoreTargetNotFoundException.class);
    }

    @Test
    void testRecordSpanScoreUsesSpanTraceWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilitySpan span = new AiObservabilitySpan(7L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);

        when(spanService.getSpan(5L)).thenReturn(span);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 7L);

        when(traceService.getTrace(7L)).thenReturn(trace);
        when(workspaceTraceService.getWorkspaceId(7L)).thenReturn(42L);

        AiEvalScore persistedScore = AiEvalScore.numeric(
            7L, "faithfulness", AiEvalScoreSource.EXTERNAL, BigDecimal.ONE, "ragas", null);

        ReflectionTestUtils.setField(persistedScore, "id", 100L);
        persistedScore.setSpanId(5L);

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong()))
            .thenReturn(persistedScore);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        AiExternalScoreResult result = facade.recordSpanScore(42L, 5L, request);

        assertThat(result).isEqualTo(new AiExternalScoreResult.Accepted(100L));
    }

    @Test
    void testRejectsSpanScoreFromForeignWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilitySpan span = new AiObservabilitySpan(7L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);

        when(spanService.getSpan(5L)).thenReturn(span);

        AiObservabilityTrace foreignTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(foreignTrace, "id", 7L);

        when(traceService.getTrace(7L)).thenReturn(foreignTrace);
        when(workspaceTraceService.getWorkspaceId(7L)).thenReturn(99L);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        assertThatThrownBy(() -> facade.recordSpanScore(42L, 5L, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class);
    }

    @Test
    void testRejectsTraceScoreWhenTargetTraceWorkspaceIsNull() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        ReflectionTestUtils.setField(trace, "id", 7L);

        when(traceService.getTrace(7L)).thenReturn(trace);
        when(workspaceTraceService.getWorkspaceId(7L)).thenReturn(null);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

        assertThatThrownBy(() -> facade.recordTraceScore(42L, 7L, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessageContaining("trace 7");
    }

    @Test
    void testRecordBatchHappyPath() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace trace1 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace1, "id", 1L);

        AiObservabilityTrace trace2 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace2, "id", 2L);

        when(traceService.getTrace(1L)).thenReturn(trace1);
        when(traceService.getTrace(2L)).thenReturn(trace2);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);
        when(workspaceTraceService.getWorkspaceId(2L)).thenReturn(42L);

        AiObservabilitySpan span = new AiObservabilitySpan(2L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);

        when(spanService.getSpan(5L)).thenReturn(span);

        AiEvalScore persisted = AiEvalScore.numeric(
            1L, "faithfulness", AiEvalScoreSource.EXTERNAL, BigDecimal.ONE, "ragas", null);

        ReflectionTestUtils.setField(persisted, "id", 1000L);

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong())).thenReturn(persisted);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
            new AiExternalScoreBatchItem(
                1L, null, "faithfulness", new BigDecimal("0.91"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()),
            new AiExternalScoreBatchItem(
                2L, null, "coherence", new BigDecimal("0.88"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()),
            new AiExternalScoreBatchItem(
                null, 5L, "relevance", BigDecimal.ONE, AiEvalScoreDataType.BOOLEAN,
                null, "internal-judge", Map.of())));

        AiExternalScoreBatchResult result = facade.recordBatch(42L, request);

        assertThat(result.acceptedCount()).isEqualTo(3);
        assertThat(result.rejectedCount()).isEqualTo(0);
        assertThat(result.rejectionReasons()).isEmpty();

        ArgumentCaptor<AiEvalScore> scoreCaptor = ArgumentCaptor.forClass(AiEvalScore.class);

        verify(workspaceAiEvalScoreService, times(3)).createInWorkspace(scoreCaptor.capture(), anyLong());

        List<AiEvalScore> created = scoreCaptor.getAllValues();

        assertThat(created.get(0)
            .getName()).isEqualTo("faithfulness");
        assertThat(created.get(0)
            .getTraceId()).isEqualTo(1L);
        assertThat(created.get(1)
            .getName()).isEqualTo("coherence");
        assertThat(created.get(1)
            .getTraceId()).isEqualTo(2L);
        assertThat(created.get(2)
            .getName()).isEqualTo("relevance");
        assertThat(created.get(2)
            .getSpanId()).isEqualTo(5L);
        assertThat(created.get(2)
            .getTraceId()).isEqualTo(2L);
    }

    @Test
    void testRecordBatchPartialSuccessOnCrossWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace traceOk1 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(traceOk1, "id", 1L);

        AiObservabilityTrace traceCrossWs = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(traceCrossWs, "id", 2L);

        AiObservabilityTrace traceOk2 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(traceOk2, "id", 3L);

        when(traceService.getTrace(1L)).thenReturn(traceOk1);
        when(traceService.getTrace(2L)).thenReturn(traceCrossWs);
        when(traceService.getTrace(3L)).thenReturn(traceOk2);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);
        when(workspaceTraceService.getWorkspaceId(2L)).thenReturn(99L);
        when(workspaceTraceService.getWorkspaceId(3L)).thenReturn(42L);

        AiEvalScore persisted = AiEvalScore.numeric(
            1L, "faithfulness", AiEvalScoreSource.EXTERNAL, BigDecimal.ONE, "ragas", null);

        ReflectionTestUtils.setField(persisted, "id", 1000L);

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong())).thenReturn(persisted);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
            new AiExternalScoreBatchItem(
                1L, null, "faithfulness", new BigDecimal("0.9"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()),
            new AiExternalScoreBatchItem(
                2L, null, "faithfulness", new BigDecimal("0.9"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()),
            new AiExternalScoreBatchItem(
                3L, null, "faithfulness", new BigDecimal("0.9"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of())));

        AiExternalScoreBatchResult result = facade.recordBatch(42L, request);

        assertThat(result.acceptedCount()).isEqualTo(2);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.rejectionReasons()).hasSize(1);
        assertThat(result.rejectionReasons()
            .get(0)
            .reason()).contains("trace 2");
        assertThat(result.rejectionReasons()
            .get(0)
            .code()).isEqualTo(RejectionCode.WORKSPACE_BOUNDARY);
        assertThat(result.rejectionReasons()
            .get(0)
            .index())
                .as("index of the rejected row within the inbound batch (0-based)")
                .isEqualTo(1);

        verify(workspaceAiEvalScoreService, times(2)).createInWorkspace(any(AiEvalScore.class), anyLong());
    }

    @Test
    void testRecordBatchPartialSuccessOnTargetNotFound() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace traceOk = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(traceOk, "id", 1L);

        when(traceService.getTrace(1L)).thenReturn(traceOk);
        when(traceService.getTrace(404L)).thenThrow(new IllegalArgumentException("not found"));
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);

        AiEvalScore persisted = AiEvalScore.numeric(
            1L, "faithfulness", AiEvalScoreSource.EXTERNAL, BigDecimal.ONE, "ragas", null);

        ReflectionTestUtils.setField(persisted, "id", 1000L);

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong())).thenReturn(persisted);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
            new AiExternalScoreBatchItem(
                1L, null, "faithfulness", new BigDecimal("0.9"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()),
            new AiExternalScoreBatchItem(
                404L, null, "faithfulness", new BigDecimal("0.9"), AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of())));

        AiExternalScoreBatchResult result = facade.recordBatch(42L, request);

        assertThat(result.acceptedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.rejectionReasons()).hasSize(1);
        assertThat(result.rejectionReasons()
            .get(0)
            .reason()).contains("trace 404");
        assertThat(result.rejectionReasons()
            .get(0)
            .code()).isEqualTo(RejectionCode.NOT_FOUND);
        assertThat(result.rejectionReasons()
            .get(0)
            .index())
                .as("index of the rejected row within the inbound batch (0-based)")
                .isEqualTo(1);

        verify(workspaceAiEvalScoreService, times(1)).createInWorkspace(any(AiEvalScore.class), anyLong());
    }

    @Test
    void testRecordBatchEmitsErrorLogOnCrossWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilityTrace foreignTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(foreignTrace, "id", 7L);

        when(traceService.getTrace(7L)).thenReturn(foreignTrace);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        ListAppender<ILoggingEvent> appender = attachAppender(AiExternalScoreFacadeImpl.class);

        try {
            AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
                new AiExternalScoreBatchItem(
                    7L, null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
                    null, "ragas", Map.of())));

            AiExternalScoreBatchResult result = facade.recordBatch(42L, request);

            assertThat(result.acceptedCount()).isZero();
            assertThat(result.rejectedCount()).isEqualTo(1);

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    String formatted = event.getFormattedMessage();

                    assertThat(formatted)
                        .as("ERROR log must mention workspaceId=42")
                        .contains("workspaceId=42");
                    assertThat(formatted)
                        .as("ERROR log must mention the target trace 7")
                        .contains("trace 7");
                });
        } finally {
            detachAppender(AiExternalScoreFacadeImpl.class, appender);
        }
    }

    @Test
    void testRecordSpanScoreEmitsErrorLogOnCrossWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilitySpan span = new AiObservabilitySpan(11L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);

        AiObservabilityTrace foreignTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(foreignTrace, "id", 11L);

        when(spanService.getSpan(5L)).thenReturn(span);
        when(traceService.getTrace(11L)).thenReturn(foreignTrace);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        ListAppender<ILoggingEvent> appender = attachAppender(AiExternalScoreFacadeImpl.class);

        try {
            AiExternalScoreRequest request = new AiExternalScoreRequest(
                "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of());

            assertThatThrownBy(() -> facade.recordSpanScore(42L, 5L, request))
                .isInstanceOf(AiScoreWorkspaceBoundaryException.class);

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    String formatted = event.getFormattedMessage();

                    assertThat(formatted)
                        .as("ERROR log must mention workspaceId=42")
                        .contains("workspaceId=42");
                    assertThat(formatted)
                        .as("ERROR log must mention the target span 5")
                        .contains("span 5");
                });
        } finally {
            detachAppender(AiExternalScoreFacadeImpl.class, appender);
        }
    }

    @Test
    void testRecordBatchEmitsErrorLogOnCrossWorkspaceForSpanTarget() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilitySpan span = new AiObservabilitySpan(11L, AiObservabilitySpanType.GENERATION);
        ReflectionTestUtils.setField(span, "id", 5L);

        AiObservabilityTrace foreignTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(foreignTrace, "id", 11L);

        when(spanService.getSpan(5L)).thenReturn(span);
        when(traceService.getTrace(11L)).thenReturn(foreignTrace);

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService,
            mock(ApplicationEventPublisher.class), new ObjectMapper(),
            staticProvider(new SimpleMeterRegistry()), noopTransactionManager());

        ListAppender<ILoggingEvent> appender = attachAppender(AiExternalScoreFacadeImpl.class);

        try {
            AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
                new AiExternalScoreBatchItem(
                    null, 5L, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
                    null, "ragas", Map.of())));

            AiExternalScoreBatchResult result = facade.recordBatch(42L, request);

            assertThat(result.acceptedCount()).isZero();
            assertThat(result.rejectedCount()).isEqualTo(1);

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    String formatted = event.getFormattedMessage();

                    assertThat(formatted)
                        .as("ERROR log must mention workspaceId=42")
                        .contains("workspaceId=42");
                    assertThat(formatted)
                        .as("ERROR log must mention the target span 5")
                        .contains("span 5");
                });
        } finally {
            detachAppender(AiExternalScoreFacadeImpl.class, appender);
        }
    }

    private static ListAppender<ILoggingEvent> attachAppender(Class<?> loggerClass) {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerClass);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppender(Class<?> loggerClass, ListAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerClass);

        logger.detachAppender(appender);
    }

    @Test
    void testRejectsNumericScoreWithNonNumericString() {
        assertThatThrownBy(() -> new AiExternalScoreRequest(
            "faithfulness", "high", AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUMERIC")
                .hasMessageContaining("String")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("high"));
    }

    @Test
    void testRejectsBooleanScoreWithNonNumericString() {
        assertThatThrownBy(() -> new AiExternalScoreRequest(
            "relevance", "yes", AiEvalScoreDataType.BOOLEAN, null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN")
                .hasMessageContaining("String")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("yes"));
    }

    @Test
    void testRecordBatchPropagatesAndCountsJvmError() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace, "id", 1L);

        when(traceService.getTrace(1L)).thenReturn(trace);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);

        OutOfMemoryError outOfMemoryError = new OutOfMemoryError("synthetic OOM during create()");

        when(workspaceAiEvalScoreService.createInWorkspace(any(AiEvalScore.class), anyLong()))
            .thenThrow(outOfMemoryError);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        AiExternalScoreFacadeImpl facade = new AiExternalScoreFacadeImpl(
            workspaceAiEvalScoreService, traceService, workspaceTraceService, spanService, publisher,
            new ObjectMapper(),
            staticProvider(registry), noopTransactionManager());

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(
            new AiExternalScoreBatchItem(
                1L, null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of())));

        ListAppender<ILoggingEvent> appender = attachAppender(AiExternalScoreFacadeImpl.class);

        try {
            assertThatThrownBy(() -> facade.recordBatch(42L, request))
                .as("Error must propagate to the caller - silent fallback would mask JVM distress")
                .isSameAs(outOfMemoryError);

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.ERROR)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                    .contains("workspaceId=42")
                    .contains("trace 1"));
        } finally {
            detachAppender(AiExternalScoreFacadeImpl.class, appender);
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        return provider;
    }

    private static PlatformTransactionManager noopTransactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        return manager;
    }
}
