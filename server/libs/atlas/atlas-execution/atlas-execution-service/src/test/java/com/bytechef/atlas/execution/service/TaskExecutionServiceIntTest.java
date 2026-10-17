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
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
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

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Test
    public void testUpdateIncrementsVersion() {
        TaskExecution taskExecution = createTaskExecution(Map.of());

        int version = taskExecution.getVersion();

        taskExecution.setProgress(50);

        TaskExecution updatedTaskExecution = taskExecutionService.update(taskExecution);

        assertThat(updatedTaskExecution.getVersion()).isEqualTo(version + 1);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(getId(taskExecution));

        assertThat(storedTaskExecution.getProgress()).isEqualTo(50);
        assertThat(storedTaskExecution.getVersion()).isEqualTo(version + 1);
    }

    @Test
    public void testUpdateOfStaleCopyKeepsMetadataWrittenConcurrently() {
        long id = getId(createTaskExecution(Map.of()));

        TaskExecution firstCopy = taskExecutionService.getTaskExecution(id);
        TaskExecution secondCopy = taskExecutionService.getTaskExecution(id);

        secondCopy.putMetadata("approvalId", "approval-1");

        taskExecutionService.update(secondCopy);

        firstCopy.putMetadata("resumeId", "resume-1");
        firstCopy.setStatus(TaskExecution.Status.COMPLETED);

        taskExecutionService.update(firstCopy);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(id);

        assertThat(storedTaskExecution.getMetadata()).containsEntry("approvalId", "approval-1")
            .containsEntry("resumeId", "resume-1");
        assertThat(storedTaskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
        assertThat(storedTaskExecution.getVersion()).isEqualTo(firstCopy.getVersion());
    }

    @Test
    public void testUpdateOfStaleCopyDropsMetadataTheCopyRemoved() {
        long id = getId(createTaskExecution(Map.of("resumeId", "resume-1")));

        TaskExecution firstCopy = taskExecutionService.getTaskExecution(id);
        TaskExecution secondCopy = taskExecutionService.getTaskExecution(id);

        secondCopy.putMetadata("approvalId", "approval-1");

        taskExecutionService.update(secondCopy);

        firstCopy.removeMetadata("resumeId");

        taskExecutionService.update(firstCopy);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(id);

        assertThat(storedTaskExecution.getMetadata()).containsEntry("approvalId", "approval-1")
            .doesNotContainKey("resumeId");
    }

    @Test
    public void testUpdateOfStaleCopyCanBeUpdatedAgain() {
        long id = getId(createTaskExecution(Map.of()));

        TaskExecution firstCopy = taskExecutionService.getTaskExecution(id);
        TaskExecution secondCopy = taskExecutionService.getTaskExecution(id);

        taskExecutionService.update(secondCopy);
        taskExecutionService.update(firstCopy);

        firstCopy.setProgress(100);

        taskExecutionService.update(firstCopy);

        TaskExecution storedTaskExecution = taskExecutionService.getTaskExecution(id);

        assertThat(storedTaskExecution.getProgress()).isEqualTo(100);
        assertThat(storedTaskExecution.getVersion()).isEqualTo(firstCopy.getVersion());
    }

    private TaskExecution createTaskExecution(Map<String, ?> metadata) {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        TaskExecution taskExecution = TaskExecution.builder()
            .metadata(metadata)
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "test/v1/noop")))
            .build();

        taskExecution.setJobId(job.getId());
        taskExecution.setStatus(TaskExecution.Status.STARTED);
        taskExecution.setTaskNumber(1);

        return taskExecutionService.create(taskExecution);
    }

    private static long getId(TaskExecution taskExecution) {
        return Validate.notNull(taskExecution.getId(), "id");
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
