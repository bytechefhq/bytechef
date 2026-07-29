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
import io.a2a.spec.InvalidParamsError;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Handles A2A (Agent2Agent) JSON-RPC requests for a single exposed agent, bridging them to an {@link A2AAgentExecutor}.
 * The addressed agent is identified by the endpoint (URL path), not by the message body, so the caller supplies the
 * {@code agentId}.
 *
 * <p>
 * Two methods are handled. {@code message/send} runs the agent synchronously and returns a completed (or failed)
 * {@link Task}. {@code message/stream} runs the same agent but emits a sequence of {@link TaskStatusUpdateEvent}s to a
 * {@link StreamSink} — a {@code working} event, then a final {@code completed}/{@code failed} event carrying the
 * response — for SSE transports. This is event-level, not token-level, streaming: the agent card therefore keeps
 * advertising {@code streaming=false}, but a streaming client that calls {@code message/stream} still gets a valid SSE
 * response. Unknown methods surface as method-not-found; a missing/empty message as invalid-params.
 * </p>
 *
 * @author Ivica Cardic
 */
public class A2AProtocolHandler {

    public static final String METHOD_SEND_MESSAGE = "message/send";
    public static final String METHOD_STREAM_MESSAGE = "message/stream";
    public static final String METHOD_GET_TASK = "tasks/get";
    public static final String METHOD_CANCEL_TASK = "tasks/cancel";

    private static final int MAX_RECENT_TASKS = 1000;

    private final A2AAgentExecutor agentExecutor;

