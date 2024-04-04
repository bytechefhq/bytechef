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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
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
    public void afterEach() {
        projectRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Project project = new Project();

        project.setName("name");

        project = projectRepository.save(project);

        Workflow workflow = projectFacade.addWorkflow(
            Validate.notNull(project.getId(), "id"),
            "{\"label\": \"New Workflow\", \"description\": \"Description\", \"tasks\": []}");

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("New Workflow");
    }

    @Test
    public void testCreate() {
        Category category = categoryRepository.save(new Category("name"));

        ProjectDTO projectDTO = ProjectDTO.builder()
            .category(category)
            .description("description")
            .name("name1")
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
            .tags(List.of(new Tag("tag1")))
            .build();

        projectDTO1 = projectFacade.createProject(projectDTO1);

        ProjectDTO projectDTO2 = ProjectDTO.builder()
            .name("name2")
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
        Project project = new Project();

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        List<ProjectDTO> projectsDTOs = projectFacade.getProjects(null, false, null, null);

        Assertions.assertThat(CollectionUtils.map(projectsDTOs, ProjectDTO::toProject))
            .isEqualTo(List.of(project));

        ProjectDTO projectDTO = projectsDTOs.get(0);

        assertThat(projectFacade.getProject(Validate.notNull(project.getId(), "id")))
            .isEqualTo(projectDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetProjectTags() {
        Project project = new Project();

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
        project.addWorkflowId(Validate.notNull(workflow.getId(), "id"));

        project = projectRepository.save(project);

        List<Workflow> workflows = projectFacade.getProjectWorkflows(Validate.notNull(project.getId(), "id"));

        List<String> ids = workflows.stream()
            .map(Workflow::getId)
            .toList();

        assertThat(ids).contains(workflow.getId());
    }

    @Test
    public void testUpdate() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .name("name")
            .tags(List.of(new Tag("tag1"), tagRepository.save(new Tag("tag2"))))
            .build();

        projectDTO = projectFacade.createProject(projectDTO);

        assertThat(projectDTO.tags()).hasSize(2);
        assertThat(projectDTO.workflowIds()).hasSize(1);

        projectDTO = ProjectDTO.builder()
            .id(projectDTO.id())
            .name("name")
            .tags(List.of(new Tag("tag1")))
            .workflowIds(projectDTO.workflowIds())
            .build();

        projectDTO = projectFacade.updateProject(projectDTO);

        assertThat(projectDTO.tags()).hasSize(1);
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
