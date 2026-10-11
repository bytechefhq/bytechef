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

package com.bytechef.atlas.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class JobFacadeIntTest {

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Test
    public void testRequiredParameters() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> jobFacade.createJob(new JobParametersDTO("aGVsbG8x", Collections.emptyMap())));
    }

    @Test
    public void testDeleteJobRecursivelyDeletesChildJobsAndTaskExecutions() {
        Job parentJob = jobRepository.save(newJob());
        long parentJobId = Validate.notNull(parentJob.getId(), "id");

        TaskExecution parentTaskExecution = taskExecutionRepository.save(newTaskExecution(parentJobId, null));

        long parentTaskExecutionId = Validate.notNull(parentTaskExecution.getId(), "id");

        Job childJob = jobRepository.save(newJob());

        childJob.setParentTaskExecutionId(parentTaskExecutionId);

        jobRepository.save(childJob);

        long childJobId = Validate.notNull(childJob.getId(), "id");

        TaskExecution childTaskExecution = taskExecutionRepository.save(newTaskExecution(childJobId, null));

        long childTaskExecutionId = Validate.notNull(childTaskExecution.getId(), "id");

        jobFacade.deleteJob(parentJobId);

        assertThat(jobRepository.findById(parentJobId)).isEmpty();
        assertThat(jobRepository.findById(childJobId)).isEmpty();
        assertThat(taskExecutionRepository.findById(parentTaskExecutionId)).isEmpty();
        assertThat(taskExecutionRepository.findById(childTaskExecutionId)).isEmpty();
    }

    private static Job newJob() {
        Job job = new Job();

        job.setStatus(Job.Status.COMPLETED);
        job.setWorkflowId("demo:1234");

        return job;
    }

    private static TaskExecution newTaskExecution(long jobId, Long parentId) {
        Map<String, Object> taskMap = new HashMap<>();

        taskMap.put("name", "task1");
        taskMap.put("type", "test/v1/noop");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(taskMap))
            .build();

        taskExecution.setJobId(jobId);
        taskExecution.setTaskNumber(1);

        if (parentId != null) {
            taskExecution.setParentId(parentId);
        }

        return taskExecution;
    }

    @ComponentScan(basePackages = {
        "com.bytechef.atlas.execution.facade", "com.bytechef.atlas.configuration.converter"
    })
    @EnableAutoConfiguration
    @Configuration
    public static class WorkflowExecutionIntTestConfiguration {

        @MockitoBean
        private ContextService contextService;

        @MockitoBean
        private WorkflowService workflowService;

        @MockitoBean
        private TaskFileStorage taskFileStorage;

        @Bean
        JobFacade jobFacade(
            ApplicationEventPublisher eventPublisher, JobService jobService,
            TaskExecutionService taskExecutionService) {

            return new JobFacadeImpl(
                eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);
        }

        @Bean
        JobService jobService(JobRepository jobRepository) {
            return new JobServiceImpl(jobRepository);
        }

        @Bean
        TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
            return new TaskExecutionServiceImpl(taskExecutionRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class WorkflowIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI")
            public WorkflowIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new ExecutionErrorToStringConverter(objectMapper),
                    new FileEntryToStringConverter(objectMapper),
                    new MapWrapperToStringConverter(objectMapper),
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
