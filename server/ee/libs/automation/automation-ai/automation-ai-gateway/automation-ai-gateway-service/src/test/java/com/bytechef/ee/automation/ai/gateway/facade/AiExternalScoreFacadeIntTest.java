/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreValue;
import com.bytechef.ee.platform.ai.eval.repository.AiEvalScoreRepository;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchItem;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.facade.RejectionCode;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Facade-level integration test for external eval score ingestion. Exercises {@link AiExternalScoreFacadeImpl}
 * end-to-end against a real PostgreSQL backend via Testcontainers — a happy-path write lands a row in
 * {@code ai_eval_score} with {@code source = EXTERNAL}, the caller-provided source identifier, and serialized metadata,
 * while a cross-workspace call surfaces {@link AiScoreWorkspaceBoundaryException} before any write occurs. Covers Task
 * 9 of the Spec B External Scores API plan.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import({
    AiExternalScoreFacadeImpl.class, PostgreSQLContainerConfiguration.class
})
@AiGatewayIntTestConfigurationSharedMocks
public class AiExternalScoreFacadeIntTest {

    private static final Long WORKSPACE_ID = 42L;
    private static final Long OTHER_WORKSPACE_ID = 99L;

    @Autowired
    private AiExternalScoreFacade aiExternalScoreFacade;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiEvalScoreRepository aiEvalScoreRepository;

    @Autowired
    private com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService workspaceAiEvalScoreService;

    @Test
    public void testRecordTraceScorePersistsExternalRow() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setExternalTraceId("int-test-trace-" + System.nanoTime());

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", new BigDecimal("0.87"), AiEvalScoreDataType.NUMERIC,
            "verified", "ragas@0.2.3", Map.of("run_id", "abc123"));

        AiExternalScoreResult result = aiExternalScoreFacade.recordTraceScore(WORKSPACE_ID, traceId, request);

        assertThat(result).isInstanceOf(AiExternalScoreResult.Accepted.class);

        Long scoreId = ((AiExternalScoreResult.Accepted) result).scoreId();

        assertThat(scoreId).isNotNull();

        Optional<AiEvalScore> persisted = aiEvalScoreRepository.findById(scoreId);

        assertThat(persisted).isPresent();

        AiEvalScore score = persisted.get();

        assertThat(workspaceAiEvalScoreService.getWorkspaceId(score.getId())).isEqualTo(WORKSPACE_ID);
        assertThat(score.getTraceId()).isEqualTo(traceId);
        assertThat(score.getName()).isEqualTo("faithfulness");
        assertThat(score.getSource()).isEqualTo(AiEvalScoreSource.EXTERNAL);
        assertThat(score.getDataType()).isEqualTo(AiEvalScoreDataType.NUMERIC);
        assertThat(score.getSourceIdentifier()).isEqualTo("ragas@0.2.3");
        assertThat(score.getComment()).isEqualTo("verified");
        assertThat(score.getMetadata()).contains("abc123");
        assertThat(score.getTypedValue())
            .isInstanceOfSatisfying(AiEvalScoreValue.Numeric.class,
                numeric -> assertThat(numeric.value()).isEqualByComparingTo("0.87"));
    }

    /**
     * Pins the partial-success contract of {@link AiExternalScoreFacadeImpl#recordBatch} against a real
     * {@code JdbcTransactionManager}: a 3-item batch with one cross-workspace row commits the two valid rows and
     * rejects the boundary violation, and the rejected row's persistence is ABSENT from the database. Without this
     * test, a regression dropping the per-row {@code TransactionTemplate} (or accidentally adding
     * {@code @Transactional} to {@code recordBatch}) would silently roll back the entire batch on any single rejection
     * — the unit-test mocked transaction template wouldn't catch it.
     */
    @Test
    public void testRecordBatchPartialSuccessAgainstRealTransactionManager() {
        AiObservabilityTrace ownTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        ownTrace.setExternalTraceId("int-test-batch-own-" + System.nanoTime());
        workspaceAiObservabilityTraceService.createInWorkspace(ownTrace, WORKSPACE_ID);

        Long ownTraceId = Validate.notNull(ownTrace.getId(), "id");

        AiObservabilityTrace foreignTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        foreignTrace.setExternalTraceId("int-test-batch-foreign-" + System.nanoTime());
        workspaceAiObservabilityTraceService.createInWorkspace(foreignTrace, OTHER_WORKSPACE_ID);

        Long foreignTraceId = Validate.notNull(foreignTrace.getId(), "id");

        AiObservabilityTrace anotherOwnTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        anotherOwnTrace.setExternalTraceId("int-test-batch-own2-" + System.nanoTime());
        workspaceAiObservabilityTraceService.createInWorkspace(anotherOwnTrace, WORKSPACE_ID);

        Long anotherOwnTraceId = Validate.notNull(anotherOwnTrace.getId(), "id");

        // 3-item batch: row 0 → own trace (should commit); row 1 → foreign trace (should reject as
        // WORKSPACE_BOUNDARY); row 2 → own trace (should commit even after the row-1 rejection).
        AiExternalScoreBatchItem rowZero = new AiExternalScoreBatchItem(
            ownTraceId, null, "faithfulness", new BigDecimal("0.95"), AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of());
        AiExternalScoreBatchItem rowOne = new AiExternalScoreBatchItem(
            foreignTraceId, null, "faithfulness", new BigDecimal("0.50"), AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of());
        AiExternalScoreBatchItem rowTwo = new AiExternalScoreBatchItem(
            anotherOwnTraceId, null, "relevance", Boolean.TRUE, AiEvalScoreDataType.BOOLEAN,
            null, "internal-judge", Map.of());

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(List.of(rowZero, rowOne, rowTwo));

        AiExternalScoreBatchResult result = aiExternalScoreFacade.recordBatch(WORKSPACE_ID, request);

        // Counters: 2 committed, 1 rejected, 0 listener failures, exactly 1 rejection detail.
        assertThat(result.acceptedCount()).isEqualTo(2);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.listenerFailedCount()).isZero();
        assertThat(result.rejectionReasons()).hasSize(1);
        assertThat(result.rejectionReasons()
            .getFirst()
            .code()).isEqualTo(RejectionCode.WORKSPACE_BOUNDARY);

        // DB-side: each own-trace row produced exactly one ai_eval_score row; the foreign-trace row produced none.
        assertThat(aiEvalScoreRepository.findAllByTraceId(ownTraceId)).hasSize(1);
        assertThat(aiEvalScoreRepository.findAllByTraceId(anotherOwnTraceId)).hasSize(1);
        assertThat(aiEvalScoreRepository.findAllByTraceId(foreignTraceId)).isEmpty();
    }

    @Test
    public void testRecordTraceScoreRejectsCrossWorkspace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setExternalTraceId("int-test-cross-ws-" + System.nanoTime());

        workspaceAiObservabilityTraceService.createInWorkspace(trace, OTHER_WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiExternalScoreRequest request = new AiExternalScoreRequest(
            "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of());

        assertThatThrownBy(() -> aiExternalScoreFacade.recordTraceScore(WORKSPACE_ID, traceId, request))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class);

        assertThat(aiEvalScoreRepository.findAllByTraceId(traceId)).isEmpty();
    }
}
