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

package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = AgentJobResumerIntTest.AgentJobResumerIntTestConfiguration.class)
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
class AgentJobResumerIntTest {

    private static final String AGENT_JOB_RESUME_ID = "agent-resume-id";
    private static final AtomicBoolean CONCURRENT_START_BEFORE_NEXT_CLAIM = new AtomicBoolean();
    private static final AtomicBoolean CONCURRENT_UPDATE_ON_NEXT_GET = new AtomicBoolean();
    private static final SubflowOutcome SUBFLOW_OUTCOME = new SubflowOutcome.Completed("done");
    private static final long TASK_EXECUTION_RESUME_ID = 7L;

    @MockitoBean
    private JobFacade jobFacade;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @AfterEach
    void afterEach() {
        CONCURRENT_START_BEFORE_NEXT_CLAIM.set(false);
        CONCURRENT_UPDATE_ON_NEXT_GET.set(false);

        for (Job job : jobRepository.findAll()) {
            jobRepository.deleteById(job.getId());
        }
    }

    @Test
    void testResumesTheAgentAndConsumesTheSuspend() {
        Job agentJob = saveAgentJob(Job.Status.STOPPED, waitingMetadata());

        boolean resumed = resume(agentJob);

        assertThat(resumed).isTrue();

        Job storedAgentJob = getStoredJob(agentJob);

        Map<String, Object> storedMetadata = new HashMap<>(storedAgentJob.getMetadata());

        assertThat(storedMetadata).containsEntry(MetadataConstants.CONSUMED_JOB_RESUME_ID, AGENT_JOB_RESUME_ID)
            .doesNotContainKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID);
        assertThat(storedAgentJob.getVersion()).isGreaterThan(agentJob.getVersion());

        assertThat(storedAgentJob.getStatus()).isEqualTo(Job.Status.STARTED);

