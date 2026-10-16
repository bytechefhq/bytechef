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
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
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
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentGraphQlController(
        EnvironmentService environmentService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService, TagService tagService) {

        this.environmentService = environmentService;
        this.projectDeploymentService = projectDeploymentService;
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

    @QueryMapping(name = "workspaceProjectDeployments")
    public List<ProjectDeployment> workspaceProjectDeployments(
        @Argument Long workspaceId, @Argument Long environmentId, @Argument Long projectId, @Argument Long tagId) {

        Environment environment = environmentService.getEnvironment(environmentId);

        return projectDeploymentService.getProjectDeployments(false, environment, projectId, tagId, workspaceId);
    }

    public record EnvironmentDTO(long id, String name) {
        public EnvironmentDTO(Environment environment) {
            this(environment.ordinal(), environment.name());
        }
    }
}