    // Bounded LRU of recently-produced tasks so tasks/get can return a task shortly after message/send. Tasks complete
    // synchronously, so this is a short-lived courtesy cache, not durable task storage.
    // taskId -> paused run's jobId for input-required tasks, so tasks/get can refresh their state once the human
    // resolves the approval. Bounded the same way as recentTasks.
    private final Map<String, Long> taskJobIds = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_RECENT_TASKS;
            }
        });

    private final Map<String, Task> recentTasks = Collections.synchronizedMap(
        new LinkedHashMap<>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Task> eldest) {
                return size() > MAX_RECENT_TASKS;
            }
        });

    public A2AProtocolHandler(A2AAgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    /**
     * Receives the JSON-RPC responses produced while streaming a {@code message/stream} request.
     */
    @FunctionalInterface
    public interface StreamSink {

        void send(JSONRPCResponse<?> event) throws Exception;
    }

    /**
     * Dispatches a parsed JSON-RPC {@code message/send} request for the addressed agent and returns the JSON-RPC
     * response.
     *
     * @param agentId   the addressed agent (derived from the request path)
     * @param requestId the JSON-RPC request id to echo back
     * @param method    the JSON-RPC method
     * @param params    the parsed {@code message/send} params, or {@code null} for other methods
     * @return a {@link SendMessageResponse} on success, or a {@link JSONRPCErrorResponse} on any protocol error
     */
    public JSONRPCResponse<?> handle(
        String agentId, @Nullable Object requestId, String method, @Nullable MessageSendParams params) {

        if (!METHOD_SEND_MESSAGE.equals(method)) {
            return new JSONRPCErrorResponse(requestId, new MethodNotFoundError());
        }

        String text = validateAndExtractText(params);

        if (text == null) {
            return new JSONRPCErrorResponse(requestId, invalidMessageError(params));
        }

        Message inboundMessage = params.message();
        String contextId = inboundMessage.getContextId();

        A2AAgentResult agentResult = runAgent(agentId, text, contextId, inboundMessage.getMessageId());

        Task task = toTask(contextId, agentResult);

        recentTasks.put(task.getId(), task);

        return new SendMessageResponse(requestId, task);
    }

    private void rememberJobId(String taskId, A2AAgentResult agentResult) {
        if (agentResult.inputRequired() && agentResult.jobId() != null) {
            taskJobIds.put(taskId, agentResult.jobId());
        } else {
            taskJobIds.remove(taskId);
        }
    }

    /**
     * Dispatches a JSON-RPC {@code tasks/get} request: returns the recently-produced task with {@code taskId}, or a
     * {@link TaskNotFoundError} if it is not in the short-lived recent-task cache (tasks are not durably stored).
     *
     * @param requestId the JSON-RPC request id to echo back
     * @param taskId    the requested task id
     * @return a {@link GetTaskResponse} carrying the task or a not-found error
     */
    public JSONRPCResponse<?> handleGetTask(@Nullable Object requestId, @Nullable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return new JSONRPCErrorResponse(requestId, new InvalidParamsError("A task id is required"));
        }

        Task task = recentTasks.get(taskId);

        if (task == null) {
            // The in-memory cache is a courtesy cache, not storage: it evicts, it is per-process, and it dies with the
            // process. Fall back to durable state so a paused run stays pollable across eviction, restart, and nodes.
            task = resolveTaskFromDurableState(taskId);
        }

        if (task == null) {
            return new GetTaskResponse(requestId, new TaskNotFoundError());
        }

        task = refreshInputRequiredTask(taskId, task);

        return new GetTaskResponse(requestId, task);
    }

    /**
     * Refreshes an {@code input-required} task by re-checking its paused run: once the human resolves the approval, the
     * stored snapshot is replaced with the run's actual outcome (completed with output / failed / a fresh
     * input-required descriptor for a follow-up approval), so pollers see the final result instead of a task frozen at
     * input-required forever. Tasks without a known jobId, non-input-required tasks, and indeterminate poll results
     * keep the stored snapshot.
     */
    private Task refreshInputRequiredTask(String taskId, Task task) {
        TaskStatus taskStatus = task.getStatus();

        if (taskStatus.state() != TaskState.INPUT_REQUIRED) {
            return task;
        }

        Long jobId = taskJobIds.get(taskId);

        if (jobId == null) {
            return task;
        }

        A2AAgentResult refreshedResult;

        try {
            refreshedResult = agentExecutor.pollRun(jobId);
        } catch (Exception exception) {
            return task;
        }

        if (refreshedResult == null) {
            return task;
        }

        Message refreshedMessage = buildAgentMessage(taskId, task.getContextId(), refreshedResult);

        Task refreshedTask = new Task(
            taskId, task.getContextId(), new TaskStatus(resolveTaskState(refreshedResult), refreshedMessage, null),
            List.of(), List.of(), null);

        recentTasks.put(taskId, refreshedTask);

        rememberJobId(taskId, refreshedResult);

        return refreshedTask;
    }

    /**
     * Rebuilds a task the in-memory cache no longer holds by resolving the id against durable state through
     * {@link A2AAgentExecutor#pollTask(String)}. Returns {@code null} when the id resolves to nothing, which the caller
     * surfaces as {@link TaskNotFoundError} — the pre-existing behavior for genuinely unknown ids.
     */
    private @Nullable Task resolveTaskFromDurableState(String taskId) {
        A2AAgentResult agentResult;

        try {
            agentResult = agentExecutor.pollTask(taskId);
        } catch (Exception exception) {
            // An unresolvable id is indistinguishable from an unknown one to the caller: both yield TaskNotFoundError.
            return null;
        }

        if (agentResult == null) {
            return null;
        }

        // The original context id died with the cache entry; the A2A task carries a fresh one rather than failing the
        // poll, since the client correlates on the task id it supplied.
        String contextId = UUID.randomUUID()
            .toString();

        Message agentMessage = buildAgentMessage(taskId, contextId, agentResult);

        Task task = new Task(
            taskId, contextId, new TaskStatus(resolveTaskState(agentResult), agentMessage, null), List.of(), List.of(),
            null);

        recentTasks.put(taskId, task);

        rememberJobId(taskId, agentResult);

        return task;
    }

    /**
     * Dispatches a JSON-RPC {@code tasks/cancel} request. This server runs tasks synchronously to a terminal state, so
     * a known task is never cancelable ({@link TaskNotCancelableError}); an unknown task id yields a
     * {@link TaskNotFoundError}.
     *
     * @param requestId the JSON-RPC request id to echo back
     * @param taskId    the task id to cancel
     * @return a {@link CancelTaskResponse} carrying the appropriate error
     */
    public JSONRPCResponse<?> handleCancelTask(@Nullable Object requestId, @Nullable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return new JSONRPCErrorResponse(requestId, new InvalidParamsError("A task id is required"));
        }

        // Existence is judged the same way tasks/get judges it — cache first, then durable state — so a task that is
        // still pollable never reports "not found" here just because this process no longer caches it.
        if (!recentTasks.containsKey(taskId) && resolveTaskFromDurableState(taskId) == null) {
            return new CancelTaskResponse(requestId, new TaskNotFoundError());
        }

        return new CancelTaskResponse(
            requestId, new TaskNotCancelableError("Task already completed and cannot be canceled"));
    }

    /**
     * Dispatches a parsed JSON-RPC {@code message/stream} request, emitting a {@code working} status event and then a
     * final {@code completed}/{@code failed} status event carrying the agent's response, to {@code sink}.
     *
     * @param agentId   the addressed agent (derived from the request path)
     * @param requestId the JSON-RPC request id to echo back on every emitted event
     * @param method    the JSON-RPC method
     * @param params    the parsed {@code message/stream} params, or {@code null} for other methods
     * @param sink      receives each JSON-RPC response event as it is produced
     */
    public void handleStream(
        String agentId, @Nullable Object requestId, String method, @Nullable MessageSendParams params, StreamSink sink)
        throws Exception {

        if (!METHOD_STREAM_MESSAGE.equals(method)) {
            sink.send(new JSONRPCErrorResponse(requestId, new MethodNotFoundError()));

            return;
        }

        String text = validateAndExtractText(params);

        if (text == null) {
            sink.send(new JSONRPCErrorResponse(requestId, invalidMessageError(params)));

            return;
        }

        Message inboundMessage = params.message();
        String contextId = inboundMessage.getContextId() != null
            ? inboundMessage.getContextId()
            : UUID.randomUUID()
                .toString();
        String taskId = UUID.randomUUID()
            .toString();

        sink.send(
            new SendStreamingMessageResponse(
                requestId,
                new TaskStatusUpdateEvent(taskId, new TaskStatus(TaskState.WORKING), contextId, false, Map.of())));

        A2AAgentResult agentResult = runAgent(agentId, text, contextId, inboundMessage.getMessageId());

        TaskState finalState = resolveTaskState(agentResult);
        Message agentMessage = buildAgentMessage(taskId, contextId, agentResult);
        TaskStatus finalStatus = new TaskStatus(finalState, agentMessage, null);

        recentTasks.put(taskId, new Task(taskId, contextId, finalStatus, List.of(), List.of(), null));

        rememberJobId(taskId, agentResult);

        // A stream must name its task before the run starts, but a paused run's durable id (its resume token) only
        // exists once it pauses — and renaming the task mid-stream would break the client's correlation. Register the
        // durable id as an additional handle instead: the streamed id stays stable, and the durable one keeps
        // resolving after this cache forgets the run. Clients recover it from the resume token in the approval form
        // URL carried by the status message.
        String durableTaskId = agentResult.taskId();

        if (durableTaskId != null && !durableTaskId.equals(taskId)) {
            recentTasks.put(
                durableTaskId,
                new Task(
                    durableTaskId, contextId, new TaskStatus(
                        finalState, buildAgentMessage(durableTaskId, contextId, agentResult), null),
                    List.of(), List.of(), null));

            rememberJobId(durableTaskId, agentResult);
        }

        sink.send(
            new SendStreamingMessageResponse(
                requestId, new TaskStatusUpdateEvent(taskId, finalStatus, contextId, true, Map.of())));
    }

    /**
     * Concatenates the text of every {@link TextPart} in the message, ignoring non-text parts.
     */
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

    private static InvalidParamsError invalidMessageError(@Nullable MessageSendParams params) {
        return new InvalidParamsError(
            params == null || params.message() == null ? "A message is required" : "The message has no text content");
    }

    /**
     * Returns the non-blank inbound text, or {@code null} when the params or message are missing or carry no text.
     */
    private static @Nullable String validateAndExtractText(@Nullable MessageSendParams params) {
        if (params == null || params.message() == null) {
            return null;
        }

        String text = extractText(params.message());

        return text.isBlank() ? null : text;
    }

    private A2AAgentResult runAgent(
        String agentId, String text, @Nullable String contextId, @Nullable String messageId) {

        try {
            return agentExecutor.execute(new A2AAgentRequest(agentId, text, contextId, messageId));
        } catch (Exception exception) {
            return A2AAgentResult.ofError(exception.getMessage());
        }
    }

    private Task toTask(@Nullable String contextId, A2AAgentResult agentResult) {
        // A paused run supplies a durable id (its resume token) so tasks/get can recover the task from persistent
        // state later; everything else is short-lived and keeps a random id.
        String taskId = agentResult.taskId() != null
            ? agentResult.taskId()
            : UUID.randomUUID()
                .toString();
        String effectiveContextId = contextId != null
            ? contextId
            : UUID.randomUUID()
                .toString();

        TaskState taskState = resolveTaskState(agentResult);

        Message agentMessage = buildAgentMessage(taskId, effectiveContextId, agentResult);

        TaskStatus taskStatus = new TaskStatus(taskState, agentMessage, null);

        rememberJobId(taskId, agentResult);

        return new Task(taskId, effectiveContextId, taskStatus, List.of(), List.of(), null);
    }

    /**
     * Maps the agent result onto the A2A task state: a run paused on a human decision surfaces as
     * {@code input-required} (per the A2A task lifecycle) rather than completed, so the calling agent knows the task is
     * blocked on the human, not finished.
     */
    private static TaskState resolveTaskState(A2AAgentResult agentResult) {
        if (agentResult.inputRequired()) {
            return TaskState.INPUT_REQUIRED;
        }

        return agentResult.success() ? TaskState.COMPLETED : TaskState.FAILED;
    }

    private static Message buildAgentMessage(String taskId, String contextId, A2AAgentResult agentResult) {
        String responseText = agentResult.success()
            ? agentResult.text()
            : "Error: " + agentResult.errorMessage();

        return new Message.Builder()
            .role(Message.Role.AGENT)
            .parts(new TextPart(responseText))
            .messageId(UUID.randomUUID()
                .toString())
            .contextId(contextId)
            .taskId(taskId)
            .build();
    }
}
