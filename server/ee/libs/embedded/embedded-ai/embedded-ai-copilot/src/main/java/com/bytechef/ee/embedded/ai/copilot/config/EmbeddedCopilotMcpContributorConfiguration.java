/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the embedded workflow-editor copilot subagent to the management MCP server through the
 * {@link McpServerToolCallbackContributor} SPI — the same extension point the automation copilot specialists use (see
 * {@code ToolCallbackContributorConfiguration}). It exposes the embedded integration-workflow editor as the
 * {@code buildIntegrationWorkflow} tool so external MCP clients can build and edit integration workflows, the embedded
 * counterpart to the automation {@code buildWorkflow}.
 *
 * <p>
 * The delegate is contributed via {@link IntelligentToolCatalog#getByNames}, over the single definition registered by
 * {@link EmbeddedIntelligentToolContributor} — which builds the delegate around the embedded BUILD subagent
 * {@code ChatClient} reusing the shared {@code ProjectWorkflowAgentToolCallback}. A missing ChatClient bean (feature
 * module absent) skips silently. Like the automation copilot contributor, the AG-UI
 * {@code ProgressReportingToolCallback} wrapper is intentionally NOT applied on this surface. The delegate is wrapped
 * in {@link WorkspaceScopedSubAgentToolCallback} so the MCP client can supply workspace scope this surface cannot
 * infer.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class EmbeddedCopilotMcpContributorConfiguration {

    /**
     * Name of the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition} this contributor owns on the
     * management MCP surface, contributed by {@link EmbeddedIntelligentToolContributor}. Filtered with
     * {@link IntelligentToolCatalog#getByNames} over its own name partition, never the whole catalog, because the CE
     * {@code ToolCallbackContributorConfiguration} and the EE automation
     * {@code AutomationCopilotMcpContributorConfiguration} register their own management-MCP contributor configs
     * against the same catalog — {@code getAll} here would double-register their delegates.
     *
     * <p>
     * Public so {@code IntelligentToolSurfaceParityTest} (ai-hub-service) can assert this set is disjoint from, and
     * unions with, the other management-MCP surfaces' name sets to equal the full catalog.
     * </p>
     */
    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of("buildIntegrationWorkflow");

    @Bean
    McpServerToolCallbackContributor embeddedWorkflowEditorMcpToolCallbackContributor(
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> intelligentToolCatalog.getByNames(
            INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
            (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback, workspaceService),
            SubAgentQuestionRenderer.PLAIN_TEXT);
    }
}
