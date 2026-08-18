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

package com.bytechef.platform.mcp.service;

/**
 * Callback invoked by {@link McpServerService#update(long, String, Boolean)} before an MCP server transitions to
 * {@code enabled = true}. Modules that attach their own servable content to an MCP server — the automation module's
 * workflow-as-tool mapping, for instance — contribute a bean so the enable path can refuse a server whose exposed
 * content is not actually servable yet, without {@code platform-mcp} depending on those modules.
 *
 * <p>
 * Disabling a server is never validated through this callback — a user must always be able to take a broken server
 * offline.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface McpServerEnablementValidator {

    /**
     * Validates that the given MCP server is safe to enable. Implementations throw when the server would serve
     * something broken (an incomplete tool mapping, for instance); a validator that finds nothing wrong does nothing.
     *
     * @param mcpServerId the ID of the MCP server about to be enabled
     */
    void validateEnablement(long mcpServerId);
}
