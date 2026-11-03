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
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class NotificationHandlerContextsTest {

    private static final Instant END_DATE = Instant.parse("2026-11-03T10:00:05Z");
    private static final String PUBLIC_URL = "https://bytechef.example";
    private static final Instant START_DATE = Instant.parse("2026-11-03T10:00:00Z");

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private Job job;

    @BeforeEach
    void beforeEach() {
        job = new Job();

        job.setEndDate(END_DATE);
        job.setId(42L);
        job.setLabel("Daily sync");
        job.setStartDate(START_DATE);
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId("wf-1");
    }

    @Test
    void testBuilderResolvesWorkflowProjectEnvironmentAndExecutionUrl() {
        Workflow workflow = workflow("Daily sync");

        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));
        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(Optional.of(workflow));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(Type.JOB_FAILED, context.getEventType());
        assertEquals(42L, context.getJobId());
        assertEquals("Daily sync", context.getJobName());
        assertEquals("AUTOMATION", context.getSource());
        assertEquals(new ProjectSummary(1L, "CRM", 3), context.getProject());
        assertEquals("PRODUCTION", context.getEnvironment());
        assertEquals(new WorkflowSummary("wf-1", "Daily sync"), context.getWorkflow());
        assertEquals(
            new JobSummary(42L, "Daily sync", "FAILED", START_DATE, END_DATE, 5000L), context.getJob());
        assertEquals(PUBLIC_URL + "/automation/executions?id=42", context.getExecutionUrl());
        assertNotNull(context.getDeliveryId());
        assertNotNull(context.getTimestamp());
    }

    @Test
    void testBuilderKeepsProjectWhenEnvironmentLookupFails() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));
        when(environmentService.getEnvironment(2L)).thenThrow(new IllegalStateException("boom"));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
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

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_COMPLETED)
            .build();

        assertEquals("EMBEDDED", context.getSource());
        assertEquals(new ProjectSummary(5L, "HubSpot", 2), context.getProject());
        assertEquals("STAGING", context.getEnvironment());
        assertEquals(PUBLIC_URL + "/embedded/executions?id=42", context.getExecutionUrl());
    }

    @Test
    void testBuilderResolvesFailingTaskError() {
        when(taskExecutionService.getJobTaskExecutions(42L)).thenReturn(
            List.of(
                taskExecution("Read rows", "csvFile/v1/read", TaskExecution.Status.COMPLETED, null),
                taskExecution(
                    "Send email", "email/v1/send", TaskExecution.Status.FAILED,
                    new ExecutionError("boom", List.of("a", "b")))));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary("Send email", "email/v1/send", "boom", "a\nb"), context.getError());
    }

    @Test
    void testBuilderFallsBackToJobErrorWhenNoTaskFailed() {
        job.setError(new ExecutionError("job boom", List.of("c")));

        when(taskExecutionService.getJobTaskExecutions(42L)).thenReturn(List.of());

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary(null, null, "job boom", "c"), context.getError());
    }

    @Test
    void testBuilderKeepsTaskNameWhenErrorMessageIsNull() {
        when(taskExecutionService.getJobTaskExecutions(42L)).thenReturn(
            List.of(
                taskExecution(
                    "Send email", "email/v1/send", TaskExecution.Status.FAILED,
                    new ExecutionError(null, List.of("a", "b")))));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
            .build();

        assertEquals(new ErrorSummary("Send email", "email/v1/send", null, "a\nb"), context.getError());
    }

    @Test
    void testBuilderTruncatesStackTraceTo2000Characters() {
        when(taskExecutionService.getJobTaskExecutions(42L)).thenReturn(
            List.of(
                taskExecution(
                    "Send email", "email/v1/send", TaskExecution.Status.FAILED,
                    new ExecutionError("boom", List.of("x".repeat(5000))))));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
            .build();

        ErrorSummary errorSummary = context.getError();

        assertEquals(2000, errorSummary.stackTrace()
            .length());
    }

    @Test
    void testBuilderOmitsPrincipalFieldsWhenJobHasNoPrincipal() {
        Workflow workflow = workflow("Daily sync");

        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.AUTOMATION)).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(42L, PlatformType.EMBEDDED)).thenReturn(Optional.empty());
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(Optional.of(workflow));

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_COMPLETED)
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

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_COMPLETED)
            .build();

        assertEquals(new WorkflowSummary("wf-1", null), context.getWorkflow());
    }

    @Test
    void testBuilderOmitsProjectWhenAccessorThrows() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenThrow(new IllegalArgumentException("gone"));

        NotificationHandlerContext context = assertDoesNotThrow(
            () -> contexts(PUBLIC_URL).builder(job, Type.JOB_FAILED)
                .build());

        assertNull(context.getProject());
        assertNull(context.getEnvironment());
        assertEquals("AUTOMATION", context.getSource());
        assertEquals(PUBLIC_URL + "/automation/executions?id=42", context.getExecutionUrl());
    }

    @Test
    void testBuilderOmitsProjectWhenSummaryHasNoProject() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, null, null, null));
        when(environmentService.getEnvironment(2L)).thenReturn(Environment.PRODUCTION);

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getProject());
        assertEquals("PRODUCTION", context.getEnvironment());
    }

    @Test
    void testBuilderOmitsExecutionUrlWhenPublicUrlIsBlank() {
        mockPrincipal(PlatformType.AUTOMATION, 7L);
        when(jobPrincipalAccessor.getJobPrincipalSummary(7L)).thenReturn(new JobPrincipalSummary(2, 1L, "CRM", 3));

        NotificationHandlerContext context = contexts("").builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getExecutionUrl());
        assertNull(contexts(null).builder(job, Type.JOB_COMPLETED)
            .build()
            .getExecutionUrl());
    }

    @Test
    void testBuilderSkipsErrorLookupForNonFailureEvents() {
        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_COMPLETED)
            .build();

        assertNull(context.getError());

        verifyNoInteractions(taskExecutionService);
    }

    @Test
    void testBuilderOmitsDurationWhenEndDateIsMissing() {
        job.setEndDate(null);

        NotificationHandlerContext context = contexts(PUBLIC_URL).builder(job, Type.JOB_STARTED)
            .build();

        assertEquals(new JobSummary(42L, "Daily sync", "FAILED", START_DATE, null, null), context.getJob());
    }

    private NotificationHandlerContexts contexts(String publicUrl) {
        return new NotificationHandlerContexts(
            environmentService, jobPrincipalAccessorRegistry, principalJobService, publicUrl, taskExecutionService,
            workflowService);
    }

    private void mockPrincipal(PlatformType platformType, long jobPrincipalId) {
        for (PlatformType type : PlatformType.values()) {
            when(principalJobService.fetchJobPrincipalId(42L, type)).thenReturn(
                type == platformType ? Optional.of(jobPrincipalId) : Optional.empty());
        }

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(platformType)).thenReturn(jobPrincipalAccessor);
    }

    private static TaskExecution taskExecution(
        String name, String type, TaskExecution.Status status, ExecutionError executionError) {

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(42L)
            .workflowTask(new WorkflowTask(Map.of("name", name, "type", type)))
            .build();

        taskExecution.setError(executionError);
        taskExecution.setStatus(status);

        return taskExecution;
    }

    private static Workflow workflow(String label) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(label);

        return workflow;
    }
}
