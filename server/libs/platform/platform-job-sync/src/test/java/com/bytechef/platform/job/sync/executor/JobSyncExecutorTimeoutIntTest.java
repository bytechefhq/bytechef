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

package com.bytechef.platform.job.sync.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
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
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SyncTaskExecutor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class JobSyncExecutorTimeoutIntTest {

    private static final long CONSTRUCTOR_TIMEOUT_SECONDS = 1;
    private static final String TENANT = "public";

    private final CountDownLatch releaseLatch = new CountDownLatch(1);

    private JobService jobService;
    private JobSyncExecutor jobSyncExecutor;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId(TENANT);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

        jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), List.of(),
            List.of(
                new ClassPathResourceWorkflowRepository(
                    "workflows/**/*.{json|yml|yaml}", new PathMatchingResourcePatternResolver())));

        Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
            "test/v1/block", taskExecution -> {
                try {
                    releaseLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();
                }

                return null;
            },
            "test/v1/produce", taskExecution -> Map.of("value", "hello"));

        jobSyncExecutor = new JobSyncExecutor(
            contextService, SpelEvaluator.create(), jobService, -1, new AsyncMessageBroker(new StandardEnvironment()),
            List.of(), taskExecutionService, new SyncTaskExecutor(), taskHandlerMap::get, taskFileStorage,
            CONSTRUCTOR_TIMEOUT_SECONDS, workflowService);
    }

    @AfterEach
    void afterEach() {
        releaseLatch.countDown();
    }

    @Test
    void testPerCallTimeoutFailsJobThatOutlivesIt() {
        long startNanos = System.nanoTime();

        Job job = execute("syncTimeoutBlock", Duration.ofMillis(200));

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(CONSTRUCTOR_TIMEOUT_SECONDS));
    }

    @Test
    void testNullTimeoutFallsBackToConstructorTimeout() {
        long startNanos = System.nanoTime();

        Job job = execute("syncTimeoutBlock", null);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(CONSTRUCTOR_TIMEOUT_SECONDS));
    }

    @Test
    void testJobFinishingWithinTimeoutCompletes() {
        Job job = execute("syncTimeoutFast", Duration.ofSeconds(10));

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);
    }

    private Job execute(String workflowName, @Nullable Duration timeout) {
        String workflowId = EncodingUtils.base64EncodeToString(workflowName);

        return jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Map.of()),
            jobParametersDTO -> jobService.create(jobParametersDTO, workflowService.getWorkflow(workflowId)), false,
            taskExecutionCompleteEvent -> {}, timeout);
    }
}
