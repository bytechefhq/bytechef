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

import org.jspecify.annotations.Nullable;

/**
 * The result of running an agent for an inbound A2A request. A successful result carries the agent's textual response;
 * a failed result carries an error message that the protocol layer surfaces as a failed A2A task. An input-required
 * result marks a run paused on a human decision (e.g. a pending approval): the protocol layer surfaces the task with
 * {@code input-required} status, carrying the text as the agent's instruction on how to unblock it.
 *
 * @param text          the agent's textual response (empty on failure)
 * @param success       whether the agent completed successfully
 * @param errorMessage  a human-readable error message when {@code success} is {@code false}, otherwise {@code null}
 * @param inputRequired whether the run is paused waiting for human input rather than finished
 * @param jobId         the paused run's job id when known (input-required results only), enabling task refresh
 * @param taskId        a durable, unguessable id for the paused run (the run's resume token) when known, used as the
 *                      A2A task id so {@code tasks/get} can resolve the run again after the in-memory task cache has
 *                      evicted it, the process restarted, or the poll lands on another node
 * @author Ivica Cardic
 */
public record A2AAgentResult(
    String text, boolean success, @Nullable String errorMessage, boolean inputRequired, @Nullable Long jobId,
    @Nullable String taskId) {

    public static A2AAgentResult ofText(String text) {
        return new A2AAgentResult(text == null ? "" : text, true, null, false, null, null);
    }

    public static A2AAgentResult ofError(String errorMessage) {
        return new A2AAgentResult("", false, errorMessage, false, null, null);
    }

    public static A2AAgentResult ofInputRequired(String text) {
        return ofInputRequired(text, null);
    }

    /**
     * An input-required result that also carries the paused run's job id, letting the protocol layer refresh the task's
     * state on {@code tasks/get} once the human resolves the approval.
     */
    public static A2AAgentResult ofInputRequired(String text, @Nullable Long jobId) {
        return ofInputRequired(text, jobId, null);
    }

    /**
     * An input-required result carrying both the paused run's job id and a durable task id (the run's resume token).
     * The durable id lets {@code tasks/get} recover the task from persistent state instead of returning
     * {@code TaskNotFoundError} once the in-memory cache no longer holds it.
     */
    public static A2AAgentResult ofInputRequired(String text, @Nullable Long jobId, @Nullable String taskId) {
        return new A2AAgentResult(text == null ? "" : text, true, null, true, jobId, taskId);
    }
}
