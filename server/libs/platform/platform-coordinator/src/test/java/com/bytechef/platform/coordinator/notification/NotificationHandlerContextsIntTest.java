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

package com.bytechef.platform.coordinator.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorConfigurationIntTestConfiguration;
import com.bytechef.platform.notification.domain.NotificationEvent.Type;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ErrorSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.JobSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ProjectSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.WorkflowSummary;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorConfigurationIntTestConfiguration.class)
@TestPropertySource(properties = "bytechef.public-url=" + NotificationHandlerContextsIntTest.PUBLIC_URL)
class NotificationHandlerContextsIntTest {

    static final String PUBLIC_URL = "https://bytechef.example";

    private static final Instant END_DATE = Instant.parse("2026-11-03T10:00:05Z");
    private static final Instant START_DATE = Instant.parse("2026-11-03T10:00:00Z");

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

    @MockitoBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private NotificationHandlerContexts notificationHandlerContexts;

    @MockitoBean
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @MockitoSpyBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private WorkflowService workflowService;

    private Job job;
    private long jobId;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        Job newJob = new Job();

        newJob.setEndDate(END_DATE);
        newJob.setLabel("Daily sync");
        newJob.setStartDate(START_DATE);
        newJob.setStatus(Job.Status.FAILED);
        newJob.setWorkflowId("wf-1");

        job = jobRepository.save(newJob);

