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

import com.bytechef.platform.ai.a2a.A2AAgentResult.Completed;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2AProtocolHandlerTest {

    private static final A2ATaskReference TASK_REFERENCE = new A2ATaskReference("durable-task", "durable-context");

    @Test
    void testSendMessageStartsAgentAndReturnsCompletedTask() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentRun.of(new Completed("42", null));
        });

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("What is 6 times 7?"));

        assertThat(response).isInstanceOf(SendMessageResponse.class);
        assertThat(capturedRequest.get()
            .agentId()).isEqualTo("agent-1");
        assertThat(capturedRequest.get()
            .text()).isEqualTo("What is 6 times 7?");

        Task task = sentTask(response);

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(messageText(task)).isEqualTo("42");
    }

    @Test
    void testSkillIdIsTakenFromRequestMetadata() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentRun.of(new Completed("ok", null));
        });

        MessageSendParams params = new MessageSendParams(
            userMessage("hi"), null, Map.of(A2AProtocolHandler.SKILL_ID_METADATA_KEY, "summarize"));

        handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, params);

        assertThat(capturedRequest.get()
            .skillId()).isEqualTo("summarize");
    }

    @Test
    void testSkillIdFallsBackToMessageMetadata() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentRun.of(new Completed("ok", null));
        });

        Message message = new Message.Builder()
            .role(Message.Role.USER)
            .parts(new TextPart("hi"))
            .messageId("m-2")
            .metadata(Map.of(A2AProtocolHandler.SKILL_ID_METADATA_KEY, "translate"))
            .build();

        handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, new MessageSendParams(message, null, null));

        assertThat(capturedRequest.get()
            .skillId()).isEqualTo("translate");
    }

    @Test
    void testContextIdOfTheMessageIsForwardedToTheAgent() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentRun.of(new Completed("ok", null));
        });

        Message message = new Message.Builder()
            .role(Message.Role.USER)
            .parts(new TextPart("hi"))
            .messageId("m-3")
            .contextId("conversation-7")
            .build();

        Task task = sentTask(
            handler.handle(
                "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE,
                new MessageSendParams(message, null, null)));

        assertThat(capturedRequest.get()
            .contextId()).isEqualTo("conversation-7");
        assertThat(task.getContextId()).isEqualTo("conversation-7");
    }

    @Test
    void testAContextIdIsGeneratedWhenTheMessageHasNone() {
        AtomicReference<A2AAgentRequest> capturedRequest = new AtomicReference<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            capturedRequest.set(request);

            return A2AAgentRun.of(new Completed("ok", null));
        });

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(capturedRequest.get()
            .contextId()).isNotBlank();
        assertThat(task.getContextId()).isEqualTo(capturedRequest.get()
            .contextId());
    }

    @Test
    void testInputRequiredResultUsesTheDurableTaskReference() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(
                A2AAgentResult.ofInputRequired(
                    "Approval required — resolve it at: https://example.com/approval", TASK_REFERENCE)));

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("run it")));

        assertThat(task.getId()).isEqualTo("durable-task");
        assertThat(task.getContextId()).isEqualTo("durable-context");
        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.INPUT_REQUIRED);
        assertThat(messageText(task)).contains("Approval required");
    }

    @Test
    void testWorkingResultUsesTheDurableTaskReference() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofWorking("still running", TASK_REFERENCE)));

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("run it")));

        assertThat(task.getId()).isEqualTo("durable-task");
        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.WORKING);
    }

    @Test
    void testGetTaskReflectsTheCurrentStateOfADurableTaskOnEveryPoll() {
        AtomicReference<A2AAgentResult> pollResult = new AtomicReference<>(
            A2AAgentResult.ofWorking("still running", TASK_REFERENCE));

        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentRun start(A2AAgentRequest request) {
                return A2AAgentRun.of(A2AAgentResult.ofWorking("still running", TASK_REFERENCE));
            }

            @Override
            public A2AAgentResult pollTask(String agentId, String taskId) {
                return pollResult.get();
            }
        });

        handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("run it"));

        assertThat(fetchedTask(handler.handleGetTask("agent-1", "req-2", "durable-task")).getStatus()
            .state()).isEqualTo(TaskState.WORKING);

        pollResult.set(A2AAgentResult.ofCompleted("finished late", TASK_REFERENCE));

        Task task = fetchedTask(handler.handleGetTask("agent-1", "req-3", "durable-task"));

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(task.getContextId()).isEqualTo("durable-context");
        assertThat(messageText(task)).isEqualTo("finished late");
    }

    @Test
    void testFailedAgentResultProducesFailedTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("boom")));

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(messageText(task)).isEqualTo("Error: boom");
    }

    @Test
    void testAgentStartExceptionIsSurfacedAsAGenericFailedTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            throw new IllegalStateException("jdbc:postgresql://internal-host/secret");
        });

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(messageText(task)).isEqualTo("Error: " + A2AProtocolHandler.AGENT_FAILURE_MESSAGE);
    }

    @Test
    void testRunFailureKeepsTheDurableTaskId() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.started(
                TASK_REFERENCE, CompletableFuture.failedFuture(new IllegalStateException("broker down"))));

        Task task = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(task.getId()).isEqualTo("durable-task");
        assertThat(task.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(messageText(task)).isEqualTo("Error: " + A2AProtocolHandler.AGENT_FAILURE_MESSAGE);
    }

    @Test
    void testInvalidSkillReturnsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            throw new A2AInvalidParamsException("No skill with id x is exposed");
        });

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi"));

        assertThat(response.getError()).isInstanceOf(InvalidParamsError.class);
        assertThat(response.getError()
            .getMessage()).isEqualTo("No skill with id x is exposed");
    }

    @Test
    void testUnknownMethodReturnsMethodNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        JSONRPCResponse<?> response = handler.handle("agent-1", "req-1", "unknown/method", null);

        assertThat(response.getError()).isInstanceOf(MethodNotFoundError.class);
    }

    @Test
    void testMissingMessageReturnsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, null);

        assertThat(response.getError()).isInstanceOf(InvalidParamsError.class);
        assertThat(response.getError()
            .getMessage()).isEqualTo("A message is required");
    }

    @Test
    void testMessageWithoutTextReturnsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("   "));

        assertThat(response.getError()).isInstanceOf(InvalidParamsError.class);
        assertThat(response.getError()
            .getMessage()).isEqualTo("The message has no text content");
    }

    @Test
    void testMessageContinuingATaskIsRejectedWithoutStartingARun() {
        AtomicInteger startCount = new AtomicInteger();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            startCount.incrementAndGet();

            return A2AAgentRun.of(new Completed("x", null));
        });

        Message message = new Message.Builder()
            .role(Message.Role.USER)
            .parts(new TextPart("approved"))
            .messageId("m-4")
            .taskId("durable-task")
            .build();

        JSONRPCResponse<?> response = handler.handle(
            "agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, new MessageSendParams(message, null, null));

        assertThat(response.getError()).isInstanceOf(UnsupportedOperationError.class);
        assertThat(response.getError()
            .getMessage()).isEqualTo(A2AProtocolHandler.TASK_CONTINUATION_MESSAGE);
        assertThat(startCount).hasValue(0);
    }

    @Test
    void testStreamEmitsWorkingThenCompletedEvents() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(new Completed("streamed answer", null)));

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-6", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getEvents()).hasSize(2);

        TaskStatusUpdateEvent workingEvent = statusEvent(sink.getEvents()
            .get(0));

        assertThat(workingEvent.getStatus()
            .state()).isEqualTo(TaskState.WORKING);
        assertThat(workingEvent.isFinal()).isFalse();

        TaskStatusUpdateEvent finalEvent = statusEvent(sink.getEvents()
            .get(1));

        assertThat(finalEvent.getStatus()
            .state()).isEqualTo(TaskState.COMPLETED);
        assertThat(finalEvent.isFinal()).isTrue();
        assertThat(finalEvent.getTaskId()).isEqualTo(workingEvent.getTaskId());
        assertThat(A2AProtocolHandler.extractText(finalEvent.getStatus()
            .message())).isEqualTo("streamed answer");
        assertThat(sink.isCompleted()).isTrue();
    }

    @Test
    void testStreamWaitsForTheRunWithoutBlockingAndUsesTheDurableTaskId() {
        CompletableFuture<A2AAgentResult> result = new CompletableFuture<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.started(TASK_REFERENCE, result));

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-6", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getEvents()).hasSize(1);
        assertThat(statusEvent(sink.getEvents()
            .get(0)).getTaskId()).isEqualTo("durable-task");
        assertThat(sink.isCompleted()).isFalse();

        result.complete(A2AAgentResult.ofInputRequired("approve me", TASK_REFERENCE));

        TaskStatusUpdateEvent finalEvent = statusEvent(sink.getEvents()
            .get(1));

        assertThat(finalEvent.getTaskId()).isEqualTo("durable-task");
        assertThat(finalEvent.getContextId()).isEqualTo("durable-context");
        assertThat(finalEvent.getStatus()
            .state()).isEqualTo(TaskState.INPUT_REQUIRED);
        assertThat(sink.isCompleted()).isTrue();
    }

    @Test
    void testStreamRunFailureEmitsAFinalFailedEvent() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.started(
                TASK_REFERENCE, CompletableFuture.failedFuture(new IllegalStateException("broker down"))));

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-6", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        TaskStatusUpdateEvent finalEvent = statusEvent(sink.getEvents()
            .get(1));

        assertThat(finalEvent.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(finalEvent.isFinal()).isTrue();
        assertThat(sink.isCompleted()).isTrue();
    }

    @Test
    void testStreamInvalidSkillEmitsInvalidParamsAndCompletes() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            throw new A2AInvalidParamsException("Set metadata.skillId");
        });

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-6", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getEvents()).hasSize(1);
        assertThat(sink.getEvents()
            .get(0)
            .getError()).isInstanceOf(InvalidParamsError.class);
        assertThat(sink.isCompleted()).isTrue();
    }

    @Test
    void testStreamUnknownMethodEmitsMethodNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-7", "tasks/get", null, sink);

        assertThat(sink.getEvents()).hasSize(1);
        assertThat(sink.getEvents()
            .get(0)
            .getError()).isInstanceOf(MethodNotFoundError.class);
        assertThat(sink.isCompleted()).isTrue();
    }

    @Test
    void testStreamWithoutParamsEmitsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-7", A2AProtocolHandler.METHOD_STREAM_MESSAGE, null, sink);

        assertThat(sink.getEvents()).hasSize(1);
        assertThat(sink.getEvents()
            .get(0)
            .getError()).isInstanceOf(InvalidParamsError.class);
    }

    @Test
    void testStreamSendFailureCompletesTheSinkWithError() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(new Completed("x", null)));

        RecordingSink sink = new RecordingSink(new IOException("client disconnected"));

        handler.handleStream("agent-1", "req-7", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getError()).isInstanceOf(IOException.class);
        assertThat(sink.isCompleted()).isFalse();
    }

    @Test
    void testStreamFinalSendFailureCompletesTheSinkWithErrorAndKeepsTheTask() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("A2A server is disabled")));

        RecordingSink sink = new RecordingSink(new IOException("client disconnected"), 2, null);

        handler.handleStream("agent-1", "req-8", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getEvents()).hasSize(1);
        assertThat(sink.getError()).isInstanceOf(IOException.class);
        assertThat(sink.isCompleted()).isFalse();

        String taskId = statusEvent(sink.getEvents()
            .get(0)).getTaskId();

        assertThat(fetchedTask(handler.handleGetTask("agent-1", "req-9", taskId)).getStatus()
            .state()).isEqualTo(TaskState.FAILED);
    }

    @Test
    void testStreamCompletionFailureCompletesTheSinkWithError() {
        CompletableFuture<A2AAgentResult> result = new CompletableFuture<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.started(TASK_REFERENCE, result));

        RecordingSink sink = new RecordingSink(null, 0, new IllegalStateException("emitter already completed"));

        handler.handleStream("agent-1", "req-8", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        result.complete(A2AAgentResult.ofCompleted("done", TASK_REFERENCE));

        assertThat(sink.getEvents()).hasSize(2);
        assertThat(sink.getError()).isInstanceOf(IllegalStateException.class)
            .hasMessage("emitter already completed");
    }

    @Test
    void testStreamStartFailureEmitsWorkingThenFailedAndScopesTheTaskToTheServer() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> {
            throw new IllegalStateException("jdbc:postgresql://internal-host/secret");
        });

        RecordingSink sink = new RecordingSink();

        handler.handleStream("agent-1", "req-8", A2AProtocolHandler.METHOD_STREAM_MESSAGE, sendParams("hi"), sink);

        assertThat(sink.getEvents()).hasSize(2);
        assertThat(statusEvent(sink.getEvents()
            .get(0)).getStatus()
                .state()).isEqualTo(TaskState.WORKING);

        TaskStatusUpdateEvent finalEvent = statusEvent(sink.getEvents()
            .get(1));

        assertThat(finalEvent.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(A2AProtocolHandler.extractText(finalEvent.getStatus()
            .message())).isEqualTo("Error: " + A2AProtocolHandler.AGENT_FAILURE_MESSAGE);
        assertThat(sink.isCompleted()).isTrue();

        assertThat(fetchedTask(handler.handleGetTask("agent-1", "req-9", finalEvent.getTaskId())).getStatus()
            .state()).isEqualTo(TaskState.FAILED);
        assertThat(handler.handleGetTask("agent-2", "req-10", finalEvent.getTaskId())
            .getError()).isInstanceOf(TaskNotFoundError.class);
    }

    @Test
    void testEphemeralTasksAreCappedByEvictingTheOldest() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("A2A server is disabled")));

        List<String> taskIds = new ArrayList<>();

        for (int index = 0; index <= A2AProtocolHandler.MAX_EPHEMERAL_TASKS; index++) {
            Task task = sentTask(
                handler.handle("agent-1", "req-" + index, A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

            taskIds.add(task.getId());
        }

        assertThat(handler.handleGetTask("agent-1", "req-first", taskIds.getFirst())
            .getError()).isInstanceOf(TaskNotFoundError.class);
        assertThat(fetchedTask(handler.handleGetTask("agent-1", "req-last", taskIds.getLast())).getId())
            .isEqualTo(taskIds.getLast());
    }

    @Test
    void testGetTaskReturnsAFailedTaskThatNeverStartedARun() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("A2A server is disabled")));

        Task sentTask = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        Task fetchedTask = fetchedTask(handler.handleGetTask("agent-1", "req-2", sentTask.getId()));

        assertThat(fetchedTask.getId()).isEqualTo(sentTask.getId());
        assertThat(fetchedTask.getStatus()
            .state()).isEqualTo(TaskState.FAILED);
    }

    @Test
    void testGetTaskUnknownIdReturnsTaskNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        JSONRPCResponse<?> response = handler.handleGetTask("agent-1", "req-1", "does-not-exist");

        assertThat(response.getError()).isInstanceOf(TaskNotFoundError.class);
    }

    @Test
    void testGetTaskBlankIdReturnsInvalidParams() {
        A2AProtocolHandler handler = new A2AProtocolHandler(request -> A2AAgentRun.of(new Completed("x", null)));

        assertThat(handler.handleGetTask("agent-1", "req-1", " ")
            .getError()).isInstanceOf(InvalidParamsError.class);
        assertThat(handler.handleCancelTask("agent-1", "req-1", null)
            .getError()).isInstanceOf(InvalidParamsError.class);
    }

    @Test
    void testTaskIsNotVisibleThroughAnotherServer() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("private failure")));

        Task sentTask = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(handler.handleGetTask("agent-2", "req-2", sentTask.getId())
            .getError()).isInstanceOf(TaskNotFoundError.class);
        assertThat(handler.handleCancelTask("agent-2", "req-3", sentTask.getId())
            .getError()).isInstanceOf(TaskNotFoundError.class);
    }

    @Test
    void testDurableLookupIsScopedToTheCallingServer() {
        List<String> polledAgentIds = new ArrayList<>();

        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentRun start(A2AAgentRequest request) {
                return A2AAgentRun.of(A2AAgentResult.ofWorking("still running", TASK_REFERENCE));
            }

            @Override
            public @Nullable A2AAgentResult pollTask(String agentId, String taskId) {
                polledAgentIds.add(agentId);

                return "agent-1".equals(agentId) ? A2AAgentResult.ofWorking("still running", TASK_REFERENCE) : null;
            }
        });

        handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi"));

        assertThat(handler.handleGetTask("agent-2", "req-2", "durable-task")
            .getError()).isInstanceOf(TaskNotFoundError.class);
        assertThat(handler.handleCancelTask("agent-2", "req-3", "durable-task")
            .getError()).isInstanceOf(TaskNotFoundError.class);
        assertThat(polledAgentIds).containsExactly("agent-2", "agent-2");
    }

    @Test
    void testGetTaskLookupFailureReturnsInternalErrorInsteadOfNotFound() {
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentRun start(A2AAgentRequest request) {
                return A2AAgentRun.of(new Completed("unused", null));
            }

            @Override
            public A2AAgentResult pollTask(String agentId, String taskId) {
                throw new IllegalStateException("database unavailable");
            }
        });

        assertThat(handler.handleGetTask("agent-1", "req-1", "durable-task")
            .getError()).isInstanceOf(InternalError.class);
        assertThat(handler.handleCancelTask("agent-1", "req-2", "durable-task")
            .getError()).isInstanceOf(InternalError.class);
    }

    @Test
    void testCancelDurableCompletedTaskReturnsNotCancelable() {
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentRun start(A2AAgentRequest request) {
                return A2AAgentRun.of(new Completed("unused", null));
            }

            @Override
            public @Nullable A2AAgentResult pollTask(String agentId, String taskId) {
                return "durable-task".equals(taskId) ? A2AAgentResult.ofCompleted("done", TASK_REFERENCE) : null;
            }
        });

        JSONRPCResponse<?> response = handler.handleCancelTask("agent-1", "req-1", "durable-task");

        assertThat(response).isInstanceOf(CancelTaskResponse.class);
        assertThat(response.getError()).isInstanceOf(TaskNotCancelableError.class);
        assertThat(response.getError()
            .getMessage()).isEqualTo("Task already reached the completed state and cannot be canceled");
    }

    @Test
    void testCancelEphemeralFailedTaskReturnsNotCancelable() {
        A2AProtocolHandler handler = new A2AProtocolHandler(
            request -> A2AAgentRun.of(A2AAgentResult.ofFailed("A2A server is disabled")));

        Task sentTask = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        assertThat(handler.handleCancelTask("agent-1", "req-2", sentTask.getId())
            .getError()
            .getMessage()).isEqualTo("Task already reached the failed state and cannot be canceled");
    }

    @Test
    void testCancelPausedTaskDoesNotClaimItCompleted() {
        A2AProtocolHandler handler = new A2AProtocolHandler(new A2AAgentExecutor() {

            @Override
            public A2AAgentRun start(A2AAgentRequest request) {
                return A2AAgentRun.of(A2AAgentResult.ofInputRequired("Approval required", TASK_REFERENCE));
            }

            @Override
            public A2AAgentResult pollTask(String agentId, String taskId) {
                return A2AAgentResult.ofInputRequired("Approval required", TASK_REFERENCE);
            }
        });

        Task sentTask = sentTask(
            handler.handle("agent-1", "req-1", A2AProtocolHandler.METHOD_SEND_MESSAGE, sendParams("hi")));

        CancelTaskResponse cancelTaskResponse = (CancelTaskResponse) handler.handleCancelTask(
            "agent-1", "req-2", sentTask.getId());

        assertThat(cancelTaskResponse.getError()
            .getMessage()).isEqualTo("Task is input-required and cannot be canceled through A2A");
    }

    private static Task fetchedTask(JSONRPCResponse<?> response) {
        assertThat(response).isInstanceOf(GetTaskResponse.class);
        assertThat(response.getError()).isNull();

        return ((GetTaskResponse) response).getResult();
    }

    private static String messageText(Task task) {
        return A2AProtocolHandler.extractText(task.getStatus()
            .message());
    }

    private static MessageSendParams sendParams(String text) {
        return new MessageSendParams(userMessage(text), null, null);
    }

    private static Task sentTask(JSONRPCResponse<?> response) {
        assertThat(response).isInstanceOf(SendMessageResponse.class);

        return (Task) ((SendMessageResponse) response).getResult();
    }

    private static TaskStatusUpdateEvent statusEvent(JSONRPCResponse<?> event) {
        assertThat(event).isNotInstanceOf(JSONRPCErrorResponse.class);

        return (TaskStatusUpdateEvent) ((SendStreamingMessageResponse) event).getResult();
    }

    private static Message userMessage(String text) {
        return new Message.Builder()
            .role(Message.Role.USER)
            .parts(new TextPart(text))
            .messageId("m-1")
            .build();
    }

    private static final class RecordingSink implements A2AProtocolHandler.StreamSink {

        private final List<JSONRPCResponse<?>> events = new ArrayList<>();
        private final @Nullable RuntimeException completeFailure;
        private final int failingSendNumber;
        private final @Nullable Exception sendFailure;
        private boolean completed;
        private @Nullable Throwable error;
        private int sendCount;

        private RecordingSink() {
            this(null);
        }

        private RecordingSink(@Nullable Exception sendFailure) {
            this(sendFailure, 0, null);
        }

        private RecordingSink(
            @Nullable Exception sendFailure, int failingSendNumber, @Nullable RuntimeException completeFailure) {

            this.completeFailure = completeFailure;
            this.failingSendNumber = failingSendNumber;
            this.sendFailure = sendFailure;
        }

        @Override
        public void send(JSONRPCResponse<?> event) throws Exception {
            sendCount++;

            if (sendFailure != null && (failingSendNumber == 0 || failingSendNumber == sendCount)) {
                throw sendFailure;
            }

            events.add(event);
        }

        @Override
        public void complete() {
            if (completeFailure != null) {
                throw completeFailure;
            }

            completed = true;
        }

        @Override
        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public void completeWithError(Throwable throwable) {
            error = throwable;
        }

        private List<JSONRPCResponse<?>> getEvents() {
            return events;
        }

        private @Nullable Throwable getError() {
            return error;
        }

        private boolean isCompleted() {
            return completed;
        }
    }
}
