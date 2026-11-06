/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.gateway.cost.OtlpCostResolver;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelGenAiSpan;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelResourceAttributes;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanAttributes;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.OtlpIngestResult;
import com.bytechef.ee.platform.ai.observability.facade.RejectionCode;
import com.bytechef.ee.platform.ai.observability.facade.RejectionDetail;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityTraceRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiObservabilityOtlpIngestFacadeTest {

    @Test
    void testIngestsSingleGenAiSpan() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        when(workspaceTraceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());
        when(costResolver.computeCost("gpt-4o", 100, 42))
            .thenReturn(new BigDecimal("0.002"));

        doAnswer(invocation -> {
            AiObservabilityTrace created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", 42L);

            return null;
        }).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000001", "0000000000000001", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 100L,
                "gen_ai.usage.output_tokens", 42L)),
            OtelResourceAttributes.of(Map.of()),
            null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        verify(workspaceTraceService, times(1)).createInWorkspace(any(AiObservabilityTrace.class), anyLong());
        verify(spanService, times(1)).create(any(AiObservabilitySpan.class));

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "OK")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testIngestsSpanWithErrorStatus() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        when(workspaceTraceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());
        when(costResolver.computeCost("gpt-4o", 100, 42))
            .thenReturn(new BigDecimal("0.002"));

        doAnswer(invocation -> {
            AiObservabilityTrace created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", 1L);

            return null;
        }).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000002", "0000000000000002", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.ERROR,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 100L,
                "gen_ai.usage.output_tokens", 42L)),
            OtelResourceAttributes.of(Map.of()),
            null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(spanService, times(1)).create(spanCaptor.capture());

        assertThat(spanCaptor.getValue()
            .getStatus()).isEqualTo(AiObservabilitySpanStatus.ERROR);

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "ERROR")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testIngestsSpanWithUnsetStatusAsCompleted() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        when(workspaceTraceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());
        when(costResolver.computeCost("gpt-4o", 100, 42))
            .thenReturn(new BigDecimal("0.002"));

        doAnswer(invocation -> {
            AiObservabilityTrace created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", 1L);

            return null;
        }).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000003", "0000000000000003", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.UNSET,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 100L,
                "gen_ai.usage.output_tokens", 42L)),
            OtelResourceAttributes.of(Map.of()),
            null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(spanService, times(1)).create(spanCaptor.capture());

        assertThat(spanCaptor.getValue()
            .getStatus()).isEqualTo(AiObservabilitySpanStatus.COMPLETED);

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "UNSET")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testPersistSpanTypesRetrieverAsRetrieval() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        when(workspaceTraceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", 42L);

            return null;
        }).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(new SimpleMeterRegistry()));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000010", "0000000000000010", null, "retrieve",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "openinference.span.kind", "RETRIEVER",
                "retrieval.documents.0.document.content", "Paris is the capital of France.")),
            OtelResourceAttributes.of(Map.of()),
            null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(spanService, times(1)).create(spanCaptor.capture());

        AiObservabilitySpan saved = spanCaptor.getValue();

        assertThat(saved.getType()).isEqualTo(AiObservabilitySpanType.RETRIEVAL);
        assertThat(saved.getOutput()).contains("Paris is the capital of France.");
    }

    @Test
    void testPersistSpanDefaultsToGeneration() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        when(workspaceTraceService.findByExternalTraceId(any(), any())).thenReturn(Optional.empty());

        doAnswer(invocation -> {
            AiObservabilityTrace created = invocation.getArgument(0);

            ReflectionTestUtils.setField(created, "id", 43L);

            return null;
        }).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(new SimpleMeterRegistry()));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000011", "0000000000000011", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()),
            null, "some output");

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(spanService, times(1)).create(spanCaptor.capture());

        AiObservabilitySpan saved = spanCaptor.getValue();

        assertThat(saved.getType()).isEqualTo(AiObservabilitySpanType.GENERATION);
        assertThat(saved.getOutput()).isEqualTo("some output");
    }

    @Test
    void testRejectsSpanWithoutSystemAttribute() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(new SimpleMeterRegistry()));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000001", "0000000000000001", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.request.model", "gpt-4o")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(1);

        RejectionDetail detail = result.rejectionReasons()
            .getFirst();

        assertThat(detail.code()).isEqualTo(RejectionCode.REJECTED_NO_SYSTEM);
        assertThat(detail.reason()).contains("gen_ai.system");
        assertThat(detail.index()).isZero();
    }

    @Test
    void testResolvesExistingTraceWhenInsertLosesRaceToConcurrentExporter() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace existing = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(existing, "id", 7L);

        when(workspaceTraceService.findByExternalTraceId(42L, "00000000000000000000000000000004"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existing));
        doThrow(new DuplicateKeyException(
            "duplicate key value violates unique constraint \"uq_ai_obs_trace_ext_trace_id\""))
                .when(workspaceTraceService)
                .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(new SimpleMeterRegistry()));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000004", "0000000000000001", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);

        verify(spanService).create(any(AiObservabilitySpan.class));
    }

    @Test
    void testPoisonPillSpanDoesNotAbortBatch() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace traceA = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);
        AiObservabilityTrace traceB = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(traceA, "id", 100L);
        ReflectionTestUtils.setField(traceB, "id", 200L);

        when(workspaceTraceService.findByExternalTraceId(123L, "00000000000000000000000000000005"))
            .thenReturn(Optional.of(traceA));
        when(workspaceTraceService.findByExternalTraceId(123L, "00000000000000000000000000000006"))
            .thenReturn(Optional.of(traceB));

        doThrow(new DataIntegrityViolationException("not-null constraint violation on some_column"))
            .doNothing()
            .when(spanService)
            .create(any(AiObservabilitySpan.class));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan poisonSpan = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000005", "0000000000000005", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtelGenAiSpan goodSpan = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000006", "0000000000000006", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(123L, new OtelSpanBatch(List.of(poisonSpan, goodSpan), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(1);

        RejectionDetail detail = result.rejectionReasons()
            .getFirst();

        assertThat(detail.code()).isEqualTo(RejectionCode.REJECTED_PERSIST_FAILED);
        assertThat(detail.reason())
            .contains("0000000000000005")
            .contains("not-null constraint violation");
        assertThat(detail.index())
            .as("index of the rejected span within the inbound batch")
            .isZero();

        verify(spanService, times(2)).create(any(AiObservabilitySpan.class));

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "REJECTED_PERSIST_FAILED")
            .count()).isEqualTo(1.0);
        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "OK")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testDuplicateSpanCountedAsDeduplicated() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(existingTrace, "id", 7L);

        when(workspaceTraceService.findByExternalTraceId(42L, "00000000000000000000000000000007"))
            .thenReturn(Optional.of(existingTrace));
        doThrow(new DuplicateKeyException(
            "duplicate key value violates unique constraint \"uq_ai_obs_span_trace_external\""))
                .when(spanService)
                .create(any(AiObservabilitySpan.class));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000007", "0000000000000007", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "DEDUPLICATED")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testNonDuplicateKeyDataIntegrityViolationIsRejected() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(existingTrace, "id", 7L);

        when(workspaceTraceService.findByExternalTraceId(42L, "00000000000000000000000000000008"))
            .thenReturn(Optional.of(existingTrace));
        doThrow(new DataIntegrityViolationException("null value in column \"required_col\""))
            .when(spanService)
            .create(any(AiObservabilitySpan.class));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000008", "0000000000000008", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(1);

        RejectionDetail detail = result.rejectionReasons()
            .getFirst();

        assertThat(detail.code()).isEqualTo(RejectionCode.REJECTED_PERSIST_FAILED);
        assertThat(detail.reason()).contains("required_col");

        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "REJECTED_PERSIST_FAILED")
            .count()).isEqualTo(1.0);
        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "DEDUPLICATED")
            .count()).isZero();
    }

    @Test
    void testEmptyBatchReturnsZeroCountsWithoutCounterIncrement() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(), 0, 0));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isZero();
        assertThat(result.rejectionReasons()).isEmpty();

        assertThat(registry.getMeters())
            .as("Empty batch must not register any meters under bytechef_ai_otlp_*")
            .filteredOn(meter -> meter.getId()
                .getName()
                .startsWith("bytechef_ai_otlp_"))
            .isEmpty();
    }

    @Test
    void testIncrementsDropCounterForNonGenAiSpans() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelSpanBatch batch = new OtelSpanBatch(List.of(), 3, 0);

        facade.ingest(42L, batch);

        assertThat(registry.counter("bytechef_ai_otlp_spans_dropped_non_genai")
            .count()).isEqualTo(3.0);
    }

    @Test
    void testIngestSurvivesBatchWithMultipleMapperRejectedSpans() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelSpanBatch batch = new OtelSpanBatch(List.of(), 0, 4);

        OtlpIngestResult result = facade.ingest(42L, batch);

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(4);
        assertThat(result.rejectionReasons())
            .as("Aggregated breadcrumb: one row, not four identical ones")
            .hasSize(1);
        assertThat(result.rejectionReasons()
            .get(0)
            .code()).isEqualTo(RejectionCode.REJECTED_BY_MAPPER);
        assertThat(registry.counter("bytechef_ai_otlp_spans_ingested", "status", "REJECTED_BY_MAPPER")
            .count())
                .as("Per-span counter must increment N times even when the detail list aggregates")
                .isEqualTo(4.0);
    }

    @Test
    void testIngestMethodMustNotBeTransactional() throws NoSuchMethodException {
        assertThat(AiObservabilityOtlpIngestFacadeImpl.class.isAnnotationPresent(Transactional.class))
            .as(
                "AiObservabilityOtlpIngestFacadeImpl must not have a class-level @Transactional — partial-success" +
                    " ingest depends on per-span transactions")
            .isFalse();

        Method ingestMethod = AiObservabilityOtlpIngestFacadeImpl.class.getMethod(
            "ingest", Long.class, OtelSpanBatch.class);

        assertThat(ingestMethod.isAnnotationPresent(Transactional.class))
            .as(
                "AiObservabilityOtlpIngestFacadeImpl.ingest must not be @Transactional — a poison-pill span would" +
                    " roll back the rest of the batch")
            .isFalse();
    }

    @Test
    void testTraceLevelDedupRaceReFetchesWinner() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace winner = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(winner, "id", 999L);

        when(workspaceTraceService.findByExternalTraceId(42L, "00000000000000000000000000000001"))
            .thenReturn(Optional.empty(), Optional.of(winner));

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException(
            "ERROR: duplicate key value violates unique constraint \"uq_ai_obs_trace_ext_trace_id\"");

        doThrow(duplicateKeyException).when(workspaceTraceService)
            .createInWorkspace(any(AiObservabilityTrace.class), anyLong());

        when(costResolver.computeCost("gpt-4o", 100, 42))
            .thenReturn(new BigDecimal("0.002"));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000001", "0000000000000001", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 100L,
                "gen_ai.usage.output_tokens", 42L)),
            OtelResourceAttributes.of(Map.of()),
            null, null);

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans())
            .as("Race-loser still ingests the span against the winner trace")
            .isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        ArgumentCaptor<AiObservabilitySpan> spanCaptor = ArgumentCaptor.forClass(AiObservabilitySpan.class);

        verify(spanService).create(spanCaptor.capture());
        assertThat(spanCaptor.getValue()
            .getTraceId())
                .as("Span attaches to the re-fetched winner trace, not the failed-create attempt")
                .isEqualTo(999L);
    }

    @Test
    void testRejectionReasonContainsClassNameWhenExceptionMessageIsNull() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(existingTrace, "id", 7L);

        when(workspaceTraceService.findByExternalTraceId(42L, "00000000000000000000000000000099"))
            .thenReturn(Optional.of(existingTrace));
        doThrow(new DataIntegrityViolationException(null))
            .when(spanService)
            .create(any(AiObservabilitySpan.class));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000099", "0000000000000099", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtlpIngestResult result = facade.ingest(42L, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isZero();
        assertThat(result.deduplicatedSpans()).isZero();
        assertThat(result.rejectedSpans()).isEqualTo(1);

        RejectionDetail detail = result.rejectionReasons()
            .getFirst();

        assertThat(detail.code()).isEqualTo(RejectionCode.REJECTED_PERSIST_FAILED);
        assertThat(detail.reason())
            .contains("DataIntegrityViolationException")
            .doesNotContain(": null");
    }

    @Test
    void testPartialSuccessCounterConservationOnMixedBatch() {
        AiObservabilityTraceService traceService = mock(AiObservabilityTraceService.class);
        WorkspaceAiObservabilityTraceService workspaceTraceService = mock(WorkspaceAiObservabilityTraceService.class);
        AiObservabilitySpanService spanService = mock(AiObservabilitySpanService.class);
        OtlpCostResolver costResolver = mock(OtlpCostResolver.class);
        AiObservabilityTraceRepository traceRepository = mock(AiObservabilityTraceRepository.class);

        AiObservabilityTrace existingTrace = new AiObservabilityTrace(AiObservabilityTraceSource.OTLP);

        ReflectionTestUtils.setField(existingTrace, "id", 7L);

        when(workspaceTraceService.findByExternalTraceId(any(), any()))
            .thenReturn(Optional.of(existingTrace));
        when(costResolver.computeCost(any(), any(), any()))
            .thenReturn(new BigDecimal("0.001"));

        doAnswer(invocation -> null)
            .doThrow(new DuplicateKeyException(
                "duplicate key value violates unique constraint \"uq_ai_obs_span_trace_external\""))
            .doThrow(new DataIntegrityViolationException("null value in column \"required_col\""))
            .when(spanService)
            .create(any(AiObservabilitySpan.class));

        MeterRegistry registry = new SimpleMeterRegistry();

        AiObservabilityOtlpIngestFacadeImpl facade = new AiObservabilityOtlpIngestFacadeImpl(
            traceService, workspaceTraceService, spanService, traceRepository,
            mock(AiObservabilitySpanRepository.class), costResolver,
            staticProvider(registry));

        OtelGenAiSpan accepted = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000001", "0000000000000001", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);
        OtelGenAiSpan duplicate = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000002", "0000000000000002", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);
        OtelGenAiSpan rejected = OtelGenAiSpan.ofHex(
            "00000000000000000000000000000003", "0000000000000003", null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            OtelResourceAttributes.of(Map.of()), null, null);

        OtelSpanBatch batch = new OtelSpanBatch(List.of(accepted, duplicate, rejected), 0, 2);

        OtlpIngestResult result = facade.ingest(42L, batch);

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.deduplicatedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans())
            .as("One ingest-time rejection plus two mapper-rejected spans")
            .isEqualTo(3);

        int conservationLhs = result.acceptedSpans() + result.deduplicatedSpans() + result.rejectedSpans();
        int conservationRhs = batch.spans()
            .size() + batch.rejectedSpans();

        assertThat(conservationLhs)
            .as("Conservation invariant: accepted + deduplicated + rejected == batch.spans().size() + " +
                "batch.rejectedSpans(); a regression rebucketing DEDUPLICATED to neither would surface here as " +
                "an arithmetic mismatch")
            .isEqualTo(conservationRhs);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(registry);

        return provider;
    }

}
