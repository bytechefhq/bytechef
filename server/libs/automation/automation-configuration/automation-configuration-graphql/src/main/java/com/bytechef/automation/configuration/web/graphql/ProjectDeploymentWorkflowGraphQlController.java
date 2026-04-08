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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ProjectDeploymentWorkflowGraphQlController {

    private static final String CHAT_TRIGGER_TYPE_PREFIX = "chat/";
    private static final String MANUAL_TRIGGER_NAME = "manual";

    private final EnvironmentService environmentService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowGraphQlController(
        EnvironmentService environmentService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TriggerDefinitionService triggerDefinitionService,
        @Value("${bytechef.webhook.url}") String webhookUrl, WorkflowService workflowService) {

        this.environmentService = environmentService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookUrl = webhookUrl;
        this.workflowService = workflowService;
    }

    @SchemaMapping(typeName = "ProjectDeploymentWorkflow", field = "connections")
    public List<ProjectDeploymentWorkflowConnection> connections(ProjectDeploymentWorkflow projectDeploymentWorkflow) {

        return projectDeploymentWorkflow.getConnections();
    }

    @QueryMapping(name = "projectDeploymentWorkflow")
    public ProjectDeploymentWorkflow projectDeploymentWorkflow(@Argument String id) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(id);

        long projectDeploymentId = workflowExecutionId.getJobPrincipalId();
        String workflowUuid = workflowExecutionId.getWorkflowUuid();

        String workflowId = projectWorkflowService.getProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid);

        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentId, workflowId);
    }

    @BatchMapping(typeName = "ProjectDeployment", field = "projectDeploymentWorkflows")
    public Map<ProjectDeployment, List<ProjectDeploymentWorkflow>> projectDeploymentWorkflows(
        List<ProjectDeployment> projectDeployments) {

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(CollectionUtils.map(projectDeployments, ProjectDeployment::getId));

        return projectDeployments.stream()
            .collect(
                Collectors.toMap(
                    project -> project,
                    projectDeployment -> projectDeploymentWorkflows.stream()
                        .filter(projectDeploymentWorkflow -> Objects.equals(
                            projectDeploymentWorkflow.getProjectDeploymentId(), projectDeployment.getId()))
                        .toList()));
    }

    @SchemaMapping(typeName = "ProjectDeploymentWorkflow", field = "staticWebhookUrl")
    public String staticWebhookUrl(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        String workflowExecutionId = workflowExecutionId(projectDeploymentWorkflow);

        if (workflowExecutionId != null) {
            return webhookUrl.replace("{id}", workflowExecutionId);
        }

        return null;
    }

    @SchemaMapping(typeName = "ProjectDeploymentWorkflow", field = "workflowExecutionId")
    public String workflowExecutionId(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            var triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                Objects.requireNonNull(triggerWorkflowNodeType.operation()));

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), MANUAL_TRIGGER_NAME)) {

                var projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                    projectWorkflow.getUuidAsString(), workflowTrigger.getName());

                return workflowExecutionId.toString();
            }
        }

        return null;
    }

    /**
     * Returns the flat list of enabled hosted-chat workflows across the workspace. Purpose-built for the workflow chat
     * sidebar — filters, batches service calls, and memoizes trigger-definition lookups within the request to avoid the
     * N+1 that the previous {@code ProjectDeploymentWorkflow.staticWebhookUrl}/{@code workflowExecutionId} schema
     * mappings incurred.
     */
    @QueryMapping(name = "workspaceChatWorkflows")
    public List<ChatWorkflow> workspaceChatWorkflows(@Argument Long workspaceId, @Argument Long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            false, environment, null, null, workspaceId);

        if (projectDeployments.isEmpty()) {
            return List.of();
        }

        List<ProjectDeployment> enabledProjectDeployments = projectDeployments.stream()
            .filter(ProjectDeployment::isEnabled)
            .toList();

        if (enabledProjectDeployments.isEmpty()) {
            return List.of();
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
            return List.of();
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

        Map<Long, Project> projectMap = projectService.getProjects(
            enabledProjectDeployments.stream()
                .map(ProjectDeployment::getProjectId)
                .distinct()
                .toList())
            .stream()
            .collect(Collectors.toMap(project -> Objects.requireNonNull(project.getId(), "id"), Function.identity()));

        Map<Long, ProjectDeployment> projectDeploymentMap = enabledProjectDeployments.stream()
            .collect(Collectors.toMap(ProjectDeployment::getId, Function.identity()));

        Map<TriggerDefinitionKey, TriggerDefinition> triggerDefinitionMap = new HashMap<>();

        List<ChatWorkflow> chatWorkflows = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : enabledProjectDeploymentWorkflows) {
            Workflow workflow = workflowMap.get(projectDeploymentWorkflow.getWorkflowId());

            if (workflow == null) {
                continue;
            }

            List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

            if (workflowTriggers.isEmpty() || !hasHostedChatTrigger(workflowTriggers)) {
                continue;
            }

            String webhookExecutionId = resolveStaticWebhookExecutionId(
                workflow, workflowTriggers, projectDeploymentWorkflow, projectWorkflowMap, triggerDefinitionMap);

            if (webhookExecutionId == null) {
                continue;
            }

            ProjectDeployment projectDeployment =
                projectDeploymentMap.get(projectDeploymentWorkflow.getProjectDeploymentId());

            if (projectDeployment == null) {
                continue;
            }

            Project project = projectMap.get(projectDeployment.getProjectId());

            chatWorkflows.add(
                new ChatWorkflow(
                    projectDeploymentWorkflow.getProjectDeploymentId(), projectDeployment.getProjectId(),
                    project == null ? "Untitled Project" : project.getName(), webhookExecutionId,
                    workflow.getLabel() == null ? "Untitled Workflow" : workflow.getLabel()));
        }

        return chatWorkflows;
    }

    /**
     * Matches the client's legacy filter: at least one trigger type starts with {@code chat/}, and the first trigger's
     * {@code parameters.mode} is absent or equal to {@code 1}.
     */
    private static boolean hasHostedChatTrigger(List<WorkflowTrigger> workflowTriggers) {
        boolean hasChatTrigger = workflowTriggers.stream()
            .map(WorkflowTrigger::getType)
            .anyMatch(type -> type != null && type.startsWith(CHAT_TRIGGER_TYPE_PREFIX));

        if (!hasChatTrigger) {
            return false;
        }

        WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

        Map<String, ?> parameters = workflowTrigger.getParameters();

        Object mode = parameters.get("mode");

        if (mode == null) {
            return true;
        }

        return mode instanceof Number number && number.intValue() == 1;
    }

    private String resolveStaticWebhookExecutionId(
        Workflow workflow, List<WorkflowTrigger> workflowTriggers, ProjectDeploymentWorkflow deploymentWorkflow,
        Map<String, ProjectWorkflow> projectWorkflowsByWorkflowId,
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

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), MANUAL_TRIGGER_NAME)) {

                ProjectWorkflow projectWorkflow = projectWorkflowsByWorkflowId.get(workflow.getId());

                if (projectWorkflow == null) {
                    return null;
                }

                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, deploymentWorkflow.getProjectDeploymentId(),
                    projectWorkflow.getUuidAsString(), workflowTrigger.getName());

                return workflowExecutionId.toString();
            }
        }

        return null;
    }

    public record ChatWorkflow(
        long projectDeploymentId, long projectId, String projectName, String workflowExecutionId,
        String workflowLabel) {
    }

    private record TriggerDefinitionKey(String name, int version, String operation) {
    }
}
