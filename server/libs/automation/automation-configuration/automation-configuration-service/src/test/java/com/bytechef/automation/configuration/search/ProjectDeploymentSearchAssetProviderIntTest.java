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

package com.bytechef.automation.configuration.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class, properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectDeploymentSearchAssetProviderIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentSearchAssetProvider projectDeploymentSearchAssetProvider;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();

        for (Workflow workflow : workflowRepository.findAll()) {
            workflowRepository.deleteById(workflow.getId());
        }

        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testHidesDeploymentsOfProjectsOwnedByAnyEmbeddedOwner() {
        long catalogDeploymentId = createDeployment();
        long connectedUserDeploymentId = createDeployment();
        long plainDeploymentId = createDeployment();

        testOwnerContributors.addCatalogId(getProjectId(catalogDeploymentId));
        testOwnerContributors.addConnectedUserId(getProjectId(connectedUserDeploymentId));

        assertThat(search()).containsExactly(plainDeploymentId);
    }

    @Test
    void testHidesDeploymentsThatAFeatureOwns() {
        long apiCollectionDeploymentId = createDeployment();
        long a2aServerDeploymentId = createDeployment();
        long embeddedDeploymentId = createDeployment();
        long mcpServerDeploymentId = createDeployment();
        long plainDeploymentId = createDeployment();

        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.API_COLLECTION, apiCollectionDeploymentId);
        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.A2A_SERVER, a2aServerDeploymentId);
        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, embeddedDeploymentId);
        testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.MCP_SERVER, mcpServerDeploymentId);

        assertThat(search()).containsExactly(plainDeploymentId);
    }

    private long createDeployment() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        return projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO)
            .id();
    }

    private long getProjectId(long projectDeploymentId) {
        return projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow()
            .getProjectId();
    }

    private List<Long> search() {
        return projectDeploymentSearchAssetProvider.search("test project", 10, Set.of(workspace.getId()))
            .stream()
            .map(ProjectDeploymentSearchResult::id)
            .toList();
    }
}
