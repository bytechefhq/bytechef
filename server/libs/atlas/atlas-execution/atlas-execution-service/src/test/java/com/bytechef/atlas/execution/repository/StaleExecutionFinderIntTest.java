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

package com.bytechef.atlas.execution.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.ObjectMapper;

/**
 * Exercises the crash-recovery and run-timeout finders against real PostgreSQL:
 * {@code findAllByStatusAndLastModifiedDateBefore} on both the job and task_execution tables (the queries behind
 * {@code OrphanedJobRecoveryMonitor}) and {@code findAllByStatusAndStartDateBefore} on the job table (behind
 * {@code JobTimeoutMonitor}). Audit timestamps are stamped on save, so stale rows are produced by backdating the
 * columns directly.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@DirtiesContext
public class StaleExecutionFinderIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Test
    public void testStaleFindersMatchOnlyMatchingStatusRowsPastCutoff() {
        Instant staleInstant = Instant.now()
            .minus(Duration.ofMinutes(10));
        Instant cutoff = Instant.now()
            .minus(Duration.ofMinutes(5));

        long staleStartedJobId = saveJob(Job.Status.STARTED);
        long freshStartedJobId = saveJob(Job.Status.STARTED);
        long staleCompletedJobId = saveJob(Job.Status.COMPLETED);

        backdateJobLastModifiedDate(staleStartedJobId, staleInstant);
        backdateJobLastModifiedDate(staleCompletedJobId, staleInstant);

        List<Job> staleJobs = jobRepository.findAllByStatusAndLastModifiedDateBefore(
            Job.Status.STARTED.ordinal(), cutoff);

        assertThat(staleJobs).extracting(Job::getId)
            .contains(staleStartedJobId)
            .doesNotContain(freshStartedJobId, staleCompletedJobId);

        long staleTaskExecutionId = saveTaskExecution(staleStartedJobId, TaskExecution.Status.STARTED);
        long freshTaskExecutionId = saveTaskExecution(freshStartedJobId, TaskExecution.Status.STARTED);

        jdbcTemplate.update(
            "UPDATE task_execution SET last_modified_date = ? WHERE id = ?",
            java.sql.Timestamp.from(staleInstant), staleTaskExecutionId);

        List<TaskExecution> staleTaskExecutions = taskExecutionRepository.findAllByStatusAndLastModifiedDateBefore(
            TaskExecution.Status.STARTED.ordinal(), cutoff);

        assertThat(staleTaskExecutions).extracting(TaskExecution::getId)
            .contains(staleTaskExecutionId)
            .doesNotContain(freshTaskExecutionId);
    }

    @Test
    public void testLongRunningFinderMatchesOnlyStartedJobsPastCutoff() {
        Instant longAgoInstant = Instant.now()
            .minus(Duration.ofHours(3));
        Instant cutoff = Instant.now()
            .minus(Duration.ofHours(1));

        long longRunningJobId = saveJob(Job.Status.STARTED);
        long freshStartedJobId = saveJob(Job.Status.STARTED);
        long longCompletedJobId = saveJob(Job.Status.COMPLETED);

        backdateJobStartDate(longRunningJobId, longAgoInstant);
        backdateJobStartDate(freshStartedJobId, Instant.now());
        backdateJobStartDate(longCompletedJobId, longAgoInstant);

        List<Job> longRunningJobs = jobRepository.findAllByStatusAndStartDateBefore(
            Job.Status.STARTED.ordinal(), cutoff);

        assertThat(longRunningJobs).extracting(Job::getId)
            .contains(longRunningJobId)
            .doesNotContain(freshStartedJobId, longCompletedJobId);
    }

    @Test
    public void testEndedFinderMatchesOnlyJobsEndedBeforeCutoff() {
        Instant longAgoInstant = Instant.now()
            .minus(Duration.ofDays(40));
        Instant cutoff = Instant.now()
            .minus(Duration.ofDays(30));

        long expiredJobId = saveJob(Job.Status.COMPLETED);
        long recentJobId = saveJob(Job.Status.COMPLETED);
        long runningJobId = saveJob(Job.Status.STARTED);

        backdateJobEndDate(expiredJobId, longAgoInstant);
        backdateJobEndDate(recentJobId, Instant.now());

        List<Job> endedJobs = jobRepository.findAllByEndDateBefore(cutoff);

        assertThat(endedJobs).extracting(Job::getId)
            .contains(expiredJobId)
            .doesNotContain(recentJobId, runningJobId);
    }

    private long saveJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private long saveTaskExecution(long jobId, TaskExecution.Status status) {
        Map<String, Object> taskMap = new HashMap<>();

        taskMap.put("name", "task1");
        taskMap.put("type", "test/v1/noop");

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(taskMap))
            .build();

        taskExecution.setJobId(jobId);
        taskExecution.setStatus(status);
        taskExecution.setTaskNumber(1);

        taskExecution = taskExecutionRepository.save(taskExecution);

        return Objects.requireNonNull(taskExecution.getId());
    }

    private void backdateJobLastModifiedDate(long jobId, Instant instant) {
        jdbcTemplate.update(
            "UPDATE job SET last_modified_date = ? WHERE id = ?", java.sql.Timestamp.from(instant), jobId);
    }

    private void backdateJobStartDate(long jobId, Instant instant) {
        jdbcTemplate.update(
            "UPDATE job SET start_date = ? WHERE id = ?", java.sql.Timestamp.from(instant), jobId);
    }

    private void backdateJobEndDate(long jobId, Instant instant) {
        jdbcTemplate.update(
            "UPDATE job SET end_date = ? WHERE id = ?", java.sql.Timestamp.from(instant), jobId);
    }

    @ComponentScan(basePackages = {
        "com.bytechef.atlas.configuration.converter"
    })
    @EnableAutoConfiguration
    @Configuration
    public static class StaleExecutionFinderIntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class StaleExecutionFinderIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI")
            public StaleExecutionFinderIntTestJdbcConfiguration(ObjectMapper objectMapper) {
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
