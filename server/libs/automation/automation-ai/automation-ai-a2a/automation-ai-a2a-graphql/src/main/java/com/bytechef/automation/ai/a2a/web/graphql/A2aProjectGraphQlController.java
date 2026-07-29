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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing the workflows an A2A server exposes as A2A skills, modeled by {@link A2aProject} (its
 * provisioned project deployment) and its {@link A2aProjectWorkflow}s.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class A2aProjectGraphQlController {

    private final A2aProjectFacade a2aProjectFacade;
    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public A2aProjectGraphQlController(
        A2aProjectFacade a2aProjectFacade, A2aProjectService a2aProjectService,
        A2aProjectWorkflowService a2aProjectWorkflowService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.a2aProjectFacade = a2aProjectFacade;
        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<A2aProject> a2aProjectsByServerId(@Argument long a2aServerId) {
        return a2aProjectService.getA2aServerA2aProjects(a2aServerId);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProject createA2aProject(@Argument CreateA2aProjectInput input) {
        return a2aProjectFacade.createA2aProject(
            input.a2aServerId(), input.projectId(), input.projectVersion(), input.selectedWorkflowIds());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProject updateA2aProject(@Argument long id, @Argument UpdateA2aProjectInput input) {
        return a2aProjectFacade.updateA2aProject(id, input.selectedWorkflowIds());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean deleteA2aProject(@Argument long id) {
        a2aProjectFacade.deleteA2aProject(id);

        return true;
    }

    @SchemaMapping(typeName = "A2aProject", field = "projectId")
    public @Nullable Long projectId(A2aProject a2aProject) {
        ProjectDeployment projectDeployment = getProjectDeployment(a2aProject);

        return projectDeployment == null ? null : projectDeployment.getProjectId();
    }

    @SchemaMapping(typeName = "A2aProject", field = "projectVersion")
    public @Nullable Integer projectVersion(A2aProject a2aProject) {
        ProjectDeployment projectDeployment = getProjectDeployment(a2aProject);

        return projectDeployment == null ? null : projectDeployment.getProjectVersion();
    }

    @SchemaMapping(typeName = "A2aProject", field = "workflowIds")
    public List<String> workflowIds(A2aProject a2aProject) {
        List<String> workflowIds = new ArrayList<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProject.getId())) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflow(a2aProjectWorkflow.getProjectDeploymentWorkflowId());

            workflowIds.add(projectDeploymentWorkflow.getWorkflowId());
        }

        return workflowIds;
    }

    private @Nullable ProjectDeployment getProjectDeployment(A2aProject a2aProject) {
        Long projectDeploymentId = a2aProject.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return null;
        }

        return projectDeploymentService.getProjectDeployment(projectDeploymentId);
    }

    @SuppressFBWarnings("EI")
    public record CreateA2aProjectInput(
        long a2aServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateA2aProjectInput(List<String> selectedWorkflowIds) {
    }
}
