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

package com.bytechef.platform.job.sync.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
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
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult.Outcome;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SyncTaskExecutor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration-style, fully in-memory (no Testcontainers) test that drives {@link WorkflowSimulationFacadeImpl} through
 * a real {@link JobSyncExecutor}.
 *
 * @author Ivica Cardic
 */
class WorkflowSimulationFacadeIntTest {

    private static final String TENANT = "public";

    private WorkflowSimulationFacade workflowSimulationFacade;

    @BeforeEach
    void beforeEach() {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId(TENANT);

        Evaluator evaluator = SpelEvaluator.create();

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        JobService jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

        WorkflowService workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), List.of(),
            List.of(
                new ClassPathResourceWorkflowRepository(
                    "workflows/**/*.{json|yml|yaml}", new PathMatchingResourcePatternResolver())));

        Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
            "test/v1/produce", taskExecution -> Map.of("value", "hello"),
            "test/v1/consume", taskExecution -> null);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, new AsyncMessageBroker(new StandardEnvironment()), List.of(),
            taskExecutionService, new SyncTaskExecutor(), taskHandlerMap::get, taskFileStorage, 60L, workflowService);

        workflowSimulationFacade = new WorkflowSimulationFacadeImpl(jobSyncExecutor, taskExecutionService);
    }

    @Test
    void testSimulateCompletes() {
        WorkflowSimulationResult result = workflowSimulationFacade.simulate(
            EncodingUtils.base64EncodeToString("simComplete"), Map.of());

        assertThat(result.outcome()).isEqualTo(Outcome.COMPLETED);
        assertThat(result.failedTaskName()).isNull();
        assertThat(result.failedTaskType()).isNull();
        assertThat(result.reason()).isNull();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void testSimulateReportsFailedTask() {
        WorkflowSimulationResult result = workflowSimulationFacade.simulate(
            EncodingUtils.base64EncodeToString("simFailed"), Map.of());

        assertThat(result.outcome()).isEqualTo(Outcome.FAILED);
        assertThat(result.failedTaskName()).isEqualTo("brokenTask");
        assertThat(result.failedTaskType()).isEqualTo("test/v1/missingStep");
        assertThat(result.reason()).isNotNull();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void testSimulateReportsGenericReasonWhenNoFailedTask() {
        // A timeout/cancel yields a non-COMPLETED job with no FAILED task execution and no job error. The facade must
        // still report FAILED with a generic, status-bearing reason rather than a null one.
        Job job = new Job(1L);

        job.setStatus(Job.Status.STOPPED);

        JobSyncExecutor jobSyncExecutor = Mockito.mock(JobSyncExecutor.class);

        Mockito.when(jobSyncExecutor.execute(any(), anyBoolean()))
            .thenReturn(job);

        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(
            new InMemoryTaskExecutionRepository());

        WorkflowSimulationFacade facade = new WorkflowSimulationFacadeImpl(jobSyncExecutor, taskExecutionService);

        WorkflowSimulationResult result = facade.simulate(EncodingUtils.base64EncodeToString("simTimeout"), Map.of());

        assertThat(result.outcome()).isEqualTo(Outcome.FAILED);
        assertThat(result.failedTaskName()).isNull();
        assertThat(result.failedTaskType()).isNull();
        assertThat(result.reason()).isEqualTo("Workflow did not complete (status=STOPPED)");
        assertThat(result.warnings()).isEmpty();
    }
}
