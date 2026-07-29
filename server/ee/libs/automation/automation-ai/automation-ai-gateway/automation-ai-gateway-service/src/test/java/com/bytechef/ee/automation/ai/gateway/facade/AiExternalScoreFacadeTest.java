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

    /**
     * Pins the cause-preservation contract for {@code loadTrace}. A regression dropping the
     * {@code IllegalArgumentException} cause when wrapping into {@link AiScoreTargetNotFoundException} would let
     * validation bugs (e.g. an internal ID-translation that throws IllegalArgumentException for the wrong reason)
     * surface as ordinary 404 "trace not found" responses, masking the underlying defect. Asserting the cause shape
     * forces a regression to fail this test rather than silently passing.
     */
    @Test
    void testRecordTraceScoreCollapsesNotFoundOntoBoundaryException() {
        // A not-found target on the singleton path must NOT produce a 404 distinguishable from the
        // wrong-workspace 403. Distinguishability lets workspace-A enumerate workspace-B trace ids by probing
        // the 404-vs-403 delta. The facade collapses AiScoreTargetNotFoundException onto
        // AiScoreWorkspaceBoundaryException with a uniform "Caller is not authorized for trace N" message,
        // and the original cause is preserved on the new exception for server-side debugging.
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
        // Span 5 is persisted under trace 7 in workspace 99. The caller claims workspace 42. The facade
        // must re-load the parent trace (NOT trust the span's persisted workspace_id directly) and reject
        // the write — a regression that loads only the span and skips the trace re-fetch would silently
        // allow cross-tenant span scoring.
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
        // assertSameWorkspace uses Objects.equals so a null targetWorkspaceId fails CLOSED — throws the
        // boundary exception with diagnostic context — instead of NPE-ing into a 500. A regression to the
        // wrong-receiver .equals() (e.g., callerWorkspaceId.equals(targetWorkspaceId) on a null target) would
        // flip this to NPE-500 and silently lose the cross-tenant signal.
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        ReflectionTestUtils.setField(trace, "id", 7L);
        // Trace exists but its workspace_id is null — mirrors a hypothetical orphaned trace. getWorkspaceId(...)
        // returns null for that row, and the facade must fail-closed on that path.

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

        // Two traces in the caller's workspace (42).
        AiObservabilityTrace trace1 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace1, "id", 1L);

        AiObservabilityTrace trace2 = new AiObservabilityTrace(AiObservabilityTraceSource.API);
        ReflectionTestUtils.setField(trace2, "id", 2L);

        when(traceService.getTrace(1L)).thenReturn(trace1);
        when(traceService.getTrace(2L)).thenReturn(trace2);
        when(workspaceTraceService.getWorkspaceId(1L)).thenReturn(42L);
        when(workspaceTraceService.getWorkspaceId(2L)).thenReturn(42L);

        // Span belonging to trace 2 (also workspace 42).
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

        // Trace 1 + 3 belong to caller workspace 42; trace 2 belongs to workspace 99 (cross-tenant).
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

        // Only the two valid rows should reach workspaceAiEvalScoreService.createInWorkspace — the cross-workspace row
        // is rejected before
        // persistence.
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
        // Batch path keeps NOT_FOUND distinct from WORKSPACE_BOUNDARY in the structured detail. The singleton
        // path collapses the two to prevent target-existence enumeration via 403-vs-404 probing on the wire,
        // but the batch path returns one aggregate response for the whole submission so the same enumeration
        // attack would require submitting a probing batch and reading the per-row codes — a strictly weaker
        // signal that does not warrant the dashboard-readability cost of collapsing.
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

    /**
     * Pins the cross-tenant ERROR-log emission. The {@code recordBatch} path MUST emit an ERROR-level log line with
     * workspace and target context for every {@link AiScoreWorkspaceBoundaryException} so security ops dashboards see
     * cross-tenant write attempts. Without the inline ERROR log, the batch path silently absorbs boundary breaches into
     * the response body — the only signal a real attack would produce. A regression that downgrades to WARN/INFO or
     * removes context tags breaks this test.
     */
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

            // ERROR-level log MUST contain workspace and target context. WARN/INFO is insufficient — security
            // dashboards filter on level, and a downgrade hides cross-tenant attempts from the only people who
            // can act on them. Asserting hasSize(1) (not anySatisfy) catches a regression that double-logs the
            // same event at ERROR — duplicates inflate dashboards and force operators to deduplicate by hand.
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

    /**
     * Span-path counterpart to {@link #testRecordBatchEmitsErrorLogOnCrossWorkspace}. The trace-path was already
     * pinned; without a span-path test, a regression that downgrades the recordSpanScore / batch-span-target ERROR log
     * to WARN slips through CI. Span-path attempts are the more common cross-tenant probe shape (an attacker trying to
     * attach a poisoned score to another workspace's span), so the dashboard-visibility invariant is load-bearing here
     * too.
     */
    @Test
    void testRecordSpanScoreEmitsErrorLogOnCrossWorkspace() {
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService = mock(WorkspaceAiEvalScoreService.class);
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);

        // Span belongs to trace 11, which belongs to workspace 99 — caller is workspace 42, so the cross-tenant
        // boundary fires from the loadSpan workspace check.
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

    /**
     * Pins the batch-path span-target ERROR log. The trace-target batch case is covered by
     * {@link #testRecordBatchEmitsErrorLogOnCrossWorkspace}; without this companion, a regression in the batch
     * span-resolution path (e.g. switching to a code path that does not re-load the parent trace) can hide cross-tenant
     * attempts at the most attacker-relevant call site (bulk submission).
     */
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
        // Validation runs in AiExternalScoreRequest's compact constructor via ScoreValue.from. The message names
        // the dataType and the runtime class of the offending value but DOES NOT echo the value itself — leaking
        // caller-supplied content into the wire response would let an attacker reflect content via a 400.
        assertThatThrownBy(() -> new AiExternalScoreRequest(
            "faithfulness", "high", AiEvalScoreDataType.NUMERIC, null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUMERIC")
                .hasMessageContaining("String")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("high"));
    }

    @Test
    void testRejectsBooleanScoreWithNonNumericString() {
        // Same fail-closed-on-content rule as the NUMERIC variant: the dataType + runtime class names are useful
        // to the caller; the raw value is not, and including it would echo arbitrary content back via the 400.
        assertThatThrownBy(() -> new AiExternalScoreRequest(
            "relevance", "yes", AiEvalScoreDataType.BOOLEAN, null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOOLEAN")
                .hasMessageContaining("String")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("yes"));
    }

    /**
     * Pins the JVM-Error catch arm in {@code recordBatch}. Mirrors the OTLP ingest facade's discipline: a
     * {@link OutOfMemoryError} (or any {@link Error}) thrown mid-batch must increment the rejected counter and add a
     * {@code BATCH_INTERNAL_ERROR} detail row before propagating, so any caller inspecting partial state through a
     * debugger or wrapping catch sees a self-consistent count up to the failure. A regression that swallows the Error
     * (returning a partial result instead of rethrowing) or skips the pre-throw {@code rejectedCount++} would silently
     * desync accepted/rejected counts from {@code items.size()} under JVM distress — and would pass every other test.
     */
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
                .as("Error must propagate to the caller — silent fallback would mask JVM distress")
                .isSameAs(outOfMemoryError);

            // Pin the BATCH_INTERNAL_ERROR rejection log so a regression that drops the rejection detail
            // (or downgrades it from ERROR) fails this test instead of silently inflating the dashboard.
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

    /**
     * Returns a transaction manager that simply runs the callback inline (single-threaded), with no real transactional
     * semantics — sufficient for unit tests that mock the persistence layer. RuntimeExceptions thrown by the callback
     * propagate to the caller exactly as the production TransactionTemplate would.
     */
    private static PlatformTransactionManager noopTransactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        // commit / rollback are void — Mockito stubs them to no-op by default.
        return manager;
    }
}
