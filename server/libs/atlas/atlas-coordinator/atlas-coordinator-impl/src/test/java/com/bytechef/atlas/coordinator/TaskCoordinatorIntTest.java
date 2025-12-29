/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.classpath.enabled=true"
    })
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class TaskCoordinatorIntTest {

    private static final TaskFileStorage TASK_FILE_STORAGE = new TaskFileStorageImpl(new Base64FileStorageService());

    private final Evaluator evaluator = SpelEvaluator.create();

    @Autowired
    private ContextService contextService;

    @Autowired
    private Environment environment;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testPerformWorkflowJson() {
        Job completedJob = executeWorkflow("aGVsbG8x");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testPerformWorkflowYaml() {
        Job completedJob = executeWorkflow("aGVsbG8y");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    private Job executeWorkflow(String workflowId) {
        Map<String, TaskHandler<?>> taskHandlerMap = new HashMap<>();

        taskHandlerMap.put("randomHelper/v1/randomInt", taskExecution -> null);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, role -> new AsyncMessageBroker(environment), List.of(),
            taskExecutionService,
            new TaskExecutor() {
                private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

                @Override
                public void execute(Runnable task) {
                    executor.execute(task);
                }
            }, taskHandlerMap::get, TASK_FILE_STORAGE, -1, workflowService);

        return jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Collections.singletonMap("yourName", "me")), true);
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
    })
    @Configuration
    public static class CoordinatorIntTestConfiguration {

        @Bean
        ClassPathResourceWorkflowRepository classPathResourceWorkflowRepository(
            ResourcePatternResolver resourcePatternResolver) {

            return new ClassPathResourceWorkflowRepository("workflows/**/*.{json|yml|yaml}", resourcePatternResolver);
        }

        @Bean
        ContextService contextService(JdbcContextRepository jdbcContextRepository) {
            return new ContextServiceImpl(jdbcContextRepository);
        }

        @Bean
        JobService jobService(JdbcJobRepository jdbcJobRepository) {
            return new JobServiceImpl(jdbcJobRepository);
        }

        @Bean
        TaskExecutionService taskExecutionService(JdbcTaskExecutionRepository jdbcTaskExecutionRepository) {
            return new TaskExecutionServiceImpl(jdbcTaskExecutionRepository);
        }

        @Bean
        WorkflowService workflowService(
            List<WorkflowCrudRepository> workflowCrudRepositories, List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(
                new ConcurrentMapCacheManager(), workflowCrudRepositories, workflowRepositories);
        }

        @EnableJdbcRepositories(basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.atlas.execution.repository.jdbc"
        })
        public static class CoordinatorIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            public CoordinatorIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new ExecutionErrorToStringConverter(objectMapper),
                    new FileEntryToStringConverter(objectMapper),
                    new MapWrapperToStringConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper),
                    new StringToFileEntryConverter(objectMapper),
                    new StringToMapWrapperConverter(objectMapper),
                    new StringToWebhooksConverter(objectMapper),
                    new StringToWorkflowTaskConverter(objectMapper),
                    new WebhooksToStringConverter(objectMapper),
                    new WorkflowTaskToStringConverter(objectMapper));
            }
        }
    }
}
