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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pins the coordinator-emitted final {@code result} data event: a COMPLETED job with a {@code WEBHOOK_RESPONSE}-tagged
 * task execution publishes {@code {event=result, result={message=...}}} before the terminal job-status event, so SSE
 * chat surfaces render the final reply even when the run streamed nothing (the approval-routed path for chat workflows
 * without a streaming task). Jobs without a tagged response, or with a non-COMPLETED terminal status, emit only the
 * job-status event.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class SseStreamApplicationEventListenerTest {

    private static final long JOB_ID = 42L;

    @Mock
    private MessageBroker messageBroker;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    @Test
    void testCompletedJobWithWebhookResponsePublishesResultBeforeJobStatus() {
        FileEntry outputFileEntry = new FileEntry("output.json", "file:/tmp/output.json");

        TaskExecution taskExecution = taggedTaskExecution(1L, outputFileEntry);

        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(List.of(taskExecution));
        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry))
            .thenReturn(Map.of("body", Map.of("message", "final reply"), "statusCode", 200));

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, times(2))
            .send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        List<SseStreamEvent> events = eventCaptor.getAllValues();

        // The result must be published FIRST — the terminal job-status event closes downstream bridges.
        SseStreamEvent resultEvent = events.get(0);

        assertThat(resultEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_DATA);
        assertThat(resultEvent.getPayload())
            .isEqualTo(Map.of("event", "result", "result", Map.of("message", "final reply")));

        SseStreamEvent jobStatusEvent = events.get(1);

        assertThat(jobStatusEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_JOB_STATUS);
        assertThat(jobStatusEvent.getPayload()).isEqualTo(Job.Status.COMPLETED.name());
    }

    @Test
    void testStringBodyIsUsedAsTheMessageDirectly() {
        FileEntry outputFileEntry = new FileEntry("output.json", "file:/tmp/output.json");

        when(taskExecutionService.getJobTaskExecutions(JOB_ID))
            .thenReturn(List.of(taggedTaskExecution(1L, outputFileEntry)));
        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry))
            .thenReturn(Map.of("body", "raw text reply", "statusCode", 200));

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, times(2))
            .send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent resultEvent = eventCaptor.getAllValues()
            .get(0);

        assertThat(resultEvent.getPayload())
            .isEqualTo(Map.of("event", "result", "result", Map.of("message", "raw text reply")));
    }

    @Test
    void testCompletedJobWithoutWebhookResponseEmitsOnlyJobStatus() {
        when(taskExecutionService.getJobTaskExecutions(JOB_ID)).thenReturn(List.of());

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent sseStreamEvent = eventCaptor.getValue();

        assertThat(sseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_JOB_STATUS);
    }

    @Test
    void testFailedJobDoesNotReadTaskOutputs() {
        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.FAILED));

        verifyNoInteractions(taskExecutionService, taskFileStorage);

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent sseStreamEvent = eventCaptor.getValue();

        assertThat(sseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_JOB_STATUS);
        assertThat(sseStreamEvent.getPayload()).isEqualTo(Job.Status.FAILED.name());
    }

    @Test
    void testLastCompletedTaggedTaskWins() {
        FileEntry earlierFileEntry = new FileEntry("earlier.json", "file:/tmp/earlier.json");
        FileEntry laterFileEntry = new FileEntry("later.json", "file:/tmp/later.json");

        TaskExecution earlierTaskExecution = taggedTaskExecution(1L, earlierFileEntry);

        earlierTaskExecution.setEndDate(Instant.parse("2026-07-21T10:00:00Z"));

        TaskExecution laterTaskExecution = taggedTaskExecution(2L, laterFileEntry);

        laterTaskExecution.setEndDate(Instant.parse("2026-07-21T10:05:00Z"));

        when(taskExecutionService.getJobTaskExecutions(JOB_ID))
            .thenReturn(List.of(laterTaskExecution, earlierTaskExecution));
        when(taskFileStorage.readTaskExecutionOutput(laterFileEntry))
            .thenReturn(Map.of("body", Map.of("message", "the later reply")));

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new JobStatusApplicationEvent(JOB_ID, Job.Status.COMPLETED));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker, times(2))
            .send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent resultEvent = eventCaptor.getAllValues()
            .get(0);

        assertThat(resultEvent.getPayload())
            .isEqualTo(Map.of("event", "result", "result", Map.of("message", "the later reply")));
    }

    @Test
    void testTaskStartedIsEnrichedWithTaskNameAndType() {
        // A real WorkflowTask needs the static JsonUtils ObjectMapper; a mocked TaskExecution keeps this test
        // free of that global setup.
        TaskExecution taskExecution = org.mockito.Mockito.mock(TaskExecution.class);

        when(taskExecution.getName()).thenReturn("slack_1");
        when(taskExecution.getType()).thenReturn("slack/v1/sendMessage");

        when(taskExecutionService.getTaskExecution(7L)).thenReturn(taskExecution);

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new TaskStartedApplicationEvent(JOB_ID, 7L));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent sseStreamEvent = eventCaptor.getValue();

        assertThat(sseStreamEvent.getEventType()).isEqualTo(SseStreamEvent.EVENT_TYPE_TASK_STARTED);
        assertThat(sseStreamEvent.getPayload()).isEqualTo(
            Map.of(
                "event", "task_started",
                "payload",
                Map.of("taskExecutionId", 7L, "name", "slack_1", "type", "slack/v1/sendMessage")));
    }

    @Test
    void testTaskStartedFallsBackToBareIdWhenRowLoadFails() {
        when(taskExecutionService.getTaskExecution(7L)).thenThrow(new RuntimeException("row gone"));

        SseStreamApplicationEventListener listener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        listener.onApplicationEvent(new TaskStartedApplicationEvent(JOB_ID, 7L));

        ArgumentCaptor<SseStreamEvent> eventCaptor = ArgumentCaptor.forClass(SseStreamEvent.class);

        verify(messageBroker).send(eq(SseStreamMessageRoute.SSE_STREAM_EVENTS), eventCaptor.capture());

        SseStreamEvent sseStreamEvent = eventCaptor.getValue();

        assertThat(sseStreamEvent.getPayload()).isEqualTo(7L);
    }

    private static TaskExecution taggedTaskExecution(long id, FileEntry outputFileEntry) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.putMetadata(MetadataConstants.WEBHOOK_RESPONSE, true);
        taskExecution.setOutput(outputFileEntry);

        return taskExecution;
    }
}
