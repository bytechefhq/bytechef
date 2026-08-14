/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the embedded workflow-editor copilot subagent to the management MCP server through the
 * {@link McpServerToolCallbackContributor} SPI — the same extension point the automation copilot specialists use (see
 * {@code ToolCallbackContributorConfiguration}). It exposes the embedded integration-workflow editor as the
 * {@code integration_workflow_agent} tool so external MCP clients can build and edit integration workflows, the
 * embedded counterpart to the automation {@code project_workflow_agent}.
 *
 * <p>
 * The delegate reuses {@link ProjectWorkflowAgentToolCallback} (parameterized with the embedded tool name, description,
 * and {@link CopilotAgentType#INTEGRATION_WORKFLOW_AGENT}) wrapped around the embedded BUILD subagent
 * {@link ChatClient}. A missing ChatClient bean (feature module absent) skips silently. Like the automation copilot
 * contributor, the AG-UI {@code ProgressReportingToolCallback} wrapper is intentionally NOT applied on this surface.
 * The delegate is wrapped in {@link WorkspaceScopedSubAgentToolCallback} so the MCP client can supply workspace scope
 * this surface cannot infer.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class EmbeddedCopilotMcpContributorConfiguration {

    private static final String DESCRIPTION =
        """
            Delegate a user request about whole embedded INTEGRATION workflows to the specialised Embedded
            Workflow Editor subagent. Use this for requests that design, edit, debug, or explain an
            integration's workflows (orchestration of tasks, triggers, conditions, loops). It also manages
            the integrations themselves (list/create/update/delete/publish). Prefer calling it over
            reasoning about integration-workflow shape directly. Returns the updated workflow JSON plus a
            change rationale. This is the embedded counterpart of project_workflow_agent (which targets
            automation projects).""";

    @Bean
    McpServerToolCallbackContributor embeddedWorkflowEditorMcpToolCallbackContributor(
        @Qualifier("workflowEditorEmbeddedBuildSubAgentChatClient") //
        ObjectProvider<ChatClient> workflowEditorEmbeddedBuildSubAgentChatClientProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            workflowEditorEmbeddedBuildSubAgentChatClientProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ProjectWorkflowAgentToolCallback(
                            chatClient, "integration_workflow_agent", DESCRIPTION,
                            CopilotAgentType.INTEGRATION_WORKFLOW_AGENT),
                        workspaceService)));

            return toolCallbacks;
        };
    }
}
