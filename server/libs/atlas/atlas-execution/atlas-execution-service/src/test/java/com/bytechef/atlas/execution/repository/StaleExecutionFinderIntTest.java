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
import com.bytechef.atlas.execution.domain.Job;
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
import java.util.List;
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

    @Test
    public void testStaleJobFinderMatchesOnlyMatchingStatusRowsPastCutoff() {
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
    }

    private long saveJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private void backdateJobLastModifiedDate(long jobId, Instant instant) {
        jdbcTemplate.update(
            "UPDATE job SET last_modified_date = ? WHERE id = ?", java.sql.Timestamp.from(instant), jobId);
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
