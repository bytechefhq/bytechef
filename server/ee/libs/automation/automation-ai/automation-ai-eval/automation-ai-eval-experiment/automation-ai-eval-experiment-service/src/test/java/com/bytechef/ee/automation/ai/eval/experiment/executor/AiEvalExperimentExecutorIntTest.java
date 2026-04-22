/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.config.AiEvalExperimentIntTestConfiguration;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.scheduler.AlertScheduler;
import com.bytechef.platform.scheduler.ExportScheduler;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Persistence-boundary integration test for the experiment lifecycle. Drives the same shape of writes that
 * {@link AiEvalExperimentExecutor#execute(long)} produces — markRunning → per-item run create → run complete →
 * markFinished — but invokes the lifecycle service methods directly rather than going through {@code execute()}. This
 * decouples the test from the {@code @Async} dispatch (which has no test-classpath {@code @EnableAsync} once the
 * project-wide {@code AsyncConfiguration} is excluded), eliminating a flaky pool-warmup race that has no production
 * analog.
 *
 * <p>
 * Coverage split:
 * <ul>
 * <li>{@code AiEvalExperimentExecutorTest} (unit): pins the executor's internal control flow — replayItem branching,
 * markRunning recovery, stop-poll cadence, retry-template wiring, eval dispatch.</li>
 * <li>{@code AiEvalExperimentExecutorAsyncProxyTest} (unit): pins the {@code @Async("aiEvalExperimentTaskExecutor")}
 * proxy wiring + named-pool qualifier reflectively.</li>
 * <li>This test (integration): pins that the lifecycle writes round-trip through real Postgres + real Liquibase
 * schemas, including the {@code experiment_run.trace_id / latency_ms / cost} columns, the
 * {@code experiment.started_date / completed_date} timestamps, and FK relationships (run → experiment, run →
 * dataset_item).</li>
 * </ul>
 *
 * <p>
 * Mocks below ({@code AiEvalExecutor}, {@code AiGatewayFacade}, {@code AiObservabilityTraceService}, etc.) exist purely
 * so the component-scanned {@code AiEvalExperimentExecutor} bean can wire — the executor itself is not invoked by this
 * test, so the mocks are never called.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiEvalExperimentIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    AiEvalExecutor.class, AiGatewayChatModelFactory.class, AiGatewayEmbeddingModelFactory.class,
    AiGatewayFacade.class, AiGatewayMetrics.class, AiObservabilityTraceService.class, AlertScheduler.class,
    ExportScheduler.class, FileStorageService.class, PropertyService.class
})
public class AiEvalExperimentExecutorIntTest {

    private static final Long WORKSPACE_ID = 42L;

    @Autowired
    private AiEvalDatasetItemService aiEvalDatasetItemService;

    @Autowired
    private WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;

    @Autowired
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @Autowired
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Autowired
    private AiEvalExperimentService aiEvalExperimentService;

