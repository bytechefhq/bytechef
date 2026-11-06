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
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.configuration.service.PropertyService;
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
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiEvalExperimentIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    AiGatewayChatModelFactory.class, AiGatewayEmbeddingModelFactory.class, AiGatewayFacade.class,
    AiGatewayMetrics.class, FileStorageService.class, PropertyService.class
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

        AiEvalExperiment running = aiEvalExperimentService.markRunning(createdExperiment.getId());

        assertThat(running.getStatus()).isEqualTo(AiEvalExperimentStatus.RUNNING);
        assertThat(running.getStartedDate()).isNotNull();

        AiEvalExperimentRun firstRun = aiEvalExperimentRunService.create(
            new AiEvalExperimentRun(createdExperiment.getId(), firstItem.getId()));
        AiEvalExperimentRun secondRun = aiEvalExperimentRunService.create(
            new AiEvalExperimentRun(createdExperiment.getId(), secondItem.getId()));

        assertThat(firstRun.getId()).isNotNull();
        assertThat(secondRun.getId()).isNotNull();

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

        AiEvalExperiment completed = aiEvalExperimentService.markFinished(createdExperiment.getId(), false);

        assertThat(completed.getStatus()).isEqualTo(AiEvalExperimentStatus.COMPLETED);
        assertThat(completed.getStartedDate()).isNotNull();
        assertThat(completed.getCompletedDate()).isNotNull();
        assertThat(completed.getCompletedDate()).isAfterOrEqualTo(completed.getStartedDate());

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
