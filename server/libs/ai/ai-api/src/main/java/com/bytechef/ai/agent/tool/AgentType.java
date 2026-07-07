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

package com.bytechef.ai.agent.tool;

/**
 * Identity of an AI agent, used by the usage-observation handling (and the expensive tool callbacks) to attribute LLM
 * and tool calls to the right agent. Implemented once per module by a domain enum (e.g. {@code AiHubAgentType},
 * {@code CopilotAgentType}) so the set of valid agent types is assembled from the modules that own them rather than
 * hardcoded in one central enum. Every implementation is contributed through {@link AgentTypeProvider} and aggregated
 * by {@link AgentTypeRegistry}.
 *
 * @author Ivica Cardic
 */
public interface AgentType {

    /**
     * Stable identity used for cost-attribution persistence (e.g. {@code "RESEARCH"}). Satisfied for free by every enum
     * that implements this interface.
     */
    String name();

    /**
     * The wire-format key for this agent — matches the {@code agentId} routed by the chat REST controller (e.g.
     * {@code "research"}).
     */
    String key();

    /**
     * Whether this type represents a coarse fallback emitted because no more specific agent was bound.
     */
    boolean isFallback();
}
