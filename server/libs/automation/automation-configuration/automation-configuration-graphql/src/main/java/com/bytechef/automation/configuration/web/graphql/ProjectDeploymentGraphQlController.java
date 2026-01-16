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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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

    @SchemaMapping(typeName = "ProjectDeployment", field = "environment")
    public EnvironmentDTO environment(ProjectDeployment projectDeployment) {
        return new EnvironmentDTO(environmentService.getEnvironment(projectDeployment.getEnvironmentId()));
    }

    @SchemaMapping(typeName = "ProjectDeployment", field = "project")
    public Project project(ProjectDeployment projectDeployment) {
        return projectService.getProject(projectDeployment.getProjectId());
    }

    @SchemaMapping(typeName = "ProjectDeployment", field = "tags")
    public List<Tag> tags(ProjectDeployment projectDeployment) {
        return tagService.getTags(projectDeployment.getTagIds());
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
