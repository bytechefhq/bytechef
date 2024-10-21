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

import static com.bytechef.automation.configuration.ProjectInstanceFacadeHelper.PREFIX_CATEGORY;
import static com.bytechef.automation.configuration.ProjectInstanceFacadeHelper.PREFIX_PROJECT_DESCRIPTION;
import static com.bytechef.automation.configuration.ProjectInstanceFacadeHelper.PREFIX_PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.ProjectInstanceFacadeHelper;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class ProjectFacadeIntTest {

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

    private ProjectInstanceFacadeHelper projectFacadeInstanceHelper;
    @Autowired
    private ProjectWorkflowServiceImpl projectWorkflowServiceImpl;

    private static final Random random = new SecureRandom();

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
            categoryRepository, projectFacade, projectRepository, projectInstanceFacade, projectWorkflowRepository);
    }

    @Test
    public void testAddWorkflow() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        ProjectWorkflowDTO workflowDTO = projectFacadeInstanceHelper.addTestWorkflow(projectDTO);

        ProjectWorkflow projectWorkflow =
            projectWorkflowServiceImpl.getProjectWorkflow(workflowDTO.getProjectWorkflowId());

        Optional<Workflow> workflowOptional = workflowRepository.findById(projectWorkflow.getWorkflowId());

        Assertions.assertTrue(workflowOptional.isPresent(), "Workflow not found");

        Workflow workflow = workflowOptional.get();

        assertThat(workflowDTO.getDescription()).isEqualTo(workflow.getDescription());
        assertThat(workflowDTO.getLabel()).isEqualTo(workflow.getLabel());
    }

    @Test
    public void testCreate() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        assertThat(projectDTO.category()
            .getName()).startsWith(PREFIX_CATEGORY);
        assertThat(projectDTO.description()).startsWith(PREFIX_PROJECT_DESCRIPTION);
        assertThat(projectDTO.name()).startsWith(PREFIX_PROJECT_NAME);
        assertThat(projectDTO.id()).isNotNull();
        assertThat(projectDTO.tags()).hasSize(3);
        assertThat(projectDTO.projectWorkflowIds()).hasSize(0);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(3);
    }

    @Test
    public void testDelete() {
        ProjectDTO projectDTO1 = projectFacadeInstanceHelper.createProject(workspace.getId());

        ProjectDTO projectDTO2 = projectFacadeInstanceHelper.createProject(workspace.getId());

        assertThat(projectRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(6);

        projectFacade.deleteProject(projectDTO1.id());

        assertThat(projectRepository.count()).isEqualTo(1);

        projectFacade.deleteProject(projectDTO2.id());

        assertThat(projectRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(6);
    }

    @Test
    public void testGetProject() {
        Project project = new Project();

        project.setWorkspaceId(workspace.getId());

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        assertThat(projectFacade.getProject(Validate.notNull(project.getId(), "id")))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("id", Validate.notNull(project.getId(), "id"))
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetProjects() {
        List<ProjectDTO> testProjectDTOs = new ArrayList<>();

        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));

        List<ProjectDTO> projectsDTOs = projectFacade.getProjects(null, false, null, null);

        assertThat(projectsDTOs).hasSize(testProjectDTOs.size());

        ProjectDTO projectDTO = projectsDTOs.get(random.nextInt(testProjectDTOs.size()));

        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        projectsDTOs = projectFacade.getProjects(category.getId(), false, null, null);

        assertThat(projectsDTOs).hasSize(1);

        assertThat(projectFacade.getProject(Validate.notNull(project.getId(), "id")))
            .isEqualTo(projectDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", projectDTO.tags());
    }

    @Test
    public void testGetProjectTags() {
        Project project = new Project();

        project.setWorkspaceId(workspace.getId());

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setName("name");
        project.setTags(List.of(tag1, tag2));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        project = new Project();

        project.setName("name2");
        project.setWorkspaceId(workspace.getId());

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        projectRepository.deleteById(Validate.notNull(project.getId(), "id"));

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    public void testGetProjectWorkflows() {
        Workflow workflow = new Workflow("{\"tasks\":[]}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Project project = new Project();

        project.setName("name");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        projectWorkflowRepository.save(
            new ProjectWorkflow(
                project.getId(), project.getLastProjectVersion(), Validate.notNull(workflow.getId(), "id"),
                "workflowReferenceCode"));

        List<ProjectWorkflowDTO> workflows = projectFacade.getProjectWorkflows(Validate.notNull(project.getId(), "id"));

        List<String> ids = workflows.stream()
            .map(ProjectWorkflowDTO::getId)
            .toList();

        assertThat(ids).contains(workflow.getId());
    }

    @Test
    public void testUpdate() {
        ProjectDTO project = projectFacadeInstanceHelper.createProject(workspace.getId());

        projectFacadeInstanceHelper.addTestWorkflow(project);

        assertThat(project.tags()).hasSize(3);

        assertThat(project.projectWorkflowIds()).hasSize(0);

        project = ProjectDTO.builder()
            .id(project.id())
            .name("Updated Name")
            .tags(List.of(new Tag("TAG_UPDATE")))
            .projectWorkflowIds(project.projectWorkflowIds())
            .version(project.version())
            .workspaceId(workspace.getId())
            .build();

        project = projectFacade.updateProject(project);

        assertThat(project.tags()).hasSize(1);
        assertThat(project.name()).isEqualTo("Updated Name");
    }

    @TestConfiguration
    public static class ProjectFacadeIntTestConfiguration {

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
