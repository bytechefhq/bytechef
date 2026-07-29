/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.test.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationFacade;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationFacadeImpl;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult.Outcome;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SyncTaskExecutor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end regression proving that the {@link SimulationTaskDispatcherPreSendProcessor} wired into the simulation
 * {@link JobSyncExecutor} (mirroring the {@code workflowSimulationFacade} bean) actually propagates the
 * {@link MetadataConstants#DRY_RUN} flag from the job metadata onto each task execution, so a DRY_RUN-aware handler
 * short-circuits instead of making a real call.
 *
 * @author Ivica Cardic
 */
class SimulationDryRunPropagationTest {

    private static final String TENANT = "public";

    private final AtomicReference<String> markerReference = new AtomicReference<>();

    private ObjectMapper objectMapper;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId(TENANT);

        markerReference.set(null);

        workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), List.of(),
            List.of(
                new ClassPathResourceWorkflowRepository(
                    "workflows/**/*.{json|yml|yaml}", new PathMatchingResourcePatternResolver())));
    }

    @Test
    void testDryRunFlagReachesHandlerWhenProcessorWired() {
        WorkflowSimulationFacade facade = createFacade(true);

        WorkflowSimulationResult result = facade.simulate(
            EncodingUtils.base64EncodeToString("simDryRun"), Map.of());

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETED);
        assertThat(markerReference.get()).isEqualTo("SIMULATED");
    }

    @Test
    void testDryRunFlagMissingWithoutProcessor() {
        WorkflowSimulationFacade facade = createFacade(false);

        WorkflowSimulationResult result = facade.simulate(
            EncodingUtils.base64EncodeToString("simDryRun"), Map.of());

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETED);
        assertThat(markerReference.get()).isEqualTo("REAL");
    }

    private WorkflowSimulationFacade createFacade(boolean wireProcessor) {
        Evaluator evaluator = SpelEvaluator.create();

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        JobService jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors = wireProcessor
            ? List.of(new SimulationTaskDispatcherPreSendProcessor(jobService))
            : List.of();

        TaskHandler<?> markerTaskHandler = taskExecution -> {
            boolean dryRun = MapUtils.getBoolean(
                taskExecution.getMetadata(), MetadataConstants.DRY_RUN, false);

            markerReference.set(dryRun ? "SIMULATED" : "REAL");

            return Map.of("marker", markerReference.get());
        };

        Map<String, TaskHandler<?>> taskHandlerMap = Map.of("test/v1/marker", markerTaskHandler);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, new AsyncMessageBroker(new StandardEnvironment()),
            taskDispatcherPreSendProcessors, taskExecutionService, new SyncTaskExecutor(), taskHandlerMap::get,
            taskFileStorage, 60L, workflowService);

        return new WorkflowSimulationFacadeImpl(jobSyncExecutor, taskExecutionService);
    }
}
