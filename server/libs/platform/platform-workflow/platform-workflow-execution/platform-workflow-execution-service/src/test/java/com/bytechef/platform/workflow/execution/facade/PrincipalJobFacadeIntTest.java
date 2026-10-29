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

package com.bytechef.platform.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.config.PrincipalJobFacadeIntTestConfiguration;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@Import({
    PostgreSQLContainerConfiguration.class, PrincipalJobFacadeIntTest.StartedJobPrincipalRecorder.class
})
@SpringBootTest(classes = PrincipalJobFacadeIntTestConfiguration.class)
class PrincipalJobFacadeIntTest {

    private static final long PRINCIPAL_ID = 7L;
    private static final String WORKFLOW_ID = "wf-99";

    @MockitoBean
    private ContextService contextService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @MockitoSpyBean
    private PrincipalJobService principalJobService;

    @Autowired
    private StartedJobPrincipalRecorder startedJobPrincipalRecorder;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    @MockitoBean
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn(WORKFLOW_ID);
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
    }

    @AfterEach
    void afterEach() {
        startedJobPrincipalRecorder.clear();

        jdbcTemplate.update("DELETE FROM principal_job");
        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testCreatePrincipalLinkedJobCommitsThePrincipalLinkBeforeTheJobStarts() {
        long referenceJobId = createReferenceJob(PRINCIPAL_ID);

        long jobId = principalJobFacade.createPrincipalLinkedJob(
            referenceJobId, newJobParameters(), PlatformType.AUTOMATION);

        assertThat(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)).contains(PRINCIPAL_ID);
        assertThat(startedJobPrincipalRecorder.principalIdsAtStart()).containsExactly(
            Map.entry(jobId, Optional.of(PRINCIPAL_ID)));
    }

    @Test
    void testCreatePrincipalLinkedJobRollsBackTheJobWhenThePrincipalLinkFails() {
        long referenceJobId = createReferenceJob(PRINCIPAL_ID);

        doThrow(new DataIntegrityViolationException("link failed"))
            .when(principalJobService)
            .create(anyLong(), eq(PRINCIPAL_ID), eq(PlatformType.AUTOMATION));

        assertThatThrownBy(
            () -> principalJobFacade.createPrincipalLinkedJob(
                referenceJobId, newJobParameters(), PlatformType.AUTOMATION))
                    .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(countJobs()).isEqualTo(1);
        assertThat(startedJobPrincipalRecorder.principalIdsAtStart()).isEmpty();
    }

    @Test
    void testCreatePrincipalLinkedJobThrowsWhenPrincipalMissing() {
        long referenceJobId = createJob();

        assertThatThrownBy(
            () -> principalJobFacade.createPrincipalLinkedJob(
                referenceJobId, newJobParameters(), PlatformType.AUTOMATION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(String.valueOf(referenceJobId));

        assertThat(countJobs()).isEqualTo(1);
        assertThat(startedJobPrincipalRecorder.principalIdsAtStart()).isEmpty();
    }

    @Test
    void testCreatePrincipalLinkedJobRejectsASubflowChild() {
        long referenceJobId = createReferenceJob(PRINCIPAL_ID);

        assertThatThrownBy(
            () -> principalJobFacade.createPrincipalLinkedJob(
                referenceJobId, new JobParametersDTO(WORKFLOW_ID, 10L, Map.of()), PlatformType.AUTOMATION))
                    .isInstanceOf(IllegalArgumentException.class);

        assertThat(countJobs()).isEqualTo(1);
    }

    private long countJobs() {
        return Objects.requireNonNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job", Long.class));
    }

    private long createJob() {
        Job job = jobService.create(newJobParameters(), workflowService.getWorkflow(WORKFLOW_ID));

        return Objects.requireNonNull(job.getId());
    }

    private long createReferenceJob(long principalId) {
        long referenceJobId = createJob();

        principalJobService.create(referenceJobId, principalId, PlatformType.AUTOMATION);

        return referenceJobId;
    }

    private static JobParametersDTO newJobParameters() {
        return new JobParametersDTO(WORKFLOW_ID, Map.of(), Map.of());
    }

    static class StartedJobPrincipalRecorder {

        private final Map<Long, Optional<Long>> principalIdsAtStart = new ConcurrentHashMap<>();
        private final PrincipalJobService principalJobService;

        StartedJobPrincipalRecorder(PrincipalJobService principalJobService) {
            this.principalJobService = principalJobService;
        }

        @EventListener
        void onStartJobEvent(StartJobEvent startJobEvent) {
            long jobId = startJobEvent.getJobId();

            principalIdsAtStart.put(jobId, principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION));
        }

        Map<Long, Optional<Long>> principalIdsAtStart() {
            return Map.copyOf(principalIdsAtStart);
        }

        void clear() {
            principalIdsAtStart.clear();
        }
    }
}
