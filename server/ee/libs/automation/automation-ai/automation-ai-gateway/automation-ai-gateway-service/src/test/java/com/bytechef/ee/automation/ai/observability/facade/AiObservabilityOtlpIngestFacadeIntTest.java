/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityExportJobService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityNotificationChannelService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookSubscriptionService;
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
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.platform.ai.observability.facade.OtlpIngestResult;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for OTLP trace ingestion end-to-end: {@link AiObservabilityOtlpIngestFacadeImpl} wiring through
 * trace / span services and resolving cost via {@code OtlpCostResolver} with a real PostgreSQL backend via
 * Testcontainers. Verifies the persistence path claimed by Task 12 of the OTel ingestion plan — a protobuf-parsed
 * {@link OtelSpanBatch} produces an {@code ai_observability_trace} row with {@code source = OTLP} plus a linked
 * {@code ai_observability_span} row.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import({
    AiObservabilityOtlpIngestFacadeImpl.class, PostgreSQLContainerConfiguration.class
})
@AiGatewayIntTestConfigurationSharedMocks
public class AiObservabilityOtlpIngestFacadeIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade;

    @Autowired
    private WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;

    @Autowired
    private WorkspaceAiObservabilityExportJobService workspaceAiObservabilityExportJobService;

    @Autowired
    private WorkspaceAiObservabilityNotificationChannelService workspaceAiObservabilityNotificationChannelService;

    @Autowired
    private WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;

    @Autowired
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Test
    public void testIngestPersistsTraceAndSpan() {
        String externalTraceId = uniqueTraceId();

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            externalTraceId, uniqueSpanId(), null, "chat gpt-4o",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 120L,
                "gen_ai.usage.output_tokens", 64L)),
            OtelResourceAttributes.of(Map.of()),
            "hello",
            "hi there");

        OtlpIngestResult result =
            aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID, new OtelSpanBatch(List.of(span), 0, 0));

        assertThat(result.acceptedSpans()).isEqualTo(1);
        assertThat(result.rejectedSpans()).isZero();

        Optional<AiObservabilityTrace> persistedTrace =
            workspaceAiObservabilityTraceService.findByExternalTraceId(WORKSPACE_ID, externalTraceId);

        assertThat(persistedTrace)
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("externalTraceId", externalTraceId)
            .hasFieldOrPropertyWithValue("source", AiObservabilityTraceSource.OTLP);

        Long traceId = persistedTrace.get()
            .getId();

        assertThat(workspaceAiObservabilityTraceService.getWorkspaceId(traceId)).isEqualTo(WORKSPACE_ID);

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(spans)
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("provider", "openai")
            .hasFieldOrPropertyWithValue("model", "gpt-4o")
            .hasFieldOrPropertyWithValue("inputTokens", 120)
            .hasFieldOrPropertyWithValue("outputTokens", 64)
            .hasFieldOrPropertyWithValue("traceId", traceId);
    }

    @Test
    public void testIngestFoldsRepeatedExportsIntoSingleTrace() {
        String externalTraceId = uniqueTraceId();

        OtelGenAiSpan firstSpan = buildSpan(externalTraceId, uniqueSpanId());
        OtelGenAiSpan secondSpan = buildSpan(externalTraceId, uniqueSpanId());

        aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID, new OtelSpanBatch(List.of(firstSpan), 0, 0));
        aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID, new OtelSpanBatch(List.of(secondSpan), 0, 0));

        Optional<AiObservabilityTrace> persistedTrace =
            workspaceAiObservabilityTraceService.findByExternalTraceId(WORKSPACE_ID, externalTraceId);

        assertThat(persistedTrace).isPresent();

        Long traceId = persistedTrace.get()
            .getId();

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(spans).hasSize(2);
    }

    /**
     * Pins the partial unique index added by changelog
     * {@code 00000000000012_ai_observability_span_add_external_span_id.xml}: re-delivering the SAME (external_trace_id,
     * external_span_id) pair (as an OTLP exporter would on transient batch retry) must NOT double-insert. Without this
     * test, the unit-test mock of DuplicateKeyException covers the catch path but never exercises the actual DB
     * constraint — a regression dropping the index, or building it without {@code WHERE external_span_id IS NOT NULL},
     * would not be caught until production retries start double-counting tokens.
     */
    @Test
    public void testIngestRejectsDuplicateExternalSpanIdViaPartialUniqueIndex() {
        String externalTraceId = uniqueTraceId();
        String externalSpanId = uniqueSpanId();

        OtelGenAiSpan firstDelivery = buildSpan(externalTraceId, externalSpanId);
        OtelGenAiSpan retryDelivery = buildSpan(externalTraceId, externalSpanId);

        OtlpIngestResult firstResult =
            aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID, new OtelSpanBatch(List.of(firstDelivery), 0, 0));
        OtlpIngestResult retryResult =
            aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID, new OtelSpanBatch(List.of(retryDelivery), 0, 0));

        assertThat(firstResult.acceptedSpans()).isEqualTo(1);
        assertThat(firstResult.deduplicatedSpans()).isZero();
        assertThat(firstResult.rejectedSpans()).isZero();

        // Retry: span is bucketed into deduplicated — NOT accepted (would double-count tokens) and NOT rejected
        // (the OTLP retry semantics are explicitly partial-success / idempotent for this case). DEDUPLICATED is
        // a structural bucket separate from accepted so dashboards can reason about stored vs attempted volume
        // without having to subtract dedup counters from accepted-spans.
        assertThat(retryResult.acceptedSpans()).isZero();
        assertThat(retryResult.deduplicatedSpans()).isEqualTo(1);
        assertThat(retryResult.rejectedSpans()).isZero();

        Long traceId = workspaceAiObservabilityTraceService.findByExternalTraceId(WORKSPACE_ID, externalTraceId)
            .orElseThrow()
            .getId();

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(spans).hasSize(1);
    }

    /**
     * Pins the partial-index predicate {@code WHERE external_span_id IS NOT NULL}. A regression building the index
     * without the predicate would block legacy non-OTLP spans (where external_span_id is NULL by design — only OTLP
     * deliveries populate it) from sharing a trace, which would silently break the dual-source observability model
     * (gateway-native traces + OTLP-imported traces in the same trace store). Inserts two same-trace spans both with
     * NULL external_span_id and asserts both persist — the partial predicate must skip the unique check on NULL.
     */
    @Test
    public void testPartialUniqueIndexAllowsMultipleSpansWithNullExternalSpanId() {
        // Use the trace service directly to seed a non-OTLP trace, then write two spans without external ids
        // through the span service. This bypasses the OtelGenAiSpan path (which mandates a non-null span id) but
        // exercises the SAME (trace_id, external_span_id) unique index that the OTLP path relies on — the test
        // is specifically about the partial predicate behavior, not about the OTLP wrapper.
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setExternalTraceId(uniqueTraceId());
        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        AiObservabilitySpan firstSpan = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

        firstSpan.setExternalSpanId(null);
        firstSpan.setName("native-span-1");
        firstSpan.setStartTime(Instant.parse("2026-04-22T10:00:00Z"));
        firstSpan.close(Instant.parse("2026-04-22T10:00:01Z"), AiObservabilitySpanStatus.COMPLETED);

        aiObservabilitySpanService.create(firstSpan);

        AiObservabilitySpan secondSpan = new AiObservabilitySpan(trace.getId(), AiObservabilitySpanType.GENERATION);

        secondSpan.setExternalSpanId(null);
        secondSpan.setName("native-span-2");
        secondSpan.setStartTime(Instant.parse("2026-04-22T10:00:02Z"));
        secondSpan.close(Instant.parse("2026-04-22T10:00:03Z"), AiObservabilitySpanStatus.COMPLETED);

        // Must NOT throw DuplicateKeyException — the partial predicate keeps NULL external ids out of the
        // unique constraint scope. A regression dropping the WHERE clause would surface here as a hard failure.
        aiObservabilitySpanService.create(secondSpan);

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(trace.getId());

        assertThat(spans).hasSize(2);
    }

    @Test
    public void testConcurrentDuplicateIngestSurfacesAsDeduplicatedNotDataIntegrityViolation() throws Exception {
        // Two threads ingesting the same (traceId, externalSpanId) tuple simultaneously must produce exactly one
        // accepted span and one deduplicated span. The unit-level race tests mock the repository sequence; this
        // test pins the contract against the real Postgres unique index, which is what an OTLP exporter retrying
        // mid-ingest would actually hit.
        String externalTraceId = uniqueTraceId();
        String externalSpanId = uniqueSpanId();

        OtelGenAiSpan firstAttempt = buildSpan(externalTraceId, externalSpanId);
        OtelGenAiSpan secondAttempt = buildSpan(externalTraceId, externalSpanId);

        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Callable<OtlpIngestResult> firstTask = () -> {
                start.await();

                return aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID,
                    new OtelSpanBatch(List.of(firstAttempt), 0, 0));
            };
            java.util.concurrent.Callable<OtlpIngestResult> secondTask = () -> {
                start.await();

                return aiObservabilityOtlpIngestFacade.ingest(WORKSPACE_ID,
                    new OtelSpanBatch(List.of(secondAttempt), 0, 0));
            };

            java.util.concurrent.Future<OtlpIngestResult> firstFuture = executor.submit(firstTask);
            java.util.concurrent.Future<OtlpIngestResult> secondFuture = executor.submit(secondTask);

            start.countDown();

            OtlpIngestResult firstResult = firstFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            OtlpIngestResult secondResult = secondFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);

            // Whichever thread won the race must report acceptedSpans=1; the loser must report
            // deduplicatedSpans=1 (NOT rejectedSpans — DataIntegrityViolation here would indicate the dedup
            // index narrowing in the facade did not catch the constraint).
            int totalAccepted = firstResult.acceptedSpans() + secondResult.acceptedSpans();
            int totalDeduplicated = firstResult.deduplicatedSpans() + secondResult.deduplicatedSpans();
            int totalRejected = firstResult.rejectedSpans() + secondResult.rejectedSpans();

            assertThat(totalAccepted).isEqualTo(1);
            assertThat(totalDeduplicated).isEqualTo(1);
            assertThat(totalRejected).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testOverlappingMultiSpanBatchesPartiallyDeduplicate() throws Exception {
        // Realistic OTLP exporter retry shape: two batches that share a subset of (trace, span) ids and each
        // also carry novel ids. Without the trace-level dedup re-fetch, an Optional.empty() observed mid-race
        // could surface DataIntegrityViolation at trace-create time and reject both batches' overlap as
        // REJECTED_PERSIST_FAILED. This test pins the contract: overlapping spans MUST surface as DEDUPLICATED
        // on the loser, novel spans on both batches MUST land as ACCEPTED, and rejectedSpans MUST stay zero.
        String externalTraceId = uniqueTraceId();

        String sharedSpanA = uniqueSpanId();
        String sharedSpanB = uniqueSpanId();
        String firstOnlySpan = uniqueSpanId();
        String secondOnlySpan = uniqueSpanId();

        OtelGenAiSpan firstA = buildSpan(externalTraceId, sharedSpanA);
        OtelGenAiSpan firstB = buildSpan(externalTraceId, sharedSpanB);
        OtelGenAiSpan firstNovel = buildSpan(externalTraceId, firstOnlySpan);

        OtelGenAiSpan secondA = buildSpan(externalTraceId, sharedSpanA);
        OtelGenAiSpan secondB = buildSpan(externalTraceId, sharedSpanB);
        OtelGenAiSpan secondNovel = buildSpan(externalTraceId, secondOnlySpan);

        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Callable<OtlpIngestResult> firstTask = () -> {
                start.await();

                return aiObservabilityOtlpIngestFacade.ingest(
                    WORKSPACE_ID, new OtelSpanBatch(List.of(firstA, firstB, firstNovel), 0, 0));
            };
            java.util.concurrent.Callable<OtlpIngestResult> secondTask = () -> {
                start.await();

                return aiObservabilityOtlpIngestFacade.ingest(
                    WORKSPACE_ID, new OtelSpanBatch(List.of(secondA, secondB, secondNovel), 0, 0));
            };

            java.util.concurrent.Future<OtlpIngestResult> firstFuture = executor.submit(firstTask);
            java.util.concurrent.Future<OtlpIngestResult> secondFuture = executor.submit(secondTask);

            start.countDown();

            OtlpIngestResult firstResult = firstFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            OtlpIngestResult secondResult = secondFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);

            int totalAccepted = firstResult.acceptedSpans() + secondResult.acceptedSpans();
            int totalDeduplicated = firstResult.deduplicatedSpans() + secondResult.deduplicatedSpans();
            int totalRejected = firstResult.rejectedSpans() + secondResult.rejectedSpans();

            // 4 unique (trace, span) tuples across both batches: sharedA, sharedB, firstOnly, secondOnly.
            // Each unique tuple lands exactly once → 4 accepted total.
            assertThat(totalAccepted).isEqualTo(4);

            // Two duplicate attempts (sharedA and sharedB on the loser side) → 2 deduplicated total.
            assertThat(totalDeduplicated).isEqualTo(2);

            // Critical: the trace-level race must surface as DEDUPLICATED, not as REJECTED with
            // DataIntegrityViolation. A regression dropping the dedup re-fetch would surface here.
            assertThat(totalRejected).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private static OtelGenAiSpan buildSpan(String traceId, String spanId) {
        return OtelGenAiSpan.ofHex(
            traceId, spanId, null, "chat",
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:01Z"),
            OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of(
                "gen_ai.system", "openai",
                "gen_ai.request.model", "gpt-4o",
                "gen_ai.usage.input_tokens", 10L,
                "gen_ai.usage.output_tokens", 5L)),
            OtelResourceAttributes.of(Map.of()),
            null, null);
    }

    /** 32-char lowercase-hex trace id (canonical OTel format), seeded from a UUID for uniqueness. */
    private static String uniqueTraceId() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "");
    }

    /** 16-char lowercase-hex span id (canonical OTel format), derived from a fresh UUID. */
    private static String uniqueSpanId() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 16);
    }
}
