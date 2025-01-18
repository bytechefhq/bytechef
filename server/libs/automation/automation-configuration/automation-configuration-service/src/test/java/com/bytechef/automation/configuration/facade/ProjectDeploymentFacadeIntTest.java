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

import com.bytechef.automation.configuration.ProjectDeploymentFacadeHelper;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.repository.CategoryRepository;
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
@ProjectIntTestConfigurationSharedMocks
public class ProjectDeploymentFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

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

        projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowRepository);
    }

    @Disabled
    @Test
    public void testCreateProjectDeployment() {
        // TODO
    }

    @Disabled
    @Test
    public void testCreateProjectDeploymentJob() {
        // TODO
    }

    @Test
    public void testDeleteProjectDeployment() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        ProjectDeploymentDTO projectDeploymentToDelete =
            projectDeploymentFacade.getProjectDeployment(projectDeploymentDTO.id());

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentToDelete.id());

        List<ProjectDeploymentDTO> workspaceProjectDeployments = projectDeploymentFacade.getWorkspaceProjectDeployments(
            workspace.getId(), Environment.TEST, projectDeploymentToDelete.projectId(), null, true);

        assertThat(workspaceProjectDeployments).hasSize(0);
    }

    @Disabled
    @Test
    public void testGetProjectDeployment() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetProjectDeploymentTags() {
        // TODO
    }

    @Disabled
    @Test
    public void testSearchProjectDeployments() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdate() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdateProjectDeploymentTags() {
        // TODO
    }
}
