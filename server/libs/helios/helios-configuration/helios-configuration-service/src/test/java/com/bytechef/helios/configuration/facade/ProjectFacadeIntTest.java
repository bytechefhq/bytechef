
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.category.service.CategoryService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.helios.configuration.dto.ProjectDTO;
import com.bytechef.helios.configuration.repository.ProjectRepository;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
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
    TagRepository tagRepository;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @AfterEach
    @SuppressFBWarnings("NP")
    public void afterEach() {
        projectRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testAddWorkflow() {
        Project project = new Project();

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        project = projectRepository.save(project);

        Workflow workflow = projectFacade.addProjectWorkflow(project.getId(), "Workflow 1", "Description", null);

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("Workflow 1");
    }

    @Test
    public void testCreate() {
        Category category = categoryRepository.save(new Category("name"));

        ProjectDTO projectDTO = ProjectDTO.builder()
            .category(category)
            .description("description")
            .name("name1")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        projectDTO = projectFacade.createProject(projectDTO);

        assertThat(projectDTO.category()).isEqualTo(category);
        assertThat(projectDTO.description()).isEqualTo("description");
        assertThat(projectDTO.name()).isEqualTo("name1");
        assertThat(projectDTO.id()).isNotNull();
        assertThat(projectDTO.tags()).hasSize(1);
        assertThat(projectDTO.workflowIds()).hasSize(1);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(1);

        projectDTO = ProjectDTO.builder()
            .category(new Category("name"))
            .description("description")
            .name("name2")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .workflowIds(List.of("workflow2"))
            .build();

        projectDTO = projectFacade.createProject(projectDTO);

        assertThat(projectDTO.workflowIds()).hasSize(1);
        assertThat(projectDTO.workflowIds()).contains("workflow2");
    }

    @Test
    public void testDelete() {
        ProjectDTO projectDTO1 = ProjectDTO.builder()
            .name("name1")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        projectDTO1 = projectFacade.createProject(projectDTO1);

        ProjectDTO projectDTO2 = ProjectDTO.builder()
            .name("name2")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .build();

        projectDTO2 = projectFacade.createProject(projectDTO2);

        assertThat(projectRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        projectFacade.deleteProject(projectDTO1.id());

        assertThat(projectRepository.count()).isEqualTo(1);

        projectFacade.deleteProject(projectDTO2.id());

        assertThat(projectRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetProject() {
        Project project = new Project();

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        assertThat(projectFacade.getProject(project.getId()))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("id", project.getId())
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("status", Project.Status.UNPUBLISHED)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetProjects() {
        Project project = new Project();

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        List<ProjectDTO> projectsDTOs = projectFacade.getProjects(null, false, null);

        assertThat(CollectionUtils.map(projectsDTOs, ProjectDTO::toProject)).isEqualTo(List.of(project));

        ProjectDTO projectDTO = projectsDTOs.get(0);

        assertThat(projectFacade.getProject(project.getId()))
            .isEqualTo(projectDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProjectTags() {
        Project project = new Project();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);
        project.setTags(List.of(tag1, tag2));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        project = new Project();

        project.setName("name2");
        project.setStatus(Project.Status.UNPUBLISHED);

        tag1 = OptionalUtils.get(tagRepository.findById(tag1.getId()));

        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        projectRepository.deleteById(project.getId());

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProjectWorkflows() {
        Workflow workflow = new Workflow("{\"tasks\":[]}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Project project = new Project();

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);
        project.setWorkflowIds(List.of(workflow.getId()));

        project = projectRepository.save(project);

        List<Workflow> workflows = projectFacade.getProjectWorkflows(project.getId());

        assertThat(
            workflows.stream()
                .map(curWorkflow -> curWorkflow.getId())
                .toList())
                    .contains(workflow.getId());
    }

    @Test
    public void testUpdate() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .name("name")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1"), tagRepository.save(new Tag("tag2"))))
            .build();

        projectDTO = projectFacade.createProject(projectDTO);

        assertThat(projectDTO.tags()).hasSize(2);
        assertThat(projectDTO.workflowIds()).hasSize(1);

        projectDTO = ProjectDTO.builder()
            .id(projectDTO.id())
            .name("name")
            .status(Project.Status.UNPUBLISHED)
            .tags(List.of(new Tag("tag1")))
            .workflowIds(projectDTO.workflowIds())
            .build();

        projectDTO = projectFacade.updateProject(projectDTO);

        assertThat(projectDTO.tags()).hasSize(1);
    }

    @TestConfiguration
    public static class ProjectFacadeIntTestConfiguration {

        @Bean
        ProjectFacade projectFacade(
            CategoryService categoryService, ProjectInstanceService projectInstanceService,
            ProjectService projectService, TagService tagService, WorkflowService workflowService) {

            return new ProjectFacadeImpl(
                categoryService, projectInstanceService, projectService, tagService, workflowService);
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }
    }
}
