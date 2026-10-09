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
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SseStreamApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(SseStreamApplicationEventListener.class);

    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public SseStreamApplicationEventListener(
        MessageBroker messageBroker, TaskExecutionService taskExecutionService) {

        this.messageBroker = messageBroker;
        this.taskExecutionService = taskExecutionService;
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

            if (status == Job.Status.FAILED) {
                String errorMessage = getFailedTaskErrorMessage(jobId);

                if (errorMessage != null) {
                    sseStreamEvent.putMetadata(SseStreamEvent.METADATA_ERROR_MESSAGE, errorMessage);
                }
            } else if (status == Job.Status.STOPPED) {
                sseStreamEvent.putMetadata(
                    SseStreamEvent.METADATA_SUSPENDED, jobStatusApplicationEvent.isSuspended());
            }

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            log.warn("Failed to publish the {} status SSE event of job {}", status, jobId, exception);
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
