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
 * SPI that bridges an inbound A2A (Agent2Agent) {@code message/send} call to an actual ByteChef AI Agent execution. The
 * A2A protocol layer ({@link A2AProtocolHandler}) is intentionally decoupled from how an agent runs: an application
 * module provides the implementation that resolves the addressed agent (typically an agent-backed workflow executed
 * synchronously) and returns its textual response.
 *
 * @author Ivica Cardic
 */
public interface A2AAgentExecutor {

    /**
     * Runs the addressed agent for the given request and returns its result.
     *
     * @param request the resolved inbound request (agent identity + user text)
     * @return the agent's result; never {@code null}
     */
    A2AAgentResult execute(A2AAgentRequest request);

    /**
     * Re-checks a previously started run that paused on human input (a pending approval), identified by its job id.
     * Returns the run's CURRENT result — completed output, failure, or a fresh input-required descriptor — or
     * {@code null} when the state is unchanged / indeterminate (e.g. the run is mid-resume) and the stored task should
     * stay as-is. The default implementation never refreshes.
     *
     * @param jobId the paused run's job id
     * @return the refreshed result, or {@code null} to keep the stored task unchanged
     */
    default @Nullable A2AAgentResult pollRun(long jobId) {
        return null;
    }

    /**
     * Resolves a task id straight from durable state, for a {@code tasks/get} whose task is no longer in the protocol
     * layer's in-memory cache — evicted, lost to a restart, or produced on another node. Implementations must treat the
     * id as untrusted input and verify it against the stored run before returning anything, so a caller cannot poll a
     * run it does not own. The default implementation resolves nothing, preserving {@code TaskNotFoundError}.
     *
     * @param taskId the task id supplied by the client
     * @return the run's current result, or {@code null} when the id resolves to nothing
     */
    default @Nullable A2AAgentResult pollTask(String taskId) {
        return null;
    }
}
