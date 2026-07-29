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
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing shared workflows in the automation configuration module.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ProjectWorkflowGraphQlController {

    private static final String ERROR_TRIGGER_COMPONENT_NAME = "workflow";
    private static final String ERROR_TRIGGER_OPERATION_NAME = "newWorkflowError";

    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowGraphQlController(
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        WorkflowFacade workflowFacade, WorkflowService workflowService) {

        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
    }

    @MutationMapping
    public Boolean deleteSharedWorkflow(@Argument String workflowId) {
        projectWorkflowFacade.deleteSharedWorkflow(workflowId);

        return true;
    }

    @MutationMapping
    public Boolean exportSharedWorkflow(@Argument String workflowId, @Argument String description) {
        projectWorkflowFacade.exportSharedWorkflow(workflowId, description);

        return true;
    }

    @MutationMapping
    public Long importWorkflowTemplate(
        @Argument String id, @Argument Long projectId, @Argument boolean sharedWorkflow) {

        return projectWorkflowFacade.importWorkflowTemplate(projectId, id, sharedWorkflow);
    }

    @MutationMapping
    public Boolean updateProjectWorkflowErrorWorkflow(
        @Argument long projectId, @Argument long projectWorkflowId,
        @Argument @Nullable Long errorProjectWorkflowId, @Argument boolean errorWorkflowDisabled) {

        try {
            projectWorkflowFacade.updateWorkflowErrorWorkflow(
                projectId, projectWorkflowId, errorProjectWorkflowId, errorWorkflowDisabled);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new GraphQlBadRequestException(illegalArgumentException.getMessage(), illegalArgumentException);
        }

        return true;
    }

    // Gated: these back the error-workflow pickers, whose mutation is authorized on the same project. Leaving the
    // reads open would let any authenticated user enumerate another workspace's workflow ids and labels.
    @QueryMapping
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public List<ProjectWorkflow> eligibleErrorWorkflows(@Argument long projectId, @Argument int projectVersion) {
        return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .filter(projectWorkflow -> hasErrorTrigger(workflowService.getWorkflow(projectWorkflow.getWorkflowId())))
            .toList();
    }

    @QueryMapping(name = "preBuiltWorkflowTemplates")
    public List<WorkflowTemplateDTO> preBuiltWorkflowTemplates(@Argument String query, @Argument String category) {
        return projectWorkflowFacade.getPreBuiltWorkflowTemplates(query, category);
    }

    @QueryMapping
    @PreAuthorize("hasPermission(#id, 'ProjectWorkflow', 'WORKFLOW_EDIT')")
    public ProjectWorkflow projectWorkflow(@Argument long id) {
        return projectWorkflowService.getProjectWorkflow(id);
    }

    @SchemaMapping(typeName = "ProjectDeploymentWorkflow", field = "projectWorkflow")
    public ProjectWorkflow projectWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        return projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());
    }

    @SchemaMapping(typeName = "ProjectWorkflow", field = "sseStreamResponse")
    public boolean sseStreamResponse(ProjectWorkflow projectWorkflow) {
        return workflowFacade.hasSseStreamResponse(projectWorkflow.getWorkflowId());
    }

    @SchemaMapping(typeName = "ProjectWorkflow", field = "workflow")
    public WorkflowDTO workflow(ProjectWorkflow projectWorkflow) {
        Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        return new WorkflowDTO(workflow, List.of(), List.of());
    }

    @QueryMapping(name = "sharedWorkflow")
    public SharedWorkflowDTO sharedWorkflow(@Argument String workflowUuid) {
        return projectWorkflowFacade.getSharedWorkflow(workflowUuid);
    }

    @QueryMapping(name = "workflowTemplate")
    public WorkflowTemplateDTO workflowTemplate(@Argument String id, @Argument boolean sharedWorkflow) {
        return projectWorkflowFacade.getWorkflowTemplate(id, sharedWorkflow);
    }

    /*
     * Deliberately duplicated from ErrorWorkflowConfigurationValidator#isErrorTrigger (and
     * ErrorWorkflowResolver#isErrorTrigger) rather than shared cross-module: this module
     * (automation-configuration-graphql) depends only on automation-configuration-api, so it cannot reach the -service
     * class that owns the canonical check.
     */
    private static boolean hasErrorTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), ERROR_TRIGGER_COMPONENT_NAME) &&
                Objects.equals(workflowNodeType.operation(), ERROR_TRIGGER_OPERATION_NAME)) {

                return true;
            }
        }

        return false;
    }
}
