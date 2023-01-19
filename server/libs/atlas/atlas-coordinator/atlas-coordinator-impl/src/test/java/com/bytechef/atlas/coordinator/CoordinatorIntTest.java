
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.config.WorkflowConfiguration;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.facade.JobFacade;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.repository.config.WorkflowMapperConfiguration;
import com.bytechef.atlas.repository.jdbc.config.WorkflowRepositoryJdbcConfiguration;
import com.bytechef.atlas.repository.resource.config.ResourceWorkflowRepositoryConfiguration;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bytechef.test.config.jdbc.JdbcRepositoriesIntTestConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    properties = {
        "bytechef.workflow.context-repository.provider=jdbc",
        "bytechef.workflow.persistence.provider=jdbc",
        "bytechef.workflow.workflow-repository.classpath.enabled=true"
    })
public class CoordinatorIntTest {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorIntTest.class);

    @Autowired
    private ContextService contextService;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private SyncMessageBroker messageBroker;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testExecuteWorkflowJson() {
        Job completedJob = executeWorkflow("hello1");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testExecuteWorkflowYaml() {
        Job completedJob = executeWorkflow("hello2");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Coordinator coordinator = new Coordinator(
                null, null, null, jobFacade, jobService, null, null, taskExecutionService);

            coordinator.create(new JobParameters("hello1"));
        });
    }

    public Job executeWorkflow(String workflowId) {
        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        Map<String, TaskHandler<?>> taskHandlerMap = new HashMap<>();

        taskHandlerMap.put("randomHelper/v1/randomInt", taskExecution -> null);

        TaskHandlerResolverChain taskHandlerResolver = new TaskHandlerResolverChain();

        taskHandlerResolver.setTaskHandlerResolvers(List.of(new DefaultTaskHandlerResolver(taskHandlerMap)));

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(taskHandlerResolver)
            .withMessageBroker(messageBroker)
            .withEventPublisher(e -> {})
            .withTaskEvaluator(TaskEvaluator.create())
            .build();

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        DefaultTaskDispatcher taskDispatcher = new DefaultTaskDispatcher(coordinatorMessageBroker, List.of());

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcher, taskExecutionService, TaskEvaluator.create(), workflowService);

        DefaultTaskCompletionHandler taskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, e -> {}, jobExecutor, jobService, TaskEvaluator.create(), taskExecutionService,
            workflowService);

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        Coordinator coordinator = new Coordinator(
            e -> {}, e -> {}, jobExecutor, jobFacade, jobService, taskCompletionHandler,
            (TaskDispatcher) taskDispatcher, taskExecutionService);

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, jobId -> coordinator.start((Long) jobId));

        long jobId = jobFacade.create(new JobParameters(Collections.singletonMap("yourName", "me"), workflowId));

        return jobService.getJob(jobId);
    }

    @EmbeddedSql
    @ComponentScan(basePackages = "com.bytechef.atlas")
    @EnableAutoConfiguration
    @Import({
        ResourceWorkflowRepositoryConfiguration.class,
        WorkflowConfiguration.class,
        WorkflowMapperConfiguration.class,
        WorkflowRepositoryJdbcConfiguration.class,
    })
    @Configuration
    public static class CoordinatorIntTestConfiguration {

        @MockBean
        private EventPublisher eventPublisher;

        @Bean
        SyncMessageBroker messageBroker() {
            return new SyncMessageBroker();
        }

        @EnableCaching
        @TestConfiguration
        public static class CacheConfiguration {
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.repository.jdbc")
        public static class CoordinatorIntJdbcRepositoriesConfiguration extends JdbcRepositoriesIntTestConfiguration {
        }
    }
}
