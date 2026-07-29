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
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public SseStreamApplicationEventListener(
        MessageBroker messageBroker, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

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
        try {
            Job.Status status = jobStatusApplicationEvent.getStatus();

            if (status == Job.Status.COMPLETED) {
                publishFinalResultEvent(jobStatusApplicationEvent.getJobId());
            }

            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobStatusApplicationEvent.getJobId(), SseStreamEvent.EVENT_TYPE_JOB_STATUS, status.name());

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (log.isTraceEnabled()) {
                log.trace(exception.getMessage(), exception);
            }
        }
    }

    /**
     * Emits the run's final chat reply as a named {@code result} data event when the job completes. Async jobs never
     * persist the webhook response into {@code Job.outputs} (only the synchronous awaiter path does), so the reply is
     * read back from the {@code WEBHOOK_RESPONSE}-tagged task execution — the same last-to-complete-wins selection
     * {@code WebhookWorkflowExecutorImpl} applies on the sync path. Runs without a tagged response task (non-chat
     * workflows) emit nothing; SSE listeners that don't understand the event simply ignore it. This closes the
     * streaming-path gap where an approval-only chat workflow (no streaming task) lost its final reply text.
     */
    private void publishFinalResultEvent(long jobId) {
        String message = readWebhookResponseMessage(jobId);

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

    /**
     * Extracts the chat reply text from a persisted {@code WebhookResponse} task output (a map with {@code body},
     * {@code headers}, {@code statusCode} after the JSON round-trip). A JSON chat reply carries {@code body.message}; a
     * raw-text response carries the text directly as {@code body}. Anything else (binary, redirect, message-less JSON)
     * yields {@code null} and no event.
     */
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

    private void publishTaskStartedEvent(TaskStartedApplicationEvent taskStartedApplicationEvent) {
        Long jobId = taskStartedApplicationEvent.getJobId();

        if (jobId == null) {
            return;
        }

        try {
            SseStreamEvent sseStreamEvent = new SseStreamEvent(
                jobId, SseStreamEvent.EVENT_TYPE_TASK_STARTED,
                buildTaskStartedPayload(taskStartedApplicationEvent.getTaskExecutionId()));

            sseStreamEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, sseStreamEvent);
        } catch (Exception exception) {
            if (log.isTraceEnabled()) {
                log.trace(exception.getMessage(), exception);
            }
        }
    }

    /**
     * Builds the {@code task_started} payload. The enriched shape —
     * {@code {event=task_started, payload={taskExecutionId, name, type}}} — is the {@code "event"}-keyed form both SSE
     * bridges route as a named {@code task_started} event (mirroring the workflow-test surface's event shape), and the
     * AG-UI bridge renders as a tool-call step chip with the actual task name. Falls back to the legacy bare
     * {@code taskExecutionId} when the row can't be loaded (e.g. a remote read failure) so the event still fires.
     */
    private Object buildTaskStartedPayload(long taskExecutionId) {
        try {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

            Map<String, Object> payload = new LinkedHashMap<>();

            payload.put("taskExecutionId", taskExecutionId);

            if (taskExecution.getName() != null) {
                payload.put("name", taskExecution.getName());
            }

            payload.put("type", taskExecution.getType());

            Map<String, Object> eventData = new LinkedHashMap<>();

            eventData.put("event", "task_started");
            eventData.put("payload", payload);

            return eventData;
        } catch (Exception exception) {
            if (log.isTraceEnabled()) {
                log.trace(exception.getMessage(), exception);
            }

            return taskExecutionId;
        }
    }
}
