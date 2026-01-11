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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowGraphQlController(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TriggerDefinitionService triggerDefinitionService,
        @Value("${bytechef.webhook.url}") String webhookUrl, WorkflowService workflowService) {

        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
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
                !Objects.equals(triggerDefinition.getName(), "manual")) {

                var projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, projectDeploymentWorkflow.getProjectDeploymentId(),
                    projectWorkflow.getUuidAsString(), workflowTrigger.getName());

                return workflowExecutionId.toString();
            }
        }

        return null;
    }
}
