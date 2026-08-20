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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ProjectDeploymentGraphQlController {

    private final EnvironmentService environmentService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectService projectService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentGraphQlController(
        EnvironmentService environmentService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectService projectService, TagService tagService) {

        this.environmentService = environmentService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectService = projectService;
        this.tagService = tagService;
    }

    @BatchMapping(typeName = "ProjectDeployment", field = "environment")
    public Map<ProjectDeployment, EnvironmentDTO> environment(List<ProjectDeployment> projectDeployments) {
        return projectDeployments.stream()
            .collect(
                Collectors.toMap(
                    projectDeployment -> projectDeployment,
                    projectDeployment -> new EnvironmentDTO(
                        environmentService.getEnvironment(projectDeployment.getEnvironmentId()))));
    }

    @BatchMapping(typeName = "ProjectDeployment", field = "project")
    public Map<ProjectDeployment, Project> project(List<ProjectDeployment> projectDeployments) {
        List<Long> projectIds = projectDeployments.stream()
            .map(ProjectDeployment::getProjectId)
            .distinct()
            .toList();

        Map<Long, Project> projectMap = projectService.getProjects(projectIds)
            .stream()
            .collect(Collectors.toMap(
                project -> Objects.requireNonNull(project.getId(), "id"), Function.identity()));

        return projectDeployments.stream()
            .collect(
                Collectors.toMap(
                    projectDeployment -> projectDeployment,
                    projectDeployment -> projectMap.get(projectDeployment.getProjectId())));
    }

    @BatchMapping(typeName = "ProjectDeployment", field = "tags")
    public Map<ProjectDeployment, List<Tag>> tags(List<ProjectDeployment> projectDeployments) {
        List<Long> tagIds = projectDeployments.stream()
            .flatMap(projectDeployment -> CollectionUtils.stream(projectDeployment.getTagIds()))
            .distinct()
            .toList();

        List<Tag> tags = tagService.getTags(tagIds);

        return projectDeployments.stream()
            .collect(
                Collectors.toMap(
                    projectDeployment -> projectDeployment,
                    projectDeployment -> tags.stream()
                        .filter(tag -> projectDeployment.getTagIds()
                            .contains(tag.getId()))
                        .toList()));
    }

    /**
     * Every deployment in the workspace the caller may see.
     *
     * <p>
     * Authorization lives on {@link ProjectDeploymentFacade#getWorkspaceProjectDeployments(long, long, Long, Long)},
     * which carries {@code hasPermission(#workspaceId, 'Workspace', 'DEPLOYMENT_VIEW')} and drops both the
     * feature-owned system projects and the projects the caller cannot see — the same gate and the same two filters the
     * REST twin has always applied to the identical set. Until that seam existed this method read the rows off
     * {@code ProjectDeploymentService} directly, past the facade, which is how it came to have neither.
     *
     * <p>
     * {@code workspaceId} and {@code environmentId} are primitive and {@code projectId} and {@code tagId} are not,
     * matching the schema exactly: the first two are {@code ID!} and the last two are nullable filters. The distinction
     * is load-bearing for {@code workspaceId} in the sense {@code AiAgentFacadeAuthorizationTest} spells out —
     * {@code #workspaceId} is only a usable gate key while it cannot be null, since a boxed null would reach
     * {@code AutomationPermissionEvaluator} as a null target id. Declaring it primitive is what makes a later
     * relaxation of the schema fail here, at binding, instead of downstream as an unboxing NPE with the gate already
     * behind it.
     */
    @QueryMapping(name = "workspaceProjectDeployments")
    public List<ProjectDeployment> workspaceProjectDeployments(
        @Argument long workspaceId, @Argument long environmentId, @Argument Long projectId, @Argument Long tagId) {

        return projectDeploymentFacade.getWorkspaceProjectDeployments(workspaceId, environmentId, projectId, tagId);
    }

    public record EnvironmentDTO(long id, String name) {
        public EnvironmentDTO(Environment environment) {
            this(environment.ordinal(), environment.name());
        }
    }
}
