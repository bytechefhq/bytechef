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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for editing an exposed workflow's A2A skill metadata (name, description, tags), which lives in
 * {@link A2aProjectWorkflow#getParameters()}. The serve path falls back to the workflow's own label/description when a
 * value is unset.
 *
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
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProjectWorkflow updateA2aProjectWorkflowParameters(
        @Argument long id, @Argument A2aProjectWorkflowParametersInput input) {

        A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflowService.fetchA2aProjectWorkflow(id)
            .orElseThrow(() -> new IllegalArgumentException("A2aProjectWorkflow not found: " + id));

        Map<String, Object> parameters = new HashMap<>(a2aProjectWorkflow.getParameters());

        if (input.skillName() != null) {
            parameters.put(A2aProjectWorkflow.SKILL_NAME, input.skillName());
        }

        if (input.skillDescription() != null) {
            parameters.put(A2aProjectWorkflow.SKILL_DESCRIPTION, input.skillDescription());
        }

        if (input.skillTags() != null) {
            parameters.put(A2aProjectWorkflow.SKILL_TAGS, input.skillTags());
        }

        return a2aProjectWorkflowService.updateParameters(id, parameters);
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
        return asString(a2aProjectWorkflow.getParameters()
            .get(A2aProjectWorkflow.SKILL_NAME));
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "skillDescription")
    public @Nullable String skillDescription(A2aProjectWorkflow a2aProjectWorkflow) {
        return asString(a2aProjectWorkflow.getParameters()
            .get(A2aProjectWorkflow.SKILL_DESCRIPTION));
    }

    @SchemaMapping(typeName = "A2aProjectWorkflow", field = "skillTags")
    public List<String> skillTags(A2aProjectWorkflow a2aProjectWorkflow) {
        Object tagsValue = a2aProjectWorkflow.getParameters()
            .get(A2aProjectWorkflow.SKILL_TAGS);

        if (tagsValue instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .toList();
        }

        return List.of();
    }

    private @Nullable ProjectDeploymentWorkflow getProjectDeploymentWorkflow(A2aProjectWorkflow a2aProjectWorkflow) {
        Long projectDeploymentWorkflowId = a2aProjectWorkflow.getProjectDeploymentWorkflowId();

        if (projectDeploymentWorkflowId == null) {
            return null;
        }

        return projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId);
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressFBWarnings("EI")
    public record A2aProjectWorkflowParametersInput(
        @Nullable String skillName, @Nullable String skillDescription, @Nullable List<String> skillTags) {
    }
}
