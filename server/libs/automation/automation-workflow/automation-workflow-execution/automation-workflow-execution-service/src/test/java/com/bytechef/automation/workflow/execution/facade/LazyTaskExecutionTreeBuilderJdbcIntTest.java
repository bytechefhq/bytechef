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

package com.bytechef.automation.workflow.execution.facade;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = LazyTaskExecutionTreeBuilderJdbcIntTest.LazyTaskExecutionTreeBuilderIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class LazyTaskExecutionTreeBuilderJdbcIntTest extends AbstractLazyTaskExecutionTreeBuilderRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcJobRepository jobRepository;

    private long jobId;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @BeforeEach
    void beforeEach() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        jobId = Objects.requireNonNull(jobRepository.save(job)
            .getId());

        setUp(new TaskExecutionServiceImpl(taskExecutionRepository));
    }

    @AfterEach
    void afterEach() {
        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Override
    protected int getCreatedSecond(int sequence) {
        return sequence / 2;
    }

    @Override
    protected long getJobId() {
        return jobId;
    }

    @Override
    protected TaskExecution save(TaskExecution taskExecution, Instant createdDate) {
        TaskExecution savedTaskExecution = taskExecutionRepository.save(taskExecution);

        long id = Objects.requireNonNull(savedTaskExecution.getId());

        jdbcTemplate.update(
            "UPDATE task_execution SET created_date = ? WHERE id = ?", Timestamp.from(createdDate), id);

        return taskExecutionRepository.findById(id)
            .orElseThrow();
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class,
        LazyTaskExecutionTreeBuilderJdbcIntTest.LazyTaskExecutionTreeBuilderIntJdbcConfiguration.class
    })
    static class LazyTaskExecutionTreeBuilderIntTestConfiguration {
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class LazyTaskExecutionTreeBuilderIntJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        LazyTaskExecutionTreeBuilderIntJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new ExecutionErrorToStringConverter(objectMapper), new FileEntryToStringConverter(objectMapper),
                new MapWrapperToStringConverter(objectMapper), new StringToExecutionErrorConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper), new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper), new StringToWorkflowTaskConverter(objectMapper),
                new WebhooksToStringConverter(objectMapper), new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
