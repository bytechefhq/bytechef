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

package com.bytechef.automation.ai.tool;

import com.bytechef.ai.agent.tool.AgentType;

/**
 * Agent types for the automation-owned management specialist subagents (MCP servers, project deployments, API
 * collections) exposed to the management MCP server and the ai_hub BUILD agent. These manage automation resources
 * rather than AI-hub chat state, so they live outside {@code AiHubAgentType}; the ai_hub {@code personal_agent_manager}
 * keeps its {@code AiHubAgentType} entry. Contributed to the {@code AgentTypeRegistry} via
 * {@link ManagerAgentTypeProvider}.
 *
 * @author Ivica Cardic
 */
public enum ManagerAgentType implements AgentType {

    MCP_MANAGER("mcp_manager", false),
    DEPLOYMENT_MANAGER("deployment_manager", false),
    API_COLLECTION_MANAGER("api_collection_manager", false);

    private final String key;
    private final boolean fallback;

    ManagerAgentType(String key, boolean fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }
}
