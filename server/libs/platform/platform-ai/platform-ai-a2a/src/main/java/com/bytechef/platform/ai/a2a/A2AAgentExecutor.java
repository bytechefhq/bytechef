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
}
