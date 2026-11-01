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

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class TaskExecutionServiceIntTest {

    private static final int CONCURRENT_UPDATES = 4;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    private long jobId;

    @BeforeEach
    public void beforeEach() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        jobId = Validate.notNull(job.getId(), "id");
    }

    @Test
    public void testCreate() {
        TaskExecution taskExecution = taskExecutionService.create(newTaskExecution("task1", null, 1));

        assertThat(taskExecution.getId()).isNotNull();
        assertThat(taskExecution.getJobId()).isEqualTo(jobId);
        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.CREATED);
    }

    @Test
    public void testGetTaskExecution() {
        TaskExecution taskExecution = taskExecutionService.create(newTaskExecution("task1", null, 1));

        TaskExecution fetchedTaskExecution = taskExecutionService.getTaskExecution(
            Validate.notNull(taskExecution.getId(), "id"));

        assertThat(fetchedTaskExecution.getName()).isEqualTo("task1");
        assertThat(fetchedTaskExecution.getJobId()).isEqualTo(jobId);
    }

    @Test
    public void testGetJobTaskExecutions() {
        taskExecutionService.create(newTaskExecution("task1", null, 1));
        taskExecutionService.create(newTaskExecution("task2", null, 2));

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        assertThat(taskExecutions).extracting(TaskExecution::getName)
            .containsExactly("task1", "task2");
    }

    @Test
    public void testGetParentTaskExecutions() {
        TaskExecution parentTaskExecution = taskExecutionService.create(newTaskExecution("parent", null, 1));

        long parentTaskExecutionId = Validate.notNull(parentTaskExecution.getId(), "id");

        taskExecutionService.create(newTaskExecution("child2", parentTaskExecutionId, 2));
        taskExecutionService.create(newTaskExecution("child1", parentTaskExecutionId, 1));

        List<TaskExecution> childTaskExecutions = taskExecutionService.getParentTaskExecutions(parentTaskExecutionId);

        assertThat(childTaskExecutions).extracting(TaskExecution::getName)
            .containsExactly("child1", "child2");
    }

    @Test
    public void testUpdate() {
        TaskExecution taskExecution = taskExecutionService.create(newTaskExecution("task1", null, 1));

        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecutionService.update(taskExecution);

        TaskExecution updatedTaskExecution = taskExecutionService.getTaskExecution(
            Validate.notNull(taskExecution.getId(), "id"));

        assertThat(updatedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);
    }

    @Test
    public void testUpdateWithModifierKeepsEveryConcurrentUpdate() throws Exception {
        TaskExecution taskExecution = taskExecutionService.create(newTaskExecution("forkJoin_1", null, 1));

        long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<TaskExecution>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_UPDATES)) {
            for (int index = 0; index < CONCURRENT_UPDATES; index++) {
                String branchName = "branch_" + index;

                futures.add(executorService.submit(() -> {
                    assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();

                    return taskExecutionService.update(
                        taskExecutionId, lockedTaskExecution -> appendBranchName(lockedTaskExecution, branchName));
                }));
            }

            startLatch.countDown();

            for (Future<TaskExecution> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }

        TaskExecution updatedTaskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        FileEntry output = Validate.notNull(updatedTaskExecution.getOutput(), "output");

        assertThat(output.getName()
            .split(",")).containsExactlyInAnyOrder("branch_0", "branch_1", "branch_2", "branch_3");
    }

    private static void appendBranchName(TaskExecution taskExecution, String branchName) {
        FileEntry output = taskExecution.getOutput();

        String branchNames = output == null ? branchName : output.getName() + "," + branchName;

        try {
            Thread.sleep(100);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }

        taskExecution.setOutput(new FileEntry(branchNames, "memory://outputs"));
    }

    private TaskExecution newTaskExecution(String name, Long parentId, int taskNumber) {
        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", name, "type", "test/v1/noop")))
            .build();

        taskExecution.setJobId(jobId);
        taskExecution.setTaskNumber(taskNumber);

        if (parentId != null) {
            taskExecution.setParentId(parentId);
        }

        return taskExecution;
    }

    @ComponentScan(basePackages = "com.bytechef.atlas.configuration.converter")
    @EnableAutoConfiguration
    @Configuration
    public static class TaskExecutionServiceIntTestConfiguration {

        @Bean
        TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
            return new TaskExecutionServiceImpl(taskExecutionRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class TaskExecutionServiceIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI")
            public TaskExecutionServiceIntTestJdbcConfiguration(ObjectMapper objectMapper) {
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