    @Autowired
    private WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @Test
    public void testExperimentLifecyclePersistsThroughTerminalState() {
        AiEvalDataset dataset = new AiEvalDataset("int-test-dataset");
        AiEvalDataset createdDataset = workspaceAiEvalDatasetService.createInWorkspace(dataset, WORKSPACE_ID);

        String jsonInput = "{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]," +
            "\"stream\":false}";

        AiEvalDatasetItem firstItem = aiEvalDatasetItemService.addItem(createdDataset.getId(), jsonInput, null, null);
        AiEvalDatasetItem secondItem = aiEvalDatasetItemService.addItem(createdDataset.getId(), jsonInput, null, null);

        AiEvalDatasetVersion version = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(createdDataset.getId());

        AiEvalExperiment experiment = new AiEvalExperiment(version.getId());

        experiment.setModel("gpt-4o");

        AiEvalExperiment createdExperiment =
            workspaceAiEvalExperimentService.createInWorkspace(experiment, WORKSPACE_ID);

        assertThat(createdExperiment.getId()).isNotNull();
        assertThat(createdExperiment.getStatus()).isEqualTo(AiEvalExperimentStatus.PENDING);

        // Lifecycle step 1: PENDING → RUNNING (mirrors AiEvalExperimentExecutor.execute's first markRunning call).
        AiEvalExperiment running = aiEvalExperimentService.markRunning(createdExperiment.getId());

        assertThat(running.getStatus()).isEqualTo(AiEvalExperimentStatus.RUNNING);
        assertThat(running.getStartedDate()).isNotNull();

        // Lifecycle step 2: pre-create one experiment_run row per dataset item in PENDING state.
        AiEvalExperimentRun firstRun = aiEvalExperimentRunService.create(
            new AiEvalExperimentRun(createdExperiment.getId(), firstItem.getId()));
        AiEvalExperimentRun secondRun = aiEvalExperimentRunService.create(
            new AiEvalExperimentRun(createdExperiment.getId(), secondItem.getId()));

        assertThat(firstRun.getId()).isNotNull();
        assertThat(secondRun.getId()).isNotNull();

        // Lifecycle step 3: drive each run through PENDING → RUNNING → COMPLETED with synthetic trace metadata.
        // The synthetic traceIds (1_000L, 1_001L) stand in for the real AiObservabilityTrace rows that the
        // gateway facade would persist in production — the FK from experiment_run.trace_id to ai_observability_trace
        // is informational (no DB-level FK constraint), so a synthetic id round-trips cleanly through the schema.
        long firstTraceId = 1_000L;
        long secondTraceId = 1_001L;

        aiEvalExperimentRunService.markRunning(firstRun.getId());

        AiEvalExperimentRun firstCompleted = aiEvalExperimentRunService.complete(
            firstRun.getId(), firstTraceId, 100, new BigDecimal("0.001"));

        aiEvalExperimentRunService.markRunning(secondRun.getId());

        AiEvalExperimentRun secondCompleted = aiEvalExperimentRunService.complete(
            secondRun.getId(), secondTraceId, 200, new BigDecimal("0.002"));

        assertThat(firstCompleted.getStatus()).isEqualTo(AiEvalExperimentRunStatus.COMPLETED);
        assertThat(firstCompleted.getTraceId()).isEqualTo(firstTraceId);
        assertThat(firstCompleted.getLatencyMs()).isEqualTo(100);
        assertThat(firstCompleted.getCost()).isEqualByComparingTo("0.001");

        assertThat(secondCompleted.getStatus()).isEqualTo(AiEvalExperimentRunStatus.COMPLETED);
        assertThat(secondCompleted.getTraceId()).isEqualTo(secondTraceId);
        assertThat(secondCompleted.getLatencyMs()).isEqualTo(200);
        assertThat(secondCompleted.getCost()).isEqualByComparingTo("0.002");

        // Lifecycle step 4: RUNNING → COMPLETED. The {@code anyFailed=false} arg drives the success branch of
        // {@link AiEvalExperiment#complete()} which checks the stop-requested flag — co-existence of stopRequested=true
        // and status=COMPLETED is rejected at the domain layer.
        AiEvalExperiment completed = aiEvalExperimentService.markFinished(createdExperiment.getId(), false);

        assertThat(completed.getStatus()).isEqualTo(AiEvalExperimentStatus.COMPLETED);
        assertThat(completed.getStartedDate()).isNotNull();
        assertThat(completed.getCompletedDate()).isNotNull();
        assertThat(completed.getCompletedDate()).isAfterOrEqualTo(completed.getStartedDate());

        // Cross-check: round-trip the persisted shape through the repository to catch any post-commit drift
        // (Spring Data JDBC's id-population, JdbcTemplate's column mapping, the createdDate / lastModifiedDate
        // auditing fields) that an in-memory assertion on the returned domain object would miss.
        List<AiEvalExperimentRun> persistedRuns =
            aiEvalExperimentRunService.findAllByExperiment(createdExperiment.getId());

        assertThat(persistedRuns).hasSize(2);
        assertThat(persistedRuns).allMatch(run -> run.getStatus() == AiEvalExperimentRunStatus.COMPLETED);
        assertThat(persistedRuns).allMatch(run -> run.getTraceId() != null);
        assertThat(persistedRuns).allMatch(run -> run.getLatencyMs() != null);
        assertThat(persistedRuns).allMatch(run -> run.getCost() != null);
        assertThat(persistedRuns).extracting(AiEvalExperimentRun::getTraceId)
            .containsExactlyInAnyOrder(firstTraceId, secondTraceId);
    }
}
