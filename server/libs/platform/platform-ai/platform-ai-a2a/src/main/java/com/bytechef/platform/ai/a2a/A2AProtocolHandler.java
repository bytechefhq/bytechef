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

import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class A2AProtocolHandler {

    public static final String METHOD_SEND_MESSAGE = "message/send";
    public static final String METHOD_STREAM_MESSAGE = "message/stream";
    public static final String METHOD_GET_TASK = "tasks/get";
    public static final String METHOD_CANCEL_TASK = "tasks/cancel";
    public static final String SKILL_ID_METADATA_KEY = "skillId";

    static final String AGENT_FAILURE_MESSAGE = "The agent failed to process the message";
    static final String TASK_CONTINUATION_MESSAGE =
        "Continuing an existing task is not supported; if the task is waiting for approval, resolve it as its " +
            "status message describes, then poll tasks/get for the result";

    static final int MAX_EPHEMERAL_TASKS = 1000;

    private static final Logger log = LoggerFactory.getLogger(A2AProtocolHandler.class);

    private final A2AAgentExecutor agentExecutor;

    private final Map<TaskKey, Task> ephemeralTasks = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<TaskKey, Task> eldest) {
                return size() > MAX_EPHEMERAL_TASKS;
            }
        });

    public A2AProtocolHandler(A2AAgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    public interface StreamSink {

        void send(JSONRPCResponse<?> event) throws Exception;

        void complete();

        void completeWithError(Throwable throwable);
    }

    public JSONRPCResponse<?> handle(
        String agentId, @Nullable Object requestId, String method, @Nullable MessageSendParams params) {

        if (!METHOD_SEND_MESSAGE.equals(method)) {
            return new JSONRPCErrorResponse(requestId, new MethodNotFoundError());
        }

        JSONRPCError validationError = validateMessage(params);

        if (validationError != null) {
            return new JSONRPCErrorResponse(requestId, validationError);
        }

        Message inboundMessage = params.message();
        String contextId = resolveContextId(inboundMessage);

        A2AAgentRun agentRun;

        try {
            agentRun = agentExecutor.start(toAgentRequest(agentId, contextId, params));
        } catch (A2AInvalidParamsException invalidParamsException) {
            return new JSONRPCErrorResponse(requestId, new InvalidParamsError(invalidParamsException.getMessage()));
        } catch (Exception exception) {
            agentRun = A2AAgentRun.of(failedResult(exception, null, inboundMessage.getMessageId()));
        }

        A2AAgentResult agentResult = awaitResult(agentRun, inboundMessage.getMessageId());

        return new SendMessageResponse(requestId, toTask(agentId, contextId, agentResult));
    }

    public JSONRPCResponse<?> handleGetTask(String agentId, @Nullable Object requestId, @Nullable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return new JSONRPCErrorResponse(requestId, new InvalidParamsError("A task id is required"));
        }

        Task task = ephemeralTasks.get(new TaskKey(agentId, taskId));

        if (task != null) {
            return new GetTaskResponse(requestId, task);
        }

        A2AAgentResult agentResult;

        try {
            agentResult = agentExecutor.pollTask(agentId, taskId);
        } catch (Exception exception) {
            log.error("Failed to load A2A task {}", taskId, exception);

            return new GetTaskResponse(requestId, new InternalError("Failed to load the task"));
        }

        if (agentResult == null) {
            return new GetTaskResponse(requestId, new TaskNotFoundError());
        }

        return new GetTaskResponse(requestId, buildTask(taskId, resolveContextId(taskId, agentResult), agentResult));
    }

    public JSONRPCResponse<?> handleCancelTask(String agentId, @Nullable Object requestId, @Nullable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return new JSONRPCErrorResponse(requestId, new InvalidParamsError("A task id is required"));
        }

        TaskState taskState;

        Task task = ephemeralTasks.get(new TaskKey(agentId, taskId));

        if (task == null) {
            A2AAgentResult agentResult;

            try {
                agentResult = agentExecutor.pollTask(agentId, taskId);
            } catch (Exception exception) {
                log.error("Failed to load A2A task {}", taskId, exception);

                return new CancelTaskResponse(requestId, new InternalError("Failed to load the task"));
            }

            if (agentResult == null) {
                return new CancelTaskResponse(requestId, new TaskNotFoundError());
            }

            taskState = resolveTaskState(agentResult);
        } else {
            TaskStatus taskStatus = task.getStatus();

            taskState = taskStatus.state();
        }

        String message = taskState.isFinal()
            ? "Task already reached the " + taskState.asString() + " state and cannot be canceled"
            : "Task is " + taskState.asString() + " and cannot be canceled through A2A";

        return new CancelTaskResponse(requestId, new TaskNotCancelableError(message));
    }

    public void handleStream(
        String agentId, @Nullable Object requestId, String method, @Nullable MessageSendParams params,
        StreamSink sink) {

        if (!METHOD_STREAM_MESSAGE.equals(method)) {
            sendFinalEvent(sink, new JSONRPCErrorResponse(requestId, new MethodNotFoundError()));

            return;
        }

        JSONRPCError validationError = validateMessage(params);

        if (validationError != null) {
            sendFinalEvent(sink, new JSONRPCErrorResponse(requestId, validationError));

            return;
        }

        Message inboundMessage = params.message();
        String contextId = resolveContextId(inboundMessage);

        A2AAgentRun agentRun;

        try {
            agentRun = agentExecutor.start(toAgentRequest(agentId, contextId, params));
        } catch (A2AInvalidParamsException invalidParamsException) {
            sendFinalEvent(
                sink, new JSONRPCErrorResponse(requestId, new InvalidParamsError(invalidParamsException.getMessage())));

            return;
        } catch (Exception exception) {
            agentRun = A2AAgentRun.of(failedResult(exception, null, inboundMessage.getMessageId()));
        }

        A2ATaskReference taskReference = agentRun.taskReference();

        String taskId = taskReference == null ? newId() : taskReference.taskId();
        String streamContextId = taskReference == null ? contextId : taskReference.contextId();

        try {
            sink.send(
                new SendStreamingMessageResponse(
                    requestId,
                    new TaskStatusUpdateEvent(
                        taskId, new TaskStatus(TaskState.WORKING), streamContextId, false, Map.of())));
        } catch (Exception exception) {
            log.warn("Failed to send the initial event of A2A task {}", taskId, exception);

            sink.completeWithError(exception);

            return;
        }

        String messageId = inboundMessage.getMessageId();

        agentRun.result()
            .whenComplete((agentResult, throwable) -> {
                try {
                    finishStream(
                        agentId, requestId, taskId, streamContextId, taskReference, messageId, agentResult, throwable,
                        sink);
                } catch (RuntimeException runtimeException) {
                    log.error("Failed to finish the A2A stream of task {}", taskId, runtimeException);

                    sink.completeWithError(runtimeException);
                }
            });
    }

    private void finishStream(
        String agentId, @Nullable Object requestId, String taskId, String streamContextId,
        @Nullable A2ATaskReference taskReference, @Nullable String messageId, @Nullable A2AAgentResult agentResult,
        @Nullable Throwable throwable, StreamSink sink) {

        A2AAgentResult finalResult = throwable == null && agentResult != null
            ? agentResult : failedResult(throwable, taskReference, messageId);

        Task task = buildTask(taskId, streamContextId, finalResult);

        if (taskReference == null) {
            ephemeralTasks.put(new TaskKey(agentId, taskId), task);
        }

        sendFinalEvent(
            sink,
            new SendStreamingMessageResponse(
                requestId, new TaskStatusUpdateEvent(taskId, task.getStatus(), streamContextId, true, Map.of())));
    }

    static String extractText(Message message) {
        StringBuilder textBuilder = new StringBuilder();

        List<Part<?>> parts = message.getParts();

        if (parts != null) {
            for (Part<?> part : parts) {
                if (part instanceof TextPart textPart) {
                    textBuilder.append(textPart.getText());
                }
            }
        }

        return textBuilder.toString();
    }

    private static void sendFinalEvent(StreamSink sink, JSONRPCResponse<?> event) {
        try {
            sink.send(event);
        } catch (Exception exception) {
            log.warn("Failed to send the final A2A stream event", exception);

            sink.completeWithError(exception);

            return;
        }

        sink.complete();
    }

    private static @Nullable JSONRPCError validateMessage(@Nullable MessageSendParams params) {
        if (params == null || params.message() == null) {
            return new InvalidParamsError("A message is required");
        }

        Message message = params.message();

        if (message.getTaskId() != null) {
            return new UnsupportedOperationError(null, TASK_CONTINUATION_MESSAGE, null);
        }

        String text = extractText(message);

        if (text.isBlank()) {
            return new InvalidParamsError("The message has no text content");
        }

        return null;
    }

    private static String resolveContextId(Message message) {
        String contextId = message.getContextId();

        return contextId == null || contextId.isBlank() ? newId() : contextId;
    }

    private static String resolveContextId(String taskId, A2AAgentResult agentResult) {
        A2ATaskReference taskReference = agentResult.taskReference();

        return taskReference == null ? taskId : taskReference.contextId();
    }

    private static @Nullable String extractSkillId(MessageSendParams params) {
        Object skillId = getSkillIdMetadata(params.metadata());

        if (skillId == null) {
            Message message = params.message();

            skillId = getSkillIdMetadata(message.getMetadata());
        }

        return skillId == null ? null : String.valueOf(skillId);
    }

    private static @Nullable Object getSkillIdMetadata(@Nullable Map<String, Object> metadata) {
        return metadata == null ? null : metadata.get(SKILL_ID_METADATA_KEY);
    }

    private static A2AAgentRequest toAgentRequest(String agentId, String contextId, MessageSendParams params) {
        Message message = params.message();

        return new A2AAgentRequest(agentId, extractText(message), contextId, extractSkillId(params));
    }

    private static A2AAgentResult awaitResult(A2AAgentRun agentRun, @Nullable String messageId) {
        try {
            return agentRun.result()
                .join();
        } catch (RuntimeException runtimeException) {
            return failedResult(runtimeException, agentRun.taskReference(), messageId);
        }
    }

    private static A2AAgentResult failedResult(
        @Nullable Throwable throwable, @Nullable A2ATaskReference taskReference, @Nullable String messageId) {

        log.error("A2A agent failed to process message {}", messageId, throwable);

        return taskReference == null
            ? A2AAgentResult.ofFailed(AGENT_FAILURE_MESSAGE)
            : A2AAgentResult.ofFailed(AGENT_FAILURE_MESSAGE, taskReference);
    }

    private Task toTask(String agentId, String contextId, A2AAgentResult agentResult) {
        A2ATaskReference taskReference = agentResult.taskReference();

        if (taskReference != null) {
            return buildTask(taskReference.taskId(), taskReference.contextId(), agentResult);
        }

        String taskId = newId();

        Task task = buildTask(taskId, contextId, agentResult);

        ephemeralTasks.put(new TaskKey(agentId, taskId), task);

        return task;
    }

    private static Task buildTask(String taskId, String contextId, A2AAgentResult agentResult) {
        Message agentMessage = buildAgentMessage(taskId, contextId, agentResult);

        return new Task(
            taskId, contextId, new TaskStatus(resolveTaskState(agentResult), agentMessage, null), List.of(), List.of(),
            null);
    }

    private static TaskState resolveTaskState(A2AAgentResult agentResult) {
        return switch (agentResult) {
            case A2AAgentResult.Completed _ -> TaskState.COMPLETED;
            case A2AAgentResult.Failed _ -> TaskState.FAILED;
            case A2AAgentResult.InputRequired _ -> TaskState.INPUT_REQUIRED;
            case A2AAgentResult.Working _ -> TaskState.WORKING;
        };
    }

    private static Message buildAgentMessage(String taskId, String contextId, A2AAgentResult agentResult) {
        String responseText = switch (agentResult) {
            case A2AAgentResult.Completed completed -> completed.text();
            case A2AAgentResult.Failed failed -> "Error: " + failed.errorMessage();
            case A2AAgentResult.InputRequired inputRequired -> inputRequired.text();
            case A2AAgentResult.Working working -> working.text();
        };

        return new Message.Builder()
            .role(Message.Role.AGENT)
            .parts(new TextPart(responseText))
            .messageId(newId())
            .contextId(contextId)
            .taskId(taskId)
            .build();
    }

    private static String newId() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }

    private record TaskKey(String agentId, String taskId) {

        @Override
        public String toString() {
            return "TaskKey{taskId=" + taskId + "}";
        }
    }
}
