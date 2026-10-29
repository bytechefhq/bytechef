/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists the chat workflows of the current workspace in the turn's environment,
 * through {@link ProjectDeploymentFacade#getWorkspaceChatWorkflows(long, Long)}, the same listing the workflow chats
 * sidebar uses. Returns a compact JSON array for the AI Hub BUILD agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListChatWorkflowsToolCallback implements ToolCallback {

    private static final String DESCRIPTION = """
        List all workflows in the workspace that can be run as a chat, i.e. those
        whose trigger is `chat/newChatRequest` in hosted-chat mode. Returns a JSON
        array where each item has: projectDeploymentId (string), projectId (string),
        workflowExecutionTriggerId (the webhook execution id) and workflowLabel (string).
        Use this before calling createWorkflowChat so you know which
        workflowExecutionTriggerId and projectDeploymentId to pass. Optionally filter by
        projectId. Returns an empty array when no eligible workflows are found.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "projectId": {
                    "type": "string",
                    "description": "Optional project id to filter results to a single project"
                }
            }
        }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    private final ProjectDeploymentFacade projectDeploymentFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListChatWorkflowsToolCallback(ProjectDeploymentFacade projectDeploymentFacade) {
        this.projectDeploymentFacade = projectDeploymentFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("listChatWorkflows")
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            ListChatWorkflowsInput input = jsonMapper.readValue(toolInput, ListChatWorkflowsInput.class);

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return toolError("Workspace context unavailable. Open this chat from the AI Hub of a workspace.");
            }

            Long filterProjectId = null;

            if (input.projectId() != null && !input.projectId()
                .isBlank()) {

                try {
                    filterProjectId = Long.parseLong(input.projectId());
                } catch (NumberFormatException exception) {
                    return toolError("Invalid projectId: " + input.projectId());
                }
            }

            List<ChatWorkflow> chatWorkflows = projectDeploymentFacade.getWorkspaceChatWorkflows(
                invocationContext.workspaceId(), invocationContext.environmentId());

            return jsonMapper.writeValueAsString(toSummaries(chatWorkflows, filterProjectId));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListChatWorkflowsToolCallback.class, "listChatWorkflows", exception);
        }
    }

    private static List<ChatWorkflowSummary> toSummaries(
        List<ChatWorkflow> chatWorkflows, @Nullable Long filterProjectId) {

        return chatWorkflows.stream()
            .filter(chatWorkflow -> filterProjectId == null || chatWorkflow.projectId() == filterProjectId)
            .map(ListChatWorkflowsToolCallback::toSummary)
            .toList();
    }

    private static ChatWorkflowSummary toSummary(ChatWorkflow chatWorkflow) {
        return new ChatWorkflowSummary(
            String.valueOf(chatWorkflow.projectDeploymentId()), String.valueOf(chatWorkflow.projectId()),
            chatWorkflow.workflowExecutionId(), chatWorkflow.workflowLabel());
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ListChatWorkflowsInput(@Nullable String projectId) {
    }

    public record ChatWorkflowSummary(
        String projectDeploymentId, String projectId, String workflowExecutionTriggerId, String workflowLabel) {
    }
}
