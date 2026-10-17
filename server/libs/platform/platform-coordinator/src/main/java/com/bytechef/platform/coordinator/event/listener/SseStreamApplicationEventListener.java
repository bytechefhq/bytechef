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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.platform.workflow.execution.JobSuspends;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SseStreamApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(SseStreamApplicationEventListener.class);

    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public SseStreamApplicationEventListener(
        JobService jobService, MessageBroker messageBroker, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {
            publishJobStatusEvent(jobStatusApplicationEvent);
        } else if (applicationEvent instanceof TaskStartedApplicationEvent taskStartedApplicationEvent) {
            publishTaskStartedEvent(taskStartedApplicationEvent);
        }
    }

    private void publishJobStatusEvent(JobStatusApplicationEvent jobStatusApplicationEvent) {
        long jobId = jobStatusApplicationEvent.getJobId();
        Job.Status status = jobStatusApplicationEvent.getStatus();

        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobId, SseStreamEvent.EVENT_TYPE_JOB_STATUS, status.name());

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            if (status == Job.Status.COMPLETED) {
                publishFinalResultEvent(jobId);
            }

            if (status == Job.Status.FAILED) {
                String errorMessage = getFailedTaskErrorMessage(jobId);

                if (errorMessage != null) {
                    sseStreamEvent.putMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE, errorMessage);
                }
            } else if (status == Job.Status.STOPPED) {
                sseStreamEvent.putMetadata(
                    SseStreamEvent.METADATA_SUSPENDED, jobStatusApplicationEvent.isSuspended());

                if (jobStatusApplicationEvent.isSuspended() && isAwaitingSubflow(jobId)) {
                    sseStreamEvent.putMetadata(SseStreamEvent.METADATA_AWAITING_SUBFLOW, true);
                }
            }

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            log.warn("Failed to publish the {} status SSE event of job {}", status, jobId, exception);
        }
    }

    /**
     * Sends the run's final chat reply as a named {@code result} event before the terminal status event. An async job
     * never stores the webhook response in its outputs, so the reply is read from the last task execution tagged
     * {@code WEBHOOK_RESPONSE}; a run without one sends nothing.
     */
    private void publishFinalResultEvent(long jobId) {
        String message;

        try {
            message = readWebhookResponseMessage(jobId);
        } catch (Exception exception) {
            log.warn("Failed to read the final reply of job {}", jobId, exception);

            return;
        }

        if (message == null || message.isEmpty()) {
            return;
        }

        SseStreamEvent sseStreamEvent = new SseStreamEvent(
            jobId, SseStreamEvent.EVENT_TYPE_DATA, Map.of("event", "result", "result", Map.of("message", message)));

        sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
    }

    private @Nullable String readWebhookResponseMessage(long jobId) {
        TaskExecution lastTaskExecution = taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getMetadata()
                .containsKey(MetadataConstants.WEBHOOK_RESPONSE))
            .filter(taskExecution -> taskExecution.getOutput() != null)
            .max(Comparator
                .comparing(TaskExecution::getEndDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(TaskExecution::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(null);

        if (lastTaskExecution == null) {
            return null;
        }

        FileEntry outputFileEntry = lastTaskExecution.getOutput();

        if (outputFileEntry == null) {
            return null;
        }

        return extractMessage(taskFileStorage.readTaskExecutionOutput(outputFileEntry));
    }

    private static @Nullable String extractMessage(@Nullable Object output) {
        if (!(output instanceof Map<?, ?> outputMap)) {
            return null;
        }

        Object body = outputMap.get("body");

        if (body instanceof String bodyText) {
            return bodyText;
        }

        if (body instanceof Map<?, ?> bodyMap && bodyMap.get("message") instanceof String messageText) {
            return messageText;
        }

        return null;
    }

    private boolean isAwaitingSubflow(long jobId) {
        try {
            return jobService.fetchJob(jobId)
                .filter(JobSuspends::isSuspendedForSubflow)
                .isPresent();
        } catch (Exception exception) {
            log.error("Failed to look up whether job {} is waiting on a sub-workflow", jobId, exception);

            return false;
        }
    }

    private @Nullable String getFailedTaskErrorMessage(long jobId) {
        List<TaskExecution> taskExecutions;

        try {
            taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);
        } catch (Exception exception) {
            log.warn("Failed to look up the failed task of job {}", jobId, exception);

            return null;
        }

        String errorMessage = null;

        for (TaskExecution taskExecution : taskExecutions) {
            ExecutionError executionError = taskExecution.getError();

            if (taskExecution.getStatus() == TaskExecution.Status.FAILED && executionError != null &&
                executionError.getMessage() != null) {

                errorMessage = executionError.getMessage();
            }
        }

        return errorMessage;
    }

    private void publishTaskStartedEvent(TaskStartedApplicationEvent taskStartedApplicationEvent) {
        Long jobId = taskStartedApplicationEvent.getJobId();

        if (jobId == null) {
            return;
        }

        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobId, SseStreamEvent.EVENT_TYPE_TASK_STARTED, taskStartedApplicationEvent.getTaskExecutionId());

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            log.warn("Failed to publish the task started SSE event of job {}", jobId, exception);
        }
    }
}
