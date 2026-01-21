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

package com.bytechef.automation.configuration.util;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Igor Beslic
 */
@Component
public class ProjectDeploymentFacadeHelper {

    public static final String PREFIX_PROJECT_DESCRIPTION = "Test description explains purpose of ";
    public static final String PREFIX_PROJECT_NAME = "Test Project ";
    public static final String PREFIX_PROJECT_DEPLOYMENT = "DEPLOYMENT OF ";
    public static final String PREFIX_TAG = "TAG_";
    public static final String PREFIX_CATEGORY = "CATEGORY_";

    private final CategoryRepository categoryRepository;
    private final ProjectFacade projectFacade;
    private final ProjectRepository projectRepository;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowRepository projectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentFacadeHelper(
        CategoryRepository categoryRepository, ProjectFacade projectFacade, ProjectRepository projectRepository,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowRepository projectWorkflowRepository) {

        this.categoryRepository = categoryRepository;
        this.projectFacade = projectFacade;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectRepository = projectRepository;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowRepository = projectWorkflowRepository;
    }

    public ProjectDTO createProject(long workspaceId) {
        Category category = categoryRepository.save(new Category(PREFIX_CATEGORY + RandomUtils.generatePassword()));

        ProjectDTO projectDTO = ProjectDTO.builder()
            .category(category)
            .description(randomDescription())
            .name(randomName())
            .tags(randomTags())
            .workspaceId(workspaceId)
            .build();

        return projectFacade.getProject(projectFacade.createProject(projectDTO));
    }

    public ProjectDeploymentDTO createProjectDeployment(long workspaceId, ProjectDTO projectDTO) {
        ProjectWorkflowDTO workflowDTO = addTestWorkflow(projectDTO);

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findById(workflowDTO.getProjectWorkflowId())
            .get();

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        ProjectDTO publishedProject = projectFacade.getProject(projectDTO.id());

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO =
            new ProjectDeploymentWorkflowDTO(List.of(), null, null, Map.of(), true, null, null, null, null, null, null,
                0,
                workflowDTO.getId(), projectWorkflow.getUuidAsString());

        Project dbProject = projectRepository.findById(publishedProject.id())
            .orElseThrow();

        // TODO this should be sorted and extracted to DTO to serve other callers
        ProjectVersion lastPublishedVersion = dbProject.getProjectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
            .toList()
            .getLast();

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectDTO.id())
            .name(PREFIX_PROJECT_DEPLOYMENT)
            .environment(Environment.DEVELOPMENT)
            .projectVersion(lastPublishedVersion.getVersion())
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        return projectDeploymentFacade.getProjectDeployment(
            projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO));
    }

    public ProjectDeploymentDTO createProjectDeploymentForEnvironment(
        long workspaceId, ProjectDTO projectDTO, Environment environment) {

        Project dbProject = projectRepository.findById(projectDTO.id())
            .orElseThrow();

        ProjectVersion lastPublishedVersion = dbProject.getProjectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
            .toList()
            .getLast();

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), lastPublishedVersion.getVersion())
            .stream()
            .findFirst()
            .orElseThrow();

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO =
            new ProjectDeploymentWorkflowDTO(List.of(), null, null, Map.of(), true, null, null, null, null, null, null,
                0, projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectDTO.id())
            .name(PREFIX_PROJECT_DEPLOYMENT + environment.name())
            .environment(environment)
            .projectVersion(lastPublishedVersion.getVersion())
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        return projectDeploymentFacade.getProjectDeployment(
            projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO));
    }

    public ProjectWorkflowDTO addTestWorkflow(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            Validate.notNull(project.getId(), "id"),
            "{\"label\": \"Test Workflow\", \"description\": \"Test Description\", \"tasks\": []}");

        return projectWorkflowFacade.getProjectWorkflow(projectWorkflow.getId());
    }

    private List<Tag> randomTags() {
        return List.of(
            new Tag(PREFIX_TAG + RandomUtils.generatePassword()), new Tag(PREFIX_TAG + RandomUtils.generatePassword()),
            new Tag(PREFIX_TAG + RandomUtils.generatePassword()));
    }

    private String randomName() {
        return PREFIX_PROJECT_NAME + RandomUtils.generatePassword();
    }

    private String randomDescription() {
        return PREFIX_PROJECT_DESCRIPTION + RandomUtils.generateActivationKey();
    }
}
