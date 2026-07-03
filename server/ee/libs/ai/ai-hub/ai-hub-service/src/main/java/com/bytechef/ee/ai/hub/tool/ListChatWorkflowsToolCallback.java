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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that lists all chat-eligible workflows in the current workspace. A workflow is
 * chat-eligible if its trigger type starts with {@code chat/} and the trigger's {@code mode} parameter is absent or
 * equal to 1 (hosted-chat mode). Returns a compact JSON array suitable for the AI Hub BUILD agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ListChatWorkflowsToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(ListChatWorkflowsToolCallback.class);

    private static final String CHAT_TRIGGER_TYPE_PREFIX = "chat/";

    private static final String DESCRIPTION = """
        List all workflows in the workspace that can be run as a chat — i.e. those
        whose trigger is `chat/newChatRequest` in hosted-chat mode. Returns a JSON
        array where each item has: projectDeploymentId (string), projectId (string),
        workflowExecutionTriggerId (the webhook execution id), workflowLabel (string),
        and streaming (boolean — true when the workflow streams chunks rather than
        returning a single response). Use this before calling runChatWorkflow so you
        know which workflowExecutionTriggerId to pass. Optionally filter by projectId.
        Returns an empty array when no eligible workflows are found.""";

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

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final JsonMapper jsonMapper = new JsonMapper();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ListChatWorkflowsToolCallback(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        TriggerDefinitionService triggerDefinitionService, WorkflowFacade workflowFacade,
        WorkflowService workflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
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

            AiHubToolInvocationContext invocationContext =
                AiHubToolInvocationContext.fromToolContext(toolContext);

            Long workspaceId = invocationContext.workspaceId();

            if (workspaceId == null) {
                return toolError(
                    "Workspace context unavailable — open this chat from the AI Hub of a workspace.");
            }

            List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
                false, null, null, null, workspaceId);

            if (projectDeployments.isEmpty()) {
                return jsonMapper.writeValueAsString(List.of());
            }

            List<ProjectDeployment> enabledProjectDeployments = projectDeployments.stream()
                .filter(ProjectDeployment::isEnabled)
                .toList();

            if (enabledProjectDeployments.isEmpty()) {
                return jsonMapper.writeValueAsString(List.of());
            }

            if (input.projectId() != null) {
                try {
                    Long filterProjectId = Long.parseLong(input.projectId());

                    enabledProjectDeployments = enabledProjectDeployments.stream()
                        .filter(deployment -> filterProjectId.equals(deployment.getProjectId()))
                        .toList();
                } catch (NumberFormatException exception) {
                    return toolError("Invalid projectId: " + input.projectId());
                }
            }

            if (enabledProjectDeployments.isEmpty()) {
                return jsonMapper.writeValueAsString(List.of());
            }

            List<Long> projectDeploymentIds = enabledProjectDeployments.stream()
                .map(ProjectDeployment::getId)
                .toList();

            List<ProjectDeploymentWorkflow> enabledProjectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(projectDeploymentIds)
                .stream()
                .filter(ProjectDeploymentWorkflow::isEnabled)
                .toList();

            if (enabledProjectDeploymentWorkflows.isEmpty()) {
                return jsonMapper.writeValueAsString(List.of());
            }

            List<String> workflowIds = enabledProjectDeploymentWorkflows.stream()
                .map(ProjectDeploymentWorkflow::getWorkflowId)
                .distinct()
                .toList();

            Map<String, Workflow> workflowMap = workflowService.getWorkflows(workflowIds)
                .stream()
                .collect(Collectors.toMap(Workflow::getId, Function.identity()));

            Map<String, ProjectWorkflow> projectWorkflowMap = projectWorkflowService
                .getWorkflowProjectWorkflows(workflowIds)
                .stream()
                .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, Function.identity()));

            Map<TriggerDefinitionKey, TriggerDefinition> triggerDefinitionCache = new HashMap<>();

            List<ChatWorkflowSummary> summaries = new ArrayList<>();

            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : enabledProjectDeploymentWorkflows) {
                Workflow workflow = workflowMap.get(projectDeploymentWorkflow.getWorkflowId());

                if (workflow == null) {
                    continue;
                }

                List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

                if (workflowTriggers.isEmpty() || !hasHostedChatTrigger(workflowTriggers)) {
                    continue;
                }

                ProjectWorkflow projectWorkflow = projectWorkflowMap.get(workflow.getId());

                if (projectWorkflow == null) {
                    continue;
                }

                String webhookExecutionId = resolveStaticWebhookExecutionId(
                    workflowTriggers, projectDeploymentWorkflow, projectWorkflow,
                    triggerDefinitionCache);

                if (webhookExecutionId == null) {
                    continue;
                }

                Optional<Long> resolvedProjectId = findProjectId(
                    enabledProjectDeployments, projectDeploymentWorkflow.getProjectDeploymentId());

                if (resolvedProjectId.isEmpty()) {
                    log.warn(
                        "Skipping chat-eligible workflow {} (deploymentId={}) — no resolvable projectId. Either the "
                            + "deployment is missing from the enabled list (data race?) or the deployment row has a "
                            + "null projectId (data integrity bug). Surfacing this workflow with projectId=\"0\" would "
                            + "produce phantom downstream tool calls.",
                        workflow.getId(), projectDeploymentWorkflow.getProjectDeploymentId());

                    continue;
                }

                boolean streaming = workflowFacade.hasSseStreamResponse(workflow.getId());

                summaries.add(
                    new ChatWorkflowSummary(
                        String.valueOf(projectDeploymentWorkflow.getProjectDeploymentId()),
                        String.valueOf(resolvedProjectId.get()),
                        webhookExecutionId,
                        workflow.getLabel() == null ? "Untitled Workflow" : workflow.getLabel(),
                        streaming));
            }

            return jsonMapper.writeValueAsString(summaries);
        } catch (JacksonException exception) {
            return toolError("Serialization error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(
                jsonMapper, ListChatWorkflowsToolCallback.class, "listChatWorkflows", exception);
        }
    }

    private static boolean hasHostedChatTrigger(List<WorkflowTrigger> workflowTriggers) {
        boolean hasChatTrigger = workflowTriggers.stream()
            .map(WorkflowTrigger::getType)
            .anyMatch(type -> type != null && type.startsWith(CHAT_TRIGGER_TYPE_PREFIX));

        if (!hasChatTrigger) {
            return false;
        }

        WorkflowTrigger firstTrigger = workflowTriggers.getFirst();
        Map<String, ?> parameters = firstTrigger.getParameters();
        Object mode = parameters.get("mode");

        if (mode == null) {
            return true;
        }

        return mode instanceof Number number && number.intValue() == 1;
    }

    private @Nullable String resolveStaticWebhookExecutionId(
        List<WorkflowTrigger> workflowTriggers,
        ProjectDeploymentWorkflow projectDeploymentWorkflow, ProjectWorkflow projectWorkflow,
        Map<TriggerDefinitionKey, TriggerDefinition> triggerDefinitionCache) {

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinitionKey triggerDefinitionKey = new TriggerDefinitionKey(
                workflowNodeType.name(), workflowNodeType.version(),
                Objects.requireNonNull(workflowNodeType.operation()));

            TriggerDefinition triggerDefinition = triggerDefinitionCache.computeIfAbsent(
                triggerDefinitionKey,
                lookupKey -> triggerDefinitionService.getTriggerDefinition(
                    lookupKey.name(), lookupKey.version(), lookupKey.operation()));

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK) {
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                    projectWorkflow.getUuidAsString(), workflowTrigger.getName());

                return workflowExecutionId.toString();
            }
        }

        return null;
    }

    private static Optional<Long> findProjectId(List<ProjectDeployment> deployments, Long projectDeploymentId) {
        return deployments.stream()
            .filter(deployment -> projectDeploymentId.equals(deployment.getId()))
            .findFirst()
            .map(ProjectDeployment::getProjectId);
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record ListChatWorkflowsInput(@Nullable String projectId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatWorkflowSummary(
        String projectDeploymentId, String projectId, String workflowExecutionTriggerId, String workflowLabel,
        boolean streaming) {
    }

    private record TriggerDefinitionKey(String name, int version, String operation) {
    }
}
