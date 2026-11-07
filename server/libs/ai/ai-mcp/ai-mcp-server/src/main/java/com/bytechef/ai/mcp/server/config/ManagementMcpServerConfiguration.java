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

package com.bytechef.ai.mcp.server.config;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.EnvironmentScopedToolCallback;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.mcp.server.McpApps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@DependsOn(JacksonConfiguration.JSON_UTILS_CONFIGURATION)
public class ManagementMcpServerConfiguration {

    private static final Set<String> EXCLUDED_TOOL_NAMES = Set.of("getProjectStatus", "searchProjects");

    private static final Set<String> ENVIRONMENT_SCOPED_TOOL_NAMES = Set.of(
        "updateWorkflow", "saveWorkflowTestConnection");

    private static final Set<String> WORKFLOW_EDITOR_TOOL_NAMES = Set.of("getWorkflow");

    private static final Set<String> READ_ONLY_WORKFLOW_EDITOR_TOOL_NAMES = Set.of("getWorkflow");

    private static final String DEFINITION = "definition";

    private static final Logger log = LoggerFactory.getLogger(ManagementMcpServerConfiguration.class);

    private final ProjectTools projectTools;
    private final ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools;
    private final WorkspaceTools workspaceTools;
    private final List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ProjectTools projectTools, ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools,
        WorkspaceTools workspaceTools, List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors) {

        this.projectTools = projectTools;
        this.projectWorkflowLifecycleTools = projectWorkflowLifecycleTools;
        this.workspaceTools = workspaceTools;
        this.mcpServerToolCallbackContributors = mcpServerToolCallbackContributors;
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/{secretKey}/mcp")
            .build();
    }

    @Bean
    WebMvcStreamableServerTransportProvider canonicalWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/mcp")
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction() {
        return webMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    RouterFunction<ServerResponse> canonicalMcpRouterFunction() {
        return canonicalWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer(ApplicationProperties applicationProperties) {
        return createMcpAsyncServer(
            webMvcStreamableHttpServerTransportProvider(), applicationProperties.getPublicUrl());
    }

    @Bean
    McpAsyncServer canonicalMcpAsyncServer(ApplicationProperties applicationProperties) {
        return createMcpAsyncServer(
            canonicalWebMvcStreamableHttpServerTransportProvider(), applicationProperties.getPublicUrl());
    }

    static List<McpServerFeatures.AsyncToolSpecification> attachWorkflowEditorUi(
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications) {

        return attachMcpAppUi(toolSpecifications, Map.of());
    }

    static List<McpServerFeatures.AsyncToolSpecification> attachMcpAppUi(
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications,
        Map<String, McpAppUiDescriptor> descriptors) {

        return toolSpecifications.stream()
            .map(toolSpecification -> {
                String name = toolSpecification.tool()
                    .name();

                if (WORKFLOW_EDITOR_TOOL_NAMES.contains(name)) {
                    return toWorkflowEditorToolSpecification(toolSpecification);
                }

                McpAppUiDescriptor descriptor = descriptors.get(name);

                return descriptor == null
                    ? toolSpecification
                    : toMcpAppToolSpecification(toolSpecification, descriptor);
            })
            .toList();
    }

    private Map<String, McpAppUiDescriptor> collectMcpAppUiDescriptors() {
        Map<String, McpAppUiDescriptor> descriptors = new HashMap<>();

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            descriptors.putAll(contributor.getMcpAppUiDescriptors());
        }

        return descriptors;
    }

    private List<McpServerFeatures.AsyncResourceSpecification> mcpAppResourceSpecifications(String publicUrl) {
        List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications =
            new ArrayList<>(McpApps.getResourceSpecifications(
                McpAppResources.WORKFLOW_EDITOR_URI, "ByteChef Workflow Editor",
                "Read-only workflow canvas rendered while workflows are built via MCP tools", publicUrl));

        resourceSpecifications.addAll(McpApps.getResourceSpecifications(
            McpAppResources.DATA_TABLE_VIEWER_URI, "ByteChef Data Table Viewer",
            "Read-only data table rendered while data tables are queried via MCP tools"));
        resourceSpecifications.addAll(McpApps.getResourceSpecifications(
            McpAppResources.FILE_VIEWER_URI, "ByteChef File Viewer",
            "Read-only file content rendered while asset files are read via MCP tools"));

        return resourceSpecifications;
    }

    private static McpServerFeatures.AsyncToolSpecification toMcpAppToolSpecification(
        McpServerFeatures.AsyncToolSpecification toolSpecification, McpAppUiDescriptor descriptor) {

        McpSchema.Tool tool = toolSpecification.tool();

        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder(tool.name(), tool.inputSchema())
            .description(tool.description())
            .meta(Map.of("ui", Map.of("resourceUri", descriptor.resourceUri())));

        if (tool.title() != null) {
            toolBuilder.title(tool.title());
        }

        if (tool.outputSchema() != null) {
            toolBuilder.outputSchema(tool.outputSchema());
        }

        if (tool.icons() != null) {
            toolBuilder.icons(tool.icons());
        }

        toolBuilder.annotations(McpSchema.ToolAnnotations.builder()
            .readOnlyHint(true)
            .build());

        var delegateCallHandler = toolSpecification.callHandler();

        return new McpServerFeatures.AsyncToolSpecification(
            toolBuilder.build(),
            (exchange, callToolRequest) -> delegateCallHandler.apply(exchange, callToolRequest)
                .map(callToolResult -> withShapedStructuredContent(callToolResult, descriptor)));
    }

    static McpSchema.CallToolResult withShapedStructuredContent(
        McpSchema.CallToolResult callToolResult, McpAppUiDescriptor descriptor) {

        if (Boolean.TRUE.equals(callToolResult.isError()) || callToolResult.structuredContent() != null) {
            return callToolResult;
        }

        McpSchema.TextContent firstTextContent = callToolResult.content()
            .stream()
            .filter(McpSchema.TextContent.class::isInstance)
            .map(McpSchema.TextContent.class::cast)
            .findFirst()
            .orElse(null);

        if (firstTextContent == null) {
            return callToolResult;
        }

        Map<String, Object> structuredContent;

        try {
            structuredContent = descriptor.structuredContentShaper()
                .apply(firstTextContent.text());
        } catch (RuntimeException exception) {
            log.warn("MCP App structuredContent shaper failed for {}; structuredContent omitted",
                descriptor.resourceUri(), exception);

            return callToolResult;
        }

        if (structuredContent == null) {
            return callToolResult;
        }

        return McpSchema.CallToolResult.builder()
            .content(callToolResult.content())
            .structuredContent(structuredContent)
            .build();
    }

    private static McpServerFeatures.AsyncToolSpecification toWorkflowEditorToolSpecification(
        McpServerFeatures.AsyncToolSpecification toolSpecification) {

        McpSchema.Tool tool = toolSpecification.tool();

        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder(tool.name(), tool.inputSchema())
            .description(tool.description())
            .meta(McpApps.getToolMeta(McpAppResources.WORKFLOW_EDITOR_URI));

        if (tool.title() != null) {
            toolBuilder.title(tool.title());
        }

        if (tool.outputSchema() != null) {
            toolBuilder.outputSchema(tool.outputSchema());
        }

        if (tool.icons() != null) {
            toolBuilder.icons(tool.icons());
        }

        if (READ_ONLY_WORKFLOW_EDITOR_TOOL_NAMES.contains(tool.name())) {
            toolBuilder.annotations(McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .build());
        } else if (tool.annotations() != null) {
            toolBuilder.annotations(tool.annotations());
        }

        var delegateCallHandler = toolSpecification.callHandler();

        return new McpServerFeatures.AsyncToolSpecification(
            toolBuilder.build(),
            (exchange, callToolRequest) -> delegateCallHandler.apply(exchange, callToolRequest)
                .map(ManagementMcpServerConfiguration::withDefinitionStructuredContent));
    }

    static McpSchema.CallToolResult withDefinitionStructuredContent(McpSchema.CallToolResult callToolResult) {
        if (Boolean.TRUE.equals(callToolResult.isError()) || callToolResult.structuredContent() != null) {
            return callToolResult;
        }

        Map<String, ?> definition = extractDefinition(callToolResult);

        if (definition == null) {
            return callToolResult;
        }

        return McpSchema.CallToolResult.builder()
            .content(callToolResult.content())
            .structuredContent(Map.of(DEFINITION, definition))
            .build();
    }

    private static Map<String, ?> extractDefinition(McpSchema.CallToolResult callToolResult) {
        McpSchema.TextContent firstTextContent = callToolResult.content()
            .stream()
            .filter(McpSchema.TextContent.class::isInstance)
            .map(McpSchema.TextContent.class::cast)
            .findFirst()
            .orElse(null);

        if (firstTextContent != null) {
            try {
                Map<String, ?> resultMap = JsonUtils.readMap(firstTextContent.text());

                Map<String, ?> definition = toDefinitionMap(resultMap.get(DEFINITION));

                if (definition != null) {
                    return definition;
                }
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace("Tool result text is not a JSON object; skipping definition extraction", exception);
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> toDefinitionMap(Object definition) {
        try {
            if (definition instanceof String string && !string.isBlank()) {
                return JsonUtils.readMap(string);
            }

            if (definition instanceof Map<?, ?> map) {
                return (Map<String, ?>) map;
            }
        } catch (Exception exception) {
            log.warn("Workflow definition is not valid JSON; structuredContent.definition will be omitted", exception);
        }

        return null;
    }

    ToolCallbackProvider toolCallbackProvider() {
        List<Object> tools = List.of(projectTools, projectWorkflowLifecycleTools, workspaceTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ToolCallback toolCallback : ToolCallbacks.from(tools.toArray())) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            String name = toolDefinition.name();

            if (EXCLUDED_TOOL_NAMES.contains(name)) {
                continue;
            }

            toolCallbacks.add(
                ENVIRONMENT_SCOPED_TOOL_NAMES.contains(name)
                    ? new EnvironmentScopedToolCallback(toolCallback)
                    : toolCallback);
        }

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            toolCallbacks.addAll(contributor.getToolCallbacks());
        }

        return ToolCallbackProvider.from(toolCallbacks);
    }

    List<McpServerFeatures.AsyncToolSpecification> toolSpecifications() {
        return toolSpecifications(List.of(toolCallbackProvider().getToolCallbacks()));
    }

    private List<McpServerFeatures.AsyncToolSpecification> toolSpecifications(List<ToolCallback> toolCallbacks) {
        return attachMcpAppUi(
            ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(toolCallbacks),
            collectMcpAppUiDescriptors());
    }

    private McpAsyncServer createMcpAsyncServer(
        WebMvcStreamableServerTransportProvider transportProvider, String publicUrl) {

        List<ToolCallback> toolCallbacks = List.of(toolCallbackProvider().getToolCallbacks());

        List<String> toolNames = new ArrayList<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();

            toolNames.add(toolDefinition.name());
        }

        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .instructions(ManagementMcpInstructions.build(toolNames))
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(toolSpecifications(toolCallbacks))
            .resources(mcpAppResourceSpecifications(publicUrl))
            .build();
    }
}
