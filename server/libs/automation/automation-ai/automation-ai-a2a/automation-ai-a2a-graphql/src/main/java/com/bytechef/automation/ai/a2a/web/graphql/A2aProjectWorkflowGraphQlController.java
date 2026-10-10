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

package com.bytechef.automation.ai.a2a.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class A2aProjectWorkflowGraphQlController {

    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public A2aProjectWorkflowGraphQlController(
        A2aProjectWorkflowService a2aProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowService = workflowService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<A2aProjectWorkflow> a2aProjectWorkflowsByA2aProjectId(@Argument Long a2aProjectId) {
        return a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId);
    }

    @MutationMapping
    public A2aProjectWorkflow updateA2aProjectWorkflowEnabled(@Argument long id, @Argument boolean enabled) {
        return a2aProjectWorkflowService.updateEnabled(id, enabled);
    }

    @MutationMapping
    public A2aProjectWorkflow updateA2aProjectWorkflowParameters(
        @Argument long id, @Argument A2aProjectWorkflowParametersInput input) {

        return a2aProjectWorkflowService.updateSkill(id, input.skillName(), input.skillDescription());
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "workflowId")
    public @Nullable String workflowId(A2aProjectWorkflow a2aProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = getProjectDeploymentWorkflow(a2aProjectWorkflow);

        return projectDeploymentWorkflow == null ? null : projectDeploymentWorkflow.getWorkflowId();
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "workflowLabel")
    public @Nullable String workflowLabel(A2aProjectWorkflow a2aProjectWorkflow) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = getProjectDeploymentWorkflow(a2aProjectWorkflow);

        if (projectDeploymentWorkflow == null) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

        return workflow.getLabel();
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "skillName")
    public @Nullable String skillName(A2aProjectWorkflow a2aProjectWorkflow) {
        return a2aProjectWorkflow.getSkillName();
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "skillDescription")
    public @Nullable String skillDescription(A2aProjectWorkflow a2aProjectWorkflow) {
        return a2aProjectWorkflow.getSkillDescription();
    }

    private @Nullable ProjectDeploymentWorkflow getProjectDeploymentWorkflow(A2aProjectWorkflow a2aProjectWorkflow) {
        Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

        if (projectDeploymentWorkflowId == null) {
            return null;
        }

        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);
    }

    @SuppressFBWarnings("EI")
    public record A2aProjectWorkflowParametersInput(@Nullable String skillName, @Nullable String skillDescription) {
    }
}