        verify(jobFacade).resumeClaimedJob(
            getAgentJobId(agentJob), TASK_EXECUTION_RESUME_ID, Map.of("status", "completed", "result", "done"));
    }

    @Test
    void testDoesNotResumeTheSameSuspendTwice() {
        Job agentJob = saveAgentJob(Job.Status.STOPPED, waitingMetadata());

        assertThat(resume(agentJob)).isTrue();
        assertThat(resume(agentJob)).isFalse();

        verify(jobFacade).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testDoesNotResumeAnAgentThatIsNotStopped() {
        Job agentJob = saveAgentJob(Job.Status.STARTED, waitingMetadata());

        assertNotResumedAndUnchanged(agentJob);
    }

    @Test
    void testDoesNotResumeAnAgentWaitingOnAnotherSuspend() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.put(MetadataConstants.JOB_RESUME_ID, "another-resume-id");

        Job agentJob = saveAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumedAndUnchanged(agentJob);
    }

    @Test
    void testDoesNotResumeAnAlreadyConsumedSuspend() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, AGENT_JOB_RESUME_ID);

        Job agentJob = saveAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumedAndUnchanged(agentJob);
    }

    @Test
    void testDoesNotResumeWithoutATaskExecutionToResume() {
        Map<String, Object> metadata = waitingMetadata();

        metadata.remove(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        Job agentJob = saveAgentJob(Job.Status.STOPPED, metadata);

        assertNotResumedAndUnchanged(agentJob);
    }

    @Test
    void testDoesNotResumeWhenAConcurrentUpdateWins() {
        Job agentJob = saveAgentJob(Job.Status.STOPPED, waitingMetadata());

        CONCURRENT_UPDATE_ON_NEXT_GET.set(true);

        boolean resumed = resume(agentJob);

        assertThat(resumed).isFalse();

        Job storedAgentJob = getStoredJob(agentJob);

        assertThat(storedAgentJob.getMetadata()).doesNotContainKey(MetadataConstants.CONSUMED_JOB_RESUME_ID);

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testDoesNotResumeWhenTheClaimIsLost() {
        Job agentJob = saveAgentJob(Job.Status.STOPPED, waitingMetadata());

        CONCURRENT_START_BEFORE_NEXT_CLAIM.set(true);

        boolean resumed = resume(agentJob);

        assertThat(resumed).isFalse();

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    @Test
    void testKeepsTheClaimWhenDispatchingTheResumeFails() {
        Job agentJob = saveAgentJob(Job.Status.STOPPED, waitingMetadata());

        doThrow(new IllegalStateException("broker down")).when(jobFacade)
            .resumeClaimedJob(anyLong(), any(), any());

        boolean resumed = resume(agentJob);

        assertThat(resumed).isFalse();

        Job storedAgentJob = getStoredJob(agentJob);

        Map<String, Object> storedMetadata = new HashMap<>(storedAgentJob.getMetadata());

        assertThat(storedMetadata).containsEntry(MetadataConstants.CONSUMED_JOB_RESUME_ID, AGENT_JOB_RESUME_ID);
    }

    private void assertNotResumedAndUnchanged(Job agentJob) {
        boolean resumed = resume(agentJob);

        assertThat(resumed).isFalse();

        Job storedAgentJob = getStoredJob(agentJob);

        assertThat(storedAgentJob.getVersion()).isEqualTo(agentJob.getVersion());

        verify(jobFacade, never()).resumeClaimedJob(anyLong(), any(), any());
    }

    private Job getStoredJob(Job job) {
        return OptionalUtils.get(jobRepository.findById(job.getId()));
    }

    private boolean resume(Job agentJob) {
        AgentJobResumer agentJobResumer = new AgentJobResumer(jobFacade, jobService);

        return agentJobResumer.resume(getAgentJobId(agentJob), AGENT_JOB_RESUME_ID, SUBFLOW_OUTCOME);
    }

    private static long getAgentJobId(Job agentJob) {
        return Objects.requireNonNull(agentJob.getId());
    }

    private Job saveAgentJob(Job.Status status, Map<String, Object> metadata) {
        Job job = new Job();

        job.setMetadata(metadata);
        job.setStatus(status);
        job.setWorkflowId("agent:1");

        return jobRepository.save(job);
    }

    private static Map<String, Object> waitingMetadata() {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.JOB_RESUME_ID, AGENT_JOB_RESUME_ID);
        metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_RESUME_ID);
        metadata.put(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID, AGENT_JOB_RESUME_ID);

        return metadata;
    }

    @EnableAutoConfiguration
    @Configuration
    static class AgentJobResumerIntTestConfiguration {

        @Bean
        JobService jobService(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            TransactionTemplate concurrentTransactionTemplate = new TransactionTemplate(transactionManager);

            concurrentTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            return new JobServiceImpl(jobRepository) {

                @Override
                public Job getJob(long id) {
                    Job job = super.getJob(id);

                    if (CONCURRENT_UPDATE_ON_NEXT_GET.getAndSet(false)) {
                        concurrentTransactionTemplate.executeWithoutResult(status -> {
                            Job concurrentCopy = OptionalUtils.get(jobRepository.findById(id));

                            concurrentCopy.setLabel("concurrently updated");

                            jobRepository.save(concurrentCopy);
                        });
                    }

                    return job;
                }

                @Override
                public boolean tryClaimResume(Job job) {
                    if (CONCURRENT_START_BEFORE_NEXT_CLAIM.getAndSet(false)) {
                        concurrentTransactionTemplate.executeWithoutResult(
                            status -> jobRepository.updateStatusAndClearEndDateIfCurrentStatus(
                                Objects.requireNonNull(job.getId()), Job.Status.STOPPED.ordinal(),
                                Job.Status.STARTED.ordinal()));
                    }

                    return super.tryClaimResume(job);
                }
            };
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class AgentJobResumerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            AgentJobResumerIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new ExecutionErrorToStringConverter(objectMapper),
                    new FileEntryToStringConverter(objectMapper),
                    new MapWrapperToStringConverter(objectMapper),
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
