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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacadeImpl;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectServiceIntTest {

    private Category category;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        category = categoryRepository.save(new Category("name"));
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Project project = getProject();

        Tag tag = tagRepository.save(new Tag("tag1"));

        project.setTags(List.of(tag));

        project = projectService.create(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("categoryId", category.getId())
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")));
    }

    @Test
    public void testDelete() {
        Project project = projectRepository.save(getProject());

        projectService.delete(Validate.notNull(project.getId(), "id"));

        assertThat(projectRepository.findById(project.getId()))
            .isNotPresent();
    }

    @Test
    public void testGetProject() {
        Project project = projectRepository.save(getProject());

        assertThat(project).isEqualTo(projectService.getProject(Validate.notNull(project.getId(), "id")));
    }

    @Test
    public void testGetProjects() {
        Project project = projectRepository.save(getProject());

        assertThat(projectService.getProjects()).hasSize(1);

        Category category = new Category("category1");

        category = categoryRepository.save(category);

        project.setCategory(category);

        project = projectRepository.save(project);

        assertThat(projectService.getProjects(null, category.getId(), null, null, null, null)).hasSize(1);

        assertThat(projectService.getProjects(null, Long.MAX_VALUE, null, null, null, null)).hasSize(0);

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        project.setTags(List.of(tag));

        projectRepository.save(project);

        assertThat(projectService.getProjects(null, null, false, tag.getId(), null, null)).hasSize(1);
        assertThat(projectService.getProjects(null, null, null, Long.MAX_VALUE, null, null)).hasSize(0);
        assertThat(projectService.getProjects(null, Long.MAX_VALUE, null, Long.MAX_VALUE, null, null)).hasSize(0);
    }

    @Test
    public void testGetProjectsExcludesEmbeddedOwnedProjects() {
        projectRepository.save(
            Project.builder()
                .name("Regular Project")
                .workspaceId(workspace.getId())
                .build());

        Project catalogProject = projectRepository.save(
            Project.builder()
                .name("Catalog")
                .workspaceId(workspace.getId())
                .build());

        Project connectedUserProject = projectRepository.save(
            Project.builder()
                .name("ext")
                .workspaceId(workspace.getId())
                .build());

        projectRepository.save(
            Project.builder()
                .name("abEMBEDDEDcd x")
                .workspaceId(workspace.getId())
                .build());

        testOwnerContributors.addCatalogId(catalogProject.getId());
        testOwnerContributors.addConnectedUserId(connectedUserProject.getId());

        List<Project> projects = projectService.getProjects(null, null, null, null, null, workspace.getId());

        assertThat(projects)
            .extracting(Project::getName)
            .containsExactlyInAnyOrder("Regular Project", "abEMBEDDEDcd x");
    }

    @Test
    public void testProjectTagsLeaveOutTheTagsOfOwnedProjects() {
        Tag ownedTag = tagRepository.save(new Tag("owned"));
        Tag tag = tagRepository.save(new Tag("regular"));

        Project ownedProject = getProject();

        ownedProject.setTags(List.of(ownedTag));

        ownedProject = projectRepository.save(ownedProject);

        Project project = getProject();

        project.setTags(List.of(tag));

        projectRepository.save(project);

        testOwnerContributors.addCatalogId(ownedProject.getId());

        TagService tagService = mock(TagService.class);

        when(tagService.getTags(anyList())).thenAnswer(
            invocation -> CollectionUtils.toList(tagRepository.findAllById(invocation.getArgument(0))));

        ProjectTagFacade projectTagFacade = new ProjectTagFacadeImpl(projectService, tagService);

        assertThat(projectTagFacade.getProjectTags())
            .extracting(Tag::getName)
            .containsExactly("regular");
    }

    @Test
    public void testProjectCategoriesLeaveOutTheCategoriesOfOwnedProjects() {
        Category ownedCategory = categoryRepository.save(new Category("owned"));

        Project ownedProject = getProject();

        ownedProject.setCategoryId(ownedCategory.getId());

        ownedProject = projectRepository.save(ownedProject);

        projectRepository.save(getProject());

        testOwnerContributors.addCatalogId(ownedProject.getId());

        CategoryService categoryService = mock(CategoryService.class);

        when(categoryService.getCategories(anyList())).thenAnswer(
            invocation -> CollectionUtils.toList(categoryRepository.findAllById(invocation.getArgument(0))));

        ProjectCategoryFacade projectCategoryFacade = new ProjectCategoryFacadeImpl(categoryService, projectService);

        assertThat(projectCategoryFacade.getProjectCategories(workspace.getId()))
            .extracting(Category::getName)
            .containsExactly("name");
    }

    @Test
    public void testGetProjectsExcludesOwnedProjectsEvenWhenTheyMatchEveryListFilter() {
        Tag tag = tagRepository.save(new Tag("tag1"));

        Project ownedProject = getProject();

        ownedProject.setTags(List.of(tag));

        ownedProject = projectRepository.save(ownedProject);

        testOwnerContributors.addCatalogId(ownedProject.getId());

        assertThat(
            projectService.getProjects(null, category.getId(), null, tag.getId(), Status.DRAFT, workspace.getId()))
                .isEmpty();

        testOwnerContributors.clear();

        assertThat(
            projectService.getProjects(null, category.getId(), null, tag.getId(), Status.DRAFT, workspace.getId()))
                .extracting(Project::getId)
                .containsExactly(ownedProject.getId());
    }

    @Test
    public void testGetProjectsHidesProjectsByOwnershipRegardlessOfLegacyPrefixedNames() {
        Project ownedProject = projectRepository.save(
            Project.builder()
                .name("__EMBEDDED__legacy")
                .workspaceId(workspace.getId())
                .build());
        Project unownedProject = projectRepository.save(
            Project.builder()
                .name("__EMBEDDED__legacy")
                .workspaceId(workspace.getId())
                .build());

        testOwnerContributors.addCatalogId(ownedProject.getId());

        assertThat(projectService.getProjects(null, null, null, null, null, workspace.getId()))
            .extracting(Project::getId)
            .containsExactly(unownedProject.getId());
    }

    @Test
    public void testGetProjectsKeepsAProjectWhoseDeploymentAnotherModuleOwns() {
        Project project = projectRepository.save(
            Project.builder()
                .name("Orders")
                .workspaceId(workspace.getId())
                .build());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("Orders MCP server");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        for (ProjectDeploymentOwner projectDeploymentOwner : ProjectDeploymentOwner.values()) {
            testOwnerContributors.addDeploymentId(projectDeploymentOwner, projectDeployment.getId());
        }

        assertThat(projectService.getProjects(null, null, null, null, null, workspace.getId()))
            .extracting(Project::getId)
            .containsExactly(project.getId());
        assertThat(projectService.getWorkspaceProjectIds(workspace.getId()))
            .containsExactly(project.getId());
    }

    @Test
    public void testGetWorkspaceProjectIdsExcludesEmbeddedOwnedProjects() {
        Project regularProject = projectRepository.save(
            Project.builder()
                .name("Regular Project")
                .workspaceId(workspace.getId())
                .build());

        Project catalogProject = projectRepository.save(
            Project.builder()
                .name("Catalog")
                .workspaceId(workspace.getId())
                .build());

        Project connectedUserProject = projectRepository.save(
            Project.builder()
                .name("ext")
                .workspaceId(workspace.getId())
                .build());

        testOwnerContributors.addCatalogId(catalogProject.getId());
        testOwnerContributors.addConnectedUserId(connectedUserProject.getId());

        assertThat(projectService.getWorkspaceProjectIds(workspace.getId()))
            .containsExactly(regularProject.getId());
    }

    @Test
    public void testFetchProjectByNameIgnoresOwnedProjects() {
        Project catalogProject = projectRepository.save(
            Project.builder()
                .name("Sales")
                .workspaceId(workspace.getId())
                .build());

        testOwnerContributors.addCatalogId(catalogProject.getId());

        assertThat(projectService.fetchProject("sales", workspace.getId())).isEmpty();
    }

    @Test
    public void testFetchProjectByNameReturnsTheUnownedProjectWhenAnOwnedOneSharesItsName() {
        Project catalogProject = projectRepository.save(
            Project.builder()
                .name("Sales")
                .workspaceId(workspace.getId())
                .build());

        Project regularProject = projectRepository.save(
            Project.builder()
                .name("Sales")
                .workspaceId(workspace.getId())
                .build());

        testOwnerContributors.addCatalogId(catalogProject.getId());

        assertThat(projectService.fetchProject("SALES", workspace.getId()))
            .isPresent()
            .map(Project::getId)
            .hasValue(regularProject.getId());
    }

    @Test
    public void testFetchProjectByNameNamesTheProjectAndWorkspaceWhenSeveralProjectsMatch() {
        projectRepository.save(
            Project.builder()
                .name("Sales")
                .workspaceId(workspace.getId())
                .build());
        projectRepository.save(
            Project.builder()
                .name("sales")
                .workspaceId(workspace.getId())
                .build());

        assertThatThrownBy(() -> projectService.fetchProject("SALES", workspace.getId()))
            .isInstanceOf(IncorrectResultSizeDataAccessException.class)
            .hasMessageContaining("'SALES'")
            .hasMessageContaining("workspace " + workspace.getId());
    }

    @Test
    public void testFetchProjectByNameLooksOnlyInTheGivenWorkspace() {
        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other"));

        Project otherWorkspaceProject = projectRepository.save(
            Project.builder()
                .name("Sales")
                .workspaceId(otherWorkspace.getId())
                .build());

        assertThat(projectService.fetchProject("Sales", workspace.getId())).isEmpty();
        assertThat(projectService.fetchProject("Sales", otherWorkspace.getId()))
            .map(Project::getId)
            .hasValue(otherWorkspaceProject.getId());
    }

    @Test
    public void testUpdate() {
        Project project = projectRepository.save(getProject());

        Tag tag = tagRepository.save(new Tag("tag2"));

        project.setDescription("description2");
        project.setName("name2");
        project.setTagIds(List.of(tag.getId()));

        project = projectService.update(project);

        Category category2 = categoryRepository.save(new Category("name2"));

        project.setCategory(category2);
        project.setDescription("description2");
        project.setName("name2");
        project.setTagIds(List.of(tag.getId()));

        project = projectService.update(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("categoryId", category2.getId())
            .hasFieldOrPropertyWithValue("description", "description2")
            .hasFieldOrPropertyWithValue("name", "name2")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()));
    }

    @Test
    public void testGetProjectsFilteredByStatusWithoutApiCollections() {
        Project project = projectRepository.save(getProject());

        List<Project> projects = projectService.getProjects(null, null, null, null, Status.DRAFT, null);

        assertThat(projects)
            .extracting(Project::getId)
            .contains(project.getId());
    }

    private Project getProject() {
        return Project.builder()
            .categoryId(category.getId())
            .description("description")
            .name("name")
            .workspaceId(workspace.getId())
            .build();
    }
}
