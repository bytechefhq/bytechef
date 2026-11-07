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
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RunChatWorkflowToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(RunChatWorkflowToolCallback.class);

    private static final String DESCRIPTION = """
        Run a chat workflow returned by listChatWorkflows inline in this chat. Use this when the user
        asks to run one of their existing chat workflows (e.g. "run the Daily Summary workflow").
        Returns one of:
        - {workflowExecutionId, streaming: true, streamUrl}: the client opens the stream and pipes
          the workflow's reply into the chat.
        - {workflowExecutionId, streaming: false, responseUrl}: the client calls the workflow and
          appends its single reply to the chat.
        The workflow's reply IS your response: do NOT produce further content after this call.
        Always call listChatWorkflows first to obtain a real workflowExecutionTriggerId; never invent one.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "workflowExecutionTriggerId": {
                    "type": "string",
                    "description": "The workflowExecutionTriggerId from listChatWorkflows. NOT the workflow id."
                }
            },
            "required": ["workflowExecutionTriggerId"]
        }""";

    private static final String TOOL_NAME = "runChatWorkflow";

    private static final String WORKSPACE_CONTEXT_UNAVAILABLE =
        "Workspace context unavailable. Open this chat from the AI Hub of a workspace.";

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final JsonMapper jsonMapper = new JsonMapper();
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final WorkflowFacade workflowFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RunChatWorkflowToolCallback(
        ProjectDeploymentFacade projectDeploymentFacade, WorkflowFacade workflowFacade,
        @Nullable AiHubChatArtifactRecorder artifactRecorder) {

        this.artifactRecorder = artifactRecorder;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.workflowFacade = workflowFacade;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
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
            RunChatWorkflowInput input = jsonMapper.readValue(toolInput, RunChatWorkflowInput.class);

            String workflowExecutionTriggerId = input.workflowExecutionTriggerId();

            if (workflowExecutionTriggerId == null || workflowExecutionTriggerId.isBlank()) {
                return toolError("workflowExecutionTriggerId is required");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null
                || invocationContext.userId() == null) {

                return toolError(WORKSPACE_CONTEXT_UNAVAILABLE);
            }

            ChatWorkflow chatWorkflow;

            try {
                chatWorkflow = projectDeploymentFacade
                    .findWorkspaceChatWorkflow(invocationContext.workspaceId(), workflowExecutionTriggerId)
                    .orElse(null);
            } catch (AccessDeniedException exception) {
                return toolError("You do not have permission to view the workflows of this workspace.");
            }

            if (chatWorkflow == null) {
                return toolError(
                    workflowExecutionTriggerId + " is not an enabled chat workflow of this workspace. Call " +
                        "listChatWorkflows and use a workflowExecutionTriggerId it returned.");
            }

            boolean streaming = workflowFacade.hasSseStreamResponse(chatWorkflow.workflowId());

            recordExecutionStarted(invocationContext, chatWorkflow);

            if (streaming) {
                return writeResult(
                    new RunChatWorkflowStreamingResult(
                        workflowExecutionTriggerId, true, "/webhooks/" + workflowExecutionTriggerId + "/sse"));
            }

            return writeResult(
                new RunChatWorkflowSyncResult(
                    workflowExecutionTriggerId, false, "/webhooks/" + workflowExecutionTriggerId));
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, RunChatWorkflowToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordExecutionStarted(AiHubToolInvocationContext invocationContext, ChatWorkflow chatWorkflow) {
        if (artifactRecorder == null || invocationContext.threadId() == null) {
            return;
        }

        String workflowLabel = chatWorkflow.workflowLabel() == null
            ? chatWorkflow.workflowId() : chatWorkflow.workflowLabel();

        try {
            artifactRecorder.record(
                invocationContext.threadId(), invocationContext.userId(),
                AiHubChatArtifactKind.WORKFLOW_EXECUTION_STARTED.name(), chatWorkflow.workflowExecutionId(),
                workflowLabel);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to record the workflow execution artifact for runChatWorkflow (workflowExecutionId={})",
                chatWorkflow.workflowExecutionId(), exception);
        }
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    private String writeResult(Object result) {
        try {
            return jsonMapper.writeValueAsString(result);
        } catch (JacksonException exception) {
            return ToolErrors.runtimeFailure(jsonMapper, RunChatWorkflowToolCallback.class, TOOL_NAME, exception);
        }
    }

    public record RunChatWorkflowInput(String workflowExecutionTriggerId) {
    }

    public record RunChatWorkflowStreamingResult(String workflowExecutionId, boolean streaming, String streamUrl) {
    }

    public record RunChatWorkflowSyncResult(String workflowExecutionId, boolean streaming, String responseUrl) {
    }
}
