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
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
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

    private static final String MANUAL_TRIGGER_NAME = "manual";

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final String webhookUrl;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowGraphQlController(
        ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TriggerDefinitionService triggerDefinitionService,
        @Value("${bytechef.webhook.url}") String webhookUrl, WorkflowService workflowService) {

        this.projectDeploymentFacade = projectDeploymentFacade;
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

    /**
     * Authorization lives on {@link ProjectDeploymentFacade#getProjectDeploymentWorkflow(WorkflowExecutionId)}, which
     * carries {@code DEPLOYMENT_VIEW and WORKFLOW_VIEW} on the deployment the id names. The body used to resolve the
     * row itself, out of {@code ProjectWorkflowService} and {@code ProjectDeploymentWorkflowService} past the facade
     * entirely — which is how a root query returning a deployment's inputs, its connection bindings and, through
     * {@code projectWorkflow.workflow}, the whole workflow definition, shipped with no gate at all, keyed by the string
     * that is also the path segment of the workflow's public static webhook URL.
     *
     * <p>
     * What remains here is a decode of the caller's own argument and nothing else: no lookup, no service call, and no
     * reading of the deployment id out of the result — {@code WorkflowExecutionId.parse} validates the tenant and fails
     * closed on anything malformed, and it is the facade's {@code @PreAuthorize} that takes {@code jobPrincipalId} off
     * the decoded id and decides. Spring evaluates that expression against the arguments of the method it enters, so a
     * facade that took the undecoded string could not gate on the deployment inside it, and a facade that decoded in
     * its own body and then called its own guarded method would call it past the proxy.
     */
    @QueryMapping(name = "projectDeploymentWorkflow")
    public ProjectDeploymentWorkflow projectDeploymentWorkflow(@Argument String id) {
        return projectDeploymentFacade.getProjectDeploymentWorkflow(WorkflowExecutionId.parse(id));
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
     * Authorization lives on {@link ProjectDeploymentFacade#getWorkspaceChatWorkflows(long, long)}, which carries
     * {@code hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')} and filters the listing through
     * {@code ProjectVisibilityFilter} &mdash; the API facade is this codebase's authorization layer, and this
     * controller carries no gate of its own. Both halves used to live in this method's body, which is also where the
     * hole they close came from: the listing was assembled here, out of services, past the facade entirely.
     *
     * <p>
     * The primitive {@code long} arguments are load-bearing rather than incidental, for the reason
     * {@code AiAgentFacadeAuthorizationTest} spells out: {@code #workspaceId} is only a usable gate key while it cannot
     * be null, since a boxed null would reach {@code AutomationPermissionEvaluator} as a null target id. The schema
     * declares both as {@code ID!} today, so null cannot arrive — declaring them primitive is what makes a later
     * relaxation of the schema fail here, at binding, instead of downstream as an unboxing NPE with the gate already
     * behind it.
     */
    @QueryMapping(name = "workspaceChatWorkflows")
    public List<ChatWorkflow> workspaceChatWorkflows(@Argument long workspaceId, @Argument long environmentId) {
        return projectDeploymentFacade.getWorkspaceChatWorkflows(workspaceId, environmentId);
    }
}
