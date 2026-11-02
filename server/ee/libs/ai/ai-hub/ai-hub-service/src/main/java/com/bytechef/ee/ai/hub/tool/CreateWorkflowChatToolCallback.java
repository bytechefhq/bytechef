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
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that creates a workflow chat for a chat workflow of the current workspace. Pairs with
 * {@link ListChatWorkflowsToolCallback}, which surfaces the {@code workflowExecutionTriggerId} the agent passes here,
 * and with {@link OpenWorkflowChatTabToolCallback}, which opens the chat by the returned {@code threadId}.
 *
 * <p>
 * Workspace, user and environment come from the {@link ToolContext}, never from the tool input. A
 * {@code workflowExecutionTriggerId} that {@link ProjectDeploymentFacade#findWorkspaceChatWorkflow(long, String)} does
 * not find is refused with a tool error before any chat is created. The chat stores the project deployment id of the
 * listing row the lookup returns; the {@code projectDeploymentId} input is only validated. Each call creates a new
 * chat; earlier chats with the same workflow stay in the chats list.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CreateWorkflowChatToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateWorkflowChatToolCallback.class);

    private static final String DESCRIPTION = """
        Create a workflow chat for a workflow returned by listChatWorkflows. Use this when the
        user asks to start chatting with a workflow by name (e.g. "open a chat with my customer support
        workflow"). Each call creates a fresh chat; past chats with the same workflow remain
        in the chats list. Returns {threadId, chatId, title} so you can pass threadId to
        openWorkflowChatTab to surface the new chat in the AI Hub. Always call
        listChatWorkflows first to obtain a real workflowExecutionTriggerId; never invent one.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "workflowExecutionTriggerId": {
                        "type": "string",
                        "description": "The workflowExecutionTriggerId from listChatWorkflows. NOT the workflow id."
                    },
                    "projectDeploymentId": {
                        "type": "string",
                        "description": "The projectDeploymentId from listChatWorkflows."
                    },
                    "title": {
                        "type": "string",
                        "description": "Optional initial title for the chat row in the sidebar. Use a short human-readable label like the workflow name."
                    }
                },
                "required": ["workflowExecutionTriggerId", "projectDeploymentId"]
            }""";

    private static final String WORKSPACE_CONTEXT_UNAVAILABLE =
        "Workspace context unavailable. Open this chat from the AI Hub of a workspace.";

    private final AiHubChatService chatService;
    private final JsonMapper jsonMapper = new JsonMapper();
    private final ProjectDeploymentFacade projectDeploymentFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CreateWorkflowChatToolCallback(
        AiHubChatService chatService, ProjectDeploymentFacade projectDeploymentFacade) {

        this.chatService = chatService;
        this.projectDeploymentFacade = projectDeploymentFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("createWorkflowChat")
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
            CreateWorkflowChatInput input = jsonMapper.readValue(toolInput, CreateWorkflowChatInput.class);

            String workflowExecutionTriggerId = input.workflowExecutionTriggerId();

            if (workflowExecutionTriggerId == null || workflowExecutionTriggerId.isBlank()) {
                return toolError("workflowExecutionTriggerId is required");
            }

            if (input.projectDeploymentId() == null || input.projectDeploymentId()
                .isBlank()) {
                return toolError("projectDeploymentId is required");
            }

            long projectDeploymentId;

            try {
                projectDeploymentId = Long.parseLong(input.projectDeploymentId());
            } catch (NumberFormatException exception) {
                return toolError("projectDeploymentId must be a numeric id: " + input.projectDeploymentId());
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null
                || invocationContext.userId() == null) {

                return toolError(WORKSPACE_CONTEXT_UNAVAILABLE);
            }

            long workspaceId = invocationContext.workspaceId();

            ChatWorkflow chatWorkflow = projectDeploymentFacade
                .findWorkspaceChatWorkflow(workspaceId, workflowExecutionTriggerId)
                .orElse(null);

            if (chatWorkflow == null) {
                return toolError(
                    workflowExecutionTriggerId + " is not a chat workflow of this workspace. Call " +
                        "listChatWorkflows and use a workflowExecutionTriggerId it returned.");
            }

            long listedProjectDeploymentId = chatWorkflow.projectDeploymentId();

            if (listedProjectDeploymentId != projectDeploymentId && log.isDebugEnabled()) {
                log.debug(
                    "Replacing tool-supplied projectDeploymentId {} with the listed {} for workflow chat {}",
                    projectDeploymentId, listedProjectDeploymentId, workflowExecutionTriggerId);
            }

            int environment = AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext);

            AiHubChat chat = chatService.createWorkflowChat(
                workspaceId, invocationContext.userId(), environment, workflowExecutionTriggerId,
                listedProjectDeploymentId, input.title());

            return jsonMapper.writeValueAsString(
                new CreateWorkflowChatOutput(chat.getThreadId(), chat.getId(), chat.getTitle()));
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, CreateWorkflowChatToolCallback.class, "createWorkflowChat", exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record CreateWorkflowChatInput(
        String workflowExecutionTriggerId, String projectDeploymentId, @Nullable String title) {
    }

    public record CreateWorkflowChatOutput(String threadId, long chatId, @Nullable String title) {
    }
}
