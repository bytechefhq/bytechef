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

package com.bytechef.ai.mcp.server.spi;

import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Extension point for contributing {@link ToolCallback}s to the management MCP server. Implementations are collected by
 * {@code ManagementMcpServerConfiguration} and folded into the server's tool catalog. This keeps the CE MCP server
 * independent of EE modules: EE deployments supply an implementation; CE-only deployments simply have none.
 *
 * @author Ivica Cardic
 */
public interface McpServerToolCallbackContributor {

    List<ToolCallback> getToolCallbacks();
}
