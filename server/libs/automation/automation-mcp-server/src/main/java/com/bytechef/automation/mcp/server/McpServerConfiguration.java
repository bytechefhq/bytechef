/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
//@Configuration
public class McpServerConfiguration {

//    private final ToolFacade toolFacade;

//    @SuppressFBWarnings("EI")
//    public McpServerConfiguration(ToolFacade toolFacade) {
//        this.toolFacade = toolFacade;
//    }

    @Bean
    WebMvcSseServerTransportProvider webMvcSseServerTransportProvider(ObjectMapper objectMapper) {
        // TODO - Set /embedded/mcp/message, check ConnectedUserAuthenticationFilter
        // TODO - Set /embedded/sse

        return new WebMvcSseServerTransportProvider(objectMapper, "/api/embedded/v1/mcp/message");
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    McpSyncServer mcpServer(McpServerTransportProvider transportProvider) {
        var capabilities = McpSchema.ServerCapabilities.builder()
            .resources(false, true)
            .tools(true)
            .prompts(true)
            .logging()
            .build();

        return McpServer.sync(transportProvider)
            .serverInfo("MCP ByteChef Embedded Server", "1.0.0")
            .capabilities(capabilities)
            .tools(McpToolUtils.toSyncToolSpecification(getToolCallbacks()))
            .build();
    }

    public List<ToolCallback> getToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

//        List<ToolDTO> toolDTOs = toolFacade.getTools();
//
//        for (ToolDTO toolDTO : toolDTOs) {
//            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
//                .builder(toolDTO.name(), getToolCallbackFunction(toolDTO.name()))
//                .inputType(Map.class)
//                .inputSchema(toolDTO.parameters());
//
//            if (toolDTO.description() != null) {
//                builder.description(toolDTO.description());
//            }
//
//            toolCallbacks.add(builder.build());
//        }

        return toolCallbacks;
    }

//    private Function<Map<String, Object>, Object> getToolCallbackFunction(String toolName) {
//        return null;
//        return request -> toolFacade.executeTool(toolName, request, Environment.PRODUCTION, null);
//    }
}
