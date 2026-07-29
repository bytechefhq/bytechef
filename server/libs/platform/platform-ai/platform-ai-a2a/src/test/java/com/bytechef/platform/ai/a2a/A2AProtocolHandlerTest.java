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

package com.bytechef.platform.ai.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2AProtocolHandlerTest {

    @Test
    void testSendMessageExecutesAgentAndReturnsCompletedTask() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentResult.ofText("42");
        });

        MessageSendParams params = new MessageSendParams(userMessage("What is 6 times 7?"), null, null);

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        assertThat(response).isInstanceOf(SendMessageResponse.class);
        assertThat(capturedRequest.get()
            .agentId()).isEqualTo("agent-1");
        assertThat(capturedRequest.get()
            .text()).isEqualTo("What is 6 times 7?");

        Task task = (Task) ((SendMessageResponse) response).getResult();

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(A2AProtocolHandler.extractText(task.getStatus()
            .message())).isEqualTo("42");
    }

    @Test
    void testInputRequiredAgentResultProducesInputRequiredTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentResult
                .ofInputRequired("Approval required — resolve it at: https://example.com/resume/t"));

        MessageSendParams params = new MessageSendParams(userMessage("run it"), null, null);

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        Task task = (Task) ((SendMessageResponse) response).getResult();

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.INPUT_REQUIRED);
        assertThat(A2AProtocolHandler.extractText(task.getStatus()
            .message())).contains("Approval required");
    }

    @Test
    void testInputRequiredTaskRefreshesOnTasksGetOnceTheRunCompletes() {
        // First call pauses on approval; the poll (fired by tasks/get) reports the run completed.
        A2AAgentExecutor agentExecutor = new A2AAgentExecutor() {

            @Override
            public A2AAgentResult execute(A2AAgentRequest request) {
                return A2AAgentResult.ofInputRequired("Approval required", 42L);
            }

            @Override
            public A2AAgentResult pollRun(long jobId) {
                return A2AAgentResult.ofText("approved and finished");
            }
        };

        A2AProtocolHandler handler = new A2AProtocolHandler(agentExecutor);

        MessageSendParams params = new MessageSendParams(userMessage("run it"), null, null);

        JSONRPCResponse<?> sendResponse = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        Task pendingTask = (Task) ((SendMessageResponse) sendResponse).getResult();

        assertThat(pendingTask.getStatus()
            .state()).isEqualTo(TaskState.INPUT_REQUIRED);

        GetTaskResponse getResponse = (GetTaskResponse) handler.handleGetTask("req-2", pendingTask.getId());

        Task refreshedTask = (Task) getResponse.getResult();

        assertThat(refreshedTask.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(A2AProtocolHandler.extractText(refreshedTask.getStatus()
            .message())).isEqualTo("approved and finished");
    }

    @Test
    void testFailedAgentResultProducesFailedTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofError("boom"));

        MessageSendParams params = new MessageSendParams(userMessage("hi"), null, null);

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-2", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        Task task = (Task) ((SendMessageResponse) response).getResult();

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(A2AProtocolHandler.extractText(task.getStatus()
            .message())).contains("boom");
    }

    @Test
    void testAgentExceptionIsSurfacedAsFailedTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            throw new IllegalStateException("kaboom");
        });

        MessageSendParams params = new MessageSendParams(userMessage("hi"), null, null);

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-3", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        Task task = (Task) ((SendMessageResponse) response).getResult();

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
    }

    @Test
    void testUnknownMethodReturnsMethodNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("x"));

        JSONRPCResponse<?> response = handler.handle("agent-1", "req-4", "tasks/get", null);

        assertThat(response).isInstanceOf(JSONRPCErrorResponse.class);
        assertThat(((JSONRPCErrorResponse) response).getError()
            .getCode()).isEqualTo(-32601);
    }

    @Test
    void testMissingMessageReturnsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("x"));

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-5", A2AProtocolHandler.METHOD_SEND_MESSAGE, null);

        assertThat(response).isInstanceOf(JSONRPCErrorResponse.class);
        assertThat(((JSONRPCErrorResponse) response).getError()
            .getCode()).isEqualTo(-32602);
    }

    @Test
    void testStreamEmitsWorkingThenCompletedEvents() throws Exception {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("streamed answer"));

        MessageSendParams params = new MessageSendParams(userMessage("hi"), null, null);

        List<JSONRPCResponse<?>> events = new ArrayList<>();

        handler.handleStream("agent-1", "req-6", A2AProtocolHandler.METHOD_STREAM_MESSAGE, params, events::add);

        assertThat(events).hasSize(2);

        TaskStatusUpdateEvent workingEvent =
            (TaskStatusUpdateEvent) ((SendStreamingMessageResponse) events.get(0)).getResult();

        assertThat(workingEvent.getStatus()
            .state()).isEqualTo(TaskState.WORKING);
        assertThat(workingEvent.isFinal()).isFalse();

        TaskStatusUpdateEvent finalEvent =
            (TaskStatusUpdateEvent) ((SendStreamingMessageResponse) events.get(1)).getResult();

        assertThat(finalEvent.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(finalEvent.isFinal()).isTrue();
        assertThat(A2AProtocolHandler.extractText(finalEvent.getStatus()
            .message())).isEqualTo("streamed answer");
    }

    @Test
    void testStreamUnknownMethodEmitsMethodNotFound() throws Exception {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("x"));

        List<JSONRPCResponse<?>> events = new ArrayList<>();

        handler.handleStream("agent-1", "req-7", "tasks/get", null, events::add);

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(JSONRPCErrorResponse.class);
    }

    @Test
    void testGetTaskReturnsRecentlyProducedTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("done"));

        MessageSendParams params = new MessageSendParams(userMessage("hi"), null, null);

        SendMessageResponse sendResponse = (SendMessageResponse) handler.handle(
            "agent-1", "req-a", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);
        Task sentTask = (Task) sendResponse.getResult();

        JSONRPCResponse<?> getResponse = handler.handleGetTask("req-b", sentTask.getId());

        assertThat(getResponse).isInstanceOf(GetTaskResponse.class);

        Task fetchedTask = (Task) getResponse.getResult();

        assertThat(fetchedTask.getId()).isEqualTo(sentTask.getId());
    }

    @Test
    void testGetTaskUnknownIdReturnsTaskNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("x"));

        JSONRPCResponse<?> response = handler.handleGetTask("req-c", "no-such-task");

        assertThat(response).isInstanceOf(GetTaskResponse.class);
        assertThat(((GetTaskResponse) response).getError()).isNotNull();
    }

    @Test
    void testGetTaskResolvesUncachedTaskFromDurableState() {
        // The task was never produced by this handler instance — the cache evicted it, the process restarted, or the
        // poll landed on another node. It must still resolve through durable state rather than 404.
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentResult execute(A2AAgentRequest request) {
                return A2AAgentResult.ofText("unused");
            }

            @Override
            public A2AAgentResult pollTask(String taskId) {
                return "durable-task-1".equals(taskId) ? A2AAgentResult.ofText("resolved output") : null;
            }
        });

        JSONRPCResponse<?> response = handler.handleGetTask("req-durable", "durable-task-1");

        assertThat(response).isInstanceOf(GetTaskResponse.class);

        GetTaskResponse getTaskResponse = (GetTaskResponse) response;

        assertThat(getTaskResponse.getError()).isNull();

        Task task = getTaskResponse.getResult();

        assertThat(task.getId()).isEqualTo("durable-task-1");
        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void testGetTaskStillReturnsNotFoundWhenDurableStateResolvesNothing() {
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentResult execute(A2AAgentRequest request) {
                return A2AAgentResult.ofText("unused");
            }

            @Override
            public A2AAgentResult pollTask(String taskId) {
                // An id that does not verify against stored state resolves to nothing.
                return null;
            }
        });

        JSONRPCResponse<?> response = handler.handleGetTask("req-durable-miss", "forged-task-id");

        assertThat(response).isInstanceOf(GetTaskResponse.class);
        assertThat(((GetTaskResponse) response).getError()).isNotNull();
    }

    @Test
    void testInputRequiredTaskUsesDurableTaskId() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentResult.ofInputRequired("approve me", 42L, "job-resume-token"));

        SendMessageResponse sendResponse = (SendMessageResponse) handler.handle(
            "agent-1", "req-durable-id", A2AProtocolHandler.METHOD_SEND_MESSAGE,
            new MessageSendParams(userMessage("hi"), null, null));

        Task task = (Task) sendResponse.getResult();

        // The paused run's resume token becomes the task id, so a later tasks/get can recover it from the job.
        assertThat(task.getId()).isEqualTo("job-resume-token");
        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.INPUT_REQUIRED);
    }

    @Test
    void testStreamedPausedRunIsAlsoPollableByItsDurableId() throws Exception {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentResult.ofInputRequired("approve me", 42L, "stream-resume-token"));

        List<JSONRPCResponse<?>> events = new ArrayList<>();

        handler.handleStream(
            "agent-1", "req-stream-durable", A2AProtocolHandler.METHOD_STREAM_MESSAGE,
            new MessageSendParams(userMessage("run it"), null, null), events::add);

        // The streamed task keeps one stable id across both events — renaming it mid-stream would break correlation.
        TaskStatusUpdateEvent workingEvent =
            (TaskStatusUpdateEvent) ((SendStreamingMessageResponse) events.get(0)).getResult();
        TaskStatusUpdateEvent finalEvent =
            (TaskStatusUpdateEvent) ((SendStreamingMessageResponse) events.get(1)).getResult();

        assertThat(finalEvent.getTaskId()).isEqualTo(workingEvent.getTaskId());

        // ...and the durable id is registered as an additional handle, so the paused run stays pollable by it.
        GetTaskResponse getTaskResponse =
            (GetTaskResponse) handler.handleGetTask("req-poll", "stream-resume-token");

        assertThat(getTaskResponse.getError()).isNull();
        assertThat(((Task) getTaskResponse.getResult()).getId()).isEqualTo("stream-resume-token");
    }

    @Test
    void testCancelResolvesUncachedTaskFromDurableStateInsteadOfNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentResult execute(A2AAgentRequest request) {
                return A2AAgentResult.ofText("unused");
            }

            @Override
            public A2AAgentResult pollTask(String taskId) {
                return "durable-task-2".equals(taskId) ? A2AAgentResult.ofText("done") : null;
            }
        });

        JSONRPCResponse<?> response = handler.handleCancelTask("req-cancel-durable", "durable-task-2");

        // A task that tasks/get can still resolve must not report "not found" here — it reports not-cancelable.
        assertThat(response).isInstanceOf(CancelTaskResponse.class);
        assertThat(((CancelTaskResponse) response).getError()
            .getMessage()).contains("cannot be canceled");
    }

    @Test
    void testCancelKnownTaskReturnsNotCancelable() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentResult.ofText("done"));

        SendMessageResponse sendResponse = (SendMessageResponse) handler.handle(
            "agent-1", "req-d", A2AProtocolHandler.METHOD_SEND_MESSAGE,
            new MessageSendParams(userMessage("hi"), null, null));
        Task sentTask = (Task) sendResponse.getResult();

        JSONRPCResponse<?> response = handler.handleCancelTask("req-e", sentTask.getId());

        assertThat(response).isInstanceOf(CancelTaskResponse.class);
        assertThat(((CancelTaskResponse) response).getError()).isNotNull();
    }

    private static Message userMessage(String text) {
        return new Message.Builder()
            .role(Message.Role.USER)
            .parts(new TextPart(text))
            .messageId("m-1")
            .build();
    }
}
