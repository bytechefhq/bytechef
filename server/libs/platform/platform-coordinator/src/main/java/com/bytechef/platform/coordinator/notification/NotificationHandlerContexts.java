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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ErrorSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.JobSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.ProjectSummary;
import com.bytechef.platform.notification.handler.NotificationHandlerContext.WorkflowSummary;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the {@link NotificationHandlerContext} of a job event: the job, its workflow, the project (or integration) and
 * environment it runs in, the failing task's error and a link to the execution. Each lookup is best-effort; a failed
 * lookup omits its field and never prevents the notification.
 *
 * @author Ivica Cardic
 */
public class NotificationHandlerContexts {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandlerContexts.class);

    private static final int MAX_STACK_TRACE_LENGTH = 2000;
    private static final List<PlatformType> PLATFORM_TYPES = List.of(PlatformType.AUTOMATION, PlatformType.EMBEDDED);

    private final EnvironmentService environmentService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobService principalJobService;
    private final @Nullable String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public NotificationHandlerContexts(
        EnvironmentService environmentService, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        PrincipalJobService principalJobService, @Nullable String publicUrl,
        TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        this.environmentService = environmentService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobService = principalJobService;
        this.publicUrl = StringUtils.isBlank(publicUrl) ? null : StringUtils.removeEnd(publicUrl, "/");
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    public NotificationHandlerContext.Builder builder(Job job, NotificationEvent.Type eventType) {
        Long jobId = job.getId();

        NotificationHandlerContext.Builder builder = new NotificationHandlerContext.Builder()
            .deliveryId(String.valueOf(UUID.randomUUID()))
            .eventType(eventType)
            .job(getJobSummary(job))
            .jobId(jobId)
            .jobName(job.getLabel())
            .timestamp(Instant.now());

        String workflowId = job.getWorkflowId();

        if (workflowId != null) {
            builder.workflow(new WorkflowSummary(workflowId, fetchWorkflowName(workflowId, jobId)));
        }

        if (jobId == null) {
            return builder;
        }

        resolvePrincipal(builder, jobId);

        if (eventType == NotificationEvent.Type.JOB_FAILED) {
            try {
                builder.error(getErrorSummary(job, jobId));
            } catch (RuntimeException runtimeException) {
                log.warn("Could not resolve the error of job {} for its notification", jobId, runtimeException);
            }
        }

        return builder;
    }

    private @Nullable String fetchWorkflowName(String workflowId, @Nullable Long jobId) {
        try {
            return workflowService.fetchWorkflow(workflowId)
                .map(Workflow::getLabel)
                .orElse(null);
        } catch (RuntimeException runtimeException) {
            log.warn("Could not resolve the workflow of job {} for its notification", jobId, runtimeException);

            return null;
        }
    }

    private @Nullable ErrorSummary getErrorSummary(Job job, long jobId) {
        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        for (TaskExecution taskExecution : taskExecutions.reversed()) {
            ExecutionError error = taskExecution.getError();

            if (taskExecution.getStatus() == TaskExecution.Status.FAILED && error != null) {
                return new ErrorSummary(
                    taskExecution.getName(), taskExecution.getType(), error.getMessage(), getStackTrace(error));
            }
        }

        ExecutionError jobError = job.getError();

        if (jobError == null) {
            return null;
        }

        return new ErrorSummary(null, null, jobError.getMessage(), getStackTrace(jobError));
    }

    private @Nullable String getExecutionUrl(PlatformType platformType, long jobId) {
        if (publicUrl == null) {
            return null;
        }

        String executionsPath =
            platformType == PlatformType.EMBEDDED ? "/embedded/executions" : "/automation/executions";

        return publicUrl + executionsPath + "?id=" + jobId;
    }

    private static JobSummary getJobSummary(Job job) {
        Instant startDate = job.getStartDate();
        Instant endDate = job.getEndDate();
        Job.Status status = job.getStatus();

        Long durationMillis = null;

        if (startDate != null && endDate != null) {
            Duration duration = Duration.between(startDate, endDate);

            durationMillis = duration.toMillis();
        }

        return new JobSummary(
            job.getId(), job.getLabel(), status == null ? null : status.name(), startDate, endDate, durationMillis);
    }

    private static @Nullable String getStackTrace(ExecutionError error) {
        List<String> stackTrace = error.getStackTrace();

        if (stackTrace.isEmpty()) {
            return null;
        }

        return StringUtils.truncate(String.join("\n", stackTrace), MAX_STACK_TRACE_LENGTH);
    }

    private void resolvePrincipal(NotificationHandlerContext.Builder builder, long jobId) {
        PlatformType platformType = null;
        Long jobPrincipalId = null;

        try {
            for (PlatformType candidatePlatformType : PLATFORM_TYPES) {
                Optional<Long> jobPrincipalIdOptional = principalJobService.fetchJobPrincipalId(
                    jobId, candidatePlatformType);

                if (jobPrincipalIdOptional.isPresent()) {
                    platformType = candidatePlatformType;
                    jobPrincipalId = jobPrincipalIdOptional.get();

                    break;
                }
            }
        } catch (RuntimeException runtimeException) {
            log.warn("Could not resolve the principal of job {} for its notification", jobId, runtimeException);

            return;
        }

        if (platformType == null) {
            return;
        }

        builder.executionUrl(getExecutionUrl(platformType, jobId));
        builder.source(platformType.name());

        JobPrincipalSummary jobPrincipalSummary;

        try {
            JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
                platformType);

            jobPrincipalSummary = jobPrincipalAccessor.getJobPrincipalSummary(jobPrincipalId);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not resolve the project and environment of job {} for its notification", jobId,
                runtimeException);

            return;
        }

        if (jobPrincipalSummary.projectId() != null || jobPrincipalSummary.projectName() != null ||
            jobPrincipalSummary.projectVersion() != null) {

            builder.project(
                new ProjectSummary(
                    jobPrincipalSummary.projectId(), jobPrincipalSummary.projectName(),
                    jobPrincipalSummary.projectVersion()));
        }

        try {
            Environment environment = environmentService.getEnvironment(jobPrincipalSummary.environmentId());

            builder.environment(environment.name());
        } catch (RuntimeException runtimeException) {
            log.warn("Could not resolve the environment of job {} for its notification", jobId, runtimeException);
        }
    }
}
