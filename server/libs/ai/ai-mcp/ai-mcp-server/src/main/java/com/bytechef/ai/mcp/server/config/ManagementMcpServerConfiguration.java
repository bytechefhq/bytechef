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

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.mcp.server.McpAppWorkflowEditor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Configuration for ByteChef MCP Server using Streamable HTTP transport.
 *
 * This configuration registers a set of deterministic CE automation/platform tools directly, and folds in any
 * {@link McpServerToolCallbackContributor} beans (EE deployments contribute the Copilot subagent agent-tools). The
 * server is exposed via Streamable HTTP at /api/management/{secretKey}/mcp.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class ManagementMcpServerConfiguration {

    // Workflow tools whose results (or, for create, whose input argument) carry the nested workflow definition. They
    // are marked with the MCP App workflow editor UI so hosts render the canvas alongside the conversation, and their
    // results gain structuredContent.definition — the payload the editor widget consumes.
    private static final Set<String> WORKFLOW_EDITOR_TOOL_NAMES =
        Set.of("getWorkflow", "createProjectWorkflow", "updateWorkflow");

    private static final Set<String> READ_ONLY_WORKFLOW_EDITOR_TOOL_NAMES = Set.of("getWorkflow");

    private static final String DEFINITION = "definition";

    private static final Logger log = LoggerFactory.getLogger(ManagementMcpServerConfiguration.class);

    private final ComponentTools componentTools;
    private final ProjectTools projectTools;
    private final ProjectWorkflowTools projectWorkflowTools;
    private final TaskTools taskTools;
    private final TaskDispatcherTools taskDispatcherTools;
    private final ScriptTools scriptTools;
    private final ClusterElementTools clusterElementTools;
    private final List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerConfiguration(
        ComponentTools componentTools, ProjectTools projectTools, ProjectWorkflowTools projectWorkflowTools,
        TaskTools taskTools, TaskDispatcherTools taskDispatcherTools, ScriptTools scriptTools,
        ClusterElementTools clusterElementTools,
        List<McpServerToolCallbackContributor> mcpServerToolCallbackContributors) {

        this.componentTools = componentTools;
        this.projectTools = projectTools;
        this.projectWorkflowTools = projectWorkflowTools;
        this.taskTools = taskTools;
        this.taskDispatcherTools = taskDispatcherTools;
        this.scriptTools = scriptTools;
        this.clusterElementTools = clusterElementTools;
        this.mcpServerToolCallbackContributors = mcpServerToolCallbackContributors;
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/management/{secretKey}/mcp")
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction() {
        return webMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer(ApplicationProperties applicationProperties) {
        return McpServer.async(webMvcStreamableHttpServerTransportProvider())
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .tools(attachWorkflowEditorUi(
                McpToolUtils.toAsyncToolSpecifications(toolCallbackProvider().getToolCallbacks())))
            .resources(McpAppWorkflowEditor.getResourceSpecifications(applicationProperties.getPublicUrl()))
            .build();
    }

    /**
     * Marks the workflow tools with the MCP App workflow editor UI ({@code _meta.ui.resourceUri}) and wraps their call
     * handlers so results carry the nested workflow definition as {@code structuredContent.definition}. Spring AI's
     * {@code McpToolUtils} conversion exposes no metadata hook and the converted {@code Tool} is immutable, so the
     * affected tools are rebuilt with the metadata attached.
     */
    static List<McpServerFeatures.AsyncToolSpecification> attachWorkflowEditorUi(
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications) {

        return toolSpecifications.stream()
            .map(toolSpecification -> {
                McpSchema.Tool tool = toolSpecification.tool();

                return WORKFLOW_EDITOR_TOOL_NAMES.contains(tool.name())
                    ? toWorkflowEditorToolSpecification(toolSpecification)
                    : toolSpecification;
            })
            .toList();
    }

    private static McpServerFeatures.AsyncToolSpecification toWorkflowEditorToolSpecification(
        McpServerFeatures.AsyncToolSpecification toolSpecification) {

        McpSchema.Tool tool = toolSpecification.tool();

        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder(tool.name(), tool.inputSchema())
            .description(tool.description())
            .meta(McpAppWorkflowEditor.getToolMeta());

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
                .map(callToolResult -> withDefinitionStructuredContent(callToolRequest, callToolResult)));
    }

    static McpSchema.CallToolResult withDefinitionStructuredContent(
        McpSchema.CallToolRequest callToolRequest, McpSchema.CallToolResult callToolResult) {

        if (Boolean.TRUE.equals(callToolResult.isError()) || callToolResult.structuredContent() != null) {
            return callToolResult;
        }

        Map<String, ?> definition = extractDefinition(callToolRequest, callToolResult);

        if (definition == null) {
            return callToolResult;
        }

        return McpSchema.CallToolResult.builder()
            .content(callToolResult.content())
            .structuredContent(Map.of(DEFINITION, definition))
            .build();
    }

    private static Map<String, ?> extractDefinition(
        McpSchema.CallToolRequest callToolRequest, McpSchema.CallToolResult callToolResult) {

        // getWorkflow/updateWorkflow return WorkflowInfo whose 'definition' field holds the nested definition JSON.
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

        // createProjectWorkflow's result omits the definition; it is the tool call's input argument.
        Map<String, Object> arguments = callToolRequest.arguments();

        return arguments == null ? null : toDefinitionMap(arguments.get(DEFINITION));
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
        List<Object> tools = List.of(
            projectTools, projectWorkflowTools, componentTools, taskTools, taskDispatcherTools, scriptTools,
            clusterElementTools);

        List<ToolCallback> toolCallbacks = new ArrayList<>(List.of(ToolCallbacks.from(tools.toArray())));

        for (McpServerToolCallbackContributor contributor : mcpServerToolCallbackContributors) {
            toolCallbacks.addAll(contributor.getToolCallbacks());
        }

        return ToolCallbackProvider.from(toolCallbacks);
    }
}
