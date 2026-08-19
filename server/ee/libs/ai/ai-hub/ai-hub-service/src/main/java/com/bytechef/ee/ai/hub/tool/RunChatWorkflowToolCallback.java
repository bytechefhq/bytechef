/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that validates a chat-eligible workflow and returns the webhook URL the client needs
 * to trigger it. The actual HTTP call (POST or SSE) is performed client-side by the AG-UI subscriber, which pipes the
 * response into the active assistant message.
 *
 * <p>
 * Returns one of three shapes:
 * <ol>
 * <li>{@code {awaitingInput: true, inputSchema, reason}} — required fields are missing from {@code input}.
 * <li>{@code {workflowExecutionId, streaming: true, streamUrl}} — workflow uses SSE streaming.
 * <li>{@code {workflowExecutionId, streaming: false, responseUrl}} — workflow returns a single response.
 * </ol>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RunChatWorkflowToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(RunChatWorkflowToolCallback.class);

    private static final String DESCRIPTION = """
        Fire a chat-eligible workflow (one with a chat.newChatRequest trigger).
        Provide the workflowExecutionTriggerId returned by listChatWorkflows plus
        an optional input object. Returns one of:
        - {awaitingInput: true, reason, inputSchema} — required input fields are
          missing; ask the user for them and call again with the completed input.
        - {workflowExecutionId, streaming: true, streamUrl} — the client will open
          the SSE stream and pipe chunks into the chat. Do NOT produce further content
          after this call.
        - {workflowExecutionId, streaming: false, responseUrl} — the client will POST
          and append the single response to the chat. Same contract — stop here.
        Never call this for workflows that do not appear in listChatWorkflows.""";

    private static final String INPUT_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "workflowExecutionTriggerId": {
                    "type": "string",
                    "description": "The workflowExecutionTriggerId from listChatWorkflows"
                },
                "input": {
                    "type": "object",
                    "description": "Optional input fields required by the workflow trigger"
                }
            },
            "required": ["workflowExecutionTriggerId"]
        }""";

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final AiHubChatArtifactService chatArtifactService;
    private final JsonMapper jsonMapper;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RunChatWorkflowToolCallback(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        WorkflowFacade workflowFacade, WorkflowService workflowService) {

        this(projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService, workflowFacade,
            workflowService, null, new JsonMapper());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RunChatWorkflowToolCallback(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        WorkflowFacade workflowFacade, WorkflowService workflowService,
        AiHubChatArtifactService chatArtifactService) {

        this(projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService, workflowFacade,
            workflowService, chatArtifactService, new JsonMapper());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public RunChatWorkflowToolCallback(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        WorkflowFacade workflowFacade, WorkflowService workflowService,
        AiHubChatArtifactService chatArtifactService, JsonMapper jsonMapper) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.chatArtifactService = chatArtifactService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("runChatWorkflow")
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

            if (input.workflowExecutionTriggerId() == null || input.workflowExecutionTriggerId()
                .isBlank()) {
                return toolError("workflowExecutionTriggerId is required");
            }

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            WorkflowExecutionId workflowExecutionId;

            try {
                workflowExecutionId = WorkflowExecutionId.parse(input.workflowExecutionTriggerId());
            } catch (IllegalArgumentException exception) {
                log.warn(
                    "Invalid workflowExecutionTriggerId '{}' supplied to runChatWorkflow tool",
                    input.workflowExecutionTriggerId(), exception);

                return toolError("Invalid workflowExecutionTriggerId: " + input.workflowExecutionTriggerId());
            }

            long projectDeploymentId = workflowExecutionId.getJobPrincipalId();

            try {
                projectDeploymentService.getProjectDeployment(projectDeploymentId);
            } catch (IllegalArgumentException exception) {
                // Service throws IllegalArgumentException for "deployment not found"; re-package as a tool error
                // so the LLM can recover. Other exceptions (data-access, NPE) propagate so the caller sees them.
                log.warn(
                    "Workflow deployment {} not found while running chat workflow {}",
                    projectDeploymentId, input.workflowExecutionTriggerId(), exception);

                return toolError("Workflow deployment not found: " + input.workflowExecutionTriggerId());
            }

            List<ProjectDeployment> workspaceDeployments = projectDeploymentService.getProjectDeployments(
                false, null, null, null, workspaceId);

            boolean belongsToWorkspace = workspaceDeployments.stream()
                .anyMatch(deployment -> Objects.equals(deployment.getId(), projectDeploymentId));

            if (!belongsToWorkspace) {
                return toolError(
                    "Workflow does not belong to the current workspace. Cross-workspace runs are not allowed.");
            }

            String workflowUuid = workflowExecutionId.getWorkflowUuid();
            String workflowId = projectWorkflowService.getProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);

            Optional<ProjectDeploymentWorkflow> projectDeploymentWorkflowOptional =
                projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId);

            if (projectDeploymentWorkflowOptional.isEmpty() ||
                !projectDeploymentWorkflowOptional.get()
                    .isEnabled()) {
                return toolError("Workflow is not enabled in this deployment.");
            }

            Workflow workflow = workflowService.getWorkflow(workflowId);

            if (workflow == null) {
                return toolError("Workflow not found for id: " + workflowId);
            }

            boolean streaming = workflowFacade.hasSseStreamResponse(workflowId);

            String triggerId = input.workflowExecutionTriggerId();

            recordWorkflowExecutionStarted(invocationContext, workflow, triggerId);

            if (streaming) {
                return jsonMapper.writeValueAsString(
                    new RunChatWorkflowStreamingResult(triggerId, true, "/webhooks/" + triggerId + "/sse"));
            } else {
                return jsonMapper.writeValueAsString(
                    new RunChatWorkflowSyncResult(triggerId, false, "/webhooks/" + triggerId));
            }
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, RunChatWorkflowToolCallback.class, "runChatWorkflow", exception);
        }
    }

    private void recordWorkflowExecutionStarted(
        AiHubToolInvocationContext invocationContext, Workflow workflow, String triggerId) {

        if (chatArtifactService == null || invocationContext.threadId() == null) {
            return;
        }

        if (invocationContext.userId() == null) {
            log.warn(
                "Skipping WORKFLOW_EXECUTION_STARTED artifact record for triggerId={} — userId is null. Ensure "
                    + "the tool context carries a threadId before invoking the tool.",
                triggerId);

            return;
        }

        String workflowLabel = workflow.getLabel() != null ? workflow.getLabel() : workflow.getId();

        chatArtifactService.record(
            invocationContext.threadId(), invocationContext.userId(),
            AiHubChatArtifactKind.WORKFLOW_EXECUTION_STARTED, triggerId, workflowLabel, null);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record RunChatWorkflowInput(
        String workflowExecutionTriggerId, @Nullable JsonNode input) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RunChatWorkflowStreamingResult(
        String workflowExecutionId, boolean streaming, String streamUrl) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RunChatWorkflowSyncResult(
        String workflowExecutionId, boolean streaming, String responseUrl) {
    }
}
