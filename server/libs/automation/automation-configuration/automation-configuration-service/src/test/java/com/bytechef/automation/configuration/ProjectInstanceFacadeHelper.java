/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.automation.configuration;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.automation.configuration.dto.ProjectInstanceWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectInstanceFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Igor Beslic
 */

public class ProjectInstanceFacadeHelper {

    public static final String PREFIX_PROJECT_DESCRIPTION = "Test description explains purpose of ";
    public static final String PREFIX_PROJECT_NAME = "Test Project ";
    public static final String PREFIX_PROJECT_INSTANCE = "INSTANCE OF ";
    public static final String PREFIX_TAG = "TAG_";
    public static final String PREFIX_CATEGORY = "CATEGORY_";

    private final CategoryRepository categoryRepository;

    private final ProjectFacade projectFacade;

    private final ProjectRepository projectRepository;

    private final ProjectInstanceFacade projectInstanceFacade;

    private final ProjectWorkflowRepository projectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectInstanceFacadeHelper(CategoryRepository categoryRepository, ProjectFacade projectFacade,
        ProjectRepository projectRepository, ProjectInstanceFacade projectInstanceFacade,
        ProjectWorkflowRepository projectWorkflowRepository) {

        this.categoryRepository = categoryRepository;
        this.projectFacade = projectFacade;
        this.projectInstanceFacade = projectInstanceFacade;
        this.projectRepository = projectRepository;
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

        return projectFacade.createProject(projectDTO);
    }

    public ProjectInstanceDTO createProjectInstance(long workspaceId, ProjectDTO projectDTO) {
        WorkflowDTO workflowDTO = addTestWorkflow(projectDTO);

        ProjectWorkflow projectWorkflow = projectWorkflowRepository.findById(workflowDTO.projectWorkflowId())
            .get();

        projectFacade.publishProject(projectDTO.id(), "Published for test");

        ProjectDTO publishedProject = projectFacade.getProject(projectDTO.id());

        ProjectInstanceWorkflowDTO projectInstanceWorkflowDTO =
            new ProjectInstanceWorkflowDTO(List.of(), null, null, Map.of(), true, null, null, null, null, null, null, 0,
                workflowDTO.id(), projectWorkflow.getWorkflowReferenceCode());

        Project dbProject = projectRepository.findById(publishedProject.id())
            .orElseThrow();

        // TODO this should be sorted and extracted to DTO to serve other callers
        ProjectVersion lastPublishedVersion = dbProject.getProjectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED)
            .toList()
            .getLast();

        ProjectInstanceDTO projectInstanceDTO = ProjectInstanceDTO.builder()
            .projectId(projectDTO.id())
            .name(PREFIX_PROJECT_INSTANCE)
            .environment(Environment.TEST)
            .projectVersion(lastPublishedVersion.getVersion())
            .projectInstanceWorkflows(List.of(projectInstanceWorkflowDTO))
            .build();

        return projectInstanceFacade.createProjectInstance(projectInstanceDTO);
    }

    public WorkflowDTO addTestWorkflow(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        return projectFacade.addWorkflow(
            Validate.notNull(project.getId(), "id"),
            "{\"label\": \"Test Workflow\", \"description\": \"Test Description\", \"tasks\": []}");
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
