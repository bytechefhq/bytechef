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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.ProjectInstanceFacadeHelper;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class ProjectInstanceFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectInstanceFacade projectInstanceFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    ProjectInstanceFacadeHelper projectFacadeInstanceHelper;

    @AfterEach
    public void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();

    }

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));

        projectFacadeInstanceHelper = new ProjectInstanceFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectInstanceFacade, projectWorkflowRepository,
            tagRepository);
    }

    @Disabled
    @Test
    public void testCreateProjectInstance() {
        // TODO
    }

    @Disabled
    @Test
    public void testCreateProjectInstanceJob() {
        // TODO
    }

    @Test
    public void testDeleteProjectInstance() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());
        ProjectInstanceDTO projectInstanceDTO =
            projectFacadeInstanceHelper.createProjectInstance(workspace.getId(), projectDTO);

        ProjectInstanceDTO projectInstanceToDelete = projectInstanceFacade.getProjectInstance(projectInstanceDTO.id());

        projectInstanceFacade.deleteProjectInstance(projectInstanceToDelete.id());

        List<ProjectInstanceDTO> workspaceProjectInstances = projectInstanceFacade
            .getWorkspaceProjectInstances(workspace.getId(), Environment.TEST, projectInstanceToDelete.projectId(),
                null);

        assertThat(workspaceProjectInstances).hasSize(0);
    }

    @Disabled
    @Test
    public void testGetProjectInstance() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetProjectInstanceTags() {
        // TODO
    }

    @Disabled
    @Test
    public void testSearchProjectInstances() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdate() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdateProjectInstanceTags() {
        // TODO
    }
}
