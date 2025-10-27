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

package com.bytechef.automation.ai.mcp.server.config;

/**
 * @author Ivica Cardic
 */
// @Configuration TODO remove comment once Spring supports multiple MCP servers
public class AutomationMcpServerConfiguration {
//
//    private final ToolFacade toolFacade;
//
//    public AutomationMcpServerConfiguration(ToolFacade toolFacade) {
//        this.toolFacade = toolFacade;
//    }
//
//    @Bean
//    WebMvcSseServerTransportProvider webMvcSseServerTransportProvider(ObjectMapper objectMapper) {
//        return new WebMvcSseServerTransportProvider(
//            objectMapper, "/api/automation/v1/mcp/message", "/api/automation/sse");
//    }
//
//    @Bean
//    RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransportProvider transportProvider) {
//        return transportProvider.getRouterFunction();
//    }
//
//    @Bean
//    McpSyncServer mcpServer(McpServerTransportProvider transportProvider) {
//        var capabilities = McpSchema.ServerCapabilities.builder()
//            .resources(false, true)
//            .tools(true)
//            .prompts(true)
//            .logging()
//            .build();
//
//        return McpServer.sync(transportProvider)
//            .serverInfo("bytechef-automation-mcp-server", "1.0.0")
//            .capabilities(capabilities)
//            .tools(McpToolUtils.toSyncToolSpecification(getToolCallbacks()))
//            .build();
//    }
//
//    public List<ToolCallback> getToolCallbacks() {
//        List<ToolCallback> toolCallbacks = new ArrayList<>();
//
//        List<ToolDTO> toolDTOs = toolFacade.getTools();
//
//        for (ToolDTO toolDTO : toolDTOs) {
//
//            toolCallbacks.add(toolFacade.getFunctionToolCallback(toolDTO));
//        }
//
//        return toolCallbacks;
//    }
//
}