        jobId = Objects.requireNonNull(job.getId());
    }

    @Test
    void testBuilderResolvesWorkflowProjectEnvironmentAndExecutionUrl() {
        Workflow workflow = workflow("Daily sync");

        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));
        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(Optional.of(workflow));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(Type.JOB_FAILED, context.getEventType());
        assertEquals(jobId, context.getJobId());
        assertEquals("Daily sync", context.getJobName());
        assertEquals("AUTOMATION", context.getSource());
        assertEquals(new ProjectSummary(1L, "CRM", 3), context.getProject());
        assertEquals("PRODUCTION", context.getEnvironment());
        assertEquals(new WorkflowSummary("wf-1", "Daily sync"), context.getWorkflow());
        assertEquals(
            new JobSummary(jobId, "Daily sync", "FAILED", START_DATE, END_DATE, 5000L), context.getJob());
        assertEquals(PUBLIC_URL + "/automation/executions?id=" + jobId, context.getExecutionUrl());
        assertNotNull(context.getDeliveryId());
        assertNotNull(context.getTimestamp());
    }

    @Test
    void testBuilderKeepsProjectWhenEnvironmentLookupFails() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));
        when(environmentService.getEnvironment(2L)).thenThrow(new IllegalStateException("boom"));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ProjectSummary(1L, "CRM", 3), context.getProject());
        assertNull(context.getEnvironment());
    }

    @Test
    void testBuilderUsesEmbeddedExecutionsPathForEmbeddedJobs() {
        mockPrincipal(PlatformType.EMBEDDED, 9L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(9L))
            .thenReturn(new JobPrincipalSummary(1, 5L, "HubSpot", 2));
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.STAGING);

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
            .build();

        assertEquals("EMBEDDED", context.getSource());
        assertEquals(new ProjectSummary(5L, "HubSpot", 2), context.getProject());
        assertEquals("STAGING", context.getEnvironment());
        assertEquals(PUBLIC_URL + "/embedded/executions?id=" + jobId, context.getExecutionUrl());
    }

    @Test
    void testBuilderResolvesFailingTaskError() {
        saveTaskExecution(1, "Read rows", "csvFile/v1/read", TaskExecution.Status.COMPLETED, null);
        saveTaskExecution(
            2, "Send email", "email/v1/send", TaskExecution.Status.FAILED,
            new ExecutionError("boom", List.of("a", "b")));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary("Send email", "email/v1/send", "boom", "a\nb"), context.getError());
    }

    @Test
    void testBuilderFallsBackToJobErrorWhenNoTaskFailed() {
        job.setError(new ExecutionError("job boom", List.of("c")));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary(null, null, "job boom", "c"), context.getError());
    }

    @Test
    void testBuilderKeepsTaskNameWhenErrorMessageIsNull() {
        saveTaskExecution(
            1, "Send email", "email/v1/send", TaskExecution.Status.FAILED,
            new ExecutionError(null, List.of("a", "b")));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary("Send email", "email/v1/send", null, "a\nb"), context.getError());
    }

    @Test
    void testBuilderTruncatesStackTraceTo2000Characters() {
        saveTaskExecution(
            1, "Send email", "email/v1/send", TaskExecution.Status.FAILED,
            new ExecutionError("boom", List.of("x".repeat(5000))));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_FAILED)
            .build();

        ErrorSummary errorSummary = context.getError();

        String stackTrace = errorSummary.stackTrace();

        assertEquals(2000, stackTrace.length());
    }

    @Test
    void testBuilderOmitsPrincipalFieldsWhenJobHasNoPrincipal() {
        Workflow workflow = workflow("Daily sync");

        when(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(jobId, PlatformType.EMBEDDED)).thenReturn(Optional.empty());
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(Optional.of(workflow));

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getSource());
        assertNull(context.getProject());
        assertNull(context.getEnvironment());
        assertNull(context.getExecutionUrl());
        assertEquals(new WorkflowSummary("wf-1", "Daily sync"), context.getWorkflow());
    }

    @Test
    void testBuilderKeepsWorkflowIdWhenWorkflowIsMissing() {
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(Optional.empty());

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
            .build();

        assertEquals(new WorkflowSummary("wf-1", null), context.getWorkflow());
    }

    @Test
    void testBuilderOmitsProjectWhenAccessorThrows() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenThrow(new IllegalArgumentException("gone"));

        NotificationHandlerContext context = assertDoesNotThrow(
            () -> notificationHandlerContexts.builder(job, Type.JOB_FAILED)
                .build());

        assertNull(context.getProject());
        assertNull(context.getEnvironment());
        assertEquals("AUTOMATION", context.getSource());
        assertEquals(PUBLIC_URL + "/automation/executions?id=" + jobId, context.getExecutionUrl());
    }

    @Test
    void testBuilderOmitsProjectWhenSummaryHasNoProject() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, null, null, null));
        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getProject());
        assertEquals("PRODUCTION", context.getEnvironment());
    }

    @Test
    void testBuilderSkipsErrorLookupForNonFailureEvents() {
        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getError());

        verifyNoInteractions(taskExecutionService);
    }

    @Test
    void testBuilderOmitsDurationWhenEndDateIsMissing() {
        job.setEndDate(null);

        NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_STARTED)
            .build();

        assertEquals(new JobSummary(jobId, "Daily sync", "FAILED", START_DATE, null, null), context.getJob());
    }

    @Nested
    @TestPropertySource(properties = "bytechef.public-url=")
    class BlankPublicUrlIntTest {

        @Test
        void testBuilderOmitsExecutionUrlWhenPublicUrlIsBlank() {
            mockPrincipal(PlatformType.AUTOMATION, 7L);
            when(jobPrincipalAccessor.getJobPrincipalSummary(7L))
                .thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));

            NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
                .build();

            assertEquals("AUTOMATION", context.getSource());
            assertNull(context.getExecutionUrl());
        }
    }

    @Nested
    @TestPropertySource(inheritProperties = false, properties = "bytechef.coordinator.enabled=true")
    class MissingPublicUrlIntTest {

        @Test
        void testBuilderOmitsExecutionUrlWhenPublicUrlIsMissing() {
            mockPrincipal(PlatformType.AUTOMATION, 7L);
            when(jobPrincipalAccessor.getJobPrincipalSummary(7L))
                .thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));

            NotificationHandlerContext context = notificationHandlerContexts.builder(job, Type.JOB_COMPLETED)
                .build();

            assertEquals("AUTOMATION", context.getSource());
            assertNull(context.getExecutionUrl());
        }
    }

    private void mockPrincipal(PlatformType platformType, long jobPrincipalId) {
        for (PlatformType type : PlatformType.values()) {
            when(principalJobService.fetchJobPrincipalId(jobId, type)).thenReturn(
                type == platformType ? Optional.of(jobPrincipalId) : Optional.empty());
        }

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(platformType)).thenReturn(jobPrincipalAccessor);
    }

    private void saveTaskExecution(
        int taskNumber, String name, String type, TaskExecution.Status status, ExecutionError executionError) {

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(taskNumber)
            .workflowTask(new WorkflowTask(Map.of("name", name, "type", type)))
            .build();

        taskExecution.setError(executionError);
        taskExecution.setStatus(status);

        taskExecutionRepository.save(taskExecution);
    }

    private static Workflow workflow(String label) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(label);

        return workflow;
    }
}
